package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES PQC 组长关联检验员 Request VO")
@Data
@Accessors(chain = true)
public class MesPqcLeaderPersonnelLinkReqVO {

    @Schema(description = "PQC 检验员系统用户编号", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "2001")
    @NotNull(message = "PQC 检验员系统用户编号不能为空")
    private Long systemUserId;
}
