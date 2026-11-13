package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogPageReqVO;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.hutool.core.util.StrUtil;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProBatchRecordExecutionFieldAuditBatchMapper
        extends BaseMapperX<MesProBatchRecordExecutionFieldAuditBatchDO> {
    default LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditBatchDO> buildFormFillLogPageQuery(MesProEdhrFormFillLogPageReqVO reqVO) {
        LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditBatchDO> query =
                new LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditBatchDO>()
                        .eqIfPresent(MesProBatchRecordExecutionFieldAuditBatchDO::getActorId, reqVO.getActorId())
                        .likeIfPresent(MesProBatchRecordExecutionFieldAuditBatchDO::getActorName, reqVO.getActorName())
                        .betweenIfPresent(MesProBatchRecordExecutionFieldAuditBatchDO::getChangedAt,
                                reqVO.getChangedAtStart(), reqVO.getChangedAtEnd())
                        .orderByDesc(MesProBatchRecordExecutionFieldAuditBatchDO::getChangedAt)
                        .orderByDesc(MesProBatchRecordExecutionFieldAuditBatchDO::getId);
        if (hasExecutionFilter(reqVO)) {
            String execution = "SELECT 1 FROM mes_pro_batch_record_execution e WHERE e.id = mes_pro_batch_record_execution_field_audit_batch.execution_id AND e.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND e.deleted = 0";
            if (cn.hutool.core.util.StrUtil.isNotEmpty(reqVO.getBatchRecordReportId())) query.apply("EXISTS (" + execution + " AND e.batch_record_report_id = {0})", reqVO.getBatchRecordReportId());
            if (cn.hutool.core.util.StrUtil.isNotEmpty(reqVO.getExecutionCode())) query.apply("EXISTS (" + execution + " AND e.execution_code LIKE {0})", "%" + reqVO.getExecutionCode() + "%");
            if (cn.hutool.core.util.StrUtil.isNotEmpty(reqVO.getBatchCode())) query.apply("EXISTS (" + execution + " AND e.batch_code LIKE {0})", "%" + reqVO.getBatchCode() + "%");
            if (cn.hutool.core.util.StrUtil.isNotEmpty(reqVO.getWorkOrderCode())) query.apply("EXISTS (" + execution + " AND e.work_order_code LIKE {0})", "%" + reqVO.getWorkOrderCode() + "%");
            if (cn.hutool.core.util.StrUtil.isNotBlank(reqVO.getFormKeyword())) {
                String keyword = "%" + reqVO.getFormKeyword().trim() + "%";
                query.apply("EXISTS (" + execution + " AND (e.template_name LIKE {0} OR e.batch_record_report_id LIKE {0}))", keyword);
            }
        }
        ListFilterQueryUtils.apply(reqVO, query, java.util.Map.ofEntries(
                java.util.Map.entry("formName", text("COALESCE(NULLIF((SELECT t.batch_record_report_name FROM mes_pro_edhr_batch_execution_task t WHERE t.execution_id = mes_pro_batch_record_execution_field_audit_batch.execution_id AND t.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND t.deleted = 0 ORDER BY t.id DESC LIMIT 1),''),NULLIF((SELECT e.template_name FROM mes_pro_batch_record_execution e WHERE e.id = mes_pro_batch_record_execution_field_audit_batch.execution_id AND e.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND e.deleted = 0),''),NULLIF((SELECT t.batch_record_report_id FROM mes_pro_edhr_batch_execution_task t WHERE t.execution_id = mes_pro_batch_record_execution_field_audit_batch.execution_id AND t.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND t.deleted = 0 ORDER BY t.id DESC LIMIT 1),''),(SELECT e.batch_record_report_id FROM mes_pro_batch_record_execution e WHERE e.id = mes_pro_batch_record_execution_field_audit_batch.execution_id AND e.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND e.deleted = 0))")),
                java.util.Map.entry("batchCode", text("CONCAT(COALESCE((SELECT e.batch_code FROM mes_pro_batch_record_execution e WHERE e.id = mes_pro_batch_record_execution_field_audit_batch.execution_id AND e.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND e.deleted = 0),''),' ',COALESCE(NULLIF((SELECT e.batch_code FROM mes_pro_batch_record_execution e WHERE e.id = mes_pro_batch_record_execution_field_audit_batch.execution_id AND e.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND e.deleted = 0),''),'--'),' ',CASE WHEN (SELECT e.id FROM mes_pro_batch_record_execution e WHERE e.id = mes_pro_batch_record_execution_field_audit_batch.execution_id AND e.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND e.deleted = 0) IS NULL THEN '执行上下文缺失' WHEN (SELECT t.batch_execution_id FROM mes_pro_edhr_batch_execution_task t WHERE t.execution_id = mes_pro_batch_record_execution_field_audit_batch.execution_id AND t.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND t.deleted = 0 ORDER BY t.id DESC LIMIT 1) IS NULL THEN '批次上下文缺失' ELSE '证据完整' END)")),
                java.util.Map.entry("workOrderCode", text("CONCAT(COALESCE((SELECT e.work_order_code FROM mes_pro_batch_record_execution e WHERE e.id = mes_pro_batch_record_execution_field_audit_batch.execution_id AND e.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND e.deleted = 0),''),' ',COALESCE(NULLIF((SELECT e.work_order_code FROM mes_pro_batch_record_execution e WHERE e.id = mes_pro_batch_record_execution_field_audit_batch.execution_id AND e.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND e.deleted = 0),''),'--'),' ',CASE WHEN (SELECT e.id FROM mes_pro_batch_record_execution e WHERE e.id = mes_pro_batch_record_execution_field_audit_batch.execution_id AND e.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND e.deleted = 0) IS NULL THEN '执行上下文缺失' WHEN (SELECT t.batch_execution_id FROM mes_pro_edhr_batch_execution_task t WHERE t.execution_id = mes_pro_batch_record_execution_field_audit_batch.execution_id AND t.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND t.deleted = 0 ORDER BY t.id DESC LIMIT 1) IS NULL THEN '批次上下文缺失' ELSE '证据完整' END)")),
                java.util.Map.entry("executionCode", text("(SELECT e.execution_code FROM mes_pro_batch_record_execution e WHERE e.id = mes_pro_batch_record_execution_field_audit_batch.execution_id AND e.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND e.deleted = 0)")),
                java.util.Map.entry("actorName", text("actor_name")),
                java.util.Map.entry("changedAt", dateTime("changed_at")),
                java.util.Map.entry("fieldCount", number("field_count")),
                java.util.Map.entry("cellSummary", text("CONCAT_WS('；',(SELECT CONCAT(COALESCE(NULLIF(i.field_label,''),i.field_key,''),'=', CASE WHEN i.recordbook_value_display IS NOT NULL OR i.batch_record_value_display IS NOT NULL THEN CONCAT('记录本 ',COALESCE(i.recordbook_value_display,''),' / 批记录 ',COALESCE(i.batch_record_value_display,'')) ELSE COALESCE(i.new_value_display,'') END) FROM mes_pro_batch_record_execution_field_audit_item i WHERE i.audit_batch_id = mes_pro_batch_record_execution_field_audit_batch.id AND i.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND i.deleted = 0 ORDER BY i.batch_item_index IS NULL,i.batch_item_index,i.id LIMIT 0,1),(SELECT CONCAT(COALESCE(NULLIF(i.field_label,''),i.field_key,''),'=', CASE WHEN i.recordbook_value_display IS NOT NULL OR i.batch_record_value_display IS NOT NULL THEN CONCAT('记录本 ',COALESCE(i.recordbook_value_display,''),' / 批记录 ',COALESCE(i.batch_record_value_display,'')) ELSE COALESCE(i.new_value_display,'') END) FROM mes_pro_batch_record_execution_field_audit_item i WHERE i.audit_batch_id = mes_pro_batch_record_execution_field_audit_batch.id AND i.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND i.deleted = 0 ORDER BY i.batch_item_index IS NULL,i.batch_item_index,i.id LIMIT 1,1),(SELECT CONCAT(COALESCE(NULLIF(i.field_label,''),i.field_key,''),'=', CASE WHEN i.recordbook_value_display IS NOT NULL OR i.batch_record_value_display IS NOT NULL THEN CONCAT('记录本 ',COALESCE(i.recordbook_value_display,''),' / 批记录 ',COALESCE(i.batch_record_value_display,'')) ELSE COALESCE(i.new_value_display,'') END) FROM mes_pro_batch_record_execution_field_audit_item i WHERE i.audit_batch_id = mes_pro_batch_record_execution_field_audit_batch.id AND i.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND i.deleted = 0 ORDER BY i.batch_item_index IS NULL,i.batch_item_index,i.id LIMIT 2,1))")),
                java.util.Map.entry("contextStatus", text("CASE WHEN (SELECT e.id FROM mes_pro_batch_record_execution e WHERE e.id = mes_pro_batch_record_execution_field_audit_batch.execution_id AND e.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND e.deleted = 0) IS NULL THEN '执行上下文缺失' WHEN (SELECT t.batch_execution_id FROM mes_pro_edhr_batch_execution_task t WHERE t.execution_id = mes_pro_batch_record_execution_field_audit_batch.execution_id AND t.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND t.deleted = 0 ORDER BY t.id DESC LIMIT 1) IS NULL THEN '批次上下文缺失' ELSE '证据完整' END")),
                java.util.Map.entry("evidenceStatus", text("CASE WHEN (SELECT e.id FROM mes_pro_batch_record_execution e WHERE e.id = mes_pro_batch_record_execution_field_audit_batch.execution_id AND e.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND e.deleted = 0) IS NULL THEN '执行上下文缺失' WHEN (SELECT t.batch_execution_id FROM mes_pro_edhr_batch_execution_task t WHERE t.execution_id = mes_pro_batch_record_execution_field_audit_batch.execution_id AND t.tenant_id = mes_pro_batch_record_execution_field_audit_batch.tenant_id AND t.deleted = 0 ORDER BY t.id DESC LIMIT 1) IS NULL THEN '批次上下文缺失' ELSE '证据完整' END"))));
        return query;
    }

    private static boolean hasExecutionFilter(MesProEdhrFormFillLogPageReqVO reqVO) {
        return StrUtil.isNotBlank(reqVO.getBatchRecordReportId())
                || StrUtil.isNotBlank(reqVO.getFormKeyword())
                || StrUtil.isNotBlank(reqVO.getExecutionCode())
                || StrUtil.isNotBlank(reqVO.getBatchCode())
                || StrUtil.isNotBlank(reqVO.getWorkOrderCode());
    }



    default MesProBatchRecordExecutionFieldAuditBatchDO selectByIdempotencyKey(Long tenantId, Long executionId,
                                                                               String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditBatchDO>()
                .eq(MesProBatchRecordExecutionFieldAuditBatchDO::getTenantId, tenantId)
                .eq(MesProBatchRecordExecutionFieldAuditBatchDO::getExecutionId, executionId)
                .eq(MesProBatchRecordExecutionFieldAuditBatchDO::getIdempotencyKey, idempotencyKey));
    }

    default List<MesProBatchRecordExecutionFieldAuditBatchDO> selectListByExecutionId(Long executionId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditBatchDO>()
                .eq(MesProBatchRecordExecutionFieldAuditBatchDO::getExecutionId, executionId)
                .orderByAsc(MesProBatchRecordExecutionFieldAuditBatchDO::getAfterFieldAuditRevision)
                .orderByAsc(MesProBatchRecordExecutionFieldAuditBatchDO::getId));
    }
}
