package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationHandlingDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MesProEdhrDeviationHandlingMapper extends BaseMapperX<MesProEdhrDeviationHandlingDO> {

    @Select("""
            SELECT *
            FROM mes_pro_edhr_deviation_handling
            WHERE tenant_id = #{tenantId}
              AND deviation_id = #{deviationId}
              AND deleted = 0
            LIMIT 1
            FOR UPDATE
            """)
    MesProEdhrDeviationHandlingDO selectByTenantAndDeviationIdForUpdate(
            @Param("tenantId") Long tenantId, @Param("deviationId") Long deviationId);

    @Select("""
            SELECT *
            FROM mes_pro_edhr_deviation_handling
            WHERE tenant_id = #{tenantId}
              AND deviation_id = #{deviationId}
              AND deleted = 0
            LIMIT 1
            """)
    MesProEdhrDeviationHandlingDO selectByTenantAndDeviationId(
            @Param("tenantId") Long tenantId, @Param("deviationId") Long deviationId);

    @Update("""
            UPDATE mes_pro_edhr_deviation_handling
            SET investigation_started_at = #{handling.investigationStartedAt},
                planned_completed_at = #{handling.plannedCompletedAt},
                completed_at = #{handling.completedAt},
                investigation_members_json = #{handling.investigationMembersJson},
                root_cause_analysis = #{handling.rootCauseAnalysis},
                impact_scope = #{handling.impactScope},
                risk_assessment = #{handling.riskAssessment},
                product_disposition = #{handling.productDisposition},
                nonconformance_review_code = #{handling.nonconformanceReviewCode},
                corrective_owner_id = #{handling.correctiveOwnerId},
                corrective_owner_name = #{handling.correctiveOwnerName},
                corrective_due_at = #{handling.correctiveDueAt},
                capa_required = #{handling.capaRequired},
                capa_code = #{handling.capaCode},
                capa_attachments_json = #{handling.capaAttachmentsJson},
                handling_conclusion = #{handling.handlingConclusion},
                verification_result = #{handling.verificationResult},
                verification_content = #{handling.verificationContent},
                content_version = #{handling.contentVersion},
                content_hash = #{handling.contentHash}
            WHERE tenant_id = #{tenantId}
              AND id = #{handling.id}
              AND deviation_id = #{deviationId}
              AND content_version = #{expectedContentVersion}
              AND deleted = 0
            """)
    int updateContent(@Param("tenantId") Long tenantId,
                      @Param("deviationId") Long deviationId,
                      @Param("expectedContentVersion") Integer expectedContentVersion,
                      @Param("handling") MesProEdhrDeviationHandlingDO handling);
}
