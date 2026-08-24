package cn.iocoder.yudao.module.mes.productionrelease.core;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseApproveReqVO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MesReleaseFinalizationRequestContractTest {

    @Test
    void clientReceiptPayloadsAreIgnoredAndOwnerPortRemainsAuthoritative() throws Exception {
        assertJsonIgnored(MesReleaseFinalizationCommand.class, "independentPrerequisiteReceipt");
        assertJsonIgnored(MesReleaseFinalizationCommand.class, "materialGateReceipt");
        assertJsonIgnored(MesProEdhrReleaseApproveReqVO.class, "independentPrerequisiteReceipt");
        assertJsonIgnored(MesProEdhrReleaseApproveReqVO.class, "materialGateReceipt");
    }

    private void assertJsonIgnored(Class<?> type, String fieldName) throws NoSuchFieldException {
        Field field = type.getDeclaredField(fieldName);
        assertNotNull(field.getAnnotation(JsonIgnore.class),
                type.getSimpleName() + "." + fieldName + " must not be accepted from HTTP");
    }
}
