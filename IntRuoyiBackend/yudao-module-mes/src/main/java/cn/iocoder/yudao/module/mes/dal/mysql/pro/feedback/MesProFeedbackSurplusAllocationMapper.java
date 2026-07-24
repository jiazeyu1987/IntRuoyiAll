package cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackSurplusAllocationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProFeedbackSurplusAllocationMapper extends BaseMapperX<MesProFeedbackSurplusAllocationDO> {

    default List<MesProFeedbackSurplusAllocationDO> selectListByImportRecordId(Long importRecordId) {
        if (importRecordId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackSurplusAllocationDO>()
                .eq(MesProFeedbackSurplusAllocationDO::getImportRecordId, importRecordId)
                .orderByAsc(MesProFeedbackSurplusAllocationDO::getId));
    }

    default List<MesProFeedbackSurplusAllocationDO> selectListByPoolIds(Collection<Long> poolIds) {
        if (poolIds == null || poolIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackSurplusAllocationDO>()
                .in(MesProFeedbackSurplusAllocationDO::getPoolId, poolIds)
                .orderByAsc(MesProFeedbackSurplusAllocationDO::getId));
    }
}
