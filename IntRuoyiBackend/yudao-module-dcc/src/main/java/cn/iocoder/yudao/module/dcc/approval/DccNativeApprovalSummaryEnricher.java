package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummaryEnricher;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileProcessDefinitionKeys;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class DccNativeApprovalSummaryEnricher implements ApprovalTaskSummaryEnricher {
    private final DccControlledFileMapper files;
    private final DccApprovalTaskAdapter summaries;

    public DccNativeApprovalSummaryEnricher(DccControlledFileMapper files, DccApprovalTaskAdapter summaries) {
        this.files = files;
        this.summaries = summaries;
    }

    @Override
    public void enrich(ApprovalTaskSummary summary, String definitionKey) {
        // Task views already use the DCC adapter; copy records use a process id as their business key.
        if (!"BPM_PROCESS_INSTANCE".equals(summary.getSourceTaskType()) || definitionKey == null
                || !DccControlledFileProcessDefinitionKeys.NATIVE_FINALIZATION_KEYS.contains(definitionKey)) return;
        String businessKey = summary.getBusinessKey();
        Long fileId;
        try {
            fileId = Long.valueOf(businessKey);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("文控流程缺少有效的文件版本编号", ex);
        }
        if (fileId <= 0) throw new IllegalStateException("文控流程缺少有效的文件版本编号");
        // The process business key identifies its exact file version, including earlier application rounds.
        var file = files.selectByIdIncludingDeleted(fileId);
        if (file == null || !Objects.equals(file.getId(), fileId)
                || !Objects.equals(file.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || file.getTitle() == null || file.getTitle().isBlank()
                || file.getFileNumber() == null || file.getFileNumber().isBlank()) {
            throw new IllegalStateException("文控流程引用的本租户文件版本不存在或信息不完整");
        }
        summary.setBusinessTitle(file.getTitle());
        summary.setBusinessCode(file.getFileNumber());
        summary.setBusinessContextTags(summaries.buildDccBusinessContextTags(file, summary.getCurrentNodeName()));
        summary.setBusinessDeleted(Boolean.TRUE.equals(file.getDeleted()));
    }
}
