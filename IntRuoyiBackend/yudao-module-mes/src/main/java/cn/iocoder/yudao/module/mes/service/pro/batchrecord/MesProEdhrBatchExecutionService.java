package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionArchiveGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionArchiveDownloadRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionArchiveRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionCloseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionGoldenFingerBulkVoidReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionGoldenFingerBulkVoidRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionOpenOrCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionManualOpenOrCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionQualityRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionReexecuteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRouteOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionReviewTimelineRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionTaskOpenReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionTaskOpenRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionTaskPreviewRespVO;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseReportNodeEvidence;

import java.util.List;

public interface MesProEdhrBatchExecutionService {

    PageResult<EdhrBatchExecutionRespVO> getPage(EdhrBatchExecutionPageReqVO reqVO);

    EdhrBatchExecutionRespVO get(Long id);

    EdhrBatchExecutionRespVO openOrCreate(EdhrBatchExecutionOpenOrCreateReqVO reqVO);

    EdhrBatchExecutionRespVO openOrCreateManual(EdhrBatchExecutionManualOpenOrCreateReqVO reqVO);

    Long openOrCreateFromProductionRelease(MesProEdhrProductionReleaseBatchCommand command);

    EdhrBatchExecutionRespVO reexecuteRejectedBatch(EdhrBatchExecutionReexecuteReqVO reqVO);

    EdhrBatchExecutionGoldenFingerBulkVoidRespVO goldenFingerBulkVoid(
            EdhrBatchExecutionGoldenFingerBulkVoidReqVO reqVO);

    List<EdhrBatchExecutionRouteOptionRespVO> listRouteOptionsByWorkOrder(Long workOrderId);

    List<String> getScheduleCompletionMissingItems(EdhrScheduleCompletionCreateCommand command);

    EdhrBatchExecutionRespVO openOrCreateFromScheduleCompletion(EdhrScheduleCompletionCreateCommand command);

    EdhrBatchExecutionTaskOpenRespVO openTask(EdhrBatchExecutionTaskOpenReqVO reqVO);

    EdhrBatchExecutionTaskPreviewRespVO previewTask(Long batchExecutionId, Long taskId);

    EdhrBatchExecutionRespVO skipSpecialNode(Long taskId, String reason, String password,
                                             List<MesProEdhrSpecialNodeAttachment> attachments);

    EdhrBatchExecutionRespVO completeSpecialNode(Long taskId, String sterilizationBatchNo);

    EdhrBatchExecutionRespVO completeSpecialNode(Long taskId, String sterilizationBatchNo,
                                                 List<MesProEdhrSpecialNodeAttachment> attachments);

    MesProductionReleaseReportNodeEvidence completeProductionReleaseReportNode(
            Long taskId, Long actorUserId, String sterilizationBatchNo,
            List<MesProEdhrSpecialNodeAttachment> attachments);

    MesProEdhrSpecialNodeAttachmentPrepareUploadResult prepareSpecialNodeAttachmentUpload(
            MesProEdhrSpecialNodeAttachmentPrepareUploadCommand command);

    MesProEdhrSpecialNodeAttachmentPrepareUploadResult prepareProductionReleaseReportAttachmentUpload(
            MesProEdhrSpecialNodeAttachmentPrepareUploadCommand command, Long actorUserId);

    void deletePendingSpecialNodeAttachment(Long taskId, MesProEdhrSpecialNodeAttachment attachment, String reason);

    EdhrBatchExecutionRespVO savePendingSpecialNodeAttachments(Long batchExecutionId, String reason);

    EdhrBatchExecutionRespVO syncStatus(Long id);

    EdhrBatchExecutionReviewTimelineRespVO getReviewTimeline(Long id);

    EdhrBatchExecutionRespVO close(EdhrBatchExecutionCloseReqVO reqVO);

    EdhrBatchExecutionRespVO qualityReject(EdhrBatchExecutionQualityRejectReqVO reqVO);

    EdhrBatchExecutionArchiveRespVO generateArchive(EdhrBatchExecutionArchiveGenerateReqVO reqVO);

    EdhrBatchExecutionArchiveRespVO getLatestArchive(Long batchExecutionId);

    EdhrBatchExecutionArchiveDownloadRespVO downloadArchive(Long id);
}
