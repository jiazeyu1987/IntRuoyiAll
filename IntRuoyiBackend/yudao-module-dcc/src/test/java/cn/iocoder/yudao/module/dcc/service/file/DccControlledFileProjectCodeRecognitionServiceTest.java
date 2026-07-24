package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileProjectCodeRecognitionRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRecognitionClaimDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRecognitionRecordDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeAliasMappingDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRecognitionClaimMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRecognitionRecordMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAliasMappingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryLifecycleStageEnum;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_AMBIGUOUS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_IN_PROGRESS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_INVALID_CANDIDATE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_NO_CANDIDATE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_SOURCE_MISSING;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileProjectCodeRecognitionServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Mock
    private DccProjectCodeMapper projectCodeMapper;
    @Mock
    private DccProjectCodeAliasMappingMapper projectCodeAliasMappingMapper;
    @Mock
    private DccFileDirectoryMapper directoryMapper;
    @Mock
    private DccFileCategoryMapper categoryMapper;
    @Mock
    private DccFileTypeTaxonomyAdminService fileTypeTaxonomyAdminService;
    @Mock
    private DccControlledFileRecognitionRecordMapper recognitionRecordMapper;
    @Mock
    private DccControlledFileRecognitionClaimMapper recognitionClaimMapper;
    @Mock
    private FileService fileService;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private DccProjectCodeRecognitionCodexCliClient codexCliClient;
    @Mock
    private DccProjectCodeRecognitionProperties recognitionProperties;
    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private DccControlledFileProjectCodeRecognitionServiceImpl recognitionService;

    @Test
    void recognizeProjectCodeStartsWriteTransactionOnlyAfterCodexReturns() throws Exception {
        byte[] sourceContent = new byte[] {1, 2, 3, 4};
        DccProjectCodeDO candidate = projectCode(700L, "项目A", "CODE-A");
        mockReadableFile(fileWithSource(), sourceContent, List.of(candidate));
        AtomicInteger sequence = new AtomicInteger();
        AtomicInteger codexSequence = new AtomicInteger();
        AtomicInteger transactionSequence = new AtomicInteger();
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> {
            transactionSequence.compareAndSet(0, sequence.incrementAndGet());
            return new SimpleTransactionStatus();
        });
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class))).thenAnswer(invocation -> {
            codexSequence.set(sequence.incrementAndGet());
            return new DccProjectCodeRecognitionResult(700L,
                    DccProjectCodeRecognitionMatchType.PROJECT_CODE, "CODE-A");
        });
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);
        when(recognitionRecordMapper.upsert(any(DccControlledFileRecognitionRecordDO.class))).thenReturn(1);

        recognitionService.recognizeProjectCode(99L, 900L);

        assertTrue(codexSequence.get() > 0, "Codex recognition should be executed");
        assertTrue(transactionSequence.get() > codexSequence.get(),
                "The database write transaction must start after the slow Codex call returns");
    }

    @Test
    void recognizeProjectCode_uniqueProjectNamePersistsProjectCodeSnapshotAndKeepsProductMaster() throws Exception {
        byte[] sourceContent = new byte[] {1, 2, 3, 4};
        DccProjectCodeDO candidate = projectCode(700L, "万级净化车间沉降菌测试报告", "CODE-A");
        DccControlledFileDO sourceFileChain = fileWithSource();
        sourceFileChain.setFileName("6.4-51.xls");
        sourceFileChain.setTitle("6.4-51");
        mockReadableFile(sourceFileChain, sourceContent, List.of(candidate));
        when(recognitionProperties.getVersion()).thenReturn("project-code-v1");
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class)))
                .thenReturn(new DccProjectCodeRecognitionResult(700L,
                        DccProjectCodeRecognitionMatchType.PROJECT_NAME, "万级净化车间沉降菌测试报告"));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);
        when(recognitionRecordMapper.upsert(any(DccControlledFileRecognitionRecordDO.class))).thenReturn(1);

        DccControlledFileProjectCodeRecognitionRespVO respVO =
                recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals(900L, respVO.getControlledFileId());
        assertEquals(700L, respVO.getDccProjectCodeId());
        assertEquals("万级净化车间沉降菌测试报告", respVO.getProjectName());
        assertEquals("CODE-A", respVO.getProjectCode());
        assertEquals("PROJECT_NAME", respVO.getMatchType());
        assertEquals("万级净化车间沉降菌测试报告", respVO.getMatchText());
        assertEquals("CODEX_CLI_CONTENT", respVO.getRecognitionMethod());
        assertEquals("project-code-v1", respVO.getRecognitionVersion());

        ArgumentCaptor<DccProjectCodeRecognitionCommand> commandCaptor =
                ArgumentCaptor.forClass(DccProjectCodeRecognitionCommand.class);
        verify(codexCliClient).recognizeProjectCode(commandCaptor.capture());
        assertEquals(900L, commandCaptor.getValue().controlledFileId());
        assertEquals(321L, commandCaptor.getValue().sourceFileId());
        assertEquals("6.4-51.xls", commandCaptor.getValue().sourceFileName());
        assertEquals("application/vnd.ms-excel", commandCaptor.getValue().contentType());
        assertArrayEquals(sourceContent, commandCaptor.getValue().sourceContent());
        assertEquals(1, commandCaptor.getValue().candidates().size());
        assertEquals(700L, commandCaptor.getValue().candidates().get(0).id());

        DccControlledFileDO updated = captureUpdatedFile();
        assertEquals(900L, updated.getId());
        assertNull(updated.getFileName(), "recognition must not rewrite controlled-file chain file_name");
        assertNull(updated.getTitle(), "recognition must not rewrite controlled-file title from the source file name");
        assertEquals(700L, updated.getDccProjectCodeId());
        assertEquals("万级净化车间沉降菌测试报告", updated.getProductName());
        assertEquals("CODE-A", updated.getProductCode());
        assertEquals("PROJECT_NAME", updated.getProjectCodeRecognitionType());
        assertEquals("万级净化车间沉降菌测试报告", updated.getProjectCodeRecognitionText());
        assertEquals(99L, updated.getProjectCodeRecognizedBy());
        assertNotNull(updated.getProjectCodeRecognizedTime());
        assertNull(updated.getProductMasterId(), "recognition update must not modify MDM productMasterId");

        verify(controlledFileMasterMapper, never()).updateById(any(DccControlledFileMasterDO.class));

        DccControlledFileRecognitionRecordDO record = captureRecognitionRecord();
        assertEquals(900L, record.getControlledFileId());
        assertEquals("BASIC_INFO", record.getRecognitionScope());
        assertEquals("CODEX_CLI_CONTENT", record.getRecognitionMethod());
        assertEquals("project-code-v1", record.getRecognitionVersion());
        assertEquals("SUCCESS", record.getStatus());
        assertEquals(700L, record.getMatchedProjectCodeId());
        assertEquals("CODE-A", record.getRecognizedProductCode());
        assertEquals("万级净化车间沉降菌测试报告", record.getRecognizedProductName());
        assertEquals("PROJECT_NAME", record.getMatchType());
        assertEquals("万级净化车间沉降菌测试报告", record.getMatchText());
        assertEquals(99L, record.getRecognizedBy());
        assertEquals(321L, record.getSourceFileId());
        assertEquals(0L, record.getTenantId());
        assertNotNull(record.getRecognizedTime());
    }

    @Test
    void recognizeProjectCode_uniqueProjectCodeInSourceFileNameSkipsCodexAndFileContentRead() throws Exception {
        DccProjectCodeDO candidate = projectCode(700L, "万级浮游菌控制趋势图", "RE-STM-MM-017-04");
        mockReadableFile(fileWithSource(), sourceFile("RE-STM-MM-017-04（A∕0）万级浮游菌控制趋势图.pdf"),
                null, List.of(candidate));
        when(recognitionProperties.getVersion()).thenReturn("project-code-v1");
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);
        when(recognitionRecordMapper.upsert(any(DccControlledFileRecognitionRecordDO.class))).thenReturn(1);

        DccControlledFileProjectCodeRecognitionRespVO respVO =
                recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals(700L, respVO.getDccProjectCodeId());
        assertEquals("万级浮游菌控制趋势图", respVO.getProjectName());
        assertEquals("RE-STM-MM-017-04", respVO.getProjectCode());
        assertEquals("PROJECT_CODE", respVO.getMatchType());
        assertEquals("RE-STM-MM-017-04", respVO.getMatchText());
        assertEquals("FILE_NAME_SHORTCUT", respVO.getRecognitionMethod());
        assertEquals("project-code-v1", respVO.getRecognitionVersion());
        verify(fileService, never()).getFileContent(28L, "qms/6.4-51.xls");
        verify(codexCliClient, never()).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));

        DccControlledFileDO updated = captureUpdatedFile();
        assertNull(updated.getFileName(), "recognition must not rewrite controlled-file chain file_name");
        assertNull(updated.getTitle(), "recognition must not rewrite controlled-file title from the source file name");
        assertEquals("万级浮游菌控制趋势图", updated.getProductName());
        assertEquals("RE-STM-MM-017-04", updated.getProductCode());
        assertEquals("PROJECT_CODE", updated.getProjectCodeRecognitionType());
        assertEquals("RE-STM-MM-017-04", updated.getProjectCodeRecognitionText());

        DccControlledFileRecognitionRecordDO record = captureRecognitionRecord();
        assertEquals("FILE_NAME_SHORTCUT", record.getRecognitionMethod());
        assertEquals("project-code-v1", record.getRecognitionVersion());
        assertEquals("SUCCESS", record.getStatus());
        assertEquals("PROJECT_CODE", record.getMatchType());
    }

    @Test
    void recognizeProjectCode_confirmedFileNameAliasWinsBeforeStandardRulesAndCodex() throws Exception {
        DccProjectCodeDO project = projectCode(117L, "一次性使用指引导管", "CEGCT");
        DccProjectCodeDO standardRuleProject = projectCode(118L, "注册检验资料", "RGJY");
        mockReadableFile(fileInDirectory(1003L),
                sourceFile("一次性使用指引导管三类 注册检验资料.pdf"),
                null,
                List.of(project, standardRuleProject));
        mockTechnicalDirectoryPath();
        when(projectCodeAliasMappingMapper.selectConfirmedActiveList()).thenReturn(List.of(
                aliasMapping(901L, 117L, "一次性使用指引导管三类", "DIRECTORY", "CONFIRMED", true)));
        when(categoryMapper.selectList()).thenReturn(List.of(category(300L, "DCC_INPUT", "输入阶段")));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);

        DccControlledFileProjectCodeRecognitionRespVO respVO =
                recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals(117L, respVO.getDccProjectCodeId());
        assertEquals("一次性使用指引导管", respVO.getProjectName());
        assertEquals("CEGCT", respVO.getProjectCode());
        assertEquals("PROJECT_NAME", respVO.getMatchType());
        assertEquals("一次性使用指引导管三类", respVO.getMatchText());
        assertEquals("FILE_NAME_ALIAS", respVO.getRecognitionMethod());
        assertEquals(901L, respVO.getMatchedProjectAliasId());
        assertEquals("一次性使用指引导管三类", respVO.getMatchedProjectAliasText());
        assertEquals("DIRECTORY", respVO.getMatchedProjectAliasSource());
        verify(fileService, never()).getFileContent(any(), any());
        verify(codexCliClient, never()).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));

        DccControlledFileRecognitionRecordDO record = captureRecognitionRecord();
        assertEquals("FILE_NAME_ALIAS", record.getRecognitionMethod());
        assertEquals(901L, record.getMatchedProjectAliasId());
        assertEquals("一次性使用指引导管三类", record.getMatchedProjectAliasText());
        assertEquals("DIRECTORY", record.getMatchedProjectAliasSource());
        assertEquals("PROJECT_NAME", record.getMatchType());
        assertEquals("一次性使用指引导管三类", record.getMatchText());
    }

    @Test
    void recognizeProjectCode_missingRecognitionVersionFailsFastBeforeAnyWrite() throws Exception {
        DccProjectCodeDO candidate = projectCode(700L, "项目A", "CODE-A");
        mockReadableFile(new byte[] {1}, List.of(candidate));
        when(recognitionProperties.getVersion()).thenReturn(" ");

        assertEquals(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING.getCode(),
                assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                        () -> recognitionService.recognizeProjectCode(99L, 900L)).getCode());

        verify(codexCliClient, never()).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(recognitionRecordMapper, never()).upsert(any(DccControlledFileRecognitionRecordDO.class));
    }

    @Test
    void recognizeProjectCode_unresolvedRecognitionVersionPlaceholderFailsFastBeforeAnyWrite() throws Exception {
        DccProjectCodeDO candidate = projectCode(700L, "项目A", "CODE-A");
        mockReadableFile(new byte[] {1}, List.of(candidate));
        when(recognitionProperties.getVersion()).thenReturn("${DCC_PROJECT_CODE_RECOGNITION_VERSION}");

        assertEquals(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING.getCode(),
                assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                        () -> recognitionService.recognizeProjectCode(99L, 900L)).getCode());

        verify(codexCliClient, never()).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(recognitionRecordMapper, never()).upsert(any(DccControlledFileRecognitionRecordDO.class));
    }

    @Test
    void recognizeProjectCode_failurePersistsFailedLedgerBeforeThrowing() throws Exception {
        DccProjectCodeDO candidate = projectCode(700L, "项目A", "CODE-A");
        mockReadableFile(new byte[] {1}, List.of(candidate));
        when(recognitionProperties.getVersion()).thenReturn("project-code-v1");
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class)))
                .thenThrow(new IllegalStateException("codex timeout"));
        when(recognitionRecordMapper.upsert(any(DccControlledFileRecognitionRecordDO.class))).thenReturn(1);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> recognitionService.recognizeProjectCode(99L, 900L));
        assertEquals("codex timeout", ex.getMessage());

        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        DccControlledFileRecognitionRecordDO record = captureRecognitionRecord();
        assertEquals("BASIC_INFO", record.getRecognitionScope());
        assertEquals("CODEX_CLI_CONTENT", record.getRecognitionMethod());
        assertEquals("project-code-v1", record.getRecognitionVersion());
        assertEquals("FAILED", record.getStatus());
        assertEquals("AI_CLASSIFICATION", record.getFailureStage());
        assertEquals("AI_REQUEST_FAILED", record.getFailureCode());
        assertEquals("codex timeout", record.getFailureMessage());
    }

    @Test
    void recognizeProjectCode_failsFastWhenBasicInfoRecognitionAlreadyClaimed() throws Exception {
        DccProjectCodeDO candidate = projectCode(700L, "项目A", "CODE-A");
        mockReadableFile(new byte[] {1}, List.of(candidate));
        when(recognitionClaimMapper.tryClaimBasicInfo(eq(0L), eq(900L), eq("BASIC_INFO"), eq(99L), eq(null), any()))
                .thenReturn(0);
        when(recognitionClaimMapper.selectByFileAndScope(900L, "BASIC_INFO")).thenReturn(
                DccControlledFileRecognitionClaimDO.builder()
                        .controlledFileId(900L)
                        .recognitionScope("BASIC_INFO")
                        .claimedBy(1000L)
                        .claimTaskId(300L)
                        .build());

        assertServiceException(() -> recognitionService.recognizeProjectCode(99L, 900L),
                CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_IN_PROGRESS, "900");
        verify(codexCliClient, never()).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        verify(recognitionRecordMapper, never()).upsert(any(DccControlledFileRecognitionRecordDO.class));
    }

    @Test
    void recognizeProjectCode_sourceFileNameUsesLongestUniqueProjectCodeBeforeCodex() throws Exception {
        DccProjectCodeDO shorter = projectCode(700L, "短编码项目", "RE-STM-MM-017");
        DccProjectCodeDO longer = projectCode(701L, "长编码项目", "RE-STM-MM-017-04");
        mockReadableFile(fileWithSource(), sourceFile("RE-STM-MM-017-04（A∕0）万级浮游菌控制趋势图.pdf"),
                null, List.of(shorter, longer));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);

        DccControlledFileProjectCodeRecognitionRespVO respVO =
                recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals(701L, respVO.getDccProjectCodeId());
        assertEquals("长编码项目", respVO.getProjectName());
        assertEquals("RE-STM-MM-017-04", respVO.getProjectCode());
        verify(fileService, never()).getFileContent(28L, "qms/6.4-51.xls");
        verify(codexCliClient, never()).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));
    }

    @Test
    void recognizeProjectCode_shortProjectCodeInsideLongerTokenFallsBackToCodex() throws Exception {
        DccProjectCodeDO candidate = projectCode(700L, "短编码项目", "IN");
        mockReadableFile(fileWithSource(), sourceFile("INT-培训记录.pdf"), new byte[] {1}, List.of(candidate));
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class)))
                .thenReturn(new DccProjectCodeRecognitionResult(700L,
                        DccProjectCodeRecognitionMatchType.PROJECT_CODE, "IN"));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);

        DccControlledFileProjectCodeRecognitionRespVO respVO =
                recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals(700L, respVO.getDccProjectCodeId());
        assertEquals("IN", respVO.getProjectCode());
        verify(fileService).getFileContent(28L, "qms/6.4-51.xls");
        verify(codexCliClient).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));
    }

    @Test
    void recognizeProjectCode_isolatedTwoLetterProjectCodeFallsBackToCodex() throws Exception {
        DccProjectCodeDO candidate = projectCode(700L, "短编码项目", "EC");
        mockReadableFile(fileWithSource(), sourceFile("EC-现场记录.pdf"), new byte[] {1}, List.of(candidate));
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class)))
                .thenReturn(new DccProjectCodeRecognitionResult(700L,
                        DccProjectCodeRecognitionMatchType.PROJECT_CODE, "EC"));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);

        DccControlledFileProjectCodeRecognitionRespVO respVO =
                recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals(700L, respVO.getDccProjectCodeId());
        assertEquals("EC", respVO.getProjectCode());
        verify(fileService).getFileContent(28L, "qms/6.4-51.xls");
        verify(codexCliClient).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));
    }

    @Test
    void recognizeProjectCode_projectCodeInsideLongerAsciiTokenFallsBackToCodex() throws Exception {
        DccProjectCodeDO candidate = projectCode(700L, "编码项目", "CODE-A");
        mockReadableFile(fileWithSource(), sourceFile("XCODE-AX审批单.pdf"), new byte[] {1}, List.of(candidate));
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class)))
                .thenReturn(new DccProjectCodeRecognitionResult(700L,
                        DccProjectCodeRecognitionMatchType.PROJECT_CODE, "CODE-A"));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);

        DccControlledFileProjectCodeRecognitionRespVO respVO =
                recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals(700L, respVO.getDccProjectCodeId());
        assertEquals("CODE-A", respVO.getProjectCode());
        verify(fileService).getFileContent(28L, "qms/6.4-51.xls");
        verify(codexCliClient).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));
    }

    @Test
    void recognizeProjectCode_codexProjectCodeMatchIgnoresCommonSeparators() throws Exception {
        DccProjectCodeDO candidate = projectCode(701L, "长编码项目", "RE-STM-MM-017-04");
        mockReadableFile(fileWithSource(), sourceFile("浮游菌控制趋势图.pdf"), new byte[] {1}, List.of(candidate));
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class)))
                .thenReturn(new DccProjectCodeRecognitionResult(701L,
                        DccProjectCodeRecognitionMatchType.PROJECT_CODE, "re_stm mm 017 04"));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);

        DccControlledFileProjectCodeRecognitionRespVO respVO =
                recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals(701L, respVO.getDccProjectCodeId());
        assertEquals("长编码项目", respVO.getProjectName());
        assertEquals("RE-STM-MM-017-04", respVO.getProjectCode());
        assertEquals("PROJECT_CODE", respVO.getMatchType());
        assertEquals("re_stm mm 017 04", respVO.getMatchText());
        verify(fileService).getFileContent(28L, "qms/6.4-51.xls");
        verify(codexCliClient).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));
    }

    @Test
    void recognizeProjectCode_uniqueNonBlankProjectCodeMayResolveDuplicateProjectNames() throws Exception {
        DccProjectCodeDO first = projectCode(700L, "重复项目", "CODE-A");
        DccProjectCodeDO second = projectCode(701L, "重复项目", "CODE-B");
        mockReadableFile(new byte[] {1}, List.of(first, second));
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class)))
                .thenReturn(new DccProjectCodeRecognitionResult(701L,
                        DccProjectCodeRecognitionMatchType.PROJECT_CODE, "CODE-B"));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);

        DccControlledFileProjectCodeRecognitionRespVO respVO =
                recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals(701L, respVO.getDccProjectCodeId());
        assertEquals("重复项目", respVO.getProjectName());
        assertEquals("CODE-B", respVO.getProjectCode());
        assertEquals("PROJECT_CODE", respVO.getMatchType());
    }

    @Test
    void recognizeProjectCode_exactDuplicateCandidatesCollapseBeforeCodexAndForwardDirectoryContext() throws Exception {
        DccProjectCodeDO first = projectCode(700L, "一次性使用导管鞘套装（FDA)", "IKFDA");
        DccProjectCodeDO duplicate = projectCode(701L, "一次性使用导管鞘套装（FDA)", "IKFDA");
        mockReadableFile(fileInDirectory(913858L),
                sourceFile("07 Compatibility (RE-VER-CR-43).pdf"),
                null,
                List.of(first, duplicate));
        when(directoryMapper.selectById(913858L)).thenReturn(
                directory(913858L, 913857L, "Appendix 3 Raw Data of All Performance Tests"));
        when(directoryMapper.selectById(913857L)).thenReturn(
                directory(913857L, 913797L, "包装运输"));
        when(directoryMapper.selectById(913797L)).thenReturn(
                directory(913797L, 908991L, "一次性使用导管鞘套装（FDA) IKFDA"));
        when(directoryMapper.selectById(908991L)).thenReturn(
                directory(908991L, null, "质量管理"));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);

        DccControlledFileProjectCodeRecognitionRespVO respVO =
                recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals(700L, respVO.getDccProjectCodeId());
        assertEquals("IKFDA", respVO.getProjectCode());
        verify(fileService, never()).getFileContent(any(), any());
        verify(codexCliClient, never()).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));
    }

    @Test
    void recognizeProjectCode_directoryPathUsesUniqueProjectCodeShortcutBeforeCodex() throws Exception {
        DccProjectCodeDO first = projectCode(700L, "一次性使用导管鞘套装（FDA)", "IKFDA");
        DccProjectCodeDO duplicate = projectCode(701L, "一次性使用导管鞘套装（FDA)", "IKFDA");
        mockReadableFile(fileInDirectory(913858L),
                sourceFile("07 Compatibility (RE-VER-CR-43).pdf"),
                null,
                List.of(first, duplicate));
        when(directoryMapper.selectById(913858L)).thenReturn(
                directory(913858L, 913857L, "Appendix 3 Raw Data of All Performance Tests"));
        when(directoryMapper.selectById(913857L)).thenReturn(
                directory(913857L, 913797L, "包装运输"));
        when(directoryMapper.selectById(913797L)).thenReturn(
                directory(913797L, 908991L, "一次性使用导管鞘套装（FDA) IKFDA"));
        when(directoryMapper.selectById(908991L)).thenReturn(
                directory(908991L, null, "质量管理"));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);

        DccControlledFileProjectCodeRecognitionRespVO respVO =
                recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals(700L, respVO.getDccProjectCodeId());
        assertEquals("IKFDA", respVO.getProjectCode());
        verify(fileService, never()).getFileContent(any(), any());
        verify(codexCliClient, never()).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));
    }

    @Test
    void recognizeProjectCode_fileNameRuleTakesPriorityOverDirectoryRuleAndSkipsCodex() throws Exception {
        DccProjectCodeDO fileNameProject = projectCode(117L, "一次性使用指引导管", "CEGCT");
        DccProjectCodeDO directoryProject = projectCode(118L, "支撑导管", "SC");
        mockReadableFile(fileInDirectory(1003L),
                sourceFile("一次性使用指引导管（三类）CEGCT 输入清单.pdf"),
                null,
                List.of(fileNameProject, directoryProject));
        mockTechnicalDirectoryPath();
        when(categoryMapper.selectList()).thenReturn(List.of(category(300L, "DCC_INPUT", "输入阶段")));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);

        DccControlledFileProjectCodeRecognitionRespVO respVO =
                recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals(117L, respVO.getDccProjectCodeId());
        assertEquals("CEGCT", respVO.getProjectCode());
        assertEquals("PROJECT_CODE", respVO.getMatchType());
        assertEquals("CEGCT", respVO.getMatchText());
        assertEquals("FILE_NAME_SHORTCUT", respVO.getRecognitionMethod());
        verify(fileService, never()).getFileContent(any(), any());
        verify(codexCliClient, never()).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));

        DccControlledFileDO updated = captureUpdatedFile();
        assertEquals("技术文档", updated.getFileTypeLevel1());
        assertNull(updated.getFileTypeLevel2());
        assertNull(updated.getFileTypeLevel3());
        assertNull(updated.getFileTypeLevel4());
        assertNull(updated.getFileTypeLevel5());

        DccControlledFileRecognitionRecordDO record = captureRecognitionRecord();
        assertEquals("FILE_NAME_SHORTCUT", record.getRecognitionMethod());
        assertEquals("技术文档", record.getFileTypeLevel1());
        assertNull(record.getFileTypeLevel2());
        assertNull(record.getFileTypeLevel3());
        assertNull(record.getFileTypeLevel4());
        assertNull(record.getFileTypeLevel5());
    }

    @Test
    void recognizeProjectCode_fileNamePartialProductNameDoesNotGuessLongerCandidateBeforeCodex() throws Exception {
        DccProjectCodeDO project = projectCode(126L, "Y型连接阀套件", "HV");
        mockReadableFile(fileInDirectory(1003L),
                sourceFile("推拉式Y阀套装1 220YCK118A-CP-102.PDF"),
                new byte[] {1},
                List.of(project));
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class))).thenReturn(null);

        DccControlledFileProjectCodeRecognitionRespVO respVO =
                recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals("UNKNOWN_DCC", respVO.getRecognitionStatus());
        assertNull(respVO.getDccProjectCodeId());
        assertNull(respVO.getProjectName());
        assertNull(respVO.getProjectCode());
        assertEquals("CODEX_CLI_CONTENT", respVO.getRecognitionMethod());
        verify(codexCliClient).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void recognizeProjectCode_fileNameSharedLooseCharactersMustNotMatchUnrelatedLongCandidate() throws Exception {
        DccProjectCodeDO project = projectCode(248L, "一次性使用导管鞘套装（FDA)", "IKFDA");
        mockReadableFile(fileInDirectory(1003L),
                sourceFile("Pebax管72D黑色（土耳其定制） 82705A-LJ-101 2026.3.4作废.pdf"),
                new byte[] {1},
                List.of(project));
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class))).thenReturn(null);

        DccControlledFileProjectCodeRecognitionRespVO respVO =
                recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals("UNKNOWN_DCC", respVO.getRecognitionStatus());
        assertNull(respVO.getDccProjectCodeId());
        assertNull(respVO.getProjectName());
        assertNull(respVO.getProjectCode());
        assertEquals("CODEX_CLI_CONTENT", respVO.getRecognitionMethod());
        verify(codexCliClient).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void recognizeProjectCode_batchRuleOnlyUnknownDoesNotInvokeCodex() throws Exception {
        DccProjectCodeDO project = projectCode(248L, "一次性使用导管鞘套装（FDA)", "IKFDA");
        mockReadableFile(fileInDirectory(1003L),
                sourceFile("Pebax管72D黑色（土耳其定制） 82705A-LJ-101 2026.3.4作废.pdf"),
                null,
                List.of(project));

        DccControlledFileProjectCodeRecognitionRespVO respVO =
                recognitionService.recognizeProjectCode(99L, 900L, 319L);

        assertEquals("UNKNOWN_DCC", respVO.getRecognitionStatus());
        assertNull(respVO.getDccProjectCodeId());
        assertNull(respVO.getProjectName());
        assertNull(respVO.getProjectCode());
        assertEquals("BATCH_RULE_ONLY", respVO.getRecognitionMethod());
        verify(fileService, never()).getFileContent(any(), any());
        verify(codexCliClient, never()).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));

        DccControlledFileRecognitionRecordDO record = captureRecognitionRecord();
        assertEquals("UNKNOWN_DCC", record.getStatus());
        assertEquals("BATCH_RULE_ONLY", record.getRecognitionMethod());
        assertEquals(319L, record.getBatchTaskId());
        assertNull(record.getMatchedProjectCodeId());
        assertNull(record.getRecognizedProductName());
    }

    @Test
    void recognizeProjectCode_directoryAliasRuleMatchesNormalizedProjectNameAndSkipsCodex() throws Exception {
        DccProjectCodeDO project = projectCode(117L, "一次性使用指引导管", "CEGCT");
        mockReadableFile(fileInDirectory(1003L), sourceFile("设计输入表.pdf"), null, List.of(project));
        mockTechnicalDirectoryPath();
        when(categoryMapper.selectList()).thenReturn(List.of(category(300L, "DCC_INPUT", "输入阶段")));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);

        DccControlledFileProjectCodeRecognitionRespVO respVO =
                recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals(117L, respVO.getDccProjectCodeId());
        assertEquals("一次性使用指引导管", respVO.getProjectName());
        assertEquals("CEGCT", respVO.getProjectCode());
        assertEquals("PROJECT_NAME", respVO.getMatchType());
        assertEquals("一次性使用指引导管（三类）", respVO.getMatchText());
        assertEquals("DIRECTORY_RULE", respVO.getRecognitionMethod());
        verify(fileService, never()).getFileContent(any(), any());
        verify(codexCliClient, never()).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));

        DccControlledFileDO updated = captureUpdatedFile();
        assertEquals("技术文档", updated.getFileTypeLevel1());
        assertNull(updated.getFileTypeLevel2());

        DccControlledFileRecognitionRecordDO record = captureRecognitionRecord();
        assertEquals("DIRECTORY_RULE", record.getRecognitionMethod());
        assertEquals("一次性使用指引导管（三类）", record.getMatchText());
        assertEquals("技术文档", record.getFileTypeLevel1());
        assertNull(record.getFileTypeLevel2());
    }

    @Test
    void recognizeProjectCode_matchedCategoryWithoutTaxonomyDoesNotDeriveLegacyLifecycleStage() throws Exception {
        DccProjectCodeDO project = projectCode(242L, "冠脉球囊扩张导管", "IRPTCA");
        mockReadableFile(fileInDirectory(1003L),
                sourceFile("05 项目立项书 R&D-IRPTCA-005.pdf"),
                null,
                List.of(project));
        mockTechnicalDirectoryPath();
        when(categoryMapper.selectList()).thenReturn(List.of(category(300L, "DCC_FVM_DHF_004", "项目立项书",
                DccFileCategoryLifecycleStageEnum.PLAN.getCode())));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);

        recognitionService.recognizeProjectCode(99L, 900L);

        DccControlledFileDO updated = captureUpdatedFile();
        assertNull(updated.getFileTypeTaxonomyId());
        assertEquals("技术文档", updated.getFileTypeLevel1());
        assertNull(updated.getFileTypeLevel2());
        assertNull(updated.getFileTypeLevel3());
        assertNull(updated.getFileTypeLevel4());
        assertNull(updated.getFileTypeLevel5());

        DccControlledFileRecognitionRecordDO record = captureRecognitionRecord();
        assertNull(record.getFileTypeTaxonomyId());
        assertEquals("技术文档", record.getFileTypeLevel1());
        assertNull(record.getFileTypeLevel2());
        assertNull(record.getFileTypeLevel3());
        assertNull(record.getFileTypeLevel4());
        assertNull(record.getFileTypeLevel5());
    }

    @Test
    void recognizeProjectCode_matchedCategoryWithTaxonomyUsesConfiguredFiveLevelPath() throws Exception {
        DccProjectCodeDO project = projectCode(242L, "冠脉球囊扩张导管", "IRPTCA");
        mockReadableFile(fileInDirectory(1003L),
                sourceFile("05 项目立项书 R&D-IRPTCA-005.pdf"),
                null,
                List.of(project));
        mockTechnicalDirectoryPath();
        DccFileCategoryDO category = category(300L, "DCC_FVM_DHF_004", "项目立项书",
                DccFileCategoryLifecycleStageEnum.PLAN.getCode());
        category.setFileTypeTaxonomyId(8801L);
        when(categoryMapper.selectList()).thenReturn(List.of(category));
        when(fileTypeTaxonomyAdminService.resolveActivePath(8801L)).thenReturn(new DccFileTypeTaxonomyPath(
                8801L, "技术文档", taxonomyStageName(DccFileCategoryLifecycleStageEnum.PLAN), "项目策划书", "草案", "归档件"));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);

        recognitionService.recognizeProjectCode(99L, 900L);

        DccControlledFileDO updated = captureUpdatedFile();
        assertEquals(8801L, updated.getFileTypeTaxonomyId());
        assertEquals("技术文档", updated.getFileTypeLevel1());
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.PLAN), updated.getFileTypeLevel2());
        assertEquals("项目策划书", updated.getFileTypeLevel3());
        assertEquals("草案", updated.getFileTypeLevel4());
        assertEquals("归档件", updated.getFileTypeLevel5());

        DccControlledFileRecognitionRecordDO record = captureRecognitionRecord();
        assertEquals(8801L, record.getFileTypeTaxonomyId());
        assertEquals("技术文档", record.getFileTypeLevel1());
        assertEquals(taxonomyStageName(DccFileCategoryLifecycleStageEnum.PLAN), record.getFileTypeLevel2());
        assertEquals("项目策划书", record.getFileTypeLevel3());
        assertEquals("草案", record.getFileTypeLevel4());
        assertEquals("归档件", record.getFileTypeLevel5());
    }

    @Test
    void recognizeProjectCode_qmsDirectorySetsOnlyFirstFileTypeLevel() throws Exception {
        DccProjectCodeDO project = projectCode(117L, "质量手册", "QMS-001");
        mockReadableFile(fileInDirectory(2002L), sourceFile("质量手册 QMS-001.pdf"), null, List.of(project));
        when(directoryMapper.selectById(2002L)).thenReturn(directory(2002L, 2001L, "管理制度"));
        when(directoryMapper.selectById(2001L)).thenReturn(directory(2001L, null, "1.QMS documents"));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);

        recognitionService.recognizeProjectCode(99L, 900L);

        DccControlledFileDO updated = captureUpdatedFile();
        assertEquals("QMS文档", updated.getFileTypeLevel1());
        assertNull(updated.getFileTypeLevel2());
        assertNull(updated.getFileTypeLevel3());
        assertNull(updated.getFileTypeLevel4());
        assertNull(updated.getFileTypeLevel5());
    }

    @Test
    void recognizeProjectCode_allowsOverwriteExistingAssociationWithoutProductMasterChange() throws Exception {
        DccProjectCodeDO candidate = projectCode(702L, "新项目", "CODE-NEW");
        DccControlledFileDO file = fileWithSource();
        file.setDccProjectCodeId(701L);
        file.setProductMasterId(5000L);
        mockReadableFile(file, new byte[] {1}, List.of(candidate));
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class)))
                .thenReturn(new DccProjectCodeRecognitionResult(702L,
                        DccProjectCodeRecognitionMatchType.PROJECT_CODE, "CODE-NEW"));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);

        recognitionService.recognizeProjectCode(99L, 900L);

        DccControlledFileDO updated = captureUpdatedFile();
        assertNull(updated.getFileName(), "recognition must not rewrite controlled-file chain file_name");
        assertNull(updated.getTitle(), "recognition must not rewrite controlled-file title from the source file name");
        assertEquals(702L, updated.getDccProjectCodeId());
        assertEquals("新项目", updated.getProductName());
        assertEquals("CODE-NEW", updated.getProductCode());
        assertNull(updated.getProductMasterId(), "recognition must not overwrite productMasterId");

        verify(controlledFileMasterMapper, never()).updateById(any(DccControlledFileMasterDO.class));
    }

    @Test
    void recognizeProjectCode_failsBeforeReadingFileWhenUserIsNotDocControl() {
        when(permissionApi.hasAnyRoles(99L, "doc_control")).thenReturn(false);

        assertServiceException(() -> recognitionService.recognizeProjectCode(99L, 900L),
                CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED);

        verify(controlledFileMapper, never()).selectById(900L);
        verify(codexCliClient, never()).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void recognizeProjectCode_missingSourceFileFailsBeforeCodexInvocation() {
        mockDocControl();
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .sourceFileId(null)
                .build());

        assertServiceException(() -> recognitionService.recognizeProjectCode(99L, 900L),
                CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_SOURCE_MISSING);

        verify(fileService, never()).getFile(any());
        verify(codexCliClient, never()).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void recognizeProjectCode_noEnabledCandidatesFailsBeforeCodexInvocation() throws Exception {
        mockReadableFile(null, List.of());

        assertServiceException(() -> recognitionService.recognizeProjectCode(99L, 900L),
                CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_NO_CANDIDATE);

        verify(codexCliClient, never()).recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class));
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void recognizeProjectCode_emptyAiResultPersistsUnknownBasicDataWithoutUpdatingFile() throws Exception {
        DccProjectCodeDO candidate = projectCode(700L, "项目A", "CODE-A");
        mockReadableFile(new byte[] {1}, List.of(candidate));
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class))).thenReturn(null);

        DccControlledFileProjectCodeRecognitionRespVO result = recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals(900L, result.getControlledFileId());
        assertEquals("UNKNOWN_DCC", result.getRecognitionStatus());
        assertNull(result.getDccProjectCodeId());
        assertNull(result.getProjectName());
        assertNull(result.getProjectCode());
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        DccControlledFileRecognitionRecordDO record = captureRecognitionRecord();
        assertEquals("UNKNOWN_DCC", record.getStatus());
        assertEquals("CODEX_CLI_CONTENT", record.getRecognitionMethod());
        assertNull(record.getFailureMessage());
        assertNull(record.getMatchedProjectCodeId());
        assertNull(record.getRecognizedProductName());
        assertEquals(99L, record.getRecognizedBy());
    }

    @Test
    void recognizeProjectCode_emptyAiResultPersistsDatabaseSafeStatus() throws Exception {
        DccProjectCodeDO candidate = projectCode(700L, "项目A", "CODE-A");
        mockReadableFile(new byte[] {1}, List.of(candidate));
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class))).thenReturn(null);

        DccControlledFileProjectCodeRecognitionRespVO result = recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals("UNKNOWN_DCC", result.getRecognitionStatus());
        DccControlledFileRecognitionRecordDO record = captureRecognitionRecord();
        assertEquals("UNKNOWN_DCC", record.getStatus());
        assertTrue(record.getStatus().length() <= 16);
        assertNull(record.getFailureMessage());
    }

    @Test
    void upsertRecognitionRecordRejectsUnsupportedStatusBeforeDatabaseWrite() {
        DccControlledFileRecognitionRecordDO record = DccControlledFileRecognitionRecordDO.builder()
                .tenantId(0L)
                .controlledFileId(900L)
                .recognitionScope("BASIC_INFO")
                .recognitionMethod("CODEX_CLI_CONTENT")
                .recognitionVersion("project-code-v1")
                .status("UNRECOGNIZED_PROJECT_NAME")
                .build();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(recognitionService, "upsertRecognitionRecord", record));

        assertEquals("unsupported DCC recognition record status: UNRECOGNIZED_PROJECT_NAME",
                exception.getMessage());
        verify(recognitionRecordMapper, never()).upsert(any(DccControlledFileRecognitionRecordDO.class));
    }

    @Test
    void recognizeProjectCode_preservesLongFailureMessageBeforePersistingRecord() throws Exception {
        DccProjectCodeDO candidate = projectCode(700L, "项目A", "CODE-A");
        mockReadableFile(new byte[] {1}, List.of(candidate));
        String longFailureMessage = "x".repeat(600);
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class)))
                .thenThrow(new IllegalStateException(longFailureMessage));

        assertThrows(IllegalStateException.class, () -> recognitionService.recognizeProjectCode(99L, 900L));

        DccControlledFileRecognitionRecordDO record = captureRecognitionRecord();
        assertEquals("FAILED", record.getStatus());
        assertNull(record.getBatchTaskId());
        assertEquals(longFailureMessage, record.getFailureMessage());
    }

    @Test
    void recognizeProjectCode_candidateOutsideEnabledListOrDisabledCandidateFails() throws Exception {
        mockReadableFile(new byte[] {1}, List.of(projectCode(700L, "项目A", "CODE-A")));
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class)))
                .thenReturn(new DccProjectCodeRecognitionResult(701L,
                        DccProjectCodeRecognitionMatchType.PROJECT_NAME, "禁用项目"));

        assertServiceException(() -> recognitionService.recognizeProjectCode(99L, 900L),
                CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_INVALID_CANDIDATE);

        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        DccControlledFileRecognitionRecordDO record = captureRecognitionRecord();
        assertEquals("RESULT_VALIDATION", record.getFailureStage());
        assertEquals("INVALID_RESULT", record.getFailureCode());
        assertTrue(record.getFailureMessage().contains("projectCodeId=701"));
        assertTrue(record.getFailureMessage().contains("matchType=PROJECT_NAME"));
        assertTrue(record.getFailureMessage().contains("matchText=禁用项目"));
    }

    @Test
    void recognizeProjectCode_candidateIdDriftResolvesUniqueProjectCodeMatch() throws Exception {
        DccProjectCodeDO candidate = projectCode(700L, "血管指引导丝", "HGGW");
        mockReadableFile(new byte[] {1}, List.of(candidate));
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class)))
                .thenReturn(new DccProjectCodeRecognitionResult(9999L,
                        DccProjectCodeRecognitionMatchType.PROJECT_CODE, "HGGW"));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);

        DccControlledFileProjectCodeRecognitionRespVO result = recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals("SUCCESS", result.getRecognitionStatus());
        assertEquals(700L, result.getDccProjectCodeId());
        assertEquals("血管指引导丝", result.getProjectName());
        assertEquals("HGGW", result.getProjectCode());
        DccControlledFileDO updated = captureUpdatedFile();
        assertEquals(700L, updated.getDccProjectCodeId());
        DccControlledFileRecognitionRecordDO record = captureRecognitionRecord();
        assertEquals("SUCCESS", record.getStatus());
        assertEquals(700L, record.getMatchedProjectCodeId());
        assertEquals("HGGW", record.getMatchText());
    }

    @Test
    void recognizeProjectCode_candidateIdDriftResolvesUniqueProjectNameMatch() throws Exception {
        DccProjectCodeDO candidate = projectCode(700L, "按压式球囊扩张压力泵", "PQB");
        mockReadableFile(new byte[] {1}, List.of(candidate));
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class)))
                .thenReturn(new DccProjectCodeRecognitionResult(9999L,
                        DccProjectCodeRecognitionMatchType.PROJECT_NAME, "按压式球囊扩张压力泵"));
        when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenReturn(1);

        DccControlledFileProjectCodeRecognitionRespVO result = recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals("SUCCESS", result.getRecognitionStatus());
        assertEquals(700L, result.getDccProjectCodeId());
        assertEquals("按压式球囊扩张压力泵", result.getProjectName());
        assertEquals("PQB", result.getProjectCode());
        DccControlledFileRecognitionRecordDO record = captureRecognitionRecord();
        assertEquals("SUCCESS", record.getStatus());
        assertEquals(700L, record.getMatchedProjectCodeId());
        assertEquals("按压式球囊扩张压力泵", record.getMatchText());
    }

    @Test
    void recognizeProjectCode_projectNameTextMismatchPersistsUnrecognizedProjectNameWithoutUpdatingFile() throws Exception {
        DccProjectCodeDO candidate = projectCode(700L, "按压式球囊扩张压力泵", "CODE-A");
        mockReadableFile(new byte[] {1}, List.of(candidate));
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class)))
                .thenReturn(new DccProjectCodeRecognitionResult(700L,
                        DccProjectCodeRecognitionMatchType.PROJECT_NAME, "按压式球囊扩张压力泵（螺杆带封盖）"));

        DccControlledFileProjectCodeRecognitionRespVO result = recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals(900L, result.getControlledFileId());
        assertEquals("NAME_MISMATCH", result.getRecognitionStatus());
        assertNull(result.getDccProjectCodeId());
        assertNull(result.getProjectName());
        assertNull(result.getProjectCode());
        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
        DccControlledFileRecognitionRecordDO record = captureRecognitionRecord();
        assertEquals("NAME_MISMATCH", record.getStatus());
        assertEquals("CODEX_CLI_CONTENT", record.getRecognitionMethod());
        assertNull(record.getFailureMessage());
        assertEquals(700L, record.getMatchedProjectCodeId());
        assertEquals("按压式球囊扩张压力泵（螺杆带封盖）", record.getMatchText());
    }

    @Test
    void recognizeProjectCode_projectNameTextMismatchPersistsDatabaseSafeStatus() throws Exception {
        DccProjectCodeDO candidate = projectCode(700L, "按压式球囊扩张压力泵", "CODE-A");
        mockReadableFile(new byte[] {1}, List.of(candidate));
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class)))
                .thenReturn(new DccProjectCodeRecognitionResult(700L,
                        DccProjectCodeRecognitionMatchType.PROJECT_NAME, "按压式球囊扩张压力泵（螺杆带封盖）"));

        DccControlledFileProjectCodeRecognitionRespVO result = recognitionService.recognizeProjectCode(99L, 900L);

        assertEquals("NAME_MISMATCH", result.getRecognitionStatus());
        DccControlledFileRecognitionRecordDO record = captureRecognitionRecord();
        assertEquals("NAME_MISMATCH", record.getStatus());
        assertTrue(record.getStatus().length() <= 16);
        assertEquals("按压式球囊扩张压力泵（螺杆带封盖）", record.getMatchText());
    }

    @Test
    void recognizeProjectCode_duplicateProjectCodeFailsForCodeMatch() throws Exception {
        DccProjectCodeDO first = projectCode(700L, "项目A", "CODE-DUP");
        DccProjectCodeDO second = projectCode(701L, "项目B", "CODE-DUP");
        mockReadableFile(new byte[] {1}, List.of(first, second));
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class)))
                .thenReturn(new DccProjectCodeRecognitionResult(700L,
                        DccProjectCodeRecognitionMatchType.PROJECT_CODE, "CODE-DUP"));

        assertServiceException(() -> recognitionService.recognizeProjectCode(99L, 900L),
                CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_AMBIGUOUS, "projectCode=CODE-DUP");

        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void recognizeProjectCode_blankProjectCodeCannotResolveCodeMatch() throws Exception {
        DccProjectCodeDO candidate = projectCode(700L, "项目A", "");
        mockReadableFile(new byte[] {1}, List.of(candidate));
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class)))
                .thenReturn(new DccProjectCodeRecognitionResult(700L,
                        DccProjectCodeRecognitionMatchType.PROJECT_CODE, ""));

        assertServiceException(() -> recognitionService.recognizeProjectCode(99L, 900L),
                CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_INVALID_CANDIDATE);

        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    @Test
    void recognizeProjectCode_duplicateProjectNameFailsForNameMatch() throws Exception {
        DccProjectCodeDO first = projectCode(700L, "重复项目", "CODE-A");
        DccProjectCodeDO second = projectCode(701L, "重复项目", "CODE-B");
        mockReadableFile(new byte[] {1}, List.of(first, second));
        when(codexCliClient.recognizeProjectCode(any(DccProjectCodeRecognitionCommand.class)))
                .thenReturn(new DccProjectCodeRecognitionResult(700L,
                        DccProjectCodeRecognitionMatchType.PROJECT_NAME, "重复项目"));

        assertServiceException(() -> recognitionService.recognizeProjectCode(99L, 900L),
                CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_AMBIGUOUS, "projectName=重复项目");

        verify(controlledFileMapper, never()).updateById(any(DccControlledFileDO.class));
    }

    private void mockReadableFile(byte[] sourceContent, List<DccProjectCodeDO> candidates) throws Exception {
        mockReadableFile(fileWithSource(), sourceContent, candidates);
    }

    private void mockReadableFile(DccControlledFileDO file, byte[] sourceContent,
                                  List<DccProjectCodeDO> candidates) throws Exception {
        mockReadableFile(file, sourceFile(), sourceContent, candidates);
    }

    private void mockReadableFile(DccControlledFileDO file, FileDO sourceFile, byte[] sourceContent,
                                  List<DccProjectCodeDO> candidates) throws Exception {
        mockDocControl();
        lenient().when(controlledFileMapper.selectById(900L)).thenReturn(file);
        lenient().when(fileService.getFile(321L)).thenReturn(sourceFile);
        if (sourceContent != null) {
            lenient().when(fileService.getFileContent(28L, "qms/6.4-51.xls")).thenReturn(sourceContent);
        }
        lenient().when(projectCodeMapper.selectEnabledList()).thenReturn(candidates);
    }

    private void mockDocControl() {
        when(permissionApi.hasAnyRoles(99L, "doc_control")).thenReturn(true);
        lenient().when(recognitionProperties.getVersion()).thenReturn("project-code-v1");
        lenient().when(recognitionClaimMapper.tryClaimBasicInfo(any(), any(), any(), any(), any(), any())).thenReturn(1);
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    private DccControlledFileDO captureUpdatedFile() {
        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(updateCaptor.capture());
        return updateCaptor.getValue();
    }

    private DccControlledFileMasterDO captureUpdatedMaster() {
        ArgumentCaptor<DccControlledFileMasterDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileMasterDO.class);
        verify(controlledFileMasterMapper).updateById(updateCaptor.capture());
        return updateCaptor.getValue();
    }

    private DccControlledFileRecognitionRecordDO captureRecognitionRecord() {
        ArgumentCaptor<DccControlledFileRecognitionRecordDO> updateCaptor =
                ArgumentCaptor.forClass(DccControlledFileRecognitionRecordDO.class);
        verify(recognitionRecordMapper).upsert(updateCaptor.capture());
        return updateCaptor.getValue();
    }

    private DccControlledFileDO fileWithSource() {
        return DccControlledFileDO.builder()
                .id(900L)
                .tenantId(0L)
                .masterId(800L)
                .sourceFileId(321L)
                .build();
    }

    private DccControlledFileDO fileInDirectory(Long directoryId) {
        DccControlledFileDO file = fileWithSource();
        file.setDirectoryId(directoryId);
        return file;
    }

    private FileDO sourceFile() {
        return sourceFile("6.4-51.xls");
    }

    private FileDO sourceFile(String name) {
        return FileDO.builder()
                .id(321L)
                .configId(28L)
                .name(name)
                .path("qms/6.4-51.xls")
                .type("application/vnd.ms-excel")
                .build();
    }

    private DccProjectCodeDO projectCode(Long id, String projectName, String projectCode) {
        return DccProjectCodeDO.builder()
                .id(id)
                .projectName(projectName)
                .projectCode(projectCode)
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build();
    }

    private DccProjectCodeAliasMappingDO aliasMapping(Long id, Long projectCodeId, String aliasText,
                                                      String aliasSource, String status, Boolean active) {
        return DccProjectCodeAliasMappingDO.builder()
                .id(id)
                .projectCodeId(projectCodeId)
                .aliasText(aliasText)
                .normalizedAliasText(aliasText)
                .aliasSource(aliasSource)
                .status(status)
                .active(active)
                .build();
    }

    private DccFileDirectoryDO directory(Long id, Long parentId, String name) {
        return DccFileDirectoryDO.builder()
                .id(id)
                .parentId(parentId)
                .name(name)
                .build();
    }

    private DccFileCategoryDO category(Long id, String code, String name) {
        return category(id, code, name, null);
    }

    private DccFileCategoryDO category(Long id, String code, String name, String lifecycleStage) {
        return DccFileCategoryDO.builder()
                .id(id)
                .code(code)
                .name(name)
                .active(Boolean.TRUE)
                .lifecycleStage(lifecycleStage)
                .build();
    }

    private String taxonomyStageName(DccFileCategoryLifecycleStageEnum lifecycleStage) {
        return switch (lifecycleStage) {
            case PLAN -> "设计和开发策划阶段";
            case INPUT -> "设计和开发输入阶段";
            case OUTPUT -> "设计和开发输出阶段";
            case VERIFICATION -> "设计和开发验证";
            case VALIDATION -> "设计确认";
            case TRANSFER -> "设计和开发转换阶段";
        };
    }

    private void mockTechnicalDirectoryPath() {
        when(directoryMapper.selectById(1003L)).thenReturn(directory(1003L, 1002L, "2输入阶段"));
        when(directoryMapper.selectById(1002L)).thenReturn(directory(1002L, 1001L, "81 一次性使用指引导管（三类） CEGCT"));
        when(directoryMapper.selectById(1001L)).thenReturn(directory(1001L, null, "2.DHF"));
    }
}
