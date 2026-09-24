package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDossierFileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderDossierFileMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class MesActiveOrderDossierFileServiceTest {

    @Test
    void pqcReleaseViewerListsActiveOrderFilesBeforeP2WithoutFormalReleaseLookup() {
        var fixture = fixture();
        when(fixture.applicationMapper.selectById(55L)).thenReturn(MesProcessPoolActiveOrderReleaseApplicationDO.builder()
                .id(55L).activeOrderId(10L).workOrderId(20L).build());
        when(fixture.activeOrderMapper.selectById(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L).leaderUserId(99L).workOrderId(20L).build());
        when(fixture.dossierFileMapper.selectListByActiveOrderId(10L)).thenReturn(List.of(
                dossierFile(8001L, 10L, 55L, "INCOMING_INSPECTION_FILE", 7001L, "incoming.pdf")));

        var result = fixture.service.list(7L, new MesActiveOrderDossierFileService.Query(10L, 55L));

        assertEquals(10L, result.activeOrderId());
        assertEquals(55L, result.applicationId());
        assertEquals(4, result.categories().size());
        assertEquals("来料检文件", result.categories().get(0).label());
        assertEquals(1, result.categories().get(0).files().size());
        assertEquals("incoming.pdf", result.categories().get(0).files().get(0).fileName());
        assertTrue(result.categories().stream().skip(1).allMatch(category -> category.files().isEmpty()));

        var order = inOrder(fixture.applicationMapper, fixture.activeOrderMapper,
                fixture.dossierFileMapper);
        order.verify(fixture.applicationMapper).selectById(55L);
        order.verify(fixture.activeOrderMapper).selectById(10L);
        order.verify(fixture.dossierFileMapper).selectListByActiveOrderId(10L);
        verify(fixture.fileService, never()).createFileAndReturnId(any(byte[].class), any(), any(), any());
    }

    @Test
    void leaderListsEmptyActiveOrderDossierTabsBeforeP2() {
        var fixture = fixture();
        when(fixture.activeOrderMapper.selectById(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L).leaderUserId(7L).workOrderId(20L).build());
        when(fixture.dossierFileMapper.selectListByActiveOrderId(10L)).thenReturn(List.of());

        var result = fixture.service.list(7L, new MesActiveOrderDossierFileService.Query(10L, null));

        assertEquals(4, result.categories().size());
        assertTrue(result.categories().stream().allMatch(category -> category.files().isEmpty()));
        verify(fixture.dossierFileMapper).selectListByActiveOrderId(10L);
    }

    @Test
    void requireReadableFileUsesActiveOrderApplicationWithoutP2Lookup() {
        var fixture = fixture();
        MesProcessPoolActiveOrderDossierFileDO row = dossierFile(
                8001L, 10L, 55L, "INCOMING_INSPECTION_FILE", 7001L, "incoming.pdf");
        when(fixture.dossierFileMapper.selectListByFileId(7001L)).thenReturn(List.of(row));
        when(fixture.applicationMapper.selectById(55L)).thenReturn(
                MesProcessPoolActiveOrderReleaseApplicationDO.builder()
                        .id(55L).activeOrderId(10L).workOrderId(20L).build());
        when(fixture.activeOrderMapper.selectById(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L).leaderUserId(99L).workOrderId(20L).build());

        MesProcessPoolActiveOrderDossierFileDO readable = fixture.service.requireReadableFile(7L, 7001L);

        assertEquals(8001L, readable.getId());
        assertEquals(10L, readable.getActiveOrderId());
        verify(fixture.dossierFileMapper).selectListByFileId(7001L);
        verify(fixture.applicationMapper).selectById(55L);
        verify(fixture.activeOrderMapper).selectById(10L);
    }

    @Test
    void requireReadableFileRejectsDuplicateOwnerRows() {
        var fixture = fixture();
        when(fixture.dossierFileMapper.selectListByFileId(7001L)).thenReturn(List.of(
                dossierFile(8001L, 10L, null, "INCOMING_INSPECTION_FILE", 7001L, "incoming-a.pdf"),
                dossierFile(8002L, 10L, null, "INCOMING_INSPECTION_FILE", 7001L, "incoming-b.pdf")));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> fixture.service.requireReadableFile(7L, 7001L));

        assertTrue(ex.getMessage().contains("归属不唯一"));
        verify(fixture.activeOrderMapper, never()).selectById(any());
    }

    @Test
    void uploadStoresActiveOrderDossierBeforeP2WithRealMetadata() {
        var fixture = fixture();
        when(fixture.activeOrderMapper.selectById(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L).leaderUserId(7L).workOrderId(20L).build());
        when(fixture.adminUserApi.getUser(7L)).thenReturn(user(7L, "生产组长"));
        byte[] content = "pdf-content".getBytes(StandardCharsets.UTF_8);
        when(fixture.fileService.createFileAndReturnId(eq(content), eq("incoming.pdf"),
                eq("mes/active-order-dossier/10/INCOMING_INSPECTION_FILE"), eq("application/pdf")))
                .thenReturn(7001L);
        when(fixture.fileService.getFile(7001L)).thenReturn(FileDO.builder()
                .id(7001L).configId(1L).name("incoming.pdf")
                .path("mes/active-order-dossier/10/INCOMING_INSPECTION_FILE/incoming.pdf")
                .url("http://file/incoming.pdf").type("application/pdf").size((long) content.length).build());
        when(fixture.dossierFileMapper.insert(any(MesProcessPoolActiveOrderDossierFileDO.class)))
                .thenAnswer(invocation -> {
                    MesProcessPoolActiveOrderDossierFileDO row = invocation.getArgument(0);
                    row.setId(8001L);
                    return 1;
                });

        var file = fixture.service.upload(7L, new MesActiveOrderDossierFileService.UploadCommand(
                10L, null, "INCOMING_INSPECTION_FILE", "incoming.pdf", "application/pdf", content));

        assertEquals(8001L, file.attachmentId());
        assertEquals(7001L, file.fileId());
        assertEquals("incoming.pdf", file.fileName());
        assertEquals("生产组长", file.operatorName());
        assertEquals((long) content.length, file.fileSize());
        assertEquals("3c41d3835155c97d51a836c887be9c0063b7b45f61e14017a9d653fa4c655802", file.sha256());
        var captor = org.mockito.ArgumentCaptor.forClass(MesProcessPoolActiveOrderDossierFileDO.class);
        verify(fixture.dossierFileMapper).insert(captor.capture());
        MesProcessPoolActiveOrderDossierFileDO row = captor.getValue();
        assertEquals(10L, row.getActiveOrderId());
        assertNull(row.getApplicationId());
        assertEquals("INCOMING_INSPECTION_FILE", row.getCategoryKey());
        assertEquals(7001L, row.getFileId());
        assertEquals("生产组长", row.getOperatorName());
        assertFalse(row.getStoragePath().contains("batch"));
        var auditCaptor = org.mockito.ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(fixture.operationAuditService).recordInCallerTransaction(auditCaptor.capture());
        assertEquals("DOSSIER_UPLOAD", auditCaptor.getValue().getOperationType());
        assertEquals("3c41d3835155c97d51a836c887be9c0063b7b45f61e14017a9d653fa4c655802",
                auditCaptor.getValue().getAfterSummaryHash());
        assertTrue(auditCaptor.getValue().getMetadataJson().contains("\"activeOrderId\":10"));
    }

    @Test
    void uploadFailsBeforeFileWriteWhenActiveOrderIsMissing() {
        var fixture = fixture();
        when(fixture.activeOrderMapper.selectById(10L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> fixture.service.upload(7L,
                new MesActiveOrderDossierFileService.UploadCommand(
                        10L, null, "INCOMING_INSPECTION_FILE", "incoming.pdf", "application/pdf",
                        "pdf".getBytes(StandardCharsets.UTF_8))));

        assertTrue(ex.getMessage().contains("活跃订单不存在"));
        verify(fixture.fileService, never()).createFileAndReturnId(any(byte[].class), any(), any(), any());
        verify(fixture.dossierFileMapper, never()).insert(any(MesProcessPoolActiveOrderDossierFileDO.class));
    }

    @Test
    void uploadRejectsReleasedActiveOrderBeforeFileWrite() {
        var fixture = fixture();
        when(fixture.activeOrderMapper.selectById(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L).leaderUserId(7L).workOrderId(20L).build());
        when(fixture.applicationMapper.selectLatestByActiveOrderId(10L)).thenReturn(
                MesProcessPoolActiveOrderReleaseApplicationDO.builder()
                        .id(55L).activeOrderId(10L).applicationStatus("RELEASED").build());

        ServiceException ex = assertThrows(ServiceException.class, () -> fixture.service.upload(7L,
                new MesActiveOrderDossierFileService.UploadCommand(
                        10L, null, "INCOMING_INSPECTION_FILE", "incoming.pdf", "application/pdf",
                        "pdf".getBytes(StandardCharsets.UTF_8))));

        assertTrue(ex.getMessage().contains("已上市放行"));
        verify(fixture.fileService, never()).createFileAndReturnId(any(byte[].class), any(), any(), any());
        verify(fixture.dossierFileMapper, never()).insert(any(MesProcessPoolActiveOrderDossierFileDO.class));
        verify(fixture.operationAuditService, never()).recordInCallerTransaction(any());
    }

    @Test
    void deleteRejectsVoidedActiveOrderBeforePhysicalDelete() throws Exception {
        var fixture = fixture();
        when(fixture.activeOrderMapper.selectById(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L).leaderUserId(7L).workOrderId(20L).build());
        when(fixture.applicationMapper.selectLatestByActiveOrderId(10L)).thenReturn(
                MesProcessPoolActiveOrderReleaseApplicationDO.builder()
                        .id(55L).activeOrderId(10L).applicationStatus("PQC_RELEASE_REJECTED").build());
        when(fixture.nonconformanceReviewMapper.selectLatestBySource("PQC_RELEASE", 55L)).thenReturn(
                MesProEdhrNonconformanceReviewDO.builder()
                        .id(66L).sourceType("PQC_RELEASE").sourceId(55L)
                        .reviewStatus("closed").disposition("void").build());

        ServiceException ex = assertThrows(ServiceException.class, () -> fixture.service.delete(7L,
                new MesActiveOrderDossierFileService.DeleteCommand(
                        10L, null, "INCOMING_INSPECTION_FILE", 8001L)));

        assertTrue(ex.getMessage().contains("已作废"));
        verify(fixture.dossierFileMapper, never()).selectById(8001L);
        verify(fixture.dossierFileMapper, never()).deleteById(8001L);
        verify(fixture.fileService, never()).deleteFile(7001L);
        verify(fixture.operationAuditService, never()).recordInCallerTransaction(any());
    }

    @Test
    void deleteRemovesOnlyTheCurrentActiveOrderFileAndOwnedInfraFile() throws Exception {
        var fixture = fixture();
        when(fixture.activeOrderMapper.selectById(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L).leaderUserId(7L).workOrderId(20L).build());
        when(fixture.adminUserApi.getUser(7L)).thenReturn(user(7L, "生产组长"));
        when(fixture.dossierFileMapper.selectById(8001L)).thenReturn(
                dossierFile(8001L, 10L, null, "INCOMING_INSPECTION_FILE", 7001L, "incoming.pdf"));
        when(fixture.fileService.getFile(7001L)).thenReturn(FileDO.builder().id(7001L).name("incoming.pdf").build());
        when(fixture.dossierFileMapper.deleteById(8001L)).thenReturn(1);

        fixture.service.delete(7L, new MesActiveOrderDossierFileService.DeleteCommand(
                10L, null, "INCOMING_INSPECTION_FILE", 8001L));

        verify(fixture.dossierFileMapper).deleteById(8001L);
        verify(fixture.fileService).deleteFile(7001L);
        var auditCaptor = org.mockito.ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(fixture.operationAuditService).recordInCallerTransaction(auditCaptor.capture());
        assertEquals("DOSSIER_DELETE", auditCaptor.getValue().getOperationType());
        assertEquals("HASH", auditCaptor.getValue().getBeforeSummaryHash());
        assertNull(auditCaptor.getValue().getAfterSummaryHash());
        assertTrue(auditCaptor.getValue().getMetadataJson().contains("\"activeOrderId\":10"));
    }

    @Test
    void deleteRejectsFileOwnedByAnotherActiveOrderBeforePhysicalDelete() throws Exception {
        var fixture = fixture();
        when(fixture.activeOrderMapper.selectById(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L).leaderUserId(7L).workOrderId(20L).build());
        when(fixture.dossierFileMapper.selectById(8001L)).thenReturn(
                dossierFile(8001L, 99L, null, "INCOMING_INSPECTION_FILE", 7001L, "foreign.pdf"));

        ServiceException ex = assertThrows(ServiceException.class, () -> fixture.service.delete(7L,
                new MesActiveOrderDossierFileService.DeleteCommand(
                        10L, null, "INCOMING_INSPECTION_FILE", 8001L)));

        assertTrue(ex.getMessage().contains("不属于当前活跃订单和资料页签"));
        verify(fixture.dossierFileMapper, never()).deleteById(8001L);
        verify(fixture.fileService, never()).getFile(7001L);
        verify(fixture.fileService, never()).deleteFile(7001L);
    }

    @Test
    void deleteDoesNotReportSuccessWhenOwnerDeleteAffectsNoRow() throws Exception {
        var fixture = fixture();
        when(fixture.activeOrderMapper.selectById(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L).leaderUserId(7L).workOrderId(20L).build());
        when(fixture.adminUserApi.getUser(7L)).thenReturn(user(7L, "生产组长"));
        when(fixture.dossierFileMapper.selectById(8001L)).thenReturn(
                dossierFile(8001L, 10L, null, "INCOMING_INSPECTION_FILE", 7001L, "incoming.pdf"));
        when(fixture.fileService.getFile(7001L)).thenReturn(FileDO.builder().id(7001L).name("incoming.pdf").build());
        when(fixture.dossierFileMapper.deleteById(8001L)).thenReturn(0);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> fixture.service.delete(7L,
                new MesActiveOrderDossierFileService.DeleteCommand(
                        10L, null, "INCOMING_INSPECTION_FILE", 8001L)));

        assertTrue(ex.getMessage().contains("删除资料文件关系失败"));
        verify(fixture.fileService, never()).deleteFile(7001L);
        verify(fixture.operationAuditService, never()).recordInCallerTransaction(any());
    }

    @Test
    void deletePropagatesPhysicalFileDeleteFailure() throws Exception {
        var fixture = fixture();
        when(fixture.activeOrderMapper.selectById(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L).leaderUserId(7L).workOrderId(20L).build());
        when(fixture.adminUserApi.getUser(7L)).thenReturn(user(7L, "生产组长"));
        when(fixture.dossierFileMapper.selectById(8001L)).thenReturn(
                dossierFile(8001L, 10L, null, "INCOMING_INSPECTION_FILE", 7001L, "incoming.pdf"));
        when(fixture.fileService.getFile(7001L)).thenReturn(FileDO.builder().id(7001L).name("incoming.pdf").build());
        when(fixture.dossierFileMapper.deleteById(8001L)).thenReturn(1);
        doThrow(new Exception("storage unavailable")).when(fixture.fileService).deleteFile(7001L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> fixture.service.delete(7L,
                new MesActiveOrderDossierFileService.DeleteCommand(
                        10L, null, "INCOMING_INSPECTION_FILE", 8001L)));

        assertTrue(ex.getMessage().contains("删除资料文件实体失败"));
        assertNotNull(ex.getCause());
        verify(fixture.operationAuditService, never()).recordInCallerTransaction(any());
    }

    private static MesProcessPoolActiveOrderDossierFileDO dossierFile(Long id, Long activeOrderId,
                                                                        Long applicationId, String categoryKey,
                                                                        Long fileId, String fileName) {
        MesProcessPoolActiveOrderDossierFileDO row = MesProcessPoolActiveOrderDossierFileDO.builder()
                .id(id).activeOrderId(activeOrderId).applicationId(applicationId).categoryKey(categoryKey)
                .fileId(fileId).fileUrl("http://file/" + fileName).storageConfigId(1L)
                .storagePath("mes/active-order-dossier/" + activeOrderId + "/" + categoryKey + "/" + fileName)
                .fileName(fileName).contentType("application/pdf").fileSize(12L).sha256("HASH")
                .operatorId(7L).operatorName("生产组长").operatedAt(java.time.LocalDateTime.now()).build();
        row.setTenantId(1L);
        return row;
    }

    private static AdminUserRespDTO user(Long id, String nickname) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setNickname(nickname);
        return user;
    }

    private static Fixture fixture() {
        var applicationMapper = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        var activeOrderMapper = mock(MesProcessPoolActiveOrderMapper.class);
        var dossierFileMapper = mock(MesProcessPoolActiveOrderDossierFileMapper.class);
        var adminUserApi = mock(AdminUserApi.class);
        var fileService = mock(FileService.class);
        var operationAuditService = mock(MesProEdhrOperationAuditService.class);
        var nonconformanceReviewMapper = mock(MesProEdhrNonconformanceReviewMapper.class);
        var service = new MesActiveOrderDossierFileService(applicationMapper, activeOrderMapper,
                dossierFileMapper, nonconformanceReviewMapper, adminUserApi, fileService, operationAuditService);
        return new Fixture(applicationMapper, activeOrderMapper, dossierFileMapper,
                nonconformanceReviewMapper, adminUserApi, fileService, operationAuditService, service);
    }

    private record Fixture(
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper,
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProcessPoolActiveOrderDossierFileMapper dossierFileMapper,
            MesProEdhrNonconformanceReviewMapper nonconformanceReviewMapper,
            AdminUserApi adminUserApi,
            FileService fileService,
            MesProEdhrOperationAuditService operationAuditService,
            MesActiveOrderDossierFileService service) {
    }
}
