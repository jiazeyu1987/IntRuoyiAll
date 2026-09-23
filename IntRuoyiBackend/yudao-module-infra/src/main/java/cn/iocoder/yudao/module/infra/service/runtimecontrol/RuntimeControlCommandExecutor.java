package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import java.nio.file.Path;
import java.time.Duration;

public interface RuntimeControlCommandExecutor {

    RuntimeControlStatusResult queryStatus(RuntimeControlCommand command);

    String executeForOutput(RuntimeControlCommand command, Duration timeout);

    void restart(RuntimeControlCommand command);

    void executeOperation(RuntimeControlCommand command, Path logPath);

    boolean isOperationExecutorAlive(String operationId);

    default void registerOperation(String operationId, Path logPath) {
        // Implementations that can terminate a running process override this hook.
    }

    default boolean cancelOperation(String operationId) {
        throw new IllegalStateException("RUNTIME_CONTROL_OPERATION_CANCELLATION_UNAVAILABLE");
    }

    void executeDetachedOperation(RuntimeControlCommand command, Path logPath, String operationId, String successSummary);
}
