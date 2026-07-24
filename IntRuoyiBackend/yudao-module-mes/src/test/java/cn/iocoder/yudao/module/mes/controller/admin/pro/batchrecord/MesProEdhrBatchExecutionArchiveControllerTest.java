package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionArchiveDownloadRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionArchiveGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionArchiveRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrBatchExecutionArchiveControllerTest {

    @Mock
    private MesProEdhrBatchExecutionService batchExecutionService;
    @InjectMocks
    private MesProEdhrBatchExecutionArchiveController controller;

    @Test
    void generateLatestAndDownload_delegateToService() {
        EdhrBatchExecutionArchiveRespVO archive = new EdhrBatchExecutionArchiveRespVO()
                .setId(99L)
                .setBatchExecutionId(1L)
                .setFileName("final.pdf");
        when(batchExecutionService.generateArchive(any())).thenReturn(archive);
        when(batchExecutionService.getLatestArchive(1L)).thenReturn(archive);
        byte[] content = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);
        when(batchExecutionService.downloadArchive(99L)).thenReturn(new EdhrBatchExecutionArchiveDownloadRespVO()
                .setFileName("final.pdf")
                .setContentType(MediaType.APPLICATION_PDF_VALUE)
                .setFileSize((long) content.length)
                .setContent(content));

        assertSame(archive, controller.generate(new EdhrBatchExecutionArchiveGenerateReqVO()).getData());
        assertSame(archive, controller.latest(1L).getData());
        ResponseEntity<byte[]> response = controller.download(99L);

        assertArrayEquals(content, response.getBody());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertEquals(content.length, response.getHeaders().getContentLength());
        verify(batchExecutionService).generateArchive(any());
        verify(batchExecutionService).getLatestArchive(1L);
        verify(batchExecutionService).downloadArchive(99L);
    }

    @Test
    void contractMappings_matchArchiveEndpointsAndPermissions() throws Exception {
        Method generate = MesProEdhrBatchExecutionArchiveController.class.getDeclaredMethod("generate",
                EdhrBatchExecutionArchiveGenerateReqVO.class);
        assertArrayEquals(new String[]{"/generate"}, generate.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-batch-execution-archive:create')",
                generate.getAnnotation(PreAuthorize.class).value());

        Method latest = MesProEdhrBatchExecutionArchiveController.class.getDeclaredMethod("latest", Long.class);
        assertArrayEquals(new String[]{"/latest"}, latest.getAnnotation(GetMapping.class).value());
        assertEquals("batchExecutionId", latest.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-batch-execution-archive:query')",
                latest.getAnnotation(PreAuthorize.class).value());

        Method download = MesProEdhrBatchExecutionArchiveController.class.getDeclaredMethod("download", Long.class);
        assertArrayEquals(new String[]{"/download"}, download.getAnnotation(GetMapping.class).value());
        assertEquals("id", download.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-batch-execution-archive:download')",
                download.getAnnotation(PreAuthorize.class).value());
    }
}
