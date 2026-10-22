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

    private MesProEdhrDeviationServiceImpl service;
    private final AtomicReference<String> reservedPayloadHash = new AtomicReference<>();

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

    @Test
    void batchOptionsByActiveOrderResolvesFormalBatchSource() {
        when(batchExecutionOriginMapper.selectListByTraceFilter(991L, null, null, null))
                .thenReturn(List.of(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO()
                        .setBatchExecutionId(41L).setActiveOrderId(991L)));
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
                .setBatchExecutionId(41L)
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
