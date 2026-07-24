package cn.iocoder.yudao.module.system.dal.mysql.controlledcontent;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentTransitionAuditDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ControlledContentTransitionAuditMapper extends BaseMapperX<ControlledContentTransitionAuditDO> {

    default ControlledContentTransitionAuditDO selectByVersionRefIdAndActionAndEventKey(Long versionRefId,
                                                                                        String action,
                                                                                        String eventKey) {
        return selectOne(new LambdaQueryWrapperX<ControlledContentTransitionAuditDO>()
                .eq(ControlledContentTransitionAuditDO::getVersionRefId, versionRefId)
                .eq(ControlledContentTransitionAuditDO::getAction, action)
                .eq(ControlledContentTransitionAuditDO::getEventKey, eventKey));
    }

    default Long countTransitions(Long tenantId, String contentType, String contentKey) {
        return selectCount(new LambdaQueryWrapperX<ControlledContentTransitionAuditDO>()
                .eq(ControlledContentTransitionAuditDO::getTenantId, tenantId)
                .eq(ControlledContentTransitionAuditDO::getContentType, contentType)
                .eq(ControlledContentTransitionAuditDO::getContentKey, contentKey));
    }

}
