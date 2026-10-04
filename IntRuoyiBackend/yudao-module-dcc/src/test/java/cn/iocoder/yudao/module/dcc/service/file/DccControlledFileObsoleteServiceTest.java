package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.BusinessActionContextReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceCreateReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceSubmitReqVO;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileObsoleteAuditDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileObsoleteAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileMasterStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileObsoleteServiceTest extends BaseMockitoUnitTest {
    @Mock private DccWorkflowFileStateAudit fileStateAudit;
    @org.mockito.junit.jupiter.MockitoSettings(strictness=org.mockito.quality.Strictness.LENIENT)
    @Test void lockedObsoleteEffectCannotWriteIntoAForeignTenantVersion() {
        var locked=approvedEffectIdentity();locked.setTenantId(122L);assertObsoleteIdentityDenied();
    }
    @org.mockito.junit.jupiter.MockitoSettings(strictness=org.mockito.quality.Strictness.LENIENT)
    @Test void lockedObsoleteEffectCannotRetainThePrelockMasterAfterItsFileMasterChanged() {
        var locked=approvedEffectIdentity();locked.setMasterId(701L);assertObsoleteIdentityDenied();
    }
    @org.mockito.junit.jupiter.MockitoSettings(strictness=org.mockito.quality.Strictness.LENIENT)
    @Test void lockedObsoleteEffectCannotUseAForeignTenantMasterForTheSelectedFile() {
        approvedEffectIdentity();when(controlledFileMasterMapper.selectByIdForUpdate(700L)).thenReturn(DccControlledFileMasterDO.builder().id(700L).tenantId(122L).build());
        assertObsoleteIdentityDenied();
    }
    @org.mockito.junit.jupiter.MockitoSettings(strictness=org.mockito.quality.Strictness.LENIENT)
    @Test void lockedObsoleteEffectCannotReplaceTheRequestedFileWithAnotherReturnedId() {
        var locked=approvedEffectIdentity();locked.setId(901L);assertObsoleteIdentityDenied();
    }
    private DccControlledFileDO approvedEffectIdentity() {
        var initial=DccControlledFileDO.builder().id(900L).masterId(700L).tenantId(1L).categoryId(10L).versionNo("A/1").status("ACTIVE").build();
        var locked=DccControlledFileDO.builder().id(900L).masterId(700L).tenantId(1L).categoryId(10L).versionNo("A/1").status("ACTIVE").build();
        when(controlledFileMapper.selectById(900L)).thenReturn(initial);
        org.mockito.Mockito.doReturn(locked).when(controlledFileMapper).selectByIdAndTenantForUpdate(1L,900L);
        when(permissionSupport.hasCategoryPermission(10L,99L,DccFileCategoryPermissionActionEnum.OBSOLETE)).thenReturn(true);
        var process=org.mockito.Mockito.mock(org.flowable.engine.history.HistoricProcessInstance.class);
        when(process.getId()).thenReturn("approved-round");when(process.getTenantId()).thenReturn("1");when(process.getStartUserId()).thenReturn("99");
        when(process.getProcessDefinitionKey()).thenReturn(DccControlledFileProcessDefinitionKeys.OBSOLETE);
        when(process.getProcessVariables()).thenReturn(Map.of("PROCESS_STATUS",2,"systemCode","DCC","objectType","CONTROLLED_FILE","actionCode","OBSOLETE","objectId","900","objectVersion","A/1"));
        when(obsoleteProcessService.getHistoricProcessInstance("approved-round")).thenReturn(process);
        when(obsoleteEvidenceGuard.require(any(),any(),any())).thenReturn(100L);
        ReflectionTestUtils.setField(obsoleteService,"messageDeliveryService",org.mockito.Mockito.mock(DccControlledFileMessageDeliveryService.class));
        return locked;
    }
    private void assertObsoleteIdentityDenied() {
        var request=new DccControlledFileObsoleteReqVO();request.setReason("作废");request.setApprovalProcessInstanceId("approved-round");request.setApprovedVersionNo("A/1");
        assertServiceException(()->obsoleteService.applyApprovedObsoleteControlledFile(99L,900L,request),CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED);
        verify(obsoleteProcessService,never()).getHistoricProcessInstance(any());
        verify(obsoleteArchiveRequestService,never()).request(any(),any());
        verify(obsoleteRetentionService,never()).retain(any(),any(),any());
        verify(controlledFileMapper,never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void obsoleteDomainEffectWithoutItsApprovedRoundCannotWriteAnObsoleteFact() {
        var request=new DccControlledFileObsoleteReqVO();request.setReason("作废");
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder().id(900L).masterId(700L).tenantId(1L)
                .categoryId(10L).versionNo("A/1").status("ACTIVE").build());
        when(permissionSupport.hasCategoryPermission(10L,99L,DccFileCategoryPermissionActionEnum.OBSOLETE)).thenReturn(true);
        assertThrows(RuntimeException.class,()->obsoleteService.applyApprovedObsoleteControlledFile(99L,900L,request));
        verify(obsoleteArchiveRequestService,never()).request(any(),any());
        verify(controlledFileMapper,never()).updateById(any(DccControlledFileDO.class));
        verify(obsoleteAuditMapper,never()).insert(any(DccControlledFileObsoleteAuditDO.class));
    }

    @Test
    void exactObsoleteCommandReplayReturnsSavedApplicationBeforeMutableStatusAndPendingChecks() {
        var request=new DccControlledFileObsoleteReqVO();request.setReason("作废原因");request.setIdempotencyKey("obsolete-once");
        var file=DccControlledFileDO.builder().id(900L).masterId(700L).tenantId(1L).categoryId(10L)
                .versionNo("A/1").status("OBSOLETE").build();
        when(controlledFileMapper.selectById(900L)).thenReturn(file);
        org.mockito.Mockito.lenient().when(controlledFileMasterMapper.selectByIdForUpdate(700L))
                .thenReturn(DccControlledFileMasterDO.builder().id(700L).tenantId(1L).build());
        org.mockito.Mockito.lenient().when(permissionSupport.hasCategoryPermission(10L,99L,DccFileCategoryPermissionActionEnum.OBSOLETE)).thenReturn(true);
        var context=new BusinessActionContextReqVO();context.setTenantId(1L);context.setDataDomain("DCC");
        context.setSystemCode("DCC");context.setObjectType("CONTROLLED_FILE");context.setObjectId("900");
        context.setObjectVersion("A/1");context.setActionCode("OBSOLETE");context.setReason("作废原因");
        var saved=new FormInstanceRespVO();saved.setId(31L);saved.setStatus("EFFECTIVE");saved.setContext(context);
        org.mockito.Mockito.lenient().when(formCenterRuntimeService.findBusinessActionByIdempotency(any(),eq("obsolete-once"))).thenReturn(saved);
        var snapshot=new cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceSnapshotRespVO();
        snapshot.setSnapshotVersion(1);
        snapshot.setSnapshotType("SUBMIT");snapshot.setFormData(Map.of("controlledFileId",900L,"reason","作废原因","dccObsoleteSubmitActorId",99L,
                "dccApplicationPayloadHash",cn.hutool.crypto.digest.DigestUtil.sha256Hex(JsonUtils.toJsonString(
                        java.util.Arrays.asList(99L,900L,"A/1","作废原因",null,null)))));
        org.mockito.Mockito.lenient().when(formCenterRuntimeService.getInstanceSnapshots(31L)).thenReturn(List.of(snapshot));
        assertEquals(saved,obsoleteService.obsoleteControlledFile(99L,900L,request));
        verify(pendingActionGuard,never()).assertNoPendingBusinessAction(any());
        verify(formCenterRuntimeService,never()).createInstance(any(),any());
        verify(formCenterRuntimeService,never()).submitInstance(any(),any(),any());
        verify(taskSnapshotMapper,never()).insert(any(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTaskAssigneeSnapshotDO.class));
        request.setProjectAttributes(new cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes(List.of("CE"),null,"Y","N","N",null));
        assertThrows(RuntimeException.class,()->obsoleteService.obsoleteControlledFile(99L,900L,request));
        request.setProjectAttributes(null);request.setSelectedSignoffDepartmentIds(List.of(51L));
        assertThrows(RuntimeException.class,()->obsoleteService.obsoleteControlledFile(99L,900L,request));
        request.setSelectedSignoffDepartmentIds(null);
        request.setReason("不同原因");
        assertThrows(RuntimeException.class,()->obsoleteService.obsoleteControlledFile(99L,900L,request));
        request.setReason("作废原因");
        snapshot.setFormData(Map.of("controlledFileId",900L,"reason","作废原因","dccObsoleteSubmitActorId",98L));
        assertThrows(RuntimeException.class,()->obsoleteService.obsoleteControlledFile(99L,900L,request));
    }

    @Test
    void firstObsoleteSubmissionValidatesTheLockedFileInsteadOfAnEarlierActiveSnapshot() {
        var request=new DccControlledFileObsoleteReqVO();request.setReason("作废");request.setIdempotencyKey("fresh-obsolete");
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder().id(900L).masterId(700L).tenantId(1L)
                .categoryId(10L).versionNo("A/1").status("ACTIVE").build());
        when(controlledFileMapper.selectByIdAndTenantForUpdate(1L,900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L).masterId(700L).tenantId(1L).categoryId(10L).versionNo("A/1").status("OBSOLETE").build());
        when(permissionSupport.hasCategoryPermission(10L,99L,DccFileCategoryPermissionActionEnum.OBSOLETE)).thenReturn(true);
        assertServiceException(()->obsoleteService.obsoleteControlledFile(99L,900L,request),CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED);
        verify(formCenterRuntimeService,never()).createInstance(any(),any());
    }

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Mock
    private DccControlledFileObsoleteAuditMapper obsoleteAuditMapper;
    @Mock
    private DccControlledFileDistributionMapper distributionMapper;
    @Mock
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Mock
    private DccControlledFileTrainingMapper trainingMapper;
    @Mock
    private DccControlledFileTrainingAssignmentMapper trainingAssignmentMapper;
    @Mock
    private DccControlledFileMessageJobMapper messageJobMapper;
    @Mock
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Mock
    private DccObsoleteFileStorageService obsoleteFileStorageService;
    @Mock
    private NotifyMessageSendApi notifyMessageSendApi;
    @Mock
    private DccControlledContentAdapter platformAdapter;
    @Mock
    private FormCenterRuntimeService formCenterRuntimeService;
    @Mock
    private DccControlledFilePendingActionGuard pendingActionGuard;
    @Mock
    private DccControlledFileApprovalRouteAssigneeResolver approvalRouteAssigneeResolver;
    @Mock
    private DccControlledFileNameClaimService nameClaimService;
    @Mock private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTaskAssigneeSnapshotMapper taskSnapshotMapper;
    @Mock private DccWorkflowObsoleteArchiveRequestService obsoleteArchiveRequestService;
    @Mock private cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService obsoleteProcessService;
    @Mock private DccWorkflowObsoleteEvidenceGuard obsoleteEvidenceGuard;
    @Mock private DccObsoleteRetentionService obsoleteRetentionService;
    @Mock private DccApplicationRoundService applicationRoundService;
    @Mock private DccControlledFileRouteReadinessService routeReadinessService;
    @Mock private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributesService projectAttributesService;

    private DccControlledFileMessageDeliveryService messageDeliveryService;
    @InjectMocks
    private DccControlledFileObsoleteServiceImpl obsoleteService;

    @org.junit.jupiter.api.AfterEach
    void clearMessageTenant() { cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear(); }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(applicationRoundService.bind(any(),any(),any(),any())).thenReturn(4);
        org.mockito.Mockito.lenient().when(approvalRouteAssigneeResolver.withSelectedSignoffDepartments(any(),any()))
                .thenAnswer(call->call.getArgument(0));
        org.mockito.Mockito.lenient().when(controlledFileMapper.selectByIdAndTenantForUpdate(any(),any())).thenAnswer(call -> controlledFileMapper.selectById(call.getArgument(1)));
        org.mockito.Mockito.lenient().when(controlledFileMasterMapper.selectByIdForUpdate(any())).thenAnswer(call -> {
            DccControlledFileMasterDO saved=controlledFileMasterMapper.selectById(call.getArgument(0));
            return saved!=null ? saved:DccControlledFileMasterDO.builder().id(call.getArgument(0)).tenantId(1L).build();
        });
        org.mockito.Mockito.lenient().when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);
        org.mockito.Mockito.lenient().when(obsoleteAuditMapper.insert(any(DccControlledFileObsoleteAuditDO.class))).thenReturn(1);
        org.mockito.Mockito.lenient().when(controlledFileMasterMapper.clearCurrentActive(any(),any(),any())).thenReturn(1);
        org.mockito.Mockito.lenient().when(taskSnapshotMapper.insert(any(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTaskAssigneeSnapshotDO.class))).thenReturn(1);
        messageDeliveryService = new DccControlledFileMessageDeliveryService();
        DccMessageDeliveryTestSupport.wire(messageDeliveryService, messageJobMapper);
        ReflectionTestUtils.setField(messageDeliveryService, "messageJobMapper", messageJobMapper);
        ReflectionTestUtils.setField(messageDeliveryService, "notifyMessageSendApi", notifyMessageSendApi);
        ReflectionTestUtils.setField(messageDeliveryService, "controlledFileMapper", controlledFileMapper);
        ReflectionTestUtils.setField(messageDeliveryService, "distributionMapper", distributionMapper);
        ReflectionTestUtils.setField(messageDeliveryService, "trainingMapper", trainingMapper);
        ReflectionTestUtils.setField(obsoleteService, "messageDeliveryService", messageDeliveryService);
    }

    @Test
    void obsoleteRequestJsonRejectsNeedTrainingEvenWhenGlobalMapperAllowsUnknownFields() {
        assertThrows(Exception.class, () -> JsonUtils.getObjectMapper().readValue("""
                {
                  "reason": "No longer effective",
                  "idempotencyKey": "DCC-OBSOLETE-900-V1",
                  "needTraining": true
                }
                """, DccControlledFileObsoleteReqVO.class));
    }

    @Test
    void obsoleteControlledFile_usesObsoleteRouteAssigneesEvenWhenRequestCarriesManualAssignees() {
        DccControlledFileObsoleteReqVO reqVO = new DccControlledFileObsoleteReqVO();
        reqVO.setReason("Superseded by FI-001 V2.0");
        reqVO.setIdempotencyKey("DCC-OBSOLETE-900-V1");
        var actual=new cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes(List.of("CE"),null,"Y","N","N",null);
        reqVO.setProjectAttributes(actual);
        reqVO.setStartUserSelectAssignees(Map.of("MATRIX_APPROVAL", List.of(914518L)));
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(900L)
                .masterId(700L)
                .dccProjectCodeId(3000L)
                .tenantId(1L)
                .categoryId(10L)
                .productCode("PRD-001")
                .versionNo("V1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
        when(controlledFileMapper.selectById(900L)).thenReturn(file);
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.OBSOLETE))
                .thenReturn(true);
        DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute = resolvedRoute();
        when(approvalRouteAssigneeResolver.resolveRoute(10L, 99L,
                DccControlledFileProcessDefinitionKeys.toActionType(DccControlledFileProcessDefinitionKeys.OBSOLETE)))
                .thenReturn(resolvedRoute);
        when(approvalRouteAssigneeResolver.buildStartUserSelectAssigneeMap(resolvedRoute.nodes()))
                .thenReturn(Map.of("MATRIX_REVIEW", List.of(7201L, 7202L)));
        when(approvalRouteAssigneeResolver.buildApproveUserSelectAssigneeMap(resolvedRoute.nodes()))
                .thenReturn(Map.of("MATRIX_APPROVAL", List.of(914518L)));
        FormInstanceRespVO draft = new FormInstanceRespVO();
        draft.setId(37L);
        draft.setStatus("DRAFT");
        FormInstanceRespVO submitted = new FormInstanceRespVO();
        submitted.setId(37L);
        submitted.setStatus("IN_APPROVAL");
        submitted.setBpmProcessInstanceId("form-process");
        submitted.setBpmProcessInstanceId("process-37");
        when(formCenterRuntimeService.createInstance(any(FormInstanceCreateReqVO.class), eq(99L))).thenReturn(draft);
        when(formCenterRuntimeService.submitInstance(eq(37L), any(FormInstanceSubmitReqVO.class), eq(99L)))
                .thenReturn(submitted);

        FormInstanceRespVO result = obsoleteService.obsoleteControlledFile(99L, 900L, reqVO);

        assertEquals("IN_APPROVAL", result.getStatus());
        assertEquals("process-37", result.getBpmProcessInstanceId());
        verify(applicationRoundService).bind(3000L,"OBSOLETE",900L,"process-37");
        verify(projectAttributesService).beginDraft(99L,3000L,"OBSOLETE",900L,4);
        verify(projectAttributesService).saveDraft(99L,3000L,"OBSOLETE",900L,4,actual);
        verify(projectAttributesService).freeze(99L,3000L,"OBSOLETE",900L,4);
        ArgumentCaptor<FormInstanceCreateReqVO> createCaptor = ArgumentCaptor.forClass(FormInstanceCreateReqVO.class);
        verify(formCenterRuntimeService).createInstance(createCaptor.capture(), eq(99L));
        BusinessActionContextReqVO context = createCaptor.getValue().getContext();
        assertEquals("DCC", context.getDataDomain());
        assertEquals("DCC", context.getSystemCode());
        assertEquals("CONTROLLED_FILE", context.getObjectType());
        assertEquals("900", context.getObjectId());
        assertEquals("V1.0", context.getObjectVersion());
        assertEquals("OBSOLETE", context.getActionCode());
        assertEquals(DccControlledFileStatusEnum.ACTIVE.getStatus(), context.getObjectState());
        assertEquals("PRD-001", context.getProductCode());
        assertEquals("10", context.getCategoryCode());
        assertEquals("Superseded by FI-001 V2.0", context.getReason());
        assertEquals("DCC-OBSOLETE-900-V1", createCaptor.getValue().getIdempotencyKey());
        assertEquals(900L, createCaptor.getValue().getFormData().get("controlledFileId"));
        assertEquals("Superseded by FI-001 V2.0", createCaptor.getValue().getFormData().get("reason"));

        ArgumentCaptor<FormInstanceSubmitReqVO> submitCaptor = ArgumentCaptor.forClass(FormInstanceSubmitReqVO.class);
        verify(formCenterRuntimeService).submitInstance(eq(37L), submitCaptor.capture(), eq(99L));
        assertEquals(createCaptor.getValue().getFormData(), submitCaptor.getValue().getFormData());
        assertEquals(Map.of("MATRIX_REVIEW", List.of(7201L, 7202L)),
                submitCaptor.getValue().getStartUserSelectAssignees());
        verify(approvalRouteAssigneeResolver, times(1)).resolveRoute(10L, 99L,
                DccControlledFileProcessDefinitionKeys.toActionType(DccControlledFileProcessDefinitionKeys.OBSOLETE));
        verify(approvalRouteAssigneeResolver).buildStartUserSelectAssigneeMap(resolvedRoute.nodes());
        verify(approvalRouteAssigneeResolver).buildApproveUserSelectAssigneeMap(resolvedRoute.nodes());
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(obsoleteAuditMapper, never()).insert(any(DccControlledFileObsoleteAuditDO.class));
        verify(obsoleteFileStorageService, never()).moveControlledFileArtifactsToObsoleteFolder(any());
        verify(platformAdapter, never()).recordObsoleted(any(), any(), any(), any());
    }

    @Test
    void obsoleteControlledFile_derivesStartUserSelectAssigneesFromDccRouteWhenRequestOmitted() {
        DccControlledFileObsoleteReqVO reqVO = new DccControlledFileObsoleteReqVO();
        reqVO.setReason("Superseded by FI-001 V2.0");
        reqVO.setIdempotencyKey("DCC-OBSOLETE-900-V1-AUTO");
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(900L)
                .masterId(700L)
                .tenantId(1L)
                .categoryId(10L)
                .productCode("PRD-001")
                .versionNo("V1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
        when(controlledFileMapper.selectById(900L)).thenReturn(file);
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.OBSOLETE))
                .thenReturn(true);
        DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute = resolvedRoute();
        when(approvalRouteAssigneeResolver.resolveRoute(10L, 99L,
                DccControlledFileProcessDefinitionKeys.toActionType(DccControlledFileProcessDefinitionKeys.OBSOLETE)))
                .thenReturn(resolvedRoute);
        when(approvalRouteAssigneeResolver.buildStartUserSelectAssigneeMap(resolvedRoute.nodes()))
                .thenReturn(Map.of("MATRIX_APPROVAL", List.of(914518L)));
        when(approvalRouteAssigneeResolver.buildApproveUserSelectAssigneeMap(resolvedRoute.nodes()))
                .thenReturn(Map.of());
        FormInstanceRespVO draft = new FormInstanceRespVO();
        draft.setId(38L);
        draft.setStatus("DRAFT");
        FormInstanceRespVO submitted = new FormInstanceRespVO();
        submitted.setId(38L);
        submitted.setStatus("IN_APPROVAL");
        submitted.setBpmProcessInstanceId("form-process");
        submitted.setBpmProcessInstanceId("process-38");
        when(formCenterRuntimeService.createInstance(any(FormInstanceCreateReqVO.class), eq(99L))).thenReturn(draft);
        when(formCenterRuntimeService.submitInstance(eq(38L), any(FormInstanceSubmitReqVO.class), eq(99L)))
                .thenReturn(submitted);

        FormInstanceRespVO result = obsoleteService.obsoleteControlledFile(99L, 900L, reqVO);

        assertEquals("IN_APPROVAL", result.getStatus());
        verify(approvalRouteAssigneeResolver, times(1)).resolveRoute(10L, 99L,
                DccControlledFileProcessDefinitionKeys.toActionType(DccControlledFileProcessDefinitionKeys.OBSOLETE));
        verify(approvalRouteAssigneeResolver).buildStartUserSelectAssigneeMap(resolvedRoute.nodes());
        verify(approvalRouteAssigneeResolver).buildApproveUserSelectAssigneeMap(resolvedRoute.nodes());
        ArgumentCaptor<FormInstanceSubmitReqVO> submitCaptor = ArgumentCaptor.forClass(FormInstanceSubmitReqVO.class);
        verify(formCenterRuntimeService).submitInstance(eq(38L), submitCaptor.capture(), eq(99L));
        assertEquals(Map.of("MATRIX_APPROVAL", List.of(914518L)),
                submitCaptor.getValue().getStartUserSelectAssignees());
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(obsoleteAuditMapper, never()).insert(any(DccControlledFileObsoleteAuditDO.class));
    }

    @Test
    void obsoleteControlledFile_derivesAssigneesFromObsoleteActionRouteRegardlessOriginalProcessKey() {
        DccControlledFileObsoleteReqVO reqVO = new DccControlledFileObsoleteReqVO();
        reqVO.setReason("Controlled file no longer applies");
        reqVO.setIdempotencyKey("DCC-OBSOLETE-904-V1-AUTO");
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(904L)
                .masterId(704L)
                .tenantId(1L)
                .categoryId(10L)
                .productCode("PRD-004")
                .versionNo("V1.0")
                .processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
        when(controlledFileMapper.selectById(904L)).thenReturn(file);
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.OBSOLETE))
                .thenReturn(true);
        DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute = resolvedRoute();
        when(approvalRouteAssigneeResolver.resolveRoute(10L, 99L,
                DccControlledFileProcessDefinitionKeys.toActionType(DccControlledFileProcessDefinitionKeys.OBSOLETE)))
                .thenReturn(resolvedRoute);
        when(approvalRouteAssigneeResolver.buildStartUserSelectAssigneeMap(resolvedRoute.nodes()))
                .thenReturn(Map.of("MATRIX_REVIEW", List.of(7201L)));
        when(approvalRouteAssigneeResolver.buildApproveUserSelectAssigneeMap(resolvedRoute.nodes()))
                .thenReturn(Map.of());
        FormInstanceRespVO draft = new FormInstanceRespVO();
        draft.setId(39L);
        draft.setStatus("DRAFT");
        FormInstanceRespVO submitted = new FormInstanceRespVO();
        submitted.setId(39L);
        submitted.setStatus("IN_APPROVAL");
        submitted.setBpmProcessInstanceId("form-process");
        submitted.setBpmProcessInstanceId("process-39");
        when(formCenterRuntimeService.createInstance(any(FormInstanceCreateReqVO.class), eq(99L))).thenReturn(draft);
        when(formCenterRuntimeService.submitInstance(eq(39L), any(FormInstanceSubmitReqVO.class), eq(99L)))
                .thenReturn(submitted);

        FormInstanceRespVO result = obsoleteService.obsoleteControlledFile(99L, 904L, reqVO);

        assertEquals("IN_APPROVAL", result.getStatus());
        verify(approvalRouteAssigneeResolver, times(1)).resolveRoute(10L, 99L,
                DccControlledFileProcessDefinitionKeys.toActionType(DccControlledFileProcessDefinitionKeys.OBSOLETE));
        verify(approvalRouteAssigneeResolver).buildStartUserSelectAssigneeMap(resolvedRoute.nodes());
        verify(approvalRouteAssigneeResolver).buildApproveUserSelectAssigneeMap(resolvedRoute.nodes());
        ArgumentCaptor<FormInstanceSubmitReqVO> submitCaptor = ArgumentCaptor.forClass(FormInstanceSubmitReqVO.class);
        verify(formCenterRuntimeService).submitInstance(eq(39L), submitCaptor.capture(), eq(99L));
        assertEquals(Map.of("MATRIX_REVIEW", List.of(7201L)),
                submitCaptor.getValue().getStartUserSelectAssignees());
    }

    private DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute() {
        return new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(null,List.of(
            new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(1,"MATRIX_REVIEW","会签",1,
                "DEPT",51L,List.of(51L,52L),"ALL",100,true,List.of(7201L,7202L))));
    }

    @Test
    void obsoleteControlledFile_nonActiveDeniedBeforeFormCenterSubmission() {
        DccControlledFileObsoleteReqVO reqVO = new DccControlledFileObsoleteReqVO();
        reqVO.setReason("No longer effective");
        reqVO.setIdempotencyKey("DCC-OBSOLETE-901-V1");
        when(controlledFileMapper.selectById(901L)).thenReturn(DccControlledFileDO.builder()
                .id(901L)
                .categoryId(11L)
                .versionNo("V1.0")
                .status(DccControlledFileStatusEnum.OBSOLETE.getStatus())
                .build());

        assertServiceException(() -> obsoleteService.obsoleteControlledFile(99L, 901L, reqVO),
                CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED);

        verify(formCenterRuntimeService, never()).createInstance(any(FormInstanceCreateReqVO.class), any());
        verify(formCenterRuntimeService, never()).submitInstance(any(), any(), any());
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void obsoleteControlledFile_activePendingActionFailsBeforeCreatingAnotherFormInstance() {
        DccControlledFileObsoleteReqVO reqVO = new DccControlledFileObsoleteReqVO();
        reqVO.setReason("No longer effective");
        reqVO.setIdempotencyKey("DCC-OBSOLETE-900-LOCKED");
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(900L)
                .masterId(700L).tenantId(1L)
                .categoryId(10L)
                .versionNo("V1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
        when(controlledFileMapper.selectById(900L)).thenReturn(file);
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.OBSOLETE))
                .thenReturn(true);
        doThrow(new IllegalStateException("controlled file is locked by active form action"))
                .when(pendingActionGuard).assertNoPendingBusinessAction(file);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> obsoleteService.obsoleteControlledFile(99L, 900L, reqVO));

        verify(pendingActionGuard).assertNoPendingBusinessAction(file);
        verify(formCenterRuntimeService, never()).createInstance(any(FormInstanceCreateReqVO.class), any());
        verify(formCenterRuntimeService, never()).submitInstance(any(), any(), any());
    }

    @Test
    void applyApprovedObsoleteControlledFile_successUpdatesLifecycleAuditAndMessages() {
        DccControlledFileObsoleteReqVO reqVO = new DccControlledFileObsoleteReqVO();
        reqVO.setReason("Superseded by FI-001 V2.0");
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L).versionNo("A/1")
                .masterId(700L)
                .tenantId(1L)
                .categoryId(10L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(controlledFileMasterMapper.selectById(700L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(700L)
                .tenantId(1L)
                .currentActiveControlledFileId(900L)
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.OBSOLETE))
                .thenReturn(true);
        when(distributionMapper.selectListByControlledFileId(900L)).thenReturn(List.of(
                DccControlledFileDistributionDO.builder().id(301L).controlledFileId(900L).build()));
        when(distributionRecipientMapper.selectListByDistributionId(301L)).thenReturn(List.of(
                DccControlledFileDistributionRecipientDO.builder().id(401L).distributionId(301L).userId(501L).build()));
        when(trainingMapper.selectListByControlledFileId(900L)).thenReturn(List.of(
                DccControlledFileTrainingDO.builder().id(302L).controlledFileId(900L).build()));
        when(trainingAssignmentMapper.selectListByTrainingId(302L)).thenReturn(List.of(
                DccControlledFileTrainingAssignmentDO.builder().id(402L).trainingId(302L).userId(601L).build()));
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any(NotifySendSingleToUserIdempotentReqDTO.class)))
                .thenReturn(9001L, 9002L);

        reqVO.setApprovalProcessInstanceId("approved-obsolete-ROUND");
        reqVO.setApprovedVersionNo("A/1");
        var process=org.mockito.Mockito.mock(org.flowable.engine.history.HistoricProcessInstance.class);
        when(process.getId()).thenReturn("approved-obsolete-ROUND");
        when(process.getTenantId()).thenReturn("1");
        when(process.getStartUserId()).thenReturn("99");
        when(process.getProcessDefinitionKey()).thenReturn(DccControlledFileProcessDefinitionKeys.OBSOLETE);
        when(process.getProcessVariables()).thenReturn(Map.of("PROCESS_STATUS",2,"systemCode","DCC","objectType","CONTROLLED_FILE",
                "actionCode","OBSOLETE","objectId","900","objectVersion","A/1","PROCESS_LAST_APPROVER_USER_ID",100L));
        when(obsoleteProcessService.getHistoricProcessInstance("approved-obsolete-ROUND")).thenReturn(process);
        when(obsoleteEvidenceGuard.require(any(),eq("approved-obsolete-ROUND"),any())).thenReturn(100L);
        obsoleteService.applyApprovedObsoleteControlledFile(99L, 900L, reqVO);

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.OBSOLETE.getStatus(), fileCaptor.getValue().getStatus());
        assertEquals("Superseded by FI-001 V2.0", fileCaptor.getValue().getObsoleteReason());
        assertEquals(100L, fileCaptor.getValue().getObsoletedBy());

        verify(controlledFileMasterMapper).clearCurrentActive(1L,700L,900L);
        verify(nameClaimService,never()).release(any(),any());

        verify(obsoleteAuditMapper).insert(org.mockito.ArgumentMatchers.argThat((DccControlledFileObsoleteAuditDO audit) -> Long.valueOf(100L).equals(audit.getOperatorId())));
        verify(obsoleteFileStorageService,never()).moveControlledFileArtifactsToObsoleteFolder(
                org.mockito.ArgumentMatchers.argThat(file -> Long.valueOf(900L).equals(file.getId())));
        verify(messageJobMapper, times(2)).insert(org.mockito.ArgumentMatchers.any(DccControlledFileMessageJobDO.class));
        verify(messageJobMapper, times(2)).updateById(org.mockito.ArgumentMatchers.any(DccControlledFileMessageJobDO.class));
        ArgumentCaptor<NotifySendSingleToUserIdempotentReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserIdempotentReqDTO.class);
        verify(notifyMessageSendApi, times(2)).sendSingleMessageIdempotentlyToAdmin(notifyCaptor.capture());
        assertEquals(List.of(501L, 601L),
                notifyCaptor.getAllValues().stream().map(NotifySendSingleToUserIdempotentReqDTO::getUserId).toList());
        assertTrue(notifyCaptor.getAllValues().stream()
                .allMatch(req -> "dcc_obsolete".equals(req.getTemplateCode())));
        verify(platformAdapter).recordObsoleted(
                org.mockito.ArgumentMatchers.argThat(file -> Long.valueOf(900L).equals(file.getId())),
                eq(100L), eq("Superseded by FI-001 V2.0"), eq("dcc-obsolete:900"));
    }

    @Test
    void applyApprovedObsoleteControlledFile_withoutAffectedRecipientsNotifiesRequester() {
        DccControlledFileObsoleteReqVO reqVO = new DccControlledFileObsoleteReqVO();
        reqVO.setReason("No longer used");
        when(controlledFileMapper.selectById(902L)).thenReturn(DccControlledFileDO.builder()
                .id(902L).versionNo("A/1")
                .masterId(702L)
                .tenantId(1L)
                .categoryId(10L)
                .requesterId(701L)
                .submitterId(702L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(controlledFileMasterMapper.selectById(702L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(702L)
                .tenantId(1L)
                .currentActiveControlledFileId(902L)
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.OBSOLETE))
                .thenReturn(true);
        when(distributionMapper.selectListByControlledFileId(902L)).thenReturn(List.of());
        when(trainingMapper.selectListByControlledFileId(902L)).thenReturn(List.of());
        when(notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(any(NotifySendSingleToUserIdempotentReqDTO.class)))
                .thenReturn(9003L);

        reqVO.setApprovalProcessInstanceId("approved-obsolete-ROUND");
        reqVO.setApprovedVersionNo("A/1");
        var process=org.mockito.Mockito.mock(org.flowable.engine.history.HistoricProcessInstance.class);
        when(process.getId()).thenReturn("approved-obsolete-ROUND");
        when(process.getTenantId()).thenReturn("1");
        when(process.getStartUserId()).thenReturn("99");
        when(process.getProcessDefinitionKey()).thenReturn(DccControlledFileProcessDefinitionKeys.OBSOLETE);
        when(process.getProcessVariables()).thenReturn(Map.of("PROCESS_STATUS",2,"systemCode","DCC","objectType","CONTROLLED_FILE",
                "actionCode","OBSOLETE","objectId","902","objectVersion","A/1"));
        when(obsoleteProcessService.getHistoricProcessInstance("approved-obsolete-ROUND")).thenReturn(process);
        when(obsoleteEvidenceGuard.require(any(),eq("approved-obsolete-ROUND"),any())).thenReturn(99L);
        obsoleteService.applyApprovedObsoleteControlledFile(99L, 902L, reqVO);

        ArgumentCaptor<NotifySendSingleToUserIdempotentReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserIdempotentReqDTO.class);
        verify(messageJobMapper).insert(org.mockito.ArgumentMatchers.any(DccControlledFileMessageJobDO.class));
        verify(notifyMessageSendApi).sendSingleMessageIdempotentlyToAdmin(notifyCaptor.capture());
        assertEquals(701L, notifyCaptor.getValue().getUserId());
        assertEquals("dcc_obsolete", notifyCaptor.getValue().getTemplateCode());
    }

    @Test
    void applyApprovedObsoleteControlledFile_withoutCategoryPermission_throws() {
        DccControlledFileObsoleteReqVO reqVO = new DccControlledFileObsoleteReqVO();
        reqVO.setReason("No longer effective");
        when(controlledFileMapper.selectById(901L)).thenReturn(DccControlledFileDO.builder()
                .id(901L)
                .categoryId(11L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        org.mockito.Mockito.lenient().when(permissionSupport.hasCategoryPermission(11L, 99L, DccFileCategoryPermissionActionEnum.OBSOLETE))
                .thenReturn(false);

        assertServiceException(() -> obsoleteService.applyApprovedObsoleteControlledFile(99L, 901L, reqVO),
                CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED);
    }
}
