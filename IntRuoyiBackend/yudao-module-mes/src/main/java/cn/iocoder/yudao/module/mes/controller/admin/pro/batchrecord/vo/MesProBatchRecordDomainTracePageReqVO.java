package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES eDHR 主数据追溯分页 Request VO")
@Data
public class MesProBatchRecordDomainTracePageReqVO extends PageParam {

    @Schema(description = "执行记录编号", example = "9")
    private Long executionId;

    @Schema(description = "执行编号", example = "BRE202605280001")
    private String executionCode;

    @Schema(description = "生产工单编码", example = "MO001")
    private String workOrderCode;

    @Schema(description = "批次号", example = "BATCH001")
    private String batchCode;

    @Schema(description = "追溯状态：VERIFIED、BLOCKED", example = "BLOCKED")
    private String status;
}
