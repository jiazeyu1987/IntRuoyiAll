package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqCaseCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqCasePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqCaseRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationCloseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationRemediateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationRetestReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqRunCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqRunPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqRunRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqStepResultRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqStepSubmitReqVO;

public interface MesProEdhrOqPqService {

    PageResult<MesProEdhrOqPqCaseRespVO> getCasePage(MesProEdhrOqPqCasePageReqVO reqVO);

    MesProEdhrOqPqCaseRespVO createCase(MesProEdhrOqPqCaseCreateReqVO reqVO);

    PageResult<MesProEdhrOqPqRunRespVO> getRunPage(MesProEdhrOqPqRunPageReqVO reqVO);

    MesProEdhrOqPqRunRespVO createRun(MesProEdhrOqPqRunCreateReqVO reqVO);

    MesProEdhrOqPqStepResultRespVO submitStepResult(MesProEdhrOqPqStepSubmitReqVO reqVO);

    MesProEdhrOqPqRunRespVO completeRun(Long runId);

    PageResult<MesProEdhrOqPqDeviationRespVO> getDeviationPage(MesProEdhrOqPqDeviationPageReqVO reqVO);

    MesProEdhrOqPqDeviationRespVO remediateDeviation(MesProEdhrOqPqDeviationRemediateReqVO reqVO);

    MesProEdhrOqPqDeviationRespVO retestDeviation(MesProEdhrOqPqDeviationRetestReqVO reqVO);

    MesProEdhrOqPqDeviationRespVO closeDeviation(MesProEdhrOqPqDeviationCloseReqVO reqVO);
}
