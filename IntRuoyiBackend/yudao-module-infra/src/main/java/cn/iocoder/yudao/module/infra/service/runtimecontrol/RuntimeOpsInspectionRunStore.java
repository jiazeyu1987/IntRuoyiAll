package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlInspectionRunRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_OPERATION_STORE_FAILED;

@Component
public class RuntimeOpsInspectionRunStore {

    private static final String FILE_NAME = "inspection-runs.json";

    private final RuntimeControlProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public RuntimeOpsInspectionRunStore(RuntimeControlProperties properties) {
        this.properties = properties;
    }

    public synchronized RuntimeControlInspectionRunRespVO save(RuntimeControlInspectionRunRespVO run) {
        List<RuntimeControlInspectionRunRespVO> runs = new ArrayList<>(readAll());
        if (run.getId() == null) {
            run.setId(nextId(runs));
        }
        runs.removeIf(item -> run.getId().equals(item.getId()));
        runs.add(run);
        writeAll(runs);
        return run;
    }

    public synchronized RuntimeControlInspectionRunRespVO get(Long id) {
        return readAll().stream()
                .filter(run -> id.equals(run.getId()))
                .findFirst()
                .orElseThrow(() -> exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "巡检报告不存在：" + id));
    }

    private Long nextId(List<RuntimeControlInspectionRunRespVO> runs) {
        return runs.stream()
                .map(RuntimeControlInspectionRunRespVO::getId)
                .max(Comparator.naturalOrder())
                .orElse(0L) + 1;
    }

    private List<RuntimeControlInspectionRunRespVO> readAll() {
        Path path = storePath();
        if (!Files.isRegularFile(path)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(path.toFile(), new TypeReference<>() {
            });
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    private void writeAll(List<RuntimeControlInspectionRunRespVO> runs) {
        try {
            Files.createDirectories(storePath().getParent());
            Path tmpPath = storePath().resolveSibling(FILE_NAME + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmpPath.toFile(), runs);
            Files.move(tmpPath, storePath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    private Path storePath() {
        return Path.of(properties.getStateDir()).normalize().resolve("runtime-ops").resolve(FILE_NAME);
    }
}
