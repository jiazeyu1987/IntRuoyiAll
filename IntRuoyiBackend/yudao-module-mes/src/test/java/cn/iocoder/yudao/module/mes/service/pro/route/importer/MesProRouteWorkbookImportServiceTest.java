package cn.iocoder.yudao.module.mes.service.pro.route.importer;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class MesProRouteWorkbookImportServiceTest {
    @Mock private MesProRouteResourceImportService resourceImport;
    @InjectMocks private MesProRouteWorkbookImportServiceImpl service;
    private byte[] workbook() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.createSheet(MesProRouteResourceTransferService.SHEET);
            workbook.write(output); return output.toByteArray();
        }
    }
    @Test void forwardsCompleteWorkbookAndResult() throws Exception {
        var expected = new MesProRouteWorkbookImportResult(); expected.setRouteCount(5);
        when(resourceImport.importWorkbook(any())).thenAnswer(call -> {
            assertNotNull(((Workbook)call.getArgument(0)).getSheet(MesProRouteResourceTransferService.SHEET));
            return expected;
        });
        assertSame(expected, service.importWorkbook(new MockMultipartFile("file", "routes.xlsx", "application/octet-stream", workbook())));
    }
    @Test void rejectsMissingFileBeforeWriting() {
        assertThrows(ServiceException.class, () -> service.importWorkbook(null));
        assertThrows(ServiceException.class, () -> service.importWorkbook(new MockMultipartFile("file", new byte[0])));
        verifyNoInteractions(resourceImport);
    }
    @Test void doesNotHideResourceFailure() throws Exception {
        byte[] data=workbook();
        ServiceException expected = new ServiceException(400, "关联资源缺失");
        when(resourceImport.importWorkbook(any())).thenThrow(expected);
        assertSame(expected, assertThrows(ServiceException.class, () -> service.importWorkbook(new MockMultipartFile("file", data))));
    }
}
