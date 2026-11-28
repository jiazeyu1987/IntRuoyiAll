package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class DccRegistrationCertificateCreationDateTest {
    private DccRegistrationCertificateFacts facts(String type) {
        var facts = new DccRegistrationCertificateFacts();
        facts.setCertificateType(type);
        facts.setOwnProduct(false);
        facts.setCertificateStatus("VALID");
        facts.setClassification("II");
        facts.setApprovalDate(LocalDate.of(2026, 11, 27));
        facts.setExpiryDate(LocalDate.of(2027, 1, 20));
        if ("DOMESTIC".equals(type)) {
            facts.setCertificateNo("DATE-TEST");
            facts.setProjectCode("DATE-PROJECT");
            facts.setFirstObtainedDate(LocalDate.of(2026, 11, 26));
            facts.setEffectiveDate(LocalDate.of(2026, 11, 28));
        }
        return facts;
    }

    @Test
    void domesticAndCeCreationRetainExplicitApprovalDate() {
        for (String type : new String[]{"DOMESTIC", "CE"}) {
            var facts = facts(type);
            assertDoesNotThrow(facts::validateCreation);
            assertEquals(facts.getApprovalDate(), DccRegistrationCertificateFacts.parse(facts.toJson()).getApprovalDate());
        }
    }

    @Test
    void otherDomesticCreationRequiresActualProjectCodeButOwnProductUsesSystemProject() {
        var facts = facts("DOMESTIC");
        facts.setProjectCode("  ");
        assertEquals("请填写实际项目代码", assertThrows(RuntimeException.class, facts::validateCreation).getMessage());
        facts.setOwnProduct(true);
        assertDoesNotThrow(facts::validateCreation);
        assertDoesNotThrow(facts("CE")::validateCreation);
    }

    @Test
    void missingApprovalDateCannotCreateEitherType() {
        for (String type : new String[]{"DOMESTIC", "CE"}) {
            var facts = facts(type);
            facts.setApprovalDate(null);
            assertEquals("请填写批准日期", assertThrows(RuntimeException.class, facts::validateCreation).getMessage());
        }
    }

    @Test
    void domesticApprovalMustNotFollowEffectiveDate() {
        var facts = facts("DOMESTIC");
        facts.setApprovalDate(LocalDate.of(2026, 11, 29));
        assertEquals("批准日期不能晚于生效日期", assertThrows(RuntimeException.class, facts::validateCreation).getMessage());
    }

    @Test
    void ceApprovalMustPrecedeExpiryWithoutIndependentEffectiveDate() {
        var facts = facts("CE");
        assertNull(facts.getEffectiveDate());
        facts.setApprovalDate(facts.getExpiryDate());
        assertEquals("批准日期必须早于有效期至", assertThrows(RuntimeException.class, facts::validateCreation).getMessage());
    }
}
