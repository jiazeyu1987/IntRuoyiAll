package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAttachmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccControlledFileAttachmentMapper extends BaseMapperX<DccControlledFileAttachmentDO> {

    default List<DccControlledFileAttachmentDO> selectListByControlledFileId(Long controlledFileId) {
        return selectList(new LambdaQueryWrapperX<DccControlledFileAttachmentDO>()
                .eq(DccControlledFileAttachmentDO::getControlledFileId, controlledFileId)
                .orderByAsc(DccControlledFileAttachmentDO::getSortNo)
                .orderByAsc(DccControlledFileAttachmentDO::getId));
    }

}
