package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesProEdhrReleaseTransactionMapper extends BaseMapperX<MesProEdhrReleaseTransactionDO> {

    default MesProEdhrReleaseTransactionDO selectByBatchExecutionId(Long batchExecutionId) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrReleaseTransactionDO>()
                .eq(MesProEdhrReleaseTransactionDO::getBatchExecutionId, batchExecutionId)
                .orderByDesc(MesProEdhrReleaseTransactionDO::getId));
    }

    default List<MesProEdhrReleaseTransactionDO> selectListByBatchExecutionIds(Collection<Long> batchExecutionIds) {
        if (batchExecutionIds == null || batchExecutionIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProEdhrReleaseTransactionDO>()
                .in(MesProEdhrReleaseTransactionDO::getBatchExecutionId, batchExecutionIds)
                .orderByDesc(MesProEdhrReleaseTransactionDO::getId));
    }

    default MesProEdhrReleaseTransactionDO selectByIdForUpdate(Long id) {
        if (id == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProEdhrReleaseTransactionDO>()
                .eq(MesProEdhrReleaseTransactionDO::getId, id)
                .last("FOR UPDATE"));
    }
}
