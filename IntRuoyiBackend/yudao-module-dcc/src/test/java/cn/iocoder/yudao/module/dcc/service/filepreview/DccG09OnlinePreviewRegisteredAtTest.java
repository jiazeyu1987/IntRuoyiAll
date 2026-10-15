package cn.iocoder.yudao.module.dcc.service.filepreview;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledPreviewWatermarkService;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import cn.iocoder.yudao.module.dcc.service.file.DccOnlyOfficePreviewProperties;
import cn.iocoder.yudao.module.dcc.service.preview.DccControlledPreviewAccessService;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessRequest;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessResult;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessDeniedException;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DccG09OnlinePreviewRegisteredAtTest {
    private final DccOnlineFilePreviewServiceImpl service = new DccOnlineFilePreviewServiceImpl();
    private final DccControlledPreviewAccessService access = mock(DccControlledPreviewAccessService.class);
    private final BusinessFileAccessService permission = mock(BusinessFileAccessService.class);
    private final FileMapper files = mock(FileMapper.class);
    private static final LocalDateTime UTC = LocalDateTime.of(2026, 10, 15, 0, 30, 10);
    private static final DccRequestAuditContext AUDIT = new DccRequestAuditContext("127.0.0.1", "JUnit", "g09-time");
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        ReflectionTestUtils.setField(service, "previewAccessService", access);
        ReflectionTestUtils.setField(service, "businessFileAccessService", permission);
        ReflectionTestUtils.setField(service, "fileMapper", files);
        ReflectionTestUtils.setField(service, "watermarkService", mock(DccControlledPreviewWatermarkService.class));
        var file = new FileDO(); file.setId(20L); file.setName("formal.pdf"); file.setPath("exact/formal.pdf");
        file.setType("application/pdf"); file.setConfigId(1L);
        when(files.selectById(20L)).thenReturn(file);
        when(permission.assertAllowed(any())).thenReturn(Optional.empty());
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    private DccPreviewAccessResult result(LocalDateTime issuedAt) {
        return new DccPreviewAccessResult(12L, "AE-G09", 13L, "WM-G09", "token", "token-id", "nonce",
                issuedAt, UTC.plusMinutes(5), "{}");
    }
    @Test void metadataMapsTheVerifiedEventInstantWithExplicitUtcOffset() throws Exception {
        when(access.prepareAccess(any(DccPreviewAccessRequest.class))).thenReturn(result(UTC));
        var metadata = service.getPreviewMetadata(99L, 20L, AUDIT);
        String timestamp = (String) metadata.getClass().getMethod("getPreviewRegisteredAt").invoke(metadata);
        assertNotNull(timestamp);
        assertEquals(Instant.parse("2026-10-15T00:30:10Z"), OffsetDateTime.parse(timestamp).toInstant());
        assertEquals(ZoneOffset.UTC, OffsetDateTime.parse(timestamp).getOffset());
        assertEquals("AE-G09", metadata.getAccessEventCode());
        assertNull(metadata.getWatermark(), "registration must not depend on watermark timestamp");
    }
    @Test void missingVerifiedRegistrationInstantCannotBecomeMetadataSuccess() {
        when(access.prepareAccess(any(DccPreviewAccessRequest.class))).thenReturn(result(null));
        assertThrows(RuntimeException.class, () -> service.getPreviewMetadata(99L, 20L, AUDIT));
    }
    @Test void registrationCannotBypassTheExistingBusinessFileAuthorization() {
        when(permission.assertAllowed(any())).thenThrow(new BusinessFileAccessDeniedException("denied", BusinessFileAccessOperation.PREVIEW, 20L, "dcc"));
        assertThrows(RuntimeException.class, () -> service.getPreviewMetadata(99L, 20L, AUDIT));
        verifyNoInteractions(access);
        verify(files, never()).selectById(any());
    }
    @Test void responseRegistrationPropertyCannotBeSuppliedThroughJsonInput() throws Exception {
        var mapper = new ObjectMapper();
        var metadata = mapper.readValue("{\"previewRegisteredAt\":\"2020-01-01T00:00:00Z\"}", DccControlledFilePreviewMetadataRespVO.class);
        assertNull(metadata.getClass().getMethod("getPreviewRegisteredAt").invoke(metadata));
        metadata.getClass().getMethod("setPreviewRegisteredAt", String.class).invoke(metadata, "2026-10-15T00:30:10Z");
        assertEquals("2026-10-15T00:30:10Z", mapper.readTree(mapper.writeValueAsString(metadata)).get("previewRegisteredAt").asText());
    }

    @Test void onlineOfficeUnavailableKeepsItsRealPriorEventTimeAndSpecificReasonWithoutClaimingBodySuccess() throws Exception {
        var file = new FileDO(); file.setId(20L); file.setName("formal.docx"); file.setPath("exact/formal.docx");
        file.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"); file.setConfigId(1L);
        when(files.selectById(20L)).thenReturn(file);
        var office = mock(DccOnlyOfficePreviewProperties.class);
        when(office.isConfigured()).thenReturn(false);
        when(office.missingReason()).thenReturn("base-url missing");
        ReflectionTestUtils.setField(service, "onlyOfficePreviewProperties", office);
        when(access.prepareAccess(any(DccPreviewAccessRequest.class))).thenReturn(result(UTC));
        var metadata = service.getPreviewMetadata(99L, 20L, AUDIT);
        assertEquals("OnlyOffice preview config is missing: base-url missing", metadata.getPreviewUnavailableReason());
        assertEquals("AE-G09", metadata.getAccessEventCode());
        assertEquals("2026-10-15T00:30:10Z", metadata.getClass().getMethod("getPreviewRegisteredAt").invoke(metadata));
        assertNull(metadata.getOnlyofficeDocumentUrl(), "unavailable response has no body to claim as successfully opened");
        verify(access, times(1)).prepareAccess(any(DccPreviewAccessRequest.class));
    }
}
