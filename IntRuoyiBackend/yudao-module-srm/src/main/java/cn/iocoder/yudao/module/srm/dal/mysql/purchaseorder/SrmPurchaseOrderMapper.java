package cn.iocoder.yudao.module.srm.dal.mysql.purchaseorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.purchaseorder.SrmPurchaseOrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPurchaseOrderMapper extends BaseMapperX<SrmPurchaseOrderDO> {

    default SrmPurchaseOrderDO selectBySourcePlanIdAndSupplierId(Long tenantId, Long sourcePlanId, Long supplierId) {
        return selectOne(new LambdaQueryWrapperX<SrmPurchaseOrderDO>()
                .eq(SrmPurchaseOrderDO::getTenantId, tenantId)
                .eq(SrmPurchaseOrderDO::getSourcePlanId, sourcePlanId)
                .eq(SrmPurchaseOrderDO::getSupplierId, supplierId)
                .orderByDesc(SrmPurchaseOrderDO::getId)
                .last("LIMIT 1"));
    }

    default PageResult<SrmPurchaseOrderDO> selectPage(SrmPurchaseOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmPurchaseOrderDO>()
                .likeIfPresent(SrmPurchaseOrderDO::getOrderNo, reqVO.getOrderNo())
                .likeIfPresent(SrmPurchaseOrderDO::getSourcePlanNo, reqVO.getSourcePlanNo())
                .likeIfPresent(SrmPurchaseOrderDO::getSupplierName, reqVO.getSupplierName())
                .eqIfPresent(SrmPurchaseOrderDO::getOrderStatus, reqVO.getOrderStatus())
                .orderByDesc(SrmPurchaseOrderDO::getId));
    }

    default PageResult<SrmPurchaseOrderDO> selectMyPage(Long tenantId, Long supplierId, SrmPurchaseOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmPurchaseOrderDO>()
                .eq(SrmPurchaseOrderDO::getTenantId, tenantId)
                .eq(SrmPurchaseOrderDO::getSupplierId, supplierId)
                .likeIfPresent(SrmPurchaseOrderDO::getOrderNo, reqVO.getOrderNo())
                .likeIfPresent(SrmPurchaseOrderDO::getSourcePlanNo, reqVO.getSourcePlanNo())
                .eqIfPresent(SrmPurchaseOrderDO::getOrderStatus, reqVO.getOrderStatus())
                .orderByDesc(SrmPurchaseOrderDO::getId));
    }
}
