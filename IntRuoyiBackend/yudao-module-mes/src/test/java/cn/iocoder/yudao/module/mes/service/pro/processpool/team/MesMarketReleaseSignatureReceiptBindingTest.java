package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalReviewReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Receipt verification belongs to ApprovalCenterReceiptService; these tests cover the MES association. */
class MesMarketReleaseSignatureReceiptBindingTest {
    private final MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO();
    private final MesProEdhrReleaseTransactionDO transaction = new MesProEdhrReleaseTransactionDO();
    private final ElectronicSignatureEvidenceDTO evidence = mock(ElectronicSignatureEvidenceDTO.class);

    @BeforeEach void prepare() {
        TenantContextHolder.setTenantId(1L);
        task.setId(2837L);
        transaction.setId(274L);
        transaction.setApprovalOpinion("测试上市放行");
        when(evidence.id()).thenReturn(62169L);
        when(evidence.actorId()).thenReturn(9L);
        when(evidence.evidenceHash()).thenReturn("signed-evidence-hash");
        when(evidence.subjectVersion()).thenReturn("locked-dossier-version");
        when(evidence.subjectId()).thenReturn("EDHR_RELEASE:274");
        when(evidence.taskId()).thenReturn("2837");
    }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void currentCenterReceiptBindsToReleasedTransactionAndTask() {
        assertDoesNotThrow(() -> bind(receipt()));
        assertThrows(RuntimeException.class, () -> bind(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"tenantId", "actorId", "centerSignatureId", "centerEvidenceHash", "subjectVersion",
            "actionCode", "resultCode", "resultingStatus", "reason"})
    void anotherReceiptOrDecisionCannotBeShownAsThisMarketRelease(String field) {
        var receipt = receipt();
        Object value = field.endsWith("Id") ? 999L : "foreign";
        ReflectionTestUtils.setField(receipt, field, value);
        assertThrows(RuntimeException.class, () -> bind(receipt), field);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ownerCode", "sourceTaskType", "sourceTaskId", "domainSubjectType", "domainSubjectId"})
    void centerIdentityMustReferToThisEdhrTaskAndTransaction(String field) {
        var receipt = receipt();
        var identity = (com.fasterxml.jackson.databind.node.ObjectNode)
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(receipt.getIdentityJson());
        identity.put(field, "foreign");
        receipt.setIdentityJson(identity.toString());
        assertThrows(RuntimeException.class, () -> bind(receipt), field);
    }

    @Test void signatureSubjectTaskAndDomainLinkMustAllMatch() {
        when(evidence.subjectId()).thenReturn("EDHR_RELEASE:275");
        assertThrows(RuntimeException.class, () -> bind(receipt()));
        when(evidence.subjectId()).thenReturn("EDHR_RELEASE:274");
        when(evidence.taskId()).thenReturn("2838");
        assertThrows(RuntimeException.class, () -> bind(receipt()));
        when(evidence.taskId()).thenReturn("2837");
        var receipt = receipt();
        receipt.setDomainLinksJson("{\"releaseId\":\"275\"}");
        assertThrows(RuntimeException.class, () -> bind(receipt));
    }

    private void bind(BpmApprovalReviewReceiptDO receipt) {
        MesActiveOrderSignatureEvidenceService.requireCenterMarketReleaseBinding(task, transaction, receipt, evidence);
    }
    private BpmApprovalReviewReceiptDO receipt() {
        var receipt = new BpmApprovalReviewReceiptDO();
        receipt.setTenantId(1L); receipt.setActorId(9L); receipt.setCenterSignatureId(62169L);
        receipt.setCenterEvidenceHash("signed-evidence-hash"); receipt.setSubjectVersion("locked-dossier-version");
        receipt.setActionCode("APPROVE"); receipt.setResultCode("APPROVE"); receipt.setResultingStatus("RELEASED");
        receipt.setReason("测试上市放行");
        receipt.setIdentityJson("{\"ownerCode\":\"EDHR\",\"sourceTaskType\":\"EDHR_WORK_TASK\",\"sourceTaskId\":\"2837\",\"domainSubjectType\":\"EDHR_RELEASE\",\"domainSubjectId\":\"274\"}");
        receipt.setDomainLinksJson("{\"releaseId\":\"274\"}");
        return receipt;
    }
}
