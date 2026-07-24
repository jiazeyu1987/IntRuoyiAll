package cn.iocoder.yudao.module.infra.service.externalwritepermission;

public interface ExternalWritePermissionService {

    String ERP_EXTERNAL_WRITE_ENABLED_CONFIG_KEY = "yudao.erp.kingdee.external-write-enabled";

    boolean isErpExternalWriteEnabled();

    void updateErpExternalWriteEnabled(Boolean enabled);

}
