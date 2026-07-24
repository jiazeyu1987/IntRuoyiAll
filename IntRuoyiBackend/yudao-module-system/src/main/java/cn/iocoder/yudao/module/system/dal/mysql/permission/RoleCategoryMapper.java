package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface RoleCategoryMapper extends BaseMapperX<RoleCategoryDO> {

    default RoleCategoryDO selectByName(String name) {
        return selectOne(RoleCategoryDO::getName, name);
    }

    default RoleCategoryDO selectByCode(String code) {
        return selectOne(RoleCategoryDO::getCode, code);
    }

    default List<RoleCategoryDO> selectListByStatus(Collection<Integer> statuses) {
        return selectList(new LambdaQueryWrapperX<RoleCategoryDO>()
                .inIfPresent(RoleCategoryDO::getStatus, statuses)
                .orderByAsc(RoleCategoryDO::getSort)
                .orderByAsc(RoleCategoryDO::getId));
    }

    default List<RoleCategoryDO> selectListOrderBySort() {
        return selectListByStatus(null);
    }

    default List<RoleCategoryDO> selectEnabledList() {
        return selectListByStatus(List.of(CommonStatusEnum.ENABLE.getStatus()));
    }

}
