package cn.iocoder.yudao.module.erp.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - ERP 金蝶配置 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpKingdeeConfigRespVO extends ErpKingdeeConfigSaveReqVO {
}
