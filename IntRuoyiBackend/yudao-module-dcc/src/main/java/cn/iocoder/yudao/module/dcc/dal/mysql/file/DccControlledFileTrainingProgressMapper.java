package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingProgressDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccControlledFileTrainingProgressMapper extends BaseMapperX<DccControlledFileTrainingProgressDO> {

    default DccControlledFileTrainingProgressDO selectByControlledFileIdAndUserId(Long controlledFileId, Long userId) {
        return selectOne(DccControlledFileTrainingProgressDO::getControlledFileId, controlledFileId,
                DccControlledFileTrainingProgressDO::getUserId, userId);
    }

    default List<DccControlledFileTrainingProgressDO> selectListByControlledFileId(Long controlledFileId) {
        return selectList(DccControlledFileTrainingProgressDO::getControlledFileId, controlledFileId);
    }

    default List<DccControlledFileTrainingProgressDO> selectListByUserId(Long userId) {
        return selectList(DccControlledFileTrainingProgressDO::getUserId, userId);
    }
}
