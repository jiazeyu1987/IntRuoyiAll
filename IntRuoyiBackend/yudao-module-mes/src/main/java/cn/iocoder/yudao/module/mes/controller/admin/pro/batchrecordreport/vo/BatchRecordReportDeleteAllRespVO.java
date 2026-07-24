package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 电子批记录批量删除报表 Response VO")
@Data
@Accessors(chain = true)
public class BatchRecordReportDeleteAllRespVO {

    @Schema(description = "删除的积木报表数量", example = "106")
    private Integer deletedReportCount;

    @Schema(description = "删除的元数据数量", example = "106")
    private Integer deletedMetadataCount;

    @Schema(description = "因已绑定工艺路线工序而保留的报表数量", example = "3")
    private Integer skippedBoundReportCount;

    @Schema(description = "已解除的工艺路线工序默认批记录绑定数量", example = "2")
    private Integer unboundRouteProcessCount;

    @Schema(description = "已删除的工艺流程批记录绑定数量", example = "4")
    private Integer deletedRouteFlowBindingCount;

    @Schema(description = "已解除的工艺用途工序配置批记录绑定数量", example = "4")
    private Integer unboundRouteFlowProcessConfigCount;
}
