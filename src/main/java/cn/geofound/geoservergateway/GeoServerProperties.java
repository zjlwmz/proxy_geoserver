package cn.geofound.geoservergateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.net.URI;

@ConfigurationProperties(prefix = "geoserver")
public class GeoServerProperties {

    /** 上游服务器源地址，不包含 GeoServer 上下文路径。 */
    private String upstream;

    /** 对外暴露的网关路径前缀。 */
    private String gatewayPath;

    /** 移除对外的 /server 前缀后，要补到路径前的 GeoServer 上下文路径。 */
    private String basePath;

    public String getUpstream() {
        return upstream;
    }

    public void setUpstream(String upstream) {
        this.upstream = upstream;
    }

    public String getGatewayPath() {
        return gatewayPath;
    }

    public void setGatewayPath(String gatewayPath) {
        this.gatewayPath = gatewayPath;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    @PostConstruct
    public void validate() {
        URI uri = URI.create(upstream);
        if (uri.getScheme() == null || uri.getHost() == null || uri.getRawPath() != null && !uri.getRawPath().isEmpty()
                || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalStateException("geoserver.upstream must be an absolute origin without a path, query, or fragment");
        }
        normalizedGatewayPath();
        normalizedBasePath();
    }

    public String normalizedGatewayPath() {
        if (gatewayPath == null || gatewayPath.trim().isEmpty()) {
            throw new IllegalStateException("geoserver.gateway-path must not be blank");
        }
        String path = gatewayPath.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public String normalizedBasePath() {
        if (basePath == null || basePath.trim().isEmpty()) {
            return "";
        }
        String path = basePath.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
