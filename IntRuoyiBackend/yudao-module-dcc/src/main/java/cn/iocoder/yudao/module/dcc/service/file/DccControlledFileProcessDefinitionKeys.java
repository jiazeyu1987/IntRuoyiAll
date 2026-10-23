package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.enums.DccControlledFileChangeTypeEnum;

import java.util.List;
import java.util.Set;

public final class DccControlledFileProcessDefinitionKeys {

    public static final String LEGACY_APPROVAL = "dcc-controlled-file-approval";
    public static final String UPLOAD = "dcc-controlled-file-upload";
    public static final String REVISION = "dcc-controlled-file-revision";
    public static final String OBSOLETE = "dcc-controlled-file-obsolete";
    public static final String LEGACY_OBSOLETE_FORM_CENTER = "dcc-controlled-file-obsolete-approval";

    public static final List<String> APPROVAL_CENTER_KEYS = List.of(
            LEGACY_APPROVAL,
            UPLOAD,
            REVISION,
            OBSOLETE,
            LEGACY_OBSOLETE_FORM_CENTER
    );

    public static final Set<String> FORM_CENTER_OBSOLETE_KEYS = Set.of(
            OBSOLETE,
            LEGACY_OBSOLETE_FORM_CENTER
    );

    public static final Set<String> NATIVE_FINALIZATION_KEYS = Set.of(
            LEGACY_APPROVAL,
            UPLOAD,
            REVISION
    );

    public static String toActionType(String processDefinitionKey) {
        if (UPLOAD.equals(processDefinitionKey)) {
            return DccControlledFileChangeTypeEnum.NEW.getCode();
        }
        if (REVISION.equals(processDefinitionKey)) {
            return DccControlledFileChangeTypeEnum.REVISION.getCode();
        }
        if (OBSOLETE.equals(processDefinitionKey)) {
            return DccControlledFileChangeTypeEnum.OBSOLETE.getCode();
        }
        return null;
    }

    private DccControlledFileProcessDefinitionKeys() {
    }
}
