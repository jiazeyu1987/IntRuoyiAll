package cn.iocoder.yudao.module.mdm.service.companyscope;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MdmCompanyScopeErrorCodes {

    ErrorCode MDM_COMPANY_SCOPE_FIELD_REQUIRED = new ErrorCode(1_081_001_001,
            "Company scope field is required or invalid: {}");
    ErrorCode MDM_USER_COMPANY_SCOPE_DENIED = new ErrorCode(1_081_001_002,
            "User has no enabled company scope");
    ErrorCode MDM_USER_COMPANY_SCOPE_DISABLED = new ErrorCode(1_081_001_003,
            "User company scope is disabled");
    ErrorCode MDM_ROLE_COMPANY_SCOPE_MISSING = new ErrorCode(1_081_001_004,
            "Role company scope is missing: {}");
    ErrorCode MDM_ROLE_COMPANY_SCOPE_DISABLED = new ErrorCode(1_081_001_005,
            "Role company scope is disabled: {}");
    ErrorCode MDM_COMPANY_SCOPE_CONFIG_INVALID = new ErrorCode(1_081_001_006,
            "Company scope configuration is invalid");
    ErrorCode MDM_USER_COMPANY_SCOPE_DUPLICATE = new ErrorCode(1_081_001_007,
            "User company scope already exists in the current tenant");
    ErrorCode MDM_ROLE_COMPANY_SCOPE_DUPLICATE = new ErrorCode(1_081_001_008,
            "Role company scope already exists in the current tenant");
    ErrorCode MDM_COMPANY_SCOPE_RECIPIENT_NOT_FOUND = new ErrorCode(1_081_001_009,
            "No enabled recipient satisfies the role, permission and company scope");
    ErrorCode MDM_COMPANY_SCOPE_SYSTEM_USER_INVALID = new ErrorCode(1_081_001_010,
            "System user configuration is missing or disabled: {}");
    ErrorCode MDM_COMPANY_SCOPE_SYSTEM_ROLE_INVALID = new ErrorCode(1_081_001_011,
            "System role configuration is missing or disabled: {}");
    ErrorCode MDM_COMPANY_SCOPE_PERMISSION_MISSING = new ErrorCode(1_081_001_012,
            "Role lacks the required explicit permission: {}");
    ErrorCode MDM_COMPANY_SCOPE_WRITE_RESULT_INVALID = new ErrorCode(1_081_001_013,
            "Company scope write result is inconsistent");
}
