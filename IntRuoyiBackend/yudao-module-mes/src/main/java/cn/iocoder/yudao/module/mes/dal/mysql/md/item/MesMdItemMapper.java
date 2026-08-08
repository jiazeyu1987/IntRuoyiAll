package cn.iocoder.yudao.module.mes.dal.mysql.md.item;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.md.item.vo.MesMdItemPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * MES 物料产品 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesMdItemMapper extends BaseMapperX<MesMdItemDO> {

    default List<MesMdItemDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<MesMdItemDO>()
                .orderByDesc(MesMdItemDO::getId));
    }

    default PageResult<MesMdItemDO> selectPage(MesMdItemPageReqVO reqVO, Collection<Long> itemTypeIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesMdItemDO>()
                .likeIfPresent(MesMdItemDO::getCode, reqVO.getCode())
                .likeIfPresent(MesMdItemDO::getName, reqVO.getName())
                .inIfPresent(MesMdItemDO::getItemTypeId, itemTypeIds)
                .eqIfPresent(MesMdItemDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesMdItemDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesMdItemDO::getId));
    }

    default MesMdItemDO selectByCode(String code) {
        return selectOne(MesMdItemDO::getCode, code);
    }

    default List<MesMdItemDO> selectListByCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesMdItemDO>()
                .in(MesMdItemDO::getCode, codes)
                .orderByDesc(MesMdItemDO::getId));
    }

    default List<MesMdItemDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesMdItemDO>()
                .in(MesMdItemDO::getId, ids)
                .orderByDesc(MesMdItemDO::getId));
    }

    default MesMdItemDO selectByName(String name) {
        return selectOne(MesMdItemDO::getName, name);
    }

    default List<MesMdItemDO> selectListByName(String name) {
        return selectList(new LambdaQueryWrapperX<MesMdItemDO>()
                .eqIfPresent(MesMdItemDO::getName, name)
                .orderByAsc(MesMdItemDO::getId));
    }

    default List<MesMdItemDO> selectListByNameLike(String name) {
        return selectList(new LambdaQueryWrapperX<MesMdItemDO>()
                .likeIfPresent(MesMdItemDO::getName, name)
                .orderByAsc(MesMdItemDO::getId));
    }

    default List<MesMdItemDO> selectListByCodeLike(String code) {
        return selectList(new LambdaQueryWrapperX<MesMdItemDO>()
                .likeIfPresent(MesMdItemDO::getCode, code)
                .orderByAsc(MesMdItemDO::getId));
    }

    default List<MesMdItemDO> selectListByCodeOrNameLike(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 20));
        String searchText = keyword.trim();
        return selectList(new LambdaQueryWrapperX<MesMdItemDO>()
                .and(wrapper -> wrapper.like(MesMdItemDO::getCode, searchText)
                        .or()
                        .like(MesMdItemDO::getName, searchText))
                .orderByDesc(MesMdItemDO::getId)
                .last("LIMIT " + safeLimit));
    }

    default List<MesMdItemDO> selectListBySpecificationLike(String specification) {
        return selectList(new LambdaQueryWrapperX<MesMdItemDO>()
                .likeIfPresent(MesMdItemDO::getSpecification, specification)
                .orderByAsc(MesMdItemDO::getId));
    }

    default Long selectCountByItemTypeId(Long itemTypeId) {
        return selectCount(MesMdItemDO::getItemTypeId, itemTypeId);
    }

    default Long selectCountByUnitMeasureId(Long unitMeasureId) {
        return selectCount(MesMdItemDO::getUnitMeasureId, unitMeasureId);
    }

}
