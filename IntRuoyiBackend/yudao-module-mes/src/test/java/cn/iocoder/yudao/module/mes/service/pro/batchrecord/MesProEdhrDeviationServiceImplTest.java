package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationBatchOptionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationCreateRequestDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationCreateRequestMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditAppendResult;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import cn.iocoder.yudao.framework.common.pojo.PageResult;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_BATCH_MARKET_RELEASED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_BATCH_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_DEVIATION_IDEMPOTENCY_CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrDeviationServiceImplTest {

    @Mock
    private MesProEdhrDeviationMapper deviationMapper;
    @Mock
    private MesProEdhrDeviationCreateRequestMapper createRequestMapper;
    @Mock
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Mock
    private MesProEdhrDeviationNumberGenerator numberGenerator;
    @Mock
    private MesProBatchRecordExecutionSignatureService signatureService;
    @Mock
    private GxpAuditService gxpAuditService;
    @Mock
    private MesProEdhrBatchExecutionOriginMapper batchExecutionOriginMapper;
    @Mock
    private MesProEdhrWorkTaskService workTaskService;

    @Mock
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper activeOrderMapper;

    private MesProEdhrDeviationServiceImpl service;
    @Mock
    private MesDeviationAdministratorService administrators;
    private final AtomicReference<String> reservedPayloadHash = new AtomicReference<>();

    private void prepareAdministratorOrder(boolean formalBatch) {
        when(activeOrderMapper.selectDeviationSourcesForUpdate(71L, 51L)).thenReturn(List.of(order(999L)));
        var workOrders = org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper.class);
        ReflectionTestUtils.setField(service, "workOrderMapper", workOrders);
        var workOrder = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO();
        workOrder.setId(51L); workOrder.setCode("WO-51"); workOrder.setBatchCode("LOT-41"); workOrder.setTenantId(71L);
        when(workOrders.selectById(51L)).thenReturn(workOrder);
        if (formalBatch) {
            when(batchExecutionOriginMapper.selectListByTraceFilter(991L, null, null, null))
                    .thenReturn(List.of(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO()
                            .setBatchExecutionId(41L).setActiveOrderId(991L).setEntryType("ACTIVE_ORDER_COMPLETION")));
            when(batchExecutionMapper.selectByTenantIdAndId(71L, 41L)).thenReturn(batch(71L));
            when(batchExecutionMapper.selectByTenantIdAndIdForUpdate(71L, 41L)).thenReturn(batch(71L));
        }
    }

    private void prepareSuccessfulAdministratorCreation() {
        when(numberGenerator.nextCode(71L)).thenReturn("PC-ADMIN-ORDER");
        doAnswer(call -> { ((MesProEdhrDeviationDO) call.getArgument(0)).setId(901L); return 1; })
                .when(deviationMapper).insert(any(MesProEdhrDeviationDO.class));
        when(createRequestMapper.linkDeviation(eq(71L), eq("req-1"), any(), eq(901L))).thenReturn(1);
    }

    @Test
    void administratorSelectsProductionOrderWithoutNeedingToBeItsLeader() {
        prepareAdministratorOrder(true);
        prepareSuccessfulAdministratorCreation();
        var result = service.createByWorkOrder(201L, request().setWorkOrderId(51L).setBatchExecutionId(null).setReceiverId(301L));
        assertEquals(991L, result.getActiveOrderId());
        assertEquals(41L, result.getBatchExecutionId());
        assertEquals("WO-51", result.getWorkOrderCode());
        verify(workTaskService).assignDeviationTask(any(MesProEdhrDeviationDO.class),
                eq(MesProEdhrWorkTaskService.TASK_TYPE_DEVIATION_RECEIVE), eq(201L), any());
        verify(signatureService).recordDeviationInitiationSignature(eq(201L), eq(41L), eq(901L), any(), any(), any(), any());
    }

    @Test
    void administratorCanInitiateDuringProductionBeforeFormalBatchExists() {
        prepareAdministratorOrder(false);
        prepareSuccessfulAdministratorCreation();
        when(signatureService.recordActiveOrderDeviationInitiationSignature(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(88001L);
        var result = service.createByWorkOrder(201L, request().setWorkOrderId(51L).setBatchExecutionId(null));
        assertEquals(991L, result.getActiveOrderId());
        org.junit.jupiter.api.Assertions.assertNull(result.getBatchExecutionId());
        assertEquals("WO-51", result.getWorkOrderCode());
        verify(signatureService).recordActiveOrderDeviationInitiationSignature(eq(201L), eq(991L), eq(901L), any(), eq("WO-51"), any(), any());
    }

    @Test
    void administratorCannotForgeOrderSourceOrUseMissingAndAmbiguousCycles() {
        assertThrows(ServiceException.class, () -> service.createByWorkOrder(201L, request().setWorkOrderId(51L)));
        assertThrows(ServiceException.class, () -> service.createByWorkOrder(201L, request().setBatchExecutionId(null)));
        assertThrows(ServiceException.class, () -> service.createByWorkOrder(201L, request().setWorkOrderId(51L).setBatchExecutionId(null)));
        when(activeOrderMapper.selectDeviationSourcesForUpdate(71L, 51L)).thenReturn(List.of(order(999L), order(998L).setId(992L)));
        assertThrows(ServiceException.class, () -> service.createByWorkOrder(201L, request().setWorkOrderId(51L).setBatchExecutionId(null)));
        verify(numberGenerator, never()).nextCode(any());
        verify(deviationMapper, never()).insert(any(MesProEdhrDeviationDO.class));
    }

    @Test
    void administratorCannotInitiateAfterMarketRelease() {
        prepareAdministratorOrder(true);
        when(releaseTransactionMapper.selectListByTenantAndBatchExecutionIdForUpdate(71L, 41L))
                .thenReturn(List.of(new MesProEdhrReleaseTransactionDO().setReleaseStatus("RELEASED")));
        assertThrows(ServiceException.class, () -> service.createByWorkOrder(201L, request().setWorkOrderId(51L).setBatchExecutionId(null)));
        verify(deviationMapper, never()).insert(any(MesProEdhrDeviationDO.class));
    }

    @Test
    void completedOrderRequiresItsFormalBatch() {
        prepareAdministratorOrder(false);
        when(activeOrderMapper.selectDeviationSourcesForUpdate(71L, 51L))
                .thenReturn(List.of(order(999L).setBusinessStatus("COMPLETED")));
        assertThrows(ServiceException.class, () -> service.createByWorkOrder(201L, request().setWorkOrderId(51L).setBatchExecutionId(null)));
        verify(deviationMapper, never()).insert(any(MesProEdhrDeviationDO.class));
    }

    @Test
    void signedOrderReplayDoesNotResolveANewBatchOrSignAgain() {
        var req = request().setWorkOrderId(51L).setBatchExecutionId(null).setReceiverName(null);
        var signed = new MesProEdhrDeviationDO().setId(901L).setTenantId(71L).setActiveOrderId(991L).setBatchExecutionId(null);
        when(createRequestMapper.selectForUpdate(71L, "req-1")).thenReturn(
                new MesProEdhrDeviationCreateRequestDO().setPayloadHash(service.buildCreatePayloadHash(req)).setDeviationId(901L));
        when(deviationMapper.selectByTenantAndId(71L, 901L)).thenReturn(signed);
        assertEquals(901L, service.createByWorkOrder(201L, req).getId());
        verify(activeOrderMapper, never()).selectDeviationSourcesForUpdate(any(), any());
        verify(deviationMapper, never()).insert(any(MesProEdhrDeviationDO.class));
        verify(signatureService, never()).recordActiveOrderDeviationInitiationSignature(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void differentProductionOrderCannotReuseSignedRequestKey() {
        String payloadHash = service.buildCreatePayloadHash(request().setWorkOrderId(51L).setBatchExecutionId(null).setReceiverName(null));
        when(createRequestMapper.selectForUpdate(71L, "req-1")).thenReturn(
                new MesProEdhrDeviationCreateRequestDO().setPayloadHash(payloadHash).setDeviationId(901L));
        assertThrows(ServiceException.class, () -> service.createByWorkOrder(201L, request().setWorkOrderId(52L).setBatchExecutionId(null)));
        verify(activeOrderMapper, never()).selectDeviationSourcesForUpdate(any(), any());
    }


    private cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO order(Long owner) {
        var order = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO()
                .setId(991L).setLeaderUserId(owner).setWorkOrderId(batch(71L).getWorkOrderId())
                .setActiveStatus("ACTIVE").setBusinessStatus("ACTIVE");
        order.setTenantId(71L);
        return order;
    }

    @Test
    void productionOrderWithoutBatchSignsImmutableOrderAndDispatchesInvestigator() {
        when(activeOrderMapper.selectById(991L)).thenReturn(order(201L));
        when(activeOrderMapper.selectByIdForUpdate(991L)).thenReturn(order(201L));
        var workOrders = org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper.class);
        ReflectionTestUtils.setField(service, "workOrderMapper", workOrders);
        when(workOrders.selectById(order(201L).getWorkOrderId())).thenReturn(
                new cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO()
                        .setId(order(201L).getWorkOrderId()).setCode("WO-ORDER").setBatchCode("B-ORDER"));
        when(numberGenerator.nextCode(71L)).thenReturn("PC-ORDER-PRODUCTION");
        doAnswer(call -> { ((MesProEdhrDeviationDO) call.getArgument(0)).setId(901L); return 1; })
                .when(deviationMapper).insert(any(MesProEdhrDeviationDO.class));
        when(createRequestMapper.linkDeviation(eq(71L), eq("req-1"), any(), eq(901L))).thenReturn(1);
        when(signatureService.recordActiveOrderDeviationInitiationSignature(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(88001L);
        var result = service.create(201L, request().setActiveOrderId(991L).setBatchExecutionId(null).setReceiverId(301L));
        assertEquals(991L, result.getActiveOrderId());
        org.junit.jupiter.api.Assertions.assertNull(result.getBatchExecutionId());
        assertEquals(88001L, result.getInitiatorSignatureId());
        verify(signatureService).recordActiveOrderDeviationInitiationSignature(eq(201L), eq(991L), eq(901L),
                eq("PC-ORDER-PRODUCTION"), eq("WO-ORDER"), any(), any());
        verify(workTaskService).assignDeviationTask(org.mockito.ArgumentMatchers.argThat(row ->
                row.getBatchExecutionId() == null && row.getActiveOrderId().equals(991L)),
                eq(MesProEdhrWorkTaskService.TASK_TYPE_DEVIATION_RECEIVE), eq(301L), any());
        var audit = org.mockito.ArgumentCaptor.forClass(GxpAuditCommand.class);
        verify(gxpAuditService).append(audit.capture());
        org.junit.jupiter.api.Assertions.assertTrue(audit.getValue().toString().contains("991"));
    }

    @Test
    void retiredOrderCannotCreateInNewCycle() {
        when(activeOrderMapper.selectById(991L)).thenReturn(order(201L).setActiveStatus("RETIRED"));
        assertThrows(ServiceException.class, () -> service.create(201L,
                request().setActiveOrderId(991L).setBatchExecutionId(null).setReceiverId(301L)));
        verify(signatureService, never()).recordActiveOrderDeviationInitiationSignature(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void forgedOrRevokedOrderReceiverRejectsCreateBeforeReservationAndSignature() {
        when(activeOrderMapper.selectById(991L)).thenReturn(order(201L));
        org.mockito.Mockito.doThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_DEVIATION_ADMINISTRATOR_REQUIRED))
                .when(administrators).requireReceiver(301L);
        var error = assertThrows(ServiceException.class,
                () -> service.create(201L, request().setActiveOrderId(991L).setReceiverId(301L)));
        assertEquals(MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_DEVIATION_ADMINISTRATOR_REQUIRED.getCode(), error.getCode());
        var locks = org.mockito.Mockito.inOrder(gxpAuditService, administrators);
        locks.verify(gxpAuditService).acquireLedgerLock();
        locks.verify(administrators).requireReceiver(301L);
        verify(createRequestMapper, never()).reserveOrLock(any(), any(), any());
        verify(deviationMapper, never()).insert(any(MesProEdhrDeviationDO.class));
    }

    @Test
    void orderEntryRejectsOtherLeaderAndForeignTenantBeforeCreatingAnything() {
        when(activeOrderMapper.selectById(991L)).thenReturn(order(202L));
        assertThrows(ServiceException.class, () -> service.create(201L, request().setActiveOrderId(991L)));
        var foreign = order(201L);
        foreign.setTenantId(72L);
        when(activeOrderMapper.selectById(991L)).thenReturn(foreign);
        assertThrows(ServiceException.class, () -> service.getCreationBatchOptionsByActiveOrder(201L, 991L));
        verify(gxpAuditService, never()).acquireLedgerLock();
        verify(deviationMapper, never()).insert(any(MesProEdhrDeviationDO.class));
    }

    @Test
    void orderEntryRejectsBatchFromAnotherCycle() {
        when(activeOrderMapper.selectById(992L)).thenReturn(order(201L).setId(992L));
        when(activeOrderMapper.selectByIdForUpdate(992L)).thenReturn(order(201L).setId(992L));
        when(batchExecutionMapper.selectByTenantIdAndIdForUpdate(71L, 41L)).thenReturn(batch(71L));
        assertThrows(ServiceException.class, () -> service.create(201L, request().setActiveOrderId(992L).setReceiverId(301L)));
        verify(numberGenerator, never()).nextCode(any());
    }

    @Test
    void orderEntryRechecksLockedOwnerBeforeSignatureOrInsert() {
        when(activeOrderMapper.selectById(991L)).thenReturn(order(201L));
        when(activeOrderMapper.selectByIdForUpdate(991L)).thenReturn(order(202L));
        assertThrows(ServiceException.class, () -> service.create(201L, request().setActiveOrderId(991L).setReceiverId(301L)));
        verify(numberGenerator, never()).nextCode(any());
    }

    @Test
    void orderCreationRequiresAssignedInvestigator() {
        when(activeOrderMapper.selectById(991L)).thenReturn(order(201L));
        assertThrows(ServiceException.class, () -> service.create(201L, request().setActiveOrderId(991L).setReceiverId(null)));
        verify(gxpAuditService, never()).acquireLedgerLock();
    }

    @Test
    void orderEndpointsRequireDedicatedCreationPermission() throws Exception {
        var controller = cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.MesProEdhrDeviationController.class;
        for (var name : List.of("createByActiveOrder", "getCreationBatchOptionsByActiveOrder")) {
            var method = java.util.Arrays.stream(controller.getMethods()).filter(item -> item.getName().equals(name)).findFirst().orElseThrow();
            assertEquals("@ss.hasPermission('mes:pro-process-pool-team-leader:deviation-create')",
                    method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value());
        }
    }

    @Test
    void orderWithoutFormalBatchReturnsNoInventedSource() {
        when(activeOrderMapper.selectById(991L)).thenReturn(order(201L));
        when(batchExecutionOriginMapper.selectListByTraceFilter(991L, null, null, null)).thenReturn(List.of());
        assertEquals(List.of(), service.getCreationBatchOptionsByActiveOrder(201L, 991L));
        verify(batchExecutionMapper, never()).selectByTenantIdAndId(any(), any());
    }

    @Test
    void orderScopedCreationUsesExistingSignedCreationAndTaskDispatch() {
        when(activeOrderMapper.selectById(991L)).thenReturn(order(201L));
        when(activeOrderMapper.selectByIdForUpdate(991L)).thenReturn(order(201L));
        when(batchExecutionMapper.selectByTenantIdAndIdForUpdate(71L, 41L)).thenReturn(batch(71L));
        when(releaseTransactionMapper.selectListByTenantAndBatchExecutionIdForUpdate(71L, 41L)).thenReturn(List.of());
        when(numberGenerator.nextCode(71L)).thenReturn("PC-ORDER-1");
        doAnswer(call -> { ((MesProEdhrDeviationDO) call.getArgument(0)).setId(901L); return 1; })
                .when(deviationMapper).insert(any(MesProEdhrDeviationDO.class));
        when(createRequestMapper.linkDeviation(eq(71L), eq("req-1"), any(), eq(901L))).thenReturn(1);
        var result = service.create(201L, request().setActiveOrderId(991L).setReceiverId(301L));
        assertEquals(41L, result.getBatchExecutionId());
        assertEquals("PC-ORDER-1", result.getDeviationCode());
        verify(signatureService).recordDeviationInitiationSignature(eq(201L), eq(41L), eq(901L), any(), any(), any(), any());
        verify(workTaskService).assignDeviationTask(any(MesProEdhrDeviationDO.class),
                eq(MesProEdhrWorkTaskService.TASK_TYPE_DEVIATION_RECEIVE), eq(301L), any());
    }

    @Test
    void orderIdentityIsPartOfIdempotencyHash() {
        org.junit.jupiter.api.Assertions.assertNotEquals(service.buildCreatePayloadHash(request()),
                service.buildCreatePayloadHash(request().setActiveOrderId(991L)));
        org.junit.jupiter.api.Assertions.assertNotEquals(service.buildCreatePayloadHash(request().setActiveOrderId(991L)),
                service.buildCreatePayloadHash(request().setActiveOrderId(992L)));
    }

    @Test
    void ledgerLockPrecedesBusinessLocks() {
        assertThrows(ServiceException.class, () -> service.create(201L, request()));
        var order = org.mockito.Mockito.inOrder(gxpAuditService, createRequestMapper, batchExecutionMapper);
        order.verify(gxpAuditService).acquireLedgerLock();
        order.verify(createRequestMapper).reserveOrLock(eq(71L), eq("req-1"), any());
        order.verify(batchExecutionMapper).selectByTenantIdAndIdForUpdate(71L, 41L);
    }

    @Test
    void signaturePasswordIsExcludedFromRequestToString() {
        MesProEdhrDeviationCreateReqVO reqVO = request().setSignaturePassword("do-not-log-this-password");

        assertFalse(reqVO.toString().contains("do-not-log-this-password"));
    }

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(71L);
        lenient().when(createRequestMapper.reserveOrLock(org.mockito.ArgumentMatchers.eq(71L),
                org.mockito.ArgumentMatchers.eq("req-1"), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    reservedPayloadHash.set(invocation.getArgument(2));
                    return 1;
                });
        lenient().when(createRequestMapper.selectForUpdate(71L, "req-1")).thenAnswer(invocation ->
                new MesProEdhrDeviationCreateRequestDO().setTenantId(71L).setIdempotencyKey("req-1")
                        .setPayloadHash(reservedPayloadHash.get()));
        lenient().when(signatureService.recordDeviationInitiationSignature(
                any(), any(), any(), any(), any(), any(), any())).thenReturn(88001L);
        lenient().when(deviationMapper.attachInitiatorSignature(
                any(), any(), any(), any(), nullable(String.class))).thenReturn(1);
        lenient().when(gxpAuditService.append(any(GxpAuditCommand.class)))
                .thenReturn(new GxpAuditAppendResult(1L, 1L, "event-hash", false));
        lenient().when(batchExecutionOriginMapper.selectListByBatchExecutionId(41L))
                .thenReturn(List.of(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO()
                        .setBatchExecutionId(41L).setActiveOrderId(991L).setEntryType("ACTIVE_ORDER_COMPLETION")));
        service = new MesProEdhrDeviationServiceImpl(
                deviationMapper, createRequestMapper, batchExecutionMapper, releaseTransactionMapper, numberGenerator,
                signatureService, gxpAuditService);
        ReflectionTestUtils.setField(service, "batchExecutionOriginMapper", batchExecutionOriginMapper);
        ReflectionTestUtils.setField(service, "accessService", org.mockito.Mockito.mock(MesProEdhrDeviationAccessService.class));
        ReflectionTestUtils.setField(service, "workTaskService", workTaskService);
        ReflectionTestUtils.setField(service, "activeOrderMapper", activeOrderMapper);
        lenient().when(administrators.requireReceiver(201L)).thenReturn(
                new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesDeviationAdministratorRespVO().setId(201L).setNickname("发起管理员"));
        lenient().when(administrators.requireReceiver(301L)).thenReturn(
                new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesDeviationAdministratorRespVO().setId(301L).setNickname("偏差管理员"));
        ReflectionTestUtils.setField(service, "administratorService", administrators);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
        reservedPayloadHash.set(null);
    }

    @Test
    void createUsesFormalBatchTenantAndPersistsSingleCreateIdempotencyFact() {
        MesProEdhrDeviationCreateReqVO reqVO = request();
        String payloadHash = service.buildCreatePayloadHash(reqVO);
        when(createRequestMapper.reserveOrLock(71L, "req-1", payloadHash)).thenReturn(1);
        when(createRequestMapper.selectForUpdate(71L, "req-1")).thenReturn(
                new MesProEdhrDeviationCreateRequestDO().setTenantId(71L).setIdempotencyKey("req-1")
                        .setPayloadHash(payloadHash));
        when(batchExecutionMapper.selectByTenantIdAndIdForUpdate(71L, 41L)).thenReturn(batch(71L));
        when(releaseTransactionMapper.selectListByTenantAndBatchExecutionIdForUpdate(71L, 41L))
                .thenReturn(List.of());
        when(numberGenerator.nextCode(71L)).thenReturn("PC-202610-0001");
        doAnswer(invocation -> {
            MesProEdhrDeviationDO inserted = invocation.getArgument(0);
            inserted.setId(901L);
            return 1;
        }).when(deviationMapper).insert(any(MesProEdhrDeviationDO.class));
        when(createRequestMapper.linkDeviation(eq(71L), eq("req-1"), any(), eq(901L))).thenReturn(1);

        var result = service.create(201L, reqVO);

        assertEquals(901L, result.getId());
        assertEquals("PC-202610-0001", result.getDeviationCode());
        assertEquals(41L, result.getBatchExecutionId());
        verify(deviationMapper).insert(any(MesProEdhrDeviationDO.class));
        verify(batchExecutionMapper).selectByTenantIdAndIdForUpdate(71L, 41L);
    }

    @Test
    void batchOptionsReadFormalBatchExecutionsWithoutRequiringPqcReleaseApplication() {
        MesProEdhrDeviationBatchOptionPageReqVO reqVO = new MesProEdhrDeviationBatchOptionPageReqVO();
        when(batchExecutionMapper.selectDeviationOptionsPage(reqVO, 71L, null))
                .thenReturn(new PageResult<>(List.of(batch(71L)), 1L));

        var page = service.getBatchOptions(reqVO);

        assertEquals(1L, page.getTotal());
        assertEquals(41L, page.getList().get(0).getBatchExecutionId());
        assertEquals("BR-41", page.getList().get(0).getBatchExecutionCode());
        verify(batchExecutionMapper).selectDeviationOptionsPage(reqVO, 71L, null);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullSource
    @org.junit.jupiter.params.provider.ValueSource(longs = {301L})
    void standaloneCreationAssignsInitiatorEvenWhenClientSelectsAnotherReceiver(Long submittedReceiver) {
        when(batchExecutionMapper.selectByTenantIdAndIdForUpdate(71L, 41L)).thenReturn(batch(71L));
        when(numberGenerator.nextCode(71L)).thenReturn("PC-SELF-1");
        doAnswer(call -> { ((MesProEdhrDeviationDO) call.getArgument(0)).setId(901L); return 1; })
                .when(deviationMapper).insert(any(MesProEdhrDeviationDO.class));
        when(createRequestMapper.linkDeviation(eq(71L), eq("req-1"), any(), eq(901L))).thenReturn(1);
        var req = request().setReceiverId(submittedReceiver).setReceiverName("客户端指定人员");

        service.create(201L, req);

        var inserted = org.mockito.ArgumentCaptor.forClass(MesProEdhrDeviationDO.class);
        verify(deviationMapper).insert(inserted.capture());
        assertEquals(201L, inserted.getValue().getInitiatorUserId());
        assertEquals(201L, inserted.getValue().getReceiverId());
        assertEquals("发起管理员", inserted.getValue().getReceiverName());
        verify(administrators).requireReceiver(201L);
        verify(administrators, never()).requireReceiver(301L);
        assertEquals(service.buildCreatePayloadHash(request()), reservedPayloadHash.get());
        verify(workTaskService).assignDeviationTask(any(MesProEdhrDeviationDO.class),
                eq(MesProEdhrWorkTaskService.TASK_TYPE_DEVIATION_RECEIVE), eq(201L), any());
    }

    @Test
    void standaloneInitiatorWithoutAdministratorEligibilityCannotDelegateToAnotherAdministrator() {
        var denied = cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_DEVIATION_ADMINISTRATOR_REQUIRED);
        when(administrators.requireReceiver(201L)).thenThrow(denied);

        assertEquals(denied, assertThrows(ServiceException.class,
                () -> service.create(201L, request().setReceiverId(301L))));

        verify(createRequestMapper, never()).reserveOrLock(any(), any(), any());
        verify(deviationMapper, never()).insert(any(MesProEdhrDeviationDO.class));
        verify(workTaskService, never()).assignDeviationTask(any(), any(), any(), any());
    }

    @Test
    void batchOptionsByActiveOrderResolvesFormalBatchSource() {
        when(batchExecutionOriginMapper.selectListByTraceFilter(991L, null, null, null))
                .thenReturn(List.of(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO()
                        .setBatchExecutionId(41L).setActiveOrderId(991L).setEntryType("ACTIVE_ORDER_COMPLETION")));
        when(batchExecutionMapper.selectByTenantIdAndId(71L, 41L)).thenReturn(batch(71L));

        var result = service.getBatchOptionsByActiveOrder(991L);

        assertEquals(1, result.size());
        assertEquals(41L, result.get(0).getBatchExecutionId());
    }

    @Test
    void createRejectsCrossTenantOrMissingBatchBeforeConsumingNumber() {
        when(batchExecutionMapper.selectByTenantIdAndIdForUpdate(71L, 41L)).thenReturn(null);

        ServiceException error = assertThrows(ServiceException.class, () -> service.create(201L, request()));

        assertEquals(PRO_EDHR_DEVIATION_BATCH_NOT_EXISTS.getCode(), error.getCode());
        verify(numberGenerator, never()).nextCode(any());
        verify(deviationMapper, never()).insert(any(MesProEdhrDeviationDO.class));
    }

    @Test
    void createRejectsBatchMapperReturningAnotherTenantBeforeConsumingNumber() {
        when(batchExecutionMapper.selectByTenantIdAndIdForUpdate(71L, 41L)).thenReturn(batch(99L));

        ServiceException error = assertThrows(ServiceException.class, () -> service.create(201L, request()));

        assertEquals(PRO_EDHR_DEVIATION_BATCH_NOT_EXISTS.getCode(), error.getCode());
        verify(numberGenerator, never()).nextCode(any());
        verify(deviationMapper, never()).insert(any(MesProEdhrDeviationDO.class));
    }

    @Test
    void createRejectsMarketReleasedBatchAndDoesNotInsertDeviation() {
        when(batchExecutionMapper.selectByTenantIdAndIdForUpdate(71L, 41L)).thenReturn(batch(71L));
        when(releaseTransactionMapper.selectListByTenantAndBatchExecutionIdForUpdate(71L, 41L))
                .thenReturn(List.of(MesProEdhrReleaseTransactionDO.builder()
                        .batchExecutionId(41L).releaseStatus("RELEASED").build()));

        ServiceException error = assertThrows(ServiceException.class, () -> service.create(201L, request()));

        assertEquals(PRO_EDHR_DEVIATION_BATCH_MARKET_RELEASED.getCode(), error.getCode());
        verify(numberGenerator, never()).nextCode(any());
        verify(deviationMapper, never()).insert(any(MesProEdhrDeviationDO.class));
    }

    @Test
    void createReplaysSamePayloadWithoutRelockingBatchOrAllocatingAnotherNumber() {
        MesProEdhrDeviationCreateReqVO reqVO = request();
        MesProEdhrDeviationDO existing = new MesProEdhrDeviationDO()
                .setId(901L).setTenantId(71L).setCreateIdempotencyKey("req-1")
                .setCreatePayloadHash(service.buildCreatePayloadHash(reqVO))
                .setDeviationCode("PC-202610-0001").setBatchExecutionId(41L)
                .setBatchExecutionCode("BR-41").setBatchCode("LOT-41")
                .setLevel("NORMAL").setStatus("OPEN");
        String payloadHash = service.buildCreatePayloadHash(reqVO);
        when(createRequestMapper.reserveOrLock(71L, "req-1", payloadHash)).thenReturn(1);
        when(createRequestMapper.selectForUpdate(71L, "req-1")).thenReturn(
                new MesProEdhrDeviationCreateRequestDO().setTenantId(71L).setIdempotencyKey("req-1")
                        .setPayloadHash(payloadHash).setDeviationId(901L));
        when(deviationMapper.selectByTenantAndId(71L, 901L)).thenReturn(existing);

        var result = service.create(201L, reqVO);

        assertEquals(901L, result.getId());
        verify(batchExecutionMapper, never()).selectByTenantIdAndIdForUpdate(any(), any());
        verify(numberGenerator, never()).nextCode(any());
    }

    @Test
    void createRejectsSameIdempotencyKeyWithDifferentPayload() {
        MesProEdhrDeviationCreateReqVO original = request();
        MesProEdhrDeviationDO existing = new MesProEdhrDeviationDO()
                .setId(901L).setCreateIdempotencyKey("req-1")
                .setCreatePayloadHash(service.buildCreatePayloadHash(original));
        MesProEdhrDeviationCreateReqVO changed = request().setDescription("different payload");
        when(createRequestMapper.selectForUpdate(71L, "req-1")).thenReturn(
                new MesProEdhrDeviationCreateRequestDO().setTenantId(71L).setIdempotencyKey("req-1")
                        .setPayloadHash(existing.getCreatePayloadHash()));

        ServiceException error = assertThrows(ServiceException.class, () -> service.create(201L, changed));

        assertEquals(PRO_EDHR_DEVIATION_IDEMPOTENCY_CONFLICT.getCode(), error.getCode());
    }

    private static MesProEdhrDeviationCreateReqVO request() {
        return new MesProEdhrDeviationCreateReqVO()
                .setBatchExecutionId(41L).setReceiverId(201L)
                .setLevel("NORMAL")
                .setCategoryCodes(List.of("PRODUCTION_PROCESS"))
                .setDescription("生产过程发生偏差")
                .setLevelBasis("经评估属于普通偏差")
                .setIdempotencyKey("req-1")
                .setSignaturePassword("unit-test-password");
    }

    private static MesProEdhrBatchExecutionDO batch(Long tenantId) {
        return MesProEdhrBatchExecutionDO.builder()
                .id(41L).tenantId(tenantId).batchExecutionCode("BR-41")
                .workOrderId(51L).workOrderCode("WO-51").batchCode("LOT-41")
                .status(20).build();
    }
}
