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
    // 唯一索引也保留已删除工艺的编号，必须一并计入已占用范围。
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(code,4) AS SIGNED)),0) FROM mes_pro_craft WHERE tenant_id=#{tenantId} AND code REGEXP '^GY-[0-9]+$'")
    long selectMaxCodeNumber(Long tenantId);
    @Update("UPDATE mes_pro_craft_sequence SET last_number=#{number} WHERE tenant_id=#{tenantId}")
    int advanceSequence(@Param("tenantId") Long tenantId, @Param("number") Long number);
}
