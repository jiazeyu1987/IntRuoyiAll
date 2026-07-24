package cn.iocoder.yudao.module.mes.dal.mysql.md.workstation;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesMdProductionLineMapper extends BaseMapperX<MesMdProductionLineDO> {

    default MesMdProductionLineDO selectByCode(String code) {
        return selectOne(MesMdProductionLineDO::getCode, code);
    }

    default MesMdProductionLineDO selectByName(String name) {
        return selectOne(MesMdProductionLineDO::getName, name);
    }

    default List<MesMdProductionLineDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<MesMdProductionLineDO>()
                .eqIfPresent(MesMdProductionLineDO::getStatus, status)
                .orderByAsc(MesMdProductionLineDO::getId));
    }

    default List<MesMdProductionLineDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectByIds(ids);
    }

}
