package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProBatchRecordVersionMapper extends BaseMapperX<MesProBatchRecordVersionDO> {

    default MesProBatchRecordVersionDO selectByDefinitionIdAndVersionNo(Long definitionId, String versionNo) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordVersionDO>()
                .eq(MesProBatchRecordVersionDO::getDefinitionId, definitionId)
                .eq(MesProBatchRecordVersionDO::getVersionNo, versionNo));
    }

    default MesProBatchRecordVersionDO selectReusablePendingByHash(Long definitionId, String sourceFileSha256) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordVersionDO>()
                .eq(MesProBatchRecordVersionDO::getDefinitionId, definitionId)
                .eq(MesProBatchRecordVersionDO::getSourceFileSha256, sourceFileSha256)
                .in(MesProBatchRecordVersionDO::getStatus, List.of("DRAFT", "PRECHECK_PASSED", "PENDING_APPROVAL"))
                .orderByDesc(MesProBatchRecordVersionDO::getId));
    }

    default List<MesProBatchRecordVersionDO> selectListByDefinitionId(Long definitionId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordVersionDO>()
                .eq(MesProBatchRecordVersionDO::getDefinitionId, definitionId)
                .orderByDesc(MesProBatchRecordVersionDO::getId));
    }

    default MesProBatchRecordVersionDO selectLatestApprovedByDefinitionId(Long definitionId) {
        if (definitionId == null) {
            return null;
        }
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordVersionDO>()
                .eq(MesProBatchRecordVersionDO::getDefinitionId, definitionId)
                .eq(MesProBatchRecordVersionDO::getStatus, "APPROVED")
                .orderByDesc(MesProBatchRecordVersionDO::getId))
                .stream()
                .findFirst()
                .orElse(null);
    }

    default List<MesProBatchRecordVersionDO> selectListByRouteIds(Collection<Long> routeIds) {
        if (routeIds == null || routeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordVersionDO>()
                .in(MesProBatchRecordVersionDO::getRouteId, routeIds)
                .orderByDesc(MesProBatchRecordVersionDO::getId));
    }

    default Long countByDefinitionId(Long definitionId) {
        return selectCount(MesProBatchRecordVersionDO::getDefinitionId, definitionId);
    }

    default Long countByDefinitionIdAndStatus(Long definitionId, String status) {
        return selectCount(new LambdaQueryWrapperX<MesProBatchRecordVersionDO>()
                .eq(MesProBatchRecordVersionDO::getDefinitionId, definitionId)
                .eq(MesProBatchRecordVersionDO::getStatus, status));
    }

    default List<MesProBatchRecordVersionDO> selectListByStatus(String status) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordVersionDO>()
                .eq(MesProBatchRecordVersionDO::getStatus, status)
                .orderByDesc(MesProBatchRecordVersionDO::getSubmittedAt)
                .orderByDesc(MesProBatchRecordVersionDO::getId));
    }

    default MesProBatchRecordVersionDO selectByApprovalInstanceId(String approvalInstanceId) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordVersionDO>()
                .eq(MesProBatchRecordVersionDO::getApprovalInstanceId, approvalInstanceId));
    }

    @Select("SELECT * FROM mes_pro_batch_record_version "
            + "WHERE definition_id = #{definitionId} AND status = 'PENDING_APPROVAL' "
            + "AND (deleted = FALSE OR deleted IS NULL) "
            + "ORDER BY id DESC LIMIT 1 FOR UPDATE")
    MesProBatchRecordVersionDO selectPendingApprovalByDefinitionIdForUpdate(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM mes_pro_batch_record_version WHERE id = #{id} FOR UPDATE")
    MesProBatchRecordVersionDO selectByIdForUpdate(@Param("id") Long id);

    @Update("UPDATE mes_pro_batch_record_version "
            + "SET status = 'OBSOLETE', update_time = CURRENT_TIMESTAMP "
            + "WHERE definition_id = #{definitionId} AND status = 'APPROVED' AND id <> #{currentVersionId}")
    int obsoleteApprovedVersionsExcept(@Param("definitionId") Long definitionId,
                                       @Param("currentVersionId") Long currentVersionId);

    @Delete("DELETE FROM mes_pro_batch_record_version WHERE definition_id = #{definitionId}")
    int deleteHardByDefinitionId(@Param("definitionId") Long definitionId);
}
