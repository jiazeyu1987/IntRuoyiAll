package cn.iocoder.yudao.module.srm.dal.mysql.outsourceexecution;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.outsourceexecution.SrmReconciliationDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmReconciliationMapper extends BaseMapperX<SrmReconciliationDO> {

    default SrmReconciliationDO selectByExecutionId(Long executionId) {
        return selectOne(new LambdaQueryWrapperX<SrmReconciliationDO>()
                .eq(SrmReconciliationDO::getExecutionId, executionId)
                .last("LIMIT 1"));
    }
}
