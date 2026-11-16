package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicyTransferVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalPolicyMapper;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectExecutor;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.flowable.engine.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Validated
public class BusinessApprovalPolicyTransferService {
    @Resource private BusinessApprovalPolicyMapper policyMapper;
    @Resource private BusinessApprovalEffectExecutorRegistry executorRegistry;
    @Autowired(required = false)
    private List<FormBusinessEffectExecutor> formEffectExecutors = List.of();
    @Resource private RepositoryService repositoryService;
    @Resource private BusinessApprovalProcessTransferService processTransferService;

    public BusinessApprovalPolicyTransferVO exportPolicies() {
        BusinessApprovalPolicyTransferVO result = new BusinessApprovalPolicyTransferVO();
        result.setSchemaVersion(2);
        result.setFormat("BUSINESS_APPROVAL_POLICIES");
        List<BusinessApprovalPolicyDO> all = currentTenantPolicies(false);
        Map<List<String>, Long> latest = new HashMap<>();
        all.forEach(policy -> latest.put(identity(policy), policy.getId()));
        // Export current configuration plus any still-effective version, not retired audit history.
        result.setPolicies(all.stream()
                .filter(policy -> "PUBLISHED".equals(policy.getStatus())
                        || Objects.equals(policy.getId(), latest.get(identity(policy))))
                .sorted(Comparator.comparing(BusinessApprovalPolicyDO::getDataDomain)
                        .thenComparing(BusinessApprovalPolicyDO::getSystemCode)
                        .thenComparing(BusinessApprovalPolicyDO::getObjectType)
                        .thenComparing(BusinessApprovalPolicyDO::getActionCode)
                        .thenComparing(BusinessApprovalPolicyDO::getObjectState)
                        .thenComparing(BusinessApprovalPolicyDO::getId))
                .map(policy -> BeanUtils.toBean(policy, BusinessApprovalPolicyTransferVO.Item.class)).toList());
        result.setProcesses(processTransferService.exportProcesses(result.getPolicies()));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public int importPolicies(@Valid BusinessApprovalPolicyTransferVO transfer) {
        if (!Objects.equals(transfer.getSchemaVersion(), 2) || transfer.getProcesses() == null) {
            throw invalid("旧版审批策略文件不包含依赖流程，请从源租户重新导出版本2完整配置");
        }
        if (transfer.getPolicies().isEmpty()) {
            throw invalid("审批策略文件没有可导入的策略");
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<BusinessApprovalPolicyDO> existing = currentTenantPolicies(true);
        List<BusinessApprovalPolicyDO> incoming = transfer.getPolicies().stream()
                .map(item -> BeanUtils.toBean(item, BusinessApprovalPolicyDO.class)).toList();
        // Validate the whole package before any writes. Preserve field values, row order and status exactly.
        for (BusinessApprovalPolicyDO policy : incoming) {
            if ("PUBLISHED".equals(policy.getStatus())) {
                requireRegisteredExecutor(policy.getEffectExecutorCode());
            }
        }
        List<BusinessApprovalPolicyDO> published = new ArrayList<>(existing.stream()
                .filter(policy -> "PUBLISHED".equals(policy.getStatus())
                        && incoming.stream().noneMatch(item -> identity(item).equals(identity(policy)))).toList());
        for (BusinessApprovalPolicyDO policy : incoming) {
            if (!"PUBLISHED".equals(policy.getStatus())) continue;
            if (published.stream().anyMatch(other -> sameAction(other, policy)
                    && (Objects.equals(other.getObjectState(), policy.getObjectState())
                    || "ALL".equals(other.getObjectState()) || "ALL".equals(policy.getObjectState())))) {
                throw conflict("已发布策略的适用范围冲突：" + describe(policy) + "，整批未导入");
            }
            published.add(policy);
        }
        // Flowable, role creation and policy writes share the same transaction.
        processTransferService.importProcesses(transfer);
        for (BusinessApprovalPolicyDO policy : incoming) {
            if ("PUBLISHED".equals(policy.getStatus()) && "BPM_REQUIRED".equals(policy.getPolicyMode())) {
                requireTargetProcess(policy, tenantId);
            }
        }
        // Retain old row identities for in-flight approval references; replace only matched business scopes.
        for (BusinessApprovalPolicyDO policy : existing) {
            if ("PUBLISHED".equals(policy.getStatus())
                    && incoming.stream().anyMatch(item -> identity(item).equals(identity(policy)))) {
                policy.setStatus("DISABLED");
                policyMapper.updateById(policy);
            }
        }
        for (BusinessApprovalPolicyDO policy : incoming) {
            policy.setTenantId(tenantId);
            policyMapper.insert(policy);
        }
        return incoming.size();
    }

    private void requireRegisteredExecutor(String executorCode) {
        // The shared policy table also contains actions executed by the form runtime.
        // Validate against its actual registered beans as well as business approval executors.
        if (StrUtil.isNotBlank(executorCode) && formEffectExecutors.stream()
                .anyMatch(executor -> executorCode.equals(executor.getExecutorCode()))) {
            return;
        }
        executorRegistry.requireExecutor(executorCode);
    }

    private List<BusinessApprovalPolicyDO> currentTenantPolicies(boolean lock) {
        QueryWrapperX<BusinessApprovalPolicyDO> query = new QueryWrapperX<>();
        query.eq("tenant_id", TenantContextHolder.getRequiredTenantId()).orderByAsc("id");
        if (lock) query.last("FOR UPDATE");
        return policyMapper.selectList(query);
    }

    private List<String> identity(BusinessApprovalPolicyDO policy) {
        return List.of(policy.getDataDomain(), policy.getSystemCode(), policy.getObjectType(),
                policy.getActionCode(), policy.getObjectState());
    }

    private void requireTargetProcess(BusinessApprovalPolicyDO policy, Long tenantId) {
        String key = policy.getProcessDefinitionKey();
        if (StrUtil.isBlank(key)) {
            throw invalid("审批策略缺少关联流程：" + describe(policy));
        }
        var definition = repositoryService.createProcessDefinitionQuery().processDefinitionKey(key)
                .processDefinitionTenantId(String.valueOf(tenantId)).latestVersion().singleResult();
        if (definition == null || definition.isSuspended()) {
            throw invalid("目标租户缺少已部署且启用的审批流程：" + key + "，请先配置该流程，整批未导入");
        }
    }

    private boolean sameAction(BusinessApprovalPolicyDO first, BusinessApprovalPolicyDO second) {
        return Objects.equals(first.getDataDomain(), second.getDataDomain())
                && Objects.equals(first.getSystemCode(), second.getSystemCode())
                && Objects.equals(first.getObjectType(), second.getObjectType())
                && Objects.equals(first.getActionCode(), second.getActionCode());
    }

    private String describe(BusinessApprovalPolicyDO policy) {
        return String.join(" / ", policy.getDataDomain(), policy.getSystemCode(), policy.getObjectType(),
                policy.getActionCode(), policy.getObjectState());
    }

    private BusinessApprovalException invalid(String message) {
        return new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID, message);
    }

    private BusinessApprovalException conflict(String message) {
        return new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_POLICY_CONFLICT, message);
    }
}
