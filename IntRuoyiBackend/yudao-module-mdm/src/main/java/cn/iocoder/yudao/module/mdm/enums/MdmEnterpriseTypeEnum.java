package cn.iocoder.yudao.module.mdm.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum MdmEnterpriseTypeEnum {

    OWNED_COMPANY("OWNED_COMPANY"),
    ENTRUSTED_PARTY("ENTRUSTED_PARTY");

    private final String type;

    MdmEnterpriseTypeEnum(String type) {
        this.type = type;
    }

    public static boolean isValid(String type) {
        return Arrays.stream(values()).anyMatch(item -> item.type.equals(type));
    }

}
