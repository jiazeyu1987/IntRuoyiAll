package cn.iocoder.yudao.module.srm.service.naslocator;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasListRespVO;
import cn.iocoder.yudao.module.infra.framework.file.core.utils.FileTypeUtils;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasConnectionConfig;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo.SrmNasLocatorBlacklistRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo.SrmNasLocatorBlacklistSaveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo.SrmNasLocatorFileRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo.SrmNasLocatorPageReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo.SrmNasLocatorStatusRespVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.naslocator.SrmNasLocatorEntryDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.naslocator.SrmNasLocatorRefreshTaskDO;
import cn.iocoder.yudao.module.srm.dal.mysql.naslocator.SrmNasLocatorEntryMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.naslocator.SrmNasLocatorRefreshTaskMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_READ_FAILED;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.NAS_LOCATOR_ENTRY_NOT_EXISTS;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.NAS_LOCATOR_ENTRY_NOT_FILE;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.NAS_LOCATOR_NO_SUCCESS_SNAPSHOT;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.NAS_LOCATOR_REFRESH_RUNNING;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.NAS_LOCATOR_REFRESH_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.NAS_LOCATOR_SHARE_CONFIG_INVALID;

@Service
@Validated
@Slf4j
public class SrmNasLocatorServiceImpl implements SrmNasLocatorService {

    static final String STATUS_IDLE = "IDLE";
    static final String STATUS_RUNNING = "RUNNING";
    static final String STATUS_SUCCESS = "SUCCESS";
    static final String STATUS_FAILED = "FAILED";
    static final String ENTRY_TYPE_FILE = "FILE";
    static final String ENTRY_TYPE_DIRECTORY = "DIRECTORY";
    static final String PROTECTED_SERVER = "172.30.30.4";
    static final String PROTECTED_SHARE_QUALITY = "质量体系文件";
    static final String PROTECTED_SHARE_PRODUCTION = "生产部";
    static final Set<String> PROTECTED_SHARES = Set.of(PROTECTED_SHARE_QUALITY, PROTECTED_SHARE_PRODUCTION);
    static final String PROTECTED_SCOPE_SHARE_DISPLAY = "\\\\" + PROTECTED_SERVER + "\\" + PROTECTED_SHARE_QUALITY
            + "；\\\\" + PROTECTED_SERVER + "\\" + PROTECTED_SHARE_PRODUCTION;
    static final String ROOT_PATH = "";
    static final Duration STALE_RUNNING_TASK_TIMEOUT = Duration.ofMinutes(10);
    private static final Map<Long, RunningProgress> RUNNING_PROGRESS = new ConcurrentHashMap<>();

    @Resource
    private SrmNasLocatorRefreshTaskMapper refreshTaskMapper;
    @Resource
    private SrmNasLocatorEntryMapper entryMapper;
    @Resource
    private NasBrowserService nasBrowserService;
    @Resource
    private NasSettingsService nasSettingsService;
    @Resource
    private SrmNasLocatorBlacklistSettingsService blacklistSettingsService;
    private final TransactionTemplate transactionTemplate;

    public SrmNasLocatorServiceImpl(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public SrmNasLocatorStatusRespVO getStatus() {
        Long tenantId = getRequiredTenantId();
        SrmNasLocatorRefreshTaskDO latestTask = refreshTaskMapper.selectLatestTask(tenantId);
        SrmNasLocatorRefreshTaskDO successTask = refreshTaskMapper.selectLatestSuccessTask(tenantId);

        SrmNasLocatorStatusRespVO respVO = new SrmNasLocatorStatusRespVO();
        respVO.setScopeShare(PROTECTED_SCOPE_SHARE_DISPLAY);
        respVO.setRootPath(ROOT_PATH);
        respVO.setLatestTaskStatus(latestTask == null ? STATUS_IDLE : latestTask.getStatus());
        if (successTask != null) {
            respVO.setLatestSuccessTime(successTask.getFinishedTime() == null
                    ? null
                    : successTask.getFinishedTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            respVO.setFileCount(successTask.getFileCount());
            respVO.setDirectoryCount(successTask.getDirectoryCount());
        } else {
            respVO.setFileCount(0L);
            respVO.setDirectoryCount(0L);
        }
        if (latestTask != null && STATUS_RUNNING.equals(latestTask.getStatus())) {
            RunningProgress runningProgress = RUNNING_PROGRESS.get(latestTask.getId());
            if (runningProgress != null) {
                respVO.setRunningShare(runningProgress.share());
                respVO.setRunningPath(runningProgress.path());
                respVO.setRunningDirectoryCount(runningProgress.directoryCount());
                respVO.setRunningFileCount(runningProgress.fileCount());
                respVO.setRunningShareIndex(runningProgress.shareIndex());
                respVO.setRunningShareTotal(runningProgress.shareTotal());
            }
        }
        if (latestTask == null) {
            respVO.setMessage("请先刷新 NAS 索引");
        } else if (STATUS_FAILED.equals(latestTask.getStatus())) {
            respVO.setMessage(StrUtil.blankToDefault(latestTask.getErrorMessage(), "最近一次刷新失败"));
        } else if (STATUS_RUNNING.equals(latestTask.getStatus())) {
            if (respVO.getRunningPath() != null) {
                respVO.setMessage("NAS 索引刷新进行中，当前正在扫描 " + respVO.getRunningPath());
            } else {
                respVO.setMessage("NAS 索引刷新进行中");
            }
        } else {
            respVO.setMessage("最近一次刷新成功");
        }
        return respVO;
    }

    @Override
    public SrmNasLocatorBlacklistRespVO getBlacklist() {
        SrmNasLocatorBlacklistRespVO respVO = new SrmNasLocatorBlacklistRespVO();
        respVO.setPatterns(blacklistSettingsService.getPatterns());
        return respVO;
    }

    @Override
    public void saveBlacklist(SrmNasLocatorBlacklistSaveReqVO reqVO) {
        blacklistSettingsService.savePatterns(reqVO == null ? List.of() : reqVO.getPatterns());
    }

    @Override
    public PageResult<SrmNasLocatorFileRespVO> getFilePage(SrmNasLocatorPageReqVO pageReqVO) {
        SrmNasLocatorRefreshTaskDO successTask = requireLatestSuccessTask();
        PageResult<SrmNasLocatorEntryDO> pageResult = entryMapper.selectFilePage(successTask.getId(), pageReqVO);
        List<SrmNasLocatorFileRespVO> list = pageResult.getList().stream()
                .map(this::buildFileResp)
                .toList();
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public void triggerRefresh() {
        verifyProtectedConfig(nasSettingsService.getRequiredNasConfig());
        Long tenantId = getRequiredTenantId();
        SrmNasLocatorRefreshTaskDO runningTask = refreshTaskMapper.selectRunningTask(tenantId);
        if (runningTask != null && isStaleRunningTask(runningTask)) {
            markTaskFailed(runningTask.getId(), "stale running task: previous refresh did not finish before timeout");
            runningTask = null;
        }
        if (runningTask != null) {
            throw exception(NAS_LOCATOR_REFRESH_RUNNING);
        }
        SrmNasLocatorRefreshTaskDO task = SrmNasLocatorRefreshTaskDO.builder()
                .status(STATUS_RUNNING)
                .scopeShare(PROTECTED_SCOPE_SHARE_DISPLAY)
                .rootPath(ROOT_PATH)
                .startedTime(LocalDateTime.now())
                .directoryCount(0L)
                .fileCount(0L)
                .build();
        task.setTenantId(tenantId);
        refreshTaskMapper.insert(task);
        CompletableFuture.runAsync(() -> TenantUtils.execute(tenantId, () -> executeRefreshSafely(task.getId())));
    }

    @Override
    public void download(Long id, HttpServletResponse response) throws Exception {
        SrmNasLocatorEntryDO entry = entryMapper.selectById(id);
        if (entry == null || !Objects.equals(entry.getTenantId(), getRequiredTenantId())) {
            throw exception(NAS_LOCATOR_ENTRY_NOT_EXISTS);
        }
        if (!ENTRY_TYPE_FILE.equals(entry.getEntryType())) {
            throw exception(NAS_LOCATOR_ENTRY_NOT_FILE);
        }
        SrmNasLocatorRefreshTaskDO task = refreshTaskMapper.selectById(entry.getRefreshTaskId());
        if (task == null
                || !Objects.equals(task.getTenantId(), getRequiredTenantId())
                || !STATUS_SUCCESS.equals(task.getStatus())) {
            throw exception(NAS_LOCATOR_ENTRY_NOT_EXISTS);
        }
        verifyProtectedConfig(nasSettingsService.getRequiredNasConfig());
        ShareResolvedPath shareResolvedPath = resolveSharePath(entry.getPath());
        String fileName = entry.getName();
        response.setContentType(StrUtil.blankToDefault(FileTypeUtils.getMineType(fileName), "application/octet-stream"));
        response.setHeader("Content-Disposition", ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString());
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        if (entry.getSize() != null && entry.getSize() >= 0) {
            response.setContentLengthLong(entry.getSize());
        }
        nasBrowserService.writeFileTo(toShareConfig(nasSettingsService.getRequiredNasConfig(), shareResolvedPath.share()),
                shareResolvedPath.relativePath(), response.getOutputStream());
    }

    void runRefreshNow(Long taskId) {
        executeRefreshSafely(taskId);
    }

    private void executeRefreshSafely(Long taskId) {
        try {
            executeRefresh(taskId);
        } catch (RuntimeException exception) {
            log.error("[executeRefreshSafely][taskId({}) SRM NAS locator refresh failed]", taskId, exception);
            markTaskFailed(taskId, exception.getMessage());
        }
    }

    private void executeRefresh(Long taskId) {
        SrmNasLocatorRefreshTaskDO task = requireRunningTask(taskId);
        NasConnectionConfig config = nasSettingsService.getRequiredNasConfig();
        verifyProtectedConfig(config);
        RUNNING_PROGRESS.put(taskId, RunningProgress.initial(orderedProtectedShares().size()));
        List<FilenamePatternMatcher> blacklistMatchers = buildBlacklistMatchers(blacklistSettingsService.getPatterns());
        RefreshSnapshot snapshot = loadSnapshot(taskId, config, blacklistMatchers);
        persistSuccessSnapshot(task, snapshot);
        cleanupOldSnapshots(task.getTenantId());
    }

    private RefreshSnapshot loadSnapshot(Long taskId, NasConnectionConfig config,
                                         List<FilenamePatternMatcher> blacklistMatchers) {
        long directoryCount = 0L;
        long fileCount = 0L;
        List<SrmNasLocatorEntryDO> entries = new ArrayList<>();
        List<String> shares = orderedProtectedShares();
        for (int i = 0; i < shares.size(); i++) {
            String share = shares.get(i);
            int shareIndex = i + 1;
            long baseDirectoryCount = directoryCount;
            long baseFileCount = fileCount;
            updateRunningProgress(taskId, share, share, baseDirectoryCount, baseFileCount, shareIndex, shares.size());
            NasConnectionConfig shareConfig = toShareConfig(config, share);
            RefreshSnapshot shareSnapshot = nasBrowserService.executeInSession(shareConfig,
                    scope -> loadSnapshotForShare(taskId, share, shareIndex, shares.size(),
                            baseDirectoryCount, baseFileCount, scope, blacklistMatchers));
            directoryCount += shareSnapshot.directoryCount();
            fileCount += shareSnapshot.fileCount();
            entries.addAll(shareSnapshot.entries());
        }
        return new RefreshSnapshot(entries, directoryCount, fileCount);
    }

    private RefreshSnapshot loadSnapshotForShare(Long taskId, String share, int shareIndex, int shareTotal,
                                                 long baseDirectoryCount, long baseFileCount,
                                                 NasBrowserService.NasSessionScope nasSessionScope,
                                                 List<FilenamePatternMatcher> blacklistMatchers) {
        long directoryCount = 0L;
        long fileCount = 0L;
        List<SrmNasLocatorEntryDO> entries = new ArrayList<>();
        Deque<QueuedDirectory> queue = new ArrayDeque<>();
        entries.add(buildDirectoryEntry(share, share, "", null));
        queue.add(QueuedDirectory.rootDirectory(share));
        while (!queue.isEmpty()) {
            QueuedDirectory currentDirectory = queue.removeFirst();
            String currentScopedPath = currentDirectory.root() ? share : buildScopedPath(share, currentDirectory.path());
            updateRunningProgress(taskId, share, currentScopedPath,
                    baseDirectoryCount + directoryCount, baseFileCount + fileCount, shareIndex, shareTotal);
            FileNasListRespVO response;
            try {
                response = nasSessionScope.listFiles(currentDirectory.path());
            } catch (RuntimeException exception) {
                if (shouldSkipUnreadableDirectory(currentDirectory, exception)) {
                    log.warn("[loadSnapshot][skip unreadable directory({}) because {}]",
                            currentDirectory.path(), StrUtil.blankToDefault(exception.getMessage(), exception.getClass().getSimpleName()));
                    continue;
                }
                throw exception;
            }
            if (!currentDirectory.root()) {
                FileNasListRespVO.Item currentItem = currentDirectory.item();
                directoryCount++;
                entries.add(buildDirectoryEntry(
                        share,
                        currentItem.getName(),
                        currentItem.getPath(),
                        currentItem.getModifiedAt()
                ));
            }
            List<FileNasListRespVO.Item> items = response.getItems().stream()
                    .sorted(Comparator.comparing(FileNasListRespVO.Item::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            for (FileNasListRespVO.Item item : items) {
                if (shouldSkipFromSnapshot(item)) {
                    continue;
                }
                if (Boolean.TRUE.equals(item.getDir())) {
                    queue.addLast(QueuedDirectory.fromItem(share, item));
                } else {
                    if (matchesBlacklist(item.getName(), blacklistMatchers)) {
                        continue;
                    }
                    fileCount++;
                    String fullPath = buildScopedPath(share, item.getPath());
                    updateRunningProgress(taskId, share, fullPath,
                            baseDirectoryCount + directoryCount, baseFileCount + fileCount, shareIndex, shareTotal);
                    entries.add(SrmNasLocatorEntryDO.builder()
                            .entryType(ENTRY_TYPE_FILE)
                            .name(item.getName())
                            .path(fullPath)
                            .pathHash(DigestUtil.sha256Hex(fullPath))
                            .parentPath(resolveParentPath(fullPath))
                            .size(item.getSize())
                            .modifiedAt(item.getModifiedAt())
                            .build());
                }
            }
        }
        return new RefreshSnapshot(entries, directoryCount, fileCount);
    }

    protected void persistSuccessSnapshot(SrmNasLocatorRefreshTaskDO task, RefreshSnapshot snapshot) {
        transactionTemplate.executeWithoutResult(status -> {
            for (SrmNasLocatorEntryDO entry : snapshot.entries()) {
                entry.setTenantId(task.getTenantId());
                entry.setRefreshTaskId(task.getId());
            }
            entryMapper.insertBatch(snapshot.entries());
            task.setStatus(STATUS_SUCCESS);
            task.setFinishedTime(LocalDateTime.now());
            task.setDirectoryCount(snapshot.directoryCount());
            task.setFileCount(snapshot.fileCount());
            task.setErrorMessage(null);
            refreshTaskMapper.updateById(task);
        });
        RUNNING_PROGRESS.remove(task.getId());
    }

    protected void markTaskFailed(Long taskId, String message) {
        transactionTemplate.executeWithoutResult(status -> {
            SrmNasLocatorRefreshTaskDO task = refreshTaskMapper.selectById(taskId);
            if (task == null) {
                return;
            }
            task.setStatus(STATUS_FAILED);
            task.setFinishedTime(LocalDateTime.now());
            task.setErrorMessage(StrUtil.blankToDefault(message, "NAS 索引刷新失败"));
            refreshTaskMapper.updateById(task);
        });
        RUNNING_PROGRESS.remove(taskId);
    }

    protected void cleanupOldSnapshots(Long tenantId) {
        transactionTemplate.executeWithoutResult(status -> {
            for (SrmNasLocatorRefreshTaskDO failedTask : refreshTaskMapper.selectFailedTasks(tenantId)) {
                deleteSnapshot(failedTask.getId());
                refreshTaskMapper.deleteById(failedTask.getId());
            }
            List<SrmNasLocatorRefreshTaskDO> successTasks = refreshTaskMapper.selectSuccessTasksDesc(tenantId);
            for (int i = 2; i < successTasks.size(); i++) {
                SrmNasLocatorRefreshTaskDO staleTask = successTasks.get(i);
                deleteSnapshot(staleTask.getId());
                refreshTaskMapper.deleteById(staleTask.getId());
            }
        });
    }

    private boolean isStaleRunningTask(SrmNasLocatorRefreshTaskDO runningTask) {
        if (runningTask == null || runningTask.getStartedTime() == null) {
            return false;
        }
        return runningTask.getStartedTime().isBefore(LocalDateTime.now().minus(STALE_RUNNING_TASK_TIMEOUT));
    }

    private void deleteSnapshot(Long refreshTaskId) {
        entryMapper.deleteByRefreshTaskId(refreshTaskId);
    }

    private SrmNasLocatorRefreshTaskDO requireLatestSuccessTask() {
        SrmNasLocatorRefreshTaskDO successTask = refreshTaskMapper.selectLatestSuccessTask(getRequiredTenantId());
        if (successTask == null) {
            throw exception(NAS_LOCATOR_NO_SUCCESS_SNAPSHOT);
        }
        return successTask;
    }

    private SrmNasLocatorRefreshTaskDO requireRunningTask(Long taskId) {
        SrmNasLocatorRefreshTaskDO task = refreshTaskMapper.selectById(taskId);
        if (task == null
                || !Objects.equals(task.getTenantId(), getRequiredTenantId())
                || !STATUS_RUNNING.equals(task.getStatus())) {
            throw exception(NAS_LOCATOR_REFRESH_TASK_NOT_EXISTS);
        }
        return task;
    }

    private void verifyProtectedConfig(NasConnectionConfig config) {
        if (config == null
                || !PROTECTED_SERVER.equals(StrUtil.trim(config.server()))
                || !PROTECTED_SHARES.contains(StrUtil.trim(config.share()))) {
            throw exception(NAS_LOCATOR_SHARE_CONFIG_INVALID);
        }
    }

    private Long getRequiredTenantId() {
        return TenantContextHolder.getRequiredTenantId();
    }

    private void updateRunningProgress(Long taskId, String share, String path, long directoryCount, long fileCount,
                                       int shareIndex, int shareTotal) {
        RUNNING_PROGRESS.put(taskId, new RunningProgress(
                share,
                StrUtil.blankToDefault(path, share),
                directoryCount,
                fileCount,
                shareIndex,
                shareTotal
        ));
    }

    private String resolveParentPath(String path) {
        if (StrUtil.isBlank(path) || !path.contains("/")) {
            return "";
        }
        return path.substring(0, path.lastIndexOf('/'));
    }

    private String buildScopedPath(String share, String relativePath) {
        return StrUtil.isBlank(relativePath) ? share : share + "/" + relativePath;
    }

    private SrmNasLocatorEntryDO buildDirectoryEntry(String share, String name, String relativePath, Long modifiedAt) {
        String fullPath = buildScopedPath(share, relativePath);
        return SrmNasLocatorEntryDO.builder()
                .entryType(ENTRY_TYPE_DIRECTORY)
                .name(name)
                .path(fullPath)
                .pathHash(DigestUtil.sha256Hex(fullPath))
                .parentPath(resolveParentPath(fullPath))
                .size(0L)
                .modifiedAt(modifiedAt)
                .build();
    }

    private NasConnectionConfig toShareConfig(NasConnectionConfig baseConfig, String share) {
        return new NasConnectionConfig(baseConfig.server(), baseConfig.port(), share, baseConfig.domain(),
                baseConfig.username(), baseConfig.password());
    }

    private List<String> orderedProtectedShares() {
        return List.of(PROTECTED_SHARE_QUALITY, PROTECTED_SHARE_PRODUCTION);
    }

    private ShareResolvedPath resolveSharePath(String fullPath) {
        if (StrUtil.isBlank(fullPath)) {
            throw exception(NAS_LOCATOR_SHARE_CONFIG_INVALID);
        }
        String clean = fullPath.replace("\\", "/");
        int slashIndex = clean.indexOf('/');
        String share = slashIndex >= 0 ? clean.substring(0, slashIndex) : clean;
        String relativePath = slashIndex >= 0 ? clean.substring(slashIndex + 1) : "";
        if (!PROTECTED_SHARES.contains(share) || StrUtil.isBlank(relativePath)) {
            throw exception(NAS_LOCATOR_SHARE_CONFIG_INVALID);
        }
        return new ShareResolvedPath(share, relativePath);
    }

    private boolean shouldSkipFromSnapshot(FileNasListRespVO.Item item) {
        return Boolean.TRUE.equals(item.getDir())
                && (Boolean.TRUE.equals(item.getSystem()) || Boolean.TRUE.equals(item.getHidden()));
    }

    private boolean shouldSkipUnreadableDirectory(QueuedDirectory directory, RuntimeException exception) {
        return !directory.root() && isAccessDeniedReadFailure(exception);
    }

    private boolean isAccessDeniedReadFailure(RuntimeException exception) {
        if (!(exception instanceof ServiceException serviceException)
                || !Objects.equals(serviceException.getCode(), FILE_NAS_READ_FAILED.getCode())) {
            return false;
        }
        String message = StrUtil.nullToEmpty(serviceException.getMessage()).toLowerCase();
        return message.contains("access denied")
                || message.contains("status_access_denied")
                || message.contains("拒绝访问");
    }

    private SrmNasLocatorFileRespVO buildFileResp(SrmNasLocatorEntryDO entry) {
        SrmNasLocatorFileRespVO respVO = new SrmNasLocatorFileRespVO();
        respVO.setId(entry.getId());
        respVO.setFileName(entry.getName());
        respVO.setDirectoryPath(entry.getParentPath());
        respVO.setFullPath(entry.getPath());
        respVO.setSize(entry.getSize());
        respVO.setModifiedAt(entry.getModifiedAt());
        return respVO;
    }

    private List<FilenamePatternMatcher> buildBlacklistMatchers(List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return List.of();
        }
        return patterns.stream()
                .map(FilenamePatternMatcher::fromPattern)
                .toList();
    }

    private boolean matchesBlacklist(String fileName, List<FilenamePatternMatcher> blacklistMatchers) {
        if (StrUtil.isBlank(fileName) || blacklistMatchers == null || blacklistMatchers.isEmpty()) {
            return false;
        }
        for (FilenamePatternMatcher matcher : blacklistMatchers) {
            if (matcher.matches(fileName)) {
                return true;
            }
        }
        return false;
    }

    private record RefreshSnapshot(List<SrmNasLocatorEntryDO> entries, long directoryCount, long fileCount) {
    }

    private record RunningProgress(String share, String path, long directoryCount, long fileCount,
                                   int shareIndex, int shareTotal) {

        private static RunningProgress initial(int shareTotal) {
            return new RunningProgress(null, null, 0L, 0L, 0, shareTotal);
        }
    }

    private record QueuedDirectory(String share, FileNasListRespVO.Item item, String path, boolean root) {

        private static QueuedDirectory rootDirectory(String share) {
            return new QueuedDirectory(share, null, ROOT_PATH, true);
        }

        private static QueuedDirectory fromItem(String share, FileNasListRespVO.Item item) {
            return new QueuedDirectory(share, item, item.getPath(), false);
        }
    }

    private record FilenamePatternMatcher(Pattern regex) {

        private static FilenamePatternMatcher fromPattern(String pattern) {
            return new FilenamePatternMatcher(Pattern.compile(toRegex(pattern),
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        }

        private boolean matches(String fileName) {
            return regex.matcher(fileName).matches();
        }

        private static String toRegex(String pattern) {
            StringBuilder builder = new StringBuilder("^");
            for (int i = 0; i < pattern.length(); i++) {
                char current = pattern.charAt(i);
                if (current == '*') {
                    builder.append(".*");
                    continue;
                }
                if ("\\.[]{}()+-^$|?".indexOf(current) >= 0) {
                    builder.append('\\');
                }
                builder.append(current);
            }
            builder.append('$');
            return builder.toString();
        }
    }

    private record ShareResolvedPath(String share, String relativePath) {
    }
}
