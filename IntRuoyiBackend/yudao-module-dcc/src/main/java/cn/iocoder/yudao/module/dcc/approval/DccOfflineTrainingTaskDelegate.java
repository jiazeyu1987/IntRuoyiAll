package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.dcc.service.file.DccOfflineTrainingRecordService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Native work for a real ReceiveTask; never a second DCC provider or a fabricated UserTask. */
@Component
public class DccOfflineTrainingTaskDelegate {
    private final DccOfflineTrainingRecordService training;

    public DccOfflineTrainingTaskDelegate(DccOfflineTrainingRecordService training) {
        this.training = java.util.Objects.requireNonNull(training);
    }

    public List<ApprovalTaskSummary> list(ApprovalTaskQueryContext context) {
        if (context.getViewType() != ApprovalTaskViewType.TODO) return List.of();
        return training.listForActor(context.getLoginUserId()).stream().map(file -> {
            String source = DccOfflineTrainingRecordService.SOURCE_TYPE + ":" + file.getTenantId()
                    + ":" + file.getId() + ":" + file.getProcessInstanceId();
            return ApprovalTaskSummary.builder().id("DCC:" + source).moduleCode(ApprovalModuleCode.DCC)
                    .sourceTaskType(DccOfflineTrainingRecordService.SOURCE_TYPE).sourceTaskId(source)
                    .businessKey(file.getId().toString()).businessTitle(file.getTitle()).businessCode(file.getFileNumber())
                    .businessStatus(file.getStatus()).businessDeleted(false).processInstanceId(file.getProcessInstanceId())
                    .currentNodeCode("TRAINING").currentNodeName(trainingDutyName(context.getLoginUserId(),file))
                    .businessContextTags(List.of("版本：" + file.getVersionNo()))
                    .initiatorUserId(file.getRequesterId()).assigneeUserId(context.getLoginUserId())
                    .initiatedAt(file.getSubmittedTime()).taskCreatedAt(file.getApprovedTime())
                    .requiresSignature(false).availableActions(Set.of("PROCESS_IN_MODULE"))
                    .detailRoute("/dcc/controlled-file/detail/" + file.getId())
                    .detailQuery(DccOfflineTrainingRecordService.detailQuery(file.getProcessInstanceId())).build();
        }).filter(row -> matches(row, context.getKeyword())).toList();
    }

    private String trainingDutyName(Long actor, cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO file) {
        boolean uploads=training.canUpload(actor,file);
        boolean organizes=training.canOrganizeTraining(actor,file);
        if (uploads && organizes) return "组织培训并上传本次记录";
        if (uploads) return "上传本次线下培训记录";
        if (organizes) return "组织本轮文件培训";
        throw new IllegalStateException("培训待办缺少本次上传或组织职责");
    }

    private boolean matches(ApprovalTaskSummary row, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String expected = keyword.trim().toLowerCase(Locale.ROOT);
        return java.util.stream.Stream.of(row.getBusinessTitle(), row.getBusinessCode(), row.getBusinessKey())
                .anyMatch(value -> value != null && value.toLowerCase(Locale.ROOT).contains(expected));
    }
}
