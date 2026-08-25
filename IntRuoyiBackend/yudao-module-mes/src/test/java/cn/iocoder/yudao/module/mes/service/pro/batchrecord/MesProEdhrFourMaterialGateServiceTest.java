package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceSourcePrecheckRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseMaterialGateReceipt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProEdhrFourMaterialGateServiceTest {

    private static final Long BATCH_ID = 88L;
    private static final String SOURCE_HASH = "source-hash";

    @Mock private MesProEdhrBatchExecutionTaskMapper taskMapper;
    @Mock private MesProBatchRecordExecutionAttachmentMapper attachmentMapper;
    @Mock private MesProEdhrBatchTraceabilityService traceabilityService;
    @Mock private FileService fileService;
    @Mock private MesReleaseMaterialGateReceiptWriter receiptWriter;

    private MesProEdhrFourMaterialGateService gate;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        gate = new MesProEdhrFourMaterialGateServiceImpl(taskMapper, attachmentMapper,
                traceabilityService, fileService, receiptWriter);
        lenient().when(traceabilityService.resolveSourcePrecheck(any()))
                .thenReturn(new MesProEdhrBatchTraceSourcePrecheckRespVO()
                        .setBatchExecutionId(BATCH_ID).setOriginLinkId(9L)
                        .setTraceLinkHash("trace-hash").setSourceSnapshotHash(SOURCE_HASH)
                        .setSourceVersion(1).setRelationStatus("CAPTURED")
                .setReadAt(LocalDateTime.now()));
        lenient().when(receiptWriter.persistReady(any(), any(), any(), any(), any()))
                .thenReturn(new MesReleaseMaterialGateReceipt()
                        .setReceiptId("MATERIALS-88-1").setBatchExecutionId(BATCH_ID)
                        .setGateStatus(MesReleaseMaterialGateReceipt.STATUS_MATERIALS_READY)
                        .setMaterialTypeKeys(Set.copyOf(MesReleaseMaterialGateReceipt.REQUIRED_MATERIAL_TYPES))
                        .setManifestHash("manifest-hash").setSourceSnapshotHash(SOURCE_HASH)
                        .setMaterialVersionSetHash("version-hash").setReceiptHash("receipt-hash")
                        .setIssuedBy(1001L).setAuditEventId("FLOW8-MATERIALS-88-1").setVersion(1));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void missingMaterialBlocks() {
        when(taskMapper.selectListByBatchExecutionId(BATCH_ID)).thenReturn(List.of(task("INCOMING_INSPECTION_REPORT", 1L)));
        when(attachmentMapper.selectListByBatchExecutionId(BATCH_ID)).thenReturn(new ArrayList<>());
        MesProEdhrFourMaterialGateResult result = gate.evaluate(BATCH_ID);
        assertEquals(MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_PENDING, result.status());
        assertFalse(result.ready());
        verify(traceabilityService).resolveSourcePrecheck(any());
        verifyNoInteractions(fileService);
    }

    @Test
    void missingBatchIdFailsFast() {
        ServiceException error = assertThrows(ServiceException.class, () -> gate.evaluate(null));
        assertEquals(MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS.getCode(),
                error.getCode());
        verifyNoInteractions(traceabilityService, taskMapper, attachmentMapper, fileService);
    }

    @Test
    void fourCurrentMaterialsReturnReady() {
        List<MesProEdhrBatchExecutionTaskDO> tasks = new ArrayList<>();
        List<MesProBatchRecordExecutionAttachmentDO> attachments = new ArrayList<>();
        long id = 1L;
        for (String node : MesProEdhrFourMaterialGateService.REQUIRED_MATERIAL_TYPES) {
            tasks.add(task(node, id));
            attachments.add(attachment(node, id, id));
            id++;
        }
        when(taskMapper.selectListByBatchExecutionId(BATCH_ID)).thenReturn(tasks);
        when(attachmentMapper.selectListByBatchExecutionId(BATCH_ID)).thenReturn(attachments);
        when(fileService.getFile(any())).thenAnswer(invocation -> FileDO.builder().id(invocation.getArgument(0))
                .configId(3L).name("report.pdf").path("/report.pdf").url("https://files/report.pdf")
                .type("application/pdf").size(10L).build());
        MesProEdhrFourMaterialGateResult result = gate.evaluate(BATCH_ID);
        assertTrue(result.ready());
        assertEquals(MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_READY, result.status());
        assertEquals(4, result.materials().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "VOID"})
    void latestInvalidActionDoesNotFallbackToOlderAttachment(String latestAction) {
        List<MesProEdhrBatchExecutionTaskDO> tasks = new ArrayList<>();
        List<MesProBatchRecordExecutionAttachmentDO> attachments = new ArrayList<>();
        long id = 1L;
        for (String node : MesProEdhrFourMaterialGateService.REQUIRED_MATERIAL_TYPES) {
            tasks.add(task(node, id));
            attachments.add(attachment(node, id, 1L));
            id++;
        }
        attachments.add(attachment("INCOMING_INSPECTION_REPORT", 1L, 2L).setAttachmentAction(latestAction));
        when(taskMapper.selectListByBatchExecutionId(BATCH_ID)).thenReturn(tasks);
        when(attachmentMapper.selectListByBatchExecutionId(BATCH_ID)).thenReturn(List.of(
                attachments.toArray(MesProBatchRecordExecutionAttachmentDO[]::new)));
        MesProEdhrFourMaterialGateResult result = gate.evaluate(BATCH_ID);
        assertEquals(MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_RECHECK_REQUIRED, result.status());
        assertFalse(result.ready());
        verify(fileService, never()).getFile(2L);
    }

    @Test
    void sourceMappingIsResolvedBeforeMaterialQueries() {
        when(traceabilityService.resolveSourcePrecheck(any())).thenReturn(null);
        ServiceException error = assertThrows(ServiceException.class, () -> gate.evaluate(BATCH_ID));
        assertEquals(1_040_760_401, error.getCode());
        assertTrue(error.getMessage().contains(MesProEdhrBatchTraceabilityBlocker.TRACE_MAPPING_BLOCKED));
        verifyNoInteractions(taskMapper, attachmentMapper, fileService);
    }

    @Test
    void sourceSnapshotChangeProducesNewManifest() {
        List<MesProEdhrBatchExecutionTaskDO> tasks = new ArrayList<>();
        List<MesProBatchRecordExecutionAttachmentDO> attachments = new ArrayList<>();
        long id = 1L;
        for (String node : MesProEdhrFourMaterialGateService.REQUIRED_MATERIAL_TYPES) {
            tasks.add(task(node, id));
            attachments.add(attachment(node, id, id));
            id++;
        }
        when(taskMapper.selectListByBatchExecutionId(BATCH_ID)).thenReturn(tasks);
        when(attachmentMapper.selectListByBatchExecutionId(BATCH_ID)).thenReturn(attachments);
        when(fileService.getFile(any())).thenAnswer(invocation -> FileDO.builder().id(invocation.getArgument(0))
                .configId(3L).name("report.pdf").path("/report.pdf").url("https://files/report.pdf")
                .type("application/pdf").size(10L).build());

        String firstManifest = gate.evaluate(BATCH_ID).manifestHash();
        when(traceabilityService.resolveSourcePrecheck(any()))
                .thenReturn(new MesProEdhrBatchTraceSourcePrecheckRespVO()
                        .setBatchExecutionId(BATCH_ID).setOriginLinkId(9L)
                        .setTraceLinkHash("trace-hash-v2").setSourceSnapshotHash("source-hash-v2")
                        .setSourceVersion(2).setRelationStatus("CAPTURED")
                        .setReadAt(LocalDateTime.now()));

        MesProEdhrFourMaterialGateResult changed = gate.evaluate(BATCH_ID);
        assertEquals(MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_RECHECK_REQUIRED, changed.status());
        assertFalse(changed.ready());
        assertNotEquals(firstManifest, changed.manifestHash());
        assertEquals(MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_RECHECK_REQUIRED, gate.evaluate(BATCH_ID).status());
        tasks.forEach(task -> task.setRouteBindingSnapshotHash("source-hash-v2"));
        assertEquals(MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_READY, gate.evaluate(BATCH_ID).status());
    }

    @Test
    void finishedProductReportCannotReplaceFinishedProductRecord() {
        List<MesProEdhrBatchExecutionTaskDO> tasks = List.of(
                task("INCOMING_INSPECTION_REPORT", 1L),
                task("STERILIZATION_REPORT", 2L),
                task("FINISHED_PRODUCT_INSPECTION_REPORT", 3L));
        when(taskMapper.selectListByBatchExecutionId(BATCH_ID)).thenReturn(tasks);
        when(attachmentMapper.selectListByBatchExecutionId(BATCH_ID)).thenReturn(List.of(
                attachment("INCOMING_INSPECTION_REPORT", 1L, 1L),
                attachment("STERILIZATION_REPORT", 2L, 1L),
                attachment("FINISHED_PRODUCT_INSPECTION_REPORT", 3L, 1L)));

        MesProEdhrFourMaterialGateResult result = gate.evaluate(BATCH_ID);
        assertEquals(MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_PENDING, result.status());
        assertFalse(result.ready());
    }

    @Test
    void attachmentHashTamperRequiresRecheck() {
        List<MesProEdhrBatchExecutionTaskDO> tasks = new ArrayList<>();
        List<MesProBatchRecordExecutionAttachmentDO> attachments = new ArrayList<>();
        long id = 1L;
        for (String node : MesProEdhrFourMaterialGateService.REQUIRED_MATERIAL_TYPES) {
            tasks.add(task(node, id));
            attachments.add(attachment(node, id, id));
            id++;
        }
        attachments.get(0).setAttachmentHash("tampered");
        when(taskMapper.selectListByBatchExecutionId(BATCH_ID)).thenReturn(tasks);
        when(attachmentMapper.selectListByBatchExecutionId(BATCH_ID)).thenReturn(attachments);
        MesProEdhrFourMaterialGateResult result = gate.evaluate(BATCH_ID);
        assertEquals(MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_RECHECK_REQUIRED, result.status());
        assertFalse(result.ready());
    }

    private MesProEdhrBatchExecutionTaskDO task(String node, Long id) {
        return MesProEdhrBatchExecutionTaskDO.builder().id(id).batchExecutionId(BATCH_ID)
                .nodeType(node).status(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)
                .routeBindingSnapshotHash(SOURCE_HASH).build();
    }

    private MesProBatchRecordExecutionAttachmentDO attachment(String node, Long taskId, Long version) {
        MesProBatchRecordExecutionAttachmentDO attachment = MesProBatchRecordExecutionAttachmentDO.builder()
                .id(version * 10 + taskId).executionId(0L).batchExecutionId(BATCH_ID)
                .batchTaskId(taskId).fieldKey(node).fieldPath("SPECIAL_NODE:" + node)
                .attachmentType("FILE").attachmentGroupKey(node).attachmentAction("ADD")
                .versionNo(version.intValue()).fileId(version)
                .fileUrl("https://files/report.pdf").storageConfigId(3L).storagePath("/report.pdf")
                .fileName("report.pdf").contentType("application/pdf").fileSize(10L)
                .sha256("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .storageRetentionJson("{}").operatorId(7L).operatorName("tester")
                .reasonCategory("SPECIAL_NODE_ATTACHMENT").reasonText("test")
                .operatedAt(LocalDateTime.of(2026, 8, 24, 1, 0)).build();
        attachment.setStorageRetentionHash(MesProEdhrSpecialNodeAttachmentHasher.retentionHash("{}"));
        attachment.setAttachmentHash(MesProEdhrSpecialNodeAttachmentHasher.attachmentHash(attachment));
        return attachment;
    }
}
