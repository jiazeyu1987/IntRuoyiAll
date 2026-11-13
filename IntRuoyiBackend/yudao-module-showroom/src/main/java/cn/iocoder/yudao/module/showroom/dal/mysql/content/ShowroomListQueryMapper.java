package cn.iocoder.yudao.module.showroom.dal.mysql.content;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomAdminController.PageQueryReqVO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.Map;
import java.util.Set;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

/** Select and count IDs in SQL before loading the page's detailed content. */
@Mapper
public interface ShowroomListQueryMapper {
    String PRODUCT_STATUS = "CASE WHEN EXISTS (SELECT 1 FROM showroom_field_assignment fa WHERE fa.tenant_id=p.tenant_id AND fa.deleted=0 AND fa.target_type='PRODUCT' AND fa.target_id=p.id AND fa.field_code='__PRODUCT_ALL_FIELDS__' AND fa.status='OPEN') THEN 'IN_FILLING' WHEN cr.target_revision_id=l.id THEN cr.status ELSE l.status END";
    String PRODUCT_INCOMPLETE = "(NULLIF(TRIM(d.name_cn),'') IS NULL OR NULLIF(TRIM(d.name_en),'') IS NULL OR d.owner_company_id IS NULL OR NULLIF(TRIM(d.product_owner_type),'') IS NULL OR NULLIF(TRIM(d.lifecycle_stage),'') IS NULL)";
    String PRODUCT_FROM = " FROM showroom_product p JOIN showroom_product_revision l ON l.id=(SELECT r.id FROM showroom_product_revision r WHERE r.product_id=p.id AND r.tenant_id=p.tenant_id AND r.deleted=0 ORDER BY r.revision_no DESC,r.id DESC LIMIT 1) JOIN showroom_product_revision d ON d.id=COALESCE(p.current_revision_id,l.id) AND d.tenant_id=p.tenant_id AND d.deleted=0 LEFT JOIN showroom_change_request cr ON cr.id=(SELECT c.id FROM showroom_change_request c WHERE c.target_type='PRODUCT' AND c.target_id=p.id AND c.tenant_id=p.tenant_id AND c.deleted=0 ORDER BY c.submitted_at DESC,c.id DESC LIMIT 1) ";
    String AWARD_FROM = " FROM showroom_award p JOIN showroom_award_revision d ON d.id=COALESCE(p.current_revision_id,(SELECT r.id FROM showroom_award_revision r WHERE r.award_id=p.id AND r.tenant_id=p.tenant_id AND r.deleted=0 ORDER BY r.revision_no DESC,r.id DESC LIMIT 1)) AND d.tenant_id=p.tenant_id AND d.deleted=0 ";

    @Select("SELECT p.id" + PRODUCT_FROM + "${ew.customSqlSegment}")
    @InterceptorIgnore(tenantLine="true")
    IPage<Long> selectProductIds(IPage<Long> page, @Param("ew") QueryWrapper<Long> wrapper);

    @Select("SELECT p.id" + AWARD_FROM + "${ew.customSqlSegment}")
    @InterceptorIgnore(tenantLine="true")
    IPage<Long> selectAwardIds(IPage<Long> page, @Param("ew") QueryWrapper<Long> wrapper);

    default IPage<Long> productPage(PageQueryReqVO req, Set<Long> visibleIds) {
        PageParam params = params(req);
        QueryWrapperX<Long> wrapper = base();
        if (visibleIds != null) {
            if (visibleIds.isEmpty()) wrapper.apply("1=0");
            else wrapper.in("p.id", visibleIds);
        }
        wrapper.eqIfPresent("p.id", req.productId());
        if (hasText(req.keyword())) wrapper.and(w -> w.like("p.product_code", req.keyword().trim()).or().like("p.legacy_product_code", req.keyword().trim()).or().like("d.name_cn", req.keyword().trim()).or().like("d.name_en", req.keyword().trim()));
        wrapper.eqIfPresent("d.owner_company_id", blank(req.ownerCompanyId()));
        wrapper.eqIfPresent("d.product_owner_type", blank(req.ownerType()));
        wrapper.eqIfPresent("d.lifecycle_stage", blank(req.lifecycleStage()));
        if ("COMPLETE".equalsIgnoreCase(req.incompleteStatus())) wrapper.apply("NOT "+PRODUCT_INCOMPLETE);
        if ("INCOMPLETE".equalsIgnoreCase(req.incompleteStatus())) wrapper.apply(PRODUCT_INCOMPLETE);
        if (hasText(req.approvalStatus())) wrapper.apply("("+PRODUCT_STATUS+") = {0}", "PENDING_SUPERVISOR_APPROVAL".equals(req.approvalStatus()) ? "PENDING_SUPERVISOR_REVIEW" : req.approvalStatus().trim());
        ListFilterQueryUtils.apply(params, wrapper, productFields());
        wrapper.last("ORDER BY CASE WHEN p.legacy_product_code IS NULL OR p.legacy_product_code='' THEN 1 ELSE 0 END,p.legacy_product_code ASC,p.id ASC");
        return selectProductIds(new Page<>(params.getPageNo(), params.getPageSize()), wrapper);
    }

    default IPage<Long> awardPage(PageQueryReqVO req) {
        PageParam params = params(req);
        QueryWrapperX<Long> wrapper = base();
        if (hasText(req.keyword())) wrapper.and(w -> w.like("p.award_code", req.keyword().trim()).or().like("d.name_cn", req.keyword().trim()).or().like("d.name_en", req.keyword().trim()).or().like("d.issuer", req.keyword().trim()));
        ListFilterQueryUtils.apply(params, wrapper, Map.of(
            "awardCode", text("TRIM(p.award_code)"), "nameCn", text("TRIM(d.name_cn)"),
            "nameEn", text("COALESCE(NULLIF(TRIM(d.name_en),''),'未填写')"), "issuer", text("TRIM(d.issuer)"),
            "awardDateText", text("TRIM(d.award_date_text)"), "coverImageUrl", bool("NULLIF(TRIM(d.cover_image),'') IS NOT NULL"),
            "incomplete", text("CASE WHEN p.incomplete_flag THEN '待完善' ELSE '已发布' END")));
        wrapper.orderByAsc("p.id");
        return selectAwardIds(new Page<>(params.getPageNo(), params.getPageSize()), wrapper);
    }

    private static QueryWrapperX<Long> base() {
        return new QueryWrapperX<Long>().eq("p.tenant_id", TenantContextHolder.getRequiredTenantId()).eq("p.deleted", false);
    }
    static PageParam params(PageQueryReqVO req) {
        PageParam params = new PageParam();
        params.setPageNo(req.pageNo() == null || req.pageNo()<1 ? 1 : req.pageNo());
        params.setPageSize(req.pageSize() == null || req.pageSize()<1 ? 20 : Math.min(req.pageSize(), 100));
        params.setListFilters(req.listFilters());
        return params;
    }
    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
    private static String blank(String value) { return hasText(value) ? value.trim() : null; }
    static Map<String, Field> productFields() {
        return Map.ofEntries(
            Map.entry("nameCn", text("d.name_cn")), Map.entry("legacyProductCode", text("p.legacy_product_code")),
            Map.entry("revisionNo", text("CONCAT('V',d.revision_no)")),
            Map.entry("approvalText", text("CASE "+PRODUCT_STATUS+" WHEN 'IN_FILLING' THEN '指派中' WHEN 'DRAFT' THEN '草稿' WHEN 'PENDING_SUPERVISOR_REVIEW' THEN '审核中' WHEN 'PENDING_SUPERVISOR_APPROVAL' THEN '审核中' WHEN 'PENDING_GAOXIN_APPROVAL' THEN '审核中' WHEN 'PENDING' THEN '审核中' WHEN 'APPROVED' THEN '已批准' WHEN 'REJECTED' THEN '已驳回' WHEN 'PUBLISHED' THEN '已发布' ELSE '' END")),
            Map.entry("pipelineLayout", text("COALESCE(NULLIF(TRIM(d.pipeline_layout),''),'未填写')")),
            Map.entry("ownerTypeText", text("CASE WHEN d.product_owner_type IN ('YINGTAI','SUBSIDIARY') THEN '瑛泰医疗' ELSE '未完善' END")),
            Map.entry("lifecycleText", text("CASE d.lifecycle_stage WHEN 'REGISTERED' THEN '已注册' WHEN 'R_AND_D' THEN '研发中' ELSE '未填写' END")),
            Map.entry("targetMarket", text("d.target_market")),
            Map.entry("coverImageUrl", bool("COALESCE(NULLIF(TRIM(l.cover_image),''),NULLIF(TRIM(d.cover_image),'')) IS NOT NULL")),
            Map.entry("displayColumn9", text("CONCAT('在售国家 ',CASE WHEN NULLIF(TRIM(d.target_market),'') IS NULL THEN 'MISS' ELSE 'OK' END,' 在售国家(英) ',CASE WHEN NULLIF(TRIM(d.target_market_en),'') IS NULL THEN 'MISS' ELSE 'OK' END,' 中音频 ',CASE WHEN "+audio("ZH",true)+" THEN 'OK' ELSE 'MISS' END,' 英音频 ',CASE WHEN "+audio("EN",false)+" THEN 'OK' ELSE 'MISS' END)")));
    }
    private static String audio(String language, boolean includeLatest) {
        String live = "EXISTS (SELECT 1 FROM showroom_narration_version n WHERE n.id=(SELECT n2.id FROM showroom_narration_version n2 WHERE n2.tenant_id=p.tenant_id AND n2.deleted=0 AND n2.target_type='PRODUCT' AND n2.target_id=p.id AND n2.audience_type='PUBLIC' AND n2.language='"+language+"' AND n2.status='PUBLISHED' ORDER BY n2.version_no DESC,n2.id DESC LIMIT 1) AND n.audio_file_id IS NOT NULL)";
        return includeLatest ? "("+live+" OR EXISTS (SELECT 1 FROM showroom_narration_version n WHERE n.id=(SELECT n2.id FROM showroom_narration_version n2 WHERE n2.tenant_id=p.tenant_id AND n2.deleted=0 AND n2.target_type='PRODUCT' AND n2.target_id=p.id AND n2.audience_type='PUBLIC' AND n2.language='ZH' AND n2.source_revision_id=d.id ORDER BY n2.version_no DESC,n2.id DESC LIMIT 1) AND n.audio_file_id IS NOT NULL))" : live;
    }
}
