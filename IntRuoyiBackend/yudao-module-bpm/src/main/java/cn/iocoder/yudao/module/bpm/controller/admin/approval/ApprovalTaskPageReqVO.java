package cn.iocoder.yudao.module.bpm.controller.admin.approval;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 统一审批中心任务分页 Request VO")
@Data
@Accessors(chain = true)
public class ApprovalTaskPageReqVO {

    @Schema(description = "视图类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "TODO")
    @NotNull(message = "视图类型不能为空")
    private ApprovalTaskViewType viewType = ApprovalTaskViewType.TODO;

    @Schema(description = "模块编码", example = "DCC")
    private ApprovalModuleCode moduleCode;

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码最小值为 1")
    private Integer pageNo = 1;

    @Schema(description = "每页条数", example = "10")
    @Min(value = 1, message = "每页条数最小值为 1")
    @Max(value = 200, message = "每页条数最大值为 200")
    private Integer pageSize = 10;

    public ApprovalTaskQuery toQuery() {
        return new ApprovalTaskQuery()
                .setViewType(viewType)
                .setModuleCode(moduleCode)
                .setKeyword(keyword)
                .setPageNo(pageNo)
                .setPageSize(pageSize);
    }
}
