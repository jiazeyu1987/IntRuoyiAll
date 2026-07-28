package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessFlowService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProductBomService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProductService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteControlledContentAdapter;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteOwnerPermissionService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteServiceImpl;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteVersionAndCopyTest {

    @InjectMocks
    private MesProRouteServiceImpl routeService;

    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteProductBomMapper routeProductBomMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteScheduleConfigMapper routeScheduleConfigMapper;
    @Mock
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Mock
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Mock
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Mock
    private MesProRouteProcessFlowService routeProcessFlowService;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProRouteProductService routeProductService;
    @Mock
    private MesProRouteProductBomService routeProductBomService;
    @Mock
    private MesProRouteControlledContentAdapter controlledContentAdapter;
    @Mock
    private MesProRouteOwnerPermissionService routeOwnerPermissionService;

    @BeforeEach
    void stubCurrentRouteProcessIdentity() {
        lenient().when(routeProcessService.resolveCurrentRouteProcess(
                        org.mockito.ArgumentMatchers.nullable(Long.class),
                        org.mockito.ArgumentMatchers.nullable(Long.class),
                        org.mockito.ArgumentMatchers.nullable(Long.class)))
                .thenAnswer(invocation -> MesProRouteProcessDO.builder()
                        .id(invocation.getArgument(0))
                        .routeId(invocation.getArgument(1))
                        .processId(invocation.getArgument(2))
                        .build());
    }

    @Test
    void updateRoute_shouldCreateDraftCandidateAndKeepActiveVersion() {
        Long routeId = 10L;
        MesProRouteDO existing = MesProRouteDO.builder()
                .id(routeId)
                .code("R-001")
                .name("old route")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        MesProRouteVersionDO oldVersion = MesProRouteVersionDO.builder()
                .id(100L)
                .routeId(routeId)
                .versionNo("R-001-V1")
                .active(Boolean.TRUE)
                .routeSnapshotJson("{\"name\":\"old route\"}")
                .build();
        MesProRouteSaveReqVO reqVO = new MesProRouteSaveReqVO();
        reqVO.setId(routeId);
        reqVO.setCode("R-001");
        reqVO.setName("new route");
        reqVO.setDescription("new description");

        when(routeMapper.selectById(routeId)).thenReturn(existing);
        when(routeMapper.selectByCode("R-001")).thenReturn(existing);
        when(routeVersionMapper.selectActiveByRouteId(routeId)).thenReturn(oldVersion);
        when(routeVersionMapper.selectMaxVersionNoByRouteId(routeId)).thenReturn("R-001-V1");
        doAnswer(invocation -> {
            MesProRouteVersionDO data = invocation.getArgument(0);
            data.setId(101L);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));

        routeService.updateRoute(reqVO);

        verify(routeMapper, never()).updateById(any(MesProRouteDO.class));
        verify(routeVersionMapper, never()).updateById(any(MesProRouteVersionDO.class));

        ArgumentCaptor<MesProRouteVersionDO> versionInsertCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).insert(versionInsertCaptor.capture());
        MesProRouteVersionDO newVersion = versionInsertCaptor.getValue();
        assertEquals(routeId, newVersion.getRouteId());
        assertEquals("R-001-V2", newVersion.getVersionNo());
        assertEquals(Boolean.FALSE, newVersion.getActive());
        assertEquals("DRAFT", newVersion.getLifecycleStatus());
        assertEquals(100L, newVersion.getSourceRouteVersionId());
        assertTrue(newVersion.getRouteSnapshotJson().contains("new route"));
    }

    @Test
    void createRoute_shouldRegisterInitialActiveVersionAsControlledContentActiveRef() {
        MesProRouteSaveReqVO reqVO = new MesProRouteSaveReqVO();
        reqVO.setCode("R-INITIAL-REF");
        reqVO.setName("initial active ref route");
        reqVO.setDescription("initial route");

        when(routeMapper.selectByCode("R-INITIAL-REF")).thenReturn(null);
        when(routeMapper.selectByName("initial active ref route")).thenReturn(null);
        doAnswer(invocation -> {
            MesProRouteDO data = invocation.getArgument(0);
            data.setId(28L);
            return 1;
        }).when(routeMapper).insert(any(MesProRouteDO.class));
        doAnswer(invocation -> {
            MesProRouteVersionDO data = invocation.getArgument(0);
            data.setId(480L);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));

        Long routeId = routeService.createRoute(reqVO);

        assertEquals(28L, routeId);
        verify(controlledContentAdapter).recordActiveRegistered(argThat(version ->
                        Long.valueOf(28L).equals(version.getRouteId())
                                && Long.valueOf(480L).equals(version.getId())
                                && "V1".equals(version.getVersionNo())
                                && Boolean.TRUE.equals(version.getActive())
                                && "ACTIVE".equals(version.getLifecycleStatus())),
                eq(null), eq("route active version registered"));
    }

    @Test
    void copyRoute_shouldCopyScheduleConfigsToIndependentNewConfigs() {
        Long sourceRouteId = 20L;
        Long targetRouteId = 21L;
        MesProRouteDO sourceRoute = MesProRouteDO.builder()
                .id(sourceRouteId)
                .code("R-COPY")
                .name("source")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        MesProRouteProcessDO sourceProcess = MesProRouteProcessDO.builder()
                .id(200L)
                .routeId(sourceRouteId)
                .processId(300L)
                .sort(1)
                .keyFlag(true)
                .build();
        MesProRouteVersionDO sourceVersion = MesProRouteVersionDO.builder()
                .id(400L)
                .routeId(sourceRouteId)
                .versionNo("R-COPY-V1")
                .active(Boolean.TRUE)
                .build();
        MesProRouteScheduleConfigDO sourceConfig = MesProRouteScheduleConfigDO.builder()
                .id(500L)
                .routeVersionId(400L)
                .routeProcessId(200L)
                .capacityMode("FINITE_HOURLY")
                .hourlyCapacity(new BigDecimal("12"))
                .nightShiftEnabled(Boolean.TRUE)
                .configVersion("CFG-V1")
                .build();

        when(routeMapper.selectById(sourceRouteId)).thenReturn(sourceRoute);
        when(routeMapper.selectByCode("R-COPY-2")).thenReturn(null);
        when(routeProcessMapper.selectListByRouteId(sourceRouteId)).thenReturn(List.of(sourceProcess));
        when(routeVersionMapper.selectActiveByRouteId(sourceRouteId)).thenReturn(sourceVersion);
        when(routeScheduleConfigMapper.selectListByRouteVersionId(400L)).thenReturn(List.of(sourceConfig));
        doAnswer(invocation -> {
            MesProRouteDO data = invocation.getArgument(0);
            data.setId(targetRouteId);
            return 1;
        }).when(routeMapper).insert(any(MesProRouteDO.class));
        doAnswer(invocation -> {
            MesProRouteProcessDO data = invocation.getArgument(0);
            data.setId(201L);
            return 1;
        }).when(routeProcessMapper).insert(any(MesProRouteProcessDO.class));
        doAnswer(invocation -> {
            MesProRouteVersionDO data = invocation.getArgument(0);
            data.setId(401L);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));

        Long copiedRouteId = routeService.copyRoute(sourceRouteId, "R-COPY-2", "copied");

        assertEquals(targetRouteId, copiedRouteId);
        ArgumentCaptor<MesProRouteScheduleConfigDO> configCaptor =
                ArgumentCaptor.forClass(MesProRouteScheduleConfigDO.class);
        verify(routeScheduleConfigMapper).insert(configCaptor.capture());
        MesProRouteScheduleConfigDO copiedConfig = configCaptor.getValue();
        assertNotEquals(500L, copiedConfig.getId());
        assertEquals(401L, copiedConfig.getRouteVersionId());
        assertNull(copiedConfig.getItemId());
        assertEquals(201L, copiedConfig.getRouteProcessId());
        assertEquals(500L, copiedConfig.getCopiedFromConfigId());
        assertEquals("MANUAL_OVERRIDE", copiedConfig.getCapacityMode());
        assertEquals(new BigDecimal("12"), copiedConfig.getHourlyCapacity());
        assertEquals(Boolean.TRUE, copiedConfig.getNightShiftEnabled());
    }

    @Test
    void copyRoute_shouldRegisterCopiedActiveVersionAsControlledContentActiveRef() {
        Long sourceRouteId = 26L;
        Long targetRouteId = 27L;
        MesProRouteDO sourceRoute = MesProRouteDO.builder()
                .id(sourceRouteId)
                .code("R-ACTIVE-REF")
                .name("source active ref route")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        MesProRouteVersionDO sourceVersion = MesProRouteVersionDO.builder()
                .id(460L)
                .routeId(sourceRouteId)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .build();

        when(routeMapper.selectById(sourceRouteId)).thenReturn(sourceRoute);
        when(routeMapper.selectByCode("R-ACTIVE-REF-COPY")).thenReturn(null);
        when(routeMapper.selectByName("copied active ref route")).thenReturn(null);
        when(routeProductMapper.selectListByRouteId(sourceRouteId)).thenReturn(List.of());
        when(routeProductBomMapper.selectList(sourceRouteId, null, null)).thenReturn(List.of());
        when(routeProcessMapper.selectListByRouteId(sourceRouteId)).thenReturn(List.of());
        when(routeVersionMapper.selectActiveByRouteId(sourceRouteId)).thenReturn(sourceVersion);
        when(routeScheduleConfigMapper.selectListByRouteVersionId(460L)).thenReturn(List.of());
        doAnswer(invocation -> {
            MesProRouteDO data = invocation.getArgument(0);
            data.setId(targetRouteId);
            return 1;
        }).when(routeMapper).insert(any(MesProRouteDO.class));
        doAnswer(invocation -> {
            MesProRouteVersionDO data = invocation.getArgument(0);
            data.setId(461L);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));

        Long copiedRouteId = routeService.copyRoute(sourceRouteId, "R-ACTIVE-REF-COPY",
                "copied active ref route");

        assertEquals(targetRouteId, copiedRouteId);
        verify(controlledContentAdapter).recordActiveRegistered(argThat(version ->
                        targetRouteId.equals(version.getRouteId())
                                && Long.valueOf(461L).equals(version.getId())
                                && "V1".equals(version.getVersionNo())
                                && Boolean.TRUE.equals(version.getActive())
                                && "ACTIVE".equals(version.getLifecycleStatus())),
                eq(null), eq("route active version registered"));
    }

    @Test
    void copyRoute_shouldFailWhenRouteProcessMissingCanonicalScheduleConfig() {
        Long sourceRouteId = 22L;
        MesProRouteDO sourceRoute = MesProRouteDO.builder()
                .id(sourceRouteId)
                .code("R-MISSING-CONFIG")
                .name("missing config")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        MesProRouteProcessDO sourceProcess = MesProRouteProcessDO.builder()
                .id(220L)
                .routeId(sourceRouteId)
                .processId(320L)
                .sort(1)
                .build();
        MesProRouteVersionDO sourceVersion = MesProRouteVersionDO.builder()
                .id(420L)
                .routeId(sourceRouteId)
                .versionNo("R-MISSING-CONFIG-V1")
                .active(Boolean.TRUE)
                .build();

        when(routeMapper.selectById(sourceRouteId)).thenReturn(sourceRoute);
        when(routeMapper.selectByCode("R-MISSING-CONFIG-2")).thenReturn(null);
        when(routeProcessMapper.selectListByRouteId(sourceRouteId)).thenReturn(List.of(sourceProcess));
        when(routeVersionMapper.selectActiveByRouteId(sourceRouteId)).thenReturn(sourceVersion);
        when(routeScheduleConfigMapper.selectListByRouteVersionId(420L)).thenReturn(List.of());
        doAnswer(invocation -> {
            MesProRouteDO data = invocation.getArgument(0);
            data.setId(23L);
            return 1;
        }).when(routeMapper).insert(any(MesProRouteDO.class));
        doAnswer(invocation -> {
            MesProRouteProcessDO data = invocation.getArgument(0);
            data.setId(221L);
            return 1;
        }).when(routeProcessMapper).insert(any(MesProRouteProcessDO.class));
        doAnswer(invocation -> {
            MesProRouteVersionDO data = invocation.getArgument(0);
            data.setId(421L);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> routeService.copyRoute(sourceRouteId, "R-MISSING-CONFIG-2", "copied"));

        assertEquals(1_040_271_013, ex.getCode());
    }

    @Test
    void copyRoute_shouldCopyScheduleUseConfigChain() {
        Long sourceRouteId = 24L;
        Long targetRouteId = 25L;
        MesProRouteDO sourceRoute = MesProRouteDO.builder()
                .id(sourceRouteId)
                .code("R-USE")
                .name("source use route")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        MesProRouteProcessDO sourceProcess = MesProRouteProcessDO.builder()
                .id(240L)
                .routeId(sourceRouteId)
                .processId(340L)
                .sort(1)
                .keyFlag(true)
                .build();
        MesProRouteFlowConfigDO sourceUseConfig = MesProRouteFlowConfigDO.builder()
                .id(241L)
                .routeId(sourceRouteId)
                .useType("SCHEDULE")
                .configVersion("ROUTE-USE-V1")
                .remark("schedule use config")
                .build();
        MesProRouteFlowProcessConfigDO sourceUseProcessConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(242L)
                .routeFlowConfigId(241L)
                .routeId(sourceRouteId)
                .routeProcessId(240L)
                .useType("SCHEDULE")
                .enabled(Boolean.TRUE)
                .remark("schedule process config")
                .build();
        MesProRouteFlowProcessConfigDO staleProcessConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(244L)
                .routeFlowConfigId(999L)
                .routeId(sourceRouteId)
                .routeProcessId(999L)
                .useType("SCHEDULE")
                .enabled(Boolean.TRUE)
                .remark("stale process config")
                .build();
        MesProRouteFlowProcessBatchRecordDO sourceBatchRecord = MesProRouteFlowProcessBatchRecordDO.builder()
                .id(243L)
                .routeFlowProcessConfigId(242L)
                .routeId(sourceRouteId)
                .routeProcessId(240L)
                .useType("SCHEDULE")
                .batchRecordReportId("REPORT-1")
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .reportSort(1)
                .remark("batch record binding")
                .build();

        when(routeMapper.selectById(sourceRouteId)).thenReturn(sourceRoute);
        when(routeMapper.selectByCode("R-USE-COPY")).thenReturn(null);
        when(routeProcessMapper.selectListByRouteId(sourceRouteId)).thenReturn(List.of(sourceProcess));
        when(routeVersionMapper.selectActiveByRouteId(sourceRouteId)).thenReturn(null);
        doAnswer(invocation -> List.of(sourceUseConfig))
                .when(routeFlowConfigMapper).selectList((SFunction<MesProRouteFlowConfigDO, ?>) any(), anyLong());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(sourceRouteId, "SCHEDULE"))
                .thenReturn(List.of(sourceUseProcessConfig, staleProcessConfig));
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(sourceRouteId, "SCHEDULE"))
                .thenReturn(List.of(sourceBatchRecord));
        doAnswer(invocation -> {
            MesProRouteDO data = invocation.getArgument(0);
            data.setId(targetRouteId);
            return 1;
        }).when(routeMapper).insert(any(MesProRouteDO.class));
        doAnswer(invocation -> {
            MesProRouteProcessDO data = invocation.getArgument(0);
            data.setId(250L);
            return 1;
        }).when(routeProcessMapper).insert(any(MesProRouteProcessDO.class));
        doAnswer(invocation -> {
            MesProRouteFlowConfigDO data = invocation.getArgument(0);
            data.setId(251L);
            return 1;
        }).when(routeFlowConfigMapper).insert(any(MesProRouteFlowConfigDO.class));
        doAnswer(invocation -> {
            MesProRouteFlowProcessConfigDO data = invocation.getArgument(0);
            data.setId(252L);
            return 1;
        }).when(routeFlowProcessConfigMapper).insert(any(MesProRouteFlowProcessConfigDO.class));

        Long copiedRouteId = routeService.copyRoute(sourceRouteId, "R-USE-COPY", "copied use route");

        assertEquals(targetRouteId, copiedRouteId);

        ArgumentCaptor<MesProRouteFlowConfigDO> useConfigCaptor =
                ArgumentCaptor.forClass(MesProRouteFlowConfigDO.class);
        verify(routeFlowConfigMapper).insert(useConfigCaptor.capture());
        assertEquals(targetRouteId, useConfigCaptor.getValue().getRouteId());
        assertEquals("SCHEDULE", useConfigCaptor.getValue().getUseType());
        assertEquals("ROUTE-USE-V1", useConfigCaptor.getValue().getConfigVersion());

        ArgumentCaptor<MesProRouteFlowProcessConfigDO> useProcessConfigCaptor =
                ArgumentCaptor.forClass(MesProRouteFlowProcessConfigDO.class);
        verify(routeFlowProcessConfigMapper).insert(useProcessConfigCaptor.capture());
        assertEquals(251L, useProcessConfigCaptor.getValue().getRouteFlowConfigId());
        assertEquals(targetRouteId, useProcessConfigCaptor.getValue().getRouteId());
        assertEquals(250L, useProcessConfigCaptor.getValue().getRouteProcessId());
        assertEquals("SCHEDULE", useProcessConfigCaptor.getValue().getUseType());

        ArgumentCaptor<MesProRouteFlowProcessBatchRecordDO> batchRecordCaptor =
                ArgumentCaptor.forClass(MesProRouteFlowProcessBatchRecordDO.class);
        verify(routeFlowProcessBatchRecordMapper).insert(batchRecordCaptor.capture());
        assertEquals(252L, batchRecordCaptor.getValue().getRouteFlowProcessConfigId());
        assertEquals(targetRouteId, batchRecordCaptor.getValue().getRouteId());
        assertEquals(250L, batchRecordCaptor.getValue().getRouteProcessId());
        assertEquals("REPORT-1", batchRecordCaptor.getValue().getBatchRecordReportId());
    }

    @Test
    void copyRoute_shouldCopyProductAndProductBomBindingsToTargetRoute() {
        Long sourceRouteId = 22L;
        Long targetRouteId = 23L;
        MesProRouteDO sourceRoute = MesProRouteDO.builder()
                .id(sourceRouteId)
                .code("R-PRODUCT")
                .name("source product route")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        MesProRouteProductDO sourceProduct = MesProRouteProductDO.builder()
                .id(600L)
                .routeId(sourceRouteId)
                .itemId(700L)
                .quantity(10)
                .productionTime(new BigDecimal("2.5"))
                .timeUnitType("HOUR")
                .remark("source product binding")
                .build();
        MesProRouteProductBomDO sourceProductBom = MesProRouteProductBomDO.builder()
                .id(601L)
                .routeId(sourceRouteId)
                .processId(800L)
                .productId(700L)
                .itemId(900L)
                .quantity(new BigDecimal("1.25"))
                .remark("source bom binding")
                .build();

        when(routeMapper.selectById(sourceRouteId)).thenReturn(sourceRoute);
        when(routeMapper.selectByCode("R-PRODUCT-COPY")).thenReturn(null);
        when(routeProcessMapper.selectListByRouteId(sourceRouteId)).thenReturn(List.of());
        when(routeVersionMapper.selectActiveByRouteId(sourceRouteId)).thenReturn(null);
        when(routeProductMapper.selectListByRouteId(sourceRouteId)).thenReturn(List.of(sourceProduct));
        when(routeProductBomMapper.selectList(sourceRouteId, null, null)).thenReturn(List.of(sourceProductBom));
        doAnswer(invocation -> {
            MesProRouteDO data = invocation.getArgument(0);
            data.setId(targetRouteId);
            return 1;
        }).when(routeMapper).insert(any(MesProRouteDO.class));

        Long copiedRouteId = routeService.copyRoute(sourceRouteId, "R-PRODUCT-COPY", "copied product route");

        assertEquals(targetRouteId, copiedRouteId);
        ArgumentCaptor<MesProRouteProductDO> productCaptor = ArgumentCaptor.forClass(MesProRouteProductDO.class);
        verify(routeProductMapper).insert(productCaptor.capture());
        MesProRouteProductDO copiedProduct = productCaptor.getValue();
        assertNotEquals(600L, copiedProduct.getId());
        assertEquals(targetRouteId, copiedProduct.getRouteId());
        assertEquals(700L, copiedProduct.getItemId());
        assertEquals(10, copiedProduct.getQuantity());
        assertEquals(new BigDecimal("2.5"), copiedProduct.getProductionTime());
        assertEquals("HOUR", copiedProduct.getTimeUnitType());
        assertEquals("source product binding", copiedProduct.getRemark());

        ArgumentCaptor<MesProRouteProductBomDO> productBomCaptor =
                ArgumentCaptor.forClass(MesProRouteProductBomDO.class);
        verify(routeProductBomMapper).insert(productBomCaptor.capture());
        MesProRouteProductBomDO copiedProductBom = productBomCaptor.getValue();
        assertNotEquals(601L, copiedProductBom.getId());
        assertEquals(targetRouteId, copiedProductBom.getRouteId());
        assertEquals(800L, copiedProductBom.getProcessId());
        assertEquals(700L, copiedProductBom.getProductId());
        assertEquals(900L, copiedProductBom.getItemId());
        assertEquals(new BigDecimal("1.25"), copiedProductBom.getQuantity());
        assertEquals("source bom binding", copiedProductBom.getRemark());
    }

    @Test
    void copyRoute_shouldCopyFlowGraphWithMappedRouteProcessIds() {
        Long sourceRouteId = 40L;
        Long targetRouteId = 41L;
        MesProRouteDO sourceRoute = MesProRouteDO.builder()
                .id(sourceRouteId)
                .code("R-FLOW-COPY")
                .name("flow source")
                .status(CommonStatusEnum.DISABLE.getStatus())
                .build();
        MesProRouteProcessDO sourceStart = MesProRouteProcessDO.builder()
                .id(401L)
                .routeId(sourceRouteId)
                .processId(4001L)
                .sort(1)
                .keyFlag(true)
                .build();
        MesProRouteProcessDO sourceEnd = MesProRouteProcessDO.builder()
                .id(402L)
                .routeId(sourceRouteId)
                .processId(4002L)
                .sort(2)
                .keyFlag(false)
                .build();

        when(routeMapper.selectById(sourceRouteId)).thenReturn(sourceRoute);
        when(routeMapper.selectByCode("R-FLOW-COPY-2")).thenReturn(null);
        when(routeProcessMapper.selectListByRouteId(sourceRouteId)).thenReturn(List.of(sourceStart, sourceEnd));
        when(routeVersionMapper.selectActiveByRouteId(sourceRouteId)).thenReturn(null);
        doAnswer(invocation -> {
            MesProRouteDO data = invocation.getArgument(0);
            data.setId(targetRouteId);
            return 1;
        }).when(routeMapper).insert(any(MesProRouteDO.class));
        doAnswer(invocation -> {
            MesProRouteProcessDO data = invocation.getArgument(0);
            data.setId(data.getProcessId().equals(4001L) ? 411L : 412L);
            return 1;
        }).when(routeProcessMapper).insert(any(MesProRouteProcessDO.class));
        doAnswer(invocation -> {
            MesProRouteVersionDO data = invocation.getArgument(0);
            data.setId(4100L);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));

        Long copiedRouteId = routeService.copyRoute(sourceRouteId, "R-FLOW-COPY-2", "flow copied");

        assertEquals(targetRouteId, copiedRouteId);
        verify(routeProcessFlowService).copyGraph(eq(sourceRouteId), eq(targetRouteId),
                argThat((Map<Long, Long> idMap) -> idMap.size() == 2
                        && idMap.get(401L).equals(411L)
                        && idMap.get(402L).equals(412L)));
    }

    @Test
    void deleteRoute_shouldDeleteFlowGraphWithRouteCascade() {
        Long routeId = 50L;
        MesProRouteDO route = MesProRouteDO.builder()
                .id(routeId)
                .code("R-FLOW-DELETE")
                .name("delete flow")
                .status(CommonStatusEnum.DISABLE.getStatus())
                .build();
        when(routeMapper.selectById(routeId)).thenReturn(route);

        routeService.deleteRoute(routeId);

        verify(routeProcessFlowService).deleteByRouteId(routeId);
        verify(routeProcessService).deleteRouteProcessByRouteId(routeId);
        verify(routeProductService).deleteRouteProductByRouteId(routeId);
        verify(routeProductBomService).deleteRouteProductBomByRouteId(routeId);
        verify(routeMapper).deleteById(routeId);
    }

    @Test
    void maintainRouteVersionAfterProcessChange_shouldCreateDraftCandidateAndCopyExistingProcessConfigs() {
        Long routeId = 30L;
        MesProRouteDO route = MesProRouteDO.builder()
                .id(routeId)
                .code("R-PROC")
                .name("process route")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        MesProRouteVersionDO oldVersion = MesProRouteVersionDO.builder()
                .id(600L)
                .routeId(routeId)
                .versionNo("R-PROC-V1")
                .active(Boolean.TRUE)
                .build();
        MesProRouteProcessDO existingProcess = MesProRouteProcessDO.builder()
                .id(700L)
                .routeId(routeId)
                .processId(800L)
                .sort(1)
                .build();
        MesProRouteScheduleConfigDO keptConfig = MesProRouteScheduleConfigDO.builder()
                .id(900L)
                .routeVersionId(600L)
                .routeProcessId(700L)
                .capacityMode("FINITE_HOURLY")
                .hourlyCapacity(new BigDecimal("16"))
                .build();
        MesProRouteScheduleConfigDO deletedProcessConfig = MesProRouteScheduleConfigDO.builder()
                .id(901L)
                .routeVersionId(600L)
                .routeProcessId(701L)
                .capacityMode("FINITE_HOURLY")
                .hourlyCapacity(new BigDecimal("8"))
                .build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeVersionMapper.selectActiveByRouteId(routeId)).thenReturn(oldVersion);
        when(routeVersionMapper.selectMaxVersionNoByRouteId(routeId)).thenReturn("R-PROC-V1");
        when(routeProcessMapper.selectListByRouteId(routeId)).thenReturn(List.of(existingProcess));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(600L))
                .thenReturn(List.of(keptConfig, deletedProcessConfig));
        doAnswer(invocation -> {
            MesProRouteVersionDO data = invocation.getArgument(0);
            data.setId(601L);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));

        routeService.maintainRouteVersionAfterProcessChange(routeId);

        verify(routeVersionMapper, never()).updateById(any(MesProRouteVersionDO.class));

        ArgumentCaptor<MesProRouteVersionDO> versionInsertCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).insert(versionInsertCaptor.capture());
        assertEquals("R-PROC-V2", versionInsertCaptor.getValue().getVersionNo());
        assertEquals(Boolean.FALSE, versionInsertCaptor.getValue().getActive());
        assertEquals("DRAFT", versionInsertCaptor.getValue().getLifecycleStatus());
        assertEquals(600L, versionInsertCaptor.getValue().getSourceRouteVersionId());

        ArgumentCaptor<MesProRouteScheduleConfigDO> configCaptor =
                ArgumentCaptor.forClass(MesProRouteScheduleConfigDO.class);
        verify(routeScheduleConfigMapper).insert(configCaptor.capture());
        MesProRouteScheduleConfigDO copiedConfig = configCaptor.getValue();
        assertEquals(601L, copiedConfig.getRouteVersionId());
        assertEquals(700L, copiedConfig.getRouteProcessId());
        assertEquals(900L, copiedConfig.getCopiedFromConfigId());
        assertEquals("MANUAL_OVERRIDE", copiedConfig.getCapacityMode());
        assertEquals(new BigDecimal("16"), copiedConfig.getHourlyCapacity());
    }

    @Test
    void maintainRouteVersionAfterProcessChange_shouldCreateDraftCandidateWhenOpen() {
        Long routeId = 31L;
        MesProRouteDO route = MesProRouteDO.builder()
                .id(routeId)
                .code("R-PROC-OPEN")
                .name("open route")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        MesProRouteVersionDO oldVersion = MesProRouteVersionDO.builder()
                .id(610L)
                .routeId(routeId)
                .versionNo("R-PROC-OPEN-V1")
                .active(Boolean.TRUE)
                .build();
        MesProRouteProcessDO existingProcess = MesProRouteProcessDO.builder()
                .id(710L)
                .routeId(routeId)
                .processId(810L)
                .sort(1)
                .build();
        MesProRouteScheduleConfigDO keptConfig = MesProRouteScheduleConfigDO.builder()
                .id(910L)
                .routeVersionId(610L)
                .routeProcessId(710L)
                .capacityMode("FINITE_HOURLY")
                .hourlyCapacity(new BigDecimal("20"))
                .build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeVersionMapper.selectActiveByRouteId(routeId)).thenReturn(oldVersion);
        when(routeVersionMapper.selectMaxVersionNoByRouteId(routeId)).thenReturn("R-PROC-OPEN-V1");
        when(routeProcessMapper.selectListByRouteId(routeId)).thenReturn(List.of(existingProcess));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(610L)).thenReturn(List.of(keptConfig));
        doAnswer(invocation -> {
            MesProRouteVersionDO data = invocation.getArgument(0);
            data.setId(611L);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));

        routeService.maintainRouteVersionAfterProcessChange(routeId);

        verify(routeVersionMapper, never()).updateById(any(MesProRouteVersionDO.class));

        ArgumentCaptor<MesProRouteVersionDO> versionInsertCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).insert(versionInsertCaptor.capture());
        assertEquals("R-PROC-OPEN-V2", versionInsertCaptor.getValue().getVersionNo());
        assertEquals(Boolean.FALSE, versionInsertCaptor.getValue().getActive());
        assertEquals("DRAFT", versionInsertCaptor.getValue().getLifecycleStatus());
        assertEquals(610L, versionInsertCaptor.getValue().getSourceRouteVersionId());

        ArgumentCaptor<MesProRouteScheduleConfigDO> configCaptor =
                ArgumentCaptor.forClass(MesProRouteScheduleConfigDO.class);
        verify(routeScheduleConfigMapper).insert(configCaptor.capture());
        assertEquals(611L, configCaptor.getValue().getRouteVersionId());
        assertEquals(910L, configCaptor.getValue().getCopiedFromConfigId());
        assertEquals("MANUAL_OVERRIDE", configCaptor.getValue().getCapacityMode());
    }

    @Test
    void maintainRouteVersionAfterProcessChange_shouldReuseExistingDraftCandidateAndRefreshSnapshot() {
        Long routeId = 32L;
        MesProRouteDO route = MesProRouteDO.builder()
                .id(routeId)
                .code("R-PROC-REUSE")
                .name("reuse candidate route")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        MesProRouteVersionDO activeVersion = MesProRouteVersionDO.builder()
                .id(620L)
                .routeId(routeId)
                .versionNo("R-PROC-REUSE-V1")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .build();
        MesProRouteVersionDO draftCandidate = MesProRouteVersionDO.builder()
                .id(621L)
                .routeId(routeId)
                .versionNo("R-PROC-REUSE-V2")
                .active(Boolean.FALSE)
                .lifecycleStatus("DRAFT")
                .sourceRouteVersionId(activeVersion.getId())
                .build();
        MesProRouteProcessDO existingProcess = MesProRouteProcessDO.builder()
                .id(720L)
                .routeId(routeId)
                .processId(820L)
                .sort(1)
                .build();
        MesProRouteScheduleConfigDO activeConfig = MesProRouteScheduleConfigDO.builder()
                .id(920L)
                .routeVersionId(activeVersion.getId())
                .routeProcessId(existingProcess.getId())
                .capacityMode("FINITE_HOURLY")
                .hourlyCapacity(new BigDecimal("20"))
                .build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeVersionMapper.selectActiveByRouteId(routeId)).thenReturn(activeVersion);
        when(routeVersionMapper.selectOpenCandidateByRouteId(routeId)).thenReturn(draftCandidate);
        when(routeProcessMapper.selectListByRouteId(routeId)).thenReturn(List.of(existingProcess));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(activeVersion.getId()))
                .thenReturn(List.of(activeConfig));
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(
                draftCandidate.getId(), existingProcess.getId())).thenReturn(null);

        routeService.maintainRouteVersionAfterProcessChange(routeId);

        verify(routeVersionMapper, never()).insert(any(MesProRouteVersionDO.class));
        verify(controlledContentAdapter, never()).recordCandidateCreated(any(), any(), any(), any());

        ArgumentCaptor<MesProRouteVersionDO> versionUpdateCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(versionUpdateCaptor.capture());
        assertEquals(draftCandidate.getId(), versionUpdateCaptor.getValue().getId());
        assertTrue(versionUpdateCaptor.getValue().getRouteSnapshotJson().contains("R-PROC-REUSE"));

        ArgumentCaptor<MesProRouteScheduleConfigDO> configCaptor =
                ArgumentCaptor.forClass(MesProRouteScheduleConfigDO.class);
        verify(routeScheduleConfigMapper).insert(configCaptor.capture());
        assertEquals(draftCandidate.getId(), configCaptor.getValue().getRouteVersionId());
        assertEquals(activeConfig.getId(), configCaptor.getValue().getCopiedFromConfigId());
        assertEquals("MANUAL_OVERRIDE", configCaptor.getValue().getCapacityMode());
    }
}
