package cn.iocoder.yudao.module.mes.service.pro.scheduleorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffPageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderCreateFromWorkOrderReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderCreateFromWorkOrdersReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderProcessWipSettingsReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderProcessWipRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderUpdateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDailyCompareDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderOperationLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Set;

/**
 * MES 排产工单 Service 接口
 */
public interface MesProScheduleOrderService {

    /**
     * 从生产工单生成排产工单。
     *
     * @param reqVO 生成参数
     * @return 排产工单编号
     */
    Long createFromWorkOrder(@Valid MesProScheduleOrderCreateFromWorkOrderReqVO reqVO);

    /**
     * 从生产工单批量生成排产工单，任一工单失败时整体回滚。
     *
     * @param reqVO 批量生成参数
     * @return 排产工单编号列表
     */
    List<Long> createFromWorkOrders(@Valid MesProScheduleOrderCreateFromWorkOrdersReqVO reqVO);

    /**
     * 更新排产工单优先级。
     *
     * @param id 排产工单编号
     * @param priorityNo 优先级排序
     */
    void updatePriority(Long id, Integer priorityNo);

    /**
     * 修改排产工单交期、优先级和备注。
     */
    void updateScheduleOrder(@Valid MesProScheduleOrderUpdateReqVO reqVO);

    /**
     * 批量冻结排产工单。
     */
    void freezeScheduleOrders(@Valid MesProScheduleOrderBatchReqVO reqVO);

    /**
     * 批量解冻排产工单。
     */
    void unfreezeScheduleOrders(@Valid MesProScheduleOrderBatchReqVO reqVO);

    /**
     * 人工完成排产工单。
     */
    void manualFinish(@Valid MesProScheduleOrderActionReqVO reqVO);

    /**
     * 撤销人工完成。
     */
    void revokeManualFinish(@Valid MesProScheduleOrderActionReqVO reqVO);

    /**
     * 批量删除排产工单。
     */
    void deleteScheduleOrders(@Valid MesProScheduleOrderBatchReqVO reqVO);

    /**
     * 获得排产工单。
     */
    MesProScheduleOrderDO getScheduleOrder(Long id);

    /**
     * 获得排产工单分页。
     */
    PageResult<MesProScheduleOrderDO> getScheduleOrderPage(MesProScheduleOrderPageReqVO pageReqVO);

    /**
     * 获得待同步生产工单差异清单。
     */
    MesProScheduleOrderAdmissionDiffPageRespVO getAdmissionDiff(MesProScheduleOrderAdmissionDiffPageReqVO pageReqVO);

    /**
     * 执行排产前健康检查。
     */
    MesProScheduleOrderPreflightRespVO preflight(@Valid MesProScheduleOrderPreflightReqVO reqVO);

    /**
     * 获得排产工单工序快照列表。
     */
    List<MesProScheduleOrderProcessDO> getScheduleOrderProcessList(Long scheduleOrderId);

    /**
     * 批量获得排产工单工序快照列表。
     */
    List<MesProScheduleOrderProcessDO> getScheduleOrderProcessListByScheduleOrderIds(Collection<Long> scheduleOrderIds);

    /**
     * 获得排产工单用于进度展示的报工列表。
     */
    List<MesProFeedbackDO> getProgressFeedbackList(Long scheduleOrderId);

    /**
     * 按真实已完成报工同步排产工单和工序进度。
     */
    void syncFeedbackProgress(Long scheduleOrderId);

    /**
     * 获得排产工单按天计划实际对比。
     */
    List<MesProScheduleOrderDailyCompareDO> getDailyCompare(Long scheduleOrderId, LocalDate startDate, LocalDate endDate);

    /**
     * 按当前工序聚合未完成排产工单在制订单数。
     */
    List<MesProScheduleOrderProcessWipRespVO> getProcessWipStatistics();

    /**
     * 获得最近一次成功排产涉及的排产工单编号集合。
     */
    Set<Long> getLatestSuccessfulApplyScheduleOrderIds();

    /**
     * 保存当前工序在制夜班与开排日期设置。
     */
    void saveProcessWipSettings(@Valid MesProScheduleOrderProcessWipSettingsReqVO reqVO);

    /**
     * 统一班次小时变化后，刷新当前在制工序里已保存的工作台产能快照。
     */
    void refreshProcessWipCapacitySnapshotsForShiftHours(BigDecimal shiftHours);

    /**
     * 获得排产工单操作追溯。
     */
    List<MesProScheduleOrderOperationLogDO> getOperationLogList(Long scheduleOrderId);

    /**
     * 根据已启用工序的计划、报工、剩余数量合计计算排产工单进度摘要。
     */
    ProgressSummary calculateProcessAggregateProgressSummary(BigDecimal scheduleOrderQuantity,
                                                             List<MesProScheduleOrderProcessDO> processes);

    /**
     * 根据真实报工聚合排产工单工序的解释性进度指标。
     */
    Map<Long, ProcessProgressMetrics> calculateProcessProgressMetrics(Long scheduleOrderId,
                                                                      List<MesProScheduleOrderProcessDO> processes);

    record ProgressSummary(BigDecimal totalQuantity, BigDecimal completedQuantity, BigDecimal uncompletedQuantity,
                           BigDecimal progressPercent) {
    }

    record ProcessProgressMetrics(BigDecimal effectiveCompletedQuantity,
                                  BigDecimal pendingApprovalQuantity,
                                  BigDecimal pendingInspectionQuantity,
                                  BigDecimal overReportedQuantity,
                                  BigDecimal reportedQuantity,
                                  BigDecimal remainingQuantity,
                                  BigDecimal progressPercent) {
    }

}
