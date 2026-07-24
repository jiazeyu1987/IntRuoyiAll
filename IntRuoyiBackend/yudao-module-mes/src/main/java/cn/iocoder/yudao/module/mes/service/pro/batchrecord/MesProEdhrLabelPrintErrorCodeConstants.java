package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProEdhrLabelPrintErrorCodeConstants {

    ErrorCode PRO_EDHR_LABEL_TEMPLATE_NOT_EXISTS =
            new ErrorCode(1_040_752_500, "eDHR 标签模板不存在");
    ErrorCode PRO_EDHR_LABEL_TEMPLATE_CODE_DUPLICATE =
            new ErrorCode(1_040_752_501, "eDHR 标签模板编码已存在");
    ErrorCode PRO_EDHR_LABEL_TEMPLATE_STATUS_INVALID =
            new ErrorCode(1_040_752_502, "eDHR 标签模板状态不允许该操作");
    ErrorCode PRO_EDHR_LABEL_ACTIVE_TEMPLATE_EXISTS =
            new ErrorCode(1_040_752_503, "同一业务对象类型已存在启用标签模板");
    ErrorCode PRO_EDHR_LABEL_PREVIEW_FIELD_MISSING =
            new ErrorCode(1_040_752_504, "eDHR 标签预览缺少真实业务字段快照");
    ErrorCode PRO_EDHR_PRINT_TASK_NOT_EXISTS =
            new ErrorCode(1_040_752_510, "eDHR 打印任务不存在");
    ErrorCode PRO_EDHR_PRINT_REPRINT_REASON_REQUIRED =
            new ErrorCode(1_040_752_511, "eDHR 补打必须填写原因");
    ErrorCode PRO_EDHR_PRINT_ORIGINAL_TASK_REQUIRED =
            new ErrorCode(1_040_752_512, "eDHR 补打必须关联原打印任务");
    ErrorCode PRO_EDHR_PRINT_FAILURE_REASON_REQUIRED =
            new ErrorCode(1_040_752_513, "eDHR 标记打印失败必须填写失败原因");
    ErrorCode PRO_EDHR_PRINT_CONFIRMATION_EVIDENCE_REQUIRED =
            new ErrorCode(1_040_752_514, "eDHR 确认打印成功必须提供确认凭证");
    ErrorCode PRO_EDHR_PRINT_TASK_STATUS_INVALID =
            new ErrorCode(1_040_752_515, "eDHR 打印任务状态不允许该操作");
    ErrorCode PRO_EDHR_PRINT_POLICY_NOT_EXISTS =
            new ErrorCode(1_040_752_520, "eDHR 打印策略不存在或未启用");
    ErrorCode PRO_EDHR_PRINT_POLICY_CODE_DUPLICATE =
            new ErrorCode(1_040_752_521, "eDHR 打印策略编码已存在");
    ErrorCode PRO_EDHR_PRINT_POLICY_STATUS_INVALID =
            new ErrorCode(1_040_752_522, "eDHR 打印策略状态不允许该操作");
    ErrorCode PRO_EDHR_PRINT_POLICY_SCOPE_ACTIVE_EXISTS =
            new ErrorCode(1_040_752_523, "eDHR 同一业务范围已存在启用打印策略");
    ErrorCode PRO_EDHR_REPRINT_REASON_INVALID =
            new ErrorCode(1_040_752_524, "eDHR 补打原因不在当前打印策略原因字典内");
    ErrorCode PRO_EDHR_REPRINT_LIMIT_EXCEEDED =
            new ErrorCode(1_040_752_525, "eDHR 补打次数已达到策略上限");
    ErrorCode PRO_EDHR_PRINT_VOID_COPY_SOURCE_INVALID =
            new ErrorCode(1_040_752_529, "eDHR 仅作废受限打印任务允许生成历史副本");
    ErrorCode PRO_EDHR_PRINT_VOID_COPY_WATERMARK_REQUIRED =
            new ErrorCode(1_040_752_526, "eDHR 作废历史副本必须带水印");
    ErrorCode PRO_EDHR_PRINT_EXPORT_FILTER_REQUIRED =
            new ErrorCode(1_040_752_527, "eDHR 打印历史导出必须记录筛选快照");
    ErrorCode PRO_EDHR_PRINT_EXPORT_IDEMPOTENCY_REQUIRED =
            new ErrorCode(1_040_752_528, "eDHR 打印历史导出必须提供幂等键");
}
