package cn.geofound.geoservergateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({GeoServerProperties.class, AesProperties.class})
public class GeoServerGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeoServerGatewayApplication.class, args);
    }
}
