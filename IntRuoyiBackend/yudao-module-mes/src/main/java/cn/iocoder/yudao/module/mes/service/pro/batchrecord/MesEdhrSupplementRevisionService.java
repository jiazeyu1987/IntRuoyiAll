package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordChangeEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Supplement drafts retain the original signed values, signatures and archive without rewriting them. */
@Service
public class MesEdhrSupplementRevisionService {
    @Resource private MesProBatchRecordExecutionMapper executions;
    @Resource private MesProEdhrBatchExecutionTaskMapper batchTasks;

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public MesProBatchRecordExecutionDO create(MesProEdhrRecordChangeEventDO event, MesProBatchRecordExecutionDO source) {
        if (source.getSupersededByExecutionId() != null || Boolean.FALSE.equals(source.getActiveRevisionFlag())) {
            throw new IllegalStateException("原执行记录已产生后续修订，不能再次从历史版本补录");
        }
        var revision = MesProBatchRecordExecutionDO.builder()
                .executionCode("BRE-SUPPLEMENT-" + event.getId()).templateId(source.getTemplateId())
                .templateCode(source.getTemplateCode()).templateName(source.getTemplateName())
                .workOrderId(source.getWorkOrderId()).workOrderCode(source.getWorkOrderCode())
                .routeProcessId(source.getRouteProcessId()).taskId(source.getTaskId()).workstationId(source.getWorkstationId())
                .batchRecordReportId(source.getBatchRecordReportId()).batchRecordDefinitionId(source.getBatchRecordDefinitionId())
                .batchRecordVersionId(source.getBatchRecordVersionId()).batchExecutionId(source.getBatchExecutionId())
                .routeId(source.getRouteId()).instanceScope(source.getInstanceScope()).sharedFormKey(source.getSharedFormKey())
                .formSlotType(source.getFormSlotType()).recordCategory(source.getRecordCategory())
                .validationProfile(source.getValidationProfile()).recordbookEnabled(source.getRecordbookEnabled())
                .permissionScopeId(source.getPermissionScopeId()).routeBindingId(source.getRouteBindingId())
                .routeBindingSnapshotHash(source.getRouteBindingSnapshotHash()).archiveVisibility(source.getArchiveVisibility())
                .slotConfigSnapshotHash(source.getSlotConfigSnapshotHash()).batchCode(source.getBatchCode())
                .status(0).sheetLayoutJson(source.getSheetLayoutJson()).metaJson(source.getMetaJson())
                .executionSnapshotJson(source.getExecutionSnapshotJson()).cellValuesJson(source.getCellValuesJson())
                .cellValuesHash(source.getCellValuesHash()).fieldAuditRevision(0L)
                .fieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                .revisionRootExecutionId(source.getRevisionRootExecutionId() == null ? source.getId() : source.getRevisionRootExecutionId())
                .revisionNo(source.getRevisionNo() == null ? 2 : source.getRevisionNo() + 1)
                .revisionReason(event.getReasonText()).revisionParentHash(source.getFieldAuditHeadHash())
                .activeRevisionFlag(true).supplementFlag(true).supplementSourceExecutionId(source.getId())
                .supplementReason(event.getReasonText()).domainTraceSnapshotId(source.getDomainTraceSnapshotId())
                .domainTraceHash(source.getDomainTraceHash()).domainTraceStatus(source.getDomainTraceStatus())
                .domainTraceVerifiedAt(source.getDomainTraceVerifiedAt()).build();
        String activeKey = source.getActiveContextKey();
        if (activeKey != null) executions.clearActiveContextKey(source.getId());
        revision.setActiveContextKey(activeKey);
        if (executions.insert(revision) != 1 || revision.getId() == null) throw new IllegalStateException("补录修订草稿保存失败");
        executions.updateById(new MesProBatchRecordExecutionDO().setId(source.getId()).setActiveRevisionFlag(false)
                .setSupersededByExecutionId(revision.getId()));
        var bindings = batchTasks.selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionTaskDO>()
                .eq(MesProEdhrBatchExecutionTaskDO::getExecutionId, source.getId()));
        for (var binding : bindings) {
            if (batchTasks.updateById(new MesProEdhrBatchExecutionTaskDO().setId(binding.getId()).setExecutionId(revision.getId())
                    .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_REWORK_REQUIRED)) != 1) {
                throw new IllegalStateException("补录修订的正式批记录绑定更新失败");
            }
        }
        return revision;
    }
}
