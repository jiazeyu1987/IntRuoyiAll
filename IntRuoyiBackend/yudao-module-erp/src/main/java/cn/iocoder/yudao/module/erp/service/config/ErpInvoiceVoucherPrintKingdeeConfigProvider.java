package cn.iocoder.yudao.module.erp.service.config;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.system.service.invoicevoucherprintassistant.InvoiceVoucherPrintKingdeeConfigProvider;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;

@Service
public class ErpInvoiceVoucherPrintKingdeeConfigProvider implements InvoiceVoucherPrintKingdeeConfigProvider {

    private static final String CONFIG_MISSING_PREFIX = "发票凭证打印助手 ERP 配置缺失：";

    @Resource
    private ErpKingdeeConfigService kingdeeConfigService;

    @Override
    public KingdeeConfigSnapshot getCurrentConfigSnapshot() {
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        return KingdeeConfigSnapshot.builder()
                .baseUrl(requireNotBlank(properties.getBaseUrl(), "baseUrl"))
                .acctId(requireNotBlank(properties.getAcctId(), "acctId"))
                .username(requireNotBlank(properties.getUsername(), "username"))
                .password(requireNotBlank(properties.getPassword(), "password"))
                .appId(requireNotBlank(properties.getAppId(), "appId"))
                .appSecret(requireNotBlank(properties.getAppSecret(), "appSecret"))
                .lcid(requireLcid(properties.getLcid()))
                .build();
    }

    private static String requireNotBlank(String value, String fieldName) {
        String text = StrUtil.trim(value);
        if (StrUtil.isBlank(text)) {
            throw missingConfig(fieldName);
        }
        return text;
    }

    private static Integer requireLcid(Integer value) {
        if (value == null) {
            throw missingConfig("lcid");
        }
        return value;
    }

    private static RuntimeException missingConfig(String fieldName) {
        return exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), CONFIG_MISSING_PREFIX + fieldName);
    }

}
