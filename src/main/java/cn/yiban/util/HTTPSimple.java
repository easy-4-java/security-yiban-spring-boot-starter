package cn.yiban.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/** 保留旧版易班 SDK 的静态传输入口，使用 JVM 默认信任库和主机名验证。 */
@Slf4j
public final class HTTPSimple {

    private static final RequestConfig TIMEOUTS = RequestConfig.custom()
        .setConnectTimeout(5000)
        .setSocketTimeout(10000)
        .build();

    private HTTPSimple() {
    }

    /** 返回易班 GET 响应正文。 */
    public static String GET(String url) {
        return execute(new HttpGet(url));
    }

    /** 以 UTF-8 表单发送易班 POST 请求。 */
    public static String POST(String url, List<NameValuePair> parameters) {
        HttpPost request = new HttpPost(url);
        request.setEntity(new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8));
        return execute(request);
    }

    private static String execute(HttpUriRequest request) {
        try (CloseableHttpClient client = HttpClients.custom()
                .disableRedirectHandling()
                .setDefaultRequestConfig(TIMEOUTS)
                .build();
             CloseableHttpResponse response = client.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 300 && status < 400) {
                log.warn("Yiban API returned a redirect, status={}", status);
                return "";
            }
            HttpEntity entity = response.getEntity();
            return Objects.isNull(entity) ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.warn("Yiban API request failed, errorType={}", ex.getClass().getSimpleName());
            return "";
        }
    }
}
