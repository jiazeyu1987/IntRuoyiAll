package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.math.BigDecimal;

public record MesFrontlineDeviceParameterOption(String parameterCode,
                                                String parameterName,
                                                String unit,
                                                BigDecimal lowerLimit,
                                                BigDecimal upperLimit,
                                                BigDecimal defaultValue,
                                                String valueType) {
}
