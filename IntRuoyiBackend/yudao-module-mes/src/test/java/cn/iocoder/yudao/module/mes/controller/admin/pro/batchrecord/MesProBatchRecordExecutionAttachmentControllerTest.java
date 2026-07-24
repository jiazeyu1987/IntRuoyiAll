package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionAttachmentPrepareUploadReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionAttachmentPrepareUploadRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionAttachmentPrepareUploadCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionAttachmentPrepareUploadResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionAttachmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProBatchRecordExecutionAttachmentControllerTest {

    @Mock
    private MesProBatchRecordExecutionAttachmentService attachmentService;

    @InjectMocks
    private MesProBatchRecordExecutionAttachmentController controller;

    @Test
    void prepareUpload_delegatesMultipartFileAndReturnsStructuredMetadata() throws Exception {
        byte[] content = "edhr image bytes".getBytes(StandardCharsets.UTF_8);
        when(attachmentService.prepareUpload(any())).thenReturn(MesProBatchRecordExecutionAttachmentPrepareUploadResult.builder()
                .uploadToken("EDHR_ATTACHMENT_UPLOAD:10:901:hash")
                .fileId(901L)
                .fileUrl("http://127.0.0.1:9000/yudao/edhr/evidence.png")
                .storageConfigId(28L)
                .storagePath("edhr/executions/10/attachments/evidence.png")
                .fileName("evidence.png")
                .contentType("image/png")
                .fileSize((long) content.length)
                .sha256("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .storageRetentionJson("{\"fileId\":901,\"storageConfigId\":28}")
                .storageRetentionHash("abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789")
                .build());

        MesProBatchRecordExecutionAttachmentPrepareUploadReqVO reqVO =
                new MesProBatchRecordExecutionAttachmentPrepareUploadReqVO()
                        .setExecutionId(10L)
                        .setWorkTaskId(31L);
        MockMultipartFile file = new MockMultipartFile("file", "evidence.png", "image/png", content);

        CommonResult<MesProBatchRecordExecutionAttachmentPrepareUploadRespVO> response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            response = controller.prepareUpload(reqVO, file);
        }

        assertEquals(901L, response.getData().getFileId());
        assertEquals(28L, response.getData().getStorageConfigId());
        assertEquals("edhr/executions/10/attachments/evidence.png", response.getData().getStoragePath());
        assertEquals("evidence.png", response.getData().getFileName());
        assertEquals("image/png", response.getData().getContentType());
        assertEquals(content.length, response.getData().getFileSize());
        assertEquals("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                response.getData().getSha256());
        assertEquals("{\"fileId\":901,\"storageConfigId\":28}", response.getData().getStorageRetentionJson());
        assertEquals("abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789",
                response.getData().getStorageRetentionHash());
        assertEquals("EDHR_ATTACHMENT_UPLOAD:10:901:hash", response.getData().getUploadToken());

        ArgumentCaptor<MesProBatchRecordExecutionAttachmentPrepareUploadCommand> captor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionAttachmentPrepareUploadCommand.class);
        verify(attachmentService).prepareUpload(captor.capture());
        assertEquals(10L, captor.getValue().getExecutionId());
        assertEquals(31L, captor.getValue().getWorkTaskId());
        assertEquals(99L, captor.getValue().getOperatorId());
        assertEquals("evidence.png", captor.getValue().getFileName());
        assertEquals("image/png", captor.getValue().getContentType());
        assertArrayEquals(content, captor.getValue().getContent());
    }

    @Test
    void mappingsAndPermissions_matchFrozenAttachmentPrepareUploadContract() throws Exception {
        RequestMapping requestMapping = MesProBatchRecordExecutionAttachmentController.class
                .getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping);
        assertArrayEquals(new String[]{"/mes/pro/batch-record-execution/attachment"}, requestMapping.value());

        Method method = MesProBatchRecordExecutionAttachmentController.class.getDeclaredMethod("prepareUpload",
                MesProBatchRecordExecutionAttachmentPrepareUploadReqVO.class,
                org.springframework.web.multipart.MultipartFile.class);
        assertArrayEquals(new String[]{"/prepare-upload"}, method.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasAnyPermissions('mes:pro-batch-record-execution:field-audit-update', "
                        + "'mes:pro-batch-record-execution:golden-finger')",
                method.getAnnotation(PreAuthorize.class).value());

        requireGetter(MesProBatchRecordExecutionAttachmentPrepareUploadReqVO.class, "getExecutionId");
        requireGetter(MesProBatchRecordExecutionAttachmentPrepareUploadReqVO.class, "getWorkTaskId");
        requireGetter(MesProBatchRecordExecutionAttachmentPrepareUploadRespVO.class, "getUploadToken");
        requireGetter(MesProBatchRecordExecutionAttachmentPrepareUploadRespVO.class, "getFileId");
        requireGetter(MesProBatchRecordExecutionAttachmentPrepareUploadRespVO.class, "getFileUrl");
        requireGetter(MesProBatchRecordExecutionAttachmentPrepareUploadRespVO.class, "getStorageConfigId");
        requireGetter(MesProBatchRecordExecutionAttachmentPrepareUploadRespVO.class, "getStoragePath");
        requireGetter(MesProBatchRecordExecutionAttachmentPrepareUploadRespVO.class, "getFileName");
        requireGetter(MesProBatchRecordExecutionAttachmentPrepareUploadRespVO.class, "getContentType");
        requireGetter(MesProBatchRecordExecutionAttachmentPrepareUploadRespVO.class, "getFileSize");
        requireGetter(MesProBatchRecordExecutionAttachmentPrepareUploadRespVO.class, "getSha256");
        requireGetter(MesProBatchRecordExecutionAttachmentPrepareUploadRespVO.class, "getStorageRetentionJson");
        requireGetter(MesProBatchRecordExecutionAttachmentPrepareUploadRespVO.class, "getStorageRetentionHash");
    }

    private void requireGetter(Class<?> type, String getterName) {
        try {
            assertNotNull(type.getMethod(getterName));
        } catch (NoSuchMethodException ex) {
            fail("Expected getter to exist: " + type.getName() + "#" + getterName, ex);
        }
    }
}
