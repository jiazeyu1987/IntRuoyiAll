package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SUBMIT_DEVICE_CONTEXT_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_SIGNATURE_EMPLOYEE_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_MISMATCH;

@Service
public class MesFrontlineSubmitAuthorizationServiceImpl implements MesFrontlineSubmitAuthorizationService {

    private final MesFrontlineDeviceAccountContextService contextService;
    private final MesFrontlineTemplateResolver templateResolver;

    public MesFrontlineSubmitAuthorizationServiceImpl(MesFrontlineDeviceAccountContextService contextService,
                                                      MesFrontlineTemplateResolver templateResolver) {
        this.contextService = contextService;
        this.templateResolver = templateResolver;
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
        if (!Objects.equals(command.deviceId(), process.deviceId())
                || !Objects.equals(command.workstationId(), process.workstationId())) {
            throw exception(PRO_FRONTLINE_SUBMIT_DEVICE_CONTEXT_MISMATCH,
                    command.deviceId(), command.workstationId(), process.deviceId(), process.workstationId());
        }
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
