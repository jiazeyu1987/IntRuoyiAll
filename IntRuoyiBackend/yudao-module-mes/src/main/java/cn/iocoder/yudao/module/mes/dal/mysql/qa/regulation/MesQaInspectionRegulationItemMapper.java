package cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesQaInspectionRegulationItemMapper
        extends BaseMapperX<MesQaInspectionRegulationItemDO> {

    default List<MesQaInspectionRegulationItemDO> selectListByVersionId(Long regulationVersionId) {
        return selectList(new LambdaQueryWrapperX<MesQaInspectionRegulationItemDO>()
                .eq(MesQaInspectionRegulationItemDO::getRegulationVersionId, regulationVersionId)
                .orderByAsc(MesQaInspectionRegulationItemDO::getInspectionType)
                .orderByAsc(MesQaInspectionRegulationItemDO::getItemCode)
                .orderByAsc(MesQaInspectionRegulationItemDO::getId));
    }
}
