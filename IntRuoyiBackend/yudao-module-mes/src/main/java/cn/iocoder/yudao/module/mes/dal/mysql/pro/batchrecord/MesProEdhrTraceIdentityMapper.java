package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.Set;

@Mapper
public interface MesProEdhrTraceIdentityMapper {
    @Select("SELECT id FROM mes_pro_process_pool_team_employee_profile WHERE tenant_id=#{tenantId} AND deleted=0 "
            + "AND (LOCATE(#{keyword},employee_code)>0 OR LOCATE(#{keyword},employee_name)>0 OR LOCATE(#{keyword},display_name)>0)")
    Set<Long> employeeIds(@Param("tenantId") Long tenantId, @Param("keyword") String keyword);

    @Select("SELECT id FROM system_users WHERE tenant_id=#{tenantId} AND deleted=0 "
            + "AND (LOCATE(#{keyword},username)>0 OR LOCATE(#{keyword},nickname)>0)")
    Set<Long> userIds(@Param("tenantId") Long tenantId, @Param("keyword") String keyword);

    @Select("SELECT id FROM mes_pro_process_pool_team_device WHERE tenant_id=#{tenantId} AND deleted=0 "
            + "AND (LOCATE(#{keyword},device_code)>0 OR LOCATE(#{keyword},device_name)>0)")
    Set<Long> deviceIds(@Param("tenantId") Long tenantId, @Param("keyword") String keyword);
}
