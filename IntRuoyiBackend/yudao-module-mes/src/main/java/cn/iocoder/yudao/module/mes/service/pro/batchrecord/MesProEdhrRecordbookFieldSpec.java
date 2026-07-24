package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MesProEdhrRecordbookFieldSpec {

    private String key;

    private String label;

    private String type;

    private Boolean required;

    private BigDecimal min;

    private BigDecimal max;

    private List<String> options;
}
