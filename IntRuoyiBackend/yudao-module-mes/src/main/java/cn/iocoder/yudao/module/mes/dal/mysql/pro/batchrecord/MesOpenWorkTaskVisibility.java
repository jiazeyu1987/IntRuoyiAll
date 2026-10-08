package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

/** Fixed SQL predicates shared by the existing my-page wrapper and workbench count/chunks. */
public final class MesOpenWorkTaskVisibility {
    private MesOpenWorkTaskVisibility() { }
    private static final String TASK = "mes_pro_edhr_work_task";
    public static final String OWNER_SQL = "(" + TASK + ".assignee_user_id = #{q.userId}"
            + " OR LOCATE(CONCAT(',', #{q.userId}, ','), CONCAT(',', " + TASK + ".candidate_user_snapshot, ',')) > 0)";
    public static final String OPEN_BATCH_SQL = batchSql(null);

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
        return "(" + TASK + ".batch_execution_id IS NULL OR NOT EXISTS ("
                + "SELECT 1 FROM mes_pro_edhr_batch_execution b WHERE b.id = " + TASK + ".batch_execution_id"
                + " AND b.tenant_id = " + TASK + ".tenant_id AND b.deleted = 0 AND " + excluded + "))";
    }
}
