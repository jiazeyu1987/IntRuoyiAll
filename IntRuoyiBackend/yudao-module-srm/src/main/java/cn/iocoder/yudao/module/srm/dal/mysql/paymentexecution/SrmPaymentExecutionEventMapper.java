package cn.iocoder.yudao.module.srm.dal.mysql.paymentexecution;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.paymentexecution.SrmPaymentExecutionEventDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmPaymentExecutionEventMapper extends BaseMapperX<SrmPaymentExecutionEventDO> {

    default List<SrmPaymentExecutionEventDO> selectListByPaymentId(Long paymentId) {
        return selectList(new LambdaQueryWrapperX<SrmPaymentExecutionEventDO>()
                .eq(SrmPaymentExecutionEventDO::getPaymentId, paymentId)
                .orderByAsc(SrmPaymentExecutionEventDO::getId));
    }
}
