package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesEdhrManagedHandoffMaterialAssociationDO;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MesEdhrManagedHandoffMaterialAssociationMapper extends BaseMapperX<MesEdhrManagedHandoffMaterialAssociationDO> {
    @Select("SELECT * FROM mes_edhr_managed_handoff_material_association WHERE tenant_id=#{tenant} AND release_transaction_id=#{release} AND manager_work_task_id=#{task}")
    List<MesEdhrManagedHandoffMaterialAssociationDO> selectByTask(@Param("tenant") Long tenant, @Param("release") Long release, @Param("task") Long task);
    @Select("SELECT * FROM mes_edhr_managed_handoff_material_association WHERE tenant_id=#{tenant} AND handoff_key=#{key}")
    List<MesEdhrManagedHandoffMaterialAssociationDO> selectByKey(@Param("tenant") Long tenant, @Param("key") String key);
}
