package cn.iocoder.yudao.module.showroom.foundation.contract;

public final class ShowroomBackendRootContract {

    private static final String MODULE_NAME = "yudao-module-showroom";
    private static final String MODULE_ROOT = "D:/ProjectPackage/Int/IntRuoyi/ruoyi-vue-pro/yudao-module-showroom";

    private ShowroomBackendRootContract() {
    }

    public static String moduleName() {
        return MODULE_NAME;
    }

    public static String moduleRoot() {
        return MODULE_ROOT;
    }

}
