package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MesProBatchRecordGenericFilenameTest {
    private byte[] document() throws Exception {
        Path fixture = Path.of(System.getProperty("productionRecordFixture",
                "../../resource/按压式球囊扩充压力泵IDI-001/RE-PP-IDI-01（A 1） 按压式球囊扩充压力泵生产记录--2026.02.02生效.docx"));
        return Files.readAllBytes(fixture);
    }

    @Test
    void genericFilenameReadsCodeFromDocumentHeaderThroughParseService() throws Exception {
        MesProBatchRecordReportServiceImpl service = new MesProBatchRecordReportServiceImpl();
        var field = MesProBatchRecordReportServiceImpl.class.getDeclaredField("docParser");
        field.setAccessible(true);
        field.set(service, new MesProBatchRecordDocParser());
        String json = service.parseProductionBatchRecordTotalRecognitionJson(new MockMultipartFile(
                "file", "生产记录.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                document()));
        var result = new ObjectMapper().readTree(json);
        assertEquals("IDI-01", result.path("product").path("code").asText());
        assertEquals("按压式球囊扩充压力泵", result.path("product").path("name").asText());
        assertFalse(result.path("processes").isEmpty());
    }

    @Test
    void conflictingFilenameAndHeaderAreRejected() throws Exception {
        var tables = new MesProBatchRecordDocParser().parseWord(document(), "生产记录.docx");
        var error = assertThrows(IllegalStateException.class, () ->
                new MesProBatchRecordTotalRecognitionExtractor().extract("RE-PP-OTHER-99.docx", tables));
        assertTrue(error.getMessage().contains("产品编码不一致"));
    }

    @Test
    void absentCodeIsRejectedWithoutGuessing() throws Exception {
        var tables = new MesProBatchRecordDocParser().parseWord(document(), "生产记录.docx");
        tables.forEach(table -> table.setDocumentFrame(null));
        var error = assertThrows(IllegalStateException.class, () ->
                new MesProBatchRecordTotalRecognitionExtractor().extract("生产记录.docx", tables));
        assertTrue(error.getMessage().contains("未找到产品编码"));
    }

    @Test
    void extractionFailureIsReportedAsParseError() throws Exception {
        var service = new MesProBatchRecordReportServiceImpl();
        var method = MesProBatchRecordReportServiceImpl.class.getDeclaredMethod(
                "buildTotalRecognitionJson", String.class, List.class);
        method.setAccessible(true);
        var tables = new MesProBatchRecordDocParser().parseWord(document(), "生产记录.docx");
        tables.forEach(table -> table.setDocumentFrame(null));
        var error = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> method.invoke(service, "生产记录.docx", tables));
        assertInstanceOf(ServiceException.class, error.getCause());
        assertTrue(error.getCause().getMessage().contains("未找到产品编码"));
    }
}
