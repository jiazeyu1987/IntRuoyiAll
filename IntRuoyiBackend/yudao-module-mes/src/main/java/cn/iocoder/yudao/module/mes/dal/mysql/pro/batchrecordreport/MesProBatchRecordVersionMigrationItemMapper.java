package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionMigrationItemDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MesProBatchRecordVersionMigrationItemMapper
        extends BaseMapperX<MesProBatchRecordVersionMigrationItemDO> {

    default List<MesProBatchRecordVersionMigrationItemDO> selectListByVersionId(Long versionId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordVersionMigrationItemDO>()
                .eq(MesProBatchRecordVersionMigrationItemDO::getVersionId, versionId)
                .orderByAsc(MesProBatchRecordVersionMigrationItemDO::getId));
    }

    default Long countBlockingItems(Long versionId) {
        return selectListByVersionId(versionId).stream()
                .filter(item -> "BLOCKER".equals(item.getRiskLevel())
                        || ("CONFIRM_REQUIRED".equals(item.getRiskLevel()) && !Boolean.TRUE.equals(item.getConfirmed())))
                .count();
    }

    default Long countUnconfirmedConfirmRequiredItems(Long versionId) {
        return selectListByVersionId(versionId).stream()
                .filter(item -> "CONFIRM_REQUIRED".equals(item.getRiskLevel()))
                .filter(item -> !Boolean.TRUE.equals(item.getConfirmed()))
                .count();
    }

    default Long countByVersionIdAndRiskLevel(Long versionId, String riskLevel) {
        return selectCount(new LambdaQueryWrapperX<MesProBatchRecordVersionMigrationItemDO>()
                .eq(MesProBatchRecordVersionMigrationItemDO::getVersionId, versionId)
                .eq(MesProBatchRecordVersionMigrationItemDO::getRiskLevel, riskLevel));
    }

    default Long countConfirmedByVersionId(Long versionId) {
        return selectCount(new LambdaQueryWrapperX<MesProBatchRecordVersionMigrationItemDO>()
                .eq(MesProBatchRecordVersionMigrationItemDO::getVersionId, versionId)
                .eq(MesProBatchRecordVersionMigrationItemDO::getConfirmed, true));
    }

    default boolean existsCellRuleReconciledEvidence(Long versionId) {
        if (versionId == null) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<MesProBatchRecordVersionMigrationItemDO>()
                .eq(MesProBatchRecordVersionMigrationItemDO::getVersionId, versionId)
                .eq(MesProBatchRecordVersionMigrationItemDO::getItemType, "CELL_RULE")
                .eq(MesProBatchRecordVersionMigrationItemDO::getDiffGroup, "CELL_RULE")
                .eq(MesProBatchRecordVersionMigrationItemDO::getDiffType, "CELL_RULE_RECONCILED")
                .eq(MesProBatchRecordVersionMigrationItemDO::getRiskLevel, "INFO")
                .eq(MesProBatchRecordVersionMigrationItemDO::getRuleType, "CELL_RULE")) > 0;
    }

    default MesProBatchRecordVersionMigrationItemDO selectByVersionIdAndConfirmIdempotencyKey(
            Long versionId, String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordVersionMigrationItemDO>()
                .eq(MesProBatchRecordVersionMigrationItemDO::getVersionId, versionId)
                .eq(MesProBatchRecordVersionMigrationItemDO::getConfirmIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
    }

    @Delete("DELETE FROM mes_pro_batch_record_version_migration_item WHERE definition_id = #{definitionId}")
    int deleteHardByDefinitionId(@Param("definitionId") Long definitionId);
}
