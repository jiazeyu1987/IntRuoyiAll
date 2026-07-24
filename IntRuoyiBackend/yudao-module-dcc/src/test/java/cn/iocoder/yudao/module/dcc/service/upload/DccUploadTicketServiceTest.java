package cn.iocoder.yudao.module.dcc.service.upload;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_TICKET_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DccUploadTicketServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileTemporaryFileMapper temporaryFileMapper;
    @Mock
    private FileMapper fileMapper;
    @Mock
    private FileService fileService;

    @InjectMocks
    private DccUploadTicketServiceImpl uploadTicketService;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(31L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void createTicket_persistsAvailableTemporaryFileWithoutExposingStorageId() {
        doAnswer(invocation -> {
            DccControlledFileTemporaryFileDO temporaryFile = invocation.getArgument(0);
            temporaryFile.setId(9001L);
            return 1;
        }).when(temporaryFileMapper).insert(any(DccControlledFileTemporaryFileDO.class));

        DccUploadTicketCreated created = uploadTicketService.createTicket(new DccUploadTicketCreateCommand(
                99L, 10L, "session-1", "SOURCE", 700L, "sample.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 4L,
                "docx".getBytes(), "req-1"));

        assertNotNull(created.uploadTicket());
        assertEquals("session-1", created.sessionId());
        assertEquals("SOURCE", created.purpose());
        assertEquals("AVAILABLE", created.status());
        assertNotNull(created.expireTime());
        ArgumentCaptor<DccControlledFileTemporaryFileDO> tempCaptor =
                ArgumentCaptor.forClass(DccControlledFileTemporaryFileDO.class);
        verify(temporaryFileMapper).insert(tempCaptor.capture());
        assertEquals(created.uploadTicket(), tempCaptor.getValue().getUploadTicket());
        assertEquals("session-1", tempCaptor.getValue().getSessionId());
        assertEquals("SOURCE", tempCaptor.getValue().getPurpose());
        assertEquals(99L, tempCaptor.getValue().getUploaderId());
        assertEquals(700L, tempCaptor.getValue().getStorageFileId());
        assertEquals("AVAILABLE", tempCaptor.getValue().getStatus());
        assertEquals("ACTIVE", tempCaptor.getValue().getCleanupStatus());
        assertEquals("req-1", tempCaptor.getValue().getRequestId());
        assertEquals("584cb925e6ad45273e46037369c5ec3a5d7cfdd409ce13a69e7087f8accd1c79",
                tempCaptor.getValue().getFileSha256());
    }

    @Test
    void resolveAndMarkBound_acceptsOnlySameUserSessionPurposeAvailableAndUnexpiredTicket() {
        LocalDateTime future = LocalDateTime.now().plusMinutes(20);
        when(temporaryFileMapper.selectOne(org.mockito.ArgumentMatchers.<SFunction<DccControlledFileTemporaryFileDO, ?>>any(),
                eq("UT-1"))).thenReturn(temporaryFile("UT-1", 99L, "session-1", "SOURCE", "AVAILABLE", future, null));
        when(fileMapper.selectById(700L)).thenReturn(FileDO.builder()
                .id(700L)
                .name("sample.docx")
                .type("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .build());
        when(temporaryFileMapper.update(eq(null), any(UpdateWrapper.class))).thenReturn(1);

        DccUploadTicketBoundFile file = uploadTicketService.resolveForBinding(new DccUploadTicketResolveCommand(
                "UT-1", 99L, "session-1", "SOURCE"));
        uploadTicketService.markBound(new DccUploadTicketMarkBoundCommand(
                "UT-1", 99L, "session-1", "SOURCE", 900L));

        assertEquals("UT-1", file.uploadTicket());
        assertEquals(700L, file.storageFileId());
        assertEquals("sample.docx", file.fileName());
        verify(temporaryFileMapper).update(eq(null), any(UpdateWrapper.class));
    }

    @Test
    void resolveForBinding_rejectsCrossUserCrossSessionWrongPurposeExpiredOrBoundTicket() {
        LocalDateTime future = LocalDateTime.now().plusMinutes(20);
        assertInvalid(temporaryFile("UT-1", 100L, "session-1", "SOURCE", "AVAILABLE", future, null));
        assertInvalid(temporaryFile("UT-1", 99L, "other-session", "SOURCE", "AVAILABLE", future, null));
        assertInvalid(temporaryFile("UT-1", 99L, "session-1", "DRAWING_PDF", "AVAILABLE", future, null));
        assertInvalid(temporaryFile("UT-1", 99L, "session-1", "SOURCE", "AVAILABLE",
                LocalDateTime.now().minusSeconds(1), null));
        assertInvalid(temporaryFile("UT-1", 99L, "session-1", "SOURCE", "BOUND", future, 900L));
    }

    @Test
    void createTicket_requiresTenantContextBeforeMapperAccess() {
        TenantContextHolder.clear();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> uploadTicketService.createTicket(
                new DccUploadTicketCreateCommand(99L, 10L, "session-1", "SOURCE", 700L, "sample.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 4L,
                        "docx".getBytes(), "req-1")));

        assertEquals("DCC upload ticket requires tenant context", ex.getMessage());
        assertNotNull(ex.getCause());
        assertEquals(NullPointerException.class, ex.getCause().getClass());
        verifyNoInteractions(temporaryFileMapper, fileMapper, fileService);
    }

    @Test
    void resolveForBinding_requiresTenantContextBeforeMapperAccess() {
        TenantContextHolder.clear();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> uploadTicketService.resolveForBinding(
                new DccUploadTicketResolveCommand("UT-1", 99L, "session-1", "SOURCE")));

        assertEquals("DCC upload ticket requires tenant context", ex.getMessage());
        assertNotNull(ex.getCause());
        assertEquals(NullPointerException.class, ex.getCause().getClass());
        verifyNoInteractions(temporaryFileMapper, fileMapper, fileService);
    }

    @Test
    void markBound_requiresTenantContextBeforeMapperAccess() {
        TenantContextHolder.clear();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> uploadTicketService.markBound(
                new DccUploadTicketMarkBoundCommand("UT-1", 99L, "session-1", "SOURCE", 900L)));

        assertEquals("DCC upload ticket requires tenant context", ex.getMessage());
        assertNotNull(ex.getCause());
        assertEquals(NullPointerException.class, ex.getCause().getClass());
        verifyNoInteractions(temporaryFileMapper, fileMapper, fileService);
    }

    @Test
    void cleanupExpiredTemporaryFiles_deletesExpiredAvailableActiveStorageThenMarksCleanupAtomically() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 10, 0);
        DccControlledFileTemporaryFileDO expired = temporaryFile("UT-1", 99L, "session-1",
                "SOURCE", "AVAILABLE", now.minusMinutes(1), null);
        when(temporaryFileMapper.selectList(any())).thenReturn(List.of(expired));
        when(temporaryFileMapper.update(eq(null), any(UpdateWrapper.class))).thenReturn(1);

        int cleaned = uploadTicketService.cleanupExpiredTemporaryFiles(now, 50);

        assertEquals(1, cleaned);
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<UpdateWrapper<DccControlledFileTemporaryFileDO>> updateCaptor =
                ArgumentCaptor.forClass((Class) UpdateWrapper.class);
        InOrder inOrder = inOrder(fileService, temporaryFileMapper);
        inOrder.verify(fileService).deleteFile(700L);
        inOrder.verify(temporaryFileMapper).update(eq(null), updateCaptor.capture());
        String sqlSet = updateCaptor.getValue().getSqlSet();
        assertTrue(sqlSet.contains("cleanup_status"));
        assertTrue(sqlSet.contains("cleanup_reason"));
        assertTrue(sqlSet.contains("cleanup_time"));
    }

    @Test
    void cleanupExpiredTemporaryFiles_skipsBoundOrNotExpiredTemporaryFiles() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 10, 0);
        DccControlledFileTemporaryFileDO bound = temporaryFile("UT-1", 99L, "session-1",
                "SOURCE", "BOUND", now.minusMinutes(1), 900L);
        DccControlledFileTemporaryFileDO notExpired = temporaryFile("UT-2", 99L, "session-1",
                "SOURCE", "AVAILABLE", now.plusMinutes(1), null);
        when(temporaryFileMapper.selectList(any())).thenReturn(List.of(bound, notExpired));

        int cleaned = uploadTicketService.cleanupExpiredTemporaryFiles(now, 50);

        assertEquals(0, cleaned);
        verify(fileService, never()).deleteFile(any(Long.class));
        verify(temporaryFileMapper, never()).update(eq(null), any(UpdateWrapper.class));
    }

    @Test
    void cleanupExpiredTemporaryFiles_exposesDeleteFailureAndDoesNotMarkCleaned() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 10, 0);
        DccControlledFileTemporaryFileDO expired = temporaryFile("UT-1", 99L, "session-1",
                "SOURCE", "AVAILABLE", now.minusMinutes(1), null);
        Exception failure = new Exception("delete failed");
        when(temporaryFileMapper.selectList(any())).thenReturn(List.of(expired));
        doThrow(failure).when(fileService).deleteFile(700L);

        Exception thrown = assertThrows(Exception.class,
                () -> uploadTicketService.cleanupExpiredTemporaryFiles(now, 50));

        assertSame(failure, thrown);
        verify(temporaryFileMapper, never()).update(eq(null), any(UpdateWrapper.class));
    }

    @Test
    void cleanupExpiredTemporaryFiles_requiresTenantContextBeforeMapperAccess() {
        TenantContextHolder.clear();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> uploadTicketService.cleanupExpiredTemporaryFiles(LocalDateTime.now(), 50));

        assertEquals("DCC upload ticket requires tenant context", ex.getMessage());
        assertNotNull(ex.getCause());
        assertEquals(NullPointerException.class, ex.getCause().getClass());
        verifyNoInteractions(temporaryFileMapper, fileMapper, fileService);
    }

    @Test
    void cleanupSessionTemporaryFiles_deletesOnlyCurrentUserSessionUnboundFilesAndMarksUserDiscarded() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 11, 0);
        DccControlledFileTemporaryFileDO candidate = temporaryFile("UT-1", 99L, "session-1",
                "SOURCE", "AVAILABLE", now.plusMinutes(20), null);
        when(temporaryFileMapper.selectList(any())).thenReturn(List.of(candidate));
        when(temporaryFileMapper.update(eq(null), any(UpdateWrapper.class))).thenReturn(1);

        int cleaned = uploadTicketService.cleanupSessionTemporaryFiles(99L, "session-1", now, "USER_DISCARDED");

        assertEquals(1, cleaned);
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<UpdateWrapper<DccControlledFileTemporaryFileDO>> updateCaptor =
                ArgumentCaptor.forClass((Class) UpdateWrapper.class);
        InOrder inOrder = inOrder(fileService, temporaryFileMapper);
        inOrder.verify(fileService).deleteFile(700L);
        inOrder.verify(temporaryFileMapper).update(eq(null), updateCaptor.capture());
        String sqlSet = updateCaptor.getValue().getSqlSet();
        assertTrue(sqlSet.contains("cleanup_status"));
        assertTrue(sqlSet.contains("cleanup_reason"));
        assertTrue(updateCaptor.getValue().getParamNameValuePairs().containsValue("USER_DISCARDED"));
        assertTrue(sqlSet.contains("cleanup_time"));
    }

    @Test
    void getTemporaryFileStatusByRequestId_returnsNonBindableStatusWithoutCapabilityFields() {
        LocalDateTime future = LocalDateTime.now().plusMinutes(20);
        DccControlledFileTemporaryFileDO temporaryFile = temporaryFile("UT-1", 99L, "session-1",
                "SOURCE", "AVAILABLE", future, null);
        temporaryFile.setRequestId("REQ-UPLOAD-001");
        when(temporaryFileMapper.selectList(any())).thenReturn(List.of(temporaryFile));

        DccUploadTemporaryFileStatus status =
                uploadTicketService.getTemporaryFileStatusByRequestId(99L, "REQ-UPLOAD-001");

        assertEquals("REQ-UPLOAD-001", status.requestId());
        assertEquals(1, status.temporaryFileCount());
        assertTrue(status.bindable());
        assertEquals("AVAILABLE", status.status());
        assertEquals("ACTIVE", status.cleanupStatus());
        assertEquals("SOURCE", status.purpose());
    }

    @Test
    void getTemporaryFileStatusByRequestId_missingRowReturnsFalseWithoutExposingMissingAsSuccess() {
        when(temporaryFileMapper.selectList(any())).thenReturn(List.of());

        DccUploadTemporaryFileStatus status =
                uploadTicketService.getTemporaryFileStatusByRequestId(99L, "REQ-MISSING-001");

        assertEquals("REQ-MISSING-001", status.requestId());
        assertEquals(0, status.temporaryFileCount());
        assertFalse(status.bindable());
    }

    private void assertInvalid(DccControlledFileTemporaryFileDO temporaryFile) {
        when(temporaryFileMapper.selectOne(org.mockito.ArgumentMatchers.<SFunction<DccControlledFileTemporaryFileDO, ?>>any(),
                eq("UT-1"))).thenReturn(temporaryFile);

        assertServiceException(() -> uploadTicketService.resolveForBinding(new DccUploadTicketResolveCommand(
                "UT-1", 99L, "session-1", "SOURCE")), CONTROLLED_FILE_UPLOAD_TICKET_INVALID);

        verify(temporaryFileMapper, never()).update(eq(null), any(UpdateWrapper.class));
    }

    private DccControlledFileTemporaryFileDO temporaryFile(String uploadTicket, Long uploaderId, String sessionId,
                                                          String purpose, String status, LocalDateTime expireTime,
                                                          Long boundControlledFileId) {
        return DccControlledFileTemporaryFileDO.builder()
                .id(8001L)
                .uploadTicket(uploadTicket)
                .sessionId(sessionId)
                .purpose(purpose)
                .uploaderId(uploaderId)
                .storageFileId(700L)
                .originalFileName("sample.docx")
                .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .fileSize(4L)
                .fileSha256("hash")
                .status(status)
                .expireTime(expireTime)
                .boundControlledFileId(boundControlledFileId)
                .cleanupStatus("ACTIVE")
                .build();
    }
}
