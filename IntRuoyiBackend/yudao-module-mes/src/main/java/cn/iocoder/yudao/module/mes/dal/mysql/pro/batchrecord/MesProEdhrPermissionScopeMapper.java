package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPermissionScopeDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrPermissionScopeMapper extends BaseMapperX<MesProEdhrPermissionScopeDO> {

    default MesProEdhrPermissionScopeDO selectByObject(String objectType, String objectId) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrPermissionScopeDO>()
                .eq(MesProEdhrPermissionScopeDO::getObjectType, objectType)
                .eq(MesProEdhrPermissionScopeDO::getObjectId, objectId)
                .eq(MesProEdhrPermissionScopeDO::getStatus, "ENABLED"));
    }
}
