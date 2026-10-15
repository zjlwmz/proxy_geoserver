# GeoServer 网关

这是一个基于 Java 8、Spring Boot 2.5.15 和 Spring Cloud Gateway 2020.0.6 的 GeoServer
透明代理工程。

访问 `/server/**` 时，网关会移除 `/server`，再加上 `geoserver.base-path`（默认
`/geoserver`），转发到 `geoserver.upstream`。Gateway 原生透传请求方法、请求头、请求体和
二进制响应流，因此覆盖 WMS、WFS、WMTS、REST、静态资源，以及 GET、POST、PUT、DELETE、PATCH
和 OPTIONS 请求。

## 配置

运行配置均位于 `src/main/resources/application.yml`：

| 配置项 | 当前值 |
| --- | --- |
| 监听端口 | `8080` |
| GeoServer 上游源地址 | `http://localhost:8081` |
| GeoServer 基路径 | `/geoserver` |
| AES 密钥 | 旧版密钥 |
| CORS 允许来源、方法和请求头 | `*` |

AES 兼容协议严格采用 `AES/CBC/PKCS5Padding`、标准 Base64 和 UTF-8。为兼容既有调用方，
配置的 16 字节密钥同时作为 AES 密钥与 CBC 初始向量。

针对 `/server/**`：明文 `CQL_FILTER`、缺失或重复的 `CND_PARAMS`、格式非法或无法解密的
`CND_PARAMS` 都将得到 HTTP 400。合法的加密 `CND_PARAMS` 会在转发前被移除，并替换为解密后的
`CQL_FILTER`。

可使用以下命令执行测试：

```powershell
mvn test
```
