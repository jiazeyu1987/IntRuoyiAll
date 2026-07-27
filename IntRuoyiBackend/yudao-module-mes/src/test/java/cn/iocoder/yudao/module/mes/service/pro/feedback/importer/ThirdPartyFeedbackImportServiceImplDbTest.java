package cn.iocoder.yudao.module.mes.service.pro.feedback.importer;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackImportRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackImportRecordMapper;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Set;
import java.util.List;
import javax.sql.DataSource;

import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackImportRecordDO.ATTRIBUTION_STATUS_PENDING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_ROW_DUPLICATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({
        ThirdPartyFeedbackImportServiceImpl.class,
        ThirdPartyFeedbackExcelParser.class
})
class ThirdPartyFeedbackImportServiceImplDbTest extends BaseDbUnitTest {

    @Resource
    private ThirdPartyFeedbackImportService importService;
    @Resource
    private MesProFeedbackImportRecordMapper importRecordMapper;
    @Resource
    private DataSource dataSource;
    @MockitoBean
    private MesMdAutoCodeRecordService autoCodeRecordService;
    @MockitoBean
    private MesProFeedbackService feedbackService;
    @MockitoBean
    private MesProRouteProcessService routeProcessService;

    @BeforeEach
    void prepareSchema() throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE mes_pro_feedback_import_record ADD COLUMN IF NOT EXISTS attribution_status VARCHAR(32)");
            statement.execute("ALTER TABLE mes_pro_feedback_import_record ADD COLUMN IF NOT EXISTS work_order_code VARCHAR(64)");
            statement.execute("ALTER TABLE mes_pro_feedback_import_record ADD COLUMN IF NOT EXISTS item_code VARCHAR(64)");
            statement.execute("ALTER TABLE mes_pro_feedback_import_record ADD COLUMN IF NOT EXISTS process_code VARCHAR(64)");
            statement.execute("ALTER TABLE mes_pro_feedback_import_record ADD COLUMN IF NOT EXISTS source_payload_json CLOB");
            statement.execute("ALTER TABLE mes_pro_feedback_import_record ADD COLUMN IF NOT EXISTS schedule_order_id BIGINT");
            statement.execute("ALTER TABLE mes_pro_feedback_import_record ADD COLUMN IF NOT EXISTS schedule_order_process_id BIGINT");
            statement.execute("ALTER TABLE mes_pro_feedback_import_record ADD COLUMN IF NOT EXISTS candidate_count INT");
        }
    }

    @Test
    void importWorkbook_success_createsPendingImportRecordsOnly() throws Exception {
        MockMultipartFile file = buildWorkbook(List.of(
                new WorkbookRow("棘突球囊报工", LocalDateTime.of(2026, 4, 9, 15, 27, 17), "U001", "吴廷", "潘金华",
                        "MO-001", "包装工段", "纸塑袋封口全检", "TASK-001", "ITEM-001", "产品A", "SPEC-A",
                        "", "PROC-001", "纸塑袋封口全检", "组装", new BigDecimal("234")),
                new WorkbookRow("造影导管", LocalDateTime.of(2026, 4, 8, 21, 40, 37), "U002", "汤小芹", "刘青",
                        "MO-002", "造影导管工段", "多功能造影导管包装", "TASK-002", "ITEM-002", "产品B", "SPEC-B",
                        "", "PROC-002", "多功能造影导管包装", "组装", new BigDecimal("250"))
        ));

        ThirdPartyFeedbackImportResult result = importService.importWorkbook(file);

        assertEquals(2, result.getSheetCount());
        assertEquals(2, result.getImportedCount());
        assertEquals(2, result.getPendingCount());
        assertEquals(0, result.getSubmittedCount());
        assertEquals(0, result.getFeedbackCodes().size());
        assertEquals(2, result.getImportRecordIds().size());

        List<MesProFeedbackImportRecordDO> records = importRecordMapper.selectListByIds(result.getImportRecordIds());
        assertEquals(2, records.size());
        assertTrue(records.stream().allMatch(item -> ATTRIBUTION_STATUS_PENDING.equals(item.getAttributionStatus())));
        assertEquals(Set.of("TASK-001", "TASK-002"), records.stream().map(MesProFeedbackImportRecordDO::getTaskCode).collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of("ITEM-001", "ITEM-002"), records.stream().map(MesProFeedbackImportRecordDO::getItemCode).collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of("PROC-001", "PROC-002"), records.stream().map(MesProFeedbackImportRecordDO::getProcessCode).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void importWorkbook_whenSameFileImportedTwice_rejectsDuplicateRow() throws Exception {
        MockMultipartFile file = buildWorkbook(List.of(
                new WorkbookRow("棘突球囊报工", LocalDateTime.of(2026, 4, 9, 15, 27, 17), "U001", "吴廷", "潘金华",
                        "MO-001", "包装工段", "纸塑袋封口全检", "TASK-001", "ITEM-001", "产品A", "SPEC-A",
                        "", "PROC-001", "纸塑袋封口全检", "组装", new BigDecimal("234"))
        ));

        importService.importWorkbook(file);

        ServiceException exception = assertThrows(ServiceException.class, () -> importService.importWorkbook(file));

        assertEquals(PRO_FEEDBACK_IMPORT_ROW_DUPLICATE.getCode(), exception.getCode());
        assertEquals(1, importRecordMapper.selectList().stream()
                .filter(item -> "third-party-feedback.xlsx".equals(item.getSourceFileName()))
                .count());
    }

    @Test
    void importRecordTable_shouldAllowRepeatedSourceRowForDirectWorkReportRetest() {
        MesProFeedbackImportRecordDO first = MesProFeedbackImportRecordDO.builder()
                .sourceFileName("李萍.xlsx")
                .sourceFileSha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .sheetName("生产报工")
                .rowNo(8)
                .feedbackId(900L)
                .attributionStatus(MesProFeedbackImportRecordDO.ATTRIBUTION_STATUS_ATTRIBUTED)
                .taskCode("881MO093613-1-11")
                .build();
        MesProFeedbackImportRecordDO second = MesProFeedbackImportRecordDO.builder()
                .sourceFileName("李萍.xlsx")
                .sourceFileSha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .sheetName("生产报工")
                .rowNo(8)
                .feedbackId(901L)
                .attributionStatus(MesProFeedbackImportRecordDO.ATTRIBUTION_STATUS_ATTRIBUTED)
                .taskCode("881MO093613-1-11")
                .build();

        importRecordMapper.insert(first);
        importRecordMapper.insert(second);

        assertEquals(2, importRecordMapper.selectList().stream()
                .filter(item -> "李萍.xlsx".equals(item.getSourceFileName()))
                .count());
    }

    private MockMultipartFile buildWorkbook(List<WorkbookRow> rows) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            for (WorkbookRow workbookRow : rows) {
                var sheet = workbook.getSheet(workbookRow.sheetName());
                if (sheet == null) {
                    sheet = workbook.createSheet(workbookRow.sheetName());
                    createHeader(sheet);
                }
                var row = sheet.createRow(sheet.getLastRowNum() + 1);
                fillRow(row, workbookRow);
            }
            workbook.write(outputStream);
            return new MockMultipartFile("file", "third-party-feedback.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", outputStream.toByteArray());
        }
    }

    private void createHeader(org.apache.poi.ss.usermodel.Sheet sheet) {
        var header = sheet.createRow(0);
        List.of(
                "报工日期", "报工人编码", "报工人名称", "工段长", "生产订单号", "生产资源组", "生产资源", "派工单号",
                "产品编码", "产品名称", "规格", "模具编码", "工序编码", "工序名称", "所属部门", "报工数量", "支数",
                "公斤数", "实腔数", "全程时间", "生产定额", "工作时长", "注塑合模/组装公斤数", "注塑个数/组装个重", "操作"
        ).forEach(value -> header.createCell(header.getPhysicalNumberOfCells()).setCellValue(value));
    }

    private void fillRow(org.apache.poi.ss.usermodel.Row row, WorkbookRow workbookRow) {
        row.createCell(0).setCellValue(Date.from(workbookRow.feedbackTime().atZone(ZoneId.systemDefault()).toInstant()));
        row.createCell(1).setCellValue(workbookRow.feedbackUserCode());
        row.createCell(2).setCellValue(workbookRow.feedbackUserName());
        row.createCell(3).setCellValue(workbookRow.approverName());
        row.createCell(4).setCellValue(workbookRow.workOrderCode());
        row.createCell(5).setCellValue(workbookRow.resourceGroup());
        row.createCell(6).setCellValue(workbookRow.resourceName());
        row.createCell(7).setCellValue(workbookRow.taskCode());
        row.createCell(8).setCellValue(workbookRow.itemCode());
        row.createCell(9).setCellValue(workbookRow.itemName());
        row.createCell(10).setCellValue(workbookRow.specification());
        row.createCell(11).setCellValue(workbookRow.moldCode());
        row.createCell(12).setCellValue(workbookRow.processCode());
        row.createCell(13).setCellValue(workbookRow.processName());
        row.createCell(14).setCellValue(workbookRow.department());
        row.createCell(15).setCellValue(workbookRow.feedbackQuantity().doubleValue());
        row.createCell(24).setCellValue("删除");
    }

    private record WorkbookRow(String sheetName, LocalDateTime feedbackTime, String feedbackUserCode, String feedbackUserName,
                               String approverName, String workOrderCode, String resourceGroup, String resourceName,
                               String taskCode, String itemCode, String itemName, String specification, String moldCode,
                               String processCode, String processName, String department, BigDecimal feedbackQuantity) {
    }
}
