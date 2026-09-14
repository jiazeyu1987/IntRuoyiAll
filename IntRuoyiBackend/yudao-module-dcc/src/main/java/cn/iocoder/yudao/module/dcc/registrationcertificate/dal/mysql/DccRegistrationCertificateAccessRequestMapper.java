package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DccRegistrationCertificateAccessRequestMapper
        extends BaseMapperX<DccRegistrationCertificateAccessRequestDO> {

    default DccRegistrationCertificateAccessRequestDO selectByTenantAndRequestKey(Long tenantId, String requestKey) {
        return selectOne(new LambdaQueryWrapperX<DccRegistrationCertificateAccessRequestDO>()
                .eq(DccRegistrationCertificateAccessRequestDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateAccessRequestDO::getRequestKey, requestKey));
    }
}
