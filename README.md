# GeoServer 网关

这是一个基于 Java 8、Spring Boot 2.5.15 和 Spring Cloud Gateway 2020.0.6 的 GeoServer
透明代理工程。

访问 `/gis_server/**` 时，网关会将 `/gis_server` 替换为 `geoserver.base-path`
（当前为 `/TDPJ_geoserver_proxy`），再转发到 `geoserver.upstream`。Gateway 原生透传请求方法、
请求头、请求体和二进制响应流，因此覆盖 WMS、WFS、WMTS、REST、静态资源，以及 GET、POST、PUT、
DELETE、PATCH 和 OPTIONS 请求。

## 配置

运行配置均位于 `src/main/resources/application.yml`：

| 配置项 | 当前值 |
| --- | --- |
| 监听端口 | `8080` |
| `geoserver.upstream` | `http://39.106.68.85:9051`，上游服务的协议、主机和端口，不包含路径 |
| `geoserver.gateway-path` | `/gis_server`，客户端访问本代理时使用的路径前缀 |
| `geoserver.base-path` | `/TDPJ_geoserver_proxy`，转发到上游时替换 `gateway-path` 的路径前缀 |
| AES 密钥 | 旧版密钥 |
| CORS 允许来源、方法和请求头 | `*` |

例如，客户端请求 `/gis_server/tdpj/wms` 时，网关会将其转发到
`http://39.106.68.85:9051/TDPJ_geoserver_proxy/tdpj/wms`，并保留请求查询参数。

## 测试代理

服务启动后，可在浏览器中访问以下 WMS `GetMap` 请求，验证代理是否能返回地图图片：

```text
https://fuzzy-yodel-jw6qpv46wcj5v6-8080.app.github.dev/gis_server/tdpj/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&FORMAT=image%2Fpng&TRANSPARENT=true&STYLES&LAYERS=tdpj%3At_9cbac00554d04ed68825d32b389ec54e&exceptions=application%2Fvnd.ogc.se_inimage&SRS=EPSG%3A4490&WIDTH=716&HEIGHT=769&BBOX=119.04164117574692%2C31.64896023273468%2C119.04547888040543%2C31.653082251548767
```

其中 `app.github.dev` 域名对应当前 VS Code 转发的 8080 端口；在其他环境测试时，将域名替换为该环境中服务的访问地址，路径及查询参数保持不变。

### 测试加密 CQL_FILTER

客户端不能直接传明文 `CQL_FILTER`，需要传加密后的 `CND_PARAMS`。下面的密文来自
`CndParamsFilterTest`，使用当前配置的 AES 密钥解密后对应过滤条件 `region_code like '43%'`；
网关会在转发时移除 `CND_PARAMS`，并添加解密后的 `CQL_FILTER`：

```text
https://fuzzy-yodel-jw6qpv46wcj5v6-8080.app.github.dev/gis_server/tdpj/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&FORMAT=image%2Fpng&TRANSPARENT=true&STYLES&LAYERS=tdpj%3At_9cbac00554d04ed68825d32b389ec54e&exceptions=application%2Fvnd.ogc.se_inimage&SRS=EPSG%3A4490&WIDTH=716&HEIGHT=769&BBOX=119.04164117574692%2C31.64896023273468%2C119.04547888040543%2C31.653082251548767&CND_PARAMS=PH0ZG1%2BqSs2ikj4KKTqmB91odWJalEmMAvwKRFW%2BSTU%3D
```

请确认目标图层包含 `region_code` 属性，否则 GeoServer 会因过滤字段不存在而返回错误。要直接测试网关是否拒绝明文过滤，可将请求参数改为 `CQL_FILTER=INCLUDE`；该请求应返回 HTTP 400。

AES 兼容协议严格采用 `AES/CBC/PKCS5Padding`、标准 Base64 和 UTF-8。为兼容既有调用方，
配置的 16 字节密钥同时作为 AES 密钥与 CBC 初始向量。

针对 `/gis_server/**`：明文 `CQL_FILTER`、重复或空的 `CND_PARAMS`、格式非法或无法解密的
`CND_PARAMS` 都将得到 HTTP 400。不传 `CND_PARAMS` 时请求会正常转发；合法的加密 `CND_PARAMS`
会在转发前被移除，并替换为解密后的 `CQL_FILTER`。

可使用以下命令执行测试：

```powershell
mvn test
```
