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
   assertEquals("请填写K/P号",assertThrows(ServiceException.class,f::validateCreation).getMessage());}
  f.setKpNumber("K001");assertDoesNotThrow(f::validateCreation);
  f.setSubmissionType("EXEMPT");f.setKpNumber(null);assertDoesNotThrow(f::validateCreation);
 }
 @Test void creationRejectsRemovedFieldsWithoutChangingChangeValidation() {
  var ce=base("CE");ce.setExpiryDate(LocalDate.of(2030,1,1));
  assertDoesNotThrow(ce::validateCreation);
  ce.setFirstObtainedDate(LocalDate.of(2025,1,1));assertThrows(ServiceException.class,ce::validateCreation);assertDoesNotThrow(ce::validate);ce.setFirstObtainedDate(null);
  ce.setModelSpecifications(List.of("A"));assertThrows(ServiceException.class,ce::validateCreation);assertDoesNotThrow(ce::validate);ce.setModelSpecifications(null);
  ce.setApprovalDate(LocalDate.of(2025,1,1));assertThrows(ServiceException.class,ce::validateCreation);ce.setApprovalDate(null);
  ce.setChangeDescription("变更内容");assertThrows(ServiceException.class,ce::validateCreation);
  var domestic=base("DOMESTIC");domestic.setCertificateNo("CN-1");domestic.setFirstObtainedDate(LocalDate.of(2025,1,1));domestic.setEffectiveDate(LocalDate.of(2025,1,2));domestic.setExpiryDate(LocalDate.of(2030,1,1));
  assertDoesNotThrow(domestic::validateCreation);domestic.setApprovalDate(LocalDate.of(2025,1,1));assertThrows(ServiceException.class,domestic::validateCreation);assertDoesNotThrow(domestic::validate);
  domestic.setApprovalDate(null);domestic.setChangeDescription("变更内容");assertThrows(ServiceException.class,domestic::validateCreation);
  var fda=base("FDA");fda.setSubmissionType("PMA");fda.setCustomerHeld(false);fda.setApprovalLetterStatus("VALID");fda.setFactoryRegistrationStatus("ACTIVE");fda.setApprovalDate(LocalDate.of(2025,1,1));
  assertDoesNotThrow(fda::validateCreation);fda.setChangeDescription("变更内容");assertThrows(ServiceException.class,fda::validateCreation);assertDoesNotThrow(fda::validate);
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
 @Test void changedApprovalOrModelsRequireFreshDescriptionWhileStatusDoesNot() {
  var fda=base("FDA");fda.setSubmissionType("PMA");fda.setCustomerHeld(false);fda.setApprovalLetterStatus("VALID");fda.setFactoryRegistrationStatus("ACTIVE");fda.setKpNumber("P1");fda.setChangeDescription("历史说明");
  var changed=DccRegistrationCertificateFacts.parse(fda.toJson());changed.setFactoryRegistrationStatus("INACTIVE");assertDoesNotThrow(()->changed.validateChangeFrom(fda));
  changed.setKpNumber("P2");assertThrows(ServiceException.class,()->changed.validateChangeFrom(fda));changed.setChangeDescription("新P号替代原批准信息");assertDoesNotThrow(()->changed.validateChangeFrom(fda));
  var ce=base("CE");ce.setExpiryDate(LocalDate.of(2030,1,1));ce.setChangeDescription("历史调整");
  var models=DccRegistrationCertificateFacts.parse(ce.toJson());models.setModelSpecifications(List.of("A"));assertThrows(ServiceException.class,()->models.validateChangeFrom(ce));models.setChangeDescription("本次增加A型号");assertDoesNotThrow(()->models.validateChangeFrom(ce));
  models.setOtherContentDescription("其他内容");assertEquals("其他内容",DccRegistrationCertificateFacts.parse(models.toJson()).getOtherContentDescription());
 }
 private static DccRegistrationCertificateFacts base(String type){var f=new DccRegistrationCertificateFacts();f.setCertificateType(type);f.setOwnProduct(false);f.setClassification("II");if(!"FDA".equals(type))f.setCertificateStatus("VALID");return f;}
}
