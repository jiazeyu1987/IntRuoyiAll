package cn.iocoder.yudao.module.mes.dal.mysql.pro.mesprocess;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.mesprocess.MesProMesProcessCatalogMachineryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesProMesProcessCatalogMachineryMapper
        extends BaseMapperX<MesProMesProcessCatalogMachineryDO> {

    default List<MesProMesProcessCatalogMachineryDO> selectListByCatalogIds(Collection<Long> catalogIds) {
        if (catalogIds == null || catalogIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProMesProcessCatalogMachineryDO>()
                .eq(MesProMesProcessCatalogMachineryDO::getDeleted, false)
                .in(MesProMesProcessCatalogMachineryDO::getCatalogId, catalogIds)
                .orderByAsc(MesProMesProcessCatalogMachineryDO::getCatalogId)
                .orderByAsc(MesProMesProcessCatalogMachineryDO::getMachinerySortNo)
                .orderByAsc(MesProMesProcessCatalogMachineryDO::getId));
    }
}
