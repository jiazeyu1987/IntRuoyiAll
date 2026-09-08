package cn.iocoder.yudao.module.system.controller.admin.permission.vo.temporaryrole;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 临时角色授权审查归集 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemporaryRoleGrantReviewSummaryRespVO {

    @Schema(description = "仍有效数量")
    private Long activeCount;

    @Schema(description = "即将到期数量")
    private Long expiringSoonCount;

    @Schema(description = "异常逾期数量")
    private Long overdueCount;

}
