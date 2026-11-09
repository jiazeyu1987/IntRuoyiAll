package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitIterationReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccRevisionLibraryPayloadTest {
    private final DccControlledFileRevisionServiceImpl service = new DccControlledFileRevisionServiceImpl();
    private final DccControlledFileSubmitIterationReqVO request = new DccControlledFileSubmitIterationReqVO();
    private final DccControlledFileDO body = DccControlledFileDO.builder()
            .id(21L).versionNo("A/1-1").sourceOriginalFileName("library.txt").build();

    @BeforeEach void setup() {
        var attachments = mock(DccControlledFileAttachmentService.class);
        var manifest = new DccControlledFileAttachmentService.OrdinaryAttachmentManifest(List.of(), "a".repeat(64));
        when(attachments.readOrdinaryAttachmentManifest(99L, 21L)).thenReturn(manifest);
        ReflectionTestUtils.setField(service, "attachmentService", attachments);
        request.setChangeDescription("library revision");
        request.setSelectedDistributionDepartmentIds(List.of(201L));
        request.setExpectedOrdinaryAttachmentIds(manifest.attachmentIds());
        request.setExpectedOrdinaryAttachmentManifestHash(manifest.payloadHash());
    }

    @ParameterizedTest
    @CsvSource({"QMS,PARTIAL", "QMS,REPLACEMENT", "GENERAL,PARTIAL", "GENERAL,REPLACEMENT"})
    void libraryNotApplicableAttributesHaveStableRevisionPayloadHash(String scope, String intent) {
        request.setRevisionChangeType(intent);
        request.setProjectAttributes(DccProjectAttributes.forLibrary(scope));
        String hash = hash();
        assertTrue(hash.matches("[a-f0-9]{64}"));
        assertEquals(hash, hash());
        assertNull(request.getProjectAttributes().targetMarkets());
        assertEquals(scope, request.getProjectAttributes().libraryScope());
    }

    @Test void libraryScopeRemainsPartOfIdempotencyIdentity() {
        request.setProjectAttributes(DccProjectAttributes.forLibrary("QMS"));
        String qms = hash();
        request.setProjectAttributes(DccProjectAttributes.forLibrary("GENERAL"));
        assertNotEquals(qms, hash());
    }

    @Test void projectMarketsKeepExistingOrderIndependentHash() {
        request.setProjectAttributes(new DccProjectAttributes(List.of("CE", "FDA"), null, "Y", "N", "N", null));
        String original = hash();
        request.setProjectAttributes(new DccProjectAttributes(List.of("FDA", "CE"), null, "Y", "N", "N", null));
        assertEquals(original, hash());
    }

    @Test void libraryCannotSupplyFakeProjectAttributes() {
        request.setProjectAttributes(new DccProjectAttributes(List.of("NA"), null, "Y", "N", "N", null, "GENERAL"));
        assertThrows(RuntimeException.class, this::hash);
    }

    private String hash() {
        return ReflectionTestUtils.invokeMethod(service, "payloadHash", 99L, 20L, 21L, request, body);
    }
}
