package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import java.nio.file.Path;
import java.time.Duration;

public interface RuntimeControlCommandExecutor {

    RuntimeControlStatusResult queryStatus(RuntimeControlCommand command);

    String executeForOutput(RuntimeControlCommand command, Duration timeout);

    void restart(RuntimeControlCommand command);

    void executeOperation(RuntimeControlCommand command, Path logPath);

    void executeDetachedOperation(RuntimeControlCommand command, Path logPath, String operationId, String successSummary);
}
