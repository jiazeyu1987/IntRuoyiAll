package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.util.AssertUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowGraphRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flow.MesProRouteProcessFlowNodeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchPolicySettingsRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleCalendarRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteScheduleConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleCalendarRuleMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionRuleCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeSaveCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeService;
import cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench.MesProSchedulerWorkbenchService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteServiceImplTest {

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
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProRouteProcessFlowService routeProcessFlowService;
    @Mock
    private MesProRouteProductService routeProductService;
    @Mock
    private MesProRouteProductBomService routeProductBomService;
    @Mock
    private MesMdItemService itemService;
    @Mock
    private MesProSchedulerWorkbenchService schedulerWorkbenchService;
    @Mock
    private MesProScheduleCalendarRuleMapper scheduleCalendarRuleMapper;
    @Mock
    private MesProRouteControlledContentAdapter platformAdapter;
    @Mock
    private MesProEdhrPermissionScopeService permissionScopeService;

    @BeforeEach
    void stubCurrentRouteProcessIdentity() {
        ReflectionTestUtils.setField(routeService, "routeOwnerPermissionService",
                new MesProRouteOwnerPermissionServiceImpl(permissionScopeService));
        lenient().when(routeProcessService.resolveCurrentRouteProcess(
                        anyLong(), nullable(Long.class), nullable(Long.class)))
                .thenAnswer(invocation -> MesProRouteProcessDO.builder()
                        .id(invocation.getArgument(0))
                        .routeId(invocation.getArgument(1))
                        .processId(invocation.getArgument(2))
                        .build());
        lenient().when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(anyLong(), any()))
                .thenReturn(emptyList());
    }

    @Test
    void createRoute_shouldRejectDuplicateNameAndSkipCreate() {
        MesProRouteSaveReqVO reqVO = new MesProRouteSaveReqVO();
        reqVO.setCode("ROUTE-DUP-NAME");
        reqVO.setName("重复工艺路线");

        when(routeMapper.selectByCode(reqVO.getCode())).thenReturn(null);
        when(routeMapper.selectByName(reqVO.getName())).thenReturn(MesProRouteDO.builder()
                .id(9001L)
                .code("ROUTE-EXISTING")
                .name(reqVO.getName())
                .build());

        AssertUtils.assertServiceException(
                () -> routeService.createRoute(reqVO),
                ErrorCodeConstants.PRO_ROUTE_NAME_DUPLICATE
        );

        verify(routeMapper, never()).insert(any(MesProRouteDO.class));
        verify(routeVersionMapper, never()).insert(any(cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO.class));
    }

    @Test
    void createRoute_shouldRegisterInitialActiveVersionRef() {
        MesProRouteSaveReqVO reqVO = new MesProRouteSaveReqVO();
        reqVO.setCode("ROUTE-NEW-ACTIVE-REF");
        reqVO.setName("新建路线");
        reqVO.setDescription("新建路线说明");

        when(routeMapper.selectByCode(reqVO.getCode())).thenReturn(null);
        when(routeMapper.selectByName(reqVO.getName())).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteDO route = invocation.getArgument(0);
            route.setId(9008L);
            return 1;
        }).when(routeMapper).insert(any(MesProRouteDO.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteVersionDO activeVersion = invocation.getArgument(0);
            activeVersion.setId(9208L);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));

        Long routeId = routeService.createRoute(reqVO);

        assertEquals(9008L, routeId);
        ArgumentCaptor<MesProRouteVersionDO> activeCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(platformAdapter).recordActiveRegistered(activeCaptor.capture(), eq(null),
                eq("route active version registered"));
        MesProRouteVersionDO activeVersion = activeCaptor.getValue();
        assertEquals(9208L, activeVersion.getId());
        assertEquals(9008L, activeVersion.getRouteId());
        assertEquals("V1", activeVersion.getVersionNo());
        assertEquals(Boolean.TRUE, activeVersion.getActive());
        assertEquals("ACTIVE", activeVersion.getLifecycleStatus());
    }

    @Test
    void createRoute_shouldGrantRouteEditPermissionToCreator() {
        MesProRouteSaveReqVO reqVO = new MesProRouteSaveReqVO();
        reqVO.setCode("ROUTE-NEW-PERM");
        reqVO.setName("新建权限路线");
        Long routeId = 922119L;
        Long creatorUserId = 1L;

        when(routeMapper.selectByCode(reqVO.getCode())).thenReturn(null);
        when(routeMapper.selectByName(reqVO.getName())).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteDO route = invocation.getArgument(0);
            route.setId(routeId);
            return 1;
        }).when(routeMapper).insert(any(MesProRouteDO.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteVersionDO activeVersion = invocation.getArgument(0);
            activeVersion.setId(922120L);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(creatorUserId);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("admin");

            routeService.createRoute(reqVO);
        }

        ArgumentCaptor<MesProEdhrPermissionScopeSaveCommand> scopeCaptor =
                ArgumentCaptor.forClass(MesProEdhrPermissionScopeSaveCommand.class);
        verify(permissionScopeService).saveRules(scopeCaptor.capture());
        MesProEdhrPermissionScopeSaveCommand command = scopeCaptor.getValue();
        assertEquals("route-" + routeId, command.getScopeName());
        assertEquals("ROUTE", command.getObjectType());
        assertEquals(String.valueOf(routeId), command.getObjectId());
        assertEquals(creatorUserId, command.getActorUserId());
        assertEquals("admin", command.getActorUsername());
        assertEquals(List.of("VIEW", "ROUTE_EDIT", "PERMISSION_ADMIN"),
                command.getRules().stream().map(MesProEdhrPermissionRuleCommand::getAbility).toList());
        command.getRules().forEach(rule -> {
            assertEquals("USER", rule.getSubjectType());
            assertEquals(creatorUserId, rule.getSubjectId());
            assertEquals("ALLOW", rule.getDecision());
            assertEquals("ENABLED", rule.getStatus());
        });
    }

    @Test
    void updateRoute_shouldRejectDuplicateNameAndSkipUpdate() {
        MesProRouteSaveReqVO reqVO = new MesProRouteSaveReqVO();
        reqVO.setId(9002L);
        reqVO.setCode("ROUTE-UPDATE");
        reqVO.setName("重复工艺路线");

        when(routeMapper.selectById(reqVO.getId())).thenReturn(MesProRouteDO.builder()
                .id(reqVO.getId())
                .code(reqVO.getCode())
                .name("原工艺路线")
                .build());
        when(routeMapper.selectByName(reqVO.getName())).thenReturn(MesProRouteDO.builder()
                .id(9003L)
                .code("ROUTE-EXISTING")
                .name(reqVO.getName())
                .build());

        AssertUtils.assertServiceException(
                () -> routeService.updateRoute(reqVO),
                ErrorCodeConstants.PRO_ROUTE_NAME_DUPLICATE
        );

        verify(routeMapper, never()).updateById(any(MesProRouteDO.class));
        verify(routeVersionMapper, never()).insert(any(cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO.class));
    }

    @Test
    void updateRoute_shouldCreateDraftCandidateVersionWithoutInactivatingActiveVersion() {
        MesProRouteSaveReqVO reqVO = new MesProRouteSaveReqVO();
        reqVO.setId(9101L);
        reqVO.setCode("ROUTE-CANDIDATE");
        reqVO.setName("候选路线");
        reqVO.setDescription("候选变更");

        MesProRouteDO oldRoute = MesProRouteDO.builder()
                .id(reqVO.getId())
                .code(reqVO.getCode())
                .name("原路线")
                .description("原说明")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        MesProRouteVersionDO activeVersion = MesProRouteVersionDO.builder()
                .id(9201L)
                .routeId(reqVO.getId())
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .build();

        when(routeMapper.selectById(reqVO.getId())).thenReturn(oldRoute);
        when(routeMapper.selectByCode(reqVO.getCode())).thenReturn(oldRoute);
        when(routeMapper.selectByName(reqVO.getName())).thenReturn(null);
        when(routeVersionMapper.selectActiveByRouteId(reqVO.getId())).thenReturn(activeVersion);
        when(routeVersionMapper.selectMaxVersionNoByRouteId(reqVO.getId())).thenReturn("V1");

        routeService.updateRoute(reqVO);

        verify(routeMapper, never()).updateById(any(MesProRouteDO.class));
        verify(routeVersionMapper, never()).updateById(any(MesProRouteVersionDO.class));
        ArgumentCaptor<MesProRouteVersionDO> versionCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).insert(versionCaptor.capture());
        MesProRouteVersionDO candidate = versionCaptor.getValue();
        assertEquals(reqVO.getId(), candidate.getRouteId());
        assertEquals("V2", candidate.getVersionNo());
        assertEquals(Boolean.FALSE, candidate.getActive());
        assertEquals("DRAFT", candidate.getLifecycleStatus());
        assertEquals(activeVersion.getId(), candidate.getSourceRouteVersionId());
        JSONObject snapshot = JSON.parseObject(candidate.getRouteSnapshotJson());
        assertEquals(reqVO.getId(), snapshot.getLong("routeId"));
        assertEquals(reqVO.getCode(), snapshot.getString("routeCode"));
        assertEquals(reqVO.getName(), snapshot.getString("routeName"));
        verify(platformAdapter).recordCandidateCreated(activeVersion, candidate, null,
                "route version candidate created");
    }

    @Test
    void updateRoute_shouldCarryActiveConfigSnapshotIntoDraftCandidate() {
        MesProRouteSaveReqVO reqVO = new MesProRouteSaveReqVO();
        reqVO.setId(9102L);
        reqVO.setCode("ROUTE-CANDIDATE-FULL");
        reqVO.setName("候选路线改名");
        reqVO.setDescription("候选变更");

        MesProRouteDO oldRoute = MesProRouteDO.builder()
                .id(reqVO.getId())
                .code(reqVO.getCode())
                .name("原路线")
                .description("原说明")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        MesProRouteVersionDO activeVersion = MesProRouteVersionDO.builder()
                .id(9202L)
                .routeId(reqVO.getId())
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .routeSnapshotJson(validCompleteRouteSnapshotJson(reqVO.getId(), reqVO.getCode(), "原路线"))
                .build();

        when(routeMapper.selectById(reqVO.getId())).thenReturn(oldRoute);
        when(routeMapper.selectByCode(reqVO.getCode())).thenReturn(oldRoute);
        when(routeMapper.selectByName(reqVO.getName())).thenReturn(null);
        when(routeVersionMapper.selectActiveByRouteId(reqVO.getId())).thenReturn(activeVersion);
        when(routeVersionMapper.selectMaxVersionNoByRouteId(reqVO.getId())).thenReturn("V1");

        routeService.updateRoute(reqVO);

        ArgumentCaptor<MesProRouteVersionDO> versionCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).insert(versionCaptor.capture());
        MesProRouteVersionDO candidate = versionCaptor.getValue();
        assertTrue(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(candidate.getRouteSnapshotJson()));
        JSONObject snapshot = JSON.parseObject(candidate.getRouteSnapshotJson());
        assertEquals(reqVO.getName(), snapshot.getString("routeName"));
        assertTrue(snapshot.getJSONObject("configSnapshots").containsKey("flowGraph"));
        assertTrue(snapshot.getJSONObject("configSnapshots").containsKey("scheduleUseConfigs"));
    }

    @Test
    void copyRoute_shouldRejectDuplicateNameAndSkipCreate() {
        Long sourceRouteId = 9004L;
        String targetCode = "ROUTE-COPY";
        String targetName = "重复工艺路线";

        when(routeMapper.selectById(sourceRouteId)).thenReturn(MesProRouteDO.builder()
                .id(sourceRouteId)
                .code("ROUTE-SOURCE")
                .name("源工艺路线")
                .build());
        when(routeMapper.selectByName(targetName)).thenReturn(MesProRouteDO.builder()
                .id(9005L)
                .code("ROUTE-EXISTING")
                .name(targetName)
                .build());

        AssertUtils.assertServiceException(
                () -> routeService.copyRoute(sourceRouteId, targetCode, targetName),
                ErrorCodeConstants.PRO_ROUTE_NAME_DUPLICATE
        );

        verify(routeMapper, never()).insert(any(MesProRouteDO.class));
        verify(routeVersionMapper, never()).insert(any(cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO.class));
    }

    @Test
    void copyRoute_shouldRefreshActiveVersionWithCompleteConfigSnapshotAfterChildConfigsCopied() {
        Long sourceRouteId = 9006L;
        Long targetRouteId = 9007L;
        Long sourceVersionId = 9206L;
        Long targetVersionId = 9207L;
        String targetCode = "ROUTE-COPY-FULL";
        String targetName = "复制完整快照路线";
        MesProRouteDO sourceRoute = MesProRouteDO.builder()
                .id(sourceRouteId)
                .code("ROUTE-SOURCE-FULL")
                .name("源完整路线")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        MesProRouteProcessDO sourceProcess = MesProRouteProcessDO.builder()
                .id(9306L)
                .routeId(sourceRouteId)
                .processId(9406L)
                .sort(1)
                .build();
        MesProRouteScheduleConfigDO sourceScheduleConfig = MesProRouteScheduleConfigDO.builder()
                .id(9506L)
                .routeVersionId(sourceVersionId)
                .routeProcessId(sourceProcess.getId())
                .capacityMode(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode())
                .hourlyCapacity(new BigDecimal("12"))
                .build();
        MesProRouteScheduleConfigDO targetScheduleConfig = MesProRouteScheduleConfigDO.builder()
                .id(9507L)
                .routeVersionId(targetVersionId)
                .routeProcessId(9307L)
                .capacityMode(MesProScheduleCapacityModeEnum.MANUAL_OVERRIDE.getMode())
                .hourlyCapacity(new BigDecimal("12"))
                .build();
        MesProRouteFlowProcessConfigDO targetScheduleUseConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(9607L)
                .routeId(targetRouteId)
                .routeProcessId(9307L)
                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                .enabled(Boolean.TRUE)
                .build();
        MesProRouteFlowProcessConfigDO targetBatchUseConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(9608L)
                .routeId(targetRouteId)
                .routeProcessId(9307L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .build();

        when(routeMapper.selectById(sourceRouteId)).thenReturn(sourceRoute);
        when(routeMapper.selectByCode(targetCode)).thenReturn(null);
        when(routeMapper.selectByName(targetName)).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteDO targetRoute = invocation.getArgument(0);
            targetRoute.setId(targetRouteId);
            return 1;
        }).when(routeMapper).insert(any(MesProRouteDO.class));
        when(routeProductMapper.selectListByRouteId(sourceRouteId)).thenReturn(List.of(MesProRouteProductDO.builder()
                .id(9706L)
                .routeId(sourceRouteId)
                .itemId(9806L)
                .quantity(1)
                .build()));
        when(routeProductMapper.selectListByRouteId(targetRouteId)).thenReturn(List.of(MesProRouteProductDO.builder()
                .id(9707L)
                .routeId(targetRouteId)
                .itemId(9806L)
                .quantity(1)
                .build()));
        when(routeProductBomMapper.selectList(sourceRouteId, null, null)).thenReturn(List.of());
        when(routeProcessMapper.selectListByRouteId(sourceRouteId)).thenReturn(List.of(sourceProcess));
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteProcessDO targetProcess = invocation.getArgument(0);
            targetProcess.setId(9307L);
            return 1;
        }).when(routeProcessMapper).insert(any(MesProRouteProcessDO.class));
        when(routeVersionMapper.selectActiveByRouteId(sourceRouteId)).thenReturn(MesProRouteVersionDO.builder()
                .id(sourceVersionId)
                .routeId(sourceRouteId)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .build());
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteVersionDO targetVersion = invocation.getArgument(0);
            targetVersion.setId(targetVersionId);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(sourceVersionId))
                .thenReturn(List.of(sourceScheduleConfig));
        when(routeScheduleConfigMapper.selectListByRouteVersionId(targetVersionId))
                .thenReturn(List.of(targetScheduleConfig));
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                targetRouteId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(targetScheduleUseConfig));
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                targetRouteId, MesProRouteFlowConfigTypeEnum.BATCH.getType()))
                .thenReturn(List.of(targetBatchUseConfig));
        MesProRouteProcessFlowGraphRespVO graph = new MesProRouteProcessFlowGraphRespVO();
        graph.setRouteId(targetRouteId);
        MesProRouteProcessFlowNodeRespVO node = new MesProRouteProcessFlowNodeRespVO();
        node.setRouteProcessId(9307L);
        node.setProcessId(9406L);
        node.setProcessName("工序");
        node.setSort(1);
        graph.setNodes(List.of(node));
        when(routeProcessFlowService.getGraph(targetRouteId)).thenReturn(graph);

        Long copiedRouteId = routeService.copyRoute(sourceRouteId, targetCode, targetName);

        assertEquals(targetRouteId, copiedRouteId);
        ArgumentCaptor<MesProRouteVersionDO> updateCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(updateCaptor.capture());
        MesProRouteVersionDO update = updateCaptor.getValue();
        assertEquals(targetVersionId, update.getId());
        assertTrue(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(update.getRouteSnapshotJson()));
        JSONObject snapshot = JSON.parseObject(update.getRouteSnapshotJson());
        assertEquals(targetRouteId, snapshot.getLong("routeId"));
        assertEquals(targetCode, snapshot.getString("routeCode"));
        assertEquals(1, snapshot.getJSONObject("configSnapshots").getJSONArray("products").size());
        assertEquals(1, snapshot.getJSONObject("configSnapshots").getJSONArray("scheduleConfigs").size());
        assertEquals(1, snapshot.getJSONObject("configSnapshots").getJSONArray("scheduleUseConfigs").size());
        assertEquals(1, snapshot.getJSONObject("configSnapshots").getJSONArray("batchUseConfigs").size());
        ArgumentCaptor<MesProRouteVersionDO> activeCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(platformAdapter).recordActiveRegistered(activeCaptor.capture(), eq(null),
                eq("route active version registered"));
        assertEquals(targetVersionId, activeCaptor.getValue().getId());
        assertEquals(targetRouteId, activeCaptor.getValue().getRouteId());
        assertEquals("V1", activeCaptor.getValue().getVersionNo());
    }

    @Test
    void buildCurrentRouteSnapshotJson_shouldSerializeCurrentBatchRecordBindingsFromProcessSettings() {
        Long routeId = 10L;
        Long routeVersionId = 100L;
        MesProRouteDO route = MesProRouteDO.builder().id(routeId).code("ROUTE-1").name("路线").build();
        MesProRouteFlowProcessConfigDO processConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(901L)
                .routeFlowConfigId(800L)
                .routeId(routeId)
                .routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .enabled(Boolean.TRUE)
                .productionQuantityFactor(BigDecimal.ONE)
                .build();
        MesProRouteFlowProcessBatchRecordDO formBinding = MesProRouteFlowProcessBatchRecordDO.builder()
                .id(9901L)
                .routeFlowProcessConfigId(901L)
                .routeId(routeId)
                .routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .formBindingKey("FB-LIVE")
                .formTemplateId(2002L)
                .formTemplateNameSnapshot("工序设置当前生产记录")
                .lastPublishedTemplateVersionId(3002L)
                .lastPublishedTemplateVersionNo("V2")
                .instanceScope("PROCESS")
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .candidateSourceType("USERS")
                .candidateSourceIds("9002")
                .candidateSourceNames("[\"李四\"]")
                .reportSort(1)
                .build();
        MesProRouteFlowProcessBatchRecordDO legacyReport = MesProRouteFlowProcessBatchRecordDO.builder()
                .id(9902L)
                .routeFlowProcessConfigId(901L)
                .routeId(routeId)
                .routeProcessId(100L)
                .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType())
                .batchRecordReportId("REPORT-LIVE")
                .formSlotType("MAIN")
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .reportSort(2)
                .build();
        MesProRouteProcessFlowGraphRespVO graph = new MesProRouteProcessFlowGraphRespVO();
        graph.setRouteId(routeId);
        graph.setNodes(emptyList());
        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeProcessFlowService.getGraph(routeId)).thenReturn(graph);
        when(routeProductMapper.selectListByRouteId(routeId)).thenReturn(emptyList());
        when(routeProductBomMapper.selectList(routeId, null, null)).thenReturn(emptyList());
        when(routeScheduleConfigMapper.selectListByRouteVersionId(routeVersionId)).thenReturn(emptyList());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of(processConfig));
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(emptyList());
        when(routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(List.of(formBinding, legacyReport));

        JSONObject snapshot = JSON.parseObject(routeService.buildCurrentRouteSnapshotJson(routeId, routeVersionId));

        JSONObject batchUseConfig = snapshot.getJSONObject("configSnapshots")
                .getJSONArray("batchUseConfigs").getJSONObject(0);
        JSONArray formBindings = batchUseConfig.getJSONArray("formBindings");
        assertEquals(1, formBindings.size());
        assertEquals("FB-LIVE", formBindings.getJSONObject(0).getString("formBindingKey"));
        assertEquals(2002L, formBindings.getJSONObject(0).getLong("formTemplateId"));
        assertEquals("工序设置当前生产记录", formBindings.getJSONObject(0).getString("formTemplateName"));
        assertEquals("USERS", formBindings.getJSONObject(0).getString("candidateSourceType"));
        assertEquals(List.of(9002), formBindings.getJSONObject(0)
                .getJSONArray("candidateSourceIds").toJavaList(Integer.class));
        assertEquals(List.of("李四"), formBindings.getJSONObject(0)
                .getJSONArray("candidateSourceNames").toJavaList(String.class));
        JSONArray batchRecordReports = batchUseConfig.getJSONArray("batchRecordReports");
        assertEquals(1, batchRecordReports.size());
        assertEquals("REPORT-LIVE", batchRecordReports.getJSONObject(0).getString("batchRecordReportId"));
        assertEquals("MAIN", batchRecordReports.getJSONObject(0).getString("formSlotType"));
    }

    @Test
    void buildCurrentRouteSnapshotJson_shouldPreserveExistingBatchRecordAttachmentOwners() {
        Long routeId = 922119L;
        Long routeVersionId = 34126020001L;
        MesProRouteDO route = MesProRouteDO.builder()
                .id(routeId)
                .code("RT000028")
                .name("球囊扩张压力泵")
                .build();
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder()
                .id(routeVersionId)
                .routeId(routeId)
                .routeSnapshotJson("""
                        {"routeId":922119,"configSnapshots":{"batchRecordAttachmentOwners":[{"attachmentCode":"INCOMING_INSPECTION_REPORT","candidateSourceType":"USERS","candidateSourceIds":[912398],"candidateSourceNames":["张三"]},{"attachmentCode":"STERILIZATION_REPORT","candidateSourceType":"USERS","candidateSourceIds":[912398],"candidateSourceNames":["张三"]},{"attachmentCode":"FINISHED_PRODUCT_INSPECTION_REPORT","candidateSourceType":"USERS","candidateSourceIds":[912399],"candidateSourceNames":["李四"]},{"attachmentCode":"FINISHED_PRODUCT_INSPECTION_RECORD","candidateSourceType":"USERS","candidateSourceIds":[912399],"candidateSourceNames":["李四"]}]}}
                        """)
                .build();
        MesProRouteProcessFlowGraphRespVO graph = new MesProRouteProcessFlowGraphRespVO();
        graph.setRouteId(routeId);
        graph.setNodes(emptyList());
        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeVersionMapper.selectById(routeVersionId)).thenReturn(routeVersion);
        when(routeProcessFlowService.getGraph(routeId)).thenReturn(graph);
        when(routeProductMapper.selectListByRouteId(routeId)).thenReturn(emptyList());
        when(routeProductBomMapper.selectList(routeId, null, null)).thenReturn(emptyList());
        when(routeScheduleConfigMapper.selectListByRouteVersionId(routeVersionId)).thenReturn(emptyList());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.BATCH.getType())).thenReturn(emptyList());
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(emptyList());

        JSONObject snapshot = JSON.parseObject(routeService.buildCurrentRouteSnapshotJson(routeId, routeVersionId));

        JSONArray owners = snapshot.getJSONObject("configSnapshots").getJSONArray("batchRecordAttachmentOwners");
        assertEquals(4, owners.size());
        assertEquals("INCOMING_INSPECTION_REPORT", owners.getJSONObject(0).getString("attachmentCode"));
        assertEquals(List.of(912398), owners.getJSONObject(0)
                .getJSONArray("candidateSourceIds").toJavaList(Integer.class));
    }

    @Test
    void copyRoute_shouldGrantRouteEditPermissionToCopier() {
        Long sourceRouteId = 9016L;
        Long targetRouteId = 9017L;
        Long targetVersionId = 9217L;
        Long copierUserId = 501L;
        String targetCode = "ROUTE-COPY-PERM";
        String targetName = "复制权限路线";

        when(routeMapper.selectById(sourceRouteId)).thenReturn(MesProRouteDO.builder()
                .id(sourceRouteId)
                .code("ROUTE-SOURCE-PERM")
                .name("源权限路线")
                .build());
        when(routeMapper.selectByCode(targetCode)).thenReturn(null);
        when(routeMapper.selectByName(targetName)).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteDO targetRoute = invocation.getArgument(0);
            targetRoute.setId(targetRouteId);
            return 1;
        }).when(routeMapper).insert(any(MesProRouteDO.class));
        when(routeProductMapper.selectListByRouteId(anyLong())).thenReturn(emptyList());
        when(routeProductBomMapper.selectList(anyLong(), nullable(Long.class), nullable(Long.class)))
                .thenReturn(emptyList());
        when(routeProcessMapper.selectListByRouteId(sourceRouteId)).thenReturn(emptyList());
        when(routeVersionMapper.selectActiveByRouteId(sourceRouteId)).thenReturn(null);
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteVersionDO targetVersion = invocation.getArgument(0);
            targetVersion.setId(targetVersionId);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(copierUserId);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");

            routeService.copyRoute(sourceRouteId, targetCode, targetName);
        }

        ArgumentCaptor<MesProEdhrPermissionScopeSaveCommand> scopeCaptor =
                ArgumentCaptor.forClass(MesProEdhrPermissionScopeSaveCommand.class);
        verify(permissionScopeService).saveRules(scopeCaptor.capture());
        MesProEdhrPermissionScopeSaveCommand command = scopeCaptor.getValue();
        assertEquals("route-" + targetRouteId, command.getScopeName());
        assertEquals("ROUTE", command.getObjectType());
        assertEquals(String.valueOf(targetRouteId), command.getObjectId());
        assertEquals(copierUserId, command.getActorUserId());
        assertEquals("aoteman", command.getActorUsername());
        assertEquals(List.of("VIEW", "ROUTE_EDIT", "PERMISSION_ADMIN"),
                command.getRules().stream().map(MesProEdhrPermissionRuleCommand::getAbility).toList());
        command.getRules().forEach(rule -> {
            assertEquals("USER", rule.getSubjectType());
            assertEquals(copierUserId, rule.getSubjectId());
            assertEquals("ALLOW", rule.getDecision());
            assertEquals("ENABLED", rule.getStatus());
        });
    }

    @Test
    void writeMethodsThatCreateRouteVersions_shouldBeTransactionalWithPlatformRefs() throws NoSuchMethodException {
        assertTransactional("createRoute", MesProRouteSaveReqVO.class);
        assertTransactional("updateRoute", MesProRouteSaveReqVO.class);
        assertTransactional("copyRoute", Long.class, String.class, String.class);
        assertTransactional("maintainRouteVersionAfterProcessChange", Long.class);
    }

    @Test
    void updateRouteStatus_shouldEnableRouteEvenWithoutRouteProductBomConsumption() {
        Long routeId = 1001L;
        Long productId = 2001L;

        MesProRouteDO route = MesProRouteDO.builder()
                .id(routeId)
                .status(CommonStatusEnum.DISABLE.getStatus())
                .build();
        MesProRouteProcessDO process = MesProRouteProcessDO.builder()
                .id(3001L)
                .routeId(routeId)
                .keyFlag(true)
                .build();
        MesProRouteProductDO product = MesProRouteProductDO.builder()
                .id(4001L)
                .routeId(routeId)
                .itemId(productId)
                .build();
        MesMdItemDO item = MesMdItemDO.builder()
                .id(productId)
                .name("冠状动脉棘突球囊扩张导管")
                .build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeProcessService.getRouteProcessListByRouteId(routeId)).thenReturn(List.of(process));
        lenient().when(routeProductService.getRouteProductListByRouteId(routeId)).thenReturn(List.of(product));
        lenient().when(routeProductBomService.getRouteProductBomList(routeId, null, productId)).thenReturn(emptyList());
        lenient().when(itemService.validateItemExists(productId)).thenReturn(item);

        assertDoesNotThrow(() -> routeService.updateRouteStatus(routeId, CommonStatusEnum.ENABLE.getStatus()));

        verify(routeProductService, never()).getRouteProductListByRouteId(eq(routeId));
        verify(routeProductBomService, never()).getRouteProductBomList(any(), any(), any());
        verify(itemService, never()).validateItemExists(any());
        verify(routeMapper).updateById(any(MesProRouteDO.class));
    }

    @Test
    void updateRouteStatus_shouldKeepKeyProcessRequirement() {
        Long routeId = 1002L;

        MesProRouteDO route = MesProRouteDO.builder()
                .id(routeId)
                .status(CommonStatusEnum.DISABLE.getStatus())
                .build();
        MesProRouteProcessDO process = MesProRouteProcessDO.builder()
                .id(3002L)
                .routeId(routeId)
                .keyFlag(false)
                .build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeProcessService.getRouteProcessListByRouteId(routeId)).thenReturn(List.of(process));

        AssertUtils.assertServiceException(
                () -> routeService.updateRouteStatus(routeId, CommonStatusEnum.ENABLE.getStatus()),
                ErrorCodeConstants.PRO_ROUTE_ENABLE_NO_KEY_PROCESS
        );

        verify(routeMapper, never()).updateById(any(MesProRouteDO.class));
        verify(routeProductService, never()).getRouteProductListByRouteId(eq(routeId));
        verify(routeProductBomService, never()).getRouteProductBomList(any(), any(), any());
    }

    @Test
    void updateRouteStatus_shouldIgnoreNullKeyFlagWhenAnotherProcessIsKey() {
        Long routeId = 1003L;

        MesProRouteDO route = MesProRouteDO.builder()
                .id(routeId)
                .status(CommonStatusEnum.DISABLE.getStatus())
                .build();
        MesProRouteProcessDO normalProcess = MesProRouteProcessDO.builder()
                .id(3003L)
                .routeId(routeId)
                .keyFlag(null)
                .build();
        MesProRouteProcessDO keyProcess = MesProRouteProcessDO.builder()
                .id(3004L)
                .routeId(routeId)
                .keyFlag(true)
                .build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeProcessService.getRouteProcessListByRouteId(routeId)).thenReturn(List.of(normalProcess, keyProcess));

        assertDoesNotThrow(() -> routeService.updateRouteStatus(routeId, CommonStatusEnum.ENABLE.getStatus()));

        verify(routeMapper).updateById(any(MesProRouteDO.class));
    }

    @Test
    void ensureDefaultScheduleArtifacts_shouldCreateDefaultUseConfigAndScheduleConfig() {
        Long routeId = 2001L;
        Long routeProcessId = 3001L;
        MesProRouteDO route = MesProRouteDO.builder().id(routeId).code("ROUTE-AUTO").build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(routeProcessId).routeId(routeId).processId(4001L).sort(1).build();
        cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO routeVersion =
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO.builder()
                        .id(5001L).routeId(routeId).active(Boolean.TRUE).versionNo("V1").build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeProcessService.resolveCurrentRouteProcess(routeProcessId, routeId, null)).thenReturn(routeProcess);
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(null);
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(routeProcessId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(null);
        when(routeVersionMapper.selectActiveByRouteId(routeId)).thenReturn(routeVersion);
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(routeVersion.getId(), routeProcessId))
                .thenReturn(null);
        when(schedulerWorkbenchService.getPolicySettings()).thenReturn(defaultPolicySettings());
        when(scheduleCalendarRuleMapper.selectByTenantId(1L)).thenReturn(MesProScheduleCalendarRuleDO.builder()
                .id(7001L)
                .build());
        org.mockito.Mockito.doAnswer(invocation -> {
            MesProRouteFlowConfigDO data = invocation.getArgument(0);
            data.setId(6001L);
            return 1;
        }).when(routeFlowConfigMapper).insert(any(MesProRouteFlowConfigDO.class));

        TenantContextHolder.setTenantId(1L);
        try {
            routeService.ensureDefaultScheduleArtifacts(routeId, routeProcessId);
        } finally {
            TenantContextHolder.clear();
        }

        ArgumentCaptor<MesProRouteFlowConfigDO> useConfigCaptor = ArgumentCaptor.forClass(MesProRouteFlowConfigDO.class);
        verify(routeFlowConfigMapper).insert(useConfigCaptor.capture());
        MesProRouteFlowConfigDO useConfig = useConfigCaptor.getValue();
        assertEquals(routeId, useConfig.getRouteId());
        assertEquals(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType(), useConfig.getUseType());
        assertEquals("AUTO-SCHEDULE", useConfig.getConfigVersion());
        assertEquals("[AUTO_DEFAULT_SCHEDULE_USE]", useConfig.getRemark());

        ArgumentCaptor<MesProRouteFlowProcessConfigDO> processConfigCaptor =
                ArgumentCaptor.forClass(MesProRouteFlowProcessConfigDO.class);
        verify(routeFlowProcessConfigMapper).insert(processConfigCaptor.capture());
        MesProRouteFlowProcessConfigDO processConfig = processConfigCaptor.getValue();
        assertEquals(6001L, processConfig.getRouteFlowConfigId());
        assertEquals(routeId, processConfig.getRouteId());
        assertEquals(routeProcessId, processConfig.getRouteProcessId());
        assertEquals(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType(), processConfig.getUseType());
        assertEquals(Boolean.TRUE, processConfig.getEnabled());
        assertEquals("SEQUENTIAL", processConfig.getExecutionMode());
        assertEquals("[AUTO_DEFAULT_SCHEDULE_USE]", processConfig.getRemark());

        ArgumentCaptor<MesProRouteScheduleConfigDO> scheduleConfigCaptor =
                ArgumentCaptor.forClass(MesProRouteScheduleConfigDO.class);
        verify(routeScheduleConfigMapper).insert(scheduleConfigCaptor.capture());
        MesProRouteScheduleConfigDO scheduleConfig = scheduleConfigCaptor.getValue();
        assertEquals(routeVersion.getId(), scheduleConfig.getRouteVersionId());
        assertEquals(routeProcessId, scheduleConfig.getRouteProcessId());
        assertEquals(MesProScheduleCapacityModeEnum.RESOURCE_CALCULATED.getMode(), scheduleConfig.getCapacityMode());
        assertNull(scheduleConfig.getHourlyCapacity());
        assertNull(scheduleConfig.getInfiniteDurationQuantityFactor());
        assertNull(scheduleConfig.getInfiniteDurationBaseMinutes());
        assertEquals(Boolean.TRUE, scheduleConfig.getNightShiftEnabled());
        assertEquals(7001L, scheduleConfig.getCalendarRuleId());
        assertEquals(MesProRouteServiceImpl.DEFAULT_SCHEDULE_CONFIG_VERSION, scheduleConfig.getConfigVersion());
        assertEquals("[AUTO_DEFAULT_SCHEDULE_CONFIG]", scheduleConfig.getRemark());
    }

    @Test
    void ensureDefaultScheduleArtifacts_shouldRebindProcessConfigOwnedByDifferentFlow() {
        Long routeId = 2002L;
        Long routeProcessId = 3002L;
        MesProRouteDO route = MesProRouteDO.builder().id(routeId).code("ROUTE-REBIND").build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(routeProcessId).routeId(routeId).processId(4002L).sort(1).build();
        MesProRouteFlowConfigDO currentFlow = MesProRouteFlowConfigDO.builder()
                .id(6002L)
                .routeId(routeId)
                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                .enabled(Boolean.TRUE)
                .build();
        MesProRouteFlowProcessConfigDO staleProcessConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(6102L)
                .routeFlowConfigId(9999L)
                .routeId(routeId)
                .routeProcessId(routeProcessId)
                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                .enabled(Boolean.TRUE)
                .build();
        cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO routeVersion =
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO.builder()
                        .id(5002L).routeId(routeId).active(Boolean.TRUE).versionNo("V1").build();
        MesProRouteScheduleConfigDO scheduleConfig = MesProRouteScheduleConfigDO.builder()
                .id(7002L).routeVersionId(routeVersion.getId()).routeProcessId(routeProcessId).build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeProcessService.resolveCurrentRouteProcess(routeProcessId, routeId, null)).thenReturn(routeProcess);
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(currentFlow);
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(
                routeProcessId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(staleProcessConfig);
        when(routeVersionMapper.selectActiveByRouteId(routeId)).thenReturn(routeVersion);
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(
                routeVersion.getId(), routeProcessId)).thenReturn(scheduleConfig);
        when(schedulerWorkbenchService.getPolicySettings()).thenReturn(defaultPolicySettings());

        routeService.ensureDefaultScheduleArtifacts(routeId, routeProcessId);

        verify(routeFlowProcessConfigMapper).updateById(
                org.mockito.ArgumentMatchers.<MesProRouteFlowProcessConfigDO>argThat(config ->
                Long.valueOf(6102L).equals(config.getId())
                        && Long.valueOf(6002L).equals(config.getRouteFlowConfigId())
                        && routeId.equals(config.getRouteId())
                        && routeProcessId.equals(config.getRouteProcessId())
                        && MesProRouteFlowConfigTypeEnum.SCHEDULE.getType().equals(config.getUseType())));
        verify(routeFlowProcessConfigMapper, never()).insert(any(MesProRouteFlowProcessConfigDO.class));
        verify(routeScheduleConfigMapper, never()).insert(any(MesProRouteScheduleConfigDO.class));
    }

    @Test
    void ensureDefaultScheduleArtifacts_shouldNormalizeHistoricalProcessConfigs() {
        Long routeId = 2003L;
        Long routeProcessId = 3003L;
        Long historicalRouteProcessId = 2999L;
        MesProRouteDO route = MesProRouteDO.builder().id(routeId).code("ROUTE-HISTORICAL").build();
        MesProRouteProcessDO routeProcess = MesProRouteProcessDO.builder()
                .id(routeProcessId).routeId(routeId).processId(4003L).sort(1).build();
        MesProRouteFlowConfigDO currentFlow = MesProRouteFlowConfigDO.builder()
                .id(6003L)
                .routeId(routeId)
                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                .enabled(Boolean.TRUE)
                .build();
        MesProRouteFlowProcessConfigDO historicalProcessConfig = MesProRouteFlowProcessConfigDO.builder()
                .id(6103L)
                .routeFlowConfigId(currentFlow.getId())
                .routeId(routeId)
                .routeProcessId(historicalRouteProcessId)
                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                .enabled(Boolean.TRUE)
                .build();
        cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO routeVersion =
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO.builder()
                        .id(5003L).routeId(routeId).active(Boolean.TRUE).versionNo("V1").build();
        MesProRouteScheduleConfigDO historicalScheduleConfig = MesProRouteScheduleConfigDO.builder()
                .id(7003L)
                .routeVersionId(routeVersion.getId())
                .routeProcessId(historicalRouteProcessId)
                .capacityMode(MesProScheduleCapacityModeEnum.FINITE_HOURLY.getMode())
                .hourlyCapacity(new BigDecimal("88"))
                .build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeProcessService.resolveCurrentRouteProcess(routeProcessId, routeId, null)).thenReturn(routeProcess);
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(currentFlow);
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(
                routeProcessId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(null);
        lenient().when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()))
                .thenReturn(List.of(historicalProcessConfig));
        when(routeVersionMapper.selectActiveByRouteId(routeId)).thenReturn(routeVersion);
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(
                routeVersion.getId(), routeProcessId)).thenReturn(null);
        lenient().when(routeScheduleConfigMapper.selectListByRouteVersionId(routeVersion.getId()))
                .thenReturn(List.of(historicalScheduleConfig));
        lenient().when(routeProcessService.resolveCurrentRouteProcess(
                historicalRouteProcessId, routeId, null)).thenReturn(routeProcess);
        when(schedulerWorkbenchService.getPolicySettings()).thenReturn(defaultPolicySettings());
        lenient().when(scheduleCalendarRuleMapper.selectByTenantId(1L))
                .thenReturn(MesProScheduleCalendarRuleDO.builder().id(7004L).build());

        TenantContextHolder.setTenantId(1L);
        try {
            routeService.ensureDefaultScheduleArtifacts(routeId, routeProcessId);
        } finally {
            TenantContextHolder.clear();
        }

        verify(routeFlowProcessConfigMapper).updateById(
                org.mockito.ArgumentMatchers.<MesProRouteFlowProcessConfigDO>argThat(config ->
                        Long.valueOf(6103L).equals(config.getId())
                                && routeProcessId.equals(config.getRouteProcessId())));
        verify(routeFlowProcessConfigMapper, never()).insert(any(MesProRouteFlowProcessConfigDO.class));
        verify(routeScheduleConfigMapper).updateById(
                org.mockito.ArgumentMatchers.<MesProRouteScheduleConfigDO>argThat(config ->
                        Long.valueOf(7003L).equals(config.getId())
                                && routeProcessId.equals(config.getRouteProcessId())));
        verify(routeScheduleConfigMapper, never()).insert(any(MesProRouteScheduleConfigDO.class));
    }

    @Test
    void ensureDefaultScheduleArtifacts_shouldNormalizeHistoricalRequestedRouteProcess() {
        Long routeId = 2004L;
        Long historicalRouteProcessId = 2998L;
        Long currentRouteProcessId = 3004L;
        MesProRouteDO route = MesProRouteDO.builder().id(routeId).code("ROUTE-HISTORICAL-REQ").build();
        MesProRouteProcessDO currentRouteProcess = MesProRouteProcessDO.builder()
                .id(currentRouteProcessId).routeId(routeId).processId(4004L).sort(1).build();
        MesProRouteFlowConfigDO currentFlow = MesProRouteFlowConfigDO.builder()
                .id(6004L)
                .routeId(routeId)
                .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())
                .enabled(Boolean.TRUE)
                .build();
        cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO routeVersion =
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO.builder()
                        .id(5004L).routeId(routeId).active(Boolean.TRUE).versionNo("V1").build();

        when(routeMapper.selectById(routeId)).thenReturn(route);
        when(routeProcessService.resolveCurrentRouteProcess(historicalRouteProcessId, routeId, null))
                .thenReturn(currentRouteProcess);
        when(routeFlowConfigMapper.selectByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(currentFlow);
        when(routeFlowProcessConfigMapper.selectByRouteProcessIdAndUseType(
                currentRouteProcessId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(null);
        when(routeFlowProcessConfigMapper.selectListByRouteIdAndUseType(
                routeId, MesProRouteFlowConfigTypeEnum.SCHEDULE.getType())).thenReturn(List.of());
        when(routeVersionMapper.selectActiveByRouteId(routeId)).thenReturn(routeVersion);
        when(routeScheduleConfigMapper.selectByRouteVersionIdAndRouteProcessId(
                routeVersion.getId(), currentRouteProcessId)).thenReturn(null);
        when(routeScheduleConfigMapper.selectListByRouteVersionId(routeVersion.getId())).thenReturn(List.of());
        when(schedulerWorkbenchService.getPolicySettings()).thenReturn(defaultPolicySettings());
        when(scheduleCalendarRuleMapper.selectByTenantId(1L))
                .thenReturn(MesProScheduleCalendarRuleDO.builder().id(7005L).build());

        TenantContextHolder.setTenantId(1L);
        try {
            routeService.ensureDefaultScheduleArtifacts(routeId, historicalRouteProcessId);
        } finally {
            TenantContextHolder.clear();
        }

        verify(routeFlowProcessConfigMapper).insert(
                org.mockito.ArgumentMatchers.<MesProRouteFlowProcessConfigDO>argThat(config ->
                        currentRouteProcessId.equals(config.getRouteProcessId())));
        verify(routeScheduleConfigMapper).insert(
                org.mockito.ArgumentMatchers.<MesProRouteScheduleConfigDO>argThat(config ->
                        currentRouteProcessId.equals(config.getRouteProcessId())));
    }

    @Test
    void resolveCopiedRouteProcessId_shouldMapHistoricalSourceIdentity() {
        Long routeId = 2100L;
        MesProRouteProcessDO currentRouteProcess = MesProRouteProcessDO.builder()
                .id(3100L)
                .routeId(routeId)
                .processId(4100L)
                .build();
        when(routeProcessService.resolveCurrentRouteProcess(3099L, routeId, null))
                .thenReturn(currentRouteProcess);

        Long result = ReflectionTestUtils.invokeMethod(
                routeService, "resolveCopiedRouteProcessId", routeId, 3099L, Map.of(3100L, 3200L));

        assertEquals(3200L, result);
    }

    private MesProSchedulerWorkbenchPolicySettingsRespVO defaultPolicySettings() {
        MesProSchedulerWorkbenchPolicySettingsRespVO respVO = new MesProSchedulerWorkbenchPolicySettingsRespVO();
        respVO.setErpWorkOrderSyncTime("02:00");
        respVO.setNightlyReplanTime("03:00");
        respVO.setPriorityRule("PROMISE_DATE");
        respVO.setProtectReportedTasks(true);
        respVO.setProtectCompletedTasks(true);
        respVO.setProtectLockedTasks(true);
        respVO.setDefaultScheduleUseEnabled(true);
        respVO.setDefaultScheduleCapacityMode("FINITE_HOURLY");
        respVO.setDefaultFiniteHourlyCapacity(new BigDecimal("88"));
        respVO.setDefaultNightShiftEnabled(true);
        respVO.setDefaultWorkerQuantity(5);
        respVO.setDefaultWorkerSingleHourlyCapacity(new BigDecimal("30"));
        return respVO;
    }

    private String validCompleteRouteSnapshotJson(Long routeId, String routeCode, String routeName) {
        return """
                {
                  "routeId": %d,
                  "routeCode": "%s",
                  "routeName": "%s",
                  "configSnapshots": {
                    "flowGraph": {
                      "routeId": %d,
                      "nodes": [
                        { "routeProcessId": 3001, "processId": 4001, "processName": "工序", "sort": 1 }
                      ],
                      "edges": [],
                      "boundaryEdges": []
                    },
                    "products": [
                      { "id": 5001, "routeId": %d, "itemId": 6001, "quantity": 1 }
                    ],
                    "scheduleConfigs": [
                      { "id": 7001, "routeVersionId": 8001, "routeProcessId": 3001 }
                    ],
                    "batchUseConfigs": [
                      { "id": 9001, "routeId": %d, "routeProcessId": 3001, "useType": "BATCH" }
                    ],
                    "scheduleUseConfigs": [
                      { "id": 9002, "routeId": %d, "routeProcessId": 3001, "useType": "SCHEDULE" }
                    ]
                  }
                }
                """.formatted(routeId, routeCode, routeName, routeId, routeId, routeId, routeId);
    }

    private void assertTransactional(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = MesProRouteServiceImpl.class.getMethod(methodName, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertTrue(transactional != null, methodName + " should be transactional");
        assertEquals(Exception.class, transactional.rollbackFor()[0]);
    }
}
