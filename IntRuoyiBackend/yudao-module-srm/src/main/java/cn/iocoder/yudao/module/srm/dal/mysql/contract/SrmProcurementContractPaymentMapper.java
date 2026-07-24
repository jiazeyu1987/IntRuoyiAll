package cn.iocoder.yudao.module.srm.dal.mysql.contract;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.contract.SrmProcurementContractPaymentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmProcurementContractPaymentMapper extends BaseMapperX<SrmProcurementContractPaymentDO> {

    default List<SrmProcurementContractPaymentDO> selectListByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<SrmProcurementContractPaymentDO>()
                .eq(SrmProcurementContractPaymentDO::getContractId, contractId)
                .orderByAsc(SrmProcurementContractPaymentDO::getId));
    }
}
