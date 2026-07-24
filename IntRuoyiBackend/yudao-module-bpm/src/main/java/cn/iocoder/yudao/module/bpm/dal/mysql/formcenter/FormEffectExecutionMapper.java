package cn.iocoder.yudao.module.bpm.dal.mysql.formcenter;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormEffectPendingPageReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormEffectExecutionDO;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormEffectStatus;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FormEffectExecutionMapper extends BaseMapperX<FormEffectExecutionDO> {

    default FormEffectExecutionDO selectByInstanceIdAndIdempotencyKey(Long tenantId, Long instanceId,
            String idempotencyKey) {
        return selectOne(new QueryWrapperX<FormEffectExecutionDO>()
                .eq("tenant_id", tenantId)
                .eq("instance_id", instanceId)
                .eq("idempotency_key", idempotencyKey)
                .last("LIMIT 1"));
    }

    default PageResult<FormEffectExecutionDO> selectPendingPage(FormEffectPendingPageReqVO reqVO) {
        return selectPage(reqVO, new QueryWrapperX<FormEffectExecutionDO>()
                .eq("tenant_id", reqVO.getTenantId())
                .eq("status", FormEffectStatus.FAILED_PENDING.name())
                .eqIfPresent("instance_id", reqVO.getInstanceId())
                .orderByDesc("id"));
    }

}
