package cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * MES 设备台账 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesDvMachineryMapper extends BaseMapperX<MesDvMachineryDO> {

    java.util.Map<String, ListFilterQueryUtils.Field> LIST_FILTER_FIELDS = java.util.Map.ofEntries(
            java.util.Map.entry("id", ListFilterQueryUtils.number("id")),
            java.util.Map.entry("code", ListFilterQueryUtils.text("code")),
            java.util.Map.entry("name", ListFilterQueryUtils.text("name")),
            java.util.Map.entry("brand", ListFilterQueryUtils.text("brand")),
            java.util.Map.entry("specification", ListFilterQueryUtils.text("specification")),
            java.util.Map.entry("machineryTypeId", ListFilterQueryUtils.number("machinery_type_id")),
            java.util.Map.entry("workshopId", ListFilterQueryUtils.number("workshop_id")),
            java.util.Map.entry("processName", ListFilterQueryUtils.text("process_name")),
            java.util.Map.entry("standardHourlyCapacity", ListFilterQueryUtils.number("standard_hourly_capacity")),
            java.util.Map.entry("status", ListFilterQueryUtils.number("status")),
            java.util.Map.entry("lastMaintenTime", ListFilterQueryUtils.dateTime("last_mainten_time")),
            java.util.Map.entry("lastCheckTime", ListFilterQueryUtils.dateTime("last_check_time")),
            java.util.Map.entry("remark", ListFilterQueryUtils.text("remark")),
            java.util.Map.entry("createTime", ListFilterQueryUtils.dateTime("create_time")),
            java.util.Map.entry("creator", ListFilterQueryUtils.text("creator")),
            java.util.Map.entry("machineryTypeName", ListFilterQueryUtils.text("(SELECT x.name FROM mes_dv_machinery_type x WHERE x.id=mes_dv_machinery.machinery_type_id AND x.tenant_id=mes_dv_machinery.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("workshopName", ListFilterQueryUtils.text("(SELECT x.name FROM mes_md_workshop x WHERE x.id=mes_dv_machinery.workshop_id AND x.tenant_id=mes_dv_machinery.tenant_id AND x.deleted=0)"))
    );



    default PageResult<MesDvMachineryDO> selectPage(MesDvMachineryPageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<MesDvMachineryDO>()
                .likeIfPresent(MesDvMachineryDO::getCode, reqVO.getCode())
                .likeIfPresent(MesDvMachineryDO::getName, reqVO.getName())
                .likeIfPresent(MesDvMachineryDO::getBrand, reqVO.getBrand())
                .eqIfPresent(MesDvMachineryDO::getMachineryTypeId, reqVO.getMachineryTypeId())
                .inIfPresent(MesDvMachineryDO::getMachineryTypeId, reqVO.getMachineryTypeIds())
                .eqIfPresent(MesDvMachineryDO::getWorkshopId, reqVO.getWorkshopId())
                .eqIfPresent(MesDvMachineryDO::getStatus, reqVO.getStatus())
                .orderByDesc(MesDvMachineryDO::getId), LIST_FILTER_FIELDS));
    }

    default MesDvMachineryDO selectByCode(String code) {
        return selectOne(MesDvMachineryDO::getCode, code);
    }

    default List<MesDvMachineryDO> selectListByCodes(Collection<String> codes) {
        return selectList(MesDvMachineryDO::getCode, codes);
    }

    default Long selectCountByMachineryTypeId(Long machineryTypeId) {
        return selectCount(MesDvMachineryDO::getMachineryTypeId, machineryTypeId);
    }

}
