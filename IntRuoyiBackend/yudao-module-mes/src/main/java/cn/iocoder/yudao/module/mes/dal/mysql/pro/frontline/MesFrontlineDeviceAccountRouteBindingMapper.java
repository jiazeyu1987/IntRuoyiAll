package cn.iocoder.yudao.module.mes.dal.mysql.pro.frontline;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.frontline.MesFrontlineDeviceAccountRouteBindingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesFrontlineDeviceAccountRouteBindingMapper
        extends BaseMapperX<MesFrontlineDeviceAccountRouteBindingDO> {

    default List<MesFrontlineDeviceAccountRouteBindingDO> selectEnabledListByDeviceAccountUserId(
            Long deviceAccountUserId) {
        return selectList(new LambdaQueryWrapperX<MesFrontlineDeviceAccountRouteBindingDO>()
                .eq(MesFrontlineDeviceAccountRouteBindingDO::getDeviceAccountUserId, deviceAccountUserId)
                .eq(MesFrontlineDeviceAccountRouteBindingDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .orderByAsc(MesFrontlineDeviceAccountRouteBindingDO::getRouteId)
                .orderByAsc(MesFrontlineDeviceAccountRouteBindingDO::getDeviceId));
    }

    default MesFrontlineDeviceAccountRouteBindingDO selectEnabledByDeviceAccountUserIdAndRouteIdAndDeviceIdAndWorkstationId(
            Long deviceAccountUserId, Long routeId, Long deviceId, Long workstationId) {
        return selectOne(new LambdaQueryWrapperX<MesFrontlineDeviceAccountRouteBindingDO>()
                .eq(MesFrontlineDeviceAccountRouteBindingDO::getDeviceAccountUserId, deviceAccountUserId)
                .eq(MesFrontlineDeviceAccountRouteBindingDO::getRouteId, routeId)
                .eq(MesFrontlineDeviceAccountRouteBindingDO::getDeviceId, deviceId)
                .eq(MesFrontlineDeviceAccountRouteBindingDO::getWorkstationId, workstationId)
                .eq(MesFrontlineDeviceAccountRouteBindingDO::getStatus, CommonStatusEnum.ENABLE.getStatus()));
    }
}
