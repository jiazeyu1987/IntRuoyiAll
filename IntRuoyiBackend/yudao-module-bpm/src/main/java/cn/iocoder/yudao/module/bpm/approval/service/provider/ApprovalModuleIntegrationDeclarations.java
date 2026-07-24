package cn.iocoder.yudao.module.bpm.approval.service.provider;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Code-owned source of truth for modules that must be on the unified approval platform.
 */
@Component
public class ApprovalModuleIntegrationDeclarations {

    private final List<ApprovalModuleIntegrationDeclaration> requiredProviders;

    public ApprovalModuleIntegrationDeclarations() {
        this(List.of(
                ApprovalModuleIntegrationDeclaration.required(ApprovalModuleCode.BPM, "BPM 原生审批",
                        "bpm-native-approval",
                        Set.of(ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE,
                                ApprovalTaskViewType.MY_INITIATED, ApprovalTaskViewType.CC),
                        Set.of(ApprovalTaskCapability.NOTIFICATION, ApprovalTaskCapability.AUDIT),
                        "/bpm/process-instance/detail"),
                ApprovalModuleIntegrationDeclaration.required(ApprovalModuleCode.DCC, "DCC 文控审批",
                        "dcc-controlled-file-approval",
                        Set.of(ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE),
                        Set.of(ApprovalTaskCapability.TIMELINE, ApprovalTaskCapability.NOTIFICATION,
                                ApprovalTaskCapability.AUDIT, ApprovalTaskCapability.SIGNATURE_AUTHORIZATION,
                                ApprovalTaskCapability.EVIDENCE_LEDGER),
                        "/dcc/controlled-file/detail"),
                ApprovalModuleIntegrationDeclaration.required(ApprovalModuleCode.EDHR, "eDHR 审批",
                        "edhr-work-task-approval",
                        Set.of(ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE),
                        Set.of(ApprovalTaskCapability.TIMELINE, ApprovalTaskCapability.AUDIT,
                                ApprovalTaskCapability.SIGNATURE_AUTHORIZATION,
                                ApprovalTaskCapability.EVIDENCE_LEDGER),
                        "/mes/pro/edhr-work-task"),
                ApprovalModuleIntegrationDeclaration.required(ApprovalModuleCode.SHOWROOM, "Showroom 审批",
                        "showroom-approval",
                        Set.of(ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE,
                                ApprovalTaskViewType.MY_INITIATED),
                        Set.of(ApprovalTaskCapability.TIMELINE, ApprovalTaskCapability.NOTIFICATION,
                                ApprovalTaskCapability.AUDIT, ApprovalTaskCapability.SIGNATURE_AUTHORIZATION,
                                ApprovalTaskCapability.EVIDENCE_LEDGER),
                        "/showroom/approval"),
                ApprovalModuleIntegrationDeclaration.required(ApprovalModuleCode.SRM, "SRM 供应商门户审核",
                        "srm-supplier-portal-approval",
                        Set.of(ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE,
                                ApprovalTaskViewType.MY_INITIATED),
                        Set.of(ApprovalTaskCapability.TIMELINE, ApprovalTaskCapability.AUDIT),
                        "/srm/supplier-portal-review"),
                ApprovalModuleIntegrationDeclaration.required(ApprovalModuleCode.MES_FEEDBACK, "MES 报工审批",
                        "mes-feedback-approval",
                        Set.of(ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE,
                                ApprovalTaskViewType.MY_INITIATED),
                        Set.of(ApprovalTaskCapability.TIMELINE, ApprovalTaskCapability.AUDIT),
                        "/mes/pro/feedback")
        ));
    }

    ApprovalModuleIntegrationDeclarations(List<ApprovalModuleIntegrationDeclaration> requiredProviders) {
        this.requiredProviders = List.copyOf(requiredProviders);
    }

    public List<ApprovalModuleIntegrationDeclaration> listRequiredProviders() {
        return requiredProviders;
    }
}
