package cn.iocoder.yudao.module.srm.dal.mysql.supplier;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.srm.controller.admin.supplierrisk.vo.SrmSupplierRiskPageReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierRiskDO;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierRiskLevelEnum;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierRiskStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface SrmSupplierRiskMapper extends BaseMapperX<SrmSupplierRiskDO> {

    default PageResult<SrmSupplierRiskDO> selectPage(SrmSupplierRiskPageReqVO reqVO, Collection<Long> supplierIds) {
        if (supplierIds != null && supplierIds.isEmpty()) {
            return PageResult.empty();
        }
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmSupplierRiskDO>()
                .inIfPresent(SrmSupplierRiskDO::getSupplierId, supplierIds)
                .eqIfPresent(SrmSupplierRiskDO::getRiskLevel, reqVO.getRiskLevel())
                .eqIfPresent(SrmSupplierRiskDO::getRiskStatus, reqVO.getRiskStatus())
                .betweenIfPresent(SrmSupplierRiskDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SrmSupplierRiskDO::getId));
    }

    default List<SrmSupplierRiskDO> selectOpenHighRiskListBySupplierId(Long supplierId) {
        return selectList(new LambdaQueryWrapperX<SrmSupplierRiskDO>()
                .eq(SrmSupplierRiskDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(SrmSupplierRiskDO::getSupplierId, supplierId)
                .eq(SrmSupplierRiskDO::getRiskStatus, SrmSupplierRiskStatusEnum.OPEN.getStatus())
                .eq(SrmSupplierRiskDO::getRiskLevel, SrmSupplierRiskLevelEnum.HIGH.getLevel())
                .orderByDesc(SrmSupplierRiskDO::getId));
    }

    default List<SrmSupplierRiskDO> selectOpenHighRiskListBySupplierId(Long tenantId, Long supplierId) {
        return selectList(new LambdaQueryWrapperX<SrmSupplierRiskDO>()
                .eq(SrmSupplierRiskDO::getTenantId, tenantId)
                .eq(SrmSupplierRiskDO::getSupplierId, supplierId)
                .eq(SrmSupplierRiskDO::getRiskStatus, SrmSupplierRiskStatusEnum.OPEN.getStatus())
                .eq(SrmSupplierRiskDO::getRiskLevel, SrmSupplierRiskLevelEnum.HIGH.getLevel())
                .orderByDesc(SrmSupplierRiskDO::getId));
    }

    default Long selectOpenHighRiskCountBySupplierId(Long supplierId) {
        return selectCount(new LambdaQueryWrapperX<SrmSupplierRiskDO>()
                .eq(SrmSupplierRiskDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(SrmSupplierRiskDO::getSupplierId, supplierId)
                .eq(SrmSupplierRiskDO::getRiskStatus, SrmSupplierRiskStatusEnum.OPEN.getStatus())
                .eq(SrmSupplierRiskDO::getRiskLevel, SrmSupplierRiskLevelEnum.HIGH.getLevel()));
    }

    default Long selectOpenHighRiskCountBySupplierId(Long tenantId, Long supplierId) {
        return selectCount(new LambdaQueryWrapperX<SrmSupplierRiskDO>()
                .eq(SrmSupplierRiskDO::getTenantId, tenantId)
                .eq(SrmSupplierRiskDO::getSupplierId, supplierId)
                .eq(SrmSupplierRiskDO::getRiskStatus, SrmSupplierRiskStatusEnum.OPEN.getStatus())
                .eq(SrmSupplierRiskDO::getRiskLevel, SrmSupplierRiskLevelEnum.HIGH.getLevel()));
    }

    default List<SrmSupplierRiskDO> selectListBySupplierIds(Collection<Long> supplierIds) {
        if (supplierIds == null || supplierIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<SrmSupplierRiskDO>()
                .in(SrmSupplierRiskDO::getSupplierId, supplierIds));
    }
}
