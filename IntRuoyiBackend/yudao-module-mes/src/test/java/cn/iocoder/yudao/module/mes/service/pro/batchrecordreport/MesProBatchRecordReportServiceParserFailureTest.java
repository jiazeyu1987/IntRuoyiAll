package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordDefinitionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionApprovalEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMigrationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.wordparser.SharedWordDocumentParser;
import cn.iocoder.yudao.module.wordparser.WordParseCommand;
import cn.iocoder.yudao.module.wordparser.WordParseDiagnostics;
import cn.iocoder.yudao.module.wordparser.WordParseException;
import cn.iocoder.yudao.module.wordparser.WordParseFailureCode;
import cn.iocoder.yudao.module.wordparser.WordParseProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FILE_EMPTY;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FILE_EXTENSION_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PARSE_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_TABLE_COUNT_INVALID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProBatchRecordReportServiceParserFailureTest {

    private MesProBatchRecordReportServiceImpl service;

    @Mock
    private SharedWordDocumentParser sharedParser;
    @Mock
    private MesProBatchRecordReportMapper reportMapper;
    @Mock
    private MesProBatchRecordDefinitionMapper definitionMapper;
    @Mock
    private MesProBatchRecordVersionMapper versionMapper;
    @Mock
    private MesProBatchRecordVersionMigrationItemMapper migrationItemMapper;
    @Mock
    private MesProBatchRecordVersionApprovalEventMapper approvalEventMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Mock
    private MesProBatchRecordRouteGenerationService routeGenerationService;
    @Mock
    private MesProBatchRecordJimuReportGateway jimuReportGateway;

    @BeforeEach
    void setUp() {
        service = new MesProBatchRecordReportServiceImpl();
        ReflectionTestUtils.setField(service, "docParser", new MesProBatchRecordDocParser(sharedParser));
        ReflectionTestUtils.setField(service, "reportMapper", reportMapper);
        ReflectionTestUtils.setField(service, "definitionMapper", definitionMapper);
        ReflectionTestUtils.setField(service, "versionMapper", versionMapper);
        ReflectionTestUtils.setField(service, "migrationItemMapper", migrationItemMapper);
        ReflectionTestUtils.setField(service, "approvalEventMapper", approvalEventMapper);
        ReflectionTestUtils.setField(service, "routeProcessMapper", routeProcessMapper);
        ReflectionTestUtils.setField(service, "routeMapper", routeMapper);
        ReflectionTestUtils.setField(service, "routeProductMapper", routeProductMapper);
        ReflectionTestUtils.setField(service, "routeVersionMapper", routeVersionMapper);
        ReflectionTestUtils.setField(service, "routeFlowProcessBatchRecordMapper", routeFlowProcessBatchRecordMapper);
        ReflectionTestUtils.setField(service, "routeGenerationService", routeGenerationService);
        ReflectionTestUtils.setField(service, "jimuReportGateway", jimuReportGateway);
    }

    @Test
    void importPilotDoc_emptySource_mapsExactErrorWithoutParserOrWrites() {
        ServiceException exception = assertThrows(ServiceException.class, () -> service.importPilotDoc(
                new MockMultipartFile("file", "empty.doc", "application/msword", new byte[0])));

        assertEquals(PRO_BATCH_RECORD_REPORT_FILE_EMPTY.getCode(), exception.getCode());
        verifyNoInteractions(sharedParser);
        assertNoWriteSideEffects();
    }

    @Test
    void importPilotDoc_unsupportedExtension_mapsExactErrorWithoutParserOrWrites() {
        ServiceException exception = assertThrows(ServiceException.class, () -> service.importPilotDoc(
                new MockMultipartFile("file", "source.pdf", "application/pdf", new byte[]{1})));

        assertEquals(PRO_BATCH_RECORD_REPORT_FILE_EXTENSION_INVALID.getCode(), exception.getCode());
        verifyNoInteractions(sharedParser);
        assertNoWriteSideEffects();
    }

    @Test
    void importPilotDoc_corruptSource_mapsExactErrorWithOneParserCallAndNoWrites() {
        assertSharedFailure(WordParseFailureCode.CORRUPT_SOURCE, PRO_BATCH_RECORD_REPORT_PARSE_FAILED);
    }

    @Test
    void importPilotDoc_invalidTable_mapsExactErrorWithOneParserCallAndNoWrites() {
        assertSharedFailure(WordParseFailureCode.INVALID_TABLE_STRUCTURE, PRO_BATCH_RECORD_REPORT_PARSE_FAILED);
    }

    @Test
    void importPilotDoc_noContent_mapsExactErrorWithOneParserCallAndNoWrites() {
        assertSharedFailure(WordParseFailureCode.NO_PARSEABLE_CONTENT, PRO_BATCH_RECORD_REPORT_TABLE_COUNT_INVALID);
    }

    private void assertSharedFailure(WordParseFailureCode failureCode, ErrorCode expectedError) {
        byte[] source = new byte[]{1, 2, 3};
        when(sharedParser.parse(any(WordParseCommand.class))).thenThrow(sharedFailure(failureCode));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.importPilotDoc(
                new MockMultipartFile("file", "source.doc", "application/msword", source)));

        assertEquals(expectedError.getCode(), exception.getCode());
        ArgumentCaptor<WordParseCommand> commandCaptor = ArgumentCaptor.forClass(WordParseCommand.class);
        verify(sharedParser, times(1)).parse(commandCaptor.capture());
        verifyNoMoreInteractions(sharedParser);
        WordParseCommand command = commandCaptor.getValue();
        assertArrayEquals(source, command.source());
        assertEquals(".doc", command.extension());
        assertEquals("mes-word-source.doc", command.originalFileName());
        assertEquals(WordParseProfile.STRUCTURAL_CANONICAL, command.profile());
        assertNoWriteSideEffects();
    }

    private WordParseException sharedFailure(WordParseFailureCode failureCode) {
        return new WordParseException(failureCode, new WordParseDiagnostics(
                "test", "source-hash", ".doc", "file-name-hash", 0, 0, List.of(), failureCode));
    }

    private void assertNoWriteSideEffects() {
        verifyNoInteractions(reportMapper, definitionMapper, versionMapper, migrationItemMapper,
                approvalEventMapper, routeProcessMapper, routeMapper, routeProductMapper, routeVersionMapper,
                routeFlowProcessBatchRecordMapper, routeGenerationService, jimuReportGateway);
    }
}
