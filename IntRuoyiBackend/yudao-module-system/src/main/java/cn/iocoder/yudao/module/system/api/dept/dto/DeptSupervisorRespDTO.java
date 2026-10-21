package cn.iocoder.yudao.module.system.api.dept.dto;

import lombok.Data;

/** 部门自身配置与按组织树实时解析的主管，继承结果不写回部门。 */
@Data
public class DeptSupervisorRespDTO {
    private Long configuredUserId;
    private String configuredUserName;
    private boolean configuredUserInvalid;
    /** DIRECT / INHERITED / MISSING / INVALID_HIERARCHY */
    private String status;
    private Long userId;
    private String userName;
    private Long deptId;
    private String deptName;
}
