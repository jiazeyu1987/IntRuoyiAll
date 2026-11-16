package cn.iocoder.yudao.module.system.service.sms;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.system.framework.sms.core.client.dto.SmsReceiveRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** External receipts have no login tenant. Resolve their owner from the original sent record. */
@Component
@RequiredArgsConstructor
public class SmsReceiptTenantRouter {
    private final JdbcTemplate jdbcTemplate;

    public void route(String channelCode, SmsReceiveRespDTO receipt, java.util.function.Consumer<Long> update) {
        boolean hasId = receipt.getLogId() != null && receipt.getLogId() > 0;
        if (!hasId && (receipt.getSerialNo() == null || receipt.getSerialNo().isBlank())) {
            throw new IllegalArgumentException("SMS_RECEIPT_IDENTIFIER_REQUIRED");
        }
        List<ReceiptOwner> owners = jdbcTemplate.query(
                "SELECT id,tenant_id FROM system_tenant_sms_log WHERE deleted=0 AND channel_code=? AND "
                        + (hasId ? "id=?" : "api_serial_no=?"),
                (row, index) -> new ReceiptOwner(row.getLong("id"), row.getLong("tenant_id")), channelCode, hasId ? receipt.getLogId() : receipt.getSerialNo());
        if (owners.size() != 1) throw new IllegalStateException("SMS_RECEIPT_OWNER_NOT_UNIQUE");
        ReceiptOwner owner = owners.get(0);
        TenantUtils.execute(owner.tenantId(), () -> update.accept(owner.logId()));
    }
    private record ReceiptOwner(Long logId, Long tenantId) {}
}
