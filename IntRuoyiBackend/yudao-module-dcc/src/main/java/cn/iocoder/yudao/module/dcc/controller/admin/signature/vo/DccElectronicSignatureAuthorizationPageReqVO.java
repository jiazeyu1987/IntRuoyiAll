package cn.iocoder.yudao.module.dcc.controller.admin.signature.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - DCC电子签名授权分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DccElectronicSignatureAuthorizationPageReqVO extends PageParam {

    @Schema(description = "用户账号", example = "admin")
    private String username;

    @Schema(description = "手机号", example = "18888888888")
    private String mobile;

    @Schema(description = "用户状态", example = "0")
    private Integer status;
}
