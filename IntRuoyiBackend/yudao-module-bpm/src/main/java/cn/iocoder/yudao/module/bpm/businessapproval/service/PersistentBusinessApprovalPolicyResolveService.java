package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicy;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalPolicyMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PersistentBusinessApprovalPolicyResolveService extends BusinessApprovalPolicyResolveService {

    private final BusinessApprovalPolicyMapper policyMapper;

    public PersistentBusinessApprovalPolicyResolveService(BusinessApprovalPolicyMapper policyMapper) {
        super(List.of());
        this.policyMapper = policyMapper;
    }

    @Override
    protected List<BusinessApprovalPolicy> findPublishedPolicies(BusinessApprovalContext context) {
        return policyMapper.selectPublishedByAction(context).stream()
                .map(PersistentBusinessApprovalPolicyResolveService::toPolicy)
                .toList();
    }

    static BusinessApprovalPolicy toPolicy(BusinessApprovalPolicyDO policyDO) {
        return BusinessApprovalPolicy.builder()
                .policyId(policyDO.getId())
                .tenantId(policyDO.getTenantId())
                .dataDomain(policyDO.getDataDomain())
                .systemCode(policyDO.getSystemCode())
                .objectType(policyDO.getObjectType())
                .actionCode(policyDO.getActionCode())
                .objectState(policyDO.getObjectState())
                .mode(BusinessApprovalPolicyMode.valueOf(policyDO.getPolicyMode()))
                .processDefinitionKey(policyDO.getProcessDefinitionKey())
                .effectExecutorCode(policyDO.getEffectExecutorCode())
                .status(policyDO.getStatus())
                .build();
    }

}
