package cn.iocoder.yudao.module.srm.dal.mysql.contract;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.contract.SrmProcurementContractSigningDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmProcurementContractSigningMapper extends BaseMapperX<SrmProcurementContractSigningDO> {

    default List<SrmProcurementContractSigningDO> selectListByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<SrmProcurementContractSigningDO>()
                .eq(SrmProcurementContractSigningDO::getContractId, contractId)
                .orderByAsc(SrmProcurementContractSigningDO::getId));
    }
}
