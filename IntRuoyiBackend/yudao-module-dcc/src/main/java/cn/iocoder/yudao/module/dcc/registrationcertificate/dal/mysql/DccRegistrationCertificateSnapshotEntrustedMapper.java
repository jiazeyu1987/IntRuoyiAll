package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql;

import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotEntrustedDO;
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
}
