package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileNasTransferReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileNasTransferRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileLocalFolderUploadChunkDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasSourceDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasTransferTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasTransferTaskItemDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryDirectoryBindingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryDistributionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryTrainingRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccDirectoryAccessRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileLocalFolderUploadChunkMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasSourceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasTransferTaskItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasTransferTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.service.permission.DccNasPermissionSnapshotCaptureService;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasListRespVO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.service.file.NasAclAce;
import cn.iocoder.yudao.module.infra.service.file.NasAclReadResult;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasConnectionConfig;
import cn.iocoder.yudao.module.infra.service.file.NasFileReadResult;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileNasTransferServiceTest extends BaseMockitoUnitTest {

    @Mock
    private NasBrowserService nasBrowserService;
    @Mock
    private FileService fileService;
    @Mock
    private DccControlledFileWorkflowService workflowService;
    @Mock
    private DccFileDirectoryMapper directoryMapper;
    @Mock
    private DccDirectoryAccessRuleMapper directoryAccessRuleMapper;
    @Mock
    private DccFileCategoryMapper categoryMapper;
    @Mock
    private DccCategoryDirectoryBindingMapper categoryDirectoryBindingMapper;
    @Mock
    private DccFileCategoryPermissionRuleMapper permissionRuleMapper;
    @Mock
    private DccFileCategoryDistributionRuleMapper distributionRuleMapper;
    @Mock
    private DccFileCategoryTrainingRuleMapper trainingRuleMapper;
    @Mock
    private DccCategoryApprovalRouteMapper routeMapper;
    @Mock
    private DccCategoryApprovalRouteNodeMapper routeNodeMapper;
    @Mock
    private DccControlledFileNasTransferFailureReportService failureReportService;
    @Mock
    private DccControlledFileNasTransferTaskMapper taskMapper;
    @Mock
    private DccControlledFileNasTransferTaskItemMapper taskItemMapper;
    @Mock
    private DccControlledFileNasSourceMapper nasSourceMapper;
    @Mock
    private DccProjectCodeMapper projectCodeMapper;
    @Mock
    private DccControlledFileLocalFolderUploadChunkMapper uploadChunkMapper;
    @Mock
    private DccNasPermissionSnapshotCaptureService snapshotCaptureService;
    @Mock
    private NasSettingsService nasSettingsService;

    @InjectMocks
    private DccControlledFileNasTransferServiceImpl transferService;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void stubEmptyTaskSummary() {
        lenient().when(taskItemMapper.selectCountByTaskIdAndItemTypeAndDirectoryOutcome(anyLong(), anyString(), anyString()))
                .thenReturn(0L);
        lenient().when(taskItemMapper.selectCountByTaskIdAndItemTypeAndCategoryOutcome(anyLong(), anyString(), anyString()))
                .thenReturn(0L);
        lenient().when(taskItemMapper.selectCompletedFileCountByTaskId(anyLong())).thenReturn(0L);
        lenient().when(taskItemMapper.selectPreviewDownloadOnlyCompletedFileCountByTaskId(anyLong())).thenReturn(0L);
        lenient().when(taskItemMapper.selectPendingItemCountByTaskId(anyLong())).thenReturn(0L);
        lenient().when(taskItemMapper.selectFailedItemsByTaskId(anyLong())).thenReturn(List.of());
        lenient().when(projectCodeMapper.selectById(3000L)).thenReturn(DccProjectCodeDO.builder()
                .id(3000L)
                .projectName("验证项目")
                .projectCode("PRJ-20260728")
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build());
        lenient().when(nasSettingsService.getRequiredNasConfig())
                .thenReturn(new NasConnectionConfig("nas.local", 445, "quality", "", "user", "pwd"));
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void transfer_createsAsyncTaskWithoutImmediateNasTraversal() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        when(categoryMapper.selectById(900250L)).thenReturn(templateCategory());
        when(categoryDirectoryBindingMapper.selectActiveByCategoryId(900250L))
                .thenReturn(binding(900250L, 902634L));
        when(taskMapper.selectActiveTask()).thenReturn(null);
        lenient().when(taskMapper.selectWaitingTasks(any(LocalDateTime.class))).thenReturn(List.of());

        AtomicLong nextTaskId = new AtomicLong(1000L);
        List<DccControlledFileNasTransferTaskItemDO> storedItems = new ArrayList<>();
        Map<Long, DccControlledFileNasTransferTaskDO> storedTasks = new LinkedHashMap<>();
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO task = invocation.getArgument(0);
            task.setId(nextTaskId.getAndIncrement());
            storedTasks.put(task.getId(), copyTask(task));
            return 1;
        }).when(taskMapper).insert(any(DccControlledFileNasTransferTaskDO.class));
        AtomicLong nextItemId = new AtomicLong(2000L);
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            item.setId(nextItemId.getAndIncrement());
            storedItems.add(copyItem(item));
            return 1;
        }).when(taskItemMapper).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        when(taskMapper.selectById(anyLong())).thenAnswer(invocation ->
                storedTasks.get(invocation.getArgument(0)));
        stubAggregatedTaskItemSummary(() -> storedItems);

        DccControlledFileNasTransferRespVO response = transferService.transfer(99L, buildReq());

        assertEquals(1000L, response.getTaskId());
        assertEquals(DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING, response.getStatus());
        assertEquals(List.of("3.DMR/01.图纸"), response.getSelectedNasPaths());
        assertEquals(2, response.getRemainingPendingCount());
        assertEquals(3000L, readLongProperty(storedTasks.get(1000L), "dccProjectCodeId"));
        assertEquals(null, readLongProperty(storedTasks.get(1000L), "productMasterId"));
        verify(nasBrowserService, never()).listFiles(any());
        verify(nasBrowserService, never()).readFile(any());
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
    }

    @Test
    void localFolderImportContractShouldExist() throws Exception {
        Class<?> requestType = localFolderImportRequestType();
        Method method = DccControlledFileNasTransferService.class
                .getMethod("importLocalFolder", Long.class, requestType);

        assertEquals(DccControlledFileNasTransferRespVO.class, method.getReturnType());
    }

    @Test
    void largeLocalFolderImportSessionContractShouldExist() throws Exception {
        Class<?> createRequestType = localFolderImportSessionCreateRequestType();
        Method createMethod = DccControlledFileNasTransferService.class
                .getMethod("createLocalFolderImportSession", Long.class, createRequestType);
        assertEquals(DccControlledFileNasTransferRespVO.class, createMethod.getReturnType());

        Class<?> batchRequestType = localFolderImportBatchRequestType();
        Method batchMethod = DccControlledFileNasTransferService.class
                .getMethod("uploadLocalFolderImportBatch", Long.class, Long.class, batchRequestType);
        assertEquals(DccControlledFileNasTransferRespVO.class, batchMethod.getReturnType());

        Method uploadStateMethod = DccControlledFileNasTransferService.class
                .getMethod("getLocalFolderImportUploadState", Long.class, Long.class);
        assertEquals("DccControlledFileLocalFolderImportUploadStateRespVO",
                uploadStateMethod.getReturnType().getSimpleName());

        Class<?> chunkRequestType = localFolderImportChunkRequestType();
        Method chunkMethod = DccControlledFileNasTransferService.class
                .getMethod("uploadLocalFolderImportChunk", Long.class, Long.class, chunkRequestType);
        assertEquals("DccControlledFileLocalFolderImportChunkRespVO",
                chunkMethod.getReturnType().getSimpleName());

        Method completeMethod = DccControlledFileNasTransferService.class
                .getMethod("completeLocalFolderImportSession", Long.class, Long.class);
        assertEquals(DccControlledFileNasTransferRespVO.class, completeMethod.getReturnType());
    }

    @Test
    void createLocalFolderImportSession_marksTaskUploadingWithExpectedProgress() throws Exception {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        when(categoryMapper.selectById(900250L)).thenReturn(templateCategory());
        when(categoryDirectoryBindingMapper.selectActiveByCategoryId(900250L))
                .thenReturn(binding(900250L, 902634L));
        when(taskMapper.selectActiveTask()).thenReturn(null);
        lenient().when(taskMapper.selectWaitingTasks(any(LocalDateTime.class))).thenReturn(List.of());

        AtomicLong nextTaskId = new AtomicLong(1000L);
        Map<Long, DccControlledFileNasTransferTaskDO> storedTasks = new LinkedHashMap<>();
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO task = invocation.getArgument(0);
            task.setId(nextTaskId.getAndIncrement());
            storedTasks.put(task.getId(), copyTask(task));
            return 1;
        }).when(taskMapper).insert(any(DccControlledFileNasTransferTaskDO.class));
        when(taskMapper.selectById(anyLong())).thenAnswer(invocation -> copyTask(storedTasks.get(invocation.getArgument(0))));
        stubAggregatedTaskItemSummary(() -> List.of());

        Object reqVO = newLocalFolderImportSessionCreateReq(
                "2.DHF",
                15644L,
                85509730995L);

        DccControlledFileNasTransferRespVO response = invokeCreateLocalFolderImportSession(reqVO);

        assertEquals(1000L, response.getTaskId());
        assertEquals("UPLOADING", readStringProperty(response, "status"));
        assertEquals("LOCAL_FOLDER", readStringProperty(response, "sourceType"));
        assertEquals(List.of("2.DHF"), response.getSelectedNasPaths());
        assertEquals(15644L, readLongProperty(response, "expectedFileCount"));
        assertEquals(0L, readLongProperty(response, "uploadedFileCount"));
        assertEquals(85509730995L, readLongProperty(response, "expectedTotalBytes"));
        assertEquals(0L, readLongProperty(response, "uploadedTotalBytes"));
        verify(taskItemMapper, never()).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        verify(nasBrowserService, never()).listFiles(anyString());
        verify(nasBrowserService, never()).readFile(anyString());
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
    }

    @Test
    void uploadLocalFolderImportBatch_appendsFilesAndUpdatesProgressWithoutNasTraversal() throws Exception {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        when(categoryMapper.selectById(900250L)).thenReturn(templateCategory());
        when(categoryDirectoryBindingMapper.selectActiveByCategoryId(900250L))
                .thenReturn(binding(900250L, 902634L));
        when(taskMapper.selectActiveTask()).thenReturn(null);
        lenient().when(taskMapper.selectWaitingTasks(any(LocalDateTime.class))).thenReturn(List.of());

        AtomicLong nextTaskId = new AtomicLong(1000L);
        AtomicLong nextItemId = new AtomicLong(2000L);
        Map<Long, DccControlledFileNasTransferTaskDO> storedTasks = new LinkedHashMap<>();
        List<DccControlledFileNasTransferTaskItemDO> storedItems = new ArrayList<>();
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO task = invocation.getArgument(0);
            task.setId(nextTaskId.getAndIncrement());
            writeProperty(task, "expectedFileCount", 2L);
            writeProperty(task, "expectedTotalBytes", 6L);
            writeProperty(task, "uploadedFileCount", 0L);
            writeProperty(task, "uploadedTotalBytes", 0L);
            writeProperty(task, "uploadCompletedAt", null);
            storedTasks.put(task.getId(), copyTask(task));
            return 1;
        }).when(taskMapper).insert(any(DccControlledFileNasTransferTaskDO.class));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO task = invocation.getArgument(0);
            storedTasks.put(task.getId(), copyTask(task));
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileNasTransferTaskDO.class));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            item.setId(nextItemId.getAndIncrement());
            storedItems.add(copyItem(item));
            return 1;
        }).when(taskItemMapper).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        when(taskMapper.selectById(anyLong())).thenAnswer(invocation -> copyTask(storedTasks.get(invocation.getArgument(0))));
        stubAggregatedTaskItemSummary(() -> storedItems);
        when(fileService.createFileAndReturnId(any(byte[].class), eq("A.txt"), eq("dcc/original"),
                eq("text/plain"))).thenReturn(7001L);
        when(fileService.createFileAndReturnId(any(byte[].class), eq("B.txt"), eq("dcc/original"),
                eq("text/plain"))).thenReturn(7002L);

        Object createReqVO = newLocalFolderImportSessionCreateReq("2.DHF", 2L, 6L);
        invokeCreateLocalFolderImportSession(createReqVO);

        Object batchReqVO = newLocalFolderImportBatchReq(
                List.of("2.DHF/A.txt", "2.DHF/Sub/B.txt"),
                new MockMultipartFile("files[]", "A.txt", "text/plain", "abc".getBytes()),
                new MockMultipartFile("files[]", "B.txt", "text/plain", "def".getBytes()));

        DccControlledFileNasTransferRespVO response = invokeUploadLocalFolderImportBatch(1000L, batchReqVO);

        assertEquals("UPLOADING", readStringProperty(response, "status"));
        assertEquals(2L, readLongProperty(response, "expectedFileCount"));
        assertEquals(2L, readLongProperty(response, "uploadedFileCount"));
        assertEquals(6L, readLongProperty(response, "expectedTotalBytes"));
        assertEquals(6L, readLongProperty(response, "uploadedTotalBytes"));
        assertEquals(4, response.getRemainingPendingCount());
        assertEquals(2, storedItems.stream()
                .filter(item -> DccControlledFileNasTransferServiceImpl.ITEM_TYPE_FILE.equals(item.getItemType()))
                .count());
        verify(nasBrowserService, never()).listFiles(anyString());
        verify(nasBrowserService, never()).readFile(anyString());
        verify(nasBrowserService, never()).readDirectoryAcl(anyString());
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
    }

    @Test
    void uploadLocalFolderImportChunk_mergesCompletedFileAndUpdatesUploadState() throws Exception {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        ReflectionTestUtils.setField(transferService, "multipartLocation", tempDir.toString());
        TenantContextHolder.setTenantId(1L);

        Map<Long, DccControlledFileNasTransferTaskDO> storedTasks = new LinkedHashMap<>();
        DccControlledFileNasTransferTaskDO task = DccControlledFileNasTransferTaskDO.builder()
                .id(10L)
                .operatorUserId(99L)
                .templateCategoryId(900250L)
                .dccProjectCodeId(3000L)
                .productMasterId(null)
                .effectiveDate(LocalDate.of(2026, 6, 14))
                .selectedNasPathsJson("[\"3.DMR\"]")
                .sourceType(DccControlledFileNasTransferServiceImpl.SOURCE_TYPE_LOCAL_FOLDER)
                .status(DccControlledFileNasTransferServiceImpl.TASK_STATUS_UPLOADING)
                .build();
        writeProperty(task, "expectedFileCount", 1L);
        writeProperty(task, "expectedTotalBytes", 6L);
        writeProperty(task, "uploadedFileCount", 0L);
        writeProperty(task, "uploadedTotalBytes", 0L);
        storedTasks.put(10L, copyTask(task));

        AtomicLong nextChunkId = new AtomicLong(3000L);
        AtomicLong nextItemId = new AtomicLong(4000L);
        List<DccControlledFileLocalFolderUploadChunkDO> storedChunks = new ArrayList<>();
        List<DccControlledFileNasTransferTaskItemDO> storedItems = new ArrayList<>();
        when(taskMapper.selectById(10L)).thenAnswer(invocation -> copyTask(storedTasks.get(10L)));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO updatedTask = invocation.getArgument(0);
            storedTasks.put(updatedTask.getId(), copyTask(updatedTask));
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileNasTransferTaskDO.class));
        when(taskItemMapper.selectListByTaskId(10L)).thenAnswer(invocation -> storedItems.stream()
                .map(DccControlledFileNasTransferServiceTest::copyItem)
                .toList());
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            item.setId(nextItemId.getAndIncrement());
            storedItems.add(copyItem(item));
            return 1;
        }).when(taskItemMapper).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        when(uploadChunkMapper.selectByTaskIdAndRelativePathAndChunkIndex(anyLong(), anyString(), any()))
                .thenAnswer(invocation -> storedChunks.stream()
                        .filter(chunk -> chunk.getTaskId().equals(invocation.getArgument(0)))
                        .filter(chunk -> chunk.getRelativePath().equals(invocation.getArgument(1)))
                        .filter(chunk -> chunk.getChunkIndex().equals(invocation.getArgument(2)))
                        .findFirst()
                        .orElse(null));
        when(uploadChunkMapper.selectListByTaskIdAndRelativePath(anyLong(), anyString()))
                .thenAnswer(invocation -> storedChunks.stream()
                        .filter(chunk -> chunk.getTaskId().equals(invocation.getArgument(0)))
                        .filter(chunk -> chunk.getRelativePath().equals(invocation.getArgument(1)))
                        .sorted(Comparator.comparing(DccControlledFileLocalFolderUploadChunkDO::getChunkIndex))
                        .toList());
        doAnswer(invocation -> {
            DccControlledFileLocalFolderUploadChunkDO chunk = invocation.getArgument(0);
            chunk.setId(nextChunkId.getAndIncrement());
            storedChunks.add(chunk);
            return 1;
        }).when(uploadChunkMapper).insert(any(DccControlledFileLocalFolderUploadChunkDO.class));
        when(fileService.createFileAndReturnId(any(Path.class), eq(6L), eq("Manual.pdf"),
                eq("dcc/original"), eq("application/pdf"))).thenReturn(7100L);
        stubAggregatedTaskItemSummary(() -> storedItems);

        byte[] firstChunk = "abc".getBytes(StandardCharsets.UTF_8);
        byte[] secondChunk = "def".getBytes(StandardCharsets.UTF_8);
        Object firstReqVO = newLocalFolderImportChunkReq(
                "3.DMR/Manual.pdf", "Manual.pdf", 6L, 0, 2,
                sha256Hex(firstChunk), "application/pdf",
                new MockMultipartFile("chunk", "Manual.pdf.part0", "application/octet-stream", firstChunk));
        Object firstResponse = invokeUploadLocalFolderImportChunk(10L, firstReqVO);

        assertEquals(Boolean.FALSE, readProperty(firstResponse, "fileCompleted"));
        assertEquals(1, ((Number) readProperty(firstResponse, "uploadedChunkCount")).intValue());
        assertEquals(0L, readLongProperty(readProperty(firstResponse, "task"), "uploadedFileCount"));
        assertTrue(storedItems.isEmpty());

        Object secondReqVO = newLocalFolderImportChunkReq(
                "3.DMR/Manual.pdf", "Manual.pdf", 6L, 1, 2,
                sha256Hex(secondChunk), "application/pdf",
                new MockMultipartFile("chunk", "Manual.pdf.part1", "application/octet-stream", secondChunk));
        Object secondResponse = invokeUploadLocalFolderImportChunk(10L, secondReqVO);

        assertEquals(Boolean.TRUE, readProperty(secondResponse, "fileCompleted"));
        assertEquals(2, ((Number) readProperty(secondResponse, "uploadedChunkCount")).intValue());
        assertEquals(1L, readLongProperty(readProperty(secondResponse, "task"), "uploadedFileCount"));
        assertEquals(6L, readLongProperty(readProperty(secondResponse, "task"), "uploadedTotalBytes"));
        assertEquals(1, storedItems.stream()
                .filter(item -> DccControlledFileNasTransferServiceImpl.ITEM_TYPE_FILE.equals(item.getItemType()))
                .filter(item -> "3.DMR/Manual.pdf".equals(item.getNasPath()))
                .filter(item -> Long.valueOf(7100L).equals(item.getSourceFileId()))
                .count());
        verify(fileService).createFileAndReturnId(any(Path.class), eq(6L), eq("Manual.pdf"),
                eq("dcc/original"), eq("application/pdf"));
        verify(nasBrowserService, never()).listFiles(anyString());
        verify(nasBrowserService, never()).readFile(anyString());
    }

    @Test
    void uploadLocalFolderImportChunk_acceptsZeroByteFileAndUpdatesUploadState() throws Exception {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        ReflectionTestUtils.setField(transferService, "multipartLocation", tempDir.toString());
        TenantContextHolder.setTenantId(1L);

        Map<Long, DccControlledFileNasTransferTaskDO> storedTasks = new LinkedHashMap<>();
        DccControlledFileNasTransferTaskDO task = DccControlledFileNasTransferTaskDO.builder()
                .id(18L)
                .operatorUserId(99L)
                .templateCategoryId(900250L)
                .dccProjectCodeId(3000L)
                .productMasterId(null)
                .effectiveDate(LocalDate.of(2026, 6, 15))
                .selectedNasPathsJson("[\"3.DMR\"]")
                .sourceType(DccControlledFileNasTransferServiceImpl.SOURCE_TYPE_LOCAL_FOLDER)
                .status(DccControlledFileNasTransferServiceImpl.TASK_STATUS_UPLOADING)
                .build();
        writeProperty(task, "expectedFileCount", 1L);
        writeProperty(task, "expectedTotalBytes", 0L);
        writeProperty(task, "uploadedFileCount", 0L);
        writeProperty(task, "uploadedTotalBytes", 0L);
        storedTasks.put(18L, copyTask(task));

        AtomicLong nextChunkId = new AtomicLong(3000L);
        AtomicLong nextItemId = new AtomicLong(4000L);
        List<DccControlledFileLocalFolderUploadChunkDO> storedChunks = new ArrayList<>();
        List<DccControlledFileNasTransferTaskItemDO> storedItems = new ArrayList<>();
        when(taskMapper.selectById(18L)).thenAnswer(invocation -> copyTask(storedTasks.get(18L)));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO updatedTask = invocation.getArgument(0);
            storedTasks.put(updatedTask.getId(), copyTask(updatedTask));
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileNasTransferTaskDO.class));
        when(taskItemMapper.selectListByTaskId(18L)).thenAnswer(invocation -> storedItems.stream()
                .map(DccControlledFileNasTransferServiceTest::copyItem)
                .toList());
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            item.setId(nextItemId.getAndIncrement());
            storedItems.add(copyItem(item));
            return 1;
        }).when(taskItemMapper).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        when(uploadChunkMapper.selectByTaskIdAndRelativePathAndChunkIndex(anyLong(), anyString(), any()))
                .thenAnswer(invocation -> storedChunks.stream()
                        .filter(chunk -> chunk.getTaskId().equals(invocation.getArgument(0)))
                        .filter(chunk -> chunk.getRelativePath().equals(invocation.getArgument(1)))
                        .filter(chunk -> chunk.getChunkIndex().equals(invocation.getArgument(2)))
                        .findFirst()
                        .orElse(null));
        when(uploadChunkMapper.selectListByTaskIdAndRelativePath(anyLong(), anyString()))
                .thenAnswer(invocation -> storedChunks.stream()
                        .filter(chunk -> chunk.getTaskId().equals(invocation.getArgument(0)))
                        .filter(chunk -> chunk.getRelativePath().equals(invocation.getArgument(1)))
                        .sorted(Comparator.comparing(DccControlledFileLocalFolderUploadChunkDO::getChunkIndex))
                        .toList());
        doAnswer(invocation -> {
            DccControlledFileLocalFolderUploadChunkDO chunk = invocation.getArgument(0);
            chunk.setId(nextChunkId.getAndIncrement());
            storedChunks.add(chunk);
            return 1;
        }).when(uploadChunkMapper).insert(any(DccControlledFileLocalFolderUploadChunkDO.class));
        when(fileService.createFileAndReturnId(any(Path.class), eq(0L), eq("非精准分类.txt"),
                eq("dcc/original"), eq("text/plain"))).thenReturn(7101L);
        stubAggregatedTaskItemSummary(() -> storedItems);

        byte[] emptyChunk = new byte[0];
        Object reqVO = newLocalFolderImportChunkReq(
                "3.DMR/06.物料清单/非精准分类.txt", "非精准分类.txt", 0L, 0, 1,
                sha256Hex(emptyChunk), "text/plain",
                new MockMultipartFile("chunk", "非精准分类.txt.part0", "text/plain", emptyChunk));
        Object response = invokeUploadLocalFolderImportChunk(18L, reqVO);

        assertEquals(Boolean.TRUE, readProperty(response, "fileCompleted"));
        assertEquals(1, ((Number) readProperty(response, "uploadedChunkCount")).intValue());
        assertEquals(1, ((Number) readProperty(response, "totalChunks")).intValue());
        assertEquals(1L, readLongProperty(readProperty(response, "task"), "uploadedFileCount"));
        assertEquals(0L, readLongProperty(readProperty(response, "task"), "uploadedTotalBytes"));
        assertEquals(1, storedItems.stream()
                .filter(item -> DccControlledFileNasTransferServiceImpl.ITEM_TYPE_FILE.equals(item.getItemType()))
                .filter(item -> "3.DMR/06.物料清单/非精准分类.txt".equals(item.getNasPath()))
                .filter(item -> Long.valueOf(7101L).equals(item.getSourceFileId()))
                .count());
        verify(fileService).createFileAndReturnId(any(Path.class), eq(0L), eq("非精准分类.txt"),
                eq("dcc/original"), eq("text/plain"));
    }

    @Test
    void completeLocalFolderImportSession_movesUploadedTaskToWaitingAndKeepsProgressEvidence() throws Exception {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        Map<Long, DccControlledFileNasTransferTaskDO> storedTasks = new LinkedHashMap<>();
        DccControlledFileNasTransferTaskDO task = DccControlledFileNasTransferTaskDO.builder()
                .id(10L)
                .operatorUserId(99L)
                .templateCategoryId(900250L)
                .dccProjectCodeId(3000L)
                .productMasterId(null)
                .effectiveDate(LocalDate.of(2026, 6, 14))
                .selectedNasPathsJson("[\"2.DHF\"]")
                .sourceType(DccControlledFileNasTransferServiceImpl.SOURCE_TYPE_LOCAL_FOLDER)
                .status("UPLOADING")
                .build();
        writeProperty(task, "expectedFileCount", 1L);
        writeProperty(task, "expectedTotalBytes", 3L);
        writeProperty(task, "uploadedFileCount", 1L);
        writeProperty(task, "uploadedTotalBytes", 3L);
        storedTasks.put(10L, copyTask(task));

        List<DccControlledFileNasTransferTaskItemDO> storedItems = List.of(
                DccControlledFileNasTransferTaskItemDO.builder()
                        .id(100L)
                        .taskId(10L)
                        .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_DIRECTORY)
                        .nasPath("2.DHF")
                        .itemName("2.DHF")
                        .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING)
                        .attemptCount(0)
                        .previewDownloadOnly(Boolean.FALSE)
                        .build(),
                DccControlledFileNasTransferTaskItemDO.builder()
                        .id(101L)
                        .taskId(10L)
                        .parentItemId(100L)
                        .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_FILE)
                        .nasPath("2.DHF/A.txt")
                        .itemName("A.txt")
                        .sourceFileId(7001L)
                        .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING)
                        .attemptCount(0)
                        .previewDownloadOnly(Boolean.FALSE)
                        .build());
        when(taskMapper.selectById(10L)).thenAnswer(invocation -> copyTask(storedTasks.get(10L)));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO updatedTask = invocation.getArgument(0);
            storedTasks.put(updatedTask.getId(), copyTask(updatedTask));
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileNasTransferTaskDO.class));
        lenient().when(taskMapper.selectWaitingTasks(any(LocalDateTime.class))).thenReturn(List.of());
        stubAggregatedTaskItemSummary(() -> storedItems);

        DccControlledFileNasTransferRespVO response = invokeCompleteLocalFolderImportSession(10L);

        assertEquals(DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING, response.getStatus());
        assertEquals(1L, readLongProperty(response, "expectedFileCount"));
        assertEquals(1L, readLongProperty(response, "uploadedFileCount"));
        assertEquals(3L, readLongProperty(response, "expectedTotalBytes"));
        assertEquals(3L, readLongProperty(response, "uploadedTotalBytes"));
        assertTrue(readStringProperty(response, "uploadCompletedAt") != null);
        assertEquals(2, response.getRemainingPendingCount());
    }

    @Test
    void createLocalFolderImportSession_reusesActiveUploadingTaskForSameSelection() throws Exception {
        TenantContextHolder.setTenantId(1L);
        when(categoryMapper.selectById(900250L)).thenReturn(templateCategory());
        when(categoryDirectoryBindingMapper.selectActiveByCategoryId(900250L))
                .thenReturn(binding(900250L, 1L));

        DccControlledFileNasTransferTaskDO task = DccControlledFileNasTransferTaskDO.builder()
                .id(18L)
                .operatorUserId(99L)
                .templateCategoryId(900250L)
                .dccProjectCodeId(3000L)
                .productMasterId(null)
                .effectiveDate(LocalDate.of(2026, 6, 15))
                .selectedNasPathsJson("[\"3.DMR\"]")
                .sourceType(DccControlledFileNasTransferServiceImpl.SOURCE_TYPE_LOCAL_FOLDER)
                .status(DccControlledFileNasTransferServiceImpl.TASK_STATUS_UPLOADING)
                .build();
        writeProperty(task, "expectedFileCount", 2L);
        writeProperty(task, "expectedTotalBytes", 1234L);

        when(taskMapper.selectActiveTask()).thenReturn(copyTask(task));
        when(taskMapper.selectById(18L)).thenReturn(copyTask(task));

        Object reqVO = newLocalFolderImportSessionCreateReq("3.DMR", 2L, 1234L);
        DccControlledFileNasTransferRespVO response = invokeCreateLocalFolderImportSession(reqVO);

        assertEquals(18L, response.getTaskId());
        assertEquals(DccControlledFileNasTransferServiceImpl.TASK_STATUS_UPLOADING, response.getStatus());
        verify(taskMapper, never()).insert(any(DccControlledFileNasTransferTaskDO.class));
    }

    @Test
    void importLocalFolder_createsTaskItemsFromRelativePathsWithoutNasTraversal() throws Exception {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        when(categoryMapper.selectById(900250L)).thenReturn(templateCategory());
        when(categoryDirectoryBindingMapper.selectActiveByCategoryId(900250L))
                .thenReturn(binding(900250L, 1L));
        when(taskMapper.selectActiveTask()).thenReturn(null);
        lenient().when(taskMapper.selectWaitingTasks(any(LocalDateTime.class))).thenReturn(List.of());
        when(fileService.createFileAndReturnId(any(byte[].class), eq("SOP.docx"), eq("dcc/original"),
                eq("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))).thenReturn(7001L);
        when(fileService.createFileAndReturnId(any(byte[].class), eq("Spec.pdf"), eq("dcc/original"),
                eq("application/pdf"))).thenReturn(7002L);

        AtomicLong nextTaskId = new AtomicLong(1000L);
        AtomicLong nextItemId = new AtomicLong(2000L);
        Map<Long, DccControlledFileNasTransferTaskDO> storedTasks = new LinkedHashMap<>();
        List<DccControlledFileNasTransferTaskItemDO> storedItems = new ArrayList<>();
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO task = invocation.getArgument(0);
            task.setId(nextTaskId.getAndIncrement());
            storedTasks.put(task.getId(), copyTask(task));
            return 1;
        }).when(taskMapper).insert(any(DccControlledFileNasTransferTaskDO.class));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            item.setId(nextItemId.getAndIncrement());
            storedItems.add(copyItem(item));
            return 1;
        }).when(taskItemMapper).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        when(taskMapper.selectById(anyLong())).thenAnswer(invocation ->
                storedTasks.get(invocation.getArgument(0)));
        stubAggregatedTaskItemSummary(() -> storedItems);

        Object reqVO = newLocalFolderImportReq(
                "3.DMR",
                List.of("3.DMR/SOP.docx", "3.DMR/Sub/Spec.pdf"),
                new MockMultipartFile("files[]", "SOP.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx".getBytes()),
                new MockMultipartFile("files[]", "Spec.pdf", "application/pdf", "pdf".getBytes()));

        DccControlledFileNasTransferRespVO response = invokeLocalFolderImport(reqVO);

        assertEquals(1000L, response.getTaskId());
        assertEquals("LOCAL_FOLDER", readStringProperty(response, "sourceType"));
        assertEquals(List.of("3.DMR"), response.getSelectedNasPaths());
        assertEquals(4, response.getRemainingPendingCount());
        assertEquals("LOCAL_FOLDER", readStringProperty(storedTasks.get(1000L), "sourceType"));
        assertEquals(3000L, readLongProperty(storedTasks.get(1000L), "dccProjectCodeId"));
        assertEquals(null, readLongProperty(storedTasks.get(1000L), "productMasterId"));
        assertEquals(2, storedItems.stream()
                .filter(item -> DccControlledFileNasTransferServiceImpl.ITEM_TYPE_FILE.equals(item.getItemType()))
                .count());
        assertEquals(List.of(7001L, 7002L), storedItems.stream()
                .filter(item -> DccControlledFileNasTransferServiceImpl.ITEM_TYPE_FILE.equals(item.getItemType()))
                .map(item -> readLongProperty(item, "sourceFileId"))
                .toList());
        verify(nasBrowserService, never()).listFiles(anyString());
        verify(nasBrowserService, never()).readFile(anyString());
        verify(nasBrowserService, never()).readDirectoryAcl(anyString());
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
    }

    @Test
    void importLocalFolder_rejectsUnsafeRelativePathBeforeCreatingTask() throws Exception {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);

        Object reqVO = newLocalFolderImportReq(
                "3.DMR",
                List.of("3.DMR/../Spec.pdf"),
                new MockMultipartFile("files[]", "Spec.pdf", "application/pdf", "pdf".getBytes()));

        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> invokeLocalFolderImport(reqVO));

        assertTrue(exception.getTargetException().getMessage().contains("relative path"));
        verify(taskMapper, never()).insert(any(DccControlledFileNasTransferTaskDO.class));
        verify(taskItemMapper, never()).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        verify(fileService, never()).createFileAndReturnId(any(byte[].class), anyString(), anyString(), anyString());
    }

    @Test
    void processWaitingTasks_importsLocalFolderUnderSelectedCategoryDirectoryWithoutNasTraversalOrAclSnapshot() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        when(categoryMapper.selectById(900250L)).thenReturn(templateCategory());

        Map<Long, DccControlledFileNasTransferTaskDO> tasks = new LinkedHashMap<>();
        Map<Long, DccControlledFileNasTransferTaskItemDO> items = new LinkedHashMap<>();
        AtomicLong nextDirectoryId = new AtomicLong(200L);
        List<DccFileDirectoryDO> directories = new ArrayList<>(List.of(
                directory(1L, null, "DCC Local Imports", 1)
        ));
        tasks.put(10L, DccControlledFileNasTransferTaskDO.builder()
                .id(10L)
                .operatorUserId(99L)
                .templateCategoryId(900250L)
                .dccProjectCodeId(3000L)
                .productMasterId(null)
                .effectiveDate(LocalDate.of(2026, 6, 13))
                .selectedNasPathsJson("[\"3.DMR\"]")
                .sourceType(DccControlledFileNasTransferServiceImpl.SOURCE_TYPE_LOCAL_FOLDER)
                .status(DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING)
                .build());
        items.put(100L, DccControlledFileNasTransferTaskItemDO.builder()
                .id(100L)
                .taskId(10L)
                .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_DIRECTORY)
                .nasPath("3.DMR")
                .itemName("3.DMR")
                .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING)
                .attemptCount(0)
                .build());
        items.put(101L, DccControlledFileNasTransferTaskItemDO.builder()
                .id(101L)
                .taskId(10L)
                .parentItemId(100L)
                .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_DIRECTORY)
                .nasPath("3.DMR/Sub")
                .itemName("Sub")
                .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING)
                .attemptCount(0)
                .build());
        items.put(102L, DccControlledFileNasTransferTaskItemDO.builder()
                .id(102L)
                .taskId(10L)
                .parentItemId(101L)
                .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_FILE)
                .nasPath("3.DMR/Sub/Spec.pdf")
                .itemName("Spec.pdf")
                .sourceFileId(7002L)
                .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING)
                .attemptCount(0)
                .build());

        when(taskMapper.selectWaitingTasks(any(LocalDateTime.class))).thenAnswer(invocation -> tasks.values().stream()
                .filter(task -> DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING.equals(task.getStatus()))
                .map(DccControlledFileNasTransferServiceTest::copyTask)
                .toList());
        when(taskMapper.claimWaitingTask(eq(10L), any(LocalDateTime.class))).thenAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO task = tasks.get(10L);
            if (task == null || !DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING.equals(task.getStatus())) {
                return 0;
            }
            task.setStatus(DccControlledFileNasTransferServiceImpl.TASK_STATUS_RUNNING);
            task.setLastRunAt(invocation.getArgument(1));
            return 1;
        });
        when(taskMapper.selectById(10L)).thenAnswer(invocation -> copyTask(tasks.get(10L)));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO task = invocation.getArgument(0);
            tasks.put(task.getId(), copyTask(task));
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileNasTransferTaskDO.class));
        when(taskItemMapper.selectListByTaskId(10L)).thenAnswer(invocation -> items.values().stream()
                .sorted(Comparator.comparingLong(DccControlledFileNasTransferTaskItemDO::getId))
                .map(DccControlledFileNasTransferServiceTest::copyItem)
                .toList());
        stubAggregatedTaskItemSummary(() -> items.values().stream().toList());
        when(taskItemMapper.selectFirstWaitingItemByTaskId(10L)).thenAnswer(invocation -> items.values().stream()
                .filter(item -> DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING.equals(item.getStatus()))
                .sorted(Comparator.comparingLong(DccControlledFileNasTransferTaskItemDO::getId))
                .map(DccControlledFileNasTransferServiceTest::copyItem)
                .findFirst()
                .orElse(null));
        when(taskItemMapper.claimWaitingItem(anyLong())).thenAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = items.get(invocation.getArgument(0));
            if (item == null || !DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING.equals(item.getStatus())) {
                return 0;
            }
            item.setStatus(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_RUNNING);
            return 1;
        });
        when(taskItemMapper.selectById(anyLong())).thenAnswer(invocation ->
                copyItem(items.get(invocation.getArgument(0))));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            items.put(item.getId(), copyItem(item));
            return 1;
        }).when(taskItemMapper).updateById(any(DccControlledFileNasTransferTaskItemDO.class));

        when(directoryMapper.selectList()).thenAnswer(invocation -> directories.stream()
                .map(DccControlledFileNasTransferServiceTest::copyDirectory)
                .toList());
        doAnswer(invocation -> {
            DccFileDirectoryDO directory = invocation.getArgument(0);
            directory.setId(nextDirectoryId.getAndIncrement());
            directories.add(copyDirectory(directory));
            return 1;
        }).when(directoryMapper).insert(any(DccFileDirectoryDO.class));
        when(directoryAccessRuleMapper.selectList()).thenReturn(List.of());
        when(categoryMapper.selectList()).thenReturn(List.of(templateCategory()));
        when(categoryDirectoryBindingMapper.selectList()).thenReturn(List.of(binding(900250L, 1L)));
        when(permissionRuleMapper.selectList()).thenReturn(List.of());
        when(distributionRuleMapper.selectList()).thenReturn(List.of());
        when(trainingRuleMapper.selectList()).thenReturn(List.of());
        when(routeMapper.selectList()).thenReturn(List.of());
        when(routeNodeMapper.selectList()).thenReturn(List.of());
        when(workflowService.submitControlledFileWithoutApproval(eq(99L), any(DccControlledFileSubmitReqVO.class)))
                .thenReturn(6001L);

        transferService.processWaitingTasks();
        DccControlledFileNasTransferRespVO result = transferService.getTask(99L, 10L);

        assertEquals(DccControlledFileNasTransferServiceImpl.TASK_STATUS_COMPLETED, result.getStatus());
        assertEquals("LOCAL_FOLDER", readStringProperty(result, "sourceType"));
        assertEquals(2, result.getCreatedDirectoryCount());
        assertEquals(1, result.getCreatedFileCount());
        assertEquals(0, result.getFailedFileCount());
        assertEquals(0, result.getRemainingPendingCount());
        DccFileDirectoryDO importedRoot = directories.stream()
                .filter(directory -> "3.DMR".equals(directory.getName()) && Long.valueOf(1L).equals(directory.getParentId()))
                .findFirst()
                .orElseThrow();
        DccFileDirectoryDO importedSub = directories.stream()
                .filter(directory -> "Sub".equals(directory.getName())
                        && importedRoot.getId().equals(directory.getParentId()))
                .findFirst()
                .orElseThrow();
        ArgumentCaptor<DccControlledFileSubmitReqVO> submitCaptor =
                ArgumentCaptor.forClass(DccControlledFileSubmitReqVO.class);
        verify(workflowService).submitControlledFileWithoutApproval(eq(99L), submitCaptor.capture());
        assertEquals(7002L, submitCaptor.getValue().getOriginalFileId());
        assertEquals(3000L, submitCaptor.getValue().getDccProjectCodeId());
        assertEquals(null, submitCaptor.getValue().getProductMasterId());
        assertEquals("Spec.pdf", submitCaptor.getValue().getFileName());
        assertEquals(importedSub.getId(), submitCaptor.getValue().getDirectoryId());
        assertEquals("Local folder import source: 3.DMR/Sub/Spec.pdf", submitCaptor.getValue().getRemark());
        verify(nasBrowserService, never()).listFiles(anyString());
        verify(nasBrowserService, never()).readFile(anyString());
        verify(nasBrowserService, never()).readDirectoryAcl(anyString());
        verify(fileService, never()).createFileAndReturnId(any(byte[].class), anyString(), anyString(), anyString());
        verify(snapshotCaptureService, never()).captureDirectorySnapshot(anyLong(), anyLong(), anyString(),
                anyLong(), any(NasAclReadResult.class));
        verify(snapshotCaptureService, never()).completeSnapshotForTask(10L);
    }

    @Test
    void processWaitingTasks_reusesSameNamedBindingDirectoryAsLocalFolderRoot() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        when(categoryMapper.selectById(900250L)).thenReturn(templateCategory());

        Map<Long, DccControlledFileNasTransferTaskDO> tasks = new LinkedHashMap<>();
        Map<Long, DccControlledFileNasTransferTaskItemDO> items = new LinkedHashMap<>();
        List<DccFileDirectoryDO> directories = new ArrayList<>(List.of(
                directory(1L, null, "1. QMS documents", 1)
        ));
        tasks.put(10L, DccControlledFileNasTransferTaskDO.builder()
                .id(10L)
                .operatorUserId(99L)
                .templateCategoryId(900250L)
                .dccProjectCodeId(3000L)
                .productMasterId(null)
                .effectiveDate(LocalDate.of(2026, 6, 14))
                .selectedNasPathsJson("[\"1. QMS documents\"]")
                .sourceType(DccControlledFileNasTransferServiceImpl.SOURCE_TYPE_LOCAL_FOLDER)
                .status(DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING)
                .build());
        items.put(100L, DccControlledFileNasTransferTaskItemDO.builder()
                .id(100L)
                .taskId(10L)
                .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_DIRECTORY)
                .nasPath("1. QMS documents")
                .itemName("1. QMS documents")
                .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING)
                .attemptCount(0)
                .build());
        items.put(101L, DccControlledFileNasTransferTaskItemDO.builder()
                .id(101L)
                .taskId(10L)
                .parentItemId(100L)
                .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_FILE)
                .nasPath("1. QMS documents/QMS文件清单.xlsx")
                .itemName("QMS文件清单.xlsx")
                .sourceFileId(7001L)
                .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING)
                .attemptCount(0)
                .build());

        when(taskMapper.selectWaitingTasks(any(LocalDateTime.class))).thenAnswer(invocation -> tasks.values().stream()
                .filter(task -> DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING.equals(task.getStatus()))
                .map(DccControlledFileNasTransferServiceTest::copyTask)
                .toList());
        when(taskMapper.claimWaitingTask(eq(10L), any(LocalDateTime.class))).thenAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO task = tasks.get(10L);
            if (task == null || !DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING.equals(task.getStatus())) {
                return 0;
            }
            task.setStatus(DccControlledFileNasTransferServiceImpl.TASK_STATUS_RUNNING);
            task.setLastRunAt(invocation.getArgument(1));
            return 1;
        });
        when(taskMapper.selectById(10L)).thenAnswer(invocation -> copyTask(tasks.get(10L)));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO task = invocation.getArgument(0);
            tasks.put(task.getId(), copyTask(task));
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileNasTransferTaskDO.class));
        when(taskItemMapper.selectListByTaskId(10L)).thenAnswer(invocation -> items.values().stream()
                .sorted(Comparator.comparingLong(DccControlledFileNasTransferTaskItemDO::getId))
                .map(DccControlledFileNasTransferServiceTest::copyItem)
                .toList());
        stubAggregatedTaskItemSummary(() -> items.values().stream().toList());
        when(taskItemMapper.selectFirstWaitingItemByTaskId(10L)).thenAnswer(invocation -> items.values().stream()
                .filter(item -> DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING.equals(item.getStatus()))
                .sorted(Comparator.comparingLong(DccControlledFileNasTransferTaskItemDO::getId))
                .map(DccControlledFileNasTransferServiceTest::copyItem)
                .findFirst()
                .orElse(null));
        when(taskItemMapper.claimWaitingItem(anyLong())).thenAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = items.get(invocation.getArgument(0));
            if (item == null || !DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING.equals(item.getStatus())) {
                return 0;
            }
            item.setStatus(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_RUNNING);
            return 1;
        });
        when(taskItemMapper.selectById(anyLong())).thenAnswer(invocation ->
                copyItem(items.get(invocation.getArgument(0))));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            items.put(item.getId(), copyItem(item));
            return 1;
        }).when(taskItemMapper).updateById(any(DccControlledFileNasTransferTaskItemDO.class));

        when(directoryMapper.selectList()).thenAnswer(invocation -> directories.stream()
                .map(DccControlledFileNasTransferServiceTest::copyDirectory)
                .toList());
        lenient().doAnswer(invocation -> {
            DccFileDirectoryDO directory = invocation.getArgument(0);
            directory.setId(200L);
            directories.add(copyDirectory(directory));
            return 1;
        }).when(directoryMapper).insert(any(DccFileDirectoryDO.class));
        when(directoryAccessRuleMapper.selectList()).thenReturn(List.of());
        when(categoryMapper.selectList()).thenReturn(List.of(templateCategory()));
        when(categoryDirectoryBindingMapper.selectList()).thenReturn(List.of(binding(900250L, 1L)));
        when(permissionRuleMapper.selectList()).thenReturn(List.of());
        when(distributionRuleMapper.selectList()).thenReturn(List.of());
        when(trainingRuleMapper.selectList()).thenReturn(List.of());
        when(routeMapper.selectList()).thenReturn(List.of());
        when(routeNodeMapper.selectList()).thenReturn(List.of());
        when(workflowService.submitControlledFileWithoutApproval(eq(99L), any(DccControlledFileSubmitReqVO.class)))
                .thenReturn(6001L);

        transferService.processWaitingTasks();
        DccControlledFileNasTransferRespVO result = transferService.getTask(99L, 10L);

        assertEquals(DccControlledFileNasTransferServiceImpl.TASK_STATUS_COMPLETED, result.getStatus());
        assertEquals(0, result.getCreatedDirectoryCount());
        assertEquals(1, result.getCreatedFileCount());
        assertEquals(0, directories.stream()
                .filter(directory -> Long.valueOf(1L).equals(directory.getParentId()))
                .filter(directory -> "1. QMS documents".equals(directory.getName()))
                .count());
        ArgumentCaptor<DccControlledFileSubmitReqVO> submitCaptor =
                ArgumentCaptor.forClass(DccControlledFileSubmitReqVO.class);
        verify(workflowService).submitControlledFileWithoutApproval(eq(99L), submitCaptor.capture());
        assertEquals(3000L, submitCaptor.getValue().getDccProjectCodeId());
        assertEquals(null, submitCaptor.getValue().getProductMasterId());
        assertEquals(1L, submitCaptor.getValue().getDirectoryId());
        assertEquals("Local folder import source: 1. QMS documents/QMS文件清单.xlsx",
                submitCaptor.getValue().getRemark());
        verify(nasBrowserService, never()).listFiles(anyString());
        verify(nasBrowserService, never()).readFile(anyString());
        verify(nasBrowserService, never()).readDirectoryAcl(anyString());
        verify(fileService, never()).createFileAndReturnId(any(byte[].class), anyString(), anyString(), anyString());
    }

    @Test
    void processWaitingTasks_createdChildDirectoryDoesNotBecomeManualAccessRuleBinding() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        when(categoryMapper.selectById(900250L)).thenReturn(templateCategory());

        Map<Long, DccControlledFileNasTransferTaskDO> tasks = new LinkedHashMap<>();
        Map<Long, DccControlledFileNasTransferTaskItemDO> items = new LinkedHashMap<>();
        List<DccFileDirectoryDO> directories = new ArrayList<>(List.of(
                directory(1L, null, "质量管理", 1)
        ));
        tasks.put(10L, DccControlledFileNasTransferTaskDO.builder()
                .id(10L)
                .operatorUserId(99L)
                .templateCategoryId(900250L)
                .dccProjectCodeId(3000L)
                .productMasterId(null)
                .effectiveDate(LocalDate.of(2026, 6, 26))
                .selectedNasPathsJson("[\"质量管理/Sub\"]")
                .sourceType(DccControlledFileNasTransferServiceImpl.SOURCE_TYPE_LOCAL_FOLDER)
                .status(DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING)
                .build());
        items.put(100L, DccControlledFileNasTransferTaskItemDO.builder()
                .id(100L)
                .taskId(10L)
                .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_DIRECTORY)
                .nasPath("质量管理/Sub")
                .itemName("Sub")
                .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING)
                .attemptCount(0)
                .build());
        items.put(101L, DccControlledFileNasTransferTaskItemDO.builder()
                .id(101L)
                .taskId(10L)
                .parentItemId(100L)
                .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_FILE)
                .nasPath("质量管理/Sub/Spec.pdf")
                .itemName("Spec.pdf")
                .sourceFileId(7001L)
                .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING)
                .attemptCount(0)
                .build());

        when(taskMapper.selectWaitingTasks(any(LocalDateTime.class))).thenAnswer(invocation -> tasks.values().stream()
                .filter(task -> DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING.equals(task.getStatus()))
                .map(DccControlledFileNasTransferServiceTest::copyTask)
                .toList());
        when(taskMapper.claimWaitingTask(eq(10L), any(LocalDateTime.class))).thenAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO task = tasks.get(10L);
            if (task == null || !DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING.equals(task.getStatus())) {
                return 0;
            }
            task.setStatus(DccControlledFileNasTransferServiceImpl.TASK_STATUS_RUNNING);
            task.setLastRunAt(invocation.getArgument(1));
            return 1;
        });
        when(taskMapper.selectById(10L)).thenAnswer(invocation -> copyTask(tasks.get(10L)));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO task = invocation.getArgument(0);
            tasks.put(task.getId(), copyTask(task));
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileNasTransferTaskDO.class));
        when(taskItemMapper.selectListByTaskId(10L)).thenAnswer(invocation -> items.values().stream()
                .sorted(Comparator.comparingLong(DccControlledFileNasTransferTaskItemDO::getId))
                .map(DccControlledFileNasTransferServiceTest::copyItem)
                .toList());
        stubAggregatedTaskItemSummary(() -> items.values().stream().toList());
        when(taskItemMapper.selectFirstWaitingItemByTaskId(10L)).thenAnswer(invocation -> items.values().stream()
                .filter(item -> DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING.equals(item.getStatus()))
                .sorted(Comparator.comparingLong(DccControlledFileNasTransferTaskItemDO::getId))
                .map(DccControlledFileNasTransferServiceTest::copyItem)
                .findFirst()
                .orElse(null));
        when(taskItemMapper.claimWaitingItem(anyLong())).thenAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = items.get(invocation.getArgument(0));
            if (item == null || !DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING.equals(item.getStatus())) {
                return 0;
            }
            item.setStatus(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_RUNNING);
            return 1;
        });
        when(taskItemMapper.selectById(anyLong())).thenAnswer(invocation ->
                copyItem(items.get(invocation.getArgument(0))));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            items.put(item.getId(), copyItem(item));
            return 1;
        }).when(taskItemMapper).updateById(any(DccControlledFileNasTransferTaskItemDO.class));

        when(directoryMapper.selectList()).thenAnswer(invocation -> directories.stream()
                .map(DccControlledFileNasTransferServiceTest::copyDirectory)
                .toList());
        lenient().doAnswer(invocation -> {
            DccFileDirectoryDO directory = invocation.getArgument(0);
            directory.setId(200L);
            directories.add(copyDirectory(directory));
            return 1;
        }).when(directoryMapper).insert(any(DccFileDirectoryDO.class));
        when(directoryAccessRuleMapper.selectList()).thenReturn(List.of(
                DccDirectoryAccessRuleDO.builder()
                        .id(1L)
                        .directoryId(1L)
                        .subjectType("ROLE")
                        .subjectId(100L)
                        .canQuery(Boolean.TRUE)
                        .canPreview(Boolean.TRUE)
                        .canDownload(Boolean.FALSE)
                        .active(Boolean.TRUE)
                        .changeReason("seed")
                        .build()
        ));
        when(categoryMapper.selectList()).thenReturn(List.of(templateCategory()));
        when(categoryDirectoryBindingMapper.selectList()).thenReturn(List.of(binding(900250L, 1L)));
        when(permissionRuleMapper.selectList()).thenReturn(List.of());
        when(distributionRuleMapper.selectList()).thenReturn(List.of());
        when(trainingRuleMapper.selectList()).thenReturn(List.of());
        when(routeMapper.selectList()).thenReturn(List.of());
        when(routeNodeMapper.selectList()).thenReturn(List.of());
        when(workflowService.submitControlledFileWithoutApproval(eq(99L), any(DccControlledFileSubmitReqVO.class)))
                .thenReturn(6001L);

        transferService.processWaitingTasks();

        DccFileDirectoryDO createdDirectory = directories.stream()
                .filter(directory -> Long.valueOf(200L).equals(directory.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(Boolean.FALSE, createdDirectory.getAccessRuleManuallyBound());
    }

    @Test
    void transfer_rejectsSelectedCategoryWithoutDirectoryBinding() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        when(categoryMapper.selectById(900250L)).thenReturn(otherCategory());
        when(categoryDirectoryBindingMapper.selectActiveByCategoryId(900250L)).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> transferService.transfer(99L, buildReq()));

        assertEquals("当前 DCC 模板类别未绑定受控目录，请先在 DCC 文件类别维护目录绑定", exception.getMessage());
        verify(taskMapper, never()).insert(any(DccControlledFileNasTransferTaskDO.class));
        verify(taskItemMapper, never()).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        verify(nasBrowserService, never()).listFiles(any());
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
    }

    @Test
    void nasTransferTaskStateShouldUseAggregatedSummaryWithoutLoadingAllItems() {
        DccControlledFileNasTransferTaskDO task = DccControlledFileNasTransferTaskDO.builder()
                .id(43L)
                .operatorUserId(99L)
                .templateCategoryId(900250L)
                .dccProjectCodeId(3000L)
                .productMasterId(null)
                .effectiveDate(LocalDate.now())
                .selectedNasPathsJson("[\"9. 其他\"]")
                .status(DccControlledFileNasTransferServiceImpl.TASK_STATUS_RUNNING)
                .build();
        when(taskMapper.selectById(43L)).thenReturn(task);
        when(taskItemMapper.selectFailedItemsByTaskId(43L)).thenReturn(List.of());

        DccControlledFileNasTransferRespVO response = transferService.getTask(99L, 43L);

        assertEquals(43L, response.getTaskId());
        assertEquals(DccControlledFileNasTransferServiceImpl.TASK_STATUS_RUNNING, response.getStatus());
        verify(taskItemMapper, never()).selectListByTaskId(43L);
    }

    @Test
    void processWaitingTasks_failsTaskWhenSelectedCategoryBindingMissing() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        DccControlledFileNasTransferTaskDO task = DccControlledFileNasTransferTaskDO.builder()
                .id(10L)
                .operatorUserId(99L)
                .templateCategoryId(900250L)
                .dccProjectCodeId(3000L)
                .productMasterId(null)
                .effectiveDate(LocalDate.of(2026, 5, 23))
                .selectedNasPathsJson("[\"1. QMS documents\"]")
                .status(DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING)
                .build();
        when(taskMapper.selectWaitingTasks(any(LocalDateTime.class))).thenReturn(List.of(task));
        when(taskMapper.claimWaitingTask(eq(10L), any(LocalDateTime.class))).thenReturn(1);
        when(taskMapper.selectById(10L)).thenReturn(task);
        when(directoryMapper.selectList()).thenReturn(List.of(directory(902634L, null, "1. QMS documents", 1)));
        when(directoryAccessRuleMapper.selectList()).thenReturn(List.of());
        when(categoryMapper.selectList()).thenReturn(List.of(otherCategory()));
        when(categoryMapper.selectById(900250L)).thenReturn(otherCategory());
        when(categoryDirectoryBindingMapper.selectList()).thenReturn(List.of());
        when(permissionRuleMapper.selectList()).thenReturn(List.of());
        when(distributionRuleMapper.selectList()).thenReturn(List.of());
        when(trainingRuleMapper.selectList()).thenReturn(List.of());
        when(routeMapper.selectList()).thenReturn(List.of());
        when(routeNodeMapper.selectList()).thenReturn(List.of());

        transferService.processWaitingTasks();

        ArgumentCaptor<DccControlledFileNasTransferTaskDO> taskCaptor =
                ArgumentCaptor.forClass(DccControlledFileNasTransferTaskDO.class);
        verify(taskMapper).updateById(taskCaptor.capture());
        DccControlledFileNasTransferTaskDO failedTask = taskCaptor.getValue();
        assertEquals(DccControlledFileNasTransferServiceImpl.TASK_STATUS_FAILED, failedTask.getStatus());
        assertEquals("当前 DCC 模板类别未绑定受控目录，请先在 DCC 文件类别维护目录绑定",
                failedTask.getLastFailureMessage());
        verify(taskItemMapper, never()).selectFirstWaitingItemByTaskId(10L);
        verify(nasBrowserService, never()).listFiles(any());
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
    }

    @Test
    void processWaitingTasks_expandsDirectoriesAndImportsFiles() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        when(categoryMapper.selectById(900250L)).thenReturn(templateCategory());

        Map<Long, DccControlledFileNasTransferTaskDO> tasks = new LinkedHashMap<>();
        Map<Long, DccControlledFileNasTransferTaskItemDO> items = new LinkedHashMap<>();
        AtomicLong nextItemId = new AtomicLong(102L);
        AtomicLong nextDirectoryId = new AtomicLong(200L);
        List<DccFileDirectoryDO> directories = new ArrayList<>(List.of(
                directory(1L, null, "3.DMR", 1)
        ));
        List<DccFileCategoryDO> categories = new ArrayList<>(List.of(templateCategory()));
        List<DccCategoryDirectoryBindingDO> bindings = new ArrayList<>(List.of(
                binding(900250L, 1L)
        ));
        tasks.put(10L, DccControlledFileNasTransferTaskDO.builder()
                .id(10L)
                .operatorUserId(99L)
                .templateCategoryId(900250L)
                .dccProjectCodeId(3000L)
                .productMasterId(null)
                .effectiveDate(LocalDate.of(2026, 5, 23))
                .selectedNasPathsJson("[\"3.DMR/01.图纸\"]")
                .status(DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING)
                .build());
        items.put(100L, DccControlledFileNasTransferTaskItemDO.builder()
                .id(100L)
                .taskId(10L)
                .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_DIRECTORY)
                .nasPath("3.DMR")
                .itemName("3.DMR")
                .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING)
                .attemptCount(0)
                .build());
        items.put(101L, DccControlledFileNasTransferTaskItemDO.builder()
                .id(101L)
                .taskId(10L)
                .parentItemId(100L)
                .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_DIRECTORY)
                .nasPath("3.DMR/01.图纸")
                .itemName("01.图纸")
                .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING)
                .attemptCount(0)
                .build());

        when(taskMapper.selectById(anyLong())).thenAnswer(invocation ->
                copyTask(tasks.get(invocation.getArgument(0))));
        when(taskMapper.selectWaitingTasks(any(LocalDateTime.class))).thenAnswer(invocation -> {
            LocalDateTime now = invocation.getArgument(0);
            return tasks.values().stream()
                    .filter(task -> DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING.equals(task.getStatus()))
                    .filter(task -> task.getNextCheckAt() == null || !task.getNextCheckAt().isAfter(now))
                    .sorted(Comparator.comparingLong(DccControlledFileNasTransferTaskDO::getId))
                    .map(DccControlledFileNasTransferServiceTest::copyTask)
                    .toList();
        });
        when(taskMapper.claimWaitingTask(anyLong(), any(LocalDateTime.class))).thenAnswer(invocation -> {
            Long taskId = invocation.getArgument(0);
            LocalDateTime lastRunAt = invocation.getArgument(1);
            DccControlledFileNasTransferTaskDO task = tasks.get(taskId);
            if (task == null || !DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING.equals(task.getStatus())) {
                return 0;
            }
            task.setStatus(DccControlledFileNasTransferServiceImpl.TASK_STATUS_RUNNING);
            task.setLastRunAt(lastRunAt);
            task.setNextCheckAt(null);
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO task = invocation.getArgument(0);
            tasks.put(task.getId(), copyTask(task));
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileNasTransferTaskDO.class));

        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            item.setId(nextItemId.getAndIncrement());
            items.put(item.getId(), copyItem(item));
            return 1;
        }).when(taskItemMapper).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        when(taskItemMapper.selectById(anyLong())).thenAnswer(invocation ->
                copyItem(items.get(invocation.getArgument(0))));
        when(taskItemMapper.selectListByTaskId(anyLong())).thenAnswer(invocation -> items.values().stream()
                .filter(item -> item.getTaskId().equals(invocation.getArgument(0)))
                .sorted(Comparator.comparingLong(DccControlledFileNasTransferTaskItemDO::getId))
                .map(DccControlledFileNasTransferServiceTest::copyItem)
                .toList());
        stubAggregatedTaskItemSummary(() -> items.values().stream().toList());
        when(taskItemMapper.selectFirstWaitingItemByTaskId(anyLong())).thenAnswer(invocation -> items.values().stream()
                .filter(item -> item.getTaskId().equals(invocation.getArgument(0)))
                .filter(item -> DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING.equals(item.getStatus()))
                .sorted(Comparator.comparingLong(DccControlledFileNasTransferTaskItemDO::getId))
                .map(DccControlledFileNasTransferServiceTest::copyItem)
                .findFirst()
                .orElse(null));
        when(taskItemMapper.claimWaitingItem(anyLong())).thenAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = items.get(invocation.getArgument(0));
            if (item == null || !DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING.equals(item.getStatus())) {
                return 0;
            }
            item.setStatus(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_RUNNING);
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            items.put(item.getId(), copyItem(item));
            return 1;
        }).when(taskItemMapper).updateById(any(DccControlledFileNasTransferTaskItemDO.class));

        when(directoryMapper.selectList()).thenAnswer(invocation -> directories.stream()
                .map(DccControlledFileNasTransferServiceTest::copyDirectory)
                .toList());
        doAnswer(invocation -> {
            DccFileDirectoryDO directory = invocation.getArgument(0);
            directory.setId(nextDirectoryId.getAndIncrement());
            directories.add(copyDirectory(directory));
            return 1;
        }).when(directoryMapper).insert(any(DccFileDirectoryDO.class));
        when(directoryAccessRuleMapper.selectList()).thenReturn(List.of());
        when(categoryMapper.selectList()).thenAnswer(invocation -> categories.stream()
                .map(DccControlledFileNasTransferServiceTest::copyCategory)
                .toList());
        when(categoryDirectoryBindingMapper.selectList()).thenAnswer(invocation -> bindings.stream()
                .map(DccControlledFileNasTransferServiceTest::copyBinding)
                .toList());
        when(permissionRuleMapper.selectList()).thenReturn(List.of());
        when(distributionRuleMapper.selectList()).thenReturn(List.of());
        when(trainingRuleMapper.selectList()).thenReturn(List.of());
        when(routeMapper.selectList()).thenReturn(List.of());
        when(routeNodeMapper.selectList()).thenReturn(List.of());

        when(nasBrowserService.readDirectoryAcl("3.DMR")).thenReturn(sampleAcl("3.DMR"));
        when(nasBrowserService.readDirectoryAcl("3.DMR/01.图纸")).thenReturn(sampleAcl("3.DMR/01.图纸"));
        when(nasBrowserService.listFiles("3.DMR")).thenReturn(new FileNasListRespVO().setItems(List.of(
                new FileNasListRespVO.Item().setName("01.图纸").setPath("3.DMR/01.图纸").setDir(true).setSize(0L)
        )));
        when(nasBrowserService.listFiles("3.DMR/01.图纸")).thenReturn(new FileNasListRespVO().setItems(List.of(
                new FileNasListRespVO.Item().setName("Spec.docx").setPath("3.DMR/01.图纸/Spec.docx").setDir(false).setSize(10L)
        )));
        when(nasBrowserService.readFile("3.DMR/01.图纸/Spec.docx"))
                .thenReturn(new NasFileReadResult("Spec.docx", "3.DMR/01.图纸/Spec.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx".getBytes()));
        when(fileService.createFileAndReturnId(eq("docx".getBytes()), eq("Spec.docx"), eq("dcc/original"),
                eq("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))).thenReturn(5001L);
        when(workflowService.submitControlledFileWithoutApproval(eq(99L), any(DccControlledFileSubmitReqVO.class)))
                .thenReturn(6001L);

        transferService.processWaitingTasks();
        DccControlledFileNasTransferRespVO result = transferService.getTask(99L, 10L);

        assertEquals(DccControlledFileNasTransferServiceImpl.TASK_STATUS_COMPLETED, result.getStatus());
        assertEquals(1, result.getCreatedDirectoryCount());
        assertEquals(1, result.getReusedDirectoryCount());
        assertEquals(0, result.getCreatedCategoryCount());
        assertEquals(1, result.getCreatedFileCount());
        assertEquals(0, result.getFailedFileCount());
        assertEquals(0, result.getRemainingPendingCount());
        assertTrue(result.getFailures().isEmpty());

        ArgumentCaptor<DccControlledFileSubmitReqVO> submitCaptor = ArgumentCaptor.forClass(DccControlledFileSubmitReqVO.class);
        verify(workflowService).submitControlledFileWithoutApproval(eq(99L), submitCaptor.capture());
        assertEquals(900250L, submitCaptor.getValue().getCategoryId());
        assertEquals(3000L, submitCaptor.getValue().getDccProjectCodeId());
        assertEquals(null, submitCaptor.getValue().getProductMasterId());
        assertEquals("Spec", submitCaptor.getValue().getFileNumber());
        assertEquals("V1.0", submitCaptor.getValue().getVersionNo());
        verify(categoryMapper, never()).insert(any(DccFileCategoryDO.class));
        verify(categoryDirectoryBindingMapper, never()).insert(any(DccCategoryDirectoryBindingDO.class));
    }

    @Test
    void processWaitingTasks_usesSelectedCategoryInsteadOfCreatingDirectoryCategory() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        Map<Long, DccControlledFileNasTransferTaskDO> tasks = new LinkedHashMap<>();
        Map<Long, DccControlledFileNasTransferTaskItemDO> items = new LinkedHashMap<>();
        configureSingleDirectoryTask(tasks, items, "1. QMS documents");
        when(categoryMapper.selectById(900250L)).thenReturn(otherCategory());
        AtomicLong nextItemId = new AtomicLong(101L);
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            item.setId(nextItemId.getAndIncrement());
            items.put(item.getId(), copyItem(item));
            return 1;
        }).when(taskItemMapper).insert(any(DccControlledFileNasTransferTaskItemDO.class));

        when(taskItemMapper.claimWaitingItem(anyLong())).thenAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = items.get(invocation.getArgument(0));
            if (item == null || !DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING.equals(item.getStatus())) {
                return 0;
            }
            item.setStatus(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_RUNNING);
            return 1;
        });
        when(taskItemMapper.selectById(anyLong())).thenAnswer(invocation -> copyItem(items.get(invocation.getArgument(0))));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO updatedItem = invocation.getArgument(0);
            items.put(updatedItem.getId(), copyItem(updatedItem));
            return 1;
        }).when(taskItemMapper).updateById(any(DccControlledFileNasTransferTaskItemDO.class));

        when(directoryMapper.selectList()).thenReturn(List.of(directory(902634L, null, "1. QMS documents", 1)));
        when(nasBrowserService.readDirectoryAcl("1. QMS documents")).thenReturn(sampleAcl("1. QMS documents"));
        when(nasBrowserService.listFiles("1. QMS documents")).thenReturn(new FileNasListRespVO().setItems(List.of(
                new FileNasListRespVO.Item().setName("Quality Manual.pdf")
                        .setPath("1. QMS documents/Quality Manual.pdf").setDir(false).setSize(10L)
        )));
        when(nasBrowserService.readFile("1. QMS documents/Quality Manual.pdf"))
                .thenReturn(new NasFileReadResult("Quality Manual.pdf", "1. QMS documents/Quality Manual.pdf",
                        "application/pdf", "pdf".getBytes()));
        when(fileService.createFileAndReturnId(eq("pdf".getBytes()), eq("Quality Manual.pdf"), eq("dcc/original"),
                eq("application/pdf"))).thenReturn(5001L);
        when(workflowService.submitControlledFileWithoutApproval(eq(99L), any(DccControlledFileSubmitReqVO.class)))
                .thenReturn(6001L);

        transferService.processWaitingTasks();
        DccControlledFileNasTransferRespVO result = transferService.getTask(99L, 10L);

        assertEquals(DccControlledFileNasTransferServiceImpl.TASK_STATUS_COMPLETED, result.getStatus());
        assertEquals(0, result.getCreatedCategoryCount());
        ArgumentCaptor<DccControlledFileSubmitReqVO> submitCaptor =
                ArgumentCaptor.forClass(DccControlledFileSubmitReqVO.class);
        verify(workflowService).submitControlledFileWithoutApproval(eq(99L), submitCaptor.capture());
        assertEquals(900250L, submitCaptor.getValue().getCategoryId());
        verify(categoryMapper, never()).insert(any(DccFileCategoryDO.class));
    }

    @Test
    void processWaitingTasks_ignoresStaleNasCategoryCodeAndUsesSelectedCategory() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        Map<Long, DccControlledFileNasTransferTaskDO> tasks = new LinkedHashMap<>();
        Map<Long, DccControlledFileNasTransferTaskItemDO> items = new LinkedHashMap<>();
        configureSingleDirectoryTask(tasks, items, "1. QMS documents");
        AtomicLong nextItemId = new AtomicLong(101L);
        DccFileCategoryDO staleNasCategory = DccFileCategoryDO.builder()
                .id(310L)
                .code("NASCAT-STALE")
                .name("1. QMS documents")
                .active(Boolean.TRUE)
                .sort(2)
                .build();
        List<DccFileDirectoryDO> directories = new ArrayList<>(List.of(
                directory(902634L, null, "1. QMS documents", 1)
        ));
        List<DccFileCategoryDO> categories = new ArrayList<>(List.of(templateCategory(), staleNasCategory));
        List<DccCategoryDirectoryBindingDO> bindings = new ArrayList<>(List.of(
                binding(900250L, 902634L)
        ));

        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            item.setId(nextItemId.getAndIncrement());
            items.put(item.getId(), copyItem(item));
            return 1;
        }).when(taskItemMapper).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        when(taskItemMapper.claimWaitingItem(anyLong())).thenAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = items.get(invocation.getArgument(0));
            if (item == null || !DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING.equals(item.getStatus())) {
                return 0;
            }
            item.setStatus(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_RUNNING);
            return 1;
        });
        when(taskItemMapper.selectById(anyLong())).thenAnswer(invocation -> copyItem(items.get(invocation.getArgument(0))));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO updatedItem = invocation.getArgument(0);
            items.put(updatedItem.getId(), copyItem(updatedItem));
            return 1;
        }).when(taskItemMapper).updateById(any(DccControlledFileNasTransferTaskItemDO.class));

        when(directoryMapper.selectList()).thenAnswer(invocation -> directories.stream()
                .map(DccControlledFileNasTransferServiceTest::copyDirectory)
                .toList());
        when(categoryMapper.selectList()).thenAnswer(invocation -> categories.stream()
                .map(DccControlledFileNasTransferServiceTest::copyCategory)
                .toList());
        when(categoryDirectoryBindingMapper.selectList()).thenAnswer(invocation -> bindings.stream()
                .map(DccControlledFileNasTransferServiceTest::copyBinding)
                .toList());

        when(nasBrowserService.readDirectoryAcl("1. QMS documents")).thenReturn(sampleAcl("1. QMS documents"));
        when(nasBrowserService.listFiles("1. QMS documents")).thenReturn(new FileNasListRespVO().setItems(List.of(
                new FileNasListRespVO.Item().setName("Quality Manual.pdf")
                        .setPath("1. QMS documents/Quality Manual.pdf").setDir(false).setSize(10L)
        )));
        when(nasBrowserService.readFile("1. QMS documents/Quality Manual.pdf"))
                .thenReturn(new NasFileReadResult("Quality Manual.pdf", "1. QMS documents/Quality Manual.pdf",
                        "application/pdf", "pdf".getBytes()));
        when(fileService.createFileAndReturnId(eq("pdf".getBytes()), eq("Quality Manual.pdf"), eq("dcc/original"),
                eq("application/pdf"))).thenReturn(5001L);
        when(workflowService.submitControlledFileWithoutApproval(eq(99L), any(DccControlledFileSubmitReqVO.class)))
                .thenReturn(6001L);

        transferService.processWaitingTasks();
        DccControlledFileNasTransferRespVO result = transferService.getTask(99L, 10L);

        assertEquals(DccControlledFileNasTransferServiceImpl.TASK_STATUS_COMPLETED, result.getStatus());
        assertEquals(0, result.getCreatedDirectoryCount());
        assertEquals(1, result.getReusedDirectoryCount());
        assertEquals(0, result.getCreatedCategoryCount());
        assertEquals(1, result.getCreatedFileCount());
        assertEquals(0, result.getFailedFileCount());
        assertEquals(1, bindings.size());
        ArgumentCaptor<DccControlledFileSubmitReqVO> submitCaptor =
                ArgumentCaptor.forClass(DccControlledFileSubmitReqVO.class);
        verify(workflowService).submitControlledFileWithoutApproval(eq(99L), submitCaptor.capture());
        assertEquals(900250L, submitCaptor.getValue().getCategoryId());
        verify(categoryMapper, never()).insert(any(DccFileCategoryDO.class));
        verify(categoryDirectoryBindingMapper, never()).insert(any(DccCategoryDirectoryBindingDO.class));
    }

    @Test
    void processWaitingTasks_commitsSelectedCategoryAssignmentBeforeWorkflowSubmit() {
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        ReflectionTestUtils.setField(transferService, "transactionManager", transactionManager);
        TenantContextHolder.setTenantId(1L);
        when(categoryMapper.selectById(900250L)).thenReturn(templateCategory());

        Map<Long, DccControlledFileNasTransferTaskDO> tasks = new LinkedHashMap<>();
        Map<Long, DccControlledFileNasTransferTaskItemDO> items = new LinkedHashMap<>();
        tasks.put(10L, DccControlledFileNasTransferTaskDO.builder()
                .id(10L)
                .operatorUserId(99L)
                .templateCategoryId(900250L)
                .dccProjectCodeId(3000L)
                .productMasterId(null)
                .effectiveDate(LocalDate.of(2026, 5, 23))
                .selectedNasPathsJson("[\"1. QMS documents\"]")
                .status(DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING)
                .build());
        items.put(100L, DccControlledFileNasTransferTaskItemDO.builder()
                .id(100L)
                .taskId(10L)
                .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_DIRECTORY)
                .nasPath("1. QMS documents")
                .itemName("1. QMS documents")
                .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_COMPLETED)
                .resolvedDirectoryId(200L)
                .directoryOutcome(DccControlledFileNasTransferServiceImpl.OUTCOME_REUSED)
                .attemptCount(1)
                .build());
        items.put(101L, DccControlledFileNasTransferTaskItemDO.builder()
                .id(101L)
                .taskId(10L)
                .parentItemId(100L)
                .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_FILE)
                .nasPath("1. QMS documents/Quality Manual.pdf")
                .itemName("Quality Manual.pdf")
                .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING)
                .attemptCount(0)
                .build());
        List<DccFileDirectoryDO> directories = new ArrayList<>(List.of(
                directory(200L, null, "1. QMS documents", 1)
        ));
        List<DccFileCategoryDO> categories = new ArrayList<>(List.of(templateCategory()));
        List<DccCategoryDirectoryBindingDO> bindings = new ArrayList<>(List.of(
                binding(900250L, 200L)
        ));

        when(taskMapper.selectWaitingTasks(any(LocalDateTime.class))).thenAnswer(invocation -> tasks.values().stream()
                .filter(task -> DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING.equals(task.getStatus()))
                .map(DccControlledFileNasTransferServiceTest::copyTask)
                .toList());
        when(taskMapper.claimWaitingTask(eq(10L), any(LocalDateTime.class))).thenAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO task = tasks.get(10L);
            task.setStatus(DccControlledFileNasTransferServiceImpl.TASK_STATUS_RUNNING);
            task.setLastRunAt(invocation.getArgument(1));
            return 1;
        });
        when(taskMapper.selectById(anyLong())).thenAnswer(invocation -> copyTask(tasks.get(invocation.getArgument(0))));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO task = invocation.getArgument(0);
            tasks.put(task.getId(), copyTask(task));
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileNasTransferTaskDO.class));
        when(taskItemMapper.selectListByTaskId(10L)).thenAnswer(invocation -> items.values().stream()
                .sorted(Comparator.comparingLong(DccControlledFileNasTransferTaskItemDO::getId))
                .map(DccControlledFileNasTransferServiceTest::copyItem)
                .toList());
        stubAggregatedTaskItemSummary(() -> items.values().stream().toList());
        when(taskItemMapper.selectFirstWaitingItemByTaskId(10L)).thenAnswer(invocation -> items.values().stream()
                .filter(item -> DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING.equals(item.getStatus()))
                .sorted(Comparator.comparingLong(DccControlledFileNasTransferTaskItemDO::getId))
                .map(DccControlledFileNasTransferServiceTest::copyItem)
                .findFirst()
                .orElse(null));
        when(taskItemMapper.claimWaitingItem(101L)).thenAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO fileItem = items.get(101L);
            fileItem.setStatus(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_RUNNING);
            return 1;
        });
        when(taskItemMapper.selectById(anyLong())).thenAnswer(invocation -> copyItem(items.get(invocation.getArgument(0))));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            items.put(item.getId(), copyItem(item));
            return 1;
        }).when(taskItemMapper).updateById(any(DccControlledFileNasTransferTaskItemDO.class));

        when(directoryMapper.selectList()).thenAnswer(invocation -> directories.stream()
                .map(DccControlledFileNasTransferServiceTest::copyDirectory)
                .toList());
        when(directoryAccessRuleMapper.selectList()).thenReturn(List.of());
        when(categoryMapper.selectList()).thenAnswer(invocation -> categories.stream()
                .map(DccControlledFileNasTransferServiceTest::copyCategory)
                .toList());
        when(categoryDirectoryBindingMapper.selectList()).thenAnswer(invocation -> bindings.stream()
                .map(DccControlledFileNasTransferServiceTest::copyBinding)
                .toList());
        when(permissionRuleMapper.selectList()).thenReturn(List.of());
        when(distributionRuleMapper.selectList()).thenReturn(List.of());
        when(trainingRuleMapper.selectList()).thenReturn(List.of());
        when(routeMapper.selectList()).thenReturn(List.of());
        when(routeNodeMapper.selectList()).thenReturn(List.of());
        when(nasBrowserService.readFile("1. QMS documents/Quality Manual.pdf"))
                .thenReturn(new NasFileReadResult("Quality Manual.pdf",
                        "1. QMS documents/Quality Manual.pdf", "application/pdf", "pdf".getBytes()));
        when(fileService.createFileAndReturnId(any(byte[].class), eq("Quality Manual.pdf"),
                eq("dcc/original"), eq("application/pdf"))).thenReturn(5001L);
        when(workflowService.submitControlledFileWithoutApproval(eq(99L), any(DccControlledFileSubmitReqVO.class)))
                .thenAnswer(invocation -> {
                    assertEquals(1, transactionManager.commits());
                    throw new IllegalStateException("duplicate master");
                });

        transferService.processWaitingTasks();

        assertEquals(1, bindings.size());
        assertEquals(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_FAILED, items.get(101L).getStatus());
        verify(workflowService).submitControlledFileWithoutApproval(eq(99L), any(DccControlledFileSubmitReqVO.class));
        verify(categoryMapper, never()).insert(any(DccFileCategoryDO.class));
        verify(categoryDirectoryBindingMapper, never()).insert(any(DccCategoryDirectoryBindingDO.class));
    }

    @Test
    void processWaitingTasks_failsFileBeforeReadWhenSelectedCategoryBindingDoesNotCoverDirectory() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        Map<Long, DccControlledFileNasTransferTaskDO> tasks = new LinkedHashMap<>();
        Map<Long, DccControlledFileNasTransferTaskItemDO> items = new LinkedHashMap<>();
        configureSingleDirectoryTask(tasks, items, "1. QMS documents");
        AtomicLong nextItemId = new AtomicLong(101L);
        List<DccCategoryDirectoryBindingDO> bindings = new ArrayList<>(List.of(
                binding(900250L, 300L)
        ));

        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            item.setId(nextItemId.getAndIncrement());
            items.put(item.getId(), copyItem(item));
            return 1;
        }).when(taskItemMapper).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        when(taskItemMapper.claimWaitingItem(anyLong())).thenAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = items.get(invocation.getArgument(0));
            if (item == null || !DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING.equals(item.getStatus())) {
                return 0;
            }
            item.setStatus(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_RUNNING);
            return 1;
        });
        when(taskItemMapper.selectById(anyLong())).thenAnswer(invocation -> copyItem(items.get(invocation.getArgument(0))));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO updatedItem = invocation.getArgument(0);
            items.put(updatedItem.getId(), copyItem(updatedItem));
            return 1;
        }).when(taskItemMapper).updateById(any(DccControlledFileNasTransferTaskItemDO.class));

        when(directoryMapper.selectList()).thenReturn(List.of(
                directory(902634L, null, "1. QMS documents", 1),
                directory(300L, null, "Other root", 2)
        ));
        when(categoryDirectoryBindingMapper.selectList()).thenAnswer(invocation -> bindings.stream()
                .map(DccControlledFileNasTransferServiceTest::copyBinding)
                .toList());
        when(nasBrowserService.readDirectoryAcl("1. QMS documents")).thenReturn(sampleAcl("1. QMS documents"));
        when(nasBrowserService.listFiles("1. QMS documents")).thenReturn(new FileNasListRespVO().setItems(List.of(
                new FileNasListRespVO.Item().setName("Quality Manual.pdf")
                        .setPath("1. QMS documents/Quality Manual.pdf").setDir(false).setSize(10L)
        )));

        transferService.processWaitingTasks();

        assertEquals(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_FAILED, items.get(101L).getStatus());
        assertEquals("category", items.get(101L).getFailureStage());
        assertTrue(items.get(101L).getLastError().contains("does not cover target directory"));
        verify(nasBrowserService, never()).readFile(anyString());
        verify(fileService, never()).createFileAndReturnId(any(byte[].class), anyString(), anyString(), anyString());
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
    }

    @Test
    void processWaitingTasks_fitsLongNasFileNameIntoFileNumberLimit() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        Map<Long, DccControlledFileNasTransferTaskDO> tasks = new LinkedHashMap<>();
        Map<Long, DccControlledFileNasTransferTaskItemDO> items = new LinkedHashMap<>();
        configureSingleDirectoryTask(tasks, items, "3.DMR");
        AtomicLong nextItemId = new AtomicLong(101L);
        String longFileName = "指引导丝采购物资清单Finethrough Guidewire Materials Procurement ListP-CEMGWA0.pdf";
        String longNasPath = "3.DMR/04.物资采购清单/01 导丝类/" + longFileName;

        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            item.setId(nextItemId.getAndIncrement());
            items.put(item.getId(), copyItem(item));
            return 1;
        }).when(taskItemMapper).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        when(taskItemMapper.claimWaitingItem(anyLong())).thenAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = items.get(invocation.getArgument(0));
            if (item == null || !DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING.equals(item.getStatus())) {
                return 0;
            }
            item.setStatus(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_RUNNING);
            return 1;
        });
        when(taskItemMapper.selectById(anyLong())).thenAnswer(invocation -> copyItem(items.get(invocation.getArgument(0))));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO updatedItem = invocation.getArgument(0);
            items.put(updatedItem.getId(), copyItem(updatedItem));
            return 1;
        }).when(taskItemMapper).updateById(any(DccControlledFileNasTransferTaskItemDO.class));
        when(directoryMapper.selectList()).thenReturn(List.of(directory(902634L, null, "3.DMR", 1)));
        when(nasBrowserService.readDirectoryAcl("3.DMR")).thenReturn(sampleAcl("3.DMR"));
        when(nasBrowserService.listFiles("3.DMR")).thenReturn(new FileNasListRespVO().setItems(List.of(
                new FileNasListRespVO.Item().setName(longFileName).setPath(longNasPath).setDir(false).setSize(10L)
        )));
        when(nasBrowserService.readFile(longNasPath))
                .thenReturn(new NasFileReadResult(longFileName, longNasPath, "application/pdf", "pdf".getBytes()));
        when(fileService.createFileAndReturnId(eq("pdf".getBytes()), eq(longFileName), eq("dcc/original"),
                eq("application/pdf"))).thenReturn(5002L);
        when(workflowService.submitControlledFileWithoutApproval(eq(99L), any(DccControlledFileSubmitReqVO.class)))
                .thenReturn(6002L);

        transferService.processWaitingTasks();

        ArgumentCaptor<DccControlledFileSubmitReqVO> submitCaptor =
                ArgumentCaptor.forClass(DccControlledFileSubmitReqVO.class);
        verify(workflowService).submitControlledFileWithoutApproval(eq(99L), submitCaptor.capture());
        DccControlledFileSubmitReqVO submitReqVO = submitCaptor.getValue();
        assertEquals(longFileName, submitReqVO.getFileName());
        assertTrue(submitReqVO.getFileNumber().length() <= 64);
        assertTrue(submitReqVO.getFileNumber().startsWith("指引导丝采购物资清单Finethrough"));
        ArgumentCaptor<DccControlledFileNasSourceDO> nasSourceCaptor =
                ArgumentCaptor.forClass(DccControlledFileNasSourceDO.class);
        verify(nasSourceMapper).insert(nasSourceCaptor.capture());
        DccControlledFileNasSourceDO nasSource = nasSourceCaptor.getValue();
        assertEquals(6002L, nasSource.getControlledFileId());
        assertEquals("quality", nasSource.getNasShareName());
        assertEquals(longNasPath, nasSource.getNormalizedRelativePath());
        assertEquals(DccNasControlAuditServiceImpl.SOURCE_TYPE_NAS_TRANSFER, nasSource.getSourceType());
        assertEquals(DccNasControlAuditServiceImpl.SOURCE_CONFIDENCE_EXACT, nasSource.getSourceConfidence());
    }

    @Test
    void processWaitingTasks_deduplicatesDuplicateNasChildrenBeforeInsert() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        Map<Long, DccControlledFileNasTransferTaskDO> tasks = new LinkedHashMap<>();
        Map<Long, DccControlledFileNasTransferTaskItemDO> items = new LinkedHashMap<>();
        configureSingleDirectoryTask(tasks, items, "3.DMR");
        AtomicLong nextItemId = new AtomicLong(101L);
        AtomicLong nextDirectoryId = new AtomicLong(902635L);
        List<String> insertedNasPaths = new ArrayList<>();

        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            if (insertedNasPaths.contains(item.getNasPath())) {
                throw new IllegalStateException("Duplicate entry for task item path: " + item.getNasPath());
            }
            insertedNasPaths.add(item.getNasPath());
            item.setId(nextItemId.getAndIncrement());
            items.put(item.getId(), copyItem(item));
            return 1;
        }).when(taskItemMapper).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        when(taskItemMapper.claimWaitingItem(anyLong())).thenAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = items.get(invocation.getArgument(0));
            if (item == null || !DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING.equals(item.getStatus())) {
                return 0;
            }
            item.setStatus(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_RUNNING);
            return 1;
        });
        when(taskItemMapper.selectById(anyLong())).thenAnswer(invocation -> copyItem(items.get(invocation.getArgument(0))));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO updatedItem = invocation.getArgument(0);
            items.put(updatedItem.getId(), copyItem(updatedItem));
            return 1;
        }).when(taskItemMapper).updateById(any(DccControlledFileNasTransferTaskItemDO.class));
        when(directoryMapper.selectList()).thenReturn(List.of(directory(902634L, null, "3.DMR", 1)));
        doAnswer(invocation -> {
            DccFileDirectoryDO directory = invocation.getArgument(0);
            directory.setId(nextDirectoryId.getAndIncrement());
            return 1;
        }).when(directoryMapper).insert(any(DccFileDirectoryDO.class));
        when(nasBrowserService.readDirectoryAcl("3.DMR")).thenReturn(sampleAcl("3.DMR"));
        when(nasBrowserService.readDirectoryAcl("3.DMR/A")).thenReturn(sampleAcl("3.DMR/A"));
        when(nasBrowserService.listFiles("3.DMR")).thenReturn(new FileNasListRespVO().setItems(List.of(
                new FileNasListRespVO.Item().setName("A").setPath("3.DMR/A").setDir(true).setSize(0L),
                new FileNasListRespVO.Item().setName("A").setPath("3.DMR/A").setDir(true).setSize(0L)
        )));
        when(nasBrowserService.listFiles("3.DMR/A")).thenReturn(new FileNasListRespVO().setItems(List.of()));

        transferService.processWaitingTasks();
        DccControlledFileNasTransferRespVO result = transferService.getTask(99L, 10L);

        assertEquals(DccControlledFileNasTransferServiceImpl.TASK_STATUS_COMPLETED, result.getStatus());
        assertEquals(1, insertedNasPaths.size());
        assertEquals(0, result.getFailedFileCount());
        assertTrue(result.getFailures().isEmpty());
    }

    @Test
    void processWaitingTasks_capturesNasAclSnapshotAfterDirectoryResolved() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        Map<Long, DccControlledFileNasTransferTaskDO> tasks = new LinkedHashMap<>();
        Map<Long, DccControlledFileNasTransferTaskItemDO> items = new LinkedHashMap<>();
        DccControlledFileNasTransferTaskItemDO directoryItem = configureSingleDirectoryTask(tasks, items, "3.DMR");
        NasAclReadResult acl = sampleAcl("3.DMR");

        when(directoryMapper.selectList()).thenReturn(List.of(directory(902634L, null, "3.DMR", 1)));
        when(nasBrowserService.readDirectoryAcl("3.DMR")).thenReturn(acl);
        when(nasBrowserService.listFiles("3.DMR")).thenReturn(new FileNasListRespVO().setItems(List.of()));

        transferService.processWaitingTasks();

        verify(snapshotCaptureService).captureDirectorySnapshot(10L, directoryItem.getId(), "3.DMR", 902634L, acl);
        verify(snapshotCaptureService).completeSnapshotForTask(10L);
        ArgumentCaptor<DccControlledFileNasTransferTaskItemDO> itemCaptor =
                ArgumentCaptor.forClass(DccControlledFileNasTransferTaskItemDO.class);
        verify(taskItemMapper).updateById(itemCaptor.capture());
        DccControlledFileNasTransferTaskItemDO completedItem = itemCaptor.getValue();
        assertEquals(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_COMPLETED, completedItem.getStatus());
        assertEquals(902634L, completedItem.getResolvedDirectoryId());
    }

    @Test
    void processWaitingTasks_failsDirectoryItemWhenNasAclReadFails() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        Map<Long, DccControlledFileNasTransferTaskDO> tasks = new LinkedHashMap<>();
        Map<Long, DccControlledFileNasTransferTaskItemDO> items = new LinkedHashMap<>();
        configureSingleDirectoryTask(tasks, items, "3.DMR");

        when(directoryMapper.selectList()).thenReturn(List.of(directory(902634L, null, "3.DMR", 1)));
        when(nasBrowserService.readDirectoryAcl("3.DMR"))
                .thenThrow(new IllegalStateException("NAS ACL read failed: STATUS_ACCESS_DENIED"));

        transferService.processWaitingTasks();

        verify(nasBrowserService, never()).listFiles("3.DMR");
        verify(snapshotCaptureService, never()).captureDirectorySnapshot(anyLong(), anyLong(), any(), anyLong(), any());
        verify(taskItemMapper, never()).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        ArgumentCaptor<DccControlledFileNasTransferTaskItemDO> itemCaptor =
                ArgumentCaptor.forClass(DccControlledFileNasTransferTaskItemDO.class);
        verify(taskItemMapper).updateById(itemCaptor.capture());
        DccControlledFileNasTransferTaskItemDO failedItem = itemCaptor.getValue();
        assertEquals(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_FAILED, failedItem.getStatus());
        assertEquals("acl", failedItem.getFailureStage());
        assertTrue(failedItem.getLastError().contains("STATUS_ACCESS_DENIED"));
    }

    @Test
    void processWaitingTasks_truncatesLongTaskFailureMessage() {
        TenantContextHolder.setTenantId(1L);
        DccControlledFileNasTransferTaskDO task = DccControlledFileNasTransferTaskDO.builder()
                .id(10L)
                .operatorUserId(99L)
                .templateCategoryId(900250L)
                .dccProjectCodeId(3000L)
                .productMasterId(null)
                .effectiveDate(LocalDate.of(2026, 5, 23))
                .selectedNasPathsJson("[\"3.DMR\"]")
                .status(DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING)
                .build();
        String longFailureMessage = "template failure " + "x".repeat(700);
        when(taskMapper.selectWaitingTasks(any(LocalDateTime.class))).thenReturn(List.of(task));
        when(taskMapper.claimWaitingTask(eq(10L), any(LocalDateTime.class))).thenReturn(1);
        when(taskMapper.selectById(10L)).thenReturn(task);
        when(directoryMapper.selectList()).thenReturn(List.of());
        when(directoryAccessRuleMapper.selectList()).thenReturn(List.of());
        when(categoryMapper.selectList()).thenReturn(List.of());
        when(categoryDirectoryBindingMapper.selectList()).thenReturn(List.of());
        when(permissionRuleMapper.selectList()).thenReturn(List.of());
        when(distributionRuleMapper.selectList()).thenReturn(List.of());
        when(trainingRuleMapper.selectList()).thenReturn(List.of());
        when(routeMapper.selectList()).thenReturn(List.of());
        when(routeNodeMapper.selectList()).thenReturn(List.of());
        when(categoryMapper.selectById(900250L)).thenThrow(new IllegalStateException(longFailureMessage));
        when(taskItemMapper.selectListByTaskId(10L)).thenReturn(List.of());

        transferService.processWaitingTasks();

        ArgumentCaptor<DccControlledFileNasTransferTaskDO> taskCaptor =
                ArgumentCaptor.forClass(DccControlledFileNasTransferTaskDO.class);
        verify(taskMapper).updateById(taskCaptor.capture());
        DccControlledFileNasTransferTaskDO failedTask = taskCaptor.getValue();
        assertEquals(DccControlledFileNasTransferServiceImpl.TASK_STATUS_FAILED, failedTask.getStatus());
        assertTrue(failedTask.getLastFailureMessage().length() <= 512);
        assertTrue(failedTask.getLastFailureMessage().endsWith("[truncated]"));
    }

    @Test
    void processWaitingTasks_truncatesLongItemFailureMessage() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        DccControlledFileNasTransferTaskDO task = DccControlledFileNasTransferTaskDO.builder()
                .id(10L)
                .operatorUserId(99L)
                .templateCategoryId(900250L)
                .dccProjectCodeId(3000L)
                .productMasterId(null)
                .effectiveDate(LocalDate.of(2026, 5, 23))
                .selectedNasPathsJson("[\"3.DMR\"]")
                .status(DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING)
                .build();
        DccControlledFileNasTransferTaskItemDO directoryItem = DccControlledFileNasTransferTaskItemDO.builder()
                .id(100L)
                .taskId(10L)
                .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_DIRECTORY)
                .nasPath("3.DMR")
                .itemName("3.DMR")
                .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING)
                .attemptCount(0)
                .build();
        String longFailureMessage = "nas list failure " + "x".repeat(700);
        when(taskMapper.selectWaitingTasks(any(LocalDateTime.class))).thenReturn(List.of(task));
        when(taskMapper.claimWaitingTask(eq(10L), any(LocalDateTime.class))).thenReturn(1);
        when(taskMapper.selectById(10L)).thenReturn(task);
        when(taskItemMapper.selectFirstWaitingItemByTaskId(10L))
                .thenReturn(directoryItem)
                .thenReturn(null);
        when(taskItemMapper.claimWaitingItem(100L)).thenReturn(1);
        when(taskItemMapper.selectById(100L)).thenReturn(directoryItem);
        when(taskItemMapper.selectListByTaskId(10L)).thenReturn(List.of(directoryItem));
        when(directoryMapper.selectList()).thenReturn(List.of(directory(902634L, null, "3.DMR", 1)));
        when(directoryAccessRuleMapper.selectList()).thenReturn(List.of());
        when(categoryMapper.selectList()).thenReturn(List.of(templateCategory()));
        when(categoryMapper.selectById(900250L)).thenReturn(templateCategory());
        when(categoryDirectoryBindingMapper.selectList()).thenReturn(List.of(binding(900250L, 902634L)));
        when(permissionRuleMapper.selectList()).thenReturn(List.of());
        when(distributionRuleMapper.selectList()).thenReturn(List.of());
        when(trainingRuleMapper.selectList()).thenReturn(List.of());
        when(routeMapper.selectList()).thenReturn(List.of());
        when(routeNodeMapper.selectList()).thenReturn(List.of());
        when(nasBrowserService.readDirectoryAcl("3.DMR")).thenReturn(sampleAcl("3.DMR"));
        when(nasBrowserService.listFiles("3.DMR")).thenThrow(new IllegalStateException(longFailureMessage));

        transferService.processWaitingTasks();

        ArgumentCaptor<DccControlledFileNasTransferTaskItemDO> itemCaptor =
                ArgumentCaptor.forClass(DccControlledFileNasTransferTaskItemDO.class);
        verify(taskItemMapper).updateById(itemCaptor.capture());
        DccControlledFileNasTransferTaskItemDO failedItem = itemCaptor.getValue();
        assertEquals(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_FAILED, failedItem.getStatus());
        assertTrue(failedItem.getLastError().length() <= 512);
        assertTrue(failedItem.getLastError().endsWith("[truncated]"));
    }

    private static DccControlledFileNasTransferReqVO buildReq() {
        DccControlledFileNasTransferReqVO reqVO = new DccControlledFileNasTransferReqVO();
        reqVO.setSelectedNasPaths(List.of("3.DMR/01.图纸"));
        reqVO.setTemplateCategoryId(900250L);
        reqVO.setDccProjectCodeId(3000L);
        reqVO.setProductMasterId(5000L);
        reqVO.setEffectiveDate(LocalDate.of(2026, 5, 23));
        return reqVO;
    }

    private DccControlledFileNasTransferTaskItemDO configureSingleDirectoryTask(
            Map<Long, DccControlledFileNasTransferTaskDO> tasks,
            Map<Long, DccControlledFileNasTransferTaskItemDO> items,
            String nasPath) {
        DccControlledFileNasTransferTaskDO task = DccControlledFileNasTransferTaskDO.builder()
                .id(10L)
                .operatorUserId(99L)
                .templateCategoryId(900250L)
                .dccProjectCodeId(3000L)
                .productMasterId(null)
                .effectiveDate(LocalDate.of(2026, 5, 23))
                .selectedNasPathsJson("[\"" + nasPath + "\"]")
                .status(DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING)
                .build();
        DccControlledFileNasTransferTaskItemDO directoryItem = DccControlledFileNasTransferTaskItemDO.builder()
                .id(100L)
                .taskId(10L)
                .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_DIRECTORY)
                .nasPath(nasPath)
                .itemName(nasPath)
                .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING)
                .attemptCount(0)
                .build();
        tasks.put(task.getId(), task);
        items.put(directoryItem.getId(), directoryItem);

        when(taskMapper.selectWaitingTasks(any(LocalDateTime.class))).thenAnswer(invocation -> tasks.values().stream()
                .filter(storedTask -> DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING.equals(storedTask.getStatus()))
                .map(DccControlledFileNasTransferServiceTest::copyTask)
                .toList());
        when(taskMapper.claimWaitingTask(eq(10L), any(LocalDateTime.class))).thenAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO storedTask = tasks.get(10L);
            if (storedTask == null || !DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING.equals(storedTask.getStatus())) {
                return 0;
            }
            storedTask.setStatus(DccControlledFileNasTransferServiceImpl.TASK_STATUS_RUNNING);
            storedTask.setLastRunAt(invocation.getArgument(1));
            return 1;
        });
        when(taskMapper.selectById(10L)).thenAnswer(invocation -> copyTask(tasks.get(10L)));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO updatedTask = invocation.getArgument(0);
            tasks.put(updatedTask.getId(), copyTask(updatedTask));
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileNasTransferTaskDO.class));
        when(taskItemMapper.selectListByTaskId(10L)).thenAnswer(invocation -> items.values().stream()
                .map(DccControlledFileNasTransferServiceTest::copyItem)
                .toList());
        stubAggregatedTaskItemSummary(() -> items.values().stream().toList());
        when(taskItemMapper.selectFirstWaitingItemByTaskId(10L)).thenAnswer(invocation -> items.values().stream()
                .filter(item -> DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING.equals(item.getStatus()))
                .map(DccControlledFileNasTransferServiceTest::copyItem)
                .findFirst()
                .orElse(null));
        lenient().when(taskItemMapper.claimWaitingItem(100L)).thenAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = items.get(100L);
            if (item == null || !DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING.equals(item.getStatus())) {
                return 0;
            }
            item.setStatus(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_RUNNING);
            return 1;
        });
        lenient().when(taskItemMapper.selectById(100L)).thenAnswer(invocation -> copyItem(items.get(100L)));
        lenient().doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO updatedItem = invocation.getArgument(0);
            items.put(updatedItem.getId(), copyItem(updatedItem));
            return 1;
        }).when(taskItemMapper).updateById(any(DccControlledFileNasTransferTaskItemDO.class));
        when(directoryAccessRuleMapper.selectList()).thenReturn(List.of());
        when(categoryMapper.selectList()).thenReturn(List.of(templateCategory()));
        when(categoryMapper.selectById(900250L)).thenReturn(templateCategory());
        when(categoryDirectoryBindingMapper.selectList()).thenReturn(List.of(binding(900250L, 902634L)));
        when(permissionRuleMapper.selectList()).thenReturn(List.of());
        when(distributionRuleMapper.selectList()).thenReturn(List.of());
        when(trainingRuleMapper.selectList()).thenReturn(List.of());
        when(routeMapper.selectList()).thenReturn(List.of());
        when(routeNodeMapper.selectList()).thenReturn(List.of());
        return directoryItem;
    }

    private static NasAclReadResult sampleAcl(String path) {
        return new NasAclReadResult(
                path,
                "S-1-5-21-1000-2000-3000-500",
                "S-1-5-21-1000-2000-3000-513",
                List.of("SE_DACL_PRESENT", "SE_DACL_PROTECTED"),
                true,
                true,
                List.of(new NasAclAce(
                        0,
                        "ACCESS_ALLOWED_ACE_TYPE",
                        List.of("CONTAINER_INHERIT_ACE", "OBJECT_INHERIT_ACE"),
                        2032127L,
                        "S-1-5-21-1000-2000-3000-1101",
                        false
                ))
        );
    }

    private static Class<?> localFolderImportRequestType() throws ClassNotFoundException {
        return Class.forName(
                "cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportReqVO");
    }

    private static Class<?> localFolderImportSessionCreateRequestType() throws ClassNotFoundException {
        return Class.forName(
                "cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportSessionCreateReqVO");
    }

    private static Class<?> localFolderImportBatchRequestType() throws ClassNotFoundException {
        return Class.forName(
                "cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportBatchReqVO");
    }

    private static Class<?> localFolderImportChunkRequestType() throws ClassNotFoundException {
        return Class.forName(
                "cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportChunkReqVO");
    }

    private static Object newLocalFolderImportReq(String rootDirectoryName,
                                                  List<String> relativePaths,
                                                  MockMultipartFile... files) throws Exception {
        Class<?> requestType = localFolderImportRequestType();
        Object reqVO = requestType.getDeclaredConstructor().newInstance();
        requestType.getMethod("setTemplateCategoryId", Long.class).invoke(reqVO, 900250L);
        requestType.getMethod("setDccProjectCodeId", Long.class).invoke(reqVO, 3000L);
        requestType.getMethod("setProductMasterId", Long.class).invoke(reqVO, 5000L);
        requestType.getMethod("setEffectiveDate", LocalDate.class).invoke(reqVO, LocalDate.of(2026, 6, 13));
        requestType.getMethod("setRootDirectoryName", String.class).invoke(reqVO, rootDirectoryName);
        requestType.getMethod("setRelativePaths", List.class).invoke(reqVO, relativePaths);
        MultipartFile[] multipartFiles = files;
        requestType.getMethod("setFiles", MultipartFile[].class).invoke(reqVO, (Object) multipartFiles);
        return reqVO;
    }

    private static Object newLocalFolderImportSessionCreateReq(String rootDirectoryName,
                                                               Long expectedFileCount,
                                                               Long expectedTotalBytes) throws Exception {
        Class<?> requestType = localFolderImportSessionCreateRequestType();
        Object reqVO = requestType.getDeclaredConstructor().newInstance();
        requestType.getMethod("setTemplateCategoryId", Long.class).invoke(reqVO, 900250L);
        requestType.getMethod("setDccProjectCodeId", Long.class).invoke(reqVO, 3000L);
        requestType.getMethod("setProductMasterId", Long.class).invoke(reqVO, 5000L);
        requestType.getMethod("setEffectiveDate", LocalDate.class).invoke(reqVO, LocalDate.of(2026, 6, 14));
        requestType.getMethod("setRootDirectoryName", String.class).invoke(reqVO, rootDirectoryName);
        requestType.getMethod("setExpectedFileCount", Long.class).invoke(reqVO, expectedFileCount);
        requestType.getMethod("setExpectedTotalBytes", Long.class).invoke(reqVO, expectedTotalBytes);
        return reqVO;
    }

    private static Object newLocalFolderImportBatchReq(List<String> relativePaths,
                                                       MockMultipartFile... files) throws Exception {
        Class<?> requestType = localFolderImportBatchRequestType();
        Object reqVO = requestType.getDeclaredConstructor().newInstance();
        requestType.getMethod("setRelativePaths", List.class).invoke(reqVO, relativePaths);
        MultipartFile[] multipartFiles = files;
        requestType.getMethod("setFiles", MultipartFile[].class).invoke(reqVO, (Object) multipartFiles);
        return reqVO;
    }

    private static Object newLocalFolderImportChunkReq(String relativePath,
                                                       String fileName,
                                                       Long fileSize,
                                                       Integer chunkIndex,
                                                       Integer totalChunks,
                                                       String chunkSha256,
                                                       String contentType,
                                                       MockMultipartFile chunk) throws Exception {
        Class<?> requestType = localFolderImportChunkRequestType();
        Object reqVO = requestType.getDeclaredConstructor().newInstance();
        requestType.getMethod("setRelativePath", String.class).invoke(reqVO, relativePath);
        requestType.getMethod("setFileName", String.class).invoke(reqVO, fileName);
        requestType.getMethod("setFileSize", Long.class).invoke(reqVO, fileSize);
        requestType.getMethod("setChunkIndex", Integer.class).invoke(reqVO, chunkIndex);
        requestType.getMethod("setTotalChunks", Integer.class).invoke(reqVO, totalChunks);
        requestType.getMethod("setChunkSha256", String.class).invoke(reqVO, chunkSha256);
        requestType.getMethod("setContentType", String.class).invoke(reqVO, contentType);
        requestType.getMethod("setChunk", MultipartFile.class).invoke(reqVO, chunk);
        return reqVO;
    }

    private DccControlledFileNasTransferRespVO invokeLocalFolderImport(Object reqVO) throws Exception {
        Method method = DccControlledFileNasTransferService.class
                .getMethod("importLocalFolder", Long.class, reqVO.getClass());
        return (DccControlledFileNasTransferRespVO) method.invoke(transferService, 99L, reqVO);
    }

    private DccControlledFileNasTransferRespVO invokeCreateLocalFolderImportSession(Object reqVO)
            throws Exception {
        Method method = DccControlledFileNasTransferService.class
                .getMethod("createLocalFolderImportSession", Long.class, reqVO.getClass());
        return (DccControlledFileNasTransferRespVO) method.invoke(transferService, 99L, reqVO);
    }

    private DccControlledFileNasTransferRespVO invokeUploadLocalFolderImportBatch(Long taskId, Object reqVO)
            throws Exception {
        Method method = DccControlledFileNasTransferService.class
                .getMethod("uploadLocalFolderImportBatch", Long.class, Long.class, reqVO.getClass());
        return (DccControlledFileNasTransferRespVO) method.invoke(transferService, 99L, taskId, reqVO);
    }

    private Object invokeUploadLocalFolderImportChunk(Long taskId, Object reqVO)
            throws Exception {
        Method method = DccControlledFileNasTransferService.class
                .getMethod("uploadLocalFolderImportChunk", Long.class, Long.class, reqVO.getClass());
        return method.invoke(transferService, 99L, taskId, reqVO);
    }

    private DccControlledFileNasTransferRespVO invokeCompleteLocalFolderImportSession(Long taskId)
            throws Exception {
        Method method = DccControlledFileNasTransferService.class
                .getMethod("completeLocalFolderImportSession", Long.class, Long.class);
        return (DccControlledFileNasTransferRespVO) method.invoke(transferService, 99L, taskId);
    }

    private static String readStringProperty(Object target, String property) {
        Object value = readProperty(target, property);
        return value == null ? null : value.toString();
    }

    private static Long readLongProperty(Object target, String property) {
        Object value = readProperty(target, property);
        return value == null ? null : ((Number) value).longValue();
    }

    private static Object readProperty(Object target, String property) {
        try {
            String methodName = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Cannot read property " + property + " from " + target.getClass().getName(), ex);
        }
    }

    private static void writeProperty(Object target, String property, Object value) {
        try {
            String methodName = "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
            Method setter = null;
            for (Method method : target.getClass().getMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                    setter = method;
                    break;
                }
            }
            if (setter == null) {
                throw new NoSuchMethodException(methodName);
            }
            setter.invoke(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Cannot write property " + property + " on " + target.getClass().getName(), ex);
        }
    }

    private static String sha256Hex(byte[] content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(content));
    }

    private static void copyPropertyIfPresent(Object source, Object target, String property) {
        try {
            String getterName = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
            String setterName = "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
            Object value = source.getClass().getMethod(getterName).invoke(source);
            for (Method method : target.getClass().getMethods()) {
                if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                    method.invoke(target, value);
                    return;
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // Older schema classes do not have large-folder progress fields yet.
        }
    }

    private static DccFileCategoryDO templateCategory() {
        return DccFileCategoryDO.builder()
                .id(900250L)
                .code("INTAUTH-1")
                .name("产品技术要求")
                .active(Boolean.TRUE)
                .distributionRequired(Boolean.TRUE)
                .trainingRequired(Boolean.TRUE)
                .sort(1)
                .build();
    }

    private static DccFileCategoryDO otherCategory() {
        return DccFileCategoryDO.builder()
                .id(900250L)
                .code("DCC_OTHER")
                .name("其他")
                .active(Boolean.TRUE)
                .distributionRequired(Boolean.TRUE)
                .trainingRequired(Boolean.TRUE)
                .sort(1)
                .build();
    }

    private static DccFileDirectoryDO directory(Long id, Long parentId, String name, int sort) {
        return DccFileDirectoryDO.builder()
                .id(id)
                .parentId(parentId)
                .code("DIR-" + id)
                .name(name)
                .active(Boolean.TRUE)
                .sort(sort)
                .remark("test")
                .accessRuleManuallyBound(Boolean.FALSE)
                .build();
    }

    private static DccCategoryDirectoryBindingDO binding(Long categoryId, Long directoryId) {
        return DccCategoryDirectoryBindingDO.builder()
                .id(300L + categoryId + directoryId)
                .categoryId(categoryId)
                .directoryId(directoryId)
                .active(Boolean.TRUE)
                .build();
    }

    private void stubAggregatedTaskItemSummary(Supplier<List<DccControlledFileNasTransferTaskItemDO>> itemsSupplier) {
        when(taskItemMapper.selectCountByTaskIdAndItemTypeAndDirectoryOutcome(anyLong(), anyString(), anyString()))
                .thenAnswer(invocation -> itemsSupplier.get().stream()
                        .filter(item -> item.getTaskId().equals(invocation.getArgument(0)))
                        .filter(item -> item.getItemType().equals(invocation.getArgument(1)))
                        .filter(item -> item.getDirectoryOutcome() != null
                                && item.getDirectoryOutcome().equals(invocation.getArgument(2)))
                        .count());
        when(taskItemMapper.selectCountByTaskIdAndItemTypeAndCategoryOutcome(anyLong(), anyString(), anyString()))
                .thenAnswer(invocation -> itemsSupplier.get().stream()
                        .filter(item -> item.getTaskId().equals(invocation.getArgument(0)))
                        .filter(item -> item.getItemType().equals(invocation.getArgument(1)))
                        .filter(item -> item.getCategoryOutcome() != null
                                && item.getCategoryOutcome().equals(invocation.getArgument(2)))
                        .count());
        when(taskItemMapper.selectCompletedFileCountByTaskId(anyLong()))
                .thenAnswer(invocation -> itemsSupplier.get().stream()
                        .filter(item -> item.getTaskId().equals(invocation.getArgument(0)))
                        .filter(item -> DccControlledFileNasTransferServiceImpl.ITEM_TYPE_FILE.equals(item.getItemType()))
                        .filter(item -> DccControlledFileNasTransferServiceImpl.ITEM_STATUS_COMPLETED.equals(item.getStatus()))
                        .count());
        when(taskItemMapper.selectPreviewDownloadOnlyCompletedFileCountByTaskId(anyLong()))
                .thenAnswer(invocation -> itemsSupplier.get().stream()
                        .filter(item -> item.getTaskId().equals(invocation.getArgument(0)))
                        .filter(item -> DccControlledFileNasTransferServiceImpl.ITEM_TYPE_FILE.equals(item.getItemType()))
                        .filter(item -> DccControlledFileNasTransferServiceImpl.ITEM_STATUS_COMPLETED.equals(item.getStatus()))
                        .filter(item -> Boolean.TRUE.equals(item.getPreviewDownloadOnly()))
                        .count());
        when(taskItemMapper.selectPendingItemCountByTaskId(anyLong()))
                .thenAnswer(invocation -> itemsSupplier.get().stream()
                        .filter(item -> item.getTaskId().equals(invocation.getArgument(0)))
                        .filter(item -> DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING.equals(item.getStatus())
                                || DccControlledFileNasTransferServiceImpl.ITEM_STATUS_RUNNING.equals(item.getStatus()))
                        .count());
        when(taskItemMapper.selectFailedItemsByTaskId(anyLong()))
                .thenAnswer(invocation -> itemsSupplier.get().stream()
                        .filter(item -> item.getTaskId().equals(invocation.getArgument(0)))
                        .filter(item -> DccControlledFileNasTransferServiceImpl.ITEM_STATUS_FAILED.equals(item.getStatus()))
                        .sorted(Comparator.comparingLong(DccControlledFileNasTransferTaskItemDO::getId))
                        .map(DccControlledFileNasTransferServiceTest::copyItem)
                        .toList());
    }

    private static DccControlledFileNasTransferTaskDO copyTask(DccControlledFileNasTransferTaskDO source) {
        if (source == null) {
            return null;
        }
        DccControlledFileNasTransferTaskDO copied = DccControlledFileNasTransferTaskDO.builder()
                .id(source.getId())
                .operatorUserId(source.getOperatorUserId())
                .templateCategoryId(source.getTemplateCategoryId())
                .dccProjectCodeId(source.getDccProjectCodeId())
                .productMasterId(source.getProductMasterId())
                .effectiveDate(source.getEffectiveDate())
                .selectedNasPathsJson(source.getSelectedNasPathsJson())
                .sourceType(source.getSourceType())
                .status(source.getStatus())
                .nextCheckAt(source.getNextCheckAt())
                .lastRunAt(source.getLastRunAt())
                .completedAt(source.getCompletedAt())
                .lastFailureMessage(source.getLastFailureMessage())
                .failureReportPath(source.getFailureReportPath())
                .failureReportGeneratedAt(source.getFailureReportGeneratedAt())
                .failureReportError(source.getFailureReportError())
                .build();
        copyPropertyIfPresent(source, copied, "expectedFileCount");
        copyPropertyIfPresent(source, copied, "expectedTotalBytes");
        copyPropertyIfPresent(source, copied, "uploadedFileCount");
        copyPropertyIfPresent(source, copied, "uploadedTotalBytes");
        copyPropertyIfPresent(source, copied, "uploadCompletedAt");
        return copied;
    }

    private static DccControlledFileNasTransferTaskItemDO copyItem(DccControlledFileNasTransferTaskItemDO source) {
        if (source == null) {
            return null;
        }
        return DccControlledFileNasTransferTaskItemDO.builder()
                .id(source.getId())
                .taskId(source.getTaskId())
                .parentItemId(source.getParentItemId())
                .itemType(source.getItemType())
                .nasPath(source.getNasPath())
                .itemName(source.getItemName())
                .sourceFileId(source.getSourceFileId())
                .status(source.getStatus())
                .attemptCount(source.getAttemptCount())
                .failureStage(source.getFailureStage())
                .lastError(source.getLastError())
                .resolvedDirectoryId(source.getResolvedDirectoryId())
                .resolvedCategoryId(source.getResolvedCategoryId())
                .directoryOutcome(source.getDirectoryOutcome())
                .categoryOutcome(source.getCategoryOutcome())
                .previewDownloadOnly(source.getPreviewDownloadOnly())
                .lastAttemptAt(source.getLastAttemptAt())
                .completedAt(source.getCompletedAt())
                .build();
    }

    private static DccFileDirectoryDO copyDirectory(DccFileDirectoryDO source) {
        return DccFileDirectoryDO.builder()
                .id(source.getId())
                .parentId(source.getParentId())
                .code(source.getCode())
                .name(source.getName())
                .active(source.getActive())
                .sort(source.getSort())
                .remark(source.getRemark())
                .accessRuleManuallyBound(source.getAccessRuleManuallyBound())
                .build();
    }

    private static DccFileCategoryDO copyCategory(DccFileCategoryDO source) {
        return DccFileCategoryDO.builder()
                .id(source.getId())
                .code(source.getCode())
                .name(source.getName())
                .parentId(source.getParentId())
                .active(source.getActive())
                .sort(source.getSort())
                .source(source.getSource())
                .remark(source.getRemark())
                .description(source.getDescription())
                .distributionRequired(source.getDistributionRequired())
                .trainingRequired(source.getTrainingRequired())
                .build();
    }

    private static DccCategoryDirectoryBindingDO copyBinding(DccCategoryDirectoryBindingDO source) {
        return DccCategoryDirectoryBindingDO.builder()
                .id(source.getId())
                .categoryId(source.getCategoryId())
                .directoryId(source.getDirectoryId())
                .active(source.getActive())
                .build();
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

    private static final class RecordingTransactionManager implements PlatformTransactionManager {

        private final AtomicInteger commits = new AtomicInteger();

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            commits.incrementAndGet();
        }

        @Override
        public void rollback(TransactionStatus status) {
            // no-op
        }

        int commits() {
            return commits.get();
        }
    }
}
