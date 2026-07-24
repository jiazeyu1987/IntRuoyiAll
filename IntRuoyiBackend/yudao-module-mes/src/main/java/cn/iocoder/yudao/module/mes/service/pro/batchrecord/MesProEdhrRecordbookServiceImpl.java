package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrControlledTagDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordbookDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordbookEntryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordbookEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordbookTagBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordbookTemplateDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrControlledTagMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordbookEntryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordbookEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordbookMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordbookTagBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordbookTemplateMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_ENTRY_CONTENT_EMPTY;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_ENTRY_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_ENTRY_SCHEMA_EMPTY;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_ENTRY_SCHEMA_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_ENTRY_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_FIELD_ENUM_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_FIELD_RANGE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_FIELD_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_IDEMPOTENCY_KEY_EMPTY;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_TAG_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_TAG_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_TAG_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_TAG_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_TAG_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_TEMPLATE_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_TEMPLATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookErrorCodeConstants.PRO_EDHR_RECORDBOOK_TEMPLATE_STATUS_INVALID;

@Service
@Validated
public class MesProEdhrRecordbookServiceImpl implements MesProEdhrRecordbookService {

    private static final String RECORD_BOOK_TEMPLATE_STATUS_DRAFT = "DRAFT";
    private static final String RECORD_BOOK_TEMPLATE_STATUS_ACTIVE = "ACTIVE";
    private static final String RECORD_BOOK_STATUS_OPEN = "OPEN";
    private static final String ENTRY_STATUS_DRAFT = "DRAFT";
    private static final String ENTRY_STATUS_SUBMITTED = "SUBMITTED";
    private static final String TAG_STATUS_DRAFT = "DRAFT";
    private static final String TAG_STATUS_ACTIVE = "ACTIVE";
    private static final String TAG_STATUS_DISABLED = "DISABLED";
    private static final String FIELD_TYPE_TEXT = "text";
    private static final String FIELD_TYPE_NUMBER = "number";
    private static final String FIELD_TYPE_ENUM = "enum";
    private static final String FIELD_TYPE_DATE = "date";
    private static final String EVENT_TEMPLATE_CREATE = "TEMPLATE_CREATE";
    private static final String EVENT_TEMPLATE_ACTIVATE = "TEMPLATE_ACTIVATE";
    private static final String EVENT_RECORD_BOOK_CREATE = "RECORD_BOOK_CREATE";
    private static final String EVENT_ENTRY_CREATE = "ENTRY_CREATE";
    private static final String EVENT_DRAFT_SAVE = "DRAFT_SAVE";
    private static final String EVENT_SUBMIT = "SUBMIT";
    private static final String EVENT_TAG_CREATE = "TAG_CREATE";
    private static final String EVENT_TAG_ACTIVATE = "TAG_ACTIVATE";
    private static final String EVENT_TAG_DISABLE = "TAG_DISABLE";
    private static final String EVENT_RESULT_SUCCESS = "SUCCESS";
    private static final DateTimeFormatter CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @Resource
    private MesProEdhrRecordbookTemplateMapper templateMapper;
    @Resource
    private MesProEdhrRecordbookMapper recordbookMapper;
    @Resource
    private MesProEdhrRecordbookEntryMapper entryMapper;
    @Resource
    private MesProEdhrControlledTagMapper tagMapper;
    @Resource
    private MesProEdhrRecordbookTagBindingMapper tagBindingMapper;
    @Resource
    private MesProEdhrRecordbookEventMapper eventMapper;

    @Override
    public PageResult<MesProEdhrRecordbookTemplateRespVO> getTemplatePage(MesProEdhrRecordbookTemplatePageReqVO reqVO) {
        return BeanUtils.toBean(templateMapper.selectPage(reqVO), MesProEdhrRecordbookTemplateRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrRecordbookTemplateRespVO createTemplate(MesProEdhrRecordbookTemplateCreateReqVO reqVO) {
        String templateCode = StrUtil.trim(reqVO.getTemplateCode());
        if (templateMapper.selectByTemplateCode(templateCode) != null) {
            throw exception(PRO_EDHR_RECORDBOOK_TEMPLATE_CODE_DUPLICATE);
        }
        MesProEdhrRecordbookTemplateDO template = new MesProEdhrRecordbookTemplateDO()
                .setTemplateCode(templateCode)
                .setTemplateName(StrUtil.trim(reqVO.getTemplateName()))
                .setTemplateVersion(StrUtil.trim(reqVO.getTemplateVersion()))
                .setRecordbookType(StrUtil.trim(reqVO.getRecordbookType()))
                .setEntrySchemaJson(normalizeEntrySchema(reqVO.getEntrySchemaJson()))
                .setTagPolicyJson(normalizeTagPolicy(reqVO.getTagPolicyJson()))
                .setStatus(RECORD_BOOK_TEMPLATE_STATUS_DRAFT)
                .setRemark(reqVO.getRemark());
        templateMapper.insert(template);
        recordEvent(null, null, EVENT_TEMPLATE_CREATE, null, RECORD_BOOK_TEMPLATE_STATUS_DRAFT,
                EVENT_RESULT_SUCCESS, null, JsonUtils.toJsonString(Map.of("templateCode", templateCode)), null);
        return BeanUtils.toBean(template, MesProEdhrRecordbookTemplateRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrRecordbookTemplateRespVO activateTemplate(MesProEdhrRecordbookTemplateActivateReqVO reqVO) {
        MesProEdhrRecordbookTemplateDO template = requireTemplate(reqVO.getId());
        if (!RECORD_BOOK_TEMPLATE_STATUS_DRAFT.equals(template.getStatus())
                && !RECORD_BOOK_TEMPLATE_STATUS_ACTIVE.equals(template.getStatus())) {
            throw exception(PRO_EDHR_RECORDBOOK_TEMPLATE_STATUS_INVALID);
        }
        String fromStatus = template.getStatus();
        template.setStatus(RECORD_BOOK_TEMPLATE_STATUS_ACTIVE)
                .setActiveBy(SecurityFrameworkUtils.getLoginUserId())
                .setActiveAt(now())
                .setRemark(reqVO.getRemark());
        templateMapper.updateById(template);
        recordEvent(null, null, EVENT_TEMPLATE_ACTIVATE, fromStatus, RECORD_BOOK_TEMPLATE_STATUS_ACTIVE,
                EVENT_RESULT_SUCCESS, null, JsonUtils.toJsonString(Map.of("templateId", template.getId())), null);
        return BeanUtils.toBean(template, MesProEdhrRecordbookTemplateRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrRecordbookRespVO> getRecordbookPage(MesProEdhrRecordbookPageReqVO reqVO) {
        return BeanUtils.toBean(recordbookMapper.selectPage(reqVO), MesProEdhrRecordbookRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrRecordbookRespVO> getMyRecordbookPage(MesProEdhrRecordbookPageReqVO reqVO) {
        reqVO.setOwnerUserId(SecurityFrameworkUtils.getLoginUserId());
        return BeanUtils.toBean(recordbookMapper.selectPage(reqVO), MesProEdhrRecordbookRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrRecordbookRespVO createRecordbook(MesProEdhrRecordbookCreateReqVO reqVO) {
        MesProEdhrRecordbookTemplateDO template = requireActiveTemplate(reqVO.getTemplateId());
        String recordbookCode = StrUtil.isBlank(reqVO.getRecordbookCode())
                ? generateRecordbookCode(template.getId()) : StrUtil.trim(reqVO.getRecordbookCode());
        if (recordbookMapper.selectByRecordbookCode(recordbookCode) != null) {
            throw exception(PRO_EDHR_RECORDBOOK_CODE_DUPLICATE);
        }
        MesProEdhrRecordbookDO recordbook = new MesProEdhrRecordbookDO()
                .setRecordbookCode(recordbookCode)
                .setRecordbookName(StrUtil.trim(reqVO.getRecordbookName()))
                .setTemplateId(template.getId())
                .setTemplateCode(template.getTemplateCode())
                .setTemplateName(template.getTemplateName())
                .setTemplateVersion(template.getTemplateVersion())
                .setRecordbookType(template.getRecordbookType())
                .setStatus(RECORD_BOOK_STATUS_OPEN)
                .setOwnerUserId(reqVO.getOwnerUserId() == null ? SecurityFrameworkUtils.getLoginUserId() : reqVO.getOwnerUserId())
                .setOwnerDeptId(reqVO.getOwnerDeptId() == null ? SecurityFrameworkUtils.getLoginUserDeptId() : reqVO.getOwnerDeptId())
                .setBusinessScope(StrUtil.emptyToNull(StrUtil.trim(reqVO.getBusinessScope())))
                .setBusinessObjectType(StrUtil.emptyToNull(StrUtil.trim(reqVO.getBusinessObjectType())))
                .setBusinessObjectId(reqVO.getBusinessObjectId())
                .setBusinessObjectCode(StrUtil.emptyToNull(StrUtil.trim(reqVO.getBusinessObjectCode())))
                .setOpenedAt(now())
                .setEntryCount(0)
                .setRemark(reqVO.getRemark());
        recordbookMapper.insert(recordbook);
        recordEvent(recordbook.getId(), null, EVENT_RECORD_BOOK_CREATE, null, RECORD_BOOK_STATUS_OPEN,
                EVENT_RESULT_SUCCESS, null, JsonUtils.toJsonString(Map.of("recordbookCode", recordbookCode)), null);
        return BeanUtils.toBean(recordbook, MesProEdhrRecordbookRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrRecordbookEntryRespVO> getEntryPage(MesProEdhrRecordbookEntryPageReqVO reqVO) {
        PageResult<MesProEdhrRecordbookEntryDO> page = entryMapper.selectPage(reqVO);
        List<MesProEdhrRecordbookEntryRespVO> list = new ArrayList<>();
        for (MesProEdhrRecordbookEntryDO entry : page.getList()) {
            MesProEdhrRecordbookTemplateDO template = requireTemplate(entry.getTemplateId());
            list.add(toEntryResp(entry, template, tagBindingMapper.selectListByEntryId(entry.getId())));
        }
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public MesProEdhrRecordbookEntryRespVO getEntry(Long id) {
        MesProEdhrRecordbookEntryDO entry = requireEntry(id);
        MesProEdhrRecordbookTemplateDO template = requireTemplate(entry.getTemplateId());
        return toEntryResp(entry, template, tagBindingMapper.selectListByEntryId(entry.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrRecordbookEntryRespVO createEntry(MesProEdhrRecordbookEntryCreateReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getIdempotencyKey())) {
            throw exception(PRO_EDHR_RECORDBOOK_IDEMPOTENCY_KEY_EMPTY);
        }
        MesProEdhrRecordbookEntryDO existing =
                entryMapper.selectByRecordbookIdAndIdempotencyKey(reqVO.getRecordbookId(), StrUtil.trim(reqVO.getIdempotencyKey()));
        if (existing != null) {
            MesProEdhrRecordbookTemplateDO template = requireTemplate(existing.getTemplateId());
            return toEntryResp(existing, template, tagBindingMapper.selectListByEntryId(existing.getId()));
        }
        MesProEdhrRecordbookDO recordbook = requireOpenRecordbook(reqVO.getRecordbookId());
        MesProEdhrRecordbookTemplateDO template = requireTemplate(recordbook.getTemplateId());
        validateEntryContent(template.getEntrySchemaJson(), reqVO.getEntryContent(), false);
        List<MesProEdhrControlledTagDO> tags = validateControlledTags(template, reqVO.getTagCodes(), false);
        MesProEdhrRecordbookEntryDO entry = new MesProEdhrRecordbookEntryDO()
                .setEntryCode(generateEntryCode(recordbook.getId()))
                .setRecordbookId(recordbook.getId())
                .setRecordbookCode(recordbook.getRecordbookCode())
                .setTemplateId(template.getId())
                .setTemplateCode(template.getTemplateCode())
                .setTemplateVersion(template.getTemplateVersion())
                .setStatus(ENTRY_STATUS_DRAFT)
                .setVersion(1)
                .setEntryTitle(StrUtil.trim(reqVO.getEntryTitle()))
                .setEntryContentJson(JsonUtils.toJsonString(reqVO.getEntryContent()))
                .setTagSnapshotJson(buildTagSnapshotJson(tags))
                .setIdempotencyKey(StrUtil.trim(reqVO.getIdempotencyKey()))
                .setRemark(reqVO.getRemark());
        entryMapper.insert(entry);
        replaceTagBindings(entry, tags);
        recordbook.setEntryCount((recordbook.getEntryCount() == null ? 0 : recordbook.getEntryCount()) + 1);
        recordbookMapper.updateById(recordbook);
        recordEvent(recordbook.getId(), entry.getId(), EVENT_ENTRY_CREATE, null, ENTRY_STATUS_DRAFT,
                EVENT_RESULT_SUCCESS, null, entry.getEntryContentJson(), entry.getIdempotencyKey());
        return toEntryResp(entry, template, tagBindingMapper.selectListByEntryId(entry.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrRecordbookEntryRespVO saveDraft(MesProEdhrRecordbookEntrySaveDraftReqVO reqVO) {
        MesProEdhrRecordbookEntryDO entry = requireEntry(reqVO.getId());
        assertEntryEditable(entry);
        MesProEdhrRecordbookDO recordbook = requireOpenRecordbook(entry.getRecordbookId());
        MesProEdhrRecordbookTemplateDO template = requireTemplate(entry.getTemplateId());
        validateEntryContent(template.getEntrySchemaJson(), reqVO.getEntryContent(), false);
        List<MesProEdhrControlledTagDO> tags = validateControlledTags(template, reqVO.getTagCodes(), false);
        entry.setEntryTitle(StrUtil.trim(reqVO.getEntryTitle()))
                .setEntryContentJson(JsonUtils.toJsonString(reqVO.getEntryContent()))
                .setTagSnapshotJson(buildTagSnapshotJson(tags))
                .setRemark(reqVO.getRemark());
        entryMapper.updateById(entry);
        replaceTagBindings(entry, tags);
        recordEvent(recordbook.getId(), entry.getId(), EVENT_DRAFT_SAVE, ENTRY_STATUS_DRAFT, ENTRY_STATUS_DRAFT,
                EVENT_RESULT_SUCCESS, null, entry.getEntryContentJson(), entry.getIdempotencyKey());
        return toEntryResp(entry, template, tagBindingMapper.selectListByEntryId(entry.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrRecordbookEntryRespVO submit(MesProEdhrRecordbookEntrySubmitReqVO reqVO) {
        MesProEdhrRecordbookEntryDO entry = requireEntry(reqVO.getId());
        assertEntryEditable(entry);
        MesProEdhrRecordbookDO recordbook = requireOpenRecordbook(entry.getRecordbookId());
        MesProEdhrRecordbookTemplateDO template = requireTemplate(entry.getTemplateId());
        validateEntryContent(template.getEntrySchemaJson(), reqVO.getEntryContent(), true);
        List<MesProEdhrControlledTagDO> tags = validateControlledTags(template, reqVO.getTagCodes(), true);
        entry.setEntryContentJson(JsonUtils.toJsonString(reqVO.getEntryContent()))
                .setTagSnapshotJson(buildTagSnapshotJson(tags))
                .setStatus(ENTRY_STATUS_SUBMITTED)
                .setVersion(entry.getVersion() == null ? 2 : entry.getVersion() + 1)
                .setSubmittedBy(SecurityFrameworkUtils.getLoginUserId())
                .setSubmittedAt(now())
                .setLockedAt(now())
                .setRemark(reqVO.getRemark());
        entryMapper.updateById(entry);
        replaceTagBindings(entry, tags);
        recordEvent(recordbook.getId(), entry.getId(), EVENT_SUBMIT, ENTRY_STATUS_DRAFT, ENTRY_STATUS_SUBMITTED,
                EVENT_RESULT_SUCCESS, null, entry.getEntryContentJson(), entry.getIdempotencyKey());
        return toEntryResp(entry, template, tagBindingMapper.selectListByEntryId(entry.getId()));
    }

    @Override
    public PageResult<MesProEdhrRecordbookEventRespVO> getEventPage(MesProEdhrRecordbookEventPageReqVO reqVO) {
        return BeanUtils.toBean(eventMapper.selectPage(reqVO), MesProEdhrRecordbookEventRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrControlledTagRespVO> getTagPage(MesProEdhrControlledTagPageReqVO reqVO) {
        return BeanUtils.toBean(tagMapper.selectPage(reqVO), MesProEdhrControlledTagRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrControlledTagRespVO createTag(MesProEdhrControlledTagCreateReqVO reqVO) {
        String tagCode = StrUtil.trim(reqVO.getTagCode());
        if (tagMapper.selectByTagCode(tagCode) != null) {
            throw exception(PRO_EDHR_RECORDBOOK_TAG_CODE_DUPLICATE);
        }
        MesProEdhrControlledTagDO tag = new MesProEdhrControlledTagDO()
                .setTagCode(tagCode)
                .setTagName(StrUtil.trim(reqVO.getTagName()))
                .setTagType(StrUtil.emptyToNull(StrUtil.trim(reqVO.getTagType())))
                .setTagStatus(TAG_STATUS_DRAFT)
                .setRemark(reqVO.getRemark());
        tagMapper.insert(tag);
        recordEvent(null, null, EVENT_TAG_CREATE, null, TAG_STATUS_DRAFT,
                EVENT_RESULT_SUCCESS, null, JsonUtils.toJsonString(Map.of("tagCode", tagCode)), null);
        return BeanUtils.toBean(tag, MesProEdhrControlledTagRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrControlledTagRespVO activateTag(MesProEdhrControlledTagStatusReqVO reqVO) {
        MesProEdhrControlledTagDO tag = requireTag(reqVO.getId());
        if (!TAG_STATUS_DRAFT.equals(tag.getTagStatus()) && !TAG_STATUS_ACTIVE.equals(tag.getTagStatus())) {
            throw exception(PRO_EDHR_RECORDBOOK_TAG_STATUS_INVALID);
        }
        String fromStatus = tag.getTagStatus();
        tag.setTagStatus(TAG_STATUS_ACTIVE)
                .setActiveBy(SecurityFrameworkUtils.getLoginUserId())
                .setActiveAt(now())
                .setRemark(reqVO.getRemark());
        tagMapper.updateById(tag);
        recordEvent(null, null, EVENT_TAG_ACTIVATE, fromStatus, TAG_STATUS_ACTIVE,
                EVENT_RESULT_SUCCESS, null, JsonUtils.toJsonString(Map.of("tagId", tag.getId())), null);
        return BeanUtils.toBean(tag, MesProEdhrControlledTagRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrControlledTagRespVO disableTag(MesProEdhrControlledTagStatusReqVO reqVO) {
        MesProEdhrControlledTagDO tag = requireTag(reqVO.getId());
        if (!TAG_STATUS_ACTIVE.equals(tag.getTagStatus())) {
            throw exception(PRO_EDHR_RECORDBOOK_TAG_STATUS_INVALID);
        }
        tag.setTagStatus(TAG_STATUS_DISABLED)
                .setDisabledBy(SecurityFrameworkUtils.getLoginUserId())
                .setDisabledAt(now())
                .setRemark(reqVO.getRemark());
        tagMapper.updateById(tag);
        recordEvent(null, null, EVENT_TAG_DISABLE, TAG_STATUS_ACTIVE, TAG_STATUS_DISABLED,
                EVENT_RESULT_SUCCESS, null, JsonUtils.toJsonString(Map.of("tagId", tag.getId())), null);
        return BeanUtils.toBean(tag, MesProEdhrControlledTagRespVO.class);
    }

    private MesProEdhrRecordbookTemplateDO requireTemplate(Long id) {
        MesProEdhrRecordbookTemplateDO template = id == null ? null : templateMapper.selectById(id);
        if (template == null) {
            throw exception(PRO_EDHR_RECORDBOOK_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }

    private MesProEdhrRecordbookTemplateDO requireActiveTemplate(Long id) {
        MesProEdhrRecordbookTemplateDO template = requireTemplate(id);
        if (!RECORD_BOOK_TEMPLATE_STATUS_ACTIVE.equals(template.getStatus())) {
            throw exception(PRO_EDHR_RECORDBOOK_TEMPLATE_STATUS_INVALID);
        }
        return template;
    }

    private MesProEdhrRecordbookDO requireRecordbook(Long id) {
        MesProEdhrRecordbookDO recordbook = id == null ? null : recordbookMapper.selectById(id);
        if (recordbook == null) {
            throw exception(PRO_EDHR_RECORDBOOK_NOT_EXISTS);
        }
        return recordbook;
    }

    private MesProEdhrRecordbookDO requireOpenRecordbook(Long id) {
        MesProEdhrRecordbookDO recordbook = requireRecordbook(id);
        if (!RECORD_BOOK_STATUS_OPEN.equals(recordbook.getStatus())) {
            throw exception(PRO_EDHR_RECORDBOOK_STATUS_INVALID);
        }
        return recordbook;
    }

    private MesProEdhrRecordbookEntryDO requireEntry(Long id) {
        MesProEdhrRecordbookEntryDO entry = id == null ? null : entryMapper.selectById(id);
        if (entry == null) {
            throw exception(PRO_EDHR_RECORDBOOK_ENTRY_NOT_EXISTS);
        }
        return entry;
    }

    private MesProEdhrControlledTagDO requireTag(Long id) {
        MesProEdhrControlledTagDO tag = id == null ? null : tagMapper.selectById(id);
        if (tag == null) {
            throw exception(PRO_EDHR_RECORDBOOK_TAG_NOT_EXISTS);
        }
        return tag;
    }

    private void assertEntryEditable(MesProEdhrRecordbookEntryDO entry) {
        if (!ENTRY_STATUS_DRAFT.equals(entry.getStatus())) {
            throw exception(PRO_EDHR_RECORDBOOK_ENTRY_STATUS_INVALID);
        }
    }

    private String normalizeEntrySchema(String entrySchemaJson) {
        List<MesProEdhrRecordbookFieldSpec> fields = parseEntrySchema(entrySchemaJson);
        return JsonUtils.toJsonString(fields);
    }

    private List<MesProEdhrRecordbookFieldSpec> parseEntrySchema(String entrySchemaJson) {
        List<MesProEdhrRecordbookFieldSpec> fields = JsonUtils.parseArray(entrySchemaJson, MesProEdhrRecordbookFieldSpec.class);
        if (fields.isEmpty()) {
            throw exception(PRO_EDHR_RECORDBOOK_ENTRY_SCHEMA_EMPTY);
        }
        for (MesProEdhrRecordbookFieldSpec field : fields) {
            validateFieldSpec(field);
        }
        return fields;
    }

    private void validateFieldSpec(MesProEdhrRecordbookFieldSpec field) {
        if (field == null || StrUtil.isBlank(field.getKey()) || StrUtil.isBlank(field.getLabel())
                || StrUtil.isBlank(field.getType())) {
            throw exception(PRO_EDHR_RECORDBOOK_ENTRY_SCHEMA_INVALID, "字段 key、label、type 必填");
        }
        String type = field.getType().toLowerCase(Locale.ROOT);
        if (!FIELD_TYPE_TEXT.equals(type) && !FIELD_TYPE_NUMBER.equals(type)
                && !FIELD_TYPE_ENUM.equals(type) && !FIELD_TYPE_DATE.equals(type)) {
            throw exception(PRO_EDHR_RECORDBOOK_ENTRY_SCHEMA_INVALID, field.getLabel());
        }
        field.setKey(StrUtil.trim(field.getKey()));
        field.setLabel(StrUtil.trim(field.getLabel()));
        field.setType(type);
    }

    private String normalizeTagPolicy(String tagPolicyJson) {
        if (StrUtil.isBlank(tagPolicyJson)) {
            return null;
        }
        MesProEdhrRecordbookTagPolicy tagPolicy = JsonUtils.parseObject(tagPolicyJson, MesProEdhrRecordbookTagPolicy.class);
        return JsonUtils.toJsonString(tagPolicy);
    }

    private MesProEdhrRecordbookTagPolicy parseTagPolicy(String tagPolicyJson) {
        if (StrUtil.isBlank(tagPolicyJson)) {
            return new MesProEdhrRecordbookTagPolicy();
        }
        return JsonUtils.parseObject(tagPolicyJson, MesProEdhrRecordbookTagPolicy.class);
    }

    private void validateEntryContent(String entrySchemaJson, Map<String, Object> values, boolean requireComplete) {
        if (values == null || values.isEmpty()) {
            throw exception(PRO_EDHR_RECORDBOOK_ENTRY_CONTENT_EMPTY);
        }
        List<MesProEdhrRecordbookFieldSpec> fieldSpecs = parseEntrySchema(entrySchemaJson);
        Map<String, MesProEdhrRecordbookFieldSpec> fieldSpecMap = new LinkedHashMap<>();
        for (MesProEdhrRecordbookFieldSpec fieldSpec : fieldSpecs) {
            fieldSpecMap.put(fieldSpec.getKey(), fieldSpec);
        }
        for (String key : values.keySet()) {
            if (!fieldSpecMap.containsKey(key)) {
                throw exception(PRO_EDHR_RECORDBOOK_ENTRY_SCHEMA_INVALID, key);
            }
        }
        if (!requireComplete) {
            return;
        }
        for (MesProEdhrRecordbookFieldSpec fieldSpec : fieldSpecs) {
            Object value = values.get(fieldSpec.getKey());
            validateRequiredField(fieldSpec, value);
            validateNumberRange(fieldSpec, value);
            validateEnumOptions(fieldSpec, value);
        }
    }

    private void validateRequiredField(MesProEdhrRecordbookFieldSpec fieldSpec, Object value) {
        if (Boolean.TRUE.equals(fieldSpec.getRequired()) && isBlankValue(value)) {
            throw exception(PRO_EDHR_RECORDBOOK_FIELD_REQUIRED, fieldSpec.getLabel());
        }
    }

    private void validateNumberRange(MesProEdhrRecordbookFieldSpec fieldSpec, Object value) {
        if (!FIELD_TYPE_NUMBER.equals(fieldSpec.getType()) || isBlankValue(value)) {
            return;
        }
        String text = String.valueOf(value);
        if (!NumberUtil.isNumber(text)) {
            throw exception(PRO_EDHR_RECORDBOOK_FIELD_RANGE_INVALID, fieldSpec.getLabel());
        }
        BigDecimal decimal = new BigDecimal(text);
        if (fieldSpec.getMin() != null && decimal.compareTo(fieldSpec.getMin()) < 0) {
            throw exception(PRO_EDHR_RECORDBOOK_FIELD_RANGE_INVALID, fieldSpec.getLabel());
        }
        if (fieldSpec.getMax() != null && decimal.compareTo(fieldSpec.getMax()) > 0) {
            throw exception(PRO_EDHR_RECORDBOOK_FIELD_RANGE_INVALID, fieldSpec.getLabel());
        }
    }

    private void validateEnumOptions(MesProEdhrRecordbookFieldSpec fieldSpec, Object value) {
        if (!FIELD_TYPE_ENUM.equals(fieldSpec.getType()) || isBlankValue(value)) {
            return;
        }
        List<String> options = fieldSpec.getOptions();
        if (options == null || options.isEmpty() || !options.contains(String.valueOf(value))) {
            throw exception(PRO_EDHR_RECORDBOOK_FIELD_ENUM_INVALID, fieldSpec.getLabel());
        }
    }

    private List<MesProEdhrControlledTagDO> validateControlledTags(MesProEdhrRecordbookTemplateDO template,
                                                                   List<String> tagCodes,
                                                                   boolean requiredForSubmit) {
        MesProEdhrRecordbookTagPolicy tagPolicy = parseTagPolicy(template.getTagPolicyJson());
        Set<String> normalizedTagCodes = normalizeTagCodes(tagCodes);
        if (requiredForSubmit && Boolean.TRUE.equals(tagPolicy.getRequired()) && normalizedTagCodes.isEmpty()) {
            throw exception(PRO_EDHR_RECORDBOOK_TAG_REQUIRED);
        }
        List<String> allowedTagCodes = tagPolicy.getAllowedTagCodes() == null ? List.of() : tagPolicy.getAllowedTagCodes();
        List<MesProEdhrControlledTagDO> tags = new ArrayList<>();
        for (String tagCode : normalizedTagCodes) {
            if (!allowedTagCodes.isEmpty() && !allowedTagCodes.contains(tagCode)) {
                throw exception(PRO_EDHR_RECORDBOOK_TAG_INVALID, tagCode);
            }
            MesProEdhrControlledTagDO tag = tagMapper.selectActiveByTagCode(tagCode);
            if (tag == null) {
                throw exception(PRO_EDHR_RECORDBOOK_TAG_INVALID, tagCode);
            }
            tags.add(tag);
        }
        return tags;
    }

    private Set<String> normalizeTagCodes(List<String> tagCodes) {
        Set<String> normalizedTagCodes = new LinkedHashSet<>();
        if (tagCodes == null) {
            return normalizedTagCodes;
        }
        for (String tagCode : tagCodes) {
            if (StrUtil.isBlank(tagCode)) {
                throw exception(PRO_EDHR_RECORDBOOK_TAG_INVALID, tagCode);
            }
            normalizedTagCodes.add(StrUtil.trim(tagCode));
        }
        return normalizedTagCodes;
    }

    private void replaceTagBindings(MesProEdhrRecordbookEntryDO entry, List<MesProEdhrControlledTagDO> tags) {
        tagBindingMapper.deleteByEntryId(entry.getId());
        LocalDateTime boundAt = now();
        for (MesProEdhrControlledTagDO tag : tags) {
            tagBindingMapper.insert(new MesProEdhrRecordbookTagBindingDO()
                    .setEntryId(entry.getId())
                    .setRecordbookId(entry.getRecordbookId())
                    .setTagCode(tag.getTagCode())
                    .setTagName(tag.getTagName())
                    .setTagStatus(tag.getTagStatus())
                    .setBoundBy(SecurityFrameworkUtils.getLoginUserId())
                    .setBoundAt(boundAt));
        }
    }

    private String buildTagSnapshotJson(List<MesProEdhrControlledTagDO> tags) {
        List<Map<String, String>> tagSnapshots = new ArrayList<>();
        for (MesProEdhrControlledTagDO tag : tags) {
            tagSnapshots.add(Map.of(
                    "tagCode", tag.getTagCode(),
                    "tagName", tag.getTagName(),
                    "tagStatus", tag.getTagStatus()));
        }
        return tagSnapshots.isEmpty() ? null : JsonUtils.toJsonString(tagSnapshots);
    }

    private MesProEdhrRecordbookEntryRespVO toEntryResp(MesProEdhrRecordbookEntryDO entry,
                                                        MesProEdhrRecordbookTemplateDO template,
                                                        List<MesProEdhrRecordbookTagBindingDO> tagBindings) {
        MesProEdhrRecordbookEntryRespVO respVO = BeanUtils.toBean(entry, MesProEdhrRecordbookEntryRespVO.class);
        respVO.setEntrySchemaJson(template.getEntrySchemaJson());
        respVO.setEntryContent(parseEntryContent(entry.getEntryContentJson()));
        respVO.setTagCodes(tagCodesFromBindings(tagBindings));
        return respVO;
    }

    private Map<String, Object> parseEntryContent(String entryContentJson) {
        if (StrUtil.isBlank(entryContentJson)) {
            return Map.of();
        }
        Map<String, Object> content = JsonUtils.parseObject(entryContentJson, MAP_TYPE);
        return content == null ? Map.of() : content;
    }

    private List<String> tagCodesFromBindings(List<MesProEdhrRecordbookTagBindingDO> tagBindings) {
        List<String> tagCodes = new ArrayList<>();
        for (MesProEdhrRecordbookTagBindingDO binding : tagBindings) {
            tagCodes.add(binding.getTagCode());
        }
        return tagCodes;
    }

    private void recordEvent(Long recordbookId, Long entryId, String eventType, String fromStatus, String toStatus,
                             String resultStatus, String failureReason, String eventSnapshotJson, String idempotencyKey) {
        eventMapper.insert(new MesProEdhrRecordbookEventDO()
                .setRecordbookId(recordbookId)
                .setEntryId(entryId)
                .setEventType(eventType)
                .setFromStatus(fromStatus)
                .setToStatus(toStatus)
                .setResultStatus(resultStatus)
                .setFailureReason(failureReason)
                .setOperatorUserId(SecurityFrameworkUtils.getLoginUserId())
                .setOperatorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setOccurredAt(now())
                .setEventSnapshotJson(eventSnapshotJson)
                .setIdempotencyKey(idempotencyKey));
    }

    private String generateRecordbookCode(Long templateId) {
        return "EDHR-RB-" + templateId + "-" + CODE_FORMATTER.format(LocalDateTime.now());
    }

    private String generateEntryCode(Long recordbookId) {
        return "EDHR-RBE-" + recordbookId + "-" + CODE_FORMATTER.format(LocalDateTime.now());
    }

    private boolean isBlankValue(Object value) {
        return value == null || StrUtil.isBlank(String.valueOf(value));
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
