package cn.iocoder.yudao.module.dcc.dal.mysql.protection;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * DCC controlled file access event mapper.
 */
@Mapper
public interface DccControlledFileAccessEventMapper extends BaseMapperX<DccControlledFileAccessEventDO> {

    default List<DccControlledFileAccessEventDO> selectListByControlledFileId(Long controlledFileId) {
        return selectList(DccControlledFileAccessEventDO::getControlledFileId, controlledFileId);
    }
}
