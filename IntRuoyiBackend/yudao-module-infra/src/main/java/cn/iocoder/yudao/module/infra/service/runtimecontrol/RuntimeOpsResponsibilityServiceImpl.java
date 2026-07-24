package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOwnerMatrixRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOwnerMatrixSaveReqVO;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED;

@Service
public class RuntimeOpsResponsibilityServiceImpl implements RuntimeOpsResponsibilityService {

    private static final Long DEFAULT_RELEASE_OWNER_USER_ID = 1L;
    private static final String DEFAULT_RELEASE_OWNER_NAME = "admin";
    private static final List<RuntimeControlOwnerMatrixRespVO> DEFAULT_RELEASE_OWNERS = List.of(
            defaultOwner(-1001L, "prod", "promote-prod", "release-owner", "默认发布责任人"),
            defaultOwner(-1002L, "backup", "promote-backup", "release-owner", "默认发布责任人"),
            defaultOwner(-1003L, "prod", "rollback-app", "release-owner", "默认发布责任人"),
            defaultOwner(-1004L, "test", "rollback-app", "release-owner", "默认回滚发布责任人"),
            defaultOwner(-1005L, "backup", "rollback-app", "release-owner", "默认回滚发布责任人"),
            defaultOwner(-1006L, "test", "restore-data", "data-owner", "默认恢复数据责任人"),
            defaultOwner(-1007L, "backup", "restore-data", "data-owner", "默认恢复数据责任人"),
            defaultOwner(-1008L, "local", "storage-capacity-warning", "ops-owner", "默认容量告警责任人"),
            defaultOwner(-1009L, "test", "storage-capacity-warning", "ops-owner", "默认容量告警责任人"),
            defaultOwner(-1010L, "backup", "storage-capacity-warning", "ops-owner", "默认容量告警责任人"),
            defaultOwner(-1011L, "prod", "storage-capacity-warning", "ops-owner", "默认容量告警责任人"),
            defaultOwner(-1012L, "prod", "backup-now", "ops-owner", "默认备份责任人"),
            defaultOwner(-1013L, "test", "backup-now", "ops-owner", "默认备份责任人"),
            defaultOwner(-1014L, "backup", "backup-now", "ops-owner", "默认备份责任人"),
            defaultOwner(-1015L, "prod", "backup-scheduled", "ops-owner", "默认定时备份责任人"),
            defaultOwner(-1016L, "test", "rehearsal", "ops-owner", "默认恢复演练责任人"),
            defaultOwner(-1017L, "backup", "rehearsal", "ops-owner", "默认恢复演练责任人"),
            defaultOwner(-1018L, "test", "restore-data-started", "data-owner", "默认恢复数据责任人"),
            defaultOwner(-1019L, "backup", "restore-data-started", "data-owner", "默认恢复数据责任人"),
            defaultOwner(-1020L, "test", "restore-data-finished", "data-owner", "默认恢复数据责任人"),
            defaultOwner(-1021L, "backup", "restore-data-finished", "data-owner", "默认恢复数据责任人")
    );

    private final RuntimeOpsOwnerMatrixStore ownerMatrixStore;

    public RuntimeOpsResponsibilityServiceImpl(RuntimeOpsOwnerMatrixStore ownerMatrixStore) {
        this.ownerMatrixStore = ownerMatrixStore;
    }

    @Override
    public List<RuntimeControlOwnerMatrixRespVO> getOwnerMatrix(String environment, String action) {
        return effectiveOwnerMatrix().stream()
                .filter(owner -> StrUtil.isBlank(environment) || environment.equals(owner.getEnvironment()))
                .filter(owner -> StrUtil.isBlank(action) || action.equals(owner.getAction()))
                .toList();
    }

    @Override
    public RuntimeControlOwnerMatrixRespVO createOwner(RuntimeControlOwnerMatrixSaveReqVO reqVO) {
        validateSaveReq(reqVO);
        return ownerMatrixStore.save(copyToResp(null, reqVO));
    }

    @Override
    public RuntimeControlOwnerMatrixRespVO updateOwner(Long id, RuntimeControlOwnerMatrixSaveReqVO reqVO) {
        validateSaveReq(reqVO);
        if (id == null || ownerMatrixStore.findById(id) == null) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "责任人矩阵不存在：" + id);
        }
        return ownerMatrixStore.save(copyToResp(id, reqVO));
    }

    @Override
    public List<RuntimeControlOwnerMatrixRespVO> getRequiredOwners(String environment, String action) {
        return getOwnerMatrix(environment, action).stream()
                .filter(owner -> Boolean.TRUE.equals(owner.getRequired()))
                .toList();
    }

    @Override
    public String findMissingRequiredOwnerReason(String environment, String action) {
        List<RuntimeControlOwnerMatrixRespVO> requiredOwners = getRequiredOwners(environment, action);
        if (requiredOwners.isEmpty()) {
            return "缺少必填责任人矩阵：" + environment + "/" + action;
        }
        List<String> missingRoles = requiredOwners.stream()
                .filter(owner -> owner.getOwnerUserId() == null)
                .map(RuntimeControlOwnerMatrixRespVO::getRole)
                .toList();
        if (!missingRoles.isEmpty()) {
            return "必填责任人缺失：" + environment + "/" + action + "/" + String.join(",", missingRoles);
        }
        return null;
    }

    @Override
    public void validateRequiredOwners(String environment, String action) {
        String missingReason = findMissingRequiredOwnerReason(environment, action);
        if (missingReason != null) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "责任人: " + missingReason);
        }
    }

    private void validateSaveReq(RuntimeControlOwnerMatrixSaveReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getEnvironment())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "environment");
        }
        if (StrUtil.isBlank(reqVO.getAction())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "action");
        }
        if (StrUtil.isBlank(reqVO.getRole())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "role");
        }
        if (reqVO.getRequired() == null) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "required");
        }
    }

    private RuntimeControlOwnerMatrixRespVO copyToResp(Long id, RuntimeControlOwnerMatrixSaveReqVO reqVO) {
        RuntimeControlOwnerMatrixRespVO respVO = new RuntimeControlOwnerMatrixRespVO();
        respVO.setId(id);
        respVO.setEnvironment(StrUtil.trim(reqVO.getEnvironment()));
        respVO.setAction(StrUtil.trim(reqVO.getAction()));
        respVO.setRole(StrUtil.trim(reqVO.getRole()));
        respVO.setRequired(reqVO.getRequired());
        respVO.setOwnerUserId(reqVO.getOwnerUserId());
        respVO.setOwnerName(StrUtil.trim(reqVO.getOwnerName()));
        respVO.setEscalationPath(StrUtil.trim(reqVO.getEscalationPath()));
        return respVO;
    }

    private List<RuntimeControlOwnerMatrixRespVO> effectiveOwnerMatrix() {
        Map<String, RuntimeControlOwnerMatrixRespVO> owners = new LinkedHashMap<>();
        DEFAULT_RELEASE_OWNERS.forEach(owner -> owners.put(key(owner), copyOwner(owner)));
        ownerMatrixStore.list().forEach(owner -> owners.put(key(owner), mergeOwner(owners.get(key(owner)), owner)));
        return owners.values().stream()
                .sorted(Comparator
                        .comparing(RuntimeControlOwnerMatrixRespVO::getEnvironment)
                        .thenComparing(RuntimeControlOwnerMatrixRespVO::getAction)
                        .thenComparing(RuntimeControlOwnerMatrixRespVO::getRole)
                        .thenComparing(owner -> owner.getId() == null ? Long.MAX_VALUE : owner.getId()))
                .toList();
    }

    private RuntimeControlOwnerMatrixRespVO mergeOwner(RuntimeControlOwnerMatrixRespVO defaultOwner,
                                                       RuntimeControlOwnerMatrixRespVO configuredOwner) {
        RuntimeControlOwnerMatrixRespVO merged = copyOwner(configuredOwner);
        if (defaultOwner != null && DEFAULT_RELEASE_OWNER_NAME.equals(defaultOwner.getOwnerName())) {
            merged.setRequired(true);
            if (merged.getOwnerUserId() == null) {
                merged.setOwnerUserId(defaultOwner.getOwnerUserId());
            }
            if (StrUtil.isBlank(merged.getOwnerName())) {
                merged.setOwnerName(defaultOwner.getOwnerName());
            }
        }
        return merged;
    }

    private RuntimeControlOwnerMatrixRespVO copyOwner(RuntimeControlOwnerMatrixRespVO source) {
        RuntimeControlOwnerMatrixRespVO target = new RuntimeControlOwnerMatrixRespVO();
        target.setId(source.getId());
        target.setEnvironment(source.getEnvironment());
        target.setAction(source.getAction());
        target.setRole(source.getRole());
        target.setRequired(source.getRequired());
        target.setOwnerUserId(source.getOwnerUserId());
        target.setOwnerName(source.getOwnerName());
        target.setEscalationPath(source.getEscalationPath());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }

    private String key(RuntimeControlOwnerMatrixRespVO owner) {
        return owner.getEnvironment() + "/" + owner.getAction() + "/" + owner.getRole();
    }

    private static RuntimeControlOwnerMatrixRespVO defaultOwner(Long id, String environment, String action,
                                                                String role, String escalationPath) {
        RuntimeControlOwnerMatrixRespVO owner = new RuntimeControlOwnerMatrixRespVO();
        owner.setId(id);
        owner.setEnvironment(environment);
        owner.setAction(action);
        owner.setRole(role);
        owner.setRequired(true);
        owner.setOwnerUserId(DEFAULT_RELEASE_OWNER_USER_ID);
        owner.setOwnerName(DEFAULT_RELEASE_OWNER_NAME);
        owner.setEscalationPath(escalationPath);
        return owner;
    }
}
