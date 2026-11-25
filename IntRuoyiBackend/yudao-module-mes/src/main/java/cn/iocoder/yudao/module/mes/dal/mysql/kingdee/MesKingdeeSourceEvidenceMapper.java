package cn.iocoder.yudao.module.mes.dal.mysql.kingdee;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MesKingdeeSourceEvidenceMapper {
    @Select("SELECT EXISTS (SELECT 1 FROM mes_kingdee_production_order_sync_record WHERE tenant_id = #{tenantId})"
            + " OR EXISTS (SELECT 1 FROM mes_kingdee_bom_list WHERE tenant_id = #{tenantId})"
            + " OR EXISTS (SELECT 1 FROM mes_kingdee_production_material_list WHERE tenant_id = #{tenantId})")
    boolean hasEvidence(@Param("tenantId") Long tenantId);
}
