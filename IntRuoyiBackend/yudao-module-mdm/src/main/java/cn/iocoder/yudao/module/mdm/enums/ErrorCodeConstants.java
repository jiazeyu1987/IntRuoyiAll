package cn.iocoder.yudao.module.mdm.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode MDM_ENTERPRISE_BATCH_EMPTY = new ErrorCode(1_081_000_000, "Enterprise ID batch cannot be empty");
    ErrorCode MDM_ENTERPRISE_BATCH_DUPLICATE = new ErrorCode(1_081_000_001,
            "Enterprise ID batch contains duplicate IDs");
    ErrorCode MDM_ENTERPRISE_NOT_FOUND = new ErrorCode(1_081_000_002, "Enterprise does not exist: {}");
    ErrorCode MDM_ENTERPRISE_DISABLED = new ErrorCode(1_081_000_003, "Enterprise is disabled: {}");
    ErrorCode MDM_ENTERPRISE_DELETED = new ErrorCode(1_081_000_004, "Enterprise is deleted: {}");
    ErrorCode MDM_ENTERPRISE_TYPE_MISMATCH = new ErrorCode(1_081_000_005,
            "Enterprise type is not allowed: {}");
    ErrorCode MDM_ENTERPRISE_TENANT_MISMATCH = new ErrorCode(1_081_000_006,
            "Enterprise does not belong to the current tenant: {}");
    ErrorCode MDM_ENTERPRISE_CODE_DUPLICATE = new ErrorCode(1_081_000_007,
            "Enterprise code already exists in the current tenant");
    ErrorCode MDM_ENTERPRISE_FIELD_REQUIRED = new ErrorCode(1_081_000_008,
            "Enterprise field is required: {}");
    ErrorCode MDM_ENTERPRISE_TYPE_INVALID = new ErrorCode(1_081_000_009, "Enterprise type is invalid: {}");
    ErrorCode MDM_ENTERPRISE_STATUS_INVALID = new ErrorCode(1_081_000_010, "Enterprise status is invalid: {}");
    ErrorCode MDM_ENTERPRISE_BATCH_RESULT_INVALID = new ErrorCode(1_081_000_011,
            "Enterprise batch result is inconsistent");

}
