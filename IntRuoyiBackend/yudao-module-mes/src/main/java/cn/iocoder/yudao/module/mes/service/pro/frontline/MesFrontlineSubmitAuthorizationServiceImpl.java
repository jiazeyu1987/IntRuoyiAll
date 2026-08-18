package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SESSION_SNAPSHOT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SIGNATURE_EMPLOYEE_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_MISMATCH;

@Service
public class MesFrontlineSubmitAuthorizationServiceImpl implements MesFrontlineSubmitAuthorizationService {

    private final MesFrontlineDeviceAccountContextService contextService;
    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesFrontlineSessionSnapshotService sessionSnapshotService;

    public MesFrontlineSubmitAuthorizationServiceImpl(MesFrontlineDeviceAccountContextService contextService,
                                                      MesProcessPoolActiveOrderMapper activeOrderMapper,
                                                      MesFrontlineSessionSnapshotService sessionSnapshotService) {
        this.contextService = contextService;
        this.activeOrderMapper = activeOrderMapper;
        this.sessionSnapshotService = sessionSnapshotService;
    }

    @Override
    public void authorizeActiveOrder(Long loginUserId, Long activeOrderId, Long workOrderId, Long routeId,
                                     Long routeProcessId, Long processId) {
        requireValue(activeOrderId, "activeOrderId");
        requireValue(workOrderId, "workOrderId");
        requireValue(routeId, "routeId");
        requireValue(routeProcessId, "routeProcessId");
        requireValue(processId, "processId");
        Long leaderUserId = contextService.resolveResponsibleLeaderUserId(loginUserId);
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectByIdForUpdate(activeOrderId);
        if (activeOrder == null || !"ACTIVE".equals(activeOrder.getActiveStatus())
                || !Objects.equals(leaderUserId, activeOrder.getLeaderUserId())
                || !Objects.equals(workOrderId, activeOrder.getWorkOrderId())
                || !Objects.equals(routeId, activeOrder.getRouteId())) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "activeOrder");
        }
    }

    @Override
    public MesFrontlineSubmitIdentityTrace authorize(MesFrontlineSubmitIdentityCommand command) {
        requireCommand(command);
        if (!Objects.equals(command.actualEmployeeId(), command.signatureEmployeeId())) {
            throw exception(PRO_FRONTLINE_SIGNATURE_EMPLOYEE_MISMATCH,
                    command.actualEmployeeId(), command.signatureEmployeeId());
        }
        MesFrontlineSessionSnapshot snapshot = sessionSnapshotService.require(command.frontlineSessionSnapshotId(),
                command.frontlineSessionSnapshotHash(), command.loginUserId());
        MesFrontlineSessionSnapshotContent content = snapshot.content();
        if (!Objects.equals(command.routeId(), content.routeId())
                || !Objects.equals(command.routeProcessId(), content.routeProcessId())
                || !Objects.equals(command.processId(), content.processId())
                || !Objects.equals(command.workstationId(), content.workstationId())) {
            throw exception(PRO_FRONTLINE_SESSION_SNAPSHOT_INVALID, "process context");
        }
        MesFrontlineEmployeeSwitchResult employeeSnapshot = requireEmployeeSnapshot(
                content.employeeSwitchSnapshots(), command.actualEmployeeId());
        MesFrontlineTemplateDescriptor template = employeeSnapshot.template();
        if (!Objects.equals(command.templateNo(), template.templateNo())) {
            throw exception(PRO_FRONTLINE_TEMPLATE_MISMATCH, command.templateNo());
        }
        if (command.deviceId() != null && content.devices().stream()
                .noneMatch(device -> Objects.equals(device.deviceId(), command.deviceId()))) {
            throw exception(PRO_FRONTLINE_SESSION_SNAPSHOT_INVALID, "deviceId=" + command.deviceId());
        }
        return new MesFrontlineSubmitIdentityTrace(command.loginUserId(), command.actualEmployeeId(),
                command.signatureEmployeeId(), command.deviceId(), command.workstationId(), content.routeId(),
                content.routeProcessId(), content.processId(), command.templateNo(), snapshot.snapshotId(),
                snapshot.snapshotHash(), snapshot);
    }

    private static MesFrontlineEmployeeSwitchResult requireEmployeeSnapshot(
            List<MesFrontlineEmployeeSwitchResult> employeeSnapshots, Long actualEmployeeId) {
        if (employeeSnapshots == null) {
            throw exception(PRO_FRONTLINE_SESSION_SNAPSHOT_INVALID, "employee snapshots");
        }
        return employeeSnapshots.stream()
                .filter(item -> item != null && Objects.equals(item.actualEmployeeId(), actualEmployeeId))
                .findFirst()
                .orElseThrow(() -> exception(PRO_FRONTLINE_SESSION_SNAPSHOT_INVALID,
                        "actualEmployeeId=" + actualEmployeeId));
    }

    private static void requireCommand(MesFrontlineSubmitIdentityCommand command) {
        if (command == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "submitIdentityCommand");
        }
        requireValue(command.loginUserId(), "loginUserId");
        requireValue(command.actualEmployeeId(), "actualEmployeeId");
        requireValue(command.signatureEmployeeId(), "signatureEmployeeId");
        requireValue(command.workstationId(), "workstationId");
        requireValue(command.routeId(), "routeId");
        requireValue(command.routeProcessId(), "routeProcessId");
        requireValue(command.processId(), "processId");
        if (StrUtil.isBlank(command.templateNo())) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "templateNo");
        }
        if (StrUtil.isBlank(command.frontlineSessionSnapshotId())) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "frontlineSessionSnapshotId");
        }
        if (StrUtil.isBlank(command.frontlineSessionSnapshotHash())) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "frontlineSessionSnapshotHash");
        }
    }

    private static void requireValue(Object value, String fieldName) {
        if (value == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, fieldName);
        }
    }

}
