package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.system.service.gxpaudit.GxpCreationAuditVerificationService.VerifiedAuditEventFact;
import org.springframework.stereotype.Component;
import java.util.*;

/** A closed MES protocol set; policy registration cannot silently expand creation semantics. */
@Component
public class MesEdhrSourceCreationProtocolRegistry {
    public enum Kind { MATERIAL, RELATED_SUBMISSION, RELATED_HANDOFF, COMPLETION_MAIN, COMPLETION_PART }
    private record Rule(String target, String operation, String locator, String subject, String action, Kind kind) {}
    private static final String BATCH = "cn.iocoder.yudao.module.mes.service.pro.batchrecord.";
    private static final String TEAM = "cn.iocoder.yudao.module.mes.service.pro.processpool.team.";
    private static final List<Rule> RULES = List.of(
            new Rule("EDHR_MATERIAL_GATE_RECEIPT", "mes.edhr.material-gate.receipt.create",
                    BATCH + "MesReleaseMaterialGateReceiptPortImpl#persistReady", "EDHR_MATERIAL_GATE_RECEIPT", "CREATE", Kind.MATERIAL),
            new Rule("EDHR_MATERIAL_GATE_RECEIPT", "mes.market-release.submit",
                    BATCH + "MesProEdhrReleaseServiceImpl#submit", "RELEASE_TRANSACTION", "SUBMIT", Kind.RELATED_SUBMISSION),
            new Rule("EDHR_MATERIAL_GATE_RECEIPT", "mes.market-release.submit",
                    BATCH + "MesProEdhrReleaseServiceImpl#submitForApproval", "RELEASE_TRANSACTION", "SUBMIT", Kind.RELATED_SUBMISSION),
            new Rule("EDHR_MATERIAL_GATE_RECEIPT", "mes.market-release.manager-stage.initialize",
                    "cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerStageInitializerImpl#initializeManagerReleaseStage",
                    "RELEASE_TRANSACTION", "INITIALIZE_MANAGER_STAGE", Kind.RELATED_HANDOFF),
            new Rule("COMPLETION_RECEIPT", "mes.active-order.complete",
                    TEAM + "MesTeamLeaderActiveOrderCompletionServiceImpl#complete", "ACTIVE_ORDER_COMPLETION", "COMPLETE", Kind.COMPLETION_MAIN),
            new Rule("COMPLETION_RECEIPT", "mes.active-order.complete",
                    TEAM + "MesCompletionAffectedRowAudit#append", "ACTIVE_ORDER_COMPLETION", "COMPLETE", Kind.COMPLETION_PART));

    public Kind classify(String target, VerifiedAuditEventFact fact) {
        var c = fact.columns();
        return RULES.stream().filter(r -> r.target().equals(target) && r.operation().equals(c.operationId())
                && r.locator().equals(c.sourceLocator()) && r.subject().equals(c.subjectType())
                && r.action().equals(c.action()) && "MES".equals(c.domain()) && "SERVICE_METHOD".equals(c.sourceType()))
                .map(Rule::kind).findFirst().orElseThrow(() -> new IllegalStateException("UNKNOWN_CANDIDATE_PROTOCOL"));
    }
}
