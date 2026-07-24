package cn.iocoder.yudao.module.system.dal.mysql.controlledcontent;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentVersionRefDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ControlledContentVersionRefMapper extends BaseMapperX<ControlledContentVersionRefDO> {

    default ControlledContentVersionRefDO selectActive(Long tenantId, String contentType, String contentKey) {
        return selectOne(new LambdaQueryWrapperX<ControlledContentVersionRefDO>()
                .eq(ControlledContentVersionRefDO::getTenantId, tenantId)
                .eq(ControlledContentVersionRefDO::getContentType, contentType)
                .eq(ControlledContentVersionRefDO::getContentKey, contentKey)
                .eq(ControlledContentVersionRefDO::getActiveUniqueFlag, 1));
    }

    default ControlledContentVersionRefDO selectByNativeVersion(Long tenantId, String contentType, String contentKey,
                                                                Long nativeVersionId) {
        return selectOne(new LambdaQueryWrapperX<ControlledContentVersionRefDO>()
                .eq(ControlledContentVersionRefDO::getTenantId, tenantId)
                .eq(ControlledContentVersionRefDO::getContentType, contentType)
                .eq(ControlledContentVersionRefDO::getContentKey, contentKey)
                .eq(ControlledContentVersionRefDO::getNativeVersionId, nativeVersionId));
    }

    default ControlledContentVersionRefDO selectOpenCandidate(Long tenantId, String contentType, String contentKey) {
        return selectOne(new LambdaQueryWrapperX<ControlledContentVersionRefDO>()
                .eq(ControlledContentVersionRefDO::getTenantId, tenantId)
                .eq(ControlledContentVersionRefDO::getContentType, contentType)
                .eq(ControlledContentVersionRefDO::getContentKey, contentKey)
                .eq(ControlledContentVersionRefDO::getOpenCandidateUniqueFlag, 1));
    }

    default Long countActiveRefs(Long tenantId, String contentType, String contentKey) {
        return selectCount(new LambdaQueryWrapperX<ControlledContentVersionRefDO>()
                .eq(ControlledContentVersionRefDO::getTenantId, tenantId)
                .eq(ControlledContentVersionRefDO::getContentType, contentType)
                .eq(ControlledContentVersionRefDO::getContentKey, contentKey)
                .eq(ControlledContentVersionRefDO::getActiveUniqueFlag, 1));
    }

    default Long countOpenCandidateRefs(Long tenantId, String contentType, String contentKey) {
        return selectCount(new LambdaQueryWrapperX<ControlledContentVersionRefDO>()
                .eq(ControlledContentVersionRefDO::getTenantId, tenantId)
                .eq(ControlledContentVersionRefDO::getContentType, contentType)
                .eq(ControlledContentVersionRefDO::getContentKey, contentKey)
                .eq(ControlledContentVersionRefDO::getOpenCandidateUniqueFlag, 1));
    }

}
