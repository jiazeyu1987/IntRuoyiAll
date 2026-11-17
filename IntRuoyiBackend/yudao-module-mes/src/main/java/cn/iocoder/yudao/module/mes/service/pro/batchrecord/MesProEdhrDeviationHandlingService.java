package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationHandlingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationHandlingSaveReqVO;

public interface MesProEdhrDeviationHandlingService {

    MesProEdhrDeviationHandlingRespVO get(Long deviationId);

    MesProEdhrDeviationHandlingRespVO save(Long actorUserId, Long deviationId,
                                            MesProEdhrDeviationHandlingSaveReqVO reqVO);

    MesProEdhrDeviationHandlingRespVO submitInvestigation(Long actor,
            cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationHandlingSubmitReqVO req);

    MesProEdhrDeviationHandlingRespVO submitCorrective(Long actor,
            cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationCorrectiveReqVO req);

    MesProEdhrDeviationHandlingRespVO verifyCorrective(Long actor,
            cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationCorrectiveVerifyReqVO req);

    MesProEdhrDeviationHandlingRespVO returnToInvestigation(Long actor,
            cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesDeviationReviewReturnReqVO req);

    MesProEdhrDeviationHandlingRespVO withdrawInvestigation(Long actor,
            cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesDeviationInvestigationWithdrawReqVO req);

    Long sign(Long actorUserId, Long deviationId, String node, String password, String comment,
              Integer expectedContentVersion, String expectedContentHash);

    void closeNormally(Long actorUserId, Long deviationId, Integer expectedContentVersion, String expectedContentHash);
}
