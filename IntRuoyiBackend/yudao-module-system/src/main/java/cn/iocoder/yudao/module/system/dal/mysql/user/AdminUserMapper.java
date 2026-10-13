package cn.iocoder.yudao.module.system.dal.mysql.user;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface AdminUserMapper extends BaseMapperX<AdminUserDO> {

    default AdminUserDO selectSessionSubject(Long tenantId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<AdminUserDO>()
                .eq(AdminUserDO::getTenantId, tenantId).eq(AdminUserDO::getId, userId)
                .eq(AdminUserDO::getDeleted, false));
    }
    @Select("SELECT * FROM system_users WHERE tenant_id = #{tenantId} ORDER BY id FOR UPDATE")
    List<AdminUserDO> selectAllTenantIdentitiesForUpdate(@Param("tenantId") Long tenantId);

    @Update("UPDATE system_users SET login_failure_count=0, login_failure_window_start_time=NULL, login_locked=0, login_locked_time=NULL WHERE tenant_id=#{tenantId} AND id=#{userId} AND deleted=0")
    int resetSignatureFailures(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Update("UPDATE system_users SET login_failure_count=#{count}, login_failure_window_start_time=#{windowStart}, login_locked=#{locked}, login_locked_time=#{lockedAt} WHERE tenant_id=#{tenantId} AND id=#{userId} AND deleted=0")
    int recordSignatureFailure(@Param("tenantId") Long tenantId, @Param("userId") Long userId,
            @Param("count") int count, @Param("windowStart") LocalDateTime windowStart,
            @Param("locked") int locked, @Param("lockedAt") LocalDateTime lockedAt);

    default AdminUserDO selectPermissionSubjectForUpdate(Long tenantId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<AdminUserDO>()
                .eq(AdminUserDO::getTenantId, tenantId).eq(AdminUserDO::getId, userId)
                .eq(AdminUserDO::getDeleted, false).last("FOR UPDATE"));
    }

    @Select("""
            SELECT post_ids
            FROM system_users
            WHERE id = #{userId} AND tenant_id = #{tenantId} AND deleted = FALSE
            FOR UPDATE
            """)
    String selectPostIdsForUpdate(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    default AdminUserDO selectByUsername(String username) {
        return selectOne(AdminUserDO::getUsername, username);
    }

    @Select("""
            SELECT *
            FROM system_users
            WHERE canonical_username = #{canonicalUsername}
              AND (#{tenantId} IS NULL OR tenant_id = #{tenantId})
            LIMIT 1
            """)
    AdminUserDO selectByCanonicalUsernameIncludingDeleted(@Param("tenantId") Long tenantId,
                                                          @Param("canonicalUsername") String canonicalUsername);

    @Select("""
            SELECT *
            FROM system_users
            WHERE id = #{id}
            FOR UPDATE
            """)
    AdminUserDO selectByIdForUpdate(@Param("id") Long id);

    default AdminUserDO selectByEmail(String email) {
        return selectOne(AdminUserDO::getEmail, email);
    }

    default AdminUserDO selectByMobile(String mobile) {
        return selectOne(AdminUserDO::getMobile, mobile);
    }

    default PageResult<AdminUserDO> selectPage(UserPageReqVO reqVO, Collection<Long> deptIds, Collection<Long> userIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AdminUserDO>()
                .likeIfPresent(AdminUserDO::getUsername, reqVO.getUsername())
                .likeIfPresent(AdminUserDO::getMobile, reqVO.getMobile())
                .eqIfPresent(AdminUserDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AdminUserDO::getCreateTime, reqVO.getCreateTime())
                .inIfPresent(AdminUserDO::getDeptId, deptIds)
                .inIfPresent(AdminUserDO::getId, userIds)
                .orderByDesc(AdminUserDO::getId));
    }

    default List<AdminUserDO> selectListForPage(UserPageReqVO reqVO, Collection<Long> deptIds, Collection<Long> userIds) {
        return selectList(new LambdaQueryWrapperX<AdminUserDO>()
                .likeIfPresent(AdminUserDO::getUsername, reqVO.getUsername())
                .likeIfPresent(AdminUserDO::getMobile, reqVO.getMobile())
                .eqIfPresent(AdminUserDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AdminUserDO::getCreateTime, reqVO.getCreateTime())
                .inIfPresent(AdminUserDO::getDeptId, deptIds)
                .inIfPresent(AdminUserDO::getId, userIds)
                .orderByDesc(AdminUserDO::getId));
    }

    default List<AdminUserDO> selectListByNickname(String nickname) {
        return selectList(new LambdaQueryWrapperX<AdminUserDO>().like(AdminUserDO::getNickname, nickname));
    }

    default List<AdminUserDO> selectListByUsernames(Collection<String> usernames) {
        return selectList(new LambdaQueryWrapperX<AdminUserDO>().inIfPresent(AdminUserDO::getUsername, usernames));
    }

    default List<AdminUserDO> selectListByNicknamesExact(Collection<String> nicknames) {
        return selectList(new LambdaQueryWrapperX<AdminUserDO>().inIfPresent(AdminUserDO::getNickname, nicknames));
    }

    default List<AdminUserDO> selectListByStatus(Integer status) {
        return selectList(AdminUserDO::getStatus, status);
    }

    default List<AdminUserDO> selectListByPendingLifecycleDeactivation(LocalDateTime now, int limit) {
        return selectList(new LambdaQueryWrapperX<AdminUserDO>()
                .isNotNull(AdminUserDO::getLifecycleDocumentType)
                .isNotNull(AdminUserDO::getLifecycleDocumentNo)
                .isNotNull(AdminUserDO::getLifecycleEffectiveTime)
                .isNull(AdminUserDO::getLifecycleDeactivatedTime)
                .le(AdminUserDO::getLifecycleEffectiveTime, now)
                .orderByAsc(AdminUserDO::getLifecycleEffectiveTime)
                .last("LIMIT " + limit));
    }

    default List<AdminUserDO> selectListByDeptIds(Collection<Long> deptIds) {
        return selectList(AdminUserDO::getDeptId, deptIds);
    }

    default Long selectCountByDeptIds(Collection<Long> deptIds) {
        return selectCount(new LambdaQueryWrapperX<AdminUserDO>()
                .inIfPresent(AdminUserDO::getDeptId, deptIds));
    }

}
