package cn.iocoder.yudao.module.mes.dal.mysql.md.workstation;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
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

    java.util.Map<String, ListFilterQueryUtils.Field> LIST_FILTER_FIELDS = java.util.Map.ofEntries(
            java.util.Map.entry("id", ListFilterQueryUtils.number("id")),
            java.util.Map.entry("code", ListFilterQueryUtils.text("code")),
            java.util.Map.entry("name", ListFilterQueryUtils.text("name")),
            java.util.Map.entry("address", ListFilterQueryUtils.text("address")),
            java.util.Map.entry("workshopId", ListFilterQueryUtils.number("workshop_id")),
            java.util.Map.entry("processId", ListFilterQueryUtils.number("process_id")),
            java.util.Map.entry("productionLineId", ListFilterQueryUtils.number("production_line_id")),
            java.util.Map.entry("warehouseId", ListFilterQueryUtils.number("warehouse_id")),
            java.util.Map.entry("locationId", ListFilterQueryUtils.number("location_id")),
            java.util.Map.entry("areaId", ListFilterQueryUtils.number("area_id")),
            java.util.Map.entry("singleStandardHourlyCapacity", ListFilterQueryUtils.number("single_standard_hourly_capacity")),
            java.util.Map.entry("shiftHours", ListFilterQueryUtils.number("shift_hours")),
            java.util.Map.entry("status", ListFilterQueryUtils.number("status")),
            java.util.Map.entry("remark", ListFilterQueryUtils.text("remark")),
            java.util.Map.entry("createTime", ListFilterQueryUtils.dateTime("create_time")),
            java.util.Map.entry("creator", ListFilterQueryUtils.text("creator")),
            java.util.Map.entry("workshopName", ListFilterQueryUtils.text("(SELECT x.name FROM mes_md_workshop x WHERE x.id=mes_md_workstation.workshop_id AND x.tenant_id=mes_md_workstation.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("processName", ListFilterQueryUtils.text("(SELECT x.name FROM mes_pro_process x WHERE x.id=mes_md_workstation.process_id AND x.tenant_id=mes_md_workstation.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("configuredWorkerCount", ListFilterQueryUtils.number("(SELECT COALESCE(SUM(x.quantity),0) FROM mes_md_workstation_worker x WHERE x.workstation_id=mes_md_workstation.id AND x.tenant_id=mes_md_workstation.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("machineryCount", ListFilterQueryUtils.number("(SELECT COUNT(*) FROM mes_md_workstation_machine x WHERE x.workstation_id=mes_md_workstation.id AND x.tenant_id=mes_md_workstation.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("currentWorkerCount", ListFilterQueryUtils.number("(SELECT COUNT(*) FROM mes_pro_work_record x WHERE x.workstation_id=mes_md_workstation.id AND x.type=1 AND x.tenant_id=mes_md_workstation.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("machineryStandardHourlyCapacity", ListFilterQueryUtils.number("(SELECT COALESCE(SUM(x.quantity * COALESCE((SELECT mp.standard_hourly_capacity FROM mes_dv_machinery_process mp WHERE mp.machinery_id=x.machinery_id AND mp.process_id=mes_md_workstation.process_id AND mp.tenant_id=mes_md_workstation.tenant_id AND mp.deleted=0 AND mp.standard_hourly_capacity IS NOT NULL LIMIT 1),0)),0) FROM mes_md_workstation_machine x WHERE x.workstation_id=mes_md_workstation.id AND x.tenant_id=mes_md_workstation.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("todayCapacity", ListFilterQueryUtils.number("(CASE WHEN EXISTS(SELECT 1 FROM mes_md_workstation_machine x WHERE x.workstation_id=mes_md_workstation.id AND x.tenant_id=mes_md_workstation.tenant_id AND x.deleted=0) THEN (SELECT COALESCE(SUM(x.quantity * COALESCE((SELECT mp.standard_hourly_capacity FROM mes_dv_machinery_process mp WHERE mp.machinery_id=x.machinery_id AND mp.process_id=mes_md_workstation.process_id AND mp.tenant_id=mes_md_workstation.tenant_id AND mp.deleted=0 AND mp.standard_hourly_capacity IS NOT NULL LIMIT 1),0)),0) FROM mes_md_workstation_machine x WHERE x.workstation_id=mes_md_workstation.id AND x.tenant_id=mes_md_workstation.tenant_id AND x.deleted=0) ELSE COALESCE(single_standard_hourly_capacity,0) END * shift_hours)"))
    );



    default PageResult<MesMdWorkstationDO> selectPage(MesMdWorkstationPageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<MesMdWorkstationDO>()
                .eqIfPresent(MesMdWorkstationDO::getCode, reqVO.getCode())
                .likeIfPresent(MesMdWorkstationDO::getName, reqVO.getName())
                .eqIfPresent(MesMdWorkstationDO::getWorkshopId, reqVO.getWorkshopId())
                .eqIfPresent(MesMdWorkstationDO::getProcessId, reqVO.getProcessId())
                .eqIfPresent(MesMdWorkstationDO::getStatus, reqVO.getStatus())
                .orderByDesc(MesMdWorkstationDO::getId), LIST_FILTER_FIELDS));
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
