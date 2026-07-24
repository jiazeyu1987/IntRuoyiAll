package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlProbeLatestRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_OPERATION_STORE_FAILED;

@Component
public class RuntimeOpsProbeStore {

    private static final String FILE_NAME = "probes-latest.json";

    private final RuntimeControlProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public RuntimeOpsProbeStore(RuntimeControlProperties properties) {
        this.properties = properties;
    }

    public synchronized RuntimeControlProbeLatestRespVO save(RuntimeControlProbeLatestRespVO latest) {
        try {
            Files.createDirectories(storePath().getParent());
            Path tmpPath = storePath().resolveSibling(FILE_NAME + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmpPath.toFile(), latest);
            Files.move(tmpPath, storePath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return latest;
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    public synchronized RuntimeControlProbeLatestRespVO readLatest() {
        Path path = storePath();
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            return objectMapper.readValue(path.toFile(), RuntimeControlProbeLatestRespVO.class);
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    private Path storePath() {
        return Path.of(properties.getStateDir()).normalize().resolve("runtime-ops").resolve(FILE_NAME);
    }
}
