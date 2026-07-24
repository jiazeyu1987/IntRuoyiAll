package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitBatchPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrInitBatchDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrInitBatchMapper extends BaseMapperX<MesProEdhrInitBatchDO> {

    default PageResult<MesProEdhrInitBatchDO> selectPage(MesProEdhrInitBatchPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrInitBatchDO>()
                .likeIfPresent(MesProEdhrInitBatchDO::getProjectCode, reqVO.getProjectCode())
                .likeIfPresent(MesProEdhrInitBatchDO::getProjectName, reqVO.getProjectName())
                .eqIfPresent(MesProEdhrInitBatchDO::getTargetEnvironment, reqVO.getTargetEnvironment())
                .eqIfPresent(MesProEdhrInitBatchDO::getTargetTenantId, reqVO.getTargetTenantId())
                .eqIfPresent(MesProEdhrInitBatchDO::getDataVersion, reqVO.getDataVersion())
                .eqIfPresent(MesProEdhrInitBatchDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesProEdhrInitBatchDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesProEdhrInitBatchDO::getId));
    }
}
