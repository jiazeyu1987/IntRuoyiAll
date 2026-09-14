package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileNasTransferReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileNasTransferRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasOriginalPathSyncReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasUncontrolledImportLocalWriteResultReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasUncontrolledImportSelectedReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileLocalFolderUploadChunkDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasSourceDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasTransferTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasTransferTaskItemDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccNasControlAuditFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccNasControlAuditTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccNasOriginalPathSyncFileDO;
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
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccNasControlAuditFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccNasControlAuditTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccNasOriginalPathSyncFileMapper;
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
import static org.mockito.Mockito.times;
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
    private DccNasControlAuditFileMapper auditFileMapper;
    @Mock
    private DccNasControlAuditTaskMapper auditTaskMapper;
    @Mock
    private DccNasOriginalPathSyncFileMapper originalPathSyncFileMapper;
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
        lenient().when(taskMapper.selectWaitingTasks(any(LocalDateTime.class))).thenReturn(List.of());
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
    void createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs() throws Exception {
        Class<?> requestType = Class.forName("cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasUncontrolledImportSelectedReqVO");
        Method method = DccControlledFileNasTransferService.class
                .getMethod("createUncontrolledImportTask", Long.class, Long.class, requestType);

        assertEquals(DccControlledFileNasTransferRespVO.class, method.getReturnType());
        for (Method requestMethod : requestType.getMethods()) {
            String methodName = requestMethod.getName();
            assertTrue(!methodName.contains("TemplateCategoryId")
                            && !methodName.contains("EffectiveDate")
                            && !methodName.contains("DccProjectCodeId"),
                    "NAS uncontrolled import request must not expose legacy transfer target field: " + methodName);
        }
    }

    @Test
    void processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten() {
        DccControlledFileNasTransferTaskDO importTask = DccControlledFileNasTransferTaskDO.builder()
                .id(77L)
                .auditTaskId(7001L)
                .operatorUserId(99L)
                .sourceType("NAS_UNCONTROLLED_IMPORT")
                .status(DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING)
                .idempotencyKey("idem-import-77")
                .requestHash("a".repeat(64))
                .build();
        when(taskMapper.selectWaitingTasks(any(LocalDateTime.class))).thenReturn(List.of(importTask));

        transferService.processWaitingTasks();

        verify(taskMapper, never()).claimWaitingTask(eq(77L), any(LocalDateTime.class));
        verify(nasBrowserService, never()).listFiles(any());
        verify(nasBrowserService, never()).readFile(any());
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
        verify(nasSourceMapper, never()).insert(any(DccControlledFileNasSourceDO.class));
    }

    @Test
    void createOriginalPathSyncTask_doesNotRequireClassificationOrLocalDirectory() throws Exception {
        Class<?> requestType = Class.forName("cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasOriginalPathSyncReqVO");
        Method method = DccControlledFileNasTransferService.class
                .getMethod("createOriginalPathSyncTask", Long.class, Long.class, requestType);

        assertEquals(DccControlledFileNasTransferRespVO.class, method.getReturnType());
        for (Method requestMethod : requestType.getMethods()) {
            String methodName = requestMethod.getName();
            assertTrue(!methodName.contains("TemplateCategoryId")
                            && !methodName.contains("EffectiveDate")
                            && !methodName.contains("DccProjectCodeId")
                            && !methodName.contains("LocalRelativePath"),
                    "NAS original-path sync request must not expose local/category/archive field: " + methodName);
        }
    }

    @Test
    void createOriginalPathSyncTask_firstUnsyncedCreatesOnePendingRecognitionItem() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        DccNasControlAuditFileDO firstFile = pendingRecognitionAuditFile(
                101L, "QMS/PRJ-20260728/design.pdf", "sig-design", 120L);
        DccNasControlAuditFileDO secondFile = pendingRecognitionAuditFile(
                102L, "QMS/PRJ-20260728/spec.pdf", "sig-spec", 80L);
        DccNasOriginalPathSyncReqVO reqVO = originalPathSyncReq("idem-original-one-001", "FIRST_UNSYNCED");

        lenient().when(taskMapper.selectOne(any())).thenReturn(null);
        when(auditTaskMapper.selectById(7001L)).thenReturn(completedAuditTask(7001L));
        when(auditFileMapper.selectListByTaskId(7001L)).thenReturn(List.of(firstFile, secondFile));
        when(originalPathSyncFileMapper.selectActiveByPathHashes(eq("quality"), any())).thenReturn(List.of());
        AtomicLong nextTaskId = new AtomicLong(8301L);
        AtomicLong nextItemId = new AtomicLong(9301L);
        List<DccControlledFileNasTransferTaskItemDO> insertedItems = new ArrayList<>();
        final DccControlledFileNasTransferTaskDO[] storedTask = new DccControlledFileNasTransferTaskDO[1];
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO task = invocation.getArgument(0);
            task.setId(nextTaskId.getAndIncrement());
            storedTask[0] = task;
            return 1;
        }).when(taskMapper).insert(any(DccControlledFileNasTransferTaskDO.class));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            item.setId(nextItemId.getAndIncrement());
            insertedItems.add(item);
            return 1;
        }).when(taskItemMapper).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        lenient().when(taskMapper.selectById(8301L)).thenAnswer(invocation -> storedTask[0]);
        stubAggregatedTaskItemSummary(() -> insertedItems);

        DccControlledFileNasTransferRespVO response =
                transferService.createOriginalPathSyncTask(99L, 7001L, reqVO);

        assertEquals(8301L, response.getTaskId());
        ArgumentCaptor<DccControlledFileNasTransferTaskDO> taskCaptor =
                ArgumentCaptor.forClass(DccControlledFileNasTransferTaskDO.class);
        verify(taskMapper).insert(taskCaptor.capture());
        DccControlledFileNasTransferTaskDO task = taskCaptor.getValue();
        assertEquals(7001L, task.getAuditTaskId());
        assertEquals(99L, task.getOperatorUserId());
        assertEquals(DccControlledFileNasTransferServiceImpl.SOURCE_TYPE_NAS_ORIGINAL_PATH_SYNC,
                task.getSourceType());
        assertEquals(1L, task.getExpectedFileCount());
        assertEquals(120L, task.getExpectedTotalBytes());
        assertTrue(task.getTemplateCategoryId() == null);
        assertTrue(task.getEffectiveDate() == null);
        assertTrue(task.getDccProjectCodeId() == null);

        assertEquals(1, insertedItems.size());
        DccControlledFileNasTransferTaskItemDO item = insertedItems.get(0);
        assertEquals(101L, item.getAuditFileId());
        assertEquals("QMS/PRJ-20260728/design.pdf", item.getNasPath());
        assertEquals("sig-design", item.getSourceSignature());
        assertEquals("PENDING_RECOGNITION", item.getClassificationStatusSnapshot());
        assertEquals("QMS/PRJ-20260728/design.pdf", item.getLocalRelativePath());
        assertEquals(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING, item.getStatus());

        ArgumentCaptor<DccNasControlAuditFileDO> auditUpdateCaptor =
                ArgumentCaptor.forClass(DccNasControlAuditFileDO.class);
        verify(auditFileMapper).updateById(auditUpdateCaptor.capture());
        DccNasControlAuditFileDO updated = auditUpdateCaptor.getValue();
        assertEquals("ORIGINAL_PATH_WAITING", updated.getOriginalPathSyncStatus());
        assertEquals(8301L, updated.getOriginalPathSyncTaskId());
        assertEquals(9301L, updated.getOriginalPathSyncTaskItemId());
        verify(nasBrowserService, never()).readFile(any());
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
        verify(nasSourceMapper, never()).insert(any(DccControlledFileNasSourceDO.class));
    }

    @Test
    void processWaitingTasks_originalPathSyncCreatesActiveRecordAndSkipsFormalArchive() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        long modifiedAtEpoch = 1_786_854_600_000L;
        String nasPath = "QMS/PRJ-20260728/design.pdf";
        String pathHash = DccNasPathUtils.pathHash("quality", nasPath);
        String sourceSignature = sourceSignature(pathHash, 3L, modifiedAtEpoch);
        DccControlledFileNasTransferTaskDO task = originalPathSyncTask(8401L, 99L, 7001L, "idem-process-001",
                "r".repeat(64));
        DccNasControlAuditFileDO auditFile = pendingRecognitionAuditFile(
                801L, nasPath, sourceSignature, 3L);
        auditFile.setPathHash(pathHash);
        auditFile.setModifiedAt(LocalDateTime.of(2026, 8, 18, 7, 10));
        auditFile.setOriginalPathSyncStatus("ORIGINAL_PATH_WAITING");
        auditFile.setOriginalPathSyncTaskId(8401L);
        auditFile.setOriginalPathSyncTaskItemId(9401L);
        DccControlledFileNasTransferTaskItemDO item = originalPathSyncItem(9401L, task, auditFile);
        List<DccControlledFileNasTransferTaskItemDO> items = new ArrayList<>(List.of(item));
        when(taskMapper.selectWaitingTasks(any(LocalDateTime.class))).thenReturn(List.of(task));
        when(taskMapper.selectById(8401L)).thenReturn(task);
        when(taskMapper.claimWaitingTask(eq(8401L), any(LocalDateTime.class))).thenReturn(1);
        when(taskItemMapper.selectFirstWaitingItemByTaskId(8401L)).thenReturn(item, null);
        when(taskItemMapper.claimWaitingItem(9401L)).thenReturn(1);
        when(taskItemMapper.selectById(9401L)).thenReturn(item);
        when(auditFileMapper.selectById(801L)).thenReturn(auditFile);
        when(originalPathSyncFileMapper.selectActiveByPathHash("quality", pathHash)).thenReturn(null);
        when(nasBrowserService.listFiles("QMS/PRJ-20260728")).thenReturn(new FileNasListRespVO()
                .setItems(List.of(new FileNasListRespVO.Item()
                        .setName("design.pdf")
                        .setPath(nasPath)
                        .setDir(false)
                        .setSize(3L)
                        .setModifiedAt(modifiedAtEpoch))));
        when(nasBrowserService.readFile(nasPath)).thenReturn(new NasFileReadResult(
                "design.pdf", nasPath, "application/pdf", "pdf".getBytes(StandardCharsets.UTF_8)));
        when(fileService.createFileAndReturnId(any(byte[].class), eq("design.pdf"),
                eq("dcc/nas-original-path-sync/QMS/PRJ-20260728"), eq("application/pdf"))).thenReturn(5201L);
        doAnswer(invocation -> {
            DccNasOriginalPathSyncFileDO syncFile = invocation.getArgument(0);
            syncFile.setId(8802L);
            return 1;
        }).when(originalPathSyncFileMapper).insert(any(DccNasOriginalPathSyncFileDO.class));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO updated = invocation.getArgument(0);
            items.set(0, copyItem(updated));
            return 1;
        }).when(taskItemMapper).updateById(any(DccControlledFileNasTransferTaskItemDO.class));
        stubAggregatedTaskItemSummary(() -> items);

        transferService.processWaitingTasks();

        ArgumentCaptor<DccNasOriginalPathSyncFileDO> syncCaptor =
                ArgumentCaptor.forClass(DccNasOriginalPathSyncFileDO.class);
        verify(originalPathSyncFileMapper).insert(syncCaptor.capture());
        DccNasOriginalPathSyncFileDO syncFile = syncCaptor.getValue();
        assertEquals(7001L, syncFile.getAuditTaskId());
        assertEquals(801L, syncFile.getAuditFileId());
        assertEquals(8401L, syncFile.getTransferTaskId());
        assertEquals(9401L, syncFile.getTransferTaskItemId());
        assertEquals(5201L, syncFile.getSourceFileId());
        assertEquals("quality", syncFile.getNasShareName());
        assertEquals(nasPath, syncFile.getNormalizedRelativePath());
        assertEquals(pathHash, syncFile.getPathHash());
        assertEquals("ACTIVE", syncFile.getSyncStatus());

        ArgumentCaptor<DccNasControlAuditFileDO> auditCaptor =
                ArgumentCaptor.forClass(DccNasControlAuditFileDO.class);
        verify(auditFileMapper).updateById(auditCaptor.capture());
        assertEquals("ORIGINAL_PATH_ACTIVE", auditCaptor.getValue().getOriginalPathSyncStatus());
        assertEquals(8802L, auditCaptor.getValue().getOriginalPathSyncFileId());
        assertEquals(null, auditCaptor.getValue().getOriginalPathSyncErrorCode());
        assertEquals(null, auditCaptor.getValue().getOriginalPathSyncError());

        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
        verify(nasSourceMapper, never()).insert(any(DccControlledFileNasSourceDO.class));
        verify(snapshotCaptureService, never()).completeSnapshotForTask(anyLong());
    }

    @Test
    void deleteOriginalPathSyncFile_marksActiveRecordDeletedAndClearsAuditFileBinding() throws Exception {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        DccNasOriginalPathSyncFileDO syncFile = DccNasOriginalPathSyncFileDO.builder()
                .id(8802L)
                .auditFileId(801L)
                .sourceFileId(7700L)
                .syncStatus("ACTIVE")
                .build();
        DccNasControlAuditFileDO auditFile = pendingRecognitionAuditFile(
                801L, "QMS/PRJ-20260728/design.pdf", "sig-design", 3L);
        auditFile.setOriginalPathSyncStatus("ORIGINAL_PATH_ACTIVE");
        auditFile.setOriginalPathSyncFileId(8802L);
        auditFile.setOriginalPathSyncTaskId(8401L);
        auditFile.setOriginalPathSyncTaskItemId(9401L);
        when(originalPathSyncFileMapper.selectById(8802L)).thenReturn(syncFile);
        when(originalPathSyncFileMapper.softDeleteActiveById(eq(8802L), eq(99L), any(LocalDateTime.class)))
                .thenReturn(1);
        when(auditFileMapper.selectById(801L)).thenReturn(auditFile);
        when(auditFileMapper.markOriginalPathSyncDeleted(801L, 8802L)).thenReturn(1);

        transferService.deleteOriginalPathSyncFile(99L, 8802L);

        verify(originalPathSyncFileMapper).softDeleteActiveById(eq(8802L), eq(99L), any(LocalDateTime.class));
        verify(fileService).deleteFile(7700L);
        verify(auditFileMapper).markOriginalPathSyncDeleted(801L, 8802L);
        verify(auditFileMapper, never()).updateById(auditFile);
    }

    @Test
    void createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        DccNasControlAuditFileDO designFile = matchedAuditFile(101L, "QMS/PRJ-20260728/design.pdf",
                "sig-design", "PRJ-20260728/Design/design.pdf", 120L);
        DccNasControlAuditFileDO specFile = matchedAuditFile(102L, "QMS/PRJ-20260728/spec.pdf",
                "sig-spec", "PRJ-20260728/Spec/spec.pdf", 80L);
        DccNasUncontrolledImportSelectedReqVO reqVO = uncontrolledImportReq("idem-create-001",
                selectedAuditFile(102L, "sig-spec", "PRJ-20260728/Spec/spec.pdf"),
                selectedAuditFile(101L, "sig-design", "PRJ-20260728/Design/design.pdf"));

        lenient().when(taskMapper.selectOne(any())).thenReturn(null);
        lenient().when(auditFileMapper.selectBatchIds(any())).thenReturn(List.of(specFile, designFile));
        AtomicLong nextTaskId = new AtomicLong(8001L);
        AtomicLong nextItemId = new AtomicLong(9001L);
        List<DccControlledFileNasTransferTaskItemDO> insertedItems = new ArrayList<>();
        final DccControlledFileNasTransferTaskDO[] storedTask = new DccControlledFileNasTransferTaskDO[1];
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO task = invocation.getArgument(0);
            task.setId(nextTaskId.getAndIncrement());
            storedTask[0] = task;
            return 1;
        }).when(taskMapper).insert(any(DccControlledFileNasTransferTaskDO.class));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            item.setId(nextItemId.getAndIncrement());
            insertedItems.add(item);
            return 1;
        }).when(taskItemMapper).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        lenient().when(taskMapper.selectById(8001L)).thenAnswer(invocation -> storedTask[0]);

        DccControlledFileNasTransferRespVO response =
                transferService.createUncontrolledImportTask(99L, 7001L, reqVO);

        assertEquals(8001L, response.getTaskId());
        ArgumentCaptor<DccControlledFileNasTransferTaskDO> taskCaptor =
                ArgumentCaptor.forClass(DccControlledFileNasTransferTaskDO.class);
        verify(taskMapper).insert(taskCaptor.capture());
        DccControlledFileNasTransferTaskDO task = taskCaptor.getValue();
        assertEquals(7001L, task.getAuditTaskId());
        assertEquals(99L, task.getOperatorUserId());
        assertEquals(DccControlledFileNasTransferServiceImpl.SOURCE_TYPE_NAS_UNCONTROLLED_IMPORT,
                task.getSourceType());
        assertEquals(DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING, task.getStatus());
        assertEquals("idem-create-001", task.getIdempotencyKey());
        assertEquals(64, task.getRequestHash().length());
        assertTrue(task.getTemplateCategoryId() == null);
        assertTrue(task.getEffectiveDate() == null);
        assertTrue(task.getDccProjectCodeId() == null);
        assertEquals(2L, task.getExpectedFileCount());
        assertEquals(200L, task.getExpectedTotalBytes());

        assertEquals(2, insertedItems.size());
        assertEquals(101L, insertedItems.get(0).getAuditFileId());
        assertEquals("sig-design", insertedItems.get(0).getSourceSignature());
        assertEquals("MATCHED", insertedItems.get(0).getClassificationStatusSnapshot());
        assertEquals("PRJ-20260728/Design/design.pdf", insertedItems.get(0).getLocalRelativePath());
        assertEquals("NOT_STARTED", insertedItems.get(0).getLocalWriteStatus());
        assertEquals("NOT_STARTED", insertedItems.get(0).getArchiveStatus());
        assertEquals(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING, insertedItems.get(0).getStatus());
        assertEquals(102L, insertedItems.get(1).getAuditFileId());

        ArgumentCaptor<DccNasControlAuditFileDO> auditUpdateCaptor =
                ArgumentCaptor.forClass(DccNasControlAuditFileDO.class);
        verify(auditFileMapper, times(2)).updateById(auditUpdateCaptor.capture());
        Map<Long, DccNasControlAuditFileDO> updatedById = new LinkedHashMap<>();
        for (DccNasControlAuditFileDO updated : auditUpdateCaptor.getAllValues()) {
            updatedById.put(updated.getId(), updated);
        }
        assertEquals("SELECTED", updatedById.get(101L).getDownloadStatus());
        assertEquals(8001L, updatedById.get(101L).getSelectedImportTaskId());
        assertEquals(9001L, updatedById.get(101L).getSelectedImportTaskItemId());
        assertEquals("PRJ-20260728/Design/design.pdf", updatedById.get(101L).getLocalRelativePath());
        assertEquals("SELECTED", updatedById.get(102L).getDownloadStatus());
        assertEquals(9002L, updatedById.get(102L).getSelectedImportTaskItemId());
        verify(nasBrowserService, never()).readFile(any());
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
        verify(nasSourceMapper, never()).insert(any(DccControlledFileNasSourceDO.class));
    }

    @Test
    void createUncontrolledImportTask_allowsPendingReviewFilesForLocalDownloadOnly() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        DccNasControlAuditFileDO pendingFile = pendingReviewAuditFile(103L,
                "QMS/unknown/no-project-random-file.pdf", "sig-pending-review",
                "_未分类待处理/QMS/unknown/no-project-random-file.pdf", 42L,
                "UNCLASSIFIED_PENDING", "PROJECT_CODE_NOT_FOUND");
        DccNasUncontrolledImportSelectedReqVO reqVO = uncontrolledImportReq("idem-pending-review-001",
                selectedAuditFile(103L, "sig-pending-review",
                        "_未分类待处理/QMS/unknown/no-project-random-file.pdf"));

        lenient().when(taskMapper.selectOne(any())).thenReturn(null);
        lenient().when(auditFileMapper.selectBatchIds(any())).thenReturn(List.of(pendingFile));
        AtomicLong nextTaskId = new AtomicLong(8011L);
        AtomicLong nextItemId = new AtomicLong(9011L);
        List<DccControlledFileNasTransferTaskItemDO> insertedItems = new ArrayList<>();
        final DccControlledFileNasTransferTaskDO[] storedTask = new DccControlledFileNasTransferTaskDO[1];
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskDO task = invocation.getArgument(0);
            task.setId(nextTaskId.getAndIncrement());
            storedTask[0] = task;
            return 1;
        }).when(taskMapper).insert(any(DccControlledFileNasTransferTaskDO.class));
        doAnswer(invocation -> {
            DccControlledFileNasTransferTaskItemDO item = invocation.getArgument(0);
            item.setId(nextItemId.getAndIncrement());
            insertedItems.add(item);
            return 1;
        }).when(taskItemMapper).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        lenient().when(taskMapper.selectById(8011L)).thenAnswer(invocation -> storedTask[0]);

        DccControlledFileNasTransferRespVO response =
                transferService.createUncontrolledImportTask(99L, 7001L, reqVO);

        assertEquals(8011L, response.getTaskId());
        assertEquals(1, insertedItems.size());
        DccControlledFileNasTransferTaskItemDO item = insertedItems.get(0);
        assertEquals(103L, item.getAuditFileId());
        assertEquals("UNCLASSIFIED_PENDING", item.getClassificationStatusSnapshot());
        assertEquals("_未分类待处理/QMS/unknown/no-project-random-file.pdf", item.getLocalRelativePath());
        assertEquals("PENDING_MANUAL_REVIEW", item.getArchiveStatus());
        ArgumentCaptor<DccNasControlAuditFileDO> auditUpdateCaptor =
                ArgumentCaptor.forClass(DccNasControlAuditFileDO.class);
        verify(auditFileMapper).updateById(auditUpdateCaptor.capture());
        DccNasControlAuditFileDO updated = auditUpdateCaptor.getValue();
        assertEquals("SELECTED", updated.getDownloadStatus());
        assertEquals("PENDING_MANUAL_REVIEW", updated.getArchiveStatus());
        assertEquals(8011L, updated.getSelectedImportTaskId());
        assertEquals(9011L, updated.getSelectedImportTaskItemId());
        verify(nasBrowserService, never()).readFile(any());
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
        verify(nasSourceMapper, never()).insert(any(DccControlledFileNasSourceDO.class));
    }

    @Test
    void createUncontrolledImportTask_rejectsInvalidSelectionAtomically() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        DccNasControlAuditFileDO validFile = matchedAuditFile(201L, "QMS/PRJ-20260728/valid.pdf",
                "sig-valid", "PRJ-20260728/Design/valid.pdf", 11L);
        DccNasControlAuditFileDO pendingFile = matchedAuditFile(202L, "QMS/PRJ-20260728/pending.pdf",
                "sig-pending", "PRJ-20260728/Design/pending.pdf", 12L);
        pendingFile.setClassificationStatus("PENDING_RECOGNITION");
        DccNasUncontrolledImportSelectedReqVO reqVO = uncontrolledImportReq("idem-invalid-001",
                selectedAuditFile(201L, "sig-valid", "PRJ-20260728/Design/valid.pdf"),
                selectedAuditFile(202L, "sig-pending", "PRJ-20260728/Design/pending.pdf"));
        lenient().when(taskMapper.selectOne(any())).thenReturn(null);
        lenient().when(auditFileMapper.selectBatchIds(any())).thenReturn(List.of(validFile, pendingFile));

        assertThrows(IllegalStateException.class,
                () -> transferService.createUncontrolledImportTask(99L, 7001L, reqVO));

        verify(taskMapper, never()).insert(any(DccControlledFileNasTransferTaskDO.class));
        verify(taskItemMapper, never()).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        verify(auditFileMapper, never()).updateById(any(DccNasControlAuditFileDO.class));
        verify(nasBrowserService, never()).readFile(any());
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
        verify(nasSourceMapper, never()).insert(any(DccControlledFileNasSourceDO.class));
    }

    @Test
    void createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder() {
        TenantContextHolder.setTenantId(1L);
        DccNasUncontrolledImportSelectedReqVO reqVO = uncontrolledImportReq("idem-existing-001",
                selectedAuditFile(302L, "sig-spec", "PRJ-20260728/Spec/spec.pdf"),
                selectedAuditFile(301L, "sig-design", "PRJ-20260728/Design/design.pdf"));
        String requestHash = uncontrolledImportRequestHash(7001L, reqVO);
        DccControlledFileNasTransferTaskDO existingTask = uncontrolledImportTask(
                8101L, 99L, 7001L, "idem-existing-001", requestHash);
        when(taskMapper.selectOne(any())).thenReturn(existingTask);
        when(taskMapper.selectById(8101L)).thenReturn(existingTask);

        DccControlledFileNasTransferRespVO response =
                transferService.createUncontrolledImportTask(99L, 7001L, reqVO);

        assertEquals(8101L, response.getTaskId());
        verify(auditFileMapper, never()).selectBatchIds(any());
        verify(taskMapper, never()).insert(any(DccControlledFileNasTransferTaskDO.class));
        verify(taskItemMapper, never()).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        verify(auditFileMapper, never()).updateById(any(DccNasControlAuditFileDO.class));
    }

    @Test
    void createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        DccNasUncontrolledImportSelectedReqVO reqVO = uncontrolledImportReq("idem-race-001",
                selectedAuditFile(401L, "sig-design", "PRJ-20260728/Design/design.pdf"));
        String requestHash = uncontrolledImportRequestHash(7001L, reqVO);
        DccControlledFileNasTransferTaskDO existingTask = uncontrolledImportTask(
                8102L, 99L, 7001L, "idem-race-001", requestHash);
        when(taskMapper.selectOne(any())).thenReturn(null, existingTask);
        when(taskMapper.selectById(8102L)).thenReturn(existingTask);

        DccControlledFileNasTransferRespVO response =
                transferService.createUncontrolledImportTask(99L, 7001L, reqVO);

        assertEquals(8102L, response.getTaskId());
        verify(auditFileMapper, never()).selectBatchIds(any());
        verify(taskMapper, never()).insert(any(DccControlledFileNasTransferTaskDO.class));
        verify(taskItemMapper, never()).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        verify(auditFileMapper, never()).updateById(any(DccNasControlAuditFileDO.class));
    }

    @Test
    void createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash() {
        TenantContextHolder.setTenantId(1L);
        DccNasUncontrolledImportSelectedReqVO reqVO = uncontrolledImportReq("idem-conflict-001",
                selectedAuditFile(501L, "sig-design", "PRJ-20260728/Design/design.pdf"));
        DccControlledFileNasTransferTaskDO existingTask = uncontrolledImportTask(
                8103L, 99L, 7001L, "idem-conflict-001", "b".repeat(64));
        when(taskMapper.selectOne(any())).thenReturn(existingTask);

        assertThrows(IllegalStateException.class,
                () -> transferService.createUncontrolledImportTask(99L, 7001L, reqVO));

        verify(auditFileMapper, never()).selectBatchIds(any());
        verify(taskMapper, never()).insert(any(DccControlledFileNasTransferTaskDO.class));
        verify(taskItemMapper, never()).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        verify(auditFileMapper, never()).updateById(any(DccNasControlAuditFileDO.class));
    }

    @Test
    void createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites() {
        TenantContextHolder.setTenantId(1L);
        DccNasUncontrolledImportSelectedReqVO reqVO = uncontrolledImportReq("idem-duplicate-001",
                selectedAuditFile(601L, "sig-design", "PRJ-20260728/Design/design.pdf"),
                selectedAuditFile(601L, "sig-design", "PRJ-20260728/Design/design.pdf"));

        assertThrows(IllegalStateException.class,
                () -> transferService.createUncontrolledImportTask(99L, 7001L, reqVO));

        verify(taskMapper, never()).selectOne(any());
        verify(auditFileMapper, never()).selectBatchIds(any());
        verify(taskMapper, never()).insert(any(DccControlledFileNasTransferTaskDO.class));
        verify(taskItemMapper, never()).insert(any(DccControlledFileNasTransferTaskItemDO.class));
        verify(auditFileMapper, never()).updateById(any(DccNasControlAuditFileDO.class));
    }

    @Test
    void readUncontrolledImportContent_returnsBinaryForBoundTaskWithoutMutatingLocalOrArchiveState() {
        TenantContextHolder.setTenantId(1L);
        DccControlledFileNasTransferTaskDO task = uncontrolledImportTask(
                8201L, 99L, 7001L, "idem-content-001", "c".repeat(64));
        DccNasControlAuditFileDO auditFile = matchedAuditFile(701L, "QMS/PRJ-20260728/design.pdf",
                "sig-content", "PRJ-20260728/Design/design.pdf", 3L);
        auditFile.setDownloadStatus(DccControlledFileNasTransferServiceImpl.AUDIT_FILE_DOWNLOAD_STATUS_SELECTED);
        auditFile.setSelectedImportTaskId(8201L);
        auditFile.setSelectedImportTaskItemId(9301L);
        auditFile.setLocalRelativePath("PRJ-20260728/Design/design.pdf");
        DccControlledFileNasTransferTaskItemDO item = uncontrolledImportItem(9301L, task, auditFile);
        when(taskMapper.selectById(8201L)).thenReturn(task);
        when(auditFileMapper.selectById(701L)).thenReturn(auditFile);
        when(taskItemMapper.selectById(9301L)).thenReturn(item);
        when(nasBrowserService.readFile("QMS/PRJ-20260728/design.pdf"))
                .thenReturn(new NasFileReadResult("design.pdf", "QMS/PRJ-20260728/design.pdf",
                        "application/pdf", "pdf".getBytes(StandardCharsets.UTF_8)));

        DccControlledFileBinary binary = transferService.readUncontrolledImportContent(
                99L, 8201L, 701L, "sig-content", "PRJ-20260728/Design/design.pdf");

        assertEquals("design.pdf", binary.fileName());
        assertEquals("application/octet-stream", binary.contentType());
        assertEquals("pdf", new String(binary.bytes(), StandardCharsets.UTF_8));
        verify(nasBrowserService).readFile("QMS/PRJ-20260728/design.pdf");
        verify(auditFileMapper, never()).updateById(any(DccNasControlAuditFileDO.class));
        verify(taskItemMapper, never()).updateById(any(DccControlledFileNasTransferTaskItemDO.class));
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
        verify(nasSourceMapper, never()).insert(any(DccControlledFileNasSourceDO.class));
    }

    @Test
    void readUncontrolledImportContent_returnsPendingReviewBinaryWithoutArchiving() {
        TenantContextHolder.setTenantId(1L);
        DccControlledFileNasTransferTaskDO task = uncontrolledImportTask(
                8211L, 99L, 7001L, "idem-content-pending-001", "p".repeat(64));
        DccNasControlAuditFileDO auditFile = pendingReviewAuditFile(711L,
                "QMS/unknown/no-project-random-file.pdf", "sig-content-pending",
                "_未分类待处理/QMS/unknown/no-project-random-file.pdf", 4L,
                "UNCLASSIFIED_PENDING", "PROJECT_CODE_NOT_FOUND");
        auditFile.setDownloadStatus(DccControlledFileNasTransferServiceImpl.AUDIT_FILE_DOWNLOAD_STATUS_SELECTED);
        auditFile.setSelectedImportTaskId(8211L);
        auditFile.setSelectedImportTaskItemId(9311L);
        auditFile.setLocalRelativePath("_未分类待处理/QMS/unknown/no-project-random-file.pdf");
        DccControlledFileNasTransferTaskItemDO item = uncontrolledImportItem(9311L, task, auditFile);
        item.setArchiveStatus("PENDING_MANUAL_REVIEW");
        when(taskMapper.selectById(8211L)).thenReturn(task);
        when(auditFileMapper.selectById(711L)).thenReturn(auditFile);
        when(taskItemMapper.selectById(9311L)).thenReturn(item);
        when(nasBrowserService.readFile("QMS/unknown/no-project-random-file.pdf"))
                .thenReturn(new NasFileReadResult("no-project-random-file.pdf",
                        "QMS/unknown/no-project-random-file.pdf", "application/pdf",
                        "pending".getBytes(StandardCharsets.UTF_8)));

        DccControlledFileBinary binary = transferService.readUncontrolledImportContent(
                99L, 8211L, 711L, "sig-content-pending",
                "_未分类待处理/QMS/unknown/no-project-random-file.pdf");

        assertEquals("no-project-random-file.pdf", binary.fileName());
        assertEquals("pending", new String(binary.bytes(), StandardCharsets.UTF_8));
        verify(nasBrowserService).readFile("QMS/unknown/no-project-random-file.pdf");
        verify(auditFileMapper, never()).updateById(any(DccNasControlAuditFileDO.class));
        verify(taskItemMapper, never()).updateById(any(DccControlledFileNasTransferTaskItemDO.class));
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
        verify(nasSourceMapper, never()).insert(any(DccControlledFileNasSourceDO.class));
    }

    @Test
    void readUncontrolledImportContent_rejectsCrossTaskOrStaleSignatureWithoutReadingNas() {
        TenantContextHolder.setTenantId(1L);
        DccControlledFileNasTransferTaskDO task = uncontrolledImportTask(
                8202L, 99L, 7001L, "idem-content-002", "d".repeat(64));
        DccNasControlAuditFileDO auditFile = matchedAuditFile(702L, "QMS/PRJ-20260728/stale.pdf",
                "sig-current", "PRJ-20260728/Design/stale.pdf", 3L);
        auditFile.setDownloadStatus(DccControlledFileNasTransferServiceImpl.AUDIT_FILE_DOWNLOAD_STATUS_SELECTED);
        auditFile.setSelectedImportTaskId(9999L);
        auditFile.setSelectedImportTaskItemId(9302L);
        auditFile.setLocalRelativePath("PRJ-20260728/Design/stale.pdf");
        when(taskMapper.selectById(8202L)).thenReturn(task);
        when(auditFileMapper.selectById(702L)).thenReturn(auditFile);

        assertThrows(IllegalStateException.class, () -> transferService.readUncontrolledImportContent(
                99L, 8202L, 702L, "sig-stale", "PRJ-20260728/Design/stale.pdf"));

        verify(nasBrowserService, never()).readFile(anyString());
        verify(auditFileMapper, never()).updateById(any(DccNasControlAuditFileDO.class));
        verify(taskItemMapper, never()).updateById(any(DccControlledFileNasTransferTaskItemDO.class));
    }

    @Test
    void recordUncontrolledImportLocalWriteResult_marksLocalWrittenAndArchiveMetadataBlockWithoutSideEffects() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        DccControlledFileNasTransferTaskDO task = uncontrolledImportTask(
                8203L, 99L, 7001L, "idem-local-write-001", "e".repeat(64));
        DccNasControlAuditFileDO auditFile = matchedAuditFile(703L, "QMS/PRJ-20260728/local.pdf",
                "sig-local", "PRJ-20260728/Design/local.pdf", 6L);
        auditFile.setDownloadStatus(DccControlledFileNasTransferServiceImpl.AUDIT_FILE_DOWNLOAD_STATUS_SELECTED);
        auditFile.setSelectedImportTaskId(8203L);
        auditFile.setSelectedImportTaskItemId(9303L);
        auditFile.setLocalRelativePath("PRJ-20260728/Design/local.pdf");
        DccControlledFileNasTransferTaskItemDO item = uncontrolledImportItem(9303L, task, auditFile);
        when(taskMapper.selectById(8203L)).thenReturn(task);
        when(auditFileMapper.selectById(703L)).thenReturn(auditFile);
        when(taskItemMapper.selectById(9303L)).thenReturn(item);
        stubAggregatedTaskItemSummary(() -> List.of(item));

        DccControlledFileNasTransferRespVO response = transferService.recordUncontrolledImportLocalWriteResult(
                99L, 8203L, 703L, localWriteResultReq("sig-local",
                        "PRJ-20260728/Design/local.pdf", "LOCAL_WRITTEN", null, null));

        assertEquals(8203L, response.getTaskId());
        ArgumentCaptor<DccNasControlAuditFileDO> auditCaptor =
                ArgumentCaptor.forClass(DccNasControlAuditFileDO.class);
        verify(auditFileMapper).updateById(auditCaptor.capture());
        assertEquals("LOCAL_WRITTEN", auditCaptor.getValue().getDownloadStatus());
        assertEquals("FAILED", auditCaptor.getValue().getArchiveStatus());
        assertEquals("ARCHIVE_METADATA_REQUIRED", auditCaptor.getValue().getArchiveErrorCode());
        ArgumentCaptor<DccControlledFileNasTransferTaskItemDO> itemCaptor =
                ArgumentCaptor.forClass(DccControlledFileNasTransferTaskItemDO.class);
        verify(taskItemMapper).updateById(itemCaptor.capture());
        assertEquals("LOCAL_WRITTEN", itemCaptor.getValue().getLocalWriteStatus());
        assertEquals("FAILED", itemCaptor.getValue().getArchiveStatus());
        assertEquals("ARCHIVE_METADATA_REQUIRED", itemCaptor.getValue().getArchiveErrorCode());
        verify(nasBrowserService, never()).readFile(anyString());
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
        verify(nasSourceMapper, never()).insert(any(DccControlledFileNasSourceDO.class));
    }

    @Test
    void recordUncontrolledImportLocalWriteResult_marksPendingReviewLocalWrittenWithoutArchiveSideEffects() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        DccControlledFileNasTransferTaskDO task = uncontrolledImportTask(
                8212L, 99L, 7001L, "idem-local-write-pending-001", "q".repeat(64));
        DccNasControlAuditFileDO auditFile = pendingReviewAuditFile(712L,
                "QMS/unknown/no-project-random-file.pdf", "sig-local-pending",
                "_未分类待处理/QMS/unknown/no-project-random-file.pdf", 4L,
                "UNCLASSIFIED_PENDING", "PROJECT_CODE_NOT_FOUND");
        auditFile.setDownloadStatus(DccControlledFileNasTransferServiceImpl.AUDIT_FILE_DOWNLOAD_STATUS_SELECTED);
        auditFile.setSelectedImportTaskId(8212L);
        auditFile.setSelectedImportTaskItemId(9312L);
        auditFile.setLocalRelativePath("_未分类待处理/QMS/unknown/no-project-random-file.pdf");
        DccControlledFileNasTransferTaskItemDO item = uncontrolledImportItem(9312L, task, auditFile);
        item.setArchiveStatus("PENDING_MANUAL_REVIEW");
        when(taskMapper.selectById(8212L)).thenReturn(task);
        when(auditFileMapper.selectById(712L)).thenReturn(auditFile);
        when(taskItemMapper.selectById(9312L)).thenReturn(item);
        stubAggregatedTaskItemSummary(() -> List.of(item));

        DccControlledFileNasTransferRespVO response = transferService.recordUncontrolledImportLocalWriteResult(
                99L, 8212L, 712L, localWriteResultReq("sig-local-pending",
                        "_未分类待处理/QMS/unknown/no-project-random-file.pdf", "LOCAL_WRITTEN", null, null));

        assertEquals(8212L, response.getTaskId());
        ArgumentCaptor<DccNasControlAuditFileDO> auditCaptor =
                ArgumentCaptor.forClass(DccNasControlAuditFileDO.class);
        verify(auditFileMapper).updateById(auditCaptor.capture());
        assertEquals("LOCAL_WRITTEN", auditCaptor.getValue().getDownloadStatus());
        assertEquals("PENDING_MANUAL_REVIEW", auditCaptor.getValue().getArchiveStatus());
        assertTrue(auditCaptor.getValue().getArchiveErrorCode() == null);
        ArgumentCaptor<DccControlledFileNasTransferTaskItemDO> itemCaptor =
                ArgumentCaptor.forClass(DccControlledFileNasTransferTaskItemDO.class);
        verify(taskItemMapper).updateById(itemCaptor.capture());
        assertEquals("LOCAL_WRITTEN", itemCaptor.getValue().getLocalWriteStatus());
        assertEquals("PENDING_MANUAL_REVIEW", itemCaptor.getValue().getArchiveStatus());
        assertEquals(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_COMPLETED,
                itemCaptor.getValue().getStatus());
        verify(nasBrowserService, never()).readFile(anyString());
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
        verify(nasSourceMapper, never()).insert(any(DccControlledFileNasSourceDO.class));
    }

    @Test
    void recordUncontrolledImportLocalWriteResult_replaysSameSuccessWithoutMutatingOrArchivingAgain() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        DccControlledFileNasTransferTaskDO task = uncontrolledImportTask(
                8204L, 99L, 7001L, "idem-local-write-002", "f".repeat(64));
        DccNasControlAuditFileDO auditFile = matchedAuditFile(704L, "QMS/PRJ-20260728/replay.pdf",
                "sig-replay", "PRJ-20260728/Design/replay.pdf", 8L);
        auditFile.setDownloadStatus("LOCAL_WRITTEN");
        auditFile.setArchiveStatus("FAILED");
        auditFile.setArchiveErrorCode("ARCHIVE_METADATA_REQUIRED");
        auditFile.setSelectedImportTaskId(8204L);
        auditFile.setSelectedImportTaskItemId(9304L);
        auditFile.setLocalRelativePath("PRJ-20260728/Design/replay.pdf");
        DccControlledFileNasTransferTaskItemDO item = uncontrolledImportItem(9304L, task, auditFile);
        item.setLocalWriteStatus("LOCAL_WRITTEN");
        item.setArchiveStatus("FAILED");
        item.setArchiveErrorCode("ARCHIVE_METADATA_REQUIRED");
        item.setStatus(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_FAILED);
        when(taskMapper.selectById(8204L)).thenReturn(task);
        when(auditFileMapper.selectById(704L)).thenReturn(auditFile);
        when(taskItemMapper.selectById(9304L)).thenReturn(item);
        stubAggregatedTaskItemSummary(() -> List.of(item));

        DccControlledFileNasTransferRespVO response = transferService.recordUncontrolledImportLocalWriteResult(
                99L, 8204L, 704L, localWriteResultReq("sig-replay",
                        "PRJ-20260728/Design/replay.pdf", "LOCAL_WRITTEN", null, null));

        assertEquals(8204L, response.getTaskId());
        verify(auditFileMapper, never()).updateById(any(DccNasControlAuditFileDO.class));
        verify(taskItemMapper, never()).updateById(any(DccControlledFileNasTransferTaskItemDO.class));
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
        verify(nasSourceMapper, never()).insert(any(DccControlledFileNasSourceDO.class));
    }

    @Test
    void recordUncontrolledImportLocalWriteResult_rejectsConflictingTerminalResultWithoutArchive() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        DccControlledFileNasTransferTaskDO task = uncontrolledImportTask(
                8205L, 99L, 7001L, "idem-local-write-003", "a".repeat(64));
        DccNasControlAuditFileDO auditFile = matchedAuditFile(705L, "QMS/PRJ-20260728/conflict.pdf",
                "sig-conflict", "PRJ-20260728/Design/conflict.pdf", 5L);
        auditFile.setDownloadStatus("LOCAL_WRITTEN");
        auditFile.setSelectedImportTaskId(8205L);
        auditFile.setSelectedImportTaskItemId(9305L);
        auditFile.setLocalRelativePath("PRJ-20260728/Design/conflict.pdf");
        DccControlledFileNasTransferTaskItemDO item = uncontrolledImportItem(9305L, task, auditFile);
        item.setLocalWriteStatus("LOCAL_WRITTEN");
        when(taskMapper.selectById(8205L)).thenReturn(task);
        when(auditFileMapper.selectById(705L)).thenReturn(auditFile);
        when(taskItemMapper.selectById(9305L)).thenReturn(item);

        assertThrows(IllegalStateException.class, () -> transferService.recordUncontrolledImportLocalWriteResult(
                99L, 8205L, 705L, localWriteResultReq("sig-conflict",
                        "PRJ-20260728/Design/conflict.pdf", "LOCAL_WRITE_FAILED",
                        "LOCAL_PATH_COLLISION", "target already exists")));

        verify(auditFileMapper, never()).updateById(any(DccNasControlAuditFileDO.class));
        verify(taskItemMapper, never()).updateById(any(DccControlledFileNasTransferTaskItemDO.class));
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
        verify(nasSourceMapper, never()).insert(any(DccControlledFileNasSourceDO.class));
    }

    @Test
    void recordUncontrolledImportLocalWriteResult_requiresArchiveMetadataForMatchedLocalWritten() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        DccControlledFileNasTransferTaskDO task = uncontrolledImportTask(
                8206L, 99L, 7001L, "idem-local-write-004", "b".repeat(64));
        DccNasControlAuditFileDO auditFile = matchedAuditFile(706L, "QMS/PRJ-20260728/archive-metadata.pdf",
                "sig-archive-metadata", "PRJ-20260728/Design/archive-metadata.pdf", 7L);
        auditFile.setDownloadStatus(DccControlledFileNasTransferServiceImpl.AUDIT_FILE_DOWNLOAD_STATUS_SELECTED);
        auditFile.setSelectedImportTaskId(8206L);
        auditFile.setSelectedImportTaskItemId(9306L);
        auditFile.setLocalRelativePath("PRJ-20260728/Design/archive-metadata.pdf");
        DccControlledFileNasTransferTaskItemDO item = uncontrolledImportItem(9306L, task, auditFile);
        when(taskMapper.selectById(8206L)).thenReturn(task);
        when(auditFileMapper.selectById(706L)).thenReturn(auditFile);
        when(taskItemMapper.selectById(9306L)).thenReturn(item);
        stubAggregatedTaskItemSummary(() -> List.of(item));

        DccControlledFileNasTransferRespVO response = transferService.recordUncontrolledImportLocalWriteResult(
                99L, 8206L, 706L, localWriteResultReq("sig-archive-metadata",
                        "PRJ-20260728/Design/archive-metadata.pdf", "LOCAL_WRITTEN", null, null));

        assertEquals(8206L, response.getTaskId());
        ArgumentCaptor<DccNasControlAuditFileDO> auditCaptor =
                ArgumentCaptor.forClass(DccNasControlAuditFileDO.class);
        verify(auditFileMapper).updateById(auditCaptor.capture());
        assertEquals("LOCAL_WRITTEN", auditCaptor.getValue().getDownloadStatus());
        assertEquals("FAILED", auditCaptor.getValue().getArchiveStatus());
        assertEquals("ARCHIVE_METADATA_REQUIRED", auditCaptor.getValue().getArchiveErrorCode());
        assertEquals(null, auditCaptor.getValue().getControlledFileId());
        ArgumentCaptor<DccControlledFileNasTransferTaskItemDO> itemCaptor =
                ArgumentCaptor.forClass(DccControlledFileNasTransferTaskItemDO.class);
        verify(taskItemMapper).updateById(itemCaptor.capture());
        assertEquals("LOCAL_WRITTEN", itemCaptor.getValue().getLocalWriteStatus());
        assertEquals("FAILED", itemCaptor.getValue().getArchiveStatus());
        assertEquals("ARCHIVE_METADATA_REQUIRED", itemCaptor.getValue().getArchiveErrorCode());
        verify(nasBrowserService, never()).readFile(anyString());
        verify(fileService, never()).createFileAndReturnId(any(byte[].class), anyString(), anyString(), anyString());
        verify(workflowService, never()).submitControlledFileWithoutApproval(anyLong(), any(DccControlledFileSubmitReqVO.class));
        verify(nasSourceMapper, never()).insert(any(DccControlledFileNasSourceDO.class));
    }

    @Test
    void archiveAfterLocalWritten_archivesOnlyFromFormalMetadataSnapshot() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        DccControlledFileNasTransferTaskDO task = uncontrolledImportTask(
                8207L, 99L, 7001L, "idem-local-write-005", "c".repeat(64));
        DccNasControlAuditFileDO auditFile = matchedAuditFile(707L, "QMS/PRJ-20260728/archive-success.pdf",
                "sig-archive-success", "PRJ-20260728/Design/archive-success.pdf", 7L);
        auditFile.setDownloadStatus(DccControlledFileNasTransferServiceImpl.AUDIT_FILE_DOWNLOAD_STATUS_SELECTED);
        auditFile.setSelectedImportTaskId(8207L);
        auditFile.setSelectedImportTaskItemId(9307L);
        auditFile.setLocalRelativePath("PRJ-20260728/Design/archive-success.pdf");
        DccControlledFileNasTransferTaskItemDO item = uncontrolledImportItem(9307L, task, auditFile);
        item.setArchiveCategoryIdSnapshot(9101L);
        item.setArchiveDirectoryIdSnapshot(9201L);
        item.setArchiveDccProjectCodeIdSnapshot(3000L);
        item.setArchiveFileTypeTaxonomyIdSnapshot(9100L);
        item.setArchiveChangeTypeSnapshot("NEW");
        item.setArchiveFileNameSnapshot("Archive Success.pdf");
        item.setArchiveFileNumberSnapshot("DCC-UCF-0001");
        item.setArchiveVersionNoSnapshot("V1.0");
        item.setArchiveEffectiveDateSnapshot(LocalDate.of(2026, 8, 3));
        item.setArchiveRemarkSnapshot("NAS uncontrolled import source: QMS/PRJ-20260728/archive-success.pdf");
        when(taskMapper.selectById(8207L)).thenReturn(task);
        when(auditFileMapper.selectById(707L)).thenReturn(auditFile);
        when(taskItemMapper.selectById(9307L)).thenReturn(item);
        when(nasBrowserService.readFile("QMS/PRJ-20260728/archive-success.pdf"))
                .thenReturn(new NasFileReadResult("archive-success.pdf",
                        "QMS/PRJ-20260728/archive-success.pdf",
                        "application/pdf", "pdf".getBytes(StandardCharsets.UTF_8)));
        when(fileService.createFileAndReturnId(any(byte[].class), eq("archive-success.pdf"),
                eq("dcc/original"), eq("application/pdf"))).thenReturn(5107L);
        when(workflowService.submitControlledFileWithoutApproval(eq(99L), any(DccControlledFileSubmitReqVO.class)))
                .thenReturn(6107L);
        stubAggregatedTaskItemSummary(() -> List.of(item));

        DccControlledFileNasTransferRespVO response = transferService.recordUncontrolledImportLocalWriteResult(
                99L, 8207L, 707L, localWriteResultReq("sig-archive-success",
                        "PRJ-20260728/Design/archive-success.pdf", "LOCAL_WRITTEN", null, null));

        assertEquals(8207L, response.getTaskId());
        verify(nasBrowserService).readFile("QMS/PRJ-20260728/archive-success.pdf");
        verify(fileService).createFileAndReturnId(any(byte[].class), eq("archive-success.pdf"),
                eq("dcc/original"), eq("application/pdf"));
        ArgumentCaptor<DccControlledFileSubmitReqVO> submitCaptor =
                ArgumentCaptor.forClass(DccControlledFileSubmitReqVO.class);
        verify(workflowService).submitControlledFileWithoutApproval(eq(99L), submitCaptor.capture());
        DccControlledFileSubmitReqVO submitReqVO = submitCaptor.getValue();
        assertEquals(9101L, submitReqVO.getCategoryId());
        assertEquals(9201L, submitReqVO.getDirectoryId());
        assertEquals(3000L, submitReqVO.getDccProjectCodeId());
        assertEquals(9100L, submitReqVO.getFileTypeTaxonomyId());
        assertEquals("NEW", submitReqVO.getChangeType());
        assertEquals("Archive Success.pdf", submitReqVO.getFileName());
        assertEquals("DCC-UCF-0001", submitReqVO.getFileNumber());
        assertEquals("V1.0", submitReqVO.getVersionNo());
        assertEquals(LocalDate.of(2026, 8, 3), submitReqVO.getEffectiveDate());
        assertEquals("NAS uncontrolled import source: QMS/PRJ-20260728/archive-success.pdf",
                submitReqVO.getRemark());
        assertEquals(5107L, submitReqVO.getOriginalFileId());
        ArgumentCaptor<DccNasControlAuditFileDO> auditCaptor =
                ArgumentCaptor.forClass(DccNasControlAuditFileDO.class);
        verify(auditFileMapper).updateById(auditCaptor.capture());
        assertEquals("LOCAL_WRITTEN", auditCaptor.getValue().getDownloadStatus());
        assertEquals("ARCHIVED", auditCaptor.getValue().getArchiveStatus());
        assertEquals(6107L, auditCaptor.getValue().getControlledFileId());
        ArgumentCaptor<DccControlledFileNasTransferTaskItemDO> itemCaptor =
                ArgumentCaptor.forClass(DccControlledFileNasTransferTaskItemDO.class);
        verify(taskItemMapper).updateById(itemCaptor.capture());
        assertEquals("LOCAL_WRITTEN", itemCaptor.getValue().getLocalWriteStatus());
        assertEquals("ARCHIVED", itemCaptor.getValue().getArchiveStatus());
        assertEquals(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_COMPLETED,
                itemCaptor.getValue().getStatus());
        ArgumentCaptor<DccControlledFileNasSourceDO> nasSourceCaptor =
                ArgumentCaptor.forClass(DccControlledFileNasSourceDO.class);
        verify(nasSourceMapper).insert(nasSourceCaptor.capture());
        DccControlledFileNasSourceDO nasSource = nasSourceCaptor.getValue();
        assertEquals(6107L, nasSource.getControlledFileId());
        assertEquals("quality", nasSource.getNasShareName());
        assertEquals("QMS/PRJ-20260728/archive-success.pdf", nasSource.getNormalizedRelativePath());
        assertEquals(DccNasControlAuditServiceImpl.SOURCE_TYPE_NAS_TRANSFER, nasSource.getSourceType());
        assertEquals(DccNasControlAuditServiceImpl.SOURCE_CONFIDENCE_EXACT, nasSource.getSourceConfidence());
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
    void transfer_unboundSelectedCategoryUsesUnclassifiedDirectory() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        when(categoryMapper.selectById(900250L)).thenReturn(otherCategory());
        when(categoryDirectoryBindingMapper.selectActiveByCategoryId(900250L)).thenReturn(null);
        when(directoryMapper.selectEnabledList()).thenReturn(List.of(unclassifiedDirectory(910000L)));
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
        assertEquals(2, response.getRemainingPendingCount());
        assertEquals(3000L, readLongProperty(storedTasks.get(1000L), "dccProjectCodeId"));
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
    void processWaitingTasks_unboundSelectedCategoryUsesUnclassifiedDirectory() {
        ReflectionTestUtils.setField(transferService, "transactionManager", noopTransactionManager());
        TenantContextHolder.setTenantId(1L);
        Map<Long, DccControlledFileNasTransferTaskDO> tasks = new LinkedHashMap<>();
        Map<Long, DccControlledFileNasTransferTaskItemDO> items = new LinkedHashMap<>();
        configureSingleDirectoryTask(tasks, items, "1. QMS documents");
        when(categoryMapper.selectList()).thenReturn(List.of(otherCategory()));
        when(categoryMapper.selectById(900250L)).thenReturn(otherCategory());
        when(categoryDirectoryBindingMapper.selectList()).thenReturn(List.of());
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

        List<DccFileDirectoryDO> directories = new ArrayList<>(List.of(
                unclassifiedDirectory(910000L),
                directory(902634L, null, "1. QMS documents", 1)
        ));
        AtomicLong nextDirectoryId = new AtomicLong(920000L);
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
        when(permissionRuleMapper.selectList()).thenReturn(List.of());
        when(distributionRuleMapper.selectList()).thenReturn(List.of());
        when(trainingRuleMapper.selectList()).thenReturn(List.of());
        when(routeMapper.selectList()).thenReturn(List.of());
        when(routeNodeMapper.selectList()).thenReturn(List.of());
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
        assertEquals(1, result.getCreatedDirectoryCount());
        assertEquals(0, result.getFailedFileCount());
        DccFileDirectoryDO importedRoot = directories.stream()
                .filter(directory -> Long.valueOf(920000L).equals(directory.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(910000L, importedRoot.getParentId());
        assertEquals("1. QMS documents", importedRoot.getName());
        ArgumentCaptor<DccControlledFileSubmitReqVO> submitCaptor =
                ArgumentCaptor.forClass(DccControlledFileSubmitReqVO.class);
        verify(workflowService).submitControlledFileWithoutApproval(eq(99L), submitCaptor.capture());
        assertEquals(900250L, submitCaptor.getValue().getCategoryId());
        assertEquals(920000L, submitCaptor.getValue().getDirectoryId());
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

    private static DccNasUncontrolledImportSelectedReqVO uncontrolledImportReq(
            String idempotencyKey, DccNasUncontrolledImportSelectedReqVO.SelectedFile... selectedFiles) {
        DccNasUncontrolledImportSelectedReqVO reqVO = new DccNasUncontrolledImportSelectedReqVO();
        reqVO.setSelectionScope("EXPLICIT_SELECTED_FILES");
        reqVO.setIdempotencyKey(idempotencyKey);
        reqVO.setSelectedFiles(List.of(selectedFiles));
        return reqVO;
    }

    private static DccNasUncontrolledImportSelectedReqVO.SelectedFile selectedAuditFile(
            Long auditFileId, String sourceSignature, String localRelativePath) {
        DccNasUncontrolledImportSelectedReqVO.SelectedFile selectedFile =
                new DccNasUncontrolledImportSelectedReqVO.SelectedFile();
        selectedFile.setAuditFileId(auditFileId);
        selectedFile.setSourceSignature(sourceSignature);
        selectedFile.setLocalRelativePath(localRelativePath);
        return selectedFile;
    }

    private static DccNasOriginalPathSyncReqVO originalPathSyncReq(String idempotencyKey, String selectionScope,
                                                                   DccNasOriginalPathSyncReqVO.SelectedFile... selectedFiles) {
        DccNasOriginalPathSyncReqVO reqVO = new DccNasOriginalPathSyncReqVO();
        reqVO.setSelectionScope(selectionScope);
        reqVO.setIdempotencyKey(idempotencyKey);
        reqVO.setSelectedFiles(List.of(selectedFiles));
        return reqVO;
    }

    private static DccNasOriginalPathSyncReqVO.SelectedFile originalPathSelectedFile(
            Long auditFileId, String sourceSignature) {
        DccNasOriginalPathSyncReqVO.SelectedFile selectedFile =
                new DccNasOriginalPathSyncReqVO.SelectedFile();
        selectedFile.setAuditFileId(auditFileId);
        selectedFile.setSourceSignature(sourceSignature);
        return selectedFile;
    }

    private static DccNasUncontrolledImportLocalWriteResultReqVO localWriteResultReq(
            String sourceSignature, String localRelativePath, String status, String errorCode, String errorMessage) {
        DccNasUncontrolledImportLocalWriteResultReqVO reqVO = new DccNasUncontrolledImportLocalWriteResultReqVO();
        reqVO.setSourceSignature(sourceSignature);
        reqVO.setLocalRelativePath(localRelativePath);
        reqVO.setLocalWriteStatus(status);
        reqVO.setLocalWriteErrorCode(errorCode);
        reqVO.setLocalWriteError(errorMessage);
        return reqVO;
    }

    private static DccNasControlAuditFileDO matchedAuditFile(Long id,
                                                             String normalizedRelativePath,
                                                             String sourceSignature,
                                                             String expectedLocalRelativePath,
                                                             Long fileSize) {
        return DccNasControlAuditFileDO.builder()
                .id(id)
                .taskId(7001L)
                .nasShareName("quality")
                .normalizedRelativePath(normalizedRelativePath)
                .pathHash("hash-" + id)
                .fileName(normalizedRelativePath.substring(normalizedRelativePath.lastIndexOf('/') + 1))
                .fileSize(fileSize)
                .modifiedAt(LocalDateTime.of(2026, 8, 3, 9, 30))
                .sourceSignature(sourceSignature)
                .controlStatus("NOT_CONTROLLED")
                .classificationStatus("MATCHED")
                .matchedProjectCodeId(3000L)
                .matchedFileTypeTaxonomyId(9100L)
                .matchedFileTypeLevel1("Design")
                .classificationReason("MATCHED")
                .classificationCandidatesJson("[]")
                .expectedLocalRelativePath(expectedLocalRelativePath)
                .downloadStatus("NOT_SELECTED")
                .archiveStatus("NOT_STARTED")
                .build();
    }

    private static DccNasControlAuditFileDO pendingRecognitionAuditFile(Long id,
                                                                        String normalizedRelativePath,
                                                                        String sourceSignature,
                                                                        Long fileSize) {
        return DccNasControlAuditFileDO.builder()
                .id(id)
                .taskId(7001L)
                .nasShareName("quality")
                .rootPath("QMS")
                .normalizedRelativePath(normalizedRelativePath)
                .pathHash("hash-" + id)
                .fileName(normalizedRelativePath.substring(normalizedRelativePath.lastIndexOf('/') + 1))
                .fileSize(fileSize)
                .modifiedAt(LocalDateTime.of(2026, 8, 3, 9, 30))
                .sourceSignature(sourceSignature)
                .controlStatus("NOT_CONTROLLED")
                .classificationStatus("PENDING_RECOGNITION")
                .downloadStatus("NOT_SELECTED")
                .archiveStatus("NOT_STARTED")
                .build();
    }

    private static DccNasControlAuditFileDO pendingReviewAuditFile(Long id,
                                                                   String normalizedRelativePath,
                                                                   String sourceSignature,
                                                                   String expectedLocalRelativePath,
                                                                   Long fileSize,
                                                                   String classificationStatus,
                                                                   String classificationReason) {
        DccNasControlAuditFileDO file = matchedAuditFile(
                id, normalizedRelativePath, sourceSignature, expectedLocalRelativePath, fileSize);
        file.setClassificationStatus(classificationStatus);
        file.setClassificationReason(classificationReason);
        file.setMatchedProjectCodeId(null);
        file.setMatchedFileTypeTaxonomyId(null);
        file.setMatchedFileTypeLevel1(null);
        file.setClassificationCandidatesJson("[]");
        file.setArchiveStatus("PENDING_MANUAL_REVIEW");
        return file;
    }

    private static DccControlledFileNasTransferTaskDO uncontrolledImportTask(Long id,
                                                                             Long userId,
                                                                             Long auditTaskId,
                                                                             String idempotencyKey,
                                                                             String requestHash) {
        return DccControlledFileNasTransferTaskDO.builder()
                .id(id)
                .auditTaskId(auditTaskId)
                .operatorUserId(userId)
                .selectedNasPathsJson("[]")
                .sourceType(DccControlledFileNasTransferServiceImpl.SOURCE_TYPE_NAS_UNCONTROLLED_IMPORT)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .status(DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING)
                .expectedFileCount(1L)
                .expectedTotalBytes(120L)
                .uploadedFileCount(0L)
                .uploadedTotalBytes(0L)
                .build();
    }

    private static DccControlledFileNasTransferTaskDO originalPathSyncTask(Long id,
                                                                           Long userId,
                                                                           Long auditTaskId,
                                                                           String idempotencyKey,
                                                                           String requestHash) {
        return DccControlledFileNasTransferTaskDO.builder()
                .id(id)
                .auditTaskId(auditTaskId)
                .operatorUserId(userId)
                .selectedNasPathsJson("[]")
                .sourceType(DccControlledFileNasTransferServiceImpl.SOURCE_TYPE_NAS_ORIGINAL_PATH_SYNC)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .status(DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING)
                .expectedFileCount(1L)
                .expectedTotalBytes(3L)
                .uploadedFileCount(0L)
                .uploadedTotalBytes(0L)
                .build();
    }

    private static DccControlledFileNasTransferTaskItemDO uncontrolledImportItem(
            Long id, DccControlledFileNasTransferTaskDO task, DccNasControlAuditFileDO auditFile) {
        return DccControlledFileNasTransferTaskItemDO.builder()
                .id(id)
                .taskId(task.getId())
                .auditFileId(auditFile.getId())
                .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_FILE)
                .nasPath(auditFile.getNormalizedRelativePath())
                .itemName(auditFile.getFileName())
                .sourceSignature(auditFile.getSourceSignature())
                .classificationStatusSnapshot(auditFile.getClassificationStatus())
                .matchedProjectCodeIdSnapshot(auditFile.getMatchedProjectCodeId())
                .matchedFileTypeTaxonomyIdSnapshot(auditFile.getMatchedFileTypeTaxonomyId())
                .classificationReasonSnapshot(auditFile.getClassificationReason())
                .classificationCandidatesJsonSnapshot(auditFile.getClassificationCandidatesJson())
                .localRelativePath(auditFile.getLocalRelativePath())
                .localWriteStatus("NOT_STARTED")
                .archiveStatus("NOT_STARTED")
                .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING)
                .attemptCount(0)
                .build();
    }

    private static DccControlledFileNasTransferTaskItemDO originalPathSyncItem(
            Long id, DccControlledFileNasTransferTaskDO task, DccNasControlAuditFileDO auditFile) {
        return DccControlledFileNasTransferTaskItemDO.builder()
                .id(id)
                .taskId(task.getId())
                .auditFileId(auditFile.getId())
                .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_FILE)
                .nasPath(auditFile.getNormalizedRelativePath())
                .itemName(auditFile.getFileName())
                .sourceSignature(auditFile.getSourceSignature())
                .classificationStatusSnapshot(auditFile.getClassificationStatus())
                .localRelativePath(auditFile.getNormalizedRelativePath())
                .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING)
                .attemptCount(0)
                .build();
    }

    private static DccNasControlAuditTaskDO completedAuditTask(Long id) {
        return DccNasControlAuditTaskDO.builder()
                .id(id)
                .status(DccNasControlAuditServiceImpl.STATUS_COMPLETED)
                .nasShareName("quality")
                .tenantId(1L)
                .build();
    }

    private static String sourceSignature(String pathHash, Long fileSize, Long modifiedAtUtcEpochMillis) {
        String payload = pathHash + "|" + fileSize + "|" + modifiedAtUtcEpochMillis;
        try {
            return sha256Hex(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new AssertionError("Cannot compute source signature", exception);
        }
    }

    private static String uncontrolledImportRequestHash(Long auditTaskId,
                                                        DccNasUncontrolledImportSelectedReqVO reqVO) {
        StringBuilder raw = new StringBuilder("DCC_NAS_UNCONTROLLED_IMPORT");
        appendLengthPrefixed(raw, String.valueOf(auditTaskId));
        reqVO.getSelectedFiles().stream()
                .sorted(Comparator.comparing(DccNasUncontrolledImportSelectedReqVO.SelectedFile::getAuditFileId))
                .forEach(selectedFile -> {
                    appendLengthPrefixed(raw, String.valueOf(selectedFile.getAuditFileId()));
                    appendLengthPrefixed(raw, selectedFile.getSourceSignature());
                    appendLengthPrefixed(raw, selectedFile.getLocalRelativePath());
                });
        try {
            return sha256Hex(raw.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new AssertionError("Cannot compute uncontrolled import request hash", exception);
        }
    }

    private static void appendLengthPrefixed(StringBuilder builder, String value) {
        builder.append('|').append(value.length()).append(':').append(value);
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

    private static DccFileDirectoryDO unclassifiedDirectory(Long id) {
        return DccFileDirectoryDO.builder()
                .id(id)
                .parentId(null)
                .code(DccUploadDirectoryResolver.UNCLASSIFIED_UPLOAD_DIRECTORY_CODE)
                .name("未分类")
                .active(Boolean.TRUE)
                .sort(99)
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
                .auditTaskId(source.getAuditTaskId())
                .operatorUserId(source.getOperatorUserId())
                .templateCategoryId(source.getTemplateCategoryId())
                .dccProjectCodeId(source.getDccProjectCodeId())
                .productMasterId(source.getProductMasterId())
                .effectiveDate(source.getEffectiveDate())
                .selectedNasPathsJson(source.getSelectedNasPathsJson())
                .sourceType(source.getSourceType())
                .idempotencyKey(source.getIdempotencyKey())
                .requestHash(source.getRequestHash())
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
                .auditFileId(source.getAuditFileId())
                .parentItemId(source.getParentItemId())
                .itemType(source.getItemType())
                .nasPath(source.getNasPath())
                .itemName(source.getItemName())
                .sourceFileId(source.getSourceFileId())
                .sourceSignature(source.getSourceSignature())
                .classificationStatusSnapshot(source.getClassificationStatusSnapshot())
                .matchedProjectCodeIdSnapshot(source.getMatchedProjectCodeIdSnapshot())
                .matchedFileTypeTaxonomyIdSnapshot(source.getMatchedFileTypeTaxonomyIdSnapshot())
                .matchedFileTypeLevel1Snapshot(source.getMatchedFileTypeLevel1Snapshot())
                .matchedFileTypeLevel2Snapshot(source.getMatchedFileTypeLevel2Snapshot())
                .matchedFileTypeLevel3Snapshot(source.getMatchedFileTypeLevel3Snapshot())
                .matchedFileTypeLevel4Snapshot(source.getMatchedFileTypeLevel4Snapshot())
                .matchedFileTypeLevel5Snapshot(source.getMatchedFileTypeLevel5Snapshot())
                .classificationReasonSnapshot(source.getClassificationReasonSnapshot())
                .classificationCandidatesJsonSnapshot(source.getClassificationCandidatesJsonSnapshot())
                .localRelativePath(source.getLocalRelativePath())
                .localWriteStatus(source.getLocalWriteStatus())
                .localWriteErrorCode(source.getLocalWriteErrorCode())
                .localWriteError(source.getLocalWriteError())
                .archiveStatus(source.getArchiveStatus())
                .archiveErrorCode(source.getArchiveErrorCode())
                .archiveError(source.getArchiveError())
                .archiveCategoryIdSnapshot(source.getArchiveCategoryIdSnapshot())
                .archiveDirectoryIdSnapshot(source.getArchiveDirectoryIdSnapshot())
                .archiveDccProjectCodeIdSnapshot(source.getArchiveDccProjectCodeIdSnapshot())
                .archiveFileTypeTaxonomyIdSnapshot(source.getArchiveFileTypeTaxonomyIdSnapshot())
                .archiveChangeTypeSnapshot(source.getArchiveChangeTypeSnapshot())
                .archiveFileNameSnapshot(source.getArchiveFileNameSnapshot())
                .archiveFileNumberSnapshot(source.getArchiveFileNumberSnapshot())
                .archiveVersionNoSnapshot(source.getArchiveVersionNoSnapshot())
                .archiveEffectiveDateSnapshot(source.getArchiveEffectiveDateSnapshot())
                .archiveRemarkSnapshot(source.getArchiveRemarkSnapshot())
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
                .fileTypeTaxonomyId(source.getFileTypeTaxonomyId())
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
