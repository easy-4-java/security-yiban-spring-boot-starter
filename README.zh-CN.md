<a id="readme-top"></a>

<div align="center">

# security-yiban-spring-boot-starter

**Spring Boot Starter，集成 security-yiban**

[![Java](https://img.shields.io/badge/Java-8-orange)](#3-运行要求与兼容性)
[![License](https://img.shields.io/badge/license-Apache-2.0-green)](https://www.apache.org/licenses/LICENSE-2.0)

[English](./README.md) | [简体中文](./README.zh-CN.md)

[项目定位](#1-项目定位) · [核心能力](#2-核心能力) ·
[引入依赖](#5-引入依赖) · [启用配置](#6-启用配置) ·
[配置参考](#7-配置参考) · [版本线](#8-版本线与兼容性) ·
[构建测试](#9-构建与测试) · [许可证](#12-许可证)

</div>

---

> **当前版本**：`1.0.0-SNAPSHOT`<br>
> **JDK 基线**：`8`<br>
> **Group ID**：`io.github.easy4j`<br>
> **Artifact ID**：`security-yiban-spring-boot-starter`<br>
> **许可证**：Apache License 2.0<br>

## 1. 项目定位

**security-yiban-spring-boot-starter** 是一个面向 使用 security-yiban 的应用 的 Spring Boot Starter，用于将 **security-yiban** 集成到 Spring Boot 应用中。它提供自动装配、属性绑定与开箱即用的 Bean，使应用以最小配置即可使用 security-yiban 的全部能力。

| 维度 | 说明 |
|---|---|
| 类型 | Spring Boot Starter |
| 消费方 | 使用 security-yiban 的 Spring Boot 应用 |
| 核心能力 | 自动装配、属性绑定、开箱即用的 security-yiban Bean |
| JDK | `8` |
| 当前源码坐标 | `io.github.easy4j:security-yiban-spring-boot-starter:1.0.0-SNAPSHOT` |
| 配置前缀 | `spring.security.yiban` |

## 2. 核心能力

| 能力 | 状态 | 说明 |
|---|:---:|---|
| 自动装配 | 源码已声明 | 自动注册 security-yiban 相关 Bean |
| 属性绑定 | 源码已声明 | 绑定 `spring.security.yiban.authc.*` 到 `SecurityYibanAuthcProperties` |
| `YibanOAuthClient` Bean | 源码已声明 | 通过 SecurityYibanAutoConfiguration 自动注册 |

## 3. 运行要求与兼容性

| 依赖 | 最低版本 | 证据来源 |
|---|---:|---|
| JDK | `8` | `pom.xml` |
| Spring Boot | `2.3.12.RELEASE` | `pom.xml` parent |
| Maven | `3.0` | Maven Enforcer |

使用 Boot 父 POM 的 Boot 2.3 应用，请在应用 POM 中将 `spring-framework.version` 设为
`5.3.39`、`spring-security.version` 设为 `5.8.14`。Security Biz 固定使用 Easy4J 已发布的
快照构建 `2.7.x.20260630-20261029.080305-2`，本 Starter 在 JDK 8 与 Boot 2.3.12 上验证其运行兼容性。
构建和消费该构件时需配置 Easy4J Maven 快照仓库。

Starter 使用现有 Apache HttpClient 实现易班 Java 官方示例 2.26.1103 的 OAuth 协议。接口请求使用 JVM 信任库和主机名校验，并禁用重定向。

## 4. 自动装配

Starter 自动装配以下 Bean：

| Bean | 条件 | 缺失时行为 |
|---|---|---|
| `YibanOAuthClient` | classpath + property | 不创建 |
| `SecurityContextLogoutHandler` | classpath + property | 不创建 |
| `YibanMatchedAuthenticationEntryPoint` | classpath + property | 不创建 |
| `YibanMatchedAuthenticationFailureHandler` | classpath + property | 不创建 |
| `YibanAuthenticationProvider` | classpath + property | 不创建 |

Boot 2.3 通过 `META-INF/spring.factories` 发现这些自动配置。注册回调默认应指向 `/login/yiban`，也可使用应用配置的 `spring.security.yiban.authc.login-url`。

## 5. 引入依赖

使用 Starter 时需能从 Easy4J 快照仓库解析 Security Biz，应用需提供 `UserDetailsServiceAdapter` Bean，把已验证的易班凭据映射到本地账号。

## 6. 启用配置

在已加载 Yiban 配置的应用中使用以下开关：

```yaml
spring:
  security:
    yiban:
      enabled: true
      authc:
        app-key: ${YIBAN_APP_KEY}
        app-secret: ${YIBAN_APP_SECRET}
        callback: https://app.example.com/login/yiban
```

## 7. 配置参考

### 7.1 配置前缀

`spring.security.yiban`

### 7.2 配置项

| 属性 | 类型 | 默认值 | 必填 | 说明 | 敏感 |
|---|---|---|:---:|---|:---:|
| `spring.security.yiban.enabled` | boolean | `false` | 否 | 是否启用 Starter | 否 |
<!-- 更多属性见下方 -->

授权流程使用会话内的一次性状态。升级已接入易班的应用时，请核对[迁移说明](docs/reviews/pr-2-current-oauth.md)。

## 8. 版本线与兼容性

| 分支 | JDK | Spring Boot | 组件版本 | 状态 |
|---|---:|---:|---|:---:|
| `2.3.x` / `2.7.x` | `8+` | 2.3.x / 2.7.x | `1.0.x` | 维护中 |
| `3.0.x` ~ `3.5.x` | `17` | 3.x | `2.0.x` | 维护中 |
| `4.1.x`（`76dfef6`） | `21` | 4.1.2 | `4.1.x.20260630-SNAPSHOT` | 缺少 SDK 和认证实现，编译失败 |

## 9. 构建与测试

```bash
mvn -U clean verify
```

## 10. 排障

| 症状 | 诊断 | 解决 |
|---|---|---|
| Bean 未创建 | 查看自动装配报告 | 确认 `spring.security.yiban.enabled=true` 与 classpath |
| `ClassNotFoundException` | 缺少依赖 | 引入对应模块 |
| 版本冲突 | `mvn dependency:tree` | 使用 BOM 统一版本 |

## 11. 贡献

1. Fork 本仓库。
2. 创建特性分支。
3. 提交前运行 `mvn clean verify`。
4. 提交 Pull Request。

## 12. 许可证

本项目采用 [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) 许可证。

---

<div align="center">

[返回顶部](#readme-top) · [问题反馈](https://github.com/easy-4-java/security-yiban-spring-boot-starter/issues) · [仓库地址](https://github.com/easy-4-java/security-yiban-spring-boot-starter)

</div>
