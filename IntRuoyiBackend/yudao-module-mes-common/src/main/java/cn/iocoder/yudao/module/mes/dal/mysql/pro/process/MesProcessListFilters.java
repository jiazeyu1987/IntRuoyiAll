package cn.iocoder.yudao.module.mes.dal.mysql.pro.process;

/** SQL projections matching the process list's route and workstation enrichment. */
final class MesProcessListFilters {
    private MesProcessListFilters() {}

    static String quantityFactor(Long routeId) {
        String routeJoin = routeId == null ? " JOIN " : " LEFT JOIN ";
        return "(SELECT COALESCE(fc.production_quantity_factor,1.000000)"
                + " FROM mes_pro_route_process rp" + routeJoin
                + "mes_pro_route r ON r.id=rp.route_id AND r.tenant_id=rp.tenant_id AND r.deleted=0"
                + " LEFT JOIN mes_pro_route_flow_process_config fc ON fc.route_process_id=rp.id"
                + " AND fc.tenant_id=rp.tenant_id AND fc.deleted=0 AND fc.use_type='SCHEDULE'"
                + (routeId == null ? "" : " AND fc.route_id=" + routeId)
                + " WHERE " + routeScope(routeId)
                + " ORDER BY CASE WHEN r.id IS NULL OR CAST(LEFT(TRIM(r.name),4) AS BINARY)='E2E-'"
                + " OR CAST(LEFT(TRIM(r.name),6) AS BINARY)='ADMIN-' THEN 1 ELSE 0 END,"
                + "rp.route_id,rp.sort IS NULL,rp.sort,rp.id LIMIT 1)";
    }

    static String workstationNames(Long routeId) {
        String display = "COALESCE(NULLIF(CONCAT_WS(' ',NULLIF(TRIM(w.code),''),"
                + "NULLIF(TRIM(w.name),'')),''),CONCAT('工作站#',w.id))";
        String routeJoin = routeId == null
                ? " JOIN mes_pro_route r ON r.id=rp.route_id AND r.tenant_id=rp.tenant_id AND r.deleted=0" : "";
        String bound = "SELECT 1 FROM mes_pro_route_process rp" + routeJoin
                + " WHERE " + routeScope(routeId) + " AND rp.workstation_id IS NOT NULL";
        // The route service preserves route/sort order, then removes duplicate display strings.
        // Direct workstations are used only when this process has no formal workstation binding.
        String candidates = "SELECT " + display + " AS display_name,rp.route_id AS route_order,"
                + "rp.sort AS sort_order,rp.id AS source_order FROM mes_pro_route_process rp"
                + routeJoin + " JOIN mes_md_workstation w ON w.id=rp.workstation_id"
                + " AND w.tenant_id=rp.tenant_id AND w.deleted=0 WHERE " + routeScope(routeId)
                + " UNION ALL SELECT " + display + ",0,0,w.id FROM mes_md_workstation w"
                + " WHERE w.tenant_id=mes_pro_process.tenant_id AND w.deleted=0"
                + " AND NOT EXISTS(" + bound + ") AND EXISTS(SELECT 1 FROM mes_pro_process identity_process"
                + " WHERE identity_process.id=w.process_id AND identity_process.tenant_id=mes_pro_process.tenant_id"
                + " AND (identity_process.id=mes_pro_process.id OR (identity_process.deleted=1"
                + " AND identity_process.code=TRIM(mes_pro_process.code)"
                + " AND CAST(TRIM(identity_process.code) AS BINARY)=CAST(TRIM(mes_pro_process.code) AS BINARY))))";
        String array = "(SELECT JSON_ARRAYAGG(names.display_name) OVER(ORDER BY names.route_order,"
                + "names.sort_order,names.source_order ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)"
                + " FROM LATERAL (SELECT candidates.*,ROW_NUMBER() OVER(PARTITION BY CAST(candidates.display_name AS BINARY)"
                + " ORDER BY candidates.route_order,candidates.sort_order,candidates.source_order) AS duplicate_no"
                + " FROM LATERAL (" + candidates + ") candidates) names WHERE names.duplicate_no=1 LIMIT 1)";
        // JSON aggregation has no group_concat_max_len truncation; replace only JSON element boundaries.
        return "COALESCE(JSON_UNQUOTE(CONCAT(CHAR(34),REPLACE(SUBSTRING(CAST(" + array
                + " AS CHAR),3,CHAR_LENGTH(CAST(" + array + " AS CHAR))-4),"
                + "CONCAT(CHAR(34),', ',CHAR(34)),'、'),CHAR(34))),'')";
    }

    private static String routeScope(Long routeId) {
        return "rp.process_id=mes_pro_process.id AND rp.tenant_id=mes_pro_process.tenant_id AND rp.deleted=0"
                + (routeId == null ? "" : " AND rp.route_id=" + routeId);
    }
}
