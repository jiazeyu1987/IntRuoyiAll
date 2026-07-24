package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccControlledFileTrainingMapper extends BaseMapperX<DccControlledFileTrainingDO> {

    default List<DccControlledFileTrainingDO> selectListByControlledFileId(Long controlledFileId) {
        return selectList(DccControlledFileTrainingDO::getControlledFileId, controlledFileId);
    }
}
