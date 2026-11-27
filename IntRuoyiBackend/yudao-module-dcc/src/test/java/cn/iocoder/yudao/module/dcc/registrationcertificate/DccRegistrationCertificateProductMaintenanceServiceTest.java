package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.*;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.*;
import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.*;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DccRegistrationCertificateProductMaintenanceServiceTest extends BaseMockitoUnitTest {
    @InjectMocks private DccRegistrationCertificateProductMaintenanceService service;
    @Mock private DccRegistrationCertificateMapper certificates;
    @Mock private DccRegistrationCertificateSnapshotMapper snapshots;
    @Mock private DccRegistrationCertificateSnapshotEntrustedMapper entrusted;
    @Mock private DccRegistrationCertificateAuditMapper audits;
    @Mock private DccRegistrationCertificatePrerequisiteValidator validator;
    @Mock private DccRegistrationCertificateProductBindingService bindings;
    @Mock private DccRegistrationCertificateBusinessClock clock;
    private DccRegistrationCertificateDO certificate;
    private DccRegistrationCertificateSnapshotDO original;
    private final DccRegistrationCertificateProjectProduct first=product(100L,10L,"产品甲");
    private final DccRegistrationCertificateProjectProduct second=product(200L,20L,"产品乙");
    private static DccRegistrationCertificateProjectProduct product(long project,long id,String name){
        return new DccRegistrationCertificateProjectProduct(project,id,"P"+id,name,List.of(new DccRegistrationCertificateProjectProduct.Binding("CONFIRMED",id)));
    }
    @BeforeEach void fixture(){
        certificate=DccRegistrationCertificateDO.builder().id(1L).ownerCompanyId(5L).projectCodeId(100L).currentVersionId(11L).currentSnapshotId(111L).status("ACTIVE").rowVersion(3).build();
        certificate.setTenantId(1L);
        original=DccRegistrationCertificateSnapshotDO.builder().id(111L).versionId(11L).revisionNo(2).productName("产品甲").registrantName("原注册人").entrustedEnterprisesJson("[]").build();
        original.setTenantId(1L);
        var facts=new DccRegistrationCertificateFacts();facts.setCertificateType("DOMESTIC");facts.setOwnProduct(true);facts.setProjectProduct(first);facts.setProjectProducts(List.of(first));original.setFactsJson(facts.toJson());
        lenient().when(certificates.selectByIdForUpdate(1L)).thenReturn(certificate);
        lenient().when(snapshots.selectById(111L)).thenReturn(original);
    }
    private void resolveSecond(){
        var project=DccProjectCodeDO.builder().id(200L).status("ENABLE").build();project.setTenantId(1L);
        when(validator.validateProjectCode(1L,99L,200L,null)).thenReturn(project);
        when(validator.resolveProjectProduct(project)).thenReturn(second);
    }
    @Test void appendCreatesNewSnapshotAndRetainsOriginalHistory(){
        resolveSecond();String previous=original.getFactsJson();
        when(snapshots.insert(any(DccRegistrationCertificateSnapshotDO.class))).thenAnswer(inv->{inv.<DccRegistrationCertificateSnapshotDO>getArgument(0).setId(112L);return 1;});
        var oldFactory=DccRegistrationCertificateSnapshotEntrustedDO.builder().id(8L).snapshotId(111L).enterpriseId(9L).enterpriseNameSnapshot("受托工厂").sortOrder(1).build();oldFactory.setTenantId(1L);
        when(entrusted.selectListBySnapshotId(111L)).thenReturn(List.of(oldFactory));
        when(entrusted.insert(any(DccRegistrationCertificateSnapshotEntrustedDO.class))).thenReturn(1);
        when(certificates.updateById(any(DccRegistrationCertificateDO.class))).thenReturn(1);
        when(audits.insert(any(DccRegistrationCertificateAuditDO.class))).thenReturn(1);
        when(clock.now()).thenReturn(LocalDateTime.of(2026,11,25,12,0));
        service.addProducts(1L,99L,1L,3,List.of(200L));
        var created=ArgumentCaptor.forClass(DccRegistrationCertificateSnapshotDO.class);verify(snapshots).insert(created.capture());
        assertEquals(3,created.getValue().getRevisionNo());assertEquals("产品甲、产品乙",created.getValue().getProductName());
        assertEquals(List.of(first,second),DccRegistrationCertificateFacts.parse(created.getValue().getFactsJson()).getProjectProducts());
        assertEquals(previous,original.getFactsJson());assertEquals(111L,oldFactory.getSnapshotId());
        assertEquals(112L,certificate.getCurrentSnapshotId());assertEquals(4,certificate.getRowVersion());
        verify(bindings).confirm(1L,1L,List.of(first,second),99L);
        verify(snapshots).selectById(111L);verifyNoMoreInteractions(snapshots);
        verify(audits).insert(any(DccRegistrationCertificateAuditDO.class));
    }
    @Test void pendingRenewalPreventsAddingProducts(){certificate.setPendingVersionId(22L);assertThrows(ServiceException.class,()->service.addProducts(1L,99L,1L,3,List.of(200L)));verifyNoInteractions(bindings);}
    @Test void staleRevisionPreventsAddingProducts(){assertThrows(ServiceException.class,()->service.addProducts(1L,99L,1L,2,List.of(200L)));verifyNoInteractions(bindings);}
    @Test void anotherTenantCannotAddProducts(){assertThrows(ServiceException.class,()->service.addProducts(2L,99L,1L,3,List.of(200L)));verifyNoInteractions(validator,bindings);}
    @Test void duplicateProductCannotBeAddedUsingAnotherProject(){
        var project=DccProjectCodeDO.builder().id(200L).build();
        when(validator.validateProjectCode(1L,99L,200L,null)).thenReturn(project);
        when(validator.resolveProjectProduct(project)).thenReturn(product(200L,10L,"产品甲"));
        assertThrows(ServiceException.class,()->service.addProducts(1L,99L,1L,3,List.of(200L)));verifyNoInteractions(bindings);verify(snapshots,never()).insert(any(DccRegistrationCertificateSnapshotDO.class));
    }
    @Test void occupiedProductStopsBeforeCreatingSnapshot(){
        resolveSecond();doThrow(new ServiceException(400,"本注册证内项目与产品关联不一致")).when(bindings).confirm(1L,1L,List.of(first,second),99L);
        assertThrows(ServiceException.class,()->service.addProducts(1L,99L,1L,3,List.of(200L)));verify(snapshots,never()).insert(any(DccRegistrationCertificateSnapshotDO.class));
    }
}
