package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTraceLinkDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrBatchExecutionTraceLinkMapper extends BaseMapperX<MesProEdhrBatchExecutionTraceLinkDO> {

    default List<MesProEdhrBatchExecutionTraceLinkDO> selectListByBatchExecutionId(Long batchExecutionId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionTraceLinkDO>()
                .eq(MesProEdhrBatchExecutionTraceLinkDO::getBatchExecutionId, batchExecutionId)
                .orderByAsc(MesProEdhrBatchExecutionTraceLinkDO::getId));
    }

    default MesProEdhrBatchExecutionTraceLinkDO selectByIdAndBatchExecutionId(Long id, Long batchExecutionId) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrBatchExecutionTraceLinkDO>()
                .eq(MesProEdhrBatchExecutionTraceLinkDO::getId, id)
                .eq(MesProEdhrBatchExecutionTraceLinkDO::getBatchExecutionId, batchExecutionId));
    }

    default MesProEdhrBatchExecutionTraceLinkDO selectByIdentity(Long batchExecutionId, String linkType,
                                                                  String sourceObjectType, Long sourceObjectId,
                                                                  Long sourceLineId, Long sourceEventId) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrBatchExecutionTraceLinkDO>()
                .eq(MesProEdhrBatchExecutionTraceLinkDO::getBatchExecutionId, batchExecutionId)
                .eq(MesProEdhrBatchExecutionTraceLinkDO::getLinkType, linkType)
                .eq(MesProEdhrBatchExecutionTraceLinkDO::getSourceObjectType, sourceObjectType)
                .eqIfPresent(MesProEdhrBatchExecutionTraceLinkDO::getSourceObjectId, sourceObjectId)
                .eqIfPresent(MesProEdhrBatchExecutionTraceLinkDO::getSourceLineId, sourceLineId)
                .eqIfPresent(MesProEdhrBatchExecutionTraceLinkDO::getSourceEventId, sourceEventId));
    }

    default MesProEdhrBatchExecutionTraceLinkDO selectByIdentityKey(Long batchExecutionId, String sourceIdentityKey) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrBatchExecutionTraceLinkDO>()
                .eq(MesProEdhrBatchExecutionTraceLinkDO::getBatchExecutionId, batchExecutionId)
                .eq(MesProEdhrBatchExecutionTraceLinkDO::getSourceIdentityKey, sourceIdentityKey));
    }

    default List<Long> selectBatchExecutionIdsByReleaseApplicationId(Long releaseApplicationId) {
        if (releaseApplicationId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionTraceLinkDO>()
                .eq(MesProEdhrBatchExecutionTraceLinkDO::getLinkType, "RELEASE_DECISION")
                .eq(MesProEdhrBatchExecutionTraceLinkDO::getSourceObjectType, "RELEASE_APPLICATION")
                .eq(MesProEdhrBatchExecutionTraceLinkDO::getSourceObjectId, releaseApplicationId)
                .orderByAsc(MesProEdhrBatchExecutionTraceLinkDO::getBatchExecutionId))
                .stream().map(MesProEdhrBatchExecutionTraceLinkDO::getBatchExecutionId).distinct().toList();
    }
}
