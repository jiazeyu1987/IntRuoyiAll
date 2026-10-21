package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAttachmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAttachmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual service inside private read/write Spring transactions; mapper/directory/storage ports isolated. */
class DccAttachmentManifestReadOnlyTest {
    private DccControlledFileAttachmentServiceImpl service;
    private DccControlledFileMapper files;
    private DccControlledFileAttachmentMapper attachments;
    private DccControlledFileAssignmentScopeService scope;
    private DccControlledFileCategoryPermissionSupport permissions;
    private DccControlledFileSourceOwnershipService ownership;
    private TransactionTemplate readTx;
    private TransactionTemplate writeTx;
    private DccControlledFileDO selected;
    private DccControlledFileAttachmentDO ordinary;

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(1L);
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:attachment_read_" + UUID.randomUUID());
        var transactionManager = new DataSourceTransactionManager(dataSource);
        readTx = new TransactionTemplate(transactionManager);
        readTx.setReadOnly(true);
        writeTx = new TransactionTemplate(transactionManager);
        service = new DccControlledFileAttachmentServiceImpl();
        files = mock(DccControlledFileMapper.class);
        attachments = mock(DccControlledFileAttachmentMapper.class);
        scope = mock(DccControlledFileAssignmentScopeService.class);
        permissions = mock(DccControlledFileCategoryPermissionSupport.class);
        ownership = mock(DccControlledFileSourceOwnershipService.class);
        var projectAccess = mock(DccProjectAccessService.class);
        var users = mock(AdminUserApi.class);
        var storage = mock(FileMapper.class);
        ReflectionTestUtils.setField(service, "controlledFileMapper", files);
        ReflectionTestUtils.setField(service, "attachmentMapper", attachments);
        ReflectionTestUtils.setField(service, "assignmentScopeService", scope);
        ReflectionTestUtils.setField(service, "permissionSupport", permissions);
        ReflectionTestUtils.setField(service, "sourceOwnershipService", ownership);
        ReflectionTestUtils.setField(service, "projectAccessService", projectAccess);
        ReflectionTestUtils.setField(service, "users", users);
        ReflectionTestUtils.setField(service, "fileMapper", storage);
        ReflectionTestUtils.setField(service, "publicUploadPlacementService", mock(DccPublicUploadPlacementService.class));
        selected = version(20L);
        // Both ports return the same facts; current code must fail the locking-choice assertion, not fixture setup.
        when(files.selectById(20L)).thenAnswer(call -> selected);
        when(files.selectByIdAndTenantForUpdate(1L, 20L)).thenAnswer(call -> selected);
        when(users.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0));
        when(scope.isWithinAssignedFileScope(99L, 20L)).thenReturn(true);
        when(permissions.hasCategoryPermission(2L, 99L, DccFileCategoryPermissionActionEnum.UPLOAD)).thenReturn(true);
        ordinary = DccControlledFileAttachmentDO.builder().id(700L).controlledFileId(20L).storageFileId(111L)
                .originalFileName("DCC241016C15-notes.pdf").contentType("application/pdf").fileSize(12L)
                .fileSha256("a".repeat(64)).sortNo(0).attachmentPurpose("ATTACHMENT").uploadedBy(99L)
                .uploadedUsernameSnapshot("DCC241016C15U").uploadedNicknameSnapshot("本例作者")
                .uploadedAt(LocalDateTime.of(2026, 10, 17, 2, 0)).build();
        ordinary.setTenantId(1L);
        when(attachments.selectListByControlledFileId(20L)).thenAnswer(call -> List.of(ordinary));
        when(storage.selectById(111L)).thenReturn(FileDO.builder().id(111L).name(ordinary.getOriginalFileName())
                .type("application/pdf").size(12L).configId(1L).path("task-owned/notes.pdf").build());
        when(ownership.inspectSource(111L)).thenReturn(new DccControlledFilePreparedSource(111L, 111L, "a".repeat(64), false));
    }

    @AfterEach
    void clearIdentity() {
        TenantContextHolder.clear();
    }

    @Test
    void manifestReadUsesNonLockingVersionSelectAndPreservesVerifiedFacts() {
        var manifest = readTx.execute(status -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            return service.readOrdinaryAttachmentManifest(99L, 20L);
        });
        assertNotNull(manifest);
        assertEquals(List.of(700L), manifest.attachmentIds());
        assertEquals("a".repeat(64), manifest.facts().get(0).fileSha256());
        assertEquals(20L, manifest.facts().get(0).controlledFileId());
        assertTrue(manifest.payloadHash().matches("[a-f0-9]{64}"));
        verify(files).selectById(20L);
        verify(files, never()).selectByIdAndTenantForUpdate(anyLong(), anyLong());
        verify(ownership).inspectSource(111L);
        verify(attachments, never()).insert(any(DccControlledFileAttachmentDO.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TENANT", "VERSION_ID", "DELETED", "MASTER"})
    void manifestReadStillRejectsWrongVersionIdentity(String variant) {
        switch (variant) {
            case "TENANT" -> selected.setTenantId(2L);
            case "VERSION_ID" -> selected.setId(21L);
            case "DELETED" -> selected.setDeleted(true);
            case "MASTER" -> selected.setMasterId(null);
        }
        var failure = assertThrows(IllegalArgumentException.class,
                () -> readTx.execute(status -> service.readOrdinaryAttachmentManifest(99L, 20L)));
        assertEquals("普通附件对应的精确版本或租户不存在", failure.getMessage());
        verifyNoInteractions(attachments);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TENANT", "VERSION_ID"})
    void manifestReadStillRejectsForeignAttachmentFacts(String variant) {
        if ("TENANT".equals(variant)) ordinary.setTenantId(2L);
        else ordinary.setControlledFileId(21L);
        var failure = assertThrows(IllegalArgumentException.class,
                () -> readTx.execute(status -> service.readOrdinaryAttachmentManifest(99L, 20L)));
        assertEquals("附件清单含错误版本、租户或未记录用途的事实", failure.getMessage());
        verifyNoInteractions(ownership);
    }

    @ParameterizedTest
    @ValueSource(strings = {"FREEZE", "CHECKIN"})
    void attachmentWriteRetainsBothVersionLocksAndRejectsCrossTenantTarget(String operation) {
        var target = version(21L);
        target.setTenantId(2L);
        when(files.selectByIdAndTenantForUpdate(1L, 21L)).thenReturn(target);
        var failure = assertThrows(IllegalArgumentException.class, () -> writeTx.execute(status -> {
            assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            if ("FREEZE".equals(operation)) service.freezeOrdinaryAttachments(99L, 20L, 21L, List.of());
            else service.bindCheckinAttachments(99L, 20L, 21L, List.of());
            return null;
        }));
        assertEquals("普通附件对应的精确版本或租户不存在", failure.getMessage());
        verify(files).selectByIdAndTenantForUpdate(1L, 20L);
        verify(files).selectByIdAndTenantForUpdate(1L, 21L);
        verify(files, never()).selectById(anyLong());
        verifyNoInteractions(attachments, ownership);
    }

    private static DccControlledFileDO version(Long id) {
        return DccControlledFileDO.builder().id(id).tenantId(1L).masterId(10L).dccProjectCodeId(5L).categoryId(2L)
                .fileTypeTaxonomyId(6L).directoryId(3L).requesterId(99L).fileNumber("DCC241016C15DOC")
                .versionNo("A/1/2").status("WORKING").build();
    }
}
