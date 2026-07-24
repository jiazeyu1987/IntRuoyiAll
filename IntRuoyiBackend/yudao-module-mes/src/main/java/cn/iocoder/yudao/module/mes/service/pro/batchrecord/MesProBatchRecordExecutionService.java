package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionCreateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApprovalPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApprovalActionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApprovalRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionEntryContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFormReviewSignReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFormReviewSignRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSaveDraftReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSignaturePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSignatureRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionTrackingEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionTrackingPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionTrackingRespVO;

import java.util.List;

public interface MesProBatchRecordExecutionService {

    MesProBatchRecordExecutionCreateRespVO createBatchRecordExecution(MesProBatchRecordExecutionCreateReqVO reqVO);

    PageResult<MesProBatchRecordExecutionRespVO> getBatchRecordExecutionPage(MesProBatchRecordExecutionPageReqVO pageReqVO);

    MesProBatchRecordExecutionRespVO getBatchRecordExecution(Long id);

    MesProBatchRecordExecutionRespVO getBatchRecordExecution(Long id, Long workTaskId);

    void saveBatchRecordExecutionDraft(MesProBatchRecordExecutionSaveDraftReqVO reqVO);

    MesProBatchRecordExecutionEntryContextRespVO getEntryContext(MesProBatchRecordExecutionEntryContextReqVO reqVO);

    MesProBatchRecordExecutionOpenOrCreateByContextRespVO openOrCreateByContext(
            MesProBatchRecordExecutionOpenOrCreateByContextReqVO reqVO);

    void submitBatchRecordExecution(MesProBatchRecordExecutionSubmitReqVO reqVO);

    MesProBatchRecordExecutionFormReviewSignRespVO cosignBatchRecordExecution(
            MesProBatchRecordExecutionFormReviewSignReqVO reqVO);

    MesProBatchRecordExecutionApprovalActionRespVO approveBatchRecordExecution(MesProBatchRecordExecutionApproveReqVO reqVO);

    MesProBatchRecordExecutionApprovalActionRespVO rejectBatchRecordExecution(MesProBatchRecordExecutionRejectReqVO reqVO);

    PageResult<MesProBatchRecordExecutionApprovalRespVO> getApprovalPendingPage(MesProBatchRecordExecutionApprovalPageReqVO pageReqVO);

    PageResult<MesProBatchRecordExecutionApprovalRespVO> getApprovalDonePage(MesProBatchRecordExecutionApprovalPageReqVO pageReqVO);

    default MesProBatchRecordExecutionApprovalRespVO getApprovalDetail(Long id, String bpmTaskId) {
        return getApprovalDetail(id, bpmTaskId, null);
    }

    MesProBatchRecordExecutionApprovalRespVO getApprovalDetail(Long id, String bpmTaskId, Long workTaskId);

    PageResult<MesProBatchRecordExecutionTrackingRespVO> getTrackingPage(MesProBatchRecordExecutionTrackingPageReqVO pageReqVO);

    PageResult<MesProBatchRecordExecutionSignatureRespVO> getSignaturePage(MesProBatchRecordExecutionSignaturePageReqVO pageReqVO);

    List<MesProBatchRecordExecutionTrackingEventRespVO> getTrackingTimeline(Long executionId);
}
