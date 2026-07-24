package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPermissionSet;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormResourcePermission;

public class FormDownloadPermissionService {

    public void assertTemplatePoolMetadataVisible(FormPermissionSet permissions) {
        require(permissions, FormResourcePermission.TEMPLATE_POOL_METADATA_QUERY);
    }

    public void assertTemplateSourceDownload(FormPermissionSet permissions) {
        require(permissions, FormResourcePermission.TEMPLATE_SOURCE_DOWNLOAD);
    }

    public void assertFormExportPrint(FormPermissionSet permissions) {
        require(permissions, FormResourcePermission.FORM_INSTANCE_EXPORT_PRINT);
    }

    public void assertControlledFileDownload(FormPermissionSet permissions) {
        require(permissions, FormResourcePermission.CONTROLLED_FILE_DOWNLOAD);
    }

    private void require(FormPermissionSet permissions, FormResourcePermission requiredPermission) {
        if (!permissions.has(requiredPermission)) {
            throw new FormCenterException(FormCenterErrorCode.DOWNLOAD_PERMISSION_DENIED,
                    "Permission denied for resource permission: " + requiredPermission);
        }
    }

}
