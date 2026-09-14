package cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaCommonRegulationSetDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesQaCommonRegulationSetMapper extends BaseMapperX<MesQaCommonRegulationSetDO> {

    default MesQaCommonRegulationSetDO selectBySetCode(String setCode) {
        return selectOne(new LambdaQueryWrapperX<MesQaCommonRegulationSetDO>()
                .eq(MesQaCommonRegulationSetDO::getSetCode, setCode)
                .last("LIMIT 1"));
    }

    default List<MesQaCommonRegulationSetDO> selectListOrderByCode() {
        return selectList(new LambdaQueryWrapperX<MesQaCommonRegulationSetDO>()
                .orderByAsc(MesQaCommonRegulationSetDO::getSetCode)
                .orderByDesc(MesQaCommonRegulationSetDO::getId));
    }
}
