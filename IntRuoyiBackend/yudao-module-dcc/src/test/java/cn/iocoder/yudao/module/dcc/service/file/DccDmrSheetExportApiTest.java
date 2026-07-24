package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class DccDmrSheetExportApiTest extends BaseMockitoUnitTest {

    @Mock
    private DccDmrSheetExportService dmrSheetExportService;

    @InjectMocks
    private DccControlledFileController controller;

    @Test
    void exportDmrSheetWorkbook_returnsXlsxAttachment() {
        byte[] workbookBytes = new byte[]{1, 2, 3};
        when(dmrSheetExportService.exportWorkbook()).thenReturn(workbookBytes);

        ResponseEntity<byte[]> response = controller.exportDmrSheetWorkbook();

        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                response.getHeaders().getContentType().toString());
        assertNotNull(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals(true, response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("DMR-sheet.xlsx"));
        assertArrayEquals(workbookBytes, response.getBody());
    }

    @Test
    void exportDmrSheetWorkbook_requiresDocControlRole() throws Exception {
        Method method = DccControlledFileController.class.getMethod("exportDmrSheetWorkbook");
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("@ss.hasRole('doc_control')", preAuthorize.value());
    }
}

