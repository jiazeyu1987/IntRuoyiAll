package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccControlledFileMetadataChangeMapper extends BaseMapperX<DccControlledFileMetadataChangeDO> {

    default List<DccControlledFileMetadataChangeDO> selectListBySource(String source) {
        return selectList(new LambdaQueryWrapperX<DccControlledFileMetadataChangeDO>()
                .eq(DccControlledFileMetadataChangeDO::getSource, source));
    }
}
