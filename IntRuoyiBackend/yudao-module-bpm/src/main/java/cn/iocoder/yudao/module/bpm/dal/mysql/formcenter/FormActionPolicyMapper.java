package cn.iocoder.yudao.module.bpm.dal.mysql.formcenter;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormPolicyPageReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionPolicyDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FormActionPolicyMapper extends BaseMapperX<FormActionPolicyDO> {

    default PageResult<FormActionPolicyDO> selectPage(FormPolicyPageReqVO reqVO) {
        return selectPage(reqVO, new QueryWrapperX<FormActionPolicyDO>()
                .eqIfPresent("tenant_id", reqVO.getTenantId())
                .eqIfPresent("data_domain", reqVO.getDataDomain())
                .eqIfPresent("system_code", reqVO.getSystemCode())
                .eqIfPresent("object_type", reqVO.getObjectType())
                .eqIfPresent("action_code", reqVO.getActionCode())
                .eqIfPresent("object_state", reqVO.getObjectState())
                .eqIfPresent("status", reqVO.getStatus())
                .orderByDesc("id"));
    }

    default List<FormActionPolicyDO> selectPublishedByAction(Long tenantId, String dataDomain, String systemCode,
            String objectType, String actionCode, String objectState) {
        return selectList(new QueryWrapperX<FormActionPolicyDO>()
                .eq("tenant_id", tenantId)
                .eq("data_domain", dataDomain)
                .eq("system_code", systemCode)
                .eq("object_type", objectType)
                .eq("action_code", actionCode)
                .eq("object_state", objectState)
                .eq("status", "PUBLISHED")
                .orderByDesc("id"));
    }

}
