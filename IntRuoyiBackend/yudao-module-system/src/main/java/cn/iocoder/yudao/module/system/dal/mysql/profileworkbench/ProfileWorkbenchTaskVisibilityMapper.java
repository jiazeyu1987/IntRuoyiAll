package cn.iocoder.yudao.module.system.dal.mysql.profileworkbench;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.profileworkbench.ProfileWorkbenchTaskVisibilityDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProfileWorkbenchTaskVisibilityMapper extends BaseMapperX<ProfileWorkbenchTaskVisibilityDO> {

    default ProfileWorkbenchTaskVisibilityDO selectByUserAndTaskKey(Long userId, String taskKey) {
        return selectOne(new LambdaQueryWrapperX<ProfileWorkbenchTaskVisibilityDO>()
                .eq(ProfileWorkbenchTaskVisibilityDO::getUserId, userId)
                .eq(ProfileWorkbenchTaskVisibilityDO::getTaskKey, taskKey));
    }

    default List<ProfileWorkbenchTaskVisibilityDO> selectListByUser(Long userId) {
        return selectList(new LambdaQueryWrapperX<ProfileWorkbenchTaskVisibilityDO>()
                .eq(ProfileWorkbenchTaskVisibilityDO::getUserId, userId)
                .orderByAsc(ProfileWorkbenchTaskVisibilityDO::getId));
    }

    @Delete("DELETE FROM system_profile_workbench_task_visibility "
            + "WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND task_key = #{taskKey}")
    int deletePhysicalByUserAndTaskKey(@Param("tenantId") Long tenantId,
                                       @Param("userId") Long userId,
                                       @Param("taskKey") String taskKey);
}
