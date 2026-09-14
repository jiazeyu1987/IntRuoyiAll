package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasControlAuditRecognizeRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryMatchRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasSourceDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccNasControlAuditFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccNasOriginalPathSyncFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccNasControlAuditTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMatchRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasSourceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccNasControlAuditFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccNasControlAuditSkippedDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccNasControlAuditTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccNasOriginalPathSyncFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.service.file.NasConnectionConfig;
import cn.iocoder.yudao.module.infra.service.file.NasRecursiveScanHandler;
import cn.iocoder.yudao.module.infra.service.file.NasRecursiveScanService;
import cn.iocoder.yudao.module.infra.service.file.NasRecursiveScannedFile;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccNasControlAuditServiceImplTest extends BaseMockitoUnitTest {

    private static final Long TENANT_ID = 1L;
    private static final Long TASK_ID = 100L;
    private static final String NAS_SHARE_NAME = "dcc-share";
    private static final String NAS_PATH = "1. QMS documents/CODE-A/OQ/CODE-A-OQ-report.pdf";
    private static final long MODIFIED_AT = 1_690_848_000_000L;

    @TempDir
    private Path tempDir;

    @Mock
    private NasSettingsService nasSettingsService;
    @Mock
    private NasRecursiveScanService nasRecursiveScanService;
    @Mock
    private DccControlledFileNasSourceMapper nasSourceMapper;
    @Mock
    private DccNasControlAuditTaskMapper taskMapper;
    @Mock
    private DccNasControlAuditSkippedDirectoryMapper skippedDirectoryMapper;
    @Mock
    private DccNasControlAuditFileMapper auditFileMapper;
    @Mock
    private DccNasOriginalPathSyncFileMapper originalPathSyncFileMapper;
    @Mock
    private FileService fileService;
    @Mock
    private DccProjectCodeMapper projectCodeMapper;
    @Mock
    private DccFileCategoryMapper fileCategoryMapper;
    @Mock
    private DccFileCategoryMatchRuleMapper categoryMatchRuleMapper;
    @Mock
    private DccFileTypeTaxonomyAdminService fileTypeTaxonomyAdminService;

    @InjectMocks
    private DccNasControlAuditServiceImpl auditService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void recognizeUncontrolledFileDetails_marksProjectAndCategoryWhenUnique() {
        TenantContextHolder.setTenantId(TENANT_ID);
        DccNasControlAuditFileDO auditFile = pendingAuditFile(501L, NAS_PATH, "CODE-A-OQ-report.pdf");
        when(taskMapper.selectById(TASK_ID)).thenReturn(completedAuditTask());
        when(auditFileMapper.selectPendingRecognitionList(TASK_ID)).thenReturn(List.of(auditFile));
        when(projectCodeMapper.selectEnabledList()).thenReturn(List.of(projectCode(200L, "CODE-A", "Project A")));
        when(fileCategoryMapper.selectList()).thenReturn(List.of(category(300L, "OQ", 8801L)));
        when(categoryMatchRuleMapper.selectList()).thenReturn(List.of(matchRule(400L, 300L, "OQ", "CONTAINS", 100)));
        when(fileTypeTaxonomyAdminService.resolveActivePath(8801L)).thenReturn(
                new DccFileTypeTaxonomyPath(8801L, "技术文档", "设计开发", "OQ", null, null));

        DccNasControlAuditRecognizeRespVO result = auditService.recognizeTaskFiles(TASK_ID);

        assertEquals(1L, result.getMatchedCount());
        assertEquals(0L, result.getUnclassifiedPendingCount());
        assertEquals(0L, result.getAmbiguousCount());
        ArgumentCaptor<DccNasControlAuditFileDO> captor = ArgumentCaptor.forClass(DccNasControlAuditFileDO.class);
        verify(auditFileMapper).updateById(captor.capture());
        DccNasControlAuditFileDO updated = captor.getValue();
        assertEquals("MATCHED", updated.getClassificationStatus());
        assertEquals("MATCHED", updated.getClassificationReason());
        assertEquals(200L, updated.getMatchedProjectCodeId());
        assertEquals(8801L, updated.getMatchedFileTypeTaxonomyId());
        assertEquals("技术文档", updated.getMatchedFileTypeLevel1());
        assertEquals("设计开发", updated.getMatchedFileTypeLevel2());
        assertEquals("OQ", updated.getMatchedFileTypeLevel3());
        assertTrue(updated.getClassificationCandidatesJson().contains("CODE-A"));
        assertTrue(updated.getClassificationCandidatesJson().contains("OQ"));
        assertTrue(updated.getExpectedLocalRelativePath().contains("CODE-A__200"));
        assertTrue(updated.getExpectedLocalRelativePath().contains("8801__OQ"));
    }

    @Test
    void recognizeUncontrolledFileDetails_marksPendingWhenProjectOrCategoryMissing() {
        TenantContextHolder.setTenantId(TENANT_ID);
        DccNasControlAuditFileDO auditFile = pendingAuditFile(502L,
                "1. QMS documents/CODE-A/unknown/CODE-A-random.bin", "CODE-A-random.bin");
        when(taskMapper.selectById(TASK_ID)).thenReturn(completedAuditTask());
        when(auditFileMapper.selectPendingRecognitionList(TASK_ID)).thenReturn(List.of(auditFile));
        when(projectCodeMapper.selectEnabledList()).thenReturn(List.of(projectCode(200L, "CODE-A", "Project A")));
        when(fileCategoryMapper.selectList()).thenReturn(List.of(category(300L, "OQ", 8801L)));
        when(categoryMatchRuleMapper.selectList()).thenReturn(List.of(matchRule(400L, 300L, "OQ", "CONTAINS", 100)));

        DccNasControlAuditRecognizeRespVO result = auditService.recognizeTaskFiles(TASK_ID);

        assertEquals(0L, result.getMatchedCount());
        assertEquals(1L, result.getUnclassifiedPendingCount());
        ArgumentCaptor<DccNasControlAuditFileDO> captor = ArgumentCaptor.forClass(DccNasControlAuditFileDO.class);
        verify(auditFileMapper).updateById(captor.capture());
        DccNasControlAuditFileDO updated = captor.getValue();
        assertEquals("UNCLASSIFIED_PENDING", updated.getClassificationStatus());
        assertEquals("FILE_CATEGORY_NOT_FOUND", updated.getClassificationReason());
        assertEquals(200L, updated.getMatchedProjectCodeId());
        assertNull(updated.getMatchedFileTypeTaxonomyId());
        assertTrue(updated.getExpectedLocalRelativePath().startsWith("_未分类待处理/"));
        assertEquals("PENDING_MANUAL_REVIEW", updated.getArchiveStatus());
    }

    @Test
    void recognizeUncontrolledFileDetails_marksAmbiguousWhenProjectOrCategoryHasMultipleCandidates() {
        TenantContextHolder.setTenantId(TENANT_ID);
        DccNasControlAuditFileDO auditFile = pendingAuditFile(503L,
                "1. QMS documents/CODE-A/shared/CODE-A-shared-report.pdf", "CODE-A-shared-report.pdf");
        when(taskMapper.selectById(TASK_ID)).thenReturn(completedAuditTask());
        when(auditFileMapper.selectPendingRecognitionList(TASK_ID)).thenReturn(List.of(auditFile));
        when(projectCodeMapper.selectEnabledList()).thenReturn(List.of(
                projectCode(200L, "CODE-A", "Project A"),
                projectCode(201L, "CODE-A", "Project A Duplicate")));
        when(fileCategoryMapper.selectList()).thenReturn(List.of(category(300L, "Report", 8801L)));
        when(categoryMatchRuleMapper.selectList()).thenReturn(List.of(matchRule(400L, 300L, "report", "CONTAINS", 100)));

        DccNasControlAuditRecognizeRespVO result = auditService.recognizeTaskFiles(TASK_ID);

        assertEquals(0L, result.getMatchedCount());
        assertEquals(1L, result.getAmbiguousCount());
        ArgumentCaptor<DccNasControlAuditFileDO> captor = ArgumentCaptor.forClass(DccNasControlAuditFileDO.class);
        verify(auditFileMapper).updateById(captor.capture());
        DccNasControlAuditFileDO updated = captor.getValue();
        assertEquals("AMBIGUOUS", updated.getClassificationStatus());
        assertEquals("PROJECT_CODE_AMBIGUOUS", updated.getClassificationReason());
        assertNull(updated.getMatchedProjectCodeId());
        assertTrue(updated.getExpectedLocalRelativePath().startsWith("_未分类待处理/"));
        assertEquals("PENDING_MANUAL_REVIEW", updated.getArchiveStatus());
        assertTrue(updated.getClassificationCandidatesJson().contains("Project A Duplicate"));
    }

    @Test
    void recognizeUncontrolledFileDetails_doesNotRewriteImportedOrArchivedSnapshots() {
        TenantContextHolder.setTenantId(TENANT_ID);
        when(taskMapper.selectById(TASK_ID)).thenReturn(completedAuditTask());
        when(auditFileMapper.selectPendingRecognitionList(TASK_ID)).thenReturn(List.of());

        DccNasControlAuditRecognizeRespVO result = auditService.recognizeTaskFiles(TASK_ID);

        assertEquals(0L, result.getMatchedCount());
        assertEquals(0L, result.getSkippedCount());
        verify(auditFileMapper, never()).updateById(any(DccNasControlAuditFileDO.class));
    }
    @Test
    void processWaitingTasks_persistsUncontrolledFileDetailsAndKeepsReportOutput() {
        ReflectionTestUtils.setField(auditService, "transactionManager", noopTransactionManager());
        ReflectionTestUtils.setField(auditService, "multipartLocation", tempDir.toString());
        TenantContextHolder.setTenantId(TENANT_ID);
        NasConnectionConfig config = new NasConnectionConfig("nas.local", 445, NAS_SHARE_NAME, "", "user", "secret");
        DccNasControlAuditTaskDO task = DccNasControlAuditTaskDO.builder()
                .id(TASK_ID)
                .nasShareName(NAS_SHARE_NAME)
                .status(DccNasControlAuditServiceImpl.STATUS_WAITING)
                .scannedFileCount(0L)
                .controlledFileCount(0L)
                .notControlledFileCount(0L)
                .ambiguousFileCount(0L)
                .sourceMissingCount(0L)
                .skippedDirectoryCount(0L)
                .tenantId(TENANT_ID)
                .build();
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(task));
        when(taskMapper.claimWaitingTask(eq(TASK_ID), any(LocalDateTime.class))).thenReturn(1);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(nasSettingsService.getRequiredNasConfig()).thenReturn(config);
        when(nasSourceMapper.selectLegacyNasSourceCandidates(TENANT_ID, NAS_SHARE_NAME)).thenReturn(List.of());
        when(nasSourceMapper.selectCurrentActiveSources(TENANT_ID, NAS_SHARE_NAME)).thenReturn(List.of());
        doAnswer(invocation -> {
            NasRecursiveScanHandler handler = invocation.getArgument(2);
            handler.onFile(new NasRecursiveScannedFile(
                    "1. QMS documents",
                    NAS_PATH,
                    "CODE-A-OQ-report.pdf",
                    2048L,
                    MODIFIED_AT,
                    false,
                    false));
            return null;
        }).when(nasRecursiveScanService).scan(eq(config), anyCollection(), any(NasRecursiveScanHandler.class));
        when(fileService.createFileAndReturnId(any(Path.class), anyLong(), eq("NAS受控状态统计-" + TASK_ID + ".xlsx"),
                eq("dcc-nas-control-audit"), anyString())).thenReturn(900L);

        auditService.processWaitingTasks();

        ArgumentCaptor<DccNasControlAuditFileDO> auditFileCaptor =
                ArgumentCaptor.forClass(DccNasControlAuditFileDO.class);
        verify(auditFileMapper).insert(auditFileCaptor.capture());
        DccNasControlAuditFileDO auditFile = auditFileCaptor.getValue();
        String normalizedPath = DccNasPathUtils.normalizeRelativePath(NAS_PATH);
        String pathHash = DccNasPathUtils.pathHash(NAS_SHARE_NAME, normalizedPath);
        assertEquals(TASK_ID, auditFile.getTaskId());
        assertEquals(NAS_SHARE_NAME, auditFile.getNasShareName());
        assertEquals("1. QMS documents", auditFile.getRootPath());
        assertEquals(normalizedPath, auditFile.getNormalizedRelativePath());
        assertEquals(pathHash, auditFile.getPathHash());
        assertEquals("CODE-A-OQ-report.pdf", auditFile.getFileName());
        assertEquals(2048L, auditFile.getFileSize());
        assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(MODIFIED_AT), ZoneOffset.UTC),
                auditFile.getModifiedAt());
        assertEquals(sourceSignature(pathHash, 2048L, MODIFIED_AT), auditFile.getSourceSignature());
        assertEquals("NOT_CONTROLLED", auditFile.getControlStatus());
        assertEquals("PENDING_RECOGNITION", auditFile.getClassificationStatus());
        assertEquals("NOT_SELECTED", auditFile.getDownloadStatus());
        assertEquals("NOT_STARTED", auditFile.getArchiveStatus());
        assertEquals(TENANT_ID, auditFile.getTenantId());
        assertEquals(1L, task.getNotControlledFileCount());
        verify(fileService).createFileAndReturnId(any(Path.class), anyLong(),
                eq("NAS受控状态统计-" + TASK_ID + ".xlsx"), eq("dcc-nas-control-audit"), anyString());
    }

    @Test
    void processWaitingTasks_migratesFixedRootLocalFolderSourceAsLegacyExact() {
        ReflectionTestUtils.setField(auditService, "transactionManager", noopTransactionManager());
        ReflectionTestUtils.setField(auditService, "multipartLocation", tempDir.toString());
        TenantContextHolder.setTenantId(TENANT_ID);
        NasConnectionConfig config = new NasConnectionConfig("nas.local", 445, NAS_SHARE_NAME, "", "user", "secret");
        DccNasControlAuditTaskDO task = waitingAuditTask();
        DccControlledFileNasSourceMapper.LegacyNasSourceCandidate candidate =
                legacyCandidate(701L, "Local folder import source: " + NAS_PATH);
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(task));
        when(taskMapper.claimWaitingTask(eq(TASK_ID), any(LocalDateTime.class))).thenReturn(1);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(nasSettingsService.getRequiredNasConfig()).thenReturn(config);
        when(nasSourceMapper.selectLegacyNasSourceCandidates(TENANT_ID, NAS_SHARE_NAME)).thenReturn(List.of(candidate));
        when(nasSourceMapper.selectByControlledFileIdAndShareAndSourceType(701L, NAS_SHARE_NAME,
                "LEGACY_LOCAL_FOLDER_IMPORT")).thenReturn(null);
        when(nasSourceMapper.selectCurrentActiveSources(TENANT_ID, NAS_SHARE_NAME)).thenReturn(
                List.of(activeSource(701L, NAS_PATH, "LEGACY_LOCAL_FOLDER_IMPORT", "LEGACY_EXACT")));
        doAnswer(invocation -> {
            NasRecursiveScanHandler handler = invocation.getArgument(2);
            handler.onFile(scannedFile());
            return null;
        }).when(nasRecursiveScanService).scan(eq(config), anyCollection(), any(NasRecursiveScanHandler.class));
        when(fileService.createFileAndReturnId(any(Path.class), anyLong(), eq("NAS受控状态统计-" + TASK_ID + ".xlsx"),
                eq("dcc-nas-control-audit"), anyString())).thenReturn(900L);

        auditService.processWaitingTasks();

        ArgumentCaptor<DccControlledFileNasSourceDO> sourceCaptor =
                ArgumentCaptor.forClass(DccControlledFileNasSourceDO.class);
        verify(nasSourceMapper).insert(sourceCaptor.capture());
        DccControlledFileNasSourceDO source = sourceCaptor.getValue();
        assertEquals(701L, source.getControlledFileId());
        assertEquals(NAS_SHARE_NAME, source.getNasShareName());
        assertEquals(DccNasPathUtils.normalizeRelativePath(NAS_PATH), source.getNormalizedRelativePath());
        assertEquals(DccNasPathUtils.pathHash(NAS_SHARE_NAME, NAS_PATH), source.getPathHash());
        assertEquals("LEGACY_LOCAL_FOLDER_IMPORT", source.getSourceType());
        assertEquals("LEGACY_EXACT", source.getSourceConfidence());
        assertEquals(1L, task.getControlledFileCount());
        assertEquals(0L, task.getNotControlledFileCount());
        verify(auditFileMapper, never()).insert(any(DccNasControlAuditFileDO.class));
    }

    @Test
    void processWaitingTasks_doesNotMigrateLocalFolderSourceOutsideFixedRoots() {
        ReflectionTestUtils.setField(auditService, "transactionManager", noopTransactionManager());
        ReflectionTestUtils.setField(auditService, "multipartLocation", tempDir.toString());
        TenantContextHolder.setTenantId(TENANT_ID);
        NasConnectionConfig config = new NasConnectionConfig("nas.local", 445, NAS_SHARE_NAME, "", "user", "secret");
        DccNasControlAuditTaskDO task = waitingAuditTask();
        DccControlledFileNasSourceMapper.LegacyNasSourceCandidate candidate =
                legacyCandidate(703L, "Local folder import source: Downloads/CODE-A-OQ-report.pdf");
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(task));
        when(taskMapper.claimWaitingTask(eq(TASK_ID), any(LocalDateTime.class))).thenReturn(1);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(nasSettingsService.getRequiredNasConfig()).thenReturn(config);
        when(nasSourceMapper.selectLegacyNasSourceCandidates(TENANT_ID, NAS_SHARE_NAME)).thenReturn(List.of(candidate));
        when(nasSourceMapper.selectCurrentActiveSources(TENANT_ID, NAS_SHARE_NAME)).thenReturn(List.of());
        doAnswer(invocation -> {
            NasRecursiveScanHandler handler = invocation.getArgument(2);
            handler.onFile(scannedFile());
            return null;
        }).when(nasRecursiveScanService).scan(eq(config), anyCollection(), any(NasRecursiveScanHandler.class));
        when(fileService.createFileAndReturnId(any(Path.class), anyLong(), eq("NAS受控状态统计-" + TASK_ID + ".xlsx"),
                eq("dcc-nas-control-audit"), anyString())).thenReturn(900L);

        auditService.processWaitingTasks();

        assertEquals(DccNasControlAuditServiceImpl.STATUS_COMPLETED, task.getStatus());
        assertEquals(0L, task.getControlledFileCount());
        assertEquals(1L, task.getNotControlledFileCount());
        verify(nasSourceMapper, never()).insert(any(DccControlledFileNasSourceDO.class));
    }

    @Test
    void processWaitingTasks_countsOriginalPathSyncAsControlledAndDoesNotPersistAuditFile() {
        ReflectionTestUtils.setField(auditService, "transactionManager", noopTransactionManager());
        ReflectionTestUtils.setField(auditService, "multipartLocation", tempDir.toString());
        TenantContextHolder.setTenantId(TENANT_ID);
        NasConnectionConfig config = new NasConnectionConfig("nas.local", 445, NAS_SHARE_NAME, "", "user", "secret");
        DccNasControlAuditTaskDO task = waitingAuditTask();
        String normalizedPath = DccNasPathUtils.normalizeRelativePath(NAS_PATH);
        String pathHash = DccNasPathUtils.pathHash(NAS_SHARE_NAME, normalizedPath);
        DccNasOriginalPathSyncFileDO syncedFile = DccNasOriginalPathSyncFileDO.builder()
                .id(8801L)
                .nasShareName(NAS_SHARE_NAME)
                .normalizedRelativePath(normalizedPath)
                .pathHash(pathHash)
                .syncStatus("ACTIVE")
                .tenantId(TENANT_ID)
                .build();
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(task));
        when(taskMapper.claimWaitingTask(eq(TASK_ID), any(LocalDateTime.class))).thenReturn(1);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(nasSettingsService.getRequiredNasConfig()).thenReturn(config);
        when(nasSourceMapper.selectLegacyNasSourceCandidates(TENANT_ID, NAS_SHARE_NAME)).thenReturn(List.of());
        when(nasSourceMapper.selectCurrentActiveSources(TENANT_ID, NAS_SHARE_NAME)).thenReturn(List.of());
        when(originalPathSyncFileMapper.selectActiveRows(TENANT_ID, NAS_SHARE_NAME)).thenReturn(List.of(syncedFile));
        doAnswer(invocation -> {
            NasRecursiveScanHandler handler = invocation.getArgument(2);
            handler.onFile(scannedFile());
            return null;
        }).when(nasRecursiveScanService).scan(eq(config), anyCollection(), any(NasRecursiveScanHandler.class));
        when(fileService.createFileAndReturnId(any(Path.class), anyLong(), eq("NAS受控状态统计-" + TASK_ID + ".xlsx"),
                eq("dcc-nas-control-audit"), anyString())).thenReturn(900L);

        auditService.processWaitingTasks();

        assertEquals(1L, task.getControlledFileCount());
        assertEquals(0L, task.getNotControlledFileCount());
        verify(auditFileMapper, never()).insert(any(DccNasControlAuditFileDO.class));
    }

    @Test
    void processWaitingTasks_failsBeforeScanWhenMigratedSourceBaselineIsStillEmpty() {
        ReflectionTestUtils.setField(auditService, "transactionManager", noopTransactionManager());
        ReflectionTestUtils.setField(auditService, "multipartLocation", tempDir.toString());
        TenantContextHolder.setTenantId(TENANT_ID);
        NasConnectionConfig config = new NasConnectionConfig("nas.local", 445, NAS_SHARE_NAME, "", "user", "secret");
        DccNasControlAuditTaskDO task = waitingAuditTask();
        DccControlledFileNasSourceMapper.LegacyNasSourceCandidate candidate =
                legacyCandidate(702L, "Local folder import source: " + NAS_PATH);
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(task));
        when(taskMapper.claimWaitingTask(eq(TASK_ID), any(LocalDateTime.class))).thenReturn(1);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);
        when(nasSettingsService.getRequiredNasConfig()).thenReturn(config);
        when(nasSourceMapper.selectLegacyNasSourceCandidates(TENANT_ID, NAS_SHARE_NAME)).thenReturn(List.of(candidate));
        when(nasSourceMapper.selectByControlledFileIdAndShareAndSourceType(702L, NAS_SHARE_NAME,
                "LEGACY_LOCAL_FOLDER_IMPORT")).thenReturn(null);
        when(nasSourceMapper.selectCurrentActiveSources(TENANT_ID, NAS_SHARE_NAME)).thenReturn(List.of());

        auditService.processWaitingTasks();

        assertEquals(DccNasControlAuditServiceImpl.STATUS_FAILED, task.getStatus());
        assertTrue(task.getFailureReason().contains("NAS 受控来源基线不完整"));
        verify(nasRecursiveScanService, never()).scan(any(), anyCollection(), any(NasRecursiveScanHandler.class));
        verify(auditFileMapper, never()).insert(any(DccNasControlAuditFileDO.class));
        verify(fileService, never()).createFileAndReturnId(any(Path.class), anyLong(), anyString(), anyString(), anyString());
    }

    private DccNasControlAuditTaskDO completedAuditTask() {
        return DccNasControlAuditTaskDO.builder()
                .id(TASK_ID)
                .nasShareName(NAS_SHARE_NAME)
                .status(DccNasControlAuditServiceImpl.STATUS_COMPLETED)
                .tenantId(TENANT_ID)
                .build();
    }

    private DccNasControlAuditTaskDO waitingAuditTask() {
        return DccNasControlAuditTaskDO.builder()
                .id(TASK_ID)
                .nasShareName(NAS_SHARE_NAME)
                .status(DccNasControlAuditServiceImpl.STATUS_WAITING)
                .scannedFileCount(0L)
                .controlledFileCount(0L)
                .notControlledFileCount(0L)
                .ambiguousFileCount(0L)
                .sourceMissingCount(0L)
                .skippedDirectoryCount(0L)
                .tenantId(TENANT_ID)
                .build();
    }

    private DccControlledFileNasSourceMapper.LegacyNasSourceCandidate legacyCandidate(Long controlledFileId,
                                                                                      String remark) {
        DccControlledFileNasSourceMapper.LegacyNasSourceCandidate candidate =
                new DccControlledFileNasSourceMapper.LegacyNasSourceCandidate();
        candidate.setControlledFileId(controlledFileId);
        candidate.setFileName("CODE-A-OQ-report.pdf");
        candidate.setVersionNo("V1.0");
        candidate.setRemark(remark);
        return candidate;
    }

    private DccControlledFileNasSourceMapper.ActiveNasSourceRow activeSource(Long controlledFileId,
                                                                             String path,
                                                                             String sourceType,
                                                                             String confidence) {
        DccControlledFileNasSourceMapper.ActiveNasSourceRow source =
                new DccControlledFileNasSourceMapper.ActiveNasSourceRow();
        source.setControlledFileId(controlledFileId);
        source.setNasShareName(NAS_SHARE_NAME);
        source.setNormalizedRelativePath(DccNasPathUtils.normalizeRelativePath(path));
        source.setPathHash(DccNasPathUtils.pathHash(NAS_SHARE_NAME, path));
        source.setSourceType(sourceType);
        source.setSourceConfidence(confidence);
        return source;
    }

    private NasRecursiveScannedFile scannedFile() {
        return new NasRecursiveScannedFile(
                "1. QMS documents",
                NAS_PATH,
                "CODE-A-OQ-report.pdf",
                2048L,
                MODIFIED_AT,
                false,
                false);
    }

    private DccNasControlAuditFileDO pendingAuditFile(Long id, String path, String fileName) {
        String normalizedPath = DccNasPathUtils.normalizeRelativePath(path);
        return DccNasControlAuditFileDO.builder()
                .id(id)
                .taskId(TASK_ID)
                .nasShareName(NAS_SHARE_NAME)
                .rootPath("1. QMS documents")
                .normalizedRelativePath(normalizedPath)
                .pathHash(DccNasPathUtils.pathHash(NAS_SHARE_NAME, normalizedPath))
                .fileName(fileName)
                .fileSize(1024L)
                .modifiedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(MODIFIED_AT), ZoneOffset.UTC))
                .sourceSignature("signature-" + id)
                .controlStatus("NOT_CONTROLLED")
                .classificationStatus("PENDING_RECOGNITION")
                .downloadStatus("NOT_SELECTED")
                .archiveStatus("NOT_STARTED")
                .tenantId(TENANT_ID)
                .build();
    }

    private DccProjectCodeDO projectCode(Long id, String projectCode, String projectName) {
        return DccProjectCodeDO.builder()
                .id(id)
                .projectCode(projectCode)
                .projectName(projectName)
                .status("ENABLE")
                .build();
    }

    private DccFileCategoryDO category(Long id, String name, Long fileTypeTaxonomyId) {
        return DccFileCategoryDO.builder()
                .id(id)
                .name(name)
                .code(name)
                .active(true)
                .fileTypeTaxonomyId(fileTypeTaxonomyId)
                .build();
    }

    private DccFileCategoryMatchRuleDO matchRule(Long id, Long categoryId, String matchText,
                                                  String matchType, Integer weight) {
        return DccFileCategoryMatchRuleDO.builder()
                .id(id)
                .categoryId(categoryId)
                .matchText(matchText)
                .matchType(matchType)
                .weight(weight)
                .active(true)
                .build();
    }
    private static String sourceSignature(String pathHash, Long fileSize, Long modifiedAtUtcEpochMillis) {
        String payload = pathHash + "|" + fileSize + "|" + modifiedAtUtcEpochMillis;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    private static PlatformTransactionManager noopTransactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
                // no-op
            }

            @Override
            public void rollback(TransactionStatus status) {
                // no-op
            }
        };
    }
}
