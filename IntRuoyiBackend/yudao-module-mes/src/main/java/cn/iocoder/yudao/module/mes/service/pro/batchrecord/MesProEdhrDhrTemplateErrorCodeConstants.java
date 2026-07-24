package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProEdhrDhrTemplateErrorCodeConstants {

    ErrorCode PRO_EDHR_DHR_CATALOG_NOT_EXISTS =
            new ErrorCode(1_040_752_600, "eDHR DHR目录不存在");
    ErrorCode PRO_EDHR_DHR_CATALOG_CODE_DUPLICATE =
            new ErrorCode(1_040_752_601, "eDHR DHR目录编码已存在");
    ErrorCode PRO_EDHR_DHR_TEMPLATE_NOT_EXISTS =
            new ErrorCode(1_040_752_610, "eDHR DHR模板不存在");
    ErrorCode PRO_EDHR_DHR_TEMPLATE_CODE_DUPLICATE =
            new ErrorCode(1_040_752_611, "eDHR DHR模板编码已存在");
    ErrorCode PRO_EDHR_DHR_TEMPLATE_BINDING_REQUIRED =
            new ErrorCode(1_040_752_612, "eDHR DHR模板缺少必需绑定：{}");
    ErrorCode PRO_EDHR_DHR_TEMPLATE_REVIEW_REQUIRED =
            new ErrorCode(1_040_752_613, "eDHR DHR模板必须先完成审核");
    ErrorCode PRO_EDHR_DHR_TEMPLATE_SIGNOFF_REQUIRED =
            new ErrorCode(1_040_752_614, "eDHR DHR模板必须先完成签核");
    ErrorCode PRO_EDHR_DHR_TEMPLATE_IMPACT_REQUIRED =
            new ErrorCode(1_040_752_615, "eDHR DHR模板停用或作废必须确认影响范围");
    ErrorCode PRO_EDHR_DHR_TEMPLATE_STATUS_INVALID =
            new ErrorCode(1_040_752_616, "eDHR DHR模板状态不允许该操作");
    ErrorCode PRO_EDHR_DHR_TEMPLATE_SIGNOFF_EVIDENCE_REQUIRED =
            new ErrorCode(1_040_752_617, "eDHR DHR模板签核必须提供证据摘要");
}
