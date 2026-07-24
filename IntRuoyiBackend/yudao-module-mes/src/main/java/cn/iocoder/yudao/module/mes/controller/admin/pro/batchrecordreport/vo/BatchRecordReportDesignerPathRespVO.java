package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 电子批记录报表设计器路径 Response VO")
@Data
public class BatchRecordReportDesignerPathRespVO {

    @Schema(description = "设计器相对路径")
    private String path;
}
