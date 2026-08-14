package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDiffDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionDiffMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProcessPoolEventRevisionServiceTest {

    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesProProcessPoolEventRevisionMapper revisionMapper;
    @Mock
    private MesProProcessPoolEventRevisionDiffMapper revisionDiffMapper;
    @Mock
    private MesProcessPoolFifoAllocationService fifoAllocationService;
    @Mock
    private MesProcessPoolSubmissionReviewMapper submissionReviewMapper;

    private MesProcessPoolEventRevisionService service;

    @BeforeEach
    void setUp() {
        service = new MesProcessPoolEventRevisionServiceImpl(eventMapper, revisionMapper,
                revisionDiffMapper, fifoAllocationService, submissionReviewMapper);
    }

    @Test
    void updateUnallocatedEventCreatesFieldDiffAndSignatureLog() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        when(submissionReviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(rejectedReview());
        when(eventMapper.selectBySignatureId(9002L)).thenReturn(null);
        when(revisionMapper.selectBySignatureId(9002L)).thenReturn(null);
        when(revisionMapper.insert(any(MesProProcessPoolEventRevisionDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProProcessPoolEventRevisionDO.class).setId(7001L);
            return 1;
        });

        Long revisionId = service.updateOriginalRecord(updateReq());

        assertEquals(7001L, revisionId);
        ArgumentCaptor<MesProProcessPoolEventRevisionDO> revisionCaptor =
                ArgumentCaptor.forClass(MesProProcessPoolEventRevisionDO.class);
        verify(revisionMapper).insert(revisionCaptor.capture());
        MesProProcessPoolEventRevisionDO revision = revisionCaptor.getValue();
        assertEquals(1001L, revision.getEventId());
        assertEquals("{\"outputQuantity\":10,\"equipmentPressure\":\"20\"}", revision.getBeforePayload());
        assertEquals("{\"outputQuantity\":12,\"equipmentPressure\":\"22\"}", revision.getAfterPayload());
        assertEquals("录入时压力参数少填 2", revision.getChangeReason());
        assertEquals(9002L, revision.getRevisionSignatureId());
        assertEquals(2001L, revision.getRevisionSignatureUserId());
        assertEquals(2001L, revision.getModifiedByUserId());
        assertEquals(MesProProcessPoolEventRevisionDO.STATUS_EFFECTIVE, revision.getRevisionStatus());
        assertNotNull(revision.getServerRevisionTime());

        ArgumentCaptor<MesProProcessPoolEventRevisionDiffDO> diffCaptor =
                ArgumentCaptor.forClass(MesProProcessPoolEventRevisionDiffDO.class);
        verify(revisionDiffMapper).insert(diffCaptor.capture());
        MesProProcessPoolEventRevisionDiffDO diff = diffCaptor.getValue();
        assertEquals(7001L, diff.getRevisionId());
        assertEquals(1001L, diff.getEventId());
        assertEquals("equipmentPressure", diff.getFieldCode());
        assertEquals("设备压力", diff.getFieldName());
        assertEquals("20", diff.getBeforeValue());
        assertEquals("22", diff.getAfterValue());
        assertEquals(Boolean.FALSE, diff.getAffectsQuantityFragment());
        assertEquals(MesProcessPoolFragmentOriginalField.REMARK.name(), diff.getOriginalFieldCode());
        assertEquals("备注", diff.getOriginalFieldName());

        ArgumentCaptor<MesProProcessPoolEventDO> eventCaptor =
                ArgumentCaptor.forClass(MesProProcessPoolEventDO.class);
        verify(eventMapper).updateById(eventCaptor.capture());
        assertEquals(1001L, eventCaptor.getValue().getId());
        assertEquals("{\"outputQuantity\":12,\"equipmentPressure\":\"22\"}", eventCaptor.getValue().getRawPayload());
    }

    @Test
    void rejectsUpdateWithoutNewSignature() {
        MesProcessPoolEventRevisionUpdateReqBO req = updateReq().setRevisionSignatureId(9001L);
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        when(submissionReviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(rejectedReview());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateOriginalRecord(req));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_SIGNATURE_REUSED.getCode(), ex.getCode());
        verify(revisionMapper, never()).insert(any(MesProProcessPoolEventRevisionDO.class));
        verify(eventMapper, never()).updateById(any(MesProProcessPoolEventDO.class));
    }

    @Test
    void rejectsUpdateWithoutChangeReason() {
        MesProcessPoolEventRevisionUpdateReqBO req = updateReq().setChangeReason("   ");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateOriginalRecord(req));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_CHANGE_REASON_REQUIRED.getCode(), ex.getCode());
        verify(revisionMapper, never()).insert(any(MesProProcessPoolEventRevisionDO.class));
        verify(eventMapper, never()).updateById(any(MesProProcessPoolEventDO.class));
    }

    @Test
    void rejectsUpdateWhenRevisionSignatureAlreadyUsed() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        when(submissionReviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(rejectedReview());
        when(eventMapper.selectBySignatureId(9002L)).thenReturn(null);
        when(revisionMapper.selectBySignatureId(9002L)).thenReturn(new MesProProcessPoolEventRevisionDO());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateOriginalRecord(updateReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_SIGNATURE_DUPLICATE.getCode(), ex.getCode());
        verify(revisionMapper, never()).insert(any(MesProProcessPoolEventRevisionDO.class));
        verify(eventMapper, never()).updateById(any(MesProProcessPoolEventDO.class));
    }

    @Test
    void rejectsUpdateWithoutRevisionSignatureSnapshot() {
        MesProcessPoolEventRevisionUpdateReqBO req = updateReq().setRevisionSignatureSnapshot("   ");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateOriginalRecord(req));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
        verify(revisionMapper, never()).insert(any(MesProProcessPoolEventRevisionDO.class));
        verify(eventMapper, never()).updateById(any(MesProProcessPoolEventDO.class));
    }

    @Test
    void rejectsUpdateWhenEventRawPayloadMissing() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event().setRawPayload(" "));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateOriginalRecord(updateReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
        verify(revisionMapper, never()).insert(any(MesProProcessPoolEventRevisionDO.class));
        verify(eventMapper, never()).updateById(any(MesProProcessPoolEventDO.class));
    }

    @Test
    void rejectsUpdateWhenEventRawPayloadIsInvalidJson() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event().setRawPayload("{bad"));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateOriginalRecord(updateReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
        verify(revisionMapper, never()).insert(any(MesProProcessPoolEventRevisionDO.class));
        verify(eventMapper, never()).updateById(any(MesProProcessPoolEventDO.class));
    }

    @Test
    void rejectsUpdateWhenAfterPayloadIsInvalidJson() {
        MesProcessPoolEventRevisionUpdateReqBO req = updateReq().setAfterPayload("{bad");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateOriginalRecord(req));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
        verify(revisionMapper, never()).insert(any(MesProProcessPoolEventRevisionDO.class));
        verify(eventMapper, never()).updateById(any(MesProProcessPoolEventDO.class));
    }

    @Test
    void rejectsUpdateWhenAffectsQuantityFragmentIsNull() {
        MesProcessPoolEventRevisionUpdateReqBO req = updateReq();
        req.getChangedFields().get(0).setAffectsQuantityFragment(null);
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        when(submissionReviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(rejectedReview());
        when(eventMapper.selectBySignatureId(9002L)).thenReturn(null);
        when(revisionMapper.selectBySignatureId(9002L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateOriginalRecord(req));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_DIFF_REQUIRED.getCode(), ex.getCode());
        verify(revisionMapper, never()).insert(any(MesProProcessPoolEventRevisionDO.class));
        verify(eventMapper, never()).updateById(any(MesProProcessPoolEventDO.class));
    }

    @Test
    void rejectsCorrectionWhenLatestSubmissionReviewIsMissing() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        when(submissionReviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateOriginalRecord(updateReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_REJECTED_REVIEW_REQUIRED.getCode(), ex.getCode());
        verify(revisionMapper, never()).insert(any(MesProProcessPoolEventRevisionDO.class));
        verify(eventMapper, never()).updateById(any(MesProProcessPoolEventDO.class));
    }

    @Test
    void rejectsCorrectionWhenLatestSubmissionReviewIsApproved() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        when(submissionReviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(approvedReview());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateOriginalRecord(updateReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_REJECTED_REVIEW_REQUIRED.getCode(), ex.getCode());
        verify(revisionMapper, never()).insert(any(MesProProcessPoolEventRevisionDO.class));
        verify(eventMapper, never()).updateById(any(MesProProcessPoolEventDO.class));
    }

    static MesProProcessPoolEventDO event() {
        return MesProProcessPoolEventDO.builder()
                .id(1001L)
                .poolId(501L)
                .workOrderId(3001L)
                .routeId(4001L)
                .routeProcessId(5001L)
                .processId(6001L)
                .actualEmployeeId(2001L)
                .rawPayload("{\"outputQuantity\":10,\"equipmentPressure\":\"20\"}")
                .serverSubmitTime(LocalDateTime.of(2026, 7, 30, 8, 30))
                .signatureId(9001L)
                .signatureUserId(2001L)
                .build();
    }

    static MesProcessPoolEventRevisionUpdateReqBO updateReq() {
        return MesProcessPoolEventRevisionUpdateReqBO.builder()
                .eventId(1001L)
                .afterPayload("{\"outputQuantity\":12,\"equipmentPressure\":\"22\"}")
                .changeReason("录入时压力参数少填 2")
                .revisionSignatureId(9002L)
                .revisionSignatureUserId(2001L)
                .revisionSignatureSnapshot("{\"signedBy\":\"张可莹\"}")
                .modifiedByUserId(2001L)
                .changedFields(List.of(MesProcessPoolEventRevisionFieldChangeBO.builder()
                        .fieldCode("equipmentPressure")
                        .fieldName("设备压力")
                        .beforeValue("20")
                        .afterValue("22")
                        .affectsQuantityFragment(false)
                        .originalField(MesProcessPoolFragmentOriginalField.REMARK)
                        .build()))
                .build();
    }

    static MesProcessPoolSubmissionReviewDO rejectedReview() {
        return MesProcessPoolSubmissionReviewDO.builder()
                .id(8001L)
                .eventId(1001L)
                .leaderUserId(3001L)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_REJECTED)
                .reviewRemark("压力曲线异常，退回补正")
                .reviewedAt(LocalDateTime.of(2026, 8, 3, 10, 30))
                .build();
    }

    static MesProcessPoolSubmissionReviewDO approvedReview() {
        return MesProcessPoolSubmissionReviewDO.builder()
                .id(8002L)
                .eventId(1001L)
                .leaderUserId(3001L)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewRemark("数据和签名一致")
                .reviewedAt(LocalDateTime.of(2026, 8, 3, 11, 30))
                .build();
    }
}
