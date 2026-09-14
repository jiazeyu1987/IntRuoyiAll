package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationParseRespVO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesQaInspectionRegulationParseServiceTest {

    private static final String REAL_FILE_NAME =
            "PQC-IDPR-001 (D 0）按压式球囊扩张压力泵组装过程检验规程--2025.10.24生效.docx";
    private static final Path REAL_QA_DOCX = Path.of("..", "..", "resource", "按压式球囊扩张压力泵",
            REAL_FILE_NAME);
    private static final String REAL_IDI_B2_FILE_NAME =
            "PQC-IDI-001（B 2）按压式球囊扩充压力泵组装过程检验规程--2026.01.04生效.docx";
    private static final Path REAL_IDI_B2_QA_DOCX = Path.of("..", "..", "resource",
            "按压式球囊扩充压力泵IDI-001", REAL_IDI_B2_FILE_NAME);

    private final MesQaInspectionRegulationParseService service =
            new MesQaInspectionRegulationParseService(new MesQaInspectionRegulationWordParser());

    @Test
    void parseWord_buildsEditableQaJsonContractFromRealRegulation() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", REAL_FILE_NAME,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                Files.readAllBytes(REAL_QA_DOCX));

        MesQaInspectionRegulationParseRespVO result = service.parseWord(file);

        assertEquals(1, result.getSchemaVersion());
        assertEquals(REAL_FILE_NAME, result.getSourceFileName());
        assertEquals("PQC-IDPR-001", result.getRegulationCode());
        assertEquals("按压式球囊扩张压力泵组装过程检验规程", result.getRegulationName());
        assertEquals("D/0", result.getVersionNo());
        assertInstanceOf(String.class, result.getEffectiveDate());
        assertEquals("2025-10-24", result.getEffectiveDate());
        assertFalse(result.getProcesses().isEmpty());

        MesQaInspectionRegulationParseRespVO.InspectionProcess cleaning = result.getProcesses().stream()
                .filter(process -> "清洗".equals(process.getProcessName()))
                .findFirst()
                .orElseThrow();
        assertEquals("PQC-IDPR-001-P001", cleaning.getProcessCode());
        MesQaInspectionRegulationParseRespVO.InspectionItem appearance = cleaning.getItems().stream()
                .filter(item -> "外观".equals(item.getItemName()))
                .findFirst()
                .orElseThrow();
        assertEquals("PQC-IDPR-001-I001", appearance.getItemCode());
        assertEquals("目测", appearance.getInspectionTool());
        assertTrue(appearance.getStandardText().contains("表面应清洁"));
        assertTrue(appearance.getInspectionMethod().contains("300~700lx"));
        assertEquals(new BigDecimal("0.4"), appearance.getPatrolInspectionRatio());
        assertTrue(appearance.getApplicableInspectionTypes().contains("PATROL"));
    }

    @Test
    void parseWord_buildsEditableQaJsonContractFromRealIdiB2Regulation() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", REAL_IDI_B2_FILE_NAME,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                Files.readAllBytes(REAL_IDI_B2_QA_DOCX));

        MesQaInspectionRegulationParseRespVO result = service.parseWord(file);

        assertEquals(1, result.getSchemaVersion());
        assertEquals(REAL_IDI_B2_FILE_NAME, result.getSourceFileName());
        assertEquals("PQC-IDI-001", result.getRegulationCode());
        assertEquals("按压式球囊扩充压力泵组装过程检验规程", result.getRegulationName());
        assertEquals("B/2", result.getVersionNo());
        assertInstanceOf(String.class, result.getEffectiveDate());
        assertEquals("2025-09-30", result.getEffectiveDate());
        assertEquals(6, result.getProcesses().size());
        int itemCount = result.getProcesses().stream()
                .mapToInt(process -> process.getItems().size())
                .sum();
        assertEquals(22, itemCount);
    }

    @Test
    void parseWord_rejectsNonDocxWithoutReturningEmptyResult() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "错误文件.doc", "application/msword", new byte[]{1, 2, 3});

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.parseWord(file));

        assertTrue(exception.getMessage().contains("仅支持 .docx 文件"));
    }
}
