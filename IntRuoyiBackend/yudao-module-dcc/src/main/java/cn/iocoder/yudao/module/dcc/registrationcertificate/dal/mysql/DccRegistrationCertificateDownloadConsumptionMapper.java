package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDownloadConsumptionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DccRegistrationCertificateDownloadConsumptionMapper
        extends BaseMapperX<DccRegistrationCertificateDownloadConsumptionDO> {

    default DccRegistrationCertificateDownloadConsumptionDO selectByAttemptKey(Long tenantId, String attemptKey) {
        return selectOne(new LambdaQueryWrapperX<DccRegistrationCertificateDownloadConsumptionDO>()
                .eq(DccRegistrationCertificateDownloadConsumptionDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateDownloadConsumptionDO::getAttemptKey, attemptKey));
    }

    default Long countSuccess(Long tenantId, Long grantId, Long businessFileId) {
        return selectCount(new LambdaQueryWrapperX<DccRegistrationCertificateDownloadConsumptionDO>()
                .eq(DccRegistrationCertificateDownloadConsumptionDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateDownloadConsumptionDO::getGrantId, grantId)
                .eq(DccRegistrationCertificateDownloadConsumptionDO::getBusinessFileId, businessFileId)
                .eq(DccRegistrationCertificateDownloadConsumptionDO::getResult, "SUCCESS"));
    }
}
