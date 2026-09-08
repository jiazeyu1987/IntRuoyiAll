package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.TemporaryRoleGrantAuditDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TemporaryRoleGrantAuditMapper extends BaseMapperX<TemporaryRoleGrantAuditDO> {

    default List<TemporaryRoleGrantAuditDO> selectListByGrantId(Long grantId) {
        return selectList(new LambdaQueryWrapperX<TemporaryRoleGrantAuditDO>()
                .eq(TemporaryRoleGrantAuditDO::getGrantId, grantId)
                .orderByAsc(TemporaryRoleGrantAuditDO::getCreateTime));
    }

}
