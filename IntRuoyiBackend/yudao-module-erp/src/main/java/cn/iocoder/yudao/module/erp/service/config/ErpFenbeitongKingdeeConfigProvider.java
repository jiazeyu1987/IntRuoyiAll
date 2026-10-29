package cn.iocoder.yudao.module.erp.service.config;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeConfigRespVO;
import cn.iocoder.yudao.module.system.service.fenbeitongassistant.FenbeitongKingdeeConfigProvider;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;

@Service
public class ErpFenbeitongKingdeeConfigProvider implements FenbeitongKingdeeConfigProvider {
    @Resource
    private ErpKingdeeConfigService kingdeeConfigService;

    @Override
    public PasswordLoginConfig getCurrentPasswordLoginConfig() {
        // getConfig resolves the explicitly selected account without imposing
        // SimPas requirements on the assistant's ValidateUser protocol.
        ErpKingdeeConfigRespVO config = kingdeeConfigService.getConfig();
        if (config == null || config.getLcid() == null || config.getLcid() <= 0) {
            throw exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "分贝通助手 ERP 配置缺失：lcid");
        }
        return new PasswordLoginConfig(required(config.getBaseUrl(), "baseUrl"),
                required(config.getAcctId(), "acctId"), required(config.getUsername(), "username"),
                required(config.getPassword(), "password"), config.getLcid());
    }

    private static String required(String value, String field) {
        String text = StrUtil.trim(value);
        if (StrUtil.isBlank(text)) {
            throw exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "分贝通助手 ERP 配置缺失：" + field);
        }
        return text;
    }
}
