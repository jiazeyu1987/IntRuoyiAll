package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTraceDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTracePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTracePageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTraceVerifyReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordDomainTraceItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordDomainTraceSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordDomainTraceItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordDomainTraceSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordDomainTraceErrorCodeConstants.PRO_BATCH_RECORD_DOMAIN_TRACE_BLOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordDomainTraceErrorCodeConstants.PRO_BATCH_RECORD_DOMAIN_TRACE_HASH_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordDomainTraceErrorCodeConstants.PRO_BATCH_RECORD_DOMAIN_TRACE_PERSIST_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProBatchRecordDomainTraceServiceTest {

    @Mock
    private MesProBatchRecordExecutionMapper executionMapper;
    @Mock
    private MesProBatchRecordDomainTraceSnapshotMapper snapshotMapper;
    @Mock
    private MesProBatchRecordDomainTraceItemMapper itemMapper;
    @Mock
    private MesProBatchRecordExecutionAttachmentMapper attachmentMapper;
    @Mock
    private MesProBatchRecordExecutionAttachmentService attachmentService;

    @InjectMocks
    private MesProBatchRecordDomainTraceServiceImpl domainTraceService;

    @BeforeEach
    void setUp() {
        lenient().when(attachmentService.verifyAttachmentChain(anyLong()))
                .thenReturn(MesProBatchRecordExecutionAttachmentChainVerifyResult.builder()
                        .valid(true)
                        .checkedEventCount(0)
                        .headHash(null)
                        .build());
        lenient().when(attachmentMapper.selectListByExecutionId(anyLong())).thenReturn(List.of());
    }

    @Test
    void getTracePage_existingSnapshotReturnsItemCountFromPersistedItemsAndBlockerCountFromSnapshot() {
        LocalDateTime verifiedAt = LocalDateTime.of(2026, 5, 28, 20, 50);
        MesProBatchRecordExecutionDO execution = baseExecution()
                .setDomainTraceSnapshotId(11L)
                .setDomainTraceHash("hash-11")
                .setDomainTraceStatus("BLOCKED")
                .setDomainTraceVerifiedAt(verifiedAt);
        MesProBatchRecordExecutionDO secondExecution = baseExecution()
                .setId(2L)
                .setExecutionCode("BRE-002")
                .setDomainTraceSnapshotId(22L)
                .setDomainTraceHash("hash-22")
                .setDomainTraceStatus("VERIFIED")
                .setDomainTraceVerifiedAt(verifiedAt);
        when(executionMapper.selectDomainTracePage(any(MesProBatchRecordDomainTracePageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(execution, secondExecution), 2L));
        when(snapshotMapper.selectListByExecutionIds(eq(List.of(1L, 2L)))).thenReturn(List.of(
                MesProBatchRecordDomainTraceSnapshotDO.builder()
                        .id(11L)
                        .executionId(1L)
                        .snapshotVersion("EDHR_DOMAIN_TRACE_V1")
                        .snapshotHash("hash-11")
                        .completenessStatus("BLOCKED")
                        .blockerCount(1)
                        .verifiedAt(verifiedAt)
                        .build(),
                MesProBatchRecordDomainTraceSnapshotDO.builder()
                        .id(22L)
                        .executionId(2L)
                        .snapshotVersion("EDHR_DOMAIN_TRACE_V1")
                        .snapshotHash("hash-22")
                        .completenessStatus("VERIFIED")
                        .blockerCount(0)
                        .verifiedAt(verifiedAt)
                        .build()));
        when(itemMapper.selectListBySnapshotIds(eq(List.of(11L, 22L)))).thenReturn(List.of(
                MesProBatchRecordDomainTraceItemDO.builder().snapshotId(11L).build(),
                MesProBatchRecordDomainTraceItemDO.builder().snapshotId(11L).build(),
                MesProBatchRecordDomainTraceItemDO.builder().snapshotId(22L).build()));

        PageResult<MesProBatchRecordDomainTracePageRespVO> page =
                domainTraceService.getTracePage(new MesProBatchRecordDomainTracePageReqVO());

        assertEquals(2L, page.getTotal());
        assertEquals(2, page.getList().size());
        MesProBatchRecordDomainTracePageRespVO row = page.getList().get(0);
        assertEquals(1L, row.getExecutionId());
        assertEquals("BLOCKED", row.getStatus());
        assertEquals("hash-11", row.getDomainTraceHash());
        assertEquals(verifiedAt, row.getVerifiedAt());
        assertEquals(1, row.getBlockerCount());
        assertEquals(2, row.getItemCount());
        MesProBatchRecordDomainTracePageRespVO secondRow = page.getList().get(1);
        assertEquals(2L, secondRow.getExecutionId());
        assertEquals("VERIFIED", secondRow.getStatus());
        assertEquals("hash-22", secondRow.getDomainTraceHash());
        assertEquals(0, secondRow.getBlockerCount());
        assertEquals(1, secondRow.getItemCount());
        verify(snapshotMapper, never()).selectLatestByExecutionId(any());
    }

    @Test
    void getTracePage_withoutSnapshotKeepsItemCountNullAndDoesNotFakeZeroEvidence() {
        MesProBatchRecordExecutionDO execution = baseExecution();
        when(executionMapper.selectDomainTracePage(any(MesProBatchRecordDomainTracePageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(execution), 1L));

        PageResult<MesProBatchRecordDomainTracePageRespVO> page =
                domainTraceService.getTracePage(new MesProBatchRecordDomainTracePageReqVO());

        MesProBatchRecordDomainTracePageRespVO row = page.getList().get(0);
        assertEquals("BLOCKED", row.getStatus());
        assertNull(row.getBlockerCount());
        assertNull(row.getItemCount());
        verify(snapshotMapper, never()).selectLatestByExecutionId(1L);
        verify(snapshotMapper, never()).selectListByExecutionIds(any());
        verify(itemMapper, never()).selectListBySnapshotIds(any());
    }

    @Test
    void getTraceDetail_latestSnapshot_returnsBlockersAndItemsWithoutRecomputingSuccess() {
        LocalDateTime verifiedAt = LocalDateTime.of(2026, 5, 28, 9, 30);
        when(executionMapper.selectById(1L)).thenReturn(baseExecution()
                .setDomainTraceSnapshotId(11L)
                .setDomainTraceHash("hash-11")
                .setDomainTraceStatus("BLOCKED")
                .setDomainTraceVerifiedAt(verifiedAt));
        when(snapshotMapper.selectLatestByExecutionId(1L)).thenReturn(MesProBatchRecordDomainTraceSnapshotDO.builder()
                .id(11L)
                .executionId(1L)
                .snapshotVersion("EDHR_DOMAIN_TRACE_V1")
                .snapshotHash("hash-11")
                .completenessStatus("BLOCKED")
                .blockerCount(1)
                .verifiedAt(verifiedAt)
                .build());
        when(itemMapper.selectListBySnapshotId(11L)).thenReturn(List.of(
                MesProBatchRecordDomainTraceItemDO.builder()
                        .snapshotId(11L)
                        .executionId(1L)
                        .itemType("WORK_ORDER")
                        .itemKey("workOrderId")
                        .itemName("Work order")
                        .sourceId(20L)
                        .sourceCode("MO-001")
                        .sourceVersion("v1")
                        .snapshotJson("{\"workOrderId\":20}")
                        .snapshotHash("item-hash-ok")
                        .status("VERIFIED")
                        .build(),
                MesProBatchRecordDomainTraceItemDO.builder()
                        .snapshotId(11L)
                        .executionId(1L)
                        .itemType("MATERIAL_LOT")
                        .itemKey("materialLot")
                        .itemName("Material lot lineage")
                        .status("BLOCKED")
                        .blockerCode("EDHR_DOMAIN_TRACE_MATERIAL_LOT_REQUIRED")
                        .blockerMessage("Material lot lineage is required")
                        .blockerReason("Material lot lineage is required")
                        .build()));

        MesProBatchRecordDomainTraceDetailRespVO detail = domainTraceService.getTraceDetail(1L);

        assertEquals(1L, detail.getExecutionId());
        assertEquals("BRE-001", detail.getExecutionCode());
        assertEquals("BLOCKED", detail.getStatus());
        assertEquals(11L, detail.getDomainTraceSnapshotId());
        assertEquals("hash-11", detail.getDomainTraceHash());
        assertEquals(verifiedAt, detail.getVerifiedAt());
        assertEquals(2, detail.getItems().size());
        assertEquals(1, detail.getBlockers().size());
        assertEquals("MATERIAL_LOT", detail.getBlockers().get(0).getItemType());
        assertEquals("EDHR_DOMAIN_TRACE_MATERIAL_LOT_REQUIRED", detail.getBlockers().get(0).getBlockerCode());
    }

    @Test
    void getTraceDetail_includesAttachmentSummariesAndChainStatus() {
        LocalDateTime verifiedAt = LocalDateTime.of(2026, 6, 12, 17, 30);
        when(executionMapper.selectById(1L)).thenReturn(baseExecution()
                .setDomainTraceSnapshotId(11L)
                .setDomainTraceHash("hash-11")
                .setDomainTraceStatus("VERIFIED")
                .setDomainTraceVerifiedAt(verifiedAt));
        when(snapshotMapper.selectLatestByExecutionId(1L)).thenReturn(MesProBatchRecordDomainTraceSnapshotDO.builder()
                .id(11L)
                .executionId(1L)
                .snapshotVersion("EDHR_DOMAIN_TRACE_V1")
                .snapshotHash("hash-11")
                .completenessStatus("VERIFIED")
                .blockerCount(0)
                .verifiedAt(verifiedAt)
                .build());
        when(itemMapper.selectListBySnapshotId(11L)).thenReturn(List.of());
        when(attachmentService.verifyAttachmentChain(1L))
                .thenReturn(MesProBatchRecordExecutionAttachmentChainVerifyResult.builder()
                        .valid(true)
                        .checkedEventCount(1)
                        .headHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                        .build());
        when(attachmentMapper.selectListByExecutionId(1L)).thenReturn(List.of(MesProBatchRecordExecutionAttachmentDO.builder()
                .id(1001L)
                .executionId(1L)
                .workTaskId(31L)
                .rowIndex(1)
                .columnIndex(2)
                .fieldKey("visualEvidence")
                .fieldPath("sheet.main.rows[1].cells[2]")
                .fieldLabel("现场图片")
                .attachmentType("IMAGE")
                .attachmentGroupKey("R1C2-IMG-1")
                .attachmentAction("ADD")
                .versionNo(1)
                .fileId(6001L)
                .fileName("evidence.png")
                .contentType("image/png")
                .fileSize(2048L)
                .sha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .attachmentHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .operatorId(101L)
                .operatorName("QA")
                .operatedAt(verifiedAt.minusMinutes(10))
                .build()));

        MesProBatchRecordDomainTraceDetailRespVO detail = domainTraceService.getTraceDetail(1L);

        assertEquals(1, detail.getAttachmentCount());
        assertEquals("VALID", detail.getAttachmentChainStatus());
        assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                detail.getAttachmentChainHeadHash());
        assertEquals(1, detail.getAttachmentSummaries().size());
        MesProBatchRecordDomainTraceDetailRespVO.AttachmentSummary attachment =
                detail.getAttachmentSummaries().get(0);
        assertEquals(1001L, attachment.getId());
        assertEquals("visualEvidence", attachment.getFieldKey());
        assertEquals("现场图片", attachment.getFieldLabel());
        assertEquals("IMAGE", attachment.getAttachmentType());
        assertEquals("R1C2-IMG-1", attachment.getAttachmentGroupKey());
        assertEquals("ADD", attachment.getAttachmentAction());
        assertEquals("evidence.png", attachment.getFileName());
        assertEquals("image/png", attachment.getContentType());
        assertEquals(2048L, attachment.getFileSize());
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", attachment.getSha256());
    }

    @Test
    void verify_missingRequiredMasterData_persistsBlockedSnapshotAndNeverReturnsDefaultVerified() {
        MesProBatchRecordExecutionDO execution = baseExecution()
                .setWorkOrderCode(null)
                .setRouteProcessId(null)
                .setWorkstationId(null)
                .setBatchRecordReportId(null)
                .setExecutionSnapshotJson(null);
        when(executionMapper.selectById(1L)).thenReturn(execution);
        when(snapshotMapper.insert(any(MesProBatchRecordDomainTraceSnapshotDO.class))).thenAnswer(invocation -> {
            MesProBatchRecordDomainTraceSnapshotDO snapshot = invocation.getArgument(0);
            snapshot.setId(1001L);
            return 1;
        });
        when(itemMapper.insert(any(MesProBatchRecordDomainTraceItemDO.class))).thenReturn(1);
        when(executionMapper.updateById(any(MesProBatchRecordExecutionDO.class))).thenReturn(1);

        MesProBatchRecordDomainTraceDetailRespVO detail = domainTraceService.verify(new MesProBatchRecordDomainTraceVerifyReqVO()
                .setExecutionId(1L));

        assertEquals("BLOCKED", detail.getStatus());
        assertFalse(detail.getBlockers().isEmpty());
        assertTrue(detail.getBlockers().stream()
                .anyMatch(blocker -> "EDHR_DOMAIN_TRACE_WORK_ORDER_REQUIRED".equals(blocker.getBlockerCode())));
        assertNotNull(detail.getDomainTraceHash());

        ArgumentCaptor<MesProBatchRecordExecutionDO> updateCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionDO.class);
        verify(executionMapper).updateById(updateCaptor.capture());
        assertEquals(1L, updateCaptor.getValue().getId());
        assertEquals("BLOCKED", updateCaptor.getValue().getDomainTraceStatus());
        assertEquals(detail.getDomainTraceHash(), updateCaptor.getValue().getDomainTraceHash());
        assertTrue(detail.getItems().stream().noneMatch(item ->
                "TASK".equals(item.getItemType()) || "WORKSTATION".equals(item.getItemType())));
    }

    @Test
    void verify_expectedHashMismatch_failsFastBeforePersistingSnapshot() {
        when(executionMapper.selectById(1L)).thenReturn(baseExecution());

        assertServiceException(() -> domainTraceService.verify(new MesProBatchRecordDomainTraceVerifyReqVO()
                        .setExecutionId(1L)
                        .setExpectedDomainTraceHash("not-the-current-hash")),
                PRO_BATCH_RECORD_DOMAIN_TRACE_HASH_MISMATCH);

        verify(snapshotMapper, never()).insert(any(MesProBatchRecordDomainTraceSnapshotDO.class));
        verify(itemMapper, never()).insert(any(MesProBatchRecordDomainTraceItemDO.class));
        verify(executionMapper, never()).updateById(any(MesProBatchRecordExecutionDO.class));
    }

    @Test
    void verify_existingSnapshotHash_reusesPersistedSnapshotAndItemsWithoutDuplicateInsert() {
        MesProBatchRecordExecutionDO execution = baseExecution();
        AtomicReference<MesProBatchRecordDomainTraceSnapshotDO> persistedSnapshot = new AtomicReference<>();
        List<MesProBatchRecordDomainTraceItemDO> persistedItems = new ArrayList<>();
        when(executionMapper.selectById(1L)).thenReturn(execution);
        when(snapshotMapper.selectByExecutionIdAndSnapshotHash(eq(1L), anyString())).thenAnswer(invocation -> {
            MesProBatchRecordDomainTraceSnapshotDO snapshot = persistedSnapshot.get();
            String snapshotHash = invocation.getArgument(1);
            return snapshot != null && snapshot.getSnapshotHash().equals(snapshotHash) ? snapshot : null;
        });
        when(snapshotMapper.insert(any(MesProBatchRecordDomainTraceSnapshotDO.class))).thenAnswer(invocation -> {
            MesProBatchRecordDomainTraceSnapshotDO snapshot = invocation.getArgument(0);
            if (persistedSnapshot.get() != null
                    && persistedSnapshot.get().getSnapshotHash().equals(snapshot.getSnapshotHash())) {
                throw new DuplicateKeyException("Duplicate domain trace snapshot hash");
            }
            snapshot.setId(1003L);
            persistedSnapshot.set(MesProBatchRecordDomainTraceSnapshotDO.builder()
                    .id(snapshot.getId())
                    .executionId(snapshot.getExecutionId())
                    .snapshotVersion(snapshot.getSnapshotVersion())
                    .snapshotJson(snapshot.getSnapshotJson())
                    .snapshotHash(snapshot.getSnapshotHash())
                    .completenessStatus(snapshot.getCompletenessStatus())
                    .blockerCount(snapshot.getBlockerCount())
                    .verifiedAt(snapshot.getVerifiedAt())
                    .build());
            return 1;
        });
        when(itemMapper.insert(any(MesProBatchRecordDomainTraceItemDO.class))).thenAnswer(invocation -> {
            MesProBatchRecordDomainTraceItemDO item = invocation.getArgument(0);
            item.setId((long) persistedItems.size() + 1);
            persistedItems.add(MesProBatchRecordDomainTraceItemDO.builder()
                    .id(item.getId())
                    .snapshotId(item.getSnapshotId())
                    .executionId(item.getExecutionId())
                    .itemType(item.getItemType())
                    .itemKey(item.getItemKey())
                    .itemName(item.getItemName())
                    .sourceTable(item.getSourceTable())
                    .sourceId(item.getSourceId())
                    .sourceCode(item.getSourceCode())
                    .sourceVersion(item.getSourceVersion())
                    .snapshotJson(item.getSnapshotJson())
                    .snapshotHash(item.getSnapshotHash())
                    .requiredFlag(item.getRequiredFlag())
                    .status(item.getStatus())
                    .blockerCode(item.getBlockerCode())
                    .blockerMessage(item.getBlockerMessage())
                    .blockerReason(item.getBlockerReason())
                    .build());
            return 1;
        });
        when(itemMapper.selectListBySnapshotId(1003L)).thenReturn(persistedItems);
        when(executionMapper.updateById(any(MesProBatchRecordExecutionDO.class))).thenReturn(1);

        MesProBatchRecordDomainTraceDetailRespVO firstDetail = domainTraceService.verify(
                new MesProBatchRecordDomainTraceVerifyReqVO().setExecutionId(1L));
        MesProBatchRecordDomainTraceDetailRespVO secondDetail = assertDoesNotThrow(() -> domainTraceService.verify(
                new MesProBatchRecordDomainTraceVerifyReqVO()
                        .setExecutionId(1L)
                        .setExpectedDomainTraceHash(firstDetail.getDomainTraceHash())));

        assertEquals(1003L, firstDetail.getDomainTraceSnapshotId());
        assertEquals(1003L, secondDetail.getDomainTraceSnapshotId());
        assertEquals(firstDetail.getDomainTraceHash(), secondDetail.getDomainTraceHash());
        assertEquals(firstDetail.getItems().size(), secondDetail.getItems().size());
        verify(snapshotMapper, times(1)).insert(any(MesProBatchRecordDomainTraceSnapshotDO.class));
        verify(itemMapper, times(firstDetail.getItems().size())).insert(any(MesProBatchRecordDomainTraceItemDO.class));
        verify(executionMapper, times(2)).updateById(any(MesProBatchRecordExecutionDO.class));
    }

    @Test
    void verify_duplicateSnapshotInsertReloadsExistingSnapshotWithoutHidingMissingRows() {
        MesProBatchRecordExecutionDO execution = baseExecution();
        MesProBatchRecordDomainTraceSnapshotDO existingSnapshot = MesProBatchRecordDomainTraceSnapshotDO.builder()
                .id(1004L)
                .executionId(1L)
                .snapshotVersion("EDHR_DOMAIN_TRACE_V1")
                .snapshotHash("pending")
                .completenessStatus("VERIFIED")
                .blockerCount(0)
                .verifiedAt(LocalDateTime.of(2026, 5, 28, 10, 15))
                .build();
        MesProBatchRecordDomainTraceItemDO existingItem = MesProBatchRecordDomainTraceItemDO.builder()
                .id(2001L)
                .snapshotId(1004L)
                .executionId(1L)
                .itemType("WORK_ORDER")
                .itemKey("workOrderId")
                .itemName("Work order")
                .sourceId(20L)
                .sourceCode("MO-001")
                .sourceVersion("v1")
                .snapshotJson("{\"workOrderId\":20}")
                .snapshotHash("item-hash-ok")
                .status("VERIFIED")
                .build();
        AtomicReference<String> duplicateHash = new AtomicReference<>();
        when(executionMapper.selectById(1L)).thenReturn(execution);
        when(snapshotMapper.selectByExecutionIdAndSnapshotHash(eq(1L), anyString()))
                .thenAnswer(invocation -> {
                    String snapshotHash = invocation.getArgument(1);
                    if (duplicateHash.get() != null && duplicateHash.get().equals(snapshotHash)) {
                        existingSnapshot.setSnapshotHash(snapshotHash);
                        return existingSnapshot;
                    }
                    return null;
                });
        when(snapshotMapper.insert(any(MesProBatchRecordDomainTraceSnapshotDO.class))).thenAnswer(invocation -> {
            MesProBatchRecordDomainTraceSnapshotDO snapshot = invocation.getArgument(0);
            duplicateHash.set(snapshot.getSnapshotHash());
            throw new DuplicateKeyException("Concurrent duplicate domain trace snapshot hash");
        });
        lenient().when(itemMapper.selectListBySnapshotId(1004L)).thenReturn(List.of(existingItem));
        lenient().when(executionMapper.updateById(any(MesProBatchRecordExecutionDO.class))).thenReturn(1);

        MesProBatchRecordDomainTraceDetailRespVO detail = assertDoesNotThrow(() -> domainTraceService.verify(
                new MesProBatchRecordDomainTraceVerifyReqVO().setExecutionId(1L)));

        assertEquals(1004L, detail.getDomainTraceSnapshotId());
        assertEquals(existingSnapshot.getSnapshotHash(), detail.getDomainTraceHash());
        assertEquals(1, detail.getItems().size());
        verify(snapshotMapper, times(1)).insert(any(MesProBatchRecordDomainTraceSnapshotDO.class));
        verify(itemMapper, never()).insert(any(MesProBatchRecordDomainTraceItemDO.class));
        verify(executionMapper).updateById(any(MesProBatchRecordExecutionDO.class));
    }

    @Test
    void verify_executionPointerUpdateFailureRollsBackSnapshotPersistence() {
        when(executionMapper.selectById(1L)).thenReturn(baseExecution());
        when(snapshotMapper.insert(any(MesProBatchRecordDomainTraceSnapshotDO.class))).thenAnswer(invocation -> {
            MesProBatchRecordDomainTraceSnapshotDO snapshot = invocation.getArgument(0);
            snapshot.setId(1005L);
            return 1;
        });
        when(itemMapper.insert(any(MesProBatchRecordDomainTraceItemDO.class))).thenReturn(1);
        when(executionMapper.updateById(any(MesProBatchRecordExecutionDO.class))).thenReturn(0);

        assertServiceException(() -> domainTraceService.verify(new MesProBatchRecordDomainTraceVerifyReqVO()
                        .setExecutionId(1L)),
                PRO_BATCH_RECORD_DOMAIN_TRACE_PERSIST_FAILED);

        verify(snapshotMapper).insert(any(MesProBatchRecordDomainTraceSnapshotDO.class));
        verify(itemMapper, times(6)).insert(any(MesProBatchRecordDomainTraceItemDO.class));
        verify(executionMapper).updateById(any(MesProBatchRecordExecutionDO.class));
    }

    @Test
    void verifyForSubmit_blockedTraceThrowsBusinessErrorBeforeBpmOrSignatureSideEffects() {
        MesProBatchRecordExecutionDO execution = baseExecution()
                .setBatchCode(null);
        when(executionMapper.selectById(1L)).thenReturn(execution);
        when(snapshotMapper.insert(any(MesProBatchRecordDomainTraceSnapshotDO.class))).thenAnswer(invocation -> {
            MesProBatchRecordDomainTraceSnapshotDO snapshot = invocation.getArgument(0);
            snapshot.setId(1002L);
            return 1;
        });
        when(itemMapper.insert(any(MesProBatchRecordDomainTraceItemDO.class))).thenReturn(1);
        when(executionMapper.updateById(any(MesProBatchRecordExecutionDO.class))).thenReturn(1);

        assertServiceException(() -> domainTraceService.verifyForSubmit(1L),
                PRO_BATCH_RECORD_DOMAIN_TRACE_BLOCKED);

        verify(executionMapper).updateById(any(MesProBatchRecordExecutionDO.class));
    }

    private MesProBatchRecordExecutionDO baseExecution() {
        return MesProBatchRecordExecutionDO.builder()
                .id(1L)
                .executionCode("BRE-001")
                .workOrderId(20L)
                .workOrderCode("MO-001")
                .taskId(30L)
                .routeProcessId(40L)
                .workstationId(50L)
                .batchRecordReportId("RPT-001")
                .batchCode("BATCH-001")
                .executionSnapshotJson("{\"snapshot\":\"v1\"}")
                .cellValuesJson("[]")
                .cellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]"))
                .fieldAuditRevision(0L)
                .fieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                .build();
    }
}
