package cn.iocoder.yudao.module.srm.dal.mysql.supplier;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmErpSupplierDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface SrmErpSupplierMapper extends BaseMapperX<SrmErpSupplierDO> {

    default List<SrmErpSupplierDO> selectListByKeyword(String keyword) {
        return selectList(new LambdaQueryWrapperX<SrmErpSupplierDO>()
                .likeIfPresent(SrmErpSupplierDO::getName, keyword)
                .orderByAsc(SrmErpSupplierDO::getId));
    }

    default List<SrmErpSupplierDO> selectEnabledListByKeyword(String keyword) {
        return selectList(new LambdaQueryWrapperX<SrmErpSupplierDO>()
                .eq(SrmErpSupplierDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .likeIfPresent(SrmErpSupplierDO::getName, keyword)
                .orderByAsc(SrmErpSupplierDO::getId));
    }

    default List<SrmErpSupplierDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<SrmErpSupplierDO>()
                .in(SrmErpSupplierDO::getId, ids));
    }

    default SrmErpSupplierDO selectByTaxNo(Long tenantId, String taxNo) {
        return selectOne(new LambdaQueryWrapperX<SrmErpSupplierDO>()
                .eqIfPresent(SrmErpSupplierDO::getTenantId, tenantId)
                .eqIfPresent(SrmErpSupplierDO::getTaxNo, taxNo)
                .last("LIMIT 1"));
    }

    default SrmErpSupplierDO selectByExactName(Long tenantId, String name) {
        return selectOne(new LambdaQueryWrapperX<SrmErpSupplierDO>()
                .eqIfPresent(SrmErpSupplierDO::getTenantId, tenantId)
                .eqIfPresent(SrmErpSupplierDO::getName, name)
                .last("LIMIT 1"));
    }
}
