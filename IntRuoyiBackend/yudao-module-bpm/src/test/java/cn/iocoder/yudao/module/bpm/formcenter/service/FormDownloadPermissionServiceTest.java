package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormPermissionSet;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormResourcePermission;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormDownloadPermissionServiceTest {

    @Test
    void templatePoolMetadataVisibilityDoesNotGrantSourceFileDownload() {
        FormDownloadPermissionService service = new FormDownloadPermissionService();
        FormPermissionSet permissions = FormPermissionSet.of(FormResourcePermission.TEMPLATE_POOL_METADATA_QUERY);

        assertDoesNotThrow(() -> service.assertTemplatePoolMetadataVisible(permissions));
        FormCenterException ex = assertThrows(FormCenterException.class,
                () -> service.assertTemplateSourceDownload(permissions));

        assertEquals(FormCenterErrorCode.DOWNLOAD_PERMISSION_DENIED, ex.getErrorCode());
    }

    @Test
    void instanceExportPermissionDoesNotGrantControlledFileDownload() {
        FormDownloadPermissionService service = new FormDownloadPermissionService();
        FormPermissionSet permissions = FormPermissionSet.of(
                FormResourcePermission.FORM_INSTANCE_VIEW,
                FormResourcePermission.FORM_INSTANCE_EXPORT_PRINT);

        assertDoesNotThrow(() -> service.assertFormExportPrint(permissions));
        FormCenterException ex = assertThrows(FormCenterException.class,
                () -> service.assertControlledFileDownload(permissions));

        assertEquals(FormCenterErrorCode.DOWNLOAD_PERMISSION_DENIED, ex.getErrorCode());
    }

}
