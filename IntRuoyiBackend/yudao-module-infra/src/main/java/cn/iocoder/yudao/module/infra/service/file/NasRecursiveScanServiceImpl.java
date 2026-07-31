package cn.iocoder.yudao.module.infra.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasListRespVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Objects;
import java.util.Queue;

import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_READ_FAILED;

@Service
public class NasRecursiveScanServiceImpl implements NasRecursiveScanService {

    private static final String SKIP_REASON_ACCESS_DENIED = "ACCESS_DENIED";

    @Resource
    private NasBrowserService nasBrowserService;

    @Override
    public void scan(NasConnectionConfig config, Collection<String> rootPaths, NasRecursiveScanHandler handler) {
        Objects.requireNonNull(config, "config must not be null");
        Objects.requireNonNull(rootPaths, "rootPaths must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        if (rootPaths.isEmpty()) {
            throw new IllegalArgumentException("NAS recursive scan roots must not be empty");
        }
        nasBrowserService.executeInSession(config, scope -> {
            for (String rootPath : rootPaths) {
                String normalizedRoot = normalizePath(rootPath);
                scanRoot(scope, normalizedRoot, handler);
            }
            return null;
        });
    }

    private void scanRoot(NasBrowserService.NasSessionScope scope, String rootPath,
                          NasRecursiveScanHandler handler) {
        handler.onCurrentDirectory(rootPath);
        FileNasListRespVO rootListing = scope.listFiles(rootPath);
        Queue<DirectoryScanRequest> queue = new ArrayDeque<>();
        emitChildren(rootPath, rootPath, rootListing, handler, queue);
        while (!queue.isEmpty()) {
            DirectoryScanRequest next = queue.remove();
            handler.onCurrentDirectory(next.path());
            FileNasListRespVO listing;
            try {
                listing = scope.listFiles(next.path());
            } catch (RuntimeException ex) {
                if (!isAccessDenied(ex)) {
                    throw ex;
                }
                handler.onSkippedDirectory(new NasRecursiveSkippedDirectory(
                        next.path(), SKIP_REASON_ACCESS_DENIED, LocalDateTime.now()));
                continue;
            }
            emitChildren(next.rootPath(), next.path(), listing, handler, queue);
        }
    }

    private void emitChildren(String rootPath, String parentPath, FileNasListRespVO listing,
                              NasRecursiveScanHandler handler, Queue<DirectoryScanRequest> queue) {
        for (FileNasListRespVO.Item item : listing.getItems()) {
            String childPath = normalizePath(StrUtil.blankToDefault(item.getPath(), joinPath(parentPath, item.getName())));
            if (Boolean.TRUE.equals(item.getDir())) {
                queue.add(new DirectoryScanRequest(rootPath, childPath));
                continue;
            }
            handler.onFile(new NasRecursiveScannedFile(
                    rootPath,
                    childPath,
                    StrUtil.blankToDefault(item.getName(), lastPathSegment(childPath)),
                    item.getSize(),
                    item.getModifiedAt(),
                    item.getHidden(),
                    item.getSystem()
            ));
        }
    }

    private static boolean isAccessDenied(RuntimeException ex) {
        if (ex instanceof ServiceException serviceException
                && !Objects.equals(serviceException.getCode(), FILE_NAS_READ_FAILED.getCode())) {
            return false;
        }
        return StrUtil.containsIgnoreCase(ex.getMessage(), "access denied")
                || StrUtil.containsIgnoreCase(ex.getMessage(), "status_access_denied");
    }

    private static String normalizePath(String path) {
        String raw = StrUtil.nullToEmpty(path).replace('\\', '/');
        String[] parts = raw.split("/");
        java.util.List<String> normalized = new java.util.ArrayList<>();
        for (String part : parts) {
            if (part.isBlank() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!normalized.isEmpty()) {
                    normalized.remove(normalized.size() - 1);
                }
                continue;
            }
            normalized.add(part);
        }
        return String.join("/", normalized);
    }

    private static String joinPath(String parentPath, String name) {
        if (StrUtil.isBlank(parentPath)) {
            return StrUtil.nullToEmpty(name);
        }
        return parentPath + "/" + StrUtil.nullToEmpty(name);
    }

    private static String lastPathSegment(String path) {
        if (StrUtil.isBlank(path)) {
            return "";
        }
        int slashIndex = path.lastIndexOf('/');
        return slashIndex < 0 ? path : path.substring(slashIndex + 1);
    }

    private record DirectoryScanRequest(String rootPath, String path) {
    }
}
