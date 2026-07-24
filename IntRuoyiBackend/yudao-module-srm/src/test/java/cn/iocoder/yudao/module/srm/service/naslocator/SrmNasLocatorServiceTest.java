package cn.iocoder.yudao.module.srm.service.naslocator;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.test.core.util.AssertUtils;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasListRespVO;
import cn.iocoder.yudao.module.infra.service.file.NasAclReadResult;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasConnectionConfig;
import cn.iocoder.yudao.module.infra.service.file.NasFileReadResult;
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
import cn.hutool.crypto.digest.DigestUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_READ_FAILED;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.NAS_LOCATOR_ENTRY_NOT_FILE;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.NAS_LOCATOR_NO_SUCCESS_SNAPSHOT;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.NAS_LOCATOR_REFRESH_RUNNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(SrmNasLocatorServiceImpl.class)
class SrmNasLocatorServiceTest extends BaseDbUnitTest {

    @Resource
    private SrmNasLocatorServiceImpl nasLocatorService;
    @Resource
    private SrmNasLocatorRefreshTaskMapper refreshTaskMapper;
    @Resource
    private SrmNasLocatorEntryMapper entryMapper;

    @MockBean
    private NasBrowserService nasBrowserService;
    @MockBean
    private NasSettingsService nasSettingsService;
    @MockBean
    private SrmNasLocatorBlacklistSettingsService blacklistSettingsService;

    @Test
    void getStatusAndPage_shouldFailFastWhenNoSuccessSnapshot() {
        mockProtectedConfig();

        SrmNasLocatorStatusRespVO status = nasLocatorService.getStatus();

        assertEquals(SrmNasLocatorServiceImpl.STATUS_IDLE, status.getLatestTaskStatus());
        assertEquals("请先刷新 NAS 索引", status.getMessage());
        assertEquals(SrmNasLocatorServiceImpl.PROTECTED_SCOPE_SHARE_DISPLAY, status.getScopeShare());
        assertEquals(0L, status.getFileCount());
        assertEquals(0L, status.getDirectoryCount());
        assertEquals(null, status.getRunningShare());
        assertEquals(null, status.getRunningPath());
        assertEquals(null, status.getRunningDirectoryCount());
        assertEquals(null, status.getRunningFileCount());
        AssertUtils.assertServiceException(
                () -> nasLocatorService.getFilePage(buildPageReq(null)),
                NAS_LOCATOR_NO_SUCCESS_SNAPSHOT
        );
    }

    @Test
    void getAndSaveBlacklist_shouldDelegateToSettingsService() {
        when(blacklistSettingsService.getPatterns()).thenReturn(List.of("*.pyc", "*MO13*.pdf"));

        SrmNasLocatorBlacklistRespVO blacklist = nasLocatorService.getBlacklist();

        assertEquals(List.of("*.pyc", "*MO13*.pdf"), blacklist.getPatterns());

        SrmNasLocatorBlacklistSaveReqVO reqVO = new SrmNasLocatorBlacklistSaveReqVO();
        reqVO.setPatterns(List.of("*.pyc"));
        nasLocatorService.saveBlacklist(reqVO);

        verify(blacklistSettingsService).savePatterns(List.of("*.pyc"));
    }

    @Test
    void triggerRefresh_shouldBlockWhenRunningTaskAlreadyExists() {
        mockProtectedConfig();
        refreshTaskMapper.insert(buildTask(1L, SrmNasLocatorServiceImpl.STATUS_RUNNING, 0L, 0L, null));

        AssertUtils.assertServiceException(nasLocatorService::triggerRefresh, NAS_LOCATOR_REFRESH_RUNNING);
    }

    @Test
    void triggerRefresh_shouldFailStaleRunningTaskThenStartNewRefresh() {
        mockProtectedConfig();
        SrmNasLocatorRefreshTaskDO staleTask = buildTask(1L, SrmNasLocatorServiceImpl.STATUS_RUNNING, 0L, 0L, null);
        staleTask.setStartedTime(LocalDateTime.now().minusHours(1));
        staleTask.setFinishedTime(null);
        refreshTaskMapper.insert(staleTask);

        nasLocatorService.triggerRefresh();

        SrmNasLocatorRefreshTaskDO savedStaleTask = refreshTaskMapper.selectById(staleTask.getId());
        assertEquals(SrmNasLocatorServiceImpl.STATUS_FAILED, savedStaleTask.getStatus());
        assertTrue(savedStaleTask.getErrorMessage().contains("stale running task"));

        SrmNasLocatorRefreshTaskDO latestTask = refreshTaskMapper.selectLatestTask(1L);
        assertEquals(SrmNasLocatorServiceImpl.STATUS_RUNNING, latestTask.getStatus());
        assertTrue(latestTask.getId() > staleTask.getId());
    }

    @Test
    void runRefreshNow_shouldPersistDirectoriesAndFilesWhilePageReturnsOnlyFiles() {
        mockProtectedConfig();
        when(nasBrowserService.listFiles("")).thenReturn(new FileNasListRespVO()
                .setCurrentPath("")
                .setItems(List.of(
                        buildItem("制度", "制度", true, 0L, 1710000000000L),
                        buildItem("质量手册.docx", "质量手册.docx", false, 1024L, 1710000001000L),
                        buildItem("程序文件.pdf", "制度/程序文件.pdf", false, 2048L, 1710000002000L)
                )));
        when(nasBrowserService.listFiles("制度")).thenReturn(new FileNasListRespVO()
                .setCurrentPath("制度")
                .setItems(List.of(
                        buildItem("二级目录", "制度/二级目录", true, 0L, 1710000003000L)
                )));
        when(nasBrowserService.listFiles("制度/二级目录")).thenReturn(new FileNasListRespVO()
                .setCurrentPath("制度/二级目录")
                .setItems(List.of()));

        SrmNasLocatorRefreshTaskDO runningTask = buildTask(
                1L,
                SrmNasLocatorServiceImpl.STATUS_RUNNING,
                0L,
                0L,
                null
        );
        refreshTaskMapper.insert(runningTask);

        nasLocatorService.runRefreshNow(runningTask.getId());

        SrmNasLocatorRefreshTaskDO savedTask = refreshTaskMapper.selectById(runningTask.getId());
        assertEquals(SrmNasLocatorServiceImpl.STATUS_SUCCESS, savedTask.getStatus());
        assertEquals(2L, savedTask.getDirectoryCount());
        assertEquals(2L, savedTask.getFileCount());
        assertEquals(6, entryMapper.selectListByRefreshTaskId(runningTask.getId()).size());

        PageResult<SrmNasLocatorFileRespVO> page = nasLocatorService.getFilePage(buildPageReq(""));
        assertEquals(2L, page.getTotal());
        assertEquals(List.of("程序文件.pdf", "质量手册.docx"),
                page.getList().stream().map(SrmNasLocatorFileRespVO::getFileName).toList());
        assertEquals("质量体系文件/制度", page.getList().get(0).getDirectoryPath());
        assertEquals("质量体系文件", page.getList().get(1).getDirectoryPath());
    }

    @Test
    void runRefreshNow_shouldExcludeFilesMatchedByBlacklistPatterns() {
        mockProtectedConfig();
        when(blacklistSettingsService.getPatterns()).thenReturn(List.of("*.pyc"));
        when(nasBrowserService.listFiles("")).thenReturn(new FileNasListRespVO()
                .setCurrentPath("")
                .setItems(List.of(
                        buildItem("cache.pyc", "cache.pyc", false, 10L, 1710000000000L),
                        buildItem("质量手册.docx", "质量手册.docx", false, 1024L, 1710000001000L)
                )));

        SrmNasLocatorRefreshTaskDO runningTask = buildTask(1L, SrmNasLocatorServiceImpl.STATUS_RUNNING, 0L, 0L, null);
        refreshTaskMapper.insert(runningTask);

        nasLocatorService.runRefreshNow(runningTask.getId());

        SrmNasLocatorRefreshTaskDO savedTask = refreshTaskMapper.selectById(runningTask.getId());
        assertEquals(1L, savedTask.getFileCount());
        assertEquals(List.of("生产部", "质量体系文件", "质量体系文件/质量手册.docx"),
                entryMapper.selectListByRefreshTaskId(runningTask.getId()).stream()
                        .map(SrmNasLocatorEntryDO::getPath)
                        .sorted()
                        .toList());
    }

    @Test
    void getFilePage_shouldSupportWildcardKeywordMatchingOnFileNameOnly() {
        mockProtectedConfig();
        SrmNasLocatorRefreshTaskDO successTask = buildTask(1L, SrmNasLocatorServiceImpl.STATUS_SUCCESS, 0L, 3L, null);
        successTask.setFinishedTime(LocalDateTime.now().minusMinutes(3));
        refreshTaskMapper.insert(successTask);
        entryMapper.insert(buildEntry(successTask.getId(), SrmNasLocatorServiceImpl.ENTRY_TYPE_FILE,
                "MO13-report.pdf", "质量体系文件/reports/MO13-report.pdf", "质量体系文件/reports", 10L, 1710000000000L));
        entryMapper.insert(buildEntry(successTask.getId(), SrmNasLocatorServiceImpl.ENTRY_TYPE_FILE,
                "QC-MO13-final.pdf", "质量体系文件/reports/QC-MO13-final.pdf", "质量体系文件/reports", 11L, 1710000001000L));
        entryMapper.insert(buildEntry(successTask.getId(), SrmNasLocatorServiceImpl.ENTRY_TYPE_FILE,
                "MO13-report.xlsx", "质量体系文件/reports/MO13-report.xlsx", "质量体系文件/reports", 12L, 1710000002000L));

        PageResult<SrmNasLocatorFileRespVO> page = nasLocatorService.getFilePage(buildPageReq("*MO13*.pdf"));

        assertEquals(2L, page.getTotal());
        assertEquals(List.of("MO13-report.pdf", "QC-MO13-final.pdf"),
                page.getList().stream().map(SrmNasLocatorFileRespVO::getFileName).toList());
    }

    @Test
    void getFilePage_shouldKeepPlainKeywordContainsBehavior() {
        mockProtectedConfig();
        SrmNasLocatorRefreshTaskDO successTask = buildTask(1L, SrmNasLocatorServiceImpl.STATUS_SUCCESS, 0L, 2L, null);
        successTask.setFinishedTime(LocalDateTime.now().minusMinutes(3));
        refreshTaskMapper.insert(successTask);
        entryMapper.insert(buildEntry(successTask.getId(), SrmNasLocatorServiceImpl.ENTRY_TYPE_FILE,
                "MO13-report.pdf", "质量体系文件/reports/MO13-report.pdf", "质量体系文件/reports", 10L, 1710000000000L));
        entryMapper.insert(buildEntry(successTask.getId(), SrmNasLocatorServiceImpl.ENTRY_TYPE_FILE,
                "report-MO13.txt", "质量体系文件/reports/report-MO13.txt", "质量体系文件/reports", 11L, 1710000001000L));

        PageResult<SrmNasLocatorFileRespVO> page = nasLocatorService.getFilePage(buildPageReq("MO13"));

        assertEquals(2L, page.getTotal());
        assertEquals(List.of("MO13-report.pdf", "report-MO13.txt"),
                page.getList().stream().map(SrmNasLocatorFileRespVO::getFileName).toList());
    }

    @Test
    void getFilePage_shouldTreatPercentAndUnderscoreAsWildcardLiterals() {
        mockProtectedConfig();
        SrmNasLocatorRefreshTaskDO successTask = buildTask(1L, SrmNasLocatorServiceImpl.STATUS_SUCCESS, 0L, 2L, null);
        successTask.setFinishedTime(LocalDateTime.now().minusMinutes(3));
        refreshTaskMapper.insert(successTask);
        entryMapper.insert(buildEntry(successTask.getId(), SrmNasLocatorServiceImpl.ENTRY_TYPE_FILE,
                "100%_MO13-report.pdf", "质量体系文件/reports/100%_MO13-report.pdf", "质量体系文件/reports", 10L, 1710000000000L));
        entryMapper.insert(buildEntry(successTask.getId(), SrmNasLocatorServiceImpl.ENTRY_TYPE_FILE,
                "100A_MO13-report.pdf", "质量体系文件/reports/100A_MO13-report.pdf", "质量体系文件/reports", 11L, 1710000001000L));

        PageResult<SrmNasLocatorFileRespVO> page = nasLocatorService.getFilePage(buildPageReq("*100%_MO13*.pdf"));

        assertEquals(1L, page.getTotal());
        assertEquals(List.of("100%_MO13-report.pdf"),
                page.getList().stream().map(SrmNasLocatorFileRespVO::getFileName).toList());
    }

    @Test
    void runRefreshNow_shouldSkipSystemOrHiddenDirectoriesWhileKeepingBusinessDirectoriesIndexed() {
        mockProtectedConfig();
        when(nasBrowserService.listFiles("")).thenReturn(new FileNasListRespVO()
                .setCurrentPath("")
                .setItems(List.of(
                        buildItem("#recycle", "#recycle", true, 0L, 1710000000000L)
                                .setSystem(true),
                        buildItem("制度", "制度", true, 0L, 1710000001000L),
                        buildItem("隐藏目录", "隐藏目录", true, 0L, 1710000002000L)
                                .setHidden(true),
                        buildItem("质量手册.docx", "质量手册.docx", false, 1024L, 1710000003000L)
                )));
        when(nasBrowserService.listFiles("制度")).thenReturn(new FileNasListRespVO()
                .setCurrentPath("制度")
                .setItems(List.of(
                        buildItem("程序文件.pdf", "制度/程序文件.pdf", false, 2048L, 1710000004000L)
                )));

        SrmNasLocatorRefreshTaskDO runningTask = buildTask(
                1L,
                SrmNasLocatorServiceImpl.STATUS_RUNNING,
                0L,
                0L,
                null
        );
        refreshTaskMapper.insert(runningTask);

        nasLocatorService.runRefreshNow(runningTask.getId());

        SrmNasLocatorRefreshTaskDO savedTask = refreshTaskMapper.selectById(runningTask.getId());
        assertEquals(SrmNasLocatorServiceImpl.STATUS_SUCCESS, savedTask.getStatus());
        assertEquals(1L, savedTask.getDirectoryCount());
        assertEquals(2L, savedTask.getFileCount());
        assertEquals(List.of("生产部", "质量体系文件", "质量体系文件/制度", "质量体系文件/制度/程序文件.pdf", "质量体系文件/质量手册.docx"),
                entryMapper.selectListByRefreshTaskId(runningTask.getId()).stream()
                        .map(SrmNasLocatorEntryDO::getPath)
                        .sorted()
                        .toList());
    }

    @Test
    void runRefreshNow_shouldSkipUnreadableDirectoriesAndKeepReadableContentIndexed() {
        mockProtectedConfig();
        when(nasBrowserService.listFiles("")).thenReturn(new FileNasListRespVO()
                .setCurrentPath("")
                .setItems(List.of(
                        buildItem("1. QMS documents", "1. QMS documents", true, 0L, 1710000000000L),
                        buildItem("4. External documents", "4. External documents", true, 0L, 1710000001000L),
                        buildItem("质量手册.docx", "质量手册.docx", false, 1024L, 1710000002000L)
                )));
        when(nasBrowserService.listFiles("1. QMS documents")).thenReturn(new FileNasListRespVO()
                .setCurrentPath("1. QMS documents")
                .setItems(List.of(
                        buildItem("程序文件.pdf", "1. QMS documents/程序文件.pdf", false, 2048L, 1710000003000L)
                )));
        when(nasBrowserService.listFiles("4. External documents"))
                .thenThrow(exception(FILE_NAS_READ_FAILED, "access denied: 4. External documents"));

        SrmNasLocatorRefreshTaskDO runningTask = buildTask(
                1L,
                SrmNasLocatorServiceImpl.STATUS_RUNNING,
                0L,
                0L,
                null
        );
        refreshTaskMapper.insert(runningTask);

        nasLocatorService.runRefreshNow(runningTask.getId());

        SrmNasLocatorRefreshTaskDO savedTask = refreshTaskMapper.selectById(runningTask.getId());
        assertEquals(SrmNasLocatorServiceImpl.STATUS_SUCCESS, savedTask.getStatus());
        assertEquals(1L, savedTask.getDirectoryCount());
        assertEquals(2L, savedTask.getFileCount());
        assertEquals(List.of(
                        "生产部",
                        "质量体系文件",
                        "质量体系文件/1. QMS documents",
                        "质量体系文件/1. QMS documents/程序文件.pdf",
                        "质量体系文件/质量手册.docx"
                ),
                entryMapper.selectListByRefreshTaskId(runningTask.getId()).stream()
                        .map(SrmNasLocatorEntryDO::getPath)
                        .sorted()
                        .toList());
    }

    @Test
    void runRefreshNow_shouldMergeQualityAndProductionSharesIntoSingleSnapshot() {
        mockProtectedConfig();
        when(nasBrowserService.executeInSession(eq(new NasConnectionConfig("172.30.30.4", 445, "质量体系文件", "", "tester", "secret")), any()))
                .thenAnswer(invocation -> {
                    NasBrowserService.NasSessionCallback<?> callback = invocation.getArgument(1);
                    return callback.execute(new StaticNasSessionScope(List.of(
                            rootDir("1. QMS documents"),
                            rootFile("质量手册.docx", 1024L)
                    )));
                });
        when(nasBrowserService.executeInSession(eq(new NasConnectionConfig("172.30.30.4", 445, "生产部", "", "tester", "secret")), any()))
                .thenAnswer(invocation -> {
                    NasBrowserService.NasSessionCallback<?> callback = invocation.getArgument(1);
                    return callback.execute(new StaticNasSessionScope(List.of(
                            rootDir("工艺资料"),
                            rootFile("生产流程.pdf", 2048L)
                    )));
                });

        SrmNasLocatorRefreshTaskDO runningTask = buildTask(
                1L,
                SrmNasLocatorServiceImpl.STATUS_RUNNING,
                0L,
                0L,
                null
        );
        refreshTaskMapper.insert(runningTask);

        nasLocatorService.runRefreshNow(runningTask.getId());

        SrmNasLocatorRefreshTaskDO savedTask = refreshTaskMapper.selectById(runningTask.getId());
        assertEquals(SrmNasLocatorServiceImpl.STATUS_SUCCESS, savedTask.getStatus());
        assertEquals(SrmNasLocatorServiceImpl.PROTECTED_SCOPE_SHARE_DISPLAY, savedTask.getScopeShare());
        assertEquals(2L, savedTask.getDirectoryCount());
        assertEquals(2L, savedTask.getFileCount());
        assertEquals(List.of(
                        "生产部",
                        "生产部/工艺资料",
                        "生产部/生产流程.pdf",
                        "质量体系文件",
                        "质量体系文件/1. QMS documents",
                        "质量体系文件/质量手册.docx"
                ),
                entryMapper.selectListByRefreshTaskId(runningTask.getId()).stream()
                        .map(SrmNasLocatorEntryDO::getPath)
                        .sorted()
                        .toList());
    }

    @Test
    void getStatus_shouldExposeRunningProgressWhileRefreshIsInFlight() throws Exception {
        mockProtectedConfig();
        CountDownLatch enteredRootDirectory = new CountDownLatch(1);
        CountDownLatch allowRefreshContinue = new CountDownLatch(1);
        when(nasBrowserService.executeInSession(eq(new NasConnectionConfig("172.30.30.4", 445, "质量体系文件", "", "tester", "secret")), any()))
                .thenAnswer(invocation -> {
                    NasBrowserService.NasSessionCallback<?> callback = invocation.getArgument(1);
                    return callback.execute(new NasBrowserService.NasSessionScope() {
                        @Override
                        public FileNasListRespVO listFiles(String path) {
                            if (path == null || path.isBlank()) {
                                enteredRootDirectory.countDown();
                                try {
                                    assertTrue(allowRefreshContinue.await(5, TimeUnit.SECONDS));
                                } catch (InterruptedException exception) {
                                    throw new RuntimeException(exception);
                                }
                                return new FileNasListRespVO()
                                        .setCurrentPath("")
                                        .setItems(List.of(rootDir("1. QMS documents"), rootFile("质量手册.docx", 1024L)));
                            }
                            return new FileNasListRespVO()
                                    .setCurrentPath(path)
                                    .setItems(List.of());
                        }

                        @Override
                        public NasFileReadResult readFile(String path) {
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public void writeFileTo(String path, OutputStream outputStream) {
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public NasAclReadResult readDirectoryAcl(String path) {
                            throw new UnsupportedOperationException();
                        }
                    });
                });
        when(nasBrowserService.executeInSession(eq(new NasConnectionConfig("172.30.30.4", 445, "生产部", "", "tester", "secret")), any()))
                .thenAnswer(invocation -> {
                    NasBrowserService.NasSessionCallback<?> callback = invocation.getArgument(1);
                    return callback.execute(new StaticNasSessionScope(List.of()));
                });

        SrmNasLocatorRefreshTaskDO runningTask = buildTask(
                1L,
                SrmNasLocatorServiceImpl.STATUS_RUNNING,
                0L,
                0L,
                null
        );
        refreshTaskMapper.insert(runningTask);

        CompletableFuture<Void> refreshFuture = CompletableFuture.runAsync(() -> nasLocatorService.runRefreshNow(runningTask.getId()));
        assertTrue(enteredRootDirectory.await(5, TimeUnit.SECONDS));

        SrmNasLocatorStatusRespVO status = nasLocatorService.getStatus();
        assertEquals(SrmNasLocatorServiceImpl.STATUS_RUNNING, status.getLatestTaskStatus());
        assertEquals("质量体系文件", status.getRunningShare());
        assertEquals("质量体系文件", status.getRunningPath());
        assertEquals(0L, status.getRunningDirectoryCount());
        assertEquals(0L, status.getRunningFileCount());
        assertEquals(1, status.getRunningShareIndex());
        assertEquals(2, status.getRunningShareTotal());
        assertTrue(status.getMessage().contains("质量体系文件"));

        allowRefreshContinue.countDown();
        refreshFuture.join();
    }

    @Test
    void runRefreshNow_shouldMarkFailedAndKeepPreviousSuccessSnapshotSearchable() {
        mockProtectedConfig();
        SrmNasLocatorRefreshTaskDO oldSuccessTask = buildTask(
                1L,
                SrmNasLocatorServiceImpl.STATUS_SUCCESS,
                1L,
                1L,
                null
        );
        oldSuccessTask.setFinishedTime(LocalDateTime.now().minusHours(2));
        refreshTaskMapper.insert(oldSuccessTask);
        entryMapper.insert(buildEntry(oldSuccessTask.getId(),
                SrmNasLocatorServiceImpl.ENTRY_TYPE_FILE,
                "旧版手册.docx",
                "旧版手册.docx",
                "",
                4096L,
                1710000010000L));

        when(nasBrowserService.listFiles("")).thenThrow(new RuntimeException("根目录读取失败"));

        SrmNasLocatorRefreshTaskDO runningTask = buildTask(
                1L,
                SrmNasLocatorServiceImpl.STATUS_RUNNING,
                0L,
                0L,
                null
        );
        refreshTaskMapper.insert(runningTask);

        nasLocatorService.runRefreshNow(runningTask.getId());

        SrmNasLocatorRefreshTaskDO failedTask = refreshTaskMapper.selectById(runningTask.getId());
        assertEquals(SrmNasLocatorServiceImpl.STATUS_FAILED, failedTask.getStatus());
        assertTrue(failedTask.getErrorMessage().contains("根目录读取失败"));

        PageResult<SrmNasLocatorFileRespVO> page = nasLocatorService.getFilePage(buildPageReq("旧版"));
        assertEquals(1L, page.getTotal());
        assertEquals("旧版手册.docx", page.getList().get(0).getFileName());

        SrmNasLocatorStatusRespVO status = nasLocatorService.getStatus();
        assertEquals(SrmNasLocatorServiceImpl.STATUS_FAILED, status.getLatestTaskStatus());
        assertTrue(status.getMessage().contains("根目录读取失败"));
        assertEquals(SrmNasLocatorServiceImpl.PROTECTED_SCOPE_SHARE_DISPLAY, status.getScopeShare());
        assertEquals(1L, status.getFileCount());
        assertEquals(1L, status.getDirectoryCount());
    }

    @Test
    void download_shouldAllowRetainedSuccessSnapshotAndRejectDirectoryEntry() throws Exception {
        mockProtectedConfig();
        SrmNasLocatorRefreshTaskDO oldSuccessTask = buildTask(
                1L,
                SrmNasLocatorServiceImpl.STATUS_SUCCESS,
                0L,
                1L,
                null
        );
        oldSuccessTask.setFinishedTime(LocalDateTime.now().minusHours(3));
        refreshTaskMapper.insert(oldSuccessTask);
        SrmNasLocatorEntryDO oldFile = buildEntry(oldSuccessTask.getId(),
                SrmNasLocatorServiceImpl.ENTRY_TYPE_FILE,
                "旧版保留文件.pdf",
                "质量体系文件/历史/旧版保留文件.pdf",
                "质量体系文件/历史",
                321L,
                1710000100000L);
        entryMapper.insert(oldFile);

        SrmNasLocatorRefreshTaskDO latestSuccessTask = buildTask(
                1L,
                SrmNasLocatorServiceImpl.STATUS_SUCCESS,
                1L,
                1L,
                null
        );
        latestSuccessTask.setFinishedTime(LocalDateTime.now().minusHours(1));
        refreshTaskMapper.insert(latestSuccessTask);
        SrmNasLocatorEntryDO directory = buildEntry(latestSuccessTask.getId(),
                SrmNasLocatorServiceImpl.ENTRY_TYPE_DIRECTORY,
                "制度",
                "质量体系文件/制度",
                "质量体系文件",
                0L,
                1710000200000L);
        entryMapper.insert(directory);

        doAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(2);
            outputStream.write("pdf-content".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(nasBrowserService).writeFileTo(
                eq(new NasConnectionConfig("172.30.30.4", 445, "质量体系文件", "", "tester", "secret")),
                eq("历史/旧版保留文件.pdf"),
                any(OutputStream.class));

        MockHttpServletResponse response = new MockHttpServletResponse();
        nasLocatorService.download(oldFile.getId(), response);

        assertEquals("attachment; filename=\"=?UTF-8?Q?=E6=97=A7=E7=89=88=E4=BF=9D=E7=95=99=E6=96=87=E4=BB=B6.pdf?=\"; filename*=UTF-8''%E6%97%A7%E7%89%88%E4%BF%9D%E7%95%99%E6%96%87%E4%BB%B6.pdf",
                response.getHeader("Content-Disposition"));
        assertEquals("Content-Disposition", response.getHeader("Access-Control-Expose-Headers"));
        assertEquals("application/pdf", response.getContentType());
        assertEquals(321, response.getContentLength());
        assertEquals("pdf-content", response.getContentAsString(StandardCharsets.UTF_8));
        verify(nasBrowserService).writeFileTo(
                eq(new NasConnectionConfig("172.30.30.4", 445, "质量体系文件", "", "tester", "secret")),
                eq("历史/旧版保留文件.pdf"),
                any(OutputStream.class));

        AssertUtils.assertServiceException(
                () -> nasLocatorService.download(directory.getId(), new MockHttpServletResponse()),
                NAS_LOCATOR_ENTRY_NOT_FILE
        );
    }

    @Test
    void cleanupOldSnapshots_shouldKeepLatestTwoSuccessSnapshotsAndRemoveFailedSnapshot() {
        SrmNasLocatorRefreshTaskDO failedTask = buildTask(1L, SrmNasLocatorServiceImpl.STATUS_FAILED, 0L, 0L, "失败");
        failedTask.setFinishedTime(LocalDateTime.now().minusHours(1));
        refreshTaskMapper.insert(failedTask);
        entryMapper.insert(buildEntry(failedTask.getId(),
                SrmNasLocatorServiceImpl.ENTRY_TYPE_FILE,
                "失败文件.docx",
                "失败文件.docx",
                "",
                1L,
                1710000300000L));

        SrmNasLocatorRefreshTaskDO success1 = buildTask(1L, SrmNasLocatorServiceImpl.STATUS_SUCCESS, 0L, 1L, null);
        success1.setFinishedTime(LocalDateTime.now().minusHours(4));
        refreshTaskMapper.insert(success1);
        entryMapper.insert(buildEntry(success1.getId(),
                SrmNasLocatorServiceImpl.ENTRY_TYPE_FILE,
                "第一版.docx",
                "第一版.docx",
                "",
                1L,
                1710000400000L));

        SrmNasLocatorRefreshTaskDO success2 = buildTask(1L, SrmNasLocatorServiceImpl.STATUS_SUCCESS, 0L, 1L, null);
        success2.setFinishedTime(LocalDateTime.now().minusHours(3));
        refreshTaskMapper.insert(success2);
        entryMapper.insert(buildEntry(success2.getId(),
                SrmNasLocatorServiceImpl.ENTRY_TYPE_FILE,
                "第二版.docx",
                "第二版.docx",
                "",
                1L,
                1710000500000L));

        SrmNasLocatorRefreshTaskDO success3 = buildTask(1L, SrmNasLocatorServiceImpl.STATUS_SUCCESS, 0L, 1L, null);
        success3.setFinishedTime(LocalDateTime.now().minusHours(2));
        refreshTaskMapper.insert(success3);
        entryMapper.insert(buildEntry(success3.getId(),
                SrmNasLocatorServiceImpl.ENTRY_TYPE_FILE,
                "第三版.docx",
                "第三版.docx",
                "",
                1L,
                1710000600000L));

        nasLocatorService.cleanupOldSnapshots(1L);

        assertNotNull(refreshTaskMapper.selectById(success2.getId()));
        assertNotNull(refreshTaskMapper.selectById(success3.getId()));
        assertEquals(null, refreshTaskMapper.selectById(success1.getId()));
        assertEquals(null, refreshTaskMapper.selectById(failedTask.getId()));
        assertEquals(0, entryMapper.selectListByRefreshTaskId(success1.getId()).size());
        assertEquals(0, entryMapper.selectListByRefreshTaskId(failedTask.getId()).size());
    }

    private void mockProtectedConfig() {
        when(nasSettingsService.getRequiredNasConfig()).thenReturn(
                new NasConnectionConfig("172.30.30.4", 445, "质量体系文件", "", "tester", "secret")
        );
        when(blacklistSettingsService.getPatterns()).thenReturn(new ArrayList<>());
        doAnswer(invocation -> {
            NasConnectionConfig config = invocation.getArgument(0);
            NasBrowserService.NasSessionCallback<?> callback = invocation.getArgument(1);
            if (SrmNasLocatorServiceImpl.PROTECTED_SHARE_PRODUCTION.equals(config.share())) {
                return callback.execute(new StaticNasSessionScope(List.of()));
            }
            return callback.execute(new NasBrowserService.NasSessionScope() {
                @Override
                public FileNasListRespVO listFiles(String path) {
                    return nasBrowserService.listFiles(path);
                }

                @Override
                public NasFileReadResult readFile(String path) {
                    return nasBrowserService.readFile(path);
                }

                @Override
                public void writeFileTo(String path, OutputStream outputStream) {
                    nasBrowserService.writeFileTo(path, outputStream);
                }

                @Override
                public NasAclReadResult readDirectoryAcl(String path) {
                    return nasBrowserService.readDirectoryAcl(path);
                }
            });
        }).when(nasBrowserService).executeInSession(any(NasConnectionConfig.class), any());
    }

    private static FileNasListRespVO.Item rootDir(String name) {
        return buildItem(name, name, true, 0L, 1710000000000L);
    }

    private static FileNasListRespVO.Item rootFile(String name, Long size) {
        return buildItem(name, name, false, size, 1710000000000L);
    }

    private static SrmNasLocatorPageReqVO buildPageReq(String keyword) {
        SrmNasLocatorPageReqVO reqVO = new SrmNasLocatorPageReqVO();
        reqVO.setKeyword(keyword);
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        return reqVO;
    }

    private static FileNasListRespVO.Item buildItem(
            String name,
            String path,
            boolean dir,
            Long size,
            Long modifiedAt
    ) {
        return new FileNasListRespVO.Item()
                .setName(name)
                .setPath(path)
                .setDir(dir)
                .setSize(size)
                .setModifiedAt(modifiedAt);
    }

    private static SrmNasLocatorRefreshTaskDO buildTask(
            Long tenantId,
            String status,
            Long directoryCount,
            Long fileCount,
            String errorMessage
    ) {
        SrmNasLocatorRefreshTaskDO task = SrmNasLocatorRefreshTaskDO.builder()
                .status(status)
                .scopeShare(SrmNasLocatorServiceImpl.PROTECTED_SCOPE_SHARE_DISPLAY)
                .rootPath(SrmNasLocatorServiceImpl.ROOT_PATH)
                .startedTime(LocalDateTime.now().minusMinutes(5))
                .finishedTime(SrmNasLocatorServiceImpl.STATUS_RUNNING.equals(status) ? null : LocalDateTime.now().minusMinutes(1))
                .directoryCount(directoryCount)
                .fileCount(fileCount)
                .errorMessage(errorMessage)
                .build();
        task.setTenantId(tenantId);
        return task;
    }

    private static SrmNasLocatorEntryDO buildEntry(
            Long refreshTaskId,
            String entryType,
            String name,
            String path,
            String parentPath,
            Long size,
            Long modifiedAt
    ) {
        SrmNasLocatorEntryDO entry = SrmNasLocatorEntryDO.builder()
                .refreshTaskId(refreshTaskId)
                .entryType(entryType)
                .name(name)
                .path(path)
                .pathHash(DigestUtil.sha256Hex(path))
                .parentPath(parentPath)
                .size(size)
                .modifiedAt(modifiedAt)
                .build();
        entry.setTenantId(1L);
        return entry;
    }

    private static final class StaticNasSessionScope implements NasBrowserService.NasSessionScope {

        private final List<FileNasListRespVO.Item> rootItems;

        private StaticNasSessionScope(List<FileNasListRespVO.Item> rootItems) {
            this.rootItems = rootItems;
        }

        @Override
        public FileNasListRespVO listFiles(String path) {
            if (path == null || path.isBlank()) {
                return new FileNasListRespVO()
                        .setCurrentPath("")
                        .setItems(rootItems);
            }
            return new FileNasListRespVO()
                    .setCurrentPath(path)
                    .setItems(List.of());
        }

        @Override
        public NasFileReadResult readFile(String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeFileTo(String path, OutputStream outputStream) {
            throw new UnsupportedOperationException();
        }

        @Override
        public NasAclReadResult readDirectoryAcl(String path) {
            throw new UnsupportedOperationException();
        }
    }
}
