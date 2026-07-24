package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - eDHR 报表定义分页 Request VO")
@Data
public class MesProEdhrReportDefinitionPageReqVO extends PageParam {

    private String reportCode;
    private String reportName;
    private String reportType;
    private String datasetCode;
    private String status;
}
