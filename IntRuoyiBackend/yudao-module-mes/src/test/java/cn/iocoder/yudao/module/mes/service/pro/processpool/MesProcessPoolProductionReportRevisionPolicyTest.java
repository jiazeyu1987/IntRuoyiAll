package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionDiffMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProcessPoolProductionReportRevisionPolicyTest {

    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesProProcessPoolEventRevisionMapper revisionMapper;
    @Mock
    private MesProProcessPoolEventRevisionDiffMapper revisionDiffMapper;
    @Mock
    private MesProcessPoolFifoAllocationService fifoAllocationService;
    @Mock
    private MesProcessPoolSubmissionReviewMapper reviewMapper;

    private MesProcessPoolEventRevisionService service;

    @BeforeEach
    void setUp() {
        service = new MesProcessPoolEventRevisionServiceImpl(
                eventMapper, revisionMapper, revisionDiffMapper, fifoAllocationService, reviewMapper);
    }

    @Test
    void productionLeaderCanCorrectAnUnreviewedProductionReport() {
        MesProProcessPoolEventDO event = MesProcessPoolEventRevisionServiceTest.event()
                .setEventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT);
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event);
        when(reviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(null);
        when(eventMapper.selectBySignatureId(9002L)).thenReturn(null);
        when(revisionMapper.selectBySignatureId(9002L)).thenReturn(null);
        when(revisionMapper.insert(any(MesProProcessPoolEventRevisionDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProProcessPoolEventRevisionDO.class).setId(7010L);
            return 1;
        });

        assertEquals(7010L, service.updateProductionReportRecord(
                MesProcessPoolEventRevisionServiceTest.updateReq()));

        verify(revisionMapper).insert(any(MesProProcessPoolEventRevisionDO.class));
    }

    @Test
    void approvedProductionReportRemainsImmutable() {
        MesProProcessPoolEventDO event = MesProcessPoolEventRevisionServiceTest.event()
                .setEventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT);
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event);
        when(reviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(
                MesProcessPoolSubmissionReviewDO.builder()
                        .eventId(1001L)
                        .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                        .build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.updateProductionReportRecord(MesProcessPoolEventRevisionServiceTest.updateReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_PRODUCTION_REPORT_APPROVED_LOCKED.getCode(),
                ex.getCode());
        verify(revisionMapper, never()).insert(any(MesProProcessPoolEventRevisionDO.class));
    }
}
