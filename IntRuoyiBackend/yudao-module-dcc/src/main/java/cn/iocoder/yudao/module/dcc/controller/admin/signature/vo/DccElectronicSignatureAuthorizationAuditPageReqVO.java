package cn.iocoder.yudao.module.dcc.controller.admin.signature.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - DCC电子签名授权审计分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DccElectronicSignatureAuthorizationAuditPageReqVO extends PageParam {
}
