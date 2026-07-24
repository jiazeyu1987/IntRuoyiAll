package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_OPERATION_STORE_FAILED;

@Component
public class RuntimeControlOperationStore {

    private final RuntimeControlProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public RuntimeControlOperationStore(RuntimeControlProperties properties) {
        this.properties = properties;
    }

    public Path getOperationPath(String operationId) {
        return stateDir().resolve(operationId + ".json");
    }

    public Path getOperationLogPath(String operationId) {
        return stateDir().resolve("logs").resolve(operationId + ".log");
    }

    public void initializeLog(Path logPath) {
        try {
            Files.createDirectories(logPath.getParent());
            if (!Files.exists(logPath)) {
                Files.createFile(logPath);
            }
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    public void save(RuntimeControlOperationRespVO operation) {
        try {
            Files.createDirectories(stateDir());
            Path operationPath = getOperationPath(operation.getOperationId());
            Path tmpPath = stateDir().resolve(operation.getOperationId() + ".json.tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmpPath.toFile(), operation);
            Files.move(tmpPath, operationPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    public List<RuntimeControlOperationRespVO> listLatest(int limit) {
        try {
            if (!Files.isDirectory(stateDir())) {
                return List.of();
            }
            return Files.list(stateDir())
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing((Path path) -> path.toFile().lastModified()).reversed())
                    .limit(limit)
                    .map(this::read)
                    .toList();
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    public RuntimeControlOperationRespVO findLatest(String environment, String component) {
        return listLatest(200).stream()
                .filter(operation -> environment.equals(operation.getEnvironment())
                        && component.equals(operation.getComponent()))
                .findFirst()
                .orElse(null);
    }

    public RuntimeControlOperationRespVO findById(String operationId) {
        Path path = getOperationPath(operationId);
        return Files.isRegularFile(path) ? read(path) : null;
    }

    public Path getStateDir() {
        return stateDir();
    }

    public void updateStatus(String operationId, String status, String summary) {
        RuntimeControlOperationRespVO operation = findById(operationId);
        if (operation == null) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, "operation not found: " + operationId);
        }
        operation.setStatus(status);
        operation.setSummary(summary);
        save(operation);
    }

    private RuntimeControlOperationRespVO read(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), RuntimeControlOperationRespVO.class);
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    private Path stateDir() {
        return Path.of(properties.getStateDir()).normalize();
    }
}
