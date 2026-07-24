package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_CONFIG_MISSING;

@Data
@Component
@ConfigurationProperties(prefix = "dcc.signature.evidence")
public class DccSignatureEvidenceProperties {

    private String hmacSecret;
    private String keyVersion;

    @PostConstruct
    public void validateStartupConfig() {
        validateRuntimeConfig();
    }

    public void validateRuntimeConfig() {
        if (StrUtil.isBlank(hmacSecret) || StrUtil.isBlank(keyVersion)) {
            throw exception(CONTROLLED_FILE_SIGNATURE_CONFIG_MISSING);
        }
    }
}
