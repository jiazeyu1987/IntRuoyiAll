package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRequestReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRespVO;

public interface MesProEdhrBatchVoidEffectService {

    EdhrRecordChangeRespVO precheckPlatformVoidBatchExecution(EdhrRecordChangeRequestReqVO reqVO);

    EdhrRecordChangeRespVO requestPlatformVoidBatchExecution(EdhrRecordChangeRequestReqVO reqVO,
                                                             String bpmProcessInstanceId);

    EdhrRecordChangeRespVO executeDirectPlatformVoidBatchExecution(EdhrRecordChangeRequestReqVO reqVO,
                                                                   Long actorUserId);

    EdhrRecordChangeRespVO handleVoidBatchExecutionApprovalCallback(String approvalInstanceId,
                                                                    String approvalEventId,
                                                                    String approvalResult,
                                                                    String rejectReason,
                                                                    Long actorUserId);
}
