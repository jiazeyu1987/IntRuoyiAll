package cn.iocoder.yudao.module.srm.service.procurement;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.*;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.*;
import cn.iocoder.yudao.module.srm.dal.mysql.procurement.*;
import cn.iocoder.yudao.module.srm.enums.coderule.SrmCodeRuleTargetFormEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.*;
import cn.iocoder.yudao.module.srm.service.coderule.SrmCodeRuleService;
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
public class SrmProcurementPlanServiceImpl implements SrmProcurementPlanService {

    public static final String BIZ_TYPE_PROCUREMENT_PLAN = "PROCUREMENT_PLAN";

    @Resource
    private SrmCodeRuleService codeRuleService;
    @Resource
    private SrmProcurementPlanMapper procurementPlanMapper;
    @Resource
    private SrmProcurementPlanLineMapper procurementPlanLineMapper;
    @Resource
    private SrmProcurementApprovalRecordMapper approvalRecordMapper;
    @Resource
    private SrmSourcingProjectMapper sourcingProjectMapper;
    @Resource
    private SrmSourcingProjectLineMapper sourcingProjectLineMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProcurementPlan(SrmProcurementPlanSaveReqVO createReqVO) {
        validateProcurementMethod(createReqVO.getProcurementMethod());
        validateExpectedAmount(createReqVO.getExpectedAmount());
        validatePlanLines(createReqVO.getLines());
        SrmProcurementPlanDO plan = SrmProcurementPlanDO.builder()
                .planNo(codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm()))
                .planTitle(createReqVO.getPlanTitle())
                .procurementMethod(createReqVO.getProcurementMethod())
                .expectedAmount(createReqVO.getExpectedAmount())
                .planStatus(SrmProcurementPlanStatusEnum.DRAFT.getStatus())
                .remark(createReqVO.getRemark())
                .build();
        plan.setTenantId(getRequiredTenantId());
        procurementPlanMapper.insert(plan);
        for (SrmProcurementPlanSaveReqVO.Line reqLine : createReqVO.getLines()) {
            SrmProcurementPlanLineDO line = SrmProcurementPlanLineDO.builder()
                    .planId(plan.getId())
                    .lineNo(codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN_LINE.getTargetForm()))
                    .materialId(reqLine.getMaterialId())
                    .materialCode(reqLine.getMaterialCode())
                    .materialName(reqLine.getMaterialName())
                    .quantity(reqLine.getQuantity())
                    .unit(reqLine.getUnit())
                    .requiredDate(reqLine.getRequiredDate())
                    .build();
            line.setTenantId(getRequiredTenantId());
            procurementPlanLineMapper.insert(line);
        }
        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitProcurementPlan(Long id) {
        SrmProcurementPlanDO plan = validateProcurementPlanExists(id);
        if (!SrmProcurementPlanStatusEnum.DRAFT.getStatus().equals(plan.getPlanStatus())
                && !SrmProcurementPlanStatusEnum.REJECTED.getStatus().equals(plan.getPlanStatus())) {
            throw exception(PROCUREMENT_PLAN_STATUS_INVALID, SrmProcurementPlanStatusEnum.getLabel(plan.getPlanStatus()));
        }
        LocalDateTime now = LocalDateTime.now();
        Long userId = getRequiredLoginUserId();
        String nickname = getRequiredLoginUserNickname();
        plan.setPlanStatus(SrmProcurementPlanStatusEnum.SUBMITTED.getStatus());
        plan.setSubmittedBy(userId);
        plan.setSubmittedName(nickname);
        plan.setSubmittedTime(now);
        procurementPlanMapper.updateById(plan);
        insertApprovalRecord(BIZ_TYPE_PROCUREMENT_PLAN, id, SrmProcurementApprovalActionEnum.SUBMIT, userId, nickname, now, "提交采购计划");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveProcurementPlan(SrmProcurementPlanAuditReqVO auditReqVO) {
        SrmProcurementPlanDO plan = validateSubmittedPlan(auditReqVO.getId());
        LocalDateTime now = LocalDateTime.now();
        Long userId = getRequiredLoginUserId();
        String nickname = getRequiredLoginUserNickname();
        plan.setPlanStatus(SrmProcurementPlanStatusEnum.APPROVED.getStatus());
        plan.setAuditBy(userId);
        plan.setAuditName(nickname);
        plan.setAuditTime(now);
        plan.setAuditRemark(auditReqVO.getAuditRemark());
        procurementPlanMapper.updateById(plan);
        insertApprovalRecord(BIZ_TYPE_PROCUREMENT_PLAN, plan.getId(), SrmProcurementApprovalActionEnum.APPROVE, userId, nickname, now, auditReqVO.getAuditRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectProcurementPlan(SrmProcurementPlanAuditReqVO auditReqVO) {
        if (StrUtil.isBlank(auditReqVO.getAuditRemark())) {
            throw exception(PROCUREMENT_PLAN_AUDIT_REMARK_REQUIRED);
        }
        SrmProcurementPlanDO plan = validateSubmittedPlan(auditReqVO.getId());
        LocalDateTime now = LocalDateTime.now();
        Long userId = getRequiredLoginUserId();
        String nickname = getRequiredLoginUserNickname();
        plan.setPlanStatus(SrmProcurementPlanStatusEnum.REJECTED.getStatus());
        plan.setAuditBy(userId);
        plan.setAuditName(nickname);
        plan.setAuditTime(now);
        plan.setAuditRemark(auditReqVO.getAuditRemark());
        procurementPlanMapper.updateById(plan);
        insertApprovalRecord(BIZ_TYPE_PROCUREMENT_PLAN, plan.getId(), SrmProcurementApprovalActionEnum.REJECT, userId, nickname, now, auditReqVO.getAuditRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SrmSourcingProjectRespVO generateSourcingProject(SrmProcurementPlanGenerateReqVO generateReqVO) {
        validateProcurementMethod(generateReqVO.getProjectType());
        SrmProcurementPlanDO plan = validateProcurementPlanExists(generateReqVO.getId());
        if (plan.getGeneratedProjectId() != null || sourcingProjectMapper.selectBySourcePlanId(plan.getId()) != null) {
            throw exception(PROCUREMENT_PLAN_GENERATE_DUPLICATE);
        }
        if (!SrmProcurementPlanStatusEnum.APPROVED.getStatus().equals(plan.getPlanStatus())) {
            throw exception(PROCUREMENT_PLAN_GENERATE_NOT_APPROVED);
        }
        String targetProjectForm = SrmProcurementMethodEnum.TENDER.getMethod().equals(generateReqVO.getProjectType())
                ? SrmCodeRuleTargetFormEnum.TENDER_PROJECT.getTargetForm()
                : SrmCodeRuleTargetFormEnum.NON_TENDER_PROJECT.getTargetForm();
        SrmSourcingProjectDO project = SrmSourcingProjectDO.builder()
                .projectNo(codeRuleService.generateCode(targetProjectForm))
                .projectTitle(plan.getPlanTitle())
                .projectType(generateReqVO.getProjectType())
                .projectStatus(SrmSourcingProjectStatusEnum.DRAFT.getStatus())
                .sourcePlanId(plan.getId())
                .sourcePlanNo(plan.getPlanNo())
                .expectedAmount(plan.getExpectedAmount())
                .build();
        project.setTenantId(getRequiredTenantId());
        sourcingProjectMapper.insert(project);

        for (SrmProcurementPlanLineDO sourceLine : procurementPlanLineMapper.selectListByPlanId(plan.getId())) {
            SrmSourcingProjectLineDO targetLine = SrmSourcingProjectLineDO.builder()
                    .projectId(project.getId())
                    .sourcePlanId(plan.getId())
                    .sourcePlanLineId(sourceLine.getId())
                    .lineNo(sourceLine.getLineNo())
                    .materialId(sourceLine.getMaterialId())
                    .materialCode(sourceLine.getMaterialCode())
                    .materialName(sourceLine.getMaterialName())
                    .quantity(sourceLine.getQuantity())
                    .unit(sourceLine.getUnit())
                    .requiredDate(sourceLine.getRequiredDate())
                    .build();
            targetLine.setTenantId(getRequiredTenantId());
            sourcingProjectLineMapper.insert(targetLine);
        }

        plan.setPlanStatus(SrmProcurementPlanStatusEnum.GENERATED.getStatus());
        plan.setGeneratedProjectId(project.getId());
        plan.setGeneratedProjectNo(project.getProjectNo());
        plan.setGeneratedProjectType(project.getProjectType());
        plan.setGeneratedTime(LocalDateTime.now());
        procurementPlanMapper.updateById(plan);
        return buildSourcingProjectResp(project);
    }

    @Override
    public SrmProcurementPlanRespVO getProcurementPlan(Long id) {
        return buildProcurementPlanResp(validateProcurementPlanExists(id));
    }

    @Override
    public PageResult<SrmProcurementPlanRespVO> getProcurementPlanPage(SrmProcurementPlanPageReqVO pageReqVO) {
        PageResult<SrmProcurementPlanDO> pageResult = procurementPlanMapper.selectPage(pageReqVO);
        return new PageResult<>(pageResult.getList().stream()
                .map(this::buildProcurementPlanResp)
                .collect(Collectors.toList()), pageResult.getTotal());
    }

    private SrmProcurementPlanDO validateProcurementPlanExists(Long id) {
        SrmProcurementPlanDO plan = procurementPlanMapper.selectById(id);
        if (plan == null) {
            throw exception(PROCUREMENT_PLAN_NOT_EXISTS);
        }
        return plan;
    }

    private SrmProcurementPlanDO validateSubmittedPlan(Long id) {
        SrmProcurementPlanDO plan = validateProcurementPlanExists(id);
        if (!SrmProcurementPlanStatusEnum.SUBMITTED.getStatus().equals(plan.getPlanStatus())) {
            throw exception(PROCUREMENT_PLAN_STATUS_INVALID, SrmProcurementPlanStatusEnum.getLabel(plan.getPlanStatus()));
        }
        return plan;
    }

    private void validateProcurementMethod(String method) {
        if (!SrmProcurementMethodEnum.contains(method)) {
            throw exception(PROCUREMENT_METHOD_INVALID, method);
        }
    }

    private void validateExpectedAmount(BigDecimal expectedAmount) {
        if (expectedAmount == null || expectedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PROCUREMENT_PLAN_AMOUNT_INVALID);
        }
    }

    private void validatePlanLines(List<SrmProcurementPlanSaveReqVO.Line> lines) {
        if (lines == null || lines.isEmpty()) {
            throw exception(PROCUREMENT_PLAN_LINE_REQUIRED);
        }
        for (SrmProcurementPlanSaveReqVO.Line line : lines) {
            if (line.getQuantity() == null || line.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PROCUREMENT_PLAN_LINE_QUANTITY_INVALID);
            }
        }
    }

    private SrmProcurementPlanRespVO buildProcurementPlanResp(SrmProcurementPlanDO plan) {
        SrmProcurementPlanRespVO respVO = new SrmProcurementPlanRespVO();
        respVO.setId(plan.getId());
        respVO.setPlanNo(plan.getPlanNo());
        respVO.setPlanTitle(plan.getPlanTitle());
        respVO.setProcurementMethod(plan.getProcurementMethod());
        respVO.setProcurementMethodLabel(SrmProcurementMethodEnum.getLabel(plan.getProcurementMethod()));
        respVO.setExpectedAmount(plan.getExpectedAmount());
        respVO.setPlanStatus(plan.getPlanStatus());
        respVO.setPlanStatusLabel(SrmProcurementPlanStatusEnum.getLabel(plan.getPlanStatus()));
        respVO.setRemark(plan.getRemark());
        respVO.setSubmittedName(plan.getSubmittedName());
        respVO.setSubmittedTime(plan.getSubmittedTime());
        respVO.setAuditName(plan.getAuditName());
        respVO.setAuditTime(plan.getAuditTime());
        respVO.setAuditRemark(plan.getAuditRemark());
        respVO.setGeneratedProjectId(plan.getGeneratedProjectId());
        respVO.setGeneratedProjectNo(plan.getGeneratedProjectNo());
        respVO.setGeneratedProjectType(plan.getGeneratedProjectType());
        respVO.setGeneratedTime(plan.getGeneratedTime());
        respVO.setCreateTime(plan.getCreateTime());
        respVO.setLines(procurementPlanLineMapper.selectListByPlanId(plan.getId()).stream()
                .map(this::buildPlanLineResp)
                .collect(Collectors.toList()));
        respVO.setApprovalRecords(approvalRecordMapper.selectListByBiz(BIZ_TYPE_PROCUREMENT_PLAN, plan.getId()).stream()
                .map(this::buildApprovalRecordResp)
                .collect(Collectors.toList()));
        return respVO;
    }

    private SrmProcurementPlanRespVO.Line buildPlanLineResp(SrmProcurementPlanLineDO line) {
        SrmProcurementPlanRespVO.Line respVO = new SrmProcurementPlanRespVO.Line();
        respVO.setId(line.getId());
        respVO.setLineNo(line.getLineNo());
        respVO.setMaterialId(line.getMaterialId());
        respVO.setMaterialCode(line.getMaterialCode());
        respVO.setMaterialName(line.getMaterialName());
        respVO.setQuantity(line.getQuantity());
        respVO.setUnit(line.getUnit());
        respVO.setRequiredDate(line.getRequiredDate());
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

    private SrmSourcingProjectRespVO buildSourcingProjectResp(SrmSourcingProjectDO project) {
        SrmSourcingProjectRespVO respVO = new SrmSourcingProjectRespVO();
        respVO.setId(project.getId());
        respVO.setProjectNo(project.getProjectNo());
        respVO.setProjectTitle(project.getProjectTitle());
        respVO.setProjectType(project.getProjectType());
        respVO.setProjectTypeLabel(SrmProcurementMethodEnum.getLabel(project.getProjectType()));
        respVO.setProjectStatus(project.getProjectStatus());
        respVO.setProjectStatusLabel(SrmSourcingProjectStatusEnum.getLabel(project.getProjectStatus()));
        respVO.setSourcePlanId(project.getSourcePlanId());
        respVO.setSourcePlanNo(project.getSourcePlanNo());
        respVO.setExpectedAmount(project.getExpectedAmount());
        respVO.setLines(sourcingProjectLineMapper.selectListByProjectId(project.getId()).stream()
                .map(this::buildSourcingLineResp)
                .collect(Collectors.toList()));
        return respVO;
    }

    private SrmSourcingProjectRespVO.Line buildSourcingLineResp(SrmSourcingProjectLineDO line) {
        SrmSourcingProjectRespVO.Line respVO = new SrmSourcingProjectRespVO.Line();
        respVO.setId(line.getId());
        respVO.setSourcePlanLineId(line.getSourcePlanLineId());
        respVO.setLineNo(line.getLineNo());
        respVO.setMaterialId(line.getMaterialId());
        respVO.setMaterialCode(line.getMaterialCode());
        respVO.setMaterialName(line.getMaterialName());
        respVO.setQuantity(line.getQuantity());
        respVO.setUnit(line.getUnit());
        respVO.setRequiredDate(line.getRequiredDate());
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
