package cn.iocoder.yudao.module.mdm.enums;

public final class MdmProductStatusConstants {

    public static final String ENABLE = "ENABLE";
    public static final String DISABLE = "DISABLE";

    private MdmProductStatusConstants() {
    }

    public static boolean isValid(String status) {
        return ENABLE.equals(status) || DISABLE.equals(status);
    }

}
