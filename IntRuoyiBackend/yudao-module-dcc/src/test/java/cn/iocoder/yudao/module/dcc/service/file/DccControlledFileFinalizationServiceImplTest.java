package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
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
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileProcessTypeEnum;
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
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_MANUAL_RELEASE_NOT_ALLOWED;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileFinalizationServiceImplTest extends BaseMockitoUnitTest {

    @Test
    void ic1FutureControlledRevisionPreservesExecutingOldVersion() throws Exception {
        DccControlledFileDO file = buildRevisionApprovalCandidate(9914L, 7714L, 18L, 114L);
        file.setVersionNo("B/1");
        file.setStatus("PENDING_DOC_CONTROL_REVIEW");
        file.setProcessDefinitionKey(DccControlledFileProcessDefinitionKeys.REVISION);
        file.setEffectiveDate(java.time.LocalDate.now().plusDays(10));
        file.setPublishedFileId(null);
        file.setStampedFileId(null);
        DccControlledFileDO old = DccControlledFileDO.builder().id(8814L).masterId(7714L)
                .versionNo("A/1").status("ACTIVE").build();
        when(controlledFileMapper.selectById(9914L)).thenReturn(file);
        when(controlledFileMapper.selectById(8814L)).thenReturn(old);
        when(controlledFileMasterMapper.selectById(7714L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(7714L).currentActiveControlledFileId(8814L).build());
        when(categoryMapper.selectById(18L)).thenReturn(category(18L, false, false));
        stubCompleteOrdinaryApprovalRoster(file);
        stubStampedArtifact(114L, 614L);
        finalizationService.handleProcessInstanceStatusChanged(
                approveEvent(9914L, DccControlledFileProcessDefinitionKeys.REVISION));
        verify(controlledFileMapper, never()).updateById(org.mockito.ArgumentMatchers.<DccControlledFileDO>argThat(
                row -> row.getId().equals(8814L)));
        ArgumentCaptor<DccControlledFileDO> updates = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper, atLeastOnce()).updateById(updates.capture());
        assertTrue(updates.getAllValues().stream().anyMatch(row -> row.getId().equals(9914L)
                && "CONTROLLED_PENDING_EFFECTIVE".equals(row.getStatus())));
    }

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
    @Mock
    private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Mock
    private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper approvalSignatureMapper;
    @Mock
    private DccElectronicSignatureManagementService signatureManagementService;
    @Mock
    private DccPublicationFollowupService publicationFollowupService;
    @Mock
    private DccControlledFileFinalizationFailureService finalizationFailureService;
    @Mock
    private BpmTaskService bpmTaskService;
    @Mock private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTaskAssigneeSnapshotMapper taskAssigneeSnapshotMapper;
    @Mock private org.springframework.jdbc.core.JdbcTemplate lifecycleJdbc;
    @Mock private org.springframework.context.ApplicationEventPublisher lifecyclePublisher;

    private DccControlledFileMessageDeliveryService messageDeliveryService;
    private java.util.Map<Long, DccControlledFileMessageJobDO> messageJobs;
    @InjectMocks
    private DccControlledFileFinalizationServiceImpl finalizationService;

    private final AtomicLong distributionIdGenerator = new AtomicLong(2000L);
    private final AtomicLong trainingIdGenerator = new AtomicLong(3000L);
    private final AtomicLong messageJobIdGenerator = new AtomicLong(4000L);
    private final AtomicLong trainingProgressIdGenerator = new AtomicLong(5000L);
    private final Map<Long, List<DccControlledFileDistributionRecipientDO>> recipientsByDistributionId = new HashMap<>();

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        var lifecycle = new DccControlledFileLifecycleService();
        ReflectionTestUtils.setField(lifecycle,"platformAdapter",org.mockito.Mockito.mock(DccControlledContentAdapter.class));
        ReflectionTestUtils.setField(lifecycle,"fileStateAudit",org.mockito.Mockito.mock(DccWorkflowFileStateAudit.class));
        ReflectionTestUtils.setField(lifecycle,"obsoleteRetentionService",org.mockito.Mockito.mock(DccObsoleteRetentionService.class));
        ReflectionTestUtils.setField(lifecycle,"controlledFileMapper",controlledFileMapper);
        ReflectionTestUtils.setField(lifecycle,"masterMapper",controlledFileMasterMapper);
        ReflectionTestUtils.setField(lifecycle,"obsoleteAuditMapper",obsoleteAuditMapper);
        ReflectionTestUtils.setField(lifecycle,"jdbcTemplate",lifecycleJdbc);
        ReflectionTestUtils.setField(lifecycle,"eventPublisher",lifecyclePublisher);
        ReflectionTestUtils.setField(lifecycle,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy());
        var dates = new DccWorkflowDatePolicy(); dates.setZoneId("Asia/Singapore"); dates.setReminderLeadDays(3);
        ReflectionTestUtils.setField(lifecycle,"datePolicy",dates);
        ReflectionTestUtils.setField(finalizationService,"lifecycleService",lifecycle);
        lenient().when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);
        lenient().when(controlledFileMasterMapper.updateById(any(DccControlledFileMasterDO.class))).thenReturn(1);
        lenient().when(obsoleteAuditMapper.insert(any(DccControlledFileObsoleteAuditDO.class))).thenReturn(1);
        lenient().when(lifecycleJdbc.update(anyString(),org.mockito.ArgumentMatchers.anyLong(),any(),any(),any(),any(),any(),any(),any(),any())).thenReturn(1);
        messageDeliveryService = new DccControlledFileMessageDeliveryService();
        messageJobs = DccMessageDeliveryTestSupport.wire(messageDeliveryService, messageJobMapper);
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
            org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
            try { action.accept(null); } finally {
                org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(false);
            }
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        lenient().when(controlledFileMapper.markReadyToPublishAfterApproval(
                any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        lenient().when(controlledFileMapper.transitionStatus(any(), any(), any(), any(), any())).thenReturn(1);
        lenient().when(bpmTaskService.triggerTask(anyString(), anyString())).thenReturn(Boolean.TRUE);
        lenient().when(controlledFileMapper.selectByIdAndTenantForUpdate(any(), any()))
                .thenAnswer(invocation -> controlledFileMapper.selectById(invocation.getArgument(1)));
        lenient().when(controlledFileMasterMapper.selectByIdForUpdate(any()))
                .thenAnswer(invocation -> {
                    DccControlledFileMasterDO master=controlledFileMasterMapper.selectById(invocation.getArgument(0));
                    if(master!=null) master.setTenantId(1L); return master;
                });
        lenient().when(signatureManagementService.verifySignatureEvidence(any())).thenAnswer(invocation -> {
            var result = new cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureVerifyRespVO();
            result.setSignatureId(invocation.getArgument(0));
            result.setVerificationStatus("VALID");
            return result;
        });
        lenient().when(permissionApi.hasAnyRoles(any(), eq("doc_control"))).thenReturn(true);
        lenient().when(permissionSupport.hasCategoryPermission(any(), any(),
                eq(DccFileCategoryPermissionActionEnum.APPROVE))).thenReturn(true);
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
            messageJobs.put(messageJob.getId(), messageJob);
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
    void ordinaryApprovalImmediatelyActivatesDespiteHistoricalDistributionAndTrainingFlags() {
        assertOrdinaryActivation(DccControlledFileChangeTypeEnum.REVISION.getCode(), true, true, false);
    }

    @Test
    void ordinaryApprovalCannotActivateWithoutTheFrozenSignoffRoster() {
        DccControlledFileDO file = buildRevisionApprovalCandidate(992L, 792L, 18L, 192L);
        file.setPublishedFileId(192L);
        file.setStampedFileId(192L);
        when(controlledFileMapper.selectById(992L)).thenReturn(file);
        when(controlledFileMasterMapper.selectById(792L))
                .thenReturn(DccControlledFileMasterDO.builder().id(792L).build());
        lenient().when(categoryMapper.selectById(18L)).thenReturn(category(18L, false, false));

        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> finalizationService.handleProcessInstanceStatusChanged(approveEvent(992L)));
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(platformAdapter, never()).recordFinalized(any(), any(), any(), any());
    }

    @Test
    void ordinaryApprovalCannotActivateWhenStoredValidSignatureFailsCryptographicVerification() {
        DccControlledFileDO file = buildRevisionApprovalCandidate(993L, 793L, 18L, 193L);
        file.setPublishedFileId(193L);
        file.setStampedFileId(193L);
        when(controlledFileMapper.selectById(993L)).thenReturn(file);
        when(controlledFileMasterMapper.selectById(793L))
                .thenReturn(DccControlledFileMasterDO.builder().id(793L).build());
        stubCompleteOrdinaryApprovalRoster(file);
        var invalid = new cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureVerifyRespVO();
        invalid.setSignatureId(1L);
        invalid.setVerificationStatus("INVALID");
        when(signatureManagementService.verifySignatureEvidence(1L)).thenReturn(invalid);

        assertServiceException(() -> finalizationService.handleProcessInstanceStatusChanged(approveEvent(993L)),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_INVALID);
        verify(controlledFileMapper, never()).markReadyToPublishAfterApproval(any(), any(), any(), any(), any(), any(), any());
        verify(platformAdapter, never()).recordFinalizationStarted(any(), any(), any());
    }

    @Test
    void ordinaryFileCannotStartAnIndependentPublishAction() {
        DccControlledFileDO file = buildReadyToPublishCandidate(991L, 791L, 18L, 191L);
        file.setProcessType(DccControlledFileProcessTypeEnum.CONTROLLED_FILE.getCode());
        when(controlledFileMapper.selectById(991L)).thenReturn(file);
        lenient().when(controlledFileMasterMapper.selectById(791L))
                .thenReturn(DccControlledFileMasterDO.builder().id(791L).build());
        lenient().when(categoryMapper.selectById(18L)).thenReturn(category(18L, false, false));
        lenient().when(permissionApi.hasAnyPermissions(99L, "dcc:controlled-file:approve")).thenReturn(true);

        assertServiceException(() -> finalizationService.precheckPublishControlledFile(99L, 991L),
                CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void missingProcessTypeCannotStartAnIndependentPublishAction() {
        DccControlledFileDO file = buildReadyToPublishCandidate(992L, 792L, 18L, 192L);
        file.setProcessType(null);
        when(controlledFileMapper.selectById(992L)).thenReturn(file);

        assertServiceException(() -> finalizationService.precheckPublishControlledFile(99L, 992L),
                CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);

        verify(permissionApi, never()).hasAnyRoles(any(), any());
        verify(permissionSupport, never()).hasCategoryPermission(any(), any(), any());
        verify(pendingActionGuard, never()).assertNoPendingBusinessAction(any());
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void ordinaryFileCannotApplyApprovedPublishAction() {
        DccControlledFileDO file = buildReadyToPublishCandidate(9921L, 7921L, 18L, 1921L);
        file.setProcessType(DccControlledFileProcessTypeEnum.CONTROLLED_FILE.getCode());
        when(controlledFileMapper.selectById(9921L)).thenReturn(file);

        assertServiceException(() -> finalizationService.applyApprovedPublishControlledFile(99L, 9921L,
                "publish-effect-ordinary"), CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);

        verify(controlledFileMapper, never()).transitionStatus(any(), any(), any(), any(), any());
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(platformAdapter, never()).recordPublishFinalizationStarted(any(), any(), any());
        verify(platformAdapter, never()).recordFinalized(any(), any(), any(), any());
        verify(controlledFileMasterMapper, never()).updateById(any(DccControlledFileMasterDO.class));
    }

    private void assertOrdinaryActivation(String changeType, boolean distribution, boolean training, boolean savedPlan) {
        DccControlledFileDO file = buildRevisionApprovalCandidate(990L, 790L, 18L, 190L);
        file.setChangeType(changeType);
        file.setPublishedFileId(190L);
        file.setStampedFileId(190L);
        when(controlledFileMapper.selectById(990L)).thenReturn(file);
        when(controlledFileMasterMapper.selectById(790L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(790L).categoryId(18L).fileName("SOP-001").fileNumber("FI-001").build());
        when(categoryMapper.selectById(18L)).thenReturn(category(18L, distribution, training));
        stubCompleteOrdinaryApprovalRoster(file);
        if (savedPlan) {
            lenient().when(distributionMapper.selectListByControlledFileId(990L)).thenReturn(List.of(
                    DccControlledFileDistributionDO.builder().id(100L).controlledFileId(990L)
                            .departmentId(300L).distributionMedium("PUBLIC_FOLDER").build()));
        }

        finalizationService.handleProcessInstanceStatusChanged(approveEvent(990L));

        ArgumentCaptor<DccControlledFileDO> updates = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper, atLeastOnce()).updateById(updates.capture());
        assertTrue(updates.getAllValues().stream().anyMatch(row -> row.getId().equals(990L)
                && DccControlledFileStatusEnum.ACTIVE.getStatus().equals(row.getStatus())));
        verify(signatureBindingService).bindPublishedCopy(file, 190L, 99L, "process-990");
        verify(distributionMapper, never()).insert(any(DccControlledFileDistributionDO.class));
        verify(trainingMapper, never()).insert(any(DccControlledFileTrainingDO.class));
        verify(platformAdapter, never()).recordApprovedReadyToPublish(any(), any(), any());
        verify(distributionMapper, never()).selectListByControlledFileId(990L);
        verify(publicationFollowupService).recordPublishedRevision(eq(file), any());
    }

    private void stubCompleteOrdinaryApprovalRoster(DccControlledFileDO file) {
        stubOrdinaryApprovalRoster(file, 2);
    }
    @org.mockito.junit.jupiter.MockitoSettings(strictness=org.mockito.quality.Strictness.LENIENT)
    @Test void aStoredValidNativeSignatureWithoutItsCompletedBpmTaskCannotControlTheFile() throws Exception {
        var file=nativeEvidenceCandidate();
        when(bpmTaskService.getHistoricTask("MATRIX_APPROVAL-task-1")).thenReturn(null);
        var failure=assertThrows(ServiceException.class,()->finalizationService.handleProcessInstanceStatusChanged(
                approveEvent(file.getId(),DccControlledFileProcessDefinitionKeys.UPLOAD)));
        assertEquals(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING.getCode(),failure.getCode());
        verify(fileService,never()).createFile(any(),any(),any(),any());
        verify(lifecyclePublisher,never()).publishEvent(any(Object.class));
    }
    @org.mockito.junit.jupiter.MockitoSettings(strictness=org.mockito.quality.Strictness.LENIENT)
    @Test void aForeignRejectedOrWrongRoundTaskCannotSupplyTheNewNativeApprovalEvidence() throws Exception {
        var file=nativeEvidenceCandidate();var task=completedNativeTask(file,"MATRIX_APPROVAL-task-1","MATRIX_APPROVAL");
        when(bpmTaskService.getHistoricTask("MATRIX_APPROVAL-task-1")).thenReturn(task);
        when(task.getTenantId()).thenReturn("122");
        assertThrows(ServiceException.class,()->finalizationService.handleProcessInstanceStatusChanged(approveEvent(file.getId(),DccControlledFileProcessDefinitionKeys.UPLOAD)));
        when(task.getTenantId()).thenReturn("1");when(task.getProcessInstanceId()).thenReturn("previous-round");
        assertThrows(ServiceException.class,()->finalizationService.handleProcessInstanceStatusChanged(approveEvent(file.getId(),DccControlledFileProcessDefinitionKeys.UPLOAD)));
        when(task.getProcessInstanceId()).thenReturn(file.getProcessInstanceId());
        when(task.getTaskLocalVariables()).thenReturn(Map.of(cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.TASK_VARIABLE_STATUS,3));
        assertThrows(ServiceException.class,()->finalizationService.handleProcessInstanceStatusChanged(approveEvent(file.getId(),DccControlledFileProcessDefinitionKeys.UPLOAD)));
        verify(fileService,never()).createFile(any(),any(),any(),any());
    }
    private DccControlledFileDO nativeEvidenceCandidate() throws Exception {
        var file=buildRevisionApprovalCandidate(914L,714L,18L,114L);file.setVersionNo("A/1");file.setChangeType("NEW");
        file.setStatus("PENDING_DOC_CONTROL_REVIEW");file.setProcessDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD);
        file.setPublishedFileId(null);file.setStampedFileId(null);file.setEffectiveDate(java.time.LocalDate.now().plusDays(10));
        when(controlledFileMapper.selectById(914L)).thenReturn(file);
        when(controlledFileMasterMapper.selectById(714L)).thenReturn(DccControlledFileMasterDO.builder().id(714L).categoryId(18L).build());
        when(categoryMapper.selectById(18L)).thenReturn(category(18L,false,false));
        stubCompleteOrdinaryApprovalRoster(file);stubStampedArtifact(114L,614L);return file;
    }
    private org.flowable.task.api.history.HistoricTaskInstance completedNativeTask(DccControlledFileDO file,String id,String stage) {
        var task=org.mockito.Mockito.mock(org.flowable.task.api.history.HistoricTaskInstance.class);
        lenient().when(task.getId()).thenReturn(id);lenient().when(task.getProcessInstanceId()).thenReturn(file.getProcessInstanceId());
        lenient().when(task.getTenantId()).thenReturn("1");lenient().when(task.getTaskDefinitionKey()).thenReturn(stage);
        lenient().when(task.getAssignee()).thenReturn("99");lenient().when(task.getEndTime()).thenReturn(new java.util.Date());
        lenient().when(task.getTaskLocalVariables()).thenReturn(Map.of(cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.TASK_VARIABLE_STATUS,2));
        return task;
    }

    private void stubOrdinaryApprovalRoster(DccControlledFileDO file, int departmentSignoffTaskCount) {
        boolean threeWorkflow = Set.of(DccControlledFileProcessDefinitionKeys.UPLOAD,
                DccControlledFileProcessDefinitionKeys.REVISION).contains(file.getProcessDefinitionKey());
        List<String> stages = threeWorkflow
                ? List.of("DOC_CONTROL_REVIEW", "MATRIX_REVIEW", "MATRIX_APPROVAL")
                : List.of("DOC_CONTROL_REVIEW", "MATRIX_REVIEW", "MATRIX_APPROVAL", "DOC_CONTROL_APPROVAL");
        when(routeSnapshotMapper.selectListByControlledFileId(file.getId())).thenReturn(stages.stream().map(stage -> {
            boolean departmentSignoff = threeWorkflow && "MATRIX_REVIEW".equals(stage);
            return cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO.builder()
                    .controlledFileId(file.getId()).stageCode(stage)
                    .candidateSourceType(departmentSignoff ? "DEPT" : null)
                    .candidateSourceIds(departmentSignoff ? "300,301" : null)
                    .resolvedUserIds(departmentSignoff ? "99,99" : "99")
                    .requireAllApprovals("MATRIX_REVIEW".equals(stage))
                    .approveMethod("MATRIX_REVIEW".equals(stage) ? "ALL" : "ANY").build();
        }).toList());
        List<cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO> signatures =
                new ArrayList<>();
        long signatureId = 1L;
        for (String stage : stages) {
            int taskCount = threeWorkflow && "MATRIX_REVIEW".equals(stage) ? departmentSignoffTaskCount : 1;
            for (int taskIndex = 1; taskIndex <= taskCount; taskIndex++) {
                signatures.add(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO.builder()
                        .id(signatureId++).controlledFileId(file.getId()).revisionId(file.getId())
                        .versionNo(file.getVersionNo()).actorId(99L).actionType("APPROVE")
                        .evidencePayloadVersion(threeWorkflow?"v4-workflow":null)
                        .processInstanceId(threeWorkflow?file.getProcessInstanceId():null)
                        .meaningCode(stage + "_APPROVE").passwordVerified(true).signedAt(LocalDateTime.now())
                        .taskId(stage + "-task-" + taskIndex).evidenceStatus("VALID")
                        .evidenceHash("signed-evidence").build());
                if(threeWorkflow) {
                    var task=completedNativeTask(file,stage+"-task-"+taskIndex,stage);
                    lenient().when(bpmTaskService.getHistoricTask(stage+"-task-"+taskIndex)).thenReturn(task);
                }
            }
        }
        if (threeWorkflow) {
            List<cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTaskAssigneeSnapshotDO> assignments = new ArrayList<>();
            for(int index=1;index<=departmentSignoffTaskCount;index++) {
                long assignmentId=100+index;
                assignments.add(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTaskAssigneeSnapshotDO.builder()
                    .controlledFileId(file.getId()).stageCode("MATRIX_REVIEW").departmentId(299L+index)
                    .processInstanceId(file.getProcessInstanceId()).leaderUserId(99L).assigneeUserId(99L)
                    .assignmentSignatureId(assignmentId).assignedTime(LocalDateTime.now())
                    .bpmTaskId("MATRIX_REVIEW-task-"+index).build());
                signatures.add(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO.builder()
                    .id(assignmentId).controlledFileId(file.getId()).revisionId(file.getId()).versionNo(file.getVersionNo())
                    .actorId(99L).actionType("ASSIGN").meaningCode("MATRIX_REVIEW_ASSIGN").passwordVerified(true)
                    .evidencePayloadVersion("v4-workflow").processInstanceId(file.getProcessInstanceId())
                    .signedAt(LocalDateTime.now()).taskId("MATRIX_REVIEW-task-"+index).evidenceStatus("VALID")
                    .evidenceHash("assignment-evidence").build());
            }
            when(taskAssigneeSnapshotMapper.selectListByControlledFileId(file.getId())).thenReturn(assignments);
        }
        when(approvalSignatureMapper.selectListByControlledFileId(file.getId())).thenReturn(signatures);
    }

    @Test
    void handleProcessInstanceStatusChanged_revisionApprovalMarksReadyToPublishWithoutActivating_newOrdinaryPolicy() {
        assertOrdinaryActivation("REVISION", false, false, false);
    }

    @Test
    void handleProcessInstanceStatusChanged_preservesContentApprovalTimeDuringFinalization() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 9, 29, 9, 15);
        DccControlledFileDO file = buildRevisionApprovalCandidate(917L, 717L, 18L, 117L);
        file.setPublishedFileId(617L);
        file.setStampedFileId(617L);
        file.setApprovedTime(approvedAt);
        when(controlledFileMapper.selectById(917L)).thenReturn(file);
        when(controlledFileMasterMapper.selectById(717L))
                .thenReturn(DccControlledFileMasterDO.builder().id(717L).build());
        when(categoryMapper.selectById(18L)).thenReturn(category(18L, false, false));
        stubCompleteOrdinaryApprovalRoster(file);

        finalizationService.handleProcessInstanceStatusChanged(approveEvent(917L));

        verify(controlledFileMapper).markReadyToPublishAfterApproval(any(), eq(917L), eq("process-917"),
                eq(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY),
                eq(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus()), eq(approvedAt), eq(99L));
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
        when(permissionApi.hasAnyPermissions(99L, "dcc:controlled-file:approve")).thenReturn(true);
        finalizationService.applyApprovedPublishControlledFile(99L, 920L, "publish-effect-1");

        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper, org.mockito.Mockito.times(2)).updateById(updateCaptor.capture());
        List<DccControlledFileDO> updates = updateCaptor.getAllValues();
        verify(controlledFileMapper).transitionStatus(any(), eq(920L),
                eq(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus()),
                eq(DccControlledFileStatusEnum.FINALIZING.getStatus()), eq(99L));
        assertEquals(DccControlledFileStatusEnum.SUPERSEDED.getStatus(),
                updates.stream().filter(item -> item.getId().equals(820L)).findFirst().orElseThrow().getStatus());
        assertTrue(updates.stream().anyMatch(item -> item.getId().equals(920L)
                && DccControlledFileStatusEnum.ACTIVE.getStatus().equals(item.getStatus())));
        DccControlledFileDO activeUpdate = updates.stream().filter(item -> item.getId().equals(920L)
                && DccControlledFileStatusEnum.ACTIVE.getStatus().equals(item.getStatus())).findFirst().orElseThrow();
        assertEquals(activeUpdate.getPublishedTime(), file.getPublishedTime());
        verify(platformAdapter).recordPublishFinalizationStarted(file, 99L, "publish-effect-1");
        org.mockito.InOrder completionOrder = org.mockito.Mockito.inOrder(
                controlledFileMapper, controlledFileMasterMapper, publicationFollowupService, platformAdapter);
        completionOrder.verify(publicationFollowupService).recordPublishedRevision(file, previousActive);
        completionOrder.verify(platformAdapter).recordFinalized(previousActive, file, 99L, "publish-effect-1");
        verify(platformAdapter).recordFinalized(previousActive, file, 99L, "publish-effect-1");
        ArgumentCaptor<DccControlledFileMasterDO> masterCaptor = ArgumentCaptor.forClass(DccControlledFileMasterDO.class);
        verify(controlledFileMasterMapper).updateById(masterCaptor.capture());
        assertEquals(920L, masterCaptor.getValue().getCurrentActiveControlledFileId());
    }

    @Test
    void handleProcessInstanceStatusChanged_newControlledFileMarksReadyToPublishWithoutActivating_newOrdinaryPolicy() {
        assertOrdinaryActivation("NEW", false, false, false);
    }

    @AfterEach
    void tearDownTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void handleProcessInstanceStatusChanged_replayedApproveDoesNotDowngradeActiveFile() {
        DccControlledFileDO file = buildRevisionApprovalCandidate(912L, 712L, 18L, 112L);
        file.setStatus(DccControlledFileStatusEnum.ACTIVE.getStatus());
        file.setProcessInstanceId("process-912");
        when(controlledFileMapper.selectById(912L)).thenReturn(file);

        finalizationService.handleProcessInstanceStatusChanged(approveEvent(912L));

        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(platformAdapter, never()).recordApprovedReadyToPublish(any(), any(), any());
    }

    @Test
    void handleProcessInstanceStatusChanged_acceptsTheFrozenUploadProcessDefinitionKey() {
        DccControlledFileDO file = buildRevisionApprovalCandidate(912L, 712L, 18L, 112L);
        file.setStatus(DccControlledFileStatusEnum.ACTIVE.getStatus());
        file.setProcessInstanceId("process-912");
        file.setProcessDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD);
        when(controlledFileMapper.selectById(912L)).thenReturn(file);

        finalizationService.handleProcessInstanceStatusChanged(
                approveEvent(912L, DccControlledFileProcessDefinitionKeys.UPLOAD));

        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(platformAdapter, never()).recordApprovedReadyToPublish(any(), any(), any());
    }

    @Test
    void handleProcessInstanceStatusChanged_uploadDocControlReviewActivatesControlledFile() throws Exception {
        DccControlledFileDO file = buildRevisionApprovalCandidate(914L, 714L, 18L, 114L);
        file.setVersionNo("A/1");
        file.setChangeType(DccControlledFileChangeTypeEnum.NEW.getCode());
        file.setStatus(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus());
        file.setProcessDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD);
        file.setPublishedFileId(null);
        file.setStampedFileId(null);
        when(controlledFileMapper.selectById(914L)).thenReturn(file);
        when(controlledFileMasterMapper.selectById(714L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(714L).categoryId(18L).fileName("SOP-001").fileNumber("FI-001").build());
        when(categoryMapper.selectById(18L)).thenReturn(category(18L, false, false));
        stubCompleteOrdinaryApprovalRoster(file);
        stubStampedArtifact(114L, 614L);

        finalizationService.handleProcessInstanceStatusChanged(
                approveEvent(914L, DccControlledFileProcessDefinitionKeys.UPLOAD));

        verify(controlledFileMapper).markReadyToPublishAfterApproval(any(), eq(914L), eq("process-914"),
                eq(DccControlledFileProcessDefinitionKeys.UPLOAD),
                eq(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus()), any(), eq(99L));
        ArgumentCaptor<DccControlledFileDO> updates = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper, atLeastOnce()).updateById(updates.capture());
        assertTrue(updates.getAllValues().stream().anyMatch(row -> Long.valueOf(914L).equals(row.getId())
                && DccControlledFileStatusEnum.ACTIVE.getStatus().equals(row.getStatus())));
        verify(pdfStampService).stamp("source-pdf".getBytes());
        verify(fileService).createFile("stamped-pdf".getBytes(), "source.pdf", "dcc/stamped", "application/pdf");
        verify(signatureBindingService).bindPublishedCopy(file, 614L, 99L, "process-914");
        verify(platformAdapter).recordFinalizationStarted(file, 99L, "process-914");
        verify(publicationFollowupService,never()).recordPublishedRevision(any(),any());
    }

    @Test
    void handleProcessInstanceStatusChanged_sameDepartmentLeaderMustSignEachObligation() throws Exception {
        DccControlledFileDO file = buildRevisionApprovalCandidate(926L, 726L, 18L, 126L);
        file.setChangeType(DccControlledFileChangeTypeEnum.NEW.getCode());
        file.setStatus(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus());
        file.setProcessDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD);
        file.setPublishedFileId(null);
        file.setStampedFileId(null);
        when(controlledFileMapper.selectById(926L)).thenReturn(file);
        when(controlledFileMasterMapper.selectById(726L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(726L).categoryId(18L).fileName("SOP-002").fileNumber("FI-002").build());
        stubOrdinaryApprovalRoster(file, 1);

        ServiceException failure = assertThrows(ServiceException.class,
                () -> finalizationService.handleProcessInstanceStatusChanged(
                        approveEvent(926L, DccControlledFileProcessDefinitionKeys.UPLOAD)));

        assertEquals(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING.getMsg(), failure.getMessage());
        verify(controlledFileMapper, never()).markReadyToPublishAfterApproval(any(), any(), any(), any(), any(), any(), any());
        verify(pdfStampService, never()).stamp(any());
    }

    @Test
    void handleProcessInstanceStatusChanged_legacyApprovalRequiresBoundPublishedArtifact() throws IOException {
        DccControlledFileDO file = buildRevisionApprovalCandidate(916L, 716L, 18L, 116L);
        file.setPublishedFileId(null);
        file.setStampedFileId(null);
        when(controlledFileMapper.selectById(916L)).thenReturn(file);

        assertThrows(ServiceException.class,
                () -> finalizationService.handleProcessInstanceStatusChanged(approveEvent(916L)));

        verify(controlledFileMapper, never()).markReadyToPublishAfterApproval(any(), any(), any(), any(), any(), any(), any());
        verify(pdfStampService, never()).stamp(any());
    }

    @Test
    void handleProcessInstanceStatusChanged_replayedRejectDoesNotDowngradeActiveFile() {
        DccControlledFileDO file = buildRevisionApprovalCandidate(914L, 714L, 18L, 114L);
        file.setStatus(DccControlledFileStatusEnum.ACTIVE.getStatus());
        file.setProcessInstanceId("process-914");
        when(controlledFileMapper.selectById(914L)).thenReturn(file);

        finalizationService.handleProcessInstanceStatusChanged(rejectEvent(914L));

        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(controlledFileMapper, never()).markRejectedAfterApprovalEvent(any(), any(), any(), any(), any(), any(), any(), any());
        verify(platformAdapter, never()).recordRejected(any(), any(), any(), any());
    }

    @Test
    void handleProcessInstanceStatusChanged_mismatchedProcessInstanceFailsBeforeStatusChange() {
        DccControlledFileDO file = buildRevisionApprovalCandidate(913L, 713L, 18L, 113L);
        file.setProcessInstanceId("different-process");
        when(controlledFileMapper.selectById(913L)).thenReturn(file);

        assertThrows(IllegalStateException.class,
                () -> finalizationService.handleProcessInstanceStatusChanged(approveEvent(913L)));

        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(platformAdapter, never()).recordApprovedReadyToPublish(any(), any(), any());
    }

    @Test
    void handleProcessInstanceStatusChanged_mismatchedRejectProcessFailsBeforeStatusChange() {
        DccControlledFileDO file = buildRevisionApprovalCandidate(915L, 715L, 18L, 115L);
        file.setProcessInstanceId("different-process");
        when(controlledFileMapper.selectById(915L)).thenReturn(file);

        assertThrows(IllegalStateException.class,
                () -> finalizationService.handleProcessInstanceStatusChanged(rejectEvent(915L)));

        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(controlledFileMapper, never()).markRejectedAfterApprovalEvent(any(), any(), any(), any(), any(), any(), any(), any());
        verify(platformAdapter, never()).recordRejected(any(), any(), any(), any());
    }

    @Test
    void handleProcessInstanceStatusChanged_rejectUsesConditionalApprovalStatusTransition() {
        DccControlledFileDO file = buildRevisionApprovalCandidate(916L, 716L, 18L, 116L);
        when(controlledFileMapper.selectById(916L)).thenReturn(file);
        when(controlledFileMapper.markRejectedAfterApprovalEvent(any(), eq(916L), eq("process-916"),
                eq(DccControlledFileProcessDefinitionKeys.LEGACY_APPROVAL),
                eq(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus()), any(), eq("资料不通过"),
                eq(99L))).thenReturn(1);

        finalizationService.handleProcessInstanceStatusChanged(rejectEvent(916L));

        verify(controlledFileMapper).markRejectedAfterApprovalEvent(any(), eq(916L), eq("process-916"),
                eq(DccControlledFileProcessDefinitionKeys.LEGACY_APPROVAL),
                eq(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus()), any(), eq("资料不通过"),
                eq(99L));
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(platformAdapter).recordRejected(file, 99L, "资料不通过", "process-916");
    }

    @Test
    void handleProcessInstanceStatusChanged_approvalStatusCasLostFailsClosed() {
        DccControlledFileDO file = buildRevisionApprovalCandidate(917L, 717L, 18L, 117L);
        file.setPublishedFileId(117L);
        file.setStampedFileId(117L);
        when(controlledFileMasterMapper.selectById(717L)).thenReturn(DccControlledFileMasterDO.builder().id(717L).build());
        stubCompleteOrdinaryApprovalRoster(file);
        when(controlledFileMapper.selectById(917L)).thenReturn(file);
        when(controlledFileMapper.markReadyToPublishAfterApproval(
                any(), eq(917L), any(), any(), any(), any(), any())).thenReturn(0);

        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> finalizationService.handleProcessInstanceStatusChanged(approveEvent(917L)));

        verify(platformAdapter, never()).recordApprovedReadyToPublish(any(), any(), any());
    }

    @Test
    void controlledFileMapper_exposesLockedReadAndConditionalStatusTransitions() {
        List<String> methods = Arrays.stream(DccControlledFileMapper.class.getDeclaredMethods())
                .map(Method::getName)
                .toList();

        assertTrue(methods.contains("selectByIdAndTenantForUpdate"));
        assertTrue(methods.contains("markReadyToPublishAfterApproval"));
        assertTrue(methods.contains("transitionStatus"));
        assertTrue(methods.contains("markFinalizationFailedWhenStatus"));
    }

    @Test
    void precheckPublishControlledFile_requiresDocControlRole() {
        DccControlledFileDO file = buildReadyToPublishCandidate(914L, 714L, 18L, 114L);
        file.setPublishedFileId(614L);
        file.setStampedFileId(614L);
        lenient().when(controlledFileMapper.selectById(914L)).thenReturn(file);
        lenient().when(permissionApi.hasAnyRoles(99L, "doc_control")).thenReturn(false);

        assertServiceException(() -> finalizationService.precheckPublishControlledFile(99L, 914L),
                CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);
    }

    @Test
    void precheckPublishControlledFile_requiresCategoryApprovePermission() {
        DccControlledFileDO file = buildReadyToPublishCandidate(915L, 715L, 18L, 115L);
        file.setPublishedFileId(615L);
        file.setStampedFileId(615L);
        lenient().when(controlledFileMapper.selectById(915L)).thenReturn(file);
        lenient().when(controlledFileMasterMapper.selectById(715L))
                .thenReturn(DccControlledFileMasterDO.builder().id(715L).build());
        lenient().when(categoryMapper.selectById(18L)).thenReturn(category(18L, false, false));
        lenient().when(permissionApi.hasAnyRoles(99L, "doc_control")).thenReturn(true);
        lenient().when(permissionApi.hasAnyPermissions(99L, "dcc:controlled-file:approve")).thenReturn(true);
        lenient().when(permissionSupport.hasCategoryPermission(18L, 99L,
                DccFileCategoryPermissionActionEnum.APPROVE)).thenReturn(false);

        assertServiceException(() -> finalizationService.precheckPublishControlledFile(99L, 915L),
                CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);
    }

    @Test
    void precheckPublishControlledFile_allowsStampGenerationDuringPublishEffect() throws Exception {
        DccControlledFileDO file = buildReadyToPublishCandidate(916L, 716L, 18L, 116L);
        file.setPublishedFileId(null);
        file.setStampedFileId(null);
        lenient().when(controlledFileMapper.selectById(916L)).thenReturn(file);
        lenient().when(controlledFileMasterMapper.selectById(716L))
                .thenReturn(DccControlledFileMasterDO.builder().id(716L).build());
        lenient().when(categoryMapper.selectById(18L)).thenReturn(category(18L, false, false));
        lenient().when(permissionApi.hasAnyRoles(99L, "doc_control")).thenReturn(true);
        lenient().when(permissionApi.hasAnyPermissions(99L, "dcc:controlled-file:approve")).thenReturn(true);
        lenient().when(permissionSupport.hasCategoryPermission(18L, 99L,
                DccFileCategoryPermissionActionEnum.APPROVE)).thenReturn(true);

        finalizationService.precheckPublishControlledFile(99L, 916L);

        verify(fileService, never()).createFile(any(), any(), any(), any());
        verify(pdfStampService, never()).stamp(any());
    }

    @Test
    void applyApprovedPublishControlledFile_followupSnapshotFailureFailsPublicationBeforeCompletionEvent() throws Exception {
        DccControlledFileDO file = buildReadyToPublishCandidate(923L, 723L, 18L, 123L);
        file.setIterationNo(1);
        file.setRevisionCode("B");
        file.setVersionNo("B/1");
        DccControlledFileDO previousActive = DccControlledFileDO.builder()
                .id(823L).masterId(723L).categoryId(18L).versionNo("A/1")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus()).build();
        when(controlledFileMapper.selectById(923L)).thenReturn(file, file);
        when(controlledFileMapper.selectById(823L)).thenReturn(previousActive);
        when(controlledFileMasterMapper.selectById(723L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(723L).categoryId(18L).currentActiveControlledFileId(823L)
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode()).build());
        when(categoryMapper.selectById(18L)).thenReturn(category(18L, false, false));
        when(permissionApi.hasAnyPermissions(99L, "dcc:controlled-file:approve")).thenReturn(true);
        doThrow(new IllegalStateException("follow-up snapshot insert failed"))
                .when(publicationFollowupService).recordPublishedRevision(file, previousActive);

        ServiceException error = assertThrows(ServiceException.class,
                () -> finalizationService.applyApprovedPublishControlledFile(99L, 923L, "publish-effect-failure"));

        assertTrue(error.getMessage().contains("follow-up snapshot insert failed"));
        verify(platformAdapter, never()).recordFinalized(any(), any(), any(), any());
        verify(transactionTemplate).executeWithoutResult(any());
        verify(finalizationFailureService).recordFailure(any(), eq(923L),
                eq(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus()), eq(99L),
                org.mockito.ArgumentMatchers.contains("follow-up snapshot insert failed"),
                eq("publish-effect-failure"));
        verify(obsoleteFileStorageService, never()).moveControlledFileArtifactsToObsoleteFolder(any());
    }

    @Test
    void precheckPublishControlledFile_readyCandidateWithoutApprovePermissionThrows() {
        DccControlledFileDO file = buildReadyToPublishCandidate(922L, 722L, 18L, 122L);
        lenient().when(controlledFileMapper.selectById(922L)).thenReturn(file);
        lenient().when(permissionApi.hasAnyPermissions(99L, "dcc:controlled-file:approve")).thenReturn(false);

        assertServiceException(() -> finalizationService.precheckPublishControlledFile(99L, 922L),
                CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);

        verify(pendingActionGuard, never()).assertNoPendingBusinessAction(any());
    }

    @Test
    void precheckPublishControlledFile_savedRecipientMissingFailsBeforePublicationSideEffects() {
        DccControlledFileDO file = buildReadyToPublishCandidate(925L, 725L, 18L, 125L);
        lenient().when(controlledFileMapper.selectById(925L)).thenReturn(file);
        lenient().when(controlledFileMasterMapper.selectById(725L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(725L)
                .categoryId(18L)
                .fileName("SOP-025")
                .fileNumber("FI-025")
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build());
        lenient().when(permissionApi.hasAnyPermissions(99L, "dcc:controlled-file:approve")).thenReturn(true);
        lenient().when(categoryMapper.selectById(18L)).thenReturn(category(18L, true, true));
        lenient().when(distributionMapper.selectListByControlledFileId(925L)).thenReturn(List.of(
                DccControlledFileDistributionDO.builder()
                        .id(2205L)
                        .controlledFileId(925L)
                        .departmentId(300L)
                        .distributionMedium(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())
                        .status(DccControlledFileDistributionStatusEnum.PENDING.getCode())
                        .build()));
        lenient().when(distributionRecipientMapper.selectListByDistributionId(2205L)).thenReturn(List.of(
                DccControlledFileDistributionRecipientDO.builder()
                        .id(3105L)
                        .distributionId(2205L)
                        .userId(501L)
                        .build(),
                DccControlledFileDistributionRecipientDO.builder()
                        .id(3106L)
                        .distributionId(2205L)
                        .userId(502L)
                        .build()));
        lenient().when(adminUserApi.getUserList(List.of(501L, 502L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(501L).setStatus(0)));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> finalizationService.precheckPublishControlledFile(99L, 925L));
        assertEquals(CONTROLLED_FILE_PUBLISH_NOT_ALLOWED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("distributionId=2205"));
        assertTrue(ex.getMessage().contains("502"));
        verify(pendingActionGuard).assertNoPendingBusinessAction(file);
        verify(trainingMapper, never()).insert(any(DccControlledFileTrainingDO.class));
        verify(messageJobMapper, never()).insert(any(DccControlledFileMessageJobDO.class));
        verify(fileMapper, never()).selectById(any());
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
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
    void applyApprovedPublishControlledFile_supersedesStalePendingIterationAndPreservesApprovalTime() {
        java.time.LocalDateTime approvedAt = java.time.LocalDateTime.of(2026, 9, 12, 9, 30);
        DccControlledFileDO candidate = buildReadyToPublishCandidate(920L, 720L, 18L, 120L);
        candidate.setVersionNo("B/1");
        candidate.setRevisionCode("B");
        candidate.setIterationNo(1);
        candidate.setApprovedTime(approvedAt);
        DccControlledFileDO active = DccControlledFileDO.builder().id(918L).masterId(720L)
                .versionNo("A/1").revisionCode("A").iterationNo(1)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus()).build();
        DccControlledFileDO stalePending = DccControlledFileDO.builder().id(919L).masterId(720L)
                .versionNo("A/2").revisionCode("A").iterationNo(2)
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus()).build();
        DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                .id(720L).currentActiveControlledFileId(918L)
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode()).build();
        when(controlledFileMapper.selectById(920L)).thenReturn(candidate);
        when(controlledFileMapper.selectById(918L)).thenReturn(active);
        when(controlledFileMasterMapper.selectById(720L)).thenReturn(master);
        when(controlledFileMapper.selectListByMasterId(720L)).thenReturn(List.of(active, stalePending, candidate));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);
        when(categoryMapper.selectById(18L)).thenReturn(category(18L, false, false));
        when(permissionApi.hasAnyPermissions(99L, "dcc:controlled-file:approve")).thenReturn(true);

        finalizationService.applyApprovedPublishControlledFile(99L, 920L, "publish-B1");

        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper, times(3)).updateById(updateCaptor.capture());
        List<DccControlledFileDO> updates = updateCaptor.getAllValues();
        assertTrue(updates.stream().anyMatch(update -> Long.valueOf(919L).equals(update.getId())
                && DccControlledFileStatusEnum.SUPERSEDED.getStatus().equals(update.getStatus())
                && Long.valueOf(920L).equals(update.getSupersededByFileId())));
        DccControlledFileDO activation = updates.stream()
                .filter(update -> Long.valueOf(920L).equals(update.getId()))
                .findFirst().orElseThrow();
        assertNull(activation.getApprovedTime());
        assertTrue(activation.getPublishedTime() != null);
        assertEquals(approvedAt, candidate.getApprovedTime());
    }

    @Test
    void handleProcessInstanceStatusChanged_approveCreatesActiveRevisionAndDownstreamRecords_newOrdinaryPolicy() {
        assertOrdinaryActivation("NEW", true, false, false);
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
        LocalDateTime approvedAt = LocalDateTime.of(2026, 9, 12, 9, 30);
        file.setApprovedTime(approvedAt);
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
        assertNull(fileCaptor.getValue().getApprovedTime());
        ArgumentCaptor<DccControlledFileMasterDO> masterCaptor = ArgumentCaptor.forClass(DccControlledFileMasterDO.class);
        verify(controlledFileMasterMapper).updateById(masterCaptor.capture());
        assertEquals(991L, masterCaptor.getValue().getCurrentActiveControlledFileId());
    }

    @Test
    void handleProcessInstanceStatusChanged_existingElectronicDistributionPlanDispatchesSelectedRecipientsAndActivates_newOrdinaryPolicy() {
        assertOrdinaryActivation("REVISION", true, false, true);
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
    void activateWithoutApproval_skipGovernance_pdfStampFailureFailsClosed() throws Exception {
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

        ServiceException ex = assertThrows(ServiceException.class,
                () -> finalizationService.activateWithoutApproval(993L, true));

        assertEquals(CONTROLLED_FILE_STAMP_GENERATION_FAILED.getCode(), ex.getCode());
        verify(fileService, never()).createFile(any(), any(), any(), any());
        verify(controlledFileMasterMapper, never()).updateById(any(DccControlledFileMasterDO.class));
        verify(distributionMapper, never()).insert(any(DccControlledFileDistributionDO.class));
        verify(trainingMapper, never()).insert(any(DccControlledFileTrainingDO.class));
        verify(trainingAssignmentMapper, never()).insert(any(DccControlledFileTrainingAssignmentDO.class));
        verify(messageJobMapper, never()).insert(any(DccControlledFileMessageJobDO.class));
        verify(platformAdapter, never()).recordFinalized(any(), any(), any(), any());
    }

    @Test
    void handleProcessInstanceStatusChanged_missingDistributionRuleMarksFailureAndThrows_newOrdinaryPolicy() {
        assertOrdinaryActivation("REVISION", true, false, false);
    }

    @Test
    void applyApprovedPublishControlledFile_candidateOlderThanCurrentActiveFailsBeforeSupersession() {
        DccControlledFileDO staleCandidate = buildReadyToPublishCandidate(924L, 724L, 18L, 124L);
        staleCandidate.setVersionNo("A/2");
        staleCandidate.setRevisionCode("A");
        staleCandidate.setIterationNo(2);
        DccControlledFileDO currentActive = DccControlledFileDO.builder()
                .id(824L)
                .masterId(724L)
                .categoryId(18L)
                .fileName("SOP-001")
                .fileNumber("FI-001")
                .versionNo("B/1")
                .revisionCode("B")
                .iterationNo(1)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
        when(controlledFileMapper.selectById(924L)).thenReturn(staleCandidate, staleCandidate);
        when(controlledFileMapper.selectById(824L)).thenReturn(currentActive);
        when(controlledFileMasterMapper.selectById(724L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(724L)
                .categoryId(18L)
                .currentActiveControlledFileId(824L)
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build());
        when(permissionApi.hasAnyPermissions(99L, "dcc:controlled-file:approve")).thenReturn(true);

        assertServiceException(() -> finalizationService.applyApprovedPublishControlledFile(
                99L, 924L, "publish-stale-revision"), CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);

        verify(controlledFileMasterMapper, never()).updateById(any(DccControlledFileMasterDO.class));
        verify(publicationFollowupService, never()).recordPublishedRevision(any(), any());
        verify(platformAdapter, never()).recordFinalized(any(), any(), any(), any());
    }

    @Test
    void handleProcessInstanceStatusChanged_completionAuditFailureRecordsOriginalStatus() {
        DccControlledFileDO file = buildFinalizingFile(918L, 718L, 18L, 118L);
        file.setPublishedFileId(618L);
        file.setStampedFileId(618L);
        when(controlledFileMapper.selectById(918L)).thenReturn(file);
        when(controlledFileMasterMapper.selectById(718L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(718L).categoryId(18L).fileName("SOP-018").fileNumber("FI-018").build());
        when(categoryMapper.selectById(18L)).thenReturn(category(18L, false, false));
        doThrow(new IllegalStateException("completion audit failed"))
                .when(platformAdapter).recordFinalized(any(), eq(file), eq(99L), eq("process-918"));

        assertThrows(ServiceException.class,
                () -> finalizationService.handleProcessInstanceStatusChanged(approveEvent(918L)));

        verify(finalizationFailureService).recordFailure(any(), eq(918L),
                eq(DccControlledFileStatusEnum.FINALIZING.getStatus()), eq(99L),
                eq("completion audit failed"), eq("process-918"));
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
        stubCompleteOrdinaryApprovalRoster(file);
        stubStampedArtifact(102L, 602L);
        when(permissionApi.hasAnyPermissions(99L, "dcc:controlled-file:approve")).thenReturn(true);
        when(platformAdapter.nextFinalizationRetryEventKey(file))
                .thenReturn("dcc-finalization-retry:902:attempt-1");

        finalizationService.retryStamp(99L, 902L);

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.ACTIVE.getStatus(), fileCaptor.getValue().getStatus());
        assertEquals(602L, fileCaptor.getValue().getPublishedFileId());
        assertEquals(602L, fileCaptor.getValue().getStampedFileId());
        verify(platformAdapter).recordFinalizationRetried(file, 99L, "dcc-finalization-retry:902:attempt-1");
        verify(platformAdapter).recordFinalized(null, file, 99L, "dcc-finalization-retry:902:attempt-1");
    }

    @Test
    void retryStamp_failedRetryThenSecondRetryUsesNewAttemptEventKey() throws Exception {
        DccControlledFileDO file = buildFinalizingFile(902L, 702L, 12L, 102L);
        file.setStatus(DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus());
        file.setFinalizationError("initial failure");
        when(controlledFileMapper.selectById(902L)).thenReturn(file, file, file, file);
        when(controlledFileMasterMapper.selectById(702L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(702L).categoryId(12L).fileName("SOP-003").fileNumber("FI-003").build());
        when(categoryMapper.selectById(12L)).thenReturn(category(12L, false, false));
        stubCompleteOrdinaryApprovalRoster(file);
        stubStampedArtifact(102L, 602L);
        when(permissionApi.hasAnyPermissions(99L, "dcc:controlled-file:approve")).thenReturn(true);
        when(platformAdapter.nextFinalizationRetryEventKey(file))
                .thenReturn("dcc-finalization-retry:902:attempt-1",
                        "dcc-finalization-retry:902:attempt-2");
        doThrow(new IllegalStateException("stamp binding failed"))
                .doNothing()
                .when(signatureBindingService).bindPublishedCopy(eq(file), eq(602L), eq(99L), any());

        ServiceException firstRetryFailure = assertThrows(ServiceException.class,
                () -> finalizationService.retryStamp(99L, 902L));
        assertEquals(CONTROLLED_FILE_STAMP_GENERATION_FAILED.getCode(), firstRetryFailure.getCode());

        finalizationService.retryStamp(99L, 902L);

        ArgumentCaptor<String> retryEventKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(platformAdapter, times(2)).recordFinalizationRetried(eq(file), eq(99L),
                retryEventKeyCaptor.capture());
        assertEquals(List.of("dcc-finalization-retry:902:attempt-1",
                "dcc-finalization-retry:902:attempt-2"), retryEventKeyCaptor.getAllValues());
        verify(finalizationFailureService).recordFailure(any(), eq(902L),
                eq(DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus()), eq(99L),
                eq("stamp binding failed"), eq("dcc-finalization-retry:902:attempt-1"));
        verify(platformAdapter).recordFinalized(null, file, 99L,
                "dcc-finalization-retry:902:attempt-2");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"role", "permission"})
    void retryStamp_missingPublishAuthorizationMustNotAdvance(String missing) {
        DccControlledFileDO file = buildFinalizingFile(902L, 702L, 12L, 102L);
        file.setStatus(DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus());
        when(controlledFileMapper.selectById(902L)).thenReturn(file);
        when(permissionApi.hasAnyRoles(99L, "doc_control")).thenReturn(!"role".equals(missing));
        if (!"role".equals(missing)) {
            when(permissionApi.hasAnyPermissions(99L, "dcc:controlled-file:approve"))
                    .thenReturn(!"permission".equals(missing));
        }
        assertServiceException(() -> finalizationService.retryStamp(99L, 902L), CONTROLLED_FILE_PUBLISH_NOT_ALLOWED);
        verify(platformAdapter, never()).recordFinalizationRetried(any(), any(), any());
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
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

        verify(finalizationFailureService).recordFailure(any(), eq(994L),
                eq(DccControlledFileStatusEnum.FINALIZING.getStatus()), eq(99L),
                eq("OnlyOffice conversion failed"), eq("process-994"));
        verify(pdfStampService, never()).stamp(any());
    }

    @Test
    void retryStamp_invalidStatus_throws() {
        when(controlledFileMapper.selectById(903L)).thenReturn(DccControlledFileDO.builder()
                .id(903L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());

        assertServiceException(() -> finalizationService.retryStamp(99L, 903L), CONTROLLED_FILE_STAMP_RETRY_NOT_ALLOWED);
    }

    @Test
    void readPreviewFile_legacyFinalizationEntryFailsClosedWithoutReadSideEffects() {
        assertServiceException(() -> finalizationService.readPreviewFile(99L, 904L),
                CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        verify(queryService, never()).readPreviewFile(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void handleProcessInstanceStatusChanged_trainingRequiredCategory_entersTrainingInsteadOfCreatingFormalDistribution_newOrdinaryPolicy() {
        assertOrdinaryActivation("NEW", true, true, false);
    }

    @Test
    void handleProcessInstanceStatusChanged_trainingRequiredUsesSavedSingleFileDistributionPlan_newOrdinaryPolicy() {
        assertOrdinaryActivation("REVISION", true, true, true);
    }

    @Test
    void handleProcessInstanceStatusChanged_trainingRequiredSavedRecipientDisabledFailsBeforeTrainingRows_newOrdinaryPolicy() {
        assertOrdinaryActivation("REVISION", true, true, true);
    }

    @Test
    void releaseManualDistribution_rejectsRetiredOrdinaryActionBeforeSideEffects() {
        DccControlledFileDO file = buildFinalizingFile(907L, 707L, 17L, 107L);
        file.setStatus(DccControlledFileStatusEnum.PENDING_MANUAL_DISTRIBUTION.getStatus());
        when(controlledFileMapper.selectById(907L)).thenReturn(file);
        assertServiceException(() -> finalizationService.releaseManualDistribution(99L, 907L),
                CONTROLLED_FILE_MANUAL_RELEASE_NOT_ALLOWED);
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        org.mockito.Mockito.verifyNoInteractions(distributionMapper, categoryMapper, trainingMapper,
                controlledFileMasterMapper, platformAdapter);
    }

    @Test
    void releaseManualDistribution_requiresControlledArtifactAndNeverReentersApproval() {
        var file=distributionReady(908L);
        finalizationService.releaseManualDistribution(99L,908L);
        verify(bpmTaskService,never()).triggerTask(anyString(),anyString());
        verify(controlledFileMapper,never()).transitionStatus(any(),any(),any(),any(),any());
        verify(controlledFileMapper).updateById(org.mockito.ArgumentMatchers.<DccControlledFileDO>argThat(row ->
                row.getId().equals(908L) && row.getDistributedTime()!=null && row.getStatus()==null && row.getEffectiveDate()==null));
        assertEquals("CONTROLLED_PENDING_EFFECTIVE",file.getStatus());
    }

    @Test
    void releaseManualDistribution_nativeTrainingEvidenceDoesNotWaitForLegacyTrainingRows() {
        var file=distributionReady(909L); file.setNeedTraining(true); file.setTrainingRecordFileId(110L);
        finalizationService.releaseManualDistribution(99L,909L);
        verify(trainingMapper,never()).selectListByControlledFileId(909L);
        verify(bpmTaskService,never()).triggerTask(anyString(),anyString());
    }

    @Test
    void releaseManualDistribution_realStorageFailureDoesNotMarkCompleted() {
        distributionReady(910L);
        doThrow(new IllegalStateException("distribution persistence failed")).when(distributionRecipientMapper)
                .updateById(any(DccControlledFileDistributionRecipientDO.class));
        assertThrows(IllegalStateException.class,()->finalizationService.releaseManualDistribution(99L,910L));
        verify(controlledFileMapper,never()).updateById(any(DccControlledFileDO.class));
        verify(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void releaseManualDistribution_lostCompletionWriteFailsAndDoesNotTriggerBpm() {
        distributionReady(911L);
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(0);
        assertThrows(IllegalStateException.class,()->finalizationService.releaseManualDistribution(99L,911L));
        verify(bpmTaskService,never()).triggerTask(anyString(),anyString());
    }

    private DccControlledFileDO distributionReady(long id) {
        var file=buildFinalizingFile(id,700L,17L,108L);
        file.setStatus("CONTROLLED_PENDING_EFFECTIVE"); file.setProcessDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD);
        file.setControlledTime(LocalDateTime.now()); file.setEffectiveDate(java.time.LocalDate.now().plusDays(3));
        file.setDistributionPayloadHash("a".repeat(64));
        when(controlledFileMapper.selectById(id)).thenReturn(file);
        when(permissionSupport.hasCategoryPermission(17L,99L,DccFileCategoryPermissionActionEnum.DISTRIBUTE)).thenReturn(true);
        when(categoryMapper.selectById(17L)).thenReturn(category(17L,true,false));
        when(distributionMapper.selectListByControlledFileId(id)).thenReturn(List.of(DccControlledFileDistributionDO.builder()
                .id(id+1000).controlledFileId(id).departmentId(31L).distributionMedium("PUBLIC_FOLDER").build()));
        when(distributionRecipientMapper.selectListByDistributionId(id+1000)).thenReturn(List.of(DccControlledFileDistributionRecipientDO.builder()
                .id(id+2000).distributionId(id+1000).userId(501L).build()));
        when(adminUserApi.getUserList(List.of(501L))).thenReturn(List.of(new AdminUserRespDTO().setId(501L).setStatus(0)));
        return file;
    }

    @Test
    void nativeDistributionCannotUseCategoryDefaultsWithoutAConfirmedPerFilePayload() {
        var file=buildFinalizingFile(930L,700L,17L,108L);
        file.setStatus("CONTROLLED_PENDING_EFFECTIVE");
        file.setProcessDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD);
        file.setControlledTime(LocalDateTime.now());
        when(controlledFileMapper.selectById(930L)).thenReturn(file);
        when(permissionSupport.hasCategoryPermission(17L,99L,DccFileCategoryPermissionActionEnum.DISTRIBUTE)).thenReturn(true);
        org.mockito.Mockito.lenient().when(categoryMapper.selectById(17L)).thenReturn(category(17L,true,false));
        org.mockito.Mockito.lenient().when(distributionRuleMapper.selectList(any(SFunction.class),eq(17L))).thenReturn(List.of(
                DccFileCategoryDistributionRuleDO.builder().categoryId(17L).departmentId(31L).active(true)
                        .distributionMedium("PAPER").build()));
        assertThrows(RuntimeException.class,()->finalizationService.releaseManualDistribution(99L,930L));
        verify(distributionMapper,never()).insert(any(DccControlledFileDistributionDO.class));
        verify(controlledFileMapper,never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void handleProcessInstanceStatusChanged_paperDistributionSkipsDigitalRecipientsButSnapshotsMedium_newOrdinaryPolicy() {
        assertOrdinaryActivation("NEW", true, false, false);
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
                .id(id).tenantId(1L)
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
                .processType(DccControlledFileProcessTypeEnum.CONTROLLED_FILE.getCode())
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-1")
                .build();
    }

    private static DccControlledFileDO buildRevisionApprovalCandidate(Long id, Long masterId, Long categoryId,
                                                                       Long sourceFileId) {
        DccControlledFileDO file = buildFinalizingFile(id, masterId, categoryId, sourceFileId);
        file.setVersionNo("V2.0");
        file.setEffectiveDate(java.time.LocalDate.now());
        file.setStatus(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus());
        file.setChangeType(DccControlledFileChangeTypeEnum.REVISION.getCode());
        file.setProcessInstanceId("process-" + id);
        return file;
    }

    private static DccControlledFileDO buildReadyToPublishCandidate(Long id, Long masterId, Long categoryId,
                                                                     Long sourceFileId) {
        DccControlledFileDO file = buildRevisionApprovalCandidate(id, masterId, categoryId, sourceFileId);
        file.setStatus(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus());
        file.setPublishedFileId(sourceFileId);
        file.setStampedFileId(sourceFileId);
        file.setProcessType(DccControlledFileProcessTypeEnum.EXTERNAL_REVIEW.getCode());
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
        return approveEvent(fileId, DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY);
    }

    private static BpmProcessInstanceStatusEvent approveEvent(Long fileId, String processDefinitionKey) {
        BpmProcessInstanceStatusEvent event = new BpmProcessInstanceStatusEvent(new Object());
        event.setId("process-" + fileId);
        event.setProcessDefinitionKey(processDefinitionKey);
        event.setBusinessKey(String.valueOf(fileId));
        event.setStatus(BpmProcessInstanceStatusEnum.APPROVE.getStatus());
        event.setActorUserId(99L);
        return event;
    }

    private static BpmProcessInstanceStatusEvent rejectEvent(Long fileId) {
        BpmProcessInstanceStatusEvent event = new BpmProcessInstanceStatusEvent(new Object());
        event.setId("process-" + fileId);
        event.setProcessDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY);
        event.setBusinessKey(String.valueOf(fileId));
        event.setStatus(BpmProcessInstanceStatusEnum.REJECT.getStatus());
        event.setActorUserId(99L);
        event.setReason("资料不通过");
        return event;
    }
}
