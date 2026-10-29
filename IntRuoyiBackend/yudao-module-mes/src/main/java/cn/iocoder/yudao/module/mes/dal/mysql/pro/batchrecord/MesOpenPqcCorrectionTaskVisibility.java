package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

/** Fixed open-task business predicate. Caller retains actor permissions, ownership and status predicates. */
public final class MesOpenPqcCorrectionTaskVisibility {
    private MesOpenPqcCorrectionTaskVisibility() { }

    public static final String OPEN_SQL = """
            (mes_pro_edhr_work_task.task_type IS NULL
             OR mes_pro_edhr_work_task.task_type <> 'PQC_RELEASE_CORRECTION'
             OR (
              mes_pro_edhr_work_task.business_scope_type = 'RELEASE_APPLICATION'
              AND mes_pro_edhr_work_task.ownership_locked = 1
              AND EXISTS (
                SELECT 1 FROM mes_pro_process_pool_active_order_release_application correction_app
                INNER JOIN mes_pro_process_pool_active_order correction_order
                  ON correction_order.id = correction_app.active_order_id
                  AND correction_order.tenant_id = correction_app.tenant_id AND correction_order.deleted = 0
                INNER JOIN mes_pro_edhr_batch_execution correction_batch
                  ON correction_batch.id = correction_app.batch_execution_id
                  AND correction_batch.tenant_id = correction_app.tenant_id AND correction_batch.deleted = 0
                INNER JOIN mes_pro_work_order correction_work_order
                  ON correction_work_order.id = correction_app.work_order_id
                  AND correction_work_order.tenant_id = correction_app.tenant_id AND correction_work_order.deleted = 0
                WHERE correction_app.id = mes_pro_edhr_work_task.business_scope_id
                  AND correction_app.tenant_id = mes_pro_edhr_work_task.tenant_id AND correction_app.deleted = 0
                  AND ((correction_app.application_status = 'PQC_RELEASE_REJECTED' AND correction_app.pqc_decision = 'REJECT'
                    AND mes_pro_edhr_work_task.review_source_type = 'PQC_REJECTION'
                    AND correction_app.pqc_release_work_task_id = mes_pro_edhr_work_task.review_source_id)
                    OR (correction_app.pqc_decision = 'APPROVE'
                    AND ((correction_app.application_status = 'MARKET_RELEASE_WITHDRAWN'
                          AND mes_pro_edhr_work_task.review_source_type = 'MARKET_WITHDRAWAL')
                         OR (correction_app.application_status = 'MARKET_RELEASE_REJECTED'
                          AND mes_pro_edhr_work_task.review_source_type = 'MARKET_REJECTION'))
                    AND correction_app.release_transaction_id = mes_pro_edhr_work_task.review_source_id
                    AND EXISTS (SELECT 1 FROM mes_pro_edhr_release_transaction correction_release
                      WHERE correction_release.id = correction_app.release_transaction_id
                        AND correction_release.tenant_id = correction_app.tenant_id AND correction_release.deleted = 0
                        AND correction_release.batch_execution_id = correction_app.batch_execution_id
                        AND ((correction_app.application_status = 'MARKET_RELEASE_WITHDRAWN' AND correction_release.release_status = 'WITHDRAWN')
                          OR (correction_app.application_status = 'MARKET_RELEASE_REJECTED' AND correction_release.release_status = 'REJECTED'
                           AND EXISTS (SELECT 1 FROM mes_pro_edhr_nonconformance_review correction_concession
                            WHERE correction_concession.id = (SELECT MAX(latest_concession.id) FROM mes_pro_edhr_nonconformance_review latest_concession
                              WHERE latest_concession.tenant_id = correction_app.tenant_id AND latest_concession.deleted = 0
                                AND latest_concession.source_type = 'ACTIVE_ORDER' AND latest_concession.source_id = correction_app.active_order_id)
                              AND correction_concession.batch_execution_id = correction_app.batch_execution_id
                              AND correction_concession.review_status = 'closed' AND correction_concession.disposition = 'concession_release'
                              AND correction_concession.qa_user_id IS NOT NULL AND correction_concession.closed_at IS NOT NULL
                              AND correction_concession.remark = CONCAT('上市放行不予批准；原放行事务:',correction_release.id)))))))
                  AND correction_app.applied_by = mes_pro_edhr_work_task.assignee_user_id
                  AND correction_order.leader_user_id = mes_pro_edhr_work_task.assignee_user_id
                  AND correction_app.batch_execution_id = mes_pro_edhr_work_task.batch_execution_id
                  AND correction_app.work_order_id = mes_pro_edhr_work_task.work_order_id
                  AND correction_order.work_order_id = correction_app.work_order_id
                  AND correction_batch.work_order_id = correction_app.work_order_id
                  AND correction_order.active_status = 'ACTIVE'
                  AND (correction_order.business_status IS NULL
                       OR correction_order.business_status NOT IN ('FROZEN', 'CLOSED', 'VOIDED', 'RELEASED', 'CANCELED'))
                  AND correction_batch.status NOT IN (15, 40, 50, 60)
                  AND (correction_work_order.temporary_frozen IS NULL OR correction_work_order.temporary_frozen = 0)
                  AND (correction_work_order.status IS NULL OR correction_work_order.status <> 3)
                  AND NOT EXISTS (
                    SELECT 1 FROM mes_pro_process_pool_active_order_release_application correction_next
                    WHERE correction_next.active_order_id = correction_app.active_order_id
                      AND correction_next.tenant_id = correction_app.tenant_id AND correction_next.deleted = 0
                      AND correction_next.id > correction_app.id)
                  AND NOT EXISTS (
                    SELECT 1 FROM mes_pro_edhr_nonconformance_review correction_ncr
                    WHERE correction_ncr.tenant_id = correction_app.tenant_id AND correction_ncr.deleted = 0
                      AND (correction_ncr.work_order_id = correction_app.work_order_id
                           OR correction_ncr.batch_execution_id = correction_batch.id
                           OR (correction_ncr.active_order_id = correction_order.id AND correction_ncr.source_type <> 'PQC_RELEASE'))
                      AND (correction_ncr.review_status = 'pending_review' OR correction_ncr.disposition = 'void'))
                  AND NOT EXISTS (
                    SELECT 1 FROM mes_pro_edhr_record_change_event correction_void
                    WHERE correction_void.tenant_id = correction_app.tenant_id AND correction_void.deleted = 0
                      AND correction_void.batch_execution_id = correction_batch.id
                      AND correction_void.target_scope = 'BATCH' AND correction_void.change_type = 'VOID'
                      AND correction_void.change_status IN ('DRAFT', 'SUBMITTED', 'APPROVED'))
              )
             ))
            """;
}
