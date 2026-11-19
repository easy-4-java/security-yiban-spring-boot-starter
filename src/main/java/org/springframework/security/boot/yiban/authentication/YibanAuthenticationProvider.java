package org.springframework.security.boot.yiban.authentication;

import java.util.Locale;

import org.springframework.security.boot.yiban.client.YibanOAuthClient;
import org.springframework.util.StringUtils;
import java.util.Objects;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.boot.biz.userdetails.SecurityPrincipal;
import org.springframework.security.boot.biz.userdetails.UserDetailsServiceAdapter;
import org.springframework.security.boot.yiban.exception.AuthenticationYibanServerException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSONObject;


public class YibanAuthenticationProvider implements AuthenticationProvider {
	
	protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
	private final YibanOAuthClient yibanOAuthClient;
    private final UserDetailsServiceAdapter userDetailsService;
    private UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();
    
    public YibanAuthenticationProvider(UserDetailsServiceAdapter userDetailsService, YibanOAuthClient yibanOAuthClient) {
        this.userDetailsService = userDetailsService;
        this.yibanOAuthClient = yibanOAuthClient;
    }

    /**
     * 
     * <p>完成匹配Token的认证，这里返回的对象最终会通过：SecurityContextHolder.getContext().setAuthentication(authResult); 放置在上下文中</p>
     * @author <a href="https://github.com/loong10k">Loong Wan</a>
     * @param authentication  {@link YibanAuthenticationToken IdentityCodeAuthenticationToken} 对象
     * @return 认证结果{@link Authentication}对象
     * @throws AuthenticationException  认证失败会抛出异常
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        
    	Assert.notNull(authentication, "No authentication data provided");
    	
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof String) || !StringUtils.hasText((String) principal)) {
            throw new BadCredentialsException("Missing Yiban access token");
        }
        JSONObject profile = yibanOAuthClient.userInfo((String) principal);
        JSONObject info = profile.getJSONObject("info");
        if ("error".equalsIgnoreCase(profile.getString("status")) && Objects.nonNull(info)) {
            String messageKey = Locale.CHINESE.getLanguage().equals(LocaleContextHolder.getLocale().getLanguage())
                    ? "msgCN" : "msgEN";
            throw new AuthenticationYibanServerException(info.getString("code"), info.getString(messageKey));
        }
        if (!"success".equalsIgnoreCase(profile.getString("status")) || Objects.isNull(info)
                || !StringUtils.hasText(info.getString("yb_userid"))) {
            throw new BadCredentialsException("Missing Yiban user identity");
        }

        UserDetails ud = getUserDetailsService().loadUserDetails(authentication);
        
        // User Status Check
        getUserDetailsChecker().check(ud);
        
        YibanAuthenticationToken authenticationToken = null;
        if(SecurityPrincipal.class.isAssignableFrom(ud.getClass())) {
        	authenticationToken = new YibanAuthenticationToken(ud, ud.getPassword(), ud.getAuthorities());        	
        } else {
        	authenticationToken = new YibanAuthenticationToken(ud.getUsername(), ud.getPassword(), ud.getAuthorities());
		}
        authenticationToken.setDetails(authentication.getDetails());
        
        return authenticationToken;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (YibanAuthenticationToken.class.isAssignableFrom(authentication));
    }

	public void setUserDetailsChecker(UserDetailsChecker userDetailsChecker) {
		this.userDetailsChecker = userDetailsChecker;
	}

	public UserDetailsChecker getUserDetailsChecker() {
		return userDetailsChecker;
	}

	public UserDetailsServiceAdapter getUserDetailsService() {
		return userDetailsService;
	}
    
}
