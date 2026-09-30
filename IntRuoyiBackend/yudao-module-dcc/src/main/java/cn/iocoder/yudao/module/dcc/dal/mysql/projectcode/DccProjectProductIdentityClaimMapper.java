package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductIdentityClaimDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DccProjectProductIdentityClaimMapper extends BaseMapperX<DccProjectProductIdentityClaimDO> {

    default DccProjectProductIdentityClaimDO selectByTypeAndValue(String identityType, String identityValue) {
        return selectOne(new LambdaQueryWrapperX<DccProjectProductIdentityClaimDO>()
                .eq(DccProjectProductIdentityClaimDO::getIdentityType, identityType)
                .eq(DccProjectProductIdentityClaimDO::getIdentityValue, identityValue));
    }

    @Delete("DELETE FROM dcc_project_product_identity_claim WHERE request_id = #{requestId}")
    void deleteByRequestId(@Param("requestId") Long requestId);
}
