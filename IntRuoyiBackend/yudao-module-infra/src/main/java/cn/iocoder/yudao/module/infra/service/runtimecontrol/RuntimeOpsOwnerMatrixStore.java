package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOwnerMatrixRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_OPERATION_STORE_FAILED;

@Component
public class RuntimeOpsOwnerMatrixStore {

    private static final String FILE_NAME = "owner-matrix.json";

    private final RuntimeControlProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public RuntimeOpsOwnerMatrixStore(RuntimeControlProperties properties) {
        this.properties = properties;
    }

    public synchronized List<RuntimeControlOwnerMatrixRespVO> list() {
        return readAll().stream()
                .sorted(Comparator.comparing(RuntimeControlOwnerMatrixRespVO::getId))
                .toList();
    }

    public synchronized RuntimeControlOwnerMatrixRespVO save(RuntimeControlOwnerMatrixRespVO owner) {
        List<RuntimeControlOwnerMatrixRespVO> owners = new ArrayList<>(readAll());
        if (owner.getId() == null) {
            owner.setId(nextId(owners));
        }
        owner.setUpdatedAt(LocalDateTime.now());
        owners.removeIf(item -> owner.getId().equals(item.getId()));
        owners.add(owner);
        writeAll(owners);
        return owner;
    }

    public synchronized RuntimeControlOwnerMatrixRespVO findById(Long id) {
        return readAll().stream()
                .filter(owner -> id.equals(owner.getId()))
                .findFirst()
                .orElse(null);
    }

    private Long nextId(List<RuntimeControlOwnerMatrixRespVO> owners) {
        return owners.stream()
                .map(RuntimeControlOwnerMatrixRespVO::getId)
                .filter(id -> id != null)
                .max(Long::compareTo)
                .orElse(0L) + 1;
    }

    private List<RuntimeControlOwnerMatrixRespVO> readAll() {
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

    private void writeAll(List<RuntimeControlOwnerMatrixRespVO> owners) {
        try {
            Files.createDirectories(storePath().getParent());
            Path tmpPath = storePath().resolveSibling(FILE_NAME + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmpPath.toFile(), owners);
            Files.move(tmpPath, storePath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    private Path storePath() {
        return Path.of(properties.getStateDir()).normalize().resolve("runtime-ops").resolve(FILE_NAME);
    }
}
