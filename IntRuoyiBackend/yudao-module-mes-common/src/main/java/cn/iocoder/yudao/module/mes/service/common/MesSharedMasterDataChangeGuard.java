package cn.iocoder.yudao.module.mes.service.common;

/** 设备模块通过公共契约检查路线引用，避免反向依赖生产模块。 */
public interface MesSharedMasterDataChangeGuard {
    void assertDirectUpdateAllowed(String subjectType, Long id);
}
