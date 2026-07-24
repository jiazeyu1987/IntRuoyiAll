package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccControlledFileDistributionMapper extends BaseMapperX<DccControlledFileDistributionDO> {

    default List<DccControlledFileDistributionDO> selectListByControlledFileId(Long controlledFileId) {
        return selectList(DccControlledFileDistributionDO::getControlledFileId, controlledFileId);
    }
}
