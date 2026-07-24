package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingAssignmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccControlledFileTrainingAssignmentMapper extends BaseMapperX<DccControlledFileTrainingAssignmentDO> {

    default List<DccControlledFileTrainingAssignmentDO> selectListByTrainingId(Long trainingId) {
        return selectList(DccControlledFileTrainingAssignmentDO::getTrainingId, trainingId);
    }
}
