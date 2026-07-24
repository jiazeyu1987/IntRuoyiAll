package cn.iocoder.yudao.module.mes.dal.mysql.md.workstation;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.MesMdWorkstationPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * MES 工作站 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesMdWorkstationMapper extends BaseMapperX<MesMdWorkstationDO> {

    default PageResult<MesMdWorkstationDO> selectPage(MesMdWorkstationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesMdWorkstationDO>()
                .eqIfPresent(MesMdWorkstationDO::getCode, reqVO.getCode())
                .likeIfPresent(MesMdWorkstationDO::getName, reqVO.getName())
                .eqIfPresent(MesMdWorkstationDO::getWorkshopId, reqVO.getWorkshopId())
                .eqIfPresent(MesMdWorkstationDO::getProcessId, reqVO.getProcessId())
                .eqIfPresent(MesMdWorkstationDO::getStatus, reqVO.getStatus())
                .orderByDesc(MesMdWorkstationDO::getId));
    }

    default MesMdWorkstationDO selectByCode(String code) {
        return selectOne(MesMdWorkstationDO::getCode, code);
    }

    default MesMdWorkstationDO selectByName(String name) {
        return selectOne(MesMdWorkstationDO::getName, name);
    }

    default List<MesMdWorkstationDO> selectListByStatus(Integer status) {
        return selectList(MesMdWorkstationDO::getStatus, status);
    }

    default List<MesMdWorkstationDO> selectListByProcessIds(Collection<Long> processIds, Integer status) {
        if (processIds == null || processIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesMdWorkstationDO>()
                .in(MesMdWorkstationDO::getProcessId, processIds)
                .eqIfPresent(MesMdWorkstationDO::getStatus, status)
                .orderByAsc(MesMdWorkstationDO::getId));
    }

    default Long selectCountByWorkshopId(Long workshopId) {
        return selectCount(MesMdWorkstationDO::getWorkshopId, workshopId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(MesMdWorkstationDO::getWarehouseId, warehouseId);
    }

    default Long selectCountByLocationId(Long locationId) {
        return selectCount(MesMdWorkstationDO::getLocationId, locationId);
    }

    default Long selectCountByAreaId(Long areaId) {
        return selectCount(MesMdWorkstationDO::getAreaId, areaId);
    }

    default List<MesMdWorkstationDO> selectListByProcessIds(Collection<Long> processIds) {
        return selectList(new LambdaQueryWrapperX<MesMdWorkstationDO>()
                .in(MesMdWorkstationDO::getProcessId, processIds)
                .orderByAsc(MesMdWorkstationDO::getId));
    }

    default List<MesMdWorkstationDO> selectListForShiftHours() {
        return selectList(new LambdaQueryWrapperX<MesMdWorkstationDO>()
                .select(MesMdWorkstationDO::getId, MesMdWorkstationDO::getShiftHours)
                .orderByAsc(MesMdWorkstationDO::getId));
    }

    default int updateAllShiftHours(BigDecimal shiftHours) {
        LambdaUpdateWrapper<MesMdWorkstationDO> updateWrapper = new LambdaUpdateWrapper<MesMdWorkstationDO>()
                .set(MesMdWorkstationDO::getShiftHours, shiftHours);
        return update(null, updateWrapper);
    }

    default void updateSingleStandardHourlyCapacity(Long id, BigDecimal singleStandardHourlyCapacity) {
        updateWorkerCapacity(id, singleStandardHourlyCapacity, null);
    }

    default void updateWorkerCapacity(Long id, BigDecimal singleStandardHourlyCapacity, BigDecimal shiftHours) {
        if (singleStandardHourlyCapacity == null && shiftHours == null) {
            return;
        }
        LambdaUpdateWrapper<MesMdWorkstationDO> updateWrapper = new LambdaUpdateWrapper<MesMdWorkstationDO>()
                .eq(MesMdWorkstationDO::getId, id);
        if (singleStandardHourlyCapacity != null) {
            updateWrapper.set(MesMdWorkstationDO::getSingleStandardHourlyCapacity, singleStandardHourlyCapacity);
        }
        if (shiftHours != null) {
            updateWrapper.set(MesMdWorkstationDO::getShiftHours, shiftHours);
        }
        update(null, updateWrapper);
    }

}
