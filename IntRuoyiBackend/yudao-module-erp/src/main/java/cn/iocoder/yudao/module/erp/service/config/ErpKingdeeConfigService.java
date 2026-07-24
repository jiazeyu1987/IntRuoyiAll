package cn.iocoder.yudao.module.erp.service.config;

import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.config.vo.ErpKingdeeConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import jakarta.validation.Valid;

public interface ErpKingdeeConfigService {

    ErpKingdeeConfigRespVO getConfig();

    void saveConfig(@Valid ErpKingdeeConfigSaveReqVO saveReqVO);

    ErpKingdeeProperties getEffectiveProperties();

    boolean isExternalWriteEnabled();

    void updateExternalWriteEnabled(Boolean enabled);

    void assertExternalWriteEnabled();

}
