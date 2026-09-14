package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MesRouteDccProjectBindingMapper extends BaseMapperX<MesRouteDccProjectBindingDO> {

    default MesRouteDccProjectBindingDO selectCurrentByRouteId(Long routeId) {
        return selectOne(new LambdaQueryWrapperX<MesRouteDccProjectBindingDO>()
                .eq(MesRouteDccProjectBindingDO::getRouteId, routeId));
    }

    default MesRouteDccProjectBindingDO selectCurrentByRouteIdForUpdate(Long routeId) {
        return selectOne(new LambdaQueryWrapperX<MesRouteDccProjectBindingDO>()
                .eq(MesRouteDccProjectBindingDO::getRouteId, routeId)
                .last("FOR UPDATE"));
    }

    default List<MesRouteDccProjectBindingDO> selectCurrentListByDccProjectCodeId(Long dccProjectCodeId) {
        return selectList(new LambdaQueryWrapperX<MesRouteDccProjectBindingDO>()
                .eq(MesRouteDccProjectBindingDO::getDccProjectCodeId, dccProjectCodeId)
                .orderByAsc(MesRouteDccProjectBindingDO::getRouteId));
    }

    @Select("SELECT COALESCE(MAX(version), 0) FROM mes_pro_route_dcc_project_binding "
            + "WHERE route_id = #{routeId}")
    Long selectMaxVersionByRouteIdIncludeDeleted(@Param("routeId") Long routeId);

    @Update("UPDATE mes_pro_route_dcc_project_binding SET deleted = b'1' "
            + "WHERE id = #{id} AND deleted = b'0'")
    int markDeletedById(@Param("id") Long id);
}
