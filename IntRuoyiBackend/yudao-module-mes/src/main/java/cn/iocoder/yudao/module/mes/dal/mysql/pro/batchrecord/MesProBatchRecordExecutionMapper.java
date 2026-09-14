package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTracePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionTrackingPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProBatchRecordExecutionMapper extends BaseMapperX<MesProBatchRecordExecutionDO> {

    @Update("UPDATE mes_pro_batch_record_execution SET active_context_key = NULL WHERE id = #{id}")
    void clearActiveContextKey(@Param("id") Long id);

    @Select("SELECT * FROM mes_pro_batch_record_execution WHERE id = #{id} FOR UPDATE")
    MesProBatchRecordExecutionDO selectByIdForUpdate(@Param("id") Long id);

    @Update("UPDATE mes_pro_batch_record_execution "
            + "SET cell_values_json = #{cellValuesJson}, "
            + "cell_values_hash = #{afterCellValuesHash}, "
            + "field_audit_revision = #{afterFieldAuditRevision}, "
            + "field_audit_head_hash = #{newHeadHash}, "
            + "field_audit_last_batch_id = #{auditBatchId}, "
            + "update_time = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} "
            + "AND cell_values_hash = #{beforeCellValuesHash} "
            + "AND field_audit_revision = #{beforeFieldAuditRevision} "
            + "AND field_audit_head_hash = #{previousHeadHash}")
    int updateFieldAuditProjection(@Param("id") Long id,
                                   @Param("cellValuesJson") String cellValuesJson,
                                   @Param("beforeCellValuesHash") String beforeCellValuesHash,
                                   @Param("afterCellValuesHash") String afterCellValuesHash,
                                   @Param("beforeFieldAuditRevision") Long beforeFieldAuditRevision,
                                   @Param("afterFieldAuditRevision") Long afterFieldAuditRevision,
                                   @Param("previousHeadHash") String previousHeadHash,
                                   @Param("newHeadHash") String newHeadHash,
                                   @Param("auditBatchId") Long auditBatchId);

    default PageResult<MesProBatchRecordExecutionDO> selectPage(MesProBatchRecordExecutionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProBatchRecordExecutionDO>()
                .eqIfPresent(MesProBatchRecordExecutionDO::getTemplateId, reqVO.getTemplateId())
                .eqIfPresent(MesProBatchRecordExecutionDO::getWorkOrderId, reqVO.getWorkOrderId())
                .eqIfPresent(MesProBatchRecordExecutionDO::getBatchExecutionId, reqVO.getBatchExecutionId())
                .eqIfPresent(MesProBatchRecordExecutionDO::getInstanceScope, reqVO.getInstanceScope())
                .eqIfPresent(MesProBatchRecordExecutionDO::getSharedFormKey, reqVO.getSharedFormKey())
                .eqIfPresent(MesProBatchRecordExecutionDO::getRouteProcessId, reqVO.getRouteProcessId())
                .eqIfPresent(MesProBatchRecordExecutionDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(MesProBatchRecordExecutionDO::getWorkstationId, reqVO.getWorkstationId())
                .eqIfPresent(MesProBatchRecordExecutionDO::getBatchRecordReportId, reqVO.getBatchRecordReportId())
                .likeIfPresent(MesProBatchRecordExecutionDO::getBatchCode, reqVO.getBatchCode())
                .eqIfPresent(MesProBatchRecordExecutionDO::getStatus, reqVO.getStatus())
                .orderByDesc(MesProBatchRecordExecutionDO::getId));
    }

    default PageResult<MesProBatchRecordExecutionDO> selectTrackingPage(MesProBatchRecordExecutionTrackingPageReqVO reqVO) {
        return selectTrackingPage(reqVO, null, null);
    }

    default PageResult<MesProBatchRecordExecutionDO> selectDomainTracePage(MesProBatchRecordDomainTracePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProBatchRecordExecutionDO>()
                .eqIfPresent(MesProBatchRecordExecutionDO::getId, reqVO.getExecutionId())
                .likeIfPresent(MesProBatchRecordExecutionDO::getExecutionCode, reqVO.getExecutionCode())
                .likeIfPresent(MesProBatchRecordExecutionDO::getWorkOrderCode, reqVO.getWorkOrderCode())
                .likeIfPresent(MesProBatchRecordExecutionDO::getBatchCode, reqVO.getBatchCode())
                .eqIfPresent(MesProBatchRecordExecutionDO::getDomainTraceStatus, reqVO.getStatus())
                .ne(MesProBatchRecordExecutionDO::getStatus, 4)
                .orderByDesc(MesProBatchRecordExecutionDO::getDomainTraceVerifiedAt)
                .orderByDesc(MesProBatchRecordExecutionDO::getId));
    }

    default List<Long> selectIdsByExecutionCode(String executionCode) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionDO>()
                        .likeIfPresent(MesProBatchRecordExecutionDO::getExecutionCode, executionCode))
                .stream()
                .map(MesProBatchRecordExecutionDO::getId)
                .toList();
    }

    default PageResult<MesProBatchRecordExecutionDO> selectTrackingPage(MesProBatchRecordExecutionTrackingPageReqVO reqVO,
                                                                        Collection<Long> routeProcessIds,
                                                                        Collection<Long> actorMatchedExecutionIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProBatchRecordExecutionDO>()
                .likeIfPresent(MesProBatchRecordExecutionDO::getExecutionCode, reqVO.getExecutionCode())
                .likeIfPresent(MesProBatchRecordExecutionDO::getWorkOrderCode, reqVO.getWorkOrderCode())
                .likeIfPresent(MesProBatchRecordExecutionDO::getBatchCode, reqVO.getBatchCode())
                .eqIfPresent(MesProBatchRecordExecutionDO::getProcessInstanceId, reqVO.getProcessInstanceId())
                .inIfPresent(MesProBatchRecordExecutionDO::getRouteProcessId, routeProcessIds)
                .inIfPresent(MesProBatchRecordExecutionDO::getId, actorMatchedExecutionIds)
                .eqIfPresent(MesProBatchRecordExecutionDO::getWorkstationId, reqVO.getWorkstationId())
                .eqIfPresent(MesProBatchRecordExecutionDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MesProBatchRecordExecutionDO::getSubmittedBy, reqVO.getSubmittedBy())
                .eqIfPresent(MesProBatchRecordExecutionDO::getApprovedBy, reqVO.getApprovedBy())
                .betweenIfPresent(MesProBatchRecordExecutionDO::getUpdateTime,
                        reqVO.getOccurredAtStart() == null ? reqVO.getBeginTime() : reqVO.getOccurredAtStart(),
                        reqVO.getOccurredAtEnd() == null ? reqVO.getEndTime() : reqVO.getOccurredAtEnd())
                .orderByDesc(MesProBatchRecordExecutionDO::getUpdateTime)
                .orderByDesc(MesProBatchRecordExecutionDO::getId));
    }

    default MesProBatchRecordExecutionDO selectActiveByContext(Long workOrderId, Long routeProcessId,
                                                               String batchRecordReportId,
                                                               String batchCode,
                                                               Collection<Integer> activeStatuses) {
        return selectActiveByContext(null, null, null, workOrderId, routeProcessId, batchRecordReportId,
                batchCode, activeStatuses);
    }

    default MesProBatchRecordExecutionDO selectActiveByContext(Long batchExecutionId, Long taskId,
                                                               Long workstationId,
                                                               Long workOrderId, Long routeProcessId,
                                                               String batchRecordReportId,
                                                               String batchCode,
                                                               Collection<Integer> activeStatuses) {
        LambdaQueryWrapperX<MesProBatchRecordExecutionDO> query = new LambdaQueryWrapperX<>();
        query.eqIfPresent(MesProBatchRecordExecutionDO::getBatchExecutionId, batchExecutionId);
        query.eq(MesProBatchRecordExecutionDO::getWorkOrderId, workOrderId);
        query.eq(MesProBatchRecordExecutionDO::getRouteProcessId, routeProcessId);
        query.eq(MesProBatchRecordExecutionDO::getBatchRecordReportId, batchRecordReportId);
        query.eq(MesProBatchRecordExecutionDO::getBatchCode, batchCode);
        query.in(MesProBatchRecordExecutionDO::getStatus, activeStatuses);
        if (workstationId == null) {
            query.isNull(MesProBatchRecordExecutionDO::getWorkstationId);
        } else {
            query.eq(MesProBatchRecordExecutionDO::getWorkstationId, workstationId);
        }
        if (taskId == null) {
            query.isNull(MesProBatchRecordExecutionDO::getTaskId);
        } else {
            query.eq(MesProBatchRecordExecutionDO::getTaskId, taskId);
        }
        query.orderByDesc(MesProBatchRecordExecutionDO::getId);
        return selectOne(query);
    }

    default MesProBatchRecordExecutionDO selectActiveByBatchShared(Long batchExecutionId,
                                                                       String sharedFormKey,
                                                                       String batchCode,
                                                                       Collection<Integer> activeStatuses) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordExecutionDO>()
                .eq(MesProBatchRecordExecutionDO::getBatchExecutionId, batchExecutionId)
                .eq(MesProBatchRecordExecutionDO::getInstanceScope, "BATCH_SHARED")
                .eq(MesProBatchRecordExecutionDO::getSharedFormKey, sharedFormKey)
                .eq(MesProBatchRecordExecutionDO::getBatchCode, batchCode)
                .in(MesProBatchRecordExecutionDO::getStatus, activeStatuses)
                .isNull(MesProBatchRecordExecutionDO::getTaskId)
                .isNull(MesProBatchRecordExecutionDO::getWorkstationId)
                .orderByDesc(MesProBatchRecordExecutionDO::getId));
    }

    default MesProBatchRecordExecutionDO selectLatestByWorkOrderVersionBatchAndReport(Long workOrderId,
                                                                                      Long batchRecordVersionId,
                                                                                      String batchCode,
                                                                                      String batchRecordReportId,
                                                                                      Collection<Integer> activeStatuses) {
        LambdaQueryWrapperX<MesProBatchRecordExecutionDO> query = new LambdaQueryWrapperX<MesProBatchRecordExecutionDO>()
                .eq(MesProBatchRecordExecutionDO::getWorkOrderId, workOrderId)
                .eq(MesProBatchRecordExecutionDO::getBatchCode, batchCode)
                .eq(MesProBatchRecordExecutionDO::getBatchRecordReportId, batchRecordReportId)
                .inIfPresent(MesProBatchRecordExecutionDO::getStatus, activeStatuses)
                .orderByDesc(MesProBatchRecordExecutionDO::getUpdateTime)
                .orderByDesc(MesProBatchRecordExecutionDO::getId);
        if (batchRecordVersionId != null) {
            query.eq(MesProBatchRecordExecutionDO::getBatchRecordVersionId, batchRecordVersionId);
        }
        return selectOne(query);
    }

    default List<MesProBatchRecordExecutionDO> selectListByTaskIds(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionDO>()
                .in(MesProBatchRecordExecutionDO::getTaskId, taskIds));
    }

    default List<MesProBatchRecordExecutionDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionDO>()
                .in(MesProBatchRecordExecutionDO::getId, ids));
    }

    default Long countByBatchRecordVersionId(Long batchRecordVersionId) {
        return selectCount(MesProBatchRecordExecutionDO::getBatchRecordVersionId, batchRecordVersionId);
    }

    default Long countByBatchRecordDefinitionIdAndNotVersionId(Long batchRecordDefinitionId, Long batchRecordVersionId) {
        return selectCount(new LambdaQueryWrapperX<MesProBatchRecordExecutionDO>()
                .eq(MesProBatchRecordExecutionDO::getBatchRecordDefinitionId, batchRecordDefinitionId)
                .ne(MesProBatchRecordExecutionDO::getBatchRecordVersionId, batchRecordVersionId));
    }

    default List<MesProBatchRecordExecutionDO> selectListByBatchRecordVersionId(Long batchRecordVersionId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionDO>()
                .eq(MesProBatchRecordExecutionDO::getBatchRecordVersionId, batchRecordVersionId)
                .orderByDesc(MesProBatchRecordExecutionDO::getId));
    }
}
