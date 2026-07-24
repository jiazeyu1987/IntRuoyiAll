package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProEdhrFormErrorCodeConstants {

    ErrorCode PRO_EDHR_FORM_TEMPLATE_NOT_EXISTS =
            new ErrorCode(1_040_753_400, "eDHR 独立表单模板不存在");
    ErrorCode PRO_EDHR_FORM_TEMPLATE_CODE_DUPLICATE =
            new ErrorCode(1_040_753_401, "eDHR 独立表单模板编码已存在");
    ErrorCode PRO_EDHR_FORM_TEMPLATE_STATUS_INVALID =
            new ErrorCode(1_040_753_402, "eDHR 独立表单模板状态不允许该操作");
    ErrorCode PRO_EDHR_FORM_FIELD_SCHEMA_EMPTY =
            new ErrorCode(1_040_753_403, "eDHR 独立表单字段定义不能为空");
    ErrorCode PRO_EDHR_FORM_FIELD_SCHEMA_INVALID =
            new ErrorCode(1_040_753_404, "eDHR 独立表单字段定义无效：{}");
    ErrorCode PRO_EDHR_FORM_INSTANCE_NOT_EXISTS =
            new ErrorCode(1_040_753_410, "eDHR 独立表单实例不存在");
    ErrorCode PRO_EDHR_FORM_INSTANCE_STATUS_INVALID =
            new ErrorCode(1_040_753_411, "eDHR 独立表单实例已提交，直接修改被禁止，请走变更流程");
    ErrorCode PRO_EDHR_FORM_FIELD_REQUIRED =
            new ErrorCode(1_040_753_420, "eDHR 独立表单必填字段未填写：{}");
    ErrorCode PRO_EDHR_FORM_FIELD_RANGE_INVALID =
            new ErrorCode(1_040_753_421, "eDHR 独立表单数字字段超出范围：{}");
    ErrorCode PRO_EDHR_FORM_FIELD_ENUM_INVALID =
            new ErrorCode(1_040_753_422, "eDHR 独立表单枚举字段取值无效：{}");
}
