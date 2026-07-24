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
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingDO;
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
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingProgressMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileMessageJobStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_STAMP_GENERATION_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileMessageOutboxTest extends BaseMockitoUnitTest {

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
    private AdminUserApi adminUserApi;
    @Mock
    private NotifyMessageSendApi notifyMessageSendApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private DccControlledContentAdapter platformAdapter;

    private DccControlledFileMessageDeliveryService messageDeliveryService;
    @InjectMocks
    private DccControlledFileFinalizationServiceImpl finalizationService;

    private final AtomicLong distributionIdGenerator = new AtomicLong(5100L);
    private final AtomicLong trainingIdGenerator = new AtomicLong(6100L);

    @BeforeEach
    void setUp() {
        messageDeliveryService = new DccControlledFileMessageDeliveryService();
        ReflectionTestUtils.setField(messageDeliveryService, "messageJobMapper", messageJobMapper);
        ReflectionTestUtils.setField(messageDeliveryService, "notifyMessageSendApi", notifyMessageSendApi);
        ReflectionTestUtils.setField(messageDeliveryService, "controlledFileMapper", controlledFileMapper);
        ReflectionTestUtils.setField(messageDeliveryService, "distributionMapper", distributionMapper);
        ReflectionTestUtils.setField(messageDeliveryService, "trainingMapper", trainingMapper);
        ReflectionTestUtils.setField(finalizationService, "messageDeliveryService", messageDeliveryService);
        ReflectionTestUtils.setField(finalizationService, "platformAdapter", platformAdapter);
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
    }

    @Test
    void handleProcessInstanceStatusChanged_trainingGatedRevisionPersistsTrainingOutboxJobsOnly() throws Exception {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(920L)
                .masterId(720L)
                .categoryId(20L)
                .sourceFileId(120L)
                .originalFileId(120L)
                .fileName("SOP-020")
                .title("SOP-020")
                .fileNumber("FI-020")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.FINALIZING.getStatus())
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-4")
                .build();
        when(controlledFileMapper.selectById(920L)).thenReturn(file, file);
        when(controlledFileMasterMapper.selectById(720L)).thenReturn(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO.builder()
                .id(720L).categoryId(20L).fileName("SOP-020").fileNumber("FI-020").build());
        when(categoryMapper.selectById(20L)).thenReturn(DccFileCategoryDO.builder()
                .id(20L).active(Boolean.TRUE).distributionRequired(Boolean.TRUE).trainingRequired(Boolean.TRUE).build());
        when(distributionRuleMapper.selectList(
                org.mockito.ArgumentMatchers.<SFunction<DccFileCategoryDistributionRuleDO, ?>>any(), eq(20L))).thenReturn(List.of(
                DccFileCategoryDistributionRuleDO.builder().id(10L).categoryId(20L).departmentId(500L).active(Boolean.TRUE).build()));
        when(adminUserApi.getUserListByDeptIds(List.of(500L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(701L),
                new AdminUserRespDTO().setId(702L)));
        when(notifyMessageSendApi.sendSingleMessageToAdmin(any(NotifySendSingleToUserReqDTO.class)))
                .thenReturn(9101L, 9102L);
        stubStampedArtifact(120L, 9120L);

        finalizationService.handleProcessInstanceStatusChanged(approveEvent(920L));

        ArgumentCaptor<DccControlledFileMessageJobDO> messageCaptor = ArgumentCaptor.forClass(DccControlledFileMessageJobDO.class);
        verify(messageJobMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        List<DccControlledFileMessageJobDO> jobs = messageCaptor.getAllValues();
        assertEquals(List.of("TRAINING", "TRAINING"),
                jobs.stream().map(DccControlledFileMessageJobDO::getBusinessType).toList());
        assertEquals(List.of(701L, 702L),
                jobs.stream().map(DccControlledFileMessageJobDO::getRecipientUserId).toList());
        assertEquals(List.of(DccControlledFileMessageJobStatusEnum.PENDING.getCode(),
                        DccControlledFileMessageJobStatusEnum.PENDING.getCode()),
                jobs.stream().map(DccControlledFileMessageJobDO::getStatus).toList());
    }

    @Test
    void handleProcessInstanceStatusChanged_messagePersistenceFailureMarksFinalizationFailed() throws Exception {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(921L)
                .masterId(721L)
                .categoryId(21L)
                .sourceFileId(121L)
                .originalFileId(121L)
                .fileName("SOP-021")
                .title("SOP-021")
                .fileNumber("FI-021")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.FINALIZING.getStatus())
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-5")
                .build();
        when(controlledFileMapper.selectById(921L)).thenReturn(file, file);
        when(controlledFileMasterMapper.selectById(721L)).thenReturn(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO.builder()
                .id(721L).categoryId(21L).fileName("SOP-021").fileNumber("FI-021").build());
        when(categoryMapper.selectById(21L)).thenReturn(DccFileCategoryDO.builder()
                .id(21L).active(Boolean.TRUE).distributionRequired(Boolean.TRUE).trainingRequired(Boolean.FALSE).build());
        when(distributionRuleMapper.selectList(
                org.mockito.ArgumentMatchers.<SFunction<DccFileCategoryDistributionRuleDO, ?>>any(), eq(21L))).thenReturn(List.of(
                DccFileCategoryDistributionRuleDO.builder().id(12L).categoryId(21L).departmentId(501L).active(Boolean.TRUE).build()));
        when(adminUserApi.getUserListByDeptIds(List.of(501L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(801L)));
        stubStampedArtifact(121L, 9121L);
        doAnswer(invocation -> {
            throw new IllegalStateException("message persistence failed");
        }).when(messageJobMapper).insert(any(DccControlledFileMessageJobDO.class));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> finalizationService.handleProcessInstanceStatusChanged(approveEvent(921L)));

        assertEquals(CONTROLLED_FILE_STAMP_GENERATION_FAILED.getCode(), ex.getCode());
        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(updateCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus(), updateCaptor.getValue().getStatus());
        verify(controlledFileMasterMapper, never()).updateById(any(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO.class));
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

    private static BpmProcessInstanceStatusEvent approveEvent(Long fileId) {
        BpmProcessInstanceStatusEvent event = new BpmProcessInstanceStatusEvent(new Object());
        event.setProcessDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY);
        event.setBusinessKey(String.valueOf(fileId));
        event.setStatus(BpmProcessInstanceStatusEnum.APPROVE.getStatus());
        return event;
    }
}
