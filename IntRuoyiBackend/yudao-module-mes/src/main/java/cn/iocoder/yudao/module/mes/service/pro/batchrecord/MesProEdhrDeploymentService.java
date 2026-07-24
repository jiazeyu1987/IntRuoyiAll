package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentPrecheckRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentUpdateReqVO;

public interface MesProEdhrDeploymentService {

    PageResult<MesProEdhrDeploymentRespVO> getPage(MesProEdhrDeploymentPageReqVO reqVO);

    MesProEdhrDeploymentRespVO createEvidence(MesProEdhrDeploymentCreateReqVO reqVO);

    MesProEdhrDeploymentRespVO getDetail(Long id);

    MesProEdhrDeploymentRespVO updateEvidence(MesProEdhrDeploymentUpdateReqVO reqVO);

    MesProEdhrDeploymentPrecheckRespVO precheckEvidence(Long deploymentId);
}

