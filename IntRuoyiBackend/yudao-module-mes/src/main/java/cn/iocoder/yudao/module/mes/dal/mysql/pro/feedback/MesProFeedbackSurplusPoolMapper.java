package cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackSurplusPoolDO;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProFeedbackSurplusPoolMapper extends BaseMapperX<MesProFeedbackSurplusPoolDO> {

    default List<MesProFeedbackSurplusPoolDO> selectAvailableListByProcessId(Long processId) {
        return selectList(new LambdaQueryWrapperX<MesProFeedbackSurplusPoolDO>()
                .eq(MesProFeedbackSurplusPoolDO::getProcessId, processId)
                .eq(MesProFeedbackSurplusPoolDO::getStatus, MesProFeedbackSurplusPoolDO.STATUS_AVAILABLE)
                .gt(MesProFeedbackSurplusPoolDO::getAvailableQuantity, BigDecimal.ZERO)
                .orderByAsc(MesProFeedbackSurplusPoolDO::getId));
    }

    default BigDecimal sumAvailableQuantityByProcessId(Long processId) {
        return selectAvailableListByProcessId(processId).stream()
                .map(MesProFeedbackSurplusPoolDO::getAvailableQuantity)
                .filter(quantity -> quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    default List<MesProFeedbackSurplusPoolDO> selectListBySourceImportRecordId(Long sourceImportRecordId) {
        if (sourceImportRecordId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackSurplusPoolDO>()
                .eq(MesProFeedbackSurplusPoolDO::getSourceImportRecordId, sourceImportRecordId)
                .orderByAsc(MesProFeedbackSurplusPoolDO::getId));
    }

    default List<MesProFeedbackSurplusPoolDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackSurplusPoolDO>()
                .in(MesProFeedbackSurplusPoolDO::getId, ids)
                .orderByAsc(MesProFeedbackSurplusPoolDO::getId));
    }
}
