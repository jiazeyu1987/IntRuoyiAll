package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;

import java.util.List;

@Data
public class MesProEdhrRecordbookTagPolicy {

    private Boolean required;

    private List<String> allowedTagCodes;
}
