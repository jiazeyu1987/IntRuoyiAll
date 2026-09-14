package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalOrchestrator;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_NAME_EMPTY;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_NAME_TOO_LONG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@Import(MesProBatchRecordReportServiceImpl.class)
class MesProBatchRecordReportRenameServiceImplDbTest extends BaseDbUnitTest {

    @Resource
    private MesProBatchRecordReportService reportService;
    @Resource
    private MesProBatchRecordReportMapper reportMapper;

    @MockitoBean
    private MesProBatchRecordDocParser parser;
    @MockitoBean
    private MesProBatchRecordImageParser imageParser;
    @MockitoBean
    private MesProBatchRecordJimuReportGateway jimuReportGateway;
    @MockitoBean
    private MesProBatchRecordRouteGenerationService routeGenerationService;
    @MockitoBean
    private MesProBatchRecordFormProfileRegistry formProfileRegistry;
    @MockitoBean
    private BusinessApprovalOrchestrator businessApprovalOrchestrator;
    @MockitoBean
    private MesProBatchRecordVersionBusinessApprovalEffectExecutor batchRecordVersionApprovalEffectExecutor;

    @BeforeEach
    void setUp() {
        reportMapper.deleteHardByReportId("rename-report-1");
    }

    @Test
    void renameGeneratedReport_updatesMetadataAndDelegatesGatewayRename() {
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                91L, "rename-sample", 1, "rename-report-1", "EBR_A_T01", "旧名称", "pilot.doc");
        reportMapper.insert(report);

        reportService.renameGeneratedReport("rename-report-1", "新报表名称");

        List<MesProBatchRecordReportDO> reports = reportMapper.selectListByReportIds(List.of("rename-report-1"));
        assertEquals(1, reports.size());
        assertEquals("新报表名称", reports.get(0).getReportName());
        verify(jimuReportGateway).renameReportName("rename-report-1", "新报表名称");
    }

    @Test
    void renameGeneratedReport_whenNameBlank_failsFast() {
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                92L, "rename-sample-blank", 1, "rename-report-1", "EBR_A_T01", "旧名称", "pilot.doc");
        reportMapper.insert(report);

        assertServiceException(() -> reportService.renameGeneratedReport("rename-report-1", "   "),
                PRO_BATCH_RECORD_REPORT_NAME_EMPTY);
    }

    @Test
    void renameGeneratedReport_whenNameTooLong_failsFast() {
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                93L, "rename-sample-long", 1, "rename-report-1", "EBR_A_T01", "旧名称", "pilot.doc");
        reportMapper.insert(report);
        String longName = "重".repeat(51);

        assertServiceException(() -> reportService.renameGeneratedReport("rename-report-1", longName),
                PRO_BATCH_RECORD_REPORT_NAME_TOO_LONG);
    }
}
