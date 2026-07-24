package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignaturePolicyDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DccElectronicSignaturePolicyMapper extends BaseMapperX<DccElectronicSignaturePolicyDO> {

    default DccElectronicSignaturePolicyDO selectEnabledPolicy() {
        return selectOne(new LambdaQueryWrapperX<DccElectronicSignaturePolicyDO>()
                .eq(DccElectronicSignaturePolicyDO::getStatus, 0)
                .orderByDesc(DccElectronicSignaturePolicyDO::getId)
                .last("LIMIT 1"));
    }
}
