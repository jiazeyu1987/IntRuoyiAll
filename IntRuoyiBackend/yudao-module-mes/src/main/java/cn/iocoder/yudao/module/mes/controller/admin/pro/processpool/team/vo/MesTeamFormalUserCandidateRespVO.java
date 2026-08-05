package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产人员正式工候选 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamFormalUserCandidateRespVO {

    @Schema(description = "正式工系统用户编号", example = "2001")
    private Long systemUserId;

    @Schema(description = "正式工显示名", example = "张三")
    private String displayName;
}
