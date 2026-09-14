package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MesProEdhrProcessFormPermissionRuleMapper
        extends BaseMapperX<MesProEdhrProcessFormPermissionRuleDO> {

    Long FORM_LEVEL_ROUTE_PROCESS_ID = 0L;
    List<String> PROCESS_FORM_FILL_RULE_TYPES = List.of("FILL", "EQUIPMENT_FILL", "QUALITY_FILL");

    default List<MesProEdhrProcessFormPermissionRuleDO> selectListByRouteProcessAndReport(
            Long routeProcessId, String batchRecordReportId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrProcessFormPermissionRuleDO>()
                .eq(MesProEdhrProcessFormPermissionRuleDO::getRouteProcessId, routeProcessId)
                .eq(MesProEdhrProcessFormPermissionRuleDO::getBatchRecordReportId, batchRecordReportId)
                .orderByAsc(MesProEdhrProcessFormPermissionRuleDO::getRuleType)
                .orderByAsc(MesProEdhrProcessFormPermissionRuleDO::getScopeKey)
                .orderByAsc(MesProEdhrProcessFormPermissionRuleDO::getSignatureCellKey)
                .orderByAsc(MesProEdhrProcessFormPermissionRuleDO::getId));
    }

    default List<MesProEdhrProcessFormPermissionRuleDO> selectListByRouteProcessReportAndVersion(
            Long routeProcessId, String batchRecordReportId, Long batchRecordVersionId) {
        return selectList(withBatchRecordVersion(new LambdaQueryWrapperX<MesProEdhrProcessFormPermissionRuleDO>()
                .eq(MesProEdhrProcessFormPermissionRuleDO::getRouteProcessId, routeProcessId)
                .eq(MesProEdhrProcessFormPermissionRuleDO::getBatchRecordReportId, batchRecordReportId)
                .orderByAsc(MesProEdhrProcessFormPermissionRuleDO::getRuleType)
                .orderByAsc(MesProEdhrProcessFormPermissionRuleDO::getScopeKey)
                .orderByAsc(MesProEdhrProcessFormPermissionRuleDO::getSignatureCellKey)
                .orderByAsc(MesProEdhrProcessFormPermissionRuleDO::getId), batchRecordVersionId));
    }

    default MesProEdhrProcessFormPermissionRuleDO selectEnabledFillRule(Long routeProcessId,
                                                                        String batchRecordReportId) {
        return selectEnabledFillRule(routeProcessId, batchRecordReportId, null);
    }

    default MesProEdhrProcessFormPermissionRuleDO selectEnabledFillRule(Long routeProcessId,
                                                                        String batchRecordReportId,
                                                                        Long batchRecordVersionId) {
        return selectEnabledFillRuleInVersionScope(routeProcessId, batchRecordReportId, batchRecordVersionId);
    }

    default List<MesProEdhrProcessFormPermissionRuleDO> selectEnabledFillRules(Long routeProcessId,
                                                                               String batchRecordReportId) {
        return selectEnabledFillRules(routeProcessId, batchRecordReportId, null);
    }

    default List<MesProEdhrProcessFormPermissionRuleDO> selectEnabledFillRules(Long routeProcessId,
                                                                               String batchRecordReportId,
                                                                               Long batchRecordVersionId) {
        return selectEnabledFillRulesInVersionScope(routeProcessId, batchRecordReportId, batchRecordVersionId);
    }

    default MesProEdhrProcessFormPermissionRuleDO selectEnabledFillRuleForRouteOrReport(
            Long routeProcessId, String batchRecordReportId) {
        return selectEnabledFillRuleForRouteOrReport(routeProcessId, batchRecordReportId, null);
    }

    default MesProEdhrProcessFormPermissionRuleDO selectEnabledFillRuleForRouteOrReport(
            Long routeProcessId, String batchRecordReportId, Long batchRecordVersionId) {
        MesProEdhrProcessFormPermissionRuleDO routeRule =
                selectEnabledFillRule(routeProcessId, batchRecordReportId, batchRecordVersionId);
        return routeRule == null
                ? selectEnabledFillRule(FORM_LEVEL_ROUTE_PROCESS_ID, batchRecordReportId, batchRecordVersionId)
                : routeRule;
    }

    default List<MesProEdhrProcessFormPermissionRuleDO> selectEnabledFillRulesForRouteOrReport(
            Long routeProcessId, String batchRecordReportId) {
        return selectEnabledFillRulesForRouteOrReport(routeProcessId, batchRecordReportId, null);
    }

    default List<MesProEdhrProcessFormPermissionRuleDO> selectEnabledFillRulesForRouteOrReport(
            Long routeProcessId, String batchRecordReportId, Long batchRecordVersionId) {
        List<MesProEdhrProcessFormPermissionRuleDO> routeRules =
                selectEnabledFillRules(routeProcessId, batchRecordReportId, batchRecordVersionId);
        return routeRules.isEmpty()
                ? selectEnabledFillRules(FORM_LEVEL_ROUTE_PROCESS_ID, batchRecordReportId, batchRecordVersionId)
                : routeRules;
    }

    default List<MesProEdhrProcessFormPermissionRuleDO> selectEnabledFillRulesByReportId(
            String batchRecordReportId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrProcessFormPermissionRuleDO>()
                .eq(MesProEdhrProcessFormPermissionRuleDO::getBatchRecordReportId, batchRecordReportId)
                .eq(MesProEdhrProcessFormPermissionRuleDO::getRuleType, "FILL")
                .eq(MesProEdhrProcessFormPermissionRuleDO::getEnabled, true)
                .orderByAsc(MesProEdhrProcessFormPermissionRuleDO::getRouteProcessId)
                .orderByAsc(MesProEdhrProcessFormPermissionRuleDO::getScopeKey)
                .orderByDesc(MesProEdhrProcessFormPermissionRuleDO::getId));
    }

    default MesProEdhrProcessFormPermissionRuleDO selectEnabledSignatureRule(Long routeProcessId,
                                                                             String batchRecordReportId,
                                                                             String signatureCellKey) {
        return selectEnabledSignatureRule(routeProcessId, batchRecordReportId, signatureCellKey, null);
    }

    default MesProEdhrProcessFormPermissionRuleDO selectEnabledSignatureRule(Long routeProcessId,
                                                                              String batchRecordReportId,
                                                                              String signatureCellKey,
                                                                              Long batchRecordVersionId) {
        return selectEnabledSignatureRuleInVersionScope(
                routeProcessId, batchRecordReportId, signatureCellKey, batchRecordVersionId);
    }

    default void deleteByRouteProcessAndReport(Long routeProcessId, String batchRecordReportId) {
        delete(new LambdaQueryWrapperX<MesProEdhrProcessFormPermissionRuleDO>()
                .eq(MesProEdhrProcessFormPermissionRuleDO::getRouteProcessId, routeProcessId)
                .eq(MesProEdhrProcessFormPermissionRuleDO::getBatchRecordReportId, batchRecordReportId));
    }

    @Delete("""
            DELETE FROM mes_pro_edhr_process_form_permission_rule
            WHERE route_process_id = #{routeProcessId}
              AND batch_record_report_id = #{batchRecordReportId}
            """)
    int physicalDeleteByRouteProcessAndReport(@Param("routeProcessId") Long routeProcessId,
                                              @Param("batchRecordReportId") String batchRecordReportId);

    @Delete("""
            DELETE FROM mes_pro_edhr_process_form_permission_rule
            WHERE route_process_id = #{routeProcessId}
              AND batch_record_report_id = #{batchRecordReportId}
              AND (
                    (#{batchRecordVersionId} IS NULL AND batch_record_version_id IS NULL)
                    OR batch_record_version_id = #{batchRecordVersionId}
                  )
            """)
    int physicalDeleteByRouteProcessReportAndVersion(@Param("routeProcessId") Long routeProcessId,
                                                     @Param("batchRecordReportId") String batchRecordReportId,
                                                     @Param("batchRecordVersionId") Long batchRecordVersionId);

    @Delete("""
            DELETE FROM mes_pro_edhr_process_form_permission_rule
            WHERE batch_record_report_id = #{batchRecordReportId}
              AND batch_record_version_id = #{batchRecordVersionId}
              AND route_process_id <> #{formLevelRouteProcessId}
              AND rule_type IN ('FILL', 'EQUIPMENT_FILL', 'QUALITY_FILL')
            """)
    int physicalDeleteRouteFillRulesByReportAndVersion(
            @Param("batchRecordReportId") String batchRecordReportId,
            @Param("batchRecordVersionId") Long batchRecordVersionId,
            @Param("formLevelRouteProcessId") Long formLevelRouteProcessId);

    default Long countByBatchRecordVersionId(Long batchRecordVersionId) {
        return selectCount(MesProEdhrProcessFormPermissionRuleDO::getBatchRecordVersionId, batchRecordVersionId);
    }

    private MesProEdhrProcessFormPermissionRuleDO selectEnabledFillRuleInVersionScope(
            Long routeProcessId, String batchRecordReportId, Long batchRecordVersionId) {
        return selectOne(withBatchRecordVersion(new LambdaQueryWrapperX<MesProEdhrProcessFormPermissionRuleDO>()
                .eq(MesProEdhrProcessFormPermissionRuleDO::getRouteProcessId, routeProcessId)
                .eq(MesProEdhrProcessFormPermissionRuleDO::getBatchRecordReportId, batchRecordReportId)
                .eq(MesProEdhrProcessFormPermissionRuleDO::getRuleType, "FILL")
                .eq(MesProEdhrProcessFormPermissionRuleDO::getEnabled, true)
                .orderByDesc(MesProEdhrProcessFormPermissionRuleDO::getId), batchRecordVersionId));
    }

    private List<MesProEdhrProcessFormPermissionRuleDO> selectEnabledFillRulesInVersionScope(
            Long routeProcessId, String batchRecordReportId, Long batchRecordVersionId) {
        return selectList(withBatchRecordVersion(new LambdaQueryWrapperX<MesProEdhrProcessFormPermissionRuleDO>()
                .eq(MesProEdhrProcessFormPermissionRuleDO::getRouteProcessId, routeProcessId)
                .eq(MesProEdhrProcessFormPermissionRuleDO::getBatchRecordReportId, batchRecordReportId)
                .in(MesProEdhrProcessFormPermissionRuleDO::getRuleType, PROCESS_FORM_FILL_RULE_TYPES)
                .eq(MesProEdhrProcessFormPermissionRuleDO::getEnabled, true)
                .orderByAsc(MesProEdhrProcessFormPermissionRuleDO::getRuleType)
                .orderByAsc(MesProEdhrProcessFormPermissionRuleDO::getScopeKey)
                .orderByDesc(MesProEdhrProcessFormPermissionRuleDO::getId), batchRecordVersionId));
    }

    private MesProEdhrProcessFormPermissionRuleDO selectEnabledSignatureRuleInVersionScope(
            Long routeProcessId, String batchRecordReportId, String signatureCellKey, Long batchRecordVersionId) {
        return selectOne(withBatchRecordVersion(new LambdaQueryWrapperX<MesProEdhrProcessFormPermissionRuleDO>()
                .eq(MesProEdhrProcessFormPermissionRuleDO::getRouteProcessId, routeProcessId)
                .eq(MesProEdhrProcessFormPermissionRuleDO::getBatchRecordReportId, batchRecordReportId)
                .eq(MesProEdhrProcessFormPermissionRuleDO::getRuleType, "SIGNATURE")
                .eq(MesProEdhrProcessFormPermissionRuleDO::getSignatureCellKey, signatureCellKey)
                .eq(MesProEdhrProcessFormPermissionRuleDO::getEnabled, true)
                .orderByDesc(MesProEdhrProcessFormPermissionRuleDO::getId), batchRecordVersionId));
    }

    private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MesProEdhrProcessFormPermissionRuleDO> withBatchRecordVersion(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MesProEdhrProcessFormPermissionRuleDO> query,
            Long batchRecordVersionId) {
        if (batchRecordVersionId == null) {
            return query.isNull(MesProEdhrProcessFormPermissionRuleDO::getBatchRecordVersionId);
        }
        return query.eq(MesProEdhrProcessFormPermissionRuleDO::getBatchRecordVersionId, batchRecordVersionId);
    }
}
