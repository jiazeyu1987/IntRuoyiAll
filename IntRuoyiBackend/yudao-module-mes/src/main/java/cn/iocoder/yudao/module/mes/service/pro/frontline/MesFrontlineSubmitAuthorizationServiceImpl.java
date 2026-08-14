package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SIGNATURE_EMPLOYEE_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_MISMATCH;

@Service
public class MesFrontlineSubmitAuthorizationServiceImpl implements MesFrontlineSubmitAuthorizationService {

    private final MesFrontlineDeviceAccountContextService contextService;
    private final MesFrontlineTemplateResolver templateResolver;
    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;

    public MesFrontlineSubmitAuthorizationServiceImpl(MesFrontlineDeviceAccountContextService contextService,
                                                      MesFrontlineTemplateResolver templateResolver,
                                                      MesProcessPoolActiveOrderMapper activeOrderMapper,
                                                      MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper) {
        this.contextService = contextService;
        this.templateResolver = templateResolver;
        this.activeOrderMapper = activeOrderMapper;
        this.processSnapshotMapper = processSnapshotMapper;
    }

    @Override
    public void authorizeActiveOrder(Long loginUserId, Long workOrderId, Long routeId,
                                     Long routeProcessId, Long processId) {
        requireValue(workOrderId, "workOrderId");
        requireValue(routeId, "routeId");
        requireValue(routeProcessId, "routeProcessId");
        requireValue(processId, "processId");
        Long leaderUserId = contextService.resolveResponsibleLeaderUserId(loginUserId);
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper
                .selectActiveByLeaderAndWorkOrderForUpdate(leaderUserId, workOrderId);
        if (activeOrder == null || !Objects.equals(routeId, activeOrder.getRouteId())) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "activeOrder");
        }
        MesProcessPoolActiveOrderProcessSnapshotDO processSnapshot = processSnapshotMapper
                .selectByActiveOrderAndProcess(activeOrder.getId(), routeProcessId, processId);
        if (processSnapshot == null
                || !Objects.equals(workOrderId, processSnapshot.getWorkOrderId())
                || !Objects.equals(routeId, processSnapshot.getRouteId())) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "activeOrderProcess");
        }
    }

    @Override
    public MesFrontlineSubmitIdentityTrace authorize(MesFrontlineSubmitIdentityCommand command) {
        requireCommand(command);
        if (!Objects.equals(command.actualEmployeeId(), command.signatureEmployeeId())) {
            throw exception(PRO_FRONTLINE_SIGNATURE_EMPLOYEE_MISMATCH,
                    command.actualEmployeeId(), command.signatureEmployeeId());
        }
        MesFrontlineRouteProcessCandidate process = contextService.requireAuthorizedProcess(command.loginUserId(),
                command.routeId(), command.routeProcessId(), command.processId());
        contextService.requireTeamEmployee(command.loginUserId(), process.routeId(), process.routeProcessId(),
                process.processId(), command.actualEmployeeId());
        MesFrontlineTemplateDescriptor template = templateResolver.resolve(new MesFrontlineTemplateRequest(
                command.loginUserId(), command.actualEmployeeId(), process.routeId(),
                process.routeProcessId(), process.processId()));
        if (!Objects.equals(command.templateNo(), template.templateNo())) {
            throw exception(PRO_FRONTLINE_TEMPLATE_MISMATCH, command.templateNo());
        }
        return new MesFrontlineSubmitIdentityTrace(command.loginUserId(), command.actualEmployeeId(),
                command.signatureEmployeeId(), command.deviceId(), command.workstationId(), process.routeId(),
                process.routeProcessId(), process.processId(), command.templateNo());
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
    }

    private static void requireValue(Object value, String fieldName) {
        if (value == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, fieldName);
        }
    }

}
