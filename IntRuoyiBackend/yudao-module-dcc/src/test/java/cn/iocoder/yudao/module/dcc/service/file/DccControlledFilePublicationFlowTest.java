package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryTrainingRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
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
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileMasterStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.Consumer;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_STAMP_GENERATION_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFilePublicationFlowTest extends BaseMockitoUnitTest {

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
    private DccObsoleteFileStorageService obsoleteFileStorageService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private DccControlledContentAdapter platformAdapter;
    @Mock
    private DccControlledFileSignatureBindingService signatureBindingService;

    @InjectMocks
    private DccControlledFileFinalizationServiceImpl finalizationService;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void handleProcessInstanceStatusChanged_supersedesPreviousActiveRevision() throws Exception {
        DccControlledFileDO currentFile = DccControlledFileDO.builder()
                .id(900L)
                .masterId(700L)
                .categoryId(10L)
                .sourceFileId(100L)
                .originalFileId(100L)
                .fileName("SOP-001")
                .title("SOP-001")
                .fileNumber("FI-001")
                .versionNo("2.0")
                .status(DccControlledFileStatusEnum.FINALIZING.getStatus())
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-2")
                .build();
        DccControlledFileDO previousActive = DccControlledFileDO.builder()
                .id(800L)
                .masterId(700L)
                .categoryId(10L)
                .sourceFileId(99L)
                .publishedFileId(99L)
                .fileName("SOP-001")
                .fileNumber("FI-001")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
        DccControlledFileMasterDO master = DccControlledFileMasterDO.builder()
                .id(700L)
                .categoryId(10L)
                .fileName("SOP-001")
                .fileNumber("FI-001")
                .currentActiveControlledFileId(800L)
                .status(DccControlledFileMasterStatusEnum.ACTIVE_CHAIN.getCode())
                .build();
        when(controlledFileMapper.selectById(900L)).thenReturn(currentFile, currentFile);
        when(controlledFileMapper.selectById(800L)).thenReturn(previousActive);
        when(controlledFileMasterMapper.selectById(700L)).thenReturn(master);
        when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).active(Boolean.TRUE).distributionRequired(Boolean.FALSE).trainingRequired(Boolean.FALSE).build());
        stubStampedArtifact(100L, 9100L);

        finalizationService.handleProcessInstanceStatusChanged(approveEvent(900L));

        verify(platformAdapter).recordFinalizationStarted(eq(currentFile), any(), any());
        verify(platformAdapter).recordFinalized(eq(previousActive), eq(currentFile), any(), any());
        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper, org.mockito.Mockito.times(2)).updateById(updateCaptor.capture());
        List<DccControlledFileDO> updates = updateCaptor.getAllValues();
        assertEquals(DccControlledFileStatusEnum.SUPERSEDED.getStatus(),
                updates.stream().filter(item -> item.getId().equals(800L)).findFirst().orElseThrow().getStatus());
        assertEquals(900L,
                updates.stream().filter(item -> item.getId().equals(800L)).findFirst().orElseThrow().getSupersededByFileId());
        assertEquals(DccControlledFileStatusEnum.ACTIVE.getStatus(),
                updates.stream().filter(item -> item.getId().equals(900L)).findFirst().orElseThrow().getStatus());
        verify(obsoleteFileStorageService).moveControlledFileArtifactsToObsoleteFolder(previousActive);
    }

    @Test
    void handleProcessInstanceStatusChanged_trainingRecipientResolutionFailureDoesNotPartiallyPublish() throws Exception {
        DccControlledFileDO currentFile = DccControlledFileDO.builder()
                .id(901L)
                .masterId(701L)
                .categoryId(11L)
                .sourceFileId(101L)
                .originalFileId(101L)
                .fileName("SOP-010")
                .title("SOP-010")
                .fileNumber("FI-010")
                .versionNo("1.0")
                .status(DccControlledFileStatusEnum.FINALIZING.getStatus())
                .processDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                .processInstanceId("proc-3")
                .build();
        when(controlledFileMapper.selectById(901L)).thenReturn(currentFile, currentFile);
        when(controlledFileMasterMapper.selectById(701L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(701L).categoryId(11L).fileName("SOP-010").fileNumber("FI-010").currentActiveControlledFileId(801L).build());
        when(categoryMapper.selectById(11L)).thenReturn(DccFileCategoryDO.builder()
                .id(11L).active(Boolean.TRUE).distributionRequired(Boolean.FALSE).trainingRequired(Boolean.TRUE).build());
        stubStampedArtifact(101L, 9101L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> finalizationService.handleProcessInstanceStatusChanged(approveEvent(901L)));

        assertEquals(CONTROLLED_FILE_STAMP_GENERATION_FAILED.getCode(), ex.getCode());
        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(updateCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus(), updateCaptor.getValue().getStatus());
        verify(platformAdapter).recordFinalizationStarted(eq(currentFile), any(), any());
        verify(platformAdapter).recordFinalizationFailed(eq(currentFile), any(), any(), any());
        verify(platformAdapter, never()).recordFinalized(any(), any(), any(), any());
        verify(controlledFileMasterMapper, never()).updateById(any(DccControlledFileMasterDO.class));
        verify(trainingMapper, never()).insert(any(DccControlledFileTrainingDO.class));
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
