package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MesProcessPoolTeamEmployeeProfileMapper extends BaseMapperX<MesProcessPoolTeamEmployeeProfileDO> {
    @org.apache.ibatis.annotations.Update("UPDATE mes_pro_process_pool_team_employee_profile SET signature_credential_type=#{type},signature_pin_hash=#{hash},update_time=CURRENT_TIMESTAMP WHERE tenant_id=#{tenantId} AND id=#{profileId} AND deleted=0")
    int updateSignatureSettings(@org.apache.ibatis.annotations.Param("tenantId") Long tenantId,@org.apache.ibatis.annotations.Param("profileId") Long profileId,@org.apache.ibatis.annotations.Param("type") String type,@org.apache.ibatis.annotations.Param("hash") String hash);

    @Select("SELECT password_hash FROM mes_employee_signature_password_history WHERE tenant_id=#{tenant} AND employee_profile_id=#{id} ORDER BY id DESC LIMIT 5")
    java.util.List<String> selectSignaturePasswordHistory(@Param("tenant") Long tenant,@Param("id") Long id);
    @org.apache.ibatis.annotations.Insert("INSERT INTO mes_employee_signature_password_history(tenant_id,employee_profile_id,password_hash,changed_at) VALUES(#{tenant},#{id},#{hash},#{at})")
    int insertSignaturePasswordHistory(@Param("tenant") Long tenant,@Param("id") Long id,@Param("hash") String hash,@Param("at") java.time.LocalDateTime at);
    @Select("SELECT * FROM mes_pro_process_pool_team_employee_profile WHERE tenant_id=#{tenant} AND id=#{id} AND deleted=0 FOR UPDATE")
    MesProcessPoolTeamEmployeeProfileDO selectSignatureIdentityForUpdate(@Param("tenant") Long tenant, @Param("id") Long id);
    @Update("UPDATE mes_pro_process_pool_team_employee_profile SET signature_failure_count=#{count},signature_failure_window_start_at=#{start},signature_locked_until=#{until} WHERE tenant_id=#{tenant} AND id=#{id} AND deleted=0")
    int writeSignatureFailures(@Param("tenant") Long tenant,@Param("id") Long id,@Param("count") int count,@Param("start") java.time.LocalDateTime start,@Param("until") java.time.LocalDateTime until);
}
