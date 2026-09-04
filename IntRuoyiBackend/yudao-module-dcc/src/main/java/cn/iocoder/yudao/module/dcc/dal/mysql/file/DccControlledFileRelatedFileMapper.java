package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRelatedFileDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccControlledFileRelatedFileMapper extends BaseMapperX<DccControlledFileRelatedFileDO> {

    default List<DccControlledFileRelatedFileDO> selectListByControlledFileId(Long controlledFileId) {
        return selectList(new LambdaQueryWrapperX<DccControlledFileRelatedFileDO>()
                .eq(DccControlledFileRelatedFileDO::getControlledFileId, controlledFileId)
                .orderByAsc(DccControlledFileRelatedFileDO::getId));
    }

}
