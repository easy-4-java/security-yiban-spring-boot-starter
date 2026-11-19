package org.springframework.security.boot;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.extension.web.servlet.i18n.LocaleContextFilter;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.boot.biz.authentication.AuthenticationListener;
import org.springframework.security.boot.biz.authentication.nested.MatchedAuthenticationEntryPoint;
import org.springframework.security.boot.biz.authentication.nested.MatchedAuthenticationFailureHandler;
import org.springframework.security.boot.biz.authentication.nested.MatchedAuthenticationSuccessHandler;
import org.springframework.security.boot.biz.property.SecuritySessionMgtProperties;
import org.springframework.security.boot.utils.StringUtils;
import org.springframework.security.boot.utils.WebSecurityUtils;
import org.springframework.security.boot.yiban.authentication.YibanAuthorizationProcessingFilter;
import org.springframework.security.boot.yiban.authentication.YibanPreAuthenticatedProcessingFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import org.springframework.security.boot.yiban.client.YibanOAuthClient;

@Configuration
@AutoConfigureAfter(name = {
	"org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
})
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = SecurityYibanProperties.PREFIX, value = "enabled", havingValue = "true")
@EnableConfigurationProperties({ SecurityYibanProperties.class, SecurityBizProperties.class, ServerProperties.class })
public class SecurityYibanFilterConfiguration {
	
	@Configuration
	@EnableConfigurationProperties({ SecurityYibanProperties.class, SecurityYibanAuthcProperties.class, SecurityBizProperties.class })
	static class YibanWebSecurityConfigurerAdapter extends WebSecurityBizConfigurerAdapter {
		
		private final SecurityYibanAuthcProperties authcProperties;
		private final YibanOAuthClient yibanAuthorize;

		private final LocaleContextFilter localeContextFilter;
	    private final AuthenticationEntryPoint authenticationEntryPoint;
	    private final AuthenticationSuccessHandler authenticationSuccessHandler;
	    private final AuthenticationFailureHandler authenticationFailureHandler;
    	private final RememberMeServices rememberMeServices;
		private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
		
		public YibanWebSecurityConfigurerAdapter(
				
				SecurityBizProperties bizProperties,
				SecuritySessionMgtProperties sessionMgtProperties,
				SecurityYibanAuthcProperties authcProperties,

				ObjectProvider<YibanOAuthClient> authorizeProvider,
				ObjectProvider<LocaleContextFilter> localeContextProvider,
				ObjectProvider<AuthenticationProvider> authenticationProvider,
   				ObjectProvider<AuthenticationListener> authenticationListenerProvider,
   				ObjectProvider<MatchedAuthenticationEntryPoint> authenticationEntryPointProvider,
   				ObjectProvider<MatchedAuthenticationSuccessHandler> authenticationSuccessHandlerProvider,
   				ObjectProvider<MatchedAuthenticationFailureHandler> authenticationFailureHandlerProvider,
   				ObjectProvider<RememberMeServices> rememberMeServicesProvider,
   				ObjectProvider<SessionAuthenticationStrategy> sessionAuthenticationStrategyProvider
				
				) {
			
			super(bizProperties, sessionMgtProperties, authenticationProvider.stream().collect(Collectors.toList()));
			
			this.authcProperties = authcProperties;

			this.yibanAuthorize = authorizeProvider.getIfAvailable();
			this.localeContextFilter = localeContextProvider.getIfAvailable();
   			List<AuthenticationListener> authenticationListeners = authenticationListenerProvider.stream().collect(Collectors.toList());
   			this.authenticationEntryPoint = WebSecurityUtils.authenticationEntryPoint(authcProperties, sessionMgtProperties, authenticationEntryPointProvider.stream().collect(Collectors.toList()));
   			this.authenticationSuccessHandler = WebSecurityUtils.authenticationSuccessHandler(authcProperties, sessionMgtProperties, authenticationListeners, authenticationSuccessHandlerProvider.stream().collect(Collectors.toList()));
   			this.authenticationFailureHandler = WebSecurityUtils.authenticationFailureHandler(authcProperties, sessionMgtProperties, authenticationListeners, authenticationFailureHandlerProvider.stream().collect(Collectors.toList()));
   			this.rememberMeServices = rememberMeServicesProvider.getIfAvailable();
   			this.sessionAuthenticationStrategy = sessionAuthenticationStrategyProvider.getIfAvailable();
		}
		
		public YibanPreAuthenticatedProcessingFilter preAuthenticatedProcessingFilter() throws Exception {
	    	
			return new YibanPreAuthenticatedProcessingFilter(yibanAuthorize,
					authcProperties.getLoginUrl(), authcProperties.getAuthorizationParamName());
	    }
		
		public YibanAuthorizationProcessingFilter authenticationProcessingFilter() throws Exception {
	    	
			YibanAuthorizationProcessingFilter authcFilter = new YibanAuthorizationProcessingFilter(yibanAuthorize);
			authcFilter.setAuthorizationParamName(authcProperties.getAuthorizationParamName());

			authcFilter.setAllowSessionCreation(getSessionMgtProperties().isAllowSessionCreation());
			authcFilter.setAuthenticationFailureHandler(authenticationFailureHandler);
			authcFilter.setAuthenticationManager(authenticationManagerBean());
			authcFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
			authcFilter.setContinueChainBeforeSuccessfulAuthentication(authcProperties.isContinueChainBeforeSuccessfulAuthentication());
			
			if (StringUtils.hasText(authcProperties.getLoginUrl())) {
				authcFilter.setFilterProcessesUrl(authcProperties.getLoginUrl());
			}
			if (Objects.nonNull(rememberMeServices)) {
				authcFilter.setRememberMeServices(rememberMeServices);
			}
			if (Objects.nonNull(sessionAuthenticationStrategy)) {
				authcFilter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy);
			}
			
	        return authcFilter;
	    }
		
		@Bean
		@Order(100)
		SecurityFilterChain yibanSecurityFilterChain(HttpSecurity http) throws Exception {
			configure(http);
			return http.build();
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			if (Objects.nonNull(localeContextFilter)) {
				http.addFilterBefore(localeContextFilter, UsernamePasswordAuthenticationFilter.class);
			}

			RequestMatcher login = new AntPathRequestMatcher(authcProperties.getLoginUrl());
			String pathPattern = authcProperties.getPathPattern();
			RequestMatcher scope = StringUtils.hasText(pathPattern)
					? new OrRequestMatcher(login, new AntPathRequestMatcher(pathPattern)) : login;
			http.requestMatcher(scope)
	        	.exceptionHandling()
	        	.authenticationEntryPoint(authenticationEntryPoint)
	        	.and()
	        	.httpBasic()
	        	.disable()
	        	.addFilterBefore(preAuthenticatedProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
	        	.addFilterBefore(authenticationProcessingFilter(), UsernamePasswordAuthenticationFilter.class); 
	    	
	    	super.configure(http, authcProperties.getCors());
	    	super.configure(http, authcProperties.getCsrf());
	    	super.configure(http, authcProperties.getHeaders());
    	super.configure(http);
			
		}

	}
	
}
