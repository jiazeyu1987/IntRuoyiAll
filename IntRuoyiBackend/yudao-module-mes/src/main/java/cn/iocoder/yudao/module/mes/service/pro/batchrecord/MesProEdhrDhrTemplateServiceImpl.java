package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrCatalogCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrCatalogPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrCatalogRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateBindingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateImpactPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateImpactReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateImpactRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateLifecycleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateSignoffReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateVersionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDhrCatalogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDhrTemplateBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDhrTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDhrTemplateImpactDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDhrTemplateVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDhrCatalogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDhrTemplateBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDhrTemplateImpactMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDhrTemplateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDhrTemplateVersionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDhrTemplateErrorCodeConstants.PRO_EDHR_DHR_CATALOG_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDhrTemplateErrorCodeConstants.PRO_EDHR_DHR_CATALOG_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDhrTemplateErrorCodeConstants.PRO_EDHR_DHR_TEMPLATE_BINDING_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDhrTemplateErrorCodeConstants.PRO_EDHR_DHR_TEMPLATE_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDhrTemplateErrorCodeConstants.PRO_EDHR_DHR_TEMPLATE_IMPACT_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDhrTemplateErrorCodeConstants.PRO_EDHR_DHR_TEMPLATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDhrTemplateErrorCodeConstants.PRO_EDHR_DHR_TEMPLATE_REVIEW_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDhrTemplateErrorCodeConstants.PRO_EDHR_DHR_TEMPLATE_SIGNOFF_EVIDENCE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDhrTemplateErrorCodeConstants.PRO_EDHR_DHR_TEMPLATE_SIGNOFF_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDhrTemplateErrorCodeConstants.PRO_EDHR_DHR_TEMPLATE_STATUS_INVALID;

@Service
@Validated
public class MesProEdhrDhrTemplateServiceImpl implements MesProEdhrDhrTemplateService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PRECHECK_FAILED = "PRECHECK_FAILED";
    public static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_SIGNOFF_PENDING = "SIGNOFF_PENDING";
    public static final String STATUS_EFFECTIVE = "EFFECTIVE";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    public static final String STATUS_RETIRED = "RETIRED";
    public static final String STATUS_OBSOLETE = "OBSOLETE";
    private static final String CATALOG_STATUS_ACTIVE = "ACTIVE";
    private static final String REVIEW_STATUS_NOT_SUBMITTED = "NOT_SUBMITTED";
    private static final String REVIEW_STATUS_APPROVED = "APPROVED";
    private static final String SIGNOFF_STATUS_NOT_SIGNED = "NOT_SIGNED";
    private static final String SIGNOFF_STATUS_SIGNED = "SIGNED";
    private static final String ACTION_TYPE_RETIRE = "RETIRE";
    private static final String ACTION_TYPE_VOID = "VOID";
    private static final String BINDING_TYPE_PRODUCT = "PRODUCT";
    private static final String BINDING_TYPE_ROUTE = "ROUTE";
    private static final String BINDING_TYPE_PROCESS = "PROCESS";
    private static final String BINDING_TYPE_BATCH_TYPE = "BATCH_TYPE";
    private static final List<String> REQUIRED_BINDING_TYPES = List.of(
            BINDING_TYPE_PRODUCT, BINDING_TYPE_ROUTE, BINDING_TYPE_PROCESS, BINDING_TYPE_BATCH_TYPE);

    @Resource
    private MesProEdhrDhrCatalogMapper catalogMapper;
    @Resource
    private MesProEdhrDhrTemplateMapper templateMapper;
    @Resource
    private MesProEdhrDhrTemplateVersionMapper versionMapper;
    @Resource
    private MesProEdhrDhrTemplateBindingMapper bindingMapper;
    @Resource
    private MesProEdhrDhrTemplateImpactMapper impactMapper;

    @Override
    public PageResult<MesProEdhrDhrCatalogRespVO> getCatalogPage(MesProEdhrDhrCatalogPageReqVO reqVO) {
        return BeanUtils.toBean(catalogMapper.selectPage(reqVO), MesProEdhrDhrCatalogRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrDhrCatalogRespVO createCatalog(MesProEdhrDhrCatalogCreateReqVO reqVO) {
        if (catalogMapper.selectByCatalogCode(reqVO.getCatalogCode()) != null) {
            throw exception(PRO_EDHR_DHR_CATALOG_CODE_DUPLICATE);
        }
        MesProEdhrDhrCatalogDO catalog = new MesProEdhrDhrCatalogDO()
                .setCatalogCode(StrUtil.trim(reqVO.getCatalogCode()))
                .setCatalogName(StrUtil.trim(reqVO.getCatalogName()))
                .setParentCatalogId(reqVO.getParentCatalogId())
                .setStatus(CATALOG_STATUS_ACTIVE)
                .setRemark(reqVO.getRemark());
        catalogMapper.insert(catalog);
        return BeanUtils.toBean(catalog, MesProEdhrDhrCatalogRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrDhrTemplateRespVO> getTemplatePage(MesProEdhrDhrTemplatePageReqVO reqVO) {
        PageResult<MesProEdhrDhrTemplateRespVO> page =
                BeanUtils.toBean(templateMapper.selectPage(reqVO), MesProEdhrDhrTemplateRespVO.class);
        page.getList().forEach(this::fillTemplateChildren);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrDhrTemplateRespVO createTemplate(MesProEdhrDhrTemplateCreateReqVO reqVO) {
        requireCatalog(reqVO.getCatalogId());
        if (templateMapper.selectByTemplateCode(reqVO.getTemplateCode()) != null) {
            throw exception(PRO_EDHR_DHR_TEMPLATE_CODE_DUPLICATE);
        }
        MesProEdhrDhrTemplateDO template = new MesProEdhrDhrTemplateDO()
                .setCatalogId(reqVO.getCatalogId())
                .setTemplateCode(StrUtil.trim(reqVO.getTemplateCode()))
                .setTemplateName(StrUtil.trim(reqVO.getTemplateName()))
                .setCurrentVersion(StrUtil.trim(reqVO.getCurrentVersion()))
                .setStatus(STATUS_DRAFT)
                .setReviewStatus(REVIEW_STATUS_NOT_SUBMITTED)
                .setSignoffStatus(SIGNOFF_STATUS_NOT_SIGNED)
                .setBindingCount(0)
                .setIntegrityIssueCount(0)
                .setRemark(reqVO.getRemark());
        templateMapper.insert(template);

        versionMapper.insert(new MesProEdhrDhrTemplateVersionDO()
                .setTemplateId(template.getId())
                .setVersionNo(template.getCurrentVersion())
                .setTemplateSnapshotJson(StrUtil.trim(reqVO.getTemplateSnapshotJson()))
                .setChangeSummary("初始版本"));

        List<MesProEdhrDhrTemplateBindingDO> bindings = buildBindings(template.getId(), reqVO);
        for (MesProEdhrDhrTemplateBindingDO binding : bindings) {
            bindingMapper.insert(binding);
        }
        refreshBindingCount(template);
        return toRespVO(requireTemplate(template.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrDhrTemplateRespVO runIntegrityCheck(MesProEdhrDhrTemplateLifecycleReqVO reqVO) {
        MesProEdhrDhrTemplateDO template = requireTemplate(reqVO.getId());
        List<String> missingTypes = missingRequiredBindings(template.getId());
        String status = missingTypes.isEmpty() ? STATUS_PENDING_REVIEW : STATUS_PRECHECK_FAILED;
        template.setStatus(status)
                .setIntegrityIssueCount(missingTypes.size())
                .setIntegrityIssueJson(missingTypes.isEmpty() ? null : JsonUtils.toJsonString(Map.of(
                        "missingBindingTypes", missingTypes,
                        "message", "DHR模板缺少必需绑定，禁止生效。")));
        templateMapper.updateById(template);
        return toRespVO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrDhrTemplateRespVO approveTemplate(MesProEdhrDhrTemplateLifecycleReqVO reqVO) {
        MesProEdhrDhrTemplateDO template = requireTemplate(reqVO.getId());
        requireNoIntegrityIssues(template);
        if (!STATUS_PENDING_REVIEW.equals(template.getStatus())) {
            throw exception(PRO_EDHR_DHR_TEMPLATE_STATUS_INVALID);
        }
        template.setStatus(STATUS_APPROVED)
                .setReviewStatus(REVIEW_STATUS_APPROVED);
        templateMapper.updateById(template);
        return toRespVO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrDhrTemplateRespVO signoffTemplate(MesProEdhrDhrTemplateSignoffReqVO reqVO) {
        MesProEdhrDhrTemplateDO template = requireTemplate(reqVO.getId());
        requireApproved(template);
        String signoffEvidenceHash = StrUtil.trim(reqVO.getSignoffEvidenceHash());
        if (StrUtil.isBlank(signoffEvidenceHash)) {
            throw exception(PRO_EDHR_DHR_TEMPLATE_SIGNOFF_EVIDENCE_REQUIRED);
        }
        template.setStatus(STATUS_SIGNOFF_PENDING)
                .setSignoffStatus(SIGNOFF_STATUS_SIGNED)
                .setSignoffEvidenceHash(signoffEvidenceHash);
        templateMapper.updateById(template);
        return toRespVO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrDhrTemplateRespVO activateTemplate(MesProEdhrDhrTemplateLifecycleReqVO reqVO) {
        MesProEdhrDhrTemplateDO template = requireTemplate(reqVO.getId());
        requireNoIntegrityIssues(template);
        requireApproved(template);
        requireSignedOff(template);
        if (!STATUS_SIGNOFF_PENDING.equals(template.getStatus())) {
            throw exception(PRO_EDHR_DHR_TEMPLATE_STATUS_INVALID);
        }
        template.setStatus(STATUS_EFFECTIVE)
                .setEffectiveAt(now());
        templateMapper.updateById(template);
        return toRespVO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrDhrTemplateRespVO retireTemplate(MesProEdhrDhrTemplateImpactReqVO reqVO) {
        MesProEdhrDhrTemplateDO template = requireTemplate(reqVO.getId());
        requireImpactConfirmed(reqVO);
        recordImpact(template.getId(), ACTION_TYPE_RETIRE, reqVO);
        template.setStatus(STATUS_RETIRED)
                .setRetiredAt(now());
        templateMapper.updateById(template);
        return toRespVO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrDhrTemplateRespVO voidTemplate(MesProEdhrDhrTemplateImpactReqVO reqVO) {
        MesProEdhrDhrTemplateDO template = requireTemplate(reqVO.getId());
        requireImpactConfirmed(reqVO);
        recordImpact(template.getId(), ACTION_TYPE_VOID, reqVO);
        template.setStatus(STATUS_OBSOLETE)
                .setVoidedAt(now());
        templateMapper.updateById(template);
        return toRespVO(template);
    }

    @Override
    public PageResult<MesProEdhrDhrTemplateImpactRespVO> getImpactPage(MesProEdhrDhrTemplateImpactPageReqVO reqVO) {
        return BeanUtils.toBean(impactMapper.selectPage(reqVO), MesProEdhrDhrTemplateImpactRespVO.class);
    }

    private MesProEdhrDhrCatalogDO requireCatalog(Long id) {
        MesProEdhrDhrCatalogDO catalog = id == null ? null : catalogMapper.selectById(id);
        if (catalog == null) {
            throw exception(PRO_EDHR_DHR_CATALOG_NOT_EXISTS);
        }
        return catalog;
    }

    private MesProEdhrDhrTemplateDO requireTemplate(Long id) {
        MesProEdhrDhrTemplateDO template = id == null ? null : templateMapper.selectById(id);
        if (template == null) {
            throw exception(PRO_EDHR_DHR_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }

    private void requireNoIntegrityIssues(MesProEdhrDhrTemplateDO template) {
        List<String> missingTypes = missingRequiredBindings(template.getId());
        if (!missingTypes.isEmpty()) {
            throw exception(PRO_EDHR_DHR_TEMPLATE_BINDING_REQUIRED, String.join(",", missingTypes));
        }
        if (template.getIntegrityIssueCount() != null && template.getIntegrityIssueCount() > 0) {
            throw exception(PRO_EDHR_DHR_TEMPLATE_BINDING_REQUIRED, template.getIntegrityIssueJson());
        }
    }

    private void requireApproved(MesProEdhrDhrTemplateDO template) {
        if (!REVIEW_STATUS_APPROVED.equals(template.getReviewStatus())) {
            throw exception(PRO_EDHR_DHR_TEMPLATE_REVIEW_REQUIRED);
        }
    }

    private void requireSignedOff(MesProEdhrDhrTemplateDO template) {
        if (!SIGNOFF_STATUS_SIGNED.equals(template.getSignoffStatus())
                || StrUtil.isBlank(template.getSignoffEvidenceHash())) {
            throw exception(PRO_EDHR_DHR_TEMPLATE_SIGNOFF_REQUIRED);
        }
    }

    private void requireImpactConfirmed(MesProEdhrDhrTemplateImpactReqVO reqVO) {
        if (!Boolean.TRUE.equals(reqVO.getImpactConfirmed()) || StrUtil.isBlank(reqVO.getImpactScopeJson())) {
            throw exception(PRO_EDHR_DHR_TEMPLATE_IMPACT_REQUIRED);
        }
    }

    private List<String> missingRequiredBindings(Long templateId) {
        Set<String> existingTypes = bindingMapper.selectListByTemplateId(templateId).stream()
                .map(MesProEdhrDhrTemplateBindingDO::getBindingType)
                .collect(Collectors.toSet());
        List<String> missingTypes = new ArrayList<>();
        for (String requiredType : REQUIRED_BINDING_TYPES) {
            if (!existingTypes.contains(requiredType)) {
                missingTypes.add(requiredType);
            }
        }
        return missingTypes;
    }

    private List<MesProEdhrDhrTemplateBindingDO> buildBindings(Long templateId, MesProEdhrDhrTemplateCreateReqVO reqVO) {
        List<MesProEdhrDhrTemplateBindingDO> bindings = new ArrayList<>();
        addBinding(bindings, templateId, BINDING_TYPE_PRODUCT, reqVO.getProductCode());
        addBinding(bindings, templateId, BINDING_TYPE_ROUTE, reqVO.getRouteCode());
        addBinding(bindings, templateId, BINDING_TYPE_PROCESS, reqVO.getProcessCode());
        addBinding(bindings, templateId, BINDING_TYPE_BATCH_TYPE, reqVO.getBatchType());
        return bindings;
    }

    private void addBinding(List<MesProEdhrDhrTemplateBindingDO> bindings, Long templateId,
                            String bindingType, String bindingObjectCode) {
        if (StrUtil.isBlank(bindingObjectCode)) {
            return;
        }
        bindings.add(new MesProEdhrDhrTemplateBindingDO()
                .setTemplateId(templateId)
                .setBindingType(bindingType)
                .setBindingObjectCode(StrUtil.trim(bindingObjectCode)));
    }

    private void refreshBindingCount(MesProEdhrDhrTemplateDO template) {
        int bindingCount = bindingMapper.selectListByTemplateId(template.getId()).size();
        template.setBindingCount(bindingCount);
        templateMapper.updateById(new MesProEdhrDhrTemplateDO()
                .setId(template.getId())
                .setBindingCount(bindingCount));
    }

    private void recordImpact(Long templateId, String actionType, MesProEdhrDhrTemplateImpactReqVO reqVO) {
        String impactScopeJson = StrUtil.trim(reqVO.getImpactScopeJson());
        impactMapper.insert(new MesProEdhrDhrTemplateImpactDO()
                .setTemplateId(templateId)
                .setActionType(actionType)
                .setImpactScopeJson(impactScopeJson)
                .setImpactConfirmed(true)
                .setConfirmedBy(SecurityFrameworkUtils.getLoginUserId())
                .setConfirmedAt(now()));
    }

    private MesProEdhrDhrTemplateRespVO toRespVO(MesProEdhrDhrTemplateDO template) {
        MesProEdhrDhrTemplateRespVO respVO = BeanUtils.toBean(template, MesProEdhrDhrTemplateRespVO.class);
        fillTemplateChildren(respVO);
        return respVO;
    }

    private void fillTemplateChildren(MesProEdhrDhrTemplateRespVO respVO) {
        respVO.setVersions(BeanUtils.toBean(
                versionMapper.selectListByTemplateId(respVO.getId()), MesProEdhrDhrTemplateVersionRespVO.class));
        respVO.setBindings(BeanUtils.toBean(
                bindingMapper.selectListByTemplateId(respVO.getId()), MesProEdhrDhrTemplateBindingRespVO.class));
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }
}
