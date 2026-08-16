package cn.iocoder.yudao.module.mes.productionrelease.core;

public final class MesReleaseFlowAuditEventType {

    public static final String PQC_PRODUCTION_RELEASE_APPLIED = "PQC_PRODUCTION_RELEASE_APPLIED";
    public static final String PQC_PRODUCTION_RELEASE_APPROVED = "PQC_PRODUCTION_RELEASE_APPROVED";
    public static final String PQC_PRODUCTION_RELEASE_REJECTED = "PQC_PRODUCTION_RELEASE_REJECTED";
    public static final String BATCH_EXECUTION_CREATED_FROM_RELEASE = "BATCH_EXECUTION_CREATED_FROM_RELEASE";
    public static final String RELEASE_REPORT_NODE_COMPLETED = "RELEASE_REPORT_NODE_COMPLETED";
    public static final String RELEASE_REPORT_UPLOAD_COMPLETED = "RELEASE_REPORT_UPLOAD_COMPLETED";
    public static final String MANAGER_RELEASE_TASK_CREATED = "MANAGER_RELEASE_TASK_CREATED";
    public static final String BATCH_RECORD_RELEASE_APPROVED = "BATCH_RECORD_RELEASE_APPROVED";

    private MesReleaseFlowAuditEventType() {
    }
}
