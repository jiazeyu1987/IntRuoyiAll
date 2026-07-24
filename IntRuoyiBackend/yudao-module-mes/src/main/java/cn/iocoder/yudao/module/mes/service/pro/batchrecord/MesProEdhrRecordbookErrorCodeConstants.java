package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProEdhrRecordbookErrorCodeConstants {

    ErrorCode PRO_EDHR_RECORDBOOK_TEMPLATE_NOT_EXISTS =
            new ErrorCode(1_040_753_500, "eDHR 记录本模板不存在");
    ErrorCode PRO_EDHR_RECORDBOOK_TEMPLATE_CODE_DUPLICATE =
            new ErrorCode(1_040_753_501, "eDHR 记录本模板编码已存在");
    ErrorCode PRO_EDHR_RECORDBOOK_TEMPLATE_STATUS_INVALID =
            new ErrorCode(1_040_753_502, "eDHR 记录本模板状态不允许该操作");
    ErrorCode PRO_EDHR_RECORDBOOK_ENTRY_SCHEMA_EMPTY =
            new ErrorCode(1_040_753_503, "eDHR 记录本条目字段定义不能为空");
    ErrorCode PRO_EDHR_RECORDBOOK_ENTRY_SCHEMA_INVALID =
            new ErrorCode(1_040_753_504, "eDHR 记录本条目字段定义无效：{}");
    ErrorCode PRO_EDHR_RECORDBOOK_CODE_DUPLICATE =
            new ErrorCode(1_040_753_510, "eDHR 记录本编码已存在");
    ErrorCode PRO_EDHR_RECORDBOOK_NOT_EXISTS =
            new ErrorCode(1_040_753_511, "eDHR 记录本不存在");
    ErrorCode PRO_EDHR_RECORDBOOK_STATUS_INVALID =
            new ErrorCode(1_040_753_512, "eDHR 记录本状态不允许该操作");
    ErrorCode PRO_EDHR_RECORDBOOK_ENTRY_NOT_EXISTS =
            new ErrorCode(1_040_753_520, "eDHR 记录本条目不存在");
    ErrorCode PRO_EDHR_RECORDBOOK_ENTRY_STATUS_INVALID =
            new ErrorCode(1_040_753_521, "eDHR 记录本条目已提交，正文锁定，后续必须走审核或变更流程");
    ErrorCode PRO_EDHR_RECORDBOOK_ENTRY_CONTENT_EMPTY =
            new ErrorCode(1_040_753_522, "eDHR 记录本条目正文不能为空");
    ErrorCode PRO_EDHR_RECORDBOOK_FIELD_REQUIRED =
            new ErrorCode(1_040_753_523, "eDHR 记录本必填字段未填写：{}");
    ErrorCode PRO_EDHR_RECORDBOOK_FIELD_RANGE_INVALID =
            new ErrorCode(1_040_753_524, "eDHR 记录本数字字段超出范围：{}");
    ErrorCode PRO_EDHR_RECORDBOOK_FIELD_ENUM_INVALID =
            new ErrorCode(1_040_753_525, "eDHR 记录本枚举字段取值无效：{}");
    ErrorCode PRO_EDHR_RECORDBOOK_TAG_NOT_EXISTS =
            new ErrorCode(1_040_753_530, "eDHR 受控标签不存在");
    ErrorCode PRO_EDHR_RECORDBOOK_TAG_CODE_DUPLICATE =
            new ErrorCode(1_040_753_531, "eDHR 受控标签编码已存在");
    ErrorCode PRO_EDHR_RECORDBOOK_TAG_STATUS_INVALID =
            new ErrorCode(1_040_753_532, "eDHR 受控标签状态不允许该操作");
    ErrorCode PRO_EDHR_RECORDBOOK_TAG_INVALID =
            new ErrorCode(1_040_753_533, "eDHR 记录本标签无效或未启用：{}");
    ErrorCode PRO_EDHR_RECORDBOOK_TAG_REQUIRED =
            new ErrorCode(1_040_753_534, "eDHR 记录本条目必须绑定受控标签");
    ErrorCode PRO_EDHR_RECORDBOOK_IDEMPOTENCY_KEY_EMPTY =
            new ErrorCode(1_040_753_540, "eDHR 记录本条目幂等键不能为空");
}
