package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureImageDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureImageMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import cn.iocoder.yudao.module.system.api.permission.dto.ApprovalAuthorityRevisionDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.service.signature.SignatureIdentityLockContext;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DefaultSigningImageSnapshotTest {
    private final DccElectronicSignatureImageMapper images = mock(DccElectronicSignatureImageMapper.class);
    private final AdminUserApi users = mock(AdminUserApi.class);
    private final ApprovalAuthorityApi authority = mock(ApprovalAuthorityApi.class);
    private final FileService files = mock(FileService.class);
    private final cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService audit =
            mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class);
    private final DefaultSignatureImageRenderer renderer = spy(new DefaultSignatureImageRenderer());
    private final DccElectronicSignatureImageServiceImpl service = new DccElectronicSignatureImageServiceImpl();
    private final TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(
            new DriverManagerDataSource("jdbc:h2:mem:default_image_" + UUID.randomUUID(), "sa", "")));
    private byte[] content;

    @BeforeEach
    void setup() throws Exception {
        TenantContextHolder.setTenantId(7L);
        ReflectionTestUtils.setField(service, "signatureImageMapper", images);
        ReflectionTestUtils.setField(service, "users", users);
        ReflectionTestUtils.setField(service, "authority", authority);
        ReflectionTestUtils.setField(service, "fileService", files);
        ReflectionTestUtils.setField(service, "signingAudit", audit);
        ReflectionTestUtils.setField(service, "defaultSignatureImageRenderer", renderer);
        when(authority.lockTenantApprovalAuthority(7L)).thenReturn(new ApprovalAuthorityRevisionDTO(7L, 3L));
        var user = new AdminUserRespDTO();
        user.setId(12L); user.setNickname("张三");
        when(users.getUser(12L)).thenReturn(user);
        doAnswer(invocation -> {
            SignatureIdentityLockContext.registerSystemIdentityLock(7L, 12L);
            return user;
        }).when(users).lockSignatureIdentity(12L);
        when(images.selectIdentityHistoryForUpdate(12L)).thenReturn(List.of());
        content = new DefaultSignatureImageRenderer().render("张三");
        when(files.createFileAndReturnId(any(byte[].class), eq("default-signature-12.png"),
                eq("dcc/signature-images"), eq("image/png"))).thenReturn(61L);
        when(files.getFile(61L)).thenReturn(FileDO.builder().id(61L).configId(1L)
                .name("default-signature-12.png").path("dcc/signature-images/default-signature-12.png").build());
        when(files.getFileContent(1L, "dcc/signature-images/default-signature-12.png")).thenReturn(content);
        doAnswer(invocation -> {
            DccElectronicSignatureImageDO row = invocation.getArgument(0);
            row.setId(31L);
            when(images.selectIdentityHistoryForUpdate(12L)).thenReturn(List.of(row));
            return 1;
        }).when(images).insert(any(DccElectronicSignatureImageDO.class));
    }

    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test
    void firstSigningCapturesPersistedImageAndNextSigningReusesIt() {
        transaction.executeWithoutResult(status -> {
            var first = service.requireSigningSnapshot(12L);
            var next = service.requireSigningSnapshot(12L);
            assertEquals(31L, first.getImageId());
            assertEquals(first.getImageId(), next.getImageId());
            assertEquals("ACTIVE", first.getImageStatus());
            assertEquals("VALID", first.getVerifiedStatus());
            assertEquals(DigestUtil.sha256Hex(content), first.getSha256());
            assertArrayEquals(content, first.getContent());
        });
        verify(images).insert(any(DccElectronicSignatureImageDO.class));
        verify(renderer).render("张三");
        verify(authority).incrementLockedAuthorityRevision(7L, 3L);
        var order = inOrder(audit, users, authority, images);
        order.verify(audit).acquireLedgerLock();
        order.verify(users).lockSignatureIdentity(12L);
        order.verify(authority).lockTenantApprovalAuthority(7L);
        order.verify(images).selectIdentityHistoryForUpdate(12L);
    }

    @Test
    void authenticatedApprovalCaptureCreatesImageWhileRetainingIdentityLock() {
        transaction.executeWithoutResult(status -> {
            SignatureIdentityLockContext.registerSystemIdentityLock(7L, 12L);
            var snapshot = service.requireActiveSnapshotLocked(12L);
            assertEquals(31L, snapshot.getImageId());
            assertEquals(DigestUtil.sha256Hex(content), snapshot.getSha256());
        });
        verify(users, never()).lockSignatureIdentity(anyLong());
        verify(images).insert(any(DccElectronicSignatureImageDO.class));
    }

    @Test
    void signingDoesNotReplaceDisabledUploadedOrInvalidActiveHistory() {
        for (String state : List.of("DISABLED", "UPLOADED", "SUPERSEDED")) {
            when(images.selectIdentityHistoryForUpdate(12L)).thenReturn(List.of(
                    DccElectronicSignatureImageDO.builder().id(31L).userId(12L)
                            .active(false).imageStatus(state).build()));
            transaction.executeWithoutResult(status ->
                    assertThrows(RuntimeException.class, () -> service.requireSigningSnapshot(12L)));
        }
        verifyNoInteractions(renderer, files);
        verify(images, never()).insert(any(DccElectronicSignatureImageDO.class));
    }

    @Test
    void historicalReadDoesNotGenerateAnImage() {
        assertThrows(RuntimeException.class, () -> service.requireActiveSnapshot(12L));
        verifyNoInteractions(renderer, users, authority, files);
        verify(images, never()).insert(any(DccElectronicSignatureImageDO.class));
    }

    @Test
    void corruptedExistingFileFailsInsteadOfGeneratingReplacement() {
        when(images.selectIdentityHistoryForUpdate(12L)).thenReturn(List.of(
                DccElectronicSignatureImageDO.builder().id(31L).userId(12L).fileId(61L)
                        .active(true).imageStatus("ACTIVE").sha256("bad-hash").build()));
        transaction.executeWithoutResult(status ->
                assertThrows(RuntimeException.class, () -> service.requireSigningSnapshot(12L)));
        verifyNoInteractions(renderer);
        verify(images, never()).insert(any(DccElectronicSignatureImageDO.class));
    }

    @Test
    void readinessAcceptsGeneratableNameWithoutPersistingAnything() {
        when(images.selectListByUserId(12L)).thenReturn(List.of());
        assertTrue(service.canInitializeDefaultSignatureImage(12L));
        verifyNoInteractions(files, authority);
        verify(images, never()).insert(any(DccElectronicSignatureImageDO.class));
    }

    @Test
    void storageFailureStopsSigningBeforeImageRecordIsInserted() {
        when(files.createFileAndReturnId(any(byte[].class), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("storage unavailable"));
        transaction.executeWithoutResult(status -> assertEquals("storage unavailable",
                assertThrows(IllegalStateException.class, () -> service.requireSigningSnapshot(12L)).getMessage()));
        verify(images, never()).insert(any(DccElectronicSignatureImageDO.class));
        verify(authority, never()).incrementLockedAuthorityRevision(anyLong(), anyLong());
    }
}
