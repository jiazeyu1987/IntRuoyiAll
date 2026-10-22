package cn.iocoder.yudao.module.bpm.approval.service.signature;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureResult;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalSignatureRecordServiceImplTest {
    @Test
    void fullOpinionSurvivesActualKernelMapperCapacityAndPrefixUniquenessContract() {
        var rows = org.mockito.Mockito.mock(cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper.class);
        var kernel = new cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl();
        var auth = org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);
        var audit = org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class);
        var images = org.mockito.Mockito.mock(ApprovalSignatureImageSnapshotProvider.class);
        org.springframework.test.util.ReflectionTestUtils.setField(kernel, "signatureRecordMapper", rows);
        org.springframework.test.util.ReflectionTestUtils.setField(kernel, "adminUserApi", auth);
        org.springframework.test.util.ReflectionTestUtils.setField(kernel, "gxpAuditService", audit);
        org.springframework.test.util.ReflectionTestUtils.setField(kernel, "subjectAdapters", java.util.List.of(new BpmApprovalSignatureSubjectAdapter()));
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), ""),
                cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO.class);
        var persisted = new java.util.LinkedHashMap<String, cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO>();
        when(rows.selectOne(org.mockito.ArgumentMatchers.any())).thenAnswer(call -> {
            var wrapper = (com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<?>) call.getArgument(0);
            wrapper.getSqlSegment();
            return wrapper.getParamNameValuePairs().values().stream().filter(String.class::isInstance)
                    .map(String.class::cast).map(persisted::get).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        });
        when(rows.insert(org.mockito.ArgumentMatchers.any(cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO.class)))
                .thenAnswer(call -> {
                    cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO row = call.getArgument(0);
                    // system_electronic_signature_record: varchar(512), unique tenant/key(191)/deleted.
                    assertTrue(row.getIdempotencyKey().length() <= 512);
                    assertTrue(row.getIdempotencyKey().length() <= 191);
                    assertTrue(persisted.values().stream().noneMatch(existing -> existing.getIdempotencyKey()
                            .substring(0, Math.min(191, existing.getIdempotencyKey().length())).equals(row.getIdempotencyKey())));
                    row.setId(820000L + persisted.size()); persisted.put(row.getIdempotencyKey(), row); return 1;
                });
        when(images.requireActiveSnapshot(100L)).thenReturn(signatureImageSnapshot(), signatureImageSnapshot(),
                signatureImageSnapshot(), signatureImageSnapshot(3), signatureImageSnapshot());
        when(images.requireActiveSnapshot(101L)).thenReturn(signatureImageSnapshot());
        var service = new ApprovalSignatureRecordServiceImpl(kernel, images);
        String opinion = "证".repeat(499) + "甲";
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
        try (var security = org.mockito.Mockito.mockStatic(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.class)) {
            security.when(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);
            var first = service.recordReviewSignature(longOpinionContext(100L, "9001", "9001", ApprovalTaskReviewResult.APPROVE, opinion));
            var replay = service.recordReviewSignature(longOpinionContext(100L, "9001", "9001", ApprovalTaskReviewResult.APPROVE, "  " + opinion + "  "));
            assertEquals(first.getUnifiedSignatureId(), replay.getUnifiedSignatureId());
            service.recordReviewSignature(longOpinionContext(100L, "9001", "9001", ApprovalTaskReviewResult.APPROVE, "证".repeat(499) + "乙"));
            service.recordReviewSignature(longOpinionContext(100L, "9001", "9001", ApprovalTaskReviewResult.APPROVE, opinion));
            service.recordReviewSignature(longOpinionContext(100L, "9001", "9001", ApprovalTaskReviewResult.REJECT, opinion));
            security.when(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            service.recordReviewSignature(longOpinionContext(101L, "9001", "9001", ApprovalTaskReviewResult.APPROVE, opinion));
            assertEquals(5, persisted.size());
            for (var row : persisted.values()) {
                assertEquals(500, row.getReason().length());
                assertEquals(row.getReason(), JsonUtils.parseTree(row.getCanonicalContentJson()).path("reason").asText());
                assertEquals(cn.hutool.crypto.digest.DigestUtil.sha256Hex(row.getCanonicalContentJson()), row.getContentHash());
            }
            verify(rows, org.mockito.Mockito.times(5)).insert(org.mockito.ArgumentMatchers.any(
                    cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO.class));
        } finally {
            cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
        }
    }

    @Mock
    private ElectronicSignatureService electronicSignatureService;
    @Mock
    private ApprovalSignatureImageSnapshotProvider signatureImageSnapshotProvider;

    @Test
    void recordReviewSignatureDelegatesToUnifiedKernelForNativeBpmReviewSignature() {
        ApprovalSignatureRecordServiceImpl service = newService();
        when(signatureImageSnapshotProvider.requireActiveSnapshot(100L)).thenReturn(signatureImageSnapshot());
        when(electronicSignatureService.sign(org.mockito.ArgumentMatchers.any())).thenReturn(signatureResult(810001L));

        ApprovalSignatureRecordResult result = service.recordReviewSignature(ApprovalTaskReviewContext.of(100L,
                ApprovalModuleCode.BPM, "BPM_TASK_TODO", "task-100", "pi-100", "pi-100",
                ApprovalTaskReviewResult.APPROVE, "reviewed production evidence", "secret", false));

        ElectronicSignatureCommand command = captureSignatureCommand();
        assertEquals("BPM", command.moduleCode());
        assertEquals("APPROVE", command.actionCode());
        assertEquals("BPM_APPROVAL_TASK", command.subjectType());
        assertEquals(command.expectedSubjectVersion(),
                BpmApprovalSignatureSubjectAdapter.subjectVersion(command.subjectId()));
        assertEquals("secret", command.credential());
        assertEquals("reviewed production evidence", command.reason());
        assertTrue(command.idempotencyKey().matches("BPM_APPROVAL_REVIEW_V1\\|[0-9a-f]{64}"));
        assertEquals(810001L, result.getRecordId());
        assertEquals(810001L, result.getUnifiedSignatureId());
        assertEquals(command.subjectId(), result.getSubjectId());
        assertEquals(signatureResult(810001L).evidenceHash(), result.getEvidenceHash());
        assertEquals(9101L, result.getSignatureImageId());
        assertEquals("/admin-api/infra/file/28/get/dcc/signature-images/signature.png",
                result.getSignatureImageFileUrl());
        verify(signatureImageSnapshotProvider).markReferenced(9101L);
    }

    @Test
    void recordReviewSignatureDelegatesToUnifiedKernelForMesFeedbackReviewSignature() {
        ApprovalSignatureRecordServiceImpl service = newService();
        when(signatureImageSnapshotProvider.requireActiveSnapshot(101L)).thenReturn(signatureImageSnapshot());
        when(electronicSignatureService.sign(org.mockito.ArgumentMatchers.any())).thenReturn(signatureResult(810002L));

        ApprovalSignatureRecordResult result = service.recordReviewSignature(ApprovalTaskReviewContext.of(101L,
                ApprovalModuleCode.MES_FEEDBACK, "MES_PRO_FEEDBACK", "9001", "9001", "pi-9001",
                ApprovalTaskReviewResult.REJECT, "quality data missing", "secret", false));

        ElectronicSignatureCommand command = captureSignatureCommand();
        assertEquals("BPM", command.moduleCode());
        assertEquals("REJECT", command.actionCode());
        assertEquals("BPM_APPROVAL_TASK", command.subjectType());
        assertEquals("quality data missing", command.reason());
        assertTrue(command.idempotencyKey().matches("BPM_APPROVAL_REVIEW_V1\\|[0-9a-f]{64}"));
        assertEquals(810002L, result.getUnifiedSignatureId());
        verify(signatureImageSnapshotProvider).markReferenced(9101L);
    }

    @ParameterizedTest
    @MethodSource("blankReviewReasons")
    void recordReviewSignatureRejectsBlankReasonBeforeImageAndSignatureAccess(
            ApprovalTaskReviewResult result, String reason) {
        ApprovalTaskReviewContext context = ApprovalTaskReviewContext.of(100L,
                ApprovalModuleCode.EDHR, "EDHR_WORK_TASK", "9001", "9001", null,
                result, reason, "test-credential", false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> newService().recordReviewSignature(context));

        assertEquals("APPROVAL_REVIEW_REASON_REQUIRED", ex.getMessage());
        verifyNoInteractions(signatureImageSnapshotProvider, electronicSignatureService);
    }

    @ParameterizedTest
    @EnumSource(ApprovalTaskReviewResult.class)
    void recordReviewSignaturePreservesOriginalMultilineReasonInKernelCommand(ApprovalTaskReviewResult result) {
        String originalReason = "\u00a0\uFEFF\u2003  production evidence checked\n  user \"explanation\" \\ kept\tsecond line\r\nlast line  \r\n\u2003\u202f";
        String normalizedReason = "production evidence checked\n  user \"explanation\" \\ kept\tsecond line\r\nlast line";
        when(signatureImageSnapshotProvider.requireActiveSnapshot(100L)).thenReturn(signatureImageSnapshot());
        when(electronicSignatureService.sign(org.mockito.ArgumentMatchers.any())).thenReturn(signatureResult(810004L));

        newService().recordReviewSignature(ApprovalTaskReviewContext.of(100L,
                ApprovalModuleCode.EDHR, "EDHR_WORK_TASK", "9001", "9001", null,
                result, originalReason, "test-credential", false));

        ElectronicSignatureCommand command = captureSignatureCommand();
        assertEquals(normalizedReason, command.reason());
        assertTrue(command.idempotencyKey().length() <= 191);
        SignatureSubjectSnapshot snapshot = new BpmApprovalSignatureSubjectAdapter().loadAndAuthorize(
                new SignatureSubjectCommand(100L, command.moduleCode(), command.actionCode(), command.subjectType(),
                        command.subjectId(), command.expectedSubjectVersion(), command.reason()));
        Map<?, ?> canonicalContent = JsonUtils.parseObject(snapshot.canonicalContentJson(), Map.class);
        assertEquals(normalizedReason, canonicalContent.get("reason"));
        assertEquals("EDHR", canonicalContent.get("moduleCode"));
        assertEquals("EDHR_WORK_TASK", canonicalContent.get("sourceTaskType"));
        assertEquals("9001", canonicalContent.get("sourceTaskId"));
        assertEquals("", canonicalContent.get("processInstanceId"));
        assertEquals(result.name(), canonicalContent.get("reviewResult"));
        verify(signatureImageSnapshotProvider).markReferenced(9101L);
    }

    private static Stream<Arguments> blankReviewReasons() {
        return Stream.of(ApprovalTaskReviewResult.values()).flatMap(result ->
                Stream.of(null, "", "  ", " \t\r\n ", "\u2003", "\u2003 \t\r\n\u2003", "\000", "\u2003\000 ",
                                "\u00a0", "\u202f\u2007", "\uFEFF")
                        .map(reason -> Arguments.of(result, reason)));
    }

    @Test
    void recordReviewSignatureRejectsMissingProcessInstanceForOrderedApprovalEvidence() {
        ApprovalSignatureRecordServiceImpl service = newService();

        assertThrows(NullPointerException.class, () -> service.recordReviewSignature(ApprovalTaskReviewContext.of(101L,
                ApprovalModuleCode.BPM, "BPM_TASK_TODO", "task-100", "pi-100", null,
                ApprovalTaskReviewResult.APPROVE, "approved", "secret", false)));

        verify(signatureImageSnapshotProvider, never()).requireActiveSnapshot(101L);
        verify(electronicSignatureService, never()).sign(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void recordReviewSignatureAllowsSignatureRequiredDirectBusinessApprovalWithoutProcessInstance() {
        ApprovalSignatureRecordServiceImpl service = newService();
        when(signatureImageSnapshotProvider.requireActiveSnapshot(102L)).thenReturn(signatureImageSnapshot());
        when(electronicSignatureService.sign(org.mockito.ArgumentMatchers.any())).thenReturn(signatureResult(810003L));

        ApprovalSignatureRecordResult result = service.recordReviewSignature(ApprovalTaskReviewContext.of(102L,
                ApprovalModuleCode.BPM, "BUSINESS_APPROVAL_POLICY_SWITCH", "policy-100", "MES:POLICY",
                null, ApprovalTaskReviewResult.APPROVE, "SIGNATURE_REQUIRED", "secret", false));

        ElectronicSignatureCommand command = captureSignatureCommand();
        assertEquals(810003L, result.getUnifiedSignatureId());
        assertTrue(command.idempotencyKey().matches("BPM_APPROVAL_REVIEW_V1\\|[0-9a-f]{64}"));
        verify(signatureImageSnapshotProvider).markReferenced(9101L);
    }

    @Test
    void adapterBuildsDeterministicContentSnapshotFromServerSideReviewContext() {
        ApprovalTaskReviewContext context = ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.BPM,
                "BPM_TASK_TODO", "task-100", "pi-100", "pi-100",
                ApprovalTaskReviewResult.APPROVE, "approved", "secret", false);
        String subjectId = BpmApprovalSignatureSubjectAdapter.encodeSubjectId(context, signatureImageSnapshot());
        BpmApprovalSignatureSubjectAdapter adapter = new BpmApprovalSignatureSubjectAdapter();

        SignatureSubjectSnapshot snapshot = adapter.loadAndAuthorize(new SignatureSubjectCommand(100L,
                "BPM", "APPROVE", "BPM_APPROVAL_TASK", subjectId,
                BpmApprovalSignatureSubjectAdapter.subjectVersion(subjectId), "approved"));

        assertEquals("BPM_APPROVAL_TASK", snapshot.subjectType());
        assertEquals(subjectId, snapshot.subjectId());
        assertEquals(BpmApprovalSignatureSubjectAdapter.subjectVersion(subjectId), snapshot.subjectVersion());
        assertTrue(snapshot.canonicalContentJson().contains("\"moduleCode\":\"BPM\""));
        assertTrue(snapshot.canonicalContentJson().contains("\"sourceTaskType\":\"BPM_TASK_TODO\""));
        assertTrue(snapshot.canonicalContentJson().contains("\"sourceTaskId\":\"task-100\""));
        assertTrue(snapshot.canonicalContentJson().contains("\"reviewResult\":\"APPROVE\""));
        assertTrue(snapshot.canonicalContentJson().contains("\"signatureImagePayloadHash\":\""));
        String expectedJson = "{\"adapter\":\"BPM_APPROVAL\",\"moduleCode\":\"BPM\","
                + "\"sourceTaskType\":\"BPM_TASK_TODO\",\"sourceTaskId\":\"task-100\","
                + "\"businessKey\":\"pi-100\",\"processInstanceId\":\"pi-100\","
                + "\"reviewResult\":\"APPROVE\",\"reason\":\"approved\","
                + "\"signatureImagePayloadHash\":\"" + ApprovalSignatureRecordServiceImpl.sha256(
                        ApprovalSignatureRecordServiceImpl.signatureImagePayload(signatureImageSnapshot())) + "\"}";
        assertEquals(expectedJson, snapshot.canonicalContentJson());
        assertEquals("pi-100", snapshot.processInstanceId());
        assertEquals("task-100", snapshot.taskId());
        assertEquals("BPM_TASK_TODO", snapshot.nodeCode());
        assertFalse(snapshot.canonicalContentJson().contains("secret"));
    }

    private ApprovalSignatureRecordServiceImpl newService() {
        return new ApprovalSignatureRecordServiceImpl(electronicSignatureService, signatureImageSnapshotProvider);
    }

    @Test
    void completeFiveHundredCharacterOpinionHasBoundedStableAndIsolatedProductionIdentity() {
        var image = signatureImageSnapshot();
        var nextImage = signatureImageSnapshot(3);
        when(signatureImageSnapshotProvider.requireActiveSnapshot(org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(image, image, image, nextImage, image, image, image, image);
        when(electronicSignatureService.sign(org.mockito.ArgumentMatchers.any())).thenReturn(signatureResult(810005L));
        String opinion = "证".repeat(499) + "甲";
        var service = newService();
        service.recordReviewSignature(longOpinionContext(100L, "9001", "9001", ApprovalTaskReviewResult.APPROVE, opinion));
        service.recordReviewSignature(longOpinionContext(100L, "9001", "9001", ApprovalTaskReviewResult.APPROVE, "  " + opinion + "  "));
        service.recordReviewSignature(longOpinionContext(100L, "9001", "9001", ApprovalTaskReviewResult.APPROVE, "证".repeat(499) + "乙"));
        service.recordReviewSignature(longOpinionContext(100L, "9001", "9001", ApprovalTaskReviewResult.APPROVE, opinion));
        service.recordReviewSignature(longOpinionContext(101L, "9001", "9001", ApprovalTaskReviewResult.APPROVE, opinion));
        service.recordReviewSignature(longOpinionContext(100L, "9001", "9001", ApprovalTaskReviewResult.REJECT, opinion));
        service.recordReviewSignature(longOpinionContext(100L, "a|b", "c", ApprovalTaskReviewResult.APPROVE, opinion));
        service.recordReviewSignature(longOpinionContext(100L, "a", "b|c", ApprovalTaskReviewResult.APPROVE, opinion));
        var captor = ArgumentCaptor.forClass(ElectronicSignatureCommand.class);
        verify(electronicSignatureService, org.mockito.Mockito.times(8)).sign(captor.capture());
        var commands = captor.getAllValues();
        assertEquals(commands.get(0).idempotencyKey(), commands.get(1).idempotencyKey());
        for (int index = 2; index < 6; index++) {
            assertNotEquals(commands.get(0).idempotencyKey(), commands.get(index).idempotencyKey());
        }
        assertNotEquals(commands.get(6).idempotencyKey(), commands.get(7).idempotencyKey());
        for (var command : commands) {
            assertTrue(command.idempotencyKey().length() <= 191);
            assertEquals(500, command.reason().length());
            var content = new BpmApprovalSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(
                    100L, command.moduleCode(), command.actionCode(), command.subjectType(), command.subjectId(),
                    command.expectedSubjectVersion(), command.reason()));
            assertEquals(command.reason(), JsonUtils.parseTree(content.canonicalContentJson()).path("reason").asText());
        }
    }

    private static ApprovalTaskReviewContext longOpinionContext(Long actor, String task, String business,
            ApprovalTaskReviewResult action, String opinion) {
        return ApprovalTaskReviewContext.of(actor, ApprovalModuleCode.EDHR, "EDHR_WORK_TASK", task,
                business, null, action, opinion, "test-credential", false);
    }

    private ElectronicSignatureCommand captureSignatureCommand() {
        ArgumentCaptor<ElectronicSignatureCommand> captor =
                ArgumentCaptor.forClass(ElectronicSignatureCommand.class);
        verify(electronicSignatureService).sign(captor.capture());
        return captor.getValue();
    }

    private static ElectronicSignatureResult signatureResult(Long id) {
        return new ElectronicSignatureResult(id, "VALID", LocalDateTime.now(), "SERVER_CLOCK:now",
                "version", "contentHash", "evidenceHash", "SHA-256", "system-local-v1");
    }

    private static ApprovalSignatureImageSnapshot signatureImageSnapshot() {
        return signatureImageSnapshot(2);
    }

    private static ApprovalSignatureImageSnapshot signatureImageSnapshot(int version) {
        return ApprovalSignatureImageSnapshot.builder()
                .imageId(9101L)
                .versionNo(version)
                .fileId(8101L)
                .fileUrl("/admin-api/infra/file/28/get/dcc/signature-images/signature.png")
                .sha256("87b335f7e9429e37ff0df4c0c966681a86932139eade14bf1957d1fda2a19430")
                .contentType("image/png")
                .fileSize(2048L)
                .imageStatus("ACTIVE")
                .verifiedStatus("VALID")
                .build();
    }

}
