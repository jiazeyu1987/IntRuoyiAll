package cn.iocoder.yudao.module.mes.service.pro.frontline;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_ACTUAL_EMPLOYEE_NOT_IN_TEAM;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED;

@Service
public class MesFrontlineEmployeeSwitchServiceImpl implements MesFrontlineEmployeeSwitchService {

    private final MesFrontlineTemplateResolver templateResolver;
    private final MesFrontlineRuntimeConfigService runtimeConfigService;

    public MesFrontlineEmployeeSwitchServiceImpl(MesFrontlineTemplateResolver templateResolver,
                                                 MesFrontlineRuntimeConfigService runtimeConfigService) {
        this.templateResolver = templateResolver;
        this.runtimeConfigService = runtimeConfigService;
    }

    @Override
    public MesFrontlineEmployeeSwitchResult switchActualEmployee(MesFrontlineEmployeeSwitchCommand command) {
        if (command == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "employeeSwitchCommand");
        }
        MesFrontlineRuntimeConfig runtimeConfig = runtimeConfigService.getRuntimeConfig(command.loginUserId(),
                command.activeOrderId(), command.routeId(), command.routeProcessId(), command.processId());
        requireRuntimeEmployee(runtimeConfig.employees(), command.actualEmployeeId(), runtimeConfig.processId());
        MesFrontlineTemplateDescriptor template = requireEmployeeSwitchTemplate(runtimeConfig.employeeSwitchSnapshots(),
                command.actualEmployeeId(), runtimeConfig.processId());
        return new MesFrontlineEmployeeSwitchResult(command.loginUserId(), command.actualEmployeeId(),
                runtimeConfig.routeId(), runtimeConfig.routeProcessId(), runtimeConfig.processId(), false, template);
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

    private static MesFrontlineTemplateDescriptor requireEmployeeSwitchTemplate(
            List<MesFrontlineEmployeeSwitchResult> employeeSwitchSnapshots,
            Long actualEmployeeId,
            Long processId) {
        if (employeeSwitchSnapshots == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "employeeSwitchSnapshots");
        }
        return employeeSwitchSnapshots.stream()
                .filter(snapshot -> snapshot != null
                        && Objects.equals(snapshot.actualEmployeeId(), actualEmployeeId)
                        && snapshot.template() != null)
                .map(MesFrontlineEmployeeSwitchResult::template)
                .findFirst()
                .orElseThrow(() -> exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED,
                        "employeeSwitchSnapshot.actualEmployeeId=" + actualEmployeeId + ", processId=" + processId));
    }

}
