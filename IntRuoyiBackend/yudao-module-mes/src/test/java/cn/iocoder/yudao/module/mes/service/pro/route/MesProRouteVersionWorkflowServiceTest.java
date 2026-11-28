package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionBlockerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteProductionConfigMigrateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderLossReasonItem;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderProcessConfigDevice;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderProcessConfigParameter;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderProcessConfigRow;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderProcessConfigService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteVersionWorkflowServiceTest {
    @org.mockito.Mock
    private MesProRouteDeviceIdentitySnapshotService deviceIdentitySnapshots;


    @Mock
    private cn.iocoder.yudao.module.mes.service.audit.MesVersionOperateLogService versionOperateLogService;

    @Mock
    private cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftService craftService;

    @InjectMocks
    private MesProRouteVersionWorkflowServiceImpl service;

    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesProRouteControlledContentAdapter platformAdapter;
    @Mock
    private MesTeamLeaderProcessConfigService teamLeaderProcessConfigService;

    @Mock
    private MesProRouteCandidateConfigService candidateConfigService;

    @Test
    void migrateProductionConfig_shouldOnlyAddProductionConfigToSelectedDraftAndPreserveFormalPercent() {
        MesProRouteVersionDO candidate = draftCandidate(activeVersion());
        candidate.setRouteSnapshotJson(snapshotWithoutProductionConfigs());
        candidate.setRouteSnapshotSha256("draft-sha");
        when(routeVersionMapper.selectByIdForUpdate(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(teamLeaderProcessConfigService.listProcessConfigs(eq(507L), any()))
                .thenReturn(List.of(legacyProcessConfig(new BigDecimal("12"))));
        var request = migrationRequest(candidate);
        request.setMissingOveragePercent(new BigDecimal("5"));
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(507L);
            service.migrateProductionConfig(request);
        }
        ArgumentCaptor<java.util.Map<String, Object>> configs = ArgumentCaptor.forClass(java.util.Map.class);
        verify(candidateConfigService).migrateProductionConfigSnapshots(eq(candidate.getId()), eq("draft-sha"), configs.capture());
        assertEquals(java.util.Set.of("productionProcessConfigSchemaVersion", "productionProcessConfigs"),
                configs.getValue().keySet());
        var process = ((JSONArray) configs.getValue().get("productionProcessConfigs")).getJSONObject(0);
        assertEquals(12, process.getBigDecimal("overagePercent").intValueExact());
        assertEquals("CLEANING", process.getJSONArray("deviceSelectionGroups").getJSONObject(0).getString("deviceGroupKey"));
        verify(versionOperateLogService).route("迁移生产配置", candidate, candidate, 507L);
        verify(routeVersionMapper, never()).insert(any(MesProRouteVersionDO.class));
    }

    @Test
    void migrateProductionConfig_shouldRejectActiveStaleAndExistingSnapshotsWithoutSaving() {
        for (String condition : List.of("active", "pending", "stale", "existing")) {
            MesProRouteVersionDO candidate = draftCandidate(activeVersion());
            candidate.setRouteSnapshotJson(snapshotWithoutProductionConfigs());
            candidate.setRouteSnapshotSha256("draft-sha");
            if (condition.equals("active")) candidate.setActive(true);
            if (condition.equals("pending")) candidate.setLifecycleStatus("PENDING_APPROVAL");
            if (condition.equals("existing")) candidate.setRouteSnapshotJson(validSnapshotJson(9001L, "RT", "路线"));
            when(routeVersionMapper.selectByIdForUpdate(candidate.getId())).thenReturn(candidate);
            var request = migrationRequest(candidate);
            if (condition.equals("stale")) request.setExpectedRouteSnapshotSha256("old-sha");
            try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
                security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(507L);
                assertThrows(ServiceException.class, () -> service.migrateProductionConfig(request));
            }
        }
        verify(candidateConfigService, never()).migrateProductionConfigSnapshots(any(), any(), any());
    }

    @Test
    void migrateProductionConfig_shouldRejectMissingBusinessPercentWithoutDefault() {
        MesProRouteVersionDO candidate = draftCandidate(activeVersion());
        candidate.setRouteSnapshotJson(snapshotWithoutProductionConfigs());
        candidate.setRouteSnapshotSha256("draft-sha");
        when(routeVersionMapper.selectByIdForUpdate(candidate.getId())).thenReturn(candidate);
        when(teamLeaderProcessConfigService.listProcessConfigs(eq(507L), any()))
                .thenReturn(List.of(legacyProcessConfig(null)));
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(507L);
            ServiceException error = assertThrows(ServiceException.class,
                    () -> service.migrateProductionConfig(migrationRequest(candidate)));
            assertTrue(error.getMessage().contains("未配置超量比例"));
        }
        verify(candidateConfigService, never()).migrateProductionConfigSnapshots(any(), any(), any());
    }

    private MesProRouteProductionConfigMigrateReqVO migrationRequest(MesProRouteVersionDO candidate) {
        var request = new MesProRouteProductionConfigMigrateReqVO();
        request.setRouteVersionId(candidate.getId());
        request.setExpectedRouteSnapshotSha256("draft-sha");
        return request;
    }

    @Test
    void createCandidate_shouldExplicitlyMigrateFormalTeamLeaderProductionConfig() {
        MesProRouteVersionDO active = activeVersion();
        String incompleteSnapshot = snapshotWithoutProductionConfigs();
        when(routeVersionMapper.selectActiveByRouteIdForUpdate(9001L)).thenReturn(active);
        when(routeVersionMapper.selectMaxVersionNoByRouteId(9001L)).thenReturn("V1");
        when(routeService.buildCurrentRouteSnapshotJson(9001L, 1001L)).thenReturn(incompleteSnapshot);
        when(teamLeaderProcessConfigService.listProcessConfigs(eq(507L), any())).thenReturn(List.of(
                legacyProcessConfig(new BigDecimal("10.0000"))));
        MesProRouteVersionCreateReqVO reqVO = new MesProRouteVersionCreateReqVO();
        reqVO.setRouteId(9001L);
        reqVO.setSourceRouteVersionId(1001L);
        reqVO.setChangeReason("显式迁移现有生产配置");
        reqVO.setMigrateLegacyProductionConfig(Boolean.TRUE);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(507L);
            service.createCandidate(reqVO);
        }

        ArgumentCaptor<MesProRouteVersionDO> captor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).insert(captor.capture());
        verify(versionOperateLogService).route("升版", active, captor.getValue(), 507L);
        JSONObject config = JSON.parseObject(captor.getValue().getRouteSnapshotJson())
                .getJSONObject("configSnapshots").getJSONArray("productionProcessConfigs").getJSONObject(0);
        assertEquals(10, config.getBigDecimal("overagePercent").intValueExact());
        assertEquals("LOSS-001", config.getJSONArray("lossReasons").getJSONObject(0).getString("reasonCode"));
        JSONObject group = config.getJSONArray("deviceSelectionGroups").getJSONObject(0);
        assertEquals("CLEANING", group.getString("deviceGroupKey"));
        assertEquals("MULTIPLE", group.getString("selectionMode"));
        assertEquals(List.of(7001L), group.getJSONArray("deviceIds").toJavaList(Long.class));
        JSONObject parameter = config.getJSONArray("parameterRules").getJSONObject(0);
        assertEquals("cleaning-medium", parameter.getString("parameterCode"));
        assertEquals("TEXT_STANDARD", parameter.getString("valueType"));
        assertEquals(10L, parameter.getLong("routeProcessId"));
    }

    @Test
    void createCandidate_shouldRejectExplicitMigrationWhenFormalOverageIsMissing() {
        MesProRouteVersionDO active = activeVersion();
        when(routeVersionMapper.selectActiveByRouteIdForUpdate(9001L)).thenReturn(active);
        when(routeService.buildCurrentRouteSnapshotJson(9001L, 1001L))
                .thenReturn(snapshotWithoutProductionConfigs());
        when(teamLeaderProcessConfigService.listProcessConfigs(eq(507L), any()))
                .thenReturn(List.of(legacyProcessConfig(null)));
        MesProRouteVersionCreateReqVO reqVO = new MesProRouteVersionCreateReqVO();
        reqVO.setRouteId(9001L);
        reqVO.setSourceRouteVersionId(1001L);
        reqVO.setMigrateLegacyProductionConfig(Boolean.TRUE);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(507L);
            assertThrows(ServiceException.class, () -> service.createCandidate(reqVO));
        }

        verify(routeVersionMapper, never()).insert(any(MesProRouteVersionDO.class));
    }

    @Test
    void createCandidate_shouldUseExplicitMissingOveragePercentOnlyForMissingFormalValue() {
        MesProRouteVersionDO active = activeVersion();
        when(routeVersionMapper.selectActiveByRouteIdForUpdate(9001L)).thenReturn(active);
        when(routeVersionMapper.selectMaxVersionNoByRouteId(9001L)).thenReturn("V1");
        when(routeService.buildCurrentRouteSnapshotJson(9001L, 1001L))
                .thenReturn(snapshotWithoutProductionConfigs());
        when(teamLeaderProcessConfigService.listProcessConfigs(eq(507L), any()))
                .thenReturn(List.of(legacyProcessConfig(null)));
        MesProRouteVersionCreateReqVO reqVO = new MesProRouteVersionCreateReqVO();
        reqVO.setRouteId(9001L);
        reqVO.setSourceRouteVersionId(1001L);
        reqVO.setMigrateLegacyProductionConfig(Boolean.TRUE);
        reqVO.setMissingOveragePercent(BigDecimal.ZERO);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(507L);
            service.createCandidate(reqVO);
        }

        ArgumentCaptor<MesProRouteVersionDO> captor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).insert(captor.capture());
        JSONObject config = JSON.parseObject(captor.getValue().getRouteSnapshotJson())
                .getJSONObject("configSnapshots").getJSONArray("productionProcessConfigs").getJSONObject(0);
        assertEquals(0, config.getBigDecimal("overagePercent").intValueExact());
    }

    @Test
    void createCandidate_shouldCanonicalizeStaleIntegerDecimalScaleDuringMigration() {
        MesProRouteVersionDO active = activeVersion();
        when(routeVersionMapper.selectActiveByRouteIdForUpdate(9001L)).thenReturn(active);
        when(routeVersionMapper.selectMaxVersionNoByRouteId(9001L)).thenReturn("V1");
        when(routeService.buildCurrentRouteSnapshotJson(9001L, 1001L))
                .thenReturn(snapshotWithoutProductionConfigs());
        MesTeamLeaderProcessConfigRow legacy = legacyProcessConfig(new BigDecimal("10.0000"));
        legacy.getDevices().get(0).getParameters().get(0)
                .setValueType("INTEGER")
                .setStandardText("3h")
                .setTargetValue(BigDecimal.valueOf(3))
                .setDecimalScale(1);
        when(teamLeaderProcessConfigService.listProcessConfigs(eq(507L), any()))
                .thenReturn(List.of(legacy));
        MesProRouteVersionCreateReqVO reqVO = new MesProRouteVersionCreateReqVO();
        reqVO.setRouteId(9001L);
        reqVO.setSourceRouteVersionId(1001L);
        reqVO.setMigrateLegacyProductionConfig(Boolean.TRUE);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(507L);
            service.createCandidate(reqVO);
        }

        ArgumentCaptor<MesProRouteVersionDO> captor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).insert(captor.capture());
        JSONObject parameter = JSON.parseObject(captor.getValue().getRouteSnapshotJson())
                .getJSONObject("configSnapshots").getJSONArray("productionProcessConfigs").getJSONObject(0)
                .getJSONArray("parameterRules").getJSONObject(0);
        assertEquals("INTEGER", parameter.getString("valueType"));
        assertEquals(3, parameter.getBigDecimal("defaultValue").intValueExact());
        assertFalse(parameter.containsKey("decimalScale"));
    }

    @Test
    void createCandidate_shouldKeepIncompleteSnapshotBlockedWithoutExplicitMigration() {
        MesProRouteVersionDO active = activeVersion();
        when(routeVersionMapper.selectActiveByRouteIdForUpdate(9001L)).thenReturn(active);
        when(routeService.buildCurrentRouteSnapshotJson(9001L, 1001L))
                .thenReturn(snapshotWithoutProductionConfigs());
        MesProRouteVersionCreateReqVO reqVO = new MesProRouteVersionCreateReqVO();
        reqVO.setRouteId(9001L);
        reqVO.setSourceRouteVersionId(1001L);

        assertThrows(ServiceException.class, () -> service.createCandidate(reqVO));

        verify(teamLeaderProcessConfigService, never()).listProcessConfigs(any(), any());
        verify(routeVersionMapper, never()).insert(any(MesProRouteVersionDO.class));
    }

    @Test
    void createCandidate_shouldRefreshCurrentConfigSnapshotAsDraft() {
        String staleActiveSnapshot = validSnapshotJson(9001L, "RT-9001", "工艺路线 V1");
        String refreshedSnapshot = validSnapshotJsonWithBatchBinding();
        MesProRouteVersionDO active = activeVersion();
        active.setRouteSnapshotJson(staleActiveSnapshot);
        active.setLastSavedBy(31L);
        when(routeVersionMapper.selectActiveByRouteIdForUpdate(9001L)).thenReturn(active);
        when(routeVersionMapper.selectMaxVersionNoByRouteId(9001L)).thenReturn("V1");
        when(routeService.buildCurrentRouteSnapshotJson(9001L, 1001L)).thenReturn(refreshedSnapshot);
        MesProRouteVersionCreateReqVO reqVO = new MesProRouteVersionCreateReqVO();
        reqVO.setRouteId(9001L);
        reqVO.setSourceRouteVersionId(1001L);
        reqVO.setChangeReason("调整排产能力");

        MesProRouteVersionDO candidate = service.createCandidate(reqVO);

        ArgumentCaptor<MesProRouteVersionDO> captor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).insert(captor.capture());
        assertEquals("V2", captor.getValue().getVersionNo());
        assertEquals(Boolean.FALSE, captor.getValue().getActive());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT, captor.getValue().getLifecycleStatus());
        assertEquals(active.getId(), captor.getValue().getSourceRouteVersionId());
        assertEquals(refreshedSnapshot, captor.getValue().getRouteSnapshotJson());
        assertTrue(captor.getValue().getRouteSnapshotJson().contains("FORM_BINDING_COPY_1"),
                "后续候选版本必须从当前工序配置生成包含 formBindings 的完整快照");
        verify(routeService).buildCurrentRouteSnapshotJson(9001L, 1001L);
        assertEquals("V2", candidate.getVersionNo());
        assertNull(candidate.getLastSavedBy(), "新建草稿不得继承来源版本保存人");
    }

    @Test
    void createCandidate_preservesExplicitUnboundSopWhenRemovingSharedChangeMarker() {
        MesProRouteVersionDO active = activeVersion();
        JSONObject root = JSON.parseObject(validSnapshotJson(9001L, "RT-9001", "工艺路线 V1"));
        JSONObject configs = root.getJSONObject("configSnapshots");
        configs.put("craftChangeId", 71L);
        configs.getJSONObject("flowGraph").getJSONArray("nodes").getJSONObject(0).put("sopFileUrl", null);
        when(routeVersionMapper.selectActiveByRouteIdForUpdate(9001L)).thenReturn(active);
        when(routeVersionMapper.selectMaxVersionNoByRouteId(9001L)).thenReturn("V1");
        when(routeService.buildCurrentRouteSnapshotJson(9001L, 1001L))
                .thenReturn(MesProRouteVersionSnapshotIdentityWriter.toSnapshotJson(root));
        var request = new MesProRouteVersionCreateReqVO();
        request.setRouteId(9001L);
        request.setSourceRouteVersionId(1001L);
        request.setChangeReason("SOP冻结回归");

        MesProRouteVersionDO candidate = service.createCandidate(request);

        JSONObject savedConfigs = JSON.parseObject(candidate.getRouteSnapshotJson()).getJSONObject("configSnapshots");
        assertFalse(savedConfigs.containsKey("craftChangeId"));
        JSONObject node = savedConfigs.getJSONObject("flowGraph").getJSONArray("nodes").getJSONObject(0);
        assertTrue(node.containsKey("sopFileUrl"));
        assertNull(node.get("sopFileUrl"));
    }

    @Test
    void createCandidate_shouldRejectSourceDrift() {
        MesProRouteVersionDO active = activeVersion();
        when(routeVersionMapper.selectActiveByRouteIdForUpdate(9001L)).thenReturn(active);
        MesProRouteVersionCreateReqVO reqVO = new MesProRouteVersionCreateReqVO();
        reqVO.setRouteId(9001L);
        reqVO.setSourceRouteVersionId(9999L);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.createCandidate(reqVO));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void createCandidate_shouldReturnExistingDraftAndRejectPendingOrReadyCandidate() {
        MesProRouteVersionDO active = activeVersion();
        MesProRouteVersionDO existingDraft = draftCandidate(active);
        MesProRouteVersionDO pendingApproval = openCandidate(active,
                MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL);
        MesProRouteVersionDO readyToPublish = openCandidate(active,
                MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH);
        when(routeVersionMapper.selectActiveByRouteIdForUpdate(9001L)).thenReturn(active);
        when(routeVersionMapper.selectOpenCandidateByRouteId(9001L))
                .thenReturn(existingDraft, pendingApproval, readyToPublish);

        MesProRouteVersionCreateReqVO reqVO = new MesProRouteVersionCreateReqVO();
        reqVO.setRouteId(9001L);
        reqVO.setSourceRouteVersionId(active.getId());
        reqVO.setChangeReason("列表编辑复用单一候选版本");

        MesProRouteVersionDO existing = service.createCandidate(reqVO);

        assertEquals(existingDraft.getId(), existing.getId());
        assertEquals(existingDraft.getVersionNo(), existing.getVersionNo());

        ServiceException pendingEx = assertThrows(ServiceException.class, () -> service.createCandidate(reqVO));
        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_CONFLICT.getCode(), pendingEx.getCode());

        ServiceException readyEx = assertThrows(ServiceException.class, () -> service.createCandidate(reqVO));
        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_CONFLICT.getCode(), readyEx.getCode());
        verify(routeVersionMapper, never()).insert(org.mockito.ArgumentMatchers.any(MesProRouteVersionDO.class));
    }

    @Test
    void createCandidate_shouldRejectExistingDraftWhenSourceDrifted() {
        MesProRouteVersionDO active = activeVersion();
        MesProRouteVersionDO existingDraft = draftCandidate(active);
        existingDraft.setSourceRouteVersionId(8888L);
        when(routeVersionMapper.selectActiveByRouteIdForUpdate(9001L)).thenReturn(active);
        when(routeVersionMapper.selectOpenCandidateByRouteId(9001L)).thenReturn(existingDraft);
        MesProRouteVersionCreateReqVO reqVO = new MesProRouteVersionCreateReqVO();
        reqVO.setRouteId(9001L);
        reqVO.setSourceRouteVersionId(active.getId());
        reqVO.setChangeReason("列表编辑复用单一候选版本");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.createCandidate(reqVO));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_CONFLICT.getCode(), ex.getCode());
        verify(routeVersionMapper, never()).insert(org.mockito.ArgumentMatchers.any(MesProRouteVersionDO.class));
    }

    @Test
    void submitCandidate_shouldRequireCompleteSnapshotAndEnterReadyToPublish() {
        MesProRouteVersionDO active = activeVersion();
        MesProRouteVersionDO candidate = draftCandidate(active);
        when(routeVersionMapper.selectByIdForUpdate(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(active);

        MesProRouteVersionDO submitted;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(500L);
            submitted = service.submitCandidate(candidate.getId());
        }

        ArgumentCaptor<MesProRouteVersionDO> captor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(captor.capture());
        assertEquals(candidate.getId(), captor.getValue().getId());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH,
                captor.getValue().getLifecycleStatus());
        assertEquals(500L, captor.getValue().getSubmittedBy());
        assertNotNull(captor.getValue().getSubmittedTime());
        assertEquals(null, captor.getValue().getApprovalProcessInstanceId());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH, submitted.getLifecycleStatus());
        assertEquals(null, submitted.getApprovalProcessInstanceId());
        verify(bpmProcessInstanceApi, never()).createProcessInstance(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(BpmProcessInstanceCreateReqDTO.class));
    }

    @Test
    void submitCandidate_shouldRecordSubmitterAuditFields() {
        MesProRouteVersionDO active = activeVersion();
        MesProRouteVersionDO candidate = draftCandidate(active);
        when(routeVersionMapper.selectByIdForUpdate(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(active);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(501L);

            MesProRouteVersionDO submitted = service.submitCandidate(candidate.getId());

            ArgumentCaptor<MesProRouteVersionDO> captor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
            verify(routeVersionMapper).updateById(captor.capture());
            assertEquals(501L, captor.getValue().getSubmittedBy());
            assertNotNull(captor.getValue().getSubmittedTime());
            assertEquals(501L, submitted.getSubmittedBy());
            assertNotNull(submitted.getSubmittedTime());
        }
    }

    @Test
    void submitCandidate_shouldNotStartPrivateBpmBeforePlatformPolicyPublish() {
        MesProRouteVersionDO active = activeVersion();
        MesProRouteVersionDO candidate = draftCandidate(active);
        when(routeVersionMapper.selectByIdForUpdate(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(active);

        MesProRouteVersionDO submitted;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(502L);
            submitted = service.submitCandidate(candidate.getId());
        }

        ArgumentCaptor<MesProRouteVersionDO> versionCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(versionCaptor.capture());
        assertEquals(candidate.getId(), versionCaptor.getValue().getId());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH,
                versionCaptor.getValue().getLifecycleStatus());
        assertEquals(502L, versionCaptor.getValue().getSubmittedBy());
        assertNotNull(versionCaptor.getValue().getSubmittedTime());
        assertEquals(null, versionCaptor.getValue().getApprovalProcessInstanceId());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH, submitted.getLifecycleStatus());
        assertEquals(null, submitted.getApprovalProcessInstanceId());
        verify(bpmProcessInstanceApi, never()).createProcessInstance(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(BpmProcessInstanceCreateReqDTO.class));
    }

    @Test
    void submitCandidate_shouldRejectWhenAnotherOpenCandidateExists() {
        MesProRouteVersionDO active = activeVersion();
        MesProRouteVersionDO candidate = draftCandidate(active);
        when(routeVersionMapper.selectByIdForUpdate(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.countOpenCandidatesByRouteId(candidate.getRouteId())).thenReturn(2L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.submitCandidate(candidate.getId()));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_CONFLICT.getCode(), ex.getCode());
        verify(bpmProcessInstanceApi, never()).createProcessInstance(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(BpmProcessInstanceCreateReqDTO.class));
    }

    @Test
    void withdrawCandidate_shouldCancelBpmAndReturnDraft() {
        MesProRouteVersionDO pending = openCandidate(activeVersion(),
                MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL);
        pending.setApprovalProcessInstanceId("fbede791-8138-11f1-80b5-00155d3585b8");
        when(routeVersionMapper.selectByIdForUpdate(pending.getId())).thenReturn(pending);
        when(routeVersionMapper.updateApprovalFieldsToDraft(pending.getId())).thenReturn(1);

        MesProRouteVersionDO withdrawn;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(503L);
            withdrawn = service.withdrawCandidate(pending.getId());
        }

        verify(bpmProcessInstanceApi).cancelProcessInstance(
                org.mockito.ArgumentMatchers.eq(503L),
                org.mockito.ArgumentMatchers.eq("fbede791-8138-11f1-80b5-00155d3585b8"),
                org.mockito.ArgumentMatchers.anyString());
        verify(routeVersionMapper).updateApprovalFieldsToDraft(pending.getId());
        verify(platformAdapter).recordWithdrawn(pending, 503L);
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT, withdrawn.getLifecycleStatus());
        assertEquals(null, withdrawn.getSubmittedBy());
        assertEquals(null, withdrawn.getSubmittedTime());
        assertEquals(null, withdrawn.getApprovalProcessInstanceId());
    }

    @Test
    void withdrawCandidate_shouldNotMutateDomainWhenBpmCancelFails() {
        MesProRouteVersionDO pending = openCandidate(activeVersion(),
                MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL);
        pending.setApprovalProcessInstanceId("fbede791-8138-11f1-80b5-00155d3585b8");
        when(routeVersionMapper.selectByIdForUpdate(pending.getId())).thenReturn(pending);
        doThrow(new IllegalStateException("approval cancel callback failed")).when(bpmProcessInstanceApi)
                .cancelProcessInstance(
                        org.mockito.ArgumentMatchers.eq(503L),
                        org.mockito.ArgumentMatchers.eq("fbede791-8138-11f1-80b5-00155d3585b8"),
                        org.mockito.ArgumentMatchers.anyString());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(503L);
            assertThrows(IllegalStateException.class, () -> service.withdrawCandidate(pending.getId()));
        }

        verify(routeVersionMapper, never()).updateApprovalFieldsToDraft(pending.getId());
        verify(platformAdapter, never()).recordWithdrawn(pending, 503L);
    }

    @Test
    void reopenRejectedCandidate_shouldReturnSameVersionToDraftWhenNoOpenCandidateExists() {
        MesProRouteVersionDO rejected = openCandidate(activeVersion(),
                MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED);
        when(routeVersionMapper.selectByIdForUpdate(rejected.getId())).thenReturn(rejected);
        when(routeVersionMapper.countOpenCandidatesByRouteId(rejected.getRouteId())).thenReturn(0L);

        MesProRouteVersionDO reopened = service.reopenRejectedCandidate(rejected.getId());

        ArgumentCaptor<MesProRouteVersionDO> captor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(captor.capture());
        assertEquals(rejected.getId(), captor.getValue().getId());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT, captor.getValue().getLifecycleStatus());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT, reopened.getLifecycleStatus());
    }

    @Test
    void getPublishBlockers_shouldExposeDriftAndSnapshotProblems() {
        MesProRouteVersionDO candidate = draftCandidate(activeVersion());
        candidate.setSourceRouteVersionId(1111L);
        candidate.setRouteSnapshotJson("{\"routeId\":9001}");
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(activeVersion());

        MesProRouteVersionBlockerRespVO blockers = service.getPublishBlockers(candidate.getId());

        assertFalse(blockers.getPublishable());
        assertTrue(blockers.getBlockers().contains("source active version drifted"));
        assertTrue(blockers.getBlockers().contains("route version snapshot is incomplete"));
    }

    @Test
    void getPublishBlockers_shouldRejectShallowSnapshotWithoutFrozenRouteIdentityAndFlowNodes() {
        MesProRouteVersionDO active = activeVersion();
        MesProRouteVersionDO candidate = draftCandidate(active);
        candidate.setRouteSnapshotJson(
                "{\"routeId\":9001,\"configSnapshots\":{\"flowGraph\":{},\"products\":[],\"scheduleConfigs\":[],\"batchUseConfigs\":[],\"scheduleUseConfigs\":[],\"productionProcessConfigSchemaVersion\":1,\"productionProcessConfigs\":[]}}");
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(active);

        MesProRouteVersionBlockerRespVO blockers = service.getPublishBlockers(candidate.getId());

        assertFalse(blockers.getPublishable());
        assertTrue(blockers.getBlockers().contains("route version snapshot is incomplete"));
    }

    @Test
    void cancelCandidate_shouldNotAllowActiveVersion() {
        MesProRouteVersionDO active = activeVersion();
        when(routeVersionMapper.selectByIdForUpdate(active.getId())).thenReturn(active);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.cancelCandidate(active.getId(), false));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE.getCode(), ex.getCode());
    }

    @Test
    void cancelCandidate_shouldClosePlatformOpenCandidate() {
        MesProRouteVersionDO candidate = draftCandidate(activeVersion());
        when(routeVersionMapper.selectByIdForUpdate(candidate.getId())).thenReturn(candidate);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(507L);
            service.cancelCandidate(candidate.getId(), false);
        }

        ArgumentCaptor<MesProRouteVersionDO> captor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(captor.capture());
        assertEquals(candidate.getId(), captor.getValue().getId());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_CANCELLED, captor.getValue().getLifecycleStatus());
        verify(platformAdapter).recordCancelled(candidate, 507L);
    }

    @Test
    void discardUnsavedCandidate_shouldPreserveDraftSavedByAnotherPage() {
        MesProRouteVersionDO candidate = draftCandidate(activeVersion());
        candidate.setLastSavedBy(9908100850L);
        when(routeVersionMapper.selectByIdForUpdate(candidate.getId())).thenReturn(candidate);

        MesProRouteVersionDO result = service.cancelCandidate(candidate.getId(), true);

        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT, result.getLifecycleStatus());
        assertEquals(9908100850L, result.getLastSavedBy());
        verify(routeVersionMapper, never()).updateById(any(MesProRouteVersionDO.class));
        verifyNoInteractions(platformAdapter);
    }

    @Test
    void discardUnsavedCandidate_shouldPreserveConcurrentlyPublishedVersion() {
        MesProRouteVersionDO active = activeVersion();
        when(routeVersionMapper.selectByIdForUpdate(active.getId())).thenReturn(active);

        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE,
                service.cancelCandidate(active.getId(), true).getLifecycleStatus());
        verify(routeVersionMapper, never()).updateById(any(MesProRouteVersionDO.class));
        verifyNoInteractions(platformAdapter);
    }

    @Test
    void discardUnsavedCandidate_shouldCancelNeverSavedDraft() {
        MesProRouteVersionDO candidate = draftCandidate(activeVersion());
        when(routeVersionMapper.selectByIdForUpdate(candidate.getId())).thenReturn(candidate);
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(507L);
            assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_CANCELLED,
                    service.cancelCandidate(candidate.getId(), true).getLifecycleStatus());
        }
        verify(platformAdapter).recordCancelled(candidate, 507L);
    }

    @Test
    void cancelDraftThenCreateCandidate_shouldCreateNewDraftFromCurrentActiveVersion() {
        MesProRouteVersionDO active = activeVersion();
        MesProRouteVersionDO candidate = draftCandidate(active);
        String refreshedSnapshot = validSnapshotJsonWithBatchBinding();
        when(routeVersionMapper.selectByIdForUpdate(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteIdForUpdate(active.getRouteId())).thenReturn(active);
        when(routeVersionMapper.selectOpenCandidateByRouteId(active.getRouteId())).thenReturn(null);
        when(routeVersionMapper.selectMaxVersionNoByRouteId(active.getRouteId())).thenReturn("V2");
        when(routeService.buildCurrentRouteSnapshotJson(active.getRouteId(), active.getId()))
                .thenReturn(refreshedSnapshot);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(508L);
            service.cancelCandidate(candidate.getId(), false);
        }
        MesProRouteVersionCreateReqVO reqVO = new MesProRouteVersionCreateReqVO();
        reqVO.setRouteId(active.getRouteId());
        reqVO.setSourceRouteVersionId(active.getId());
        reqVO.setChangeReason("删除草稿后重新编辑");

        MesProRouteVersionDO nextDraft = service.createCandidate(reqVO);

        ArgumentCaptor<MesProRouteVersionDO> updateCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(updateCaptor.capture());
        assertEquals(candidate.getId(), updateCaptor.getValue().getId());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_CANCELLED,
                updateCaptor.getValue().getLifecycleStatus());
        ArgumentCaptor<MesProRouteVersionDO> insertCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).insert(insertCaptor.capture());
        assertEquals("V3", insertCaptor.getValue().getVersionNo());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT,
                insertCaptor.getValue().getLifecycleStatus());
        assertEquals(active.getId(), insertCaptor.getValue().getSourceRouteVersionId());
        assertEquals(refreshedSnapshot, insertCaptor.getValue().getRouteSnapshotJson());
        assertEquals("V3", nextDraft.getVersionNo());
        verify(platformAdapter).recordCancelled(candidate, 508L);
    }

    @Test
    void listByRouteId_shouldDelegateToMapper() {
        when(routeVersionMapper.selectListByRouteId(9001L)).thenReturn(List.of(activeVersion()));

        assertEquals(1, service.listByRouteId(9001L).size());
    }

    private MesProRouteVersionDO activeVersion() {
        return MesProRouteVersionDO.builder()
                .id(1001L)
                .routeId(9001L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .routeSnapshotJson(validSnapshotJson(9001L, "RT-9001", "工艺路线 V1"))
                .build();
    }

    private MesProRouteVersionDO draftCandidate(MesProRouteVersionDO active) {
        return MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(active.getRouteId())
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .sourceRouteVersionId(active.getId())
                .routeSnapshotJson(active.getRouteSnapshotJson())
                .build();
    }

    private MesProRouteVersionDO openCandidate(MesProRouteVersionDO active, String lifecycleStatus) {
        MesProRouteVersionDO candidate = draftCandidate(active);
        candidate.setLifecycleStatus(lifecycleStatus);
        candidate.setApprovalProcessInstanceId(
                MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL.equals(lifecycleStatus)
                        ? "fbede791-8138-11f1-80b5-00155d3585b8" : null);
        return candidate;
    }

    private String validSnapshotJson(Long routeId, String routeCode, String routeName) {
        return """
                {
                  "routeId": %d,
                  "routeCode": "%s",
                  "routeName": "%s",
                  "configSnapshots": {
                    "flowGraph": {
                      "nodes": [
                        {"routeProcessId": 10, "processId": 20, "sort": 1, "sopFileUrl": "/sop.pdf"}
                      ],
                      "edges": []
                    },
                    "products": [],
                    "scheduleConfigs": [],
                    "batchUseConfigs": [],
                    "scheduleUseConfigs": [],
                    "productionProcessConfigSchemaVersion": 1,
                    "productionProcessConfigs": [
                      {
                        "routeProcessId": 10,
                        "processId": 20,
                        "overagePercent": 10,
                        "lossReasons": [],
                        "deviceSelectionGroups": [],
                        "parameterRules": []
                      }
                    ]
                  }
                }
                """.formatted(routeId, routeCode, routeName);
    }

    private String validSnapshotJsonWithBatchBinding() {
        return """
                {
                  "routeId": 9001,
                  "routeCode": "RT-9001",
                  "routeName": "工艺路线 V1",
                  "configSnapshots": {
                    "flowGraph": {
                      "nodes": [
                        {"routeProcessId": 10, "processId": 20, "sort": 1, "sopFileUrl": "/sop.pdf"}
                      ],
                      "edges": []
                    },
                    "products": [],
                    "scheduleConfigs": [],
                    "batchUseConfigs": [
                      {
                        "routeProcessId": 10,
                        "formBindings": [
                          {
                            "formBindingKey": "FORM_BINDING_COPY_1",
                            "batchRecordReportId": "FORM_BINDING_COPY_1",
                            "candidateSourceType": "USERS",
                            "candidateSourceIds": "914520"
                          }
                        ]
                      }
                    ],
                    "scheduleUseConfigs": [],
                    "productionProcessConfigSchemaVersion": 1,
                    "productionProcessConfigs": [
                      {
                        "routeProcessId": 10,
                        "processId": 20,
                        "overagePercent": 10,
                        "lossReasons": [],
                        "deviceSelectionGroups": [],
                        "parameterRules": []
                      }
                    ]
                  }
                }
                """;
    }

    private String snapshotWithoutProductionConfigs() {
        JSONObject snapshot = JSON.parseObject(validSnapshotJson(9001L, "RT-9001", "工艺路线 V1"));
        snapshot.getJSONObject("configSnapshots").remove("productionProcessConfigSchemaVersion");
        snapshot.getJSONObject("configSnapshots").remove("productionProcessConfigs");
        return snapshot.toJSONString();
    }

    private MesTeamLeaderProcessConfigRow legacyProcessConfig(BigDecimal overagePercent) {
        MesTeamLeaderProcessConfigParameter parameter = new MesTeamLeaderProcessConfigParameter()
                .setParameterCode("cleaning-medium")
                .setParameterName("清洗介质")
                .setValueType("TEXT_STANDARD")
                .setStandardText("纯化水")
                .setEnabled(Boolean.TRUE);
        MesTeamLeaderProcessConfigDevice device = new MesTeamLeaderProcessConfigDevice()
                .setDeviceId(7001L)
                .setDeviceGroupKey("CLEANING")
                .setSelectionMode("MULTIPLE")
                .setMapped(Boolean.TRUE)
                .setParameters(List.of(parameter));
        return new MesTeamLeaderProcessConfigRow()
                .setRouteId(9001L)
                .setRouteProcessId(10L)
                .setProcessId(20L)
                .setProcessCode("P-CLEAN")
                .setProcessName("清洗")
                .setSort(1)
                .setOveragePercent(overagePercent)
                .setLossReasons(List.of(new MesTeamLeaderLossReasonItem()
                        .setId(8301L)
                        .setReasonCode("LOSS-001")
                        .setReasonName("正常损耗")
                        .setEnabled(Boolean.TRUE)))
                .setDevices(List.of(device));
    }
}
