package cn.iocoder.yudao.module.srm.service.paymentexecution;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.srm.controller.admin.paymentexecution.vo.*;
import cn.iocoder.yudao.module.srm.dal.dataobject.contract.SrmProcurementContractDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.contract.SrmProcurementContractPaymentDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.outsourceexecution.SrmReconciliationDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.paymentexecution.SrmPaymentExecutionDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.paymentexecution.SrmPaymentExecutionEventDO;
import cn.iocoder.yudao.module.srm.dal.mysql.contract.SrmProcurementContractMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.contract.SrmProcurementContractPaymentMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.outsourceexecution.SrmReconciliationMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.paymentexecution.SrmPaymentExecutionEventMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.paymentexecution.SrmPaymentExecutionMapper;
import cn.iocoder.yudao.module.srm.enums.coderule.SrmCodeRuleTargetFormEnum;
import cn.iocoder.yudao.module.srm.enums.contract.SrmProcurementContractStatusEnum;
import cn.iocoder.yudao.module.srm.enums.outsource.SrmReconciliationStatusEnum;
import cn.iocoder.yudao.module.srm.enums.payment.SrmPaymentEventTypeEnum;
import cn.iocoder.yudao.module.srm.enums.payment.SrmPaymentExecutionStatusEnum;
import cn.iocoder.yudao.module.srm.service.coderule.SrmCodeRuleService;
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
public class SrmPaymentExecutionServiceImpl implements SrmPaymentExecutionService {

    @Resource
    private SrmCodeRuleService codeRuleService;
    @Resource
    private SrmReconciliationMapper reconciliationMapper;
    @Resource
    private SrmProcurementContractMapper contractMapper;
    @Resource
    private SrmProcurementContractPaymentMapper contractPaymentMapper;
    @Resource
    private SrmPaymentExecutionMapper paymentExecutionMapper;
    @Resource
    private SrmPaymentExecutionEventMapper paymentExecutionEventMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFromReconciliation(SrmPaymentExecutionCreateReqVO reqVO) {
        SrmReconciliationDO reconciliation = validateReconciliation(reqVO.getReconciliationId());
        if (!Objects.equals(reconciliation.getReconciliationStatus(), SrmReconciliationStatusEnum.RECONCILED.getStatus())) {
            throw exception(PAYMENT_EXECUTION_RECONCILIATION_REQUIRED);
        }
        if (paymentExecutionMapper.selectByReconciliationId(getRequiredTenantId(), reconciliation.getId()) != null) {
            throw exception(PAYMENT_EXECUTION_DUPLICATE);
        }
        SrmProcurementContractDO contract = validateContract(reqVO.getContractId());
        if (!Objects.equals(contract.getContractStatus(), SrmProcurementContractStatusEnum.EFFECTIVE.getStatus())) {
            throw exception(PROCUREMENT_CONTRACT_STATUS_INVALID,
                    SrmProcurementContractStatusEnum.getLabel(contract.getContractStatus()));
        }
        if (!Objects.equals(contract.getSupplierId(), reconciliation.getSupplierId())) {
            throw exception(PAYMENT_EXECUTION_CONTRACT_SUPPLIER_MISMATCH);
        }
        List<SrmProcurementContractPaymentDO> payments = contractPaymentMapper.selectListByContractId(contract.getId());
        if (payments.isEmpty()) {
            throw exception(PAYMENT_EXECUTION_CONTRACT_PAYMENT_REQUIRED);
        }
        SrmProcurementContractPaymentDO contractPayment = payments.get(0);
        BigDecimal applyAmount = reconciliation.getReconciliationAmount()
                .multiply(contractPayment.getPaymentRatio())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        SrmPaymentExecutionDO paymentExecution = SrmPaymentExecutionDO.builder()
                .paymentNo(codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.PAYMENT_EXECUTION.getTargetForm()))
                .reconciliationId(reconciliation.getId())
                .reconciliationNo(reconciliation.getReconciliationNo())
                .executionId(reconciliation.getExecutionId())
                .executionNo(reconciliation.getExecutionNo())
                .contractId(contract.getId())
                .contractNo(contract.getContractNo())
                .supplierId(reconciliation.getSupplierId())
                .supplierName(reconciliation.getSupplierName())
                .paymentStatus(SrmPaymentExecutionStatusEnum.DRAFT.getStatus())
                .simulationSource(reconciliation.getSimulationSource())
                .simulationLabel(reconciliation.getSimulationLabel())
                .paymentStage(contractPayment.getPaymentStage())
                .paymentRatio(contractPayment.getPaymentRatio())
                .dueDate(contractPayment.getDueDate())
                .paymentTermSummary(buildPaymentTermSummary(contract, contractPayment))
                .reconciliationAmount(reconciliation.getReconciliationAmount())
                .applyAmount(applyAmount)
                .paymentRemark(reqVO.getPaymentRemark())
                .build();
        paymentExecution.setTenantId(getRequiredTenantId());
        paymentExecutionMapper.insert(paymentExecution);
        recordEvent(paymentExecution, SrmPaymentEventTypeEnum.CREATE, null, paymentExecution.getPaymentStatus(),
                getRequiredLoginUserId(), getRequiredLoginUserNickname(),
                StrUtil.blankToDefault(reqVO.getPaymentRemark(), "根据委外对账单创建付款执行单"),
                "{\"contractNo\":\"" + contract.getContractNo() + "\"}");
        return paymentExecution.getId();
    }

    @Override
    public SrmPaymentExecutionRespVO getPaymentExecution(Long id) {
        return buildResp(validatePaymentExecution(id));
    }

    @Override
    public PageResult<SrmPaymentExecutionRespVO> getPaymentExecutionPage(SrmPaymentExecutionPageReqVO reqVO) {
        PageResult<SrmPaymentExecutionDO> pageResult = paymentExecutionMapper.selectPage(reqVO);
        return new PageResult<>(pageResult.getList().stream().map(this::buildResp).toList(), pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(SrmPaymentExecutionSubmitReqVO reqVO) {
        SrmPaymentExecutionDO paymentExecution = validatePaymentExecution(reqVO.getId());
        if (!Objects.equals(paymentExecution.getPaymentStatus(), SrmPaymentExecutionStatusEnum.DRAFT.getStatus())) {
            throw exception(PAYMENT_EXECUTION_STATUS_INVALID,
                    SrmPaymentExecutionStatusEnum.getLabel(paymentExecution.getPaymentStatus()));
        }
        String beforeStatus = paymentExecution.getPaymentStatus();
        paymentExecution.setPaymentStatus(SrmPaymentExecutionStatusEnum.PENDING_APPROVAL.getStatus());
        paymentExecution.setSubmittedBy(getRequiredLoginUserId());
        paymentExecution.setSubmittedName(getRequiredLoginUserNickname());
        paymentExecution.setSubmittedTime(LocalDateTime.now());
        paymentExecutionMapper.updateById(paymentExecution);
        recordEvent(paymentExecution, SrmPaymentEventTypeEnum.SUBMIT, beforeStatus, paymentExecution.getPaymentStatus(),
                getRequiredLoginUserId(), getRequiredLoginUserNickname(),
                reqVO.getSubmitRemark(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(SrmPaymentExecutionApproveReqVO reqVO) {
        SrmPaymentExecutionDO paymentExecution = validatePaymentExecution(reqVO.getId());
        if (!Objects.equals(paymentExecution.getPaymentStatus(), SrmPaymentExecutionStatusEnum.PENDING_APPROVAL.getStatus())) {
            throw exception(PAYMENT_EXECUTION_STATUS_INVALID,
                    SrmPaymentExecutionStatusEnum.getLabel(paymentExecution.getPaymentStatus()));
        }
        String beforeStatus = paymentExecution.getPaymentStatus();
        paymentExecution.setPaymentStatus(SrmPaymentExecutionStatusEnum.APPROVED.getStatus());
        paymentExecution.setApprovedBy(getRequiredLoginUserId());
        paymentExecution.setApprovedName(getRequiredLoginUserNickname());
        paymentExecution.setApprovedTime(LocalDateTime.now());
        paymentExecutionMapper.updateById(paymentExecution);
        recordEvent(paymentExecution, SrmPaymentEventTypeEnum.APPROVE, beforeStatus, paymentExecution.getPaymentStatus(),
                getRequiredLoginUserId(), getRequiredLoginUserNickname(),
                reqVO.getApproveRemark(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(SrmPaymentExecutionRejectReqVO reqVO) {
        SrmPaymentExecutionDO paymentExecution = validatePaymentExecution(reqVO.getId());
        if (!Objects.equals(paymentExecution.getPaymentStatus(), SrmPaymentExecutionStatusEnum.PENDING_APPROVAL.getStatus())) {
            throw exception(PAYMENT_EXECUTION_STATUS_INVALID,
                    SrmPaymentExecutionStatusEnum.getLabel(paymentExecution.getPaymentStatus()));
        }
        if (StrUtil.isBlank(reqVO.getRejectRemark())) {
            throw exception(PAYMENT_EXECUTION_REJECT_REMARK_REQUIRED);
        }
        String beforeStatus = paymentExecution.getPaymentStatus();
        paymentExecution.setPaymentStatus(SrmPaymentExecutionStatusEnum.REJECTED.getStatus());
        paymentExecution.setRejectedBy(getRequiredLoginUserId());
        paymentExecution.setRejectedName(getRequiredLoginUserNickname());
        paymentExecution.setRejectedTime(LocalDateTime.now());
        paymentExecution.setRejectRemark(reqVO.getRejectRemark());
        paymentExecutionMapper.updateById(paymentExecution);
        recordEvent(paymentExecution, SrmPaymentEventTypeEnum.REJECT, beforeStatus, paymentExecution.getPaymentStatus(),
                getRequiredLoginUserId(), getRequiredLoginUserNickname(),
                reqVO.getRejectRemark(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void financePush(SrmPaymentExecutionRejectReqVO reqVO) {
        SrmPaymentExecutionDO paymentExecution = validatePaymentExecution(reqVO.getId());
        if (!Objects.equals(paymentExecution.getPaymentStatus(), SrmPaymentExecutionStatusEnum.APPROVED.getStatus())
                && !Objects.equals(paymentExecution.getPaymentStatus(), SrmPaymentExecutionStatusEnum.PUSH_FAILED.getStatus())) {
            throw exception(PAYMENT_EXECUTION_STATUS_INVALID,
                    SrmPaymentExecutionStatusEnum.getLabel(paymentExecution.getPaymentStatus()));
        }
        if (StrUtil.isBlank(reqVO.getPushRemark())) {
            throw exception(PAYMENT_EXECUTION_PUSH_REMARK_REQUIRED);
        }
        String beforeStatus = paymentExecution.getPaymentStatus();
        SrmPaymentExecutionStatusEnum afterStatus = Boolean.TRUE.equals(reqVO.getPushSuccess())
                ? SrmPaymentExecutionStatusEnum.PUSH_SUCCESS
                : SrmPaymentExecutionStatusEnum.PUSH_FAILED;
        paymentExecution.setPaymentStatus(afterStatus.getStatus());
        paymentExecution.setPushedBy(getRequiredLoginUserId());
        paymentExecution.setPushedName(getRequiredLoginUserNickname());
        paymentExecution.setPushedTime(LocalDateTime.now());
        paymentExecution.setPushRemark(reqVO.getPushRemark());
        paymentExecutionMapper.updateById(paymentExecution);
        recordEvent(paymentExecution,
                Boolean.TRUE.equals(reqVO.getPushSuccess()) ? SrmPaymentEventTypeEnum.PUSH_SUCCESS : SrmPaymentEventTypeEnum.PUSH_FAILED,
                beforeStatus, paymentExecution.getPaymentStatus(), getRequiredLoginUserId(), getRequiredLoginUserNickname(),
                reqVO.getPushRemark(), "{\"pushSuccess\":\"" + Boolean.TRUE.equals(reqVO.getPushSuccess()) + "\"}");
    }

    private SrmPaymentExecutionRespVO buildResp(SrmPaymentExecutionDO paymentExecution) {
        SrmPaymentExecutionRespVO respVO = new SrmPaymentExecutionRespVO();
        respVO.setId(paymentExecution.getId());
        respVO.setPaymentNo(paymentExecution.getPaymentNo());
        respVO.setReconciliationId(paymentExecution.getReconciliationId());
        respVO.setReconciliationNo(paymentExecution.getReconciliationNo());
        respVO.setExecutionId(paymentExecution.getExecutionId());
        respVO.setExecutionNo(paymentExecution.getExecutionNo());
        respVO.setContractId(paymentExecution.getContractId());
        respVO.setContractNo(paymentExecution.getContractNo());
        respVO.setSupplierId(paymentExecution.getSupplierId());
        respVO.setSupplierName(paymentExecution.getSupplierName());
        respVO.setPaymentStatus(paymentExecution.getPaymentStatus());
        respVO.setPaymentStatusLabel(SrmPaymentExecutionStatusEnum.getLabel(paymentExecution.getPaymentStatus()));
        respVO.setSimulationSource(paymentExecution.getSimulationSource());
        respVO.setSimulationLabel(paymentExecution.getSimulationLabel());
        respVO.setPaymentStage(paymentExecution.getPaymentStage());
        respVO.setPaymentRatio(paymentExecution.getPaymentRatio());
        respVO.setDueDate(paymentExecution.getDueDate());
        respVO.setPaymentTermSummary(paymentExecution.getPaymentTermSummary());
        respVO.setReconciliationAmount(paymentExecution.getReconciliationAmount());
        respVO.setApplyAmount(paymentExecution.getApplyAmount());
        respVO.setPaymentRemark(paymentExecution.getPaymentRemark());
        respVO.setRejectRemark(paymentExecution.getRejectRemark());
        respVO.setPushRemark(paymentExecution.getPushRemark());
        respVO.setSubmittedTime(paymentExecution.getSubmittedTime());
        respVO.setApprovedTime(paymentExecution.getApprovedTime());
        respVO.setRejectedTime(paymentExecution.getRejectedTime());
        respVO.setPushedTime(paymentExecution.getPushedTime());
        respVO.setCreateTime(paymentExecution.getCreateTime());
        respVO.setEvents(paymentExecutionEventMapper.selectListByPaymentId(paymentExecution.getId()).stream().map(event -> {
            SrmPaymentExecutionRespVO.Event eventResp = new SrmPaymentExecutionRespVO.Event();
            eventResp.setId(event.getId());
            eventResp.setEventNo(event.getEventNo());
            eventResp.setEventType(event.getEventType());
            eventResp.setEventTypeLabel(SrmPaymentEventTypeEnum.getLabel(event.getEventType()));
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

    private void recordEvent(SrmPaymentExecutionDO paymentExecution, SrmPaymentEventTypeEnum eventType, String beforeStatus,
                             String afterStatus, Long operatorId, String operatorName, String remark, String payload) {
        SrmPaymentExecutionEventDO event = SrmPaymentExecutionEventDO.builder()
                .eventNo(codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.PAYMENT_EXECUTION_EVENT.getTargetForm()))
                .paymentId(paymentExecution.getId())
                .eventType(eventType.getEventType())
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .eventRemark(remark)
                .eventPayload(payload)
                .eventTime(LocalDateTime.now())
                .build();
        event.setTenantId(getRequiredTenantId());
        paymentExecutionEventMapper.insert(event);
    }

    private String buildPaymentTermSummary(SrmProcurementContractDO contract, SrmProcurementContractPaymentDO contractPayment) {
        String dueDate = contractPayment.getDueDate() != null ? contractPayment.getDueDate().toString() : "-";
        return String.format("%s | 比例 %s%% | 到期 %s | 来源合同 %s",
                contractPayment.getPaymentStage(), contractPayment.getPaymentRatio(), dueDate, contract.getContractNo());
    }

    private SrmReconciliationDO validateReconciliation(Long id) {
        SrmReconciliationDO reconciliation = reconciliationMapper.selectById(id);
        if (reconciliation == null || !Objects.equals(reconciliation.getTenantId(), getRequiredTenantId())) {
            throw exception(PAYMENT_EXECUTION_RECONCILIATION_REQUIRED);
        }
        return reconciliation;
    }

    private SrmProcurementContractDO validateContract(Long id) {
        SrmProcurementContractDO contract = contractMapper.selectById(id);
        if (contract == null || !Objects.equals(contract.getTenantId(), getRequiredTenantId())) {
            throw exception(PROCUREMENT_CONTRACT_NOT_EXISTS);
        }
        return contract;
    }

    private SrmPaymentExecutionDO validatePaymentExecution(Long id) {
        SrmPaymentExecutionDO paymentExecution = paymentExecutionMapper.selectById(id);
        if (paymentExecution == null || !Objects.equals(paymentExecution.getTenantId(), getRequiredTenantId())) {
            throw exception(PAYMENT_EXECUTION_NOT_EXISTS);
        }
        return paymentExecution;
    }

    private Long getRequiredLoginUserId() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw exception(SUPPLIER_LOGIN_CONTEXT_MISSING);
        }
        return userId;
    }

    private String getRequiredLoginUserNickname() {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        if (StrUtil.isBlank(nickname)) {
            throw exception(SUPPLIER_LOGIN_CONTEXT_MISSING);
        }
        return nickname;
    }

    private Long getRequiredTenantId() {
        return TenantContextHolder.getRequiredTenantId();
    }
}
