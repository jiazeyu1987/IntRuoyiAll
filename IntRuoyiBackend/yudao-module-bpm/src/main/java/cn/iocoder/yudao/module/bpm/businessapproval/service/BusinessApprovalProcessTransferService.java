package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicyTransferVO;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.category.BpmCategorySaveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.model.BpmModelMetaInfoVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.iocoder.yudao.module.bpm.service.definition.BpmCategoryService;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.rolecategory.RoleCategorySaveReqVO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.permission.RoleCategoryService;
import jakarta.annotation.Resource;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnModelConstants.*;

/** Dependency transfer participates in the policy transaction; never copies tenant-local identities. */
@Service
public class BusinessApprovalProcessTransferService {
    @Resource private RepositoryService repositoryService;
    @Resource private BpmProcessDefinitionService definitionService;
    @Resource private BpmCategoryService categoryService;
    @Resource private RoleService roleService;
    @Resource private RoleCategoryService roleCategoryService;
    @Resource private AdminUserMapper userMapper;
    @Resource private BpmTaskCandidateInvoker candidateInvoker;

    public List<BusinessApprovalPolicyTransferVO.Process> exportProcesses(List<BusinessApprovalPolicyTransferVO.Item> policies) {
        return requiredKeys(policies).stream().map(this::exportProcess).toList();
    }

    private BusinessApprovalPolicyTransferVO.Process exportProcess(String key) {
        ProcessDefinition definition = latest(key);
        if (definition == null || definition.isSuspended()) throw invalid("源租户流程未部署或已停用：" + key);
        BpmProcessDefinitionInfoDO info = definitionService.getProcessDefinitionInfo(definition.getId());
        if (info == null) throw invalid("源流程缺少发布配置：" + key);
        BpmModelMetaInfoVO meta = BeanUtils.toBean(info, BpmModelMetaInfoVO.class);
        meta.setType(info.getModelType());
        var result = new BusinessApprovalPolicyTransferVO.Process();
        result.setKey(key);
        result.setName(definition.getName());
        var category = categoryService.getCategoryMap(List.of(info.getCategory())).get(info.getCategory());
        if (category == null || !Objects.equals(category.getStatus(), 0)) throw invalid("源流程分类不存在或已停用：" + key);
        var categoryVO = BeanUtils.toBean(category, BpmCategorySaveReqVO.class);
        categoryVO.setId(null);
        result.setCategory(categoryVO);
        result.setStartUsernames(info.getStartUserIds() == null ? List.of() : info.getStartUserIds().stream().map(id -> {
            var user = userMapper.selectSessionSubject(TenantContextHolder.getRequiredTenantId(), id);
            if (user == null || !Objects.equals(user.getStatus(), 0)) throw invalid("源流程发起人不存在或已停用：" + key);
            return user.getUsername();
        }).toList());
        // The importing administrator owns newly created models. Start restrictions map by username.
        meta.setManagerUserIds(List.of());
        meta.setStartUserIds(List.of());
        validatePortableMeta(meta, key);
        result.setMetaInfo(meta);
        BpmnModel model = repositoryService.getBpmnModel(definition.getId());
        if (model == null) throw invalid("源流程缺少流程图：" + key);
        // Work on a serialized copy, never mutate the engine's cached deployed model.
        model = BpmnModelUtils.getBpmnModel(BpmnModelUtils.getBpmnXml(model).getBytes(StandardCharsets.UTF_8));
        Map<String, BusinessApprovalPolicyTransferVO.Role> roles = new LinkedHashMap<>();
        for (UserTask task : portableTasks(model, key)) {
            if (!Objects.equals(BpmnModelUtils.parseCandidateStrategy(task), 10)) continue;
            String param = BpmnModelUtils.parseCandidateParam(task);
            if (StrUtil.isBlank(param)) throw invalid("流程角色未配置：" + key + "/" + task.getId());
            List<String> codes = new ArrayList<>();
            for (String id : param.split(",")) {
                var role = roleService.getRole(Long.valueOf(id.trim()));
                if (role == null || !Objects.equals(role.getTenantId(), TenantContextHolder.getRequiredTenantId())
                        || !Objects.equals(role.getStatus(), 0)) throw invalid("源流程审批角色不存在或已停用：" + key);
                var roleCategory = roleCategoryService.getRoleCategory(role.getCategoryId());
                if (roleCategory == null || !Objects.equals(roleCategory.getStatus(), 0)) throw invalid("源审批角色分类不存在或已停用：" + role.getCode());
                var item = new BusinessApprovalPolicyTransferVO.Role();
                item.setCode(role.getCode());
                item.setName(role.getName());
                var categoryItem = BeanUtils.toBean(roleCategory, RoleCategorySaveReqVO.class);
                categoryItem.setId(null);
                item.setCategory(categoryItem);
                roles.put(item.getCode(), item);
                codes.add(item.getCode());
            }
            setCandidateParam(task, String.join(",", codes));
        }
        result.setRoles(new ArrayList<>(roles.values()));
        result.setBpmnXml(BpmnModelUtils.getBpmnXml(model));
        return result;
    }

    public void importProcesses(BusinessApprovalPolicyTransferVO transfer) {
        Set<String> keys = requiredKeys(transfer.getPolicies());
        Map<String, BusinessApprovalPolicyTransferVO.Process> processes = new LinkedHashMap<>();
        for (var process : transfer.getProcesses()) {
            if (!keys.contains(process.getKey()) || processes.putIfAbsent(process.getKey(), process) != null) {
                throw invalid("流程包包含重复或无关的流程：" + process.getKey());
            }
        }
        if (!processes.keySet().equals(keys)) throw invalid("审批策略包缺少依赖流程，请从源租户重新导出完整配置");
        for (var process : processes.values()) importProcess(process);
    }

    private void importProcess(BusinessApprovalPolicyTransferVO.Process process) {
        String key = process.getKey();
        validatePortableMeta(process.getMetaInfo(), key);
        BpmnModel model = BpmnModelUtils.getBpmnModel(process.getBpmnXml().getBytes(StandardCharsets.UTF_8));
        if (model == null || model.getProcesses().size() != 1 || !Objects.equals(model.getMainProcess().getId(), key)
                || !Objects.equals(model.getMainProcess().getName(), process.getName())
                || BpmnModelUtils.getStartEvent(model) == null) throw invalid("流程标识、名称或开始节点不正确：" + key);
        var tasks = portableTasks(model, key);
        // Existing target workflows are authoritative; do not overwrite or reactivate them silently.
        var existing = latest(key);
        if (existing != null) {
            if (existing.isSuspended()) throw invalid("目标租户同名流程已停用，请先核对并启用：" + key);
            return;
        }
        if (repositoryService.createModelQuery().modelTenantId(tenant()).modelKey(key).singleResult() != null) {
            throw invalid("目标租户已有未发布的同名流程模型，请先处理：" + key);
        }
        Map<String, BusinessApprovalPolicyTransferVO.Role> roles = new LinkedHashMap<>();
        for (var role : process.getRoles()) {
            if (roles.putIfAbsent(role.getCode(), role) != null) throw invalid("流程包审批角色重复：" + role.getCode());
        }
        Set<String> referenced = tasks.stream().filter(task -> Objects.equals(BpmnModelUtils.parseCandidateStrategy(task), 10))
                .flatMap(task -> Arrays.stream(Objects.toString(BpmnModelUtils.parseCandidateParam(task), "").split(",")))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!referenced.equals(roles.keySet())) throw invalid("流程包审批角色依赖不完整：" + key);
        Map<String, Long> targetRoles = new HashMap<>();
        for (var role : roles.values()) targetRoles.put(role.getCode(), ensureRole(role));
        for (UserTask task : tasks) {
            if (!Objects.equals(BpmnModelUtils.parseCandidateStrategy(task), 10)) continue;
            setCandidateParam(task, Arrays.stream(BpmnModelUtils.parseCandidateParam(task).split(","))
                    .map(code -> targetRoles.get(code).toString()).collect(Collectors.joining(",")));
        }
        var category = process.getCategory();
        if (category.getId() != null || !Objects.equals(category.getStatus(), 0)) throw invalid("流程分类不能含源编号或停用配置：" + key);
        var targetCategory = categoryService.getCategoryMap(List.of(category.getCode())).get(category.getCode());
        if (targetCategory == null) categoryService.createCategory(category);
        else if (!Objects.equals(targetCategory.getStatus(), 0)) throw invalid("目标流程分类已停用：" + category.getCode());

        BpmModelMetaInfoVO meta = JsonUtils.parseObject(JsonUtils.toJsonString(process.getMetaInfo()), BpmModelMetaInfoVO.class);
        Long owner = getLoginUserId();
        if (owner == null) throw invalid("导入流程需要当前登录管理员");
        var ownerUser = userMapper.selectSessionSubject(TenantContextHolder.getRequiredTenantId(), owner);
        if (ownerUser == null || !Objects.equals(ownerUser.getStatus(), 0)) throw invalid("当前流程导入人不属于目标租户或已停用");
        meta.setManagerUserIds(List.of(owner));
        meta.setStartUserIds(process.getStartUsernames().stream().map(username -> {
            var user = userMapper.selectByUsername(username);
            if (user == null || !Objects.equals(user.getTenantId(), TenantContextHolder.getRequiredTenantId())
                    || !Objects.equals(user.getStatus(), 0)) throw invalid("目标租户缺少流程发起账号或账号已停用：" + username);
            return user.getId();
        }).toList());
        byte[] xml = BpmnModelUtils.getBpmnXml(model).getBytes(StandardCharsets.UTF_8);
        candidateInvoker.validateBpmnConfig(xml);
        var targetModel = repositoryService.newModel();
        targetModel.setKey(key);
        targetModel.setName(process.getName());
        targetModel.setCategory(category.getCode());
        targetModel.setTenantId(tenant());
        targetModel.setMetaInfo(JsonUtils.toJsonString(meta));
        repositoryService.saveModel(targetModel);
        repositoryService.addModelEditorSource(targetModel.getId(), xml);
        String definitionId = definitionService.createProcessDefinition(targetModel, meta, xml, null, null);
        // addModelEditorSource updates the engine entity revision; reload before linking deployment.
        targetModel = repositoryService.getModel(targetModel.getId());
        targetModel.setDeploymentId(repositoryService.getProcessDefinition(definitionId).getDeploymentId());
        repositoryService.saveModel(targetModel);
    }

    private Long ensureRole(BusinessApprovalPolicyTransferVO.Role item) {
        var existing = roleService.getRoleByCode(item.getCode());
        if (existing != null) {
            if (!Objects.equals(existing.getTenantId(), TenantContextHolder.getRequiredTenantId())
                    || !Objects.equals(existing.getStatus(), 0)) throw invalid("目标审批角色已停用或不属于当前租户：" + item.getCode());
            return existing.getId();
        }
        var category = item.getCategory();
        if (category.getId() != null || !Objects.equals(category.getStatus(), 0)) throw invalid("角色分类不能含源编号或停用配置：" + item.getCode());
        var targetCategory = roleCategoryService.getRoleCategoryList().stream()
                .filter(c -> Objects.equals(c.getCode(), category.getCode())).findFirst().orElse(null);
        Long categoryId;
        if (targetCategory == null) categoryId = roleCategoryService.createRoleCategory(category);
        else {
            if (!Objects.equals(targetCategory.getStatus(), 0)) throw invalid("目标审批角色分类已停用：" + category.getCode());
            categoryId = targetCategory.getId();
        }
        var request = new RoleSaveReqVO();
        request.setCode(item.getCode());
        request.setName(item.getName());
        request.setCategoryId(categoryId);
        request.setSort(0);
        request.setStatus(0);
        request.setRemark("审批策略包导入的审批角色，请在目标租户分配审批人员");
        // No source users, permissions or data scopes are granted by this configuration transfer.
        return roleService.createRole(request, 2);
    }

    private void validatePortableMeta(BpmModelMetaInfoVO meta, String key) {
        if (!Objects.equals(meta.getType(), 10) || !Objects.equals(meta.getFormType(), 20) || meta.getFormId() != null
                || StrUtil.isBlank(meta.getFormCustomCreatePath()) || StrUtil.isBlank(meta.getFormCustomViewPath())
                || nonEmpty(meta.getStartDeptIds()) || nonEmpty(meta.getStartUserIds()) || nonEmpty(meta.getManagerUserIds())) {
            throw invalid("流程包含尚不能跨租户移植的模型、表单或人员部门编号：" + key);
        }
    }

    private boolean nonEmpty(Collection<?> values) { return values != null && !values.isEmpty(); }

    private List<UserTask> portableTasks(BpmnModel model, String key) {
        if (!BpmnModelUtils.getBpmnModelElements(model, CallActivity.class).isEmpty()) throw invalid("流程依赖子流程，不能导出不完整配置：" + key);
        var tasks = BpmnModelUtils.getBpmnModelElements(model, UserTask.class);
        for (var task : tasks) {
            Integer strategy = BpmnModelUtils.parseCandidateStrategy(task);
            // These strategies contain no tenant-local static identity. Others need explicit mapping.
            if (strategy == null || !Set.of(10, 34, 35, 36, 37, 38).contains(strategy)
                    || StrUtil.isNotBlank(task.getAssignee()) || StrUtil.isNotBlank(task.getOwner())
                    || !task.getCandidateUsers().isEmpty() || !task.getCandidateGroups().isEmpty()
                    || StrUtil.isNotBlank(task.getAttributeValue(NAMESPACE, USER_TASK_ASSIGN_USER_IDS))
                    || StrUtil.isNotBlank(BpmnModelUtils.parseExtensionElement(task, USER_TASK_ASSIGN_USER_IDS))) {
                throw invalid("流程节点含尚未映射的审批人配置：" + key + "/" + task.getId());
            }
        }
        return tasks;
    }

    private void setCandidateParam(UserTask task, String value) {
        task.getAttributes().remove(USER_TASK_CANDIDATE_PARAM);
        task.getExtensionElements().remove(USER_TASK_CANDIDATE_PARAM);
        BpmnModelUtils.addExtensionElement(task, USER_TASK_CANDIDATE_PARAM, value);
    }

    private Set<String> requiredKeys(List<BusinessApprovalPolicyTransferVO.Item> policies) {
        Set<String> result = new LinkedHashSet<>();
        for (var item : policies) {
            if (!"BPM_REQUIRED".equals(item.getPolicyMode()) || !"PUBLISHED".equals(item.getStatus())) continue;
            if (StrUtil.isBlank(item.getProcessDefinitionKey())) throw invalid("审批策略缺少关联流程：" + item.getObjectType());
            result.add(item.getProcessDefinitionKey());
        }
        return result;
    }

    private String tenant() { return TenantContextHolder.getRequiredTenantId().toString(); }
    private ProcessDefinition latest(String key) {
        return repositoryService.createProcessDefinitionQuery().processDefinitionKey(key)
                .processDefinitionTenantId(tenant()).latestVersion().singleResult();
    }
    private BusinessApprovalException invalid(String message) {
        return new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID, message + "，整批操作已取消");
    }
}
