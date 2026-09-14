package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessDeniedException;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessRequest;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PDF_CONVERSION_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PDF_CONVERSION_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccOnlyOfficeDocumentPdfConversionServiceTest extends BaseMockitoUnitTest {

    private static final BusinessFileAccessReference DCC_REFERENCE = new BusinessFileAccessReference(
            "dcc", "DCC_CONTROLLED_FILE", 801L, "A.1", 31L, null);

    @Mock
    private DccOnlyOfficePreviewTokenService onlyOfficePreviewTokenService;
    @Mock
    private DccOnlyOfficeConversionClient onlyOfficeConversionClient;
    @Mock
    private BusinessFileAccessService businessFileAccessService;

    @InjectMocks
    private DccOnlyOfficeDocumentPdfConversionService conversionService;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
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
