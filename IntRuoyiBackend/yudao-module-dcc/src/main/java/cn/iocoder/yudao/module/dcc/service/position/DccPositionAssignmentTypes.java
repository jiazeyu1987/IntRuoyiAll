package cn.iocoder.yudao.module.dcc.service.position;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_CONTENT_INVALID;

/** Assignment identities: account IDs and applicant-relative rules are stored separately. */
public final class DccPositionAssignmentTypes {
    public static final String DIRECT_SUPERVISOR = "DIRECT_SUPERVISOR";
    public static final String DEPT_LEADER = "DEPT_LEADER";
    private DccPositionAssignmentTypes() { }

    public static boolean isRelationship(String type) {
        return DIRECT_SUPERVISOR.equals(type) || DEPT_LEADER.equals(type);
    }

    public static void validate(String type, Long userId, Long postId) {
        boolean valid = "USER".equals(type) ? userId != null && userId > 0 && postId == null
                : "POST".equals(type) ? postId != null && postId > 0 && userId == null
                : isRelationship(type) && userId == null && postId == null;
        if (!valid) throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "审批角色分配类型与对象不一致；关系规则不能携带固定用户或岗位");
    }
}
