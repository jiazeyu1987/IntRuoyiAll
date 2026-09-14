package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureBindingDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DccControlledFileSignatureBindingMapper extends BaseMapperX<DccControlledFileSignatureBindingDO> {

    default DccControlledFileSignatureBindingDO selectBySignatureId(Long signatureId) {
        return selectOne(DccControlledFileSignatureBindingDO::getSignatureId, signatureId);
    }

}
