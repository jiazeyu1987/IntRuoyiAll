package cn.iocoder.yudao.module.mes.dal.mysql.pro.craft;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.MesProCraftDO;
import org.apache.ibatis.annotations.*;
@Mapper
public interface MesProCraftMapper extends BaseMapperX<MesProCraftDO> {
    @Insert("INSERT INTO mes_pro_craft_sequence (tenant_id,last_number) VALUES (#{tenantId},0) ON DUPLICATE KEY UPDATE tenant_id=VALUES(tenant_id)")
    void ensureSequence(Long tenantId);
    @Select("SELECT last_number FROM mes_pro_craft_sequence WHERE tenant_id=#{tenantId} FOR UPDATE")
    Long lockSequence(Long tenantId);
    @Update("UPDATE mes_pro_craft_sequence SET last_number=#{number} WHERE tenant_id=#{tenantId}")
    int advanceSequence(@Param("tenantId") Long tenantId, @Param("number") Long number);
}
