package cn.iocoder.yudao.module.dcc.controller.admin.signature.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - DCC电子签名授权审计 Response VO")
@Data
public class DccElectronicSignatureAuthorizationAuditRespVO {

    @Schema(description = "审计记录 ID", example = "61001")
    private Long id;

    @Schema(description = "目标用户 ID", example = "101")
    private Long targetUserId;

    @Schema(description = "操作人 ID", example = "1")
    private Long operatorUserId;

    @Schema(description = "操作人名称", example = "系统管理员")
    private String operatorName;

    @Schema(description = "变更前状态", example = "DISABLED")
    private String beforeState;

    @Schema(description = "变更后状态", example = "ENABLED")
    private String afterState;

    @Schema(description = "变更原因", example = "完成电子签名授权复核")
    private String reason;

    @Schema(description = "操作时间")
    private LocalDateTime operatedAt;
}
