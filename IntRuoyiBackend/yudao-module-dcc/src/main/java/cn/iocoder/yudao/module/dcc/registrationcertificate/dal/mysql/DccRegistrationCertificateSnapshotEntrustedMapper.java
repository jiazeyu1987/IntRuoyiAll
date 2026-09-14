package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql;

import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotEntrustedDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DccRegistrationCertificateSnapshotEntrustedMapper {

    @Insert("""
            INSERT INTO dcc_registration_certificate_snapshot_entrusted
              (snapshot_id, enterprise_id, enterprise_name_snapshot, sort_order, tenant_id)
            VALUES
              (#{snapshotId}, #{enterpriseId}, #{enterpriseNameSnapshot}, #{sortOrder}, #{tenantId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DccRegistrationCertificateSnapshotEntrustedDO entrusted);

    @Select("""
            SELECT * FROM dcc_registration_certificate_snapshot_entrusted
             WHERE snapshot_id = #{snapshotId}
             ORDER BY sort_order ASC, id ASC
            """)
    List<DccRegistrationCertificateSnapshotEntrustedDO> selectListBySnapshotId(
            @Param("snapshotId") Long snapshotId);

    @Delete("""
            DELETE FROM dcc_registration_certificate_snapshot_entrusted
             WHERE snapshot_id = #{snapshotId}
               AND tenant_id = #{tenantId}
               AND deleted = 0
               AND EXISTS (
                   SELECT 1
                     FROM dcc_registration_certificate_snapshot s
                     JOIN dcc_registration_certificate_version v
                       ON v.id = s.version_id
                      AND v.tenant_id = s.tenant_id
                    WHERE s.id = #{snapshotId}
                      AND s.tenant_id = #{tenantId}
                      AND s.revision_no = #{expectedRevisionNo}
                      AND s.deleted = 0
                      AND v.status = 'DRAFT'
                      AND v.deleted = 0
               )
            """)
    int deleteDraftBySnapshotIdAndRevision(
            @Param("snapshotId") Long snapshotId,
            @Param("tenantId") Long tenantId,
            @Param("expectedRevisionNo") Integer expectedRevisionNo);
}
