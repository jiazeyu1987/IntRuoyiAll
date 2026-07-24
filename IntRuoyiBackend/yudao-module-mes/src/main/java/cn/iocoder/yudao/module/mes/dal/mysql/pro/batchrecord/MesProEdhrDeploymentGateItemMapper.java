package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeploymentGateItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrDeploymentGateItemMapper extends BaseMapperX<MesProEdhrDeploymentGateItemDO> {

    default List<MesProEdhrDeploymentGateItemDO> selectListByDeploymentId(Long deploymentId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrDeploymentGateItemDO>()
                .eq(MesProEdhrDeploymentGateItemDO::getDeploymentId, deploymentId)
                .orderByAsc(MesProEdhrDeploymentGateItemDO::getId));
    }

    default MesProEdhrDeploymentGateItemDO selectByDeploymentIdAndGateCode(Long deploymentId, String gateCode) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrDeploymentGateItemDO>()
                .eq(MesProEdhrDeploymentGateItemDO::getDeploymentId, deploymentId)
                .eq(MesProEdhrDeploymentGateItemDO::getGateCode, gateCode));
    }
}

