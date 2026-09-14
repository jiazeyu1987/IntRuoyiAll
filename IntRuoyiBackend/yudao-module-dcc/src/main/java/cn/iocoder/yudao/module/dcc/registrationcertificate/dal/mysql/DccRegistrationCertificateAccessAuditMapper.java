package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessAuditDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DccRegistrationCertificateAccessAuditMapper
        extends BaseMapperX<DccRegistrationCertificateAccessAuditDO> {

    default DccRegistrationCertificateAccessAuditDO selectByEventKey(Long tenantId, String eventKey) {
        return selectOne(new LambdaQueryWrapperX<DccRegistrationCertificateAccessAuditDO>()
                .eq(DccRegistrationCertificateAccessAuditDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateAccessAuditDO::getEventKey, eventKey));
    }
}
