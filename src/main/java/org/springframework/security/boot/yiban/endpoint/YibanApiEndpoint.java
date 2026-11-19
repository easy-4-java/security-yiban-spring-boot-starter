package org.springframework.security.boot.yiban.endpoint;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.boot.yiban.client.YibanAuthorizationState;
import org.springframework.security.boot.yiban.client.YibanOAuthClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/yiban/api/")
public class YibanApiEndpoint {

    private final YibanOAuthClient authorize;

    public YibanApiEndpoint(YibanOAuthClient authorize) {
        this.authorize = authorize;
    }

    @GetMapping("login")
    public ResponseEntity<String> list(HttpServletRequest request) {
        return ResponseEntity.ok(authorize.authorizationUri(YibanAuthorizationState.issue(request)).toASCIIString());
    }
}
