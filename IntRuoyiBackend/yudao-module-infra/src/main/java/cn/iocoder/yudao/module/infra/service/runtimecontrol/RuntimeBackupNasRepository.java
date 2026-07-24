package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasListRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.file.NasConnectionConfig;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasFileReadResult;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_PATH_NOT_DIRECTORY;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_PATH_NOT_EXISTS;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED;

@Component
public class RuntimeBackupNasRepository {

    private static final Pattern BACKUP_POINT_NAME_PATTERN = Pattern.compile("\\d{8}-\\d{6}");

    private final RuntimeControlProperties properties;
    private final NasBrowserService nasBrowserService;
    private final NasSettingsService nasSettingsService;

    @Autowired
    public RuntimeBackupNasRepository(RuntimeControlProperties properties, NasBrowserService nasBrowserService,
                                      NasSettingsService nasSettingsService) {
        this.properties = properties;
        this.nasBrowserService = nasBrowserService;
        this.nasSettingsService = nasSettingsService;
    }

    RuntimeBackupNasRepository(RuntimeControlProperties properties, NasBrowserService nasBrowserService) {
        this(properties, nasBrowserService, new NasSettingsService() {
            @Override
            public FileNasConfigRespVO getNasConfig() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void saveNasConfig(FileNasConfigSaveReqVO reqVO) {
                throw new UnsupportedOperationException();
            }

            @Override
            public NasConnectionConfig toConnectionConfig(FileNasConfigSaveReqVO reqVO) {
                throw new UnsupportedOperationException();
            }

            @Override
            public NasConnectionConfig getRequiredNasConfig() {
                return new NasConnectionConfig(
                        properties.getBackupOps().getNasServer(),
                        445,
                        properties.getBackupOps().getNasShare(),
                        "",
                        "test-user",
                        "test-password"
                );
            }
        });
    }

    public List<BackupPointDir> listBackupPointDirs() {
        String root = backupPointsRoot();
        FileNasListRespVO response = nasBrowserService.listFiles(backupNasConfig(), root);
        return response.getItems().stream()
                .filter(item -> Boolean.TRUE.equals(item.getDir()) && isBackupPointName(item.getName()))
                .map(item -> new BackupPointDir(item.getName(), itemPath(root, item)))
                .sorted(Comparator.comparing(BackupPointDir::backupId).reversed())
                .toList();
    }

    public String backupPointsRoot() {
        String root = properties.getBackupOps().getNasBackupPointsRoot();
        if (StrUtil.isBlank(root)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "backupOps.nasBackupPointsRoot");
        }
        return normalizeNasPath(root, "backupOps.nasBackupPointsRoot");
    }

    public boolean isRegularFile(String path) {
        String normalized = normalizeNasPath(path, "nasPath");
        String parent = parentPath(normalized);
        String fileName = fileName(normalized);
        try {
            FileNasListRespVO response = nasBrowserService.listFiles(backupNasConfig(), parent);
            return response.getItems().stream()
                    .anyMatch(item -> !Boolean.TRUE.equals(item.getDir()) && fileName.equals(item.getName()));
        } catch (ServiceException ex) {
            if (isMissingPath(ex)) {
                return false;
            }
            throw ex;
        }
    }

    public boolean isDirectory(String path) {
        String normalized = normalizeNasPath(path, "nasPath");
        String parent = parentPath(normalized);
        String fileName = fileName(normalized);
        try {
            FileNasListRespVO response = nasBrowserService.listFiles(backupNasConfig(), parent);
            return response.getItems().stream()
                    .anyMatch(item -> Boolean.TRUE.equals(item.getDir()) && fileName.equals(item.getName()));
        } catch (ServiceException ex) {
            if (isMissingPath(ex)) {
                return false;
            }
            throw ex;
        }
    }

    public String readText(String path) {
        String normalized = normalizeNasPath(path, "nasPath");
        NasFileReadResult result = nasBrowserService.readFile(backupNasConfig(), normalized);
        byte[] bytes = Objects.requireNonNull(result.bytes(), "nas file bytes must not be null");
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public NasConnectionConfig backupNasConfig() {
        NasConnectionConfig baseConfig = nasSettingsService.getRequiredNasConfig();
        return new NasConnectionConfig(
                properties.getBackupOps().getNasServer(),
                baseConfig.port(),
                properties.getBackupOps().getNasShare(),
                baseConfig.domain(),
                baseConfig.username(),
                baseConfig.password()
        );
    }

    public String childPath(BackupPointDir backupPointDir, String... segments) {
        return childPath(backupPointDir.path(), segments);
    }

    public String childPath(String basePath, String... segments) {
        List<String> parts = new ArrayList<>();
        appendPathParts(parts, basePath, "basePath");
        for (String segment : segments) {
            appendPathParts(parts, segment, "nasPathSegment");
        }
        return String.join("/", parts);
    }

    private String itemPath(String root, FileNasListRespVO.Item item) {
        if (StrUtil.isNotBlank(item.getPath())) {
            return normalizeNasPath(item.getPath(), "nasItem.path");
        }
        return childPath(root, item.getName());
    }

    private boolean isMissingPath(ServiceException ex) {
        return FILE_NAS_PATH_NOT_EXISTS.getCode().equals(ex.getCode())
                || FILE_NAS_PATH_NOT_DIRECTORY.getCode().equals(ex.getCode());
    }

    private String parentPath(String normalizedPath) {
        int index = normalizedPath.lastIndexOf('/');
        if (index < 0) {
            return "";
        }
        return normalizedPath.substring(0, index);
    }

    private String fileName(String normalizedPath) {
        int index = normalizedPath.lastIndexOf('/');
        if (index < 0) {
            return normalizedPath;
        }
        return normalizedPath.substring(index + 1);
    }

    private boolean isBackupPointName(String name) {
        return BACKUP_POINT_NAME_PATTERN.matcher(StrUtil.trimToEmpty(name)).matches();
    }

    private String normalizeNasPath(String pathText, String fieldName) {
        List<String> parts = new ArrayList<>();
        appendPathParts(parts, pathText, fieldName);
        if (parts.isEmpty()) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, fieldName);
        }
        return String.join("/", parts);
    }

    private void appendPathParts(List<String> parts, String pathText, String fieldName) {
        String raw = StrUtil.trimToEmpty(pathText).replace("\\", "/");
        if (StrUtil.isBlank(raw)) {
            return;
        }
        for (String part : raw.split("/")) {
            String token = StrUtil.trimToEmpty(part);
            if (StrUtil.isBlank(token) || ".".equals(token)) {
                continue;
            }
            if ("..".equals(token)) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, fieldName + " 不允许包含 ..");
            }
            parts.add(token);
        }
    }

    public record BackupPointDir(String backupId, String path) {
    }
}
