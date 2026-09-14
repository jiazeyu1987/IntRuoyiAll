package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcItemEquipmentNumberConfigDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesPqcItemEquipmentNumberConfigMapper extends BaseMapperX<MesPqcItemEquipmentNumberConfigDO> {

    default List<MesPqcItemEquipmentNumberConfigDO> selectListByConfigIds(Collection<Long> configIds) {
        if (CollUtil.isEmpty(configIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesPqcItemEquipmentNumberConfigDO>()
                .in(MesPqcItemEquipmentNumberConfigDO::getConfigId, configIds)
                .orderByAsc(MesPqcItemEquipmentNumberConfigDO::getConfigId)
                .orderByAsc(MesPqcItemEquipmentNumberConfigDO::getSort)
                .orderByAsc(MesPqcItemEquipmentNumberConfigDO::getId));
    }

    default List<MesPqcItemEquipmentNumberConfigDO> selectEnabledListByConfigIds(Collection<Long> configIds) {
        if (CollUtil.isEmpty(configIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesPqcItemEquipmentNumberConfigDO>()
                .in(MesPqcItemEquipmentNumberConfigDO::getConfigId, configIds)
                .eq(MesPqcItemEquipmentNumberConfigDO::getEnabled, true)
                .orderByAsc(MesPqcItemEquipmentNumberConfigDO::getConfigId)
                .orderByAsc(MesPqcItemEquipmentNumberConfigDO::getSort)
                .orderByAsc(MesPqcItemEquipmentNumberConfigDO::getId));
    }

    default int physicalDeleteByTenantIdAndItemCode(Long tenantId, String itemCode) {
        return tenantId == null || itemCode == null ? 0 : doPhysicalDeleteByTenantIdAndItemCode(tenantId, itemCode);
    }

    @Delete("DELETE FROM mes_pqc_item_equipment_number_config WHERE tenant_id = #{tenantId} AND item_code = #{itemCode}")
    int doPhysicalDeleteByTenantIdAndItemCode(@Param("tenantId") Long tenantId, @Param("itemCode") String itemCode);
}
