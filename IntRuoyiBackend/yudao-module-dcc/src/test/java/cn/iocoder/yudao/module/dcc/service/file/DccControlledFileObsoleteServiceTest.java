package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
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
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileObsoleteServiceTest extends BaseMockitoUnitTest {

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

    private DccControlledFileMessageDeliveryService messageDeliveryService;
    @InjectMocks
    private DccControlledFileObsoleteServiceImpl obsoleteService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        messageDeliveryService = new DccControlledFileMessageDeliveryService();
        ReflectionTestUtils.setField(messageDeliveryService, "messageJobMapper", messageJobMapper);
        ReflectionTestUtils.setField(messageDeliveryService, "notifyMessageSendApi", notifyMessageSendApi);
        ReflectionTestUtils.setField(messageDeliveryService, "controlledFileMapper", controlledFileMapper);
        ReflectionTestUtils.setField(messageDeliveryService, "distributionMapper", distributionMapper);
        ReflectionTestUtils.setField(messageDeliveryService, "trainingMapper", trainingMapper);
        ReflectionTestUtils.setField(obsoleteService, "messageDeliveryService", messageDeliveryService);
    }

    @Test
    void obsoleteControlledFile_submitsFormCenterActionWithoutApplyingDomainEffect() {
        DccControlledFileObsoleteReqVO reqVO = new DccControlledFileObsoleteReqVO();
        reqVO.setReason("Superseded by FI-001 V2.0");
        reqVO.setIdempotencyKey("DCC-OBSOLETE-900-V1");
        reqVO.setStartUserSelectAssignees(Map.of("DOC_CONTROL_REVIEW", List.of(914518L)));
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .masterId(700L)
                .categoryId(10L)
                .productCode("PRD-001")
                .versionNo("V1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.OBSOLETE))
                .thenReturn(true);
        FormInstanceRespVO draft = new FormInstanceRespVO();
        draft.setId(37L);
        draft.setStatus("DRAFT");
        FormInstanceRespVO submitted = new FormInstanceRespVO();
        submitted.setId(37L);
        submitted.setStatus("IN_APPROVAL");
        submitted.setBpmProcessInstanceId("process-37");
        when(formCenterRuntimeService.createInstance(any(FormInstanceCreateReqVO.class), eq(99L))).thenReturn(draft);
        when(formCenterRuntimeService.submitInstance(eq(37L), any(FormInstanceSubmitReqVO.class), eq(99L)))
                .thenReturn(submitted);

        FormInstanceRespVO result = obsoleteService.obsoleteControlledFile(99L, 900L, reqVO);

        assertEquals("IN_APPROVAL", result.getStatus());
        assertEquals("process-37", result.getBpmProcessInstanceId());
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
        assertEquals(Map.of("DOC_CONTROL_REVIEW", List.of(914518L)),
                submitCaptor.getValue().getStartUserSelectAssignees());
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
                .categoryId(10L)
                .productCode("PRD-001")
                .versionNo("V1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
        when(controlledFileMapper.selectById(900L)).thenReturn(file);
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.OBSOLETE))
                .thenReturn(true);
        when(approvalRouteAssigneeResolver.resolveStartUserSelectAssignees(file, 99L))
                .thenReturn(Map.of("DOC_CONTROL_REVIEW", List.of(914518L)));
        FormInstanceRespVO draft = new FormInstanceRespVO();
        draft.setId(38L);
        draft.setStatus("DRAFT");
        FormInstanceRespVO submitted = new FormInstanceRespVO();
        submitted.setId(38L);
        submitted.setStatus("IN_APPROVAL");
        submitted.setBpmProcessInstanceId("process-38");
        when(formCenterRuntimeService.createInstance(any(FormInstanceCreateReqVO.class), eq(99L))).thenReturn(draft);
        when(formCenterRuntimeService.submitInstance(eq(38L), any(FormInstanceSubmitReqVO.class), eq(99L)))
                .thenReturn(submitted);

        FormInstanceRespVO result = obsoleteService.obsoleteControlledFile(99L, 900L, reqVO);

        assertEquals("IN_APPROVAL", result.getStatus());
        verify(approvalRouteAssigneeResolver).resolveStartUserSelectAssignees(file, 99L);
        ArgumentCaptor<FormInstanceSubmitReqVO> submitCaptor = ArgumentCaptor.forClass(FormInstanceSubmitReqVO.class);
        verify(formCenterRuntimeService).submitInstance(eq(38L), submitCaptor.capture(), eq(99L));
        assertEquals(Map.of("DOC_CONTROL_REVIEW", List.of(914518L)),
                submitCaptor.getValue().getStartUserSelectAssignees());
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(obsoleteAuditMapper, never()).insert(any(DccControlledFileObsoleteAuditDO.class));
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
                .id(900L)
                .masterId(700L)
                .categoryId(10L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(controlledFileMasterMapper.selectById(700L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(700L)
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
        when(messageJobMapper.insert(org.mockito.ArgumentMatchers.any(DccControlledFileMessageJobDO.class))).thenReturn(1);
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class)))
                .thenReturn(9001L, 9002L);

        obsoleteService.applyApprovedObsoleteControlledFile(99L, 900L, reqVO);

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.OBSOLETE.getStatus(), fileCaptor.getValue().getStatus());
        assertEquals("Superseded by FI-001 V2.0", fileCaptor.getValue().getObsoleteReason());
        assertEquals(99L, fileCaptor.getValue().getObsoletedBy());

        ArgumentCaptor<DccControlledFileMasterDO> masterCaptor = ArgumentCaptor.forClass(DccControlledFileMasterDO.class);
        verify(controlledFileMasterMapper).updateById(masterCaptor.capture());
        assertEquals(null, masterCaptor.getValue().getCurrentActiveControlledFileId());
        assertEquals(DccControlledFileMasterStatusEnum.OBSOLETE_CHAIN.getCode(), masterCaptor.getValue().getStatus());

        verify(obsoleteAuditMapper).insert(org.mockito.ArgumentMatchers.any(DccControlledFileObsoleteAuditDO.class));
        verify(obsoleteFileStorageService).moveControlledFileArtifactsToObsoleteFolder(
                org.mockito.ArgumentMatchers.argThat(file -> Long.valueOf(900L).equals(file.getId())));
        verify(messageJobMapper, times(2)).insert(org.mockito.ArgumentMatchers.any(DccControlledFileMessageJobDO.class));
        verify(messageJobMapper, times(2)).updateById(org.mockito.ArgumentMatchers.any(DccControlledFileMessageJobDO.class));
        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi, times(2)).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(List.of(501L, 601L),
                notifyCaptor.getAllValues().stream().map(NotifySendSingleToUserReqDTO::getUserId).toList());
        assertTrue(notifyCaptor.getAllValues().stream()
                .allMatch(req -> "dcc_obsolete".equals(req.getTemplateCode())));
        verify(platformAdapter).recordObsoleted(
                org.mockito.ArgumentMatchers.argThat(file -> Long.valueOf(900L).equals(file.getId())),
                eq(99L), eq("Superseded by FI-001 V2.0"), eq("dcc-obsolete:900"));
    }

    @Test
    void applyApprovedObsoleteControlledFile_withoutAffectedRecipientsNotifiesRequester() {
        DccControlledFileObsoleteReqVO reqVO = new DccControlledFileObsoleteReqVO();
        reqVO.setReason("No longer used");
        when(controlledFileMapper.selectById(902L)).thenReturn(DccControlledFileDO.builder()
                .id(902L)
                .masterId(702L)
                .categoryId(10L)
                .requesterId(701L)
                .submitterId(702L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(controlledFileMasterMapper.selectById(702L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(702L)
                .currentActiveControlledFileId(902L)
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.OBSOLETE))
                .thenReturn(true);
        when(distributionMapper.selectListByControlledFileId(902L)).thenReturn(List.of());
        when(trainingMapper.selectListByControlledFileId(902L)).thenReturn(List.of());
        when(messageJobMapper.insert(org.mockito.ArgumentMatchers.any(DccControlledFileMessageJobDO.class))).thenReturn(1);
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class)))
                .thenReturn(9003L);

        obsoleteService.applyApprovedObsoleteControlledFile(99L, 902L, reqVO);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(messageJobMapper).insert(org.mockito.ArgumentMatchers.any(DccControlledFileMessageJobDO.class));
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(notifyCaptor.capture());
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
        when(permissionSupport.hasCategoryPermission(11L, 99L, DccFileCategoryPermissionActionEnum.OBSOLETE))
                .thenReturn(false);

        assertServiceException(() -> obsoleteService.applyApprovedObsoleteControlledFile(99L, 901L, reqVO),
                CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED);
    }
}
