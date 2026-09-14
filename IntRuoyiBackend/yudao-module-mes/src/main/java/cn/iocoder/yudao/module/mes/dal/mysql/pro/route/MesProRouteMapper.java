package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRoutePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * MES 工艺路线 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesProRouteMapper extends BaseMapperX<MesProRouteDO> {

    default MesProRouteDO selectByIdForUpdate(Long id) {
        if (id == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProRouteDO>()
                .eq(MesProRouteDO::getId, id)
                .last("FOR UPDATE"));
    }

    default PageResult<MesProRouteDO> selectPage(MesProRoutePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProRouteDO>()
                .likeIfPresent(MesProRouteDO::getCode, reqVO.getCode())
                .likeIfPresent(MesProRouteDO::getName, reqVO.getName())
                .eqIfPresent(MesProRouteDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesProRouteDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesProRouteDO::getId));
    }

    default MesProRouteDO selectByCode(String code) {
        return selectOne(MesProRouteDO::getCode, code);
    }

    default MesProRouteDO selectByName(String name) {
        return selectOne(MesProRouteDO::getName, name);
    }

    default List<MesProRouteDO> selectListByName(String name) {
        return selectList(new LambdaQueryWrapperX<MesProRouteDO>()
                .eq(MesProRouteDO::getName, name)
                .orderByAsc(MesProRouteDO::getId));
    }

    default List<MesProRouteDO> selectListByCodePrefix(String codePrefix) {
        return selectList(new LambdaQueryWrapperX<MesProRouteDO>()
                .likeRight(MesProRouteDO::getCode, codePrefix));
    }

    default List<MesProRouteDO> selectListByStatus(Integer status) {
        return selectList(MesProRouteDO::getStatus, status);
    }

    @Select({
            "<script>",
            "SELECT id, code, name, description, status, remark, create_time, update_time, creator, updater, deleted, tenant_id ",
            "FROM mes_pro_route ",
            "WHERE id IN ",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<MesProRouteDO> selectListByIdsIgnoreDeleted(@Param("ids") Collection<Long> ids);

    @Select("SELECT id, code, name, description, status, remark, create_time, update_time, creator, updater, deleted, tenant_id " +
            "FROM mes_pro_route WHERE id = #{id} LIMIT 1")
    MesProRouteDO selectByIdIgnoreDeleted(@Param("id") Long id);

}
