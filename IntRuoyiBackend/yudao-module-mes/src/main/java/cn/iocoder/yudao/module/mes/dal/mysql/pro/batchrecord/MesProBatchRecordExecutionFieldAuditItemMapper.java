package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.projection.MesProBatchRecordExecutionFieldResponsibilityAuditProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProBatchRecordExecutionFieldAuditItemMapper
        extends BaseMapperX<MesProBatchRecordExecutionFieldAuditItemDO> {

    default List<MesProBatchRecordExecutionFieldAuditItemDO> selectListByBatchId(Long auditBatchId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditItemDO>()
                .eq(MesProBatchRecordExecutionFieldAuditItemDO::getAuditBatchId, auditBatchId)
                .orderByAsc(MesProBatchRecordExecutionFieldAuditItemDO::getBatchItemIndex)
                .orderByAsc(MesProBatchRecordExecutionFieldAuditItemDO::getId));
    }

    default List<MesProBatchRecordExecutionFieldAuditItemDO> selectSummaryListByBatchIds(
            Collection<Long> auditBatchIds, int limit) {
        if (auditBatchIds == null || auditBatchIds.isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }
        return selectSummaryListByBatchIdsInternal(auditBatchIds, limit);
    }

    @Select({
            "<script>",
            "SELECT * FROM (",
            "  SELECT item.*,",
            "         ROW_NUMBER() OVER (PARTITION BY item.audit_batch_id",
            "                            ORDER BY item.batch_item_index ASC, item.id ASC) AS row_number_in_batch",
            "  FROM mes_pro_batch_record_execution_field_audit_item item",
            "  WHERE item.deleted = 0",
            "    AND item.audit_batch_id IN",
            "    <foreach collection='auditBatchIds' item='auditBatchId' open='(' separator=',' close=')'>",
            "      #{auditBatchId}",
            "    </foreach>",
            ") ranked_items",
            "WHERE ranked_items.row_number_in_batch &lt;= #{limit}",
            "ORDER BY ranked_items.audit_batch_id ASC, ranked_items.batch_item_index ASC, ranked_items.id ASC",
            "</script>"
    })
    List<MesProBatchRecordExecutionFieldAuditItemDO> selectSummaryListByBatchIdsInternal(
            @Param("auditBatchIds") Collection<Long> auditBatchIds,
            @Param("limit") int limit);

    default List<MesProBatchRecordExecutionFieldAuditItemDO> selectListByExecutionId(Long executionId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditItemDO>()
                .eq(MesProBatchRecordExecutionFieldAuditItemDO::getExecutionId, executionId)
                .orderByAsc(MesProBatchRecordExecutionFieldAuditItemDO::getFieldAuditRevision)
                .orderByAsc(MesProBatchRecordExecutionFieldAuditItemDO::getId));
    }

    default List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection> selectResponsibilityProjectionList(
            Long executionId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditItemDO>()
                .eq(MesProBatchRecordExecutionFieldAuditItemDO::getExecutionId, executionId)
                .orderByAsc(MesProBatchRecordExecutionFieldAuditItemDO::getFieldAuditRevision)
                .orderByAsc(MesProBatchRecordExecutionFieldAuditItemDO::getId))
                .stream()
                .map(MesProBatchRecordExecutionFieldResponsibilityAuditProjection::from)
                .toList();
    }

    default List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection>
    selectResponsibilityHistoryProjectionPage(Long executionId,
                                              String fieldPath,
                                              String fieldKey,
                                              Integer rowIndex,
                                              Integer columnIndex,
                                              Long cursorFieldAuditRevision,
                                              Long cursorAuditItemId,
                                              int limit) {
        LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditItemDO> query =
                new LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditItemDO>()
                        .eq(MesProBatchRecordExecutionFieldAuditItemDO::getExecutionId, executionId)
                        .eq(MesProBatchRecordExecutionFieldAuditItemDO::getFieldPath, fieldPath)
                        .eq(MesProBatchRecordExecutionFieldAuditItemDO::getFieldKey, fieldKey)
                        .eq(MesProBatchRecordExecutionFieldAuditItemDO::getRowIndex, rowIndex)
                        .eq(MesProBatchRecordExecutionFieldAuditItemDO::getColumnIndex, columnIndex);
        if (cursorFieldAuditRevision != null && cursorAuditItemId != null) {
            query.and(cursor -> cursor
                    .lt(MesProBatchRecordExecutionFieldAuditItemDO::getFieldAuditRevision,
                            cursorFieldAuditRevision)
                    .or(sameRevision -> sameRevision
                            .eq(MesProBatchRecordExecutionFieldAuditItemDO::getFieldAuditRevision,
                                    cursorFieldAuditRevision)
                            .lt(MesProBatchRecordExecutionFieldAuditItemDO::getId, cursorAuditItemId)));
        }
        return selectList(query
                .orderByDesc(MesProBatchRecordExecutionFieldAuditItemDO::getFieldAuditRevision)
                .orderByDesc(MesProBatchRecordExecutionFieldAuditItemDO::getId)
                .last("LIMIT " + limit))
                .stream()
                .map(MesProBatchRecordExecutionFieldResponsibilityAuditProjection::from)
                .toList();
    }
}
