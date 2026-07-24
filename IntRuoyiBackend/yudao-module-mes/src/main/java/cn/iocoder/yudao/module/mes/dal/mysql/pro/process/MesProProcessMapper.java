package cn.iocoder.yudao.module.mes.dal.mysql.pro.process;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * MES 生产工序 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesProProcessMapper extends BaseMapperX<MesProProcessDO> {

    default PageResult<MesProProcessDO> selectPage(MesProProcessPageReqVO reqVO) {
        LambdaQueryWrapperX<MesProProcessDO> wrapper = new LambdaQueryWrapperX<MesProProcessDO>()
                .eq(MesProProcessDO::getDeleted, false)
                .likeIfPresent(MesProProcessDO::getCode, reqVO.getCode())
                .likeIfPresent(MesProProcessDO::getProductName, reqVO.getProductName())
                .likeIfPresent(MesProProcessDO::getName, reqVO.getName())
                .eqIfPresent(MesProProcessDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesProProcessDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesProProcessDO::getId);
        if (reqVO.getRouteId() != null) {
            wrapper.inSql(MesProProcessDO::getId,
                    "SELECT process_id FROM mes_pro_route_process WHERE deleted = 0 AND route_id = "
                            + reqVO.getRouteId());
        }
        return selectPage(reqVO, wrapper);
    }

    default List<MesProProcessDO> selectList(MesProProcessPageReqVO reqVO) {
        LambdaQueryWrapperX<MesProProcessDO> wrapper = new LambdaQueryWrapperX<MesProProcessDO>()
                .eq(MesProProcessDO::getDeleted, false)
                .likeIfPresent(MesProProcessDO::getCode, reqVO.getCode())
                .likeIfPresent(MesProProcessDO::getProductName, reqVO.getProductName())
                .likeIfPresent(MesProProcessDO::getName, reqVO.getName())
                .eqIfPresent(MesProProcessDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesProProcessDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesProProcessDO::getId);
        if (reqVO.getRouteId() != null) {
            wrapper.inSql(MesProProcessDO::getId,
                    "SELECT process_id FROM mes_pro_route_process WHERE deleted = 0 AND route_id = "
                            + reqVO.getRouteId());
        }
        return selectList(wrapper);
    }

    default MesProProcessDO selectByCode(String code) {
        return selectOne(MesProProcessDO::getCode, code);
    }

    default List<MesProProcessDO> selectListByCode(String code) {
        return selectList(new LambdaQueryWrapperX<MesProProcessDO>()
                .eq(MesProProcessDO::getCode, code)
                .orderByDesc(MesProProcessDO::getId));
    }

    @Select("SELECT * FROM mes_pro_process WHERE id = #{id} LIMIT 1")
    MesProProcessDO selectByIdIgnoreDeleted(@Param("id") Long id);

    @Select({
            "<script>",
            "SELECT * FROM mes_pro_process WHERE id IN ",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<MesProProcessDO> selectListByIdsIgnoreDeleted(@Param("ids") Collection<Long> ids);

    @Select({
            "<script>",
            "SELECT * FROM mes_pro_process WHERE code IN ",
            "<foreach collection='codes' item='code' open='(' separator=',' close=')'>#{code}</foreach>",
            " ORDER BY id",
            "</script>"
    })
    List<MesProProcessDO> selectListByCodesIgnoreDeleted(@Param("codes") Collection<String> codes);

    default MesProProcessDO selectByProductNameAndCode(String productName, String code) {
        LambdaQueryWrapperX<MesProProcessDO> wrapper = new LambdaQueryWrapperX<MesProProcessDO>()
                .eq(MesProProcessDO::getCode, code);
        if (productName == null) {
            wrapper.isNull(MesProProcessDO::getProductName);
        } else {
            wrapper.eq(MesProProcessDO::getProductName, productName);
        }
        return selectOne(wrapper);
    }

    default List<MesProProcessDO> selectListByCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProProcessDO>()
                .in(MesProProcessDO::getCode, codes)
                .orderByDesc(MesProProcessDO::getId));
    }

    default List<MesProProcessDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProProcessDO>()
                .in(MesProProcessDO::getId, ids)
                .orderByDesc(MesProProcessDO::getId));
    }

    default MesProProcessDO selectByName(String name) {
        return selectOne(MesProProcessDO::getName, name);
    }

    default MesProProcessDO selectByProductNameAndName(String productName, String name) {
        LambdaQueryWrapperX<MesProProcessDO> wrapper = new LambdaQueryWrapperX<MesProProcessDO>()
                .eq(MesProProcessDO::getName, name);
        if (productName == null) {
            wrapper.isNull(MesProProcessDO::getProductName);
        } else {
            wrapper.eq(MesProProcessDO::getProductName, productName);
        }
        return selectOne(wrapper);
    }

    default List<MesProProcessDO> selectListByCodePrefix(String codePrefix) {
        return selectList(new LambdaQueryWrapperX<MesProProcessDO>()
                .likeRight(MesProProcessDO::getCode, codePrefix));
    }

    default List<MesProProcessDO> selectListByStatus(Integer status) {
        return selectList(MesProProcessDO::getStatus, status);
    }

}
