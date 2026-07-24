package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesProBatchRecordReportMapper extends BaseMapperX<MesProBatchRecordReportDO> {

    default MesProBatchRecordReportDO selectBySampleKeyAndRouteKeyAndSourceTableIndex(
            String sampleKey, String routeKey, Integer sourceTableIndex) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordReportDO>()
                .eq(MesProBatchRecordReportDO::getSampleKey, sampleKey)
                .eq(MesProBatchRecordReportDO::getRouteKey, routeKey)
                .eq(MesProBatchRecordReportDO::getSourceTableIndex, sourceTableIndex)
                .orderByDesc(MesProBatchRecordReportDO::getId));
    }

    default List<MesProBatchRecordReportDO> selectListByBatchRecordNameAndRouteKey(
            String batchRecordName, String routeKey) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordReportDO>()
                .eq(MesProBatchRecordReportDO::getBatchRecordName, batchRecordName)
                .eq(MesProBatchRecordReportDO::getRouteKey, routeKey)
                .orderByAsc(MesProBatchRecordReportDO::getSourceTableIndex));
    }

    default Long countMainByBatchRecordNameAndRouteKey(
            String batchRecordName, String routeKey, String mainFormSlotType) {
        return selectCount(new LambdaQueryWrapperX<MesProBatchRecordReportDO>()
                .eq(MesProBatchRecordReportDO::getBatchRecordName, batchRecordName)
                .eq(MesProBatchRecordReportDO::getRouteKey, routeKey)
                .and(wrapper -> wrapper.eq(MesProBatchRecordReportDO::getFormSlotType, mainFormSlotType)
                        .or()
                        .isNull(MesProBatchRecordReportDO::getFormSlotType)));
    }

    default MesProBatchRecordReportDO selectOneByBatchRecordNameAndFormSlotType(
            String batchRecordName, String formSlotType) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordReportDO>()
                .eq(MesProBatchRecordReportDO::getBatchRecordName, batchRecordName)
                .eq(MesProBatchRecordReportDO::getFormSlotType, formSlotType)
                .orderByDesc(MesProBatchRecordReportDO::getId));
    }

    default List<MesProBatchRecordReportDO> selectListByBatchRecordNameAndFormSlotType(
            String batchRecordName, String formSlotType) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordReportDO>()
                .eq(MesProBatchRecordReportDO::getBatchRecordName, batchRecordName)
                .eq(MesProBatchRecordReportDO::getFormSlotType, formSlotType)
                .orderByAsc(MesProBatchRecordReportDO::getSourceTableIndex)
                .orderByAsc(MesProBatchRecordReportDO::getId));
    }

    default List<MesProBatchRecordReportDO> selectListByBatchRecordNameAndRouteKeyAndSourceTableIndex(
            String batchRecordName, String routeKey, Integer sourceTableIndex) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordReportDO>()
                .eq(MesProBatchRecordReportDO::getBatchRecordName, batchRecordName)
                .eq(MesProBatchRecordReportDO::getRouteKey, routeKey)
                .eq(MesProBatchRecordReportDO::getSourceTableIndex, sourceTableIndex)
                .orderByAsc(MesProBatchRecordReportDO::getId));
    }

    default MesProBatchRecordReportDO selectByReportId(String reportId) {
        return selectOne(MesProBatchRecordReportDO::getReportId, reportId);
    }

    default MesProBatchRecordReportDO selectByReportCode(String reportCode) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordReportDO>()
                .eq(MesProBatchRecordReportDO::getReportCode, reportCode)
                .orderByDesc(MesProBatchRecordReportDO::getId));
    }

    default MesProBatchRecordReportDO selectBySourceFileSha256AndRouteKeyAndSourceTableIndex(
            String sourceFileSha256, String routeKey, Integer sourceTableIndex) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordReportDO>()
                .eq(MesProBatchRecordReportDO::getSourceFileSha256, sourceFileSha256)
                .eq(MesProBatchRecordReportDO::getRouteKey, routeKey)
                .eq(MesProBatchRecordReportDO::getSourceTableIndex, sourceTableIndex)
                .orderByDesc(MesProBatchRecordReportDO::getId));
    }

    default List<MesProBatchRecordReportDO> selectListBySampleKeyAndRouteKey(String sampleKey, String routeKey) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordReportDO>()
                .eq(MesProBatchRecordReportDO::getSampleKey, sampleKey)
                .eq(MesProBatchRecordReportDO::getRouteKey, routeKey)
                .orderByAsc(MesProBatchRecordReportDO::getSourceTableIndex));
    }

    default List<MesProBatchRecordReportDO> selectListBySourceFileSha256AndRouteKey(
            String sourceFileSha256, String routeKey) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordReportDO>()
                .eq(MesProBatchRecordReportDO::getSourceFileSha256, sourceFileSha256)
                .eq(MesProBatchRecordReportDO::getRouteKey, routeKey)
                .orderByAsc(MesProBatchRecordReportDO::getSourceTableIndex)
                .orderByAsc(MesProBatchRecordReportDO::getId));
    }

    default List<MesProBatchRecordReportDO> selectListByDefinitionIdAndVersionId(Long definitionId, Long versionId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordReportDO>()
                .eq(MesProBatchRecordReportDO::getBatchRecordDefinitionId, definitionId)
                .eq(MesProBatchRecordReportDO::getBatchRecordVersionId, versionId)
                .orderByAsc(MesProBatchRecordReportDO::getSourceTableIndex));
    }

    default Long countByDefinitionId(Long definitionId) {
        return selectCount(MesProBatchRecordReportDO::getBatchRecordDefinitionId, definitionId);
    }

    default Long countMainByDefinitionId(Long definitionId, String mainFormSlotType) {
        return selectCount(new LambdaQueryWrapperX<MesProBatchRecordReportDO>()
                .eq(MesProBatchRecordReportDO::getBatchRecordDefinitionId, definitionId)
                .and(wrapper -> wrapper.eq(MesProBatchRecordReportDO::getFormSlotType, mainFormSlotType)
                        .or()
                        .isNull(MesProBatchRecordReportDO::getFormSlotType)));
    }

    default Long countMainByDefinitionIdAndVersionId(Long definitionId, Long versionId, String mainFormSlotType) {
        return selectCount(new LambdaQueryWrapperX<MesProBatchRecordReportDO>()
                .eq(MesProBatchRecordReportDO::getBatchRecordDefinitionId, definitionId)
                .eq(MesProBatchRecordReportDO::getBatchRecordVersionId, versionId)
                .and(wrapper -> wrapper.eq(MesProBatchRecordReportDO::getFormSlotType, mainFormSlotType)
                        .or()
                        .isNull(MesProBatchRecordReportDO::getFormSlotType)));
    }

    default List<MesProBatchRecordReportDO> selectListByReportIds(Collection<String> reportIds) {
        if (reportIds == null || reportIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordReportDO>()
                .in(MesProBatchRecordReportDO::getReportId, reportIds));
    }

    default List<MesProBatchRecordReportDO> selectListByReportCategoryId(String reportCategoryId) {
        return selectList(MesProBatchRecordReportDO::getReportCategoryId, reportCategoryId);
    }

    @Delete("DELETE FROM mes_pro_batch_record_report WHERE report_id = #{reportId}")
    int deleteHardByReportId(String reportId);

    @Delete("DELETE FROM mes_pro_batch_record_report WHERE report_category_id = #{reportCategoryId}")
    int deleteHardByReportCategoryId(String reportCategoryId);
}
