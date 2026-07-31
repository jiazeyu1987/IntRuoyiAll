package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - MES 工序池班组长工作台 Response VO")
@Data
@Accessors(chain = true)
public class ProcessPoolTeamLeaderWorkbenchRespVO {

    @Schema(description = "符合查询条件的提交事件总数")
    private Long total;

    @Schema(description = "当前页提交事件")
    private List<ProcessPoolTimelineEventRespVO> events;

    @Schema(description = "当前页状态摘要")
    private Summary summary;

    @Schema(description = "管理后台 - MES 工序池班组长工作台状态摘要")
    @Data
    @Accessors(chain = true)
    public static class Summary {

        @Schema(description = "当前页可见提交事件数")
        private Integer visibleEventCount;

        @Schema(description = "当前页 PQC 成功数")
        private Integer pqcSuccessCount;

        @Schema(description = "当前页 PQC 失败数")
        private Integer pqcFailureCount;

        @Schema(description = "当前页 FIFO 待分配数")
        private Integer fifoPendingCount;

        @Schema(description = "当前页 FIFO 已分配数")
        private Integer fifoAllocatedCount;

        @Schema(description = "当前页审核副本待生成数")
        private Integer auditCopyPendingCount;

        @Schema(description = "当前页审核副本已提交数")
        private Integer auditCopySubmittedCount;

        @Schema(description = "当前页原始记录已修改数")
        private Integer modifiedRecordCount;

    }

}
