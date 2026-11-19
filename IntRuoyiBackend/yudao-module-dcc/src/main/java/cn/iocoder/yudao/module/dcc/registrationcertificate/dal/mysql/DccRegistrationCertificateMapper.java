package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DccRegistrationCertificateMapper extends BaseMapperX<DccRegistrationCertificateDO> {

    @Update("""
            UPDATE dcc_registration_certificate
               SET deleted = 1, row_version = row_version + 1,
                   updater = #{actorId}, update_time = CURRENT_TIMESTAMP
             WHERE tenant_id = #{tenantId} AND id = #{id} AND deleted = 0
               AND row_version = #{rowVersion}
            """)
    int deletePreservingEvidence(@Param("tenantId") Long tenantId, @Param("id") Long id,
                                 @Param("rowVersion") Integer rowVersion, @Param("actorId") Long actorId);

    @Select("""
            SELECT COUNT(*) FROM dcc_registration_certificate_change
             WHERE tenant_id = #{tenantId} AND certificate_id = #{id}
               AND status = 'PENDING_APPROVAL' AND deleted = 0
            """)
    int countPendingChanges(@Param("tenantId") Long tenantId, @Param("id") Long id);

    @Select("""
            SELECT COUNT(*) FROM dcc_registration_certificate_access_request
             WHERE tenant_id = #{tenantId} AND certificate_id = #{id}
               AND request_type = 'UPLOAD_CERTIFICATE' AND status IN ('SUBMITTED', 'BPM_BOUND') AND deleted = 0
            """)
    int countPendingUploads(@Param("tenantId") Long tenantId, @Param("id") Long id);

    default DccRegistrationCertificateDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<DccRegistrationCertificateDO>()
                .eq(DccRegistrationCertificateDO::getId, id)
                .last("FOR UPDATE"));
    }
}
