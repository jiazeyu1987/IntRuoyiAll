package cn.iocoder.yudao.module.dcc.dal.mysql.directory;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * DCC directory access rule mapper.
 */
@Mapper
public interface DccDirectoryAccessRuleMapper extends BaseMapperX<DccDirectoryAccessRuleDO> {

    default java.util.List<DccDirectoryAccessRuleDO> selectListByDirectoryId(Long directoryId) {
        return selectList(DccDirectoryAccessRuleDO::getDirectoryId, directoryId);
    }
}
