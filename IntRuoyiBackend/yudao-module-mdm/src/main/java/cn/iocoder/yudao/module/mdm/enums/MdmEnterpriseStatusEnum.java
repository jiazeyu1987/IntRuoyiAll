package cn.iocoder.yudao.module.mdm.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum MdmEnterpriseStatusEnum {

    ENABLE("ENABLE"),
    DISABLE("DISABLE");

    private final String status;

    MdmEnterpriseStatusEnum(String status) {
        this.status = status;
    }

    public static boolean isValid(String status) {
        return Arrays.stream(values()).anyMatch(item -> item.status.equals(status));
    }

}
