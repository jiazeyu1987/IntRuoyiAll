package cn.iocoder.yudao.module.dcc.registrationcertificate;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class DccRegistrationCertificateFactsTest {
 @Test void traditional510kRequiresNumberForUploadAndReplacementWhileExemptDoesNot(){
  var f=base("FDA");f.setSubmissionType("TRADITIONAL_510K");f.setCustomerHeld(true);
  f.setApprovalLetterStatus("VALID");f.setFactoryRegistrationStatus("ACTIVE");
  for(String number:new String[]{null,"","   "}){f.setKpNumber(number);
   assertEquals("请填写K/P号",assertThrows(ServiceException.class,f::validate).getMessage());
   assertEquals("请填写K/P号",assertThrows(ServiceException.class,f::validateForUpload).getMessage());}
  f.setKpNumber("K001");assertDoesNotThrow(f::validateForUpload);
  f.setSubmissionType("EXEMPT");f.setKpNumber(null);assertDoesNotThrow(f::validateForUpload);
 }
 @Test void domesticUploadRequiresApprovalDateWithoutInventingIt(){
  var f=base("DOMESTIC");f.setCertificateNo("CERT");f.setFirstObtainedDate(LocalDate.of(2020,1,1));
  f.setEffectiveDate(LocalDate.of(2026,1,1));f.setExpiryDate(LocalDate.of(2027,1,1));
  assertEquals("请填写批准日期",assertThrows(ServiceException.class,f::validateForUpload).getMessage());
  assertNull(f.getApprovalDate());f.setApprovalDate(LocalDate.of(2025,12,1));assertDoesNotThrow(f::validateForUpload);
 }

 @Test void fdaHasNoInventedEffectiveOrExpiryDates(){
  var facts=base("FDA");facts.setSubmissionType("TRADITIONAL_510K");facts.setKpNumber("K001");facts.setCustomerHeld(true);
  facts.setApprovalLetterStatus("VALID");facts.setFactoryRegistrationStatus("ACTIVE");facts.setCertificateHolder("Customer A");
  assertDoesNotThrow(facts::validate);var saved=DccRegistrationCertificateFacts.parse(facts.toJson());
  assertNull(saved.getEffectiveDate());assertNull(saved.getExpiryDate());assertEquals("Customer A",saved.getCertificateHolder());
 }
 @Test void ceRevisionNumberAndIssueDateAreIndependentlyOptional(){
  var facts=base("CE");facts.setFirstObtainedDate(LocalDate.of(2020,1,1));facts.setExpiryDate(LocalDate.of(2030,1,1));var first=new DccRegistrationCertificateFacts.CeCertificate();first.setRevision("Rev.0");
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
 @Test void ceInitialDateIsOptionalAndModelEntriesAreExplicitDistinctFacts(){
  var f=base("CE");f.setExpiryDate(LocalDate.of(2030,1,1));
  assertDoesNotThrow(f::validate);
  assertNull(f.getFirstObtainedDate());
  f.setFirstObtainedDate(LocalDate.of(2020,1,1));f.setModelSpecifications(List.of(" A ","B"));
  assertDoesNotThrow(f::validate);assertEquals(List.of("A","B"),f.getModelSpecifications());
  assertNull(f.getEffectiveDate());assertNull(f.getCeCertificates());
  f.setModelSpecifications(List.of("A"," A "));assertThrows(ServiceException.class,f::validate);
  f.setModelSpecifications(List.of(" "));assertThrows(ServiceException.class,f::validate);
 }
 @Test void fdaRejectsHiddenDomesticDates(){var f=base("FDA");f.setExpiryDate(LocalDate.of(2020,1,1));assertThrows(ServiceException.class,f::validate);assertFalse(f.isExpired(LocalDate.now()));}
 @Test void missingMigrationIsAnError(){assertThrows(ServiceException.class,()->DccRegistrationCertificateFacts.parse(null));assertThrows(ServiceException.class,()->DccRegistrationCertificateFacts.parse(""));}
 @Test void otherDomesticProjectCodeIsAnIndependentStatedFact(){
  var f=base("DOMESTIC");f.setCertificateNo("OTHER001");f.setFirstObtainedDate(LocalDate.of(2026,1,1));f.setEffectiveDate(LocalDate.of(2026,2,1));f.setExpiryDate(LocalDate.of(2028,2,1));f.setProjectCode("CUSTOMER-PRJ-01");
  assertDoesNotThrow(f::validate);assertEquals("CUSTOMER-PRJ-01",DccRegistrationCertificateFacts.parse(f.toJson()).getProjectCode());
  f.setProjectCode(null);assertDoesNotThrow(f::validate);
 }
 @Test void textLimitsAcceptBoundaryAndReportTheOffendingField() {
  var facts=base("CE");facts.setExpiryDate(LocalDate.of(2030,1,1));
  facts.setCertificateNo("字".repeat(128));facts.setClassification("字".repeat(64));facts.setRemark("字".repeat(1024));
  assertDoesNotThrow(facts::validate);
  facts.setCertificateNo("字".repeat(129));assertEquals("注册证号长度不能超过128个字符",assertThrows(ServiceException.class,facts::validate).getMessage());facts.setCertificateNo(null);
  facts.setClassification("字".repeat(65));assertEquals("产品分类长度不能超过64个字符",assertThrows(ServiceException.class,facts::validate).getMessage());facts.setClassification("II");
  facts.setRemark("字".repeat(1025));assertEquals("备注长度不能超过1024个字符",assertThrows(ServiceException.class,facts::validate).getMessage());
 }
 @Test void validatesEveryCeCertificateNumberBeforeProjection() {
  var facts=base("CE");facts.setExpiryDate(LocalDate.of(2030,1,1));
  var first=new DccRegistrationCertificateFacts.CeCertificate();first.setRevision("Rev.0");first.setCertificateNo("字".repeat(128));
  var last=new DccRegistrationCertificateFacts.CeCertificate();last.setRevision("Rev.1");last.setCertificateNo("VALID");facts.setCeCertificates(List.of(first,last));
  assertDoesNotThrow(facts::validate);first.setCertificateNo("字".repeat(129));
  assertEquals("第1条CE证书编号长度不能超过128个字符",assertThrows(ServiceException.class,facts::validate).getMessage());
 }
 private static DccRegistrationCertificateFacts base(String type){var f=new DccRegistrationCertificateFacts();f.setCertificateType(type);f.setOwnProduct(false);f.setClassification("II");if(!"FDA".equals(type))f.setCertificateStatus("VALID");return f;}
}
