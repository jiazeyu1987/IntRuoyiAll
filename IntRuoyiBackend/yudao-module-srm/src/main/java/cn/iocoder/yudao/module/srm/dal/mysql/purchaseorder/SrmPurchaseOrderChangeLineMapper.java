package cn.iocoder.yudao.module.srm.dal.mysql.purchaseorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.purchaseorder.SrmPurchaseOrderChangeLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmPurchaseOrderChangeLineMapper extends BaseMapperX<SrmPurchaseOrderChangeLineDO> {

    default List<SrmPurchaseOrderChangeLineDO> selectListByChangeId(Long changeId) {
        return selectList(new LambdaQueryWrapperX<SrmPurchaseOrderChangeLineDO>()
                .eq(SrmPurchaseOrderChangeLineDO::getChangeId, changeId)
                .orderByAsc(SrmPurchaseOrderChangeLineDO::getId));
    }
}
