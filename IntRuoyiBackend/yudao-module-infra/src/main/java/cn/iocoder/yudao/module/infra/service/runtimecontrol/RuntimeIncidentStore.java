package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentRespVO;
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
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_OPERATION_STORE_FAILED;

@Component
public class RuntimeIncidentStore {

    private static final String FILE_NAME = "incidents.json";

    private final RuntimeControlProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public RuntimeIncidentStore(RuntimeControlProperties properties) {
        this.properties = properties;
    }

    public synchronized List<RuntimeControlIncidentRespVO> list() {
        return readAll().stream()
                .sorted(Comparator.comparing(RuntimeControlIncidentRespVO::getCreatedAt).reversed())
                .toList();
    }

    public synchronized RuntimeControlIncidentRespVO save(RuntimeControlIncidentRespVO incident) {
        List<RuntimeControlIncidentRespVO> incidents = new ArrayList<>(readAll());
        if (incident.getId() == null) {
            incident.setId(nextId(incidents));
        }
        incidents.removeIf(item -> incident.getId().equals(item.getId()));
        incidents.add(incident);
        writeAll(incidents);
        return incident;
    }

    public synchronized RuntimeControlIncidentRespVO findById(Long id) {
        return readAll().stream()
                .filter(incident -> id.equals(incident.getId()))
                .findFirst()
                .orElse(null);
    }

    private Long nextId(List<RuntimeControlIncidentRespVO> incidents) {
        return incidents.stream()
                .map(RuntimeControlIncidentRespVO::getId)
                .filter(id -> id != null)
                .max(Long::compareTo)
                .orElse(0L) + 1;
    }

    private List<RuntimeControlIncidentRespVO> readAll() {
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

    private void writeAll(List<RuntimeControlIncidentRespVO> incidents) {
        try {
            Files.createDirectories(storePath().getParent());
            Path tmpPath = storePath().resolveSibling(FILE_NAME + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmpPath.toFile(), incidents);
            Files.move(tmpPath, storePath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    private Path storePath() {
        return Path.of(properties.getStateDir()).normalize().resolve("runtime-ops").resolve(FILE_NAME);
    }
}
