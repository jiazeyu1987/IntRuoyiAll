package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestFileDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccRegistrationCertificateAccessRequestFileMapper
        extends BaseMapperX<DccRegistrationCertificateAccessRequestFileDO> {

    default List<DccRegistrationCertificateAccessRequestFileDO> selectByRequestId(Long tenantId, Long requestId) {
        return selectList(new LambdaQueryWrapperX<DccRegistrationCertificateAccessRequestFileDO>()
                .eq(DccRegistrationCertificateAccessRequestFileDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateAccessRequestFileDO::getRequestId, requestId)
                .orderByAsc(DccRegistrationCertificateAccessRequestFileDO::getId));
    }
}
