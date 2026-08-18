package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateGrantDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccRegistrationCertificateGrantMapper extends BaseMapperX<DccRegistrationCertificateGrantDO> {

    default DccRegistrationCertificateGrantDO selectByTenantAndGrantKey(Long tenantId, String grantKey) {
        return selectOne(new LambdaQueryWrapperX<DccRegistrationCertificateGrantDO>()
                .eq(DccRegistrationCertificateGrantDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateGrantDO::getGrantKey, grantKey));
    }

    default List<DccRegistrationCertificateGrantDO> selectByRequest(Long tenantId, Long requestId) {
        return selectList(new LambdaQueryWrapperX<DccRegistrationCertificateGrantDO>()
                .eq(DccRegistrationCertificateGrantDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateGrantDO::getRequestId, requestId)
                .orderByAsc(DccRegistrationCertificateGrantDO::getId));
    }

    default List<DccRegistrationCertificateGrantDO> selectActiveByCertificate(Long tenantId, Long granteeUserId,
                                                                               Long certificateId, String grantType) {
        return selectList(new LambdaQueryWrapperX<DccRegistrationCertificateGrantDO>()
                .eq(DccRegistrationCertificateGrantDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateGrantDO::getGranteeUserId, granteeUserId)
                .eq(DccRegistrationCertificateGrantDO::getCertificateId, certificateId)
                .eq(DccRegistrationCertificateGrantDO::getGrantType, grantType)
                .eq(DccRegistrationCertificateGrantDO::getStatus, "ACTIVE")
                .orderByAsc(DccRegistrationCertificateGrantDO::getId));
    }

    default List<DccRegistrationCertificateGrantDO> selectByCertificate(Long tenantId, Long granteeUserId,
                                                                         Long certificateId, String grantType) {
        return selectList(new LambdaQueryWrapperX<DccRegistrationCertificateGrantDO>()
                .eq(DccRegistrationCertificateGrantDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateGrantDO::getGranteeUserId, granteeUserId)
                .eq(DccRegistrationCertificateGrantDO::getCertificateId, certificateId)
                .eq(DccRegistrationCertificateGrantDO::getGrantType, grantType)
                .orderByAsc(DccRegistrationCertificateGrantDO::getId));
    }

    default List<DccRegistrationCertificateGrantDO> selectActiveByBusinessFile(Long tenantId, Long granteeUserId,
                                                                                Long businessFileId, String grantType) {
        return selectList(new LambdaQueryWrapperX<DccRegistrationCertificateGrantDO>()
                .eq(DccRegistrationCertificateGrantDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateGrantDO::getGranteeUserId, granteeUserId)
                .eq(DccRegistrationCertificateGrantDO::getBusinessFileId, businessFileId)
                .eq(DccRegistrationCertificateGrantDO::getGrantType, grantType)
                .eq(DccRegistrationCertificateGrantDO::getStatus, "ACTIVE")
                .orderByAsc(DccRegistrationCertificateGrantDO::getId));
    }

    default List<DccRegistrationCertificateGrantDO> selectByBusinessFile(Long tenantId, Long granteeUserId,
                                                                          Long businessFileId, String grantType) {
        return selectList(new LambdaQueryWrapperX<DccRegistrationCertificateGrantDO>()
                .eq(DccRegistrationCertificateGrantDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateGrantDO::getGranteeUserId, granteeUserId)
                .eq(DccRegistrationCertificateGrantDO::getBusinessFileId, businessFileId)
                .eq(DccRegistrationCertificateGrantDO::getGrantType, grantType)
                .orderByAsc(DccRegistrationCertificateGrantDO::getId));
    }
}
