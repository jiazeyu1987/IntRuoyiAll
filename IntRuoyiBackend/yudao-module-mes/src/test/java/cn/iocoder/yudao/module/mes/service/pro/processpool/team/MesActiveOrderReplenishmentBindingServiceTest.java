package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.*;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterialService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesActiveOrderReplenishmentBindingServiceTest {
    @Mock MesProcessPoolActiveOrderMapper orders;
    @Mock MesProcessPoolActiveOrderProcessSnapshotMapper processes;
    @Mock MesProcessPoolActiveOrderCompletionReceiptMapper receipts;
    @Mock MesProWorkOrderMapper workOrders;
    @Mock ErpKingdeeProductionReplenishmentListItemMapper items;
    @Mock ErpKingdeeProductionReplenishmentListMapper headers;
    @Mock MesLocalTestRunSourceGuard localSources;
    @Mock MesFrontlineProcessMaterialService materials;
    @Mock MesProEdhrOperationAuditEventMapper events;
    @Mock MesProEdhrOperationAuditService audit;
    @Mock MesProBatchRecordExecutionSignatureService signing;
    @Mock ElectronicSignatureQueryService signatures;
    @Mock GxpAuditService gxpAudit;
    @InjectMocks MesActiveOrderReplenishmentBindingService service;

    MesProcessPoolActiveOrderDO order;
    ErpKingdeeProductionReplenishmentListItemDO item;
    ErpKingdeeProductionReplenishmentListDO header;

    @BeforeEach void prepare() {
        TenantContextHolder.setTenantId(183L);
        order = MesProcessPoolActiveOrderDO.builder().id(1L).leaderUserId(2L).workOrderId(3L)
                .routeId(4L).routeVersionId(5L).version(1).activeStatus("ACTIVE").build();
        order.setTenantId(183L);
        item = ErpKingdeeProductionReplenishmentListItemDO.builder().id(6L).productionReplenishmentListId(7L)
                .productionOrderNo("MO1").materialNumber("LABEL1").materialName("小标签")
                .lotNumber("LOT1").actualQuantity(new BigDecimal("379")).build();
        item.setTenantId(183L);
        header = ErpKingdeeProductionReplenishmentListDO.builder().id(7L).sourceBillNo("BL1").documentStatus("C").build();
        header.setTenantId(183L);
        lenient().when(orders.selectById(1L)).thenReturn(order);
        lenient().when(orders.selectByIdForUpdate(1L)).thenReturn(order);
        lenient().when(workOrders.selectById(3L)).thenReturn(MesProWorkOrderDO.builder().id(3L).code("MO1").build());
        lenient().when(items.selectListByProductionOrderNo("MO1")).thenReturn(List.of(item));
        lenient().when(items.selectById(6L)).thenReturn(item);
        lenient().when(headers.selectById(7L)).thenReturn(header);
        lenient().when(processes.selectListByActiveOrderId(1L)).thenReturn(List.of(
                MesProcessPoolActiveOrderProcessSnapshotDO.builder().activeOrderId(1L).routeProcessId(8L)
                        .processId(9L).processNameSnapshot("中包装").build()));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    MesActiveOrderReplenishmentBindingService.BindRequest request(String hash) {
        return new MesActiveOrderReplenishmentBindingService.BindRequest(1L, 1, 6L, 8L, hash, "确认小标签用于中包装", "test-only-password");
    }
    String sourceHash() { return MesActiveOrderReplenishmentBindingService.sourceHash(item, header); }

    @Test void otherLeaderCannotReadOrBind() {
        assertThrows(ServiceException.class, () -> service.get(99L, 1L));
        assertThrows(ServiceException.class, () -> service.bind(99L, request(sourceHash())));
        verifyNoInteractions(signing, audit);
    }
    @Test void completedOrderCannotBeAmended() {
        when(receipts.selectByActiveOrderId(1L)).thenReturn(new MesProcessPoolActiveOrderCompletionReceiptDO());
        assertThrows(ServiceException.class, () -> service.bind(2L, request(sourceHash())));
        verifyNoInteractions(signing, audit);
    }
    @Test void staleSourceCannotBeSigned() {
        assertThrows(ServiceException.class, () -> service.bind(2L, request("stale-source-hash")));
        verifyNoInteractions(signing, audit);
    }
    @Test void crossTenantCannotRead() {
        TenantContextHolder.setTenantId(5L);
        assertThrows(ServiceException.class, () -> service.get(2L, 1L));
    }
    @Test void unauditedErpSourceCannotBeBound() {
        header.setDocumentStatus("A");
        assertThrows(ServiceException.class, () -> service.get(2L, 1L));
    }
    @Test void businessSourceHashIncludesLotAndQuantityButNotSyncTime() {
        String original = sourceHash();
        item.setLastSyncTime(LocalDateTime.now());
        assertEquals(original, sourceHash());
        item.setActualQuantity(BigDecimal.ONE);
        assertNotEquals(original, sourceHash());
        item.setActualQuantity(new BigDecimal("379"));
        item.setLotNumber("OTHER-LOT");
        assertNotEquals(original, sourceHash());
    }
    @Test void signedAssociationIsAppendedAndOriginalRecordsAreNotUpdated() {
        var signed = new MesProBatchRecordExecutionFieldAuditSignatureResult().setSignatureId(10L)
                .setActorId(2L).setActorName("生产组长").setSignedAt(LocalDateTime.now());
        when(signing.recordFieldChangeSignature(any())).thenAnswer(invocation -> {
            var command = invocation.getArgument(0, MesProBatchRecordExecutionFieldAuditSignatureCommand.class);
            String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, "FIELD_CHANGE", null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, command.getSignatureChallengeHash());
            var evidence = mock(ElectronicSignatureEvidenceDTO.class);
            when(evidence.actorId()).thenReturn(2L); when(evidence.moduleCode()).thenReturn("MES");
            when(evidence.actionCode()).thenReturn("FIELD_CHANGE"); when(evidence.subjectType()).thenReturn("MES_BATCH_RECORD");
            when(evidence.subjectId()).thenReturn(subject);
            when(evidence.subjectVersion()).thenReturn(MesProBatchRecordExecutionFieldAuditHasher.sha256(subject));
            when(evidence.reason()).thenReturn(command.getReasonText());
            when(evidence.contentHash()).thenReturn("content"); when(evidence.evidenceHash()).thenReturn("evidence");
            when(signatures.getById(10L)).thenReturn(evidence);
            when(signatures.verifyEvidence(10L)).thenReturn(new ElectronicSignatureVerificationDTO(10L,"VALID",
                    "content","content","evidence","evidence",null,null,null,null,null,null,null,null,List.of(),null,null));
            return signed;
        });
        when(audit.recordInCallerTransaction(any())).thenReturn(new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOperationAuditRespVO().setId(11L));
        assertEquals(11L, service.bind(2L, request(sourceHash())));
        var captor = ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(audit).recordInCallerTransaction(captor.capture());
        var stored = JsonUtils.parseObject(captor.getValue().getMetadataJson(), MesActiveOrderReplenishmentBindingService.Evidence.class);
        assertEquals(183L, stored.tenantId()); assertEquals(8L, stored.routeProcessId()); assertEquals(10L, stored.signatureId());
        assertEquals(sourceHash(), stored.sourceHash());
        assertEquals(stored.challengeHash(), captor.getValue().getAfterSummaryHash());
        verify(orders, never()).updateById(any(MesProcessPoolActiveOrderDO.class));
        verifyNoMoreInteractions(audit);
    }
    @Test void changedSignedSourceIsRejectedBeforeSignatureAcceptance() {
        var evidence = new MesActiveOrderReplenishmentBindingService.Evidence(183L,1L,3L,5L,6L,
                "changed",8L,"原因",10L,null);
        String challenge = MesActiveOrderReplenishmentBindingService.challenge(evidence);
        evidence = new MesActiveOrderReplenishmentBindingService.Evidence(183L,1L,3L,5L,6L,"changed",8L,"原因",10L,challenge);
        when(events.selectListByObject("ACTIVE_ORDER_REPLENISHMENT","1")).thenReturn(List.of(
                new MesProEdhrOperationAuditEventDO().setId(11L).setOperationType("REPLENISHMENT_PROCESS_BIND")
                        .setResultStatus("SUCCESS").setRouteId(4L).setRouteProcessId(8L).setAfterSummaryHash(challenge)
                        .setMetadataJson(JsonUtils.toJsonString(evidence))));
        assertThrows(ServiceException.class, () -> service.read(1L));
        verifyNoInteractions(signatures);
    }
}
