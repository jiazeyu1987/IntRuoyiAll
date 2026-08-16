package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasControlAuditRecognizeRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasControlAuditFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasControlAuditFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasControlAuditTaskRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryMatchRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasSourceDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccNasControlAuditFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccNasControlAuditSkippedDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccNasControlAuditTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMatchRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasSourceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccNasControlAuditFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccNasControlAuditSkippedDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccNasControlAuditTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.service.file.NasConnectionConfig;
import cn.iocoder.yudao.module.infra.service.file.NasRecursiveScanHandler;
import cn.iocoder.yudao.module.infra.service.file.NasRecursiveScanService;
import cn.iocoder.yudao.module.infra.service.file.NasRecursiveScannedFile;
import cn.iocoder.yudao.module.infra.service.file.NasRecursiveSkippedDirectory;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DccNasControlAuditServiceImpl implements DccNasControlAuditService {

    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String SOURCE_TYPE_NAS_TRANSFER = "NAS_TRANSFER";
    public static final String SOURCE_TYPE_LEGACY_NAS_TRANSFER = "LEGACY_NAS_TRANSFER";
    public static final String SOURCE_TYPE_LEGACY_LOCAL_FOLDER_IMPORT = "LEGACY_LOCAL_FOLDER_IMPORT";
    public static final String SOURCE_CONFIDENCE_EXACT = "EXACT";
    public static final String SOURCE_CONFIDENCE_LEGACY_EXACT = "LEGACY_EXACT";
    public static final String SOURCE_CONFIDENCE_PENDING_CONFIRMATION = "PENDING_CONFIRMATION";
    public static final String AUDIT_FILE_CONTROL_STATUS_NOT_CONTROLLED = "NOT_CONTROLLED";
    public static final String AUDIT_FILE_CLASSIFICATION_STATUS_PENDING_RECOGNITION = "PENDING_RECOGNITION";
    public static final String AUDIT_FILE_CLASSIFICATION_STATUS_MATCHED = "MATCHED";
    public static final String AUDIT_FILE_CLASSIFICATION_STATUS_UNCLASSIFIED_PENDING = "UNCLASSIFIED_PENDING";
    public static final String AUDIT_FILE_CLASSIFICATION_STATUS_AMBIGUOUS = "AMBIGUOUS";
    public static final String AUDIT_FILE_DOWNLOAD_STATUS_NOT_SELECTED = "NOT_SELECTED";
    public static final String AUDIT_FILE_ARCHIVE_STATUS_NOT_STARTED = "NOT_STARTED";
    public static final String AUDIT_FILE_ARCHIVE_STATUS_PENDING_MANUAL_REVIEW = "PENDING_MANUAL_REVIEW";

    private static final List<String> FIXED_SCAN_ROOTS = List.of("1. QMS documents", "2.DHF", "3.DMR");
    private static final String REPORT_DIRECTORY = "dcc-nas-control-audit";
    private static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final long PROGRESS_FLUSH_INTERVAL = 100L;
    private static final String CLASSIFICATION_REASON_MATCHED = "MATCHED";
    private static final String CLASSIFICATION_REASON_PROJECT_CODE_NOT_FOUND = "PROJECT_CODE_NOT_FOUND";
    private static final String CLASSIFICATION_REASON_PROJECT_CODE_AMBIGUOUS = "PROJECT_CODE_AMBIGUOUS";
    private static final String CLASSIFICATION_REASON_FILE_CATEGORY_NOT_FOUND = "FILE_CATEGORY_NOT_FOUND";
    private static final String CLASSIFICATION_REASON_FILE_CATEGORY_AMBIGUOUS = "FILE_CATEGORY_AMBIGUOUS";
    private static final String CATEGORY_MATCH_TYPE_CONTAINS = "CONTAINS";
    private static final String CATEGORY_MATCH_TYPE_EXACT = "EXACT";
    private static final String CATEGORY_MATCH_TYPE_PREFIX = "PREFIX";
    private static final String CATEGORY_MATCH_TYPE_SUFFIX = "SUFFIX";
    private static final String CATEGORY_MATCH_TYPE_EXTENSION = "EXTENSION";
    private static final String UNCLASSIFIED_PENDING_DIRECTORY = "_未分类待处理";

    @Resource
    private NasSettingsService nasSettingsService;
    @Resource
    private NasRecursiveScanService nasRecursiveScanService;
    @Resource
    private DccControlledFileNasSourceMapper nasSourceMapper;
    @Resource
    private DccNasControlAuditTaskMapper taskMapper;
    @Resource
    private DccNasControlAuditSkippedDirectoryMapper skippedDirectoryMapper;
    @Resource
    private DccNasControlAuditFileMapper auditFileMapper;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private DccFileCategoryMapper fileCategoryMapper;
    @Resource
    private DccFileCategoryMatchRuleMapper categoryMatchRuleMapper;
    @Resource
    private DccFileTypeTaxonomyAdminService fileTypeTaxonomyAdminService;
    @Resource
    private FileService fileService;
    @Resource
    private PlatformTransactionManager transactionManager;

    @Value("${spring.servlet.multipart.location:${java.io.tmpdir}}")
    private String multipartLocation;

    private final ReentrantLock schedulerLock = new ReentrantLock();

    @Override
    public DccNasControlAuditTaskRespVO startTask(Long userId) {
        NasConnectionConfig config = nasSettingsService.getRequiredNasConfig();
        DccNasControlAuditTaskDO activeTask = taskMapper.selectActiveTask();
        if (activeTask != null) {
            throw new IllegalStateException("nas control audit task already active: " + activeTask.getId());
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long taskId = tx().execute(status -> {
            DccNasControlAuditTaskDO task = DccNasControlAuditTaskDO.builder()
                    .operatorUserId(userId)
                    .nasShareName(config.share())
                    .scanRootsJson(JsonUtils.toJsonString(FIXED_SCAN_ROOTS))
                    .status(STATUS_WAITING)
                    .scannedFileCount(0L)
                    .controlledFileCount(0L)
                    .notControlledFileCount(0L)
                    .ambiguousFileCount(0L)
                    .sourceMissingCount(0L)
                    .skippedDirectoryCount(0L)
                    .tenantId(tenantId)
                    .build();
            taskMapper.insert(task);
            return task.getId();
        });
        triggerTaskAsync(tenantId);
        return getTask(taskId);
    }

    @Override
    public DccNasControlAuditTaskRespVO getTask(Long taskId) {
        DccNasControlAuditTaskDO task = requireTask(taskId);
        return toRespVO(task);
    }

    @Override
    public PageResult<DccNasControlAuditFileRespVO> getTaskFilePage(Long taskId,
                                                                    DccNasControlAuditFilePageReqVO reqVO) {
        requireTask(taskId);
        PageResult<DccNasControlAuditFileDO> pageResult = auditFileMapper.selectPage(taskId, reqVO);
        return new PageResult<>(pageResult.getList().stream()
                .map(this::toAuditFileRespVO)
                .toList(), pageResult.getTotal());
    }

    @Override
    public DccNasControlAuditRecognizeRespVO recognizeTaskFiles(Long taskId) {
        requireTask(taskId);
        List<DccNasControlAuditFileDO> files = nullToEmpty(auditFileMapper.selectPendingRecognitionList(taskId));
        RecognitionCounter counter = new RecognitionCounter();
        if (files.isEmpty()) {
            return counter.toRespVO();
        }
        List<DccProjectCodeDO> projectCodes = nullToEmpty(projectCodeMapper.selectEnabledList());
        List<DccFileCategoryDO> activeCategories = nullToEmpty(fileCategoryMapper.selectList()).stream()
                .filter(category -> Boolean.TRUE.equals(category.getActive()))
                .filter(category -> category.getFileTypeTaxonomyId() != null)
                .toList();
        Map<Long, List<DccFileCategoryMatchRuleDO>> rulesByCategoryId =
                nullToEmpty(categoryMatchRuleMapper.selectList()).stream()
                        .filter(rule -> Boolean.TRUE.equals(rule.getActive()))
                        .filter(rule -> rule.getCategoryId() != null)
                        .collect(Collectors.groupingBy(DccFileCategoryMatchRuleDO::getCategoryId,
                                LinkedHashMap::new, Collectors.toList()));
        for (DccNasControlAuditFileDO file : files) {
            recognizeTaskFile(file, projectCodes, activeCategories, rulesByCategoryId);
            auditFileMapper.updateById(file);
            counter.add(file.getClassificationStatus());
        }
        return counter.toRespVO();
    }

    @Override
    public DccNasControlAuditReportFile downloadReport(Long taskId) {
        DccNasControlAuditTaskDO task = requireTask(taskId);
        if (!STATUS_COMPLETED.equals(task.getStatus())) {
            throw new IllegalStateException("nas control audit task not completed: " + taskId);
        }
        if (task.getReportFileId() == null) {
            throw new IllegalStateException("nas control audit report file missing: " + taskId);
        }
        FileDO file = fileService.getFile(task.getReportFileId());
        if (file == null) {
            throw new IllegalStateException("nas control audit infra file missing: " + task.getReportFileId());
        }
        try {
            return new DccNasControlAuditReportFile(
                    StrUtil.blankToDefault(task.getReportFileName(), file.getName()),
                    fileService.getFileContent(file.getConfigId(), file.getPath()));
        } catch (Exception ex) {
            throw new IllegalStateException("NAS 受控状态统计报告读取失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void recoverInterruptedTasksOnStartup() {
        int recovered = taskMapper.recoverRunningTasksToWaiting();
        if (recovered > 0) {
            log.info("[recoverInterruptedTasksOnStartup][recoveredNasControlAuditTasks({})]", recovered);
        }
    }

    @Override
    public void processWaitingTasks() {
        if (!schedulerLock.tryLock()) {
            return;
        }
        try {
            for (DccNasControlAuditTaskDO task : taskMapper.selectWaitingTasks()) {
                try {
                    executeTask(task.getId());
                } catch (RuntimeException ex) {
                    log.error("[processWaitingTasks][taskId({}) NAS control audit failed]", task.getId(), ex);
                }
            }
        } finally {
            schedulerLock.unlock();
        }
    }

    private void triggerTaskAsync(Long tenantId) {
        CompletableFuture.runAsync(() -> TenantUtils.execute(tenantId, () -> {
            try {
                processWaitingTasks();
            } catch (RuntimeException ex) {
                log.error("[triggerTaskAsync][tenantId({}) NAS control audit async execution failed]", tenantId, ex);
            }
        }));
    }

    private void executeTask(Long taskId) {
        LocalDateTime startedAt = LocalDateTime.now();
        if (taskMapper.claimWaitingTask(taskId, startedAt) == 0) {
            return;
        }
        Path tempReport = null;
        try {
            DccNasControlAuditTaskDO task = requireTask(taskId);
            NasConnectionConfig config = nasSettingsService.getRequiredNasConfig();
            if (!Objects.equals(task.getNasShareName(), config.share())) {
                throw new IllegalStateException("NAS share changed after audit task creation: taskShare="
                        + task.getNasShareName() + ", currentShare=" + config.share());
            }
            Map<Long, ExpectedLegacyNasSource> expectedLegacySources = migrateLegacyNasSources(config.share());
            List<DccControlledFileNasSourceMapper.ActiveNasSourceRow> sources =
                    nasSourceMapper.selectCurrentActiveSources(TenantContextHolder.getRequiredTenantId(), config.share());
            verifyLegacyNasSourceBaseline(expectedLegacySources, sources);
            Map<String, List<DccControlledFileNasSourceMapper.ActiveNasSourceRow>> sourcesByHash = sources.stream()
                    .collect(Collectors.groupingBy(
                            DccControlledFileNasSourceMapper.ActiveNasSourceRow::getPathHash,
                            LinkedHashMap::new,
                            Collectors.toList()));
            Set<String> seenHashes = new HashSet<>();
            tempReport = createTempReportPath();
            try (AuditReportWriter writer = new AuditReportWriter(config.share(), FIXED_SCAN_ROOTS, startedAt)) {
                DccNasControlAuditTaskDO progress = task;
                nasRecursiveScanService.scan(config, FIXED_SCAN_ROOTS, new NasRecursiveScanHandler() {
                    @Override
                    public void onCurrentDirectory(String path) {
                        progress.setCurrentPath(path);
                        flushProgress(progress, false);
                    }

                    @Override
                    public void onFile(NasRecursiveScannedFile file) {
                        handleScannedFile(progress, writer, sourcesByHash, seenHashes, config.share(), file);
                    }

                    @Override
                    public void onSkippedDirectory(NasRecursiveSkippedDirectory directory) {
                        progress.setSkippedDirectoryCount(defaultLong(progress.getSkippedDirectoryCount()) + 1);
                        skippedDirectoryMapper.insert(DccNasControlAuditSkippedDirectoryDO.builder()
                                .taskId(progress.getId())
                                .directoryPath(directory.path())
                                .skipReason(directory.reason())
                                .skippedAt(directory.skippedAt())
                                .tenantId(TenantContextHolder.getRequiredTenantId())
                                .build());
                        writer.writeSkippedDirectory(directory);
                        flushProgress(progress, true);
                    }
                });
                writeSourceMissingRows(progress, writer, sources, seenHashes);
                writer.writeSummary(progress);
                writer.writeTo(tempReport);
            }
            String reportFileName = "NAS受控状态统计-" + taskId + ".xlsx";
            Long reportFileId = fileService.createFileAndReturnId(
                    tempReport,
                    Files.size(tempReport),
                    reportFileName,
                    REPORT_DIRECTORY,
                    EXCEL_CONTENT_TYPE);
            DccNasControlAuditTaskDO completed = requireTask(taskId);
            completed.setStatus(STATUS_COMPLETED);
            completed.setCurrentPath(null);
            completed.setReportFileId(reportFileId);
            completed.setReportFileName(reportFileName);
            completed.setCompletedAt(LocalDateTime.now());
            completed.setFailureReason(null);
            taskMapper.updateById(completed);
        } catch (RuntimeException | IOException ex) {
            markTaskFailed(taskId, resolveThrowableMessage(ex));
        } finally {
            if (tempReport != null) {
                deleteIfExists(tempReport);
            }
        }
    }

    private void handleScannedFile(DccNasControlAuditTaskDO progress,
                                   AuditReportWriter writer,
                                   Map<String, List<DccControlledFileNasSourceMapper.ActiveNasSourceRow>> sourcesByHash,
                                   Set<String> seenHashes,
                                   String nasShareName,
                                   NasRecursiveScannedFile file) {
        String normalizedPath = DccNasPathUtils.normalizeRelativePath(file.path());
        String pathHash = DccNasPathUtils.pathHash(nasShareName, normalizedPath);
        seenHashes.add(pathHash);
        progress.setScannedFileCount(defaultLong(progress.getScannedFileCount()) + 1);
        List<DccControlledFileNasSourceMapper.ActiveNasSourceRow> matches =
                sourcesByHash.getOrDefault(pathHash, List.of());
        if (matches.isEmpty()) {
            progress.setNotControlledFileCount(defaultLong(progress.getNotControlledFileCount()) + 1);
            persistNotControlledAuditFile(progress.getId(), nasShareName, file, normalizedPath, pathHash);
            writer.writeNotControlled(file, "NAS 路径没有对应的当前 ACTIVE 受控文件");
        } else if (matches.size() == 1 && isExactSource(matches.get(0))) {
            progress.setControlledFileCount(defaultLong(progress.getControlledFileCount()) + 1);
        } else {
            progress.setAmbiguousFileCount(defaultLong(progress.getAmbiguousFileCount()) + 1);
            writer.writeAmbiguous(file.path(), matches,
                    "同一路径对应多个受控记录或存在待确认来源，不能确认唯一受控文件");
        }
        flushProgress(progress, defaultLong(progress.getScannedFileCount()) % PROGRESS_FLUSH_INTERVAL == 0);
    }

    private void persistNotControlledAuditFile(Long taskId,
                                               String nasShareName,
                                               NasRecursiveScannedFile file,
                                               String normalizedPath,
                                               String pathHash) {
        if (file.size() == null || file.modifiedAt() == null) {
            throw new IllegalStateException("NAS audit file snapshot missing size or modified time: " + file.path());
        }
        auditFileMapper.insert(DccNasControlAuditFileDO.builder()
                .taskId(taskId)
                .nasShareName(nasShareName)
                .rootPath(file.rootPath())
                .normalizedRelativePath(normalizedPath)
                .pathHash(pathHash)
                .fileName(file.name())
                .fileSize(file.size())
                .modifiedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(file.modifiedAt()), ZoneOffset.UTC))
                .sourceSignature(sourceSignature(pathHash, file.size(), file.modifiedAt()))
                .controlStatus(AUDIT_FILE_CONTROL_STATUS_NOT_CONTROLLED)
                .classificationStatus(AUDIT_FILE_CLASSIFICATION_STATUS_PENDING_RECOGNITION)
                .downloadStatus(AUDIT_FILE_DOWNLOAD_STATUS_NOT_SELECTED)
                .archiveStatus(AUDIT_FILE_ARCHIVE_STATUS_NOT_STARTED)
                .tenantId(TenantContextHolder.getRequiredTenantId())
                .build());
    }

    private void recognizeTaskFile(DccNasControlAuditFileDO file,
                                   List<DccProjectCodeDO> projectCodes,
                                   List<DccFileCategoryDO> activeCategories,
                                   Map<Long, List<DccFileCategoryMatchRuleDO>> rulesByCategoryId) {
        resetRecognitionResult(file);
        List<ProjectCandidate> projectCandidates = resolveProjectCandidates(file, projectCodes);
        if (projectCandidates.isEmpty()) {
            applyUnclassifiedPending(file, CLASSIFICATION_REASON_PROJECT_CODE_NOT_FOUND,
                    projectCandidates, List.of());
            return;
        }
        if (projectCandidates.size() > 1) {
            applyAmbiguous(file, CLASSIFICATION_REASON_PROJECT_CODE_AMBIGUOUS,
                    projectCandidates, List.of());
            return;
        }

        DccProjectCodeDO projectCode = projectCandidates.get(0).projectCode();
        file.setMatchedProjectCodeId(projectCode.getId());
        CategoryRecognitionResult categoryResult = resolveCategory(file, activeCategories, rulesByCategoryId);
        if (categoryResult.candidates().isEmpty()) {
            applyUnclassifiedPending(file, CLASSIFICATION_REASON_FILE_CATEGORY_NOT_FOUND,
                    projectCandidates, categoryResult.candidates());
            return;
        }
        if (categoryResult.ambiguous()) {
            applyAmbiguous(file, CLASSIFICATION_REASON_FILE_CATEGORY_AMBIGUOUS,
                    projectCandidates, categoryResult.candidates());
            return;
        }

        DccFileCategoryDO category = categoryResult.bestCandidate().category();
        DccFileTypeTaxonomyPath taxonomyPath = fileTypeTaxonomyAdminService.resolveActivePath(
                category.getFileTypeTaxonomyId());
        if (taxonomyPath == null || taxonomyPath.id() == null) {
            throw new IllegalStateException("DCC file type taxonomy path missing: " + category.getFileTypeTaxonomyId());
        }
        file.setClassificationStatus(AUDIT_FILE_CLASSIFICATION_STATUS_MATCHED);
        file.setClassificationReason(CLASSIFICATION_REASON_MATCHED);
        file.setClassificationCandidatesJson(candidateSnapshotJson(projectCandidates, categoryResult.candidates()));
        file.setMatchedFileTypeTaxonomyId(taxonomyPath.id());
        file.setMatchedFileTypeLevel1(taxonomyPath.level1());
        file.setMatchedFileTypeLevel2(taxonomyPath.level2());
        file.setMatchedFileTypeLevel3(taxonomyPath.level3());
        file.setMatchedFileTypeLevel4(taxonomyPath.level4());
        file.setMatchedFileTypeLevel5(taxonomyPath.level5());
        file.setExpectedLocalRelativePath(resolveMatchedLocalRelativePath(file, projectCode, taxonomyPath, category));
    }

    private void resetRecognitionResult(DccNasControlAuditFileDO file) {
        file.setMatchedProjectCodeId(null);
        file.setMatchedFileTypeTaxonomyId(null);
        file.setMatchedFileTypeLevel1(null);
        file.setMatchedFileTypeLevel2(null);
        file.setMatchedFileTypeLevel3(null);
        file.setMatchedFileTypeLevel4(null);
        file.setMatchedFileTypeLevel5(null);
        file.setClassificationReason(null);
        file.setClassificationCandidatesJson(null);
        file.setExpectedLocalRelativePath(null);
    }

    private void applyUnclassifiedPending(DccNasControlAuditFileDO file,
                                          String reason,
                                          List<ProjectCandidate> projectCandidates,
                                          List<CategoryCandidate> categoryCandidates) {
        file.setClassificationStatus(AUDIT_FILE_CLASSIFICATION_STATUS_UNCLASSIFIED_PENDING);
        file.setClassificationReason(reason);
        file.setClassificationCandidatesJson(candidateSnapshotJson(projectCandidates, categoryCandidates));
        file.setExpectedLocalRelativePath(resolvePendingLocalRelativePath(file));
        file.setArchiveStatus(AUDIT_FILE_ARCHIVE_STATUS_PENDING_MANUAL_REVIEW);
        file.setArchiveErrorCode(null);
        file.setArchiveError(null);
        file.setControlledFileId(null);
    }

    private void applyAmbiguous(DccNasControlAuditFileDO file,
                                String reason,
                                List<ProjectCandidate> projectCandidates,
                                List<CategoryCandidate> categoryCandidates) {
        file.setClassificationStatus(AUDIT_FILE_CLASSIFICATION_STATUS_AMBIGUOUS);
        file.setClassificationReason(reason);
        file.setClassificationCandidatesJson(candidateSnapshotJson(projectCandidates, categoryCandidates));
        file.setExpectedLocalRelativePath(resolvePendingLocalRelativePath(file));
        file.setArchiveStatus(AUDIT_FILE_ARCHIVE_STATUS_PENDING_MANUAL_REVIEW);
        file.setArchiveErrorCode(null);
        file.setArchiveError(null);
        file.setControlledFileId(null);
    }

    private List<ProjectCandidate> resolveProjectCandidates(DccNasControlAuditFileDO file,
                                                            List<DccProjectCodeDO> projectCodes) {
        String searchText = projectSearchText(file);
        return projectCodes.stream()
                .filter(projectCode -> projectCodeMatches(searchText, projectCode))
                .map(ProjectCandidate::new)
                .sorted(Comparator.comparing(candidate -> candidate.projectCode().getId(),
                        Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private boolean projectCodeMatches(String searchText, DccProjectCodeDO projectCode) {
        return containsWithTokenBoundary(searchText, projectCode.getProjectCode())
                || containsWithTokenBoundary(searchText, projectCode.getProjectName());
    }

    private String projectSearchText(DccNasControlAuditFileDO file) {
        String normalizedPath = DccNasPathUtils.normalizeRelativePath(file.getNormalizedRelativePath());
        return (normalizedPath + "/" + StrUtil.nullToEmpty(file.getFileName())).toLowerCase(Locale.ROOT);
    }

    private boolean containsWithTokenBoundary(String text, String needle) {
        String normalizedNeedle = StrUtil.trimToNull(needle);
        if (normalizedNeedle == null) {
            return false;
        }
        String lowerNeedle = normalizedNeedle.toLowerCase(Locale.ROOT);
        int fromIndex = 0;
        while (fromIndex <= text.length() - lowerNeedle.length()) {
            int index = text.indexOf(lowerNeedle, fromIndex);
            if (index < 0) {
                return false;
            }
            int end = index + lowerNeedle.length();
            boolean beforeBoundary = index == 0 || !Character.isLetterOrDigit(text.charAt(index - 1));
            boolean afterBoundary = end == text.length() || !Character.isLetterOrDigit(text.charAt(end));
            if (beforeBoundary && afterBoundary) {
                return true;
            }
            fromIndex = index + 1;
        }
        return false;
    }

    private CategoryRecognitionResult resolveCategory(
            DccNasControlAuditFileDO file,
            List<DccFileCategoryDO> activeCategories,
            Map<Long, List<DccFileCategoryMatchRuleDO>> rulesByCategoryId) {
        List<String> normalizedTexts = resolveAuditFileNormalizedMatchTexts(file);
        List<String> rawTexts = resolveAuditFileRawMatchTexts(file);
        List<CategoryCandidate> candidates = activeCategories.stream()
                .map(category -> new CategoryCandidate(category, categoryMatchScore(
                        normalizedTexts, rawTexts, category, rulesByCategoryId.getOrDefault(category.getId(), List.of()))))
                .filter(candidate -> candidate.score() > 0)
                .sorted(Comparator.comparingInt(CategoryCandidate::score).reversed()
                        .thenComparing(candidate -> candidate.category().getId(), Comparator.nullsLast(Long::compareTo)))
                .toList();
        if (candidates.isEmpty()) {
            return new CategoryRecognitionResult(candidates, null, false);
        }
        int bestScore = candidates.get(0).score();
        List<CategoryCandidate> bestCandidates = candidates.stream()
                .filter(candidate -> candidate.score() == bestScore)
                .toList();
        return new CategoryRecognitionResult(candidates, bestCandidates.get(0), bestCandidates.size() > 1);
    }

    private int categoryMatchScore(List<String> normalizedTexts,
                                   List<String> rawTexts,
                                   DccFileCategoryDO category,
                                   List<DccFileCategoryMatchRuleDO> rules) {
        int categoryNameScore = resolveCategoryMatchNames(category).stream()
                .filter(matchName -> normalizedTexts.stream().anyMatch(text -> text.contains(matchName)))
                .mapToInt(String::length)
                .max()
                .orElse(0);
        int ruleScore = rules.stream()
                .mapToInt(rule -> categoryMatchRuleScore(rule, normalizedTexts, rawTexts))
                .max()
                .orElse(0);
        return Math.max(categoryNameScore, ruleScore);
    }

    private List<String> resolveCategoryMatchNames(DccFileCategoryDO category) {
        List<String> names = new ArrayList<>();
        addNormalizedCategoryMatchName(names, category.getName());
        addNormalizedCategoryMatchName(names, category.getCode());
        return names;
    }

    private void addNormalizedCategoryMatchName(List<String> names, String rawName) {
        String normalized = normalizeCategoryMatchText(rawName);
        if (normalized != null && !names.contains(normalized)) {
            names.add(normalized);
        }
    }

    private int categoryMatchRuleScore(DccFileCategoryMatchRuleDO rule,
                                       List<String> normalizedTexts,
                                       List<String> rawTexts) {
        String matchType = StrUtil.trimToNull(rule.getMatchType());
        if (matchType == null) {
            throw new IllegalStateException("DCC file category match rule has blank matchType: " + rule.getId());
        }
        return switch (matchType.toUpperCase(Locale.ROOT)) {
            case CATEGORY_MATCH_TYPE_CONTAINS, CATEGORY_MATCH_TYPE_EXACT, CATEGORY_MATCH_TYPE_PREFIX,
                    CATEGORY_MATCH_TYPE_SUFFIX -> categoryTextRuleScore(rule, normalizedTexts,
                    matchType.toUpperCase(Locale.ROOT));
            case CATEGORY_MATCH_TYPE_EXTENSION -> categoryExtensionRuleScore(rule, rawTexts);
            default -> throw new IllegalStateException(
                    "Unsupported DCC file category match rule type: " + rule.getMatchType());
        };
    }

    private int categoryTextRuleScore(DccFileCategoryMatchRuleDO rule,
                                      List<String> normalizedTexts,
                                      String matchType) {
        String matchText = normalizeCategoryMatchText(rule.getMatchText());
        if (matchText == null) {
            throw new IllegalStateException("DCC file category match rule has blank matchText: " + rule.getId());
        }
        boolean matched = switch (matchType) {
            case CATEGORY_MATCH_TYPE_CONTAINS -> normalizedTexts.stream().anyMatch(text -> text.contains(matchText));
            case CATEGORY_MATCH_TYPE_EXACT -> normalizedTexts.stream().anyMatch(text -> Objects.equals(text, matchText));
            case CATEGORY_MATCH_TYPE_PREFIX -> normalizedTexts.stream().anyMatch(text -> text.startsWith(matchText));
            case CATEGORY_MATCH_TYPE_SUFFIX -> normalizedTexts.stream().anyMatch(text -> text.endsWith(matchText));
            default -> throw new IllegalStateException(
                    "Unsupported DCC file category text match rule type: " + matchType);
        };
        return matched ? categoryMatchRuleBaseScore(rule, matchText) : 0;
    }

    private int categoryExtensionRuleScore(DccFileCategoryMatchRuleDO rule, List<String> rawTexts) {
        String extension = normalizeRuleExtension(rule.getMatchText());
        if (extension == null) {
            throw new IllegalStateException("DCC file category extension rule has blank matchText: " + rule.getId());
        }
        return rawTexts.stream()
                .map(this::extractFileExtension)
                .filter(Objects::nonNull)
                .anyMatch(extension::equals)
                ? categoryMatchRuleBaseScore(rule, extension)
                : 0;
    }

    private int categoryMatchRuleBaseScore(DccFileCategoryMatchRuleDO rule, String normalizedMatchText) {
        int weight = rule.getWeight() == null ? 0 : rule.getWeight();
        return weight + normalizedMatchText.length();
    }

    private String normalizeRuleExtension(String value) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        return StrUtil.trimToNull(normalized.toLowerCase(Locale.ROOT));
    }

    private String extractFileExtension(String value) {
        String fileName = StrUtil.trimToNull(value);
        if (fileName == null) {
            return null;
        }
        int slashIndex = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= slashIndex || dotIndex == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private List<String> resolveAuditFileNormalizedMatchTexts(DccNasControlAuditFileDO file) {
        return resolveAuditFileRawMatchTexts(file).stream()
                .map(this::normalizeCategoryMatchText)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<String> resolveAuditFileRawMatchTexts(DccNasControlAuditFileDO file) {
        List<String> texts = new ArrayList<>();
        String normalizedPath = DccNasPathUtils.normalizeRelativePath(file.getNormalizedRelativePath());
        addMatchText(texts, normalizedPath);
        addMatchText(texts, file.getFileName());
        for (String part : normalizedPath.split("/")) {
            addMatchText(texts, part);
        }
        return texts;
    }

    private void addMatchText(List<String> texts, String value) {
        String text = StrUtil.trimToNull(value);
        if (text != null && !texts.contains(text)) {
            texts.add(text);
        }
    }

    private String normalizeCategoryMatchText(String value) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        String withoutExtension = normalized.replaceFirst("\\.[^.\\\\/]+$", "");
        return StrUtil.trimToNull(withoutExtension
                .replace("（", "(")
                .replace("）", ")")
                .replace(" ", "")
                .replace("\u3000", "")
                .toLowerCase(Locale.ROOT));
    }

    private String candidateSnapshotJson(List<ProjectCandidate> projectCandidates,
                                         List<CategoryCandidate> categoryCandidates) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("projectCandidates", projectCandidates.stream()
                .map(this::projectCandidateSnapshot)
                .toList());
        snapshot.put("categoryCandidates", categoryCandidates.stream()
                .map(this::categoryCandidateSnapshot)
                .toList());
        return JsonUtils.toJsonString(snapshot);
    }

    private Map<String, Object> projectCandidateSnapshot(ProjectCandidate candidate) {
        DccProjectCodeDO projectCode = candidate.projectCode();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", projectCode.getId());
        snapshot.put("projectCode", projectCode.getProjectCode());
        snapshot.put("projectName", projectCode.getProjectName());
        return snapshot;
    }

    private Map<String, Object> categoryCandidateSnapshot(CategoryCandidate candidate) {
        DccFileCategoryDO category = candidate.category();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", category.getId());
        snapshot.put("code", category.getCode());
        snapshot.put("name", category.getName());
        snapshot.put("fileTypeTaxonomyId", category.getFileTypeTaxonomyId());
        snapshot.put("score", candidate.score());
        return snapshot;
    }

    private String resolveMatchedLocalRelativePath(DccNasControlAuditFileDO file,
                                                   DccProjectCodeDO projectCode,
                                                   DccFileTypeTaxonomyPath taxonomyPath,
                                                   DccFileCategoryDO category) {
        String projectName = StrUtil.blankToDefault(projectCode.getProjectCode(), projectCode.getProjectName());
        String projectSegment = safePathSegment(projectName) + "__" + projectCode.getId();
        String categoryName = firstNonBlank(taxonomyPath.level5(), taxonomyPath.level4(), taxonomyPath.level3(),
                taxonomyPath.level2(), taxonomyPath.level1(), category.getName());
        String categorySegment = taxonomyPath.id() + "__" + safePathSegment(categoryName);
        return projectSegment + "/" + categorySegment + "/" + safeRelativePath(file.getNormalizedRelativePath());
    }

    private String resolvePendingLocalRelativePath(DccNasControlAuditFileDO file) {
        return UNCLASSIFIED_PENDING_DIRECTORY + "/" + safeRelativePath(file.getNormalizedRelativePath());
    }

    private String safeRelativePath(String path) {
        String normalizedPath = DccNasPathUtils.normalizeRelativePath(path);
        if (StrUtil.isBlank(normalizedPath)) {
            normalizedPath = "unnamed";
        }
        List<String> safeParts = new ArrayList<>();
        for (String part : normalizedPath.split("/")) {
            safeParts.add(safePathSegment(part));
        }
        return String.join("/", safeParts);
    }

    private String safePathSegment(String value) {
        String raw = StrUtil.trimToNull(value);
        if (raw == null) {
            return "_";
        }
        StringBuilder builder = new StringBuilder(raw.length());
        for (int index = 0; index < raw.length(); index++) {
            char ch = raw.charAt(index);
            if (ch < 32 || ch == '/' || ch == '\\' || ch == ':' || ch == '*' || ch == '?'
                    || ch == '"' || ch == '<' || ch == '>' || ch == '|') {
                builder.append('_');
            } else {
                builder.append(ch);
            }
        }
        String safe = StrUtil.trimToNull(builder.toString());
        return safe == null ? "_" : safe;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String candidate = StrUtil.trimToNull(value);
            if (candidate != null) {
                return candidate;
            }
        }
        return "_";
    }

    private <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String sourceSignature(String pathHash, Long fileSize, Long modifiedAtUtcEpochMillis) {
        String payload = pathHash + "|" + fileSize + "|" + modifiedAtUtcEpochMillis;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    private void writeSourceMissingRows(DccNasControlAuditTaskDO progress,
                                        AuditReportWriter writer,
                                        List<DccControlledFileNasSourceMapper.ActiveNasSourceRow> sources,
                                        Set<String> seenHashes) {
        for (DccControlledFileNasSourceMapper.ActiveNasSourceRow source : sources) {
            if (!isExactSource(source) || seenHashes.contains(source.getPathHash())) {
                continue;
            }
            progress.setSourceMissingCount(defaultLong(progress.getSourceMissingCount()) + 1);
            writer.writeSourceMissing(source);
        }
        flushProgress(progress, true);
    }

    private boolean isExactSource(DccControlledFileNasSourceMapper.ActiveNasSourceRow source) {
        return SOURCE_CONFIDENCE_EXACT.equals(source.getSourceConfidence())
                || SOURCE_CONFIDENCE_LEGACY_EXACT.equals(source.getSourceConfidence());
    }

    private Map<Long, ExpectedLegacyNasSource> migrateLegacyNasSources(String nasShareName) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<DccControlledFileNasSourceMapper.LegacyNasSourceCandidate> candidates =
                nasSourceMapper.selectLegacyNasSourceCandidates(tenantId, nasShareName);
        Map<String, List<LegacyNasSourceMigration>> byHash = new HashMap<>();
        for (DccControlledFileNasSourceMapper.LegacyNasSourceCandidate candidate : candidates) {
            LegacyNasSourceEvidence evidence = parseLegacyNasSourceEvidence(candidate.getRemark());
            if (evidence == null) {
                continue;
            }
            String pathHash = DccNasPathUtils.pathHash(nasShareName, evidence.normalizedPath());
            byHash.computeIfAbsent(pathHash, ignored -> new ArrayList<>())
                    .add(new LegacyNasSourceMigration(candidate, evidence));
        }
        Map<Long, ExpectedLegacyNasSource> expectedSources = new LinkedHashMap<>();
        tx().executeWithoutResult(status -> {
            for (Map.Entry<String, List<LegacyNasSourceMigration>> entry
                    : byHash.entrySet()) {
                boolean unique = entry.getValue().size() == 1;
                String confidence = unique
                        ? SOURCE_CONFIDENCE_LEGACY_EXACT
                        : SOURCE_CONFIDENCE_PENDING_CONFIRMATION;
                for (LegacyNasSourceMigration migration : entry.getValue()) {
                    DccControlledFileNasSourceMapper.LegacyNasSourceCandidate candidate = migration.candidate();
                    String sourceType = migration.evidence().sourceType();
                    expectedSources.put(candidate.getControlledFileId(),
                            new ExpectedLegacyNasSource(entry.getKey(), sourceType, confidence));
                    if (nasSourceMapper.selectByControlledFileIdAndShareAndSourceType(
                            candidate.getControlledFileId(), nasShareName, sourceType) != null) {
                        continue;
                    }
                    nasSourceMapper.insert(DccControlledFileNasSourceDO.builder()
                            .controlledFileId(candidate.getControlledFileId())
                            .nasShareName(nasShareName)
                            .normalizedRelativePath(migration.evidence().normalizedPath())
                            .pathHash(entry.getKey())
                            .sourceType(sourceType)
                            .sourceConfidence(confidence)
                            .tenantId(tenantId)
                            .build());
                }
            }
        });
        return expectedSources;
    }

    private LegacyNasSourceEvidence parseLegacyNasSourceEvidence(String remark) {
        String sourceType;
        String prefix;
        if (StrUtil.startWith(remark, "NAS transfer source: ")) {
            sourceType = SOURCE_TYPE_LEGACY_NAS_TRANSFER;
            prefix = "NAS transfer source: ";
        } else if (StrUtil.startWith(remark, "Local folder import source: ")) {
            sourceType = SOURCE_TYPE_LEGACY_LOCAL_FOLDER_IMPORT;
            prefix = "Local folder import source: ";
        } else {
            return null;
        }
        String path = remark.substring(prefix.length()).trim();
        if (StrUtil.isBlank(path)) {
            return null;
        }
        String normalizedPath = DccNasPathUtils.normalizeRelativePath(path);
        if (!isUnderFixedScanRoot(normalizedPath)) {
            return null;
        }
        return new LegacyNasSourceEvidence(normalizedPath, sourceType);
    }

    private boolean isUnderFixedScanRoot(String normalizedPath) {
        return FIXED_SCAN_ROOTS.stream().anyMatch(root -> normalizedPath.equalsIgnoreCase(root)
                || normalizedPath.regionMatches(true, 0, root + "/", 0, root.length() + 1));
    }

    private void verifyLegacyNasSourceBaseline(
            Map<Long, ExpectedLegacyNasSource> expectedSources,
            List<DccControlledFileNasSourceMapper.ActiveNasSourceRow> activeSources) {
        if (expectedSources.isEmpty()) {
            return;
        }
        Map<Long, List<DccControlledFileNasSourceMapper.ActiveNasSourceRow>> sourcesByControlledFileId =
                activeSources.stream().collect(Collectors.groupingBy(
                        DccControlledFileNasSourceMapper.ActiveNasSourceRow::getControlledFileId));
        List<Long> missingControlledFileIds = expectedSources.entrySet().stream()
                .filter(entry -> sourcesByControlledFileId.getOrDefault(entry.getKey(), List.of()).stream()
                        .noneMatch(source -> entry.getValue().matches(source)))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (!missingControlledFileIds.isEmpty()) {
            List<Long> sampleIds = missingControlledFileIds.stream().limit(10).toList();
            throw new IllegalStateException("NAS 受控来源基线不完整：应迁移 " + expectedSources.size()
                    + " 条，迁移后缺少 " + missingControlledFileIds.size()
                    + " 条，受控文件ID示例=" + sampleIds);
        }
    }

    private record LegacyNasSourceEvidence(String normalizedPath, String sourceType) {
    }

    private record LegacyNasSourceMigration(
            DccControlledFileNasSourceMapper.LegacyNasSourceCandidate candidate,
            LegacyNasSourceEvidence evidence) {
    }

    private record ExpectedLegacyNasSource(String pathHash, String sourceType, String sourceConfidence) {

        private boolean matches(DccControlledFileNasSourceMapper.ActiveNasSourceRow source) {
            return Objects.equals(pathHash, source.getPathHash())
                    && Objects.equals(sourceType, source.getSourceType())
                    && Objects.equals(sourceConfidence, source.getSourceConfidence());
        }
    }

    private DccNasControlAuditTaskDO requireTask(Long taskId) {
        DccNasControlAuditTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalStateException("nas control audit task not found: " + taskId);
        }
        return task;
    }

    private DccNasControlAuditTaskRespVO toRespVO(DccNasControlAuditTaskDO task) {
        DccNasControlAuditTaskRespVO respVO = new DccNasControlAuditTaskRespVO();
        respVO.setTaskId(task.getId());
        respVO.setStatus(task.getStatus());
        respVO.setNasShareName(task.getNasShareName());
        respVO.setScanRoots(JsonUtils.parseArray(StrUtil.blankToDefault(task.getScanRootsJson(), "[]"), String.class));
        respVO.setCurrentPath(task.getCurrentPath());
        respVO.setScannedFileCount(defaultLong(task.getScannedFileCount()));
        respVO.setControlledFileCount(defaultLong(task.getControlledFileCount()));
        respVO.setNotControlledFileCount(defaultLong(task.getNotControlledFileCount()));
        respVO.setAmbiguousFileCount(defaultLong(task.getAmbiguousFileCount()));
        respVO.setSourceMissingCount(defaultLong(task.getSourceMissingCount()));
        respVO.setSkippedDirectoryCount(defaultLong(task.getSkippedDirectoryCount()));
        respVO.setReportFileName(task.getReportFileName());
        respVO.setStartedAt(task.getStartedAt() == null ? null : task.getStartedAt().toString());
        respVO.setCompletedAt(task.getCompletedAt() == null ? null : task.getCompletedAt().toString());
        respVO.setFailureReason(task.getFailureReason());
        return respVO;
    }

    private DccNasControlAuditFileRespVO toAuditFileRespVO(DccNasControlAuditFileDO file) {
        DccNasControlAuditFileRespVO respVO = BeanUtils.toBean(file, DccNasControlAuditFileRespVO.class);
        respVO.setAuditFileId(file.getId());
        return respVO;
    }

    private void flushProgress(DccNasControlAuditTaskDO progress, boolean force) {
        if (!force && defaultLong(progress.getScannedFileCount()) % PROGRESS_FLUSH_INTERVAL != 0) {
            return;
        }
        taskMapper.updateById(progress);
    }

    private void markTaskFailed(Long taskId, String reason) {
        DccNasControlAuditTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(STATUS_FAILED);
        task.setCompletedAt(LocalDateTime.now());
        task.setFailureReason(fitMessage(reason));
        task.setReportFileId(null);
        task.setReportFileName(null);
        taskMapper.updateById(task);
    }

    private Path createTempReportPath() throws IOException {
        Path directory = Path.of(StrUtil.blankToDefault(multipartLocation, System.getProperty("java.io.tmpdir")));
        Files.createDirectories(directory);
        return Files.createTempFile(directory, "dcc-nas-control-audit-", ".xlsx");
    }

    private void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("[deleteIfExists][path({}) temporary audit report deletion failed]", path, ex);
        }
    }

    private String resolveThrowableMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown error";
        }
        return StrUtil.blankToDefault(throwable.getMessage(), throwable.getClass().getSimpleName());
    }

    private String fitMessage(String message) {
        String raw = StrUtil.blankToDefault(message, "unknown error");
        if (raw.length() <= 512) {
            return raw;
        }
        return raw.substring(0, 500) + "...[truncated]";
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private TransactionTemplate tx() {
        return new TransactionTemplate(transactionManager);
    }

    private record ProjectCandidate(DccProjectCodeDO projectCode) {
    }

    private record CategoryCandidate(DccFileCategoryDO category, int score) {
    }

    private record CategoryRecognitionResult(List<CategoryCandidate> candidates,
                                             CategoryCandidate bestCandidate,
                                             boolean ambiguous) {
    }

    private static final class RecognitionCounter {

        private long matchedCount;
        private long unclassifiedPendingCount;
        private long ambiguousCount;
        private long skippedCount;

        private void add(String classificationStatus) {
            if (AUDIT_FILE_CLASSIFICATION_STATUS_MATCHED.equals(classificationStatus)) {
                matchedCount++;
            } else if (AUDIT_FILE_CLASSIFICATION_STATUS_UNCLASSIFIED_PENDING.equals(classificationStatus)) {
                unclassifiedPendingCount++;
            } else if (AUDIT_FILE_CLASSIFICATION_STATUS_AMBIGUOUS.equals(classificationStatus)) {
                ambiguousCount++;
            } else {
                skippedCount++;
            }
        }

        private DccNasControlAuditRecognizeRespVO toRespVO() {
            DccNasControlAuditRecognizeRespVO respVO = new DccNasControlAuditRecognizeRespVO();
            respVO.setMatchedCount(matchedCount);
            respVO.setUnclassifiedPendingCount(unclassifiedPendingCount);
            respVO.setAmbiguousCount(ambiguousCount);
            respVO.setSkippedCount(skippedCount);
            return respVO;
        }
    }

    private static final class AuditReportWriter implements AutoCloseable {

        private final SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        private final String nasShareName;
        private final List<String> roots;
        private final LocalDateTime startedAt;
        private final Sheet summarySheet;
        private final Sheet notControlledSheet;
        private final Sheet ambiguousSheet;
        private final Sheet sourceMissingSheet;
        private final Sheet skippedSheet;
        private int notControlledRowIndex = 1;
        private int ambiguousRowIndex = 1;
        private int sourceMissingRowIndex = 1;
        private int skippedRowIndex = 1;

        private AuditReportWriter(String nasShareName, List<String> roots, LocalDateTime startedAt) {
            this.nasShareName = nasShareName;
            this.roots = roots;
            this.startedAt = startedAt;
            this.summarySheet = workbook.createSheet("统计汇总");
            this.notControlledSheet = workbook.createSheet("未受控文件");
            this.ambiguousSheet = workbook.createSheet("待确认文件");
            this.sourceMissingSheet = workbook.createSheet("来源缺失");
            this.skippedSheet = workbook.createSheet("跳过目录");
            writeHeader(notControlledSheet, "根目录", "完整路径", "文件名", "大小", "修改时间", "判定原因");
            writeHeader(ambiguousSheet, "路径", "冲突的受控文件编号", "冲突原因");
            writeHeader(sourceMissingSheet, "受控文件编号", "文件名", "版本", "登记的 NAS 路径");
            writeHeader(skippedSheet, "目录路径", "跳过原因", "跳过时间");
        }

        private void writeNotControlled(NasRecursiveScannedFile file, String reason) {
            Row row = notControlledSheet.createRow(notControlledRowIndex++);
            writeCell(row, 0, file.rootPath());
            writeCell(row, 1, file.path());
            writeCell(row, 2, file.name());
            writeCell(row, 3, file.size());
            writeCell(row, 4, file.modifiedAt() == null ? null : new Date(file.modifiedAt()));
            writeCell(row, 5, reason);
        }

        private void writeAmbiguous(String path, List<DccControlledFileNasSourceMapper.ActiveNasSourceRow> rows,
                                    String reason) {
            Row row = ambiguousSheet.createRow(ambiguousRowIndex++);
            writeCell(row, 0, path);
            writeCell(row, 1, rows.stream()
                    .map(DccControlledFileNasSourceMapper.ActiveNasSourceRow::getControlledFileId)
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
            writeCell(row, 2, reason);
        }

        private void writeSourceMissing(DccControlledFileNasSourceMapper.ActiveNasSourceRow source) {
            Row row = sourceMissingSheet.createRow(sourceMissingRowIndex++);
            writeCell(row, 0, source.getControlledFileId());
            writeCell(row, 1, source.getFileName());
            writeCell(row, 2, source.getVersionNo());
            writeCell(row, 3, source.getNormalizedRelativePath());
        }

        private void writeSkippedDirectory(NasRecursiveSkippedDirectory directory) {
            Row row = skippedSheet.createRow(skippedRowIndex++);
            writeCell(row, 0, directory.path());
            writeCell(row, 1, directory.reason());
            writeCell(row, 2, directory.skippedAt() == null ? null : directory.skippedAt().toString());
        }

        private void writeSummary(DccNasControlAuditTaskDO task) {
            int rowIndex = 0;
            writeSummaryRow(rowIndex++, "扫描时间", startedAt.toString());
            writeSummaryRow(rowIndex++, "NAS 共享", nasShareName);
            writeSummaryRow(rowIndex++, "三个扫描根目录", String.join(", ", roots));
            writeSummaryRow(rowIndex++, "文件总数", task.getScannedFileCount());
            writeSummaryRow(rowIndex++, "已受控数量", task.getControlledFileCount());
            writeSummaryRow(rowIndex++, "未受控数量", task.getNotControlledFileCount());
            writeSummaryRow(rowIndex++, "待确认数量", task.getAmbiguousFileCount());
            writeSummaryRow(rowIndex++, "来源缺失数量", task.getSourceMissingCount());
            writeSummaryRow(rowIndex++, "跳过目录数量", task.getSkippedDirectoryCount());
            writeSummaryRow(rowIndex, "无法扫描的文件数量", "未知");
        }

        private void writeSummaryRow(int rowIndex, String label, Object value) {
            Row row = summarySheet.createRow(rowIndex);
            writeCell(row, 0, label);
            writeCell(row, 1, value);
        }

        private void writeHeader(Sheet sheet, String... headers) {
            Row row = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                writeCell(row, i, headers[i]);
            }
        }

        private void writeCell(Row row, int columnIndex, Object value) {
            if (value == null) {
                return;
            }
            if (value instanceof Number number) {
                row.createCell(columnIndex).setCellValue(number.doubleValue());
                return;
            }
            if (value instanceof Date date) {
                row.createCell(columnIndex).setCellValue(date);
                return;
            }
            row.createCell(columnIndex).setCellValue(String.valueOf(value));
        }

        private void writeTo(Path path) throws IOException {
            try (OutputStream outputStream = Files.newOutputStream(path)) {
                workbook.write(outputStream);
            }
        }

        @Override
        public void close() throws IOException {
            workbook.dispose();
            workbook.close();
        }
    }
}
