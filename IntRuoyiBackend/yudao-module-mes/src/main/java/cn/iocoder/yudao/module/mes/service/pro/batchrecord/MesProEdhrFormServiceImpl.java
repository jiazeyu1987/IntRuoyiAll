package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormCreateInstanceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormInstancePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormInstanceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormInstanceSaveDraftReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormInstanceSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormTemplateRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrFormEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrFormInstanceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrFormTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrFormValueDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrFormEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrFormInstanceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrFormTemplateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrFormValueMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormErrorCodeConstants.PRO_EDHR_FORM_FIELD_DATE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormErrorCodeConstants.PRO_EDHR_FORM_FIELD_ENUM_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormErrorCodeConstants.PRO_EDHR_FORM_FIELD_RANGE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormErrorCodeConstants.PRO_EDHR_FORM_FIELD_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormErrorCodeConstants.PRO_EDHR_FORM_FIELD_SCHEMA_EMPTY;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormErrorCodeConstants.PRO_EDHR_FORM_FIELD_SCHEMA_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormErrorCodeConstants.PRO_EDHR_FORM_FIELD_TEXT_TOO_LONG;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormErrorCodeConstants.PRO_EDHR_FORM_INSTANCE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormErrorCodeConstants.PRO_EDHR_FORM_INSTANCE_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormErrorCodeConstants.PRO_EDHR_FORM_TEMPLATE_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormErrorCodeConstants.PRO_EDHR_FORM_TEMPLATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormErrorCodeConstants.PRO_EDHR_FORM_TEMPLATE_STATUS_INVALID;

@Service
@Validated
public class MesProEdhrFormServiceImpl implements MesProEdhrFormService {

    private static final String TEMPLATE_STATUS_DRAFT = "DRAFT";
    private static final String TEMPLATE_STATUS_ACTIVE = "ACTIVE";
    private static final String INSTANCE_STATUS_DRAFT = "DRAFT";
    private static final String INSTANCE_STATUS_SUBMITTED = "SUBMITTED";
    private static final String FIELD_TYPE_TEXT = "text";
    private static final String FIELD_TYPE_NUMBER = "number";
    private static final String FIELD_TYPE_ENUM = "enum";
    private static final String FIELD_TYPE_DATE = "date";
    private static final String EVENT_TEMPLATE_CREATE = "TEMPLATE_CREATE";
    private static final String EVENT_TEMPLATE_ACTIVATE = "TEMPLATE_ACTIVATE";
    private static final String EVENT_INSTANCE_CREATE = "INSTANCE_CREATE";
    private static final String EVENT_DRAFT_SAVE = "DRAFT_SAVE";
    private static final String EVENT_SUBMIT = "SUBMIT";
    private static final String EVENT_RESULT_SUCCESS = "SUCCESS";
    private static final DateTimeFormatter INSTANCE_CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final DateTimeFormatter DATE_VALUE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Resource
    private MesProEdhrFormTemplateMapper templateMapper;
    @Resource
    private MesProEdhrFormInstanceMapper instanceMapper;
    @Resource
    private MesProEdhrFormValueMapper valueMapper;
    @Resource
    private MesProEdhrFormEventMapper eventMapper;

    @Override
    public PageResult<MesProEdhrFormTemplateRespVO> getTemplatePage(MesProEdhrFormTemplatePageReqVO reqVO) {
        return BeanUtils.toBean(templateMapper.selectPage(reqVO), MesProEdhrFormTemplateRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrFormTemplateRespVO createTemplate(MesProEdhrFormTemplateCreateReqVO reqVO) {
        String templateCode = StrUtil.trim(reqVO.getTemplateCode());
        if (templateMapper.selectByTemplateCode(templateCode) != null) {
            throw exception(PRO_EDHR_FORM_TEMPLATE_CODE_DUPLICATE);
        }
        String fieldSchemaJson = normalizeFieldSchema(reqVO.getFieldSchemaJson());
        MesProEdhrFormTemplateDO template = new MesProEdhrFormTemplateDO()
                .setTemplateCode(templateCode)
                .setTemplateName(StrUtil.trim(reqVO.getTemplateName()))
                .setTemplateVersion(StrUtil.trim(reqVO.getTemplateVersion()))
                .setFieldSchemaJson(fieldSchemaJson)
                .setStatus(TEMPLATE_STATUS_DRAFT)
                .setRemark(reqVO.getRemark());
        templateMapper.insert(template);
        recordEvent(null, template.getId(), null, EVENT_TEMPLATE_CREATE, EVENT_RESULT_SUCCESS, null, null);
        return BeanUtils.toBean(template, MesProEdhrFormTemplateRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrFormTemplateRespVO activateTemplate(MesProEdhrFormActivateReqVO reqVO) {
        MesProEdhrFormTemplateDO template = requireTemplate(reqVO.getId());
        if (!TEMPLATE_STATUS_DRAFT.equals(template.getStatus()) && !TEMPLATE_STATUS_ACTIVE.equals(template.getStatus())) {
            throw exception(PRO_EDHR_FORM_TEMPLATE_STATUS_INVALID);
        }
        template.setStatus(TEMPLATE_STATUS_ACTIVE)
                .setActiveBy(SecurityFrameworkUtils.getLoginUserId())
                .setActiveAt(now());
        templateMapper.updateById(template);
        recordEvent(null, template.getId(), null, EVENT_TEMPLATE_ACTIVATE, EVENT_RESULT_SUCCESS, null, null);
        return BeanUtils.toBean(template, MesProEdhrFormTemplateRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrFormInstanceRespVO> getInstancePage(MesProEdhrFormInstancePageReqVO reqVO) {
        return BeanUtils.toBean(instanceMapper.selectPage(reqVO), MesProEdhrFormInstanceRespVO.class);
    }

    @Override
    public MesProEdhrFormInstanceRespVO getInstance(Long id) {
        MesProEdhrFormInstanceDO instance = requireInstance(id);
        MesProEdhrFormTemplateDO template = requireTemplate(instance.getTemplateId());
        return toInstanceResp(instance, template, loadValues(instance.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrFormInstanceRespVO createInstance(MesProEdhrFormCreateInstanceReqVO reqVO) {
        MesProEdhrFormTemplateDO template = requireActiveTemplate(reqVO.getTemplateId());
        MesProEdhrFormInstanceDO instance = new MesProEdhrFormInstanceDO()
                .setInstanceCode(generateInstanceCode(template.getId()))
                .setTemplateId(template.getId())
                .setTemplateCode(template.getTemplateCode())
                .setTemplateName(template.getTemplateName())
                .setTemplateVersion(template.getTemplateVersion())
                .setStatus(INSTANCE_STATUS_DRAFT)
                .setVersion(1)
                .setBusinessScope(StrUtil.emptyToNull(StrUtil.trim(reqVO.getBusinessScope())))
                .setBusinessObjectType(StrUtil.emptyToNull(StrUtil.trim(reqVO.getBusinessObjectType())))
                .setBusinessObjectId(reqVO.getBusinessObjectId())
                .setBusinessObjectCode(StrUtil.emptyToNull(StrUtil.trim(reqVO.getBusinessObjectCode())))
                .setRemark(reqVO.getRemark());
        instanceMapper.insert(instance);
        recordEvent(instance.getId(), template.getId(), instance.getInstanceCode(),
                EVENT_INSTANCE_CREATE, EVENT_RESULT_SUCCESS, null, null);
        return toInstanceResp(instance, template, Map.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrFormInstanceRespVO saveDraft(MesProEdhrFormInstanceSaveDraftReqVO reqVO) {
        MesProEdhrFormInstanceDO instance = requireInstance(reqVO.getId());
        assertDraftEditable(instance);
        MesProEdhrFormTemplateDO template = requireTemplate(instance.getTemplateId());
        List<MesProEdhrFormFieldSpec> fieldSpecs = parseFieldSchema(template.getFieldSchemaJson());
        Map<String, Object> values = valuesOrEmpty(reqVO.getValues());
        validateDraftValues(fieldSpecs, values);
        replaceInstanceValues(instance.getId(), fieldSpecs, values);
        instance.setRemark(reqVO.getRemark());
        instanceMapper.updateById(instance);
        recordEvent(instance.getId(), template.getId(), instance.getInstanceCode(),
                EVENT_DRAFT_SAVE, EVENT_RESULT_SUCCESS, null, null);
        return toInstanceResp(instance, template, values);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrFormInstanceRespVO submit(MesProEdhrFormInstanceSubmitReqVO reqVO) {
        MesProEdhrFormInstanceDO instance = requireInstance(reqVO.getId());
        assertDraftEditable(instance);
        MesProEdhrFormTemplateDO template = requireTemplate(instance.getTemplateId());
        List<MesProEdhrFormFieldSpec> fieldSpecs = parseFieldSchema(template.getFieldSchemaJson());
        Map<String, Object> values = valuesOrEmpty(reqVO.getValues());
        validateSubmissionValues(fieldSpecs, values);
        replaceInstanceValues(instance.getId(), fieldSpecs, values);
        instance.setStatus(INSTANCE_STATUS_SUBMITTED)
                .setVersion(instance.getVersion() == null ? 2 : instance.getVersion() + 1)
                .setSubmittedBy(SecurityFrameworkUtils.getLoginUserId())
                .setSubmittedAt(now())
                .setRemark(reqVO.getRemark());
        instanceMapper.updateById(instance);
        recordEvent(instance.getId(), template.getId(), instance.getInstanceCode(),
                EVENT_SUBMIT, EVENT_RESULT_SUCCESS, null, null);
        return toInstanceResp(instance, template, values);
    }

    @Override
    public PageResult<MesProEdhrFormEventRespVO> getEventPage(MesProEdhrFormEventPageReqVO reqVO) {
        return BeanUtils.toBean(eventMapper.selectPage(reqVO), MesProEdhrFormEventRespVO.class);
    }

    private MesProEdhrFormTemplateDO requireTemplate(Long id) {
        MesProEdhrFormTemplateDO template = id == null ? null : templateMapper.selectById(id);
        if (template == null) {
            throw exception(PRO_EDHR_FORM_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }

    private MesProEdhrFormTemplateDO requireActiveTemplate(Long id) {
        MesProEdhrFormTemplateDO template = requireTemplate(id);
        if (!TEMPLATE_STATUS_ACTIVE.equals(template.getStatus())) {
            throw exception(PRO_EDHR_FORM_TEMPLATE_STATUS_INVALID);
        }
        return template;
    }

    private MesProEdhrFormInstanceDO requireInstance(Long id) {
        MesProEdhrFormInstanceDO instance = id == null ? null : instanceMapper.selectById(id);
        if (instance == null) {
            throw exception(PRO_EDHR_FORM_INSTANCE_NOT_EXISTS);
        }
        return instance;
    }

    private void assertDraftEditable(MesProEdhrFormInstanceDO instance) {
        if (!INSTANCE_STATUS_DRAFT.equals(instance.getStatus())) {
            throw exception(PRO_EDHR_FORM_INSTANCE_STATUS_INVALID);
        }
    }

    private String normalizeFieldSchema(String fieldSchemaJson) {
        List<MesProEdhrFormFieldSpec> fields = parseFieldSchema(fieldSchemaJson);
        return JsonUtils.toJsonString(fields);
    }

    private List<MesProEdhrFormFieldSpec> parseFieldSchema(String fieldSchemaJson) {
        List<MesProEdhrFormFieldSpec> fields = JsonUtils.parseArray(fieldSchemaJson, MesProEdhrFormFieldSpec.class);
        if (fields.isEmpty()) {
            throw exception(PRO_EDHR_FORM_FIELD_SCHEMA_EMPTY);
        }
        Set<String> fieldKeys = new HashSet<>();
        for (MesProEdhrFormFieldSpec field : fields) {
            validateFieldSpec(field);
            if (!fieldKeys.add(field.getKey())) {
                throw exception(PRO_EDHR_FORM_FIELD_SCHEMA_INVALID, "字段 key 重复：" + field.getKey());
            }
        }
        return fields;
    }

    private void validateFieldSpec(MesProEdhrFormFieldSpec field) {
        if (field == null) {
            throw exception(PRO_EDHR_FORM_FIELD_SCHEMA_INVALID, "字段 key、label、type 必填");
        }
        String key = StrUtil.trim(field.getKey());
        String label = StrUtil.trim(field.getLabel());
        String type = StrUtil.trim(field.getType());
        if (StrUtil.isBlank(key) || StrUtil.isBlank(label) || StrUtil.isBlank(type)) {
            throw exception(PRO_EDHR_FORM_FIELD_SCHEMA_INVALID, "字段 key、label、type 必填");
        }
        type = type.toLowerCase(Locale.ROOT);
        if (!FIELD_TYPE_TEXT.equals(type) && !FIELD_TYPE_NUMBER.equals(type)
                && !FIELD_TYPE_ENUM.equals(type) && !FIELD_TYPE_DATE.equals(type)) {
            throw exception(PRO_EDHR_FORM_FIELD_SCHEMA_INVALID, label);
        }
        field.setKey(key);
        field.setLabel(label);
        field.setType(type);
        if (FIELD_TYPE_NUMBER.equals(type) && field.getMin() != null && field.getMax() != null
                && field.getMin().compareTo(field.getMax()) > 0) {
            throw exception(PRO_EDHR_FORM_FIELD_SCHEMA_INVALID, label + " 最小值不能大于最大值");
        }
        if (FIELD_TYPE_ENUM.equals(type)) {
            field.setOptions(normalizeEnumOptions(field));
        }
    }

    private void validateSubmissionValues(List<MesProEdhrFormFieldSpec> fieldSpecs, Map<String, Object> values) {
        validateDraftValues(fieldSpecs, values);
        for (MesProEdhrFormFieldSpec fieldSpec : fieldSpecs) {
            Object value = values.get(fieldSpec.getKey());
            validateRequiredField(fieldSpec, value);
        }
    }

    private void validateDraftValues(List<MesProEdhrFormFieldSpec> fieldSpecs, Map<String, Object> values) {
        validateKnownFields(fieldSpecs, values);
        for (MesProEdhrFormFieldSpec fieldSpec : fieldSpecs) {
            Object value = values.get(fieldSpec.getKey());
            validateNumberRange(fieldSpec, value);
            validateEnumOptions(fieldSpec, value);
            validateDateFormat(fieldSpec, value);
            validateValueTextLength(fieldSpec, value);
        }
    }

    private void validateKnownFields(List<MesProEdhrFormFieldSpec> fieldSpecs, Map<String, Object> values) {
        Set<String> fieldKeys = new HashSet<>();
        for (MesProEdhrFormFieldSpec fieldSpec : fieldSpecs) {
            fieldKeys.add(fieldSpec.getKey());
        }
        for (String fieldKey : values.keySet()) {
            if (!fieldKeys.contains(fieldKey)) {
                throw exception(PRO_EDHR_FORM_FIELD_SCHEMA_INVALID, fieldKey);
            }
        }
    }

    private void validateRequiredField(MesProEdhrFormFieldSpec fieldSpec, Object value) {
        if (Boolean.TRUE.equals(fieldSpec.getRequired()) && isBlankValue(value)) {
            throw exception(PRO_EDHR_FORM_FIELD_REQUIRED, fieldSpec.getLabel());
        }
    }

    private void validateNumberRange(MesProEdhrFormFieldSpec fieldSpec, Object value) {
        if (!FIELD_TYPE_NUMBER.equals(fieldSpec.getType()) || isBlankValue(value)) {
            return;
        }
        BigDecimal decimal;
        try {
            decimal = new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw exception(PRO_EDHR_FORM_FIELD_RANGE_INVALID, fieldSpec.getLabel());
        }
        if (fieldSpec.getMin() != null && decimal.compareTo(fieldSpec.getMin()) < 0) {
            throw exception(PRO_EDHR_FORM_FIELD_RANGE_INVALID, fieldSpec.getLabel());
        }
        if (fieldSpec.getMax() != null && decimal.compareTo(fieldSpec.getMax()) > 0) {
            throw exception(PRO_EDHR_FORM_FIELD_RANGE_INVALID, fieldSpec.getLabel());
        }
    }

    private void validateEnumOptions(MesProEdhrFormFieldSpec fieldSpec, Object value) {
        if (!FIELD_TYPE_ENUM.equals(fieldSpec.getType()) || isBlankValue(value)) {
            return;
        }
        List<String> options = fieldSpec.getOptions();
        if (options == null || options.isEmpty() || !options.contains(String.valueOf(value))) {
            throw exception(PRO_EDHR_FORM_FIELD_ENUM_INVALID, fieldSpec.getLabel());
        }
    }

    private void validateDateFormat(MesProEdhrFormFieldSpec fieldSpec, Object value) {
        if (!FIELD_TYPE_DATE.equals(fieldSpec.getType()) || isBlankValue(value)) {
            return;
        }
        try {
            LocalDate.parse(String.valueOf(value), DATE_VALUE_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw exception(PRO_EDHR_FORM_FIELD_DATE_INVALID, fieldSpec.getLabel());
        }
    }

    private void validateValueTextLength(MesProEdhrFormFieldSpec fieldSpec, Object value) {
        if (isBlankValue(value)) {
            return;
        }
        if (String.valueOf(value).length() > 1000) {
            throw exception(PRO_EDHR_FORM_FIELD_TEXT_TOO_LONG, fieldSpec.getLabel());
        }
    }

    private List<String> normalizeEnumOptions(MesProEdhrFormFieldSpec field) {
        List<String> options = field.getOptions();
        if (options == null || options.isEmpty()) {
            throw exception(PRO_EDHR_FORM_FIELD_SCHEMA_INVALID, field.getLabel() + " 枚举选项不能为空");
        }
        Set<String> normalizedOptionSet = new LinkedHashSet<>();
        for (String option : options) {
            String normalizedOption = StrUtil.trim(option);
            if (StrUtil.isBlank(normalizedOption)) {
                throw exception(PRO_EDHR_FORM_FIELD_SCHEMA_INVALID, field.getLabel() + " 枚举选项不能为空");
            }
            if (!normalizedOptionSet.add(normalizedOption)) {
                throw exception(PRO_EDHR_FORM_FIELD_SCHEMA_INVALID, field.getLabel() + " 枚举选项重复：" + normalizedOption);
            }
        }
        return List.copyOf(normalizedOptionSet);
    }

    private void replaceInstanceValues(Long instanceId, List<MesProEdhrFormFieldSpec> fieldSpecs,
                                       Map<String, Object> values) {
        valueMapper.deleteByInstanceId(instanceId);
        Map<String, MesProEdhrFormFieldSpec> fieldSpecMap = new LinkedHashMap<>();
        for (MesProEdhrFormFieldSpec fieldSpec : fieldSpecs) {
            fieldSpecMap.put(fieldSpec.getKey(), fieldSpec);
        }
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            MesProEdhrFormFieldSpec fieldSpec = fieldSpecMap.get(entry.getKey());
            if (fieldSpec == null) {
                throw exception(PRO_EDHR_FORM_FIELD_SCHEMA_INVALID, entry.getKey());
            }
            valueMapper.insert(new MesProEdhrFormValueDO()
                    .setInstanceId(instanceId)
                    .setFieldKey(fieldSpec.getKey())
                    .setFieldLabel(fieldSpec.getLabel())
                    .setFieldType(fieldSpec.getType())
                    .setValueText(toValueText(entry.getValue()))
                    .setValueJson(entry.getValue() == null ? null : JsonUtils.toJsonString(entry.getValue())));
        }
    }

    private Map<String, Object> loadValues(Long instanceId) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (MesProEdhrFormValueDO value : valueMapper.selectListByInstanceId(instanceId)) {
            values.put(value.getFieldKey(), value.getValueText());
        }
        return values;
    }

    private MesProEdhrFormInstanceRespVO toInstanceResp(MesProEdhrFormInstanceDO instance,
                                                        MesProEdhrFormTemplateDO template,
                                                        Map<String, Object> values) {
        MesProEdhrFormInstanceRespVO respVO = BeanUtils.toBean(instance, MesProEdhrFormInstanceRespVO.class);
        respVO.setFieldSchemaJson(template.getFieldSchemaJson());
        respVO.setValues(values);
        return respVO;
    }

    private void recordEvent(Long instanceId, Long templateId, String instanceCode, String eventType,
                             String resultStatus, String failureReason, String metadataJson) {
        eventMapper.insert(new MesProEdhrFormEventDO()
                .setInstanceId(instanceId)
                .setTemplateId(templateId)
                .setInstanceCode(instanceCode)
                .setEventType(eventType)
                .setResultStatus(resultStatus)
                .setFailureReason(failureReason)
                .setOperatorUserId(SecurityFrameworkUtils.getLoginUserId())
                .setOperatorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setOccurredAt(now())
                .setMetadataJson(metadataJson));
    }

    private String generateInstanceCode(Long templateId) {
        return "EDHR-FORM-" + templateId + "-" + INSTANCE_CODE_FORMATTER.format(LocalDateTime.now());
    }

    private Map<String, Object> valuesOrEmpty(Map<String, Object> values) {
        return values == null ? Map.of() : values;
    }

    private boolean isBlankValue(Object value) {
        return value == null || StrUtil.isBlank(String.valueOf(value));
    }

    private String toValueText(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
