package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureImageDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureImageMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi;
import cn.iocoder.yudao.module.system.api.permission.dto.ApprovalAuthorityRevisionDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Shared image writer ordering, with explicit repository/SPI boundaries; not physical DB lock proof. */
class ApprovalSharedImageWriterLockTest {
    private final DccElectronicSignatureImageMapper images = mock(DccElectronicSignatureImageMapper.class);
    private final AdminUserApi users = mock(AdminUserApi.class);
    private final ApprovalAuthorityApi authority = mock(ApprovalAuthorityApi.class);
    private final FileService files = mock(FileService.class);
    private final DccElectronicSignatureImageServiceImpl service = new DccElectronicSignatureImageServiceImpl();
    private final TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(
            new DriverManagerDataSource("jdbc:h2:mem:shared_image_" + UUID.randomUUID()
                    + ";DB_CLOSE_DELAY=-1", "sa", "")));

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(7L);
        ReflectionTestUtils.setField(service, "signatureImageMapper", images);
        ReflectionTestUtils.setField(service, "users", users);
        ReflectionTestUtils.setField(service, "authority", authority);
        ReflectionTestUtils.setField(service, "fileService", files);
        when(authority.lockTenantApprovalAuthority(7L)).thenReturn(new ApprovalAuthorityRevisionDTO(7L, 3L));
        var image = DccElectronicSignatureImageDO.builder().id(31L).userId(12L)
                .active(true).imageStatus("ACTIVE").referencedCount(0).build();
        when(images.selectIdentityHistoryForUpdate(12L)).thenReturn(List.of(image));
        when(images.updateById(any(DccElectronicSignatureImageDO.class))).thenReturn(0);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void failedDisableLocksIdentityAuthorityHistoryAndDoesNotAdvanceRevision() {
        transaction.executeWithoutResult(status -> assertThrows(RuntimeException.class,
                () -> service.disableMySignatureImage(12L, 12L, "停用本人图片")));
        var ordered = inOrder(users, authority, images);
        ordered.verify(users).lockSignatureIdentity(12L);
        ordered.verify(authority).lockTenantApprovalAuthority(7L);
        ordered.verify(images).selectIdentityHistoryForUpdate(12L);
        ordered.verify(images).updateById(any(DccElectronicSignatureImageDO.class));
        verify(authority, never()).incrementLockedAuthorityRevision(anyLong(), anyLong());
    }

    @Test void lockedSignatureReadCannotBorrowAnUnheldIdentity() {
        transaction.executeWithoutResult(status -> {
            assertThrows(IllegalStateException.class, () -> service.requireActiveSnapshotLocked(12L));
            assertThrows(IllegalStateException.class, () -> service.markReferencedLocked(12L, 31L));
        });
        verifyNoInteractions(images, users, authority, files);
    }

    @Test void anotherOperatorsMutationIsRefusedBeforeLockOrWrite() {
        assertThrows(RuntimeException.class, () -> service.disableMySignatureImage(12L, 13L, "停用"));
        verifyNoInteractions(images, users, authority, files);
    }
}
