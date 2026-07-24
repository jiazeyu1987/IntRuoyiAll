package cn.iocoder.yudao.module.srm.enums.tender;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SrmTenderApplicationMethodEnum {

    DRAW("DRAW", "抽取"),
    DESIGNATE("DESIGNATE", "指定");

    private final String method;
    private final String label;
}
