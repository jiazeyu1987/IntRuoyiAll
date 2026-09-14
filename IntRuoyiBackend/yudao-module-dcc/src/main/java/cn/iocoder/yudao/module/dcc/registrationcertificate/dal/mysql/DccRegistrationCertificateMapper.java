package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DccRegistrationCertificateMapper extends BaseMapperX<DccRegistrationCertificateDO> {

    default DccRegistrationCertificateDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<DccRegistrationCertificateDO>()
                .eq(DccRegistrationCertificateDO::getId, id)
                .last("FOR UPDATE"));
    }
}
