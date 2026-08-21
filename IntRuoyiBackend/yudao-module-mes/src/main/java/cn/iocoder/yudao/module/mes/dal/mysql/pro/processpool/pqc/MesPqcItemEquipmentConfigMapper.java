package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcItemEquipmentConfigDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesPqcItemEquipmentConfigMapper extends BaseMapperX<MesPqcItemEquipmentConfigDO> {

    default List<MesPqcItemEquipmentConfigDO> selectListByItemCode(String itemCode) {
        return selectList(new LambdaQueryWrapperX<MesPqcItemEquipmentConfigDO>()
                .eq(MesPqcItemEquipmentConfigDO::getItemCode, itemCode)
                .orderByDesc(MesPqcItemEquipmentConfigDO::getDefaultFlag)
                .orderByAsc(MesPqcItemEquipmentConfigDO::getSort)
                .orderByAsc(MesPqcItemEquipmentConfigDO::getId));
    }

    default List<MesPqcItemEquipmentConfigDO> selectEnabledListByItemCodes(Collection<String> itemCodes) {
        if (CollUtil.isEmpty(itemCodes)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesPqcItemEquipmentConfigDO>()
                .in(MesPqcItemEquipmentConfigDO::getItemCode, itemCodes)
                .eq(MesPqcItemEquipmentConfigDO::getEnabled, true)
                .orderByAsc(MesPqcItemEquipmentConfigDO::getItemCode)
                .orderByDesc(MesPqcItemEquipmentConfigDO::getDefaultFlag)
                .orderByAsc(MesPqcItemEquipmentConfigDO::getSort)
                .orderByAsc(MesPqcItemEquipmentConfigDO::getId));
    }

    default int physicalDeleteByTenantIdAndItemCode(Long tenantId, String itemCode) {
        return tenantId == null || itemCode == null ? 0 : doPhysicalDeleteByTenantIdAndItemCode(tenantId, itemCode);
    }

    @Delete("DELETE FROM mes_pqc_item_equipment_config WHERE tenant_id = #{tenantId} AND item_code = #{itemCode}")
    int doPhysicalDeleteByTenantIdAndItemCode(@Param("tenantId") Long tenantId, @Param("itemCode") String itemCode);
}
