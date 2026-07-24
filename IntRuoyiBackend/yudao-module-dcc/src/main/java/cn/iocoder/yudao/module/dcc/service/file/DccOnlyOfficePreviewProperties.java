package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "yudao.dcc.preview.onlyoffice")
public class DccOnlyOfficePreviewProperties {

    private String baseUrl;
    private String jwtSecret;
    private String callbackUrl;
    private String publicFileBaseUrl;
    private Integer tokenExpireSeconds = 300;

    public boolean isConfigured() {
        return StrUtil.isNotBlank(baseUrl)
                && StrUtil.isNotBlank(jwtSecret)
                && StrUtil.isNotBlank(publicFileBaseUrl);
    }

    public String missingReason() {
        if (StrUtil.isBlank(baseUrl)) {
            return "yudao.dcc.preview.onlyoffice.base-url is missing";
        }
        if (StrUtil.isBlank(jwtSecret)) {
            return "yudao.dcc.preview.onlyoffice.jwt-secret is missing";
        }
        if (StrUtil.isBlank(publicFileBaseUrl)) {
            return "yudao.dcc.preview.onlyoffice.public-file-base-url is missing";
        }
        return "";
    }
}
