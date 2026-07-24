package cn.iocoder.yudao.module.srm.dal.mysql.outsourceexecution;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution.vo.SrmOutsourceExecutionPageReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.outsourceexecution.SrmOutsourceExecutionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmOutsourceExecutionMapper extends BaseMapperX<SrmOutsourceExecutionDO> {

    default SrmOutsourceExecutionDO selectBySourcePurchaseOrderId(Long tenantId, Long sourcePurchaseOrderId) {
        return selectOne(new LambdaQueryWrapperX<SrmOutsourceExecutionDO>()
                .eq(SrmOutsourceExecutionDO::getTenantId, tenantId)
                .eq(SrmOutsourceExecutionDO::getSourcePurchaseOrderId, sourcePurchaseOrderId)
                .orderByDesc(SrmOutsourceExecutionDO::getId)
                .last("LIMIT 1"));
    }

    default PageResult<SrmOutsourceExecutionDO> selectPage(SrmOutsourceExecutionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmOutsourceExecutionDO>()
                .likeIfPresent(SrmOutsourceExecutionDO::getExecutionNo, reqVO.getExecutionNo())
                .likeIfPresent(SrmOutsourceExecutionDO::getSourcePurchaseOrderNo, reqVO.getPurchaseOrderNo())
                .likeIfPresent(SrmOutsourceExecutionDO::getSupplierName, reqVO.getSupplierName())
                .eqIfPresent(SrmOutsourceExecutionDO::getExecutionStatus, reqVO.getExecutionStatus())
                .orderByDesc(SrmOutsourceExecutionDO::getId));
    }

    default PageResult<SrmOutsourceExecutionDO> selectMyPage(Long tenantId, Long supplierId, SrmOutsourceExecutionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmOutsourceExecutionDO>()
                .eq(SrmOutsourceExecutionDO::getTenantId, tenantId)
                .eq(SrmOutsourceExecutionDO::getSupplierId, supplierId)
                .likeIfPresent(SrmOutsourceExecutionDO::getExecutionNo, reqVO.getExecutionNo())
                .likeIfPresent(SrmOutsourceExecutionDO::getSourcePurchaseOrderNo, reqVO.getPurchaseOrderNo())
                .eqIfPresent(SrmOutsourceExecutionDO::getExecutionStatus, reqVO.getExecutionStatus())
                .orderByDesc(SrmOutsourceExecutionDO::getId));
    }
}
