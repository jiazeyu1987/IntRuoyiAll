package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDistributionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryTrainingRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileObsoleteAuditDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingProgressDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingProgressMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryDistributionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryTrainingRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccDirectoryAccessRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileObsoleteAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileChangeTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileDistributionStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileChangeTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileMasterStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileMessageJobStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileTrainingStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccDistributionMediumEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PUBLISH_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PDF_CONVERSION_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_STAMP_GENERATION_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_STAMP_RETRY_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileFinalizationServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Mock
    private DccControlledFileDistributionMapper distributionMapper;
    @Mock
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Mock
    private DccControlledFileTrainingMapper trainingMapper;
    @Mock
    private DccControlledFileTrainingAssignmentMapper trainingAssignmentMapper;
    @Mock
    private DccControlledFileTrainingProgressMapper trainingProgressMapper;
    @Mock
    private DccControlledFileMessageJobMapper messageJobMapper;
    @Mock
    private DccControlledFileObsoleteAuditMapper obsoleteAuditMapper;
    @Mock
    private DccFileCategoryMapper categoryMapper;
    @Mock
    private DccFileCategoryDistributionRuleMapper distributionRuleMapper;
    @Mock
    private DccFileCategoryTrainingRuleMapper trainingRuleMapper;
    @Mock
    private DccControlledFileAccessLogMapper accessLogMapper;
    @Mock
    private DccDirectoryAccessRuleMapper accessRuleMapper;
    @Mock
    private FileMapper fileMapper;
    @Mock
    private FileService fileService;
    @Mock
    private DccPdfStampService pdfStampService;
    @Mock
    private DccDocumentPdfConversionService pdfConversionService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private NotifyMessageSendApi notifyMessageSendApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private DccControlledFileQueryService queryService;
    @Mock
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Mock
    private DccObsoleteFileStorageService obsoleteFileStorageService;
    @Mock
    private DccControlledContentAdapter platformAdapter;
    @Mock
    private DccControlledFilePendingActionGuard pendingActionGuard;
    @Mock
    private DccControlledFileSignatureBindingService signatureBindingService;

    private DccControlledFileMessageDeliveryService messageDeliveryService;
    @InjectMocks
    private DccControlledFileFinalizationServiceImpl finalizationService;

    private final AtomicLong distributionIdGenerator = new AtomicLong(2000L);
    private final AtomicLong trainingIdGenerator = new AtomicLong(3000L);
    private final AtomicLong messageJobIdGenerator = new AtomicLong(4000L);
    private final AtomicLong trainingProgressIdGenerator = new AtomicLong(5000L);
    private final Map<Long, List<DccControlledFileDistributionRecipientDO>> recipientsByDistributionId = new HashMap<>();

    @BeforeEach
    void setUp() {
        messageDeliveryService = new DccControlledFileMessageDeliveryService();
        ReflectionTestUtils.setField(messageDeliveryService, "messageJobMapper", messageJobMapper);
        ReflectionTestUtils.setField(messageDeliveryService, "notifyMessageSendApi", notifyMessageSendApi);
        ReflectionTestUtils.setField(messageDeliveryService, "controlledFileMapper", controlledFileMapper);
        ReflectionTestUtils.setField(messageDeliveryService, "distributionMapper", distributionMapper);
        ReflectionTestUtils.setField(messageDeliveryService, "trainingMapper", trainingMapper);
        ReflectionTestUtils.setField(finalizationService, "messageDeliveryService", messageDeliveryService);
        distributionIdGenerator.set(2000L);
        trainingIdGenerator.set(3000L);
        messageJobIdGenerator.set(4000L);
        trainingProgressIdGenerator.set(5000L);
        recipientsByDistributionId.clear();
        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        lenient().doAnswer(invocation -> {
            DccControlledFileDistributionDO distribution = invocation.getArgument(0);
            distribution.setId(distributionIdGenerator.getAndIncrement());
            return 1;
        }).when(distributionMapper).insert(any(DccControlledFileDistributionDO.class));
        lenient().doAnswer(invocation -> {
            DccControlledFileTrainingDO training = invocation.getArgument(0);
            training.setId(trainingIdGenerator.getAndIncrement());
            return 1;
        }).when(trainingMapper).insert(any(DccControlledFileTrainingDO.class));
        lenient().doAnswer(invocation -> {
            DccControlledFileMessageJobDO messageJob = invocation.getArgument(0);
            messageJob.setId(messageJobIdGenerator.getAndIncrement());
            return 1;
        }).when(messageJobMapper).insert(any(DccControlledFileMessageJobDO.class));
        lenient().doAnswer(invocation -> {
            DccControlledFileDistributionRecipientDO recipient = invocation.getArgument(0);
            recipientsByDistributionId
                    .computeIfAbsent(recipient.getDistributionId(), ignored -> new ArrayList<>())
                    .add(DccControlledFileDistributionRecipientDO.builder()
                            .id(recipient.getId())
                            .distributionId(recipient.getDistributionId())
                            .userId(recipient.getUserId())
                            .messageJobId(recipient.getMessageJobId())
                            .build());
            return 1;
        }).when(distributionRecipientMapper).insert(any(DccControlledFileDistributionRecipientDO.class));
        lenient().doAnswer(invocation -> {
            Long distributionId = invocation.getArgument(0);
            return recipientsByDistributionId.getOrDefault(distributionId, List.of());
        }).when(distributionRecipientMapper).selectListByDistributionId(any());
        lenient().doAnswer(invocation -> {
            DccControlledFileTrainingProgressDO progress = invocation.getArgument(0);
            progress.setId(trainingProgressIdGenerator.getAndIncrement());
            return 1;
        }).when(trainingProgressMapper).insert(any(DccControlledFileTrainingProgressDO.class));
    }

    @Test
    void handleProcessInstanceStatusChanged_revisionApprovalMarksReadyToPublishWithoutActivating() throws Exception {
        DccControlledFileDO file = buildRevisionApprovalCandidate(910L, 710L, 18L, 110L);
        when(controlledFileMapper.selectById(910L)).thenReturn(file);

        finalizationService.handleProcessInstanceStatusChanged(approveEvent(910L));

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus(), fileCaptor.getValue().getStatus());
        assertTrue(fileCaptor.getValue().getApprovedTime() != null);
        verify(platformAdapter).recordApprovedReadyToPublish(file, 99L, "process-910");
        verify(platformAdapter, never()).recordFinalizationStarted(any(), any(), any());
        verify(platformAdapter, never()).recordFinalized(any(), any(), any(), any());
        verify(controlledFileMasterMapper, never()).updateById(any(DccControlledFileMasterDO.class));
        verify(fileMapper, never()).selectById(any());
        verify(fileService, never()).createFile(any(), any(), any(), any());
        verify(pdfStampService, never()).stamp(any());
    }

    @Test
    void applyApprovedPublishControlledFile_readyCandidateStartsFinalizationAndActivates() throws Exception {
        DccControlledFileDO file = buildReadyToPublishCandidate(920L, 720L, 18L, 120L);
        DccControlledFileDO previousActive = DccControlledFileDO.builder()
                .id(820L)
                .masterId(720L)
                .categoryId(18L)
                .sourceFileId(119L)
                .publishedFileId(119L)
                .fileName("SOP-001")
                .fileNumber("FI-001")
                .versionNo("V1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
        when(controlledFileMapper.selectById(920L)).thenReturn(file, file);
        when(controlledFileMapper.selectById(820L)).thenReturn(previousActive);
        when(controlledFileMasterMapper.selectById(720L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(720L)
                .categoryId(18L)
                .fileName("SOP-001")
                .fileNumber("FI-001")
                .currentActiveControlledFileId(820L)
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build());
        when(categoryMapper.selectById(18L)).thenReturn(category(18L, false, false));
        when(permissionSupport.hasCategoryPermission(18L, 99L, DccFileCategoryPermissionActionEnum.APPROVE))
                .thenReturn(true);
        stubStampedArtifact(120L, 620L);

        finalizationService.applyApprovedPublishControlledFile(99L, 920L, "publish-effect-1");

        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper, org.mockito.Mockito.times(3)).updateById(updateCaptor.capture());
        List<DccControlledFileDO> updates = updateCaptor.getAllValues();
        assertEquals(DccControlledFileStatusEnum.FINALIZING.getStatus(), updates.get(0).getStatus());
        assertEquals(DccControlledFileStatusEnum.SUPERSEDED.getStatus(),
                updates.stream().filter(item -> item.getId().equals(820L)).findFirst().orElseThrow().getStatus());
        assertTrue(updates.stream().anyMatch(item -> item.getId().equals(920L)
                && DccControlledFileStatusEnum.ACTIVE.getStatus().equals(item.getStatus())));
        verify(platformAdapter).recordPublishFinalizationStarted(file, 99L, "publish-effect-1");
        verify(platformAdapter).recordFinalized(previousActive, file, 99L, "publish-effect-1");
        ArgumentCaptor<DccControlledFileMasterDO> masterCaptor = ArgumentCaptor.forClass(DccControlledFileMasterDO.class);
        verify(controlledFileMasterMapper).updateById(masterCaptor.capture());
        assertEquals(920L, masterCaptor.getValue().getCurrentActiveControlledFileId());
    }

    @Test
    void applyApprovedPublishControlledFile_nonReadyCandidateThrowsWithoutSideEffects() {
        DccControlledFileDO file = buildFinalizingFile(921L, 721L, 18L, 121L);
        file.setStatus(DccControlledFileStatusEnum.ACTIVE.getStatus());
        when(controlledFileMapper.selectById(921L)).thenReturn(file);

        assertServiceException(() -> finalizationService.applyApprovedPublishControlledFile(99L, 921L,
                "publish-effect-2"), CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);

        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(platformAdapter, never()).recordPublishFinalizationStarted(any(), any(), any());
        verify(platformAdapter, never()).recordFinalized(any(), any(), any(), any());
        verify(controlledFileMasterMapper, never()).updateById(any(DccControlledFileMasterDO.class));
    }

    @Test
    void handleProcessInstanceStatusChanged_approveCreatesActiveRevisionAndDownstreamRecords() throws Exception {
        DccControlledFileDO file = buildFinalizingFile(900L, 700L, 10L, 100L);
        DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                .id(700L)
                .categoryId(10L)
                .fileName("SOP-001")
                .fileNumber("FI-001")
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build();
        when(controlledFileMapper.selectById(900L)).thenReturn(file, file);
        when(controlledFileMasterMapper.selectById(700L)).thenReturn(master);
        when(categoryMapper.selectById(10L)).thenReturn(category(10L, true, false));
        when(distributionRuleMapper.selectList(
                org.mockito.ArgumentMatchers.<SFunction<DccFileCategoryDistributionRuleDO, ?>>any(), eq(10L))).thenReturn(List.of(
                DccFileCategoryDistributionRuleDO.builder()
                        .id(1L)
                        .categoryId(10L)
                        .departmentId(300L)
                        .distributionMedium(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())
                        .active(Boolean.TRUE)
                        .build()));
        when(adminUserApi.getUserListByDeptIds(List.of(300L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(501L),
                new AdminUserRespDTO().setId(502L)));
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class)))
                .thenReturn(8001L, 8002L);
        stubStampedArtifact(100L, 600L);

        finalizationService.handleProcessInstanceStatusChanged(approveEvent(900L));

        verify(distributionMapper).insert(any(DccControlledFileDistributionDO.class));
        verify(distributionRecipientMapper, org.mockito.Mockito.times(2))
                .insert(any(DccControlledFileDistributionRecipientDO.class));
        verify(trainingMapper, never()).insert(any(DccControlledFileTrainingDO.class));
        verify(trainingAssignmentMapper, never()).insert(any(DccControlledFileTrainingAssignmentDO.class));
        verify(messageJobMapper, org.mockito.Mockito.times(2)).insert(any(DccControlledFileMessageJobDO.class));
        verify(messageJobMapper, org.mockito.Mockito.times(2)).updateById(any(DccControlledFileMessageJobDO.class));
        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi, org.mockito.Mockito.times(2)).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(List.of(501L, 502L),
                notifyCaptor.getAllValues().stream().map(NotifySendSingleToUserReqDTO::getUserId).toList());
        assertTrue(notifyCaptor.getAllValues().stream()
                .allMatch(req -> DccControlledFileFinalizationServiceImpl.MESSAGE_TEMPLATE_DISTRIBUTION.equals(req.getTemplateCode())));

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.ACTIVE.getStatus(), fileCaptor.getValue().getStatus());
        assertEquals(600L, fileCaptor.getValue().getPublishedFileId());
        assertEquals(600L, fileCaptor.getValue().getStampedFileId());
        assertTrue(fileCaptor.getValue().getStampedTime() != null);
        verify(platformAdapter).recordFinalizationStarted(file, 99L, "process-900");
        verify(platformAdapter).recordFinalized(null, file, 99L, "process-900");
        verify(signatureBindingService).bindPublishedCopy(file, 600L, 99L, "process-900");

        ArgumentCaptor<DccControlledFileDistributionDO> distributionCaptor =
                ArgumentCaptor.forClass(DccControlledFileDistributionDO.class);
        verify(distributionMapper).insert(distributionCaptor.capture());
        assertEquals(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode(),
                distributionCaptor.getValue().getDistributionMedium());
        ArgumentCaptor<DccControlledFileMasterDO> masterCaptor = ArgumentCaptor.forClass(DccControlledFileMasterDO.class);
        verify(controlledFileMasterMapper).updateById(masterCaptor.capture());
        assertEquals(900L, masterCaptor.getValue().getCurrentActiveControlledFileId());
        assertEquals(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode(), masterCaptor.getValue().getStatus());
    }

    @Test
    void handleProcessInstanceStatusChanged_obsoleteWorkflowClearsActiveAndDoesNotActivateRequest() throws Exception {
        DccControlledFileDO obsoleteRequest = buildFinalizingFile(910L, 710L, 17L, 110L);
        obsoleteRequest.setChangeType(DccControlledFileChangeTypeEnum.OBSOLETE.getCode());
        obsoleteRequest.setObsoleteReason("obsolete by workflow");
        obsoleteRequest.setVersionNo("V1.1");
        DccControlledFileDO previousActive = DccControlledFileDO.builder()
                .id(804L)
                .masterId(710L)
                .categoryId(17L)
                .directoryId(20L)
                .sourceFileId(104L)
                .originalFileId(104L)
                .fileName("DWG-001")
                .title("DWG-001")
                .fileNumber("DWG-001")
                .versionNo("V1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
        when(controlledFileMapper.selectById(910L)).thenReturn(obsoleteRequest, obsoleteRequest);
        when(controlledFileMapper.selectById(804L)).thenReturn(previousActive);
        when(controlledFileMasterMapper.selectById(710L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(710L)
                .categoryId(17L)
                .fileName("DWG-001")
                .fileNumber("DWG-001")
                .currentActiveControlledFileId(804L)
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build());
        finalizationService.handleProcessInstanceStatusChanged(approveEvent(910L));

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper, org.mockito.Mockito.times(2)).updateById(fileCaptor.capture());
        assertTrue(fileCaptor.getAllValues().stream().anyMatch(updated ->
                Long.valueOf(804L).equals(updated.getId())
                        && DccControlledFileStatusEnum.OBSOLETE.getStatus().equals(updated.getStatus())
                        && Long.valueOf(99L).equals(updated.getObsoletedBy())
                        && "obsolete by workflow".equals(updated.getObsoleteReason())));
        assertTrue(fileCaptor.getAllValues().stream().anyMatch(updated ->
                Long.valueOf(910L).equals(updated.getId())
                        && DccControlledFileStatusEnum.OBSOLETE.getStatus().equals(updated.getStatus())
                        && Long.valueOf(99L).equals(updated.getObsoletedBy())
                        && "obsolete by workflow".equals(updated.getObsoleteReason())));
        ArgumentCaptor<DccControlledFileMasterDO> masterCaptor = ArgumentCaptor.forClass(DccControlledFileMasterDO.class);
        verify(controlledFileMasterMapper).updateById(masterCaptor.capture());
        assertEquals(710L, masterCaptor.getValue().getId());
        assertNull(masterCaptor.getValue().getCurrentActiveControlledFileId());
        assertEquals(DccControlledFileMasterStatusEnum.OBSOLETE_CHAIN.getCode(), masterCaptor.getValue().getStatus());
        verify(obsoleteFileStorageService).moveControlledFileArtifactsToObsoleteFolder(previousActive);
        verify(obsoleteAuditMapper).insert(any(DccControlledFileObsoleteAuditDO.class));
        verify(pdfStampService, never()).stamp(any());
        verify(distributionMapper, never()).insert(any(DccControlledFileDistributionDO.class));
        verify(trainingMapper, never()).insert(any(DccControlledFileTrainingDO.class));
        verify(platformAdapter).recordFinalizationStarted(obsoleteRequest, 99L, "process-910");
        verify(platformAdapter).recordWorkflowObsoleted(previousActive, obsoleteRequest, 99L,
                "obsolete by workflow", "process-910");
    }

    @Test
    void activateWithoutApproval_finalizingFileCreatesActiveRevisionAndDownstreamRecords() throws Exception {
        DccControlledFileDO file = buildFinalizingFile(991L, 791L, 20L, 191L);
        DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                .id(791L)
                .categoryId(20L)
                .fileName("NAS-001")
                .fileNumber("NAS-001")
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build();
        when(controlledFileMapper.selectById(991L)).thenReturn(file, file);
        when(controlledFileMasterMapper.selectById(791L)).thenReturn(master);
        when(categoryMapper.selectById(20L)).thenReturn(category(20L, false, false));
        stubStampedArtifact(191L, 691L);

        finalizationService.activateWithoutApproval(991L);

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.ACTIVE.getStatus(), fileCaptor.getValue().getStatus());
        assertEquals(691L, fileCaptor.getValue().getPublishedFileId());
        ArgumentCaptor<DccControlledFileMasterDO> masterCaptor = ArgumentCaptor.forClass(DccControlledFileMasterDO.class);
        verify(controlledFileMasterMapper).updateById(masterCaptor.capture());
        assertEquals(991L, masterCaptor.getValue().getCurrentActiveControlledFileId());
    }

    @Test
    void handleProcessInstanceStatusChanged_existingElectronicDistributionPlanDispatchesSelectedRecipientsAndActivates() throws Exception {
        DccControlledFileDO file = buildFinalizingFile(908L, 708L, 18L, 108L);
        when(controlledFileMapper.selectById(908L)).thenReturn(file, file);
        when(controlledFileMasterMapper.selectById(708L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(708L)
                .categoryId(18L)
                .fileName("SOP-008")
                .fileNumber("FI-008")
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build());
        when(categoryMapper.selectById(18L)).thenReturn(category(18L, false, false));
        when(distributionMapper.selectListByControlledFileId(908L)).thenReturn(List.of(
                DccControlledFileDistributionDO.builder()
                        .id(2100L)
                        .controlledFileId(908L)
                        .departmentId(300L)
                        .distributionMedium(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())
                        .status(DccControlledFileDistributionStatusEnum.PENDING.getCode())
                        .build()));
        when(distributionRecipientMapper.selectListByDistributionId(2100L)).thenReturn(List.of(
                DccControlledFileDistributionRecipientDO.builder()
                        .id(3100L)
                        .distributionId(2100L)
                        .userId(501L)
                        .build(),
                DccControlledFileDistributionRecipientDO.builder()
                        .id(3101L)
                        .distributionId(2100L)
                        .userId(502L)
                        .build()));
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class)))
                .thenReturn(8301L, 8302L);
        stubStampedArtifact(108L, 608L);

        finalizationService.handleProcessInstanceStatusChanged(approveEvent(908L));

        verify(distributionMapper, never()).insert(any(DccControlledFileDistributionDO.class));
        verify(messageJobMapper, org.mockito.Mockito.times(2)).insert(any(DccControlledFileMessageJobDO.class));
        verify(messageJobMapper, org.mockito.Mockito.times(2)).updateById(any(DccControlledFileMessageJobDO.class));
        ArgumentCaptor<NotifySendSingleToUserReqDTO> notifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi, org.mockito.Mockito.times(2)).sendSingleMessageToAdmin(notifyCaptor.capture());
        assertEquals(List.of(501L, 502L),
                notifyCaptor.getAllValues().stream().map(NotifySendSingleToUserReqDTO::getUserId).toList());

        ArgumentCaptor<DccControlledFileDistributionRecipientDO> recipientUpdateCaptor =
                ArgumentCaptor.forClass(DccControlledFileDistributionRecipientDO.class);
        verify(distributionRecipientMapper, org.mockito.Mockito.times(2)).updateById(recipientUpdateCaptor.capture());
        assertEquals(List.of(3100L, 3101L),
                recipientUpdateCaptor.getAllValues().stream().map(DccControlledFileDistributionRecipientDO::getId).toList());
        assertEquals(List.of(4000L, 4001L),
                recipientUpdateCaptor.getAllValues().stream().map(DccControlledFileDistributionRecipientDO::getMessageJobId).toList());

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.ACTIVE.getStatus(), fileCaptor.getValue().getStatus());
    }

    @Test
    void activateWithoutApproval_skipGovernance_activatesWithoutDistributionOrTraining() throws Exception {
        DccControlledFileDO file = buildFinalizingFile(992L, 792L, 21L, 192L);
        DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                .id(792L)
                .categoryId(21L)
                .fileName("NAS-002")
                .fileNumber("NAS-002")
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build();
        when(controlledFileMapper.selectById(992L)).thenReturn(file, file);
        when(controlledFileMasterMapper.selectById(792L)).thenReturn(master);
        when(categoryMapper.selectById(21L)).thenReturn(category(21L, true, true));
        stubStampedArtifact(192L, 692L);

        finalizationService.activateWithoutApproval(992L, true);

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.ACTIVE.getStatus(), fileCaptor.getValue().getStatus());
        assertEquals(692L, fileCaptor.getValue().getPublishedFileId());
        verify(distributionMapper, never()).insert(any(DccControlledFileDistributionDO.class));
        verify(trainingMapper, never()).insert(any(DccControlledFileTrainingDO.class));
        verify(trainingAssignmentMapper, never()).insert(any(DccControlledFileTrainingAssignmentDO.class));
        verify(messageJobMapper, never()).insert(any(DccControlledFileMessageJobDO.class));
        verify(platformAdapter).recordFinalized(null, file, null, "dcc-finalization:992");
    }

    @Test
    void activateWithoutApproval_skipGovernance_pdfStampFailurePublishesOriginalPdf() throws Exception {
        DccControlledFileDO file = buildFinalizingFile(993L, 793L, 22L, 193L);
        DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                .id(793L)
                .categoryId(22L)
                .fileName("NAS-003")
                .fileNumber("NAS-003")
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build();
        when(controlledFileMapper.selectById(993L)).thenReturn(file, file);
        when(controlledFileMasterMapper.selectById(793L)).thenReturn(master);
        when(categoryMapper.selectById(22L)).thenReturn(category(22L, true, true));
        when(fileMapper.selectById(193L)).thenReturn(FileDO.builder()
                .id(193L)
                .configId(1L)
                .path("dcc/original/broken.pdf")
                .name("broken.pdf")
                .type("application/pdf")
                .build());
        when(fileService.getFileContent(1L, "dcc/original/broken.pdf")).thenReturn("broken-pdf".getBytes());
        when(pdfStampService.stamp("broken-pdf".getBytes()))
                .thenThrow(new IOException("Missing root object specification in trailer."));

        finalizationService.activateWithoutApproval(993L, true);

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.ACTIVE.getStatus(), fileCaptor.getValue().getStatus());
        assertEquals(193L, fileCaptor.getValue().getPublishedFileId());
        assertEquals(null, fileCaptor.getValue().getStampedFileId());
        verify(fileService, never()).createFile(any(), any(), any(), any());
        verify(distributionMapper, never()).insert(any(DccControlledFileDistributionDO.class));
        verify(trainingMapper, never()).insert(any(DccControlledFileTrainingDO.class));
        verify(trainingAssignmentMapper, never()).insert(any(DccControlledFileTrainingAssignmentDO.class));
        verify(messageJobMapper, never()).insert(any(DccControlledFileMessageJobDO.class));
        verify(platformAdapter).recordFinalized(null, file, null, "dcc-finalization:993");
    }

    @Test
    void handleProcessInstanceStatusChanged_missingDistributionRuleMarksFailureAndThrows() throws Exception {
        DccControlledFileDO file = buildFinalizingFile(901L, 701L, 11L, 101L);
        when(controlledFileMapper.selectById(901L)).thenReturn(file, file);
        when(controlledFileMasterMapper.selectById(701L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(701L).categoryId(11L).fileName("SOP-002").fileNumber("FI-002").build());
        when(categoryMapper.selectById(11L)).thenReturn(category(11L, true, false));
        when(distributionRuleMapper.selectList(
                org.mockito.ArgumentMatchers.<SFunction<DccFileCategoryDistributionRuleDO, ?>>any(), eq(11L))).thenReturn(List.of());
        stubStampedArtifact(101L, 601L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> finalizationService.handleProcessInstanceStatusChanged(approveEvent(901L)));

        assertEquals(CONTROLLED_FILE_STAMP_GENERATION_FAILED.getCode(), ex.getCode());
        ArgumentCaptor<DccControlledFileDO> failureCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(failureCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus(), failureCaptor.getValue().getStatus());
        assertTrue(failureCaptor.getValue().getFinalizationError() != null
                && !failureCaptor.getValue().getFinalizationError().isBlank());
        verify(controlledFileMasterMapper, never()).updateById(any(DccControlledFileMasterDO.class));
    }

    @Test
    void retryStamp_finalizationFailed_retriesAndClearsFailure() throws Exception {
        DccControlledFileDO file = buildFinalizingFile(902L, 702L, 12L, 102L);
        file.setStatus(DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus());
        file.setFinalizationError("previous failure");
        when(controlledFileMapper.selectById(902L)).thenReturn(file, file);
        when(controlledFileMasterMapper.selectById(702L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(702L).categoryId(12L).fileName("SOP-003").fileNumber("FI-003").build());
        when(categoryMapper.selectById(12L)).thenReturn(category(12L, false, false));
        stubStampedArtifact(102L, 602L);

        finalizationService.retryStamp(902L);

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.ACTIVE.getStatus(), fileCaptor.getValue().getStatus());
        assertEquals(602L, fileCaptor.getValue().getPublishedFileId());
        assertEquals(602L, fileCaptor.getValue().getStampedFileId());
        verify(platformAdapter).recordFinalizationRetried(file, null, "dcc-finalization-retry:902");
        verify(platformAdapter).recordFinalized(null, file, null, "dcc-finalization-retry:902");
    }

    @Test
    void handleProcessInstanceStatusChanged_nonPdfSourceConvertsToPdfBeforeStamping() throws Exception {
        DccControlledFileDO file = buildFinalizingFile(990L, 790L, 19L, 190L);
        FileDO sourceFile = FileDO.builder()
                .id(190L)
                .configId(1L)
                .path("dcc/original/spec.docx")
                .name("Spec.docx")
                .type("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .build();
        when(controlledFileMapper.selectById(990L)).thenReturn(file, file);
        when(controlledFileMasterMapper.selectById(790L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(790L)
                .categoryId(19L)
                .fileName("Spec.docx")
                .fileNumber("Spec")
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build());
        when(categoryMapper.selectById(19L)).thenReturn(category(19L, false, false));
        when(fileMapper.selectById(190L)).thenReturn(sourceFile);
        byte[] convertedPdf = "%PDF-converted".getBytes();
        when(pdfConversionService.convertToPdf(sourceFile)).thenReturn(new DccConvertedPdf("Spec.pdf", convertedPdf));
        when(pdfStampService.stamp(convertedPdf)).thenReturn("stamped-converted-pdf".getBytes());
        when(fileService.createFile("stamped-converted-pdf".getBytes(), "Spec.pdf", "dcc/stamped", "application/pdf"))
                .thenReturn("https://example.com/dcc/stamped/Spec.pdf");
        when(fileMapper.selectFirstOne(any(), eq("https://example.com/dcc/stamped/Spec.pdf"))).thenReturn(FileDO.builder()
                .id(690L)
                .configId(1L)
                .path("dcc/stamped/Spec.pdf")
                .name("Spec.pdf")
                .type("application/pdf")
                .url("https://example.com/dcc/stamped/Spec.pdf")
                .build());

        finalizationService.handleProcessInstanceStatusChanged(approveEvent(990L));

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.ACTIVE.getStatus(), fileCaptor.getValue().getStatus());
        assertEquals(690L, fileCaptor.getValue().getPublishedFileId());
        assertEquals(690L, fileCaptor.getValue().getStampedFileId());
        verify(pdfConversionService).convertToPdf(sourceFile);
        verify(pdfStampService).stamp(convertedPdf);
    }

    @Test
    void handleProcessInstanceStatusChanged_nonPdfConversionFailureMarksFinalizationFailed() throws Exception {
        DccControlledFileDO file = buildFinalizingFile(994L, 794L, 23L, 194L);
        FileDO sourceFile = FileDO.builder()
                .id(194L)
                .configId(1L)
                .path("dcc/original/spec.docx")
                .name("Spec.docx")
                .type("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .build();
        when(controlledFileMapper.selectById(994L)).thenReturn(file, file);
        when(controlledFileMasterMapper.selectById(794L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(794L)
                .categoryId(23L)
                .fileName("Spec.docx")
                .fileNumber("Spec")
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build());
        when(categoryMapper.selectById(23L)).thenReturn(category(23L, false, false));
        when(fileMapper.selectById(194L)).thenReturn(sourceFile);
        when(pdfConversionService.convertToPdf(sourceFile)).thenThrow(
                new ServiceException(CONTROLLED_FILE_PDF_CONVERSION_FAILED.getCode(), "OnlyOffice conversion failed"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> finalizationService.handleProcessInstanceStatusChanged(approveEvent(994L)));
        assertEquals(CONTROLLED_FILE_PDF_CONVERSION_FAILED.getCode(), exception.getCode());
        assertEquals("OnlyOffice conversion failed", exception.getMessage());

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper, org.mockito.Mockito.times(1)).updateById(fileCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus(), fileCaptor.getValue().getStatus());
        assertEquals("OnlyOffice conversion failed", fileCaptor.getValue().getFinalizationError());
        verify(pdfStampService, never()).stamp(any());
    }

    @Test
    void retryStamp_invalidStatus_throws() {
        when(controlledFileMapper.selectById(903L)).thenReturn(DccControlledFileDO.builder()
                .id(903L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());

        assertServiceException(() -> finalizationService.retryStamp(903L), CONTROLLED_FILE_STAMP_RETRY_NOT_ALLOWED);
    }

    @Test
    void readPreviewFile_legacyFinalizationEntryFailsClosedWithoutReadSideEffects() {
        assertServiceException(() -> finalizationService.readPreviewFile(99L, 904L),
                CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        verify(queryService, never()).readPreviewFile(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void handleProcessInstanceStatusChanged_trainingRequiredCategory_entersTrainingInsteadOfCreatingFormalDistribution() throws Exception {
        DccControlledFileDO file = buildFinalizingFile(905L, 705L, 15L, 105L);
        when(controlledFileMapper.selectById(905L)).thenReturn(file, file);
        when(controlledFileMasterMapper.selectById(705L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(705L).categoryId(15L).fileName("SOP-005").fileNumber("FI-005").build());
        when(categoryMapper.selectById(15L)).thenReturn(category(15L, true, true));
        when(distributionRuleMapper.selectList(
                org.mockito.ArgumentMatchers.<SFunction<DccFileCategoryDistributionRuleDO, ?>>any(), eq(15L))).thenReturn(List.of(
                DccFileCategoryDistributionRuleDO.builder()
                        .id(10L)
                        .categoryId(15L)
                        .departmentId(300L)
                        .distributionMedium(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())
                        .active(Boolean.TRUE)
                        .build(),
                DccFileCategoryDistributionRuleDO.builder()
                        .id(11L)
                        .categoryId(15L)
                        .departmentId(400L)
                        .distributionMedium(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())
                        .active(Boolean.TRUE)
                        .build()));
        when(adminUserApi.getUserListByDeptIds(List.of(300L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(501L),
                new AdminUserRespDTO().setId(502L)));
        when(adminUserApi.getUserListByDeptIds(List.of(400L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(502L),
                new AdminUserRespDTO().setId(503L)));
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class)))
                .thenReturn(8101L, 8102L, 8103L, 8104L);
        stubStampedArtifact(105L, 605L);

        finalizationService.handleProcessInstanceStatusChanged(approveEvent(905L));

        verify(distributionMapper, never()).insert(any(DccControlledFileDistributionDO.class));
        verify(distributionRecipientMapper, never()).insert(any(DccControlledFileDistributionRecipientDO.class));
        ArgumentCaptor<DccControlledFileTrainingAssignmentDO> assignmentCaptor =
                ArgumentCaptor.forClass(DccControlledFileTrainingAssignmentDO.class);
        verify(trainingAssignmentMapper, org.mockito.Mockito.times(4)).insert(assignmentCaptor.capture());
        assertEquals(List.of(501L, 502L, 502L, 503L),
                assignmentCaptor.getAllValues().stream().map(DccControlledFileTrainingAssignmentDO::getUserId).toList());

        ArgumentCaptor<DccControlledFileTrainingProgressDO> progressCaptor =
                ArgumentCaptor.forClass(DccControlledFileTrainingProgressDO.class);
        verify(trainingProgressMapper, org.mockito.Mockito.times(3)).insert(progressCaptor.capture());
        assertEquals(List.of(501L, 502L, 503L),
                progressCaptor.getAllValues().stream().map(DccControlledFileTrainingProgressDO::getUserId).toList());
        assertTrue(progressCaptor.getAllValues().stream()
                .allMatch(progress -> Integer.valueOf(600).equals(progress.getRequiredViewSeconds())));
        verify(messageJobMapper, org.mockito.Mockito.times(4)).insert(any(DccControlledFileMessageJobDO.class));
        verify(messageJobMapper, org.mockito.Mockito.times(4)).updateById(any(DccControlledFileMessageJobDO.class));
        ArgumentCaptor<NotifySendSingleToUserReqDTO> trainingNotifyCaptor =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi, org.mockito.Mockito.times(4)).sendSingleMessageToAdmin(trainingNotifyCaptor.capture());
        assertEquals(List.of(501L, 502L, 502L, 503L),
                trainingNotifyCaptor.getAllValues().stream().map(NotifySendSingleToUserReqDTO::getUserId).toList());
        assertTrue(trainingNotifyCaptor.getAllValues().stream()
                .allMatch(req -> DccControlledFileFinalizationServiceImpl.MESSAGE_TEMPLATE_TRAINING.equals(req.getTemplateCode())));
        verify(controlledFileMasterMapper, never()).updateById(any(DccControlledFileMasterDO.class));

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.TRAINING_IN_PROGRESS.getStatus(), fileCaptor.getValue().getStatus());
        assertEquals(605L, fileCaptor.getValue().getPublishedFileId());
        assertEquals(605L, fileCaptor.getValue().getStampedFileId());
    }

    @Test
    void handleProcessInstanceStatusChanged_trainingRequiredUsesSavedSingleFileDistributionPlan() throws Exception {
        DccControlledFileDO file = buildFinalizingFile(909L, 709L, 18L, 109L);
        when(controlledFileMapper.selectById(909L)).thenReturn(file, file);
        when(controlledFileMasterMapper.selectById(709L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(709L).categoryId(18L).fileName("SOP-009").fileNumber("FI-009").build());
        when(categoryMapper.selectById(18L)).thenReturn(category(18L, true, true));
        when(distributionMapper.selectListByControlledFileId(909L)).thenReturn(List.of(
                DccControlledFileDistributionDO.builder()
                        .id(2200L)
                        .controlledFileId(909L)
                        .departmentId(300L)
                        .distributionMedium(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())
                        .status(DccControlledFileDistributionStatusEnum.PENDING.getCode())
                        .build(),
                DccControlledFileDistributionDO.builder()
                        .id(2201L)
                        .controlledFileId(909L)
                        .departmentId(301L)
                        .distributionMedium(DccDistributionMediumEnum.PAPER.getCode())
                        .status(DccControlledFileDistributionStatusEnum.PENDING.getCode())
                        .build()));
        when(distributionRecipientMapper.selectListByDistributionId(2200L)).thenReturn(List.of(
                DccControlledFileDistributionRecipientDO.builder()
                        .id(3100L)
                        .distributionId(2200L)
                        .userId(501L)
                        .build(),
                DccControlledFileDistributionRecipientDO.builder()
                        .id(3101L)
                        .distributionId(2200L)
                        .userId(502L)
                        .build()));
        when(adminUserApi.getUserListByDeptIds(List.of(301L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(601L).setDeptId(301L)));
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class)))
                .thenReturn(8401L, 8402L, 8403L);
        stubStampedArtifact(109L, 609L);

        finalizationService.handleProcessInstanceStatusChanged(approveEvent(909L));

        verify(distributionRuleMapper, never()).selectList(
                org.mockito.ArgumentMatchers.<SFunction<DccFileCategoryDistributionRuleDO, ?>>any(), eq(18L));
        verify(distributionMapper, never()).insert(any(DccControlledFileDistributionDO.class));
        verify(distributionRecipientMapper, never()).insert(any(DccControlledFileDistributionRecipientDO.class));
        ArgumentCaptor<DccControlledFileTrainingAssignmentDO> assignmentCaptor =
                ArgumentCaptor.forClass(DccControlledFileTrainingAssignmentDO.class);
        verify(trainingAssignmentMapper, org.mockito.Mockito.times(3)).insert(assignmentCaptor.capture());
        assertEquals(List.of(501L, 502L, 601L),
                assignmentCaptor.getAllValues().stream().map(DccControlledFileTrainingAssignmentDO::getUserId).toList());

        ArgumentCaptor<DccControlledFileTrainingProgressDO> progressCaptor =
                ArgumentCaptor.forClass(DccControlledFileTrainingProgressDO.class);
        verify(trainingProgressMapper, org.mockito.Mockito.times(3)).insert(progressCaptor.capture());
        assertEquals(List.of(501L, 502L, 601L),
                progressCaptor.getAllValues().stream().map(DccControlledFileTrainingProgressDO::getUserId).toList());

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.TRAINING_IN_PROGRESS.getStatus(), fileCaptor.getValue().getStatus());
    }

    @Test
    void releaseManualDistribution_readyTrainingGatedRevision_createsFormalDistributionAndActivatesRevision() {
        DccControlledFileDO file = buildFinalizingFile(907L, 707L, 17L, 107L);
        file.setStatus(DccControlledFileStatusEnum.PENDING_MANUAL_DISTRIBUTION.getStatus());
        file.setPublishedFileId(107L);
        file.setStampedFileId(107L);
        when(controlledFileMapper.selectById(907L)).thenReturn(file);
        when(controlledFileMapper.selectById(804L)).thenReturn(DccControlledFileDO.builder()
                .id(804L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(controlledFileMasterMapper.selectById(707L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(707L)
                .categoryId(17L)
                .fileName("SOP-007")
                .fileNumber("FI-007")
                .currentActiveControlledFileId(804L)
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build());
        when(categoryMapper.selectById(17L)).thenReturn(category(17L, true, true));
        when(permissionSupport.hasCategoryPermission(17L, 99L, DccFileCategoryPermissionActionEnum.DISTRIBUTE))
                .thenReturn(true);
        when(trainingMapper.selectListByControlledFileId(907L)).thenReturn(List.of(
                DccControlledFileTrainingDO.builder()
                        .id(9901L)
                        .controlledFileId(907L)
                        .status(DccControlledFileTrainingStatusEnum.ACKNOWLEDGED.getCode())
                        .build()));
        when(distributionRuleMapper.selectList(
                org.mockito.ArgumentMatchers.<SFunction<DccFileCategoryDistributionRuleDO, ?>>any(), eq(17L))).thenReturn(List.of(
                DccFileCategoryDistributionRuleDO.builder()
                        .id(30L)
                        .categoryId(17L)
                        .departmentId(300L)
                        .distributionMedium(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())
                        .active(Boolean.TRUE)
                        .build()));
        when(adminUserApi.getUserListByDeptIds(List.of(300L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(601L),
                new AdminUserRespDTO().setId(602L)));
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class)))
                .thenReturn(8201L, 8202L);

        finalizationService.releaseManualDistribution(99L, 907L);

        verify(distributionMapper).insert(any(DccControlledFileDistributionDO.class));
        verify(distributionRecipientMapper, org.mockito.Mockito.times(2))
                .insert(any(DccControlledFileDistributionRecipientDO.class));
        verify(messageJobMapper, org.mockito.Mockito.times(2)).insert(any(DccControlledFileMessageJobDO.class));
        verify(messageJobMapper, org.mockito.Mockito.times(2)).updateById(any(DccControlledFileMessageJobDO.class));
        verify(notifyMessageSendApi, org.mockito.Mockito.times(2))
                .sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class));

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper, org.mockito.Mockito.times(2)).updateById(fileCaptor.capture());
        assertTrue(fileCaptor.getAllValues().stream().anyMatch(updated ->
                Long.valueOf(804L).equals(updated.getId())
                        && DccControlledFileStatusEnum.SUPERSEDED.getStatus().equals(updated.getStatus())
                        && Long.valueOf(907L).equals(updated.getSupersededByFileId())));
        assertTrue(fileCaptor.getAllValues().stream().anyMatch(updated ->
                Long.valueOf(907L).equals(updated.getId())
                        && DccControlledFileStatusEnum.ACTIVE.getStatus().equals(updated.getStatus())
                        && Long.valueOf(107L).equals(updated.getPublishedFileId())));
        verify(obsoleteFileStorageService).moveControlledFileArtifactsToObsoleteFolder(
                org.mockito.ArgumentMatchers.argThat(previous -> Long.valueOf(804L).equals(previous.getId())));

        ArgumentCaptor<DccControlledFileMasterDO> masterCaptor = ArgumentCaptor.forClass(DccControlledFileMasterDO.class);
        verify(controlledFileMasterMapper).updateById(masterCaptor.capture());
        assertEquals(907L, masterCaptor.getValue().getCurrentActiveControlledFileId());
    }

    @Test
    void handleProcessInstanceStatusChanged_paperDistributionSkipsDigitalRecipientsButSnapshotsMedium() throws Exception {
        DccControlledFileDO file = buildFinalizingFile(906L, 706L, 16L, 106L);
        when(controlledFileMapper.selectById(906L)).thenReturn(file, file);
        when(controlledFileMasterMapper.selectById(706L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(706L).categoryId(16L).fileName("SOP-006").fileNumber("FI-006").build());
        when(categoryMapper.selectById(16L)).thenReturn(category(16L, true, false));
        when(distributionRuleMapper.selectList(
                org.mockito.ArgumentMatchers.<SFunction<DccFileCategoryDistributionRuleDO, ?>>any(), eq(16L))).thenReturn(List.of(
                DccFileCategoryDistributionRuleDO.builder()
                        .id(20L)
                        .categoryId(16L)
                        .departmentId(300L)
                        .distributionMedium(DccDistributionMediumEnum.PAPER.getCode())
                        .active(Boolean.TRUE)
                        .build()));
        stubStampedArtifact(106L, 606L);

        finalizationService.handleProcessInstanceStatusChanged(approveEvent(906L));

        ArgumentCaptor<DccControlledFileDistributionDO> distributionCaptor =
                ArgumentCaptor.forClass(DccControlledFileDistributionDO.class);
        verify(distributionMapper).insert(distributionCaptor.capture());
        assertEquals(DccDistributionMediumEnum.PAPER.getCode(),
                distributionCaptor.getValue().getDistributionMedium());
        verify(distributionRecipientMapper, never()).insert(any(DccControlledFileDistributionRecipientDO.class));
        verify(messageJobMapper, never()).insert(any(DccControlledFileMessageJobDO.class));
        verify(notifyMessageSendApi, never()).sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class));
    }

    private void stubStampedArtifact(Long sourceFileId, Long stampedFileId) throws Exception {
        when(fileMapper.selectById(sourceFileId)).thenReturn(FileDO.builder()
                .id(sourceFileId)
                .configId(1L)
                .path("dcc/original/source.pdf")
                .name("source.pdf")
                .type("application/pdf")
                .build());
        when(fileService.getFileContent(1L, "dcc/original/source.pdf")).thenReturn("source-pdf".getBytes());
        when(pdfStampService.stamp("source-pdf".getBytes())).thenReturn("stamped-pdf".getBytes());
        when(fileService.createFile("stamped-pdf".getBytes(), "source.pdf", "dcc/stamped", "application/pdf"))
                .thenReturn("https://example.com/dcc/stamped/source.pdf");
        when(fileMapper.selectFirstOne(any(), eq("https://example.com/dcc/stamped/source.pdf"))).thenReturn(FileDO.builder()
                .id(stampedFileId)
                .configId(1L)
                .path("dcc/stamped/source.pdf")
                .name("source.pdf")
                .type("application/pdf")
                .url("https://example.com/dcc/stamped/source.pdf")
                .build());
    }

    private static DccControlledFileDO buildFinalizingFile(Long id, Long masterId, Long categoryId, Long sourceFileId) {
        return DccControlledFileDO.builder()
                .id(id)
                .masterId(masterId)
                .categoryId(categoryId)
                .directoryId(20L)
                .sourceFileId(sourceFileId)
                .originalFileId(sourceFileId)
                .fileName("SOP-001")
                .title("SOP-001")
                .fileNumber("FI-001")
                .versionNo("1.0")
                .requesterId(99L)
                .submitterId(99L)
                .status(DccControlledFileStatusEnum.FINALIZING.getStatus())
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .build();
    }

    private static DccControlledFileDO buildRevisionApprovalCandidate(Long id, Long masterId, Long categoryId,
                                                                       Long sourceFileId) {
        DccControlledFileDO file = buildFinalizingFile(id, masterId, categoryId, sourceFileId);
        file.setVersionNo("V2.0");
        file.setStatus(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus());
        file.setChangeType(DccControlledFileChangeTypeEnum.REVISION.getCode());
        return file;
    }

    private static DccControlledFileDO buildReadyToPublishCandidate(Long id, Long masterId, Long categoryId,
                                                                     Long sourceFileId) {
        DccControlledFileDO file = buildRevisionApprovalCandidate(id, masterId, categoryId, sourceFileId);
        file.setStatus(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus());
        return file;
    }

    private static DccFileCategoryDO category(Long id, boolean distributionRequired, boolean trainingRequired) {
        return DccFileCategoryDO.builder()
                .id(id)
                .code("SOP")
                .name("SOP")
                .active(Boolean.TRUE)
                .distributionRequired(distributionRequired)
                .trainingRequired(trainingRequired)
                .build();
    }

    private static BpmProcessInstanceStatusEvent approveEvent(Long fileId) {
        BpmProcessInstanceStatusEvent event = new BpmProcessInstanceStatusEvent(new Object());
        event.setId("process-" + fileId);
        event.setProcessDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY);
        event.setBusinessKey(String.valueOf(fileId));
        event.setStatus(BpmProcessInstanceStatusEnum.APPROVE.getStatus());
        event.setActorUserId(99L);
        return event;
    }
}
