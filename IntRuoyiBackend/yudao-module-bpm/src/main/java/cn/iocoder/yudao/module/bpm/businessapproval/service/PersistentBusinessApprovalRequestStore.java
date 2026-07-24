package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicy;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalRequestDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalRequestMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class PersistentBusinessApprovalRequestStore implements BusinessApprovalRequestStore {

    @Resource
    private BusinessApprovalRequestMapper requestMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessApprovalRequest createDirectRequest(BusinessApprovalContext context, BusinessApprovalPolicy policy) {
        BusinessApprovalRequestDO requestDO = buildRequestDO(context, policy,
                BusinessApprovalRequestStatus.DIRECT_EXECUTING);
        requestMapper.insert(requestDO);
        return toModel(requestDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessApprovalRequest createPendingRequest(BusinessApprovalContext context, BusinessApprovalPolicy policy) {
        if (hasPendingRequest(context)) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PENDING_CONFLICT,
                    "Business approval request already pending for object " + context.getObjectId());
        }
        BusinessApprovalRequestDO requestDO = buildRequestDO(context, policy, BusinessApprovalRequestStatus.PENDING_BPM);
        requestMapper.insert(requestDO);
        return toModel(requestDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessApprovalRequest attachProcessInstance(Long requestId, String processInstanceId) {
        BusinessApprovalRequestDO requestDO = requireRequestDO(requestId);
        requestDO.setProcessInstanceId(processInstanceId);
        requestMapper.updateById(requestDO);
        return toModel(requestDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessApprovalRequest update(BusinessApprovalRequest request) {
        requireRequestDO(request.getRequestId());
        BusinessApprovalRequestDO requestDO = toDO(request);
        requestMapper.updateById(requestDO);
        return toModel(requestDO);
    }

    @Override
    public Optional<BusinessApprovalRequest> findByProcessInstanceId(String processInstanceId) {
        BusinessApprovalRequestDO requestDO = requestMapper.selectByProcessInstanceId(processInstanceId);
        return Optional.ofNullable(requestDO).map(this::toModel);
    }

    @Override
    public Optional<BusinessApprovalRequest> findPendingByBusinessAction(BusinessApprovalContext context) {
        BusinessApprovalRequestDO requestDO = requestMapper.selectPendingByBusinessAction(context);
        return Optional.ofNullable(requestDO).map(this::toModel);
    }

    @Override
    public boolean hasPendingRequest(BusinessApprovalContext context) {
        return requestMapper.selectPendingByBusinessAction(context) != null;
    }

    private BusinessApprovalRequestDO requireRequestDO(Long requestId) {
        BusinessApprovalRequestDO requestDO = requestMapper.selectById(requestId);
        if (requestDO == null) {
            throw new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_REQUEST_NOT_FOUND,
                    "Business approval request not found: " + requestId);
        }
        return requestDO;
    }

    private BusinessApprovalRequestDO buildRequestDO(BusinessApprovalContext context,
                                                     BusinessApprovalPolicy policy,
                                                     BusinessApprovalRequestStatus status) {
        return BusinessApprovalRequestDO.builder()
                .tenantId(context.getTenantId())
                .policyId(policy.getPolicyId())
                .policyMode(policy.getMode().name())
                .dataDomain(context.getDataDomain())
                .systemCode(context.getSystemCode())
                .objectType(context.getObjectType())
                .objectId(context.getObjectId())
                .objectVersion(context.getObjectVersion())
                .actionCode(context.getActionCode())
                .objectState(context.getObjectState())
                .requestStatus(status.name())
                .applicantUserId(context.getApplicantUserId())
                .processDefinitionKey(policy.getProcessDefinitionKey())
                .effectExecutorCode(policy.getEffectExecutorCode())
                .businessContextJson(JsonUtils.toJsonString(context))
                .build();
    }

    private BusinessApprovalRequestDO toDO(BusinessApprovalRequest request) {
        BusinessApprovalContext context = request.getContext();
        return BusinessApprovalRequestDO.builder()
                .id(request.getRequestId())
                .tenantId(request.getTenantId())
                .policyId(request.getPolicyId())
                .policyMode(request.getPolicyMode().name())
                .dataDomain(context.getDataDomain())
                .systemCode(context.getSystemCode())
                .objectType(context.getObjectType())
                .objectId(context.getObjectId())
                .objectVersion(context.getObjectVersion())
                .actionCode(context.getActionCode())
                .objectState(context.getObjectState())
                .requestStatus(request.getStatus().name())
                .applicantUserId(context.getApplicantUserId())
                .processDefinitionKey(request.getProcessDefinitionKey())
                .processInstanceId(request.getProcessInstanceId())
                .effectExecutorCode(request.getEffectExecutorCode())
                .lastEventKey(request.getLastEventKey())
                .resultState(request.getResultState())
                .failureReason(request.getFailureReason())
                .businessContextJson(JsonUtils.toJsonString(context))
                .build();
    }

    private BusinessApprovalRequest toModel(BusinessApprovalRequestDO requestDO) {
        return BusinessApprovalRequest.builder()
                .requestId(requestDO.getId())
                .tenantId(requestDO.getTenantId())
                .policyId(requestDO.getPolicyId())
                .policyMode(BusinessApprovalPolicyMode.valueOf(requestDO.getPolicyMode()))
                .processDefinitionKey(requestDO.getProcessDefinitionKey())
                .effectExecutorCode(requestDO.getEffectExecutorCode())
                .status(BusinessApprovalRequestStatus.valueOf(requestDO.getRequestStatus()))
                .context(JsonUtils.parseObject(requestDO.getBusinessContextJson(), BusinessApprovalContext.class))
                .processInstanceId(requestDO.getProcessInstanceId())
                .lastEventKey(requestDO.getLastEventKey())
                .resultState(requestDO.getResultState())
                .failureReason(requestDO.getFailureReason())
                .build();
    }

}
