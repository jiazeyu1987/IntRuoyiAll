package cn.iocoder.yudao.module.dcc.registrationcertificate.service.history;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DccRegistrationCertificateHistoryServiceImpl implements DccRegistrationCertificateHistoryService {

    private final JdbcTemplate jdbcTemplate;

    public DccRegistrationCertificateHistoryServiceImpl(JdbcTemplate jdbcTemplate) {
        if (jdbcTemplate == null) {
            throw new IllegalArgumentException("jdbcTemplate must not be null");
        }
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<DccRegistrationCertificateHistoryItem> listHistory(Long tenantId, Long certificateId) {
        if (tenantId == null || tenantId <= 0 || certificateId == null || certificateId <= 0) {
            throw new IllegalArgumentException("tenantId and certificateId must be positive");
        }
        return jdbcTemplate.query("""
                SELECT e.event_type,
                       COALESCE(i.item_type, e.event_type) AS item_type,
                       COALESCE(i.before_value_json,
                                CASE WHEN e.event_type = 'CERTIFICATE_VOIDED'
                                     THEN '{\"value\":\"ACTIVE\"}' ELSE NULL END) AS before_value_json,
                       COALESCE(i.after_value_json, e.detail_json) AS after_value_json,
                       e.actor_id
                  FROM dcc_registration_certificate_lifecycle_event e
                  LEFT JOIN dcc_registration_certificate_change c
                    ON c.tenant_id = e.tenant_id AND c.event_id = e.id
                  LEFT JOIN dcc_registration_certificate_change_item i
                    ON i.tenant_id = c.tenant_id AND i.change_id = c.id
                 WHERE e.tenant_id = ? AND e.certificate_id = ?
                 ORDER BY e.event_sequence ASC, COALESCE(i.sort_order, 0) ASC, i.id ASC
                """, (rs, rowNum) -> new DccRegistrationCertificateHistoryItem(
                rs.getString("event_type"),
                rs.getString("item_type"),
                rs.getString("before_value_json"),
                rs.getString("after_value_json"),
                rs.getObject("actor_id", Long.class)), tenantId, certificateId);
    }
}
