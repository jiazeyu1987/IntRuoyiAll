package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql;

import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
