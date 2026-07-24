package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;

@Data
public class MesProEdhrReportExportAuditPageReqVO extends PageParam {

    private String reportCode;
    private String operationType;
    private String resultStatus;
}
