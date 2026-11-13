package cn.iocoder.yudao.module.infra.dal.mysql.config;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.TenantConfigDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantConfigMapper extends BaseMapperX<TenantConfigDO> {
}
