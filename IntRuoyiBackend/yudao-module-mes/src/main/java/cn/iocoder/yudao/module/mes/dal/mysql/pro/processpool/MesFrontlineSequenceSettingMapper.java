package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import org.apache.ibatis.annotations.*;

@Mapper
public interface MesFrontlineSequenceSettingMapper {
    @Select("SELECT frontline_process_sequence_enabled FROM system_tenant WHERE id = #{tenantId} AND deleted = 0")
    Boolean getEnabled(@Param("tenantId") Long tenantId);

    @Update("UPDATE system_tenant SET frontline_process_sequence_enabled = #{enabled}, updater = #{actor}, update_time = CURRENT_TIMESTAMP WHERE id = #{tenantId} AND deleted = 0")
    int updateEnabled(@Param("tenantId") Long tenantId, @Param("enabled") boolean enabled, @Param("actor") String actor);
}
