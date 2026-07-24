package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProEdhrReportErrorCodeConstants {

    ErrorCode PRO_EDHR_REPORT_CATALOG_NOT_EXISTS =
            new ErrorCode(1_040_750_600, "eDHR 报表目录不存在");
    ErrorCode PRO_EDHR_REPORT_DEFINITION_NOT_EXISTS =
            new ErrorCode(1_040_750_601, "eDHR 报表定义不存在");
    ErrorCode PRO_EDHR_REPORT_NOT_PUBLISHED =
            new ErrorCode(1_040_750_602, "eDHR 报表尚未发布，不能查询或导出");
    ErrorCode PRO_EDHR_REPORT_DATA_SOURCE_INVALID =
            new ErrorCode(1_040_750_603, "eDHR 报表数据源不可用：{}");
    ErrorCode PRO_EDHR_REPORT_CALIBER_MISSING =
            new ErrorCode(1_040_750_604, "eDHR 报表口径版本缺失");
    ErrorCode PRO_EDHR_REPORT_EXPORT_AUDIT_FAILED =
            new ErrorCode(1_040_750_605, "eDHR 报表导出审计记录失败");
}
