package cn.iocoder.yudao.module.mes.service.home;

import cn.iocoder.yudao.module.mes.controller.admin.home.vo.MesHomeSummaryRespVO;
import cn.iocoder.yudao.module.mes.dal.mysql.home.MesHomeStatisticsMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesHomeStatisticsServiceImplTest {

    @Mock
    private MesHomeStatisticsMapper homeStatisticsMapper;
    @Mock
    private MesProEdhrNonconformanceReviewMapper nonconformanceReviewMapper;
    @InjectMocks
    private MesHomeStatisticsServiceImpl homeStatisticsService;

    @Test
    void getHomeSummaryIncludesNonconformanceReviewPendingCount() {
        when(homeStatisticsMapper.selectWorkOrderCountGroupByStatus()).thenReturn(List.of());
        when(homeStatisticsMapper.selectFeedbackSummary(any(), any())).thenReturn(Map.of());
        when(homeStatisticsMapper.selectMachineryCountGroupByStatus()).thenReturn(List.of());
        when(homeStatisticsMapper.selectAndonActiveCount()).thenReturn(0L);
        when(homeStatisticsMapper.selectRepairActiveCount()).thenReturn(0L);
        when(nonconformanceReviewMapper.selectPendingCount()).thenReturn(7L);

        MesHomeSummaryRespVO summary = homeStatisticsService.getHomeSummary();

        assertEquals(7L, summary.getNonconformanceReviewPendingCount());
        assertEquals(BigDecimal.ZERO, summary.getTodayOutput());
    }
}
