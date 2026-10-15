package cn.geofound.geoservergateway;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CndParamsFilterTest {

    private static final String GATEWAY_PATH = "/gis_server";
    private final CndParamsFilter filter = new CndParamsFilter(defaultAesProperties(), defaultGeoServerProperties());

    @Test
    void replacesEncryptedParameterWithCqlFilter() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get(GATEWAY_PATH + "/wms?SERVICE=WMS&CND_PARAMS=PH0ZG1%2BqSs2ikj4KKTqmB91odWJalEmMAvwKRFW%2BSTU%3D&LAYERS=land")
                .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<ServerWebExchange>();

        assertThat(exchange.getRequest().getQueryParams().getFirst("CND_PARAMS"))
                .isEqualTo("PH0ZG1%2BqSs2ikj4KKTqmB91odWJalEmMAvwKRFW%2BSTU%3D");

        filter.filter(exchange, capture(forwarded)).block();

        assertThat(forwarded.get().getRequest().getURI().getRawQuery())
                .isEqualTo("SERVICE=WMS&LAYERS=land&CQL_FILTER=(%20region_code%20like%20'43%25'%20%20)");
    }

    @Test
    void rejectsPlainCqlFilter() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get(GATEWAY_PATH + "/wms?CQL_FILTER=INCLUDE").build());

        filter.filter(exchange, capture(new AtomicReference<ServerWebExchange>())).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsDuplicateOrInvalidEncryptedParameters() {
        MockServerWebExchange duplicate = MockServerWebExchange.from(MockServerHttpRequest
                .get(GATEWAY_PATH + "/wms?CND_PARAMS=a&CND_PARAMS=b").build());
        MockServerWebExchange invalid = MockServerWebExchange.from(MockServerHttpRequest
                .get(GATEWAY_PATH + "/wms?CND_PARAMS=not-base64").build());
        MockServerWebExchange empty = MockServerWebExchange.from(MockServerHttpRequest
                .get(GATEWAY_PATH + "/wms?CND_PARAMS=").build());

        filter.filter(duplicate, capture(new AtomicReference<ServerWebExchange>())).block();
        filter.filter(invalid, capture(new AtomicReference<ServerWebExchange>())).block();
        filter.filter(empty, capture(new AtomicReference<ServerWebExchange>())).block();

        assertThat(duplicate.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalid.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(empty.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private GatewayFilterChain capture(final AtomicReference<ServerWebExchange> forwarded) {
        return exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        };
    }

    private static AesProperties defaultAesProperties() {
        AesProperties properties = new AesProperties();
        properties.validate();
        return properties;
    }

    private static GeoServerProperties defaultGeoServerProperties() {
        GeoServerProperties properties = new GeoServerProperties();
        properties.setUpstream("http://localhost:8080");
        properties.setGatewayPath(GATEWAY_PATH);
        properties.validate();
        return properties;
    }
}
