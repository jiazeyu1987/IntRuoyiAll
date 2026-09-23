package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol;

import java.util.function.Supplier;
import java.util.regex.Pattern;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID;

/** Maps only controlled release error identifiers; never exposes raw command output or credentials. */
public final class ReleaseWorkflowApiErrors {
    private static final Pattern CODE = Pattern.compile("^(?:RELEASE_WORKFLOW|BACKUP_PUBLICATION|SOURCE|DEPENDENCY|RECOVERY|RUNTIME_RESOURCE|RESTORE_ISOLATION)_[A-Z0-9_]{2,100}(?=[:\\s]|$)");
    private static final Pattern CONFIG = Pattern.compile("^yudao\\.runtime-control\\.[a-z0-9.-]{1,160}(?=\\s|$)");
    private ReleaseWorkflowApiErrors() { }
    public static <T> T call(Supplier<T> action) {
        try { return action.get(); }
        catch (RuntimeException error) {
            String message = String.valueOf(error.getMessage());
            var code = CODE.matcher(message);
            if (code.find()) throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, code.group());
            var config = CONFIG.matcher(message);
            if (config.find()) throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    "RELEASE_WORKFLOW_CONFIGURATION_INVALID: " + config.group());
            throw error;
        }
    }
}
