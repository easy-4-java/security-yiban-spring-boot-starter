# 易班 SSL PR #2 的 Boot 2.7 构建诊断

以功能分支提交 `1f22cdaadb87a3a300f8abb6baaacbed3a857001` 为基线。JDK 8 诊断构建使用当前可解析的 Easy4J Security Biz 2.7 快照和仓库附带的 `YBApi_sdk_java.zip`。其中 `YBOpenApi.jar` 的 SHA-256 为 `9dfce10cbca8776f2b6fa144d770385b94c5054bbc01544fe1983df05c67486c`，临时 Maven 仓库使用单独版本 `20150820-local`。正式依赖仍需验证来源及版本。

1. 使用历史包进入编译后，过滤器的 `jakarta.servlet` 导入与 Boot 2.7 的 `javax.servlet` 不匹配。LocaleContextFilter 也需要使用当前 Spring Extension 中的包。
2. 临时修正导入后，唯一编译错误是 `AuthenticationYibanServerException#getCode()` 的 `String` 与 Security Biz 父类的 `int` 冲突。
3. 临时方案保留易班供应商错误码的 `String getCode()`，将其父类替换为 Spring Security 的 `AuthenticationServiceException`。错误响应测试通过，覆盖异常原因保留和两个内置处理器返回的第三方服务业务码。此方案涉及公共继承关系，待兼容性决策。
4. 此临时方案的 JDK 8 `clean verify` 执行了 1 项测试，零失败、零错误、零跳过。JaCoCo 行覆盖率约 8%，现有门禁是 90%，因此构建失败。

功能分支保存 SSL 坐标修复和诊断结果。临时 SDK 坐标及兼容方案没有进入发布 POM。[PR #2](https://github.com/easy-4-java/security-yiban-spring-boot-starter/pull/2)
