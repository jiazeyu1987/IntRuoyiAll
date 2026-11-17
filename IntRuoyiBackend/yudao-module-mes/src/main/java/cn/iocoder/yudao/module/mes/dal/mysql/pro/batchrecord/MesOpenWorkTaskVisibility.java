package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

/** Fixed SQL predicates shared by the existing my-page wrapper and workbench count/chunks. */
public final class MesOpenWorkTaskVisibility {
    private MesOpenWorkTaskVisibility() { }
    private static final String TASK = "mes_pro_edhr_work_task";
    public static final String OWNER_SQL = "(" + TASK + ".assignee_user_id = #{q.userId}"
            + " OR LOCATE(CONCAT(',', #{q.userId}, ','), CONCAT(',', " + TASK + ".candidate_user_snapshot, ',')) > 0)";
    private static final String DEVIATION_TYPES = "'DEVIATION_RECEIVE','DEVIATION_CORRECT','DEVIATION_VERIFY','DEVIATION_SIGN','DEVIATION_APPROVE','DEVIATION_CLOSE'";
    // Use the explicitly recorded production source. A batch is evidence, not deviation lifecycle authority.
    private static final String OPEN_DEVIATION_ORDER_SQL = "EXISTS (SELECT 1 FROM mes_pro_edhr_deviation dv"
            + " JOIN mes_pro_work_order wo ON wo.id = dv.work_order_id AND wo.tenant_id = dv.tenant_id AND wo.deleted = 0"
            + " WHERE " + TASK + ".business_scope_type = 'EDHR_DEVIATION'"
            + " AND dv.id = " + TASK + ".business_scope_id AND dv.tenant_id = " + TASK + ".tenant_id"
            + " AND dv.work_order_id = " + TASK + ".work_order_id AND dv.deleted = 0 AND dv.status = 'OPEN'"
            + " AND wo.status IN (1,2))";
    public static final String OPEN_BATCH_SQL = batchSql(null);
    public static final String LOCKED_SOURCE_SQL = withDeviationOrder("(#{lockedBatchStatus} IS NULL"
            + " OR (" + TASK + ".task_type IN ('RECORD_CHANGE_APPROVE','RECORD_CHANGE_CORRECTION') AND #{lockedBatchStatus} IN (30,50))"
            + " OR (" + TASK + ".task_type = 'PQC_RELEASE_CORRECTION' AND #{lockedBatchStatus} = 30)"
            + " OR (#{lockedBatchStatus} NOT IN (40,50,60) AND (#{lockedBatchStatus} != 30 OR " + TASK + ".task_type = 'ARCHIVE')))" );

    // NCR freezes the release action without completing its task. Keep pending release tasks
    // out of every open-task projection until the review has been disposed of.
    public static final String OPEN_PQC_RELEASE_SQL = """
            (mes_pro_edhr_work_task.task_type IS NULL
             OR mes_pro_edhr_work_task.task_type <> 'PQC_PRODUCTION_RELEASE'
             OR NOT EXISTS (
                SELECT 1 FROM mes_pro_edhr_nonconformance_review release_ncr
                LEFT JOIN mes_pro_process_pool_active_order_release_application release_app
                  ON mes_pro_edhr_work_task.business_scope_type = 'RELEASE_APPLICATION'
                  AND release_app.id = mes_pro_edhr_work_task.business_scope_id
                  AND release_app.tenant_id = mes_pro_edhr_work_task.tenant_id
                  AND release_app.deleted = 0
                WHERE release_ncr.tenant_id = mes_pro_edhr_work_task.tenant_id
                  AND release_ncr.deleted = 0 AND release_ncr.review_status = 'pending_review'
                  AND (release_ncr.work_order_id = mes_pro_edhr_work_task.work_order_id
                    OR release_ncr.batch_execution_id = mes_pro_edhr_work_task.batch_execution_id
                    OR release_ncr.active_order_id = release_app.active_order_id
                    OR release_ncr.work_order_id = release_app.work_order_id
                    OR release_ncr.batch_execution_id = release_app.batch_execution_id)
             ))
            """;

    public static String ownerWrapperSql() {
        return OWNER_SQL.replace("#{q.userId}", "{0}");
    }

    public static String batchSql(String taskType) {
        String normalized = taskType == null ? null : taskType.trim();
        String excluded;
        if (MesProEdhrWorkTaskMapper.TASK_TYPE_ARCHIVE.equals(normalized)) {
            excluded = "b.status IN (" + MesProEdhrWorkTaskMapper.ARCHIVE_TODO_EXCLUDED_BATCH_STATUS_SQL + ")";
        } else if (normalized != null && !normalized.isEmpty()) {
            excluded = "b.status IN (" + MesProEdhrWorkTaskMapper.TERMINAL_BATCH_STATUS_SQL + ")";
        } else {
            excluded = "(b.status IN (" + MesProEdhrWorkTaskMapper.ARCHIVE_TODO_EXCLUDED_BATCH_STATUS_SQL + ")"
                    + " OR (b.status = 30 AND (" + TASK + ".task_type IS NULL OR " + TASK + ".task_type != 'ARCHIVE')))";
        }
        return withDeviationOrder("(" + TASK + ".batch_execution_id IS NULL OR (" + TASK + ".task_type IN ('PQC_RELEASE_CORRECTION','RECORD_CHANGE_APPROVE','RECORD_CHANGE_CORRECTION')"
                + " AND EXISTS (SELECT 1 FROM mes_pro_edhr_batch_execution change_batch"
                + " WHERE change_batch.id = " + TASK + ".batch_execution_id"
                + " AND change_batch.tenant_id = " + TASK + ".tenant_id"
                + " AND change_batch.deleted = 0 AND ((" + TASK + ".task_type IN ('RECORD_CHANGE_APPROVE','RECORD_CHANGE_CORRECTION') AND change_batch.status IN (30,50))"
                + " OR (" + TASK + ".task_type = 'PQC_RELEASE_CORRECTION' AND change_batch.status = 30)))) OR NOT EXISTS ("
                + "SELECT 1 FROM mes_pro_edhr_batch_execution b WHERE b.id = " + TASK + ".batch_execution_id"
                + " AND b.tenant_id = " + TASK + ".tenant_id AND b.deleted = 0 AND " + excluded + "))");
    }

    private static String withDeviationOrder(String ordinaryTaskSql) {
        return "((" + TASK + ".task_type IN (" + DEVIATION_TYPES + ") AND " + OPEN_DEVIATION_ORDER_SQL + ")"
                + " OR ((" + TASK + ".task_type IS NULL OR " + TASK + ".task_type NOT IN (" + DEVIATION_TYPES + ")) AND "
                + ordinaryTaskSql + "))";
    }
}
