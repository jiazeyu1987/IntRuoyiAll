package cn.iocoder.yudao.module.bpm.dal.mysql.formcenter;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormCenterTemplatePoolPageReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import java.util.Map;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

@Mapper
public interface FormTemplateVersionMapper extends BaseMapperX<FormTemplateVersionDO> {

    default PageResult<FormTemplateVersionDO> selectPage(FormCenterTemplatePoolPageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new QueryWrapperX<FormTemplateVersionDO>()
                .eqIfPresent("tenant_id", reqVO.getTenantId())
                .likeIfPresent("template_name", reqVO.getTemplateName())
                .eqIfPresent("status", reqVO.getStatus())
                .orderByDesc("id"), Map.ofEntries(
                Map.entry("templateName", text("template_name")),
                Map.entry("currentEffectiveVersion", text("version_no")),
                Map.entry("pendingVersion", text("CONCAT(COALESCE(version_no, ''), ' ', CASE status WHEN 'DRAFT' THEN '草稿' WHEN 'PENDING_APPROVAL' THEN '审批中' WHEN 'REJECTED' THEN '已驳回' WHEN 'READY' THEN '待发布' WHEN 'PUBLISHED' THEN '已发布' WHEN 'DISABLED' THEN '已停用' WHEN 'OBSOLETE' THEN '已作废' ELSE '' END)")),
                Map.entry("status", text("CASE status WHEN 'DRAFT' THEN '草稿' WHEN 'PENDING_APPROVAL' THEN '审批中' WHEN 'REJECTED' THEN '已驳回' WHEN 'READY' THEN '待发布' WHEN 'PUBLISHED' THEN '已发布' WHEN 'DISABLED' THEN '已停用' WHEN 'OBSOLETE' THEN '已作废' ELSE '' END")),
                Map.entry("updatedTime", dateTime("update_time")),
                Map.entry("remark", text("remark"))
        )));
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
