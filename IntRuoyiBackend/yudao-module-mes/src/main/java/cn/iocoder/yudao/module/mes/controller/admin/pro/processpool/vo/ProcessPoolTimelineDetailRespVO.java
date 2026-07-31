package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 工序池提交事件只读详情 Response VO")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProcessPoolTimelineDetailRespVO extends ProcessPoolTimelineEventRespVO {

    @Schema(description = "只读动作边界")
    private ReadonlyActions readonlyActions;

    @Schema(description = "管理后台 - MES 工序池时间轴只读动作边界")
    @Data
    @Accessors(chain = true)
    public static class ReadonlyActions {

        @Schema(description = "是否允许修改原始记录")
        private Boolean canModifyOriginalRecord;

        @Schema(description = "是否允许生成审核副本")
        private Boolean canGenerateAuditCopy;

        @Schema(description = "是否允许执行 FIFO 分配")
        private Boolean canExecuteFifoAllocation;

    }

}
