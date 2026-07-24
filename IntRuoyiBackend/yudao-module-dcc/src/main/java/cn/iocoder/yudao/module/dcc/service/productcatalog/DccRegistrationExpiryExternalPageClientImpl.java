package cn.iocoder.yudao.module.dcc.service.productcatalog;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DccRegistrationExpiryExternalPageClientImpl implements DccRegistrationExpiryExternalPageClient {

    private static final Map<String, String> BROWSER_HEADERS = Map.of(
            "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");

    @Override
    public String fetch(String url) {
        try (HttpResponse response = HttpRequest.get(url)
                .addHeaders(BROWSER_HEADERS)
                .timeout(10_000)
                .execute()) {
            if (response.getStatus() >= 400) {
                throw new DccRegistrationExpiryExternalPageFetchException("HTTP " + response.getStatus());
            }
            String body = response.body();
            if (StrUtil.isBlank(body)) {
                throw new DccRegistrationExpiryExternalPageFetchException("empty response");
            }
            return body;
        } catch (DccRegistrationExpiryExternalPageFetchException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new DccRegistrationExpiryExternalPageFetchException(ex.getMessage());
        }
    }
}
