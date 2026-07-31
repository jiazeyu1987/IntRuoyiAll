package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasControlAuditTaskRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasSourceDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccNasControlAuditSkippedDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccNasControlAuditTaskDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasSourceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccNasControlAuditSkippedDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccNasControlAuditTaskMapper;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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
    public static final String SOURCE_CONFIDENCE_EXACT = "EXACT";
    public static final String SOURCE_CONFIDENCE_LEGACY_EXACT = "LEGACY_EXACT";
    public static final String SOURCE_CONFIDENCE_PENDING_CONFIRMATION = "PENDING_CONFIRMATION";

    private static final List<String> FIXED_SCAN_ROOTS = List.of("1. QMS documents", "2.DHF", "3.DMR");
    private static final String REPORT_DIRECTORY = "dcc-nas-control-audit";
    private static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final long PROGRESS_FLUSH_INTERVAL = 100L;

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
            migrateLegacyNasTransferSources(config.share());
            List<DccControlledFileNasSourceMapper.ActiveNasSourceRow> sources =
                    nasSourceMapper.selectCurrentActiveSources(TenantContextHolder.getRequiredTenantId(), config.share());
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

    private void migrateLegacyNasTransferSources(String nasShareName) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<DccControlledFileNasSourceMapper.LegacyNasTransferSourceCandidate> candidates =
                nasSourceMapper.selectLegacyNasTransferCandidates(tenantId);
        Map<String, List<DccControlledFileNasSourceMapper.LegacyNasTransferSourceCandidate>> byHash = new HashMap<>();
        for (DccControlledFileNasSourceMapper.LegacyNasTransferSourceCandidate candidate : candidates) {
            String path = parseLegacyNasTransferPath(candidate.getRemark());
            if (StrUtil.isBlank(path)) {
                continue;
            }
            String normalizedPath = DccNasPathUtils.normalizeRelativePath(path);
            String pathHash = DccNasPathUtils.pathHash(nasShareName, normalizedPath);
            byHash.computeIfAbsent(pathHash, ignored -> new ArrayList<>()).add(candidate);
        }
        tx().executeWithoutResult(status -> {
            for (Map.Entry<String, List<DccControlledFileNasSourceMapper.LegacyNasTransferSourceCandidate>> entry
                    : byHash.entrySet()) {
                boolean unique = entry.getValue().size() == 1;
                for (DccControlledFileNasSourceMapper.LegacyNasTransferSourceCandidate candidate : entry.getValue()) {
                    if (nasSourceMapper.selectByControlledFileIdAndSourceType(
                            candidate.getControlledFileId(), SOURCE_TYPE_LEGACY_NAS_TRANSFER) != null) {
                        continue;
                    }
                    String normalizedPath = DccNasPathUtils.normalizeRelativePath(
                            parseLegacyNasTransferPath(candidate.getRemark()));
                    nasSourceMapper.insert(DccControlledFileNasSourceDO.builder()
                            .controlledFileId(candidate.getControlledFileId())
                            .nasShareName(nasShareName)
                            .normalizedRelativePath(normalizedPath)
                            .pathHash(entry.getKey())
                            .sourceType(SOURCE_TYPE_LEGACY_NAS_TRANSFER)
                            .sourceConfidence(unique
                                    ? SOURCE_CONFIDENCE_LEGACY_EXACT
                                    : SOURCE_CONFIDENCE_PENDING_CONFIRMATION)
                            .tenantId(tenantId)
                            .build());
                }
            }
        });
    }

    private String parseLegacyNasTransferPath(String remark) {
        String prefix = "NAS transfer source: ";
        if (!StrUtil.startWith(remark, prefix)) {
            return null;
        }
        String path = remark.substring(prefix.length()).trim();
        return StrUtil.isBlank(path) ? null : path;
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
