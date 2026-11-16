package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;

import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.Field;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodePageReqVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

/** SQL read projection for project discovery and the project-code list; no rows are filtered in Java. */
public final class DccProjectCodeListQuery {
    private DccProjectCodeListQuery() {}

    public static QueryWrapper<Object> filter(DccProjectCodePageReqVO request, List<Long> scope,
                                               boolean publicReader, boolean qaVisible) {
        QueryWrapper<Object> query = new QueryWrapper<>();
        if (scope != null) {
            if (scope.isEmpty()) query.apply("1 = 0"); else query.in("q.id", scope);
        }
        if (publicReader) query.eq("q.status", "ENABLE");
        query.eq(request.getProductMasterId() != null, "q.product_master_id", request.getProductMasterId());
        query.like(hasText(request.getProjectName()), "q.project_name", request.getProjectName());
        query.like(hasText(request.getProjectCode()), "q.project_code", request.getProjectCode());
        query.eq(hasText(request.getCategory()), "q.category", request.getCategory());
        query.eq(hasText(request.getPriority()), "q.priority", request.getPriority());
        query.eq(hasText(request.getStatus()), "q.status", request.getStatus());
        if (hasText(request.getKeyword())) {
            String keyword = request.getKeyword().trim();
            query.and(w -> w.like("q.doc_control_no", keyword).or().like("q.project_name", keyword)
                    .or().like("q.project_code", keyword).or().like("q.category", keyword)
                    .or().like("q.project_leader", keyword).or().like("q.project_engineer", keyword)
                    .or().like("q.storage_location", keyword));
        }
        query.eq(request.getRouteConfigured() != null, "q.route_configured", request.getRouteConfigured());
        query.eq(request.getMainBatchRecordConfigured() != null, "q.main_batch_configured", request.getMainBatchRecordConfigured());
        query.eq(request.getQaRegulationConfigured() != null, "q.qa_configured", request.getQaRegulationConfigured());
        if (Boolean.TRUE.equals(request.getRequireDccProductCode())) query.eq("q.valid_dcc_product", 1);
        return ListFilterQueryUtils.apply(request, query, Map.ofEntries(
                entry("docControlNo", text("q.doc_control_no")), entry("primaryCode", text("'无'")),
                entry("projectName", text("q.project_name")), entry("projectCode", text("q.project_code")),
                entry("category", text("q.category")), entry("associatedFileCount", number("q.associated_file_count")),
                entry("batchRecordTotalRecognitionJson", text("q.batch_record_total_recognition_json")),
                entry("routeStatus", text("q.route_display")), entry("mainBatchRecordStatus", text("q.main_batch_display")),
                entry("qaRegulationStatus", text(qaVisible ? "q.qa_display" : "'无查询权限 '")),
                entry("updateTime", dateTime("q.update_time"))));
    }

    private static boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }

    public static String count() { return script("COUNT(*)", false); }
    public static String page() { return script("q.*", true); }
    private static String script(String select, boolean page) {
        String sql = "<script>SELECT /*+ SET_VAR(group_concat_max_len=4294967295) */ " + select
                + " FROM (" + projection() + ") q WHERE 1=1"
                + " <if test=\"ew != null and !ew.emptyOfNormal\"> AND (${ew.sqlSegment}) </if>";
        if (page) {
            String doc = "TRIM(COALESCE(q.doc_control_no, ''))";
            String digits = "COALESCE(NULLIF(TRIM(LEADING '0' FROM " + doc + "), ''), '0')";
            sql += " ORDER BY <choose><when test=\"req.fileCountSort != null and req.fileCountSort.toLowerCase() == 'asc'\">q.associated_file_count ASC,</when>"
                    + "<when test=\"req.fileCountSort != null and req.fileCountSort.toLowerCase() == 'desc'\">q.associated_file_count DESC,</when></choose>"
                    + "CASE WHEN " + doc + " REGEXP '^[0-9]+$' THEN 0 ELSE 1 END ASC,"
                    + "CASE WHEN " + doc + " REGEXP '^[0-9]+$' THEN LENGTH(" + digits + ") ELSE NULL END ASC,"
                    + "CASE WHEN " + doc + " REGEXP '^[0-9]+$' THEN CAST(" + digits + " AS BINARY) ELSE NULL END ASC,"
                    + "CAST(" + doc + " AS BINARY) ASC,q.id ASC"
                    + " <if test=\"req.pageSize != -1\"> LIMIT #{req.pageSize} OFFSET #{offset} </if>";
        }
        return sql + "</script>";
    }

    private static String projection() {
        String routes = " FROM mes_pro_route r WHERE r.tenant_id=p.tenant_id AND r.deleted=0 AND EXISTS(SELECT 1 FROM mes_pro_route_product rp JOIN mes_md_item i ON i.id=rp.item_id AND i.tenant_id=p.tenant_id AND i.deleted=0 JOIN dcc_project_code pc ON TRIM(pc.project_code)=i.code AND pc.tenant_id=p.tenant_id AND pc.deleted=0 AND pc.status='ENABLE' WHERE rp.route_id=r.id AND rp.tenant_id=p.tenant_id AND rp.deleted=0 AND pc.project_name=TRIM(p.project_name))";
        String routeCount = "(SELECT COUNT(*)" + routes + ")";
        String routeVersions = "(SELECT GROUP_CONCAT(DISTINCT TRIM(rv.version_no) ORDER BY (SELECT MIN(rp.id) FROM mes_pro_route_product rp JOIN mes_md_item i ON i.id=rp.item_id AND i.tenant_id=p.tenant_id AND i.deleted=0 JOIN dcc_project_code pc ON TRIM(pc.project_code)=i.code AND pc.tenant_id=p.tenant_id AND pc.deleted=0 AND pc.status='ENABLE' WHERE rp.route_id=r.id AND rp.tenant_id=p.tenant_id AND rp.deleted=0 AND pc.project_name=TRIM(p.project_name)) SEPARATOR '、') FROM mes_pro_route r JOIN mes_pro_route_version rv ON rv.route_id=r.id AND rv.tenant_id=p.tenant_id AND rv.deleted=0 AND rv.active=1 AND rv.lifecycle_status='ACTIVE' WHERE r.tenant_id=p.tenant_id AND r.deleted=0 AND NULLIF(TRIM(rv.version_no),'') IS NOT NULL AND EXISTS(SELECT 1 FROM mes_pro_route_product rp JOIN mes_md_item i ON i.id=rp.item_id AND i.tenant_id=p.tenant_id AND i.deleted=0 JOIN dcc_project_code pc ON TRIM(pc.project_code)=i.code AND pc.tenant_id=p.tenant_id AND pc.deleted=0 AND pc.status='ENABLE' WHERE rp.route_id=r.id AND rp.tenant_id=p.tenant_id AND rp.deleted=0 AND pc.project_name=TRIM(p.project_name)))";
        String definitions = " FROM mes_pro_batch_record_definition d WHERE d.tenant_id=p.tenant_id AND d.deleted=0 AND d.batch_record_name=TRIM(p.project_name)";
        String definitionCount = "(SELECT COUNT(*)" + definitions + ")";
        String mainExists = "EXISTS(SELECT 1" + definitions + " AND EXISTS(SELECT 1 FROM mes_pro_batch_record_report br WHERE br.batch_record_definition_id=d.id AND br.tenant_id=p.tenant_id AND br.deleted=0 AND (br.form_slot_type='MAIN' OR br.form_slot_type IS NULL)))";
        String mainState = "CASE WHEN " + definitionCount + ">1 THEN '重复' WHEN " + mainExists + " THEN '已配置' ELSE '未配置' END";
        String mainVersions = "(SELECT GROUP_CONCAT(DISTINCT TRIM(CONCAT(CASE WHEN NULLIF(TRIM(d.route_key),'') IS NULL THEN '-' ELSE d.route_key END,'/',CASE WHEN NULLIF(TRIM(v.version_no),'') IS NULL THEN '-' ELSE v.version_no END)) ORDER BY d.id SEPARATOR '、') FROM mes_pro_batch_record_definition d LEFT JOIN mes_pro_batch_record_version v ON v.id=d.current_version_id AND v.tenant_id=p.tenant_id AND v.deleted=0 WHERE d.tenant_id=p.tenant_id AND d.deleted=0 AND d.batch_record_name=TRIM(p.project_name))";
        String qaId = "(SELECT qr.id FROM mes_qa_inspection_regulation qr WHERE qr.tenant_id=p.tenant_id AND qr.deleted=0 AND qr.owner_module='MES_QA' AND qr.dcc_project_code_id=p.id ORDER BY qr.id DESC LIMIT 1)";
        String publishedVersion = "(SELECT qv.version_no FROM mes_qa_inspection_regulation_version qv WHERE qv.regulation_id=" + qaId + " AND qv.tenant_id=p.tenant_id AND qv.deleted=0 AND qv.lifecycle_status='PUBLISHED' ORDER BY qv.published_at DESC,qv.id DESC LIMIT 1)";
        String publishedExists = "EXISTS(SELECT 1 FROM mes_qa_inspection_regulation_version qv WHERE qv.regulation_id=" + qaId + " AND qv.tenant_id=p.tenant_id AND qv.deleted=0 AND qv.lifecycle_status='PUBLISHED')";
        // Aggregate effective associations once per tenant, rather than rescan every file for each project.
        String fileCounts = " LEFT JOIN (SELECT linked.project_id,COUNT(*) AS file_count FROM ("
                + "SELECT COALESCE(cf.dcc_project_code_id,(SELECT rr.matched_project_code_id FROM dcc_controlled_file_recognition_record rr WHERE rr.controlled_file_id=cf.id AND rr.tenant_id=#{tenantId} AND rr.deleted=0 AND rr.status='SUCCESS' AND rr.matched_project_code_id IS NOT NULL ORDER BY rr.id DESC LIMIT 1)) AS project_id FROM dcc_controlled_file cf WHERE cf.tenant_id=#{tenantId} AND cf.deleted=0"
                + " UNION ALL SELECT rf.dcc_project_code_id AS project_id FROM dcc_registration_certificate_file rf WHERE rf.tenant_id=#{tenantId} AND rf.deleted=0 AND rf.status='BOUND'"
                + ") linked WHERE linked.project_id IS NOT NULL GROUP BY linked.project_id) fc ON fc.project_id=p.id";
        String fileCount = "COALESCE(fc.file_count,0)";
        return "SELECT p.*," + fileCount + " AS associated_file_count,"
                + "(" + routeCount + ">0) AS route_configured,(" + definitionCount + ">1 OR " + mainExists + ") AS main_batch_configured,"
                + "(" + qaId + " IS NOT NULL) AS qa_configured,"
                + "EXISTS(SELECT 1 FROM mdm_product mp WHERE mp.id=p.product_master_id AND mp.tenant_id=p.tenant_id AND mp.deleted=0 AND mp.status='ENABLE' AND mp.dcc_product_code IS NOT NULL) AS valid_dcc_product,"
                + "CONCAT(CASE " + routeCount + " WHEN 0 THEN '未配置' WHEN 1 THEN '已配置' ELSE '重复' END,' ',COALESCE(" + routeVersions + ",'')) AS route_display,"
                + "CONCAT(" + mainState + ",' ',COALESCE(" + mainVersions + ",'')) AS main_batch_display,"
                + "CONCAT(CASE WHEN " + publishedExists + " THEN '已发布' WHEN " + qaId + " IS NOT NULL THEN '草稿' ELSE '未配置' END,' ',COALESCE(TRIM(" + publishedVersion + "),'')) AS qa_display"
                + " FROM dcc_project_code p" + fileCounts + " WHERE p.deleted=0 AND p.library_scope='PROJECT' AND p.tenant_id=#{tenantId}";
    }
}
