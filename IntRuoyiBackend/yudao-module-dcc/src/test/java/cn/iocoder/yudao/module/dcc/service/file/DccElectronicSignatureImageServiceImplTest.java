package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureImageDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureImageMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccElectronicSignatureImageServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private DccElectronicSignatureImageMapper signatureImageMapper;
    @Mock
    private FileService fileService;

    @InjectMocks
    private DccElectronicSignatureImageServiceImpl service;

    @Test
    void getMySignatureImage_returnsAdminFileAccessUrlForPreview() {
        DccElectronicSignatureImageDO image = DccElectronicSignatureImageDO.builder()
                .id(9001L)
                .userId(100L)
                .versionNo(1)
                .fileId(6001L)
                .fileUrl("http://127.0.0.1:9000/yudao/dcc/signature-images/raw.png")
                .fileName("签名 图.png")
                .contentType("image/png")
                .fileSize(128L)
                .sha256("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890")
                .imageStatus("ACTIVE")
                .active(Boolean.TRUE)
                .uploadedAt(LocalDateTime.of(2026, 7, 15, 8, 48, 32))
                .enabledAt(LocalDateTime.of(2026, 7, 15, 8, 49, 1))
                .referencedCount(0)
                .build();
        when(signatureImageMapper.selectActiveByUserId(100L)).thenReturn(image);
        when(fileService.getFile(6001L)).thenReturn(FileDO.builder()
                .id(6001L)
                .configId(10L)
                .path("dcc/signature-images/20260715/签名 图.png")
                .url("http://127.0.0.1:9000/yudao/dcc/signature-images/raw.png")
                .name("签名 图.png")
                .type("image/png")
                .size(128L)
                .build());

        var result = service.getMySignatureImage(100L);

        assertEquals("/admin-api/infra/file/10/get/dcc/signature-images/20260715/%E7%AD%BE%E5%90%8D%20%E5%9B%BE.png",
                result.getFileUrl());
        assertEquals(LocalDateTime.of(2026, 7, 15, 8, 48, 32), result.getUploadedAt());
        assertEquals(LocalDateTime.of(2026, 7, 15, 8, 49, 1), result.getEnabledAt());
        verify(fileService).getFile(6001L);
    }
}
