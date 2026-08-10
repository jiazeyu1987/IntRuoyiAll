package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesReportAllocationReleaseStateServiceTest {

    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    @Mock private MesProEdhrReleaseTransactionMapper transactionMapper;

    private MesReportAllocationReleaseStateService service;

    @BeforeEach
    void setUp() {
        service = new MesReportAllocationReleaseStateService(applicationMapper, transactionMapper);
    }

    @Test
    void laterPendingApplicationMustNotHideHistoricalReleasedTransaction() {
        List<Long> ids = List.of(8101L, 8102L);
        when(applicationMapper.selectListByActiveOrderIds(ids)).thenReturn(List.of(
                application(8101L, 9101L), application(8101L, 9102L), application(8102L, 9103L)));
        when(transactionMapper.selectListByIds(List.of(9101L, 9102L, 9103L))).thenReturn(List.of(
                transaction(9101L, "RELEASED"), transaction(9102L, "PENDING_APPROVAL"),
                transaction(9103L, "PRECHECK_PASSED")));

        assertEquals(Set.of(8101L), service.findReleasedActiveOrderIds(ids));
    }

    @Test
    void lockPathMustUseSameHistoricalExistenceSemantics() {
        List<Long> ids = List.of(8101L);
        when(applicationMapper.selectListByActiveOrderIdsForUpdate(ids))
                .thenReturn(List.of(application(8101L, 9101L), application(8101L, 9102L)));
        when(transactionMapper.selectListByIdsForUpdate(List.of(9101L, 9102L))).thenReturn(List.of(
                transaction(9101L, "RELEASED"), transaction(9102L, "PENDING_APPROVAL")));

        assertEquals(Set.of(8101L), service.findReleasedActiveOrderIdsForUpdate(ids));
    }

    private static MesProcessPoolActiveOrderReleaseApplicationDO application(Long activeOrderId, Long transactionId) {
        return MesProcessPoolActiveOrderReleaseApplicationDO.builder()
                .activeOrderId(activeOrderId).releaseTransactionId(transactionId).build();
    }

    private static MesProEdhrReleaseTransactionDO transaction(Long id, String status) {
        return MesProEdhrReleaseTransactionDO.builder().id(id).releaseStatus(status).build();
    }
}
