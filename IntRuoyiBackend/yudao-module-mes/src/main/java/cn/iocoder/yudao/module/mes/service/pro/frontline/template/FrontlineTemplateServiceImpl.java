package cn.iocoder.yudao.module.mes.service.pro.frontline.template;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_BINDING_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_FIELD_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_PQC_RESULT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_SUBMIT_TIME_FORBIDDEN;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_TEMPLATE_UNSUPPORTED;

@Service
public class FrontlineTemplateServiceImpl implements FrontlineTemplateService {

    private static final String NUMBER = "NUMBER";
    private static final String TEXT = "TEXT";
    private static final String OBJECT = "OBJECT";
    private static final String OPTION = "OPTION";

    private static final Set<String> CLIENT_TIME_FIELDS = Set.of("submitTime", "submittedAt", "feedbackTime");

    private static final List<FrontlineTemplateField> PRODUCTION_FIELDS = List.of(
            field(FrontlineTemplateFieldCodes.PREVIOUS_PROCESS_INPUT_QUANTITY, "上工序输入数量", NUMBER),
            field(FrontlineTemplateFieldCodes.DEVICE, "设备", TEXT),
            field(FrontlineTemplateFieldCodes.DEVICE_PARAMETERS, "设备参数", OBJECT),
            field(FrontlineTemplateFieldCodes.OUTPUT_QUANTITY, "输出数量", NUMBER),
            field(FrontlineTemplateFieldCodes.SCRAP_QUANTITY, "损耗数量", NUMBER)
    );

    private static final List<FrontlineTemplateField> PQC_FIELDS = List.of(
            new FrontlineTemplateField(
                    FrontlineTemplateFieldCodes.PQC_RESULT,
                    "PQC 检测结果",
                    OPTION,
                    true,
                    List.of(FrontlinePqcResults.DETECTION_SUCCESS, FrontlinePqcResults.DETECTION_FAILED))
    );

    private static final List<FrontlineTemplateDefinition> CATALOG = List.of(
            new FrontlineTemplateDefinition(
                    FrontlineTemplateCodes.PRODUCTION_SIMPLIFIED,
                    "生产简化模板",
                    FrontlineTemplateTypes.PRODUCTION,
                    false,
                    PRODUCTION_FIELDS),
            new FrontlineTemplateDefinition(
                    FrontlineTemplateCodes.PQC_SIMPLIFIED,
                    "PQC 简化模板",
                    FrontlineTemplateTypes.PQC,
                    false,
                    PQC_FIELDS)
    );

    private static final Map<String, FrontlineTemplateDefinition> TEMPLATES = CATALOG.stream()
            .collect(Collectors.toUnmodifiableMap(FrontlineTemplateDefinition::code, Function.identity()));

    @Override
    public List<FrontlineTemplateDefinition> listCatalog() {
        return CATALOG;
    }

    @Override
    public FrontlineTemplateDefinition getTemplate(String templateCode) {
        if (isBlank(templateCode)) {
            throw exception(PRO_FRONTLINE_TEMPLATE_BINDING_REQUIRED, null, null, null);
        }
        FrontlineTemplateDefinition template = TEMPLATES.get(templateCode);
        if (template == null) {
            throw exception(PRO_FRONTLINE_TEMPLATE_UNSUPPORTED, templateCode);
        }
        return template;
    }

    @Override
    public FrontlineTemplateDefinition resolveTemplate(FrontlineTemplateResolveCommand command) {
        if (command == null || command.actualEmployeeId() == null
                || command.processId() == null && command.routeProcessId() == null) {
            throw exception(PRO_FRONTLINE_TEMPLATE_CONTEXT_REQUIRED);
        }
        if (isBlank(command.templateCode())) {
            throw exception(PRO_FRONTLINE_TEMPLATE_BINDING_REQUIRED,
                    command.actualEmployeeId(), command.routeProcessId(), command.processId());
        }
        return getTemplate(command.templateCode());
    }

    @Override
    public FrontlineTemplatePayload buildPayload(FrontlineTemplatePayloadCommand command) {
        validatePayloadContext(command);
        FrontlineTemplateDefinition template = resolveTemplate(new FrontlineTemplateResolveCommand(
                command.actualEmployeeId(), command.routeProcessId(), command.processId(), command.templateCode()));
        Map<String, Object> fieldValues = sanitizeFieldValues(template, command.fieldValues());
        return new FrontlineTemplatePayload(
                command.workOrderId(),
                command.routeId(),
                command.processId(),
                command.routeProcessId(),
                command.actualEmployeeId(),
                template.code(),
                fieldValues);
    }

    private void validatePayloadContext(FrontlineTemplatePayloadCommand command) {
        if (command == null || command.workOrderId() == null || command.routeId() == null
                || command.processId() == null || command.routeProcessId() == null
                || command.actualEmployeeId() == null) {
            throw exception(PRO_FRONTLINE_TEMPLATE_CONTEXT_REQUIRED);
        }
    }

    private Map<String, Object> sanitizeFieldValues(FrontlineTemplateDefinition template, Map<String, Object> input) {
        Map<String, Object> source = input == null ? Map.of() : input;
        Set<String> allowedFields = template.fields().stream()
                .map(FrontlineTemplateField::code)
                .collect(Collectors.toUnmodifiableSet());

        for (String fieldCode : source.keySet()) {
            if (CLIENT_TIME_FIELDS.contains(fieldCode)) {
                throw exception(PRO_FRONTLINE_TEMPLATE_SUBMIT_TIME_FORBIDDEN);
            }
            if (!allowedFields.contains(fieldCode)) {
                throw exception(PRO_FRONTLINE_TEMPLATE_FIELD_INVALID, fieldCode);
            }
        }

        LinkedHashMap<String, Object> sanitized = new LinkedHashMap<>();
        for (FrontlineTemplateField field : template.fields()) {
            if (!source.containsKey(field.code()) || isEmptyValue(source.get(field.code()))) {
                throw exception(PRO_FRONTLINE_TEMPLATE_FIELD_INVALID, field.code());
            }
            sanitized.put(field.code(), source.get(field.code()));
        }

        if (FrontlineTemplateCodes.PQC_SIMPLIFIED.equals(template.code())) {
            Object result = sanitized.get(FrontlineTemplateFieldCodes.PQC_RESULT);
            if (!PQC_FIELDS.get(0).options().contains(result)) {
                throw exception(PRO_FRONTLINE_TEMPLATE_PQC_RESULT_INVALID);
            }
        }
        return sanitized;
    }

    private static FrontlineTemplateField field(String code, String name, String valueType) {
        return new FrontlineTemplateField(code, name, valueType, true, List.of());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean isEmptyValue(Object value) {
        return value == null || value instanceof String text && text.trim().isEmpty();
    }
}
