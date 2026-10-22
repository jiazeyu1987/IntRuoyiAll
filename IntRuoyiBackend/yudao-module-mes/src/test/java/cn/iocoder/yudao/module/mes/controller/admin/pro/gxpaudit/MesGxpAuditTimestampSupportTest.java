package cn.iocoder.yudao.module.mes.controller.admin.pro.gxpaudit;

import cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo.GxpAuditEventPageReqVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MesGxpAuditTimestampSupportTest {

    @Test
    void mesQueryConvertsEpochRangeAndPreservesAuthoritativeScope() {
        GxpAuditEventPageReqVO request = new GxpAuditEventPageReqVO()
                .setScopeType("OTHER").setScopeId(999L)
                .setOperationId("mes.production.allocation.save")
                .setOccurredAt(new Long[]{1790858138830L, 1790858138830L});
        var query = MesGxpAuditControllerSupport.toQuery(request, "ACTIVE_ORDER", 1009200409L);
        assertEquals("ACTIVE_ORDER", query.getScopeType());
        assertEquals(1009200409L, query.getScopeId());
        assertEquals("mes.production.allocation.save", query.getOperationId());
        LocalDateTime utc = LocalDateTime.of(2026, 10, 1, 12, 35, 38, 830_000_000);
        assertArrayEquals(new LocalDateTime[]{utc, utc}, query.getOccurredAt());
    }

    @Test
    void invalidRangeCannotBeSilentlyDiscardedOrSentToMapper() {
        GxpAuditEventPageReqVO request = new GxpAuditEventPageReqVO().setOccurredAt(new Long[]{1L});
        assertThrows(IllegalArgumentException.class,
                () -> MesGxpAuditControllerSupport.toQuery(request, "ACTIVE_ORDER", 1009200409L));
    }
}
