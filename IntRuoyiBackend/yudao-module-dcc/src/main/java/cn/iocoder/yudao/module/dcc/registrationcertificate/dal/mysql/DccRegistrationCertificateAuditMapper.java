package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql;

import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAuditDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.historicalimport.DccRegistrationCertificateHistoricalImportRow;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
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

    @Select("""
            <script>
            SELECT COUNT(*)
              FROM dcc_registration_certificate_audit a
             WHERE a.tenant_id = #{tenantId}
               AND a.event_type = 'HISTORICAL_IMPORT'
               <if test="sourceHash != null and sourceHash != ''">
                 AND a.event_key LIKE CONCAT('HISTORICAL_IMPORT:', #{sourceHash}, ':%')
               </if>
            </script>
            """)
    @InterceptorIgnore(tenantLine = "true")
    long countHistoricalImportPage(@Param("tenantId") Long tenantId,
                                   @Param("sourceHash") String sourceHash);

    @Select("""
            <script>
            SELECT a.id,
                   a.owner_company_id AS ownerCompanyId,
                   a.certificate_id AS certificateId,
                   c.id AS certificateRecordId,
                   c.owner_company_id AS certificateOwnerCompanyId,
                   a.version_id AS versionId,
                   v.id AS versionRecordId,
                   a.snapshot_id AS snapshotId,
                   s.id AS snapshotRecordId,
                   a.actor_id AS actorId,
                   a.result,
                   a.result_code AS resultCode,
                   a.request_trace_id AS requestTraceId,
                   a.detail_json AS detailJson,
                   a.occurred_at AS occurredAt,
                   v.certificate_no AS certificateNo,
                   v.version_no AS versionNo,
                   s.product_name AS productName
              FROM dcc_registration_certificate_audit a
              LEFT JOIN dcc_registration_certificate c
                ON c.tenant_id = a.tenant_id
               AND c.id = a.certificate_id
               AND c.deleted = 0
              LEFT JOIN dcc_registration_certificate_version v
                ON v.tenant_id = a.tenant_id
               AND v.id = a.version_id
               AND v.deleted = 0
              LEFT JOIN dcc_registration_certificate_snapshot s
                ON s.tenant_id = a.tenant_id
               AND s.id = a.snapshot_id
               AND s.deleted = 0
             WHERE a.tenant_id = #{tenantId}
               AND a.event_type = 'HISTORICAL_IMPORT'
               <if test="sourceHash != null and sourceHash != ''">
                 AND a.event_key LIKE CONCAT('HISTORICAL_IMPORT:', #{sourceHash}, ':%')
               </if>
             ORDER BY a.occurred_at DESC, a.id DESC
             LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<DccRegistrationCertificateHistoricalImportRow> selectHistoricalImportPage(
            @Param("tenantId") Long tenantId,
            @Param("sourceHash") String sourceHash,
            @Param("limit") int limit,
            @Param("offset") int offset);
}
