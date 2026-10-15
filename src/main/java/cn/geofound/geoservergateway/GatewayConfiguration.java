package cn.geofound.geoservergateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.regex.Pattern;

@Configuration
public class GatewayConfiguration {

    @Bean
    public RouteLocator geoServerRoute(RouteLocatorBuilder builder, GeoServerProperties properties) {
        String gatewayPath = properties.normalizedGatewayPath();
        String replacement = properties.normalizedBasePath() + "${remaining}";
        return builder.routes()
                .route("geoserver-transparent-proxy", route -> route
                        .path(gatewayPath, gatewayPath + "/**")
                        .filters(filters -> filters.rewritePath(Pattern.quote(gatewayPath) + "(?<remaining>/?.*)", replacement))
                        .uri(properties.getUpstream()))
                .build();
    }
}
