# 易班 SSL PR #2 的 Boot 4.0 构建诊断

以 SSL 功能分支提交 `a418826` 为基线。JDK 21 诊断构建使用当前可解析的 Easy4J Security Biz 4.0 快照和仓库附带的 `YBApi_sdk_java.zip`。其中 `YBOpenApi.jar` 的 SHA-256 为 `9dfce10cbca8776f2b6fa144d770385b94c5054bbc01544fe1983df05c67486c`，临时 Maven 仓库使用单独版本 `20150820-local`。

依赖解析后，`clean verify` 挡在 Java 编译阶段。自动配置还引用旧包中的 `ServerProperties` 和 `LocaleContextFilter`，也继续继承当前 Security Biz 4.0 快照中不可用的 `WebSecurityBizConfigurerAdapter`。授权过滤器使用 Spring Security 7 中无法解析的 `AntPathRequestMatcher`。本次未执行测试。

功能分支包含 SSL 坐标修复与编译诊断。依赖来源恢复后仍需处理安全过滤链配置与版本兼容，并验证登录发起、OAuth 回调、失败响应和过滤器注册。[PR #2](https://github.com/easy-4-java/security-yiban-spring-boot-starter/pull/2)
