package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessDeniedException;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessRequest;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PDF_CONVERSION_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PDF_CONVERSION_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

class DccOnlyOfficeDocumentPdfConversionServiceTest extends BaseMockitoUnitTest {

    private static final BusinessFileAccessReference DCC_REFERENCE = new BusinessFileAccessReference(
            "dcc", "DCC_CONTROLLED_FILE", 801L, "A.1", 31L, null);

    @Mock
    private DccOnlyOfficePreviewTokenService onlyOfficePreviewTokenService;
    @Mock
    private DccOnlyOfficeConversionClient onlyOfficeConversionClient;
    @Mock
    private BusinessFileAccessService businessFileAccessService;
    @Mock
    private FileService fileService;

    @InjectMocks
    private DccOnlyOfficeDocumentPdfConversionService conversionService;

    @Test
    void r06PngConvertsActualPixelsWithoutOnlyOfficeConfiguration() throws Exception {
        TenantContextHolder.setTenantId(31L);
        ReflectionTestUtils.setField(conversionService, "onlyOfficePreviewProperties", new DccOnlyOfficePreviewProperties());
        byte[] original = r06PngBytes();
        byte[] unchanged = original.clone();
        FileDO source = FileDO.builder().id(101L).configId(1L).path("dcc/original/r06.png")
                .name("R06.png").type("image/png").build();
        lenient().when(businessFileAccessService.assertAllowed(any(BusinessFileAccessRequest.class)))
                .thenReturn(java.util.Optional.of(DCC_REFERENCE));
        lenient().when(fileService.getFileContent(1L, source.getPath())).thenReturn(original);

        DccConvertedPdf result = assertDoesNotThrow(() -> conversionService.convertToPdf(source),
                "PNG must use its real pixels locally, without requiring the Office server");
        assertEquals("R06.pdf", result.fileName());
        try (PDDocument pdf = PDDocument.load(result.content())) {
            assertEquals(1, pdf.getNumberOfPages());
            var resources = pdf.getPage(0).getResources();
            PDImageXObject embedded = null;
            for (var name : resources.getXObjectNames()) {
                if (resources.getXObject(name) instanceof PDImageXObject image) embedded = image;
            }
            assertTrue(embedded != null, "PDF must embed source pixels, not a filename placeholder");
            assertEquals(8, embedded.getWidth());
            assertEquals(4, embedded.getHeight());
            assertEquals(0xFF0000, embedded.getImage().getRGB(1, 1) & 0xFFFFFF);
            assertEquals(0x0000FF, embedded.getImage().getRGB(6, 1) & 0xFFFFFF);
        }
        assertArrayEquals(unchanged, original);
        verify(businessFileAccessService).assertAllowed(org.mockito.ArgumentMatchers.argThat(request ->
                request.operation() == BusinessFileAccessOperation.CONVERT && Long.valueOf(101L).equals(request.fileId())
                        && Long.valueOf(31L).equals(request.tenantId()) && request.userId() == null));
        verify(fileService).getFileContent(1L, source.getPath());
        verify(onlyOfficeConversionClient, never()).convertToPdf(any());
        verify(onlyOfficePreviewTokenService, never()).issueBusinessFile(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void r06TxtMissingFontReportsExplicitTextFontRatherThanOfficeConfiguration() throws Exception {
        TenantContextHolder.setTenantId(31L);
        ReflectionTestUtils.setField(conversionService, "onlyOfficePreviewProperties", new DccOnlyOfficePreviewProperties());
        FileDO source = FileDO.builder().id(101L).configId(1L).path("dcc/original/r06.txt")
                .name("R06.txt").type("text/plain").build();
        lenient().when(businessFileAccessService.assertAllowed(any(BusinessFileAccessRequest.class)))
                .thenReturn(java.util.Optional.of(DCC_REFERENCE));
        lenient().when(fileService.getFileContent(1L, source.getPath()))
                .thenReturn("R06 中文正文\n第二行完整保留".getBytes(StandardCharsets.UTF_8));

        ServiceException failure = assertThrows(ServiceException.class, () -> conversionService.convertToPdf(source));
        assertTrue(failure.getMessage().contains("yudao.dcc.conversion.text-font"),
                "TXT must expose its missing font dependency instead of an unrelated Office configuration");
        verify(businessFileAccessService).assertAllowed(any(BusinessFileAccessRequest.class));
        verify(onlyOfficeConversionClient, never()).convertToPdf(any());
        verify(onlyOfficePreviewTokenService, never()).issueBusinessFile(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void r06PngDeniedConversionCannotReadBytesOrCallOnlyOffice() {
        TenantContextHolder.setTenantId(31L);
        ReflectionTestUtils.setField(conversionService, "onlyOfficePreviewProperties", configuredProperties());
        FileDO source = FileDO.builder().id(101L).configId(1L).path("dcc/original/r06.png")
                .name("R06.png").type("image/png").build();
        when(businessFileAccessService.assertAllowed(any(BusinessFileAccessRequest.class)))
                .thenThrow(new BusinessFileAccessDeniedException("denied", BusinessFileAccessOperation.CONVERT, 101L, "dcc"));
        ServiceException failure = assertThrows(ServiceException.class, () -> conversionService.convertToPdf(source));
        assertEquals(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED.getCode(), failure.getCode());
        org.mockito.Mockito.verifyNoInteractions(fileService, onlyOfficeConversionClient, onlyOfficePreviewTokenService);
    }

    private static byte[] r06PngBytes() throws Exception {
        BufferedImage image = new BufferedImage(8, 4, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 4; y++) for (int x = 0; x < 8; x++) image.setRGB(x, y, x < 4 ? 0xFF0000 : 0x0000FF);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", bytes));
        return bytes.toByteArray();
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @ParameterizedTest
    @CsvSource({
            "xls,DCC-78C-eYk7ar6ch8Map7CUxshVxPShb-HD,true",
            "xlsx,DCC-rV6NojjkG-cTOA8AatHQ_sFKqai4Fpbl,true",
            "doc,DCC-oz4tbRZIKXeKfuaBpp1n0QjekBjYOGRB,false",
            "docx,DCC-DuClQcP0KVS3xm1ytXoLsPywA8EYRIEg,false"
    })
    void buildConversionKey_versionsSpreadsheetLayoutAndPreservesWordKeys(
            String extension, String legacyKey, boolean spreadsheet) {
        FileDO source = FileDO.builder().id(101L).path("dcc/original/spec." + extension)
                .name("Spec." + extension).build();
        String key = ReflectionTestUtils.invokeMethod(conversionService, "buildConversionKey", source);

        if (spreadsheet) {
            assertNotEquals(legacyKey, key, "New spreadsheet layout must bypass cached legacy conversions");
        } else {
            assertEquals(legacyKey, key, "Word conversion keys must remain unchanged");
        }
        assertEquals(key, ReflectionTestUtils.<String>invokeMethod(conversionService, "buildConversionKey", source),
                "Retries using the same spreadsheet layout must reuse a stable key");
    }

    @Test
    void convertToPdf_configMissingFailsFastBeforeCallingOnlyOffice() {
        ReflectionTestUtils.setField(conversionService, "onlyOfficePreviewProperties",
                new DccOnlyOfficePreviewProperties());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> conversionService.convertToPdf(sourceFile()));
        assertEquals(CONTROLLED_FILE_PDF_CONVERSION_CONFIG_MISSING.getCode(), exception.getCode());
        assertEquals("Controlled file PDF conversion config is missing: yudao.dcc.preview.onlyoffice.base-url is missing",
                exception.getMessage());

        verify(onlyOfficePreviewTokenService, never()).issueBusinessFile(
                any(), any(), any(), any(), any(), any(), any(), any());
        verify(onlyOfficeConversionClient, never()).convertToPdf(any());
    }

    @Test
    void convertToPdf_successBuildsOnlyOfficeCommandAndReturnsPdf() {
        TenantContextHolder.setTenantId(31L);
        DccOnlyOfficePreviewProperties properties = configuredProperties();
        ReflectionTestUtils.setField(conversionService, "onlyOfficePreviewProperties", properties);
        when(businessFileAccessService.assertAllowed(any(BusinessFileAccessRequest.class)))
                .thenReturn(java.util.Optional.of(DCC_REFERENCE));
        when(onlyOfficePreviewTokenService.issueBusinessFile(
                eq(DccOnlyOfficePreviewTokenService.AUDIENCE_UPLOAD_PREVIEW),
                eq(BusinessFileAccessOperation.CONVERT), eq(101L), eq(31L), isNull(),
                eq(DccOnlyOfficePreviewTokenService.SERVICE_DCC_PDF_CONVERSION),
                eq(DCC_REFERENCE), eq(300L)))
                .thenReturn(new DccOnlyOfficePreviewTokenService.IssuedPreviewToken(
                        "source-token", new DccOnlyOfficePreviewTokenService.PreviewTokenPayload()));
        when(onlyOfficeConversionClient.convertToPdf(any())).thenReturn("%PDF-converted".getBytes());

        DccConvertedPdf result = conversionService.convertToPdf(sourceFile());

        assertEquals("Spec.pdf", result.fileName());
        assertEquals("%PDF-converted", new String(result.content()));
        ArgumentCaptor<DccOnlyOfficeConversionCommand> commandCaptor =
                ArgumentCaptor.forClass(DccOnlyOfficeConversionCommand.class);
        verify(onlyOfficeConversionClient).convertToPdf(commandCaptor.capture());
        DccOnlyOfficeConversionCommand command = commandCaptor.getValue();
        assertTrue(command.converterUrl().startsWith("http://onlyoffice.local/converter?shardkey=DCC-"));
        assertEquals("secret", command.jwtSecret());
        assertEquals("docx", command.fileType());
        assertEquals("Spec.docx", command.title());
        assertEquals("http://127.0.0.1:48081/admin-api/dcc/controlled-files/upload-preview/101/onlyoffice-file?token=source-token",
                command.documentUrl());
        verify(businessFileAccessService).assertAllowed(org.mockito.ArgumentMatchers.argThat(request ->
                request.operation() == BusinessFileAccessOperation.CONVERT
                        && Long.valueOf(101L).equals(request.fileId())
                        && Long.valueOf(31L).equals(request.tenantId())
                        && request.userId() == null
                        && DccOnlyOfficePreviewTokenService.SERVICE_DCC_PDF_CONVERSION
                        .equals(request.serviceIdentity())));
    }

    @Test
    void convertToPdf_businessGateDenialStopsBeforeTokenAndOnlyOfficeClient() {
        TenantContextHolder.setTenantId(31L);
        ReflectionTestUtils.setField(conversionService, "onlyOfficePreviewProperties", configuredProperties());
        when(businessFileAccessService.assertAllowed(any(BusinessFileAccessRequest.class)))
                .thenThrow(new BusinessFileAccessDeniedException("denied",
                        BusinessFileAccessOperation.CONVERT, 101L, "dcc"));

        assertThrows(ServiceException.class, () -> conversionService.convertToPdf(sourceFile()));

        verify(onlyOfficePreviewTokenService, never()).issueBusinessFile(
                any(), any(), any(), any(), any(), any(), any(), any());
        verify(onlyOfficeConversionClient, never()).convertToPdf(any());
    }

    @Test
    void convertToPdf_nonPdfConversionResultFailsFast() {
        TenantContextHolder.setTenantId(31L);
        ReflectionTestUtils.setField(conversionService, "onlyOfficePreviewProperties", configuredProperties());
        when(businessFileAccessService.assertAllowed(any(BusinessFileAccessRequest.class)))
                .thenReturn(java.util.Optional.of(DCC_REFERENCE));
        when(onlyOfficePreviewTokenService.issueBusinessFile(
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new DccOnlyOfficePreviewTokenService.IssuedPreviewToken(
                        "source-token", new DccOnlyOfficePreviewTokenService.PreviewTokenPayload()));
        when(onlyOfficeConversionClient.convertToPdf(any())).thenReturn("not-pdf".getBytes());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> conversionService.convertToPdf(sourceFile()));
        assertEquals(CONTROLLED_FILE_PDF_CONVERSION_FAILED.getCode(), exception.getCode());
        assertEquals("OnlyOffice conversion result is not a real PDF", exception.getMessage());
    }

    private static DccOnlyOfficePreviewProperties configuredProperties() {
        DccOnlyOfficePreviewProperties properties = new DccOnlyOfficePreviewProperties();
        properties.setBaseUrl("http://onlyoffice.local/");
        properties.setJwtSecret("secret");
        properties.setPublicFileBaseUrl("http://127.0.0.1:48081/");
        return properties;
    }

    private static FileDO sourceFile() {
        return FileDO.builder()
                .id(101L)
                .configId(1L)
                .path("dcc/original/spec.docx")
                .name("Spec.docx")
                .type("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .build();
    }
}
