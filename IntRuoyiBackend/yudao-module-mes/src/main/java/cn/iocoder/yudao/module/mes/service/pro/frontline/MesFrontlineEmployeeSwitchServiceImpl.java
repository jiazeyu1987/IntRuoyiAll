package cn.iocoder.yudao.module.mes.service.pro.frontline;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_ACTUAL_EMPLOYEE_NOT_IN_TEAM;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED;

@Service
public class MesFrontlineEmployeeSwitchServiceImpl implements MesFrontlineEmployeeSwitchService {

    private final MesFrontlineDeviceAccountContextService contextService;
    private final MesFrontlineTemplateResolver templateResolver;
    private final MesFrontlineRuntimeConfigService runtimeConfigService;

    public MesFrontlineEmployeeSwitchServiceImpl(MesFrontlineDeviceAccountContextService contextService,
                                                 MesFrontlineTemplateResolver templateResolver,
                                                 MesFrontlineRuntimeConfigService runtimeConfigService) {
        this.contextService = contextService;
        this.templateResolver = templateResolver;
        this.runtimeConfigService = runtimeConfigService;
    }

    @Override
    public MesFrontlineEmployeeSwitchResult switchActualEmployee(MesFrontlineEmployeeSwitchCommand command) {
        if (command == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "employeeSwitchCommand");
        }
        MesFrontlineRouteProcessCandidate process = contextService.requireAuthorizedProcess(command.loginUserId(),
                command.routeId(), command.routeProcessId(), command.processId());
        MesFrontlineRuntimeConfig runtimeConfig = runtimeConfigService.getRuntimeConfig(command.loginUserId(),
                process.routeId(), process.routeProcessId(), process.processId());
        requireRuntimeEmployee(runtimeConfig.employees(), command.actualEmployeeId(), process.processId());
        MesFrontlineTemplateDescriptor template = templateResolver.resolve(new MesFrontlineTemplateRequest(
                command.loginUserId(), command.actualEmployeeId(), process.routeId(),
                process.routeProcessId(), process.processId()));
        return new MesFrontlineEmployeeSwitchResult(command.loginUserId(), command.actualEmployeeId(),
                process.routeId(), process.routeProcessId(), process.processId(), false, template);
    }

    private static void requireRuntimeEmployee(List<MesFrontlineTeamEmployeeOption> employees,
                                               Long actualEmployeeId,
                                               Long processId) {
        boolean bound = employees != null && employees.stream()
                .anyMatch(employee -> Objects.equals(employee.systemUserId(), actualEmployeeId)
                        || Objects.equals(employee.employeeProfileId(), actualEmployeeId));
        if (!bound) {
            throw exception(PRO_FRONTLINE_ACTUAL_EMPLOYEE_NOT_IN_TEAM, actualEmployeeId, processId);
        }
    }

}
