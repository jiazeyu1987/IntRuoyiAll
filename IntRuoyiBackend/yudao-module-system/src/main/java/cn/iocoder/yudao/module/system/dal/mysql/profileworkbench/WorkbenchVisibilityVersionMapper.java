package cn.iocoder.yudao.module.system.dal.mysql.profileworkbench;
import org.apache.ibatis.annotations.*;
@Mapper
public interface WorkbenchVisibilityVersionMapper {
 @Options(timeout=10)
 @Insert("INSERT INTO system_workbench_visibility_context(tenant_id,user_id,visibility_version) VALUES(#{tenant},#{user},1) ON DUPLICATE KEY UPDATE visibility_version=visibility_version")
 int initialize(@Param("tenant") long tenant,@Param("user") long user);
 @Options(timeout=10)
 @Select("SELECT visibility_version FROM system_workbench_visibility_context WHERE tenant_id=#{tenant} AND user_id=#{user} FOR SHARE")
 Long shared(@Param("tenant") long tenant,@Param("user") long user);
 @Options(timeout=10)
 @Select("SELECT visibility_version FROM system_workbench_visibility_context WHERE tenant_id=#{tenant} AND user_id=#{user} FOR UPDATE")
 Long exclusive(@Param("tenant") long tenant,@Param("user") long user);
 @Options(timeout=10)
 @Update("UPDATE system_workbench_visibility_context SET visibility_version=#{next} WHERE tenant_id=#{tenant} AND user_id=#{user} AND visibility_version=#{expected}")
 int advance(@Param("tenant") long tenant,@Param("user") long user,@Param("expected") long expected,@Param("next") long next);
}
