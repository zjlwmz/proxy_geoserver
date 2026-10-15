package cn.geofound.geoservergateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 在路由前将唯一允许的加密 CND_PARAMS 参数转换为 CQL_FILTER。 */
@Component
public class CndParamsFilter implements GlobalFilter, Ordered {

    private static final String CND_PARAMS = "CND_PARAMS";
    private static final String CQL_FILTER = "CQL_FILTER";
    private final AesProperties aesProperties;
    private final GeoServerProperties geoServerProperties;

    public CndParamsFilter(AesProperties aesProperties, GeoServerProperties geoServerProperties) {
        this.aesProperties = aesProperties;
        this.geoServerProperties = geoServerProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String gatewayPath = geoServerProperties.normalizedGatewayPath();
        if (!gatewayPath.equals(path) && !path.startsWith(gatewayPath + "/")) {
            return chain.filter(exchange);
        }

        MultiValueMap<String, String> query = exchange.getRequest().getQueryParams();
        MultiValueMap<String, String> rewritten = new LinkedMultiValueMap<String, String>();
        List<String> encryptedValues = new ArrayList<String>();
        try {
            for (Map.Entry<String, List<String>> entry : query.entrySet()) {
                String parameterName = decode(entry.getKey());
                if (CQL_FILTER.equals(parameterName)) {
                    return badRequest(exchange, "CQL_FILTER must be supplied as encrypted CND_PARAMS");
                }
                for (String value : entry.getValue()) {
                    if (CND_PARAMS.equals(parameterName)) {
                        encryptedValues.add(value == null ? null : decode(value));
                    } else {
                        rewritten.add(parameterName, value == null ? null : decode(value));
                    }
                }
            }
        } catch (IllegalArgumentException ex) {
            return badRequest(exchange, "Query parameter encoding is invalid");
        }
        if (encryptedValues.isEmpty()) {
            return chain.filter(exchange);
        }
        if (encryptedValues.size() != 1 || encryptedValues.get(0) == null || encryptedValues.get(0).trim().isEmpty()) {
            return badRequest(exchange, "CND_PARAMS must occur exactly once and cannot be empty");
        }

        final String cqlFilter;
        try {
            cqlFilter = new LegacyAesCipher(aesProperties.getKey()).decrypt(encryptedValues.get(0));
        } catch (Exception ex) {
            return badRequest(exchange, "CND_PARAMS is invalid or cannot be decrypted");
        }

        rewritten.add(CQL_FILTER, cqlFilter);

        URI newUri = UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
                .replaceQueryParams(rewritten)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
        ServerHttpRequest request = exchange.getRequest().mutate().uri(newUri).build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    private Mono<Void> badRequest(ServerWebExchange exchange, String message) {
        byte[] body = ("{\"code\":400,\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private String decode(String value) {
        return org.springframework.web.util.UriUtils.decode(value, StandardCharsets.UTF_8);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
