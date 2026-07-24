package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionAttachmentPrepareUploadReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionAttachmentPrepareUploadRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionAttachmentPrepareUploadCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionAttachmentPrepareUploadResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionAttachmentService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/mes/pro/batch-record-execution/attachment")
@Validated
public class MesProBatchRecordExecutionAttachmentController {

    @Resource
    private MesProBatchRecordExecutionAttachmentService attachmentService;

    @PostMapping("/prepare-upload")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-batch-record-execution:field-audit-update', "
            + "'mes:pro-batch-record-execution:golden-finger')")
    public CommonResult<MesProBatchRecordExecutionAttachmentPrepareUploadRespVO> prepareUpload(
            @Valid MesProBatchRecordExecutionAttachmentPrepareUploadReqVO reqVO,
            @RequestPart("file") MultipartFile file) throws IOException {
        MesProBatchRecordExecutionAttachmentPrepareUploadResult result = attachmentService.prepareUpload(
                new MesProBatchRecordExecutionAttachmentPrepareUploadCommand()
                        .setExecutionId(reqVO.getExecutionId())
                        .setWorkTaskId(reqVO.getWorkTaskId())
                        .setOperatorId(getLoginUserId())
                        .setFileName(file.getOriginalFilename())
                        .setContentType(file.getContentType())
                        .setContent(file.getBytes()));
        return success(toResp(result));
    }

    private MesProBatchRecordExecutionAttachmentPrepareUploadRespVO toResp(
            MesProBatchRecordExecutionAttachmentPrepareUploadResult result) {
        return new MesProBatchRecordExecutionAttachmentPrepareUploadRespVO()
                .setUploadToken(result.getUploadToken())
                .setFileId(result.getFileId())
                .setFileUrl(result.getFileUrl())
                .setStorageConfigId(result.getStorageConfigId())
                .setStoragePath(result.getStoragePath())
                .setFileName(result.getFileName())
                .setContentType(result.getContentType())
                .setFileSize(result.getFileSize())
                .setSha256(result.getSha256())
                .setStorageRetentionJson(result.getStorageRetentionJson())
                .setStorageRetentionHash(result.getStorageRetentionHash());
    }
}
