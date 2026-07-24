package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionErrorCodeConstants.PRO_EDHR_PERMISSION_CONTEXT_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionErrorCodeConstants.PRO_EDHR_OBJECT_PERMISSION_DENIED;

@Service
public class MesProEdhrPermissionGateServiceImpl implements MesProEdhrPermissionGateService {

    @Resource
    private MesProEdhrPermissionScopeService permissionScopeService;

    @Override
    public void requireAbility(MesProEdhrPermissionGateCommand command) {
        if (command == null || StrUtil.isBlank(command.getAbility())) {
            throw exception(PRO_EDHR_PERMISSION_CONTEXT_MISSING);
        }
        MesProEdhrPermissionEvaluateResult result = permissionScopeService.evaluate(
                new MesProEdhrPermissionEvaluateCommand()
                        .setScopeId(command.getScopeId())
                        .setObjectType(command.getObjectType())
                        .setObjectId(command.getObjectId())
                        .setBatchExecutionId(command.getBatchExecutionId())
                        .setExecutionId(command.getExecutionId())
                        .setWorkTaskId(command.getWorkTaskId())
                        .setRouteId(command.getRouteId())
                        .setRouteProcessId(command.getRouteProcessId())
                        .setReportId(command.getReportId())
                        .setRecordCategory(command.getRecordCategory())
                        .setAbilities(List.of(command.getAbility()))
                        .setActorUserId(command.getActorUserId())
                        .setActorUsername(command.getActorUsername())
                        .setActorDeptId(command.getActorDeptId())
                        .setPermissionCode(command.getPermissionCode())
                        .setActionName(command.getActionName()));
        if (!"ALLOW".equals(result.getDecisions().get(command.getAbility()))) {
            throw exception(PRO_EDHR_OBJECT_PERMISSION_DENIED,
                    result.getObjectType() + ":" + result.getObjectId() + ":" + command.getAbility());
        }
    }
}
