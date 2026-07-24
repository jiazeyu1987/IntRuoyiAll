package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeploymentEvidenceDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrDeploymentEvidenceMapper extends BaseMapperX<MesProEdhrDeploymentEvidenceDO> {

    default PageResult<MesProEdhrDeploymentEvidenceDO> selectPage(MesProEdhrDeploymentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrDeploymentEvidenceDO>()
                .eqIfPresent(MesProEdhrDeploymentEvidenceDO::getProjectId, reqVO.getProjectId())
                .likeIfPresent(MesProEdhrDeploymentEvidenceDO::getDeploymentCode, reqVO.getDeploymentCode())
                .likeIfPresent(MesProEdhrDeploymentEvidenceDO::getDeploymentName, reqVO.getDeploymentName())
                .eqIfPresent(MesProEdhrDeploymentEvidenceDO::getDeploymentStatus, reqVO.getDeploymentStatus())
                .likeIfPresent(MesProEdhrDeploymentEvidenceDO::getReleaseTag, reqVO.getReleaseTag())
                .likeIfPresent(MesProEdhrDeploymentEvidenceDO::getTargetEnvironment, reqVO.getTargetEnvironment())
                .orderByDesc(MesProEdhrDeploymentEvidenceDO::getId));
    }
}

