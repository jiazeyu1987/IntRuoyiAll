package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.math.BigDecimal;
import java.util.List;

public record MesFrontlineDeviceParameterOption(String parameterCode,
                                                String parameterName,
                                                String unit,
                                                BigDecimal lowerLimit,
                                                BigDecimal upperLimit,
                                                BigDecimal defaultValue,
                                                String valueType,
                                                String standardText,
                                                List<String> optionValues,
                                                String defaultText,
                                                Integer decimalScale) {
}
