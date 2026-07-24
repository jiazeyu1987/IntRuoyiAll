package cn.iocoder.yudao.module.srm.service.framework;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.srm.controller.admin.framework.vo.*;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmProcurementPlanRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo.SrmSupplierEligibilityRespVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.framework.*;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.SrmProcurementApprovalRecordDO;
import cn.iocoder.yudao.module.srm.dal.mysql.framework.*;
import cn.iocoder.yudao.module.srm.dal.mysql.procurement.SrmProcurementApprovalRecordMapper;
import cn.iocoder.yudao.module.srm.enums.coderule.SrmCodeRuleTargetFormEnum;
import cn.iocoder.yudao.module.srm.enums.framework.SrmFrameworkAgreementStatusEnum;
import cn.iocoder.yudao.module.srm.enums.framework.SrmFrameworkPlanStatusEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementApprovalActionEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementMethodEnum;
import cn.iocoder.yudao.module.srm.service.coderule.SrmCodeRuleService;
import cn.iocoder.yudao.module.srm.service.procurement.SrmProcurementPlanServiceImpl;
import cn.iocoder.yudao.module.srm.service.supplier.SrmSupplierAccessRiskService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.*;

@Service
@Validated
public class SrmFrameworkAgreementServiceImpl implements SrmFrameworkAgreementService {

    public static final String BIZ_TYPE_FRAMEWORK_PLAN = "FRAMEWORK_PLAN";

    @Resource
    private SrmCodeRuleService codeRuleService;
    @Resource
    private SrmSupplierAccessRiskService supplierAccessRiskService;
    @Resource
    private SrmFrameworkPlanMapper frameworkPlanMapper;
    @Resource
    private SrmFrameworkPlanLineMapper frameworkPlanLineMapper;
    @Resource
    private SrmFrameworkAgreementMapper frameworkAgreementMapper;
    @Resource
    private SrmFrameworkAgreementLineMapper frameworkAgreementLineMapper;
    @Resource
    private SrmProcurementApprovalRecordMapper approvalRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFrameworkPlan(SrmFrameworkPlanSaveReqVO createReqVO) {
        validateProcurementMethod(createReqVO.getProcurementMethod());
        validateFrameworkBudget(createReqVO.getBudgetAmount());
        validateFrameworkLines(createReqVO.getLines());
        validateFrameworkDate(createReqVO);
        SrmSupplierEligibilityRespVO eligibility = supplierAccessRiskService.checkSupplierEligibility(createReqVO.getSupplierId());
        if (!Boolean.TRUE.equals(eligibility.getEligible())) {
            throw exception(SUPPLIER_ELIGIBILITY_BLOCKED, eligibility.getBlockedReason());
        }
        SrmFrameworkPlanDO plan = SrmFrameworkPlanDO.builder()
                .frameworkPlanNo(codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.FRAMEWORK_PLAN.getTargetForm()))
                .planTitle(createReqVO.getPlanTitle())
                .supplierId(createReqVO.getSupplierId())
                .supplierName(eligibility.getSupplierName())
                .procurementMethod(createReqVO.getProcurementMethod())
                .budgetAmount(createReqVO.getBudgetAmount())
                .validStartDate(createReqVO.getValidStartDate())
                .validEndDate(createReqVO.getValidEndDate())
                .planStatus(SrmFrameworkPlanStatusEnum.DRAFT.getStatus())
                .remark(createReqVO.getRemark())
                .build();
        plan.setTenantId(getRequiredTenantId());
        frameworkPlanMapper.insert(plan);
        for (SrmFrameworkPlanSaveReqVO.Line reqLine : createReqVO.getLines()) {
            SrmFrameworkPlanLineDO line = SrmFrameworkPlanLineDO.builder()
                    .frameworkPlanId(plan.getId())
                    .materialId(reqLine.getMaterialId())
                    .materialCode(reqLine.getMaterialCode())
                    .materialName(reqLine.getMaterialName())
                    .quantity(reqLine.getQuantity())
                    .unit(reqLine.getUnit())
                    .budgetAmount(reqLine.getBudgetAmount())
                    .build();
            line.setTenantId(getRequiredTenantId());
            frameworkPlanLineMapper.insert(line);
        }
        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitFrameworkPlan(Long id) {
        SrmFrameworkPlanDO plan = validateFrameworkPlanExists(id);
        if (!SrmFrameworkPlanStatusEnum.DRAFT.getStatus().equals(plan.getPlanStatus())
                && !SrmFrameworkPlanStatusEnum.REJECTED.getStatus().equals(plan.getPlanStatus())) {
            throw exception(FRAMEWORK_PLAN_STATUS_INVALID, SrmFrameworkPlanStatusEnum.getLabel(plan.getPlanStatus()));
        }
        LocalDateTime now = LocalDateTime.now();
        Long userId = getRequiredLoginUserId();
        String nickname = getRequiredLoginUserNickname();
        plan.setPlanStatus(SrmFrameworkPlanStatusEnum.SUBMITTED.getStatus());
        plan.setSubmittedBy(userId);
        plan.setSubmittedName(nickname);
        plan.setSubmittedTime(now);
        frameworkPlanMapper.updateById(plan);
        insertApprovalRecord(BIZ_TYPE_FRAMEWORK_PLAN, id, SrmProcurementApprovalActionEnum.SUBMIT, userId, nickname, now, "提交框架计划");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveFrameworkPlan(SrmFrameworkPlanAuditReqVO auditReqVO) {
        SrmFrameworkPlanDO plan = validateSubmittedFrameworkPlan(auditReqVO.getId());
        LocalDateTime now = LocalDateTime.now();
        Long userId = getRequiredLoginUserId();
        String nickname = getRequiredLoginUserNickname();
        plan.setPlanStatus(SrmFrameworkPlanStatusEnum.APPROVED.getStatus());
        plan.setAuditBy(userId);
        plan.setAuditName(nickname);
        plan.setAuditTime(now);
        plan.setAuditRemark(auditReqVO.getAuditRemark());
        frameworkPlanMapper.updateById(plan);
        insertApprovalRecord(BIZ_TYPE_FRAMEWORK_PLAN, plan.getId(), SrmProcurementApprovalActionEnum.APPROVE, userId, nickname, now, auditReqVO.getAuditRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectFrameworkPlan(SrmFrameworkPlanAuditReqVO auditReqVO) {
        if (StrUtil.isBlank(auditReqVO.getAuditRemark())) {
            throw exception(FRAMEWORK_PLAN_AUDIT_REMARK_REQUIRED);
        }
        SrmFrameworkPlanDO plan = validateSubmittedFrameworkPlan(auditReqVO.getId());
        LocalDateTime now = LocalDateTime.now();
        Long userId = getRequiredLoginUserId();
        String nickname = getRequiredLoginUserNickname();
        plan.setPlanStatus(SrmFrameworkPlanStatusEnum.REJECTED.getStatus());
        plan.setAuditBy(userId);
        plan.setAuditName(nickname);
        plan.setAuditTime(now);
        plan.setAuditRemark(auditReqVO.getAuditRemark());
        frameworkPlanMapper.updateById(plan);
        insertApprovalRecord(BIZ_TYPE_FRAMEWORK_PLAN, plan.getId(), SrmProcurementApprovalActionEnum.REJECT, userId, nickname, now, auditReqVO.getAuditRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SrmFrameworkAgreementRespVO createAgreement(Long frameworkPlanId) {
        SrmFrameworkPlanDO plan = validateFrameworkPlanExists(frameworkPlanId);
        if (plan.getAgreementId() != null || frameworkAgreementMapper.selectByFrameworkPlanId(plan.getId()) != null) {
            throw exception(FRAMEWORK_AGREEMENT_DUPLICATE);
        }
        if (!SrmFrameworkPlanStatusEnum.APPROVED.getStatus().equals(plan.getPlanStatus())) {
            throw exception(FRAMEWORK_AGREEMENT_NOT_APPROVED);
        }
        SrmFrameworkAgreementDO agreement = SrmFrameworkAgreementDO.builder()
                .agreementNo(codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.FRAMEWORK_AGREEMENT.getTargetForm()))
                .frameworkPlanId(plan.getId())
                .frameworkPlanNo(plan.getFrameworkPlanNo())
                .supplierId(plan.getSupplierId())
                .supplierName(plan.getSupplierName())
                .procurementMethod(plan.getProcurementMethod())
                .budgetAmount(plan.getBudgetAmount())
                .validStartDate(plan.getValidStartDate())
                .validEndDate(plan.getValidEndDate())
                .agreementStatus(SrmFrameworkAgreementStatusEnum.EFFECTIVE.getStatus())
                .remark(plan.getRemark())
                .build();
        agreement.setTenantId(getRequiredTenantId());
        frameworkAgreementMapper.insert(agreement);
        for (SrmFrameworkPlanLineDO sourceLine : frameworkPlanLineMapper.selectListByFrameworkPlanId(plan.getId())) {
            SrmFrameworkAgreementLineDO targetLine = SrmFrameworkAgreementLineDO.builder()
                    .agreementId(agreement.getId())
                    .frameworkPlanId(plan.getId())
                    .frameworkPlanLineId(sourceLine.getId())
                    .materialId(sourceLine.getMaterialId())
                    .materialCode(sourceLine.getMaterialCode())
                    .materialName(sourceLine.getMaterialName())
                    .quantity(sourceLine.getQuantity())
                    .unit(sourceLine.getUnit())
                    .budgetAmount(sourceLine.getBudgetAmount())
                    .build();
            targetLine.setTenantId(getRequiredTenantId());
            frameworkAgreementLineMapper.insert(targetLine);
        }
        plan.setPlanStatus(SrmFrameworkPlanStatusEnum.AGREEMENT_CREATED.getStatus());
        plan.setAgreementId(agreement.getId());
        plan.setAgreementNo(agreement.getAgreementNo());
        plan.setAgreementTime(LocalDateTime.now());
        frameworkPlanMapper.updateById(plan);
        return buildAgreementResp(agreement);
    }

    @Override
    public SrmFrameworkPlanRespVO getFrameworkPlan(Long id) {
        return buildFrameworkPlanResp(validateFrameworkPlanExists(id));
    }

    @Override
    public PageResult<SrmFrameworkPlanRespVO> getFrameworkPlanPage(SrmFrameworkPlanPageReqVO pageReqVO) {
        PageResult<SrmFrameworkPlanDO> pageResult = frameworkPlanMapper.selectPage(pageReqVO);
        return new PageResult<>(pageResult.getList().stream()
                .map(this::buildFrameworkPlanResp)
                .collect(Collectors.toList()), pageResult.getTotal());
    }

    @Override
    public PageResult<SrmFrameworkAgreementRespVO> getAgreementPage(SrmFrameworkAgreementPageReqVO pageReqVO) {
        PageResult<SrmFrameworkAgreementDO> pageResult = frameworkAgreementMapper.selectPage(pageReqVO);
        return new PageResult<>(pageResult.getList().stream()
                .map(this::buildAgreementResp)
                .collect(Collectors.toList()), pageResult.getTotal());
    }

    private SrmFrameworkPlanDO validateFrameworkPlanExists(Long id) {
        SrmFrameworkPlanDO plan = frameworkPlanMapper.selectById(id);
        if (plan == null) {
            throw exception(FRAMEWORK_PLAN_NOT_EXISTS);
        }
        return plan;
    }

    private SrmFrameworkPlanDO validateSubmittedFrameworkPlan(Long id) {
        SrmFrameworkPlanDO plan = validateFrameworkPlanExists(id);
        if (!SrmFrameworkPlanStatusEnum.SUBMITTED.getStatus().equals(plan.getPlanStatus())) {
            throw exception(FRAMEWORK_PLAN_STATUS_INVALID, SrmFrameworkPlanStatusEnum.getLabel(plan.getPlanStatus()));
        }
        return plan;
    }

    private void validateProcurementMethod(String method) {
        if (!SrmProcurementMethodEnum.contains(method)) {
            throw exception(PROCUREMENT_METHOD_INVALID, method);
        }
    }

    private void validateFrameworkBudget(BigDecimal budgetAmount) {
        if (budgetAmount == null || budgetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(FRAMEWORK_PLAN_BUDGET_INVALID);
        }
    }

    private void validateFrameworkLines(List<SrmFrameworkPlanSaveReqVO.Line> lines) {
        if (lines == null || lines.isEmpty()) {
            throw exception(FRAMEWORK_PLAN_LINE_REQUIRED);
        }
        for (SrmFrameworkPlanSaveReqVO.Line line : lines) {
            if (line.getQuantity() == null || line.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(FRAMEWORK_PLAN_LINE_QUANTITY_INVALID);
            }
            if (line.getBudgetAmount() == null || line.getBudgetAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(FRAMEWORK_PLAN_LINE_BUDGET_INVALID);
            }
        }
    }

    private void validateFrameworkDate(SrmFrameworkPlanSaveReqVO reqVO) {
        if (reqVO.getValidEndDate().isBefore(reqVO.getValidStartDate())) {
            throw exception(FRAMEWORK_PLAN_DATE_INVALID);
        }
    }

    private SrmFrameworkPlanRespVO buildFrameworkPlanResp(SrmFrameworkPlanDO plan) {
        SrmFrameworkPlanRespVO respVO = new SrmFrameworkPlanRespVO();
        respVO.setId(plan.getId());
        respVO.setFrameworkPlanNo(plan.getFrameworkPlanNo());
        respVO.setPlanTitle(plan.getPlanTitle());
        respVO.setSupplierId(plan.getSupplierId());
        respVO.setSupplierName(plan.getSupplierName());
        respVO.setProcurementMethod(plan.getProcurementMethod());
        respVO.setProcurementMethodLabel(SrmProcurementMethodEnum.getLabel(plan.getProcurementMethod()));
        respVO.setBudgetAmount(plan.getBudgetAmount());
        respVO.setValidStartDate(plan.getValidStartDate());
        respVO.setValidEndDate(plan.getValidEndDate());
        respVO.setPlanStatus(plan.getPlanStatus());
        respVO.setPlanStatusLabel(SrmFrameworkPlanStatusEnum.getLabel(plan.getPlanStatus()));
        respVO.setRemark(plan.getRemark());
        respVO.setSubmittedName(plan.getSubmittedName());
        respVO.setSubmittedTime(plan.getSubmittedTime());
        respVO.setAuditName(plan.getAuditName());
        respVO.setAuditTime(plan.getAuditTime());
        respVO.setAuditRemark(plan.getAuditRemark());
        respVO.setAgreementId(plan.getAgreementId());
        respVO.setAgreementNo(plan.getAgreementNo());
        respVO.setAgreementTime(plan.getAgreementTime());
        respVO.setCreateTime(plan.getCreateTime());
        respVO.setLines(frameworkPlanLineMapper.selectListByFrameworkPlanId(plan.getId()).stream()
                .map(this::buildFrameworkPlanLineResp)
                .collect(Collectors.toList()));
        respVO.setApprovalRecords(approvalRecordMapper.selectListByBiz(BIZ_TYPE_FRAMEWORK_PLAN, plan.getId()).stream()
                .map(this::buildApprovalRecordResp)
                .collect(Collectors.toList()));
        return respVO;
    }

    private SrmFrameworkPlanRespVO.Line buildFrameworkPlanLineResp(SrmFrameworkPlanLineDO line) {
        SrmFrameworkPlanRespVO.Line respVO = new SrmFrameworkPlanRespVO.Line();
        respVO.setId(line.getId());
        respVO.setMaterialId(line.getMaterialId());
        respVO.setMaterialCode(line.getMaterialCode());
        respVO.setMaterialName(line.getMaterialName());
        respVO.setQuantity(line.getQuantity());
        respVO.setUnit(line.getUnit());
        respVO.setBudgetAmount(line.getBudgetAmount());
        return respVO;
    }

    private SrmFrameworkAgreementRespVO buildAgreementResp(SrmFrameworkAgreementDO agreement) {
        SrmFrameworkAgreementRespVO respVO = new SrmFrameworkAgreementRespVO();
        respVO.setId(agreement.getId());
        respVO.setAgreementNo(agreement.getAgreementNo());
        respVO.setFrameworkPlanId(agreement.getFrameworkPlanId());
        respVO.setFrameworkPlanNo(agreement.getFrameworkPlanNo());
        respVO.setSupplierId(agreement.getSupplierId());
        respVO.setSupplierName(agreement.getSupplierName());
        respVO.setProcurementMethod(agreement.getProcurementMethod());
        respVO.setProcurementMethodLabel(SrmProcurementMethodEnum.getLabel(agreement.getProcurementMethod()));
        respVO.setBudgetAmount(agreement.getBudgetAmount());
        respVO.setValidStartDate(agreement.getValidStartDate());
        respVO.setValidEndDate(agreement.getValidEndDate());
        respVO.setAgreementStatus(agreement.getAgreementStatus());
        respVO.setAgreementStatusLabel(SrmFrameworkAgreementStatusEnum.getLabel(agreement.getAgreementStatus()));
        respVO.setRemark(agreement.getRemark());
        respVO.setLines(frameworkAgreementLineMapper.selectListByAgreementId(agreement.getId()).stream()
                .map(this::buildAgreementLineResp)
                .collect(Collectors.toList()));
        return respVO;
    }

    private SrmFrameworkAgreementRespVO.Line buildAgreementLineResp(SrmFrameworkAgreementLineDO line) {
        SrmFrameworkAgreementRespVO.Line respVO = new SrmFrameworkAgreementRespVO.Line();
        respVO.setId(line.getId());
        respVO.setFrameworkPlanLineId(line.getFrameworkPlanLineId());
        respVO.setMaterialId(line.getMaterialId());
        respVO.setMaterialCode(line.getMaterialCode());
        respVO.setMaterialName(line.getMaterialName());
        respVO.setQuantity(line.getQuantity());
        respVO.setUnit(line.getUnit());
        respVO.setBudgetAmount(line.getBudgetAmount());
        return respVO;
    }

    private SrmProcurementPlanRespVO.ApprovalRecord buildApprovalRecordResp(SrmProcurementApprovalRecordDO record) {
        SrmProcurementPlanRespVO.ApprovalRecord respVO = new SrmProcurementPlanRespVO.ApprovalRecord();
        respVO.setId(record.getId());
        respVO.setAction(record.getAction());
        respVO.setActionLabel(record.getActionLabel());
        respVO.setOperatorName(record.getOperatorName());
        respVO.setOperationTime(record.getOperationTime());
        respVO.setRemark(record.getRemark());
        return respVO;
    }

    private void insertApprovalRecord(String bizType, Long bizId, SrmProcurementApprovalActionEnum action,
                                      Long userId, String nickname, LocalDateTime operationTime, String remark) {
        SrmProcurementApprovalRecordDO record = SrmProcurementApprovalRecordDO.builder()
                .bizType(bizType)
                .bizId(bizId)
                .action(action.getAction())
                .actionLabel(action.getLabel())
                .operatorId(userId)
                .operatorName(nickname)
                .operationTime(operationTime)
                .remark(remark)
                .build();
        record.setTenantId(getRequiredTenantId());
        approvalRecordMapper.insert(record);
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
