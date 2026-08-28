package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrMaterialGateReceiptDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrMaterialGateReceiptMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseMaterialGateReceipt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesReleaseMaterialGateReceiptPortImplTest {

    private static final Long TENANT_ID = 1L;
    private static final Long BATCH_ID = 88L;

    @Mock
    private MesProEdhrMaterialGateReceiptMapper mapper;

    @Test
    void persistReadyWritesImmutableReceiptAndCanReadItBack() {
        MesReleaseMaterialGateReceiptPortImpl adapter = new MesReleaseMaterialGateReceiptPortImpl(mapper);
        MesProEdhrFourMaterialGateResult gate = readyResult();
        when(mapper.selectLatestByBatchExecutionId(TENANT_ID, BATCH_ID)).thenReturn(null);
        when(mapper.insert(any(MesProEdhrMaterialGateReceiptDO.class))).thenAnswer(invocation -> {
            MesProEdhrMaterialGateReceiptDO row = invocation.getArgument(0);
            row.setId(9L);
            row.setCreateTime(LocalDateTime.now());
            return 1;
        });

        MesReleaseMaterialGateReceipt persisted = adapter.persistReady(
                TENANT_ID, BATCH_ID, "source-hash", gate, 1001L);

        assertNotNull(persisted);
        assertEquals("MATERIALS-88-1", persisted.getReceiptId());
        assertEquals(MesReleaseMaterialGateReceipt.STATUS_MATERIALS_READY, persisted.getGateStatus());
        assertEquals(MesReleaseMaterialGateReceipt.REQUIRED_MATERIAL_TYPES, persisted.getMaterialTypeKeys());
        assertNotNull(persisted.getReceiptHash());
    }

    @Test
    void readRejectsWrongTenantSourceOrTamperedHash() {
        MesReleaseMaterialGateReceiptPortImpl adapter = new MesReleaseMaterialGateReceiptPortImpl(mapper);
        MesProEdhrMaterialGateReceiptDO row = rowFrom(readyResult(), "source-hash");
        when(mapper.selectByReceiptId(TENANT_ID, BATCH_ID, "MATERIALS-88-1")).thenReturn(row);

        assertNull(adapter.getVerifiedByReceiptId(TENANT_ID, BATCH_ID, "MATERIALS-88-1", "other-source"));
        row.setReceiptHash("tampered");
        assertNull(adapter.getVerifiedByReceiptId(TENANT_ID, BATCH_ID, "MATERIALS-88-1", "source-hash"));
    }

    @Test
    void persistReadyRejectsTamperedExistingReceiptInsteadOfReusingIt() {
        MesReleaseMaterialGateReceiptPortImpl adapter = new MesReleaseMaterialGateReceiptPortImpl(mapper);
        MesProEdhrMaterialGateReceiptDO tampered = rowFrom(readyResult(), "source-hash");
        tampered.setReceiptHash("tampered");
        when(mapper.selectLatestByBatchExecutionId(TENANT_ID, BATCH_ID)).thenReturn(tampered);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                adapter.persistReady(TENANT_ID, BATCH_ID, "source-hash", readyResult(), 1001L));

        assertEquals("MATERIAL_GATE_RECEIPT_HASH_FAILED", exception.getMessage());
    }

    @Test
    void persistReadyRejectsDuplicateMaterialTypesEvenWhenTheCountLooksComplete() {
        MesReleaseMaterialGateReceiptPortImpl adapter = new MesReleaseMaterialGateReceiptPortImpl(mapper);
        MesProEdhrFourMaterialGateResult gate = duplicateTypeResult();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                adapter.persistReady(TENANT_ID, BATCH_ID, "source-hash", gate, 1001L));

        assertEquals("MATERIAL_GATE_RECEIPT_INPUT_INCOMPLETE", exception.getMessage());
    }

    private MesProEdhrFourMaterialGateResult readyResult() {
        List<MesProBatchRecordExecutionAttachmentDO> materials =
                MesProEdhrFourMaterialGateService.REQUIRED_MATERIAL_TYPES.stream()
                        .map(type -> MesProBatchRecordExecutionAttachmentDO.builder()
                                .fieldKey(type).attachmentGroupKey(type).versionNo(1).fileId(10L)
                                .sha256(type.repeat(64).substring(0, 64))
                                .attachmentHash((type + "-attachment").repeat(8).substring(0, 64))
                                .build())
                        .toList();
        return new MesProEdhrFourMaterialGateResult(
                MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_READY, true,
                "manifest-hash", materials);
    }

    private MesProEdhrFourMaterialGateResult duplicateTypeResult() {
        List<String> required = MesProEdhrFourMaterialGateService.REQUIRED_MATERIAL_TYPES;
        List<MesProBatchRecordExecutionAttachmentDO> materials = List.of(
                material(required.get(0), 1L),
                material(required.get(0), 2L),
                material(required.get(1), 3L),
                material(required.get(2), 4L));
        return new MesProEdhrFourMaterialGateResult(
                MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_READY, true,
                "manifest-hash", materials);
    }

    private MesProBatchRecordExecutionAttachmentDO material(String type, Long fileId) {
        return MesProBatchRecordExecutionAttachmentDO.builder()
                .fieldKey(type).attachmentGroupKey(type).versionNo(1).fileId(fileId)
                .sha256((type + "-" + fileId).repeat(8).substring(0, 64))
                .attachmentHash((type + "-attachment-" + fileId).repeat(8).substring(0, 64))
                .build();
    }

    private MesProEdhrMaterialGateReceiptDO rowFrom(MesProEdhrFourMaterialGateResult result,
                                                      String sourceSnapshotHash) {
        MesReleaseMaterialGateReceiptPortImpl adapter = new MesReleaseMaterialGateReceiptPortImpl(mapper);
        when(mapper.selectLatestByBatchExecutionId(TENANT_ID, BATCH_ID)).thenReturn(null);
        when(mapper.insert(any(MesProEdhrMaterialGateReceiptDO.class))).thenAnswer(invocation -> {
            MesProEdhrMaterialGateReceiptDO row = invocation.getArgument(0);
            row.setId(9L).setCreateTime(LocalDateTime.now());
            return 1;
        });
        MesReleaseMaterialGateReceipt receipt = adapter.persistReady(TENANT_ID, BATCH_ID,
                sourceSnapshotHash, result, 1001L);
        return MesProEdhrMaterialGateReceiptDO.from(receipt);
    }
}
