package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateBpmBindingDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DccRegistrationCertificateBpmBindingMapper extends BaseMapperX<DccRegistrationCertificateBpmBindingDO> {

    default DccRegistrationCertificateBpmBindingDO selectByRequestId(Long tenantId, Long requestId) {
        return selectOne(new LambdaQueryWrapperX<DccRegistrationCertificateBpmBindingDO>()
                .eq(DccRegistrationCertificateBpmBindingDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateBpmBindingDO::getRequestId, requestId));
    }

    default DccRegistrationCertificateBpmBindingDO selectByProcessInstanceId(Long tenantId, String processInstanceId) {
        return selectOne(new LambdaQueryWrapperX<DccRegistrationCertificateBpmBindingDO>()
                .eq(DccRegistrationCertificateBpmBindingDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateBpmBindingDO::getBpmProcessInstanceId, processInstanceId));
    }

    default DccRegistrationCertificateBpmBindingDO selectByBusinessKey(Long tenantId, String businessKey) {
        return selectOne(new LambdaQueryWrapperX<DccRegistrationCertificateBpmBindingDO>()
                .eq(DccRegistrationCertificateBpmBindingDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateBpmBindingDO::getBusinessKey, businessKey));
    }
}
