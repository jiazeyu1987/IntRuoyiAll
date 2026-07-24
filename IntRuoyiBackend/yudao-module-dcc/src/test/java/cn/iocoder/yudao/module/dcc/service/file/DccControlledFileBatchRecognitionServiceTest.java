package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileProjectCodeRecognitionRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileBatchRecognitionCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileBatchRecognitionTaskRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeAssociatedFileAiCategoryRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileBatchRecognitionTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRecognitionFailureSummaryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRecognitionClaimDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRecognitionRecordDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileBatchRecognitionTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRecognitionClaimMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRecognitionRecordMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileBatchRecognitionServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileBatchRecognitionTaskMapper taskMapper;
    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Mock
    private DccFileDirectoryMapper directoryMapper;
    @Mock
    private DccControlledFileRecognitionRecordMapper recognitionRecordMapper;
    @Mock
    private DccControlledFileRecognitionClaimMapper recognitionClaimMapper;
    @Mock
    private DccControlledFileQueryService queryService;
    @Mock
    private DccControlledFileProjectCodeRecognitionService projectCodeRecognitionService;
    @Mock
    private DccProjectCodeMapper projectCodeMapper;
    @Mock
    private DccProjectCodeService projectCodeService;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private DccProjectCodeRecognitionProperties recognitionProperties;

    @InjectMocks
    private DccControlledFileBatchRecognitionServiceImpl service;

    @Test
    void createTaskReturnsActiveTaskWhenExistingTaskRunning() {
        DccControlledFileBatchRecognitionTaskDO existing = task(100L);
        existing.setStatus(DccControlledFileBatchRecognitionServiceImpl.TASK_STATUS_RUNNING);
        when(taskMapper.selectActiveTask(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_BASIC_INFO))
                .thenReturn(existing);

        DccControlledFileBatchRecognitionTaskRespVO respVO = service.createTask(99L, createReq());

        assertEquals(100L, respVO.getTaskId());
        verify(queryService, never()).listControlledFileBrowserCandidates(any(), any());
    }

    @Test
    void createTaskPersistsCurrentScopeSnapshotAndZeroCandidateCounts() {
        List<DccFileDirectoryDO> directories = List.of(
                DccFileDirectoryDO.builder().id(1L).name("QMS documents").build(),
                DccFileDirectoryDO.builder().id(2L).parentId(1L).name("3-1 RE 可编辑").build()
        );
        when(recognitionProperties.getVersion()).thenReturn("project-code-v1");
        when(taskMapper.selectActiveTask(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_BASIC_INFO))
                .thenReturn(null);
        when(directoryMapper.selectList()).thenReturn(directories);
        when(queryService.listControlledFileBrowserCandidates(eq(99L), any(DccControlledFilePageReqVO.class)))
                .thenReturn(List.of());
        final DccControlledFileBatchRecognitionTaskDO[] holder = new DccControlledFileBatchRecognitionTaskDO[1];
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO task = invocation.getArgument(0);
            task.setId(200L);
            holder[0] = cloneTask(task);
            return 1;
        }).when(taskMapper).insert(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(taskMapper.selectById(200L)).thenAnswer(invocation -> holder[0]);

        DccControlledFileBatchRecognitionTaskRespVO respVO = service.createTask(99L, createReq());

        assertEquals(200L, respVO.getTaskId());
        assertEquals("CURRENT", respVO.getScope());
        assertEquals(2L, respVO.getDirectoryId());
        assertEquals("QMS documents/3-1 RE 可编辑", respVO.getDirectoryPath());
        assertEquals(0L, respVO.getTotalCount());
        assertEquals("COMPLETED", respVO.getStatus());
        assertEquals("project-code-v1", respVO.getRecognitionVersionSnapshot());
        ArgumentCaptor<DccControlledFilePageReqVO> pageReqCaptor = ArgumentCaptor.forClass(DccControlledFilePageReqVO.class);
        verify(queryService).listControlledFileBrowserCandidates(eq(99L), pageReqCaptor.capture());
        assertEquals(2L, pageReqCaptor.getValue().getDirectoryId());
        assertEquals(true, pageReqCaptor.getValue().getIncludeDescendantDirectories());
        assertEquals(true, pageReqCaptor.getValue().getLatestVersionOnly());
    }

    @Test
    void createTaskPersistsWorkerCountSnapshotAndDeduplicatesCandidates() {
        when(recognitionProperties.getVersion()).thenReturn("project-code-v1");
        when(taskMapper.selectActiveTask(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_BASIC_INFO))
                .thenReturn(null);
        when(directoryMapper.selectList()).thenReturn(List.of(
                DccFileDirectoryDO.builder().id(1L).name("父目录").build(),
                DccFileDirectoryDO.builder().id(2L).parentId(1L).name("子目录").build()
        ));
        when(queryService.listControlledFileBrowserCandidates(eq(99L), any(DccControlledFilePageReqVO.class)))
                .thenReturn(List.of(
                        DccControlledFileDO.builder().id(900L).build(),
                        DccControlledFileDO.builder().id(901L).build(),
                        DccControlledFileDO.builder().id(900L).build()));
        DccControlledFileBatchRecognitionCreateReqVO reqVO = createReq();
        reqVO.setWorkerCount(5);
        final DccControlledFileBatchRecognitionTaskDO[] holder = new DccControlledFileBatchRecognitionTaskDO[1];
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO task = invocation.getArgument(0);
            task.setId(201L);
            holder[0] = cloneTask(task);
            return 1;
        }).when(taskMapper).insert(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(taskMapper.selectById(201L)).thenAnswer(invocation -> holder[0]);

        TenantContextHolder.setTenantId(0L);
        DccControlledFileBatchRecognitionTaskRespVO respVO;
        try {
            respVO = service.createTask(99L, reqVO);
        } finally {
            TenantContextHolder.clear();
        }

        assertEquals(201L, respVO.getTaskId());
        assertEquals(5, respVO.getWorkerCount());
        assertEquals(DccControlledFileBatchRecognitionServiceImpl.EXISTING_RECORD_POLICY_SKIP_ALL_EXISTING,
                holder[0].getExistingRecordPolicy());
        assertEquals(DccControlledFileBatchRecognitionServiceImpl.EXISTING_RECORD_POLICY_SKIP_ALL_EXISTING,
                respVO.getExistingRecordPolicy());
        assertEquals("[900,901]", holder[0].getCandidateIdsJson());
        assertEquals(2L, holder[0].getTotalCount());
        assertEquals(2L, holder[0].getRemainingCount());
    }

    @Test
    void createTaskFailsFastWithDccConfigErrorWhenRecognitionVersionMissing() {
        when(recognitionProperties.getVersion()).thenReturn(" ");
        when(taskMapper.selectActiveTask(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_BASIC_INFO))
                .thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createTask(99L, createReq()));

        assertEquals(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING.getCode(), exception.getCode());
        verify(queryService, never()).listControlledFileBrowserCandidates(any(), any());
        verify(taskMapper, never()).insert(any(DccControlledFileBatchRecognitionTaskDO.class));
    }

    @Test
    void createTaskFailsFastWhenRecognitionVersionIsUnresolvedPlaceholder() {
        when(recognitionProperties.getVersion()).thenReturn("${DCC_PROJECT_CODE_RECOGNITION_VERSION}");
        when(taskMapper.selectActiveTask(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_BASIC_INFO))
                .thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createTask(99L, createReq()));

        assertEquals(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING.getCode(), exception.getCode());
        verify(queryService, never()).listControlledFileBrowserCandidates(any(), any());
        verify(taskMapper, never()).insert(any(DccControlledFileBatchRecognitionTaskDO.class));
    }

    @Test
    void recoverInterruptedTasksOnStartupReleasesClaimsOwnedByRunningTasks() {
        DccControlledFileBatchRecognitionTaskDO runningTask = task(316L);
        runningTask.setStatus(DccControlledFileBatchRecognitionServiceImpl.TASK_STATUS_RUNNING);
        when(taskMapper.selectRunningTasks()).thenReturn(List.of(runningTask));
        when(taskMapper.requeueRunningTasksOnStartup()).thenReturn(1);

        service.recoverInterruptedTasksOnStartup();

        verify(recognitionClaimMapper).releaseClaimsByTaskId(316L);
        verify(taskMapper).requeueRunningTasksOnStartup();
    }

    @Test
    void recoverInterruptedTasksOnStartupReleasesClaimsOwnedByTerminalTasks() {
        when(recognitionClaimMapper.releaseClaimsOwnedByTerminalTasks()).thenReturn(127);
        when(taskMapper.selectRunningTasks()).thenReturn(List.of());

        service.recoverInterruptedTasksOnStartup();

        verify(recognitionClaimMapper).releaseClaimsOwnedByTerminalTasks();
        verify(taskMapper).requeueRunningTasksOnStartup();
    }

    @Test
    void createFileCategoryTaskUsesIndependentMutexAndServerSideCandidates() {
        DccControlledFileBatchRecognitionCreateReqVO reqVO = createReq();
        reqVO.setRecognitionType(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_FILE_CATEGORY);
        reqVO.setScope("GLOBAL");
        reqVO.setDirectoryId(null);
        when(taskMapper.selectActiveTask(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_FILE_CATEGORY))
                .thenReturn(null);
        when(projectCodeMapper.selectList()).thenReturn(List.of(
                DccProjectCodeDO.builder().id(700L).build(),
                DccProjectCodeDO.builder().id(701L).build()));
        when(projectCodeService.getAssociatedFileAiCategoryCandidates(99L, 700L)).thenReturn(List.of(
                DccProjectCodeAssociatedFileAiCategoryRespVO.builder().fileId(900L).build()));
        when(projectCodeService.getAssociatedFileAiCategoryCandidates(99L, 701L)).thenReturn(List.of(
                DccProjectCodeAssociatedFileAiCategoryRespVO.builder().fileId(901L).build(),
                DccProjectCodeAssociatedFileAiCategoryRespVO.builder().fileId(900L).build()));
        final DccControlledFileBatchRecognitionTaskDO[] holder = new DccControlledFileBatchRecognitionTaskDO[1];
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO task = invocation.getArgument(0);
            task.setId(320L);
            holder[0] = cloneTask(task);
            return 1;
        }).when(taskMapper).insert(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(taskMapper.selectById(320L)).thenAnswer(invocation -> holder[0]);

        TenantContextHolder.setTenantId(0L);
        DccControlledFileBatchRecognitionTaskRespVO respVO;
        try {
            respVO = service.createTask(99L, reqVO);
        } finally {
            TenantContextHolder.clear();
        }

        assertEquals(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_FILE_CATEGORY,
                holder[0].getRecognitionType());
        assertEquals(DccControlledFileBatchRecognitionServiceImpl.FILE_CATEGORY_RECOGNITION_VERSION,
                holder[0].getRecognitionVersionSnapshot());
        assertEquals("[900,901]", holder[0].getCandidateIdsJson());
        assertEquals(2L, respVO.getTotalCount());
        verify(queryService, never()).listControlledFileBrowserCandidates(any(), any());
    }

    @Test
    void processFileNumberTaskMatchesFileNameBaseToEnabledProjectCodeAndClearsUnmatched() {
        DccControlledFileBatchRecognitionTaskDO task = task(331L);
        task.setRecognitionType(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_FILE_NUMBER);
        task.setRecognitionVersionSnapshot(DccControlledFileBatchRecognitionServiceImpl.FILE_NUMBER_RECOGNITION_VERSION);
        task.setCandidateIdsJson("[900,901,902]");
        task.setTotalCount(3L);
        task.setRemainingCount(3L);
        task.setOverwriteExisting(true);
        task.setExistingRecordPolicy(DccControlledFileBatchRecognitionServiceImpl.EXISTING_RECORD_POLICY_OVERWRITE_ALL);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(331L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(331L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            return 1;
        });
        doAnswer(invocation -> {
            applyTaskUpdate(holder[0], invocation.getArgument(0));
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(projectCodeMapper.selectEnabledList()).thenReturn(List.of(
                DccProjectCodeDO.builder().id(700L).projectName("ABC项目").projectCode("ABC-CODE").build(),
                DccProjectCodeDO.builder().id(701L).projectName("DEF项目").projectCode("DEF-CODE").build()));
        when(controlledFileMapper.selectBatchIds(List.of(900L, 901L, 902L))).thenReturn(List.of(
                DccControlledFileDO.builder().id(900L).tenantId(0L).masterId(500L)
                        .fileName(" ABC项目.pdf ").fileNumber("OLD").build(),
                DccControlledFileDO.builder().id(901L).tenantId(0L).masterId(501L)
                        .fileName("未匹配.docx").fileNumber("OLD2").build(),
                DccControlledFileDO.builder().id(902L).tenantId(0L).masterId(502L)
                        .fileName("DEF项目").fileNumber("").build()));
        when(controlledFileMapper.updateBatch(any(Collection.class), eq(500))).thenReturn(true);
        when(controlledFileMasterMapper.updateBatch(any(Collection.class), eq(500))).thenReturn(true);

        service.processWaitingTasks();

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(3L, holder[0].getProcessedCount());
        assertEquals(3L, holder[0].getSuccessCount());
        assertEquals(0L, holder[0].getFailedCount());
        assertEquals(0L, holder[0].getSkippedExistingCount());
        ArgumentCaptor<Collection<DccControlledFileDO>> fileUpdatesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(controlledFileMapper).updateBatch(fileUpdatesCaptor.capture(), eq(500));
        List<DccControlledFileDO> fileUpdates = new ArrayList<>(fileUpdatesCaptor.getValue());
        assertEquals("ABC-CODE", fileUpdates.get(0).getFileNumber());
        assertEquals("", fileUpdates.get(1).getFileNumber());
        assertEquals("DEF-CODE", fileUpdates.get(2).getFileNumber());
        ArgumentCaptor<Collection<DccControlledFileMasterDO>> masterUpdatesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(controlledFileMasterMapper).updateBatch(masterUpdatesCaptor.capture(), eq(500));
        List<DccControlledFileMasterDO> masterUpdates = new ArrayList<>(masterUpdatesCaptor.getValue());
        assertEquals("ABC-CODE", masterUpdates.get(0).getFileNumber());
        assertEquals("", masterUpdates.get(1).getFileNumber());
        assertEquals("DEF-CODE", masterUpdates.get(2).getFileNumber());
        verify(projectCodeRecognitionService, never()).recognizeProjectCode(any(), any(), any());
        verify(recognitionRecordMapper, never()).upsert(any());
    }

    @Test
    void processFileNumberTaskMatchesContainedProjectCodeBeforeContainedProjectName() {
        DccControlledFileBatchRecognitionTaskDO task = task(334L);
        task.setRecognitionType(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_FILE_NUMBER);
        task.setRecognitionVersionSnapshot(DccControlledFileBatchRecognitionServiceImpl.FILE_NUMBER_RECOGNITION_VERSION);
        task.setCandidateIdsJson("[930,931,932,933]");
        task.setTotalCount(4L);
        task.setRemainingCount(4L);
        task.setOverwriteExisting(true);
        task.setExistingRecordPolicy(DccControlledFileBatchRecognitionServiceImpl.EXISTING_RECORD_POLICY_OVERWRITE_ALL);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(334L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(334L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            return 1;
        });
        doAnswer(invocation -> {
            applyTaskUpdate(holder[0], invocation.getArgument(0));
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(projectCodeMapper.selectEnabledList()).thenReturn(List.of(
                DccProjectCodeDO.builder().id(700L)
                        .projectName("一次性使用导管鞘套装").projectCode("IK").build(),
                DccProjectCodeDO.builder().id(701L)
                        .projectName("一次性使用导管鞘套装（FDA)").projectCode("IKFDA").build(),
                DccProjectCodeDO.builder().id(702L)
                        .projectName("ABC项目").projectCode("ABC-CODE").build()));
        when(controlledFileMapper.selectBatchIds(List.of(930L, 931L, 932L, 933L))).thenReturn(List.of(
                DccControlledFileDO.builder().id(930L).tenantId(0L).masterId(530L)
                        .fileName("DMR-IKFDA-001 A0一次性使用导管鞘套装技术要求.docx")
                        .fileNumber("OLD").build(),
                DccControlledFileDO.builder().id(931L).tenantId(0L).masterId(531L)
                        .fileName("技术要求-ABC项目-A0.pdf").fileNumber("OLD2").build(),
                DccControlledFileDO.builder().id(932L).tenantId(0L).masterId(532L)
                        .fileName("未匹配.docx").fileNumber("OLD3").build(),
                DccControlledFileDO.builder().id(933L).tenantId(0L).masterId(533L)
                        .fileName("DMRIKFDA001A0一次性使用导管鞘套装技术要求.docx")
                        .fileNumber("OLD4").build()));
        when(controlledFileMapper.updateBatch(any(Collection.class), eq(500))).thenReturn(true);
        when(controlledFileMasterMapper.updateBatch(any(Collection.class), eq(500))).thenReturn(true);

        service.processWaitingTasks();

        assertEquals("COMPLETED", holder[0].getStatus());
        ArgumentCaptor<Collection<DccControlledFileDO>> fileUpdatesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(controlledFileMapper).updateBatch(fileUpdatesCaptor.capture(), eq(500));
        List<DccControlledFileDO> fileUpdates = new ArrayList<>(fileUpdatesCaptor.getValue());
        assertEquals("IKFDA", fileUpdates.get(0).getFileNumber());
        assertEquals("ABC-CODE", fileUpdates.get(1).getFileNumber());
        assertEquals("", fileUpdates.get(2).getFileNumber());
        assertEquals("IKFDA", fileUpdates.get(3).getFileNumber());
    }

    @Test
    void processFileNumberTaskSkipsFilesWithExistingNumberWhenOverwriteDisabled() {
        DccControlledFileBatchRecognitionTaskDO task = task(332L);
        task.setRecognitionType(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_FILE_NUMBER);
        task.setRecognitionVersionSnapshot(DccControlledFileBatchRecognitionServiceImpl.FILE_NUMBER_RECOGNITION_VERSION);
        task.setCandidateIdsJson("[910,911]");
        task.setTotalCount(2L);
        task.setRemainingCount(2L);
        task.setOverwriteExisting(false);
        task.setExistingRecordPolicy(DccControlledFileBatchRecognitionServiceImpl.EXISTING_RECORD_POLICY_SKIP_ALL_EXISTING);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(332L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(332L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            return 1;
        });
        doAnswer(invocation -> {
            applyTaskUpdate(holder[0], invocation.getArgument(0));
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(projectCodeMapper.selectEnabledList()).thenReturn(List.of(
                DccProjectCodeDO.builder().id(700L).projectName("ABC项目").projectCode("ABC-CODE").build()));
        when(controlledFileMapper.selectBatchIds(List.of(910L, 911L))).thenReturn(List.of(
                DccControlledFileDO.builder().id(910L).tenantId(0L).masterId(510L)
                        .fileName("ABC项目.pdf").fileNumber("EXISTING").build(),
                DccControlledFileDO.builder().id(911L).tenantId(0L).masterId(511L)
                        .fileName("ABC项目.pdf").fileNumber("").build()));
        when(controlledFileMapper.updateBatch(any(Collection.class), eq(500))).thenReturn(true);
        when(controlledFileMasterMapper.updateBatch(any(Collection.class), eq(500))).thenReturn(true);

        service.processWaitingTasks();

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(2L, holder[0].getProcessedCount());
        assertEquals(1L, holder[0].getSuccessCount());
        assertEquals(1L, holder[0].getSkippedExistingCount());
        ArgumentCaptor<Collection<DccControlledFileDO>> fileUpdatesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(controlledFileMapper).updateBatch(fileUpdatesCaptor.capture(), eq(500));
        List<DccControlledFileDO> fileUpdates = new ArrayList<>(fileUpdatesCaptor.getValue());
        assertEquals(1, fileUpdates.size());
        assertEquals(911L, fileUpdates.get(0).getId());
        assertEquals("ABC-CODE", fileUpdates.get(0).getFileNumber());
    }

    @Test
    void processFileNumberTaskFailsFastWhenProjectNameMapsToMultipleProjectCodes() {
        DccControlledFileBatchRecognitionTaskDO task = task(333L);
        task.setRecognitionType(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_FILE_NUMBER);
        task.setRecognitionVersionSnapshot(DccControlledFileBatchRecognitionServiceImpl.FILE_NUMBER_RECOGNITION_VERSION);
        task.setCandidateIdsJson("[920]");
        task.setTotalCount(1L);
        task.setRemainingCount(1L);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(333L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(333L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            return 1;
        });
        doAnswer(invocation -> {
            applyTaskUpdate(holder[0], invocation.getArgument(0));
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(projectCodeMapper.selectEnabledList()).thenReturn(List.of(
                DccProjectCodeDO.builder().id(700L).projectName("ABC项目").projectCode("ABC-CODE").build(),
                DccProjectCodeDO.builder().id(701L).projectName("ABC项目").projectCode("OTHER-CODE").build()));

        service.processWaitingTasks();

        assertEquals("FAILED", holder[0].getStatus());
        assertEquals("enabled DCC project name maps to multiple project codes: ABC项目", holder[0].getLastFailureMessage());
        verify(controlledFileMapper, never()).selectBatchIds(any(Collection.class));
        verify(controlledFileMapper, never()).updateBatch(any(Collection.class), any(Integer.class));
        verify(controlledFileMasterMapper, never()).updateBatch(any(Collection.class), any(Integer.class));
    }

    @Test
    void createTaskReturnsActiveTaskWhenDatabaseUniqueGuardWinsRace() {
        DccControlledFileBatchRecognitionTaskDO existing = task(101L);
        existing.setStatus(DccControlledFileBatchRecognitionServiceImpl.TASK_STATUS_WAITING);
        when(recognitionProperties.getVersion()).thenReturn("project-code-v1");
        when(taskMapper.selectActiveTask(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_BASIC_INFO))
                .thenReturn(null, existing);
        when(directoryMapper.selectList()).thenReturn(List.of(
                DccFileDirectoryDO.builder().id(1L).name("QMS documents").build(),
                DccFileDirectoryDO.builder().id(2L).parentId(1L).name("3-1 RE 可编辑").build()
        ));
        when(queryService.listControlledFileBrowserCandidates(eq(99L), any(DccControlledFilePageReqVO.class)))
                .thenReturn(List.of(DccControlledFileDO.builder().id(900L).build()));
        when(taskMapper.insert(any(DccControlledFileBatchRecognitionTaskDO.class)))
                .thenThrow(new DuplicateKeyException(
                        "Duplicate entry for key 'uk_dcc_batch_recognition_task_active_type'"));

        DccControlledFileBatchRecognitionTaskRespVO respVO = service.createTask(99L, createReq());

        assertEquals(101L, respVO.getTaskId());
        verify(taskMapper, org.mockito.Mockito.times(2))
                .selectActiveTask(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_BASIC_INFO);
    }

    @Test
    void createTaskRethrowsUnrelatedDuplicateKeyException() {
        when(recognitionProperties.getVersion()).thenReturn("project-code-v1");
        when(taskMapper.selectActiveTask(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_BASIC_INFO))
                .thenReturn(null);
        when(directoryMapper.selectList()).thenReturn(List.of(
                DccFileDirectoryDO.builder().id(1L).name("QMS documents").build(),
                DccFileDirectoryDO.builder().id(2L).parentId(1L).name("3-1 RE 可编辑").build()
        ));
        when(queryService.listControlledFileBrowserCandidates(eq(99L), any(DccControlledFilePageReqVO.class)))
                .thenReturn(List.of(DccControlledFileDO.builder().id(900L).build()));
        DuplicateKeyException duplicateKeyException = new DuplicateKeyException(
                "Duplicate entry for key 'uk_unrelated_constraint'");
        when(taskMapper.insert(any(DccControlledFileBatchRecognitionTaskDO.class)))
                .thenThrow(duplicateKeyException);

        DuplicateKeyException thrown = assertThrows(DuplicateKeyException.class,
                () -> service.createTask(99L, createReq()));

        assertEquals(duplicateKeyException, thrown);
        verify(taskMapper).selectActiveTask(
                DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_BASIC_INFO);
    }

    @Test
    void createTaskRethrowsActiveUniqueConflictWhenWinnerCannotBeRead() {
        when(recognitionProperties.getVersion()).thenReturn("project-code-v1");
        when(taskMapper.selectActiveTask(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_BASIC_INFO))
                .thenReturn(null);
        when(directoryMapper.selectList()).thenReturn(List.of(
                DccFileDirectoryDO.builder().id(1L).name("QMS documents").build(),
                DccFileDirectoryDO.builder().id(2L).parentId(1L).name("3-1 RE 可编辑").build()
        ));
        when(queryService.listControlledFileBrowserCandidates(eq(99L), any(DccControlledFilePageReqVO.class)))
                .thenReturn(List.of(DccControlledFileDO.builder().id(900L).build()));
        DuplicateKeyException duplicateKeyException = new DuplicateKeyException(
                "Duplicate entry for key 'uk_dcc_batch_recognition_task_active_type'");
        when(taskMapper.insert(any(DccControlledFileBatchRecognitionTaskDO.class)))
                .thenThrow(duplicateKeyException);

        DuplicateKeyException thrown = assertThrows(DuplicateKeyException.class,
                () -> service.createTask(99L, createReq()));

        assertEquals(duplicateKeyException, thrown);
        verify(taskMapper, org.mockito.Mockito.times(2))
                .selectActiveTask(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_BASIC_INFO);
    }

    @Test
    void processFileCategoryTaskPersistsDistinctOutcomeCounters() {
        DccControlledFileBatchRecognitionTaskDO task = task(321L);
        task.setRecognitionType(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_FILE_CATEGORY);
        task.setRecognitionVersionSnapshot(DccControlledFileBatchRecognitionServiceImpl.FILE_CATEGORY_RECOGNITION_VERSION);
        task.setCandidateIdsJson("[900,901,902,903,904]");
        task.setTotalCount(5L);
        task.setRemainingCount(5L);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(321L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(321L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            return 1;
        });
        doAnswer(invocation -> {
            applyTaskUpdate(holder[0], invocation.getArgument(0));
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        for (long fileId = 900L; fileId <= 904L; fileId++) {
            when(controlledFileMapper.selectById(fileId)).thenReturn(DccControlledFileDO.builder()
                    .id(fileId).tenantId(0L).dccProjectCodeId(700L).build());
        }
        when(recognitionClaimMapper.tryClaimBasicInfo(any(), any(), eq("FILE_CATEGORY"), any(), any(), any()))
                .thenReturn(1);
        when(projectCodeService.classifyAssociatedFileByName(99L, 700L, 900L)).thenReturn(
                DccProjectCodeAssociatedFileAiCategoryRespVO.builder()
                        .fileId(900L).matched(true).classificationStatus("MATCHED")
                        .targetStage("设计").targetFileType("图纸").build());
        when(projectCodeService.classifyAssociatedFileByName(99L, 700L, 901L)).thenReturn(
                DccProjectCodeAssociatedFileAiCategoryRespVO.builder()
                        .fileId(901L).matched(false).classificationStatus("UNCLASSIFIED")
                        .classificationMessage("未匹配").build());
        when(projectCodeService.classifyAssociatedFileByName(99L, 700L, 902L)).thenReturn(
                DccProjectCodeAssociatedFileAiCategoryRespVO.builder()
                        .fileId(902L).matched(false).classificationStatus("AMBIGUOUS")
                        .classificationMessage("多个规则命中").build());
        when(projectCodeService.classifyAssociatedFileByName(99L, 700L, 903L)).thenThrow(
                new ServiceException(1_080_000_157, "concurrent modification"));
        when(projectCodeService.classifyAssociatedFileByName(99L, 700L, 904L)).thenThrow(
                new IllegalStateException("ai gateway timeout"));

        service.processWaitingTasks();

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(5L, holder[0].getProcessedCount());
        assertEquals(1L, holder[0].getSuccessCount());
        assertEquals(1L, holder[0].getUnclassifiedCount());
        assertEquals(1L, holder[0].getAmbiguousCount());
        assertEquals(1L, holder[0].getConflictCount());
        assertEquals(1L, holder[0].getFailedCount());
        ArgumentCaptor<DccControlledFileRecognitionRecordDO> recordCaptor =
                ArgumentCaptor.forClass(DccControlledFileRecognitionRecordDO.class);
        verify(recognitionRecordMapper, org.mockito.Mockito.times(5)).upsert(recordCaptor.capture());
        DccControlledFileRecognitionRecordDO failedRecord = recordCaptor.getAllValues().stream()
                .filter(record -> Long.valueOf(904L).equals(record.getControlledFileId()))
                .findFirst()
                .orElseThrow();
        assertEquals("FAILED", failedRecord.getStatus());
        assertEquals("AI_CLASSIFICATION", failedRecord.getFailureStage());
        assertEquals("AI_REQUEST_FAILED", failedRecord.getFailureCode());
        assertEquals("ai gateway timeout", failedRecord.getFailureMessage());
    }

    @Test
    void createTaskPersistsRetryFailedPolicyWithoutLegacyOverwriteFlag() {
        when(recognitionProperties.getVersion()).thenReturn("project-code-v1");
        when(taskMapper.selectActiveTask(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_BASIC_INFO))
                .thenReturn(null);
        when(directoryMapper.selectList()).thenReturn(List.of(
                DccFileDirectoryDO.builder().id(1L).name("父目录").build(),
                DccFileDirectoryDO.builder().id(2L).parentId(1L).name("子目录").build()
        ));
        when(queryService.listControlledFileBrowserCandidates(eq(99L), any(DccControlledFilePageReqVO.class)))
                .thenReturn(List.of(DccControlledFileDO.builder().id(900L).build()));
        DccControlledFileBatchRecognitionCreateReqVO reqVO = createReq();
        reqVO.setExistingRecordPolicy(DccControlledFileBatchRecognitionServiceImpl.EXISTING_RECORD_POLICY_RETRY_FAILED);
        final DccControlledFileBatchRecognitionTaskDO[] holder = new DccControlledFileBatchRecognitionTaskDO[1];
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO task = invocation.getArgument(0);
            task.setId(202L);
            holder[0] = cloneTask(task);
            return 1;
        }).when(taskMapper).insert(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(taskMapper.selectById(202L)).thenAnswer(invocation -> holder[0]);

        TenantContextHolder.setTenantId(0L);
        DccControlledFileBatchRecognitionTaskRespVO respVO;
        try {
            respVO = service.createTask(99L, reqVO);
        } finally {
            TenantContextHolder.clear();
        }

        assertEquals(DccControlledFileBatchRecognitionServiceImpl.EXISTING_RECORD_POLICY_RETRY_FAILED,
                holder[0].getExistingRecordPolicy());
        assertEquals(false, holder[0].getOverwriteExisting());
        assertEquals(DccControlledFileBatchRecognitionServiceImpl.EXISTING_RECORD_POLICY_RETRY_FAILED,
                respVO.getExistingRecordPolicy());
        assertEquals(false, respVO.getOverwriteExisting());
    }

    @Test
    void createTaskFiltersNonBusinessSystemFilesBeforePersistingCandidates() {
        when(recognitionProperties.getVersion()).thenReturn("project-code-v1");
        when(taskMapper.selectActiveTask(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_BASIC_INFO))
                .thenReturn(null);
        when(directoryMapper.selectList()).thenReturn(List.of(
                DccFileDirectoryDO.builder().id(1L).name("父目录").build(),
                DccFileDirectoryDO.builder().id(2L).parentId(1L).name("子目录").build()
        ));
        when(queryService.listControlledFileBrowserCandidates(eq(99L), any(DccControlledFilePageReqVO.class)))
                .thenReturn(List.of(
                        DccControlledFileDO.builder().id(900L).fileName("压力延长管 241ES1015-CP-102.pdf").build(),
                        DccControlledFileDO.builder().id(901L).fileName("Thumbs.db").build(),
                        DccControlledFileDO.builder().id(902L).fileName("desktop.ini").build(),
                        DccControlledFileDO.builder().id(903L).fileName("~$临时文件.docx").build()));
        final DccControlledFileBatchRecognitionTaskDO[] holder = new DccControlledFileBatchRecognitionTaskDO[1];
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO task = invocation.getArgument(0);
            task.setId(312L);
            holder[0] = cloneTask(task);
            return 1;
        }).when(taskMapper).insert(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(taskMapper.selectById(312L)).thenAnswer(invocation -> holder[0]);

        TenantContextHolder.setTenantId(0L);
        try {
            service.createTask(99L, createReq());
        } finally {
            TenantContextHolder.clear();
        }

        assertEquals("[900]", holder[0].getCandidateIdsJson());
        assertEquals(1L, holder[0].getTotalCount());
        assertEquals(1L, holder[0].getRemainingCount());
    }

    @Test
    void processWaitingTasksCountsExistingSuccessLedgerWhenOverwriteDisabled() {
        DccControlledFileBatchRecognitionTaskDO task = task(300L);
        task.setCandidateIdsJson("[900]");
        task.setTotalCount(1L);
        task.setRemainingCount(1L);
        task.setOverwriteExisting(false);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(300L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(300L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            holder[0].setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO update = invocation.getArgument(0);
            applyTaskUpdate(holder[0], update);
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .tenantId(0L)
                .dccProjectCodeId(700L)
                .productCode("CODE-EXIST")
                .productName("项目A")
                .build());
        when(recognitionRecordMapper.selectLatestByFileAndVersion(900L, "BASIC_INFO",
                "project-code-v1")).thenReturn(DccControlledFileRecognitionRecordDO.builder()
                .controlledFileId(900L)
                .recognitionScope("BASIC_INFO")
                .recognitionMethod("FILE_NAME_SHORTCUT")
                .recognitionVersion("project-code-v1")
                .status("SUCCESS")
                .build());

        service.processWaitingTasks();

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(1L, holder[0].getProcessedCount());
        assertEquals(1L, holder[0].getSuccessCount());
        assertEquals(0L, holder[0].getFailedCount());
        assertEquals(0L, holder[0].getSkippedExistingCount());
        verify(projectCodeRecognitionService, never()).recognizeProjectCode(any(), any());
        verify(recognitionClaimMapper).releaseClaimsByTaskId(300L);
    }

    @Test
    void processWaitingTasksReleasesClaimsWhenTaskFails() {
        DccControlledFileBatchRecognitionTaskDO task = task(317L);
        task.setCandidateIdsJson("[901]");
        task.setTotalCount(1L);
        task.setRemainingCount(1L);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(317L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(317L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            holder[0].setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO update = invocation.getArgument(0);
            applyTaskUpdate(holder[0], update);
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(controlledFileMapper.selectById(901L)).thenThrow(new IllegalStateException("database unavailable"));

        service.processWaitingTasks();

        assertEquals("FAILED", holder[0].getStatus());
        assertEquals("database unavailable", holder[0].getLastFailureMessage());
        verify(recognitionClaimMapper).releaseClaimsByTaskId(317L);
    }

    @Test
    void processWaitingTasksCountsExistingFailedLedgerWhenOverwriteDisabled() {
        DccControlledFileBatchRecognitionTaskDO task = task(307L);
        task.setCandidateIdsJson("[915]");
        task.setTotalCount(1L);
        task.setRemainingCount(1L);
        task.setOverwriteExisting(false);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(307L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(307L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            holder[0].setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO update = invocation.getArgument(0);
            applyTaskUpdate(holder[0], update);
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(controlledFileMapper.selectById(915L)).thenReturn(DccControlledFileDO.builder()
                .id(915L)
                .tenantId(0L)
                .build());
        when(recognitionRecordMapper.selectLatestByFileAndVersion(915L, "BASIC_INFO", "project-code-v1"))
                .thenReturn(DccControlledFileRecognitionRecordDO.builder()
                        .controlledFileId(915L)
                        .recognitionScope("BASIC_INFO")
                        .recognitionVersion("project-code-v1")
                        .status("FAILED")
                        .failureMessage("previous failed")
                        .build());

        service.processWaitingTasks();

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(1L, holder[0].getProcessedCount());
        assertEquals(0L, holder[0].getSuccessCount());
        assertEquals(1L, holder[0].getFailedCount());
        assertEquals(0L, holder[0].getSkippedExistingCount());
        assertEquals("previous failed", holder[0].getLastFailureMessage());
        verify(projectCodeRecognitionService, never()).recognizeProjectCode(any(), any(), any());
    }

    @Test
    void processWaitingTasksRetriesFailedAndUnrecognizedFilesWhenPolicyRetryFailed() {
        DccControlledFileBatchRecognitionTaskDO task = task(308L);
        task.setCandidateIdsJson("[916,917,918,919,920,921]");
        task.setTotalCount(6L);
        task.setRemainingCount(6L);
        task.setOverwriteExisting(false);
        task.setExistingRecordPolicy(DccControlledFileBatchRecognitionServiceImpl.EXISTING_RECORD_POLICY_RETRY_FAILED);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(308L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(308L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            holder[0].setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO update = invocation.getArgument(0);
            applyTaskUpdate(holder[0], update);
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(controlledFileMapper.selectById(916L)).thenReturn(DccControlledFileDO.builder()
                .id(916L).tenantId(0L).build());
        when(controlledFileMapper.selectById(917L)).thenReturn(DccControlledFileDO.builder()
                .id(917L).tenantId(0L).build());
        when(controlledFileMapper.selectById(918L)).thenReturn(DccControlledFileDO.builder()
                .id(918L).tenantId(0L).build());
        when(controlledFileMapper.selectById(919L)).thenReturn(DccControlledFileDO.builder()
                .id(919L).tenantId(0L).build());
        when(controlledFileMapper.selectById(920L)).thenReturn(DccControlledFileDO.builder()
                .id(920L).tenantId(0L).build());
        when(controlledFileMapper.selectById(921L)).thenReturn(DccControlledFileDO.builder()
                .id(921L).tenantId(0L).build());
        when(recognitionRecordMapper.selectLatestByFileAndVersion(916L, "BASIC_INFO", "project-code-v1"))
                .thenReturn(DccControlledFileRecognitionRecordDO.builder()
                        .controlledFileId(916L)
                        .recognitionScope("BASIC_INFO")
                        .recognitionVersion("project-code-v1")
                        .status("SUCCESS")
                        .build());
        when(recognitionRecordMapper.selectLatestByFileAndVersion(917L, "BASIC_INFO", "project-code-v1"))
                .thenReturn(DccControlledFileRecognitionRecordDO.builder()
                        .controlledFileId(917L)
                        .recognitionScope("BASIC_INFO")
                        .recognitionVersion("project-code-v1")
                        .status("FAILED")
                        .failureMessage("previous failed")
                        .build());
        when(recognitionRecordMapper.selectLatestByFileAndVersion(918L, "BASIC_INFO", "project-code-v1"))
                .thenReturn(null);
        when(recognitionRecordMapper.selectLatestByFileAndVersion(919L, "BASIC_INFO", "project-code-v1"))
                .thenReturn(DccControlledFileRecognitionRecordDO.builder()
                        .controlledFileId(919L)
                        .recognitionScope("BASIC_INFO")
                        .recognitionVersion("project-code-v1")
                        .status(DccControlledFileProjectCodeRecognitionServiceImpl.RECOGNITION_STATUS_NO_MATCH)
                        .build());
        when(recognitionRecordMapper.selectLatestByFileAndVersion(920L, "BASIC_INFO", "project-code-v1"))
                .thenReturn(DccControlledFileRecognitionRecordDO.builder()
                        .controlledFileId(920L)
                        .recognitionScope("BASIC_INFO")
                        .recognitionVersion("project-code-v1")
                        .status(DccControlledFileProjectCodeRecognitionServiceImpl.RECOGNITION_STATUS_UNKNOWN_DCC_BASIC_DATA)
                        .build());
        when(recognitionRecordMapper.selectLatestByFileAndVersion(921L, "BASIC_INFO", "project-code-v1"))
                .thenReturn(DccControlledFileRecognitionRecordDO.builder()
                        .controlledFileId(921L)
                        .recognitionScope("BASIC_INFO")
                        .recognitionVersion("project-code-v1")
                        .status(DccControlledFileProjectCodeRecognitionServiceImpl.RECOGNITION_STATUS_UNRECOGNIZED_PROJECT_NAME)
                        .build());
        when(recognitionClaimMapper.tryClaimBasicInfo(any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(projectCodeRecognitionService.recognizeProjectCode(99L, 917L, 308L))
                .thenReturn(successRecognitionResp());
        when(projectCodeRecognitionService.recognizeProjectCode(99L, 918L, 308L))
                .thenReturn(successRecognitionResp());
        when(projectCodeRecognitionService.recognizeProjectCode(99L, 919L, 308L))
                .thenReturn(successRecognitionResp());
        when(projectCodeRecognitionService.recognizeProjectCode(99L, 920L, 308L))
                .thenReturn(successRecognitionResp());
        when(projectCodeRecognitionService.recognizeProjectCode(99L, 921L, 308L))
                .thenReturn(successRecognitionResp());

        service.processWaitingTasks();

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(6L, holder[0].getProcessedCount());
        assertEquals(6L, holder[0].getSuccessCount());
        assertEquals(0L, holder[0].getFailedCount());
        assertEquals(0L, holder[0].getSkippedExistingCount());
        verify(projectCodeRecognitionService, never()).recognizeProjectCode(99L, 916L, 308L);
        verify(projectCodeRecognitionService).recognizeProjectCode(99L, 917L, 308L);
        verify(projectCodeRecognitionService).recognizeProjectCode(99L, 918L, 308L);
        verify(projectCodeRecognitionService).recognizeProjectCode(99L, 919L, 308L);
        verify(projectCodeRecognitionService).recognizeProjectCode(99L, 920L, 308L);
        verify(projectCodeRecognitionService).recognizeProjectCode(99L, 921L, 308L);
    }

    @Test
    void processWaitingTasksReprocessesWhenLedgerVersionDiffers() {
        DccControlledFileBatchRecognitionTaskDO task = task(303L);
        task.setCandidateIdsJson("[904]");
        task.setTotalCount(1L);
        task.setRemainingCount(1L);
        task.setOverwriteExisting(false);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(303L)).thenAnswer(invocation -> holder[0]);
        holder[0].setRecognitionVersionSnapshot("project-code-v2");
        when(taskMapper.claimWaitingTask(eq(303L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            holder[0].setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO update = invocation.getArgument(0);
            applyTaskUpdate(holder[0], update);
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(controlledFileMapper.selectById(904L)).thenReturn(DccControlledFileDO.builder()
                .id(904L)
                .tenantId(0L)
                .dccProjectCodeId(700L)
                .productCode("CODE-EXIST")
                .productName("项目A")
                .build());
        when(recognitionClaimMapper.tryClaimBasicInfo(any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(recognitionRecordMapper.selectLatestByFileAndVersion(904L, "BASIC_INFO",
                "project-code-v2")).thenReturn(null);
        when(projectCodeRecognitionService.recognizeProjectCode(99L, 904L, 303L))
                .thenReturn(successRecognitionResp());
        holder[0].setOperatorUserId(99L);

        service.processWaitingTasks();

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(1L, holder[0].getSuccessCount());
        assertEquals(0L, holder[0].getSkippedExistingCount());
        verify(projectCodeRecognitionService).recognizeProjectCode(99L, 904L, 303L);
    }

    @Test
    void processWaitingTasksSkipsExistingLedgerEvenWhenBusinessFieldsMissing() {
        DccControlledFileBatchRecognitionTaskDO task = task(304L);
        task.setCandidateIdsJson("[905]");
        task.setTotalCount(1L);
        task.setRemainingCount(1L);
        task.setOverwriteExisting(false);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(304L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(304L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            holder[0].setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO update = invocation.getArgument(0);
            applyTaskUpdate(holder[0], update);
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(controlledFileMapper.selectById(905L)).thenReturn(DccControlledFileDO.builder()
                .id(905L)
                .tenantId(0L)
                .dccProjectCodeId(null)
                .productCode("")
                .productName(null)
                .build());
        when(recognitionRecordMapper.selectLatestByFileAndVersion(905L, "BASIC_INFO",
                "project-code-v1")).thenReturn(
                DccControlledFileRecognitionRecordDO.builder()
                        .controlledFileId(905L)
                        .recognitionScope("BASIC_INFO")
                        .recognitionMethod("CODEX_CLI_CONTENT")
                        .recognitionVersion("project-code-v1")
                        .status("SUCCESS")
                        .build());
        holder[0].setOperatorUserId(99L);

        service.processWaitingTasks();

        assertEquals(1L, holder[0].getSuccessCount());
        assertEquals(0L, holder[0].getSkippedExistingCount());
        verify(projectCodeRecognitionService, never()).recognizeProjectCode(any(), any(), any());
    }

    @Test
    void processWaitingTasksTreatsNoMatchRecognitionAsCompletedSuccess() {
        DccControlledFileBatchRecognitionTaskDO task = task(312L);
        task.setCandidateIdsJson("[917]");
        task.setTotalCount(1L);
        task.setRemainingCount(1L);
        task.setOverwriteExisting(true);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(312L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(312L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            holder[0].setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO update = invocation.getArgument(0);
            applyTaskUpdate(holder[0], update);
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(controlledFileMapper.selectById(917L)).thenReturn(DccControlledFileDO.builder()
                .id(917L)
                .tenantId(0L)
                .build());
        when(recognitionClaimMapper.tryClaimBasicInfo(any(), any(), any(), any(), any(), any())).thenReturn(1);
        DccControlledFileProjectCodeRecognitionRespVO noMatchResp = new DccControlledFileProjectCodeRecognitionRespVO();
        noMatchResp.setControlledFileId(917L);
        noMatchResp.setRecognitionStatus("NO_MATCH");
        when(projectCodeRecognitionService.recognizeProjectCode(99L, 917L, 312L)).thenReturn(noMatchResp);
        holder[0].setOperatorUserId(99L);

        service.processWaitingTasks();

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(1L, holder[0].getProcessedCount());
        assertEquals(1L, holder[0].getSuccessCount());
        assertEquals(0L, holder[0].getFailedCount());
        assertEquals(0L, holder[0].getRemainingCount());
        assertNull(holder[0].getLastFailureMessage());
    }

    @Test
    void processWaitingTasksTreatsNewBusinessClassificationStatusesAsCompletedSuccess() {
        DccControlledFileBatchRecognitionTaskDO task = task(314L);
        task.setCandidateIdsJson("[919,920]");
        task.setTotalCount(2L);
        task.setRemainingCount(2L);
        task.setOverwriteExisting(true);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(314L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(314L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            holder[0].setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO update = invocation.getArgument(0);
            applyTaskUpdate(holder[0], update);
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(controlledFileMapper.selectById(919L)).thenReturn(DccControlledFileDO.builder()
                .id(919L)
                .tenantId(0L)
                .build());
        when(controlledFileMapper.selectById(920L)).thenReturn(DccControlledFileDO.builder()
                .id(920L)
                .tenantId(0L)
                .build());
        when(recognitionClaimMapper.tryClaimBasicInfo(any(), any(), any(), any(), any(), any())).thenReturn(1);
        DccControlledFileProjectCodeRecognitionRespVO unknownBasicDataResp =
                new DccControlledFileProjectCodeRecognitionRespVO();
        unknownBasicDataResp.setControlledFileId(919L);
        unknownBasicDataResp.setRecognitionStatus("UNKNOWN_DCC");
        DccControlledFileProjectCodeRecognitionRespVO unrecognizedNameResp =
                new DccControlledFileProjectCodeRecognitionRespVO();
        unrecognizedNameResp.setControlledFileId(920L);
        unrecognizedNameResp.setRecognitionStatus("NAME_MISMATCH");
        when(projectCodeRecognitionService.recognizeProjectCode(99L, 919L, 314L)).thenReturn(unknownBasicDataResp);
        when(projectCodeRecognitionService.recognizeProjectCode(99L, 920L, 314L)).thenReturn(unrecognizedNameResp);
        holder[0].setOperatorUserId(99L);

        service.processWaitingTasks();

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(2L, holder[0].getProcessedCount());
        assertEquals(2L, holder[0].getSuccessCount());
        assertEquals(0L, holder[0].getFailedCount());
        assertEquals(0L, holder[0].getRemainingCount());
        assertNull(holder[0].getLastFailureMessage());
    }

    @Test
    void processWaitingTasksTreatsUnexpectedRecognitionStatusAsFailedMessage() {
        DccControlledFileBatchRecognitionTaskDO task = task(315L);
        task.setCandidateIdsJson("[921]");
        task.setTotalCount(1L);
        task.setRemainingCount(1L);
        task.setOverwriteExisting(true);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(315L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(315L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            holder[0].setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO update = invocation.getArgument(0);
            applyTaskUpdate(holder[0], update);
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(controlledFileMapper.selectById(921L)).thenReturn(DccControlledFileDO.builder()
                .id(921L)
                .tenantId(0L)
                .build());
        when(recognitionClaimMapper.tryClaimBasicInfo(any(), any(), any(), any(), any(), any())).thenReturn(1);
        String longStatusLikeError = "DCC project-code recognition failed: Codex CLI exited with code 1 ".repeat(20);
        DccControlledFileProjectCodeRecognitionRespVO respVO = new DccControlledFileProjectCodeRecognitionRespVO();
        respVO.setControlledFileId(921L);
        respVO.setRecognitionStatus(longStatusLikeError);
        when(projectCodeRecognitionService.recognizeProjectCode(99L, 921L, 315L)).thenReturn(respVO);
        holder[0].setOperatorUserId(99L);

        service.processWaitingTasks();

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(1L, holder[0].getProcessedCount());
        assertEquals(0L, holder[0].getSuccessCount());
        assertEquals(1L, holder[0].getFailedCount());
        assertEquals(0L, holder[0].getRemainingCount());
        String normalizedFailureMessage = longStatusLikeError.trim();
        assertEquals(normalizedFailureMessage, holder[0].getLastFailureMessage());
        ArgumentCaptor<DccControlledFileRecognitionRecordDO> recordCaptor =
                ArgumentCaptor.forClass(DccControlledFileRecognitionRecordDO.class);
        verify(recognitionRecordMapper).upsert(recordCaptor.capture());
        assertEquals(DccControlledFileProjectCodeRecognitionServiceImpl.RECOGNITION_STATUS_FAILED,
                recordCaptor.getValue().getStatus());
        assertEquals(normalizedFailureMessage, recordCaptor.getValue().getFailureMessage());
    }

    @Test
    void processWaitingTasksTreatsExistingNoMatchLedgerAsCompletedSuccess() {
        DccControlledFileBatchRecognitionTaskDO task = task(313L);
        task.setCandidateIdsJson("[918]");
        task.setTotalCount(1L);
        task.setRemainingCount(1L);
        task.setOverwriteExisting(false);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(313L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(313L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            holder[0].setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO update = invocation.getArgument(0);
            applyTaskUpdate(holder[0], update);
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(controlledFileMapper.selectById(918L)).thenReturn(DccControlledFileDO.builder()
                .id(918L)
                .tenantId(0L)
                .build());
        when(recognitionRecordMapper.selectLatestByFileAndVersion(918L, "BASIC_INFO",
                "project-code-v1")).thenReturn(
                DccControlledFileRecognitionRecordDO.builder()
                        .controlledFileId(918L)
                        .recognitionScope("BASIC_INFO")
                        .recognitionMethod("CODEX_CLI_CONTENT")
                        .recognitionVersion("project-code-v1")
                        .status("NO_MATCH")
                        .build());
        holder[0].setOperatorUserId(99L);

        service.processWaitingTasks();

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(1L, holder[0].getSuccessCount());
        assertEquals(0L, holder[0].getFailedCount());
        assertNull(holder[0].getLastFailureMessage());
        verify(projectCodeRecognitionService, never()).recognizeProjectCode(any(), any(), any());
    }

    @Test
    void processWaitingTasksCountsSuccessAndFailure() {
        DccControlledFileBatchRecognitionTaskDO task = task(301L);
        task.setCandidateIdsJson("[901,902]");
        task.setTotalCount(2L);
        task.setRemainingCount(2L);
        task.setOverwriteExisting(true);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(301L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(301L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            holder[0].setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO update = invocation.getArgument(0);
            applyTaskUpdate(holder[0], update);
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(controlledFileMapper.selectById(901L)).thenReturn(DccControlledFileDO.builder().id(901L).build());
        when(controlledFileMapper.selectById(902L)).thenReturn(DccControlledFileDO.builder().id(902L).tenantId(0L).build());
        when(controlledFileMapper.selectById(901L)).thenReturn(DccControlledFileDO.builder().id(901L).tenantId(0L).build());
        when(recognitionClaimMapper.tryClaimBasicInfo(any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(projectCodeRecognitionService.recognizeProjectCode(99L, 901L, 301L))
                .thenReturn(successRecognitionResp());
        when(projectCodeRecognitionService.recognizeProjectCode(99L, 902L, 301L))
                .thenThrow(new IllegalStateException("no match"));
        holder[0].setOperatorUserId(99L);

        service.processWaitingTasks();

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(2L, holder[0].getProcessedCount());
        assertEquals(1L, holder[0].getSuccessCount());
        assertEquals(1L, holder[0].getFailedCount());
        assertEquals(0L, holder[0].getSkippedExistingCount());
        assertEquals(0L, holder[0].getRemainingCount());
        assertEquals("no match", holder[0].getLastFailureMessage());
    }

    @Test
    void processWaitingTasksPersistsFailedLedgerWhenRecognitionServiceFailsBeforeWritingRecord() {
        DccControlledFileBatchRecognitionTaskDO task = task(308L);
        task.setCandidateIdsJson("[916]");
        task.setTotalCount(1L);
        task.setRemainingCount(1L);
        task.setOverwriteExisting(true);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(308L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(308L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            holder[0].setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO update = invocation.getArgument(0);
            applyTaskUpdate(holder[0], update);
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(916L)
                .tenantId(0L)
                .sourceFileId(321L)
                .build();
        when(controlledFileMapper.selectById(916L)).thenReturn(file);
        when(recognitionClaimMapper.tryClaimBasicInfo(any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(recognitionRecordMapper.selectLatestByFileAndVersion(916L, "BASIC_INFO", "project-code-v1"))
                .thenReturn(null);
        when(recognitionRecordMapper.upsert(any(DccControlledFileRecognitionRecordDO.class))).thenReturn(1);
        holder[0].setOperatorUserId(99L);
        when(projectCodeRecognitionService.recognizeProjectCode(99L, 916L, 308L))
                .thenThrow(new IllegalStateException("source file missing"));

        service.processWaitingTasks();

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(1L, holder[0].getProcessedCount());
        assertEquals(0L, holder[0].getSuccessCount());
        assertEquals(1L, holder[0].getFailedCount());
        ArgumentCaptor<DccControlledFileRecognitionRecordDO> recordCaptor =
                ArgumentCaptor.forClass(DccControlledFileRecognitionRecordDO.class);
        verify(recognitionRecordMapper).upsert(recordCaptor.capture());
        DccControlledFileRecognitionRecordDO record = recordCaptor.getValue();
        assertEquals(916L, record.getControlledFileId());
        assertEquals("BASIC_INFO", record.getRecognitionScope());
        assertEquals("BATCH_PROJECT_CODE", record.getRecognitionMethod());
        assertEquals("project-code-v1", record.getRecognitionVersion());
        assertEquals("FAILED", record.getStatus());
        assertEquals(308L, record.getBatchTaskId());
        assertEquals("source file missing", record.getFailureMessage());
    }

    @Test
    void processWaitingTasksSkipsDuplicateInProgressWithoutFailedLedger() {
        DccControlledFileBatchRecognitionTaskDO task = task(309L);
        task.setCandidateIdsJson("[921]");
        task.setTotalCount(1L);
        task.setRemainingCount(1L);
        task.setOverwriteExisting(true);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(309L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(309L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            holder[0].setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO update = invocation.getArgument(0);
            applyTaskUpdate(holder[0], update);
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(controlledFileMapper.selectById(921L)).thenReturn(DccControlledFileDO.builder()
                .id(921L)
                .tenantId(0L)
                .build());
        when(recognitionClaimMapper.tryClaimBasicInfo(any(), any(), any(), any(), any(), any())).thenReturn(0);
        when(recognitionClaimMapper.selectByFileAndScope(921L, "BASIC_INFO")).thenReturn(
                DccControlledFileRecognitionClaimDO.builder()
                        .controlledFileId(921L)
                        .recognitionScope("BASIC_INFO")
                        .claimedBy(120L)
                        .claimTaskId(999L)
                        .build());
        holder[0].setOperatorUserId(99L);

        service.processWaitingTasks();

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(1L, holder[0].getProcessedCount());
        assertEquals(0L, holder[0].getSuccessCount());
        assertEquals(0L, holder[0].getFailedCount());
        assertEquals(1L, holder[0].getSkippedExistingCount());
        assertEquals(0L, holder[0].getRemainingCount());
        assertNull(holder[0].getLastFailureMessage());
        verify(recognitionRecordMapper, never()).upsert(any(DccControlledFileRecognitionRecordDO.class));
        verify(projectCodeRecognitionService, never()).recognizeProjectCode(any(), any(), any());
    }

    @Test
    void processWaitingTasksCountsParallelDuplicateInProgressAsSkippedWithoutFailedLedger() {
        DccControlledFileBatchRecognitionTaskDO task = task(310L);
        task.setCandidateIdsJson("[921,922]");
        task.setTotalCount(2L);
        task.setRemainingCount(2L);
        task.setOverwriteExisting(true);
        task.setWorkerCount(2);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(310L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(310L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            holder[0].setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO update = invocation.getArgument(0);
            applyTaskUpdate(holder[0], update);
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(controlledFileMapper.selectById(921L)).thenReturn(DccControlledFileDO.builder()
                .id(921L)
                .tenantId(0L)
                .build());
        when(controlledFileMapper.selectById(922L)).thenReturn(DccControlledFileDO.builder()
                .id(922L)
                .tenantId(0L)
                .build());
        when(recognitionClaimMapper.tryClaimBasicInfo(any(), any(), any(), any(), any(), any())).thenReturn(0);
        when(recognitionClaimMapper.selectByFileAndScope(921L, "BASIC_INFO")).thenReturn(
                DccControlledFileRecognitionClaimDO.builder()
                        .controlledFileId(921L)
                        .recognitionScope("BASIC_INFO")
                        .claimedBy(120L)
                        .claimTaskId(999L)
                        .build());
        when(recognitionClaimMapper.selectByFileAndScope(922L, "BASIC_INFO")).thenReturn(
                DccControlledFileRecognitionClaimDO.builder()
                        .controlledFileId(922L)
                        .recognitionScope("BASIC_INFO")
                        .claimedBy(121L)
                        .claimTaskId(1000L)
                        .build());
        holder[0].setOperatorUserId(99L);

        service.processWaitingTasks();

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(2L, holder[0].getProcessedCount());
        assertEquals(0L, holder[0].getSuccessCount());
        assertEquals(0L, holder[0].getFailedCount());
        assertEquals(2L, holder[0].getSkippedExistingCount());
        assertEquals(0L, holder[0].getRemainingCount());
        assertNull(holder[0].getLastFailureMessage());
        verify(recognitionRecordMapper, never()).upsert(any(DccControlledFileRecognitionRecordDO.class));
        verify(projectCodeRecognitionService, never()).recognizeProjectCode(any(), any(), any());
    }

    @Test
    void processWaitingTasksUsesConfiguredWorkersInsideOneTask() throws Exception {
        DccControlledFileBatchRecognitionTaskDO task = task(306L);
        task.setCandidateIdsJson("[910,911,912,913,914]");
        task.setTotalCount(5L);
        task.setRemainingCount(5L);
        task.setOverwriteExisting(true);
        task.setWorkerCount(5);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        CountDownLatch entered = new CountDownLatch(5);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger running = new AtomicInteger();
        AtomicInteger maxRunning = new AtomicInteger();
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(306L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(306L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            holder[0].setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO update = invocation.getArgument(0);
            applyTaskUpdate(holder[0], update);
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        for (long fileId = 910L; fileId <= 914L; fileId++) {
            when(controlledFileMapper.selectById(fileId))
                    .thenReturn(DccControlledFileDO.builder().id(fileId).tenantId(0L).build());
        }
        when(recognitionClaimMapper.tryClaimBasicInfo(any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(projectCodeRecognitionService.recognizeProjectCode(eq(99L), any(), eq(306L))).thenAnswer(invocation -> {
            int current = running.incrementAndGet();
            maxRunning.accumulateAndGet(current, Math::max);
            entered.countDown();
            assertTrue(release.await(2, TimeUnit.SECONDS));
            running.decrementAndGet();
            return successRecognitionResp();
        });
        holder[0].setOperatorUserId(99L);

        Thread worker = new Thread(service::processWaitingTasks);
        worker.start();
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        release.countDown();
        worker.join(2000);

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(5L, holder[0].getProcessedCount());
        assertEquals(5L, holder[0].getSuccessCount());
        assertEquals(0L, holder[0].getFailedCount());
        assertEquals(0L, holder[0].getRemainingCount());
        assertEquals(5, maxRunning.get());
    }

    @Test
    void processWaitingTasksDoesNotLetWorkersConcurrentlyUpdateTaskProgressRow() throws Exception {
        DccControlledFileBatchRecognitionTaskDO task = task(310L);
        task.setCandidateIdsJson("[940,941,942,943,944]");
        task.setTotalCount(5L);
        task.setRemainingCount(5L);
        task.setOverwriteExisting(true);
        task.setWorkerCount(5);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        CountDownLatch enteredRecognition = new CountDownLatch(5);
        CountDownLatch releaseRecognition = new CountDownLatch(1);
        AtomicInteger runningRecognition = new AtomicInteger();
        AtomicInteger concurrentProgressUpdates = new AtomicInteger();
        ConcurrentLinkedQueue<Long> progressUpdateThreads = new ConcurrentLinkedQueue<>();
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(310L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(310L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            holder[0].setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO update = invocation.getArgument(0);
            progressUpdateThreads.add(Thread.currentThread().getId());
            int concurrent = concurrentProgressUpdates.incrementAndGet();
            if (update.getStatus() == null && concurrent > 1) {
                throw new IllegalStateException("task progress row updated concurrently by worker threads");
            }
            try {
                Thread.sleep(40);
                applyTaskUpdate(holder[0], update);
                return 1;
            } finally {
                concurrentProgressUpdates.decrementAndGet();
            }
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        for (long fileId = 940L; fileId <= 944L; fileId++) {
            when(controlledFileMapper.selectById(fileId))
                    .thenReturn(DccControlledFileDO.builder().id(fileId).tenantId(0L).build());
        }
        when(recognitionClaimMapper.tryClaimBasicInfo(any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(projectCodeRecognitionService.recognizeProjectCode(eq(99L), any(), eq(310L))).thenAnswer(invocation -> {
            runningRecognition.incrementAndGet();
            enteredRecognition.countDown();
            assertTrue(releaseRecognition.await(2, TimeUnit.SECONDS));
            runningRecognition.decrementAndGet();
            return successRecognitionResp();
        });
        holder[0].setOperatorUserId(99L);

        Thread worker = new Thread(service::processWaitingTasks);
        worker.start();
        assertTrue(enteredRecognition.await(2, TimeUnit.SECONDS));
        releaseRecognition.countDown();
        worker.join(3000);

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(5L, holder[0].getProcessedCount());
        assertEquals(5L, holder[0].getSuccessCount());
        assertEquals(0L, holder[0].getFailedCount());
        assertEquals(0L, holder[0].getRemainingCount());
        assertEquals(1L, progressUpdateThreads.stream().distinct().count(),
                "Only the batch coordinator should persist task progress snapshots.");
    }

    @Test
    void getTaskReturnsRuntimeWorkerAndRecordedFileCounts() {
        DccControlledFileBatchRecognitionTaskDO task = task(308L);
        task.setStatus("RUNNING");
        task.setCandidateIdsJson("[920,921,922]");
        task.setTotalCount(3L);
        task.setWorkerCount(5);
        when(taskMapper.selectById(308L)).thenReturn(task);
        when(recognitionRecordMapper.countRecordedFilesByFileIdsAndVersion(
                List.of(920L, 921L, 922L), "BASIC_INFO", "project-code-v1")).thenReturn(2L);

        DccControlledFileBatchRecognitionTaskRespVO respVO = service.getTask(99L, 308L);

        assertEquals(3, respVO.getActiveWorkerCount());
        assertEquals(2L, respVO.getRecordedFileCount());
    }

    @Test
    void getTaskReturnsTopFailureSummariesForTerminalTask() {
        DccControlledFileBatchRecognitionTaskDO task = task(322L);
        task.setStatus("COMPLETED");
        task.setFailedCount(4L);
        when(taskMapper.selectById(322L)).thenReturn(task);
        when(recognitionRecordMapper.selectFailureSummariesByBatchTaskId(322L, 3)).thenReturn(List.of(
                DccControlledFileRecognitionFailureSummaryDO.builder()
                        .failureStage("AI_CLASSIFICATION")
                        .failureCode("AI_REQUEST_FAILED")
                        .failureMessage("AI classification request timed out")
                        .failureCount(3L)
                        .build(),
                DccControlledFileRecognitionFailureSummaryDO.builder()
                        .failureStage("SOURCE_ACCESS")
                        .failureCode("SOURCE_FILE_MISSING")
                        .failureMessage("source file missing")
                        .failureCount(1L)
                        .build()
        ));

        DccControlledFileBatchRecognitionTaskRespVO respVO = service.getTask(99L, 322L);

        assertEquals(2, respVO.getFailureSummaries().size());
        assertEquals("AI_CLASSIFICATION", respVO.getFailureSummaries().get(0).getStage());
        assertEquals("AI_REQUEST_FAILED", respVO.getFailureSummaries().get(0).getCode());
        assertEquals("AI classification request timed out", respVO.getFailureSummaries().get(0).getReason());
        assertEquals(3L, respVO.getFailureSummaries().get(0).getCount());
        assertEquals("SOURCE_ACCESS", respVO.getFailureSummaries().get(1).getStage());
        assertEquals("SOURCE_FILE_MISSING", respVO.getFailureSummaries().get(1).getCode());
        assertEquals("source file missing", respVO.getFailureSummaries().get(1).getReason());
        assertEquals(1L, respVO.getFailureSummaries().get(1).getCount());
    }

    @Test
    void getTaskDoesNotAggregateFailureSummariesWhileTaskIsRunning() {
        DccControlledFileBatchRecognitionTaskDO task = task(323L);
        task.setStatus("RUNNING");
        task.setFailedCount(1L);
        when(taskMapper.selectById(323L)).thenReturn(task);

        DccControlledFileBatchRecognitionTaskRespVO respVO = service.getTask(99L, 323L);

        assertTrue(respVO.getFailureSummaries().isEmpty());
        verify(recognitionRecordMapper, never()).selectFailureSummariesByBatchTaskId(any(), any());
    }

    @Test
    void getTaskAllowsDocControlToObserveSharedActiveTask() {
        DccControlledFileBatchRecognitionTaskDO task = task(318L);
        task.setOperatorUserId(88L);
        task.setStatus("RUNNING");
        when(taskMapper.selectById(318L)).thenReturn(task);

        DccControlledFileBatchRecognitionTaskRespVO respVO = service.getTask(99L, 318L);

        assertEquals(318L, respVO.getTaskId());
    }

    @Test
    void getLatestTaskReturnsSharedActiveTaskBeforeOwnHistory() {
        DccControlledFileBatchRecognitionTaskDO activeTask = task(319L);
        activeTask.setOperatorUserId(88L);
        activeTask.setRecognitionType(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_FILE_CATEGORY);
        activeTask.setStatus("RUNNING");
        when(taskMapper.selectActiveTask(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_FILE_CATEGORY))
                .thenReturn(activeTask);

        DccControlledFileBatchRecognitionTaskRespVO respVO =
                service.getLatestTask(99L, DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_FILE_CATEGORY);

        assertEquals(319L, respVO.getTaskId());
        verify(taskMapper, never()).selectLatestTask(any(Long.class), any(String.class));
    }

    @Test
    void getLatestTaskReturnsSharedLatestFileCategoryTaskWhenNoActiveTask() {
        DccControlledFileBatchRecognitionTaskDO completedTask = task(320L);
        completedTask.setOperatorUserId(88L);
        completedTask.setRecognitionType(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_FILE_CATEGORY);
        completedTask.setStatus("COMPLETED");
        when(taskMapper.selectLatestTask(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_FILE_CATEGORY))
                .thenReturn(completedTask);

        DccControlledFileBatchRecognitionTaskRespVO respVO =
                service.getLatestTask(99L, DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_FILE_CATEGORY);

        assertEquals(320L, respVO.getTaskId());
        verify(taskMapper, never()).selectLatestTask(any(Long.class), any(String.class));
    }

    @Test
    void getLatestTaskKeepsBasicInfoHistoryScopedToCurrentUser() {
        DccControlledFileBatchRecognitionTaskDO ownTask = task(321L);
        ownTask.setOperatorUserId(99L);
        ownTask.setRecognitionType(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_BASIC_INFO);
        ownTask.setStatus("COMPLETED");
        when(taskMapper.selectLatestTask(99L, DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_BASIC_INFO))
                .thenReturn(ownTask);

        DccControlledFileBatchRecognitionTaskRespVO respVO =
                service.getLatestTask(99L, DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_BASIC_INFO);

        assertEquals(321L, respVO.getTaskId());
        verify(taskMapper, never()).selectLatestTask(any(String.class));
    }

    @Test
    void stopTaskMarksActiveTaskStoppedAndReturnsSnapshot() {
        DccControlledFileBatchRecognitionTaskDO task = task(309L);
        task.setStatus("RUNNING");
        task.setCandidateIdsJson("[930,931]");
        task.setTotalCount(2L);
        task.setProcessedCount(1L);
        task.setRemainingCount(1L);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectById(309L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.stopActiveTask(eq(309L), eq(99L), any(), eq("Stopped by user"))).thenAnswer(invocation -> {
            holder[0].setStatus("STOPPED");
            holder[0].setCompletedAt(invocation.getArgument(2));
            holder[0].setLastFailureMessage(invocation.getArgument(3));
            return 1;
        });
        when(recognitionRecordMapper.countRecordedFilesByFileIdsAndVersion(
                List.of(930L, 931L), "BASIC_INFO", "project-code-v1")).thenReturn(1L);

        DccControlledFileBatchRecognitionTaskRespVO respVO = service.stopTask(99L, 309L);

        assertEquals("STOPPED", respVO.getStatus());
        assertEquals(0, respVO.getActiveWorkerCount());
        assertEquals(1L, respVO.getRecordedFileCount());
        assertEquals("Stopped by user", respVO.getLastFailureMessage());
        verify(taskMapper).stopActiveTask(eq(309L), eq(99L), any(), eq("Stopped by user"));
        verify(recognitionClaimMapper).releaseClaimsByTaskId(309L);
    }

    @Test
    void processWaitingTasksSkipsFileWhenRecognitionClaimBelongsToAnotherWorker() {
        DccControlledFileBatchRecognitionTaskDO task = task(305L);
        task.setCandidateIdsJson("[906]");
        task.setTotalCount(1L);
        task.setRemainingCount(1L);
        task.setOverwriteExisting(true);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(305L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(305L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            holder[0].setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO update = invocation.getArgument(0);
            applyTaskUpdate(holder[0], update);
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(controlledFileMapper.selectById(906L)).thenReturn(DccControlledFileDO.builder().id(906L).tenantId(0L).build());
        when(recognitionClaimMapper.tryClaimBasicInfo(eq(0L), eq(906L), eq("BASIC_INFO"), eq(99L), eq(305L), any()))
                .thenReturn(0);
        when(recognitionClaimMapper.selectByFileAndScope(906L, "BASIC_INFO")).thenReturn(
                DccControlledFileRecognitionClaimDO.builder()
                        .controlledFileId(906L)
                        .recognitionScope("BASIC_INFO")
                        .claimedBy(1001L)
                        .claimTaskId(9999L)
                        .build());
        holder[0].setOperatorUserId(99L);

        service.processWaitingTasks();

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(1L, holder[0].getProcessedCount());
        assertEquals(0L, holder[0].getSuccessCount());
        assertEquals(0L, holder[0].getFailedCount());
        assertEquals(1L, holder[0].getSkippedExistingCount());
        verify(projectCodeRecognitionService, never()).recognizeProjectCode(any(), any());
    }

    @Test
    void processWaitingTasksPreservesLongFailureMessageBeforePersistingProgressAndRecord() {
        DccControlledFileBatchRecognitionTaskDO task = task(302L);
        task.setCandidateIdsJson("[903]");
        task.setTotalCount(1L);
        task.setRemainingCount(1L);
        task.setOverwriteExisting(true);
        String longFailureMessage = "x".repeat(600);
        final DccControlledFileBatchRecognitionTaskDO[] holder = { cloneTask(task) };
        when(taskMapper.selectWaitingTasks()).thenReturn(List.of(holder[0]));
        when(taskMapper.selectById(302L)).thenAnswer(invocation -> holder[0]);
        when(taskMapper.claimWaitingTask(eq(302L), any())).thenAnswer(invocation -> {
            holder[0].setStatus("RUNNING");
            holder[0].setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        doAnswer(invocation -> {
            DccControlledFileBatchRecognitionTaskDO update = invocation.getArgument(0);
            applyTaskUpdate(holder[0], update);
            return 1;
        }).when(taskMapper).updateById(any(DccControlledFileBatchRecognitionTaskDO.class));
        when(controlledFileMapper.selectById(903L)).thenReturn(DccControlledFileDO.builder().id(903L).tenantId(0L).build());
        when(recognitionClaimMapper.tryClaimBasicInfo(any(), any(), any(), any(), any(), any())).thenReturn(1);
        holder[0].setOperatorUserId(99L);
        when(projectCodeRecognitionService.recognizeProjectCode(99L, 903L, 302L))
                .thenThrow(new IllegalStateException(longFailureMessage));

        service.processWaitingTasks();

        assertEquals("COMPLETED", holder[0].getStatus());
        assertEquals(1L, holder[0].getProcessedCount());
        assertEquals(0L, holder[0].getSuccessCount());
        assertEquals(1L, holder[0].getFailedCount());
        assertEquals(0L, holder[0].getRemainingCount());
        assertEquals(longFailureMessage, holder[0].getLastFailureMessage());
        ArgumentCaptor<DccControlledFileRecognitionRecordDO> recordCaptor =
                ArgumentCaptor.forClass(DccControlledFileRecognitionRecordDO.class);
        verify(recognitionRecordMapper).upsert(recordCaptor.capture());
        assertEquals(longFailureMessage, recordCaptor.getValue().getFailureMessage());
    }

    private DccControlledFileBatchRecognitionCreateReqVO createReq() {
        DccControlledFileBatchRecognitionCreateReqVO reqVO = new DccControlledFileBatchRecognitionCreateReqVO();
        reqVO.setRecognitionType(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_BASIC_INFO);
        reqVO.setScope("CURRENT");
        reqVO.setDirectoryId(2L);
        reqVO.setIncludeDescendantDirectories(true);
        reqVO.setOverwriteExisting(false);
        reqVO.setExistingRecordPolicy(DccControlledFileBatchRecognitionServiceImpl.EXISTING_RECORD_POLICY_SKIP_ALL_EXISTING);
        reqVO.setSyncFileNameTitle(true);
        reqVO.setWorkerCount(5);
        return reqVO;
    }

    private DccControlledFileProjectCodeRecognitionRespVO successRecognitionResp() {
        DccControlledFileProjectCodeRecognitionRespVO respVO = new DccControlledFileProjectCodeRecognitionRespVO();
        respVO.setRecognitionStatus(DccControlledFileProjectCodeRecognitionServiceImpl.RECOGNITION_STATUS_SUCCESS);
        return respVO;
    }

    private DccControlledFileBatchRecognitionTaskDO task(Long id) {
        return DccControlledFileBatchRecognitionTaskDO.builder()
                .id(id)
                .operatorUserId(99L)
                .recognitionType(DccControlledFileBatchRecognitionServiceImpl.RECOGNITION_TYPE_BASIC_INFO)
                .scopeType("CURRENT")
                .directoryId(2L)
                .directoryPathSnapshot("QMS documents/3-1 RE 可编辑")
                .recognitionVersionSnapshot("project-code-v1")
                .overwriteExisting(false)
                .existingRecordPolicy(DccControlledFileBatchRecognitionServiceImpl.EXISTING_RECORD_POLICY_SKIP_ALL_EXISTING)
                .syncFileNameTitle(true)
                .workerCount(1)
                .status("WAITING")
                .totalCount(0L)
                .processedCount(0L)
                .successCount(0L)
                .failedCount(0L)
                .skippedExistingCount(0L)
                .unclassifiedCount(0L)
                .ambiguousCount(0L)
                .conflictCount(0L)
                .remainingCount(0L)
                .candidateIdsJson("[]")
                .build();
    }

    private DccControlledFileBatchRecognitionTaskDO cloneTask(DccControlledFileBatchRecognitionTaskDO task) {
        return DccControlledFileBatchRecognitionTaskDO.builder()
                .id(task.getId())
                .operatorUserId(task.getOperatorUserId())
                .recognitionType(task.getRecognitionType())
                .scopeType(task.getScopeType())
                .directoryId(task.getDirectoryId())
                .directoryPathSnapshot(task.getDirectoryPathSnapshot())
                .recognitionVersionSnapshot(task.getRecognitionVersionSnapshot())
                .keyword(task.getKeyword())
                .statusFilter(task.getStatusFilter())
                .categoryId(task.getCategoryId())
                .overwriteExisting(task.getOverwriteExisting())
                .existingRecordPolicy(task.getExistingRecordPolicy())
                .syncFileNameTitle(task.getSyncFileNameTitle())
                .workerCount(task.getWorkerCount())
                .candidateIdsJson(task.getCandidateIdsJson())
                .status(task.getStatus())
                .totalCount(task.getTotalCount())
                .processedCount(task.getProcessedCount())
                .successCount(task.getSuccessCount())
                .failedCount(task.getFailedCount())
                .skippedExistingCount(task.getSkippedExistingCount())
                .unclassifiedCount(task.getUnclassifiedCount())
                .ambiguousCount(task.getAmbiguousCount())
                .conflictCount(task.getConflictCount())
                .remainingCount(task.getRemainingCount())
                .lastFailureMessage(task.getLastFailureMessage())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }

    private void applyTaskUpdate(DccControlledFileBatchRecognitionTaskDO target, DccControlledFileBatchRecognitionTaskDO update) {
        if (update.getStatus() != null) target.setStatus(update.getStatus());
        if (update.getProcessedCount() != null) target.setProcessedCount(update.getProcessedCount());
        if (update.getSuccessCount() != null) target.setSuccessCount(update.getSuccessCount());
        if (update.getFailedCount() != null) target.setFailedCount(update.getFailedCount());
        if (update.getSkippedExistingCount() != null) target.setSkippedExistingCount(update.getSkippedExistingCount());
        if (update.getUnclassifiedCount() != null) target.setUnclassifiedCount(update.getUnclassifiedCount());
        if (update.getAmbiguousCount() != null) target.setAmbiguousCount(update.getAmbiguousCount());
        if (update.getConflictCount() != null) target.setConflictCount(update.getConflictCount());
        if (update.getRemainingCount() != null) target.setRemainingCount(update.getRemainingCount());
        if (update.getLastFailureMessage() != null || target.getLastFailureMessage() == null) {
            target.setLastFailureMessage(update.getLastFailureMessage());
        }
        if (update.getStartedAt() != null) target.setStartedAt(update.getStartedAt());
        if (update.getCompletedAt() != null) target.setCompletedAt(update.getCompletedAt());
    }
}
