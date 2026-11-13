package cn.iocoder.yudao.module.dcc.dal.mysql.route;

import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import java.util.Map;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text;

/** Display projection is calculated once before route filtering and paging. */
public final class DccRouteListFilterSql {
    private DccRouteListFilterSql() {}
    static Map<String,ListFilterQueryUtils.Field> fields() {
        return Map.of("categoryName",text("CONCAT(COALESCE(category_name,'未关联类别'),' ',COALESCE(CAST(category_id AS CHAR),'-'))"),
                "actionType",text("CASE COALESCE(NULLIF(action_type,''),'LEGACY') WHEN 'LEGACY' THEN '历史通用流程' WHEN 'NEW' THEN '上传' WHEN 'REVISION' THEN '升版' WHEN 'OBSOLETE' THEN '作废' ELSE action_type END"),
                "node1",text("node1"),"node2",text("node2"),"node3",text("node3"),"node4",text("node4"));
    }
    public static String count() { return statement(true); }
    public static String page() { return statement(false); }
    private static String statement(boolean count) {
        String username = "TRIM(COALESCE(u.username,'')) COLLATE utf8mb4_bin";
        String nickname = "TRIM(COALESCE(u.nickname,'')) COLLATE utf8mb4_bin";
        String primary = "(CASE WHEN " + nickname + " IN ('','-') OR " + nickname + " REGEXP '[?\uFFFD□]' THEN " + username + " ELSE " + nickname + " END COLLATE utf8mb4_bin)";
        String dept = "TRIM(COALESCE(ud.name,'')) COLLATE utf8mb4_bin";
        String users="user_facts AS (SELECT u.id,u.tenant_id,"+username+" AS username,"+primary+" AS primary_name,"+dept+" AS department_name FROM system_users u LEFT JOIN system_dept ud ON ud.id=u.dept_id AND ud.tenant_id=u.tenant_id AND ud.deleted=0 WHERE u.tenant_id=#{tenant} AND u.deleted=0 AND u.status=0),"
                +"user_details AS (SELECT u.*,CONCAT_WS(' / ',CASE WHEN u.username!='' AND u.username!=u.primary_name THEN u.username END,NULLIF(u.department_name,'')) COLLATE utf8mb4_bin AS details FROM user_facts u),"
                +"user_labels AS (SELECT u.id,u.tenant_id,CASE WHEN u.primary_name!='' AND u.details!='' THEN CONCAT(u.primary_name,' (',u.details,')') ELSE COALESCE(NULLIF(u.primary_name,''),NULLIF(u.department_name,''),'-') END COLLATE utf8mb4_bin AS label FROM user_details u),";
        String user = "COALESCE(u.label,CONCAT('用户#',candidate.id))";
        String position="(COALESCE(NULLIF(TRIM(p.name),''),CASE candidate.id WHEN 900332 THEN '文控' WHEN 900333 THEN '部门负责人' WHEN 900334 THEN '部门授权代表' WHEN 900335 THEN '编制部门负责人' WHEN 900336 THEN '授权代表' ELSE CONCAT('审批角色#',candidate.id) END) COLLATE utf8mb4_bin)";
        String technical="'^(审批角色#[0-9]+|[a-z]+(-[a-z0-9]+)+)$'";
        String candidateLabel="CASE n.candidate_source_type WHEN 'USER' THEN "+user+" WHEN 'DEPT' THEN COALESCE(d.name,CONCAT('部门#',candidate.id)) WHEN 'POSITION' THEN '-' ELSE CAST(candidate.id AS CHAR) END";
        String sql="WITH RECURSIVE "+users+"""
            candidate AS (
              SELECT n.id AS node_id,1 AS ord,CAST(SUBSTRING_INDEX(COALESCE(NULLIF(TRIM(n.candidate_source_ids) COLLATE utf8mb4_bin,''),CAST(n.candidate_source_id AS CHAR),''),',',1) AS UNSIGNED) AS id,
                CAST(IF(LOCATE(',',COALESCE(NULLIF(TRIM(n.candidate_source_ids) COLLATE utf8mb4_bin,''),CAST(n.candidate_source_id AS CHAR),''))>0,SUBSTRING(COALESCE(NULLIF(TRIM(n.candidate_source_ids) COLLATE utf8mb4_bin,''),CAST(n.candidate_source_id AS CHAR),''),LOCATE(',',COALESCE(NULLIF(TRIM(n.candidate_source_ids) COLLATE utf8mb4_bin,''),CAST(n.candidate_source_id AS CHAR),''))+1),'') AS CHAR(1000)) AS rest
              FROM dcc_category_approval_route_node n WHERE n.tenant_id=#{tenant} AND n.deleted=0 AND COALESCE(NULLIF(TRIM(n.candidate_source_ids) COLLATE utf8mb4_bin,''),CAST(n.candidate_source_id AS CHAR),'')!=''
              UNION ALL
              SELECT node_id,ord+1,CAST(SUBSTRING_INDEX(rest,',',1) AS UNSIGNED),IF(LOCATE(',',rest)>0,SUBSTRING(rest,LOCATE(',',rest)+1),'') FROM candidate WHERE rest COLLATE utf8mb4_bin!=''
            ), candidate_labels AS (
              SELECT candidate.node_id,candidate.ord,
            """+candidateLabel+" AS candidate_label,CASE WHEN "+position+" REGEXP "+technical+" THEN NULL ELSE "+position+" END AS position_label "+"""
              FROM candidate JOIN dcc_category_approval_route_node n ON n.id=candidate.node_id AND n.tenant_id=#{tenant} AND n.deleted=0
              LEFT JOIN dcc_approval_position p ON p.id=candidate.id AND p.tenant_id=n.tenant_id AND p.deleted=0 AND p.active=1 AND (p.source REGEXP '^INTAUTH:[0-9]+$' OR p.code IN ('LOCAL-ROLE-APPROVER-DEPT','LOCAL-ROLE-AUTH-REP'))
              LEFT JOIN user_labels u ON u.id=candidate.id AND u.tenant_id=n.tenant_id
              LEFT JOIN system_dept d ON d.id=candidate.id AND d.tenant_id=n.tenant_id AND d.deleted=0 AND d.status=0
            ), node_candidates AS (
              SELECT node_id,GROUP_CONCAT(candidate_label ORDER BY ord SEPARATOR '、') AS candidate_label,
                GROUP_CONCAT(position_label ORDER BY ord SEPARATOR '、') AS position_label FROM candidate_labels GROUP BY node_id
            ), node_labels AS (
              SELECT n.id,n.route_id,n.stage_no,COALESCE(n.stage_order,n.stage_no) AS stage_order,n.sort,
                CASE WHEN n.candidate_source_type='POSITION' AND NULLIF(c.position_label COLLATE utf8mb4_bin,'') IS NOT NULL THEN c.position_label
            """+" WHEN COALESCE(NULLIF(TRIM(n.subject_label),''),NULLIF(TRIM(n.subject_name),''),NULLIF(TRIM(n.subject_department_path),'')) IS NOT NULL AND COALESCE(NULLIF(TRIM(n.subject_label),''),NULLIF(TRIM(n.subject_name),''),NULLIF(TRIM(n.subject_department_path),'')) NOT REGEXP "+technical
                +" THEN COALESCE(NULLIF(TRIM(n.subject_label),''),NULLIF(TRIM(n.subject_name),''),NULLIF(TRIM(n.subject_department_path),'')) WHEN n.candidate_source_type='POSITION' THEN '-' ELSE COALESCE(c.candidate_label,'-') END AS subject "+"""
              FROM dcc_category_approval_route_node n LEFT JOIN node_candidates c ON c.node_id=n.id WHERE n.tenant_id=#{tenant} AND n.deleted=0
            ), deduplicated_labels AS (
              SELECT n.*,ROW_NUMBER() OVER(PARTITION BY route_id,stage_no,subject COLLATE utf8mb4_bin ORDER BY stage_order,sort,id) AS occurrence FROM node_labels n
            ), stage_labels AS (
              SELECT route_id,stage_no,GROUP_CONCAT(subject ORDER BY stage_order,sort,id SEPARATOR '、') AS subject FROM deduplicated_labels WHERE occurrence=1 GROUP BY route_id,stage_no
            ), route_rows AS (
              SELECT route.*,category.name AS category_name,COALESCE(one.subject,'-') AS node1,COALESCE(two.subject,'-') AS node2,COALESCE(three.subject,'-') AS node3,COALESCE(four.subject,'-') AS node4
              FROM dcc_category_approval_route route
              LEFT JOIN dcc_file_category category ON category.id=route.category_id AND category.tenant_id=route.tenant_id AND category.deleted=0
              LEFT JOIN stage_labels one ON one.route_id=route.id AND one.stage_no=1
              LEFT JOIN stage_labels two ON two.route_id=route.id AND two.stage_no=2
              LEFT JOIN stage_labels three ON three.route_id=route.id AND three.stage_no=3
              LEFT JOIN stage_labels four ON four.route_id=route.id AND four.stage_no=4
              WHERE route.tenant_id=#{tenant} AND route.deleted=0
            )
            """;
        return "<script>"+sql+"SELECT /*+ SET_VAR(group_concat_max_len=4294967295) */ "+(count?"COUNT(*)":"dcc_category_approval_route.*")
                +" FROM route_rows dcc_category_approval_route WHERE 1=1 <if test='ew != null and !ew.emptyOfNormal'>AND (${ew.expression.normal.sqlSegment})</if>"
                +(count?"":" ORDER BY category_id,action_type,version_no DESC,id DESC LIMIT #{size} OFFSET #{offset}")+"</script>";
    }
}
