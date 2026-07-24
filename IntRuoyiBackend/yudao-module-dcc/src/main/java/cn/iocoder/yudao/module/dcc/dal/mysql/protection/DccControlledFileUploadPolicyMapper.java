package cn.iocoder.yudao.module.dcc.dal.mysql.protection;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileUploadPolicyDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * DCC controlled file upload size policy mapper.
 */
@Mapper
public interface DccControlledFileUploadPolicyMapper extends BaseMapperX<DccControlledFileUploadPolicyDO> {
}
