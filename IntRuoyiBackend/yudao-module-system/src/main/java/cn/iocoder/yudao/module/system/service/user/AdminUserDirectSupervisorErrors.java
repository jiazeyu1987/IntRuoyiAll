package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface AdminUserDirectSupervisorErrors {
    ErrorCode INVALID = new ErrorCode(1_002_003_040, "直属主管关系账号、租户或操作不合法");
    ErrorCode FORBIDDEN = new ErrorCode(1_002_003_041, "没有用户直属主管关系维护权限");
    ErrorCode CONFLICT = new ErrorCode(1_002_003_042, "直属主管关系已变化，请重新读取后设置");
    ErrorCode VERSION_INVALID = new ErrorCode(1_002_003_043, "直属主管关系版本不合法或已达到上限");
}
