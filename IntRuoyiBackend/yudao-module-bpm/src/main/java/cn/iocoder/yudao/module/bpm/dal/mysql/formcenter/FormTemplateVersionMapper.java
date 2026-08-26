package cn.iocoder.yudao.module.bpm.dal.mysql.formcenter;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormCenterTemplatePoolPageReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FormTemplateVersionMapper extends BaseMapperX<FormTemplateVersionDO> {

    default PageResult<FormTemplateVersionDO> selectPage(FormCenterTemplatePoolPageReqVO reqVO) {
        return selectPage(reqVO, new QueryWrapperX<FormTemplateVersionDO>()
                .eqIfPresent("tenant_id", reqVO.getTenantId())
                .likeIfPresent("template_name", reqVO.getTemplateName())
                .eqIfPresent("status", reqVO.getStatus())
                .orderByDesc("id"));
    }

    default FormTemplateVersionDO selectByTemplateIdAndVersionNo(Long templateId, String versionNo) {
        return selectOne(new QueryWrapperX<FormTemplateVersionDO>()
                .eq("template_id", templateId)
                .eq("version_no", versionNo));
    }

    default FormTemplateVersionDO selectLatestByTemplateId(Long tenantId, Long templateId) {
        return selectOne(new QueryWrapperX<FormTemplateVersionDO>()
                .eq("tenant_id", tenantId)
                .eq("template_id", templateId)
                .orderByDesc("id")
                .last("LIMIT 1"));
    }

    default FormTemplateVersionDO selectDraftByTemplateId(Long tenantId, Long templateId) {
        return selectOne(new QueryWrapperX<FormTemplateVersionDO>()
                .eq("tenant_id", tenantId)
                .eq("template_id", templateId)
                .eq("status", "DRAFT")
                .orderByDesc("id")
                .last("LIMIT 1"));
    }

    default FormTemplateVersionDO selectLatestByTemplateName(Long tenantId, String templateName) {
        return selectOne(new QueryWrapperX<FormTemplateVersionDO>()
                .eq("tenant_id", tenantId)
                .eq("template_name", templateName)
                .orderByDesc("id")
                .last("LIMIT 1"));
    }

    default FormTemplateVersionDO selectLatestPublishedByTemplateId(Long tenantId, Long templateId) {
        return selectOne(new QueryWrapperX<FormTemplateVersionDO>()
                .eq("tenant_id", tenantId)
                .eq("template_id", templateId)
                .eq("status", "PUBLISHED")
                .orderByDesc("id")
                .last("LIMIT 1"));
    }

}
