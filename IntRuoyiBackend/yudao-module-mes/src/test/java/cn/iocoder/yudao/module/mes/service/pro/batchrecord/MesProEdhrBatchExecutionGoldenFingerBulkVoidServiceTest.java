package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionGoldenFingerBulkVoidReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionGoldenFingerBulkVoidRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRequestReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.List;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_BULK_VOID_EMPTY;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_GOLDEN_FINGER_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesProEdhrBatchExecutionGoldenFingerBulkVoidServiceTest extends BaseMockitoUnitTest {

    private static final long ACTOR_ID = 101L;

    @Mock
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock
    private MesProEdhrGoldenFingerPermissionService goldenFingerPermissionService;
    @Mock
    private MesProEdhrBatchVoidEffectService batchVoidEffectService;
    @InjectMocks
    private MesProEdhrBatchExecutionServiceImpl batchExecutionService;

    @Test
    void goldenFingerBulkVoid_rejectsNonGoldenFingerBeforeSelectingBatches() {
        when(goldenFingerPermissionService.hasGoldenFingerPermission(ACTOR_ID)).thenReturn(false);

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> batchExecutionService.goldenFingerBulkVoid(validRequest()));

            assertEquals(PRO_EDHR_BATCH_EXECUTION_GOLDEN_FINGER_REQUIRED.getCode(), exception.getCode());
        }
        verify(batchExecutionMapper, never()).selectList(any(EdhrBatchExecutionPageReqVO.class));
        verify(batchVoidEffectService, never()).executeDirectPlatformVoidBatchExecution(any(), any());
    }

    @Test
    void goldenFingerBulkVoid_usesCurrentFilterSkipsTerminalRowsAndDirectlyVoidsCandidates() {
        when(goldenFingerPermissionService.hasGoldenFingerPermission(ACTOR_ID)).thenReturn(true);
        when(batchExecutionMapper.selectList(any(EdhrBatchExecutionPageReqVO.class))).thenReturn(List.of(
                batch(10L, "EDHRB-10", MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED),
                batch(11L, "EDHRB-11", MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_ARCHIVED),
                batch(12L, "EDHRB-12", MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REJECTED)
        ));
        when(batchVoidEffectService.executeDirectPlatformVoidBatchExecution(any(), eq(ACTOR_ID)))
                .thenReturn(new EdhrRecordChangeRespVO().setId(9100L));

        EdhrBatchExecutionGoldenFingerBulkVoidRespVO response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            response = batchExecutionService.goldenFingerBulkVoid(validRequest());
        }

        assertEquals(3, response.getMatchedCount());
        assertEquals(1, response.getVoidedCount());
        assertEquals(2, response.getSkippedCount());
        ArgumentCaptor<EdhrRecordChangeRequestReqVO> reqCaptor =
                ArgumentCaptor.forClass(EdhrRecordChangeRequestReqVO.class);
        verify(batchVoidEffectService).precheckPlatformVoidBatchExecution(reqCaptor.capture());
        verify(batchVoidEffectService).executeDirectPlatformVoidBatchExecution(reqCaptor.capture(), eq(ACTOR_ID));
        assertEquals(10L, reqCaptor.getAllValues().get(0).getBatchExecutionId());
        assertEquals("DATA_ERROR", reqCaptor.getAllValues().get(0).getReasonCategory());
    }

    @Test
    void goldenFingerBulkVoid_keepsSelectedBatchExecutionIdsOnFilter() {
        when(goldenFingerPermissionService.hasGoldenFingerPermission(ACTOR_ID)).thenReturn(true);
        when(batchExecutionMapper.selectList(any(EdhrBatchExecutionPageReqVO.class))).thenReturn(List.of(
                batch(10L, "EDHRB-10", MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED)
        ));
        when(batchVoidEffectService.executeDirectPlatformVoidBatchExecution(any(), eq(ACTOR_ID)))
                .thenReturn(new EdhrRecordChangeRespVO().setId(9100L));
        EdhrBatchExecutionGoldenFingerBulkVoidReqVO request = validRequest()
                .setFilter(new EdhrBatchExecutionPageReqVO()
                        .setBatchCode("BULK-VOID")
                        .setBatchExecutionIds(List.of(10L, 12L)));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            batchExecutionService.goldenFingerBulkVoid(request);
        }

        ArgumentCaptor<EdhrBatchExecutionPageReqVO> filterCaptor =
                ArgumentCaptor.forClass(EdhrBatchExecutionPageReqVO.class);
        verify(batchExecutionMapper).selectList(filterCaptor.capture());
        assertEquals(List.of(10L, 12L), filterCaptor.getValue().getBatchExecutionIds());
    }

    @Test
    void goldenFingerBulkVoid_failsWhenCurrentFilterHasNoVoidableCandidates() {
        when(goldenFingerPermissionService.hasGoldenFingerPermission(ACTOR_ID)).thenReturn(true);
        when(batchExecutionMapper.selectList(any(EdhrBatchExecutionPageReqVO.class))).thenReturn(List.of(
                batch(11L, "EDHRB-11", MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_ARCHIVED)
        ));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> batchExecutionService.goldenFingerBulkVoid(validRequest()));

            assertEquals(PRO_EDHR_BATCH_EXECUTION_BULK_VOID_EMPTY.getCode(), exception.getCode());
        }
        verify(batchVoidEffectService, never()).executeDirectPlatformVoidBatchExecution(any(), any());
    }

    private static MockedStatic<SecurityFrameworkUtils> mockLoginUser() {
        MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(ACTOR_ID);
        return security;
    }

    private static EdhrBatchExecutionGoldenFingerBulkVoidReqVO validRequest() {
        return new EdhrBatchExecutionGoldenFingerBulkVoidReqVO()
                .setFilter(new EdhrBatchExecutionPageReqVO().setBatchCode("BULK-VOID"))
                .setReasonCategory("DATA_ERROR")
                .setReasonText("金手指批量测试作废")
                .setPassword("secret")
                .setComment("bulk void");
    }

    private static MesProEdhrBatchExecutionDO batch(Long id, String code, Integer status) {
        return MesProEdhrBatchExecutionDO.builder()
                .id(id)
                .batchExecutionCode(code)
                .batchCode("BULK-VOID")
                .status(status)
                .build();
    }
}
