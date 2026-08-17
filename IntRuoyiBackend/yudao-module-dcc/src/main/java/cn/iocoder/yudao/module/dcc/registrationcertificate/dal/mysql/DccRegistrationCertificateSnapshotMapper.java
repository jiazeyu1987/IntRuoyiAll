package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql;

import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DccRegistrationCertificateSnapshotMapper {

    @Insert("""
            INSERT INTO dcc_registration_certificate_snapshot
              (version_id, revision_no, source_change_id, product_name, registrant_name,
               model_specification, structure_composition, intended_use, technical_requirements,
               residence_address, production_address, entrusted_production, self_production,
               entrusted_enterprises_json, effective_at, tenant_id)
            VALUES
              (#{versionId}, #{revisionNo}, #{sourceChangeId}, #{productName}, #{registrantName},
               #{modelSpecification}, #{structureComposition}, #{intendedUse}, #{technicalRequirements},
               #{residenceAddress}, #{productionAddress}, #{entrustedProduction}, #{selfProduction},
               #{entrustedEnterprisesJson}, #{effectiveAt}, #{tenantId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DccRegistrationCertificateSnapshotDO snapshot);

    @Select("SELECT * FROM dcc_registration_certificate_snapshot WHERE id = #{id}")
    DccRegistrationCertificateSnapshotDO selectById(@Param("id") Long id);

    @Select("""
            SELECT * FROM dcc_registration_certificate_snapshot
             WHERE version_id = #{versionId}
             ORDER BY revision_no ASC, id ASC
            """)
    List<DccRegistrationCertificateSnapshotDO> selectListByVersionId(@Param("versionId") Long versionId);

    @Update("""
            UPDATE dcc_registration_certificate_snapshot
               SET source_change_id = #{snapshot.sourceChangeId},
                   product_name = #{snapshot.productName},
                   registrant_name = #{snapshot.registrantName},
                   model_specification = #{snapshot.modelSpecification},
                   structure_composition = #{snapshot.structureComposition},
                   intended_use = #{snapshot.intendedUse},
                   technical_requirements = #{snapshot.technicalRequirements},
                   residence_address = #{snapshot.residenceAddress},
                   production_address = #{snapshot.productionAddress},
                   entrusted_production = #{snapshot.entrustedProduction},
                   self_production = #{snapshot.selfProduction},
                   entrusted_enterprises_json = #{snapshot.entrustedEnterprisesJson},
                   effective_at = #{snapshot.effectiveAt},
                   revision_no = revision_no + 1,
                   update_time = CURRENT_TIMESTAMP
             WHERE id = #{snapshot.id}
               AND tenant_id = #{tenantId}
               AND revision_no = #{expectedRevisionNo}
               AND deleted = 0
               AND EXISTS (
                   SELECT 1
                     FROM dcc_registration_certificate_version v
                    WHERE v.id = dcc_registration_certificate_snapshot.version_id
                      AND v.tenant_id = #{tenantId}
                      AND v.status = 'DRAFT'
                      AND v.deleted = 0
               )
               AND NOT EXISTS (
                   SELECT 1
                     FROM dcc_registration_certificate_snapshot_entrusted e
                    WHERE e.snapshot_id = dcc_registration_certificate_snapshot.id
                      AND e.tenant_id = #{tenantId}
                      AND e.deleted = 0
               )
            """)
    int updateDraftByIdAndRevision(
            @Param("snapshot") DccRegistrationCertificateSnapshotDO snapshot,
            @Param("tenantId") Long tenantId,
            @Param("expectedRevisionNo") Integer expectedRevisionNo);

    @Delete("""
            DELETE FROM dcc_registration_certificate_snapshot
             WHERE id = #{id}
               AND tenant_id = #{tenantId}
               AND revision_no = #{expectedRevisionNo}
               AND deleted = 0
               AND EXISTS (
                   SELECT 1
                     FROM dcc_registration_certificate_version v
                    WHERE v.id = dcc_registration_certificate_snapshot.version_id
                      AND v.tenant_id = #{tenantId}
                      AND v.status = 'DRAFT'
                      AND v.deleted = 0
               )
               AND NOT EXISTS (
                   SELECT 1
                     FROM dcc_registration_certificate_snapshot_entrusted e
                    WHERE e.snapshot_id = dcc_registration_certificate_snapshot.id
                      AND e.tenant_id = #{tenantId}
                      AND e.deleted = 0
               )
            """)
    int deleteDraftByIdAndRevision(
            @Param("id") Long id,
            @Param("tenantId") Long tenantId,
            @Param("expectedRevisionNo") Integer expectedRevisionNo);
}
