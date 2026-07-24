package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigTestRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasDirectoryTreeRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasListRespVO;
import cn.iocoder.yudao.module.infra.service.file.NasAclReadResult;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasConnectionConfig;
import cn.iocoder.yudao.module.infra.service.file.NasFileReadResult;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_PATH_NOT_DIRECTORY;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_PATH_NOT_EXISTS;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_READ_FAILED;

final class RuntimeControlNasBrowserServiceStub implements NasBrowserService {

    private final Path root;

    RuntimeControlNasBrowserServiceStub(Path root) {
        this.root = root;
    }

    @Override
    public FileNasListRespVO listFiles(String path) {
        String normalized = normalize(path);
        Path directory = resolve(normalized);
        if (!Files.exists(directory)) {
            throw exception(FILE_NAS_PATH_NOT_EXISTS, normalized);
        }
        if (!Files.isDirectory(directory)) {
            throw exception(FILE_NAS_PATH_NOT_DIRECTORY, normalized);
        }
        try (java.util.stream.Stream<Path> stream = Files.list(directory)) {
            List<FileNasListRespVO.Item> items = stream
                    .sorted(Comparator.comparing(item -> item.getFileName().toString()))
                    .map(item -> toItem(normalized, item))
                    .toList();
            return new FileNasListRespVO()
                    .setCurrentPath(normalized)
                    .setItems(items);
        } catch (IOException ex) {
            throw exception(FILE_NAS_READ_FAILED, ex.getMessage());
        }
    }

    @Override
    public FileNasListRespVO listFiles(NasConnectionConfig config, String path) {
        return listFiles(path);
    }

    @Override
    public <T> T executeInSession(NasConnectionConfig config, NasSessionCallback<T> callback) {
        return callback.execute(new NasSessionScope() {
            @Override
            public FileNasListRespVO listFiles(String path) {
                return RuntimeControlNasBrowserServiceStub.this.listFiles(config, path);
            }

            @Override
            public NasFileReadResult readFile(String path) {
                return RuntimeControlNasBrowserServiceStub.this.readFile(config, path);
            }

            @Override
            public void writeFileTo(String path, OutputStream outputStream) {
                NasFileReadResult result = readFile(path);
                try {
                    outputStream.write(result.bytes());
                } catch (IOException ex) {
                    throw exception(FILE_NAS_READ_FAILED, ex.getMessage());
                }
            }

            @Override
            public NasAclReadResult readDirectoryAcl(String path) {
                return RuntimeControlNasBrowserServiceStub.this.readDirectoryAcl(path);
            }
        });
    }

    @Override
    public FileNasConfigTestRespVO testConnection(NasConnectionConfig config) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileNasDirectoryTreeRespVO getDirectoryTree() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NasFileReadResult readFile(String path) {
        String normalized = normalize(path);
        Path file = resolve(normalized);
        if (!Files.isRegularFile(file)) {
            throw exception(FILE_NAS_PATH_NOT_EXISTS, normalized);
        }
        try {
            return new NasFileReadResult(file.getFileName().toString(), normalized,
                    "application/octet-stream", Files.readAllBytes(file));
        } catch (IOException ex) {
            throw exception(FILE_NAS_READ_FAILED, ex.getMessage());
        }
    }

    @Override
    public NasFileReadResult readFile(NasConnectionConfig config, String path) {
        return readFile(path);
    }

    @Override
    public NasAclReadResult readDirectoryAcl(String path) {
        throw new UnsupportedOperationException();
    }

    private FileNasListRespVO.Item toItem(String parent, Path item) {
        String name = item.getFileName().toString();
        String path = StrUtil.isBlank(parent) ? name : parent + "/" + name;
        return new FileNasListRespVO.Item()
                .setName(name)
                .setPath(path)
                .setDir(Files.isDirectory(item))
                .setSize(readSize(item));
    }

    private Long readSize(Path item) {
        if (Files.isDirectory(item)) {
            return 0L;
        }
        try {
            return Files.size(item);
        } catch (IOException ex) {
            throw exception(FILE_NAS_READ_FAILED, ex.getMessage());
        }
    }

    private Path resolve(String normalized) {
        Path resolved = root;
        if (StrUtil.isBlank(normalized)) {
            return resolved;
        }
        for (String part : normalized.split("/")) {
            resolved = resolved.resolve(part);
        }
        return resolved.normalize();
    }

    private String normalize(String path) {
        String raw = StrUtil.trimToEmpty(path).replace("\\", "/");
        if (StrUtil.isBlank(raw)) {
            return "";
        }
        return String.join("/", java.util.Arrays.stream(raw.split("/"))
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .toList());
    }
}
