package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

/**
 * Fixed business predicate for open dossier duties. Ownership, task status and actor permissions
 * remain the caller's predicates. The qualifier is deliberately the real work-task table so the
 * same SQL can be used by my-page, workbench count/chunks/export and identity checks.
 */
public final class MesOpenDossierTaskVisibility {
    private MesOpenDossierTaskVisibility() { }

    public static final String OPEN_SQL = """
            (mes_pro_edhr_work_task.task_type IS NULL
             OR mes_pro_edhr_work_task.task_type <> 'DOSSIER_COMPLETE'
             OR (
              mes_pro_edhr_work_task.business_scope_type = 'ACTIVE_ORDER_DOSSIER'
              AND mes_pro_edhr_work_task.review_source_type IN
                  ('INCOMING_INSPECTION_FILE', 'STERILIZATION_FILE', 'FINISHED_PRODUCT_FILE', 'OTHER_FILE')
              AND EXISTS (
                SELECT 1
                FROM mes_pro_process_pool_active_order dossier_order
                LEFT JOIN mes_pro_process_pool_active_order_release_application dossier_app
                  ON dossier_app.id = (
                    SELECT MAX(dossier_round.id)
                    FROM mes_pro_process_pool_active_order_release_application dossier_round
                    WHERE dossier_round.active_order_id = dossier_order.id
                      AND dossier_round.tenant_id = dossier_order.tenant_id
                      AND dossier_round.deleted = 0)
                  AND dossier_app.tenant_id = dossier_order.tenant_id AND dossier_app.deleted = 0
                WHERE dossier_order.id = mes_pro_edhr_work_task.business_scope_id
                  AND dossier_order.tenant_id = mes_pro_edhr_work_task.tenant_id
                  AND dossier_order.work_order_id = mes_pro_edhr_work_task.work_order_id
                  AND dossier_order.deleted = 0 AND dossier_order.active_status = 'ACTIVE'
                  AND (dossier_order.business_status IS NULL
                       OR dossier_order.business_status NOT IN ('FROZEN', 'CLOSED', 'VOIDED', 'RELEASED'))
                  AND NOT EXISTS (
                    SELECT 1 FROM mes_pro_edhr_nonconformance_review dossier_ncr
                    WHERE dossier_ncr.tenant_id = dossier_order.tenant_id
                      AND dossier_ncr.work_order_id = dossier_order.work_order_id
                      AND dossier_ncr.deleted = 0
                      AND (dossier_ncr.review_status = 'pending_review' OR dossier_ncr.disposition = 'void'))
                  AND (mes_pro_edhr_work_task.review_source_id IS NULL
                       OR mes_pro_edhr_work_task.review_source_id = dossier_app.id)
                  AND (dossier_app.id IS NULL OR (
                    dossier_app.active_order_id = dossier_order.id
                    AND dossier_app.work_order_id = dossier_order.work_order_id
                    AND (dossier_app.application_status IS NULL
                         OR dossier_app.application_status NOT IN ('RELEASED', 'BATCH_VOIDED'))
                    AND (dossier_app.batch_execution_id IS NULL OR EXISTS (
                      SELECT 1 FROM mes_pro_edhr_batch_execution dossier_batch
                      WHERE dossier_batch.id = dossier_app.batch_execution_id
                        AND dossier_batch.tenant_id = dossier_order.tenant_id
                        AND dossier_batch.work_order_id = dossier_order.work_order_id
                        AND dossier_batch.deleted = 0
                        AND dossier_batch.status NOT IN (15, 40, 50, 60)
                        AND NOT EXISTS (
                          SELECT 1 FROM mes_pro_edhr_nonconformance_review dossier_batch_ncr
                          WHERE dossier_batch_ncr.tenant_id = dossier_order.tenant_id
                            AND dossier_batch_ncr.batch_execution_id = dossier_batch.id
                            AND dossier_batch_ncr.deleted = 0 AND dossier_batch_ncr.review_status = 'pending_review')
                        AND NOT EXISTS (
                          SELECT 1 FROM mes_pro_edhr_record_change_event dossier_void
                          WHERE dossier_void.tenant_id = dossier_order.tenant_id
                            AND dossier_void.batch_execution_id = dossier_batch.id AND dossier_void.deleted = 0
                            AND dossier_void.target_scope = 'BATCH' AND dossier_void.change_type = 'VOID'
                            AND dossier_void.change_status IN ('DRAFT', 'SUBMITTED', 'APPROVED'))
                    )))
                  )
              )
             ))
            """;
}
