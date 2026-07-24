package cn.iocoder.yudao.module.dcc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DccFileCategoryPermissionScopeEnum {

    GLOBAL("GLOBAL"),
    PRODUCT_GROUP("PRODUCT_GROUP");

    private final String code;

}
