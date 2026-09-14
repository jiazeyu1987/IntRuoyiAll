package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 班组长提交看板分页 Request VO")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesTeamLeaderSubmissionPageReqVO extends ProcessPoolTimelinePageReqVO {

    @Schema(description = "班组长类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "PRODUCTION")
    private String leaderType;
}
