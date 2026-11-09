package cn.iocoder.yudao.module.dcc.registrationcertificate;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class DccRegistrationCertificateFactsTest {
 @Test void fdaHasNoInventedEffectiveOrExpiryDates(){
  var facts=base("FDA");facts.setSubmissionType("TRADITIONAL_510K");facts.setCustomerHeld(true);
  facts.setApprovalLetterStatus("VALID");facts.setFactoryRegistrationStatus("ACTIVE");facts.setCertificateHolder("Customer A");
  assertDoesNotThrow(facts::validate);var saved=DccRegistrationCertificateFacts.parse(facts.toJson());
  assertNull(saved.getEffectiveDate());assertNull(saved.getExpiryDate());assertEquals("Customer A",saved.getCertificateHolder());
 }
 @Test void ceRevisionNumberAndIssueDateAreIndependentlyOptional(){
  var facts=base("CE");facts.setExpiryDate(LocalDate.of(2030,1,1));var first=new DccRegistrationCertificateFacts.CeCertificate();first.setRevision("Rev.0");
  var second=new DccRegistrationCertificateFacts.CeCertificate();second.setRevision("Rev.1");second.setIssueDate(LocalDate.of(2026,10,30));facts.setCeCertificates(List.of(first,second));
  assertDoesNotThrow(facts::validate);second.setRevision("Rev.0");assertThrows(ServiceException.class,facts::validate);
 }
 @Test void dateOrderAndUntrustedLinksAreRejected(){
  var facts=base("DOMESTIC");facts.setCertificateNo("CERT-01");facts.setFirstObtainedDate(LocalDate.of(2026,1,1));facts.setEffectiveDate(LocalDate.of(2026,2,1));facts.setExpiryDate(LocalDate.of(2027,2,1));
  assertDoesNotThrow(facts::validate);facts.setRegistrationLink("javascript:alert(1)");assertThrows(ServiceException.class,facts::validate);
  facts.setRegistrationLink("https://example.test/certificate");facts.setExpiryDate(facts.getEffectiveDate());assertThrows(ServiceException.class,facts::validate);
 }
 @Test void datesSerializeAsIsoStringsForSqlFiltering(){
  var f=base("CE");f.setExpiryDate(LocalDate.of(2030,1,1));var ce=new DccRegistrationCertificateFacts.CeCertificate();ce.setRevision("Rev.1");ce.setIssueDate(LocalDate.of(2026,10,30));f.setCeCertificates(List.of(ce));
  var json=f.toJson();assertTrue(json.contains("\"expiryDate\":\"2030-01-01\""));assertTrue(json.contains("\"issueDate\":\"2026-10-30\""));assertEquals(f.getExpiryDate(),DccRegistrationCertificateFacts.parse(json).getExpiryDate());
 }
 @Test void fdaRejectsHiddenDomesticDates(){var f=base("FDA");f.setExpiryDate(LocalDate.of(2020,1,1));assertThrows(ServiceException.class,f::validate);assertFalse(f.isExpired(LocalDate.now()));}
 @Test void missingMigrationIsAnError(){assertThrows(ServiceException.class,()->DccRegistrationCertificateFacts.parse(null));assertThrows(ServiceException.class,()->DccRegistrationCertificateFacts.parse(""));}
 @Test void otherDomesticProjectCodeIsAnIndependentStatedFact(){
  var f=base("DOMESTIC");f.setCertificateNo("OTHER001");f.setFirstObtainedDate(LocalDate.of(2026,1,1));f.setEffectiveDate(LocalDate.of(2026,2,1));f.setExpiryDate(LocalDate.of(2028,2,1));f.setProjectCode("CUSTOMER-PRJ-01");
  assertDoesNotThrow(f::validate);assertEquals("CUSTOMER-PRJ-01",DccRegistrationCertificateFacts.parse(f.toJson()).getProjectCode());
  f.setProjectCode(null);assertDoesNotThrow(f::validate);
 }
 private static DccRegistrationCertificateFacts base(String type){var f=new DccRegistrationCertificateFacts();f.setCertificateType(type);f.setOwnProduct(false);f.setClassification("II");if(!"FDA".equals(type))f.setCertificateStatus("VALID");return f;}
}
