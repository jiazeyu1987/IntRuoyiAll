package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchFullConfigImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchManualReplanDataImportRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.PostConfigPackageService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleConfigPackageService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_CONTENT_INVALID;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_FILE_EMPTY;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_FORMAT_UNSUPPORTED;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_REFERENCE_MISSING;

@Service
public class MesProSchedulerWorkbenchFullConfigPackageServiceImpl
        implements MesProSchedulerWorkbenchFullConfigPackageService {

    private static final String PACKAGE_VERSION = "scheduler-workbench-full-config.v1";

    @Resource
    private PostConfigPackageService postConfigPackageService;
    @Resource
    private RoleConfigPackageService roleConfigPackageService;
    @Resource
    private MesProSchedulerWorkbenchRouteConfigPackageService routeConfigPackageService;
    @Resource
    private MesProSchedulerWorkbenchManualReplanDataPackageService manualReplanDataPackageService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleService roleService;
    @Resource
    private AdminUserService adminUserService;

    @Override
    public byte[] exportPackage() {
        byte[] postBytes = postConfigPackageService.exportPackage();
        byte[] roleBytes = roleConfigPackageService.exportPackage();
        byte[] routeBytes = routeConfigPackageService.exportPackage();
        byte[] manualReplanDataBytes = manualReplanDataPackageService.exportPackage();
        FullConfigPackage payload = new FullConfigPackage();
        payload.setPackageVersion(PACKAGE_VERSION);
        payload.setPostConfigPackageBase64(encode(postBytes));
        payload.setRoleConfigPackageBase64(encode(roleBytes));
        payload.setRouteConfigPackageBase64(encode(routeBytes));
        payload.setManualReplanDataPackageBase64(encode(manualReplanDataBytes));
        payload.setPostConfigPackage(JsonUtils.parseObject(postBytes, Object.class));
        payload.setRoleConfigPackage(JsonUtils.parseObject(roleBytes, Object.class));
        payload.setRouteConfigPackage(JsonUtils.parseObject(routeBytes, Object.class));
        payload.setManualReplanDataPackage(JsonUtils.parseObject(manualReplanDataBytes, Object.class));
        payload.setUserRoleBindings(buildUserRoleBindings());
        return JsonUtils.toJsonByte(payload);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProSchedulerWorkbenchFullConfigImportRespVO importPackage(byte[] content) {
        FullConfigPackage payload = parsePayload(content);
        validatePayload(payload);
        postConfigPackageService.importPackage(decode(payload.getPostConfigPackageBase64(),
                payload.getPostConfigPackage(), "岗位配置包"));
        roleConfigPackageService.importPackage(decode(payload.getRoleConfigPackageBase64(),
                payload.getRoleConfigPackage(), "角色配置包"));
        MesProSchedulerWorkbenchManualReplanDataImportRespVO manualReplanDataResult =
                manualReplanDataPackageService.importPackage(decode(payload.getManualReplanDataPackageBase64(),
                        payload.getManualReplanDataPackage(), "手动重排数据包"));
        routeConfigPackageService.importPackage(decode(payload.getRouteConfigPackageBase64(),
                payload.getRouteConfigPackage(), "排产路线配置包"));
        MesProSchedulerWorkbenchFullConfigImportRespVO result = replayUserRoles(payload.getUserRoleBindings());
        result.setReplanMasterDataCount(manualReplanDataResult.getMasterDataCount());
        result.setReplanScheduleOrderDataCount(manualReplanDataResult.getScheduleOrderDataCount());
        result.setReplanRuntimeDataCount(manualReplanDataResult.getRuntimeDataCount());
        return result;
    }

    private List<UserRoleBindingItem> buildUserRoleBindings() {
        List<RoleDO> roles = roleService.getRoleList();
        if (CollUtil.isEmpty(roles)) {
            return List.of();
        }
        Map<Long, RoleDO> roleMap = convertMap(roles, RoleDO::getId);
        Set<Long> userIds = permissionService.getUserRoleIdListByRoleId(roleMap.keySet());
        List<AdminUserDO> users = adminUserService.getUserList(userIds);
        return users.stream()
                .sorted(Comparator.comparing(AdminUserDO::getUsername))
                .map(user -> toBindingItem(user, roleMap))
                .filter(item -> CollUtil.isNotEmpty(item.getRoleCodes()))
                .toList();
    }

    private UserRoleBindingItem toBindingItem(AdminUserDO user, Map<Long, RoleDO> roleMap) {
        UserRoleBindingItem item = new UserRoleBindingItem();
        item.setUsername(user.getUsername());
        item.setNickname(user.getNickname());
        item.setRoleCodes(permissionService.getUserRoleIdListByUserId(user.getId()).stream()
                .map(roleMap::get)
                .filter(role -> role != null && StringUtils.hasText(role.getCode()))
                .sorted(Comparator.comparing(RoleDO::getCode))
                .map(RoleDO::getCode)
                .toList());
        return item;
    }

    private MesProSchedulerWorkbenchFullConfigImportRespVO replayUserRoles(List<UserRoleBindingItem> bindings) {
        Map<String, RoleDO> roleCodeMap = new HashMap<>();
        for (RoleDO role : roleService.getRoleList()) {
            if (StringUtils.hasText(role.getCode())) {
                roleCodeMap.put(role.getCode(), role);
            }
        }
        int assignedRoleCount = 0;
        for (UserRoleBindingItem item : bindings) {
            validateBinding(item);
            AdminUserDO user = adminUserService.getUserByUsername(item.getUsername());
            if (user == null) {
                throw exception(CONFIG_PACKAGE_REFERENCE_MISSING,
                        "用户【" + item.getUsername() + "】在目标环境不存在");
            }
            Set<Long> roleIds = new HashSet<>();
            for (String roleCode : item.getRoleCodes()) {
                RoleDO role = roleCodeMap.get(roleCode);
                if (role == null) {
                    throw exception(CONFIG_PACKAGE_REFERENCE_MISSING,
                            "角色编码【" + roleCode + "】在目标环境不存在，无法为用户【"
                                    + item.getUsername() + "】回放角色");
                }
                roleIds.add(role.getId());
            }
            permissionService.assignUserRole(user.getId(), roleIds);
            assignedRoleCount += roleIds.size();
        }
        MesProSchedulerWorkbenchFullConfigImportRespVO respVO = new MesProSchedulerWorkbenchFullConfigImportRespVO();
        respVO.setUserRoleBindingCount(bindings.size());
        respVO.setAssignedRoleCount(assignedRoleCount);
        return respVO;
    }

    private FullConfigPackage parsePayload(byte[] content) {
        if (content == null || content.length == 0) {
            throw exception(CONFIG_PACKAGE_FILE_EMPTY);
        }
        try {
            return JsonUtils.parseObject(content, FullConfigPackage.class);
        } catch (RuntimeException ex) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "排产员工作台全量配置包 JSON 非法");
        }
    }

    private void validatePayload(FullConfigPackage payload) {
        if (payload == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "排产员工作台全量配置包 JSON 非法");
        }
        if (!PACKAGE_VERSION.equals(payload.getPackageVersion())) {
            throw exception(CONFIG_PACKAGE_FORMAT_UNSUPPORTED, payload.getPackageVersion());
        }
        if (!StringUtils.hasText(payload.getPostConfigPackageBase64()) && payload.getPostConfigPackage() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "排产员工作台全量配置包缺少岗位配置包");
        }
        if (!StringUtils.hasText(payload.getRoleConfigPackageBase64()) && payload.getRoleConfigPackage() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "排产员工作台全量配置包缺少角色配置包");
        }
        if (!StringUtils.hasText(payload.getRouteConfigPackageBase64()) && payload.getRouteConfigPackage() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "排产员工作台全量配置包缺少排产路线配置包");
        }
        if (!StringUtils.hasText(payload.getManualReplanDataPackageBase64())
                && payload.getManualReplanDataPackage() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "排产员工作台全量配置包缺少手动重排数据包");
        }
        if (payload.getUserRoleBindings() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "排产员工作台全量配置包缺少 userRoleBindings");
        }
    }

    private void validateBinding(UserRoleBindingItem item) {
        if (item == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "排产员工作台全量配置包包含空用户角色绑定项");
        }
        if (!StringUtils.hasText(item.getUsername())) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "排产员工作台全量配置包中的用户角色绑定缺少 username");
        }
        if (item.getRoleCodes() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID,
                    "排产员工作台全量配置包中的用户【" + item.getUsername() + "】缺少 roleCodes");
        }
    }

    private String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private byte[] decode(String base64, Object fallbackObject, String packageName) {
        if (StringUtils.hasText(base64)) {
            try {
                return Base64.getDecoder().decode(base64);
            } catch (IllegalArgumentException ex) {
                throw exception(CONFIG_PACKAGE_CONTENT_INVALID,
                        "排产员工作台全量配置包中的" + packageName + " Base64 非法");
            }
        }
        return JsonUtils.toJsonByte(fallbackObject);
    }

    @Data
    public static class FullConfigPackage {
        private String packageVersion;
        private String postConfigPackageBase64;
        private String roleConfigPackageBase64;
        private String routeConfigPackageBase64;
        private String manualReplanDataPackageBase64;
        private Object postConfigPackage;
        private Object roleConfigPackage;
        private Object routeConfigPackage;
        private Object manualReplanDataPackage;
        private List<UserRoleBindingItem> userRoleBindings;
    }

    @Data
    public static class UserRoleBindingItem {
        private String username;
        private String nickname;
        private List<String> roleCodes = new ArrayList<>();
    }
}
