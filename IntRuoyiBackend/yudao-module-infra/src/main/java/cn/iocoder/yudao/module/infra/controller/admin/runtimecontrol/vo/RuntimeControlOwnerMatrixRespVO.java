package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 运行控制台责任人矩阵 Response VO")
@Data
public class RuntimeControlOwnerMatrixRespVO {

    @Schema(description = "矩阵编号")
    private Long id;

    @Schema(description = "环境", example = "prod")
    private String environment;

    @Schema(description = "动作", example = "promote-prod")
    private String action;

    @Schema(description = "角色", example = "release-owner")
    private String role;

    @Schema(description = "是否必填")
    private Boolean required;

    @Schema(description = "责任人用户编号")
    private Long ownerUserId;

    @Schema(description = "责任人姓名")
    private String ownerName;

    @Schema(description = "升级说明")
    private String escalationPath;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
