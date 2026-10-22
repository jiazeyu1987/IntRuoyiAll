package cn.iocoder.yudao.module.system.dal.mysql.user;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserPasswordHistoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AdminUserPasswordHistoryMapper extends BaseMapperX<AdminUserPasswordHistoryDO> {

    /** 用户行锁内读取当前历史，不使用外层 RR 事务的普通读快照。 */
    default List<AdminUserPasswordHistoryDO> selectLatestListForUpdate(Long tenantId, Long userId, int limit) {
        return selectList(new LambdaQueryWrapperX<AdminUserPasswordHistoryDO>()
                .eq(AdminUserPasswordHistoryDO::getTenantId, tenantId)
                .eq(AdminUserPasswordHistoryDO::getUserId, userId)
                .eq(AdminUserPasswordHistoryDO::getDeleted, false)
                .orderByDesc(AdminUserPasswordHistoryDO::getChangedAt)
                .orderByDesc(AdminUserPasswordHistoryDO::getId)
                .last("LIMIT " + limit + " FOR UPDATE"));
    }

    default List<AdminUserPasswordHistoryDO> selectLatestListByUserId(Long userId, int limit) {
        return selectList(new LambdaQueryWrapperX<AdminUserPasswordHistoryDO>()
                .eq(AdminUserPasswordHistoryDO::getUserId, userId)
                .orderByDesc(AdminUserPasswordHistoryDO::getChangedAt)
                .orderByDesc(AdminUserPasswordHistoryDO::getId)
                .last("LIMIT " + limit));
    }

}
