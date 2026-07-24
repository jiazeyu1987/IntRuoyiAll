package cn.iocoder.yudao.module.dcc.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclDescriptorDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DccNasAclDescriptorMapper extends BaseMapperX<DccNasAclDescriptorDO> {

    default DccNasAclDescriptorDO selectByDescriptorHash(String descriptorHash) {
        return selectOne(new LambdaQueryWrapperX<DccNasAclDescriptorDO>()
                .eq(DccNasAclDescriptorDO::getDescriptorHash, descriptorHash));
    }
}
