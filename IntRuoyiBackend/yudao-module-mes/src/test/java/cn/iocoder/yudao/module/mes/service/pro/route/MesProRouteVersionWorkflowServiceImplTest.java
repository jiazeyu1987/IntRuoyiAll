package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteVersionWorkflowServiceImplTest {

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

    @Test
    void createCandidate_afterReviewedArchive_shouldCreateNextDraftWithoutUsingSummaryAsSource() {
        MesProRouteVersionDO legacy = legacyVersion();
        when(routeVersionMapper.selectListByRouteId(legacy.getRouteId())).thenReturn(java.util.List.of(legacy));
        when(routeVersionMapper.selectMaxVersionNoByRouteId(legacy.getRouteId())).thenReturn("V1");
        com.alibaba.fastjson.JSONObject draftSnapshot = com.alibaba.fastjson.JSON.parseObject(completeRouteSnapshot());
        draftSnapshot.getJSONObject("configSnapshots").put("productionProcessConfigs", new com.alibaba.fastjson.JSONArray());
        when(routeService.buildCurrentRouteSnapshotJson(legacy.getRouteId(), null)).thenReturn(draftSnapshot.toJSONString());
        doAnswer(invocation -> {
            ((MesProRouteVersionDO) invocation.getArgument(0)).setId(1002L);
            return 1;
        }).when(routeVersionMapper).insert(any(MesProRouteVersionDO.class));
        var request = new cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionCreateReqVO();
        request.setRouteId(legacy.getRouteId());
        try (var identity = mockStatic(MesRouteLegacySummaryIdentity.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            identity.when(() -> MesRouteLegacySummaryIdentity.isKnownArchivedIdentity(legacy)).thenReturn(true);
            MesProRouteVersionDO created = service.createCandidate(request);
            assertEquals("V2", created.getVersionNo());
            assertEquals("DRAFT", created.getLifecycleStatus());
            assertEquals(Boolean.FALSE, created.getActive());
            org.junit.jupiter.api.Assertions.assertNull(created.getSourceRouteVersionId());
            assertEquals(MesProRouteSnapshotCanonicalizer.FORMAT_VERSION, created.getRouteSnapshotFormatVersion());
            verify(platformAdapter).recordFirstCandidateCreated(eq(legacy), eq(created), isNull(), isNull());
            verify(platformAdapter, org.mockito.Mockito.never()).recordActiveRegistered(any(), any(), any());
            when(routeVersionMapper.selectByIdForUpdate(created.getId())).thenReturn(created);
            when(routeVersionMapper.countOpenCandidatesByRouteId(created.getRouteId())).thenReturn(1L);
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> service.submitCandidate(created.getId()));
            verify(routeVersionMapper, org.mockito.Mockito.never()).updateById(any(MesProRouteVersionDO.class));
            MesProRouteVersionSnapshotIdentityWriter.apply(created, completeRouteSnapshot());
            try (var security = mockStatic(SecurityFrameworkUtils.class)) {
                security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(502L);
                assertEquals("READY_TO_PUBLISH", service.submitCandidate(created.getId()).getLifecycleStatus());
                verify(platformAdapter).recordSubmitted(created, 502L, null);
                verify(platformAdapter).recordApproved(eq(created), eq(502L),
                        org.mockito.ArgumentMatchers.startsWith("ROUTE_VERSION_READY_TO_PUBLISH:"));
            }
        }
    }

    @Test
    void createCandidate_afterArchive_shouldStillRejectIncompleteCurrentGraphWithoutWriting() {
        MesProRouteVersionDO legacy = legacyVersion();
        when(routeVersionMapper.selectListByRouteId(legacy.getRouteId())).thenReturn(java.util.List.of(legacy));
        when(routeService.buildCurrentRouteSnapshotJson(legacy.getRouteId(), null)).thenReturn("{\"routeId\":9001}");
        var request = new cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionCreateReqVO();
        request.setRouteId(legacy.getRouteId());
        try (var identity = mockStatic(MesRouteLegacySummaryIdentity.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            identity.when(() -> MesRouteLegacySummaryIdentity.isKnownArchivedIdentity(legacy)).thenReturn(true);
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> service.createCandidate(request));
            verify(routeVersionMapper, org.mockito.Mockito.never()).insert(any(MesProRouteVersionDO.class));
            org.mockito.Mockito.verifyNoInteractions(platformAdapter);
        }
    }

    @Test
    void historicalGet_shouldPreserveOriginalJsonButMutationMustRejectIt() {
        MesProRouteVersionDO legacy = legacyVersion();
        legacy.setRouteSnapshotJson("{\"routeId\":9001,\"remark\":\"original historical summary\"}");
        when(routeVersionMapper.selectById(legacy.getId())).thenReturn(legacy);
        when(routeVersionMapper.selectByIdForUpdate(legacy.getId())).thenReturn(legacy);
        assertEquals(legacy.getRouteSnapshotJson(), service.getVersion(legacy.getId()).getRouteSnapshotJson());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> service.submitCandidate(legacy.getId()));
        verify(routeVersionMapper, org.mockito.Mockito.never()).updateById(any(MesProRouteVersionDO.class));
        org.mockito.Mockito.verifyNoInteractions(platformAdapter, bpmProcessInstanceApi, routeService);
    }

    private MesProRouteVersionDO legacyVersion() {
        return MesProRouteVersionDO.builder().id(5L).tenantId(122L).routeId(9001L).versionNo("V1")
                .active(false).lifecycleStatus(MesRouteLegacySummaryIdentity.STATUS)
                .routeSnapshotFormatVersion(MesRouteLegacySummaryIdentity.FORMAT_VERSION).build();
    }

    @Test
    void submitCandidate_shouldRecordControlledContentWhileNativeCandidateIsStillDraft() {
        MesProRouteVersionDO active = activeVersion();
        MesProRouteVersionDO candidate = draftCandidate(active);
        when(routeVersionMapper.selectByIdForUpdate(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.countOpenCandidatesByRouteId(candidate.getRouteId())).thenReturn(1L);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(active);
        doAnswer(invocation -> {
            MesProRouteVersionDO submittedCandidate = invocation.getArgument(0);
            assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT,
                    submittedCandidate.getLifecycleStatus(),
                    "controlled content submit must see the historical draft state before native status mutation");
            return null;
        }).when(platformAdapter).recordSubmitted(any(MesProRouteVersionDO.class), eq(502L), isNull());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(502L);

            MesProRouteVersionDO submitted = service.submitCandidate(candidate.getId());

            assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH,
                    submitted.getLifecycleStatus());
        }

        verify(platformAdapter).recordApproved(any(MesProRouteVersionDO.class), eq(502L),
                org.mockito.ArgumentMatchers.startsWith("ROUTE_VERSION_READY_TO_PUBLISH:"));
        ArgumentCaptor<MesProRouteVersionDO> updateCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(updateCaptor.capture());
        assertEquals(candidate.getId(), updateCaptor.getValue().getId());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH,
                updateCaptor.getValue().getLifecycleStatus());
    }

    private MesProRouteVersionDO activeVersion() {
        return MesProRouteVersionDO.builder()
                .id(1001L)
                .routeId(9001L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .build();
    }

    private MesProRouteVersionDO draftCandidate(MesProRouteVersionDO active) {
        return MesProRouteVersionDO.builder()
                .id(1002L)
                .routeId(active.getRouteId())
                .versionNo("V5")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .sourceRouteVersionId(active.getId())
                .routeSnapshotJson(completeRouteSnapshot())
                .build();
    }

    private String completeRouteSnapshot() {
        return """
                {"routeId":9001,"routeCode":"ROUTE-XLSX-00001","routeName":"route",
                 "configSnapshots":{"flowGraph":{"valid":true,"nodes":[{"routeProcessId":922483,"processId":201}]},
                 "products":[],"scheduleConfigs":[],"batchUseConfigs":[],"scheduleUseConfigs":[],
                 "productionProcessConfigSchemaVersion":1,
                 "productionProcessConfigs":[{"routeProcessId":922483,"processId":201,
                   "overagePercent":0,"lossReasons":[],"deviceSelectionGroups":[],"parameterRules":[]}]}}
                """;
    }
}
