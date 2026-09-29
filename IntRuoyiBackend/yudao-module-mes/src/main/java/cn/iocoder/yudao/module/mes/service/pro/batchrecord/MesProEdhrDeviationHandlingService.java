package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationHandlingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationHandlingSaveReqVO;

public interface MesProEdhrDeviationHandlingService {

    MesProEdhrDeviationHandlingRespVO get(Long deviationId);

    MesProEdhrDeviationHandlingRespVO save(Long actorUserId, Long deviationId,
                                            MesProEdhrDeviationHandlingSaveReqVO reqVO);

    Long sign(Long actorUserId, Long deviationId, String node, String password, String comment);

    void closeNormally(Long actorUserId, Long deviationId);
}
