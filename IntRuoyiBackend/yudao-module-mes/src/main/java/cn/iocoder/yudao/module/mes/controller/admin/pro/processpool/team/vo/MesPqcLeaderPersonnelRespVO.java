package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES PQC 组长人员关联 Response VO")
@Data
@Accessors(chain = true)
public class MesPqcLeaderPersonnelRespVO {

    @Schema(description = "负责范围编号", example = "9001")
    private Long scopeId;

    @Schema(description = "PQC 检验员系统用户编号", example = "2001")
    private Long systemUserId;

    @Schema(description = "PQC 检验员显示名", example = "王检验")
    private String displayName;

    @Schema(description = "PQC 检验员登录账号", example = "pqc01")
    private String username;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;
}
