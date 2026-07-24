package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRequestReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRespVO;

public interface MesProEdhrRecordChangeService {

    EdhrRecordChangeRespVO requestVoidExecution(EdhrRecordChangeRequestReqVO reqVO);

    EdhrRecordChangeRespVO approveVoidExecution(EdhrRecordChangeApproveReqVO reqVO);

    EdhrRecordChangeRespVO requestVoidBatchExecution(EdhrRecordChangeRequestReqVO reqVO);

    EdhrRecordChangeRespVO precheckPlatformVoidBatchExecution(EdhrRecordChangeRequestReqVO reqVO);

    EdhrRecordChangeRespVO requestPlatformVoidBatchExecution(EdhrRecordChangeRequestReqVO reqVO,
                                                              String bpmProcessInstanceId);

    EdhrRecordChangeRespVO executeDirectPlatformVoidBatchExecution(EdhrRecordChangeRequestReqVO reqVO,
                                                                   Long actorUserId);

    EdhrRecordChangeRespVO withdrawVoidBatchExecution(EdhrRecordChangeApproveReqVO reqVO);

    EdhrRecordChangeRespVO handleVoidBatchExecutionApprovalCallback(String approvalInstanceId,
                                                                     String approvalEventId,
                                                                     String approvalResult,
                                                                     String rejectReason,
                                                                     Long actorUserId);

    EdhrRecordChangeRespVO requestReopenBatch(EdhrRecordChangeRequestReqVO reqVO);

    EdhrRecordChangeRespVO approveReopenBatch(EdhrRecordChangeApproveReqVO reqVO);

    EdhrRecordChangeRespVO requestReopenExecution(EdhrRecordChangeRequestReqVO reqVO);

    EdhrRecordChangeRespVO approveReopenExecution(EdhrRecordChangeApproveReqVO reqVO);

    EdhrRecordChangeRespVO requestSupplement(EdhrRecordChangeRequestReqVO reqVO);

    EdhrRecordChangeRespVO saveSupplementDraft(EdhrRecordChangeRequestReqVO reqVO);

    EdhrRecordChangeRespVO submitSupplement(EdhrRecordChangeApproveReqVO reqVO);

    EdhrRecordChangeRespVO approveSupplement(EdhrRecordChangeApproveReqVO reqVO);

    PageResult<EdhrRecordChangeRespVO> getPage(EdhrRecordChangePageReqVO reqVO);

    EdhrRecordChangeRespVO get(Long id);

}
