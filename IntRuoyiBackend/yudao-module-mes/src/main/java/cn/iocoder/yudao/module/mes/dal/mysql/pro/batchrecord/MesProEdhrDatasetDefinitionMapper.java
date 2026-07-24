package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDatasetDefinitionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrDatasetDefinitionMapper extends BaseMapperX<MesProEdhrDatasetDefinitionDO> {

    default MesProEdhrDatasetDefinitionDO selectByCodeAndVersion(String datasetCode, String datasetVersion) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrDatasetDefinitionDO>()
                .eq(MesProEdhrDatasetDefinitionDO::getDatasetCode, datasetCode)
                .eq(MesProEdhrDatasetDefinitionDO::getDatasetVersion, datasetVersion));
    }
}
