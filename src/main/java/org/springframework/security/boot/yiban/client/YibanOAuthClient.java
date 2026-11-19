package org.springframework.security.boot.yiban.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/** 访问官方易班 OAuth 授权、换令牌和用户信息接口。 */
public final class YibanOAuthClient implements Closeable {

    private static final URI AUTHORIZATION = URI.create("https://oauth.yiban.cn/code/authorize");
    private static final URI TOKEN = URI.create("https://oauth.yiban.cn/token/info");
    private static final URI USER = URI.create("https://openapi.yiban.cn/user/me");

    private final CloseableHttpClient http;
    private final URI authorizationEndpoint;
    private final URI tokenEndpoint;
    private final URI userEndpoint;
    private final String appKey;
    private final String appSecret;
    private final String redirectUri;

    public YibanOAuthClient(String appKey, String appSecret, String redirectUri) {
        this(appKey, appSecret, redirectUri, AUTHORIZATION, TOKEN, USER);
    }

    YibanOAuthClient(String appKey, String appSecret, String redirectUri,
                     URI authorizationEndpoint, URI tokenEndpoint, URI userEndpoint) {
        Assert.hasText(appKey, "Yiban app key must be configured");
        Assert.hasText(appSecret, "Yiban app secret must be configured");
        Assert.hasText(redirectUri, "Yiban redirect URI must be configured");
        URI callback = URI.create(redirectUri);
        Assert.isTrue(("https".equalsIgnoreCase(callback.getScheme()) || "http".equalsIgnoreCase(callback.getScheme()))
                && Objects.nonNull(callback.getHost()) && Objects.isNull(callback.getFragment())
                && Objects.isNull(callback.getUserInfo()), "Invalid Yiban redirect URI");
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.redirectUri = redirectUri;
        this.authorizationEndpoint = authorizationEndpoint;
        this.tokenEndpoint = tokenEndpoint;
        this.userEndpoint = userEndpoint;
        this.http = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(5000)
                        .setConnectionRequestTimeout(5000).setSocketTimeout(10000).build())
                .disableRedirectHandling()
                .disableAutomaticRetries()
                .build();
    }

    /** 生成带一次性状态的授权地址。 */
    public URI authorizationUri(String state) {
        Assert.hasText(state, "Yiban authorization state is required");
        return build(new URIBuilder(authorizationEndpoint)
                .addParameter("client_id", appKey)
                .addParameter("redirect_uri", redirectUri)
                .addParameter("state", state));
    }

    /** 使用与授权请求相同的回调地址换取令牌。 */
    public String exchangeCode(String code) {
        Assert.hasText(code, "Yiban authorization code is required");
        HttpPost request = new HttpPost(tokenEndpoint);
        request.setEntity(new UrlEncodedFormEntity(Arrays.asList(
                new BasicNameValuePair("code", code),
                new BasicNameValuePair("client_secret", appSecret),
                new BasicNameValuePair("client_id", appKey),
                new BasicNameValuePair("redirect_uri", redirectUri)), StandardCharsets.UTF_8));
        String token = execute(request).getString("access_token");
        if (!StringUtils.hasText(token)) {
            throw new AuthenticationServiceException("Yiban did not return an access token");
        }
        return token;
    }

    /** 查询易班用户信息。调用方检查响应状态和用户标识。 */
    public JSONObject userInfo(String accessToken) {
        Assert.hasText(accessToken, "Yiban access token is required");
        return execute(new HttpGet(build(new URIBuilder(userEndpoint).addParameter("access_token", accessToken))));
    }

    private JSONObject execute(HttpUriRequest request) {
        request.setHeader("Accept", "application/json");
        try (CloseableHttpResponse response = http.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (status != HttpStatus.SC_OK || Objects.isNull(entity)) {
                throw new AuthenticationServiceException("Yiban API did not return a successful response");
            }
            JSONObject body = JSON.parseObject(EntityUtils.toString(entity, StandardCharsets.UTF_8));
            if (Objects.isNull(body)) {
                throw new AuthenticationServiceException("Yiban API returned an empty response");
            }
            return body;
        } catch (IOException | JSONException exception) {
            // 请求地址或错误响应可包含令牌。对外传递固定错误消息。
            throw new AuthenticationServiceException("Unable to read the Yiban API response");
        }
    }

    private static URI build(URIBuilder builder) {
        try {
            return builder.build();
        } catch (URISyntaxException exception) {
            throw new AuthenticationServiceException("Invalid Yiban request URI");
        }
    }

    @Override
    public void close() throws IOException {
        http.close();
    }
}
