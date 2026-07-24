package cn.iocoder.yudao.module.srm.dal.mysql.paymentexecution;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.controller.admin.paymentexecution.vo.SrmPaymentExecutionPageReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.paymentexecution.SrmPaymentExecutionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPaymentExecutionMapper extends BaseMapperX<SrmPaymentExecutionDO> {

    default SrmPaymentExecutionDO selectByReconciliationId(Long tenantId, Long reconciliationId) {
        return selectOne(new LambdaQueryWrapperX<SrmPaymentExecutionDO>()
                .eq(SrmPaymentExecutionDO::getTenantId, tenantId)
                .eq(SrmPaymentExecutionDO::getReconciliationId, reconciliationId)
                .orderByDesc(SrmPaymentExecutionDO::getId)
                .last("LIMIT 1"));
    }

    default PageResult<SrmPaymentExecutionDO> selectPage(SrmPaymentExecutionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmPaymentExecutionDO>()
                .likeIfPresent(SrmPaymentExecutionDO::getPaymentNo, reqVO.getPaymentNo())
                .likeIfPresent(SrmPaymentExecutionDO::getReconciliationNo, reqVO.getReconciliationNo())
                .likeIfPresent(SrmPaymentExecutionDO::getSupplierName, reqVO.getSupplierName())
                .eqIfPresent(SrmPaymentExecutionDO::getPaymentStatus, reqVO.getPaymentStatus())
                .orderByDesc(SrmPaymentExecutionDO::getId));
    }
}
