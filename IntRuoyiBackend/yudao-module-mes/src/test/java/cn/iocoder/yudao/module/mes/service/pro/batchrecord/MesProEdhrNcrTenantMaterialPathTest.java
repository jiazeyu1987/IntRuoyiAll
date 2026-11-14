package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.service.file.FileUploadSecurityPolicy;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesProEdhrNcrTenantMaterialPathTest {
    private final MesProEdhrNonconformanceReviewServiceImpl service = new MesProEdhrNonconformanceReviewServiceImpl();
    private final FileService files = mock(FileService.class);
    private final FileMapper fileMapper = mock(FileMapper.class);
    private final MesProEdhrNonconformanceReviewMapper reviews = mock(MesProEdhrNonconformanceReviewMapper.class);
    private final byte[] content = "NCR upload verification".getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(122L);
        ReflectionTestUtils.setField(service, "fileService", files);
        ReflectionTestUtils.setField(service, "fileMapper", fileMapper);
        ReflectionTestUtils.setField(service, "reviewMapper", reviews);
        ReflectionTestUtils.setField(service, "handoffService", mock(MesActiveOrderHandoffService.class));
        ReflectionTestUtils.setField(service, "fileUploadSecurityPolicy", new FileUploadSecurityPolicy());
        when(reviews.selectById(1001L)).thenReturn(new MesProEdhrNonconformanceReviewDO().setId(1001L).setReviewStatus("pending_review"));
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    private FileDO record(String path) {
        return new FileDO().setId(70001L).setConfigId(7L).setName("evidence.txt")
                .setPath(path).setUrl("https://storage.example/evidence.txt").setSize((long) content.length);
    }

    @Test
    void uploadUsesTenantNamespaceAndReturnedMaterialRemainsUsableForDisposition() {
        when(files.createFileAndReturnId(any(byte[].class), eq("evidence.txt"), anyString(), eq("text/plain")))
                .thenAnswer(call -> {
                    String directory = call.getArgument(2);
                    assertTrue(directory.startsWith("tenants/122/mes/edhr-ncr/reviews/1001/"));
                    FileDO file = record(directory + "/20261115/evidence.txt");
                    when(files.getFile(70001L)).thenReturn(file);
                    when(fileMapper.selectById(70001L)).thenReturn(file);
                    return 70001L;
                });
        var uploaded = service.uploadMaterial(1001L, "evidence.txt", "text/plain", content);
        assertEquals(70001L, uploaded.getFileId());
        FileDO resolved = ReflectionTestUtils.invokeMethod(service, "resolveReviewMaterialFile", 1001L, 70001L, uploaded.getUrl());
        assertNotNull(resolved);
        assertEquals(uploaded.getPath(), resolved.getPath());
    }

    @Test
    void dispositionRejectsDifferentTenantReviewAndUnqualifiedPaths() {
        for (String path : new String[]{
                "tenants/123/mes/edhr-ncr/reviews/1001/upload/evidence.txt",
                "tenants/122/mes/edhr-ncr/reviews/1002/upload/evidence.txt",
                "tenants/122/mes/edhr-ncr/reviews/10010/upload/evidence.txt",
                "mes/edhr-ncr/reviews/1001/upload/evidence.txt"}) {
            when(fileMapper.selectById(70001L)).thenReturn(record(path));
            assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(service,
                    "resolveReviewMaterialFile", 1001L, 70001L, "https://storage.example/evidence.txt"));
        }
    }

    @Test
    void uploadRejectsARecordOutsideItsExactUploadDirectory() {
        when(files.createFileAndReturnId(any(byte[].class), anyString(), anyString(), anyString())).thenReturn(70001L);
        when(files.getFile(70001L)).thenReturn(record("tenants/122/mes/edhr-ncr/reviews/1001/other-upload/evidence.txt"));
        assertThrows(IllegalStateException.class, () -> service.uploadMaterial(1001L, "evidence.txt", "text/plain", content));
    }

    @Test
    void absentTenantFailsInsteadOfCreatingUnqualifiedEvidence() {
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class, () -> service.uploadMaterial(1001L, "evidence.txt", "text/plain", content));
        verify(files, never()).createFileAndReturnId(any(byte[].class), anyString(), anyString(), anyString());
    }
}
