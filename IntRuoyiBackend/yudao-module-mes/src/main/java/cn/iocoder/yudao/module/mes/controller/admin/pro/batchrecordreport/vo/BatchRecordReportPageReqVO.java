package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 电子批记录生成报表分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BatchRecordReportPageReqVO extends PageParam {

    @Schema(description = "报表名称或编码")
    private String name;

    @Schema(description = "报表编号")
    private String reportId;

    @Schema(description = "批记录名称")
    private String batchRecordName;

    @Schema(description = "表单对应的产品名称")
    private String productName;

    @Schema(description = "批记录版本号")
    private String versionNo;

    @Schema(description = "表单槽位类型：MAIN/LOSS_REPORT/PROCESS_INSPECTION/PARAMETER_RECORD")
    private String formSlotType;

    @Schema(description = "识别路线关键字")
    private String routeKey;
}
