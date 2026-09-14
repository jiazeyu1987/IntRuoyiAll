package cn.iocoder.yudao.module.mdm.service.companyscope;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MdmCompanyScopeErrorCodes {

    ErrorCode MDM_COMPANY_SCOPE_FIELD_REQUIRED = new ErrorCode(1_081_001_001,
            "授权公司字段缺失或不合法：{}");
    ErrorCode MDM_USER_COMPANY_SCOPE_DENIED = new ErrorCode(1_081_001_002,
            "当前账号未配置可用授权公司");
    ErrorCode MDM_USER_COMPANY_SCOPE_DISABLED = new ErrorCode(1_081_001_003,
            "当前账号的授权公司已停用");
    ErrorCode MDM_ROLE_COMPANY_SCOPE_MISSING = new ErrorCode(1_081_001_004,
            "角色未配置授权公司：{}");
    ErrorCode MDM_ROLE_COMPANY_SCOPE_DISABLED = new ErrorCode(1_081_001_005,
            "角色授权公司已停用：{}");
    ErrorCode MDM_COMPANY_SCOPE_CONFIG_INVALID = new ErrorCode(1_081_001_006,
            "授权公司配置不完整或不一致");
    ErrorCode MDM_USER_COMPANY_SCOPE_DUPLICATE = new ErrorCode(1_081_001_007,
            "当前租户下该用户已存在相同授权公司");
    ErrorCode MDM_ROLE_COMPANY_SCOPE_DUPLICATE = new ErrorCode(1_081_001_008,
            "当前租户下该角色已存在相同授权公司");
    ErrorCode MDM_COMPANY_SCOPE_RECIPIENT_NOT_FOUND = new ErrorCode(1_081_001_009,
            "没有找到同时满足角色、权限和授权公司的可用审批人");
    ErrorCode MDM_COMPANY_SCOPE_SYSTEM_USER_INVALID = new ErrorCode(1_081_001_010,
            "系统用户不存在或已停用：{}");
    ErrorCode MDM_COMPANY_SCOPE_SYSTEM_ROLE_INVALID = new ErrorCode(1_081_001_011,
            "系统角色不存在或已停用：{}");
    ErrorCode MDM_COMPANY_SCOPE_PERMISSION_MISSING = new ErrorCode(1_081_001_012,
            "角色缺少必要权限：{}");
    ErrorCode MDM_COMPANY_SCOPE_WRITE_RESULT_INVALID = new ErrorCode(1_081_001_013,
            "授权公司写入结果不一致");
}
