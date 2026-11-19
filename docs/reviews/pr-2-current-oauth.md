# 易班 PR #2 的 OAuth 迁移

官网发布的 Java SDK 2.26.1103 提供 JDK 11 与 Jakarta Servlet 示例源码。本 Starter 保留 JDK 8 与 Spring Boot 2.7，复用已有 Apache HttpClient 实现该协议。[官方 Java SDK 下载](https://o.yiban.cn/wiki/index.php?page=SDK%E4%B8%8B%E8%BD%BD)

| 步骤 | 接口 | 验收 |
| --- | --- | --- |
| 发起授权 | `GET https://oauth.yiban.cn/code/authorize` | 发送应用 ID、注册回调和会话级随机 state |
| 换取令牌 | `POST https://oauth.yiban.cn/token/info` | 校验并消耗 state 后，以 UTF-8 表单发送 code、应用 ID、密钥与注册回调 |
| 读取用户 | `GET https://openapi.yiban.cn/user/me` | 成功响应须包含 `info.yb_userid`，再由应用的 `UserDetailsServiceAdapter` 完成本地认证 |

用户接口遵循公开 API 指定的 `access_token` 查询参数。学校实名验证属于易班独立接口，应用应根据自身业务规则选择。[易班 user/me 文档](https://o.yiban.cn/wiki/index.php?page=user/me)

原 `yibanAuthorize` Bean 现在返回 `YibanOAuthClient`。直接引用旧 SDK 类型的代码应迁移到新客户端。已有 `state` 属性可继续绑定，实际授权使用每次生成的一次性值。新版授权接口移除了 `display` 参数。若配置 `redirect_uri`，该值优先于 `callback`，授权和换令牌请求使用同一地址。应用需在易班开放平台注册该地址，并将它路由到 Starter 的登录入口。

集成测试使用本机 HTTP 模拟易班，覆盖自动装配、浏览器授权跳转、回调、一次性状态校验、用户映射以及失败响应。真实平台联调需要已注册的应用、密钥和可用回调地址。

Boot 2.7 消费应用使用 Spring Boot 父 POM 时，应将 `spring-security.version` 对齐到 Security Biz 所用的 `5.8.14`。JDK 8 下 `mvn clean verify` 通过全部 11 项测试，JaCoCo 行覆盖率为 91.4%，达到现有 90% 门禁。
