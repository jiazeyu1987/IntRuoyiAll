package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchExecutionRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewDisposeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewActiveOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewMaterialUploadRespVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetail;

import java.util.List;

public interface MesProEdhrNonconformanceReviewService {

    String SOURCE_TYPE_PQC_SUBMISSION = "PQC_SUBMISSION";
    String SOURCE_TYPE_PQC_RELEASE = "PQC_RELEASE";
    String SOURCE_TYPE_ACTIVE_ORDER = "ACTIVE_ORDER";

    String STATUS_PENDING_REVIEW = "pending_review";
    String STATUS_CLOSED = "closed";

    String DISPOSITION_CONCESSION_RELEASE = "concession_release";
    String DISPOSITION_REWORK = "rework";
    String DISPOSITION_VOID = "void";

    MesProEdhrNonconformanceReviewRespVO create(MesProEdhrNonconformanceReviewCreateReqVO reqVO);

    List<MesProEdhrNonconformanceReviewActiveOrderRespVO> listActiveOrderCandidates();

    MesProEdhrNonconformanceReviewRespVO rejectBatch(MesProEdhrBatchExecutionRejectReqVO reqVO);

    MesProEdhrNonconformanceReviewRespVO dispose(MesProEdhrNonconformanceReviewDisposeReqVO reqVO);

    MesProEdhrNonconformanceReviewMaterialUploadRespVO uploadMaterial(Long reviewId, String fileName,
                                                                       String contentType, byte[] content);

    MesProEdhrNonconformanceReviewRespVO get(Long id);

    MesTeamLeaderActiveOrderDetail getActiveOrderDetail(Long id);

    PageResult<MesProEdhrNonconformanceReviewRespVO> getPage(
            MesProEdhrNonconformanceReviewPageReqVO reqVO);

    PageResult<MesProEdhrNonconformanceReviewRespVO> getPendingPage(
            MesProEdhrNonconformanceReviewPageReqVO reqVO);

    List<MesProEdhrNonconformanceReviewRespVO> listByBatchExecutionId(Long batchExecutionId);

    boolean isBatchFrozen(Long batchExecutionId);

    void ensureBatchNotFrozen(Long batchExecutionId, String actionName);

    void ensureWorkOrderNotFrozen(Long workOrderId, String actionName);

    void ensurePqcSubmissionNotFrozen(Long activeOrderId, Long workOrderId, String actionName);
}
