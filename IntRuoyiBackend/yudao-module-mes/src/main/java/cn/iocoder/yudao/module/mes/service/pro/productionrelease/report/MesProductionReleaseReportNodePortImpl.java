package cn.iocoder.yudao.module.mes.service.pro.productionrelease.report;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrSpecialNodeAttachmentPrepareUploadCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrSpecialNodeAttachmentPrepareUploadResult;
import org.springframework.stereotype.Component;

@Component
public class MesProductionReleaseReportNodePortImpl implements MesProductionReleaseReportNodePort {

    private final MesProEdhrBatchExecutionService batchExecutionService;

    public MesProductionReleaseReportNodePortImpl(MesProEdhrBatchExecutionService batchExecutionService) {
        this.batchExecutionService = batchExecutionService;
    }

    @Override
    public MesProductionReleaseReportAttachmentPrepareResult prepareAttachment(
            MesProductionReleaseReportAttachmentPreparePortCommand command) {
        MesProEdhrSpecialNodeAttachmentPrepareUploadResult result =
                batchExecutionService.prepareProductionReleaseReportAttachmentUpload(
                        new MesProEdhrSpecialNodeAttachmentPrepareUploadCommand()
                                .setTaskId(command.getBatchTaskId())
                                .setIdempotencyKey(command.getIdempotencyKey())
                                .setFileName(command.getFileName())
                                .setContentType(command.getContentType())
                                .setContent(command.getContent()),
                        command.getActorUserId());
        return new MesProductionReleaseReportAttachmentPrepareResult()
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

    @Override
    public MesProductionReleaseReportNodeEvidence complete(MesProductionReleaseReportNodePortCommand command) {
        return batchExecutionService.completeProductionReleaseReportNode(
                command.getBatchTaskId(), command.getActorUserId(), command.getSterilizationBatchNo(),
                command.getAttachments());
    }
}
