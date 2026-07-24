package cn.iocoder.yudao.module.mes.dal.mysql.md.workstation;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationMachineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * MES 设备资源 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesMdWorkstationMachineMapper extends BaseMapperX<MesMdWorkstationMachineDO> {

    default List<MesMdWorkstationMachineDO> selectListByWorkstationId(Long workstationId) {
        return selectList(MesMdWorkstationMachineDO::getWorkstationId, workstationId);
    }

    default List<MesMdWorkstationMachineDO> selectListByWorkstationIds(Collection<Long> workstationIds) {
        return selectList(MesMdWorkstationMachineDO::getWorkstationId, workstationIds);
    }

    default MesMdWorkstationMachineDO selectByMachineryId(Long machineryId) {
        return selectOne(MesMdWorkstationMachineDO::getMachineryId, machineryId);
    }

    default MesMdWorkstationMachineDO selectByWorkstationIdAndMachineryId(Long workstationId, Long machineryId) {
        return selectOne(new LambdaQueryWrapperX<MesMdWorkstationMachineDO>()
                .eq(MesMdWorkstationMachineDO::getWorkstationId, workstationId)
                .eq(MesMdWorkstationMachineDO::getMachineryId, machineryId));
    }

    default void deleteByWorkstationId(Long workstationId) {
        delete(MesMdWorkstationMachineDO::getWorkstationId, workstationId);
    }

}
