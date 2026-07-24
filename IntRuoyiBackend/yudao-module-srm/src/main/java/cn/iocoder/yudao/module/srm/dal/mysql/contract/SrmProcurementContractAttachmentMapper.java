package cn.iocoder.yudao.module.srm.dal.mysql.contract;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.contract.SrmProcurementContractAttachmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmProcurementContractAttachmentMapper extends BaseMapperX<SrmProcurementContractAttachmentDO> {

    default List<SrmProcurementContractAttachmentDO> selectListByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<SrmProcurementContractAttachmentDO>()
                .eq(SrmProcurementContractAttachmentDO::getContractId, contractId)
                .orderByAsc(SrmProcurementContractAttachmentDO::getId));
    }
}
