package cn.iocoder.yudao.module.mes.service.pro.frontline.template;

import java.util.List;
import java.util.Objects;

public record FrontlineTemplateDefinition(String code, String name, String type, boolean editableSubmitTime,
                                          List<FrontlineTemplateField> fields) {

    public FrontlineTemplateDefinition {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(fields, "fields");
        fields = List.copyOf(fields);
    }
}
