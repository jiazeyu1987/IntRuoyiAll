package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitIterationReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRouteReadinessRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileCheckoutDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileCheckoutMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileChangeTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ITERATION_NOT_LATEST;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DccWorkingIterationSubmissionServiceTest {

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileMasterMapper masterMapper;
    @Mock
    private DccControlledFileCheckoutMapper checkoutMapper;
    @Mock
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Mock
    private DccControlledFileRouteReadinessService routeReadinessService;
    @Mock
    private DccControlledFileApprovalRouteAssigneeResolver routeAssigneeResolver;
    @Mock
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Mock
    private DccControlledContentAdapter platformAdapter;
    @Mock
    private DccProjectAccessService projectAccessService;
    @Mock
    private DccControlledFileCategoryPermissionSupport categoryPermissionSupport;

    private DccControlledFileWorkflowServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        service = new DccControlledFileWorkflowServiceImpl();
        ReflectionTestUtils.setField(service, "controlledFileMapper", controlledFileMapper);
        ReflectionTestUtils.setField(service, "controlledFileMasterMapper", masterMapper);
        ReflectionTestUtils.setField(service, "checkoutMapper", checkoutMapper);
        ReflectionTestUtils.setField(service, "routeSnapshotMapper", routeSnapshotMapper);
        ReflectionTestUtils.setField(service, "routeReadinessService", routeReadinessService);
        ReflectionTestUtils.setField(service, "approvalRouteAssigneeResolver", routeAssigneeResolver);
        ReflectionTestUtils.setField(service, "bpmProcessInstanceApi", bpmProcessInstanceApi);
        ReflectionTestUtils.setField(service, "platformAdapter", platformAdapter);
        ReflectionTestUtils.setField(service, "projectAccessService", projectAccessService);
        ReflectionTestUtils.setField(service, "categoryPermissionSupport", categoryPermissionSupport);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void submitLatestWorkingIteration_startsApprovalWithoutCreatingAnotherIteration() {
        DccControlledFileDO active = iteration(900L, "A/1", DccControlledFileStatusEnum.ACTIVE.getStatus());
        DccControlledFileDO working = iteration(901L, "B/2", DccControlledFileStatusEnum.WORKING.getStatus());
        when(controlledFileMapper.selectById(901L)).thenReturn(working);
        when(masterMapper.selectByIdForUpdate(700L)).thenReturn(master());
        when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(active, working));
        when(categoryPermissionSupport.hasCategoryPermission(eq(10L), eq(99L), any())).thenReturn(true);
        when(checkoutMapper.selectActiveByMasterId(1L, 700L)).thenReturn(null);
        DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode node =
                new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(
                        1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "文控审核", 1,
                        "USER", 200L, List.of(200L), "ANY", 100, false, List.of(200L));
        DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute route =
                new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(
                        DccCategoryApprovalRouteDO.builder().id(30L).versionNo(2).build(), List.of(node));
        when(routeReadinessService.evaluate(10L, 99L, List.of())).thenReturn(
                new DccControlledFileRouteReadinessService.RouteReadinessEvaluation(route,
                        DccControlledFileRouteReadinessRespVO.builder()
                                .ready(true).nodes(List.of()).blockers(List.of()).build()));
        when(routeAssigneeResolver.buildStartUserSelectAssigneeMap(List.of(node))).thenReturn(
                Map.of(DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), List.of(200L)));
        when(routeAssigneeResolver.buildApproveUserSelectAssigneeMap(List.of(node))).thenReturn(Map.of());
        when(bpmProcessInstanceApi.createProcessInstance(eq(99L), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("proc-working-901");
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);
        DccControlledFileSubmitIterationReqVO request = new DccControlledFileSubmitIterationReqVO();
        request.setIdempotencyKey("submit-working-901");
        request.setSelectedSignoffUserIds(List.of());

        Long result = service.submitWorkingIteration(99L, 901L, request);

        assertEquals(901L, result);
        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(routeSnapshotMapper).insert(any(DccControlledFileRouteSnapshotDO.class));
        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper, org.mockito.Mockito.times(2)).updateById(updateCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus(),
                updateCaptor.getAllValues().get(0).getStatus());
        assertEquals("proc-working-901", updateCaptor.getAllValues().get(1).getProcessInstanceId());
        verify(platformAdapter).recordSubmitted(working, 99L, "proc-working-901");
    }

    @Test
    void submitInitialNewWorkingIteration_usesTheSameExistingIterationApprovalBoundary() {
        DccControlledFileDO working = iteration(901L, "A/1", DccControlledFileStatusEnum.WORKING.getStatus());
        working.setChangeType(DccControlledFileChangeTypeEnum.NEW.getCode());
        when(controlledFileMapper.selectById(901L)).thenReturn(working);
        when(masterMapper.selectByIdForUpdate(700L)).thenReturn(
                DccControlledFileMasterDO.builder().id(700L).build());
        when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(working));
        when(categoryPermissionSupport.hasCategoryPermission(eq(10L), eq(99L), any())).thenReturn(true);
        when(checkoutMapper.selectActiveByMasterId(1L, 700L)).thenReturn(null);
        DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode node =
                new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(
                        1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), "文控审核", 1,
                        "USER", 200L, List.of(200L), "ANY", 100, false, List.of(200L));
        DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute route =
                new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(
                        DccCategoryApprovalRouteDO.builder().id(30L).versionNo(2).build(), List.of(node));
        when(routeReadinessService.evaluate(10L, 99L, List.of())).thenReturn(
                new DccControlledFileRouteReadinessService.RouteReadinessEvaluation(route,
                        DccControlledFileRouteReadinessRespVO.builder()
                                .ready(true).nodes(List.of()).blockers(List.of()).build()));
        when(routeAssigneeResolver.buildStartUserSelectAssigneeMap(List.of(node))).thenReturn(
                Map.of(DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), List.of(200L)));
        when(routeAssigneeResolver.buildApproveUserSelectAssigneeMap(List.of(node))).thenReturn(Map.of());
        when(bpmProcessInstanceApi.createProcessInstance(eq(99L), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("proc-new-901");
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);
        DccControlledFileSubmitIterationReqVO request = new DccControlledFileSubmitIterationReqVO();
        request.setIdempotencyKey("submit-new-901");

        assertEquals(901L, service.submitWorkingIteration(99L, 901L, request));

        verify(controlledFileMapper, never()).insert(any(DccControlledFileDO.class));
        verify(platformAdapter).recordSubmitted(working, 99L, "proc-new-901");
    }

    @Test
    void submitOlderWorkingIteration_isRejectedBeforeApprovalSideEffects() {
        DccControlledFileDO older = iteration(901L, "B/1", DccControlledFileStatusEnum.WORKING.getStatus());
        DccControlledFileDO latest = iteration(902L, "B/2", DccControlledFileStatusEnum.WORKING.getStatus());
        when(controlledFileMapper.selectById(901L)).thenReturn(older);
        when(masterMapper.selectByIdForUpdate(700L)).thenReturn(master());
        when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(older, latest));
        DccControlledFileSubmitIterationReqVO request = new DccControlledFileSubmitIterationReqVO();
        request.setIdempotencyKey("submit-old-901");

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.submitWorkingIteration(99L, 901L, request));

        assertEquals(CONTROLLED_FILE_ITERATION_NOT_LATEST.getCode(), error.getCode());
        verify(routeSnapshotMapper, never()).insert(any(DccControlledFileRouteSnapshotDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(), any());
    }

    @Test
    void submitWorkingIteration_fromRevisionOlderThanCurrentActive_isRejectedBeforeApprovalSideEffects() {
        DccControlledFileDO staleWorking = iteration(901L, "A/2", DccControlledFileStatusEnum.WORKING.getStatus());
        DccControlledFileDO currentActive = iteration(900L, "B/1", DccControlledFileStatusEnum.ACTIVE.getStatus());
        when(controlledFileMapper.selectById(901L)).thenReturn(staleWorking);
        when(masterMapper.selectByIdForUpdate(700L)).thenReturn(master());
        when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(staleWorking, currentActive));
        DccControlledFileSubmitIterationReqVO request = new DccControlledFileSubmitIterationReqVO();
        request.setIdempotencyKey("submit-stale-revision-901");

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.submitWorkingIteration(99L, 901L, request));

        assertEquals(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED.getCode(), error.getCode());
        verify(routeSnapshotMapper, never()).insert(any(DccControlledFileRouteSnapshotDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(), any());
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void submitWorkingIteration_withActiveCheckout_isRejectedBeforeApprovalSideEffects() {
        DccControlledFileDO working = iteration(901L, "B/2", DccControlledFileStatusEnum.WORKING.getStatus());
        DccControlledFileDO currentActive = iteration(900L, "A/1", DccControlledFileStatusEnum.ACTIVE.getStatus());
        when(controlledFileMapper.selectById(901L)).thenReturn(working);
        when(masterMapper.selectByIdForUpdate(700L)).thenReturn(master());
        when(controlledFileMapper.selectListByMasterId(700L)).thenReturn(List.of(currentActive, working));
        when(checkoutMapper.selectActiveByMasterId(1L, 700L)).thenReturn(
                DccControlledFileCheckoutDO.builder().id(88L).masterId(700L).baseIterationId(901L)
                        .actorId(99L).status("ACTIVE").build());
        DccControlledFileSubmitIterationReqVO request = new DccControlledFileSubmitIterationReqVO();
        request.setIdempotencyKey("submit-checked-out-901");

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.submitWorkingIteration(99L, 901L, request));

        assertEquals(CONTROLLED_FILE_ITERATION_SUBMIT_NOT_ALLOWED.getCode(), error.getCode());
        verify(routeSnapshotMapper, never()).insert(any(DccControlledFileRouteSnapshotDO.class));
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(), any());
    }

    private DccControlledFileDO iteration(Long id, String versionNo, String status) {
        DccWindchillVersionNumber version = DccWindchillVersionNumber.parse(versionNo);
        return DccControlledFileDO.builder()
                .id(id).tenantId(1L).masterId(700L).categoryId(10L).dccProjectCodeId(3000L)
                .fileNumber("SOP-001").versionNo(versionNo).revisionCode(version.revisionCode())
                .iterationNo(version.iterationNo()).changeType(DccControlledFileChangeTypeEnum.REVISION.getCode())
                .status(status).requesterId(99L).build();
    }

    private DccControlledFileMasterDO master() {
        return DccControlledFileMasterDO.builder().id(700L).currentActiveControlledFileId(900L).build();
    }
}
