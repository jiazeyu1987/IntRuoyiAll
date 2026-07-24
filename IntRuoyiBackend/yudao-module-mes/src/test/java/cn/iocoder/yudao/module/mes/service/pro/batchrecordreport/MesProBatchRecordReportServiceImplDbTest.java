package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalErrorCode;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalOrchestrator;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRuleVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRulesReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRulesRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportDeleteAllRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportSignatureCellMarkerVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportSignatureCellMarkersReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportSignatureCellMarkersRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordDefinitionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionMigrationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordDefinitionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMigrationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.md.autocode.MesMdAutoCodeRuleCodeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionRuleCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeSaveCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeService;
import cn.iocoder.yudao.module.mes.service.pro.dccprojectgovernance.MesProDccProjectGovernanceService;
import cn.iocoder.yudao.module.mes.service.pro.dccprojectgovernance.MesProDccProjectGovernanceServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.dccprojectgovernance.MesProDccProjectGovernanceStatus;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteOwnerPermissionServiceImpl;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_BATCH_NAME_EMPTY;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_BATCH_NAME_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FORM_SLOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_BATCH_NAME_TOO_LONG;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_BOUND_BY_ROUTE_PROCESS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_CATEGORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_DELETE_CONFIRM_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_FILE_EXTENSION_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMPORT_ACTION_NOT_ALLOWED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMPORT_SCOPE_EMPTY;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_DCC_PROJECT_NAME_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_IMAGE_FILE_EXTENSION_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_CELL_RULE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_JSON_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PROCESS_EMPTY;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PRODUCT_INFO_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_PRODUCT_INFO_NOT_FIRST;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_SIGNATURE_REVIEW_SOURCE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_EMPTY;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_BIND_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_NAME_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_SCOPE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_CURRENT_CHANGED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_MIGRATION_BLOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_PENDING_APPROVAL_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_RESET_BLOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportErrorCodeConstants.PRO_BATCH_RECORD_REPORT_VERSION_UPGRADE_SOURCE_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Import({MesProBatchRecordReportServiceImpl.class, MesProBatchRecordRouteGenerationServiceImpl.class,
        MesProBatchRecordVersionBusinessApprovalEffectExecutor.class,
        MesProRouteOwnerPermissionServiceImpl.class,
        MesProBatchRecordFormProfileRegistry.class, MesProBatchRecordLossReportNormalizer.class,
        MesProDccProjectGovernanceServiceImpl.class})
class MesProBatchRecordReportServiceImplDbTest extends BaseDbUnitTest {

    private static final String PILOT_FILE_NAME =
            "RE-PP-ID-01\uFF08A 1\uFF09\u7403\u56CA\u6269\u5F20\u538B\u529B\u6CF5\u751F\u4EA7\u8BB0\u5F55(1).doc";

    @Resource
    private MesProBatchRecordReportService reportService;
    @Resource
    private MesProDccProjectGovernanceService dccProjectGovernanceService;
    @Resource
    private MesProBatchRecordReportMapper reportMapper;
    @Resource
    private MesProBatchRecordDefinitionMapper definitionMapper;
    @Resource
    private MesProBatchRecordVersionMapper versionMapper;
    @Resource
    private MesProBatchRecordVersionMigrationItemMapper migrationItemMapper;
    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteProductMapper routeProductMapper;
    @Resource
    private DataSource dataSource;
    private long productItemIdSequence = 880000L;
    private long productWorkOrderIdSequence = 890000L;

    @MockitoBean
    private MesProBatchRecordDocParser parser;
    @MockitoBean
    private MesProBatchRecordImageParser imageParser;
    @MockitoBean
    private MesProBatchRecordJimuReportGateway jimuReportGateway;
    @MockitoBean
    private MesProBatchRecordRouteRecognizer routeRecognizer;
    @MockitoBean
    private MesMdAutoCodeRecordService autoCodeRecordService;
    @MockitoBean
    private BusinessApprovalOrchestrator businessApprovalOrchestrator;
    @MockitoBean
    private MesProEdhrPermissionScopeService permissionScopeService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate().execute("DELETE FROM mes_pro_route_process_flow_edge");
        jdbcTemplate().execute("DELETE FROM mes_pro_route_flow_process_batch_record");
        jdbcTemplate().execute("DELETE FROM mes_pro_route_flow_process_config");
        jdbcTemplate().execute("DELETE FROM mes_pro_route_flow_config");
        jdbcTemplate().execute("DELETE FROM mes_pro_route_product");
        jdbcTemplate().execute("DELETE FROM mes_pro_route_version");
        jdbcTemplate().execute("DELETE FROM mes_pro_route_process");
        jdbcTemplate().execute("DELETE FROM mes_pro_route");
        jdbcTemplate().execute("DELETE FROM mes_pro_process");
        jdbcTemplate().execute("DELETE FROM mes_pro_batch_record_version_approval_event");
        jdbcTemplate().execute("DELETE FROM mes_pro_batch_record_version_migration_item");
        jdbcTemplate().execute("DELETE FROM mes_pro_batch_record_version");
        jdbcTemplate().execute("DELETE FROM mes_pro_batch_record_definition");
        jdbcTemplate().execute("DELETE FROM mes_pro_work_order WHERE id >= 890000");
        jdbcTemplate().execute("DELETE FROM mes_md_item WHERE id >= 880000");
        jdbcTemplate().execute("DELETE FROM dcc_project_code");
        reportMapper.delete(new QueryWrapper<>());
        when(routeRecognizer.routeKey()).thenReturn(MesProBatchRecordRecognitionRouteKeys.B);
        AtomicInteger routeCodeCounter = new AtomicInteger();
        when(autoCodeRecordService.generateAutoCode(eq(MesMdAutoCodeRuleCodeEnum.PRO_ROUTE_CODE.getCode())))
                .thenAnswer(invocation -> "ROUTE-IMPORT-" + routeCodeCounter.incrementAndGet());
        AtomicInteger processInstanceCounter = new AtomicInteger();
        when(businessApprovalOrchestrator.submit(any(BusinessApprovalContext.class)))
                .thenAnswer(invocation -> {
                    BusinessApprovalContext context = invocation.getArgument(0);
                    long requestId = processInstanceCounter.incrementAndGet();
                    String processInstanceId = "batch-version-process-" + requestId;
                    MesProBatchRecordVersionDO update = new MesProBatchRecordVersionDO();
                    update.setId(Long.valueOf(context.getObjectId()));
                    update.setStatus("PENDING_APPROVAL");
                    update.setSubmittedBy(context.getApplicantUserId());
                    update.setSubmittedAt(LocalDateTime.now());
                    update.setApprovalInstanceId(processInstanceId);
                    versionMapper.updateById(update);
                    return BusinessApprovalRequest.builder()
                        .requestId(requestId)
                        .status(BusinessApprovalRequestStatus.PENDING_BPM)
                        .processInstanceId(processInstanceId)
                        .context(context)
                        .build();
                });
    }

    @Test
    void importPilotDoc_rejectsNonDocFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pilot.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "fake".getBytes(StandardCharsets.UTF_8));

        assertServiceException(() -> reportService.importPilotDoc(file),
                PRO_BATCH_RECORD_REPORT_FILE_EXTENSION_INVALID);
    }

    @Test
    void importImage_rejectsNonImageFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pilot.doc", "application/msword",
                "fake".getBytes(StandardCharsets.UTF_8));

        assertServiceException(() -> reportService.importImage(file),
                PRO_BATCH_RECORD_REPORT_IMAGE_FILE_EXTENSION_INVALID);
    }

    @Test
    void importPilotDocWhenGatewayFails_rollsBackMetadataRows() throws Exception {
        when(parser.parse(any())).thenReturn(List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"),
                TestBatchRecordFixtures.parsedTable(2, "工序记录")));
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        when(jimuReportGateway.saveOrUpdateReport(any()))
                .thenReturn(TestBatchRecordFixtures.generatedReport("rollback-report-1", "EBR_DOC_TESTHASH_T01", "表1"))
                .thenThrow(new RuntimeException("gateway failed"));
        when(jimuReportGateway.getReportInfo("rollback-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "rollback-report-1", "EBR_DOC_TESTHASH_T01", "表1", LocalDateTime.now()));

        MockMultipartFile file = new MockMultipartFile(
                "file", PILOT_FILE_NAME, "application/msword",
                "fake".getBytes(StandardCharsets.UTF_8));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportService.importPilotDoc(file));

        assertEquals("gateway failed", exception.getMessage());
        assertEquals(0L, reportMapper.selectCount());
    }

    @Test
    void importImageWhenParserFails_rollsBackMetadataRows() {
        when(imageParser.parse(any(), any())).thenThrow(new RuntimeException("codex failed"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "batch-record.png", "image/png",
                "fake".getBytes(StandardCharsets.UTF_8));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportService.importImage(file));

        assertEquals("codex failed", exception.getMessage());
        assertEquals(0L, reportMapper.selectCount());
    }

    @Test
    void importPilotDocTwice_updatesExistingRowsInsteadOfDuplicating() throws Exception {
        when(parser.parse(any())).thenReturn(List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"),
                TestBatchRecordFixtures.parsedTable(2, "工序记录")));
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        when(jimuReportGateway.saveOrUpdateReport(any()))
                .thenReturn(TestBatchRecordFixtures.generatedReport("stable-report-1", "EBR_DOC_TESTHASH_T01", "表1"))
                .thenReturn(TestBatchRecordFixtures.generatedReport("stable-report-2", "EBR_DOC_TESTHASH_T02", "表2"))
                .thenReturn(TestBatchRecordFixtures.generatedReport("stable-report-1", "EBR_DOC_TESTHASH_T01", "表1-更新"))
                .thenReturn(TestBatchRecordFixtures.generatedReport("stable-report-2", "EBR_DOC_TESTHASH_T02", "表2-更新"));
        when(jimuReportGateway.getReportInfo("stable-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "stable-report-1", "EBR_DOC_TESTHASH_T01", "表1", LocalDateTime.now()))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "stable-report-1", "EBR_DOC_TESTHASH_T01", "表1-更新", LocalDateTime.now()));
        when(jimuReportGateway.getReportInfo("stable-report-2"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "stable-report-2", "EBR_DOC_TESTHASH_T02", "表2", LocalDateTime.now()))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "stable-report-2", "EBR_DOC_TESTHASH_T02", "表2-更新", LocalDateTime.now()));

        MockMultipartFile file = new MockMultipartFile(
                "file", PILOT_FILE_NAME, "application/msword",
                "fake".getBytes(StandardCharsets.UTF_8));

        reportService.importPilotDoc(file);
        reportService.importPilotDoc(file);

        assertEquals(2L, reportMapper.selectCount());
        verify(jimuReportGateway, times(4)).saveOrUpdateReport(any());
    }

    @Test
    void importImage_createsImageScopedReports() throws Exception {
        when(imageParser.parse(any(), any())).thenReturn(List.of(
                TestBatchRecordFixtures.parsedTable(1, "鍥剧墖鎵归噺璁板綍")));
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        when(jimuReportGateway.saveOrUpdateReport(any()))
                .thenReturn(TestBatchRecordFixtures.generatedReport("image-report-1", "EBR_IMG_HASH_T01", "鍥剧墖琛?"));
        when(jimuReportGateway.getReportInfo("image-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "image-report-1", "EBR_IMG_HASH_T01", "鍥剧墖琛?", LocalDateTime.now()));

        MockMultipartFile file = new MockMultipartFile(
                "file", "batch-record.png", "image/png",
                "fake".getBytes(StandardCharsets.UTF_8));

        MesProBatchRecordImportResult result = reportService.importImage(file);

        assertEquals(1, result.importedCount());
        assertEquals(1, result.createdCount());
        assertEquals(0, result.updatedCount());
        assertEquals(1L, reportMapper.selectCount());
        MesProBatchRecordReportDO saved = reportMapper.selectList().get(0);
        assertEquals("batch-record.png", saved.getSourceFileName());
    }

    @Test
    void uploadExtraFormSlot_whenLossReportWordHasMergedBody_expandsAllFillableFieldsAndDoesNotReuseOldHashReport()
            throws Exception {
        TenantContextHolder.setTenantId(1L);
        List<MesProBatchRecordParsedTable> lossReportTables = List.of(createLossReportSourceTable());
        when(parser.parse(any())).thenReturn(lossReportTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        String uploadedSha = sha256("same-loss-report-bytes".getBytes(StandardCharsets.UTF_8));
        String staleReportCode = "EBR_TN1_LOSS_REPORT_DOC_" + uploadedSha.substring(0, 8) + "_T01";
        MesProBatchRecordReportDO staleHashReport = TestBatchRecordFixtures.metadataReport(
                9001L, "STALE_LOSS_SLOT", 1, "stale-loss-report",
                staleReportCode,
                "历史损耗单", "loss.doc");
        staleHashReport.setRouteKey(MesProBatchRecordFormSlotType.LOSS_REPORT.getType());
        staleHashReport.setFormSlotType(MesProBatchRecordFormSlotType.LOSS_REPORT.getType());
        staleHashReport.setSourceFileSha256(uploadedSha);
        reportMapper.insert(staleHashReport);
        AtomicReference<MesProBatchRecordJimuReportSaveReq> saveReqRef = new AtomicReference<>();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            saveReqRef.set(saveReq);
            return TestBatchRecordFixtures.generatedReport(
                    "fresh-loss-report", saveReq.reportCode(), saveReq.reportName());
        });
        MockMultipartFile file = new MockMultipartFile(
                "file", "8.3-09（E 1）生产过程损耗报告单--2025.09.30生效(1).doc",
                "application/msword", "same-loss-report-bytes".getBytes(StandardCharsets.UTF_8));

        MesProBatchRecordImportResult result = reportService.uploadExtraFormSlot(
                file, "ADMIN-LOSS-BODY-REGRESSION", MesProBatchRecordFormSlotType.LOSS_REPORT.getType());

        assertEquals(1, result.importedCount());
        assertEquals(1, result.createdCount());
        assertEquals(0, result.updatedCount());
        assertNull(saveReqRef.get().existingReportId(), "extra form slot upload must not reuse stale same-hash report");
        assertNotEquals(staleReportCode, saveReqRef.get().reportCode(),
                "extra form slot upload must not create a Jimu report with the stale same-hash code");
        assertTrue(saveReqRef.get().reportCode().matches("EBR_TN1_LOSS_REPORT_DOC_[0-9a-f]{8}_V[0-9A-Z]+_T01"));
        MesProBatchRecordParsedTable parsedTable = saveReqRef.get().parsedTable();
        assertEquals(12, parsedTable.getRows().size());
        assertEquals("损耗描述：", parsedTable.getRows().get(1).get(0).getText());
        assertEquals(List.of("不合格日期", "工序名称", "不合格数量", "不合格原因", "处置方式", "生产人员/日期", "检验人员\n确认/日期"),
                parsedTable.getRows().get(2).stream().map(MesProBatchRecordParsedCell::getText).toList());
        assertEquals(9, parsedTable.getRows().get(3).size());
        assertEquals("□报废", parsedTable.getRows().get(3).get(4).getText());
        assertFalse(parsedTable.getRows().get(3).get(4).isFillable());
        assertEquals("□其他：", parsedTable.getRows().get(3).get(5).getText());
        assertFalse(parsedTable.getRows().get(3).get(5).isFillable());
        assertTrue(parsedTable.getRows().get(3).get(6).isFillable());
        assertEquals(8, parsedTable.getRows().get(3).get(7).getRowSpan());
        long fillableCount = parsedTable.getRows().stream()
                .flatMap(List::stream)
                .filter(MesProBatchRecordParsedCell::isFillable)
                .count();
        assertEquals(47, fillableCount);
        assertEquals(2L, reportMapper.selectCount());
        MesProBatchRecordReportDO saved = reportMapper.selectByReportId("fresh-loss-report");
        assertNotNull(saved);
        assertEquals("ADMIN-LOSS-BODY-REGRESSION", saved.getBatchRecordName());
        assertEquals(MesProBatchRecordFormSlotType.LOSS_REPORT.getType(), saved.getFormSlotType());
    }

    @Test
    void uploadExtraFormSlot_usesSelectedProductNameAndSlotDisplayNameInListMetadata() throws Exception {
        TenantContextHolder.setTenantId(1L);
        when(parser.parse(any())).thenReturn(List.of(TestBatchRecordFixtures.parsedTable(1, "产品名称")));
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        AtomicReference<MesProBatchRecordJimuReportSaveReq> saveReqRef = new AtomicReference<>();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            saveReqRef.set(saveReq);
            return TestBatchRecordFixtures.generatedReport(
                    "loss-slot-list-report", saveReq.reportCode(), saveReq.reportName());
        });
        MockMultipartFile file = new MockMultipartFile(
                "file", "8.3-09（E 1）生产过程损耗报告单--2025.09.30生效(1).doc",
                "application/msword", "loss-report-list-metadata".getBytes(StandardCharsets.UTF_8));

        MesProBatchRecordImportResult importResult = reportService.uploadExtraFormSlot(
                file, "球囊扩张压力泵", MesProBatchRecordFormSlotType.LOSS_REPORT.getType());

        assertEquals("损耗单", saveReqRef.get().reportName());
        assertEquals("球囊扩张压力泵", importResult.reports().get(0).productName());
        assertEquals("损耗单", importResult.reports().get(0).reportName());
        when(jimuReportGateway.getReportInfo("loss-slot-list-report"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "loss-slot-list-report", saveReqRef.get().reportCode(), "损耗单", LocalDateTime.now()));
        BatchRecordReportPageReqVO pageReqVO = new BatchRecordReportPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(20);
        pageReqVO.setProductName("球囊扩张压力泵");
        pageReqVO.setFormSlotType(MesProBatchRecordFormSlotType.LOSS_REPORT.getType());

        PageResult<MesProBatchRecordReportView> pageResult = reportService.getGeneratedReportPage(pageReqVO);

        assertEquals(1L, pageResult.getTotal());
        assertEquals("球囊扩张压力泵", pageResult.getList().get(0).productName());
        assertEquals("损耗单", pageResult.getList().get(0).reportName());
    }

    @Test
    void uploadExtraFormSlot_whenLegacySlotAlreadyExists_createsUpgradeVersionAndKeepsOldVersion() throws Exception {
        TenantContextHolder.setTenantId(1L);
        MesProBatchRecordReportDO existing = TestBatchRecordFixtures.metadataReport(
                9101L, "EXISTING_LOSS_SLOT", 1, "existing-loss-report",
                "EBR_EXISTING_LOSS_T01", "历史损耗单", "loss.doc");
        existing.setBatchRecordName("球囊扩张压力泵");
        existing.setProductName("球囊扩张压力泵");
        existing.setRouteKey(MesProBatchRecordFormSlotType.LOSS_REPORT.getType());
        existing.setFormSlotType(MesProBatchRecordFormSlotType.LOSS_REPORT.getType());
        reportMapper.insert(existing);
        when(parser.parse(any())).thenReturn(List.of(TestBatchRecordFixtures.parsedTable(1, "产品名称")));
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        AtomicReference<MesProBatchRecordJimuReportSaveReq> saveReqRef = new AtomicReference<>();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            saveReqRef.set(saveReq);
            return TestBatchRecordFixtures.generatedReport(
                    "loss-slot-upgrade-v2-report", saveReq.reportCode(), saveReq.reportName());
        });
        when(jimuReportGateway.getReportInfo(anyString())).thenAnswer(invocation -> {
            String reportId = invocation.getArgument(0);
            MesProBatchRecordReportDO metadata = reportMapper.selectByReportId(reportId);
            if (metadata == null) {
                return null;
            }
            return TestBatchRecordFixtures.reportInfo(
                    reportId, metadata.getReportCode(), metadata.getReportName(), LocalDateTime.now());
        });

        MesProBatchRecordImportResult result = reportService.uploadExtraFormSlot(
                new MockMultipartFile("file", "loss-v2.doc", "application/msword",
                        "loss-report-v2".getBytes(StandardCharsets.UTF_8)),
                "球囊扩张压力泵", MesProBatchRecordFormSlotType.LOSS_REPORT.getType(), 101L);

        assertEquals(1, result.createdCount());
        assertEquals(0, result.updatedCount());
        assertEquals("V2.0", result.versionNo());
        assertEquals("PENDING_APPROVAL", result.versionStatus());
        assertNotNull(result.sourceBatchRecordVersionId());
        assertEquals("球囊扩张压力泵", result.reports().get(0).productName());
        assertEquals("损耗单", result.reports().get(0).reportName());
        assertNull(saveReqRef.get().existingReportId());
        assertTrue(saveReqRef.get().reportCode().matches("EBR_TN1_LOSS_REPORT_DOC_[0-9a-f]{8}_V[0-9A-Z]+_T01"));
        MesProBatchRecordReportDO legacy = reportMapper.selectByReportId("existing-loss-report");
        assertNotNull(legacy.getBatchRecordDefinitionId());
        assertEquals(result.sourceBatchRecordVersionId(), legacy.getBatchRecordVersionId());
        MesProBatchRecordReportDO upgrade = reportMapper.selectByReportId("loss-slot-upgrade-v2-report");
        assertEquals(result.batchRecordDefinitionId(), upgrade.getBatchRecordDefinitionId());
        assertEquals(result.batchRecordVersionId(), upgrade.getBatchRecordVersionId());

        BatchRecordReportPageReqVO pageReqVO = new BatchRecordReportPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(20);
        pageReqVO.setProductName("球囊扩张压力泵");
        pageReqVO.setFormSlotType(MesProBatchRecordFormSlotType.LOSS_REPORT.getType());
        PageResult<MesProBatchRecordReportView> pageResult = reportService.getGeneratedReportPage(pageReqVO);

        assertEquals(2L, pageResult.getTotal());
        assertTrue(pageResult.getList().stream().anyMatch(report -> Objects.equals("V1.0", report.versionNo())));
        assertTrue(pageResult.getList().stream().anyMatch(report -> Objects.equals("V2.0", report.versionNo())));
    }

    @Test
    void uploadExtraFormSlot_whenLegacySlotAdopted_recordsDirectApprovalAndObsoletesStaleApproved() throws Exception {
        TenantContextHolder.setTenantId(1L);
        Long actorUserId = 101L;
        String batchRecordName = "历史接管损耗单";
        String formSlotType = MesProBatchRecordFormSlotType.LOSS_REPORT.getType();
        MesProBatchRecordDefinitionDO definition = insertDefinition(batchRecordName, formSlotType);
        MesProBatchRecordVersionDO staleApproved = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "stale-loss.doc", "stale-loss-sha256", null, null);
        MesProBatchRecordReportDO legacy = TestBatchRecordFixtures.metadataReport(
                9102L, "LEGACY_LOSS_SLOT_DIRECT", 1, "legacy-loss-report",
                "EBR_LEGACY_LOSS_T01", "历史损耗单", "legacy-loss.doc");
        legacy.setBatchRecordName(batchRecordName);
        legacy.setProductName(batchRecordName);
        legacy.setRouteKey(formSlotType);
        legacy.setFormSlotType(formSlotType);
        reportMapper.insert(legacy);
        when(parser.parse(any())).thenReturn(List.of(TestBatchRecordFixtures.parsedTable(1, "产品名称")));
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            return TestBatchRecordFixtures.generatedReport(
                    "legacy-loss-slot-v3-report", saveReq.reportCode(), saveReq.reportName());
        });
        when(jimuReportGateway.getReportInfo(anyString())).thenAnswer(invocation -> {
            String reportId = invocation.getArgument(0);
            MesProBatchRecordReportDO metadata = reportMapper.selectByReportId(reportId);
            if (metadata == null) {
                return null;
            }
            return TestBatchRecordFixtures.reportInfo(
                    reportId, metadata.getReportCode(), metadata.getReportName(), LocalDateTime.now());
        });

        MesProBatchRecordImportResult result = reportService.uploadExtraFormSlot(
                new MockMultipartFile("file", "loss-v3.doc", "application/msword",
                        "loss-report-v3".getBytes(StandardCharsets.UTF_8)),
                batchRecordName, formSlotType, actorUserId);

        Long adoptedVersionId = result.sourceBatchRecordVersionId();
        assertNotNull(adoptedVersionId);
        MesProBatchRecordReportDO adoptedLegacyReport = reportMapper.selectByReportId("legacy-loss-report");
        assertEquals(definition.getId(), adoptedLegacyReport.getBatchRecordDefinitionId());
        assertEquals(adoptedVersionId, adoptedLegacyReport.getBatchRecordVersionId());
        MesProBatchRecordDefinitionDO adoptedDefinition = definitionMapper.selectById(definition.getId());
        assertEquals(adoptedVersionId, adoptedDefinition.getCurrentVersionId());
        MesProBatchRecordVersionDO adoptedVersion = versionMapper.selectById(adoptedVersionId);
        assertEquals("APPROVED", adoptedVersion.getStatus());
        assertEquals(actorUserId, adoptedVersion.getApprovedBy());
        assertNotNull(adoptedVersion.getApprovedAt());
        assertEquals("OBSOLETE", versionMapper.selectById(staleApproved.getId()).getStatus());
        Long directEventCount = jdbcTemplate().queryForObject("""
                SELECT COUNT(*) FROM mes_pro_batch_record_version_approval_event
                WHERE version_id = ? AND approval_result = 'DIRECT'
                  AND processed_result = 'APPROVED' AND actor_user_id = ?
                """, Long.class, adoptedVersionId, actorUserId);
        assertEquals(1L, directEventCount);
    }

    @Test
    void importImage_trimsGeneratedReportNameToJimuLimit() throws Exception {
        when(imageParser.parse(any(), any())).thenReturn(List.of(
                TestBatchRecordFixtures.parsedTable(1, "Long image report title for screenshot verification output")));
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        when(jimuReportGateway.saveOrUpdateReport(any()))
                .thenReturn(TestBatchRecordFixtures.generatedReport("image-report-2", "EBR_IMG_HASH_T01", "trimmed"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "ScreenShot_2026-05-15_170551_614.png", "image/png",
                "fake".getBytes(StandardCharsets.UTF_8));

        reportService.importImage(file);

        ArgumentCaptor<MesProBatchRecordJimuReportSaveReq> saveReqCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordJimuReportSaveReq.class);
        verify(jimuReportGateway).saveOrUpdateReport(saveReqCaptor.capture());
        String reportName = saveReqCaptor.getValue().reportName();
        assertEquals(MesProBatchRecordReportServiceImpl.JIMU_REPORT_NAME_MAX_LENGTH, reportName.length());
        assertTrue(reportName.length() <= MesProBatchRecordReportServiceImpl.JIMU_REPORT_NAME_MAX_LENGTH);
    }

    @Test
    void importImage_withTenantContext_usesTenantScopedReportCode() throws Exception {
        TenantContextHolder.setTenantId(122L);
        when(imageParser.parse(any(), any())).thenReturn(List.of(
                TestBatchRecordFixtures.parsedTable(1, "图片批量记录")));
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        when(jimuReportGateway.saveOrUpdateReport(any()))
                .thenReturn(TestBatchRecordFixtures.generatedReport("image-report-tenant", "ignored", "图片表"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "batch-record.png", "image/png",
                "fake".getBytes(StandardCharsets.UTF_8));

        reportService.importImage(file);

        ArgumentCaptor<MesProBatchRecordJimuReportSaveReq> saveReqCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordJimuReportSaveReq.class);
        verify(jimuReportGateway).saveOrUpdateReport(saveReqCaptor.capture());
        assertEquals("EBR_TN122_IMG_b5d54c39_T01", saveReqCaptor.getValue().reportCode());
        assertEquals("IMG_b5d54c39e66671c9731b9f471e585d82_TN122",
                reportMapper.selectList().get(0).getSampleKey());
    }

    @Test
    void recognizeFixedRoute_usesConfiguredWorkspaceSamplePath() {
        List<MesProBatchRecordParsedTable> parsedTables = IntStream.rangeClosed(1, 15)
                .mapToObj(index -> TestBatchRecordFixtures.parsedTable(index, "Route B Table " + index))
                .toList();
        when(parser.parse(any())).thenReturn(parsedTables);
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        AtomicInteger counter = new AtomicInteger();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            int index = counter.incrementAndGet();
            return TestBatchRecordFixtures.generatedReport(
                    "route-b-report-" + index,
                    saveReq.reportCode(),
                    saveReq.reportName());
        });

        MesProBatchRecordImportResult result = reportService.recognizeFixedRoute(
                MesProBatchRecordRecognitionRouteKeys.B);

        ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
        ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(routeRecognizer).recognize(pathCaptor.capture(), any(), fileNameCaptor.capture());
        assertEquals(Path.of(MesProBatchRecordReportConstants.FIXED_SAMPLE_PATH), pathCaptor.getValue());
        assertEquals("批记录模板.doc", fileNameCaptor.getValue());
        assertEquals(15, result.importedCount());
        assertEquals(15L, reportMapper.selectCount());
        assertEquals("批记录模板.doc", result.reports().get(0).sourceFileName());
        assertEquals(MesProBatchRecordRecognitionRouteKeys.B, result.reports().get(0).routeKey());
    }

    @Test
    void recognizeFixedRoute_withTenantContext_usesTenantScopedReportCodes() {
        TenantContextHolder.setTenantId(122L);
        when(routeRecognizer.routeKey()).thenReturn(MesProBatchRecordRecognitionRouteKeys.A);
        List<MesProBatchRecordParsedTable> parsedTables = IntStream.rangeClosed(1, 15)
                .mapToObj(index -> TestBatchRecordFixtures.parsedTable(index, "Route A Table " + index))
                .toList();
        when(parser.parse(any())).thenReturn(parsedTables);
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        AtomicInteger counter = new AtomicInteger();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            int index = counter.incrementAndGet();
            return TestBatchRecordFixtures.generatedReport(
                    "route-a-tenant-report-" + index,
                    saveReq.reportCode(),
                    saveReq.reportName());
        });

        reportService.recognizeFixedRoute(MesProBatchRecordRecognitionRouteKeys.A);

        ArgumentCaptor<MesProBatchRecordJimuReportSaveReq> saveReqCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordJimuReportSaveReq.class);
        verify(jimuReportGateway, times(15)).saveOrUpdateReport(saveReqCaptor.capture());
        List<MesProBatchRecordJimuReportSaveReq> saveRequests = saveReqCaptor.getAllValues();
        assertTrue(saveRequests.get(0).reportCode().matches("EBR_TN122_A_DOC_[0-9a-f]{8}_T01"));
        assertTrue(saveRequests.get(14).reportCode().matches("EBR_TN122_A_DOC_[0-9a-f]{8}_T15"));
        assertEquals("FIXED_DOC_TN122", reportMapper.selectList().get(0).getSampleKey());
    }

    @Test
    void recognizeUploadedRoute_usesUploadedWordBytesAndRouteScopedMetadata() {
        List<MesProBatchRecordParsedTable> parsedTables = uploadedRouteParsedTables(15, "Uploaded Route B Table ");
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        AtomicInteger counter = new AtomicInteger();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            int index = counter.incrementAndGet();
            return TestBatchRecordFixtures.generatedReport(
                    "uploaded-route-b-report-" + index,
                    saveReq.reportCode(),
                    saveReq.reportName());
        });
        MockMultipartFile file = new MockMultipartFile(
                "file", "user-selected.doc", "application/msword",
                "uploaded-word-bytes".getBytes(StandardCharsets.UTF_8));
        seedWorkOrderProduct("球囊扩张压力泵", "BRP-001");
        seedDccProjectCode("球囊扩张压力泵", "BRP-001");

        MesProBatchRecordImportResult result = reportService.recognizeUploadedRoute(
                file, MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", false, List.of("球囊扩张压力泵"));

        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(routeRecognizer).recognize(any(), bytesCaptor.capture(), fileNameCaptor.capture());
        assertEquals("uploaded-word-bytes", new String(bytesCaptor.getValue(), StandardCharsets.UTF_8));
        assertEquals("user-selected.doc", fileNameCaptor.getValue());
        assertEquals(15, result.importedCount());
        assertEquals(15L, reportMapper.selectCount());
        MesProBatchRecordReportDO saved = reportMapper.selectList().get(0);
        assertEquals("user-selected.doc", saved.getSourceFileName());
        assertEquals("球囊扩张压力泵", saved.getBatchRecordName());
        assertEquals(MesProBatchRecordRecognitionRouteKeys.B, saved.getRouteKey());
        assertEquals("产品信息", saved.getReportName());
        assertTrue(saved.getReportCode().matches("EBR_(TN\\d+_)?B_DOC_.*"));
        assertTrue(saved.getSampleKey().startsWith("BATCH_"));
        ArgumentCaptor<MesProBatchRecordJimuReportSaveReq> saveCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordJimuReportSaveReq.class);
        verify(jimuReportGateway, times(15)).saveOrUpdateReport(saveCaptor.capture());
        assertEquals("产品信息", saveCaptor.getAllValues().get(0).reportName());
        assertTrue(saveCaptor.getAllValues().stream().noneMatch(saveReq ->
                saveReq.reportName().contains("球囊扩张压力泵")
                        || saveReq.reportName().contains("[B]")
                        || saveReq.reportName().contains("user-selected.doc")));
    }

    @Test
    void recognizeUploadedRoute_acceptsUploadedWordWithSixteenParsedTemplates() {
        List<MesProBatchRecordParsedTable> parsedTables = uploadedRouteParsedTables(16, "Uploaded Route B Table ");
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        AtomicInteger counter = new AtomicInteger();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            int index = counter.incrementAndGet();
            return TestBatchRecordFixtures.generatedReport(
                    "uploaded-route-b-report-" + index,
                    saveReq.reportCode(),
                    saveReq.reportName());
        });
        MockMultipartFile file = new MockMultipartFile(
                "file", "press-balloon-pump.doc", "application/msword",
                "uploaded-word-bytes".getBytes(StandardCharsets.UTF_8));
        seedWorkOrderProduct("球囊扩张压力泵", "BRP-001");
        seedDccProjectCode("球囊扩张压力泵", "BRP-001");

        MesProBatchRecordImportResult result = reportService.recognizeUploadedRoute(
                file, MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", false, List.of("球囊扩张压力泵"));

        assertEquals(16, result.importedCount());
        assertEquals(16, result.createdCount());
        assertEquals(0, result.updatedCount());
        assertEquals(16L, reportMapper.selectCount());
        ArgumentCaptor<MesProBatchRecordJimuReportSaveReq> saveCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordJimuReportSaveReq.class);
        verify(jimuReportGateway, times(16)).saveOrUpdateReport(saveCaptor.capture());
        assertEquals("Uploaded Route B Table 16", saveCaptor.getAllValues().get(15).reportName());
        assertTrue(saveCaptor.getAllValues().get(15).reportCode().endsWith("_T16"));
    }

    @Test
    void recognizeUploadedRoute_generatesEnabledRouteAndBatchRecordRouteBindingsSkippingProductInfo() {
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"),
                TestBatchRecordFixtures.parsedTable(2, "粗洗工序"),
                TestBatchRecordFixtures.parsedTable(3, "精洗工序"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        AtomicInteger counter = new AtomicInteger();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            int index = counter.incrementAndGet();
            return TestBatchRecordFixtures.generatedReport(
                    "uploaded-route-binding-report-" + index,
                    saveReq.reportCode(),
                    saveReq.reportName());
        });
        MockMultipartFile file = new MockMultipartFile(
                "file", "route-source.doc", "application/msword",
                "uploaded-word-route-bytes".getBytes(StandardCharsets.UTF_8));
        Long firstItemId = seedWorkOrderProduct("球囊扩张压力泵", "BRP-001");
        Long secondItemId = seedWorkOrderProduct("球囊扩张压力泵", "BRP-002");
        seedDccProjectCode("球囊扩张压力泵", "BRP-001");
        seedDccProjectCode("球囊扩张压力泵", "BRP-002");

        Long creatorUserId = 703L;
        MesProBatchRecordImportResult result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(creatorUserId);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("word-importer");
            result = reportService.recognizeUploadedRoute(
                    file, MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", false,
                    List.of("球囊扩张压力泵"));
        }

        assertEquals(3, result.importedCount());
        assertEquals(3, result.createdCount());
        assertEquals(0, result.updatedCount());
        assertNotNull(result.routeId());
        assertEquals("ROUTE-IMPORT-1", result.routeCode());
        assertEquals("球囊扩张压力泵", result.routeName());
        assertEquals(2, result.routeProcessCount());
        assertEquals(2, result.batchRecordRouteBindingCount());
        assertEquals(1, result.boundProductNameCount());
        assertEquals(2, result.boundProductCodeCount());
        assertEquals(List.of(), result.skippedProductNames());
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_route WHERE id = ? AND code = ? AND name = ? AND status = ?",
                result.routeId(), result.routeCode(), "球囊扩张压力泵", CommonStatusEnum.ENABLE.getStatus()));
        assertEquals(2, rawCount("SELECT COUNT(*) FROM mes_pro_route_product WHERE route_id = ?", result.routeId()));
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_route_product WHERE route_id = ? AND item_id = ?",
                result.routeId(), firstItemId));
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_route_product WHERE route_id = ? AND item_id = ?",
                result.routeId(), secondItemId));
        assertEquals("V1", result.routeVersionNo());
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_route_version WHERE route_id = ? AND version_no = ? AND active = TRUE",
                result.routeId(), "V1"));
        MesProRouteVersionDO activeRouteVersion = routeVersionMapper.selectById(result.routeVersionId());
        JSONObject activeSnapshot = JSONObject.parseObject(activeRouteVersion.getRouteSnapshotJson());
        JSONObject activeConfigSnapshots = activeSnapshot.getJSONObject("configSnapshots");
        assertNotNull(activeConfigSnapshots.getJSONArray("scheduleUseConfigs"));
        assertEquals(2, activeConfigSnapshots.getJSONObject("flowGraph").getJSONArray("nodes").size());
        assertEquals(2, activeConfigSnapshots.getJSONArray("batchUseConfigs").size());
        JSONObject firstActiveBatchUseConfig = activeConfigSnapshots.getJSONArray("batchUseConfigs").getJSONObject(0);
        assertEquals("BATCH_RECORD", firstActiveBatchUseConfig.getString("recordCategory"));
        assertEquals("CONTROLLED_BATCH", firstActiveBatchUseConfig.getString("validationProfile"));
        assertNotNull(firstActiveBatchUseConfig.getLong("permissionScopeId"));
        assertNotNull(firstActiveBatchUseConfig.getString("recordCategorySnapshotHash"));
        assertNotNull(firstActiveBatchUseConfig.getString("slotConfigSnapshotHash"));
        assertEquals(2, rawCount("SELECT COUNT(*) FROM mes_pro_route_process WHERE route_id = ?", result.routeId()));
        assertEquals(0, rawCount("""
                SELECT COUNT(*) FROM mes_pro_route_process
                WHERE route_id = ? AND batch_record_report_id IS NOT NULL
                """, result.routeId()));
        assertEquals(0, rawCount("""
                SELECT COUNT(*) FROM mes_pro_route_process rp
                JOIN mes_pro_process p ON p.id = rp.process_id
                WHERE rp.route_id = ? AND p.name = ?
                """, result.routeId(), "产品信息"));
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_process WHERE name = ?", "粗洗工序"));
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_process WHERE name = ?", "精洗工序"));
        String useType = MesProRouteFlowConfigTypeEnum.BATCH.getType();
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_route_flow_config WHERE route_id = ? AND use_type = ?",
                result.routeId(), useType));
        assertEquals(2, rawCount("""
                SELECT COUNT(*) FROM mes_pro_route_flow_process_config
                WHERE route_id = ? AND use_type = ? AND enabled = TRUE AND execution_mode = ?
                  AND batch_record_report_id IS NULL
                """, result.routeId(), useType, "SEQUENTIAL"));
        assertEquals(2, rawCount("""
                SELECT COUNT(*) FROM mes_pro_route_flow_process_batch_record
                WHERE route_id = ? AND use_type = ? AND record_category = ? AND validation_profile = ?
                  AND required_policy = ? AND owner_role_key = ? AND archive_visibility = ? AND report_sort = 1
                """, result.routeId(), useType, "BATCH_RECORD", "CONTROLLED_BATCH",
                "REQUIRED", "PRODUCTION", "FINAL_DHR"));
        assertEquals(2, rawCount("""
                SELECT COUNT(*) FROM mes_pro_route_flow_process_batch_record
                WHERE route_id = ? AND permission_scope_id IS NOT NULL
                  AND record_category_snapshot_hash IS NOT NULL
                  AND slot_config_snapshot_hash IS NOT NULL
                """, result.routeId()));

        ArgumentCaptor<MesProEdhrPermissionScopeSaveCommand> permissionCaptor =
                ArgumentCaptor.forClass(MesProEdhrPermissionScopeSaveCommand.class);
        verify(permissionScopeService).saveRules(permissionCaptor.capture());
        MesProEdhrPermissionScopeSaveCommand permissionCommand = permissionCaptor.getValue();
        assertEquals("route-" + result.routeId(), permissionCommand.getScopeName());
        assertEquals("ROUTE", permissionCommand.getObjectType());
        assertEquals(String.valueOf(result.routeId()), permissionCommand.getObjectId());
        assertEquals(creatorUserId, permissionCommand.getActorUserId());
        assertEquals("word-importer", permissionCommand.getActorUsername());
        assertEquals(List.of("VIEW", "ROUTE_EDIT", "PERMISSION_ADMIN"),
                permissionCommand.getRules().stream().map(MesProEdhrPermissionRuleCommand::getAbility).toList());
        permissionCommand.getRules().forEach(rule -> {
            assertEquals("USER", rule.getSubjectType());
            assertEquals(creatorUserId, rule.getSubjectId());
            assertEquals("ALLOW", rule.getDecision());
            assertEquals("ENABLED", rule.getStatus());
        });
    }

    @Test
    void recognizeUploadedRoute_whenOnlyRouteRebuildHasNoBatchRecordVersion_generatesRouteWithoutBatchRecordBinding() {
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"),
                TestBatchRecordFixtures.parsedTable(2, "粗洗工序"),
                TestBatchRecordFixtures.parsedTable(3, "精洗工序"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        seedDccProjectCode("球囊扩张压力泵", "BRP-ROUTE-ONLY");
        MockMultipartFile file = new MockMultipartFile(
                "file", "route-only.doc", "application/msword",
                "route-only-word-bytes".getBytes(StandardCharsets.UTF_8));

        MesProBatchRecordImportResult result = reportService.recognizeUploadedRoute(
                file, MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", "REBUILD_V1", null,
                List.of("球囊扩张压力泵"), false, List.of(), List.of("球囊扩张压力泵"));

        assertEquals(0, result.importedCount());
        assertEquals(0, result.createdCount());
        assertEquals(0, result.updatedCount());
        assertNull(result.batchRecordDefinitionId());
        assertNull(result.batchRecordVersionId());
        assertNull(result.versionNo());
        assertNotNull(result.routeId());
        assertEquals("ROUTE-IMPORT-1", result.routeCode());
        assertEquals("球囊扩张压力泵", result.routeName());
        assertEquals(2, result.routeProcessCount());
        assertEquals(0, result.batchRecordRouteBindingCount());
        assertEquals(1, result.boundProductNameCount());
        assertEquals(1, result.boundProductCodeCount());
        assertEquals(0L, reportMapper.selectCount());
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_definition WHERE batch_record_name = ?",
                "球囊扩张压力泵"));
        assertEquals(2, rawCount("SELECT COUNT(*) FROM mes_pro_route_process WHERE route_id = ?", result.routeId()));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_route_flow_process_batch_record WHERE route_id = ?",
                result.routeId()));
        assertEquals(2, rawCount("""
                SELECT COUNT(*) FROM mes_pro_route_flow_process_config
                WHERE route_id = ? AND batch_record_report_id IS NULL
                """, result.routeId()));
        verifyNoInteractions(jimuReportGateway);
    }

    @Test
    void recognizeUploadedRoute_whenOnlyRouteRebuildUsesExistingRouteProductWithoutBatchRecordVersion_allowsUpgrade() {
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"),
                TestBatchRecordFixtures.parsedTable(2, "粗洗工序"),
                TestBatchRecordFixtures.parsedTable(3, "精洗工序"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        seedDccProjectCode("球囊扩张压力泵", "BRP-ROUTE-ONLY-EXISTING");
        Long itemId = seedProductItem("球囊扩张压力泵", "BRP-ROUTE-ONLY-EXISTING");
        MesProRouteDO route = MesProRouteDO.builder()
                .code("ROUTE-ONLY-EXISTING")
                .name("球囊扩张压力泵")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(route);
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder()
                .routeId(route.getId())
                .versionNo("V1")
                .active(true)
                .routeSnapshotJson("{}")
                .remark("仅重建路线源版本")
                .build();
        routeVersionMapper.insert(routeVersion);
        MesProRouteProductDO routeProduct = MesProRouteProductDO.builder()
                .routeId(route.getId())
                .itemId(itemId)
                .quantity(1)
                .build();
        routeProductMapper.insert(routeProduct);
        MockMultipartFile file = new MockMultipartFile(
                "file", "route-only-existing.doc", "application/msword",
                "route-only-existing-word-bytes".getBytes(StandardCharsets.UTF_8));

        MesProBatchRecordImportResult result = reportService.recognizeUploadedRoute(
                file, MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", "REBUILD_V1",
                null, null, List.of("球囊扩张压力泵"), false,
                List.of(routeProduct.getId()), List.of("球囊扩张压力泵"),
                true, route.getId(), routeVersion.getId(), null);

        assertEquals(route.getId(), result.routeId());
        assertEquals("ROUTE-ONLY-EXISTING", result.routeCode());
        assertEquals("V2", result.routeVersionNo());
        assertEquals(0, result.importedCount());
        assertEquals(0, result.batchRecordRouteBindingCount());
        assertNull(result.batchRecordDefinitionId());
        assertNull(result.batchRecordVersionId());
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_definition WHERE batch_record_name = ?",
                "球囊扩张压力泵"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_route_flow_process_batch_record WHERE route_id = ?",
                route.getId()));
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_route_version WHERE route_id = ? AND version_no = ? AND active = FALSE AND lifecycle_status = ?",
                route.getId(), "V2", "DRAFT"));
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_route_version WHERE id = ? AND active = TRUE",
                routeVersion.getId()));
        MesProRouteVersionDO candidateRouteVersion = routeVersionMapper.selectById(result.routeVersionId());
        assertEquals(routeVersion.getId(), candidateRouteVersion.getSourceRouteVersionId());
        JSONObject candidateSnapshot = JSONObject.parseObject(candidateRouteVersion.getRouteSnapshotJson());
        JSONObject candidateConfigSnapshots = candidateSnapshot.getJSONObject("configSnapshots");
        assertNotNull(candidateConfigSnapshots.getJSONArray("scheduleUseConfigs"));
        verifyNoInteractions(jimuReportGateway);
    }

    @Test
    void recognizeUploadedRoute_whenCurrentVersionRouteDiffersFromPreflightRoute_acceptsSelectedCurrentRouteProduct() {
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"),
                TestBatchRecordFixtures.parsedTable(2, "粗洗工序"),
                TestBatchRecordFixtures.parsedTable(3, "精洗工序"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        seedDccProjectCode("球囊扩张压力泵", "BRP-CURRENT-ROUTE");
        Long currentItemId = seedProductItem("球囊扩张压力泵", "BRP-CURRENT-ROUTE");
        MesProRouteDO staleRoute = MesProRouteDO.builder()
                .code("ROUTE-STALE-LINK")
                .name("历史批记录路线")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(staleRoute);
        MesProRouteDO currentRoute = MesProRouteDO.builder()
                .code("ROUTE-CURRENT-PREFLIGHT")
                .name("球囊扩张压力泵")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(currentRoute);
        MesProRouteVersionDO currentRouteVersion = MesProRouteVersionDO.builder()
                .routeId(currentRoute.getId())
                .versionNo("V1")
                .active(true)
                .routeSnapshotJson("{}")
                .remark("当前预检路线")
                .build();
        routeVersionMapper.insert(currentRouteVersion);
        MesProRouteProductDO currentRouteProduct = MesProRouteProductDO.builder()
                .routeId(currentRoute.getId())
                .itemId(currentItemId)
                .quantity(1)
                .build();
        routeProductMapper.insert(currentRouteProduct);
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("球囊扩张压力泵");
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "current.doc", "sha-current", staleRoute.getId(), null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        for (int sourceTableIndex = 2; sourceTableIndex <= 3; sourceTableIndex++) {
            MesProBatchRecordReportDO existing = TestBatchRecordFixtures.metadataReport(
                    (long) sourceTableIndex, "CURRENT_ROUTE_SCOPE", sourceTableIndex,
                    "current-route-scope-report-" + sourceTableIndex,
                    "EBR_SCOPE_T" + sourceTableIndex, "既有表" + sourceTableIndex, "current.doc");
            existing.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
            existing.setBatchRecordName("球囊扩张压力泵");
            existing.setBatchRecordDefinitionId(definition.getId());
            existing.setBatchRecordVersionId(currentVersion.getId());
            existing.setFormSlotType(MesProBatchRecordFormSlotType.MAIN.getType());
            reportMapper.insert(existing);
            when(jimuReportGateway.getReportInfo("current-route-scope-report-" + sourceTableIndex))
                    .thenReturn(TestBatchRecordFixtures.reportInfo(
                            "current-route-scope-report-" + sourceTableIndex,
                            "EBR_SCOPE_T" + sourceTableIndex,
                            "既有表" + sourceTableIndex,
                            LocalDateTime.now()));
        }
        MockMultipartFile file = new MockMultipartFile(
                "file", "current-route-scope.doc", "application/msword",
                "current-route-scope-word-bytes".getBytes(StandardCharsets.UTF_8));

        MesProBatchRecordImportResult result = reportService.recognizeUploadedRoute(
                file, MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", "UPGRADE",
                currentVersion.getId(), null, List.of("球囊扩张压力泵"), false,
                List.of(currentRouteProduct.getId()), List.of(), true,
                currentRoute.getId(), currentRouteVersion.getId(), null);

        assertEquals(currentRoute.getId(), result.routeId());
        assertEquals("ROUTE-CURRENT-PREFLIGHT", result.routeCode());
        assertEquals(2, result.routeProcessCount());
        assertEquals(2, result.batchRecordRouteBindingCount());
        assertEquals(currentRoute.getId(), versionMapper.selectById(currentVersion.getId()).getRouteId());
        MesProRouteVersionDO candidateRouteVersion = routeVersionMapper.selectById(result.routeVersionId());
        JSONObject candidateSnapshot = JSONObject.parseObject(candidateRouteVersion.getRouteSnapshotJson());
        JSONObject candidateConfigSnapshots = candidateSnapshot.getJSONObject("configSnapshots");
        assertNotNull(candidateConfigSnapshots.getJSONArray("scheduleUseConfigs"));
        assertEquals(2, candidateConfigSnapshots.getJSONArray("batchUseConfigs").size());
        JSONObject firstBatchUseConfig = candidateConfigSnapshots.getJSONArray("batchUseConfigs").getJSONObject(0);
        assertEquals("BATCH_RECORD", firstBatchUseConfig.getString("recordCategory"));
        assertEquals("CONTROLLED_BATCH", firstBatchUseConfig.getString("validationProfile"));
        assertNotNull(firstBatchUseConfig.getLong("permissionScopeId"));
        assertNotNull(firstBatchUseConfig.getString("recordCategorySnapshotHash"));
        assertNotNull(firstBatchUseConfig.getString("slotConfigSnapshotHash"));
    }

    @Test
    void recognizeUploadedRoute_whenUpgradingRoute_preservesStableProcessConnectionInfo() {
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"),
                TestBatchRecordFixtures.parsedTable(2, "粗洗工序"),
                TestBatchRecordFixtures.parsedTable(3, "精洗工序"),
                TestBatchRecordFixtures.parsedTable(4, "终检工序"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            return TestBatchRecordFixtures.generatedReport(
                    "route-upgrade-link-report-" + saveReq.parsedTable().getSourceTableIndex(),
                    saveReq.reportCode(),
                    saveReq.reportName());
        });
        seedDccProjectCode("连接信息保留批记录", "BRP-LINK-PRESERVE");
        Long currentItemId = seedProductItem("连接信息保留批记录", "BRP-LINK-PRESERVE");
        MesProRouteDO currentRoute = MesProRouteDO.builder()
                .code("ROUTE-LINK-PRESERVE")
                .name("连接信息保留批记录")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(currentRoute);
        MesProRouteVersionDO currentRouteVersion = MesProRouteVersionDO.builder()
                .routeId(currentRoute.getId())
                .versionNo("V1")
                .active(true)
                .routeSnapshotJson("{}")
                .remark("带连接信息的当前路线")
                .build();
        routeVersionMapper.insert(currentRouteVersion);
        MesProRouteProductDO currentRouteProduct = MesProRouteProductDO.builder()
                .routeId(currentRoute.getId())
                .itemId(currentItemId)
                .quantity(1)
                .build();
        routeProductMapper.insert(currentRouteProduct);
        jdbcTemplate().update("""
                INSERT INTO mes_pro_process
                (id, code, name, status, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, 931001L, "PROC-LINK-A", "粗洗工序", CommonStatusEnum.ENABLE.getStatus(),
                "tester", "tester", false, 1L);
        jdbcTemplate().update("""
                INSERT INTO mes_pro_process
                (id, code, name, status, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, 931002L, "PROC-LINK-B", "精洗工序", CommonStatusEnum.ENABLE.getStatus(),
                "tester", "tester", false, 1L);
        jdbcTemplate().update("""
                INSERT INTO mes_pro_process
                (id, code, name, status, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, 931003L, "PROC-LINK-C", "终检工序", CommonStatusEnum.ENABLE.getStatus(),
                "tester", "tester", false, 1L);
        jdbcTemplate().update("""
                INSERT INTO mes_pro_route_process
                (id, route_id, process_id, sort, next_process_id, link_type, prepare_time, wait_time,
                 color_code, key_flag, check_flag, remark, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, 932001L, currentRoute.getId(), 931001L, 1, 932003L, 2, 15, 5,
                "#00AA00", true, false, "人工维护连接起点", "tester", "tester", false, 1L);
        jdbcTemplate().update("""
                INSERT INTO mes_pro_route_process
                (id, route_id, process_id, sort, key_flag, check_flag, remark, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, 932002L, currentRoute.getId(), 931002L, 2, false, false,
                "人工维护中间工序", "tester", "tester", false, 1L);
        jdbcTemplate().update("""
                INSERT INTO mes_pro_route_process
                (id, route_id, process_id, sort, key_flag, check_flag, remark, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, 932003L, currentRoute.getId(), 931003L, 3, false, true,
                "人工维护连接终点", "tester", "tester", false, 1L);
        jdbcTemplate().update("""
                INSERT INTO mes_pro_route_process_flow_edge
                (route_id, graph_version, source_route_process_id, target_route_process_id, relation_type,
                 sort, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, currentRoute.getId(), 7L, 932001L, 932003L, "MANUAL_SKIP", 9,
                "tester", "tester", false, 1L);
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("连接信息保留批记录");
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "link-source.doc", "link-source-sha", currentRoute.getId(), null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        for (int sourceTableIndex = 2; sourceTableIndex <= 4; sourceTableIndex++) {
            MesProBatchRecordReportDO existing = TestBatchRecordFixtures.metadataReport(
                    (long) sourceTableIndex, "LINK_SCOPE", sourceTableIndex,
                    "route-upgrade-link-existing-" + sourceTableIndex,
                    "EBR_LINK_T" + sourceTableIndex, "既有连接表" + sourceTableIndex, "link-source.doc");
            existing.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
            existing.setBatchRecordName("连接信息保留批记录");
            existing.setBatchRecordDefinitionId(definition.getId());
            existing.setBatchRecordVersionId(currentVersion.getId());
            existing.setFormSlotType(MesProBatchRecordFormSlotType.MAIN.getType());
            reportMapper.insert(existing);
            when(jimuReportGateway.getReportInfo("route-upgrade-link-existing-" + sourceTableIndex))
                    .thenReturn(TestBatchRecordFixtures.reportInfo(
                            "route-upgrade-link-existing-" + sourceTableIndex,
                            "EBR_LINK_T" + sourceTableIndex,
                            "既有连接表" + sourceTableIndex,
                            LocalDateTime.now()));
        }
        MockMultipartFile file = new MockMultipartFile(
                "file", "route-upgrade-link.doc", "application/msword",
                "route-upgrade-link-word-bytes".getBytes(StandardCharsets.UTF_8));

        MesProBatchRecordImportResult result = reportService.recognizeUploadedRoute(
                file, MesProBatchRecordRecognitionRouteKeys.B, "连接信息保留批记录", "UPGRADE",
                currentVersion.getId(), null, List.of("连接信息保留批记录"), true,
                List.of(currentRouteProduct.getId()), List.of(), true,
                currentRoute.getId(), currentRouteVersion.getId(), null);

        Long newStartRouteProcessId = jdbcTemplate().queryForObject("""
                SELECT id FROM mes_pro_route_process
                WHERE route_id = ? AND process_id = ? AND deleted = FALSE
                """, Long.class, currentRoute.getId(), 931001L);
        Long newEndRouteProcessId = jdbcTemplate().queryForObject("""
                SELECT id FROM mes_pro_route_process
                WHERE route_id = ? AND process_id = ? AND deleted = FALSE
                """, Long.class, currentRoute.getId(), 931003L);
        assertEquals(currentRoute.getId(), result.routeId());
        assertNotEquals(932001L, newStartRouteProcessId);
        assertNotEquals(932003L, newEndRouteProcessId);
        assertEquals(1, rawCount("""
                SELECT COUNT(*) FROM mes_pro_route_process
                WHERE id = ? AND next_process_id = ? AND link_type = ? AND prepare_time = ?
                  AND wait_time = ? AND color_code = ? AND key_flag = TRUE AND deleted = FALSE
                """, newStartRouteProcessId, newEndRouteProcessId, 2, 15, 5, "#00AA00"));
        assertEquals(1, rawCount("""
                SELECT COUNT(*) FROM mes_pro_route_process_flow_edge
                WHERE route_id = ? AND source_route_process_id = ? AND target_route_process_id = ?
                  AND graph_version = ? AND relation_type = ? AND sort = ? AND deleted = FALSE
                """, currentRoute.getId(), newStartRouteProcessId, newEndRouteProcessId,
                7L, "MANUAL_SKIP", 9));
    }

    @Test
    void recognizeUploadedRoute_whenSelectedProductIdBelongsToPreflightRoute_acceptsProductIdentity() {
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"),
                TestBatchRecordFixtures.parsedTable(2, "粗洗工序"),
                TestBatchRecordFixtures.parsedTable(3, "精洗工序"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        seedDccProjectCode("球囊扩张压力泵", "BRP-CURRENT-ITEM");
        Long currentItemId = seedProductItem("球囊扩张压力泵", "BRP-CURRENT-ITEM");
        MesProRouteDO staleRoute = MesProRouteDO.builder()
                .code("ROUTE-STALE-ITEM")
                .name("历史批记录路线")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(staleRoute);
        MesProRouteDO currentRoute = MesProRouteDO.builder()
                .code("ROUTE-CURRENT-ITEM")
                .name("球囊扩张压力泵")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(currentRoute);
        MesProRouteVersionDO currentRouteVersion = MesProRouteVersionDO.builder()
                .routeId(currentRoute.getId())
                .versionNo("V1")
                .active(true)
                .routeSnapshotJson("{}")
                .remark("当前预检路线")
                .build();
        routeVersionMapper.insert(currentRouteVersion);
        routeProductMapper.insert(MesProRouteProductDO.builder()
                .routeId(currentRoute.getId())
                .itemId(currentItemId)
                .quantity(1)
                .build());
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("球囊扩张压力泵");
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "current-item.doc", "sha-current-item", staleRoute.getId(), null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        for (int sourceTableIndex = 2; sourceTableIndex <= 3; sourceTableIndex++) {
            MesProBatchRecordReportDO existing = TestBatchRecordFixtures.metadataReport(
                    (long) sourceTableIndex, "CURRENT_ITEM_SCOPE", sourceTableIndex,
                    "current-item-scope-report-" + sourceTableIndex,
                    "EBR_ITEM_SCOPE_T" + sourceTableIndex, "既有表" + sourceTableIndex, "current-item.doc");
            existing.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
            existing.setBatchRecordName("球囊扩张压力泵");
            existing.setBatchRecordDefinitionId(definition.getId());
            existing.setBatchRecordVersionId(currentVersion.getId());
            existing.setFormSlotType(MesProBatchRecordFormSlotType.MAIN.getType());
            reportMapper.insert(existing);
            when(jimuReportGateway.getReportInfo("current-item-scope-report-" + sourceTableIndex))
                    .thenReturn(TestBatchRecordFixtures.reportInfo(
                            "current-item-scope-report-" + sourceTableIndex,
                            "EBR_ITEM_SCOPE_T" + sourceTableIndex,
                            "既有表" + sourceTableIndex,
                            LocalDateTime.now()));
        }
        MockMultipartFile file = new MockMultipartFile(
                "file", "current-item-scope.doc", "application/msword",
                "current-item-scope-word-bytes".getBytes(StandardCharsets.UTF_8));

        MesProBatchRecordImportResult result = reportService.recognizeUploadedRoute(
                file, MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", "UPGRADE",
                currentVersion.getId(), null, List.of("球囊扩张压力泵"), false,
                List.of(currentItemId), List.of(), true,
                currentRoute.getId(), currentRouteVersion.getId(), null);

        assertEquals(currentRoute.getId(), result.routeId());
        assertEquals("ROUTE-CURRENT-ITEM", result.routeCode());
        assertEquals(2, result.routeProcessCount());
        assertEquals(2, result.batchRecordRouteBindingCount());
        assertEquals(currentRoute.getId(), versionMapper.selectById(currentVersion.getId()).getRouteId());
    }

    @Test
    void recognizeUploadedRoute_whenSelectedProductItemIdCollidesWithOtherRouteProductId_acceptsCurrentRouteProduct() {
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"),
                TestBatchRecordFixtures.parsedTable(2, "粗洗工序"),
                TestBatchRecordFixtures.parsedTable(3, "精洗工序"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        seedDccProjectCode("球囊扩张压力泵", "BRP-COLLISION-922198");
        Long selectedProductItemId = 922198L;
        jdbcTemplate().update("""
                INSERT INTO mes_md_item
                (id, code, name, specification, unit_measure_id, item_type_id, status, safe_stock_flag,
                 min_stock, max_stock, high_value, batch_flag, remark, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                selectedProductItemId, "BRP-COLLISION-922198", "球囊扩张压力泵", "测试规格", null, null,
                CommonStatusEnum.ENABLE.getStatus(), false, null, null, false, true,
                "批记录表单测试产品", "tester", "tester", false);
        Long unrelatedItemId = seedProductItem("其它产品", "BRP-COLLISION-OTHER");
        MesProRouteDO otherRoute = MesProRouteDO.builder()
                .code("ROUTE-OTHER-COLLISION")
                .name("其它项目路线")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(otherRoute);
        routeProductMapper.insert(MesProRouteProductDO.builder()
                .id(selectedProductItemId)
                .routeId(otherRoute.getId())
                .itemId(unrelatedItemId)
                .quantity(1)
                .build());
        MesProRouteDO currentRoute = MesProRouteDO.builder()
                .code("ROUTE-CURRENT-COLLISION")
                .name("球囊扩张压力泵")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(currentRoute);
        MesProRouteVersionDO currentRouteVersion = MesProRouteVersionDO.builder()
                .routeId(currentRoute.getId())
                .versionNo("V1")
                .active(true)
                .routeSnapshotJson("{}")
                .remark("当前预检路线")
                .build();
        routeVersionMapper.insert(currentRouteVersion);
        MesProRouteProductDO currentRouteProduct = MesProRouteProductDO.builder()
                .routeId(currentRoute.getId())
                .itemId(selectedProductItemId)
                .quantity(1)
                .build();
        routeProductMapper.insert(currentRouteProduct);
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("球囊扩张压力泵");
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "current-collision.doc", "sha-current-collision", otherRoute.getId(), null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        for (int sourceTableIndex = 2; sourceTableIndex <= 3; sourceTableIndex++) {
            MesProBatchRecordReportDO existing = TestBatchRecordFixtures.metadataReport(
                    (long) sourceTableIndex, "CURRENT_COLLISION_SCOPE", sourceTableIndex,
                    "current-collision-report-" + sourceTableIndex,
                    "EBR_COLLISION_T" + sourceTableIndex, "既有表" + sourceTableIndex, "current-collision.doc");
            existing.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
            existing.setBatchRecordName("球囊扩张压力泵");
            existing.setBatchRecordDefinitionId(definition.getId());
            existing.setBatchRecordVersionId(currentVersion.getId());
            existing.setFormSlotType(MesProBatchRecordFormSlotType.MAIN.getType());
            reportMapper.insert(existing);
            when(jimuReportGateway.getReportInfo("current-collision-report-" + sourceTableIndex))
                    .thenReturn(TestBatchRecordFixtures.reportInfo(
                            "current-collision-report-" + sourceTableIndex,
                            "EBR_COLLISION_T" + sourceTableIndex,
                            "既有表" + sourceTableIndex,
                            LocalDateTime.now()));
        }
        MockMultipartFile file = new MockMultipartFile(
                "file", "current-collision.doc", "application/msword",
                "current-collision-word-bytes".getBytes(StandardCharsets.UTF_8));

        MesProBatchRecordImportResult result = reportService.recognizeUploadedRoute(
                file, MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", "UPGRADE",
                currentVersion.getId(), null, List.of("球囊扩张压力泵"), false,
                List.of(selectedProductItemId), List.of(), true,
                currentRoute.getId(), currentRouteVersion.getId(), null);

        assertEquals(currentRoute.getId(), result.routeId());
        assertEquals("ROUTE-CURRENT-COLLISION", result.routeCode());
        assertEquals(2, result.routeProcessCount());
        assertEquals(2, result.batchRecordRouteBindingCount());
        assertEquals(currentRoute.getId(), versionMapper.selectById(currentVersion.getId()).getRouteId());
    }

    @Test
    void recognizeUploadedRoute_whenSelectedProductIdNotBoundToPreflightRoute_stillFailsFast() {
        seedDccProjectCode("球囊扩张压力泵", "BRP-CURRENT-STRICT");
        Long unrelatedItemId = seedProductItem("其它产品", "BRP-OTHER-STRICT");
        MesProRouteDO currentRoute = MesProRouteDO.builder()
                .code("ROUTE-CURRENT-STRICT")
                .name("球囊扩张压力泵")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(currentRoute);
        MesProRouteVersionDO currentRouteVersion = MesProRouteVersionDO.builder()
                .routeId(currentRoute.getId())
                .versionNo("V1")
                .active(true)
                .routeSnapshotJson("{}")
                .remark("当前预检路线")
                .build();
        routeVersionMapper.insert(currentRouteVersion);
        MockMultipartFile file = new MockMultipartFile(
                "file", "current-strict.doc", "application/msword",
                "current-strict-word-bytes".getBytes(StandardCharsets.UTF_8));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(
                        file, MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", "REBUILD_V1",
                        null, null, List.of("球囊扩张压力泵"), false,
                        List.of(unrelatedItemId), List.of(), true,
                        currentRoute.getId(), currentRouteVersion.getId(), null));

        assertEquals(PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_SCOPE_INVALID.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains(String.valueOf(unrelatedItemId)));
        verifyNoInteractions(jimuReportGateway);
    }

    @Test
    void recognizeUploadedRoute_whenDccProjectNameExistsWithoutWorkOrder_generatesRouteProductBinding() {
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"),
                TestBatchRecordFixtures.parsedTable(2, "粗洗工序"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            return TestBatchRecordFixtures.generatedReport(
                    "dcc-project-route-report-" + saveReq.parsedTable().getSourceTableIndex(),
                    saveReq.reportCode(),
                    saveReq.reportName());
        });
        seedDccProjectCode("DCC球囊扩张压力泵", "DCC-BRP-001");
        MockMultipartFile file = new MockMultipartFile(
                "file", "dcc-project-route-source.doc", "application/msword",
                "dcc-project-route-bytes".getBytes(StandardCharsets.UTF_8));

        MesProBatchRecordImportResult result = reportService.recognizeUploadedRoute(
                file, MesProBatchRecordRecognitionRouteKeys.B, "DCC球囊扩张压力泵", false,
                List.of("DCC球囊扩张压力泵"));

        assertEquals(2, result.importedCount());
        assertEquals(1, result.routeProcessCount());
        assertEquals(1, result.batchRecordRouteBindingCount());
        assertEquals(1, result.boundProductNameCount());
        assertEquals(1, result.boundProductCodeCount());
        assertEquals(List.of(), result.skippedProductNames());
        assertEquals(1, rawCount("""
                SELECT COUNT(*) FROM mes_md_item
                WHERE code = ? AND name = ? AND status = ? AND batch_flag = TRUE
                """, "DCC-BRP-001", "DCC球囊扩张压力泵", CommonStatusEnum.ENABLE.getStatus()));
        assertEquals(1, rawCount("""
                SELECT COUNT(*) FROM mes_pro_route_product rp
                JOIN mes_md_item item ON item.id = rp.item_id
                WHERE rp.route_id = ? AND item.code = ? AND item.name = ?
                """, result.routeId(), "DCC-BRP-001", "DCC球囊扩张压力泵"));
    }

    @Test
    void recognizeUploadedRoute_whenDccProjectProductAlreadyBoundToOtherRoute_rollsBackGeneratedContent() {
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"),
                TestBatchRecordFixtures.parsedTable(2, "粗洗工序"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            return TestBatchRecordFixtures.generatedReport(
                    "route-product-conflict-report-" + saveReq.parsedTable().getSourceTableIndex(),
                    saveReq.reportCode(),
                    saveReq.reportName());
        });
        Long itemId = seedWorkOrderProduct("球囊扩张压力泵", "BRP-CONFLICT");
        seedDccProjectCode("球囊扩张压力泵", "BRP-CONFLICT");
        MesProRouteDO otherRoute = MesProRouteDO.builder()
                .code("ROUTE-OTHER-PRODUCT")
                .name("其它项目路线")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(otherRoute);
        routeProductMapper.insert(MesProRouteProductDO.builder()
                .routeId(otherRoute.getId())
                .itemId(itemId)
                .quantity(1)
                .build());
        MockMultipartFile file = new MockMultipartFile(
                "file", "route-product-conflict.doc", "application/msword",
                "route-product-conflict-bytes".getBytes(StandardCharsets.UTF_8));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(
                        file, MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", false,
                        List.of("球囊扩张压力泵")));

        assertEquals(PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_BIND_FAILED.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("产品已绑定其他工艺路线"));
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_route WHERE id = ?", otherRoute.getId()));
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_route_product WHERE route_id = ? AND item_id = ?",
                otherRoute.getId(), itemId));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_route WHERE code LIKE 'ROUTE-IMPORT-%'"));
        assertEquals(0L, reportMapper.selectCount());
    }

    @Test
    void recognizeUploadedRoute_whenDccProjectNameMissingEvenWithWorkOrder_rollsBackAllGeneratedContent() {
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"),
                TestBatchRecordFixtures.parsedTable(2, "粗洗工序"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            return TestBatchRecordFixtures.generatedReport(
                    "missing-dcc-route-report-" + saveReq.parsedTable().getSourceTableIndex(),
                    saveReq.reportCode(),
                    saveReq.reportName());
        });
        seedWorkOrderProduct("仅MES工单产品", "MES-WO-ONLY-001");
        MockMultipartFile file = new MockMultipartFile(
                "file", "missing-dcc-project.doc", "application/msword",
                "missing-dcc-project-bytes".getBytes(StandardCharsets.UTF_8));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(file, MesProBatchRecordRecognitionRouteKeys.B,
                        "仅MES工单产品", false, List.of("仅MES工单产品")));

        assertEquals(PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_EMPTY.getCode(), exception.getCode());
        assertEquals(0L, reportMapper.selectCount());
        assertNoGeneratedRouteData();
    }

    @Test
    void preflightUploadedRoute_returnsCurrentBatchRecordAndRouteVersionsWithProductOptions() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("球囊扩张压力泵");
        MesProRouteDO route = MesProRouteDO.builder()
                .code("ROUTE-PREFLIGHT-1")
                .name("球囊扩张压力泵")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(route);
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder()
                .routeId(route.getId())
                .versionNo("V3")
                .active(true)
                .routeSnapshotJson("{}")
                .remark("预检测试版本")
                .build();
        routeVersionMapper.insert(routeVersion);
        Long itemId = seedProductItem("球囊扩张压力泵", "BRP-PREFLIGHT");
        routeProductMapper.insert(MesProRouteProductDO.builder()
                .routeId(route.getId())
                .itemId(itemId)
                .quantity(1)
                .build());
        MesProBatchRecordVersionDO version = insertVersion(definition.getId(), "V2.0", "APPROVED",
                null, "preflight.doc", "sha-preflight", route.getId(), null);
        definition.setCurrentVersionId(version.getId());
        definitionMapper.updateById(definition);
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                null, "preflight-main", 1, "preflight-report-1", "EBR_PREFLIGHT_T01", "预检表1", "preflight.doc");
        report.setBatchRecordName("球囊扩张压力泵");
        report.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
        report.setBatchRecordDefinitionId(definition.getId());
        report.setBatchRecordVersionId(version.getId());
        report.setFormSlotType(MesProBatchRecordFormSlotType.MAIN.getType());
        reportMapper.insert(report);
        when(jimuReportGateway.getReportInfo("preflight-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "preflight-report-1", "EBR_PREFLIGHT_T01", "预检表1", LocalDateTime.now()));

        MesProBatchRecordImportPreflightResult result = reportService.preflightUploadedRoute(
                MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", List.of("球囊扩张压力泵"));

        assertEquals(definition.getId(), result.batchRecordDefinitionId());
        assertEquals(version.getId(), result.currentBatchRecordVersionId());
        assertEquals("V2.0", result.currentBatchRecordVersionNo());
        assertEquals("APPROVED", result.currentBatchRecordVersionStatus());
        assertTrue(result.currentBatchRecordHasMainReports());
        assertEquals(route.getId(), result.currentRouteId());
        assertEquals("ROUTE-PREFLIGHT-1", result.currentRouteCode());
        assertEquals(routeVersion.getId(), result.currentRouteVersionId());
        assertEquals("V3", result.currentRouteVersionNo());
        assertEquals(List.of("UPGRADE"), result.allowedActions());
        assertEquals("UPGRADE", result.recommendedAction());
        assertEquals(version.getId(), result.latestBatchRecordVersionId());
        assertEquals("V2.0", result.latestBatchRecordVersionNo());
        assertEquals("APPROVED", result.latestBatchRecordVersionStatus());
        assertEquals("V3.0", result.nextVersionNo());
        assertEquals(1, result.routeProductOptions().size());
        assertTrue(result.routeProductOptions().get(0).existing());
        assertEquals("球囊扩张压力泵", result.routeProductOptions().get(0).productName());
        assertEquals("V3", result.routeProductOptions().get(0).routeVersionNo());
    }

    @Test
    void preflightUploadedRoute_whenDuplicateRoutesExist_blocksAndListsRouteCodes() {
        MesProRouteDO firstRoute = MesProRouteDO.builder()
                .code("RT-DUP-001")
                .name("球囊扩张压力泵")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(firstRoute);
        MesProRouteDO secondRoute = MesProRouteDO.builder()
                .code("RT-DUP-002")
                .name("球囊扩张压力泵")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(secondRoute);
        routeVersionMapper.insert(MesProRouteVersionDO.builder()
                .routeId(firstRoute.getId())
                .versionNo("V1")
                .active(true)
                .routeSnapshotJson("{}")
                .remark("重复路线预检-1")
                .build());
        routeVersionMapper.insert(MesProRouteVersionDO.builder()
                .routeId(secondRoute.getId())
                .versionNo("V1")
                .active(true)
                .routeSnapshotJson("{}")
                .remark("重复路线预检-2")
                .build());

        MesProBatchRecordImportPreflightResult result = reportService.preflightUploadedRoute(
                MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", List.of("球囊扩张压力泵"));

        assertEquals("DUPLICATE_BLOCKED", result.routeGovernanceStatus());
        assertFalse(result.routeUpgradeRequired());
        assertEquals(2, result.duplicateRoutes().size());
        assertEquals(List.of("RT-DUP-001", "RT-DUP-002"),
                result.duplicateRoutes().stream()
                        .map(MesProBatchRecordImportPreflightResult.DuplicateRoute::routeCode)
                        .toList());
        assertEquals(List.of(), result.allowedActions());
        assertNull(result.recommendedAction());
        assertEquals(List.of(), result.routeProductOptions());
        assertNull(result.currentRouteId());
    }

    @Test
    void recognizeUploadedRoute_whenDuplicateRoutesExist_failsFastBeforeRecognizerAndWritesNothingNew() {
        seedDccProjectCode("球囊扩张压力泵", "BRP-DUP-ROUTE");
        MesProRouteDO firstRoute = MesProRouteDO.builder()
                .code("RT-DUP-WRITE-001")
                .name("球囊扩张压力泵")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(firstRoute);
        MesProRouteDO secondRoute = MesProRouteDO.builder()
                .code("RT-DUP-WRITE-002")
                .name("球囊扩张压力泵")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(secondRoute);
        MockMultipartFile file = new MockMultipartFile(
                "file", "duplicate-route.doc", "application/msword",
                "duplicate-route-word-bytes".getBytes(StandardCharsets.UTF_8));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(
                        file, MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", "REBUILD_V1",
                        null, List.of("球囊扩张压力泵"), true, List.of(), List.of("球囊扩张压力泵")));

        assertEquals(PRO_BATCH_RECORD_REPORT_ROUTE_DUPLICATE.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("RT-DUP-WRITE-001"));
        assertTrue(exception.getMessage().contains("RT-DUP-WRITE-002"));
        assertEquals(2, rawCount("SELECT COUNT(*) FROM mes_pro_route WHERE name = ?", "球囊扩张压力泵"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_definition WHERE batch_record_name = ?",
                "球囊扩张压力泵"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_report WHERE batch_record_name = ?",
                "球囊扩张压力泵"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_route_process"));
        verify(routeRecognizer, never()).recognize(any(), any(), any());
        verify(jimuReportGateway, never()).saveOrUpdateReport(any());
    }

    @Test
    void dccProjectGovernanceStatus_aggregatesRouteMainRecordAndAuxiliarySlotUniquenessByProjectName() {
        String projectName = "治理状态项目";
        seedDccProjectCode(projectName, "GOV-001");
        MesProRouteDO firstRoute = MesProRouteDO.builder()
                .code("RT-GOV-001")
                .name(projectName)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(firstRoute);
        MesProRouteDO secondRoute = MesProRouteDO.builder()
                .code("RT-GOV-002")
                .name(projectName)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(secondRoute);
        insertVersionedDefinition(projectName);
        insertDefinition(projectName, MesProBatchRecordRecognitionRouteKeys.E);
        insertAuxiliarySlotReport(projectName, MesProBatchRecordFormSlotType.LOSS_REPORT, "LOSS-GOV-001");
        insertAuxiliarySlotReport(projectName, MesProBatchRecordFormSlotType.PARAMETER_RECORD, "PARAM-GOV-001");
        insertAuxiliarySlotReport(projectName, MesProBatchRecordFormSlotType.PARAMETER_RECORD, "PARAM-GOV-002");

        MesProDccProjectGovernanceStatus status =
                dccProjectGovernanceService.getStatus(List.of(projectName)).get(0);

        assertEquals(projectName, status.projectName());
        assertEquals(1, status.dccProjectCodeCount());
        assertEquals("DUPLICATE", status.routeStatus());
        assertEquals(List.of("RT-GOV-001", "RT-GOV-002"), status.routeCodes());
        assertEquals("DUPLICATE", status.mainBatchRecordStatus());
        assertEquals(2L, status.mainBatchRecordCount());
        assertEquals("OK", status.lossReportStatus());
        assertEquals(List.of("LOSS-GOV-001"), status.lossReportCodes());
        assertEquals("MISSING", status.processInspectionStatus());
        assertEquals("DUPLICATE", status.parameterRecordStatus());
        assertEquals(List.of("PARAM-GOV-001", "PARAM-GOV-002"), status.parameterRecordCodes());
        assertTrue(status.blockerMessages().stream().anyMatch(message -> message.contains("工艺路线重复 2 份")));
        assertTrue(status.blockerMessages().stream().anyMatch(message -> message.contains("主批记录重复 2 份")));
        assertTrue(status.blockerMessages().stream().anyMatch(message -> message.contains("参数记录表重复 2 份")));
    }

    @Test
    void preflightUploadedRoute_whenBatchRecordNameDiffersFromDccProjectName_rejectsBeforeLookup() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.preflightUploadedRoute(
                        MesProBatchRecordRecognitionRouteKeys.B,
                        "错误批记录名",
                        List.of("球囊扩张压力泵")));

        assertEquals(PRO_BATCH_RECORD_REPORT_DCC_PROJECT_NAME_REQUIRED.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("一次只能导入一个 DCC 项目"));
    }

    @Test
    void recognizeUploadedRoute_whenBatchRecordNameDiffersFromDccProjectName_failsFastBeforeRecognizer() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "dcc-project.doc", "application/msword",
                "dcc-project-bytes".getBytes(StandardCharsets.UTF_8));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(
                        file, MesProBatchRecordRecognitionRouteKeys.B, "错误批记录名", "REBUILD_V1",
                        null, List.of("球囊扩张压力泵"), false, List.of(), List.of("球囊扩张压力泵")));

        assertEquals(PRO_BATCH_RECORD_REPORT_DCC_PROJECT_NAME_REQUIRED.getCode(), exception.getCode());
        assertNoGeneratedRouteData();
        verify(routeRecognizer, never()).recognize(any(), any(), any());
        verify(jimuReportGateway, never()).saveOrUpdateReport(any());
    }

    @Test
    void preflightUploadedRoute_whenPendingApprovalVersionAlreadyExists_locksImportActions() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("球囊扩张压力泵");
        MesProRouteDO currentRoute = MesProRouteDO.builder()
                .code("ROUTE-CURRENT-V1")
                .name("球囊扩张压力泵")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(currentRoute);
        MesProRouteVersionDO currentRouteVersion = MesProRouteVersionDO.builder()
                .routeId(currentRoute.getId())
                .versionNo("V1")
                .active(true)
                .routeSnapshotJson("{}")
                .remark("当前生效路线")
                .build();
        routeVersionMapper.insert(currentRouteVersion);
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "current.doc", "sha-current", currentRoute.getId(), null);
        MesProBatchRecordVersionDO latestPendingVersion = insertVersion(definition.getId(), "V2.0", "PENDING_APPROVAL",
                currentVersion.getId(), "pending.doc", "sha-pending", 20002L, currentRoute.getId());
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        MesProBatchRecordReportDO currentReport = TestBatchRecordFixtures.metadataReport(
                null, "latest-current", 1, "latest-current-report-1", "EBR_LATEST_T01", "当前表1", "current.doc");
        currentReport.setBatchRecordName("球囊扩张压力泵");
        currentReport.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
        currentReport.setBatchRecordDefinitionId(definition.getId());
        currentReport.setBatchRecordVersionId(currentVersion.getId());
        currentReport.setFormSlotType(MesProBatchRecordFormSlotType.MAIN.getType());
        reportMapper.insert(currentReport);
        when(jimuReportGateway.getReportInfo("latest-current-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "latest-current-report-1", "EBR_LATEST_T01", "当前表1", LocalDateTime.now()));

        MesProBatchRecordImportPreflightResult result = reportService.preflightUploadedRoute(
                MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", List.of("球囊扩张压力泵"));

        assertEquals(currentVersion.getId(), result.currentBatchRecordVersionId());
        assertEquals("V1.0", result.currentBatchRecordVersionNo());
        assertEquals(latestPendingVersion.getId(), result.latestBatchRecordVersionId());
        assertEquals("V2.0", result.latestBatchRecordVersionNo());
        assertEquals("PENDING_APPROVAL", result.latestBatchRecordVersionStatus());
        assertEquals(List.of(), result.allowedActions());
        assertNull(result.recommendedAction());
        assertEquals("V3.0", result.nextVersionNo());
    }

    @Test
    void preflightUploadedRoute_ignoresStaleVersionWhenNoCurrentMainReportOrProductBindingExists() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("无表单无产线批记录");
        MesProRouteDO route = MesProRouteDO.builder()
                .code("ROUTE-STALE-PREFLIGHT")
                .name("历史非同名路线")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        routeMapper.insert(route);
        MesProRouteVersionDO routeVersion = MesProRouteVersionDO.builder()
                .routeId(route.getId())
                .versionNo("V2")
                .active(true)
                .routeSnapshotJson("{}")
                .remark("陈旧预检测试版本")
                .build();
        routeVersionMapper.insert(routeVersion);
        Long unrelatedItemId = seedProductItem("历史产品", "BRP-STALE-OLD");
        routeProductMapper.insert(MesProRouteProductDO.builder()
                .routeId(route.getId())
                .itemId(unrelatedItemId)
                .quantity(1)
                .build());
        MesProBatchRecordVersionDO version = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "stale-preflight.doc", "sha-stale-preflight", route.getId(), null);
        definition.setCurrentVersionId(version.getId());
        definitionMapper.updateById(definition);

        MesProBatchRecordImportPreflightResult result = reportService.preflightUploadedRoute(
                MesProBatchRecordRecognitionRouteKeys.B, "无表单无产线批记录", List.of("无表单无产线批记录"));

        assertEquals(definition.getId(), result.batchRecordDefinitionId());
        assertNull(result.currentBatchRecordVersionId());
        assertNull(result.currentBatchRecordVersionNo());
        assertFalse(result.currentBatchRecordHasMainReports());
        assertNull(result.currentRouteId());
        assertNull(result.currentRouteVersionId());
        assertNull(result.currentRouteVersionNo());
        assertEquals(List.of("REBUILD_V1"), result.allowedActions());
        assertEquals("REBUILD_V1", result.recommendedAction());
        assertEquals("V1.0", result.nextVersionNo());
        assertEquals(1, result.routeProductOptions().size());
        assertFalse(result.routeProductOptions().get(0).existing());
        assertEquals("无表单无产线批记录", result.routeProductOptions().get(0).productName());
        assertNull(result.routeProductOptions().get(0).routeVersionNo());
    }

    @Test
    void preflightUploadedRoute_keepsStaleVersionWhenBusinessReferenceExists() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("无表单有执行批记录");
        MesProBatchRecordVersionDO version = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "stale-execution.doc", "sha-stale-execution", null, null);
        definition.setCurrentVersionId(version.getId());
        definitionMapper.updateById(definition);
        insertBatchRecordExecution(definition.getId(), version.getId(), "EXEC-STALE-PREFLIGHT");

        MesProBatchRecordImportPreflightResult result = reportService.preflightUploadedRoute(
                MesProBatchRecordRecognitionRouteKeys.B, "无表单有执行批记录", List.of("无表单有执行批记录"));

        assertEquals(definition.getId(), result.batchRecordDefinitionId());
        assertEquals(version.getId(), result.currentBatchRecordVersionId());
        assertEquals("V1.0", result.currentBatchRecordVersionNo());
        assertEquals("APPROVED", result.currentBatchRecordVersionStatus());
        assertFalse(result.currentBatchRecordHasMainReports());
        assertNull(result.currentRouteId());
        assertNull(result.currentRouteVersionNo());
        assertTrue(result.hasHistoricalReferences());
        assertEquals(List.of("UPGRADE"), result.allowedActions());
        assertEquals("UPGRADE", result.recommendedAction());
        assertEquals("V2.0", result.nextVersionNo());
        assertEquals(1, result.referenceBlockers().size());
        assertEquals("存在批记录执行", result.referenceBlockers().get(0).referenceName());
        assertEquals(1, result.routeProductOptions().size());
        assertFalse(result.routeProductOptions().get(0).existing());
        assertNull(result.routeProductOptions().get(0).routeVersionNo());
    }

    @Test
    void recognizeUploadedRoute_rejectsWhenNoRebuildScopeSelected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty-scope.doc", "application/msword",
                "empty-scope-bytes".getBytes(StandardCharsets.UTF_8));

        assertServiceException(() -> reportService.recognizeUploadedRoute(
                        file, MesProBatchRecordRecognitionRouteKeys.B, "空范围批记录", false,
                        List.of("空范围批记录"), false, List.of(), List.of()),
                PRO_BATCH_RECORD_REPORT_IMPORT_SCOPE_EMPTY);
        verifyNoInteractions(jimuReportGateway);
    }

    @Test
    void recognizeUploadedRoute_whenProcessNameAlreadyExists_reusesExistingProcessWithoutCreatingDuplicate() {
        jdbcTemplate().update("""
                INSERT INTO mes_pro_process
                (id, code, name, product_name, status, remark, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                910001L, "LEGACY-ROUGH-WASH", "粗洗工序", null,
                CommonStatusEnum.DISABLE.getStatus(), "历史工序", "tester", "tester", false);
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"),
                TestBatchRecordFixtures.parsedTable(2, "粗洗工序生产记录"),
                TestBatchRecordFixtures.parsedTable(3, "精洗工序"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        AtomicInteger counter = new AtomicInteger();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            int index = counter.incrementAndGet();
            return TestBatchRecordFixtures.generatedReport(
                    "reuse-existing-process-report-" + index,
                    saveReq.reportCode(),
                    saveReq.reportName());
        });
        MockMultipartFile file = new MockMultipartFile(
                "file", "route-source.doc", "application/msword",
                "uploaded-word-route-bytes".getBytes(StandardCharsets.UTF_8));
        seedWorkOrderProduct("球囊扩张压力泵", "BRP-001");
        seedDccProjectCode("球囊扩张压力泵", "BRP-001");

        MesProBatchRecordImportResult result = reportService.recognizeUploadedRoute(
                file, MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", false, List.of("球囊扩张压力泵"));

        assertEquals(2, result.routeProcessCount());
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_process WHERE name = ?", "粗洗工序"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_process WHERE name = ?", "粗洗工序生产记录"));
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_process WHERE name = ?", "精洗工序"));
        assertEquals(1, rawCount("""
                SELECT COUNT(*) FROM mes_pro_route_process
                WHERE route_id = ? AND process_id = ?
                """, result.routeId(), 910001L));
        assertEquals(0, rawCount("""
                SELECT COUNT(*) FROM mes_pro_process
                WHERE name = ? AND code LIKE ?
                """, "粗洗工序", "EBR\\_%"));
        assertEquals(0, rawCount("""
                SELECT COUNT(*) FROM mes_pro_process
                WHERE id = ? AND status = ?
                """, 910001L, CommonStatusEnum.ENABLE.getStatus()));
    }

    @Test
    void recognizeUploadedRoute_whenProductInfoMissing_rollsBackAllGeneratedContent() {
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "粗洗工序"),
                TestBatchRecordFixtures.parsedTable(2, "精洗工序"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        MockMultipartFile file = new MockMultipartFile(
                "file", "missing-product-info.doc", "application/msword",
                "missing-product-info-bytes".getBytes(StandardCharsets.UTF_8));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(file, MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", false, List.of("球囊扩张压力泵")));

        assertEquals(PRO_BATCH_RECORD_REPORT_PRODUCT_INFO_MISSING.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("已中止导入并回滚"));
        assertEquals(0L, reportMapper.selectCount());
        assertNoGeneratedRouteData();
        verify(jimuReportGateway, never()).saveOrUpdateReport(any());
    }

    @Test
    void recognizeUploadedRoute_whenProductInfoNotFirst_rollsBackAllGeneratedContent() {
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "粗洗工序"),
                TestBatchRecordFixtures.parsedTable(2, "产品信息"),
                TestBatchRecordFixtures.parsedTable(3, "精洗工序"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        MockMultipartFile file = new MockMultipartFile(
                "file", "product-info-not-first.doc", "application/msword",
                "product-info-not-first-bytes".getBytes(StandardCharsets.UTF_8));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(file, MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", false, List.of("球囊扩张压力泵")));

        assertEquals(PRO_BATCH_RECORD_REPORT_PRODUCT_INFO_NOT_FIRST.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("已中止导入并回滚"));
        assertEquals(0L, reportMapper.selectCount());
        assertNoGeneratedRouteData();
        verify(jimuReportGateway, never()).saveOrUpdateReport(any());
    }

    @Test
    void recognizeUploadedRoute_whenOnlyProductInfo_rollsBackAllGeneratedContent() {
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        MockMultipartFile file = new MockMultipartFile(
                "file", "only-product-info.doc", "application/msword",
                "only-product-info-bytes".getBytes(StandardCharsets.UTF_8));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(file, MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", false, List.of("球囊扩张压力泵")));

        assertEquals(PRO_BATCH_RECORD_REPORT_PROCESS_EMPTY.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("已中止导入并回滚"));
        assertEquals(0L, reportMapper.selectCount());
        assertNoGeneratedRouteData();
        verify(jimuReportGateway, never()).saveOrUpdateReport(any());
    }

    @Test
    void recognizeUploadedRoute_whenSameNameAndRouteExistsWithoutUpgrade_failsFastBeforeGatewaySave() {
        MesProBatchRecordReportDO existing = TestBatchRecordFixtures.metadataReport(
                31L, "BATCH_OLD", 1, "existing-report-1", "EBR_B_T01", "既有表1", "old.doc");
        existing.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
        existing.setBatchRecordName("球囊扩张压力泵");
        reportMapper.insert(existing);
        MockMultipartFile file = new MockMultipartFile(
                "file", "user-selected.doc", "application/msword",
                "uploaded-word-bytes".getBytes(StandardCharsets.UTF_8));

        assertServiceException(() -> reportService.recognizeUploadedRoute(
                        file, MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", false, List.of("球囊扩张压力泵")),
                PRO_BATCH_RECORD_REPORT_BATCH_NAME_EXISTS, "球囊扩张压力泵");

        assertEquals(1L, reportMapper.selectCount());
        verify(jimuReportGateway, never()).saveOrUpdateReport(any());
    }

    @Test
    void recognizeUploadedRoute_whenUpgradeMissingExpectedSourceVersion_failsFastBeforeGatewaySave() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("升版缺少预检源版本");
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "old.doc", "old-sha", 10001L, null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        MesProBatchRecordReportDO existing = TestBatchRecordFixtures.metadataReport(
                41L, "BATCH_OLD", 1, "existing-missing-source-report-1",
                "EBR_B_T01", "既有表1", "old.doc");
        existing.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
        existing.setBatchRecordName("升版缺少预检源版本");
        existing.setBatchRecordDefinitionId(definition.getId());
        existing.setBatchRecordVersionId(currentVersion.getId());
        existing.setFormSlotType(MesProBatchRecordFormSlotType.MAIN.getType());
        reportMapper.insert(existing);
        when(jimuReportGateway.getReportInfo("existing-missing-source-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "existing-missing-source-report-1", "EBR_B_T01", "既有表1", LocalDateTime.now()));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(
                        new MockMultipartFile("file", "missing-source.doc", "application/msword",
                                "missing-source".getBytes(StandardCharsets.UTF_8)),
                        MesProBatchRecordRecognitionRouteKeys.B, "升版缺少预检源版本", "UPGRADE", null,
                        List.of("升版缺少预检源版本"), true, List.of(), List.of()));

        assertEquals(PRO_BATCH_RECORD_REPORT_VERSION_UPGRADE_SOURCE_REQUIRED.getCode(), exception.getCode());
        assertEquals(1L, reportMapper.selectCount());
        verify(jimuReportGateway, never()).saveOrUpdateReport(any());
    }

    @Test
    void recognizeUploadedRoute_whenCurrentVersionChangedAfterPreflight_failsFastBeforeGatewaySave() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("升版当前版本变化");
        MesProBatchRecordVersionDO preflightVersion = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "old-v1.doc", "old-v1-sha", 10001L, null);
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V2.0", "APPROVED",
                preflightVersion.getId(), "old-v2.doc", "old-v2-sha", 10002L, 10001L);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        MesProBatchRecordReportDO existing = TestBatchRecordFixtures.metadataReport(
                43L, "BATCH_CHANGED", 1, "current-changed-report-1",
                "EBR_B_T01", "既有表1", "old-v2.doc");
        existing.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
        existing.setBatchRecordName("升版当前版本变化");
        existing.setBatchRecordDefinitionId(definition.getId());
        existing.setBatchRecordVersionId(currentVersion.getId());
        existing.setFormSlotType(MesProBatchRecordFormSlotType.MAIN.getType());
        reportMapper.insert(existing);
        when(jimuReportGateway.getReportInfo("current-changed-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "current-changed-report-1", "EBR_B_T01", "既有表1", LocalDateTime.now()));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(
                        new MockMultipartFile("file", "stale-source.doc", "application/msword",
                                "stale-source".getBytes(StandardCharsets.UTF_8)),
                        MesProBatchRecordRecognitionRouteKeys.B, "升版当前版本变化", "UPGRADE",
                        preflightVersion.getId(), List.of("升版当前版本变化"), true, List.of(), List.of()));

        assertEquals(PRO_BATCH_RECORD_REPORT_VERSION_CURRENT_CHANGED.getCode(), exception.getCode());
        assertEquals(1L, reportMapper.selectCount());
        verify(jimuReportGateway, never()).saveOrUpdateReport(any());
    }

    @Test
    void recognizeUploadedRoute_whenRebuildV1ActionNotAllowed_failsFastBeforeGatewaySave() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("禁止重建动作批记录");
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "old.doc", "old-sha", 10002L, null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        MesProBatchRecordReportDO existing = TestBatchRecordFixtures.metadataReport(
                42L, "BATCH_OLD", 1, "existing-action-report-1",
                "EBR_B_T01", "既有表1", "old.doc");
        existing.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
        existing.setBatchRecordName("禁止重建动作批记录");
        existing.setBatchRecordDefinitionId(definition.getId());
        existing.setBatchRecordVersionId(currentVersion.getId());
        existing.setFormSlotType(MesProBatchRecordFormSlotType.MAIN.getType());
        reportMapper.insert(existing);
        when(jimuReportGateway.getReportInfo("existing-action-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "existing-action-report-1", "EBR_B_T01", "既有表1", LocalDateTime.now()));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(
                        new MockMultipartFile("file", "illegal-rebuild.doc", "application/msword",
                                "illegal-rebuild".getBytes(StandardCharsets.UTF_8)),
                        MesProBatchRecordRecognitionRouteKeys.B, "禁止重建动作批记录", "REBUILD_V1", null,
                        List.of("禁止重建动作批记录"), false, List.of(), List.of("禁止重建动作批记录")));

        assertEquals(PRO_BATCH_RECORD_REPORT_IMPORT_ACTION_NOT_ALLOWED.getCode(), exception.getCode());
        assertEquals(1L, reportMapper.selectCount());
        verify(jimuReportGateway, never()).saveOrUpdateReport(any());
    }

    @Test
    void recognizeUploadedRoute_whenSameNameAndRouteExistsWithUpgrade_createsPendingVersionSnapshotWithoutMutatingCurrent() {
        List<MesProBatchRecordParsedTable> parsedTables = uploadedRouteParsedTables(15, "Upgrade Table ");
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        MesProBatchRecordDefinitionDO definition = MesProBatchRecordDefinitionDO.builder()
                .batchRecordName("球囊扩张压力泵")
                .routeKey(MesProBatchRecordRecognitionRouteKeys.B)
                .build();
        definitionMapper.insert(definition);
        MesProBatchRecordVersionDO currentVersion = MesProBatchRecordVersionDO.builder()
                .definitionId(definition.getId())
                .versionNo("V1.0")
                .status("APPROVED")
                .sourceFileName("old.doc")
                .sourceFileSha256("old-sha256")
                .routeId(10001L)
                .build();
        versionMapper.insert(currentVersion);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        for (int index = 1; index <= 15; index++) {
            MesProBatchRecordReportDO existing = TestBatchRecordFixtures.metadataReport(
                    (long) index, "BATCH_OLD", index, "existing-report-" + index,
                    "EBR_B_T" + String.format("%02d", index), "既有表" + index, "old.doc");
            existing.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
            existing.setBatchRecordName("球囊扩张压力泵");
            existing.setBatchRecordDefinitionId(definition.getId());
            existing.setBatchRecordVersionId(currentVersion.getId());
            reportMapper.insert(existing);
        }
        jdbcTemplate().update("""
                INSERT INTO mes_pro_edhr_process_form_permission_rule
                (route_process_id, batch_record_report_id, batch_record_definition_id, batch_record_version_id,
                 rule_type, signature_cell_key, signature_role, candidate_source_type, candidate_source_ids,
                 completion_policy, due_minutes, enabled, remark, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                0L, "existing-report-1", definition.getId(), currentVersion.getId(), "FILL", "", "",
                "USERS", "101,102", "ANY_ONE", 0, true, "source filler rule",
                "tester", "tester", false, 1L);
        when(jimuReportGateway.getReportInfo("existing-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "existing-report-1", "EBR_B_T01", "既有表1", LocalDateTime.now()));
        AtomicInteger newReportCounter = new AtomicInteger();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            return TestBatchRecordFixtures.generatedReport(
                    saveReq.existingReportId() == null ? "new-version-report-" + newReportCounter.incrementAndGet()
                            : saveReq.existingReportId(),
                    saveReq.reportCode(), saveReq.reportName());
        });
        MockMultipartFile file = new MockMultipartFile(
                "file", "new.doc", "application/msword",
                "new-word-bytes".getBytes(StandardCharsets.UTF_8));
        seedWorkOrderProduct("球囊扩张压力泵", "BRP-001");
        seedDccProjectCode("球囊扩张压力泵", "BRP-001");

        MesProBatchRecordImportResult result = reportService.recognizeUploadedRoute(
                file, MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", "UPGRADE",
                currentVersion.getId(), List.of("球囊扩张压力泵"), true, List.of(), List.of("球囊扩张压力泵"));

        assertEquals(0, result.updatedCount());
        assertEquals(15, result.createdCount());
        assertEquals(30L, reportMapper.selectCount());
        assertEquals(currentVersion.getId(), definitionMapper.selectById(definition.getId()).getCurrentVersionId());
        assertNotNull(result.batchRecordDefinitionId());
        assertNotNull(result.batchRecordVersionId());
        assertEquals(definition.getId(), result.batchRecordDefinitionId());
        assertEquals(currentVersion.getId(), result.sourceBatchRecordVersionId());
        MesProBatchRecordVersionDO pendingVersion = versionMapper.selectById(result.batchRecordVersionId());
        assertEquals("V2.0", pendingVersion.getVersionNo());
        assertEquals("PRECHECK_FAILED", pendingVersion.getStatus());
        assertEquals(currentVersion.getId(), pendingVersion.getSourceVersionId());
        assertNotNull(pendingVersion.getRouteId());
        MesProBatchRecordReportDO firstOld = reportMapper.selectByReportId("existing-report-1");
        assertEquals("old.doc", firstOld.getSourceFileName());
        assertEquals(currentVersion.getId(), firstOld.getBatchRecordVersionId());
        MesProBatchRecordReportDO firstNew = reportMapper.selectByReportId("new-version-report-1");
        assertEquals("new.doc", firstNew.getSourceFileName());
        assertEquals(pendingVersion.getId(), firstNew.getBatchRecordVersionId());
        assertEquals(1, rawCount("""
                SELECT COUNT(*) FROM mes_pro_edhr_process_form_permission_rule
                WHERE route_process_id = 0
                  AND batch_record_report_id = ?
                  AND batch_record_definition_id = ?
                  AND batch_record_version_id = ?
                  AND rule_type = 'FILL'
                  AND candidate_source_type = 'USERS'
                  AND candidate_source_ids = '101,102'
                  AND completion_policy = 'ANY_ONE'
                  AND due_minutes = 0
                  AND enabled = TRUE
                """, "new-version-report-1", definition.getId(), pendingVersion.getId()));
        assertTrue(firstNew.getSampleKey().contains("BATCH_VERSION_"),
                "version snapshot sample_key must be scoped by version to avoid real MySQL unique-key collision");
        assertTrue(firstNew.getSampleKey().contains("_TN"),
                "version snapshot sample_key must keep tenant scope");
        assertTrue(rawCount("""
                SELECT COUNT(DISTINCT sample_key) FROM mes_pro_batch_record_report
                WHERE batch_record_definition_id = ?
                """, definition.getId()) >= 2,
                "old and new version reports must not share the same sample_key under the unique index");
        assertEquals(15, rawCount("""
                SELECT COUNT(*) FROM mes_pro_batch_record_report
                WHERE batch_record_definition_id = ? AND batch_record_version_id = ?
                """, definition.getId(), pendingVersion.getId()));
        assertEquals(result.batchRecordRouteBindingCount(), rawCount("""
                SELECT COUNT(*) FROM mes_pro_route_flow_process_batch_record
                WHERE batch_record_definition_id = ? AND batch_record_version_id = ?
                """, definition.getId(), pendingVersion.getId()));
        ArgumentCaptor<MesProBatchRecordJimuReportSaveReq> saveCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordJimuReportSaveReq.class);
        verify(jimuReportGateway, times(15)).saveOrUpdateReport(saveCaptor.capture());
        assertTrue(saveCaptor.getAllValues().stream().allMatch(req -> req.existingReportId() == null));
        assertTrue(saveCaptor.getAllValues().stream().allMatch(req ->
                req.reportCode().contains("_V" + pendingVersion.getId() + "_")),
                "version snapshot reportCode must include version id to isolate same-hash Jimu reports");
        assertTrue(saveCaptor.getAllValues().stream().noneMatch(req ->
                req.reportCode().equals("EBR_B_DOC_"
                        + sha256("new-word-bytes".getBytes(StandardCharsets.UTF_8)).substring(0, 8) + "_T01")));
    }

    @Test
    void recognizeUploadedRoute_whenUpgrade_createsStructuredPhaseTwoMigrationDiffAndBlocksUntilConfirmed() {
        seedWorkOrderProduct("阶段二结构化迁移批记录", "PHASE2-DIFF");
        seedDccProjectCode("阶段二结构化迁移批记录", "PHASE2-DIFF");
        List<MesProBatchRecordParsedTable> parsedTables = uploadedRouteParsedTables(4, "迁移工序");
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");

        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("阶段二结构化迁移批记录");
        MesProBatchRecordVersionDO approvedVersion = insertVersion(definition.getId(), "V1.0", "APPROVED", null,
                "phase2-source.doc", "phase-two-source-sha", 930001L, null);
        definition.setCurrentVersionId(approvedVersion.getId());
        definitionMapper.updateById(definition);
        for (int index = 1; index <= 2; index++) {
            MesProBatchRecordReportDO existing = TestBatchRecordFixtures.metadataReport(
                    (long) index, "phase2-existing-sample-" + index, index,
                    "phase2-existing-report-" + index,
                    "EBR_TN122_B_DOC_phase2sha_V" + approvedVersion.getId() + "_T" + String.format("%02d", index),
                    "阶段二既有表" + index, "phase2-source.doc");
            existing.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
            existing.setBatchRecordName("阶段二结构化迁移批记录");
            existing.setBatchRecordDefinitionId(definition.getId());
            existing.setBatchRecordVersionId(approvedVersion.getId());
            reportMapper.insert(existing);
        }
        when(jimuReportGateway.getReportInfo("phase2-existing-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "phase2-existing-report-1", "EBR_TN122_B_DOC_phase2sha_V"
                                + approvedVersion.getId() + "_T01", "阶段二既有表1", LocalDateTime.now()));
        AtomicInteger newReportCounter = new AtomicInteger();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            return TestBatchRecordFixtures.generatedReport(
                    saveReq.existingReportId() == null ? "phase2-diff-report-" + newReportCounter.incrementAndGet()
                            : saveReq.existingReportId(),
                    saveReq.reportCode(), saveReq.reportName());
        });

        MockMultipartFile uploaded = new MockMultipartFile("file", "phase2-diff.doc",
                "application/msword", "phase-two-target".getBytes(StandardCharsets.UTF_8));
        MesProBatchRecordImportResult result = reportService.recognizeUploadedRoute(
                uploaded, MesProBatchRecordRecognitionRouteKeys.B, definition.getBatchRecordName(), "UPGRADE",
                approvedVersion.getId(), List.of("阶段二结构化迁移批记录"), true, List.of(), List.of("阶段二结构化迁移批记录"));
        assertEquals("PRECHECK_FAILED", result.versionStatus(),
                "存在 CONFIRM_REQUIRED 迁移项时新版本必须保持预检失败，防止绕过授权确认");

        List<MesProBatchRecordVersionMigrationItemDO> items =
                migrationItemMapper.selectListByVersionId(result.batchRecordVersionId());
        assertEquals(6, items.size(), "阶段二升版必须生成六类结构化迁移差异");
        assertTrue(items.stream().map(MesProBatchRecordVersionMigrationItemDO::getDiffGroup)
                        .toList()
                        .containsAll(List.of("TABLE", "PROCESS", "FIELD", "SIGNATURE_CELL",
                                "ATTACHMENT_RULE", "CELL_RULE")),
                "结构化迁移差异必须覆盖六类分组");
        assertTrue(items.stream().allMatch(item -> item.getSourceLogicalKey() != null
                        && item.getTargetLogicalKey() != null
                        && item.getMatchConfidence() != null
                        && item.getMatchEvidenceJson() != null
                        && item.getDiffType() != null
                        && item.getRuleType() != null
                        && item.getBusinessOwnerType() != null),
                "每个迁移项都必须包含可审计结构化证据");
        assertEquals(1, items.stream().filter(item -> "CONFIRM_REQUIRED".equals(item.getRiskLevel())).count(),
                "阶段二真实升版必须生成需人工确认项");
        assertTrue(migrationItemMapper.countBlockingItems(result.batchRecordVersionId()) > 0,
                "未确认 CONFIRM_REQUIRED 必须阻断审批");

        MesProBatchRecordVersionMigrationItemDO confirmRequired = items.stream()
                .filter(item -> "CONFIRM_REQUIRED".equals(item.getRiskLevel()))
                .findFirst()
                .orElseThrow();
        migrationItemMapper.updateById(new MesProBatchRecordVersionMigrationItemDO()
                .setId(confirmRequired.getId())
                .setConfirmed(true)
                .setConfirmedBy(1001L)
                .setConfirmedAt(LocalDateTime.now())
                .setConfirmComment("真实结构化迁移确认")
                .setConfirmIdempotencyKey("phase2-confirm-test"));
        assertEquals(0, migrationItemMapper.countBlockingItems(result.batchRecordVersionId()),
                "CONFIRM_REQUIRED 确认后不得继续阻断审批");
    }

    @Test
    void recognizeUploadedRoute_whenSameHashUsedByExistingApprovedVersion_generatesVersionScopedReportCodes() {
        TenantContextHolder.setTenantId(122L);
        List<MesProBatchRecordParsedTable> parsedTables = uploadedRouteParsedTables(2, "Same Hash Upgrade Table ");
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("同 hash 升版批记录");
        String shaPrefix = sha256("same-hash-word-bytes".getBytes(StandardCharsets.UTF_8)).substring(0, 8);
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED", null,
                "old.doc", "same-hash-old", 10001L, null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        for (int index = 1; index <= 2; index++) {
            MesProBatchRecordReportDO existing = TestBatchRecordFixtures.metadataReport(
                    (long) index, "BATCH_OLD", index, "existing-report-" + index,
                    "EBR_TN122_B_DOC_" + shaPrefix + "_T" + String.format("%02d", index),
                    "既有表" + index, "old.doc");
            existing.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
            existing.setBatchRecordName("同 hash 升版批记录");
            existing.setBatchRecordDefinitionId(definition.getId());
            existing.setBatchRecordVersionId(currentVersion.getId());
            reportMapper.insert(existing);
        }
        when(jimuReportGateway.getReportInfo("existing-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "existing-report-1", "EBR_TN122_B_DOC_" + shaPrefix + "_T01",
                        "既有表1", LocalDateTime.now()));
        AtomicInteger newReportCounter = new AtomicInteger();
        AtomicReference<Long> pendingVersionIdSeenByGateway = new AtomicReference<>();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            assertTrue(saveReq.reportCode().matches("EBR_TN122_B_DOC_" + shaPrefix + "_V\\d+_T0[12]"),
                    "new version reportCode must include version id: " + saveReq.reportCode());
            String marker = saveReq.reportCode().replaceAll("^.*_V(\\d+)_T\\d+$", "$1");
            pendingVersionIdSeenByGateway.set(Long.valueOf(marker));
            return TestBatchRecordFixtures.generatedReport(
                    "new-same-hash-report-" + newReportCounter.incrementAndGet(),
                    saveReq.reportCode(), saveReq.reportName());
        });
        MockMultipartFile file = new MockMultipartFile(
                "file", "same-hash.doc", "application/msword",
                "same-hash-word-bytes".getBytes(StandardCharsets.UTF_8));
        seedWorkOrderProduct("同 hash 升版批记录", "BRP-001");
        seedDccProjectCode("同 hash 升版批记录", "BRP-001");

        MesProBatchRecordImportResult result = reportService.recognizeUploadedRoute(
                file, MesProBatchRecordRecognitionRouteKeys.B, "同 hash 升版批记录", "UPGRADE",
                currentVersion.getId(), List.of("同 hash 升版批记录"), true, List.of(), List.of("同 hash 升版批记录"));

        MesProBatchRecordVersionDO pendingVersion = versionMapper.selectById(result.batchRecordVersionId());
        assertEquals(pendingVersion.getId(), pendingVersionIdSeenByGateway.get());
        assertEquals(4L, reportMapper.selectCount());
        assertEquals(2, rawCount("""
                SELECT COUNT(*) FROM mes_pro_batch_record_report
                WHERE batch_record_version_id = ? AND report_code LIKE ?
                """, pendingVersion.getId(), "%_V" + pendingVersion.getId() + "_%"));
        assertEquals(2, rawCount("""
                SELECT COUNT(*) FROM mes_pro_batch_record_report
                WHERE batch_record_version_id = ? AND report_code NOT LIKE '%\\_V%'
                """, currentVersion.getId()));
    }

    @Test
    void recognizeUploadedRoute_whenSameHashApprovedVersionIsReimported_canApproveAsNextVersion() {
        TenantContextHolder.setTenantId(122L);
        List<MesProBatchRecordParsedTable> parsedTables = uploadedRouteParsedTables(2, "Same Hash Approved Upgrade ");
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("同 hash 已审批再升版批记录");
        byte[] uploadedBytes = "same-approved-hash-word-bytes".getBytes(StandardCharsets.UTF_8);
        String sha = sha256(uploadedBytes);
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED", null,
                "same-hash.doc", sha, 10001L, null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        for (int index = 1; index <= 2; index++) {
            MesProBatchRecordReportDO existing = TestBatchRecordFixtures.metadataReport(
                    (long) index, "BATCH_OLD", index, "approved-existing-report-" + index,
                    "EBR_TN122_B_DOC_" + sha.substring(0, 8) + "_V" + currentVersion.getId()
                            + "_T" + String.format("%02d", index),
                    "既有表" + index, "same-hash.doc");
            existing.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
            existing.setBatchRecordName("同 hash 已审批再升版批记录");
            existing.setSourceFileSha256(sha);
            existing.setBatchRecordDefinitionId(definition.getId());
            existing.setBatchRecordVersionId(currentVersion.getId());
            reportMapper.insert(existing);
        }
        when(jimuReportGateway.getReportInfo("approved-existing-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "approved-existing-report-1", "EBR_TN122_B_DOC_" + sha.substring(0, 8)
                                + "_V" + currentVersion.getId() + "_T01",
                        "既有表1", LocalDateTime.now()));
        AtomicInteger newReportCounter = new AtomicInteger();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            return TestBatchRecordFixtures.generatedReport(
                    "approved-reimport-report-" + newReportCounter.incrementAndGet(),
                    saveReq.reportCode(), saveReq.reportName());
        });
        seedWorkOrderProduct("同 hash 已审批再升版批记录", "BRP-APPROVED-REIMPORT");
        seedDccProjectCode("同 hash 已审批再升版批记录", "BRP-APPROVED-REIMPORT");
        MockMultipartFile file = new MockMultipartFile(
                "file", "same-hash.doc", "application/msword", uploadedBytes);

        MesProBatchRecordImportResult imported = reportService.recognizeUploadedRoute(
                file, MesProBatchRecordRecognitionRouteKeys.B, "同 hash 已审批再升版批记录", "UPGRADE",
                currentVersion.getId(), List.of("同 hash 已审批再升版批记录"), true, List.of(), List.of("同 hash 已审批再升版批记录"));
        MesProBatchRecordVersionDO pendingVersion = versionMapper.selectById(imported.batchRecordVersionId());
        assertTrue(!currentVersion.getId().equals(pendingVersion.getId()));
        assertEquals("V2.0", pendingVersion.getVersionNo());
        assertEquals(currentVersion.getId(), pendingVersion.getSourceVersionId());
        assertEquals("PRECHECK_PASSED", pendingVersion.getStatus());
        assertEquals(currentVersion.getId(), definitionMapper.selectById(definition.getId()).getCurrentVersionId());

        MesProBatchRecordVersionApprovalResult submitted =
                reportService.submitBatchRecordVersionApproval(pendingVersion.getId(), 101L);
        MesProBatchRecordVersionApprovalResult approved =
                reportService.handleBatchRecordVersionApprovalCallback(
                        submitted.approvalInstanceId(), "event-approved-reimport", "APPROVED", 202L);

        assertEquals("APPROVED", approved.versionStatus());
        assertEquals(pendingVersion.getId(), definitionMapper.selectById(definition.getId()).getCurrentVersionId());
        assertEquals("APPROVED", versionMapper.selectById(pendingVersion.getId()).getStatus());
        assertEquals("OBSOLETE", versionMapper.selectById(currentVersion.getId()).getStatus());
        assertEquals(1L, versionMapper.countByDefinitionIdAndStatus(definition.getId(), "APPROVED"));
        assertEquals(2, rawCount("""
                SELECT COUNT(*) FROM mes_pro_batch_record_report
                WHERE batch_record_definition_id = ? AND batch_record_version_id = ?
                """, definition.getId(), currentVersion.getId()));
        assertEquals(2, rawCount("""
                SELECT COUNT(*) FROM mes_pro_batch_record_report
                WHERE batch_record_definition_id = ? AND batch_record_version_id = ?
                """, definition.getId(), pendingVersion.getId()));
    }

    @Test
    void recognizeUploadedRoute_whenSameHashPrecheckVersionTargetsOlderVersion_voidsOldPrecheckAndCreatesRequestedApproval() {
        TenantContextHolder.setTenantId(122L);
        List<MesProBatchRecordParsedTable> parsedTables = uploadedRouteParsedTables(2, "Superseded Same Hash Upgrade ");
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("同 hash 旧预检替换批记录");
        byte[] uploadedBytes = "same-hash-precheck-replaced-word".getBytes(StandardCharsets.UTF_8);
        String sha = sha256(uploadedBytes);
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED", null,
                "old.doc", "old-precheck-replaced-sha", 10001L, null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        MesProBatchRecordVersionDO oldPrecheck = insertVersion(definition.getId(), "V2.0", "PRECHECK_PASSED",
                currentVersion.getId(), "same-hash.doc", sha, null, currentVersion.getRouteId());
        for (int index = 1; index <= 2; index++) {
            MesProBatchRecordReportDO existing = TestBatchRecordFixtures.metadataReport(
                    (long) index, "existing-v1", index, "existing-v1-report-" + index,
                    "EBR_TN122_B_DOC_existing_V" + currentVersion.getId()
                            + "_T" + String.format("%02d", index),
                    "既有表" + index, "old.doc");
            existing.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
            existing.setBatchRecordName("同 hash 旧预检替换批记录");
            existing.setSourceFileSha256("old-precheck-replaced-sha");
            existing.setBatchRecordDefinitionId(definition.getId());
            existing.setBatchRecordVersionId(currentVersion.getId());
            reportMapper.insert(existing);
        }
        when(jimuReportGateway.getReportInfo("existing-v1-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "existing-v1-report-1", "EBR_TN122_B_DOC_existing_V" + currentVersion.getId() + "_T01",
                        "既有表1", LocalDateTime.now()));
        AtomicInteger newReportCounter = new AtomicInteger();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            return TestBatchRecordFixtures.generatedReport(
                    "same-hash-replaced-report-" + newReportCounter.incrementAndGet(),
                    saveReq.reportCode(), saveReq.reportName());
        });
        seedWorkOrderProduct("同 hash 旧预检替换批记录", "BRP-SAME-HASH-REPLACED");
        seedDccProjectCode("同 hash 旧预检替换批记录", "BRP-SAME-HASH-REPLACED");
        MockMultipartFile file = new MockMultipartFile(
                "file", "same-hash.doc", "application/msword", uploadedBytes);

        MesProBatchRecordImportResult result = reportService.recognizeUploadedRoute(
                file, MesProBatchRecordRecognitionRouteKeys.B, "同 hash 旧预检替换批记录", "UPGRADE",
                currentVersion.getId(), "V3.0", List.of("同 hash 旧预检替换批记录"), true, List.of(),
                List.of("同 hash 旧预检替换批记录"), 101L);

        MesProBatchRecordVersionDO replacedOldPrecheck = versionMapper.selectById(oldPrecheck.getId());
        MesProBatchRecordVersionDO submittedV3 = versionMapper.selectById(result.batchRecordVersionId());
        assertEquals("VOIDED", replacedOldPrecheck.getStatus());
        assertTrue(replacedOldPrecheck.getRemark().contains("V3.0"));
        assertEquals("V3.0", result.versionNo());
        assertEquals("PENDING_APPROVAL", result.versionStatus());
        assertEquals("PENDING_APPROVAL", submittedV3.getStatus());
        assertEquals(oldPrecheck.getId(), submittedV3.getSourceVersionId());
        assertEquals(101L, submittedV3.getSubmittedBy());
        assertNotNull(submittedV3.getApprovalInstanceId());
        assertEquals(currentVersion.getId(), definitionMapper.selectById(definition.getId()).getCurrentVersionId());
        MesProBatchRecordVersionApprovalResult approved = reportService.handleBatchRecordVersionApprovalCallback(
                submittedV3.getApprovalInstanceId(), "event-replaced-approved", "APPROVED",
                "同文件旧预检替换后审核通过", 202L);
        assertEquals("APPROVED", approved.versionStatus());
        assertEquals(submittedV3.getId(), definitionMapper.selectById(definition.getId()).getCurrentVersionId());
    }

    @Test
    void recognizeUploadedRoute_withSubmitterSubmitsV2ToV3UpgradeApprovalImmediately() {
        TenantContextHolder.setTenantId(122L);
        List<MesProBatchRecordParsedTable> parsedTables = uploadedRouteParsedTables(2, "Immediate Approval Upgrade ");
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("球囊扩张压力泵");
        byte[] uploadedBytes = "immediate-approval-upgrade-word".getBytes(StandardCharsets.UTF_8);
        String sha = sha256(uploadedBytes);
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V2.0", "APPROVED",
                null, "old-immediate.doc", sha, 10101L, null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        for (int index = 1; index <= 2; index++) {
            MesProBatchRecordReportDO existing = TestBatchRecordFixtures.metadataReport(
                    (long) index, "BATCH_IMMEDIATE_OLD", index, "immediate-existing-report-" + index,
                    "EBR_TN122_B_DOC_" + sha.substring(0, 8) + "_V" + currentVersion.getId()
                            + "_T" + String.format("%02d", index),
                    "既有表" + index, "old-immediate.doc");
            existing.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
            existing.setBatchRecordName("球囊扩张压力泵");
            existing.setSourceFileSha256(sha);
            existing.setBatchRecordDefinitionId(definition.getId());
            existing.setBatchRecordVersionId(currentVersion.getId());
            reportMapper.insert(existing);
        }
        when(jimuReportGateway.getReportInfo("immediate-existing-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "immediate-existing-report-1", "EBR_TN122_B_DOC_" + sha.substring(0, 8)
                                + "_V" + currentVersion.getId() + "_T01",
                        "既有表1", LocalDateTime.now()));
        AtomicInteger newReportCounter = new AtomicInteger();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            return TestBatchRecordFixtures.generatedReport(
                    "immediate-approval-report-" + newReportCounter.incrementAndGet(),
                    saveReq.reportCode(), saveReq.reportName());
        });
        seedDccProjectCode("球囊扩张压力泵", "BRP-IMMEDIATE-APPROVAL");
        MockMultipartFile file = new MockMultipartFile(
                "file", "immediate-approval.doc", "application/msword", uploadedBytes);

        MesProBatchRecordImportResult imported = reportService.recognizeUploadedRoute(
                file, MesProBatchRecordRecognitionRouteKeys.B, "球囊扩张压力泵", "UPGRADE",
                currentVersion.getId(), null, List.of("球囊扩张压力泵"), true, List.of(),
                List.of("球囊扩张压力泵"), 101L);

        MesProBatchRecordVersionDO submittedVersion = versionMapper.selectById(imported.batchRecordVersionId());
        assertEquals("V3.0", imported.versionNo());
        assertEquals("PENDING_APPROVAL", imported.versionStatus());
        assertNotNull(imported.approvalInstanceId());
        assertEquals("PENDING_APPROVAL", submittedVersion.getStatus());
        assertEquals(101L, submittedVersion.getSubmittedBy());
        assertNotNull(submittedVersion.getSubmittedAt());
        assertNotNull(submittedVersion.getApprovalInstanceId());
        assertEquals(submittedVersion.getApprovalInstanceId(), imported.approvalInstanceId());
        assertEquals(currentVersion.getId(), submittedVersion.getSourceVersionId());
        assertEquals(currentVersion.getId(), definitionMapper.selectById(definition.getId()).getCurrentVersionId());
    }

    @Test
    void batchRecordVersionApproval_switchesCurrentVersionAfterApprovedCallbackAndIgnoresDuplicateEvent() {
        MesProBatchRecordDefinitionDO definition = MesProBatchRecordDefinitionDO.builder()
                .batchRecordName("审批批记录")
                .routeKey(MesProBatchRecordRecognitionRouteKeys.B)
                .build();
        definitionMapper.insert(definition);
        MesProBatchRecordVersionDO currentVersion = MesProBatchRecordVersionDO.builder()
                .definitionId(definition.getId())
                .versionNo("V1.0")
                .status("APPROVED")
                .sourceFileName("old.doc")
                .sourceFileSha256("old-sha256")
                .routeId(10001L)
                .build();
        versionMapper.insert(currentVersion);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        MesProBatchRecordVersionDO pendingVersion = MesProBatchRecordVersionDO.builder()
                .definitionId(definition.getId())
                .versionNo("V2.0")
                .status("PRECHECK_PASSED")
                .sourceVersionId(currentVersion.getId())
                .sourceFileName("new.doc")
                .sourceFileSha256("new-sha256")
                .routeId(20001L)
                .sourceRouteId(10001L)
                .build();
        versionMapper.insert(pendingVersion);

        MesProBatchRecordVersionApprovalResult submitted =
                reportService.submitBatchRecordVersionApproval(pendingVersion.getId(), 101L);
        assertEquals("PENDING_APPROVAL", submitted.versionStatus());
        assertNotNull(submitted.approvalInstanceId());
        assertEquals(currentVersion.getId(), definitionMapper.selectById(definition.getId()).getCurrentVersionId());
        ArgumentCaptor<BusinessApprovalContext> contextCaptor =
                ArgumentCaptor.forClass(BusinessApprovalContext.class);
        verify(businessApprovalOrchestrator).submit(contextCaptor.capture());
        BusinessApprovalContext context = contextCaptor.getValue();
        assertEquals("BATCH_RECORD_VERSION", context.getObjectType());
        assertEquals(String.valueOf(pendingVersion.getId()), context.getObjectId());
        assertEquals("PUBLISH", context.getActionCode());
        assertEquals("PRECHECK_PASSED", context.getObjectState());

        MesProBatchRecordVersionApprovalResult approved =
                reportService.handleBatchRecordVersionApprovalCallback(
                        submitted.approvalInstanceId(), "event-self", "APPROVED", 101L);
        assertEquals("PROCESSED", approved.processedResult());
        assertEquals("APPROVED", approved.versionStatus());
        assertEquals(pendingVersion.getId(), definitionMapper.selectById(definition.getId()).getCurrentVersionId());
        assertEquals(1, rawCount("""
                SELECT COUNT(*) FROM mes_pro_batch_record_version_approval_event
                WHERE approval_instance_id = ? AND approval_event_id = ?
                """, submitted.approvalInstanceId(), "event-self"));

        MesProBatchRecordVersionApprovalResult duplicate =
                reportService.handleBatchRecordVersionApprovalCallback(
                        submitted.approvalInstanceId(), "event-self", "APPROVED", 101L);
        assertEquals("DUPLICATE", duplicate.processedResult());
        assertEquals(1, rawCount("""
                SELECT COUNT(*) FROM mes_pro_batch_record_version_approval_event
                WHERE approval_instance_id = ? AND approval_event_id = ?
                """, submitted.approvalInstanceId(), "event-self"));
    }

    @Test
    void batchRecordVersionApproval_submitsThroughBusinessApprovalOrchestrator() {
        TenantContextHolder.setTenantId(122L);
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("平台审批批记录");
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED", null,
                "old.doc", "old-platform-sha256", 10001L, null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        MesProBatchRecordVersionDO pendingVersion = insertVersion(definition.getId(), "V2.0", "PRECHECK_PASSED",
                currentVersion.getId(), "new.doc", "new-platform-sha256", 20001L, 10001L);

        reportService.submitBatchRecordVersionApproval(pendingVersion.getId(), 101L);

        ArgumentCaptor<BusinessApprovalContext> contextCaptor =
                ArgumentCaptor.forClass(BusinessApprovalContext.class);
        verify(businessApprovalOrchestrator).submit(contextCaptor.capture());
        BusinessApprovalContext context = contextCaptor.getValue();
        assertEquals(122L, context.getTenantId());
        assertEquals("MES", context.getDataDomain());
        assertEquals("MES", context.getSystemCode());
        assertEquals("BATCH_RECORD_VERSION", context.getObjectType());
        assertEquals(String.valueOf(pendingVersion.getId()), context.getObjectId());
        assertEquals("V2.0", context.getObjectVersion());
        assertEquals("PUBLISH", context.getActionCode());
        assertEquals("PRECHECK_PASSED", context.getObjectState());
        assertEquals(101L, context.getApplicantUserId());
    }

    @Test
    void batchRecordVersionApproval_obsoletesPreviousPublishedVersionWhenNewVersionApproved() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("审批自动作废旧版批记录");
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED", null,
                "old.doc", "old-obsolete-sha256", 10001L, null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        MesProBatchRecordVersionDO pendingVersion = insertVersion(definition.getId(), "V2.0", "PRECHECK_PASSED",
                currentVersion.getId(), "new.doc", "new-obsolete-sha256", 20001L, 10001L);

        MesProBatchRecordVersionApprovalResult submitted =
                reportService.submitBatchRecordVersionApproval(pendingVersion.getId(), 101L);
        MesProBatchRecordVersionApprovalResult approved =
                reportService.handleBatchRecordVersionApprovalCallback(
                        submitted.approvalInstanceId(), "event-obsolete-old", "APPROVED", 102L);

        assertEquals("APPROVED", approved.versionStatus());
        assertEquals(pendingVersion.getId(), definitionMapper.selectById(definition.getId()).getCurrentVersionId());
        assertEquals("APPROVED", versionMapper.selectById(pendingVersion.getId()).getStatus());
        assertEquals("OBSOLETE", versionMapper.selectById(currentVersion.getId()).getStatus());
        assertEquals(1L, versionMapper.countByDefinitionIdAndStatus(definition.getId(), "APPROVED"));
    }

    @Test
    void batchRecordVersionApproval_rejectedCallbackDoesNotSwitchCurrentVersion() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("审批拒绝批记录");
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED", null,
                "old.doc", "old-sha256", 10001L, null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        MesProBatchRecordVersionDO pendingVersion = insertVersion(definition.getId(), "V2.0", "PRECHECK_PASSED",
                currentVersion.getId(), "new.doc", "new-sha256", 20001L, 10001L);

        MesProBatchRecordVersionApprovalResult submitted =
                reportService.submitBatchRecordVersionApproval(pendingVersion.getId(), 301L);
        MesProBatchRecordVersionApprovalResult rejected =
                reportService.handleBatchRecordVersionApprovalCallback(
                        submitted.approvalInstanceId(), "event-rejected", "REJECTED", 302L);

        assertEquals("PROCESSED", rejected.processedResult());
        assertEquals("REJECTED", rejected.versionStatus());
        assertEquals(currentVersion.getId(), definitionMapper.selectById(definition.getId()).getCurrentVersionId());
        assertEquals("REJECTED", versionMapper.selectById(pendingVersion.getId()).getStatus());
    }

    @Test
    void batchRecordVersionApproval_failsFastWhenBpmProcessDoesNotStart() {
        when(businessApprovalOrchestrator.submit(any(BusinessApprovalContext.class)))
                .thenThrow(new BusinessApprovalException(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_NOT_STARTED,
                        "BPM process instance was not started"));
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("BPM 未启动批记录");
        MesProBatchRecordVersionDO pendingVersion = insertVersion(definition.getId(), "V2.0", "PRECHECK_PASSED",
                null, "new.doc", "new-sha256", 20001L, null);

        BusinessApprovalException exception = assertThrows(BusinessApprovalException.class,
                () -> reportService.submitBatchRecordVersionApproval(pendingVersion.getId(), 901L));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_PROCESS_NOT_STARTED, exception.getErrorCode());
        MesProBatchRecordVersionDO unchanged = versionMapper.selectById(pendingVersion.getId());
        assertEquals("PRECHECK_PASSED", unchanged.getStatus());
        assertNull(unchanged.getApprovalInstanceId());
    }

    @Test
    void batchRecordVersionApproval_blocksWhenMigrationHasBlockerOrConfirmRequired() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("迁移阻断批记录");
        MesProBatchRecordVersionDO pendingVersion = insertVersion(definition.getId(), "V1.0", "PRECHECK_PASSED",
                null, "new.doc", "new-sha256-blocker", 20001L, null);
        migrationItemMapper.insert(MesProBatchRecordVersionMigrationItemDO.builder()
                .definitionId(definition.getId())
                .versionId(pendingVersion.getId())
                .sourceVersionId(null)
                .itemType("CELL_RULE")
                .sourceLogicalKey("R1C1")
                .targetLogicalKey(null)
                .matchConfidence(BigDecimal.ZERO)
                .matchEvidenceJson("{\"reason\":\"missing target cell\"}")
                .riskLevel("BLOCKER")
                .message("关键单元格规则无法迁移")
                .build());

        ServiceException blockerException = assertThrows(ServiceException.class,
                () -> reportService.submitBatchRecordVersionApproval(pendingVersion.getId(), 401L));
        assertEquals(PRO_BATCH_RECORD_REPORT_VERSION_MIGRATION_BLOCKED.getCode(), blockerException.getCode());
        assertEquals("PRECHECK_PASSED", versionMapper.selectById(pendingVersion.getId()).getStatus());

        migrationItemMapper.insert(MesProBatchRecordVersionMigrationItemDO.builder()
                .definitionId(definition.getId())
                .versionId(pendingVersion.getId())
                .sourceVersionId(null)
                .itemType("SIGNATURE")
                .sourceLogicalKey("R2C2")
                .targetLogicalKey("R3C2")
                .matchConfidence(new BigDecimal("0.75"))
                .matchEvidenceJson("{\"reason\":\"low confidence\"}")
                .riskLevel("CONFIRM_REQUIRED")
                .message("签名位需要人工确认")
                .build());
        assertEquals(2L, migrationItemMapper.countBlockingItems(pendingVersion.getId()));
    }

    @Test
    void batchRecordVersionApproval_pendingVersionBlocksSecondSubmit() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("审批中唯一批记录");
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED", null,
                "old.doc", "old-sha256", 10001L, null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        MesProBatchRecordVersionDO pendingV2 = insertVersion(definition.getId(), "V2.0", "PRECHECK_PASSED",
                currentVersion.getId(), "new-v2.doc", "new-sha256-v2", 20001L, 10001L);
        MesProBatchRecordVersionDO pendingV3 = insertVersion(definition.getId(), "V3.0", "PRECHECK_PASSED",
                currentVersion.getId(), "new-v3.doc", "new-sha256-v3", 30001L, 10001L);

        MesProBatchRecordVersionApprovalResult submittedV2 =
                reportService.submitBatchRecordVersionApproval(pendingV2.getId(), 501L);

        assertEquals("PENDING_APPROVAL", submittedV2.versionStatus());
        ServiceException secondSubmitException = assertThrows(ServiceException.class,
                () -> reportService.submitBatchRecordVersionApproval(pendingV3.getId(), 502L));
        assertEquals(PRO_BATCH_RECORD_REPORT_VERSION_PENDING_APPROVAL_EXISTS.getCode(),
                secondSubmitException.getCode());
        assertEquals("PRECHECK_PASSED", versionMapper.selectById(pendingV3.getId()).getStatus());
    }

    @Test
    void batchRecordVersionApproval_bpmApproveCallbackCloseLoop() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("人工审批通过批记录");
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "old.doc", "old-sha256", 41001L, null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        MesProBatchRecordVersionDO pendingVersion = insertVersion(definition.getId(), "V2.0", "PRECHECK_PASSED",
                currentVersion.getId(), "new.doc", "new-sha256", 42001L, 41001L);

        MesProBatchRecordVersionApprovalResult submitted =
                reportService.submitBatchRecordVersionApproval(pendingVersion.getId(), 701L);

        assertEquals("PENDING_APPROVAL", submitted.versionStatus());
        assertNotNull(submitted.approvalInstanceId());
        MesProBatchRecordVersionApprovalResult approved = reportService.handleBatchRecordVersionApprovalCallback(
                submitted.approvalInstanceId(), "event-bpm-self-approved", "APPROVED", "提交人自审", 701L);

        assertEquals("PROCESSED", approved.processedResult());
        assertEquals("APPROVED", approved.versionStatus());
        assertEquals(pendingVersion.getId(), definitionMapper.selectById(definition.getId()).getCurrentVersionId());
        assertEquals(1, rawCount("""
                SELECT COUNT(*) FROM mes_pro_batch_record_version_approval_event
                WHERE version_id = ? AND approval_result = 'APPROVED' AND actor_user_id = ?
                """, pendingVersion.getId(), 701L));
    }

    @Test
    void batchRecordVersionApproval_bpmRejectCallbackKeepsCurrentVersionAndRecordsReason() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("人工审批驳回批记录");
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "old.doc", "old-sha256", 43001L, null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        MesProBatchRecordVersionDO pendingVersion = insertVersion(definition.getId(), "V2.0", "PRECHECK_PASSED",
                currentVersion.getId(), "new.doc", "new-sha256", 44001L, 43001L);

        MesProBatchRecordVersionApprovalResult submitted =
                reportService.submitBatchRecordVersionApproval(pendingVersion.getId(), 801L);
        MesProBatchRecordVersionApprovalResult rejected = reportService.handleBatchRecordVersionApprovalCallback(
                submitted.approvalInstanceId(), "event-bpm-rejected", "REJECTED", "资料不完整", 802L);

        MesProBatchRecordVersionDO rejectedVersion = versionMapper.selectById(pendingVersion.getId());
        assertEquals("PROCESSED", rejected.processedResult());
        assertEquals("REJECTED", rejected.versionStatus());
        assertEquals(currentVersion.getId(), definitionMapper.selectById(definition.getId()).getCurrentVersionId());
        assertEquals("REJECTED", rejectedVersion.getStatus());
        assertEquals("资料不完整", rejectedVersion.getRejectReason());
        assertEquals(1, rawCount("""
                SELECT COUNT(*) FROM mes_pro_batch_record_version_approval_event
                WHERE version_id = ? AND approval_result = 'REJECTED' AND actor_user_id = ?
                  AND remark = '资料不完整'
                """, pendingVersion.getId(), 802L));
    }

    @Test
    void recognizeUploadedRoute_whenSameFileReimportedUnderNewBatchName_createsDefinitionScopedVersionSnapshot() {
        byte[] uploadedBytes = "uploaded-word-bytes".getBytes(StandardCharsets.UTF_8);
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"),
                TestBatchRecordFixtures.parsedTable(2, "Upgrade Table 2"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");

        MesProBatchRecordReportDO existing = TestBatchRecordFixtures.metadataReport(
                1L, "BATCH_OLD", 1, "existing-report-1", "EBR_TN1_B_DOC_efb714c4_T01", "既有表1", "old.doc");
        existing.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
        existing.setBatchRecordName("旧批记录");
        existing.setSourceFileSha256(
                "efb714c4386382c720216e9a61b47c91724ee474733b0a037d2e143d90e3aa51");
        reportMapper.insert(existing);

        AtomicInteger newReportCounter = new AtomicInteger();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            return TestBatchRecordFixtures.generatedReport(
                    saveReq.existingReportId() == null ? "new-uploaded-report-" + newReportCounter.incrementAndGet()
                            : saveReq.existingReportId(),
                    saveReq.reportCode(), saveReq.reportName());
        });

        MockMultipartFile file = new MockMultipartFile(
                "file", "user-selected.doc", "application/msword", uploadedBytes);
        seedWorkOrderProduct("新批记录", "BRP-001");
        seedDccProjectCode("新批记录", "BRP-001");

        MesProBatchRecordImportResult result = reportService.recognizeUploadedRoute(
                file, MesProBatchRecordRecognitionRouteKeys.B, "新批记录", false, List.of("新批记录"));

        assertEquals(2, result.createdCount());
        assertEquals(0, result.updatedCount());
        assertEquals(3L, reportMapper.selectCount());
        assertNotNull(result.batchRecordDefinitionId());
        assertNotNull(result.batchRecordVersionId());
        assertEquals("V1.0", result.versionNo());
        assertEquals("APPROVED", result.versionStatus());
        assertEquals(result.batchRecordVersionId(), definitionMapper.selectById(result.batchRecordDefinitionId()).getCurrentVersionId(),
                "首次导入 V1.0 无需审批，必须直接成为当前可用版本");
        MesProBatchRecordReportDO oldSaved = reportMapper.selectByReportId("existing-report-1");
        assertEquals("旧批记录", oldSaved.getBatchRecordName());
        assertEquals("old.doc", oldSaved.getSourceFileName());
        MesProBatchRecordReportDO newSaved = reportMapper.selectByReportId("new-uploaded-report-1");
        assertEquals("新批记录", newSaved.getBatchRecordName());
        assertEquals("user-selected.doc", newSaved.getSourceFileName());
        assertEquals(result.batchRecordDefinitionId(), newSaved.getBatchRecordDefinitionId());
        assertEquals(result.batchRecordVersionId(), newSaved.getBatchRecordVersionId());
        ArgumentCaptor<MesProBatchRecordJimuReportSaveReq> saveCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordJimuReportSaveReq.class);
        verify(jimuReportGateway, times(2)).saveOrUpdateReport(saveCaptor.capture());
        assertTrue(saveCaptor.getAllValues().stream().allMatch(req -> req.existingReportId() == null));
    }

    @Test
    void recognizeUploadedRoute_whenSameHashReimportedForPendingVersion_returnsIdempotentResultWithoutNewSnapshots() {
        byte[] uploadedBytes = "same-hash-word-bytes".getBytes(StandardCharsets.UTF_8);
        String uploadedSha256 = sha256(uploadedBytes);
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"),
                TestBatchRecordFixtures.parsedTable(2, "粗洗工序"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("幂等批记录");
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED", null,
                "same-hash.doc", uploadedSha256, 10001L, null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        for (int index = 1; index <= 2; index++) {
            MesProBatchRecordReportDO existing = TestBatchRecordFixtures.metadataReport(
                    (long) index, "BATCH_EXISTING", index, "same-hash-existing-report-" + index,
                    "EBR_TN1_B_DOC_" + uploadedSha256.substring(0, 8)
                            + "_V" + currentVersion.getId() + "_T" + String.format("%02d", index),
                    "既有表" + index, "same-hash.doc");
            existing.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
            existing.setBatchRecordName("幂等批记录");
            existing.setSourceFileSha256(uploadedSha256);
            existing.setBatchRecordDefinitionId(definition.getId());
            existing.setBatchRecordVersionId(currentVersion.getId());
            reportMapper.insert(existing);
        }
        when(jimuReportGateway.getReportInfo("same-hash-existing-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "same-hash-existing-report-1", "EBR_TN1_B_DOC_"
                                + uploadedSha256.substring(0, 8) + "_V" + currentVersion.getId() + "_T01",
                        "既有表1", LocalDateTime.now()));
        AtomicInteger newReportCounter = new AtomicInteger();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            return TestBatchRecordFixtures.generatedReport(
                    "same-hash-report-" + newReportCounter.incrementAndGet(),
                    saveReq.reportCode(), saveReq.reportName());
        });
        seedWorkOrderProduct("幂等批记录", "BRP-001");
        seedDccProjectCode("幂等批记录", "BRP-001");
        MockMultipartFile firstFile = new MockMultipartFile(
                "file", "same-hash.doc", "application/msword", uploadedBytes);
        MockMultipartFile secondFile = new MockMultipartFile(
                "file", "same-hash.doc", "application/msword", uploadedBytes);

        MesProBatchRecordImportResult first = reportService.recognizeUploadedRoute(
                firstFile, MesProBatchRecordRecognitionRouteKeys.B, "幂等批记录", "UPGRADE",
                currentVersion.getId(), List.of("幂等批记录"), true, List.of(), List.of("幂等批记录"));
        MesProBatchRecordImportResult second = reportService.recognizeUploadedRoute(
                secondFile, MesProBatchRecordRecognitionRouteKeys.B, "幂等批记录", "UPGRADE",
                currentVersion.getId(), null, List.of("幂等批记录"), true, List.of(), List.of("幂等批记录"),
                true, first.routeId(), first.routeVersionId(), null);

        assertEquals(first.batchRecordDefinitionId(), second.batchRecordDefinitionId());
        assertEquals(first.batchRecordVersionId(), second.batchRecordVersionId());
        assertEquals("PRECHECK_PASSED", first.versionStatus());
        assertEquals(0, second.createdCount());
        assertEquals(0, second.updatedCount());
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_definition"));
        assertEquals(2, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_version"));
        assertEquals(4, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_report"));
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_route"));
        verify(jimuReportGateway, times(2)).saveOrUpdateReport(any());
    }

    @Test
    void recognizeUploadedRoute_rejectsBlankOrTooLongBatchRecordNameWithoutWritingMetadata() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "user-selected.doc", "application/msword",
                "uploaded-word-bytes".getBytes(StandardCharsets.UTF_8));

        assertServiceException(() -> reportService.recognizeUploadedRoute(
                        file, MesProBatchRecordRecognitionRouteKeys.B, " ", false, List.of("球囊扩张压力泵")),
                PRO_BATCH_RECORD_REPORT_BATCH_NAME_EMPTY);
        assertServiceException(() -> reportService.recognizeUploadedRoute(
                        file, MesProBatchRecordRecognitionRouteKeys.B, "批".repeat(101), false, List.of("球囊扩张压力泵")),
                PRO_BATCH_RECORD_REPORT_BATCH_NAME_TOO_LONG);
        assertEquals(0L, reportMapper.selectCount());
        verifyNoInteractions(jimuReportGateway);
    }

    @Test
    void recognizeUploadedRoute_rejectsInvalidRouteWithoutWritingMetadata() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "user-selected.doc", "application/msword",
                "uploaded-word-bytes".getBytes(StandardCharsets.UTF_8));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(file, "Z", "测试批记录", false, List.of("球囊扩张压力泵")));
        assertEquals(PRO_BATCH_RECORD_REPORT_ROUTE_INVALID.getCode(), exception.getCode());
        assertEquals("电子批记录识别路线无效：Z", exception.getMessage());
        assertEquals(0L, reportMapper.selectCount());
    }

    @Test
    void recognizeUploadedRoute_rejectsNonWordFileWithoutWritingMetadata() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "user-selected.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "uploaded-word-bytes".getBytes(StandardCharsets.UTF_8));

        assertServiceException(() -> reportService.recognizeUploadedRoute(
                        file, MesProBatchRecordRecognitionRouteKeys.B, "测试批记录", false, List.of("测试批记录")),
                PRO_BATCH_RECORD_REPORT_FILE_EXTENSION_INVALID);
        assertEquals(0L, reportMapper.selectCount());
    }

    @Test
    void recognizeUploadedRoute_rejectsEmptyProductNamesAndRollsBack() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "route-source.doc", "application/msword",
                "uploaded-word-route-bytes".getBytes(StandardCharsets.UTF_8));

        assertServiceException(() -> reportService.recognizeUploadedRoute(
                        file, MesProBatchRecordRecognitionRouteKeys.B, "测试批记录", false, List.of(" ", "")),
                PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_NAME_REQUIRED);

        assertEquals(0L, reportMapper.selectCount());
        assertNoGeneratedRouteData();
        verifyNoInteractions(jimuReportGateway);
    }

    @Test
    void recognizeUploadedRoute_whenAllSelectedProductNamesHaveNoCode_rollsBackAllGeneratedContent() {
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"),
                TestBatchRecordFixtures.parsedTable(2, "粗洗工序"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            return TestBatchRecordFixtures.generatedReport(
                    "rollback-product-report-" + saveReq.parsedTable().getSourceTableIndex(),
                    saveReq.reportCode(),
                    saveReq.reportName());
        });
        MockMultipartFile file = new MockMultipartFile(
                "file", "route-source.doc", "application/msword",
                "uploaded-word-route-bytes".getBytes(StandardCharsets.UTF_8));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(file, MesProBatchRecordRecognitionRouteKeys.B,
                        "测试批记录", false, List.of("测试批记录")));

        assertEquals(PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_EMPTY.getCode(), exception.getCode());
        assertEquals(0L, reportMapper.selectCount());
        assertNoGeneratedRouteData();
    }

    @Test
    void deleteGeneratedReport_removesMetadataAndDelegatesJimuReportDelete() {
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                1L, "sample-key", 1, "delete-report-1", "EBR_DOC_TESTHASH_T01", "表1", "pilot.doc");
        reportMapper.insert(report);
        when(jimuReportGateway.getReportInfo("delete-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "delete-report-1", "EBR_DOC_TESTHASH_T01", "表1", LocalDateTime.now()));

        reportService.deleteGeneratedReport(report.getReportId());

        assertEquals(0L, reportMapper.selectCount());
        assertEquals(0, rawCount(
                "SELECT COUNT(*) FROM mes_pro_batch_record_report WHERE report_id = ?",
                "delete-report-1"));
        verify(jimuReportGateway).deleteReport("delete-report-1");
    }

    @Test
    void getGeneratedReportPage_filtersExactlyByReportId() {
        MesProBatchRecordReportDO first = TestBatchRecordFixtures.metadataReport(
                61L, "sample-report-id-a", 1, "target-report-1", "EBR_TARGET_T01", "目标表1", PILOT_FILE_NAME);
        MesProBatchRecordReportDO second = TestBatchRecordFixtures.metadataReport(
                62L, "sample-report-id-b", 2, "target-report-2", "EBR_TARGET_T02", "目标表2", PILOT_FILE_NAME);
        reportMapper.insert(first);
        reportMapper.insert(second);
        when(jimuReportGateway.getReportInfo("target-report-1")).thenReturn(
                TestBatchRecordFixtures.reportInfo("target-report-1", "EBR_TARGET_T01", "目标表1", LocalDateTime.now()));
        when(jimuReportGateway.getReportInfo("target-report-2")).thenReturn(
                TestBatchRecordFixtures.reportInfo("target-report-2", "EBR_TARGET_T02", "目标表2", LocalDateTime.now()));

        BatchRecordReportPageReqVO reqVO = new BatchRecordReportPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setReportId("target-report-2");

        PageResult<MesProBatchRecordReportView> result = reportService.getGeneratedReportPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals("target-report-2", result.getList().get(0).reportId());
        assertEquals("目标表2", result.getList().get(0).reportName());
    }

    @Test
    void deleteGeneratedReport_whenBoundByRouteProcess_failsFast() {
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                11L, "sample-bind", 1, "bound-report-1", "EBR_A_T11", "绑定报表", "pilot.doc");
        reportMapper.insert(report);
        new org.springframework.jdbc.core.JdbcTemplate(dataSource).update("""
                INSERT INTO mes_pro_route_process
                (id, route_id, process_id, sort, next_process_id, link_type, prepare_time, wait_time, color_code,
                 key_flag, check_flag, remark, batch_record_report_id, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                910001L, 910010L, 910020L, 1, null, 3, 0, 0, "#00AEF3",
                false, false, "bound", "bound-report-1", "tester", "tester", false, 1L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.deleteGeneratedReport("bound-report-1"));
        assertEquals(PRO_BATCH_RECORD_REPORT_BOUND_BY_ROUTE_PROCESS.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("bound-report-1"));
    }

    @Test
    void deleteGeneratedReports_whenBoundAndForceUnbind_removesBindingsAndDeletesAllReports() {
        MesProBatchRecordReportDO routeProcessBoundReport = TestBatchRecordFixtures.metadataReport(
                13L, "batch-report-id-delete", 1, "bound-report-force-1", "EBR_FORCE_T01", "强制解绑表1", PILOT_FILE_NAME);
        MesProBatchRecordReportDO routeFlowBoundReport = TestBatchRecordFixtures.metadataReport(
                14L, "batch-report-id-delete", 2, "bound-report-force-2", "EBR_FORCE_T02", "强制解绑表2", PILOT_FILE_NAME);
        MesProBatchRecordReportDO routeFlowConfigBoundReport = TestBatchRecordFixtures.metadataReport(
                15L, "batch-report-id-delete", 3, "bound-report-force-3", "EBR_FORCE_T03", "强制解绑表3", PILOT_FILE_NAME);
        MesProBatchRecordReportDO unboundReport = TestBatchRecordFixtures.metadataReport(
                16L, "batch-report-id-delete", 4, "unbound-report-force-4", "EBR_FORCE_T04", "未绑定表4", PILOT_FILE_NAME);
        reportMapper.insert(routeProcessBoundReport);
        reportMapper.insert(routeFlowBoundReport);
        reportMapper.insert(routeFlowConfigBoundReport);
        reportMapper.insert(unboundReport);
        jdbcTemplate().update("""
                INSERT INTO mes_pro_route_process
                (id, route_id, process_id, sort, next_process_id, link_type, prepare_time, wait_time, color_code,
                 key_flag, check_flag, remark, batch_record_report_id, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                99111L, 99112L, 99113L, 1, null, 3, 0, 0, "#00AEF3",
                false, false, "force-unbind-report-id-route-process", "bound-report-force-1", "tester", "tester",
                false, 1L);
        jdbcTemplate().update("""
                INSERT INTO mes_pro_route_flow_process_batch_record
                (id, route_flow_process_config_id, route_id, route_process_id, use_type, batch_record_report_id,
                 form_slot_type, record_category, validation_profile, report_sort, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                99121L, 99122L, 99112L, 99111L, MesProRouteFlowConfigTypeEnum.BATCH.getType(),
                "bound-report-force-2", MesProBatchRecordFormSlotType.MAIN.getType(), "BATCH_RECORD",
                "CONTROLLED_BATCH", 1, "tester", "tester", false, 1L);
        jdbcTemplate().update("""
                INSERT INTO mes_pro_route_flow_process_config
                (id, route_flow_config_id, route_id, route_process_id, use_type, enabled, execution_mode,
                 batch_record_report_id, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                99131L, 99132L, 99112L, 99111L, MesProRouteFlowConfigTypeEnum.BATCH.getType(), true, "SEQUENTIAL",
                "bound-report-force-3", "tester", "tester", false, 1L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.deleteGeneratedReports(
                        List.of("unbound-report-force-4", "bound-report-force-1", "bound-report-force-2",
                                "bound-report-force-3"), false));
        assertEquals(PRO_BATCH_RECORD_REPORT_BOUND_BY_ROUTE_PROCESS.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("bound-report-force-1"));
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_report WHERE report_id = ?",
                "unbound-report-force-4"));
        verify(jimuReportGateway, never()).deleteReport("unbound-report-force-4");
        verify(jimuReportGateway, never()).deleteReport("bound-report-force-1");

        BatchRecordReportDeleteAllRespVO result = reportService.deleteGeneratedReports(
                List.of("unbound-report-force-4", "bound-report-force-1", "bound-report-force-2",
                        "bound-report-force-3"), true);

        assertEquals(4, result.getDeletedReportCount());
        assertEquals(4, result.getDeletedMetadataCount());
        assertEquals(0, result.getSkippedBoundReportCount());
        assertEquals(1, result.getUnboundRouteProcessCount());
        assertEquals(1, result.getDeletedRouteFlowBindingCount());
        assertEquals(1, result.getUnboundRouteFlowProcessConfigCount());
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_report WHERE report_id IN (?, ?, ?, ?)",
                "unbound-report-force-4", "bound-report-force-1", "bound-report-force-2", "bound-report-force-3"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_route_process WHERE batch_record_report_id IN (?, ?, ?)",
                "bound-report-force-1", "bound-report-force-2", "bound-report-force-3"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_route_flow_process_batch_record WHERE batch_record_report_id IN (?, ?, ?)",
                "bound-report-force-1", "bound-report-force-2", "bound-report-force-3"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_route_flow_process_config WHERE batch_record_report_id IN (?, ?, ?)",
                "bound-report-force-1", "bound-report-force-2", "bound-report-force-3"));
        verify(jimuReportGateway).deleteReport("unbound-report-force-4");
        verify(jimuReportGateway).deleteReport("bound-report-force-1");
        verify(jimuReportGateway).deleteReport("bound-report-force-2");
        verify(jimuReportGateway).deleteReport("bound-report-force-3");
    }

    @Test
    void deleteExtraFormSlot_whenBoundByRouteFlowProcessBatchRecord_failsFast() {
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                12L, "sample-extra-bind", 1, "loss-report-1", "EBR_LOSS_T01", "生产过程损耗报告单", "loss.doc");
        report.setBatchRecordName("球囊扩张压力泵方案");
        report.setFormSlotType(MesProBatchRecordFormSlotType.LOSS_REPORT.getType());
        report.setRouteKey(MesProBatchRecordFormSlotType.LOSS_REPORT.getType());
        reportMapper.insert(report);
        jdbcTemplate().update("""
                INSERT INTO mes_pro_route_flow_process_batch_record
                (id, route_flow_process_config_id, route_id, route_process_id, use_type, batch_record_report_id,
                 form_slot_type, record_category, validation_profile, report_sort, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                920001L, 920002L, 920003L, 920004L, MesProRouteFlowConfigTypeEnum.BATCH.getType(), "loss-report-1",
                MesProBatchRecordFormSlotType.LOSS_REPORT.getType(), "BATCH_RECORD", "CONTROLLED_BATCH",
                1, "tester", "tester", false, 1L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.deleteGeneratedReportByBatchRecordNameAndFormSlotType(
                        "球囊扩张压力泵方案", MesProBatchRecordFormSlotType.LOSS_REPORT.getType()));

        assertEquals(PRO_BATCH_RECORD_REPORT_BOUND_BY_ROUTE_PROCESS.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("loss-report-1"));
        verify(jimuReportGateway, never()).deleteReport("loss-report-1");
    }

    @Test
    void deleteAllGeneratedReports_requiresProdConfirmationBeforeGateway() {
        assertServiceException(() -> reportService.deleteAllGeneratedReports(null),
                PRO_BATCH_RECORD_REPORT_DELETE_CONFIRM_INVALID);
        assertServiceException(() -> reportService.deleteAllGeneratedReports("DEV"),
                PRO_BATCH_RECORD_REPORT_DELETE_CONFIRM_INVALID);

        verifyNoInteractions(jimuReportGateway);
    }

    @Test
    void deleteAllGeneratedReports_deletesUnboundCategoryReportsAndMetadataTogether() {
        MesProBatchRecordReportDO first = TestBatchRecordFixtures.metadataReport(
                1L, "sample-a", 1, "delete-all-report-1", "EBR_A_T01", "表1", "pilot.doc");
        first.setReportCategoryId("category-ebrr");
        MesProBatchRecordReportDO second = TestBatchRecordFixtures.metadataReport(
                2L, "sample-b", 2, "delete-all-report-2", "EBR_A_T02", "表2", "pilot.doc");
        second.setReportCategoryId("category-ebrr");
        reportMapper.insert(first);
        reportMapper.insert(second);
        when(jimuReportGateway.findElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");

        BatchRecordReportDeleteAllRespVO result = reportService.deleteAllGeneratedReports("PROD");

        assertEquals(2, result.getDeletedReportCount());
        assertEquals(2, result.getDeletedMetadataCount());
        assertEquals(0, result.getSkippedBoundReportCount());
        assertEquals(0L, reportMapper.selectCount());
        verify(jimuReportGateway).deleteReport("delete-all-report-1");
        verify(jimuReportGateway).deleteReport("delete-all-report-2");
        verify(jimuReportGateway, never()).deleteReportsByCategoryId("category-ebrr");
    }

    @Test
    void deleteAllGeneratedReports_whenCategoryContainsBoundReport_skipsBoundAndDeletesUnbound() {
        MesProBatchRecordReportDO boundReport = TestBatchRecordFixtures.metadataReport(
                21L, "sample-bind-all", 1, "bound-all-report-1", "EBR_A_T21", "目录绑定报表", "pilot.doc");
        boundReport.setReportCategoryId("category-ebrr");
        MesProBatchRecordReportDO unboundReport = TestBatchRecordFixtures.metadataReport(
                22L, "sample-bind-all", 2, "unbound-all-report-2", "EBR_A_T22", "目录未绑定报表", "pilot.doc");
        unboundReport.setReportCategoryId("category-ebrr");
        reportMapper.insert(boundReport);
        reportMapper.insert(unboundReport);
        new org.springframework.jdbc.core.JdbcTemplate(dataSource).update("""
                INSERT INTO mes_pro_route_process
                (id, route_id, process_id, sort, next_process_id, link_type, prepare_time, wait_time, color_code,
                 key_flag, check_flag, remark, batch_record_report_id, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                910101L, 910110L, 910120L, 1, null, 3, 0, 0, "#00AEF3",
                false, false, "bound-all", "bound-all-report-1", "tester", "tester", false, 1L);
        when(jimuReportGateway.findElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");

        BatchRecordReportDeleteAllRespVO result = reportService.deleteAllGeneratedReports("PROD");

        assertEquals(1, result.getDeletedReportCount());
        assertEquals(1, result.getDeletedMetadataCount());
        assertEquals(1, result.getSkippedBoundReportCount());
        assertEquals(1L, reportMapper.selectCount());
        assertEquals("bound-all-report-1", reportMapper.selectByReportId("bound-all-report-1").getReportId());
        verify(jimuReportGateway).deleteReport("unbound-all-report-2");
        verify(jimuReportGateway, never()).deleteReport("bound-all-report-1");
        verify(jimuReportGateway, never()).deleteReportsByCategoryId("category-ebrr");
    }

    @Test
    void deleteAllGeneratedReports_whenCategoryMissing_failsFast() {
        when(jimuReportGateway.findElectronicBatchRecordCategoryId()).thenReturn(null);

        assertServiceException(() -> reportService.deleteAllGeneratedReports("PROD"),
                PRO_BATCH_RECORD_REPORT_CATEGORY_NOT_EXISTS);
    }

    @Test
    void deleteGeneratedReportsByBatchRecordName_deletesOnlySelectedUnboundBatchRecord() {
        MesProBatchRecordReportDO first = TestBatchRecordFixtures.metadataReport(
                801L, "batch-delete-a", 1, "batch-delete-a-report-1", "EBR_DA_T01", "批记录A表1", PILOT_FILE_NAME);
        first.setBatchRecordName("批记录A");
        MesProBatchRecordReportDO second = TestBatchRecordFixtures.metadataReport(
                802L, "batch-delete-a", 2, "batch-delete-a-report-2", "EBR_DA_T02", "批记录A表2", PILOT_FILE_NAME);
        second.setBatchRecordName("批记录A");
        MesProBatchRecordReportDO other = TestBatchRecordFixtures.metadataReport(
                803L, "batch-delete-b", 1, "batch-delete-b-report-1", "EBR_DB_T01", "批记录B表1", PILOT_FILE_NAME);
        other.setBatchRecordName("批记录B");
        reportMapper.insert(first);
        reportMapper.insert(second);
        reportMapper.insert(other);

        BatchRecordReportDeleteAllRespVO result = reportService.deleteGeneratedReportsByBatchRecordName("批记录A");

        assertEquals(2, result.getDeletedReportCount());
        assertEquals(2, result.getDeletedMetadataCount());
        assertEquals(0, result.getSkippedBoundReportCount());
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_report WHERE batch_record_name = ?", "批记录A"));
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_report WHERE batch_record_name = ?", "批记录B"));
        verify(jimuReportGateway).deleteReport("batch-delete-a-report-1");
        verify(jimuReportGateway).deleteReport("batch-delete-a-report-2");
        verify(jimuReportGateway, never()).deleteReport("batch-delete-b-report-1");
    }

    @Test
    void deleteGeneratedReport_whenLastMainReportDeleted_cleansDefinitionVersions() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("删除后重置批记录");
        MesProBatchRecordVersionDO version = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, PILOT_FILE_NAME, "delete-reset-sha", null, null);
        definition.setCurrentVersionId(version.getId());
        definitionMapper.updateById(definition);
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                831L, "delete-reset-sample", 1, "delete-reset-report-1", "EBR_RESET_T01",
                "删除后重置表", PILOT_FILE_NAME);
        report.setBatchRecordName("删除后重置批记录");
        report.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
        report.setBatchRecordDefinitionId(definition.getId());
        report.setBatchRecordVersionId(version.getId());
        report.setFormSlotType(MesProBatchRecordFormSlotType.MAIN.getType());
        reportMapper.insert(report);

        reportService.deleteGeneratedReport("delete-reset-report-1");

        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_report WHERE report_id = ?", "delete-reset-report-1"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_version WHERE definition_id = ?", definition.getId()));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_definition WHERE id = ?", definition.getId()));
        verify(jimuReportGateway).deleteReport("delete-reset-report-1");
    }

    @Test
    void recognizeUploadedRoute_whenDeletedBatchRecordReimported_startsFromV1Again() {
        byte[] firstBytes = "delete-reset-first".getBytes(StandardCharsets.UTF_8);
        byte[] secondBytes = "delete-reset-second".getBytes(StandardCharsets.UTF_8);
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"),
                TestBatchRecordFixtures.parsedTable(2, "粗洗工序"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        AtomicInteger reportCounter = new AtomicInteger();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            return TestBatchRecordFixtures.generatedReport(
                    "reimport-reset-report-" + reportCounter.incrementAndGet(),
                    saveReq.reportCode(), saveReq.reportName());
        });
        seedWorkOrderProduct("删除重导批记录", "BRP-RESET");
        seedDccProjectCode("删除重导批记录", "BRP-RESET");

        MesProBatchRecordImportResult first = reportService.recognizeUploadedRoute(
                new MockMultipartFile("file", "delete-reset-first.doc", "application/msword", firstBytes),
                MesProBatchRecordRecognitionRouteKeys.B, "删除重导批记录", false, List.of("删除重导批记录"));
        BatchRecordReportDeleteAllRespVO deleteResult =
                reportService.deleteGeneratedReportsByBatchRecordName("删除重导批记录", true);
        assertFalse(reportService.existsBatchRecordName(MesProBatchRecordRecognitionRouteKeys.B, "删除重导批记录"));
        MesProBatchRecordImportResult second = reportService.recognizeUploadedRoute(
                new MockMultipartFile("file", "delete-reset-second.doc", "application/msword", secondBytes),
                MesProBatchRecordRecognitionRouteKeys.B, "删除重导批记录", "REBUILD_V1", null, null,
                List.of("删除重导批记录"), true, List.of(), List.of("删除重导批记录"),
                true, first.routeId(), first.routeVersionId(), null);

        assertEquals("V1.0", first.versionNo());
        assertEquals("APPROVED", first.versionStatus());
        assertEquals(2, deleteResult.getDeletedMetadataCount());
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_definition WHERE id = ?",
                first.batchRecordDefinitionId()));
        assertEquals("V1.0", second.versionNo());
        assertEquals("APPROVED", second.versionStatus());
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_definition"));
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_version"));
        assertEquals(2, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_report"));
        verify(jimuReportGateway).deleteReport("reimport-reset-report-1");
        verify(jimuReportGateway).deleteReport("reimport-reset-report-2");
    }

    @Test
    void recognizeUploadedRoute_whenOnlyDefinitionVersionsRemain_cleansOrphanAndStartsFromV1() {
        MesProBatchRecordDefinitionDO orphanDefinition = insertVersionedDefinition("历史孤儿批记录");
        MesProBatchRecordVersionDO orphanVersion = insertVersion(orphanDefinition.getId(), "V3.0", "APPROVED",
                null, "old.doc", "orphan-sha", 990001L, null);
        orphanDefinition.setCurrentVersionId(orphanVersion.getId());
        definitionMapper.updateById(orphanDefinition);
        List<MesProBatchRecordParsedTable> parsedTables = List.of(
                TestBatchRecordFixtures.parsedTable(1, "产品信息"),
                TestBatchRecordFixtures.parsedTable(2, "粗洗工序"));
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        AtomicInteger reportCounter = new AtomicInteger();
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            return TestBatchRecordFixtures.generatedReport(
                    "orphan-reset-report-" + reportCounter.incrementAndGet(),
                    saveReq.reportCode(), saveReq.reportName());
        });
        seedWorkOrderProduct("历史孤儿批记录", "BRP-ORPHAN");
        seedDccProjectCode("历史孤儿批记录", "BRP-ORPHAN");

        MesProBatchRecordImportResult result = reportService.recognizeUploadedRoute(
                new MockMultipartFile("file", "orphan-reset.doc", "application/msword",
                        "orphan-reset".getBytes(StandardCharsets.UTF_8)),
                MesProBatchRecordRecognitionRouteKeys.B, "历史孤儿批记录", false, List.of("历史孤儿批记录"));

        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_definition WHERE id = ?",
                orphanDefinition.getId()));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_version WHERE id = ?",
                orphanVersion.getId()));
        assertEquals("V1.0", result.versionNo());
        assertEquals("APPROVED", result.versionStatus());
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_definition"));
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_version"));
        assertEquals(2, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_report"));
    }

    @Test
    void recognizeUploadedRoute_whenOrphanDefinitionStillBoundWithoutUpgrade_rejectsAsExistingBatchRecord() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("引用残留批记录");
        MesProBatchRecordVersionDO version = insertVersion(definition.getId(), "V2.0", "APPROVED",
                null, "old.doc", "blocked-orphan-sha", 990101L, null);
        definition.setCurrentVersionId(version.getId());
        definitionMapper.updateById(definition);
        jdbcTemplate().update("""
                INSERT INTO mes_pro_route_flow_process_batch_record
                (id, route_flow_process_config_id, route_id, route_process_id, use_type, batch_record_report_id,
                 batch_record_definition_id, batch_record_version_id, form_slot_type, record_category,
                 validation_profile, report_sort, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                99221L, 99222L, 99223L, 99224L, MesProRouteFlowConfigTypeEnum.BATCH.getType(),
                "blocked-orphan-report", definition.getId(), version.getId(),
                MesProBatchRecordFormSlotType.MAIN.getType(), "BATCH_RECORD",
                "CONTROLLED_BATCH", 1, "tester", "tester", false, 1L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(
                        new MockMultipartFile("file", "blocked-orphan.doc", "application/msword",
                                "blocked-orphan".getBytes(StandardCharsets.UTF_8)),
                        MesProBatchRecordRecognitionRouteKeys.B, "引用残留批记录", false, List.of("引用残留批记录")));

        assertEquals(PRO_BATCH_RECORD_REPORT_VERSION_RESET_BLOCKED.getCode(), exception.getCode());
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_definition WHERE id = ?",
                definition.getId()));
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_version WHERE id = ?",
                version.getId()));
        verifyNoInteractions(jimuReportGateway);
    }

    @Test
    void recognizeUploadedRoute_whenNoMainReportsButExecutionExistsWithoutUpgrade_rejectsAsExistingBatchRecord() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("无表单有执行未升版批记录");
        MesProBatchRecordVersionDO version = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "old-execution.doc", "old-execution-sha", null, null);
        definition.setCurrentVersionId(version.getId());
        definitionMapper.updateById(definition);
        insertBatchRecordExecution(definition.getId(), version.getId(), "EXEC-STALE-NO-UPGRADE");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(
                        new MockMultipartFile("file", "stale-no-upgrade.doc", "application/msword",
                                "stale-no-upgrade".getBytes(StandardCharsets.UTF_8)),
                        MesProBatchRecordRecognitionRouteKeys.B, "无表单有执行未升版批记录", false,
                        List.of("无表单有执行未升版批记录")));

        assertEquals(PRO_BATCH_RECORD_REPORT_VERSION_RESET_BLOCKED.getCode(), exception.getCode());
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_definition WHERE id = ?",
                definition.getId()));
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_version WHERE id = ?",
                version.getId()));
        verifyNoInteractions(jimuReportGateway);
    }

    @Test
    void recognizeUploadedRoute_whenNoMainReportsButExecutionExists_rejectsVersionResetBlocked() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("无表单有执行升版批记录");
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "old-execution.doc", "old-execution-sha", null, null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        insertBatchRecordExecution(definition.getId(), currentVersion.getId(), "EXEC-STALE-UPGRADE");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(
                        new MockMultipartFile("file", "stale-execution-upgrade.doc", "application/msword",
                                "stale-execution-upgrade".getBytes(StandardCharsets.UTF_8)),
                        MesProBatchRecordRecognitionRouteKeys.B, "无表单有执行升版批记录", false,
                        List.of("无表单有执行升版批记录")));

        assertEquals(PRO_BATCH_RECORD_REPORT_VERSION_RESET_BLOCKED.getCode(), exception.getCode());
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_definition WHERE id = ?",
                definition.getId()));
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_version WHERE id = ?",
                currentVersion.getId()));
        assertEquals(0, rawCount("""
                SELECT COUNT(*) FROM mes_pro_batch_record_report
                WHERE batch_record_definition_id = ?
                """, definition.getId()));
        verifyNoInteractions(jimuReportGateway);
    }

    @Test
    void recognizeUploadedRoute_whenMultipleHistoricalReferencesRemain_listsAllCleanupEntrances() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("多引用残留批记录");
        MesProBatchRecordVersionDO version = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "old-multi-ref.doc", "old-multi-ref-sha", 99101L, null);
        definition.setCurrentVersionId(version.getId());
        definitionMapper.updateById(definition);
        insertBatchRecordExecution(definition.getId(), version.getId(), "EXEC-MULTI-REF");
        insertBatchExecutionTask(definition.getId(), version.getId(), "粗洗工序");
        insertProcessFormPermissionRule(definition.getId(), version.getId(), "multi-ref-report");
        insertRouteFlowProcessBatchRecord(definition.getId(), version.getId(), "multi-ref-report");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(
                        new MockMultipartFile("file", "multi-ref.doc", "application/msword",
                                "multi-ref".getBytes(StandardCharsets.UTF_8)),
                        MesProBatchRecordRecognitionRouteKeys.B, "多引用残留批记录", false,
                        List.of("多引用残留批记录")));

        assertEquals(PRO_BATCH_RECORD_REPORT_VERSION_RESET_BLOCKED.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("V1.0"));
        assertTrue(exception.getMessage().contains("批记录执行"));
        assertTrue(exception.getMessage().contains("批次执行"));
        assertTrue(exception.getMessage().contains("删除或作废执行记录"));
        assertTrue(exception.getMessage().contains("批记录任务"));
        assertTrue(exception.getMessage().contains("批次执行任务"));
        assertTrue(exception.getMessage().contains("删除任务或解除任务中的表单"));
        assertTrue(exception.getMessage().contains("工序表单权限规则"));
        assertTrue(exception.getMessage().contains("工序表单权限设置"));
        assertTrue(exception.getMessage().contains("删除该工序的表单权限规则"));
        assertTrue(exception.getMessage().contains("工艺流程批记录绑定"));
        assertTrue(exception.getMessage().contains("工艺路线/工序配置"));
        assertTrue(exception.getMessage().contains("删除该工序上的批记录表单绑定"));
        verifyNoInteractions(jimuReportGateway);
    }

    @Test
    void recognizeUploadedRoute_whenExistingVersionWithoutMainReportsHasExecution_rejectsV1Reset() {
        byte[] uploadedBytes = "batch-only-retry-word".getBytes(StandardCharsets.UTF_8);
        String uploadedSha256 = sha256(uploadedBytes);
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("仅重建批记录重试");
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "old-route.doc", "old-route-sha", 39001L, null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        insertBatchRecordExecution(definition.getId(), currentVersion.getId(), "EXEC-BATCH-ONLY-RETRY");
        insertVersion(definition.getId(), "V2.0", "PRECHECK_PASSED",
                currentVersion.getId(), "batch-only-retry.doc", uploadedSha256, 39001L, 39001L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(
                        new MockMultipartFile("file", "batch-only-retry.doc", "application/msword", uploadedBytes),
                        MesProBatchRecordRecognitionRouteKeys.B, "仅重建批记录重试", "REBUILD_V1", null,
                        List.of("仅重建批记录重试"), true, List.of(), List.of()));

        assertEquals(PRO_BATCH_RECORD_REPORT_VERSION_RESET_BLOCKED.getCode(), exception.getCode());
        assertEquals(0, rawCount("""
                SELECT COUNT(*) FROM mes_pro_batch_record_report
                WHERE batch_record_definition_id = ?
                """, definition.getId()));
        verifyNoInteractions(jimuReportGateway);
    }
    @Test
    void recognizeUploadedRoute_whenPendingApprovalVersionExists_rejectsBeforeRecognizerAndWritesNothingNew() {
        byte[] uploadedBytes = "pending-approval-lock-new-word".getBytes(StandardCharsets.UTF_8);
        List<MesProBatchRecordParsedTable> parsedTables = uploadedRouteParsedTables(2, "Pending Lock Process ");
        when(routeRecognizer.recognize(any(), any(), any())).thenReturn(parsedTables);
        when(jimuReportGateway.ensureElectronicBatchRecordCategoryId()).thenReturn("category-ebrr");
        when(jimuReportGateway.saveOrUpdateReport(any())).thenAnswer(invocation -> {
            MesProBatchRecordJimuReportSaveReq saveReq = invocation.getArgument(0);
            return TestBatchRecordFixtures.generatedReport(
                    "pending-lock-report-" + saveReq.parsedTable().getSourceTableIndex(),
                    saveReq.reportCode(), saveReq.reportName());
        });
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("审批中锁定批记录");
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "current.doc", "sha-current", 39201L, null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        insertVersion(definition.getId(), "V2.0", "PENDING_APPROVAL",
                currentVersion.getId(), "pending.doc", "sha-pending", 39201L, 39201L);
        MesProBatchRecordReportDO currentReport = TestBatchRecordFixtures.metadataReport(
                902L, "pending-lock-current", 1, "pending-lock-current-report-1",
                "EBR_PENDING_LOCK_T01", "当前表1", "current.doc");
        currentReport.setBatchRecordName("审批中锁定批记录");
        currentReport.setBatchRecordDefinitionId(definition.getId());
        currentReport.setBatchRecordVersionId(currentVersion.getId());
        currentReport.setFormSlotType(MesProBatchRecordFormSlotType.MAIN.getType());
        currentReport.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
        reportMapper.insert(currentReport);
        when(jimuReportGateway.getReportInfo("pending-lock-current-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "pending-lock-current-report-1", "EBR_PENDING_LOCK_T01", "当前表1", LocalDateTime.now()));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(
                        new MockMultipartFile("file", "pending-lock-new.doc", "application/msword", uploadedBytes),
                        MesProBatchRecordRecognitionRouteKeys.B, "审批中锁定批记录", "UPGRADE",
                        currentVersion.getId(), "V3.0", List.of("审批中锁定批记录"), true, List.of(),
                        List.of(), 9901L));

        assertEquals(PRO_BATCH_RECORD_REPORT_VERSION_PENDING_APPROVAL_EXISTS.getCode(), exception.getCode());
        assertEquals(1, rawCount("""
                SELECT COUNT(*) FROM mes_pro_batch_record_report
                WHERE batch_record_definition_id = ?
                """, definition.getId()));
        assertEquals(2L, versionMapper.countByDefinitionId(definition.getId()));
        verify(routeRecognizer, never()).recognize(any(), any(), any());
        verify(jimuReportGateway, never()).saveOrUpdateReport(any());
    }

    @Test
    void recognizeUploadedRoute_whenMainReportsExistInPendingVersion_rejectsReimport() {
        byte[] uploadedBytes = "batch-only-reusable-product-name".getBytes(StandardCharsets.UTF_8);
        String uploadedSha256 = sha256(uploadedBytes);
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("复用待检产品名");
        MesProBatchRecordVersionDO currentVersion = insertVersion(definition.getId(), "V1.0", "APPROVED",
                null, "old.doc", "old-sha", 39101L, null);
        definition.setCurrentVersionId(currentVersion.getId());
        definitionMapper.updateById(definition);
        MesProBatchRecordVersionDO reusableVersion = insertVersion(definition.getId(), "V2.0", "PRECHECK_PASSED",
                currentVersion.getId(), "reusable.doc", uploadedSha256, 39101L, 39101L);
        MesProBatchRecordReportDO currentReport = TestBatchRecordFixtures.metadataReport(
                900L, "current-product", 1, "reusable-current-report-1",
                "EBR_CURRENT_T01", "当前表1", "old.doc");
        currentReport.setBatchRecordName("复用待检产品名");
        currentReport.setBatchRecordDefinitionId(definition.getId());
        currentReport.setBatchRecordVersionId(currentVersion.getId());
        currentReport.setFormSlotType(MesProBatchRecordFormSlotType.MAIN.getType());
        currentReport.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
        reportMapper.insert(currentReport);
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                901L, "reusable-product", 1, "reusable-product-report-1",
                "EBR_REUSE_T01", "产品信息", "reusable.doc");
        report.setBatchRecordName("复用待检产品名");
        report.setBatchRecordDefinitionId(definition.getId());
        report.setBatchRecordVersionId(reusableVersion.getId());
        report.setFormSlotType(MesProBatchRecordFormSlotType.MAIN.getType());
        report.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
        reportMapper.insert(report);
        when(jimuReportGateway.getReportInfo("reusable-current-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "reusable-current-report-1", "EBR_CURRENT_T01", "当前表1", LocalDateTime.now()));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.recognizeUploadedRoute(
                        new MockMultipartFile("file", "reusable.doc", "application/msword", uploadedBytes),
                        MesProBatchRecordRecognitionRouteKeys.B, "复用待检产品名", "UPGRADE",
                        currentVersion.getId(), List.of("复用待检产品名"), true, List.of(), List.of()));

        assertEquals(PRO_BATCH_RECORD_REPORT_FORM_SLOT_EXISTS.getCode(), exception.getCode());
        assertNull(reportMapper.selectById(report.getId()).getProductName());
        verify(jimuReportGateway, never()).saveOrUpdateReport(any());
    }
    @Test
    void deleteGeneratedReportsByBatchRecordName_skipsBoundReports() {
        MesProBatchRecordReportDO boundReport = TestBatchRecordFixtures.metadataReport(
                811L, "batch-bound-delete", 1, "batch-bound-delete-report-1", "EBR_BD_T01", "绑定表1", PILOT_FILE_NAME);
        boundReport.setBatchRecordName("绑定批记录");
        MesProBatchRecordReportDO unboundReport = TestBatchRecordFixtures.metadataReport(
                812L, "batch-bound-delete", 2, "batch-bound-delete-report-2", "EBR_BD_T02", "未绑定表2", PILOT_FILE_NAME);
        unboundReport.setBatchRecordName("绑定批记录");
        reportMapper.insert(boundReport);
        reportMapper.insert(unboundReport);
        new org.springframework.jdbc.core.JdbcTemplate(dataSource).update("""
                INSERT INTO mes_pro_route_process
                (id, route_id, process_id, sort, next_process_id, link_type, prepare_time, wait_time, color_code,
                 key_flag, check_flag, remark, batch_record_report_id, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                99001L, 99002L, 99003L, 1, null, 3, 0, 0, "#00AEF3",
                false, false, "bound-by-name", "batch-bound-delete-report-1", "tester", "tester", false, 1L);

        BatchRecordReportDeleteAllRespVO result =
                reportService.deleteGeneratedReportsByBatchRecordName("绑定批记录");

        assertEquals(1, result.getDeletedReportCount());
        assertEquals(1, result.getDeletedMetadataCount());
        assertEquals(1, result.getSkippedBoundReportCount());
        assertEquals(1, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_report WHERE report_id = ?", "batch-bound-delete-report-1"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_report WHERE report_id = ?", "batch-bound-delete-report-2"));
        verify(jimuReportGateway, never()).deleteReport("batch-bound-delete-report-1");
        verify(jimuReportGateway).deleteReport("batch-bound-delete-report-2");
    }

    @Test
    void deleteGeneratedReportsByBatchRecordName_whenForceUnbind_removesBindingsAndDeletesAllReports() {
        MesProBatchRecordReportDO routeProcessBoundReport = TestBatchRecordFixtures.metadataReport(
                821L, "batch-force-delete", 1, "batch-force-delete-report-1", "EBR_FD_T01", "强制解绑表1", PILOT_FILE_NAME);
        routeProcessBoundReport.setBatchRecordName("强制解绑批记录");
        MesProBatchRecordReportDO routeFlowBoundReport = TestBatchRecordFixtures.metadataReport(
                822L, "batch-force-delete", 2, "batch-force-delete-report-2", "EBR_FD_T02", "强制解绑表2", PILOT_FILE_NAME);
        routeFlowBoundReport.setBatchRecordName("强制解绑批记录");
        MesProBatchRecordReportDO routeFlowConfigBoundReport = TestBatchRecordFixtures.metadataReport(
                823L, "batch-force-delete", 3, "batch-force-delete-report-3", "EBR_FD_T03", "强制解绑表3", PILOT_FILE_NAME);
        routeFlowConfigBoundReport.setBatchRecordName("强制解绑批记录");
        reportMapper.insert(routeProcessBoundReport);
        reportMapper.insert(routeFlowBoundReport);
        reportMapper.insert(routeFlowConfigBoundReport);
        jdbcTemplate().update("""
                INSERT INTO mes_pro_route_process
                (id, route_id, process_id, sort, next_process_id, link_type, prepare_time, wait_time, color_code,
                 key_flag, check_flag, remark, batch_record_report_id, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                99011L, 99012L, 99013L, 1, null, 3, 0, 0, "#00AEF3",
                false, false, "force-unbind-route-process", "batch-force-delete-report-1", "tester", "tester",
                false, 1L);
        jdbcTemplate().update("""
                INSERT INTO mes_pro_route_flow_process_batch_record
                (id, route_flow_process_config_id, route_id, route_process_id, use_type, batch_record_report_id,
                 form_slot_type, record_category, validation_profile, report_sort, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                99021L, 99022L, 99012L, 99011L, MesProRouteFlowConfigTypeEnum.BATCH.getType(),
                "batch-force-delete-report-2", MesProBatchRecordFormSlotType.MAIN.getType(), "BATCH_RECORD",
                "CONTROLLED_BATCH", 1, "tester", "tester", false, 1L);
        jdbcTemplate().update("""
                INSERT INTO mes_pro_route_flow_process_config
                (id, route_flow_config_id, route_id, route_process_id, use_type, enabled, execution_mode,
                 batch_record_report_id, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                99031L, 99032L, 99012L, 99011L, MesProRouteFlowConfigTypeEnum.BATCH.getType(), true, "SEQUENTIAL",
                "batch-force-delete-report-3", "tester", "tester", false, 1L);

        BatchRecordReportDeleteAllRespVO result =
                reportService.deleteGeneratedReportsByBatchRecordName("强制解绑批记录", true);

        assertEquals(3, result.getDeletedReportCount());
        assertEquals(3, result.getDeletedMetadataCount());
        assertEquals(0, result.getSkippedBoundReportCount());
        assertEquals(1, result.getUnboundRouteProcessCount());
        assertEquals(1, result.getDeletedRouteFlowBindingCount());
        assertEquals(1, result.getUnboundRouteFlowProcessConfigCount());
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_batch_record_report WHERE batch_record_name = ?", "强制解绑批记录"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_route_process WHERE batch_record_report_id IN (?, ?, ?)",
                "batch-force-delete-report-1", "batch-force-delete-report-2", "batch-force-delete-report-3"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_route_flow_process_batch_record WHERE batch_record_report_id IN (?, ?, ?)",
                "batch-force-delete-report-1", "batch-force-delete-report-2", "batch-force-delete-report-3"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_route_flow_process_config WHERE batch_record_report_id IN (?, ?, ?)",
                "batch-force-delete-report-1", "batch-force-delete-report-2", "batch-force-delete-report-3"));
        verify(jimuReportGateway).deleteReport("batch-force-delete-report-1");
        verify(jimuReportGateway).deleteReport("batch-force-delete-report-2");
        verify(jimuReportGateway).deleteReport("batch-force-delete-report-3");
    }

    @Test
    void deleteGeneratedReportsByBatchRecordName_rejectsBlankName() {
        assertServiceException(() -> reportService.deleteGeneratedReportsByBatchRecordName(" "),
                PRO_BATCH_RECORD_REPORT_BATCH_NAME_EMPTY);
        verifyNoInteractions(jimuReportGateway);
    }

    @Test
    void getGeneratedReportPage_filtersByRouteKey() {
        MesProBatchRecordReportDO routeAReport = TestBatchRecordFixtures.metadataReport(
                1L, "fixed-doc", 1, "route-a-report-1", "EBR_A_T01", "A-表1", PILOT_FILE_NAME);
        routeAReport.setRouteKey(MesProBatchRecordRecognitionRouteKeys.A);
        MesProBatchRecordReportDO routeBReport = TestBatchRecordFixtures.metadataReport(
                2L, "fixed-doc", 1, "route-b-report-1", "EBR_B_T01", "B-表1", PILOT_FILE_NAME);
        routeBReport.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
        reportMapper.insert(routeAReport);
        reportMapper.insert(routeBReport);
        when(jimuReportGateway.getReportInfo("route-a-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "route-a-report-1", "EBR_A_T01", "A-表1", LocalDateTime.now()));
        when(jimuReportGateway.getReportInfo("route-b-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "route-b-report-1", "EBR_B_T01", "B-表1", LocalDateTime.now()));

        BatchRecordReportPageReqVO pageReqVO = new BatchRecordReportPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(20);
        pageReqVO.setRouteKey(MesProBatchRecordRecognitionRouteKeys.A);

        PageResult<MesProBatchRecordReportView> pageResult = reportService.getGeneratedReportPage(pageReqVO);

        assertEquals(1L, pageResult.getTotal());
        assertEquals(MesProBatchRecordRecognitionRouteKeys.A, pageResult.getList().get(0).routeKey());
        assertEquals("route-a-report-1", pageResult.getList().get(0).reportId());
    }

    @Test
    void getGeneratedReportPage_filtersByBatchRecordName() {
        MesProBatchRecordReportDO first = TestBatchRecordFixtures.metadataReport(
                11L, "batch-name-a", 1, "batch-a-report-1", "EBR_BA_T01", "批记录A表1", PILOT_FILE_NAME);
        first.setBatchRecordName("批记录A");
        MesProBatchRecordReportDO second = TestBatchRecordFixtures.metadataReport(
                12L, "batch-name-b", 1, "batch-b-report-1", "EBR_BB_T01", "批记录B表1", PILOT_FILE_NAME);
        second.setBatchRecordName("批记录B");
        reportMapper.insert(first);
        reportMapper.insert(second);
        when(jimuReportGateway.getReportInfo("batch-a-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "batch-a-report-1", "EBR_BA_T01", "批记录A表1", LocalDateTime.now()));
        when(jimuReportGateway.getReportInfo("batch-b-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "batch-b-report-1", "EBR_BB_T01", "批记录B表1", LocalDateTime.now()));

        BatchRecordReportPageReqVO pageReqVO = new BatchRecordReportPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(20);
        pageReqVO.setBatchRecordName("批记录A");

        PageResult<MesProBatchRecordReportView> pageResult = reportService.getGeneratedReportPage(pageReqVO);

        assertEquals(1L, pageResult.getTotal());
        assertEquals("批记录A", pageResult.getList().get(0).batchRecordName());
        assertEquals("batch-a-report-1", pageResult.getList().get(0).reportId());
    }

    @Test
    void getGeneratedReportPage_expandsVersionRouteProductsIntoRowsAndKeepsBlankWhenUnbound() {
        MesProBatchRecordDefinitionDO definition = insertVersionedDefinition("拆行批记录");
        Long firstItemId = seedProductItem("产品A", "PRD-A");
        Long secondItemId = seedProductItem("产品B", "PRD-B");
        Long routeId = 930001L;
        jdbcTemplate().update("""
                INSERT INTO mes_pro_route
                (id, code, name, status, remark, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                routeId, "ROUTE-FORM-LIST", "拆行路线", CommonStatusEnum.ENABLE.getStatus(),
                "批记录表单拆行测试", "tester", "tester", false, 1L);
        jdbcTemplate().update("""
                INSERT INTO mes_pro_route_product
                (id, route_id, item_id, quantity, production_time, time_unit_type, remark, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                930101L, routeId, firstItemId, 1, BigDecimal.ONE, "MINUTE", "产品A", "tester", "tester", false, 1L);
        jdbcTemplate().update("""
                INSERT INTO mes_pro_route_product
                (id, route_id, item_id, quantity, production_time, time_unit_type, remark, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                930102L, routeId, secondItemId, 1, BigDecimal.ONE, "MINUTE", "产品B", "tester", "tester", false, 1L);
        MesProBatchRecordVersionDO version = insertVersion(definition.getId(), "V2.0", "APPROVED",
                null, PILOT_FILE_NAME, "hash-split-product", routeId, null);
        MesProBatchRecordReportDO productReport = TestBatchRecordFixtures.metadataReport(
                71L, "split-product", 1, "split-product-report-1", "EBR_SPLIT_T01", "拆行表单", PILOT_FILE_NAME);
        productReport.setBatchRecordName("拆行批记录");
        productReport.setBatchRecordDefinitionId(definition.getId());
        productReport.setBatchRecordVersionId(version.getId());
        productReport.setFormSlotType(MesProBatchRecordFormSlotType.MAIN.getType());
        reportMapper.insert(productReport);
        MesProBatchRecordReportDO unboundReport = TestBatchRecordFixtures.metadataReport(
                72L, "split-unbound", 2, "split-unbound-report-1", "EBR_SPLIT_T02", "无产品表单", PILOT_FILE_NAME);
        unboundReport.setBatchRecordName("无绑定批记录");
        reportMapper.insert(unboundReport);
        when(jimuReportGateway.getReportInfo("split-product-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "split-product-report-1", "EBR_SPLIT_T01", "拆行表单", LocalDateTime.now()));
        when(jimuReportGateway.getReportInfo("split-unbound-report-1"))
                .thenReturn(TestBatchRecordFixtures.reportInfo(
                        "split-unbound-report-1", "EBR_SPLIT_T02", "无产品表单", LocalDateTime.now()));

        BatchRecordReportPageReqVO pageReqVO = new BatchRecordReportPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(20);

        PageResult<MesProBatchRecordReportView> pageResult = reportService.getGeneratedReportPage(pageReqVO);

        assertEquals(3L, pageResult.getTotal());
        List<String> productNames = pageResult.getList().stream()
                .filter(row -> "split-product-report-1".equals(row.reportId()))
                .map(MesProBatchRecordReportView::productName)
                .toList();
        assertEquals(List.of("产品A", "产品B"), productNames);
        MesProBatchRecordReportView firstProductRow = pageResult.getList().stream()
                .filter(row -> "产品A".equals(row.productName()))
                .findFirst()
                .orElseThrow();
        assertEquals("V2.0", firstProductRow.versionNo());
        assertEquals("APPROVED", firstProductRow.versionStatus());
        assertEquals(MesProBatchRecordFormSlotType.MAIN.getType(), firstProductRow.formSlotType());
        MesProBatchRecordReportView unboundRow = pageResult.getList().stream()
                .filter(row -> "split-unbound-report-1".equals(row.reportId()))
                .findFirst()
                .orElseThrow();
        assertNull(unboundRow.productName());
    }

    @Test
    void getBatchRecordNameOptions_returnsDistinctSortedNames() {
        MesProBatchRecordReportDO defaultName = TestBatchRecordFixtures.metadataReport(
                21L, "batch-default", 1, "batch-default-report-1", "EBR_DEF_T01", "默认表1", PILOT_FILE_NAME);
        defaultName.setBatchRecordName("棘突球囊");
        MesProBatchRecordReportDO nameB = TestBatchRecordFixtures.metadataReport(
                22L, "batch-b", 1, "batch-b-report-1", "EBR_B_T01", "B表1", PILOT_FILE_NAME);
        nameB.setBatchRecordName("批记录B");
        MesProBatchRecordReportDO nameA = TestBatchRecordFixtures.metadataReport(
                23L, "batch-a", 1, "batch-a-report-1", "EBR_A_T01", "A表1", PILOT_FILE_NAME);
        nameA.setBatchRecordName("批记录A");
        reportMapper.insert(defaultName);
        reportMapper.insert(nameB);
        reportMapper.insert(nameA);

        List<String> names = reportService.getBatchRecordNameOptions();

        assertEquals(List.of("批记录A", "批记录B", "棘突球囊"), names);
    }

    @Test
    void getGeneratedReportPage_hidesMetadataWhoseJimuReportHasBeenCleared() {
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                31L, "sample-cleared", 13, "cleared-report-13", "EBR_A_T13", "单包装工序生产记录", PILOT_FILE_NAME);
        report.setReportCategoryId("category-ebrr");
        reportMapper.insert(report);
        when(jimuReportGateway.getReportInfo("cleared-report-13")).thenReturn(null);

        BatchRecordReportPageReqVO pageReqVO = new BatchRecordReportPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(20);

        PageResult<MesProBatchRecordReportView> pageResult = reportService.getGeneratedReportPage(pageReqVO);

        assertEquals(0L, pageResult.getTotal());
        assertTrue(pageResult.getList().isEmpty());
    }

    @Test
    void getAndSaveCellRules_suggestsAndPersistsReviewedTypedMetadata() {
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                41L, "sample-cell-rules", 1, "cell-rule-report-1", "EBR_RULE_T01", "规则表", PILOT_FILE_NAME);
        reportMapper.insert(report);
        AtomicReference<String> reportJson = new AtomicReference<>(sampleCellRuleReportJson());
        when(jimuReportGateway.getReportJson("cell-rule-report-1")).thenAnswer(invocation -> reportJson.get());
        org.mockito.Mockito.doAnswer(invocation -> {
            reportJson.set(invocation.getArgument(1));
            return null;
        }).when(jimuReportGateway).updateReportJson(eq("cell-rule-report-1"), any());

        BatchRecordReportCellRulesRespVO initial = reportService.getCellRules("cell-rule-report-1");

        assertEquals("cell-rule-report-1", initial.getReportId());
        assertEquals(2, initial.getSuggestions().size());
        assertEquals(2, initial.getUnreviewedFillableCellCount());
        assertEquals("NUMBER", initial.getSuggestions().get(0).getValueType());
        assertEquals("g", initial.getSuggestions().get(0).getUnit());
        assertEquals("DATE", initial.getSuggestions().get(1).getValueType());

        BatchRecordReportCellRulesRespVO saved = reportService.saveCellRules(new BatchRecordReportCellRulesReqVO()
                .setReportId("cell-rule-report-1")
                .setRules(List.of(
                        new BatchRecordReportCellRuleVO()
                                .setRowIndex(0)
                                .setColumnIndex(1)
                                .setValueType("NUMBER")
                                .setComponentFlag("input-number")
                                .setRequired(true)
                                .setLabel("重量")
                                .setConstraints(Map.of("min", 0, "max", 100, "scale", 2))
                                .setUnit("g")
                                .setSource("MANUAL")
                                .setConfidence(1.0)
                                .setReviewed(true),
                        new BatchRecordReportCellRuleVO()
                                .setRowIndex(1)
                                .setColumnIndex(1)
                                .setValueType("DATE")
                                .setComponentFlag("date")
                                .setRequired(true)
                                .setLabel("生产日期")
                                .setConstraints(Map.of("format", "yyyy-MM-dd"))
                                .setSource("MANUAL")
                                .setConfidence(1.0)
                                .setReviewed(true))));

        assertEquals(0, saved.getUnreviewedFillableCellCount());
        assertEquals(2, saved.getRules().size());
        assertTrue(reportJson.get().contains("\"edhrCellRule\""));
        assertTrue(reportJson.get().contains("\"valueType\":\"NUMBER\""));
        assertTrue(reportJson.get().contains("\"unit\":\"g\""));
    }

    @Test
    void saveCellRules_createsAndRemovesManualFillFormForPlainRealCell() {
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                44L, "sample-plain-cell-rule", 1, "plain-cell-rule-report-1", "EBR_RULE_T04",
                "普通真实格规则表", PILOT_FILE_NAME);
        reportMapper.insert(report);
        AtomicReference<String> reportJson = new AtomicReference<>(samplePlainCellRuleReportJson());
        when(jimuReportGateway.getReportJson("plain-cell-rule-report-1")).thenAnswer(invocation -> reportJson.get());
        org.mockito.Mockito.doAnswer(invocation -> {
            reportJson.set(invocation.getArgument(1));
            return null;
        }).when(jimuReportGateway).updateReportJson(eq("plain-cell-rule-report-1"), any());

        BatchRecordReportCellRulesRespVO saved = reportService.saveCellRules(new BatchRecordReportCellRulesReqVO()
                .setReportId("plain-cell-rule-report-1")
                .setRules(List.of(new BatchRecordReportCellRuleVO()
                        .setRowIndex(0)
                        .setColumnIndex(1)
                        .setValueType("STRING")
                        .setComponentFlag("input-text")
                        .setRequired(false)
                        .setLabel("")
                        .setSource("MANUAL")
                        .setConfidence(1.0)
                        .setReviewed(true))));

        assertEquals(1, saved.getRules().size());
        JSONObject savedRoot = JSONObject.parseObject(reportJson.get());
        JSONObject savedCell = savedRoot.getJSONObject("rows")
                .getJSONObject("0").getJSONObject("cells").getJSONObject("1");
        assertTrue(savedCell.containsKey("edhrCellRule"));
        JSONObject manualFillForm = savedCell.getJSONObject("fillForm");
        assertEquals("Input", manualFillForm.getString("component"));
        assertEquals("input-text", manualFillForm.getString("componentFlag"));
        assertEquals("ebr_EBR_RULE_T04_r0_c1", manualFillForm.getString("field"));
        assertTrue(manualFillForm.getBooleanValue("edhrManualFillCell"));

        BatchRecordReportCellRulesRespVO cleared = reportService.saveCellRules(new BatchRecordReportCellRulesReqVO()
                .setReportId("plain-cell-rule-report-1")
                .setRules(List.of()));

        assertEquals(0, cleared.getRules().size());
        JSONObject clearedRoot = JSONObject.parseObject(reportJson.get());
        JSONObject clearedCell = clearedRoot.getJSONObject("rows")
                .getJSONObject("0").getJSONObject("cells").getJSONObject("1");
        assertTrue(!clearedCell.containsKey("edhrCellRule"));
        assertTrue(!clearedCell.containsKey("fillForm"));
    }

    @Test
    void getCellRulesAndSignatureMarkers_returnRenderableSheetLayoutJson() {
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                45L, "sample-renderable-layout", 1, "renderable-layout-report-1", "EBR_LAYOUT_T01",
                "可渲染布局表", PILOT_FILE_NAME);
        reportMapper.insert(report);
        when(jimuReportGateway.getReportJson("renderable-layout-report-1")).thenReturn(sampleCellRuleReportJson());

        BatchRecordReportCellRulesRespVO cellRules =
                reportService.getCellRules("renderable-layout-report-1");
        BatchRecordReportSignatureCellMarkersRespVO markers =
                reportService.getSignatureCellMarkers("renderable-layout-report-1");

        JSONObject cellRuleLayout = JSONObject.parseObject(cellRules.getSheetLayoutJson());
        JSONObject markerLayout = JSONObject.parseObject(markers.getSheetLayoutJson());
        assertTrue(hasRenderableRows(cellRuleLayout));
        assertTrue(hasRenderableRows(markerLayout));
        assertEquals("重量（g）", cellRuleLayout.getJSONObject("rows")
                .getJSONObject("0").getJSONObject("cells").getJSONObject("0").getString("text"));
        assertEquals(2, cellRules.getSuggestions().size());
    }

    @Test
    void getCellRules_returnsStructuredHeaderBlankSuggestions() {
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                47L, "sample-structured-header-layout", 1, "structured-header-rule-1", "EBR_LAYOUT_T03",
                "结构化灰色格表", PILOT_FILE_NAME);
        reportMapper.insert(report);
        when(jimuReportGateway.getReportJson("structured-header-rule-1"))
                .thenReturn(sampleStructuredHeaderBlankRuleReportJson());

        BatchRecordReportCellRulesRespVO cellRules =
                reportService.getCellRules("structured-header-rule-1");

        assertEquals(3, cellRules.getSuggestions().size());
        assertEquals("BOOLEAN", cellRules.getSuggestions().get(0).getValueType());
        assertEquals("操作人", cellRules.getSuggestions().get(1).getLabel());
        assertEquals("复核人", cellRules.getSuggestions().get(2).getLabel());
    }

    @Test
    void getCellRules_whenLegacyLossReportJsonHasMergedBody_failsFastWithoutRuntimeUpgrade() {
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                48L, "sample-legacy-loss-report", 1, "legacy-loss-report-1", "EBR_LOSS_LEGACY_T01",
                "损耗单", PILOT_FILE_NAME);
        report.setFormSlotType(MesProBatchRecordFormSlotType.LOSS_REPORT.getType());
        reportMapper.insert(report);
        when(jimuReportGateway.getReportJson("legacy-loss-report-1"))
                .thenReturn(sampleLegacyLossReportMergedBodyJson());
        org.mockito.Mockito.doAnswer(invocation -> {
            throw new AssertionError("getCellRules must not rewrite legacy Jimu reports on read");
        }).when(jimuReportGateway).saveOrUpdateReport(any());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.getCellRules("legacy-loss-report-1"));

        assertEquals(1_040_509_069, exception.getCode());
        assertTrue(exception.getMessage().contains("旧布局"));
        verify(jimuReportGateway, never()).saveOrUpdateReport(any());
        verify(jimuReportGateway, never()).updateReportJson(eq("legacy-loss-report-1"), any());
    }

    @Test
    void getCellRules_whenLegacyLossReportJsonHasVerticalBody_failsFastWithoutRuntimeUpgrade() {
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                49L, "sample-legacy-vertical-loss-report", 1, "legacy-loss-report-vertical-1",
                "EBR_LOSS_LEGACY_VERTICAL_T01", "损耗单", PILOT_FILE_NAME);
        report.setFormSlotType(MesProBatchRecordFormSlotType.LOSS_REPORT.getType());
        reportMapper.insert(report);
        when(jimuReportGateway.getReportJson("legacy-loss-report-vertical-1"))
                .thenReturn(sampleLegacyLossReportVerticalBodyJson());
        org.mockito.Mockito.doAnswer(invocation -> {
            throw new AssertionError("getCellRules must not rewrite legacy Jimu reports on read");
        }).when(jimuReportGateway).saveOrUpdateReport(any());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.getCellRules("legacy-loss-report-vertical-1"));

        assertEquals(1_040_509_069, exception.getCode());
        assertTrue(exception.getMessage().contains("旧布局"));
        verify(jimuReportGateway, never()).saveOrUpdateReport(any());
        verify(jimuReportGateway, never()).updateReportJson(eq("legacy-loss-report-vertical-1"), any());
    }

    @Test
    void getCellRules_rejectsRowsWithoutRenderableCells() {
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                46L, "sample-empty-layout", 1, "empty-layout-report-1", "EBR_LAYOUT_T02",
                "空布局表", PILOT_FILE_NAME);
        reportMapper.insert(report);
        when(jimuReportGateway.getReportJson("empty-layout-report-1")).thenReturn("""
                {"name":"empty-layout-demo","rows":{"len":100},"cols":{"len":20},"merges":[]}
                """);

        ServiceException cellRuleException = assertThrows(ServiceException.class,
                () -> reportService.getCellRules("empty-layout-report-1"));
        assertEquals(PRO_BATCH_RECORD_REPORT_JSON_INVALID.getCode(), cellRuleException.getCode());
        assertTrue(cellRuleException.getMessage().contains("missing renderable rows"));
        ServiceException markerException = assertThrows(ServiceException.class,
                () -> reportService.getSignatureCellMarkers("empty-layout-report-1"));
        assertEquals(PRO_BATCH_RECORD_REPORT_JSON_INVALID.getCode(), markerException.getCode());
        assertTrue(markerException.getMessage().contains("missing renderable rows"));
    }

    @Test
    void getAndSaveSignatureMarkers_persistsSingleUserAndMultiRoleSources() {
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                43L, "sample-signature-markers", 1, "signature-marker-report-1", "EBR_SIGN_T01", "签名表", PILOT_FILE_NAME);
        reportMapper.insert(report);
        AtomicReference<String> reportJson = new AtomicReference<>(sampleSignatureMarkerReportJson());
        when(jimuReportGateway.getReportJson("signature-marker-report-1")).thenAnswer(invocation -> reportJson.get());
        org.mockito.Mockito.doAnswer(invocation -> {
            reportJson.set(invocation.getArgument(1));
            return null;
        }).when(jimuReportGateway).updateReportJson(eq("signature-marker-report-1"), any());

        BatchRecordReportSignatureCellMarkersRespVO saved = reportService.saveSignatureCellMarkers(
                new BatchRecordReportSignatureCellMarkersReqVO()
                        .setReportId("signature-marker-report-1")
                        .setMarkers(List.of(
                                new BatchRecordReportSignatureCellMarkerVO()
                                        .setRowIndex(0)
                                        .setColumnIndex(1)
                                        .setEnabled(true)
                                        .setActionType("APPROVE")
                                        .setReviewSourceType("USER")
                                        .setReviewSourceId(88L)
                                        .setReviewSourceName("张三"),
                                new BatchRecordReportSignatureCellMarkerVO()
                                        .setRowIndex(1)
                                        .setColumnIndex(1)
                                        .setEnabled(true)
                                        .setActionType("APPROVE")
                                        .setReviewSourceType("ROLES")
                                        .setReviewSourceIds(List.of(7001L, 7002L))
                                        .setReviewSourceName("QA 角色、生产经理"))));

        assertEquals(2, saved.getMarkers().size());
        assertEquals("USER", saved.getMarkers().get(0).getReviewSourceType());
        assertEquals(88L, saved.getMarkers().get(0).getReviewSourceId());
        assertEquals("ROLES", saved.getMarkers().get(1).getReviewSourceType());
        assertEquals(List.of(7001L, 7002L), saved.getMarkers().get(1).getReviewSourceIds());
        assertTrue(reportJson.get().contains("\"reviewSourceType\":\"USER\""));
        assertTrue(reportJson.get().contains("\"reviewSourceIds\":[7001,7002]"));
    }

    @Test
    void saveSignatureMarkers_rejectsEmptyMultiSourceIdsWithoutUpdatingJson() {
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                44L, "sample-signature-markers-invalid", 1, "signature-marker-report-invalid", "EBR_SIGN_T02", "签名表", PILOT_FILE_NAME);
        reportMapper.insert(report);
        when(jimuReportGateway.getReportJson("signature-marker-report-invalid")).thenReturn(sampleSignatureMarkerReportJson());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.saveSignatureCellMarkers(new BatchRecordReportSignatureCellMarkersReqVO()
                        .setReportId("signature-marker-report-invalid")
                        .setMarkers(List.of(new BatchRecordReportSignatureCellMarkerVO()
                                .setRowIndex(0)
                                .setColumnIndex(1)
                                .setEnabled(true)
                                .setActionType("APPROVE")
                                .setReviewSourceType("USERS")
                                .setReviewSourceIds(List.of())))));
        assertEquals(PRO_BATCH_RECORD_REPORT_SIGNATURE_REVIEW_SOURCE_REQUIRED.getCode(), exception.getCode());

        verify(jimuReportGateway, never()).updateReportJson(eq("signature-marker-report-invalid"), any());
    }

    @Test
    void saveCellRules_rejectsInvalidCellRuleWithoutUpdatingJson() {
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                42L, "sample-cell-rules-invalid", 1, "cell-rule-report-invalid", "EBR_RULE_T02", "规则表", PILOT_FILE_NAME);
        reportMapper.insert(report);
        when(jimuReportGateway.getReportJson("cell-rule-report-invalid")).thenReturn(sampleCellRuleReportJson());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> reportService.saveCellRules(new BatchRecordReportCellRulesReqVO()
                        .setReportId("cell-rule-report-invalid")
                        .setRules(List.of(new BatchRecordReportCellRuleVO()
                                .setRowIndex(0)
                                .setColumnIndex(1)
                                .setValueType("MONEY")
                                .setComponentFlag("input-number")
                                .setRequired(true)
                                .setReviewed(true)))));
        assertEquals(PRO_BATCH_RECORD_REPORT_CELL_RULE_INVALID.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("unsupported valueType MONEY"));

        verify(jimuReportGateway, org.mockito.Mockito.never()).updateReportJson(eq("cell-rule-report-invalid"), any());
    }

    private List<MesProBatchRecordParsedTable> uploadedRouteParsedTables(int count, String processNamePrefix) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> TestBatchRecordFixtures.parsedTable(index,
                        index == 1 ? "产品信息" : processNamePrefix + index))
                .toList();
    }

    private MesProBatchRecordParsedTable createLossReportSourceTable() {
        return MesProBatchRecordParsedTable.builder()
                .sourceTableIndex(1)
                .tableTitle("生产过程损耗报告单")
                .rowCount(3)
                .columnCount(8)
                .rows(List.of(
                        List.of(
                                lossReportCell("产品名称"), lossReportCell(""),
                                lossReportCell("型号规格"), lossReportCell(""),
                                lossReportCell("批号"), lossReportCell(""),
                                lossReportCell("生产数量"), lossReportCell("")),
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("""
                                        损耗描述：
                                        不合格日期
                                        工序名称
                                        不合格数量
                                        不合格原因
                                        处置方式
                                        生产人员/日期
                                        检验人员
                                        确认/日期

                                        □报废   □其他：______________

                                        □报废   □其他：______________

                                        □报废   □其他：______________

                                        □报废   □其他：______________

                                        □报废   □其他：______________

                                        □报废   □其他：______________

                                        □报废   □其他：______________

                                        □报废   □其他：______________""")
                                .colSpan(8)
                                .rowSpan(1)
                                .widthPx(960)
                                .heightPx(220)
                                .build()),
                        List.of(MesProBatchRecordParsedCell.builder()
                                .text("批准人/日期：")
                                .colSpan(8)
                                .rowSpan(1)
                                .widthPx(960)
                                .heightPx(48)
                                .build())
                ))
                .build();
    }

    private MesProBatchRecordParsedCell lossReportCell(String text) {
        return MesProBatchRecordParsedCell.builder()
                .text(text)
                .rowSpan(1)
                .colSpan(1)
                .widthPx(120)
                .heightPx(29)
                .build();
    }

    private void assertNoGeneratedRouteData() {
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_route_flow_process_batch_record"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_route_flow_process_config"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_route_flow_config"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_route_product"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_route_process"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_route"));
        assertEquals(0, rawCount("SELECT COUNT(*) FROM mes_pro_process"));
    }

    private Long seedWorkOrderProduct(String productName, String productCode) {
        long itemId = ++productItemIdSequence;
        long workOrderId = ++productWorkOrderIdSequence;
        jdbcTemplate().update("""
                INSERT INTO mes_md_item
                (id, code, name, specification, unit_measure_id, item_type_id, status, safe_stock_flag,
                 min_stock, max_stock, high_value, batch_flag, remark, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                itemId, productCode, productName, "测试规格", null, null, CommonStatusEnum.ENABLE.getStatus(),
                false, null, null, false, true, "测试产品", "tester", "tester", false);
        jdbcTemplate().update("""
                INSERT INTO mes_pro_work_order
                (id, code, name, type, order_source_type, order_source_code, product_id, quantity,
                 quantity_produced, quantity_changed, quantity_scheduled, client_id, vendor_id, batch_code,
                 workshop_name, bom_version, pick_mode, auxiliary_code, business_status, drawing_number,
                 schedule_status, parent_id, status, temporary_frozen, remark, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                workOrderId, "WO-" + productCode, "工单-" + productCode, 1, 1, "SRC-" + productCode, itemId, 1,
                0, 0, 0, null, null, "BATCH-" + productCode, "测试车间", "V1", null, null, null, null, null,
                0L, 1, false, "测试工单", "tester", "tester", false, 1L);
        return itemId;
    }

    private Long seedProductItem(String productName, String productCode) {
        long itemId = ++productItemIdSequence;
        jdbcTemplate().update("""
                INSERT INTO mes_md_item
                (id, code, name, specification, unit_measure_id, item_type_id, status, safe_stock_flag,
                 min_stock, max_stock, high_value, batch_flag, remark, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                itemId, productCode, productName, "测试规格", null, null, CommonStatusEnum.ENABLE.getStatus(),
                false, null, null, false, true, "批记录表单测试产品", "tester", "tester", false);
        return itemId;
    }

    private void seedDccProjectCode(String projectName, String projectCode) {
        jdbcTemplate().update("""
                INSERT INTO dcc_project_code
                (project_name, project_code, status, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                projectName, projectCode, "ENABLE", "tester", "tester", false, 1L);
    }

    private void insertAuxiliarySlotReport(String batchRecordName, MesProBatchRecordFormSlotType slotType,
                                           String reportCode) {
        MesProBatchRecordReportDO report = TestBatchRecordFixtures.metadataReport(
                null, "GOV-" + reportCode, 1, "governance-" + reportCode,
                reportCode, reportCode, "governance.doc");
        report.setBatchRecordName(batchRecordName);
        report.setRouteKey(MesProBatchRecordRecognitionRouteKeys.B);
        report.setFormSlotType(slotType.getType());
        reportMapper.insert(report);
    }

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }

    private int rawCount(String sql, Object... args) {
        return jdbcTemplate().queryForObject(sql, Integer.class, args);
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is required by the test JVM", exception);
        }
    }

    private boolean hasRenderableRows(JSONObject layout) {
        JSONObject rows = layout == null ? null : layout.getJSONObject("rows");
        if (rows == null) {
            return false;
        }
        return rows.keySet().stream()
                .filter(cn.hutool.core.util.StrUtil::isNumeric)
                .map(rows::getJSONObject)
                .anyMatch(row -> row != null && row.getJSONObject("cells") != null
                        && !row.getJSONObject("cells").isEmpty());
    }

    private MesProBatchRecordDefinitionDO insertVersionedDefinition(String batchRecordName) {
        return insertDefinition(batchRecordName, MesProBatchRecordRecognitionRouteKeys.B);
    }

    private MesProBatchRecordDefinitionDO insertDefinition(String batchRecordName, String routeKey) {
        MesProBatchRecordDefinitionDO definition = MesProBatchRecordDefinitionDO.builder()
                .batchRecordName(batchRecordName)
                .routeKey(routeKey)
                .build();
        definitionMapper.insert(definition);
        return definition;
    }

    private MesProBatchRecordVersionDO insertVersion(Long definitionId, String versionNo, String status,
                                                     Long sourceVersionId, String sourceFileName,
                                                     String sourceFileSha256, Long routeId, Long sourceRouteId) {
        MesProBatchRecordVersionDO version = MesProBatchRecordVersionDO.builder()
                .definitionId(definitionId)
                .versionNo(versionNo)
                .status(status)
                .sourceVersionId(sourceVersionId)
                .sourceFileName(sourceFileName)
                .sourceFileSha256(sourceFileSha256)
                .routeId(routeId)
                .sourceRouteId(sourceRouteId)
                .build();
        versionMapper.insert(version);
        return version;
    }

    private void insertBatchRecordExecution(Long definitionId, Long versionId, String executionCode) {
        jdbcTemplate().update("""
                INSERT INTO mes_pro_batch_record_execution
                (execution_code, work_order_id, work_order_code, batch_record_definition_id,
                 batch_record_version_id, form_slot_type, record_category, validation_profile,
                 batch_code, status, sheet_layout_json, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                executionCode, 990001L, "WO-" + executionCode, definitionId, versionId,
                MesProBatchRecordFormSlotType.MAIN.getType(), "BATCH_RECORD", "CONTROLLED_BATCH",
                "BATCH-" + executionCode, 0, "{\"rows\":{}}", "tester", "tester", false, 1L);
    }

    private void insertBatchExecutionTask(Long definitionId, Long versionId, String processName) {
        jdbcTemplate().update("""
                INSERT INTO mes_pro_edhr_batch_execution_task
                (batch_execution_id, node_type, route_process_id, root_process_flag, route_process_sort,
                 process_id, process_name, batch_record_report_id, batch_record_definition_id,
                 batch_record_version_id, form_slot_type, batch_record_sort, execution_mode,
                 record_category, validation_profile, required_policy, owner_role_key,
                 archive_visibility, status, required_flag, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                88001L, "PROCESS", 88101L, true, 1, 88201L, processName, "multi-ref-report",
                definitionId, versionId, MesProBatchRecordFormSlotType.MAIN.getType(), 1, "SEQUENTIAL",
                "BATCH_RECORD", "CONTROLLED_BATCH", "REQUIRED", "PRODUCTION", "FINAL_DHR", 0, true,
                "tester", "tester", false, 1L);
    }

    private void insertProcessFormPermissionRule(Long definitionId, Long versionId, String reportId) {
        jdbcTemplate().update("""
                INSERT INTO mes_pro_edhr_process_form_permission_rule
                (route_process_id, batch_record_report_id, batch_record_definition_id, batch_record_version_id,
                 rule_type, signature_cell_key, signature_role, candidate_source_type, candidate_source_ids,
                 completion_policy, due_minutes, enabled, creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                88301L, reportId, definitionId, versionId, "FILL", "", "PRODUCTION",
                "ROLE", "1", "BEFORE_SUBMIT", 60, true, "tester", "tester", false, 1L);
    }

    private void insertRouteFlowProcessBatchRecord(Long definitionId, Long versionId, String reportId) {
        jdbcTemplate().update("""
                INSERT INTO mes_pro_route_flow_process_batch_record
                (route_flow_process_config_id, route_id, route_process_id, use_type, batch_record_report_id,
                 batch_record_definition_id, batch_record_version_id, form_slot_type, record_category,
                 validation_profile, required_policy, owner_role_key, archive_visibility, report_sort,
                 creator, updater, deleted, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                88401L, 88402L, 88403L, MesProRouteFlowConfigTypeEnum.BATCH.getType(), reportId,
                definitionId, versionId, MesProBatchRecordFormSlotType.MAIN.getType(), "BATCH_RECORD",
                "CONTROLLED_BATCH", "REQUIRED", "PRODUCTION", "FINAL_DHR", 1,
                "tester", "tester", false, 1L);
    }

    private String sampleCellRuleReportJson() {
        return """
                {
                  "name":"cell-rule-demo",
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"重量（g）"},
                        "1":{"text":"","fillForm":{"field":"ebr_rule_r0_c1","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}
                      },
                      "height":24
                    },
                    "1":{
                      "cells":{
                        "0":{"text":"生产日期"},
                        "1":{"text":"","fillForm":{"field":"ebr_rule_r1_c1","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}
                      },
                      "height":24
                    }
                  },
                  "cols":{"0":{"width":100},"1":{"width":160},"len":2},
                  "merges":[],
                  "fillFormInfo":{"layout":{"direction":"horizontal","width":160,"height":32}},
                  "printConfig":{"paper":"A4"},
                  "dataRectWidth":260
                }
                """;
    }

    private String samplePlainCellRuleReportJson() {
        return """
                {
                  "name":"plain-cell-rule-demo",
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"普通字段"},
                        "1":{"text":""}
                      },
                      "height":24
                    }
                  },
                  "cols":{"0":{"width":100},"1":{"width":160},"len":2},
                  "merges":[],
                  "fillFormInfo":{"layout":{"direction":"horizontal","width":160,"height":32}},
                  "printConfig":{"paper":"A4"},
                  "dataRectWidth":260
                }
                """;
    }

    private String sampleSignatureMarkerReportJson() {
        return """
                {
                  "name":"signature-marker-demo",
                  "rows":{
                    "0":{"cells":{"0":{"text":"填写人"},"1":{"text":""}},"height":24},
                    "1":{"cells":{"0":{"text":"批准人"},"1":{"text":""}},"height":24}
                  },
                  "cols":{"0":{"width":100},"1":{"width":160},"len":2},
                  "merges":[],
                  "fillFormInfo":{"layout":{"direction":"horizontal","width":160,"height":32}},
                  "printConfig":{"paper":"A4"},
                  "dataRectWidth":260
                }
                """;
    }

    private String sampleStructuredHeaderBlankRuleReportJson() {
        return """
                {
                  "name":"structured-header-blank-demo",
                  "rows":{
                    "0":{
                      "cells":{
                        "0":{"text":"设备编码"},
                        "1":{"text":"设备名称"},
                        "2":{"text":"设备型号"},
                        "3":{"text":"设备编号"},
                        "4":{"text":"是否在计量效期内"},
                        "5":{"text":"","fillForm":{"field":"ebr_rule_r0_c5","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}},
                        "6":{"text":"操作人"},
                        "7":{"text":"","fillForm":{"field":"ebr_rule_r0_c7","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}},
                        "8":{"text":"复核人"},
                        "9":{"text":"","fillForm":{"field":"ebr_rule_r0_c9","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}
                      },
                      "height":24
                    }
                  },
                  "cols":{
                    "0":{"width":88},
                    "1":{"width":120},
                    "2":{"width":120},
                    "3":{"width":96},
                    "4":{"width":132},
                    "5":{"width":72},
                    "6":{"width":84},
                    "7":{"width":72},
                    "8":{"width":84},
                    "9":{"width":72},
                    "len":10
                  },
                  "merges":[],
                  "fillFormInfo":{"layout":{"direction":"horizontal","width":160,"height":32}},
                  "printConfig":{"paper":"A4"},
                  "dataRectWidth":940
                }
                """;
    }

    private String sampleLegacyLossReportMergedBodyJson() {
        return """
                {
                  "name":"legacy-loss-report-demo",
                  "rows":{
                    "0":{"cells":{"0":{"text":"上海瑛泰医疗器械股份有限公司","merge":[0,7]}},"height":28},
                    "1":{"cells":{
                      "0":{"text":"产品名称"},
                      "1":{"text":"","fillForm":{"field":"ebr_loss_r1_c1","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}},
                      "2":{"text":"型号规格"},
                      "3":{"text":"","fillForm":{"field":"ebr_loss_r1_c3","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}},
                      "4":{"text":"批号"},
                      "5":{"text":"","fillForm":{"field":"ebr_loss_r1_c5","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}},
                      "6":{"text":"生产数量"},
                      "7":{"text":"","fillForm":{"field":"ebr_loss_r1_c7","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}
                    },"height":36},
                    "2":{"cells":{"0":{"text":"损耗描述：\\n不合格日期\\n工序名称\\n不合格数量\\n不合格原因\\n处置方式\\n生产人员/日期\\n检验人员\\n确认/日期\\n\\n□报废  □其他：________________\\n□报废  □其他：________________\\n□报废  □其他：________________\\n□报废  □其他：________________\\n□报废  □其他：________________\\n□报废  □其他：________________\\n□报废  □其他：________________\\n□报废  □其他：________________","merge":[0,7]}},"height":300},
                    "3":{"cells":{"0":{"text":"批准人/日期：","merge":[0,7]}},"height":54},
                    "4":{"cells":{"0":{"text":"生效日期：2025年9月30日","merge":[0,7]}},"height":24}
                  },
                  "cols":{"0":{"width":120},"1":{"width":120},"2":{"width":120},"3":{"width":120},"4":{"width":120},"5":{"width":120},"6":{"width":120},"7":{"width":120},"len":8},
                  "merges":["A1:H1","A3:H3","A4:H4","A5:H5"],
                  "fillFormInfo":{"layout":{"direction":"horizontal","width":160,"height":32}},
                  "printConfig":{"paper":"A4"},
                  "dataRectWidth":960
                }
                """;
    }

    private String sampleLegacyLossReportVerticalBodyJson() {
        return """
                {
                  "name":"legacy-loss-report-vertical-demo",
                  "rows":{
                    "0":{"cells":{"0":{"text":"上海瑛泰医疗器械股份有限公司","merge":[0,7]}},"height":28},
                    "1":{"cells":{
                      "0":{"text":"产品名称"},
                      "1":{"text":"","fillForm":{"field":"ebr_loss_r1_c1","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}},
                      "2":{"text":"型号规格"},
                      "3":{"text":"","fillForm":{"field":"ebr_loss_r1_c3","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}},
                      "4":{"text":"批号"},
                      "5":{"text":"","fillForm":{"field":"ebr_loss_r1_c5","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}},
                      "6":{"text":"生产数量"},
                      "7":{"text":"","fillForm":{"field":"ebr_loss_r1_c7","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}
                    },"height":36},
                    "2":{"cells":{"0":{"text":"损耗描述","merge":[0,7]}},"height":28},
                    "3":{"cells":{"0":{"text":"不合格日期","merge":[0,2]},"3":{"text":"","merge":[0,4],"fillForm":{"field":"ebr_loss_r3_c3","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}},"height":28},
                    "4":{"cells":{"0":{"text":"工序名称","merge":[0,2]},"3":{"text":"","merge":[0,4],"fillForm":{"field":"ebr_loss_r4_c3","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}},"height":28},
                    "5":{"cells":{"0":{"text":"不合格数量","merge":[0,2]},"3":{"text":"","merge":[0,4],"fillForm":{"field":"ebr_loss_r5_c3","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}},"height":28},
                    "6":{"cells":{"0":{"text":"不合格原因","merge":[0,2]},"3":{"text":"","merge":[0,4],"fillForm":{"field":"ebr_loss_r6_c3","component":"Input","componentFlag":"input-textarea","required":false,"label":"","labelText":""}}},"height":44},
                    "7":{"cells":{"0":{"text":"处置方式","merge":[0,2]},"3":{"text":"","merge":[0,4],"fillForm":{"field":"ebr_loss_r7_c3","component":"Input","componentFlag":"input-textarea","required":false,"label":"","labelText":""}}},"height":44},
                    "8":{"cells":{"0":{"text":"生产人员/日期","merge":[0,2]},"3":{"text":"","merge":[0,4],"fillForm":{"field":"ebr_loss_r8_c3","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}},"height":28},
                    "9":{"cells":{"0":{"text":"检验人员","merge":[0,2]},"3":{"text":"","merge":[0,4],"fillForm":{"field":"ebr_loss_r9_c3","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}},"height":28},
                    "10":{"cells":{"0":{"text":"确认/日期","merge":[0,2]},"3":{"text":"","merge":[0,4],"fillForm":{"field":"ebr_loss_r10_c3","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}},"height":28},
                    "11":{"cells":{"0":{"text":"□报废"},"1":{"text":"","fillForm":{"field":"ebr_loss_r11_c1","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}},"2":{"text":"□其他："},"3":{"text":"","merge":[0,4],"fillForm":{"field":"ebr_loss_r11_c3","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}},"height":28},
                    "12":{"cells":{"0":{"text":"□报废"},"1":{"text":"","fillForm":{"field":"ebr_loss_r12_c1","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}},"2":{"text":"□其他："},"3":{"text":"","merge":[0,4],"fillForm":{"field":"ebr_loss_r12_c3","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}},"height":28},
                    "13":{"cells":{"0":{"text":"□报废"},"1":{"text":"","fillForm":{"field":"ebr_loss_r13_c1","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}},"2":{"text":"□其他："},"3":{"text":"","merge":[0,4],"fillForm":{"field":"ebr_loss_r13_c3","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}},"height":28},
                    "14":{"cells":{"0":{"text":"□报废"},"1":{"text":"","fillForm":{"field":"ebr_loss_r14_c1","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}},"2":{"text":"□其他："},"3":{"text":"","merge":[0,4],"fillForm":{"field":"ebr_loss_r14_c3","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}},"height":28},
                    "15":{"cells":{"0":{"text":"□报废"},"1":{"text":"","fillForm":{"field":"ebr_loss_r15_c1","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}},"2":{"text":"□其他："},"3":{"text":"","merge":[0,4],"fillForm":{"field":"ebr_loss_r15_c3","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}},"height":28},
                    "16":{"cells":{"0":{"text":"□报废"},"1":{"text":"","fillForm":{"field":"ebr_loss_r16_c1","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}},"2":{"text":"□其他："},"3":{"text":"","merge":[0,4],"fillForm":{"field":"ebr_loss_r16_c3","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}},"height":28},
                    "17":{"cells":{"0":{"text":"□报废"},"1":{"text":"","fillForm":{"field":"ebr_loss_r17_c1","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}},"2":{"text":"□其他："},"3":{"text":"","merge":[0,4],"fillForm":{"field":"ebr_loss_r17_c3","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}},"height":28},
                    "18":{"cells":{"0":{"text":"□报废"},"1":{"text":"","fillForm":{"field":"ebr_loss_r18_c1","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}},"2":{"text":"□其他："},"3":{"text":"","merge":[0,4],"fillForm":{"field":"ebr_loss_r18_c3","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}},"height":28},
                    "19":{"cells":{"0":{"text":"批准人/日期","merge":[0,2]},"3":{"text":"","merge":[0,4],"fillForm":{"field":"ebr_loss_r19_c3","component":"Input","componentFlag":"input-text","required":false,"label":"","labelText":""}}},"height":54},
                    "20":{"cells":{"0":{"text":"生效日期：2025年9月30日","merge":[0,7]}},"height":24}
                  },
                  "cols":{"0":{"width":120},"1":{"width":120},"2":{"width":120},"3":{"width":120},"4":{"width":120},"5":{"width":120},"6":{"width":120},"7":{"width":120},"len":8},
                  "merges":["A1:H1","A3:H3","A21:H21"],
                  "fillFormInfo":{"layout":{"direction":"horizontal","width":160,"height":32}},
                  "printConfig":{"paper":"A4"},
                  "dataRectWidth":960
                }
                """;
    }

}
