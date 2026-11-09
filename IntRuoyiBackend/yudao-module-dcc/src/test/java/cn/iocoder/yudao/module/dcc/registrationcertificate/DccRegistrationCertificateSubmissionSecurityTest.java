package cn.iocoder.yudao.module.dcc.registrationcertificate;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.*;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.*;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload.DccRegistrationCertificateUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class DccRegistrationCertificateSubmissionSecurityTest {
 @Test void requestDetailsEnforceBothTenantAndRequester(){
  var mapper=mock(DccRegistrationCertificateAccessRequestMapper.class);var service=service(mapper);
  var request=request();when(mapper.selectById(10L)).thenReturn(request);
  assertThrows(ServiceException.class,()->service.getMyRequest(2L,20L,10L));
  assertThrows(ServiceException.class,()->service.getMyRequest(1L,21L,10L));
 }
 @Test void rejectionEvidenceUsesOriginalRequestContentAndPerSubmissionFileStatus(){
  var mapper=mock(DccRegistrationCertificateAccessRequestMapper.class);var service=service(mapper);var request=request();
  request.setDetailJson("{\"factsJson\":\"original-submitted-content\"}");request.setStatus("REJECTED");when(mapper.selectById(10L)).thenReturn(request);
  var links=mock(DccRegistrationCertificateAccessRequestFileMapper.class);var files=mock(DccRegistrationCertificateFileMapper.class);
  ReflectionTestUtils.setField(service,"requestFileMapper",links);ReflectionTestUtils.setField(service,"fileMapper",files);
  var link=DccRegistrationCertificateAccessRequestFileDO.builder().businessFileId(30L).status("REJECTED").build();
  when(links.selectByRequestId(1L,10L)).thenReturn(List.of(link));
  var file=DccRegistrationCertificateFileDO.builder().id(30L).fileKind("REGISTRATION_CERTIFICATE").originalName("original.pdf").status("BOUND").build();file.setTenantId(1L);when(files.selectById(30L)).thenReturn(file);
  var result=service.getMyRequest(1L,20L,10L);assertSame(request,result.get("request"));assertTrue(result.get("files").toString().contains("REJECTED"));
  assertEquals("{\"factsJson\":\"original-submitted-content\"}",request.getDetailJson());verify(files,never()).updateById(any(DccRegistrationCertificateFileDO.class));
 }
 private static DccRegistrationCertificateUploadService service(DccRegistrationCertificateAccessRequestMapper mapper){var s=mock(DccRegistrationCertificateUploadService.class,CALLS_REAL_METHODS);ReflectionTestUtils.setField(s,"requestMapper",mapper);return s;}
 private static DccRegistrationCertificateAccessRequestDO request(){var r=DccRegistrationCertificateAccessRequestDO.builder().id(10L).requesterUserId(20L).requestType("UPLOAD_CERTIFICATE").build();r.setTenantId(1L);return r;}
}
