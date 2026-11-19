package org.springframework.security.boot.yiban.client;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/** 管理与发起会话关联的一次性易班授权状态。 */
public final class YibanAuthorizationState {

    private static final String KEY = YibanAuthorizationState.class.getName();

    private YibanAuthorizationState() {
    }

    public static String issue(HttpServletRequest request) {
        String state = UUID.randomUUID().toString();
        HttpSession session = request.getSession();
        synchronized (WebUtils.getSessionMutex(session)) {
            session.setAttribute(KEY, state);
        }
        return state;
    }

    public static void consume(HttpServletRequest request) {
        String received = request.getParameter("state");
        HttpSession session = request.getSession(false);
        if (Objects.isNull(session) || !StringUtils.hasText(received)) {
            throw new BadCredentialsException("Missing Yiban authorization state");
        }
        synchronized (WebUtils.getSessionMutex(session)) {
            Object value = session.getAttribute(KEY);
            if (!(value instanceof String) || !MessageDigest.isEqual(
                    ((String) value).getBytes(StandardCharsets.UTF_8), received.getBytes(StandardCharsets.UTF_8))) {
                throw new BadCredentialsException("Invalid Yiban authorization state");
            }
            session.removeAttribute(KEY);
        }
    }
}
