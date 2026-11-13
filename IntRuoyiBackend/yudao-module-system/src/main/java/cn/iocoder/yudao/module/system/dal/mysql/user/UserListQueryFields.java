package cn.iocoder.yudao.module.system.dal.mysql.user;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import java.util.Map;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** All column predicates and ordering run before the database page boundary. */
final class UserListQueryFields {
    private UserListQueryFields() {}
    private static final Map<String, ListFilterQueryUtils.Field> FIELDS = Map.ofEntries(
        Map.entry("id",number("system_users.id")),
        Map.entry("username",text("system_users.username")),
        Map.entry("nickname",text("system_users.nickname")),
        Map.entry("mobile",text("system_users.mobile")),
        Map.entry("status",number("system_users.status")),
        Map.entry("statusLabel",text("CASE status WHEN 0 THEN '启用' WHEN 1 THEN '停用' ELSE '—' END")),
        Map.entry("createTime",dateTime("system_users.create_time")),
        Map.entry("lifecycleDocumentNo",text("system_users.lifecycle_document_no")),
        Map.entry("lifecycleEffectiveTime",dateTime("system_users.lifecycle_effective_time")),
        Map.entry("lifecycleDeactivatedTime",dateTime("system_users.lifecycle_deactivated_time")),
        Map.entry("deptName",text("(SELECT d.name FROM system_dept d WHERE d.id=system_users.dept_id AND d.tenant_id=system_users.tenant_id AND d.deleted=0)")),
        Map.entry("roleNamesText",text("COALESCE((SELECT JSON_UNQUOTE(REPLACE(SUBSTRING(CAST(JSON_ARRAYAGG(CASE WHEN n.id IS NULL THEN CONCAT('已删除角色（',l.role_id,'）') ELSE CONCAT(n.name,CASE WHEN n.status=1 THEN '（已停用）' ELSE '' END) END) OVER (ORDER BY l.role_id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS CHAR),2,CHAR_LENGTH(CAST(JSON_ARRAYAGG(CASE WHEN n.id IS NULL THEN CONCAT('已删除角色（',l.role_id,'）') ELSE CONCAT(n.name,CASE WHEN n.status=1 THEN '（已停用）' ELSE '' END) END) OVER (ORDER BY l.role_id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS CHAR))-2),'\", \"','、')) FROM system_user_role l LEFT JOIN system_role n ON n.id=l.role_id AND n.tenant_id=system_users.tenant_id AND n.deleted=0 WHERE l.user_id=system_users.id AND l.tenant_id=system_users.tenant_id AND l.deleted=0 LIMIT 1),'-')")),
        Map.entry("postNamesText",text("COALESCE((SELECT JSON_UNQUOTE(REPLACE(SUBSTRING(CAST(JSON_ARRAYAGG(CASE WHEN n.id IS NULL THEN CONCAT('已删除岗位（',l.post_id,'）') ELSE CONCAT(n.name,CASE WHEN n.status=1 THEN '（已停用）' ELSE '' END) END) OVER (ORDER BY l.post_id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS CHAR),2,CHAR_LENGTH(CAST(JSON_ARRAYAGG(CASE WHEN n.id IS NULL THEN CONCAT('已删除岗位（',l.post_id,'）') ELSE CONCAT(n.name,CASE WHEN n.status=1 THEN '（已停用）' ELSE '' END) END) OVER (ORDER BY l.post_id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS CHAR))-2),'\", \"','、')) FROM system_user_post l LEFT JOIN system_post n ON n.id=l.post_id AND n.tenant_id=system_users.tenant_id AND n.deleted=0 WHERE l.user_id=system_users.id AND l.tenant_id=system_users.tenant_id AND l.deleted=0 LIMIT 1),'-')")),
        Map.entry("loginLocked",text("CASE WHEN login_locked=1 AND login_locked_time > DATE_SUB(NOW(),INTERVAL 30 MINUTE) THEN '已锁定' ELSE '未锁定' END"))
    );
    static void apply(UserPageReqVO request, com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AdminUserDO> query) {
        ListFilterQueryUtils.apply(request, query, FIELDS);
        if (request.getSortOrder() != null) {
            var field = FIELDS.get(request.getSortKey());
            if (field == null) throw invalidParamException("不支持的用户排序列");
            String expression = field.expression();
            if ("loginLocked".equals(request.getSortKey())) expression = "(login_locked=1 AND login_locked_time > DATE_SUB(NOW(),INTERVAL 30 MINUTE))";
            query.last("ORDER BY (" + expression + ") IS NULL ASC, " + expression
                    + ("ascending".equals(request.getSortOrder()) ? " ASC" : " DESC") + ", system_users.id ASC");
        } else if (request.getDeptId() != null) {
            query.last("ORDER BY (system_users.dept_id=" + request.getDeptId() + ") DESC, "
                    + "(system_users.dept_id IS NULL) ASC, MAX(system_users.id) OVER(PARTITION BY system_users.dept_id) DESC, "
                    + "(system_users.id=(SELECT d.leader_user_id FROM system_dept d WHERE d.id=system_users.dept_id AND d.tenant_id=system_users.tenant_id AND d.deleted=0)) DESC, system_users.id DESC");
        } else query.orderByDesc(AdminUserDO::getId);
    }
}
