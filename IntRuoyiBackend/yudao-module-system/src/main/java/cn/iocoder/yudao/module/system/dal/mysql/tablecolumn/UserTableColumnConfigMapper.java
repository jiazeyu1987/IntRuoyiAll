package cn.iocoder.yudao.module.system.dal.mysql.tablecolumn;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.tablecolumn.UserTableColumnConfigDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserTableColumnConfigMapper extends BaseMapperX<UserTableColumnConfigDO> {

    default UserTableColumnConfigDO selectByUserAndTableKey(Long userId, String tableKey) {
        return selectOne(new LambdaQueryWrapperX<UserTableColumnConfigDO>()
                .eq(UserTableColumnConfigDO::getUserId, userId)
                .eq(UserTableColumnConfigDO::getTableKey, tableKey));
    }

    default int deleteByUserAndTableKey(Long userId, String tableKey) {
        return delete(new LambdaQueryWrapperX<UserTableColumnConfigDO>()
                .eq(UserTableColumnConfigDO::getUserId, userId)
                .eq(UserTableColumnConfigDO::getTableKey, tableKey));
    }

}
