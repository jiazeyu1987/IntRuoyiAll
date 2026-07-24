package cn.iocoder.yudao.module.srm.dal.mysql.supplier;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo.SrmSupplierAccessPageReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierAccessDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface SrmSupplierAccessMapper extends BaseMapperX<SrmSupplierAccessDO> {

    @Delete("DELETE FROM srm_supplier_access WHERE id = #{id} AND tenant_id = #{tenantId}")
    int deleteByIdForce(@Param("id") Long id, @Param("tenantId") Long tenantId);

    default PageResult<SrmSupplierAccessDO> selectPage(SrmSupplierAccessPageReqVO reqVO, Collection<Long> supplierIds) {
        if (supplierIds != null && supplierIds.isEmpty()) {
            return PageResult.empty();
        }
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmSupplierAccessDO>()
                .inIfPresent(SrmSupplierAccessDO::getSupplierId, supplierIds)
                .eqIfPresent(SrmSupplierAccessDO::getAccessStatus, reqVO.getAccessStatus())
                .eqIfPresent(SrmSupplierAccessDO::getEnabled, reqVO.getEnabled())
                .betweenIfPresent(SrmSupplierAccessDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SrmSupplierAccessDO::getId));
    }

    default SrmSupplierAccessDO selectBySupplierId(Long supplierId) {
        return selectOne(new LambdaQueryWrapperX<SrmSupplierAccessDO>()
                .eq(SrmSupplierAccessDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(SrmSupplierAccessDO::getSupplierId, supplierId)
                .orderByDesc(SrmSupplierAccessDO::getId)
                .last("LIMIT 1"));
    }

    default SrmSupplierAccessDO selectBySupplierId(Long tenantId, Long supplierId) {
        return selectOne(new LambdaQueryWrapperX<SrmSupplierAccessDO>()
                .eq(SrmSupplierAccessDO::getTenantId, tenantId)
                .eq(SrmSupplierAccessDO::getSupplierId, supplierId)
                .orderByDesc(SrmSupplierAccessDO::getId)
                .last("LIMIT 1"));
    }

    default List<SrmSupplierAccessDO> selectListBySupplierIds(Collection<Long> supplierIds) {
        if (supplierIds == null || supplierIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<SrmSupplierAccessDO>()
                .in(SrmSupplierAccessDO::getSupplierId, supplierIds));
    }
}
