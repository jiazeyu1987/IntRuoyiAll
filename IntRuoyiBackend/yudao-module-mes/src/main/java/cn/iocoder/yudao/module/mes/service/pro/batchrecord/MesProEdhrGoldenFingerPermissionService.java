package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * eDHR 金手指是临时测试权限，必须同时满足显式权限码和显式启用角色。
 */
@Service
public class MesProEdhrGoldenFingerPermissionService {

    public static final String PERMISSION = "mes:pro-batch-record-execution:golden-finger";
    public static final String ROLE_CODE = "edhr_golden_finger_admin";

    @Resource
    private PermissionApi permissionApi;
    @Resource
    private RoleApi roleApi;

    public boolean hasGoldenFingerPermission(Long userId) {
        if (userId == null || userId <= 0) {
            return false;
        }
        Set<Long> roleIds = permissionApi.getUserRoleIdListByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return false;
        }
        List<RoleRespDTO> roles = roleApi.getRoleList(roleIds);
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        Set<Long> goldenFingerRoleIds = roles.stream()
                .filter(role -> role != null
                        && Objects.equals(ROLE_CODE, role.getCode())
                        && Objects.equals(CommonStatusEnum.ENABLE.getStatus(), role.getStatus()))
                .map(RoleRespDTO::getId)
                .collect(Collectors.toSet());
        return !goldenFingerRoleIds.isEmpty()
                && permissionApi.hasAnyPermissionsInRoles(goldenFingerRoleIds, PERMISSION);
    }
}
