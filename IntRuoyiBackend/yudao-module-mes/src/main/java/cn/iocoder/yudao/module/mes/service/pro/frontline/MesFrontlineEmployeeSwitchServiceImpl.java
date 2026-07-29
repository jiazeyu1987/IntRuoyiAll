package cn.iocoder.yudao.module.mes.service.pro.frontline;

import org.springframework.stereotype.Service;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED;

@Service
public class MesFrontlineEmployeeSwitchServiceImpl implements MesFrontlineEmployeeSwitchService {

    private final MesFrontlineDeviceAccountContextService contextService;
    private final MesFrontlineTemplateResolver templateResolver;

    public MesFrontlineEmployeeSwitchServiceImpl(MesFrontlineDeviceAccountContextService contextService,
                                                 MesFrontlineTemplateResolver templateResolver) {
        this.contextService = contextService;
        this.templateResolver = templateResolver;
    }

    @Override
    public MesFrontlineEmployeeSwitchResult switchActualEmployee(MesFrontlineEmployeeSwitchCommand command) {
        if (command == null) {
            throw exception(PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED, "employeeSwitchCommand");
        }
        MesFrontlineRouteProcessCandidate process = contextService.requireAuthorizedProcess(command.loginUserId(),
                command.routeId(), command.routeProcessId(), command.processId());
        contextService.requireBoundEmployee(command.loginUserId(), process.routeId(), process.routeProcessId(),
                process.processId(), command.actualEmployeeId());
        MesFrontlineTemplateDescriptor template = templateResolver.resolve(new MesFrontlineTemplateRequest(
                command.loginUserId(), command.actualEmployeeId(), process.routeId(),
                process.routeProcessId(), process.processId()));
        return new MesFrontlineEmployeeSwitchResult(command.loginUserId(), command.actualEmployeeId(),
                process.routeId(), process.routeProcessId(), process.processId(), false, template);
    }

}
