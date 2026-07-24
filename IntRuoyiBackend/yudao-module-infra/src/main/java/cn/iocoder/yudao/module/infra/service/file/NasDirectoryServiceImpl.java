package cn.iocoder.yudao.module.infra.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasDirectoryTreeRespVO;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_DIRECTORY_NOT_DIRECTORY;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_DIRECTORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_DIRECTORY_PATH_BLANK;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_DIRECTORY_PATH_INVALID;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_DIRECTORY_READ_FAILED;

@Service
public class NasDirectoryServiceImpl implements NasDirectoryService {

    @Override
    public FileNasDirectoryTreeRespVO getNasDirectoryTree(String path) {
        String cleanPath = StrUtil.trim(path);
        if (StrUtil.isBlank(cleanPath)) {
            throw exception(FILE_NAS_DIRECTORY_PATH_BLANK);
        }

        Path rootPath;
        try {
            rootPath = Path.of(cleanPath).normalize();
        } catch (InvalidPathException ex) {
            throw exception(FILE_NAS_DIRECTORY_PATH_INVALID);
        }
        if (Files.notExists(rootPath)) {
            throw exception(FILE_NAS_DIRECTORY_NOT_EXISTS);
        }
        if (!Files.isDirectory(rootPath, LinkOption.NOFOLLOW_LINKS)) {
            throw exception(FILE_NAS_DIRECTORY_NOT_DIRECTORY);
        }

        AtomicInteger directoryCount = new AtomicInteger(1);
        return new FileNasDirectoryTreeRespVO()
                .setRootName(resolveDirectoryName(rootPath))
                .setRootPath(rootPath.toString())
                .setChildren(readChildren(rootPath, directoryCount))
                .setDirectoryCount(directoryCount.get());
    }

    private List<FileNasDirectoryTreeRespVO.Node> readChildren(Path parentPath, AtomicInteger directoryCount) {
        List<Path> childDirectories = listChildDirectories(parentPath);
        List<FileNasDirectoryTreeRespVO.Node> children = new ArrayList<>(childDirectories.size());
        for (Path childDirectory : childDirectories) {
            directoryCount.incrementAndGet();
            children.add(new FileNasDirectoryTreeRespVO.Node()
                    .setName(resolveDirectoryName(childDirectory))
                    .setPath(childDirectory.toString())
                    .setChildren(readChildren(childDirectory, directoryCount)));
        }
        return children;
    }

    private List<Path> listChildDirectories(Path parentPath) {
        try (Stream<Path> stream = Files.list(parentPath)) {
            return stream
                    .filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                    .sorted(Comparator.comparing(this::resolveDirectoryName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException ex) {
            throw exception(FILE_NAS_DIRECTORY_READ_FAILED, parentPath.toString());
        }
    }

    private String resolveDirectoryName(Path path) {
        Path fileName = path.getFileName();
        if (fileName != null && StrUtil.isNotBlank(fileName.toString())) {
            return fileName.toString();
        }
        String normalized = path.toString();
        if (StrUtil.isNotBlank(normalized)) {
            return normalized;
        }
        Path root = path.getRoot();
        return root != null ? root.toString() : "";
    }
}
