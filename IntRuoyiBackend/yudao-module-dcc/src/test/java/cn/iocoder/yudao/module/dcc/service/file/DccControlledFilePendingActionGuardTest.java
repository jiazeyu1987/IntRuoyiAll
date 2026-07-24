package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.BusinessActionContextReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACTION_LOCKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFilePendingActionGuardTest extends BaseMockitoUnitTest {

    @Mock
    private FormCenterRuntimeService formCenterRuntimeService;

    @InjectMocks
    private DccControlledFilePendingActionGuard guard;

    @Test
    void assertNoPendingBusinessAction_activeFormActionFailsFastWithDccLockReason() {
        FormInstanceRespVO active = activeFormAction("OBSOLETE");
        when(formCenterRuntimeService.findActiveBusinessAction(any(BusinessActionContextReqVO.class)))
                .thenReturn(active);

        assertServiceException(() -> guard.assertNoPendingBusinessAction(activeFile()),
                CONTROLLED_FILE_ACTION_LOCKED, "OBSOLETE / FCI-OBSOLETE-37");

        ArgumentCaptor<BusinessActionContextReqVO> contextCaptor =
                ArgumentCaptor.forClass(BusinessActionContextReqVO.class);
        verify(formCenterRuntimeService).findActiveBusinessAction(contextCaptor.capture());
        assertEquals("DCC", contextCaptor.getValue().getSystemCode());
        assertEquals("CONTROLLED_FILE", contextCaptor.getValue().getObjectType());
        assertEquals("900", contextCaptor.getValue().getObjectId());
        assertEquals("V1.0", contextCaptor.getValue().getObjectVersion());
        assertEquals(DccControlledFileStatusEnum.ACTIVE.getStatus(), contextCaptor.getValue().getObjectState());
    }

    @Test
    void assertNoPendingBusinessAction_noActiveFormActionAllowsOrdinaryAction() {
        when(formCenterRuntimeService.findActiveBusinessAction(any(BusinessActionContextReqVO.class)))
                .thenReturn(null);

        guard.assertNoPendingBusinessAction(activeFile());

        verify(formCenterRuntimeService).findActiveBusinessAction(any(BusinessActionContextReqVO.class));
    }

    private DccControlledFileDO activeFile() {
        return DccControlledFileDO.builder()
                .id(900L)
                .versionNo("V1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
    }

    private FormInstanceRespVO activeFormAction(String actionCode) {
        BusinessActionContextReqVO context = new BusinessActionContextReqVO();
        context.setSystemCode("DCC");
        context.setObjectType("CONTROLLED_FILE");
        context.setObjectId("900");
        context.setObjectVersion("V1.0");
        context.setActionCode(actionCode);
        context.setObjectState(DccControlledFileStatusEnum.ACTIVE.getStatus());
        FormInstanceRespVO active = new FormInstanceRespVO();
        active.setId(37L);
        active.setInstanceCode("FCI-OBSOLETE-37");
        active.setStatus("IN_APPROVAL");
        active.setBpmProcessInstanceId("process-37");
        active.setContext(context);
        return active;
    }
}
