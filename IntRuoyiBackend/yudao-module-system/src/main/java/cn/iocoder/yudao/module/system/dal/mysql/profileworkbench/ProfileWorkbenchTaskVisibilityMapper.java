package cn.iocoder.yudao.module.system.dal.mysql.profileworkbench;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.profileworkbench.ProfileWorkbenchTaskVisibilityDO;
import org.apache.ibatis.annotations.*;
@Mapper
public interface ProfileWorkbenchTaskVisibilityMapper extends BaseMapperX<ProfileWorkbenchTaskVisibilityDO> {
 @Options(timeout=10)
 @Select("SELECT * FROM system_profile_workbench_task_visibility WHERE tenant_id=#{tenant} AND user_id=#{user} AND CAST(task_key AS BINARY)=CAST(#{key} AS BINARY) AND deleted=0 FOR UPDATE")
 ProfileWorkbenchTaskVisibilityDO lockPersonal(@Param("tenant") long tenant,@Param("user") long user,@Param("key") String key);
 @Options(timeout=10)
 @Delete("DELETE FROM system_profile_workbench_task_visibility WHERE tenant_id=#{tenant} AND user_id=#{user} AND CAST(task_key AS BINARY)=CAST(#{key} AS BINARY) AND deleted=0")
 int deletePersonal(@Param("tenant") long tenant,@Param("user") long user,@Param("key") String key);
}
