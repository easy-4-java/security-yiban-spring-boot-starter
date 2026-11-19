package org.springframework.security.boot.yiban;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.boot.biz.exception.AuthResponseCode;
import org.springframework.security.boot.yiban.authentication.YibanMatchedAuthenticationEntryPoint;
import org.springframework.security.boot.yiban.authentication.YibanMatchedAuthenticationFailureHandler;
import org.springframework.security.boot.yiban.exception.AuthenticationYibanServerException;

import static org.assertj.core.api.Assertions.assertThat;

class YibanErrorResponseTest {

    @Test
    void retainsTheVendorCodeAndTheStartersThirdPartyServiceResponse() throws Exception {
        IllegalStateException cause = new IllegalStateException("upstream unavailable");
        AuthenticationYibanServerException failure =
                new AuthenticationYibanServerException("YIBAN-429", "Please retry", cause);
        assertThat(failure.getCode()).isEqualTo("YIBAN-429");
        assertThat(failure.getAuthCode()).isEqualTo(AuthResponseCode.SC_AUTHZ_THIRD_PARTY_SERVICE);
        assertThat(failure.getCause()).isSameAs(cause);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse failedLogin = new MockHttpServletResponse();
        MockHttpServletResponse rejectedRequest = new MockHttpServletResponse();
        new YibanMatchedAuthenticationFailureHandler().onAuthenticationFailure(request, failedLogin, failure);
        new YibanMatchedAuthenticationEntryPoint().commence(request, rejectedRequest, failure);
        assertThat(failedLogin.getContentAsString()).isEqualTo(rejectedRequest.getContentAsString());
        assertThat(failedLogin.getStatus()).isEqualTo(200);
        assertThat(JSONObject.parseObject(failedLogin.getContentAsString()).getInteger("code"))
                .isEqualTo(AuthResponseCode.SC_AUTHZ_THIRD_PARTY_SERVICE.getCode());
    }
}
