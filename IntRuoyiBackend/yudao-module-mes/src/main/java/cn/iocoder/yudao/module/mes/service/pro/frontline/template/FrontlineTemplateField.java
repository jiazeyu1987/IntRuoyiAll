package cn.iocoder.yudao.module.mes.service.pro.frontline.template;

import java.util.List;
import java.util.Objects;

public record FrontlineTemplateField(String code, String name, String valueType, boolean required,
                                     List<String> options) {

    public FrontlineTemplateField {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(valueType, "valueType");
        Objects.requireNonNull(options, "options");
        options = List.copyOf(options);
    }
}
