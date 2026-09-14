package cn.iocoder.yudao.module.mes.service.pro.feedback;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.itemconsume.MesWmItemConsumeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productproduce.MesWmProductProduceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productproduce.MesWmProductProduceLineDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.enums.wm.MesWmQualityStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import cn.iocoder.yudao.module.mes.service.pro.scheduleorder.MesProScheduleOrderService;
import cn.iocoder.yudao.module.mes.service.pro.task.MesProTaskService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.iocoder.yudao.module.mes.service.wm.itemconsume.MesWmItemConsumeService;
import cn.iocoder.yudao.module.mes.service.wm.productproduce.MesWmProductProduceLineService;
import cn.iocoder.yudao.module.mes.service.wm.productproduce.MesWmProductProduceService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED;

/**
 * MES 生产报工 Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Validated
public class MesProFeedbackServiceImpl implements MesProFeedbackService {

    @Resource
    private MesProFeedbackMapper feedbackMapper;
    @Resource
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Resource
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Resource
    private FeedbackScheduleLinkageGuard feedbackScheduleLinkageGuard;
    @Resource
    private MesProEdhrNonconformanceReviewService nonconformanceReviewService;

    @Resource
    private MesProWorkOrderService workOrderService;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesMdWorkstationService workstationService;
    @Resource
    @Lazy // 避免循环依赖
    private MesProTaskService taskService;
    @Resource
    @Lazy
    private MesProScheduleOrderService scheduleOrderService;
    @Resource
    private MesWmItemConsumeService itemConsumeService;
    @Resource
    private MesWmProductProduceService productProduceService;
    @Resource
    private MesWmProductProduceLineService produceLineService;

    @Override
    public Long createFeedback(MesProFeedbackSaveReqVO createReqVO) {
        return createFeedbackInternal(createReqVO, false);
    }

    @Override
    public Long createFeedbackWithScheduleSnapshot(MesProFeedbackSaveReqVO createReqVO) {
        return createFeedbackInternal(createReqVO, true);
    }

    @Override
    public Long createFrontlineFeedback(MesProFeedbackSaveReqVO createReqVO) {
        validateFrontlineFeedbackData(createReqVO);
        MesProFeedbackDO feedback = BeanUtils.toBean(createReqVO, MesProFeedbackDO.class)
                .setStatus(MesProFeedbackStatusEnum.PREPARE.getStatus());
        feedbackMapper.insert(feedback);
        return feedback.getId();
    }

    private Long createFeedbackInternal(MesProFeedbackSaveReqVO createReqVO, boolean keepProvidedScheduleSnapshot) {
        // 1. 校验
        MesProTaskDO task = validateFeedbackData(createReqVO, keepProvidedScheduleSnapshot);
        nonconformanceReviewService.ensureWorkOrderNotFrozen(createReqVO.getWorkOrderId(), "报工");

        // 2. 插入（自动填充 itemId）
        MesProFeedbackDO feedback = BeanUtils.toBean(createReqVO, MesProFeedbackDO.class)
                .setStatus(MesProFeedbackStatusEnum.PREPARE.getStatus())
                .setItemId(task.getItemId());
        fillScheduleOrderSnapshot(feedback, keepProvidedScheduleSnapshot);
        feedbackMapper.insert(feedback);
        return feedback.getId();
    }

    @Override
    public void updateFeedback(MesProFeedbackSaveReqVO updateReqVO) {
        // 1.1 校验存在 + 草稿状态
        MesProFeedbackDO existing = validateFeedbackStatusPrepare(updateReqVO.getId());
        boolean keepExistingScheduleSnapshot = existing.getScheduleOrderId() != null
                && existing.getScheduleOrderProcessId() != null;
        if (keepExistingScheduleSnapshot) {
            updateReqVO.setScheduleOrderId(existing.getScheduleOrderId());
            updateReqVO.setScheduleOrderProcessId(existing.getScheduleOrderProcessId());
        }
        // 1.2 校验业务数据
        MesProTaskDO task = validateFeedbackData(updateReqVO, keepExistingScheduleSnapshot);

        // 2. 更新（自动填充 itemId）
        MesProFeedbackDO updateObj = BeanUtils.toBean(updateReqVO, MesProFeedbackDO.class)
                .setItemId(task.getItemId());
        fillScheduleOrderSnapshot(updateObj, keepExistingScheduleSnapshot);
        feedbackMapper.updateById(updateObj);
    }

    @Override
    public void deleteFeedback(Long id) {
        // 1. 校验存在 + 草稿状态
        validateFeedbackStatusPrepare(id);

        // 2. 删除
        feedbackMapper.deleteById(id);
    }

    @Override
    public MesProFeedbackDO getFeedback(Long id) {
        return feedbackMapper.selectById(id);
    }

    @Override
    public PageResult<MesProFeedbackDO> getFeedbackPage(MesProFeedbackPageReqVO pageReqVO) {
        return feedbackMapper.selectPage(pageReqVO);
    }

    @Override
    public void submitFeedback(Long id) {
        submitFeedback(id, false);
    }

    @Override
    public void submitFeedback(Long id, boolean allowImportedDraft) {
        // 1. 校验存在 + 草稿状态
        MesProFeedbackDO feedback = validateFeedbackStatusPrepare(id);
        nonconformanceReviewService.ensureWorkOrderNotFrozen(feedback.getWorkOrderId(), "报工");
        if (!allowImportedDraft && feedback.getSourceImportRecordId() != null) {
            throw exception(PRO_FEEDBACK_IMPORT_DIRECT_SUBMIT_FORBIDDEN);
        }

        // 2. 更新状态为审批中（报工人和报工时间由表单保存时确定，提交不覆盖）
        feedbackMapper.updateById(new MesProFeedbackDO().setId(id)
                .setStatus(MesProFeedbackStatusEnum.APPROVING.getStatus()));
        syncScheduleOrderProgressIfLinked(feedback);
    }

    @Override
    public void rejectFeedback(Long id) {
        rejectFeedback(id, null);
    }

    @Override
    public void rejectFeedback(Long id, String reason) {
        // 1. 校验存在 + 审批中状态
        validateFeedbackStatusApproving(id);

        // 2. 更新状态为草稿，并保留审批中心驳回原因
        feedbackMapper.updateById(new MesProFeedbackDO().setId(id)
                .setStatus(MesProFeedbackStatusEnum.PREPARE.getStatus())
                .setRemark(trimToNull(reason)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveFeedback(Long id) {
        // 1.1 校验存在 + 审批中状态
        MesProFeedbackDO feedback = validateFeedbackStatusApproving(id);
        nonconformanceReviewService.ensureWorkOrderNotFrozen(feedback.getWorkOrderId(), "PQC提交");
        // 1.2 校验报工数量 > 0
        if (feedback.getFeedbackQuantity() == null
                || feedback.getFeedbackQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_FEEDBACK_QUANTITY_MUST_POSITIVE);
        }
        // 1.3 校验任务未完成
        taskService.validateTaskNotFinished(feedback.getTaskId());

        // 2.1 查询工序的关键工序标识 + 检验标识（需在 uncheckQuantity 校验之前，因为质检工序允许 uncheckQuantity > 0）
        MesProRouteProcessDO routeProcess = resolveCurrentRouteProcess(feedback);
        boolean keyFlag = Boolean.TRUE.equals(routeProcess.getKeyFlag());
        boolean checkFlag = Boolean.TRUE.equals(routeProcess.getCheckFlag());
        feedback.setRouteId(routeProcess.getRouteId());
        feedback.setProcessId(routeProcess.getProcessId());
        // 2.2 非质检工序：仍有待检数量时不能审批（质检工序的 uncheckQuantity > 0 是正常状态，不做拦截）
        if (!checkFlag
                && feedback.getUncheckQuantity() != null
                && feedback.getUncheckQuantity().compareTo(BigDecimal.ZERO) > 0) {
            throw exception(PRO_FEEDBACK_UNCHECK_QUANTITY_EXISTS, feedback.getUncheckQuantity());
        }

        // 3. 物料消耗：根据工序 BOM 生成消耗记录并执行扣减
        MesWmItemConsumeDO itemConsume = itemConsumeService.generateItemConsume(feedback);
        if (itemConsume != null) {
            itemConsumeService.finishItemConsume(itemConsume.getId());
        }

        // 4. 关键工序：生成产出单，并根据是否需要检验决定入库方式
        if (keyFlag) {
            // 4.1 需要检验：生成产出单（质量状态=待检验），更新报工状态为待检验，等质检完成回调后再入库
            if (checkFlag) {
                // 完成时回调见：MesQcIpqcServiceImpl#finishIpqc → splitPendingAndFinishProduce + completeFeedbackFromIpqc
                productProduceService.generateProductProduce(feedback, true);
                feedbackMapper.updateById(new MesProFeedbackDO().setId(id)
                        .setStatus(MesProFeedbackStatusEnum.UNCHECK.getStatus()));
                return false;
            }
            // 4.2 无需检验：生成产出单（按合格/不合格拆行），直接完成入库，并更新任务/工单的已生产数量
            MesWmProductProduceDO produce = productProduceService.generateProductProduce(feedback, false);
            productProduceService.finishProductProduce(produce.getId());
            updateTaskAndWorkOrderByFeedback(feedback);
        }

        // 5. 非关键工序 / 关键非质检工序：直接完成（清零 uncheckQuantity 防止 !key+check 留脏数据）
        feedbackMapper.updateById(new MesProFeedbackDO().setId(id)
                .setStatus(MesProFeedbackStatusEnum.FINISHED.getStatus())
                .setUncheckQuantity(BigDecimal.ZERO));
        syncScheduleOrderProgressIfLinked(feedback);
        return true; // 已完成
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 根据当前报工单的最终结果，更新生产任务和生产工单的进度
     *
     * <ul>
     *   <li>使用报工数量（feedbackQuantity），累加任务和工单的已生产数量</li>
     *   <li>使用产出单行按 qualityStatus 聚合的合格品/不合格品数量，累加任务的合格品/不合格品数量
     *       （不直接用 feedback 上的数量，确保质检回调场景下数量来源正确）</li>
     * </ul>
     *
     * @param feedback 报工单
     */
    private void updateTaskAndWorkOrderByFeedback(MesProFeedbackDO feedback) {
        // 1. 查询该报工单关联的所有产出单行，按质量状态聚合数量
        BigDecimal qualifiedQty = BigDecimal.ZERO;
        BigDecimal unqualifiedQty = BigDecimal.ZERO;
        List<MesWmProductProduceLineDO> lines = produceLineService.getProductProduceLineListByFeedbackId(feedback.getId());
        for (MesWmProductProduceLineDO line : lines) {
            if (ObjUtil.equal(line.getQualityStatus(), MesWmQualityStatusEnum.FAIL.getStatus())) {
                unqualifiedQty = unqualifiedQty.add(line.getQuantity());
            }
            if (ObjUtil.equal(line.getQualityStatus(), MesWmQualityStatusEnum.PASS.getStatus())) {
                qualifiedQty = qualifiedQty.add(line.getQuantity());
            }
        }

        // 2. 更新任务的已生产/合格/不合格数量
        taskService.updateProducedQuantity(feedback.getTaskId(),
                feedback.getFeedbackQuantity(), qualifiedQty, unqualifiedQty);
        // 3. 更新工单的已生产数量
        workOrderService.updateProducedQuantity(feedback.getWorkOrderId(),
                feedback.getFeedbackQuantity());
    }

    // ==================== 校验方法 ====================

    @Override
    public MesProFeedbackDO validateFeedbackExists(Long id) {
        MesProFeedbackDO feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw exception(PRO_FEEDBACK_NOT_EXISTS);
        }
        return feedback;
    }

    private MesProFeedbackDO validateFeedbackStatusPrepare(Long id) {
        MesProFeedbackDO feedback = validateFeedbackExists(id);
        if (ObjUtil.notEqual(feedback.getStatus(), MesProFeedbackStatusEnum.PREPARE.getStatus())) {
            throw exception(PRO_FEEDBACK_NOT_PREPARE);
        }
        return feedback;
    }

    private MesProFeedbackDO validateFeedbackStatusApproving(Long id) {
        MesProFeedbackDO feedback = validateFeedbackExists(id);
        if (ObjUtil.notEqual(feedback.getStatus(), MesProFeedbackStatusEnum.APPROVING.getStatus())) {
            throw exception(PRO_FEEDBACK_NOT_APPROVING);
        }
        return feedback;
    }

    /**
     * 校验报工单的业务数据（创建 & 修改共用）
     *
     * @param reqVO 报工请求
     * @return 关联的生产任务
     */
    private MesProTaskDO validateFeedbackData(MesProFeedbackSaveReqVO reqVO,
                                              boolean keepProvidedScheduleSnapshot) {
        FeedbackRouteContext routeContext = resolveFeedbackRouteContext(reqVO, keepProvidedScheduleSnapshot);
        // 1. 校验工作站存在
        MesMdWorkstationDO workstation = workstationService.validateWorkstationExists(reqVO.getWorkstationId());
        if (ObjUtil.notEqual(workstation.getProcessId(), routeContext.relationProcessId())) {
            throw exception(PRO_WORKSTATION_PROCESS_MISMATCH);
        }

        // 2.1 校验工艺路线 + 工序配置有效
        MesProRouteProcessDO routeProcess = routeContext.routeProcess();
        // 2.2 校验数量
        validateFeedbackQuantity(reqVO, routeProcess);

        // 3. 校验工单已确认
        MesProWorkOrderDO workOrder = workOrderService.validateWorkOrderConfirmed(reqVO.getWorkOrderId());
        if (ObjUtil.notEqual(workOrder.getProductId(), reqVO.getItemId())) {
            throw exception(PRO_WORK_ORDER_PRODUCT_MISMATCH);
        }

        // 4. 校验任务存在且未终态（已完成/已取消），并返回任务用于冗余 itemId
        MesProTaskDO task = taskService.validateTaskNotFinished(reqVO.getTaskId());
        validateTaskRelation(task, workstation, workOrder, reqVO,
                routeContext.relationRouteId(), routeContext.relationProcessId());
        return task;
    }

    private void validateFrontlineFeedbackData(MesProFeedbackSaveReqVO reqVO) {
        requireFrontlineFeedbackContext(reqVO, "request");
        requireFrontlineFeedbackContext(reqVO.getCode(), "code");
        requireFrontlineFeedbackContext(reqVO.getType(), "type");
        requireFrontlineFeedbackContext(reqVO.getWorkstationId(), "workstationId");
        requireFrontlineFeedbackContext(reqVO.getRouteId(), "routeId");
        requireFrontlineFeedbackContext(reqVO.getProcessId(), "processId");
        requireFrontlineFeedbackContext(reqVO.getFeedbackQuantity(), "feedbackQuantity");
        requireFrontlineFeedbackContext(reqVO.getFeedbackUserId(), "feedbackUserId");
        requireFrontlineFeedbackContext(reqVO.getFeedbackTime(), "feedbackTime");
        requireFrontlineFeedbackContext(reqVO.getApproveUserId(), "approveUserId");

        FeedbackRouteContext routeContext = resolveFeedbackRouteContext(reqVO, false);
        MesMdWorkstationDO workstation = workstationService.validateWorkstationExists(reqVO.getWorkstationId());
        if (ObjUtil.notEqual(workstation.getProcessId(), routeContext.relationProcessId())) {
            throw exception(PRO_WORKSTATION_PROCESS_MISMATCH);
        }
        validateFrontlineFeedbackQuantity(reqVO);
    }

    private void validateFrontlineFeedbackQuantity(MesProFeedbackSaveReqVO reqVO) {
        BigDecimal feedbackQuantity = reqVO.getFeedbackQuantity();
        BigDecimal qualifiedQuantity = ObjectUtil.defaultIfNull(reqVO.getQualifiedQuantity(), BigDecimal.ZERO);
        BigDecimal unqualifiedQuantity = ObjectUtil.defaultIfNull(reqVO.getUnqualifiedQuantity(), BigDecimal.ZERO);
        if (feedbackQuantity.compareTo(BigDecimal.ZERO) < 0
                || qualifiedQuantity.compareTo(BigDecimal.ZERO) < 0
                || unqualifiedQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw exception(PRO_FEEDBACK_QUANTITY_MUST_POSITIVE);
        }
    }

    private void validateFeedbackQuantity(MesProFeedbackSaveReqVO reqVO, MesProRouteProcessDO routeProcess) {
        boolean checkFlag = Boolean.TRUE.equals(routeProcess.getCheckFlag());
        if (checkFlag) {
            // 需要检验：只需填报工数量，且必须 > 0
            if (reqVO.getFeedbackQuantity() == null
                    || reqVO.getFeedbackQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PRO_FEEDBACK_QUANTITY_MUST_POSITIVE);
            }
            return;
        }
        // 不需检验：需填合格品 + 不良品数量，合计 > 0
        BigDecimal qualified = ObjectUtil.defaultIfNull(reqVO.getQualifiedQuantity(), BigDecimal.ZERO);
        BigDecimal unqualified = ObjectUtil.defaultIfNull(reqVO.getUnqualifiedQuantity(), BigDecimal.ZERO);
        if (qualified.add(unqualified).compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_FEEDBACK_QUALIFIED_UNQUALIFIED_REQUIRED);
        }
    }

    private void requireFrontlineFeedbackContext(Object value, String fieldName) {
        if (value == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, fieldName);
        }
        if (value instanceof String text && text.isBlank()) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, fieldName);
        }
    }

    private FeedbackRouteContext resolveFeedbackRouteContext(MesProFeedbackSaveReqVO reqVO,
                                                             boolean keepProvidedScheduleSnapshot) {
        if (!keepProvidedScheduleSnapshot
                || reqVO.getScheduleOrderId() == null
                || reqVO.getScheduleOrderProcessId() == null) {
            Long relationRouteId = reqVO.getRouteId();
            Long relationProcessId = reqVO.getProcessId();
            MesProRouteProcessDO routeProcess =
                    routeProcessService.resolveCurrentRouteProcess(null, relationRouteId, relationProcessId);
            reqVO.setRouteId(routeProcess.getRouteId());
            reqVO.setProcessId(routeProcess.getProcessId());
            return new FeedbackRouteContext(routeProcess, relationRouteId, relationProcessId);
        }

        MesProScheduleOrderDO scheduleOrder = scheduleOrderMapper.selectById(reqVO.getScheduleOrderId());
        MesProScheduleOrderProcessDO scheduleOrderProcess =
                scheduleOrderProcessMapper.selectById(reqVO.getScheduleOrderProcessId());
        if (scheduleOrder == null
                || scheduleOrderProcess == null
                || ObjUtil.notEqual(scheduleOrder.getWorkOrderId(), reqVO.getWorkOrderId())
                || ObjUtil.notEqual(scheduleOrderProcess.getScheduleOrderId(), scheduleOrder.getId())
                || ObjUtil.notEqual(reqVO.getRouteId(), scheduleOrder.getRouteId())
                || !Boolean.TRUE.equals(scheduleOrderProcess.getEnabled())) {
            throw exception(PRO_FEEDBACK_ROUTE_PROCESS_INVALID);
        }
        MesProRouteProcessDO routeProcess = routeProcessService.resolveFrozenRouteProcess(
                scheduleOrderProcess.getRouteProcessId(), scheduleOrder.getRouteId(),
                scheduleOrderProcess.getProcessId());
        if (!matchesScheduleSnapshotProcess(reqVO.getProcessId(), scheduleOrderProcess, routeProcess)) {
            throw exception(PRO_FEEDBACK_ROUTE_PROCESS_INVALID);
        }

        reqVO.setRouteId(routeProcess.getRouteId());
        reqVO.setProcessId(routeProcess.getProcessId());
        return new FeedbackRouteContext(routeProcess, scheduleOrder.getRouteId(), routeProcess.getProcessId());
    }

    private boolean matchesScheduleSnapshotProcess(Long providedProcessId,
                                                   MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                   MesProRouteProcessDO routeProcess) {
        return ObjUtil.equal(providedProcessId, scheduleOrderProcess.getProcessId())
                || ObjUtil.equal(providedProcessId, routeProcess.getProcessId());
    }

    private MesProRouteProcessDO resolveCurrentRouteProcess(MesProFeedbackDO feedback) {
        if (feedback.getScheduleOrderProcessId() != null) {
            MesProScheduleOrderProcessDO scheduleOrderProcess =
                    scheduleOrderProcessMapper.selectById(feedback.getScheduleOrderProcessId());
            if (scheduleOrderProcess == null) {
                throw exception(PRO_FEEDBACK_ROUTE_PROCESS_INVALID);
            }
            return routeProcessService.resolveFrozenRouteProcess(
                    scheduleOrderProcess.getRouteProcessId(), feedback.getRouteId(), feedback.getProcessId());
        }
        if (feedback.getScheduleOrderId() != null) {
            throw exception(PRO_FEEDBACK_ROUTE_PROCESS_INVALID);
        }
        MesProRouteProcessDO routeProcess =
                routeProcessService.resolveCurrentRouteProcess(null, feedback.getRouteId(), feedback.getProcessId());
        if (routeProcess == null) {
            throw exception(PRO_FEEDBACK_ROUTE_PROCESS_INVALID);
        }
        return routeProcess;
    }

    private void validateTaskRelation(MesProTaskDO task, MesMdWorkstationDO workstation,
                                      MesProWorkOrderDO workOrder, MesProFeedbackSaveReqVO reqVO,
                                      Long relationRouteId, Long relationProcessId) {
        if (ObjUtil.notEqual(task.getWorkOrderId(), workOrder.getId())) {
            throw exception(PRO_TASK_WORK_ORDER_MISMATCH);
        }
        if (task.getWorkstationId() != null && ObjUtil.notEqual(task.getWorkstationId(), workstation.getId())) {
            throw exception(PRO_TASK_WORKSTATION_MISMATCH);
        }
        if (ObjUtil.notEqual(task.getRouteId(), relationRouteId)
                || ObjUtil.notEqual(task.getProcessId(), relationProcessId)) {
            throw exception(PRO_TASK_ROUTE_PROCESS_MISMATCH);
        }
        if (ObjUtil.notEqual(task.getItemId(), reqVO.getItemId())) {
            throw exception(PRO_TASK_ITEM_MISMATCH);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProFeedbackWhenIpqcFinish(Long feedbackId, Long sourceLineId,
                                                BigDecimal qualifiedQty, BigDecimal unqualifiedQty,
                                                BigDecimal laborScrapQty, BigDecimal materialScrapQty, BigDecimal otherScrapQty) {
        // 1. 校验报工单存在且为待检验状态
        MesProFeedbackDO feedback = validateFeedbackExists(feedbackId);
        nonconformanceReviewService.ensureWorkOrderNotFrozen(feedback.getWorkOrderId(), "PQC提交");
        if (ObjUtil.notEqual(feedback.getStatus(), MesProFeedbackStatusEnum.UNCHECK.getStatus())) {
            throw exception(PRO_FEEDBACK_NOT_UNCHECK);
        }

        // 2. 拆分待检产出行（合格/不合格），生成明细，完成产出入库
        productProduceService.splitPendingAndFinishProduce(feedbackId, sourceLineId, qualifiedQty, unqualifiedQty);

        // 3. 回写合格/不合格/废品数量，更新状态为已完成
        feedbackMapper.updateById(new MesProFeedbackDO().setId(feedbackId)
                .setQualifiedQuantity(qualifiedQty)
                .setUnqualifiedQuantity(unqualifiedQty)
                .setUncheckQuantity(BigDecimal.ZERO)
                .setLaborScrapQuantity(laborScrapQty)
                .setMaterialScrapQuantity(materialScrapQty)
                .setOtherScrapQuantity(otherScrapQty)
                .setStatus(MesProFeedbackStatusEnum.FINISHED.getStatus()));

        // 4. 更新任务/工单的已生产数量
        feedback.setQualifiedQuantity(qualifiedQty).setUnqualifiedQuantity(unqualifiedQty);
        updateTaskAndWorkOrderByFeedback(feedback);
        syncScheduleOrderProgressIfLinked(feedback);
    }

    private void fillScheduleOrderSnapshot(MesProFeedbackDO feedback, boolean keepProvidedScheduleSnapshot) {
        if (keepProvidedScheduleSnapshot
                && feedback.getScheduleOrderId() != null
                && feedback.getScheduleOrderProcessId() != null) {
            MesProScheduleOrderProcessDO scheduleOrderProcess = scheduleOrderProcessMapper
                    .selectById(feedback.getScheduleOrderProcessId());
            feedbackScheduleLinkageGuard.validateProvidedSnapshotRemaining(feedback, scheduleOrderProcess);
            return;
        }
        MesProScheduleOrderDO scheduleOrder = scheduleOrderMapper.selectEffectiveByWorkOrderId(feedback.getWorkOrderId());
        if (scheduleOrder == null) {
            return;
        }
        MesProScheduleOrderProcessDO scheduleOrderProcess = scheduleOrderProcessMapper
                .selectListByScheduleOrderId(scheduleOrder.getId()).stream()
                .filter(process -> ObjUtil.equal(process.getProcessId(), feedback.getProcessId()))
                .findFirst()
                .orElseThrow(() -> exception(PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_NOT_EXISTS));
        feedback.setScheduleOrderId(scheduleOrder.getId());
        feedback.setScheduleOrderProcessId(scheduleOrderProcess.getId());
        feedbackScheduleLinkageGuard.validateProvidedSnapshotRemaining(feedback, scheduleOrderProcess);
    }

    private void syncScheduleOrderProgressIfLinked(MesProFeedbackDO feedback) {
        if (feedback.getScheduleOrderId() == null) {
            return;
        }
        scheduleOrderService.syncFeedbackProgress(feedback.getScheduleOrderId());
    }

    private record FeedbackRouteContext(MesProRouteProcessDO routeProcess,
                                        Long relationRouteId,
                                        Long relationProcessId) {
    }

}
