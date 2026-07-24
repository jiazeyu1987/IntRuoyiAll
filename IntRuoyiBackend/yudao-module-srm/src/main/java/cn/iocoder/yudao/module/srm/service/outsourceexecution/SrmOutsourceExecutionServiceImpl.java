package cn.iocoder.yudao.module.srm.service.outsourceexecution;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution.vo.*;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmProcurementPlanRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo.SrmSupplierPortalApplicationRespVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.outsourceexecution.SrmOutsourceExecutionDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.outsourceexecution.SrmOutsourceExecutionEventDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.outsourceexecution.SrmReconciliationDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.purchaseorder.SrmPurchaseOrderDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.purchaseorder.SrmPurchaseOrderLineDO;
import cn.iocoder.yudao.module.srm.dal.mysql.outsourceexecution.SrmOutsourceExecutionEventMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.outsourceexecution.SrmOutsourceExecutionMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.outsourceexecution.SrmReconciliationMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.purchaseorder.SrmPurchaseOrderLineMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.purchaseorder.SrmPurchaseOrderMapper;
import cn.iocoder.yudao.module.srm.enums.coderule.SrmCodeRuleTargetFormEnum;
import cn.iocoder.yudao.module.srm.enums.outsource.SrmOutsourceEventTypeEnum;
import cn.iocoder.yudao.module.srm.enums.outsource.SrmOutsourceExecutionStatusEnum;
import cn.iocoder.yudao.module.srm.enums.outsource.SrmReconciliationStatusEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmPurchaseOrderStatusEnum;
import cn.iocoder.yudao.module.srm.service.coderule.SrmCodeRuleService;
import cn.iocoder.yudao.module.srm.service.procurement.SrmProcurementPlanService;
import cn.iocoder.yudao.module.srm.service.supplier.SrmSupplierPortalApplicationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.*;

@Service
@Validated
public class SrmOutsourceExecutionServiceImpl implements SrmOutsourceExecutionService {

    private static final String SIMULATION_SOURCE = "LOCAL_SIMULATED";
    private static final String SIMULATION_LABEL = "测试租户受控模拟链路";

    @Resource
    private SrmCodeRuleService codeRuleService;
    @Resource
    private SrmProcurementPlanService procurementPlanService;
    @Resource
    private SrmPurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private SrmPurchaseOrderLineMapper purchaseOrderLineMapper;
    @Resource
    private SrmOutsourceExecutionMapper outsourceExecutionMapper;
    @Resource
    private SrmOutsourceExecutionEventMapper outsourceExecutionEventMapper;
    @Resource
    private SrmReconciliationMapper reconciliationMapper;
    @Resource
    private SrmSupplierPortalApplicationService supplierPortalApplicationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFromPurchaseOrder(SrmOutsourceExecutionCreateReqVO reqVO) {
        SrmPurchaseOrderDO purchaseOrder = validatePurchaseOrder(reqVO.getPurchaseOrderId());
        if (!Objects.equals(purchaseOrder.getOrderStatus(), SrmPurchaseOrderStatusEnum.CONFIRMED.getStatus())) {
            throw exception(OUTSOURCE_EXECUTION_SOURCE_ORDER_NOT_CONFIRMED);
        }
        if (outsourceExecutionMapper.selectBySourcePurchaseOrderId(getRequiredTenantId(), purchaseOrder.getId()) != null) {
            throw exception(OUTSOURCE_EXECUTION_DUPLICATE);
        }
        List<SrmPurchaseOrderLineDO> orderLines = purchaseOrderLineMapper.selectListByOrderId(purchaseOrder.getId());
        BigDecimal plannedQuantity = orderLines.stream()
                .map(item -> item.getConfirmedQuantity() != null ? item.getConfirmedQuantity() : item.getRequestedQuantity())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (plannedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(OUTSOURCE_EXECUTION_QUANTITY_INVALID);
        }
        SrmProcurementPlanRespVO plan = procurementPlanService.getProcurementPlan(purchaseOrder.getSourcePlanId());
        if (plan.getExpectedAmount() == null || plan.getExpectedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(OUTSOURCE_EXECUTION_RECONCILIATION_PREREQUISITE_MISSING, "来源采购计划缺少预计金额");
        }
        BigDecimal unitPrice = plan.getExpectedAmount().divide(plannedQuantity, 6, RoundingMode.HALF_UP);
        SrmOutsourceExecutionDO execution = SrmOutsourceExecutionDO.builder()
                .executionNo(codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.OUTSOURCE_EXECUTION.getTargetForm()))
                .sourcePurchaseOrderId(purchaseOrder.getId())
                .sourcePurchaseOrderNo(purchaseOrder.getOrderNo())
                .sourcePlanId(purchaseOrder.getSourcePlanId())
                .sourcePlanNo(purchaseOrder.getSourcePlanNo())
                .supplierId(purchaseOrder.getSupplierId())
                .supplierName(purchaseOrder.getSupplierName())
                .executionStatus(SrmOutsourceExecutionStatusEnum.PENDING_ISSUE.getStatus())
                .simulationSource(SIMULATION_SOURCE)
                .simulationLabel(SIMULATION_LABEL)
                .simulationRemark(reqVO.getSimulationRemark())
                .plannedQuantity(plannedQuantity)
                .unitPrice(unitPrice)
                .build();
        execution.setTenantId(getRequiredTenantId());
        outsourceExecutionMapper.insert(execution);
        recordEvent(execution, SrmOutsourceEventTypeEnum.CREATE, null,
                execution.getExecutionStatus(), getRequiredLoginUserId(), getRequiredLoginUserNickname(),
                StrUtil.blankToDefault(reqVO.getSimulationRemark(), SIMULATION_LABEL),
                "{\"simulationSource\":\"LOCAL_SIMULATED\"}");
        return execution.getId();
    }

    @Override
    public SrmOutsourceExecutionRespVO getOutsourceExecution(Long id) {
        return buildResp(validateExecution(id));
    }

    @Override
    public SrmOutsourceExecutionRespVO getMyOutsourceExecution(Long id) {
        SrmOutsourceExecutionDO execution = validateExecution(id);
        if (!Objects.equals(execution.getSupplierId(), getRequiredCurrentSupplierId())) {
            throw exception(OUTSOURCE_EXECUTION_SUPPLIER_FORBIDDEN);
        }
        return buildResp(execution);
    }

    @Override
    public PageResult<SrmOutsourceExecutionRespVO> getOutsourceExecutionPage(SrmOutsourceExecutionPageReqVO reqVO) {
        PageResult<SrmOutsourceExecutionDO> pageResult = outsourceExecutionMapper.selectPage(reqVO);
        return new PageResult<>(pageResult.getList().stream().map(this::buildResp).toList(), pageResult.getTotal());
    }

    @Override
    public PageResult<SrmOutsourceExecutionRespVO> getMyOutsourceExecutionPage(SrmOutsourceExecutionPageReqVO reqVO) {
        PageResult<SrmOutsourceExecutionDO> pageResult = outsourceExecutionMapper.selectMyPage(
                getRequiredTenantId(), getRequiredCurrentSupplierId(), reqVO);
        return new PageResult<>(pageResult.getList().stream().map(this::buildResp).toList(), pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issue(SrmOutsourceExecutionIssueReqVO reqVO) {
        SrmOutsourceExecutionDO execution = validateExecution(reqVO.getId());
        if (!Objects.equals(execution.getExecutionStatus(), SrmOutsourceExecutionStatusEnum.PENDING_ISSUE.getStatus())) {
            throw exception(OUTSOURCE_EXECUTION_STATUS_INVALID,
                    SrmOutsourceExecutionStatusEnum.getLabel(execution.getExecutionStatus()));
        }
        validatePositiveQuantity(reqVO.getIssueQuantity());
        if (reqVO.getIssueQuantity().compareTo(execution.getPlannedQuantity()) > 0) {
            throw exception(OUTSOURCE_EXECUTION_QUANTITY_INVALID);
        }
        String beforeStatus = execution.getExecutionStatus();
        execution.setIssueNoticeNo(execution.getExecutionNo() + "-ISSUE");
        execution.setIssueQuantity(reqVO.getIssueQuantity());
        execution.setIssuedBy(getRequiredLoginUserId());
        execution.setIssuedName(getRequiredLoginUserNickname());
        execution.setIssuedTime(LocalDateTime.now());
        execution.setExecutionStatus(SrmOutsourceExecutionStatusEnum.IN_PRODUCTION.getStatus());
        outsourceExecutionMapper.updateById(execution);
        recordEvent(execution, SrmOutsourceEventTypeEnum.ISSUE, beforeStatus,
                execution.getExecutionStatus(), getRequiredLoginUserId(), getRequiredLoginUserNickname(),
                reqVO.getIssueRemark(), "{\"issueQuantity\":\"" + reqVO.getIssueQuantity() + "\"}");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProgress(SrmOutsourceExecutionProgressReqVO reqVO) {
        SrmOutsourceExecutionDO execution = validateSupplierExecution(reqVO.getId());
        if (!Objects.equals(execution.getExecutionStatus(), SrmOutsourceExecutionStatusEnum.IN_PRODUCTION.getStatus())) {
            throw exception(OUTSOURCE_EXECUTION_STATUS_INVALID,
                    SrmOutsourceExecutionStatusEnum.getLabel(execution.getExecutionStatus()));
        }
        if (reqVO.getProgressPercent().compareTo(BigDecimal.ZERO) < 0
                || reqVO.getProgressPercent().compareTo(new BigDecimal("100")) > 0
                || StrUtil.isBlank(reqVO.getProgressStage())) {
            throw exception(OUTSOURCE_EXECUTION_PROGRESS_INVALID);
        }
        execution.setProgressPercent(reqVO.getProgressPercent());
        execution.setProgressStage(reqVO.getProgressStage());
        outsourceExecutionMapper.updateById(execution);
        recordEvent(execution, SrmOutsourceEventTypeEnum.PROGRESS, execution.getExecutionStatus(),
                execution.getExecutionStatus(), getRequiredLoginUserId(), getRequiredLoginUserNickname(),
                reqVO.getProgressRemark(),
                "{\"progressPercent\":\"" + reqVO.getProgressPercent() + "\",\"progressStage\":\"" + reqVO.getProgressStage() + "\"}");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(SrmOutsourceExecutionReceiveReqVO reqVO) {
        SrmOutsourceExecutionDO execution = validateSupplierExecution(reqVO.getId());
        if (!Objects.equals(execution.getExecutionStatus(), SrmOutsourceExecutionStatusEnum.IN_PRODUCTION.getStatus())) {
            throw exception(OUTSOURCE_EXECUTION_STATUS_INVALID,
                    SrmOutsourceExecutionStatusEnum.getLabel(execution.getExecutionStatus()));
        }
        validatePositiveQuantity(reqVO.getReceivedQuantity());
        BigDecimal upperLimit = execution.getIssueQuantity() != null ? execution.getIssueQuantity() : execution.getPlannedQuantity();
        if (reqVO.getReceivedQuantity().compareTo(upperLimit) > 0) {
            throw exception(OUTSOURCE_EXECUTION_QUANTITY_INVALID);
        }
        String beforeStatus = execution.getExecutionStatus();
        execution.setReceivedQuantity(reqVO.getReceivedQuantity());
        execution.setDeliveredBy(getRequiredLoginUserId());
        execution.setDeliveredName(getRequiredLoginUserNickname());
        execution.setDeliveredTime(LocalDateTime.now());
        execution.setExecutionStatus(SrmOutsourceExecutionStatusEnum.DELIVERED.getStatus());
        outsourceExecutionMapper.updateById(execution);
        recordEvent(execution, SrmOutsourceEventTypeEnum.RECEIVE, beforeStatus,
                execution.getExecutionStatus(), getRequiredLoginUserId(), getRequiredLoginUserNickname(),
                reqVO.getReceiveRemark(), "{\"receivedQuantity\":\"" + reqVO.getReceivedQuantity() + "\"}");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inspect(SrmOutsourceExecutionInspectReqVO reqVO) {
        SrmOutsourceExecutionDO execution = validateExecution(reqVO.getId());
        if (!Objects.equals(execution.getExecutionStatus(), SrmOutsourceExecutionStatusEnum.DELIVERED.getStatus())) {
            throw exception(OUTSOURCE_EXECUTION_STATUS_INVALID,
                    SrmOutsourceExecutionStatusEnum.getLabel(execution.getExecutionStatus()));
        }
        validatePositiveQuantity(reqVO.getQualifiedQuantity());
        if (execution.getReceivedQuantity() == null || reqVO.getQualifiedQuantity().compareTo(execution.getReceivedQuantity()) > 0) {
            throw exception(OUTSOURCE_EXECUTION_INSPECT_INVALID);
        }
        String beforeStatus = execution.getExecutionStatus();
        execution.setQualifiedQuantity(reqVO.getQualifiedQuantity());
        execution.setInspectedBy(getRequiredLoginUserId());
        execution.setInspectedName(getRequiredLoginUserNickname());
        execution.setInspectedTime(LocalDateTime.now());
        execution.setExecutionStatus(SrmOutsourceExecutionStatusEnum.INSPECTED.getStatus());
        outsourceExecutionMapper.updateById(execution);
        recordEvent(execution, SrmOutsourceEventTypeEnum.INSPECT, beforeStatus,
                execution.getExecutionStatus(), getRequiredLoginUserId(), getRequiredLoginUserNickname(),
                reqVO.getInspectRemark(), "{\"qualifiedQuantity\":\"" + reqVO.getQualifiedQuantity() + "\"}");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reconcile(SrmOutsourceExecutionReconcileReqVO reqVO) {
        SrmOutsourceExecutionDO execution = validateExecution(reqVO.getId());
        if (!Objects.equals(execution.getExecutionStatus(), SrmOutsourceExecutionStatusEnum.INSPECTED.getStatus())) {
            throw exception(OUTSOURCE_EXECUTION_STATUS_INVALID,
                    SrmOutsourceExecutionStatusEnum.getLabel(execution.getExecutionStatus()));
        }
        if (execution.getReceivedQuantity() == null) {
            throw exception(OUTSOURCE_EXECUTION_RECONCILIATION_PREREQUISITE_MISSING, "缺少收货数量");
        }
        if (execution.getQualifiedQuantity() == null) {
            throw exception(OUTSOURCE_EXECUTION_RECONCILIATION_PREREQUISITE_MISSING, "缺少检验合格数量");
        }
        if (execution.getUnitPrice() == null || execution.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(OUTSOURCE_EXECUTION_RECONCILIATION_PREREQUISITE_MISSING, "缺少单价");
        }
        BigDecimal diffQuantity = execution.getReceivedQuantity().subtract(execution.getQualifiedQuantity());
        if (diffQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw exception(OUTSOURCE_EXECUTION_INSPECT_INVALID);
        }
        BigDecimal reconciliationAmount = execution.getQualifiedQuantity()
                .multiply(execution.getUnitPrice())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal diffAmount = diffQuantity.multiply(execution.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
        SrmReconciliationDO reconciliation = reconciliationMapper.selectByExecutionId(execution.getId());
        if (reconciliation == null) {
            reconciliation = SrmReconciliationDO.builder()
                    .id(execution.getId())
                    .reconciliationNo(codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.OUTSOURCE_RECONCILIATION.getTargetForm()))
                    .executionId(execution.getId())
                    .executionNo(execution.getExecutionNo())
                    .sourcePurchaseOrderId(execution.getSourcePurchaseOrderId())
                    .sourcePurchaseOrderNo(execution.getSourcePurchaseOrderNo())
                    .supplierId(execution.getSupplierId())
                    .supplierName(execution.getSupplierName())
                    .build();
            reconciliation.setTenantId(getRequiredTenantId());
        }
        reconciliation.setReconciliationStatus(SrmReconciliationStatusEnum.RECONCILED.getStatus());
        reconciliation.setSimulationSource(execution.getSimulationSource());
        reconciliation.setSimulationLabel(execution.getSimulationLabel());
        reconciliation.setUnitPrice(execution.getUnitPrice());
        reconciliation.setReceivedQuantity(execution.getReceivedQuantity());
        reconciliation.setQualifiedQuantity(execution.getQualifiedQuantity());
        reconciliation.setDiffQuantity(diffQuantity);
        reconciliation.setReconciliationAmount(reconciliationAmount);
        reconciliation.setDiffAmount(diffAmount);
        reconciliation.setConfirmedBy(getRequiredLoginUserId());
        reconciliation.setConfirmedName(getRequiredLoginUserNickname());
        reconciliation.setConfirmedTime(LocalDateTime.now());
        reconciliation.setConfirmRemark(reqVO.getConfirmRemark());
        if (reconciliationMapper.selectById(reconciliation.getId()) == null) {
            reconciliationMapper.insert(reconciliation);
        } else {
            reconciliationMapper.updateById(reconciliation);
        }
        String beforeStatus = execution.getExecutionStatus();
        execution.setExecutionStatus(SrmOutsourceExecutionStatusEnum.RECONCILED.getStatus());
        outsourceExecutionMapper.updateById(execution);
        recordEvent(execution, SrmOutsourceEventTypeEnum.RECONCILE, beforeStatus,
                execution.getExecutionStatus(), getRequiredLoginUserId(), getRequiredLoginUserNickname(),
                reqVO.getConfirmRemark(),
                "{\"reconciliationAmount\":\"" + reconciliationAmount + "\",\"diffAmount\":\"" + diffAmount + "\"}");
    }

    private SrmOutsourceExecutionRespVO buildResp(SrmOutsourceExecutionDO execution) {
        SrmOutsourceExecutionRespVO respVO = new SrmOutsourceExecutionRespVO();
        respVO.setId(execution.getId());
        respVO.setExecutionNo(execution.getExecutionNo());
        respVO.setSourcePurchaseOrderId(execution.getSourcePurchaseOrderId());
        respVO.setSourcePurchaseOrderNo(execution.getSourcePurchaseOrderNo());
        respVO.setSourcePlanId(execution.getSourcePlanId());
        respVO.setSourcePlanNo(execution.getSourcePlanNo());
        respVO.setSupplierId(execution.getSupplierId());
        respVO.setSupplierName(execution.getSupplierName());
        respVO.setExecutionStatus(execution.getExecutionStatus());
        respVO.setExecutionStatusLabel(SrmOutsourceExecutionStatusEnum.getLabel(execution.getExecutionStatus()));
        respVO.setSimulationSource(execution.getSimulationSource());
        respVO.setSimulationLabel(execution.getSimulationLabel());
        respVO.setSimulationRemark(execution.getSimulationRemark());
        respVO.setPlannedQuantity(execution.getPlannedQuantity());
        respVO.setIssueNoticeNo(execution.getIssueNoticeNo());
        respVO.setIssueQuantity(execution.getIssueQuantity());
        respVO.setProgressPercent(execution.getProgressPercent());
        respVO.setProgressStage(execution.getProgressStage());
        respVO.setReceivedQuantity(execution.getReceivedQuantity());
        respVO.setQualifiedQuantity(execution.getQualifiedQuantity());
        respVO.setUnitPrice(execution.getUnitPrice());
        respVO.setIssuedTime(execution.getIssuedTime());
        respVO.setDeliveredTime(execution.getDeliveredTime());
        respVO.setInspectedTime(execution.getInspectedTime());
        respVO.setCreateTime(execution.getCreateTime());
        SrmReconciliationDO reconciliation = reconciliationMapper.selectByExecutionId(execution.getId());
        if (reconciliation != null) {
            SrmOutsourceExecutionRespVO.Reconciliation reconciliationResp = new SrmOutsourceExecutionRespVO.Reconciliation();
            reconciliationResp.setId(reconciliation.getId());
            reconciliationResp.setReconciliationNo(reconciliation.getReconciliationNo());
            reconciliationResp.setReconciliationStatus(reconciliation.getReconciliationStatus());
            reconciliationResp.setReconciliationStatusLabel(SrmReconciliationStatusEnum.getLabel(reconciliation.getReconciliationStatus()));
            reconciliationResp.setUnitPrice(reconciliation.getUnitPrice());
            reconciliationResp.setReceivedQuantity(reconciliation.getReceivedQuantity());
            reconciliationResp.setQualifiedQuantity(reconciliation.getQualifiedQuantity());
            reconciliationResp.setDiffQuantity(reconciliation.getDiffQuantity());
            reconciliationResp.setReconciliationAmount(reconciliation.getReconciliationAmount());
            reconciliationResp.setDiffAmount(reconciliation.getDiffAmount());
            reconciliationResp.setConfirmRemark(reconciliation.getConfirmRemark());
            reconciliationResp.setConfirmedTime(reconciliation.getConfirmedTime());
            respVO.setReconciliation(reconciliationResp);
        }
        respVO.setEvents(outsourceExecutionEventMapper.selectListByExecutionId(execution.getId()).stream().map(event -> {
            SrmOutsourceExecutionRespVO.Event eventResp = new SrmOutsourceExecutionRespVO.Event();
            eventResp.setId(event.getId());
            eventResp.setEventNo(event.getEventNo());
            eventResp.setEventType(event.getEventType());
            eventResp.setEventTypeLabel(SrmOutsourceEventTypeEnum.getLabel(event.getEventType()));
            eventResp.setBeforeStatus(event.getBeforeStatus());
            eventResp.setAfterStatus(event.getAfterStatus());
            eventResp.setOperatorName(event.getOperatorName());
            eventResp.setEventRemark(event.getEventRemark());
            eventResp.setEventPayload(event.getEventPayload());
            eventResp.setEventTime(event.getEventTime());
            return eventResp;
        }).toList());
        return respVO;
    }

    private void recordEvent(SrmOutsourceExecutionDO execution, SrmOutsourceEventTypeEnum eventType, String beforeStatus,
                             String afterStatus, Long operatorId, String operatorName, String remark, String payload) {
        SrmOutsourceExecutionEventDO event = SrmOutsourceExecutionEventDO.builder()
                .eventNo(codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.OUTSOURCE_EXECUTION_EVENT.getTargetForm()))
                .executionId(execution.getId())
                .eventType(eventType.getEventType())
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .simulationSource(execution.getSimulationSource())
                .operatorId(operatorId)
                .operatorName(operatorName)
                .eventRemark(remark)
                .eventPayload(payload)
                .eventTime(LocalDateTime.now())
                .build();
        event.setTenantId(getRequiredTenantId());
        outsourceExecutionEventMapper.insert(event);
    }

    private SrmPurchaseOrderDO validatePurchaseOrder(Long id) {
        SrmPurchaseOrderDO order = purchaseOrderMapper.selectById(id);
        if (order == null || !Objects.equals(order.getTenantId(), getRequiredTenantId())) {
            throw exception(PURCHASE_ORDER_NOT_EXISTS);
        }
        return order;
    }

    private SrmOutsourceExecutionDO validateExecution(Long id) {
        SrmOutsourceExecutionDO execution = outsourceExecutionMapper.selectById(id);
        if (execution == null || !Objects.equals(execution.getTenantId(), getRequiredTenantId())) {
            throw exception(OUTSOURCE_EXECUTION_NOT_EXISTS);
        }
        return execution;
    }

    private SrmOutsourceExecutionDO validateSupplierExecution(Long id) {
        SrmOutsourceExecutionDO execution = validateExecution(id);
        if (!Objects.equals(execution.getSupplierId(), getRequiredCurrentSupplierId())) {
            throw exception(OUTSOURCE_EXECUTION_SUPPLIER_FORBIDDEN);
        }
        return execution;
    }

    private void validatePositiveQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(OUTSOURCE_EXECUTION_QUANTITY_INVALID);
        }
    }

    private Long getRequiredCurrentSupplierId() {
        SrmSupplierPortalApplicationRespVO currentApplication = supplierPortalApplicationService.getCurrentApplication();
        if (currentApplication == null || currentApplication.getSupplierId() == null) {
            throw exception(OUTSOURCE_EXECUTION_SUPPLIER_CONTEXT_MISSING);
        }
        return currentApplication.getSupplierId();
    }

    private Long getRequiredLoginUserId() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw exception(OUTSOURCE_EXECUTION_SUPPLIER_CONTEXT_MISSING);
        }
        return userId;
    }

    private String getRequiredLoginUserNickname() {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        if (StrUtil.isBlank(nickname)) {
            throw exception(OUTSOURCE_EXECUTION_SUPPLIER_CONTEXT_MISSING);
        }
        return nickname;
    }

    private Long getRequiredTenantId() {
        return TenantContextHolder.getRequiredTenantId();
    }
}
