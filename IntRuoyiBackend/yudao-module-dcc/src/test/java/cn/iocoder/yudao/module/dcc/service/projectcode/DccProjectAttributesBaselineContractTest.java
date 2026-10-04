package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DccProjectAttributesBaselineContractTest {
    @Test void formalLeaderAndDefaultsMustBePersistableOnProject() {
        assertDoesNotThrow(() -> DccProjectCodeDO.class.getDeclaredField("projectLeaderUserId"));
        assertDoesNotThrow(() -> DccProjectCodeDO.class.getDeclaredField("defaultAttributesJson"));
    }
    @Test void creationRequestMustFreezeDefaultsAndSelectedTemplate() {
        assertDoesNotThrow(() -> DccProjectProductCreateRequestDO.class.getDeclaredField("defaultAttributesJson"));
        assertDoesNotThrow(() -> DccProjectProductCreateRequestDO.class.getDeclaredField("folderTemplateSnapshotJson"));
    }
}
