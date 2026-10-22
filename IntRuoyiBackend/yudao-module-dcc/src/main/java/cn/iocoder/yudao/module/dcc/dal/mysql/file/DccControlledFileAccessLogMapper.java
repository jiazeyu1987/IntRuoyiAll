package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * DCC controlled file access log mapper.
 */
@Mapper
public interface DccControlledFileAccessLogMapper extends BaseMapperX<DccControlledFileAccessLogDO> {

    default List<DccControlledFileAccessLogDO> selectListByControlledFileId(Long controlledFileId) {
        return selectList(DccControlledFileAccessLogDO::getControlledFileId, controlledFileId);
    }

    default List<DccControlledFileAccessLogDO> selectListByAccessEventIds(Collection<Long> accessEventIds) {
        return selectList(new LambdaQueryWrapperX<DccControlledFileAccessLogDO>()
                .inIfPresent(DccControlledFileAccessLogDO::getAccessEventId, accessEventIds));
    }
}
