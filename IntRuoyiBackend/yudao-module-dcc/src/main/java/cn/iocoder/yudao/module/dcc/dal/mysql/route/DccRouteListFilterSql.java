package cn.iocoder.yudao.module.dcc.dal.mysql.route;

import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import java.util.Map;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text;

/** SQL counterparts of routes/index.vue's displayed category, action and stage subjects. */
final class DccRouteListFilterSql {
    private DccRouteListFilterSql() {}
    static Map<String, ListFilterQueryUtils.Field> fields() {
        return Map.of("categoryName", text("CONCAT(COALESCE((SELECT c.name FROM dcc_file_category c WHERE c.id=dcc_category_approval_route.category_id AND c.tenant_id=dcc_category_approval_route.tenant_id AND c.deleted=0),'未关联类别'),' ',COALESCE(CAST(category_id AS CHAR),'-'))"),
                "actionType", text("CASE COALESCE(NULLIF(action_type,''),'LEGACY') WHEN 'LEGACY' THEN '历史通用流程' WHEN 'NEW' THEN '上传' WHEN 'REVISION' THEN '升版' WHEN 'OBSOLETE' THEN '作废' ELSE action_type END"),
                "node1", text(stage(1)), "node2", text(stage(2)), "node3", text(stage(3)), "node4", text(stage(4)));
    }
    private static String stage(int number) {
        String ids = "CASE WHEN NULLIF(TRIM(n.candidate_source_ids),'') IS NOT NULL THEN CONCAT('[',n.candidate_source_ids,']') WHEN n.candidate_source_id IS NOT NULL THEN JSON_ARRAY(n.candidate_source_id) ELSE JSON_ARRAY() END";
        String positionName = "COALESCE(NULLIF(TRIM(p.name),''), CASE candidate.id WHEN 900332 THEN '文控' WHEN 900333 THEN '部门负责人' WHEN 900334 THEN '部门授权代表' WHEN 900335 THEN '编制部门负责人' WHEN 900336 THEN '授权代表' ELSE CONCAT('审批角色#',candidate.id) END)";
        String positions = "(SELECT GROUP_CONCAT(CASE WHEN " + positionName + " REGEXP '^(审批角色#[0-9]+|[a-z]+(-[a-z0-9]+)+)$' THEN NULL ELSE " + positionName + " END ORDER BY candidate.ord SEPARATOR '、') FROM JSON_TABLE(" + ids + ", '$[*]' COLUMNS(ord FOR ORDINALITY,id BIGINT PATH '$')) candidate LEFT JOIN dcc_approval_position p ON p.id=candidate.id AND p.tenant_id=n.tenant_id AND p.deleted=0 AND p.active=1 AND (p.source REGEXP '^INTAUTH:[0-9]+$' OR p.code IN ('LOCAL-ROLE-APPROVER-DEPT','LOCAL-ROLE-AUTH-REP')))";
        String explicit = "COALESCE(NULLIF(TRIM(n.subject_label),''),NULLIF(TRIM(n.subject_name),''),NULLIF(TRIM(n.subject_department_path),''))";
        String username = "TRIM(COALESCE(u.username,''))";
        String nickname = "TRIM(COALESCE(u.nickname,''))";
        String primary = "CASE WHEN " + nickname + " IN ('','-') OR " + nickname + " REGEXP '[?\uFFFD□]' THEN " + username + " ELSE " + nickname + " END";
        String dept = "TRIM(COALESCE(ud.name,''))";
        String details = "CONCAT_WS(' / ',CASE WHEN " + username + "<>'' AND " + username + "<>" + primary + " THEN " + username + " END,NULLIF(" + dept + ",''))";
        String user = "CASE WHEN u.id IS NULL THEN CONCAT('用户#',candidate.id) WHEN " + primary + "<>'' AND " + details + "<>'' THEN CONCAT(" + primary + ",' ('," + details + ",')') ELSE COALESCE(NULLIF(" + primary + ",''),NULLIF(" + dept + ",''),'-') END";
        String candidates = "(SELECT GROUP_CONCAT(CASE n.candidate_source_type WHEN 'USER' THEN " + user + " WHEN 'DEPT' THEN COALESCE(d.name,CONCAT('部门#',candidate.id)) WHEN 'POSITION' THEN '-' ELSE CAST(candidate.id AS CHAR) END ORDER BY candidate.ord SEPARATOR '、') FROM JSON_TABLE(" + ids + ", '$[*]' COLUMNS(ord FOR ORDINALITY,id BIGINT PATH '$')) candidate LEFT JOIN system_users u ON u.id=candidate.id AND u.tenant_id=n.tenant_id AND u.deleted=0 AND u.status=0 LEFT JOIN system_dept ud ON ud.id=u.dept_id AND ud.tenant_id=u.tenant_id AND ud.deleted=0 LEFT JOIN system_dept d ON d.id=candidate.id AND d.tenant_id=n.tenant_id AND d.deleted=0 AND d.status=0)";
        String subject = "CASE WHEN n.candidate_source_type='POSITION' AND NULLIF(" + positions + ",'') IS NOT NULL THEN " + positions + " WHEN " + explicit + " IS NOT NULL AND " + explicit + " NOT REGEXP '^(审批角色#[0-9]+|[a-z]+(-[a-z0-9]+)+)$' THEN " + explicit + " ELSE COALESCE(" + candidates + ",'-') END";
        return "COALESCE((SELECT GROUP_CONCAT(DISTINCT " + subject + " ORDER BY COALESCE(n.stage_order,n.stage_no),n.sort SEPARATOR '、') FROM dcc_category_approval_route_node n WHERE n.route_id=dcc_category_approval_route.id AND n.tenant_id=dcc_category_approval_route.tenant_id AND n.deleted=0 AND n.stage_no=" + number + "),'-')";
    }
}
