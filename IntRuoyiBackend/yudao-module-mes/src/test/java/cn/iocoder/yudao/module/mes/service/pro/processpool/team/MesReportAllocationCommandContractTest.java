package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesReportAllocationCommandContractTest {

    @Test
    void commandServiceMustExposeCurrentPreviewSaveAndAudit() throws Exception {
        Class<?> service = Class.forName(
                "cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationCommandService");
        Class<?> command = Class.forName(
                "cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationSaveCommand");
        Class<?> snapshot = Class.forName(
                "cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationSnapshot");
        assertEquals(snapshot,
                service.getMethod("getCurrent", Long.class, Long.class, String.class).getReturnType());
        assertEquals(snapshot,
                service.getMethod("previewFifo", Long.class, Long.class, String.class).getReturnType());
        assertEquals(snapshot,
                service.getMethod("save", command).getReturnType());
    }
}
