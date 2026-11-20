package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.RoleApprovalConfigPackageVO;
import cn.iocoder.yudao.module.system.service.permission.RoleConfigPackageService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class RoleApprovalConfigPackageService {
    @Resource private RoleConfigPackageService roleConfigPackageService;
    @Resource private BusinessApprovalPolicyTransferService policyTransferService;

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public byte[] exportPackage() {
        var transfer = new RoleApprovalConfigPackageVO();
        transfer.setSchemaVersion(1);
        transfer.setFormat("ROLE_APPROVAL_CONFIGURATION");
        transfer.setRoleConfig(JsonUtils.parseObject(roleConfigPackageService.exportAllPackage(), JsonNode.class));
        transfer.setApprovalPolicies(policyTransferService.exportPolicies());
        validate(transfer);
        return JsonUtils.toJsonByte(transfer);
    }

    @Transactional(rollbackFor = Exception.class)
    public int importPackage(byte[] content) {
        if (content == null || content.length == 0) throw invalid("请选择非空的权限角色与审批策略配置包");
        RoleApprovalConfigPackageVO transfer;
        try {
            transfer = JsonUtils.parseObject(content, RoleApprovalConfigPackageVO.class);
        } catch (RuntimeException exception) {
            throw invalid("配置包格式不正确，请在权限角色页面重新导出完整配置包");
        }
        validate(transfer);
        // The outer transaction covers grants, Flowable deployment and policy writes together.
        roleConfigPackageService.importPackage(JsonUtils.toJsonByte(transfer.getRoleConfig()));
        return policyTransferService.importPolicies(transfer.getApprovalPolicies());
    }

    private void validate(RoleApprovalConfigPackageVO transfer) {
        if (transfer == null) throw invalid("配置包为空，请在权限角色页面重新导出完整配置包");
        ValidationUtils.validate(transfer);
        JsonNode roleConfig = transfer.getRoleConfig();
        if (!roleConfig.isObject() || !"4".equals(roleConfig.path("packageVersion").asText())
                || !roleConfig.path("roles").isArray() || roleConfig.path("roles").isEmpty()
                || !roleConfig.path("categories").isArray() || roleConfig.path("categories").isEmpty()) {
            throw invalid("配置包缺少版本4的完整角色授权，请在权限角色页面重新导出");
        }
        Map<String, JsonNode> roles = new HashMap<>();
        for (JsonNode role : roleConfig.path("roles")) {
            String code = role.path("code").asText();
            if (code.isBlank() || roles.putIfAbsent(code, role) != null) {
                throw invalid("配置包存在空白或重复角色编码：" + code);
            }
        }
        for (var process : transfer.getApprovalPolicies().getProcesses()) {
            for (var dependency : process.getRoles()) {
                JsonNode role = roles.get(dependency.getCode());
                if (role == null || !role.path("status").isIntegralNumber() || role.path("status").intValue() != 0
                        || !dependency.getCategory().getCode().equals(role.path("categoryCode").asText())) {
                    throw invalid("流程【" + process.getKey() + "】引用的角色【" + dependency.getCode()
                            + "】缺少完整启用授权或分类不一致，整包未导入");
                }
            }
        }
    }

    private BusinessApprovalException invalid(String message) {
        return new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_CONTEXT_INVALID, message);
    }
}
