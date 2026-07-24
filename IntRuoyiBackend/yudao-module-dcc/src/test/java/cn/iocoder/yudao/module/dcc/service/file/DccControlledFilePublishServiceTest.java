package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.BusinessActionContextReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceCreateReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceSubmitReqVO;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePublishReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PUBLISH_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFilePublishServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileFinalizationService finalizationService;
    @Mock
    private FormCenterRuntimeService formCenterRuntimeService;
    @Mock
    private DccControlledFileApprovalRouteAssigneeResolver approvalRouteAssigneeResolver;

    @InjectMocks
    private DccControlledFilePublishServiceImpl publishService;

    @Test
    void publishControlledFile_submitsFormCenterActionWithoutApplyingDomainEffect() {
        DccControlledFilePublishReqVO reqVO = new DccControlledFilePublishReqVO();
        reqVO.setReason("Release V2.0 after revision approval");
        reqVO.setIdempotencyKey("DCC-PUBLISH-920-V2");
        reqVO.setStartUserSelectAssignees(Map.of("DOC_CONTROL_REVIEW", List.of(914518L)));
        when(controlledFileMapper.selectById(920L)).thenReturn(DccControlledFileDO.builder()
                .id(920L)
                .categoryId(18L)
                .productCode("PRD-002")
                .versionNo("V2.0")
                .status(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus())
                .build());
        FormInstanceRespVO draft = new FormInstanceRespVO();
        draft.setId(57L);
        draft.setStatus("DRAFT");
        FormInstanceRespVO submitted = new FormInstanceRespVO();
        submitted.setId(57L);
        submitted.setStatus("IN_APPROVAL");
        submitted.setBpmProcessInstanceId("process-57");
        when(formCenterRuntimeService.createInstance(any(FormInstanceCreateReqVO.class), eq(99L))).thenReturn(draft);
        when(formCenterRuntimeService.submitInstance(eq(57L), any(FormInstanceSubmitReqVO.class), eq(99L)))
                .thenReturn(submitted);

        FormInstanceRespVO result = publishService.publishControlledFile(99L, 920L, reqVO);

        assertEquals("IN_APPROVAL", result.getStatus());
        assertEquals("process-57", result.getBpmProcessInstanceId());
        verify(finalizationService).precheckPublishControlledFile(99L, 920L);
        ArgumentCaptor<FormInstanceCreateReqVO> createCaptor = ArgumentCaptor.forClass(FormInstanceCreateReqVO.class);
        verify(formCenterRuntimeService).createInstance(createCaptor.capture(), eq(99L));
        BusinessActionContextReqVO context = createCaptor.getValue().getContext();
        assertEquals("DCC", context.getDataDomain());
        assertEquals("DCC", context.getSystemCode());
        assertEquals("CONTROLLED_FILE", context.getObjectType());
        assertEquals("920", context.getObjectId());
        assertEquals("V2.0", context.getObjectVersion());
        assertEquals("PUBLISH", context.getActionCode());
        assertEquals(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus(), context.getObjectState());
        assertEquals("PRD-002", context.getProductCode());
        assertEquals("18", context.getCategoryCode());
        assertEquals("Release V2.0 after revision approval", context.getReason());
        assertEquals("DCC-PUBLISH-920-V2", createCaptor.getValue().getIdempotencyKey());
        assertEquals(920L, createCaptor.getValue().getFormData().get("controlledFileId"));
        assertEquals("Release V2.0 after revision approval", createCaptor.getValue().getFormData().get("reason"));

        ArgumentCaptor<FormInstanceSubmitReqVO> submitCaptor = ArgumentCaptor.forClass(FormInstanceSubmitReqVO.class);
        verify(formCenterRuntimeService).submitInstance(eq(57L), submitCaptor.capture(), eq(99L));
        assertEquals(createCaptor.getValue().getFormData(), submitCaptor.getValue().getFormData());
        assertEquals(Map.of("DOC_CONTROL_REVIEW", List.of(914518L)),
                submitCaptor.getValue().getStartUserSelectAssignees());
        verify(finalizationService, never()).applyApprovedPublishControlledFile(any(), any(), any());
    }

    @Test
    void publishControlledFile_derivesStartUserSelectAssigneesFromDccRouteWhenRequestOmitted() {
        DccControlledFilePublishReqVO reqVO = new DccControlledFilePublishReqVO();
        reqVO.setReason("Release V2.0 after revision approval");
        reqVO.setIdempotencyKey("DCC-PUBLISH-920-V2-AUTO");
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(920L)
                .categoryId(18L)
                .versionNo("V2.0")
                .status(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus())
                .build();
        when(controlledFileMapper.selectById(920L)).thenReturn(file);
        when(approvalRouteAssigneeResolver.resolveStartUserSelectAssignees(file, 99L))
                .thenReturn(Map.of("DOC_CONTROL_REVIEW", List.of(914518L)));
        FormInstanceRespVO draft = new FormInstanceRespVO();
        draft.setId(58L);
        draft.setStatus("DRAFT");
        FormInstanceRespVO submitted = new FormInstanceRespVO();
        submitted.setId(58L);
        submitted.setStatus("IN_APPROVAL");
        when(formCenterRuntimeService.createInstance(any(FormInstanceCreateReqVO.class), eq(99L))).thenReturn(draft);
        when(formCenterRuntimeService.submitInstance(eq(58L), any(FormInstanceSubmitReqVO.class), eq(99L)))
                .thenReturn(submitted);

        publishService.publishControlledFile(99L, 920L, reqVO);

        verify(approvalRouteAssigneeResolver).resolveStartUserSelectAssignees(file, 99L);
        ArgumentCaptor<FormInstanceSubmitReqVO> submitCaptor = ArgumentCaptor.forClass(FormInstanceSubmitReqVO.class);
        verify(formCenterRuntimeService).submitInstance(eq(58L), submitCaptor.capture(), eq(99L));
        assertEquals(Map.of("DOC_CONTROL_REVIEW", List.of(914518L)),
                submitCaptor.getValue().getStartUserSelectAssignees());
    }

    @Test
    void publishControlledFile_nonReadyDeniedBeforeFormCenterSubmission() {
        DccControlledFilePublishReqVO reqVO = new DccControlledFilePublishReqVO();
        reqVO.setReason("Release");
        reqVO.setIdempotencyKey("DCC-PUBLISH-921-V1");
        when(controlledFileMapper.selectById(921L)).thenReturn(DccControlledFileDO.builder()
                .id(921L)
                .categoryId(18L)
                .versionNo("V1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        doThrow(exception(CONTROLLED_FILE_PUBLISH_NOT_ALLOWED))
                .when(finalizationService).precheckPublishControlledFile(99L, 921L);

        assertServiceException(() -> publishService.publishControlledFile(99L, 921L, reqVO),
                CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);

        verify(formCenterRuntimeService, never()).createInstance(any(FormInstanceCreateReqVO.class), any());
        verify(formCenterRuntimeService, never()).submitInstance(any(), any(), any());
    }
}
