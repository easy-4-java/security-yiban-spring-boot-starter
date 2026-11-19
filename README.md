<a id="readme-top"></a>

<div align="center">

# security-yiban-spring-boot-starter

**Spring Boot Starter for security-yiban**

[![Java](https://img.shields.io/badge/Java-8-orange)](#3-requirements-and-compatibility)
[![License](https://img.shields.io/badge/license-Apache-2.0-green)](https://www.apache.org/licenses/LICENSE-2.0)

[简体中文](./README.zh-CN.md) | [English](./README.md)

[Positioning](#1-positioning) · [Capabilities](#2-core-capabilities) ·
[Dependencies](#5-dependencies) · [Enable](#6-enable-yiban-configuration) ·
[Configuration](#7-configuration-reference) · [Versions](#8-version-lines-and-compatibility) ·
[Build](#9-build-and-test) · [License](#12-license)

</div>

---

> **Current Version**：`1.0.0-SNAPSHOT`<br>
> **JDK Baseline**：`8`<br>
> **Group ID**：`io.github.easy4j`<br>
> **Artifact ID**：`security-yiban-spring-boot-starter`<br>
> **License**：Apache License 2.0<br>

## 1. Positioning

**security-yiban-spring-boot-starter** is a Spring Boot starter that integrates **security-yiban** for applications using security-yiban. It provides auto-configuration, property binding, and ready-to-use beans so that applications can consume security-yiban capabilities with minimal setup.

| Dimension | Description |
|---|---|
| Type | Spring Boot Starter |
| Consumers | Spring Boot applications using security-yiban |
| Core Capabilities | auto-configuration, property binding, ready-to-use beans for security-yiban |
| JDK | `8` |
| Source POM coordinates | `io.github.easy4j:security-yiban-spring-boot-starter:1.0.0-SNAPSHOT` |
| Config Prefix | `spring.security.yiban` |

## 2. Core Capabilities

| Capability | Status | Description |
|---|:---:|---|
| Auto-configuration | Declared in source | Registers security-yiban beans automatically |
| Property Binding | Declared in source | Binds `spring.security.yiban.authc.*` to `SecurityYibanAuthcProperties` |
| `YibanOAuthClient` bean | Declared in source | Auto-registered via SecurityYibanAutoConfiguration |

## 3. Requirements and Compatibility

| Dependency | Minimum | Evidence |
|---|---:|---|
| JDK | `8` | `pom.xml` |
| Spring Boot | `2.3.12.RELEASE` | `pom.xml` parent |
| Maven | `3.0` | Maven Enforcer |

For Boot 2.3 applications using the Boot parent, set `spring-framework.version` to `5.3.39` and
`spring-security.version` to `5.8.14` in the application POM. Security Biz is pinned to the Easy4J
snapshot build `2.7.x.20260630-20261029.080305-2`, which is verified with Boot 2.3.12 and JDK 8
in this starter. Use the Easy4J Maven snapshot settings when building or consuming it.

The starter implements the OAuth contract from the official Yiban Java example (2.26.1103) with its existing Apache HttpClient. It uses the JVM trust store and hostname verification and disables redirects for API requests.

## 4. Auto-configuration

The starter auto-configures the following beans:

| Bean | Condition | Missing Behavior |
|---|---|---|
| `YibanOAuthClient` | classpath + property | not created |
| `SecurityContextLogoutHandler` | classpath + property | not created |
| `YibanMatchedAuthenticationEntryPoint` | classpath + property | not created |
| `YibanMatchedAuthenticationFailureHandler` | classpath + property | not created |
| `YibanAuthenticationProvider` | classpath + property | not created |

Boot 2.3 discovers these configurations through `META-INF/spring.factories`. The registered callback must route to `/login/yiban` by default, or to the configured `spring.security.yiban.authc.login-url`.

## 5. Dependencies

Use the starter with the Easy4J Security Biz dependency available from the snapshot repository. Provide a `UserDetailsServiceAdapter` bean to map verified Yiban credentials to a local account.

## 6. Enable Yiban configuration

For an application that already loads the Yiban configuration:

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

## 7. Configuration Reference

### 7.1 Config Prefix

`spring.security.yiban`

### 7.2 Configuration Items

| Property | Type | Default | Required | Description | Sensitive |
|---|---|---|:---:|---|:---:|
| `spring.security.yiban.enabled` | boolean | `false` | No | Enable the starter | No |
<!-- additional properties below -->

Authorization uses a one-time session state. Check the [OAuth migration notes](docs/reviews/pr-2-current-oauth.md) when updating existing Yiban integrations.

## 8. Version Lines and Compatibility

| Branch | JDK | Spring Boot | Component Version | Status |
|---|---:|---:|---|:---:|
| `2.3.x` / `2.7.x` | `8+` | 2.3.x / 2.7.x | `1.0.x` | Maintenance |
| `3.0.x` ~ `3.5.x` | `17` | 3.x | `2.0.x` | Maintenance |
| `4.1.x` (`76dfef6`) | `21` | 4.1.2 | `4.1.x.20260630-SNAPSHOT` | Compilation blocked by missing SDK and authentication classes |

## 9. Build and Test

```bash
mvn -U clean verify
```

## 10. Troubleshooting

| Symptom | Diagnosis | Resolution |
|---|---|---|
| Bean not created | Check auto-configuration report | Verify `spring.security.yiban.enabled=true` and classpath |
| `ClassNotFoundException` | Missing dependency | Add the required module |
| Version conflict | `mvn dependency:tree` | Use BOM for version alignment |

## 11. Contribution

1. Fork the repository.
2. Create a feature branch.
3. Run `mvn clean verify` before submitting.
4. Submit a pull request.

## 12. License

This project is licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

---

<div align="center">

[Back to top](#readme-top) · [Issues](https://github.com/easy-4-java/security-yiban-spring-boot-starter/issues) · [Repository](https://github.com/easy-4-java/security-yiban-spring-boot-starter)

</div>
