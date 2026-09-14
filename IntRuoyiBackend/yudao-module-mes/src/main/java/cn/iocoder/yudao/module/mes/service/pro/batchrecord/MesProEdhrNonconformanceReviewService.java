package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewDisposeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewRespVO;

import java.util.List;

public interface MesProEdhrNonconformanceReviewService {

    String SOURCE_TYPE_PQC_SUBMISSION = "PQC_SUBMISSION";
    String SOURCE_TYPE_PQC_RELEASE = "PQC_RELEASE";

    String STATUS_PENDING_REVIEW = "pending_review";
    String STATUS_CLOSED = "closed";

    String DISPOSITION_CONCESSION_RELEASE = "concession_release";
    String DISPOSITION_REWORK = "rework";
    String DISPOSITION_VOID = "void";

    MesProEdhrNonconformanceReviewRespVO create(MesProEdhrNonconformanceReviewCreateReqVO reqVO);

    MesProEdhrNonconformanceReviewRespVO dispose(MesProEdhrNonconformanceReviewDisposeReqVO reqVO);

    MesProEdhrNonconformanceReviewRespVO get(Long id);

    PageResult<MesProEdhrNonconformanceReviewRespVO> getPendingPage(
            MesProEdhrNonconformanceReviewPageReqVO reqVO);

    List<MesProEdhrNonconformanceReviewRespVO> listByBatchExecutionId(Long batchExecutionId);

    boolean isBatchFrozen(Long batchExecutionId);

    void ensureBatchNotFrozen(Long batchExecutionId, String actionName);

    void ensureWorkOrderNotFrozen(Long workOrderId, String actionName);
}
