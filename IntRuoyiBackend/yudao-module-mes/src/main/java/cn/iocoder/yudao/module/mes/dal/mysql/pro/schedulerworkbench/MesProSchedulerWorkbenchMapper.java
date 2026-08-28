package cn.iocoder.yudao.module.mes.dal.mysql.pro.schedulerworkbench;

import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchSummaryRespVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface MesProSchedulerWorkbenchMapper {

    Long selectPendingScheduleOrderCount();

    Long selectTodayScheduledTaskCount(@Param("beginTime") LocalDateTime beginTime,
                                       @Param("endTime") LocalDateTime endTime);

    BigDecimal selectTodayPlannedCapacity(@Param("beginTime") LocalDateTime beginTime,
                                          @Param("endTime") LocalDateTime endTime);

    Long selectTodayFeedbackCount(@Param("beginTime") LocalDateTime beginTime,
                                  @Param("endTime") LocalDateTime endTime);

    BigDecimal selectTodayFeedbackQuantity(@Param("beginTime") LocalDateTime beginTime,
                                           @Param("endTime") LocalDateTime endTime);

    Long selectPendingApprovalFeedbackCount();

    BigDecimal selectCurrentSchedulePlannedQuantity(@Param("scheduleOrderIds") Collection<Long> scheduleOrderIds);

    BigDecimal selectCurrentScheduleReportedQuantity(@Param("scheduleOrderIds") Collection<Long> scheduleOrderIds);

    List<MesProSchedulerWorkbenchSummaryRespVO.ReportedDeviationDetail> selectReportedDeviationDetails(
            @Param("scheduleOrderIds") Collection<Long> scheduleOrderIds);

    List<MesProSchedulerWorkbenchSummaryRespVO.RouteActiveOrder> selectRouteActiveOrders(
            @Param("scheduleOrderIds") Collection<Long> scheduleOrderIds);

    BigDecimal selectTodayAvailableCapacity(@Param("beginTime") LocalDateTime beginTime,
                                            @Param("endTime") LocalDateTime endTime);

    Long selectRepairingMachineryCount();

    Long selectResourceUnconfiguredCount();

    Long selectMaterialShortageCount(@Param("beginTime") LocalDateTime beginTime,
                                     @Param("endTime") LocalDateTime endTime);

    List<MesProSchedulerWorkbenchSummaryRespVO.Bottleneck> selectBottlenecks(
            @Param("beginTime") LocalDateTime beginTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("scheduleOrderIds") Collection<Long> scheduleOrderIds);

}
