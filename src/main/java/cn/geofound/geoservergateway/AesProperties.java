package cn.geofound.geoservergateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "geoserver.aes")
public class AesProperties {

    /** 旧版 CND_PARAMS 密钥；为兼容旧协议，它同时作为 CBC 初始向量。 */
    private String key = "qP2$bG9;vA0^uW0:";

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @PostConstruct
    public void validate() {
        int length = key == null ? 0 : key.getBytes(StandardCharsets.UTF_8).length;
        if (length != 16 && length != 24 && length != 32) {
            throw new IllegalStateException("geoserver.aes.key must be 16, 24, or 32 UTF-8 bytes");
        }
        // CBC 初始向量必须为 16 字节；旧协议直接由密钥派生初始向量。
        if (length != 16) {
            throw new IllegalStateException("geoserver.aes.key must be exactly 16 UTF-8 bytes because it is also the legacy CBC IV");
        }
    }
}
