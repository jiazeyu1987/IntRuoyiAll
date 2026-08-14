package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesReportAllocationQualityGateServiceTest {

    @Mock private MesProProcessPoolEventMapper eventMapper;
    @Mock private MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    @Mock private MesPqcInspectionTaskMapper taskMapper;
    @Mock private MesPqcInspectionPieceDetailMapper detailMapper;

    private MesReportAllocationQualityGateService service;

    @BeforeEach
    void setUp() {
        service = new MesReportAllocationQualityGateService(new MesReportAllocationPoolQuantityService(),
                eventMapper, pqcRecordMapper, taskMapper, detailMapper);
    }

    @Test
    void shouldReturnFullOutputQuantityInsteadOfSampleCount() {
        MesProProcessPoolEventDO submit = submitEvent("411111");
        givenSuccessfulBinding(submit, List.of(detail(1, "SUCCESS"), detail(2, "SUCCESS")));

        BigDecimal quantity = service.requireAllocatablePoolQuantity(submit);

        assertEquals(0, new BigDecimal("411111").compareTo(quantity));
    }

    @Test
    void shouldBlockWhenAnyExpectedSampleIsNotSuccessful() {
        MesProProcessPoolEventDO submit = submitEvent("411111");
        givenSuccessfulBinding(submit, List.of(detail(1, "SUCCESS"), detail(2, "FAILED")));

        assertThrows(ServiceException.class, () -> service.requireAllocatablePoolQuantity(submit));
    }

    private void givenSuccessfulBinding(MesProProcessPoolEventDO submit,
                                        List<MesPqcInspectionPieceDetailDO> details) {
        when(pqcRecordMapper.selectListByProductionSubmitEventId(1001L)).thenReturn(List.of(
                MesProProcessPoolPqcRecordDO.builder()
                        .id(4001L).eventId(2001L).productionSubmitEventId(1001L)
                        .inspectionResult("SUCCESS").build()));
        when(eventMapper.selectByIdForUpdate(2001L)).thenReturn(MesProProcessPoolEventDO.builder()
                .id(2001L).eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .workOrderId(submit.getWorkOrderId()).routeProcessId(submit.getRouteProcessId())
                .processId(submit.getProcessId()).feedbackSourceId(3001L).build());
        when(taskMapper.selectByIdForUpdate(3001L)).thenReturn(MesPqcInspectionTaskDO.builder()
                .id(3001L).workOrderId(submit.getWorkOrderId()).routeProcessId(submit.getRouteProcessId())
                .processId(submit.getProcessId()).actualInspectionQuantity(2).build());
        when(detailMapper.selectListByTaskId(3001L)).thenReturn(details);
    }

    private static MesProProcessPoolEventDO submitEvent(String quantity) {
        return MesProProcessPoolEventDO.builder()
                .id(1001L).eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .workOrderId(9001L).routeProcessId(5001L).processId(6001L)
                .reportOutputQuantity(new BigDecimal(quantity))
                .rawPayload("{\"outputQuantity\":" + quantity + "}").build();
    }

    private static MesPqcInspectionPieceDetailDO detail(int sampleNo, String judgement) {
        return MesPqcInspectionPieceDetailDO.builder().sampleNo(sampleNo).judgement(judgement).build();
    }
}
