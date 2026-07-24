package cn.iocoder.yudao.module.srm.dal.mysql.procurement;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.SrmProcurementApprovalRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmProcurementApprovalRecordMapper extends BaseMapperX<SrmProcurementApprovalRecordDO> {

    default List<SrmProcurementApprovalRecordDO> selectListByBiz(String bizType, Long bizId) {
        return selectList(new LambdaQueryWrapperX<SrmProcurementApprovalRecordDO>()
                .eq(SrmProcurementApprovalRecordDO::getBizType, bizType)
                .eq(SrmProcurementApprovalRecordDO::getBizId, bizId)
                .orderByAsc(SrmProcurementApprovalRecordDO::getId));
    }
}
