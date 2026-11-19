package org.springframework.security.boot;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.boot.biz.userdetails.UserDetailsServiceAdapter;
import org.springframework.security.boot.yiban.authentication.YibanAuthenticationProvider;
import org.springframework.security.boot.yiban.authentication.YibanMatchedAuthenticationEntryPoint;
import org.springframework.security.boot.yiban.authentication.YibanMatchedAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import org.springframework.security.boot.yiban.client.YibanOAuthClient;
import org.springframework.util.StringUtils;

@Configuration
@AutoConfigureBefore(SecurityBizAutoConfiguration.class)
@ConditionalOnProperty(prefix = SecurityYibanProperties.PREFIX, value = "enabled", havingValue = "true")
@EnableConfigurationProperties({ SecurityYibanProperties.class, SecurityYibanAuthcProperties.class })
public class SecurityYibanAutoConfiguration {

	/**
	 * 创建使用当前易班 OAuth 接口的客户端。
	 */
	@Bean
	@ConditionalOnMissingBean(YibanOAuthClient.class)
	public YibanOAuthClient yibanAuthorize(SecurityYibanAuthcProperties authcProperties) {
		String redirectUri = authcProperties.getRedirect_uri();
		if (!StringUtils.hasText(redirectUri)) {
			redirectUri = authcProperties.getCallback();
		}
		return new YibanOAuthClient(authcProperties.getAppKey(), authcProperties.getAppSecret(), redirectUri);
	}


	@Bean("yibanSecurityContextLogoutHandler")
	public SecurityContextLogoutHandler yibanSecurityContextLogoutHandler(SecurityYibanAuthcProperties authcProperties) {

		SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
		logoutHandler.setClearAuthentication(authcProperties.getLogout().isClearAuthentication());
		logoutHandler.setInvalidateHttpSession(authcProperties.getLogout().isInvalidateHttpSession());

		return logoutHandler;
	}

	@Bean
	@ConditionalOnMissingBean
	public YibanMatchedAuthenticationEntryPoint yibanMatchedAuthenticationEntryPoint() {
		return new YibanMatchedAuthenticationEntryPoint();
	}

	@Bean
	@ConditionalOnMissingBean
	public YibanMatchedAuthenticationFailureHandler yibanMatchedAuthenticationFailureHandler() {
		return new YibanMatchedAuthenticationFailureHandler();
	}

	@Bean
	public YibanAuthenticationProvider yibanAuthenticationProvider(UserDetailsServiceAdapter userDetailsService,
			YibanOAuthClient yibanAuthorize) {
		return new YibanAuthenticationProvider(userDetailsService, yibanAuthorize);
	}

}
