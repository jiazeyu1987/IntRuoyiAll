package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlAlertRespVO;
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
public class RuntimeOpsAlertStore {

    private static final String FILE_NAME = "alerts.json";

    private final RuntimeControlProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public RuntimeOpsAlertStore(RuntimeControlProperties properties) {
        this.properties = properties;
    }

    public synchronized RuntimeControlAlertRespVO save(RuntimeControlAlertRespVO alert) {
        List<RuntimeControlAlertRespVO> alerts = new ArrayList<>(readAll());
        if (alert.getId() == null) {
            alert.setId(nextId(alerts));
        }
        if (alert.getCreatedAt() == null) {
            alert.setCreatedAt(LocalDateTime.now());
        }
        alerts.removeIf(item -> alert.getId().equals(item.getId()));
        alerts.add(alert);
        writeAll(alerts);
        return alert;
    }

    public synchronized RuntimeControlAlertRespVO findById(Long id) {
        return readAll().stream()
                .filter(alert -> id.equals(alert.getId()))
                .findFirst()
                .orElse(null);
    }

    public synchronized PageResult<RuntimeControlAlertRespVO> page(RuntimeControlAlertPageReqVO pageReqVO) {
        List<RuntimeControlAlertRespVO> filtered = readAll().stream()
                .filter(alert -> StrUtil.isBlank(pageReqVO.getEnvironment())
                        || pageReqVO.getEnvironment().equals(alert.getEnvironment()))
                .filter(alert -> StrUtil.isBlank(pageReqVO.getAction())
                        || pageReqVO.getAction().equals(alert.getAction()))
                .filter(alert -> pageReqVO.getSiteMessageStatus() == null
                        || pageReqVO.getSiteMessageStatus().equals(alert.getSiteMessageStatus()))
                .sorted(Comparator.comparing(RuntimeControlAlertRespVO::getCreatedAt).reversed())
                .toList();
        int fromIndex = Math.min((pageReqVO.getPageNo() - 1) * pageReqVO.getPageSize(), filtered.size());
        int toIndex = Math.min(fromIndex + pageReqVO.getPageSize(), filtered.size());
        return new PageResult<>(filtered.subList(fromIndex, toIndex), (long) filtered.size());
    }

    private Long nextId(List<RuntimeControlAlertRespVO> alerts) {
        return alerts.stream()
                .map(RuntimeControlAlertRespVO::getId)
                .filter(id -> id != null)
                .max(Long::compareTo)
                .orElse(0L) + 1;
    }

    private List<RuntimeControlAlertRespVO> readAll() {
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

    private void writeAll(List<RuntimeControlAlertRespVO> alerts) {
        try {
            Files.createDirectories(storePath().getParent());
            Path tmpPath = storePath().resolveSibling(FILE_NAME + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmpPath.toFile(), alerts);
            Files.move(tmpPath, storePath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    private Path storePath() {
        return Path.of(properties.getStateDir()).normalize().resolve("runtime-ops").resolve(FILE_NAME);
    }
}
