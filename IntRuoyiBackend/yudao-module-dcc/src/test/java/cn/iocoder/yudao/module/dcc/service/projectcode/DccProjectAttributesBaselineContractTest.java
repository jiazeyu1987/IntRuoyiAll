package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DccProjectAttributesBaselineContractTest {
    @Test void missingDefaultsExplainRequiredFieldsAndMaintenanceEntry() {
        var message = cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.DEFAULTS_MISSING.getMsg();
        for (var required : java.util.List.of("目标市场", "是否为注册人", "是否为生产方", "文件转移",
                "基础数据", "DCC项目代码", "修改项目默认属性与负责人账号")) {
            assertTrue(message.contains(required), "缺失提示未说明：" + required);
        }
    }
    @Test void formalLeaderAndDefaultsMustBePersistableOnProject() {
        assertDoesNotThrow(() -> DccProjectCodeDO.class.getDeclaredField("projectLeaderUserId"));
        assertDoesNotThrow(() -> DccProjectCodeDO.class.getDeclaredField("defaultAttributesJson"));
    }
    @Test void creationRequestMustFreezeDefaultsAndSelectedTemplate() {
        assertDoesNotThrow(() -> DccProjectProductCreateRequestDO.class.getDeclaredField("defaultAttributesJson"));
        assertDoesNotThrow(() -> DccProjectProductCreateRequestDO.class.getDeclaredField("folderTemplateSnapshotJson"));
    }
}
