package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummaryEnricher;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileProcessDefinitionKeys;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class DccObsoleteApprovalSummaryEnricher implements ApprovalTaskSummaryEnricher {
    private final FormActionInstanceMapper forms;
    private final DccControlledFileMapper files;
    private final DccApprovalTaskAdapter summaries;

    public DccObsoleteApprovalSummaryEnricher(FormActionInstanceMapper forms, DccControlledFileMapper files,
            DccApprovalTaskAdapter summaries) {
        this.forms = forms; this.files = files; this.summaries = summaries;
    }

    @Override
    public void enrich(ApprovalTaskSummary summary, String definitionKey) {
        if (definitionKey == null || !DccControlledFileProcessDefinitionKeys.FORM_CENTER_OBSOLETE_KEYS.contains(definitionKey)) return;
        Long tenant = TenantContextHolder.getRequiredTenantId();
        var rows = forms.selectOwnershipByTenantAndProcessId(tenant, summary.getProcessInstanceId());
        if (rows == null || rows.size() != 1) throw new IllegalStateException("作废流程必须唯一关联正式作废申请");
        var form = rows.get(0);
        if (!Objects.equals(form.getTenantId(), tenant) || !Objects.equals(form.getBpmProcessInstanceId(), summary.getProcessInstanceId())
                || !"DCC".equals(form.getSystemCode()) || !"CONTROLLED_FILE".equals(form.getObjectType())
                || !"OBSOLETE".equals(form.getActionCode()) || form.getObjectId() == null || !form.getObjectId().matches("[1-9][0-9]*"))
            throw new IllegalStateException("作废流程与正式申请身份不一致");
        var file = files.selectById(Long.valueOf(form.getObjectId()));
        if (file == null || !Objects.equals(file.getTenantId(), tenant) || !Objects.equals(file.getId().toString(), form.getObjectId())
                || !Objects.equals(file.getVersionNo(), form.getObjectVersion()) || file.getTitle() == null || file.getFileNumber() == null)
            throw new IllegalStateException("作废申请引用的正式文件版本不存在或不一致");
        summary.setBusinessTitle(file.getTitle());
        summary.setBusinessCode(file.getFileNumber());
        summary.setBusinessContextTags(summaries.buildDccBusinessContextTags(file, summary.getCurrentNodeName()));
        summary.setDecisionDetailRoute(summary.getDetailRoute());
        summary.setDecisionDetailQuery(Map.copyOf(summary.getDetailQuery()));
        summary.setAvailableActions(Set.of("OPEN_DETAIL"));
    }
    @Override
    public void enrichFilterFacts(java.util.Map<String, String> facts, FilterContext context) {
        DccNativeApprovalFilterSql.enrich(facts, context, true);
    }

}
