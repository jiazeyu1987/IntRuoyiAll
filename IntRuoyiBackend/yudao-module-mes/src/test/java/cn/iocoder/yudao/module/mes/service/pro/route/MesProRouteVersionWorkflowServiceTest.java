package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionBlockerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionCreateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteVersionWorkflowServiceTest {

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
    void createCandidate_shouldCopyActiveSnapshotAsDraft() {
        String completeSnapshot = validSnapshotJsonWithBatchBinding();
        MesProRouteVersionDO active = activeVersion();
        active.setRouteSnapshotJson(completeSnapshot);
        when(routeVersionMapper.selectActiveByRouteIdForUpdate(9001L)).thenReturn(active);
        when(routeVersionMapper.selectMaxVersionNoByRouteId(9001L)).thenReturn("V1");
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
        assertEquals(completeSnapshot, captor.getValue().getRouteSnapshotJson());
        assertTrue(captor.getValue().getRouteSnapshotJson().contains("FORM_BINDING_COPY_1"),
                "候选版本必须完整继承 active 快照中的批记录表单槽位绑定");
        verify(routeService, never()).buildCurrentRouteSnapshotJson(9001L, 1001L);
        assertEquals("V2", candidate.getVersionNo());
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
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
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
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
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
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
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
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
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
        when(routeVersionMapper.selectById(pending.getId())).thenReturn(pending);

        MesProRouteVersionDO withdrawn;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(503L);
            withdrawn = service.withdrawCandidate(pending.getId());
        }

        verify(bpmProcessInstanceApi).cancelProcessInstance(
                org.mockito.ArgumentMatchers.eq(503L),
                org.mockito.ArgumentMatchers.eq("fbede791-8138-11f1-80b5-00155d3585b8"),
                org.mockito.ArgumentMatchers.anyString());
        verify(routeVersionMapper, never()).updateApprovalFieldsToDraft(pending.getId());
        verify(platformAdapter, never()).recordWithdrawn(pending, 503L);
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
        when(routeVersionMapper.selectById(pending.getId())).thenReturn(pending);
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
        when(routeVersionMapper.selectById(rejected.getId())).thenReturn(rejected);
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
                "{\"routeId\":9001,\"configSnapshots\":{\"flowGraph\":{},\"products\":[],\"scheduleConfigs\":[],\"batchUseConfigs\":[]}}");
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(candidate.getRouteId())).thenReturn(active);

        MesProRouteVersionBlockerRespVO blockers = service.getPublishBlockers(candidate.getId());

        assertFalse(blockers.getPublishable());
        assertTrue(blockers.getBlockers().contains("route version snapshot is incomplete"));
    }

    @Test
    void cancelCandidate_shouldNotAllowActiveVersion() {
        MesProRouteVersionDO active = activeVersion();
        when(routeVersionMapper.selectById(active.getId())).thenReturn(active);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.cancelCandidate(active.getId()));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE.getCode(), ex.getCode());
    }

    @Test
    void cancelCandidate_shouldClosePlatformOpenCandidate() {
        MesProRouteVersionDO candidate = draftCandidate(activeVersion());
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(507L);
            service.cancelCandidate(candidate.getId());
        }

        ArgumentCaptor<MesProRouteVersionDO> captor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(captor.capture());
        assertEquals(candidate.getId(), captor.getValue().getId());
        assertEquals(MesProRouteVersionLifecycleServiceImpl.STATUS_CANCELLED, captor.getValue().getLifecycleStatus());
        verify(platformAdapter).recordCancelled(candidate, 507L);
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
                        {"routeProcessId": 10, "processId": 20, "sort": 1}
                      ],
                      "edges": []
                    },
                    "products": [],
                    "scheduleConfigs": [],
                    "batchUseConfigs": [],
                    "scheduleUseConfigs": []
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
                        {"routeProcessId": 10, "processId": 20, "sort": 1}
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
                    "scheduleUseConfigs": []
                  }
                }
                """;
    }
}
