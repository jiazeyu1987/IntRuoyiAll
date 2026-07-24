package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureRecordResult;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureRecordService;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessApprovalOrchestratorSignatureRequiredTest {

    @Test
    void submitSignatureRequiredPolicyValidatesPasswordRecordsSignatureAndExecutesDomainAction() {
        RecordingExecutor executor = new RecordingExecutor("MES_ROUTE_VERSION_PUBLISH");
        InMemoryBusinessApprovalRequestStore store = new InMemoryBusinessApprovalRequestStore();
        RecordingBpmStarter bpmStarter = new RecordingBpmStarter("should-not-start");
        RecordingAdminUserApi adminUserApi = new RecordingAdminUserApi();
        RecordingSignatureRecordService signatureRecordService = new RecordingSignatureRecordService();
        BusinessApprovalOrchestrator orchestrator = new BusinessApprovalOrchestrator(
                new BusinessApprovalPolicyResolveService(List.of(
                        BusinessApprovalPolicyResolveServiceTest.basePolicy()
                                .mode(BusinessApprovalPolicyMode.SIGNATURE_REQUIRED)
                                .processDefinitionKey(null)
                                .build())),
                new BusinessApprovalEffectExecutorRegistry(List.of(executor)),
                store,
                bpmStarter,
                adminUserApi,
                signatureRecordService);

        BusinessApprovalRequest request = orchestrator.submit(
                BusinessApprovalPolicyResolveServiceTest.baseContext().build(),
                "signature-pass");

        assertEquals(BusinessApprovalRequestStatus.DIRECT_EXECUTED, request.getStatus());
        assertEquals(1, executor.getDirectExecutions());
        assertEquals(0, executor.getPendingMarks());
        assertEquals(0, bpmStarter.getStartCount());
        assertEquals(501L, adminUserApi.validatedUserId);
        assertEquals("signature-pass", adminUserApi.validatedPassword);
        assertEquals(ApprovalModuleCode.BPM, signatureRecordService.lastContext.getModuleCode());
        assertEquals("BUSINESS_APPROVAL_ROUTE_VERSION_PUBLISH",
                signatureRecordService.lastContext.getSourceTaskType());
        assertEquals(String.valueOf(request.getRequestId()), signatureRecordService.lastContext.getSourceTaskId());
        assertEquals("ROUTE_VERSION:1001:PUBLISH", signatureRecordService.lastContext.getBusinessKey());
        assertEquals(ApprovalTaskReviewResult.APPROVE, signatureRecordService.lastContext.getResult());
        assertEquals("publish route version", signatureRecordService.lastContext.getReason());
        assertEquals("signature-pass", signatureRecordService.lastContext.getSignaturePassword());
    }

    @Test
    void submitSignatureRequiredPolicyFailsBeforeDomainActionWhenPasswordMissing() {
        RecordingExecutor executor = new RecordingExecutor("MES_ROUTE_VERSION_PUBLISH");
        BusinessApprovalOrchestrator orchestrator = new BusinessApprovalOrchestrator(
                new BusinessApprovalPolicyResolveService(List.of(
                        BusinessApprovalPolicyResolveServiceTest.basePolicy()
                                .mode(BusinessApprovalPolicyMode.SIGNATURE_REQUIRED)
                                .processDefinitionKey(null)
                                .build())),
                new BusinessApprovalEffectExecutorRegistry(List.of(executor)),
                new InMemoryBusinessApprovalRequestStore(),
                new RecordingBpmStarter("should-not-start"),
                new RecordingAdminUserApi(),
                new RecordingSignatureRecordService());

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> orchestrator.submit(BusinessApprovalPolicyResolveServiceTest.baseContext().build(), " "));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_SIGNATURE_PASSWORD_REQUIRED,
                ex.getErrorCode());
        assertEquals(0, executor.getDirectExecutions());
        assertEquals(0, executor.getPendingMarks());
    }

    private static final class RecordingAdminUserApi implements AdminUserApi {

        private Long validatedUserId;
        private String validatedPassword;

        @Override
        public AdminUserRespDTO getUser(Long id) {
            return null;
        }

        @Override
        public List<AdminUserRespDTO> getUserListBySubordinate(Long id) {
            return List.of();
        }

        @Override
        public List<AdminUserRespDTO> getUserList(Collection<Long> ids) {
            return List.of();
        }

        @Override
        public List<AdminUserRespDTO> getUserListByDeptIds(Collection<Long> deptIds) {
            return List.of();
        }

        @Override
        public List<AdminUserRespDTO> getUserListByPostIds(Collection<Long> postIds) {
            return List.of();
        }

        @Override
        public void validateUserList(Collection<Long> ids) {
            // Not needed by this test.
        }

        @Override
        public void validatePassword(Long id, String rawPassword) {
            validatedUserId = id;
            validatedPassword = rawPassword;
        }
    }

    private static final class RecordingSignatureRecordService implements ApprovalSignatureRecordService {

        private ApprovalTaskReviewContext lastContext;

        @Override
        public ApprovalSignatureRecordResult recordReviewSignature(ApprovalTaskReviewContext context) {
            assertTrue(context.getSignaturePassword() == null
                    || !context.getSignaturePassword().isBlank());
            lastContext = context;
            return ApprovalSignatureRecordResult.builder()
                    .recordId(9001L)
                    .signatureImageId(8001L)
                    .signatureImageFileUrl("http://signature.local/signature.png")
                    .build();
        }
    }
}
