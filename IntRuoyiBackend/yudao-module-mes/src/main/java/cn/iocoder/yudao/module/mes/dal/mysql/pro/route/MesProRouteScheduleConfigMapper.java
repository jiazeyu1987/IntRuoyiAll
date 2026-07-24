package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteScheduleConfigDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface MesProRouteScheduleConfigMapper extends BaseMapperX<MesProRouteScheduleConfigDO> {

    default List<MesProRouteScheduleConfigDO> selectListByRouteVersionId(Long routeVersionId) {
        return selectList(new LambdaQueryWrapperX<MesProRouteScheduleConfigDO>()
                .eq(MesProRouteScheduleConfigDO::getRouteVersionId, routeVersionId)
                .isNull(MesProRouteScheduleConfigDO::getItemId)
                .orderByAsc(MesProRouteScheduleConfigDO::getRouteProcessId));
    }

    default MesProRouteScheduleConfigDO selectByRouteVersionIdAndRouteProcessId(Long routeVersionId, Long routeProcessId) {
        return selectOne(new LambdaQueryWrapperX<MesProRouteScheduleConfigDO>()
                .eq(MesProRouteScheduleConfigDO::getRouteVersionId, routeVersionId)
                .eq(MesProRouteScheduleConfigDO::getRouteProcessId, routeProcessId)
                .isNull(MesProRouteScheduleConfigDO::getItemId));
    }

    default List<MesProRouteScheduleConfigDO> selectListForCapacityUnificationAudit() {
        return selectList(new LambdaQueryWrapperX<MesProRouteScheduleConfigDO>()
                .isNull(MesProRouteScheduleConfigDO::getItemId)
                .orderByAsc(MesProRouteScheduleConfigDO::getRouteVersionId)
                .orderByAsc(MesProRouteScheduleConfigDO::getRouteProcessId));
    }

    default int deleteByRouteVersionId(Long routeVersionId) {
        return delete(new LambdaQueryWrapperX<MesProRouteScheduleConfigDO>()
                .eq(MesProRouteScheduleConfigDO::getRouteVersionId, routeVersionId)
                .isNull(MesProRouteScheduleConfigDO::getItemId));
    }

    default int updateCopiedConfigVersion(Long id, String configVersion, BigDecimal hourlyCapacity) {
        return update(null, new LambdaUpdateWrapper<MesProRouteScheduleConfigDO>()
                .eq(MesProRouteScheduleConfigDO::getId, id)
                .set(MesProRouteScheduleConfigDO::getConfigVersion, configVersion)
                .set(MesProRouteScheduleConfigDO::getHourlyCapacity, hourlyCapacity));
    }

}
