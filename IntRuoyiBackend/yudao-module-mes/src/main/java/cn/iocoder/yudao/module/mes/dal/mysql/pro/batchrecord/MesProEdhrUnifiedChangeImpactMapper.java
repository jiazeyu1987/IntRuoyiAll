package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeImpactPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrUnifiedChangeImpactDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrUnifiedChangeImpactMapper extends BaseMapperX<MesProEdhrUnifiedChangeImpactDO> {

    default PageResult<MesProEdhrUnifiedChangeImpactDO> selectPage(MesProEdhrUnifiedChangeImpactPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrUnifiedChangeImpactDO>()
                .eqIfPresent(MesProEdhrUnifiedChangeImpactDO::getChangeRequestId, reqVO.getChangeRequestId())
                .eqIfPresent(MesProEdhrUnifiedChangeImpactDO::getImpactType, reqVO.getImpactType())
                .eqIfPresent(MesProEdhrUnifiedChangeImpactDO::getImpactObjectType, reqVO.getImpactObjectType())
                .eqIfPresent(MesProEdhrUnifiedChangeImpactDO::getRiskLevel, reqVO.getRiskLevel())
                .orderByDesc(MesProEdhrUnifiedChangeImpactDO::getId));
    }
}
