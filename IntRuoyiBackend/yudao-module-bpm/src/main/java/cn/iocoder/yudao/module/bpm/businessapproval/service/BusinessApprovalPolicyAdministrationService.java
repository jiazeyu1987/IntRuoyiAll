package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureRecordService;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicy;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicyPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicyRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicySaveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicySwitchModeReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalPolicyMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class BusinessApprovalPolicyAdministrationService {

    @Resource
    private BusinessApprovalPolicyMapper policyMapper;
    @Resource
    private BusinessApprovalEffectExecutorRegistry executorRegistry;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ApprovalSignatureRecordService signatureRecordService;

    public PageResult<BusinessApprovalPolicyRespVO> getPolicyPage(BusinessApprovalPolicyPageReqVO reqVO) {
        reqVO.setTenantId(resolveTenantId(reqVO.getTenantId()));
        PageResult<BusinessApprovalPolicyDO> pageResult = policyMapper.selectPage(reqVO);
        return new PageResult<>(pageResult.getList().stream().map(this::toResp).toList(), pageResult.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    public BusinessApprovalPolicyRespVO savePolicy(BusinessApprovalPolicySaveReqVO reqVO) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        BusinessApprovalPolicyMode mode = parseMode(reqVO.getPolicyMode());
        BusinessApprovalPolicyDO insertObj = BusinessApprovalPolicyDO.builder()
                .tenantId(tenantId)
                .dataDomain(reqVO.getDataDomain())
                .systemCode(reqVO.getSystemCode())
                .objectType(reqVO.getObjectType())
                .actionCode(reqVO.getActionCode())
                .objectState(reqVO.getObjectState())
                .policyMode(mode.name())
                .processDefinitionKey(StrUtil.isBlank(reqVO.getProcessDefinitionKey())
                        ? null : StrUtil.trim(reqVO.getProcessDefinitionKey()))
                .effectExecutorCode(reqVO.getEffectExecutorCode())
                .status("DRAFT")
                .remark(reqVO.getRemark())
                .build();
        policyMapper.insert(insertObj);
        return toResp(insertObj);
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishPolicy(Long policyId) {
        BusinessApprovalPolicyDO policy = requirePolicy(policyId);
        BusinessApprovalPolicyMode mode = parseMode(policy.getPolicyMode());
        executorRegistry.requireExecutor(policy.getEffectExecutorCode());
        if (mode == BusinessApprovalPolicyMode.BPM_REQUIRED && StrUtil.isBlank(policy.getProcessDefinitionKey())) {
            throw new BusinessApprovalException(
                    BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_DEFINITION_MISSING,
                    "BPM process definition key is required for BPM_REQUIRED policy");
        }
        if (hasOtherPublishedPolicy(policy)) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_POLICY_CONFLICT,
                    "Published business approval policy already exists for action " + policy.getActionCode());
        }
        if (mode == BusinessApprovalPolicyMode.SIGNATURE_REQUIRED) {
            policy.setProcessDefinitionKey(null);
        }
        if (mode == BusinessApprovalPolicyMode.DIRECT && StrUtil.isBlank(policy.getProcessDefinitionKey())) {
            policy.setProcessDefinitionKey(null);
        }
        policy.setStatus(BusinessApprovalPolicy.STATUS_PUBLISHED);
        policyMapper.updateById(policy);
    }

    @Transactional(rollbackFor = Exception.class)
    public void disablePolicy(Long policyId) {
        BusinessApprovalPolicyDO policy = requirePolicy(policyId);
        policy.setStatus(BusinessApprovalPolicy.STATUS_DISABLED);
        policyMapper.updateById(policy);
    }

    @Transactional(rollbackFor = Exception.class)
    public BusinessApprovalPolicyRespVO switchPolicyMode(Long loginUserId, Long policyId,
            BusinessApprovalPolicySwitchModeReqVO reqVO) {
        BusinessApprovalPolicyDO sourcePolicy = requirePolicy(policyId);
        if (!BusinessApprovalPolicy.STATUS_PUBLISHED.equals(sourcePolicy.getStatus())) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_POLICY_STATUS_INVALID,
                    "Only published business approval policy can switch mode: " + policyId);
        }
        BusinessApprovalPolicyMode targetMode = parseMode(reqVO.getPolicyMode());
        if (StrUtil.isBlank(reqVO.getSignaturePassword())) {
            throw new BusinessApprovalException(
                    BusinessApprovalErrorCode.BUSINESS_APPROVAL_SIGNATURE_PASSWORD_REQUIRED,
                    "Electronic signature password is required for business approval policy switch");
        }
        String signaturePassword = StrUtil.trim(reqVO.getSignaturePassword());
        String processDefinitionKey = resolveSwitchProcessDefinitionKey(sourcePolicy, targetMode);
        executorRegistry.requireExecutor(sourcePolicy.getEffectExecutorCode());
        adminUserApi.validatePassword(loginUserId, signaturePassword);

        sourcePolicy.setStatus(BusinessApprovalPolicy.STATUS_DISABLED);
        policyMapper.updateById(sourcePolicy);

        BusinessApprovalPolicyDO targetPolicy = BusinessApprovalPolicyDO.builder()
                .tenantId(sourcePolicy.getTenantId())
                .dataDomain(sourcePolicy.getDataDomain())
                .systemCode(sourcePolicy.getSystemCode())
                .objectType(sourcePolicy.getObjectType())
                .actionCode(sourcePolicy.getActionCode())
                .objectState(sourcePolicy.getObjectState())
                .policyMode(targetMode.name())
                .processDefinitionKey(processDefinitionKey)
                .effectExecutorCode(sourcePolicy.getEffectExecutorCode())
                .status(BusinessApprovalPolicy.STATUS_PUBLISHED)
                .remark(sourcePolicy.getRemark())
                .build();
        policyMapper.insert(targetPolicy);
        Objects.requireNonNull(signatureRecordService.recordReviewSignature(ApprovalTaskReviewContext.of(
                loginUserId,
                ApprovalModuleCode.BPM,
                "BUSINESS_APPROVAL_POLICY_SWITCH",
                String.valueOf(targetPolicy.getId()),
                buildSwitchBusinessKey(targetPolicy),
                null,
                ApprovalTaskReviewResult.APPROVE,
                targetMode.name(),
                signaturePassword,
                false)), "BUSINESS_APPROVAL_POLICY_SWITCH_SIGNATURE_RECORD_REQUIRED");
        return toResp(targetPolicy);
    }

    private String buildSwitchBusinessKey(BusinessApprovalPolicyDO policy) {
        return policy.getSystemCode() + ":" + policy.getObjectType() + ":"
                + policy.getActionCode() + ":" + policy.getObjectState();
    }

    private String resolveSwitchProcessDefinitionKey(BusinessApprovalPolicyDO sourcePolicy,
            BusinessApprovalPolicyMode targetMode) {
        if (targetMode == BusinessApprovalPolicyMode.DIRECT) {
            return StrUtil.isBlank(sourcePolicy.getProcessDefinitionKey())
                    ? null : StrUtil.trim(sourcePolicy.getProcessDefinitionKey());
        }
        if (targetMode == BusinessApprovalPolicyMode.SIGNATURE_REQUIRED) {
            return null;
        }
        String processDefinitionKey = StrUtil.trim(sourcePolicy.getProcessDefinitionKey());
        if (StrUtil.isBlank(processDefinitionKey)) {
            processDefinitionKey = resolveLatestHistoricalBpmProcessDefinitionKey(sourcePolicy);
        }
        if (StrUtil.isBlank(processDefinitionKey)) {
            processDefinitionKey = executorRegistry.requireBpmProcessDefinitionKey(
                    sourcePolicy.getEffectExecutorCode());
        }
        if (StrUtil.isBlank(processDefinitionKey)) {
            throw new BusinessApprovalException(
                    BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_DEFINITION_MISSING,
                    "BPM process definition key is required for BPM_REQUIRED policy");
        }
        return processDefinitionKey;
    }

    private String resolveLatestHistoricalBpmProcessDefinitionKey(BusinessApprovalPolicyDO sourcePolicy) {
        BusinessApprovalPolicyDO historicalPolicy = policyMapper.selectLatestBpmRequiredWithProcessDefinitionKey(
                sourcePolicy.getTenantId(),
                sourcePolicy.getDataDomain(),
                sourcePolicy.getSystemCode(),
                sourcePolicy.getObjectType(),
                sourcePolicy.getActionCode(),
                sourcePolicy.getObjectState());
        return historicalPolicy == null ? null : StrUtil.trim(historicalPolicy.getProcessDefinitionKey());
    }

    private boolean hasOtherPublishedPolicy(BusinessApprovalPolicyDO policy) {
        return policyMapper.selectPublishedByAction(policy.getTenantId(), policy.getDataDomain(),
                        policy.getSystemCode(), policy.getObjectType(), policy.getActionCode(),
                        policy.getObjectState())
                .stream()
                .anyMatch(published -> !published.getId().equals(policy.getId()));
    }

    private BusinessApprovalPolicyDO requirePolicy(Long policyId) {
        BusinessApprovalPolicyDO policy = policyMapper.selectById(policyId);
        if (policy == null) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_POLICY_NOT_FOUND,
                    "Business approval policy not found: " + policyId);
        }
        return policy;
    }

    private BusinessApprovalPolicyMode parseMode(String policyMode) {
        try {
            return BusinessApprovalPolicyMode.valueOf(policyMode);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_MODE_INVALID,
                    "Unsupported business approval mode: " + policyMode);
        }
    }

    private Long resolveTenantId(Long tenantId) {
        return tenantId == null ? TenantContextHolder.getRequiredTenantId() : tenantId;
    }

    private BusinessApprovalPolicyRespVO toResp(BusinessApprovalPolicyDO policy) {
        BusinessApprovalPolicyRespVO respVO = new BusinessApprovalPolicyRespVO();
        respVO.setId(policy.getId());
        respVO.setDataDomain(policy.getDataDomain());
        respVO.setSystemCode(policy.getSystemCode());
        respVO.setObjectType(policy.getObjectType());
        respVO.setActionCode(policy.getActionCode());
        respVO.setObjectState(policy.getObjectState());
        respVO.setPolicyMode(policy.getPolicyMode());
        respVO.setProcessDefinitionKey(policy.getProcessDefinitionKey());
        respVO.setEffectExecutorCode(policy.getEffectExecutorCode());
        respVO.setStatus(policy.getStatus());
        respVO.setRemark(policy.getRemark());
        respVO.setUpdatedTime(policy.getUpdateTime());
        return respVO;
    }

}
