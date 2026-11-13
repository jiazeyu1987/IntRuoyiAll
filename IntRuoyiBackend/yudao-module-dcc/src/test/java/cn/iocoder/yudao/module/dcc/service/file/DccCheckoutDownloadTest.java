package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileCheckoutDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileCheckoutMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileDownloadRecordMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileDownloadRecordDO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DccCheckoutDownloadTest {
    private final DccAttachmentDownloadTest fixture = new DccAttachmentDownloadTest();
    private DccControlledFileCheckoutDO checkout;

    @BeforeEach void setup() throws Exception {
        fixture.setup();
        fixture.file.setCheckedOutBy(99L);
        when(fixture.attachments.listControlledFileIdsByStorageFileId(111L)).thenReturn(java.util.List.of(20L));
        cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessService business = fixture.port("businessFileAccessService");
        when(business.assertAllowed(any())).thenAnswer(call -> {
            fixture.service.assertBusinessFileAccess(99L, 20L,
                    cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation.DOWNLOAD, fixture.audit);
            return java.util.Optional.of(new cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference(
                    "dcc", "DCC_CONTROLLED_FILE", 20L, "A/1", 1L, null));
        });
        fixture.file.setSourceFileId(112L);
        checkout = DccControlledFileCheckoutDO.builder().id(1L).masterId(700L).baseIterationId(20L)
                .actorId(99L).status("ACTIVE").build();
        checkout.setTenantId(1L);
        DccControlledFileCheckoutMapper mapper = fixture.port("checkoutMapper");
        when(mapper.selectActiveByMasterIdForRead(1L, 700L)).thenReturn(checkout);
        when(fixture.files.selectById(112L)).thenReturn(FileDO.builder().id(112L).configId(1L)
                .path("task/source.docx").name("source.docx")
                .type("application/vnd.openxmlformats-officedocument.wordprocessingml.document").build());
        when(fixture.storage.getFileContent(1L, "task/source.docx")).thenReturn(new byte[]{1, 2, 3});
    }
    @AfterEach void clear() { fixture.clear(); }
    private cn.iocoder.yudao.module.dcc.service.download.DccDownloadFileBinary download(Long attachmentId) {
        return fixture.service.readCheckoutDownloadFile(99L, 20L, attachmentId, true, "checkout-request", fixture.audit);
    }
    @Test void ownerDownloadsWordOriginalAndAuditCapturesActorTimeVersion() {
        assertEquals("source.docx", download(null).fileName());
        verify(fixture.files, never()).selectById(44L);
        DccControlledFileAccessEventMapper events = fixture.port("accessEventMapper");
        var event = ArgumentCaptor.forClass(DccControlledFileAccessEventDO.class);
        verify(events).insert(event.capture());
        assertEquals("CHECKOUT_WORD_DOWNLOAD", event.getValue().getPurpose());
        assertEquals(99L, event.getValue().getUserId());
        assertEquals("A/1", event.getValue().getFileVersionNo());
        assertNotNull(event.getValue().getOccurredAt());
        // The special authorization ends with this request; ordinary downloads stay denied.
        assertThrows(RuntimeException.class, () -> fixture.service.readEditableDownloadFile(
                99L, 20L, true, "ordinary-request", fixture.audit));
    }
    @Test void workingVersionOwnerCanDownloadExactAttachmentWithoutGlobalDownload() {
        fixture.file.setStatus("WORKING");
        assertEquals("notes.json", download(700L).fileName());
        DccControlledFileAccessEventMapper events = fixture.port("accessEventMapper");
        var event = ArgumentCaptor.forClass(DccControlledFileAccessEventDO.class);
        verify(events).insert(event.capture());
        assertEquals("ATTACHMENT_DOWNLOAD:700", event.getValue().getPurpose());
        assertEquals("A/1", event.getValue().getFileVersionNo());
    }
    @Test void anotherActorCannotUseTheCheckout() {
        checkout.setActorId(88L);
        assertThrows(RuntimeException.class, () -> download(null));
        assertThrows(RuntimeException.class, () -> download(700L));
        verifyNoInteractions(fixture.storage);
    }
    @Test void endedCheckoutAndAnotherVersionOfSameMasterAreDenied() {
        checkout.setStatus("CHECKED_IN");
        assertThrows(RuntimeException.class, () -> download(null));
        checkout.setStatus("ACTIVE"); checkout.setBaseIterationId(19L);
        assertThrows(RuntimeException.class, () -> download(700L));
        verifyNoInteractions(fixture.storage);
    }
    @Test void archiveAndObsoleteVersionsNeverInheritCheckoutAuthorization() {
        for (String status : new String[]{"OBSOLETE", "SUPERSEDED"}) {
            fixture.file.setStatus(status);
            assertThrows(RuntimeException.class, () -> download(null));
            assertThrows(RuntimeException.class, () -> download(700L));
        }
        verifyNoInteractions(fixture.storage);
    }
    @Test void tenantDeletionAndReleasedFileLockAreDenied() {
        fixture.file.setTenantId(2L);
        assertThrows(RuntimeException.class, () -> download(null));
        fixture.file.setTenantId(1L); fixture.file.setDeleted(true);
        assertThrows(RuntimeException.class, () -> download(null));
        fixture.file.setDeleted(false); fixture.file.setCheckedOutBy(null);
        assertThrows(RuntimeException.class, () -> download(null));
        fixture.file.setCheckedOutBy(99L); TenantContextHolder.setIgnore(true);
        assertThrows(RuntimeException.class, () -> download(null));
        verifyNoInteractions(fixture.storage);
    }
    @Test void failedAuditNeverReturnsBinary() {
        DccControlledFileDownloadRecordMapper records = fixture.port("downloadRecordMapper");
        when(records.insert(any(DccControlledFileDownloadRecordDO.class))).thenReturn(0);
        assertThrows(RuntimeException.class, () -> download(null));
        verifyNoInteractions(fixture.storage);
    }
    @Test void foreignAttachmentAndMissingWordAreErrors() {
        when(fixture.attachments.getAttachment(20L, 701L)).thenThrow(new IllegalArgumentException("foreign attachment"));
        assertThrows(RuntimeException.class, () -> download(701L));
        fixture.file.setSourceFileId(null);
        assertThrows(RuntimeException.class, () -> download(null));
        verifyNoInteractions(fixture.storage);
    }
}
