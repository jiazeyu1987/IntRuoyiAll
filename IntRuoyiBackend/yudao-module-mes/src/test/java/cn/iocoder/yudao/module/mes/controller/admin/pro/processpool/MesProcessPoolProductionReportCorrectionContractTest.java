package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolProductionReportCorrectionReqVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MesProcessPoolProductionReportCorrectionContractTest {

    @Test
    void requestAcceptsBusinessDataButNotClientOwnedAuditIdentity() throws Exception {
        assertField("eventId");
        assertField("outputQuantity");
        assertField("lossDetails");
        assertField("deviceParameterReadings");
        assertField("changeReason");
        assertField("signaturePassword");

        assertThrows(NoSuchFieldException.class, () -> assertField("modifiedByUserId"));
        assertThrows(NoSuchFieldException.class, () -> assertField("revisionSignatureId"));
        assertThrows(NoSuchFieldException.class, () -> assertField("revisionSignatureUserId"));
        assertThrows(NoSuchFieldException.class, () -> assertField("revisionSignatureSnapshot"));
        assertThrows(NoSuchFieldException.class, () -> assertField("changedFields"));
        assertThrows(NoSuchFieldException.class, () -> assertField("afterPayload"));
    }

    private static Field assertField(String name) throws NoSuchFieldException {
        return ProcessPoolProductionReportCorrectionReqVO.class.getDeclaredField(name);
    }
}
