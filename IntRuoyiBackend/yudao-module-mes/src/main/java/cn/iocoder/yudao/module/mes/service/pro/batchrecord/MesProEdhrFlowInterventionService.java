package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionAddSignReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionAdminReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionTransferReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionWithdrawReqVO;

public interface MesProEdhrFlowInterventionService {

    PageResult<MesProEdhrFlowInterventionRespVO> getPage(MesProEdhrFlowInterventionPageReqVO reqVO);

    PageResult<MesProEdhrFlowEventRespVO> getEventPage(MesProEdhrFlowEventPageReqVO reqVO);

    MesProEdhrFlowInterventionRespVO returnBack(MesProEdhrFlowInterventionReturnReqVO reqVO);

    MesProEdhrFlowInterventionRespVO withdraw(MesProEdhrFlowInterventionWithdrawReqVO reqVO);

    MesProEdhrFlowInterventionRespVO transfer(MesProEdhrFlowInterventionTransferReqVO reqVO);

    MesProEdhrFlowInterventionRespVO addSign(MesProEdhrFlowInterventionAddSignReqVO reqVO);

    MesProEdhrFlowInterventionRespVO adminIntervene(MesProEdhrFlowInterventionAdminReqVO reqVO);
}
