package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql;

import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAuditDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DccRegistrationCertificateAuditMapper {

    @Insert("""
            INSERT INTO dcc_registration_certificate_audit
              (tenant_id, owner_company_id, certificate_id,
               requested_owner_company_id, requested_certificate_id,
               version_id, snapshot_id, business_file_id,
               event_key, event_type, actor_id, result, result_code, request_trace_id,
               detail_json, occurred_at, creator)
            VALUES
              (#{tenantId}, #{ownerCompanyId}, #{certificateId},
               #{requestedOwnerCompanyId}, #{requestedCertificateId}, #{versionId}, #{snapshotId},
               #{businessFileId}, #{eventKey}, #{eventType}, #{actorId}, #{result}, #{resultCode},
               #{requestTraceId}, #{detailJson}, #{occurredAt}, #{creator})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DccRegistrationCertificateAuditDO audit);

    @Select("""
            SELECT * FROM dcc_registration_certificate_audit
             WHERE certificate_id = #{certificateId}
             ORDER BY occurred_at ASC, id ASC
            """)
    List<DccRegistrationCertificateAuditDO> selectListByCertificateId(
            @Param("certificateId") Long certificateId);

    @Select("""
            SELECT * FROM dcc_registration_certificate_audit
             WHERE tenant_id = #{tenantId}
               AND event_key = #{eventKey}
             LIMIT 1
            """)
    DccRegistrationCertificateAuditDO selectByTenantIdAndEventKey(
            @Param("tenantId") Long tenantId,
            @Param("eventKey") String eventKey);
}
