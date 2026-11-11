package cn.iocoder.yudao.module.bpm.approval.content;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.mysql.approval.BpmApprovalReviewReceiptMapper;
import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalReviewReceiptDO;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpCreationAuditVerificationService;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApprovalReceiptQueryTest {
    @Test void noCommittedReceiptReturnsUnknownWithoutCreatingOrLockingAnything() {
        var mapper = mock(BpmApprovalReviewReceiptMapper.class);
        var signatures = mock(ElectronicSignatureVerifiedFactService.class);
        var audits = mock(GxpCreationAuditVerificationService.class);
        var service = new ApprovalCenterReceiptService(mapper, signatures, audits);
        try {
            TenantContextHolder.setTenantId(168L);
            assertNull(service.findDecisionReceipt(7L,"original","EDHR","EDHR_WORK_TASK","3006","prepare"));
            verify(mapper).selectExactForActor(168L,"original",7L);
            verifyNoMoreInteractions(mapper);
            verifyNoInteractions(signatures,audits);
        } finally { TenantContextHolder.clear(); }
    }

    @Test void malformedReceiptCannotBePresentedAsSuccessEvenWithMatchingKey() {
        var mapper = mock(BpmApprovalReviewReceiptMapper.class);
        var row = new BpmApprovalReviewReceiptDO(); row.setTenantId(168L); row.setActorId(7L); row.setOperatorId(7L);
        when(mapper.selectExactForActor(168L,"original",7L)).thenReturn(row);
        var service = new ApprovalCenterReceiptService(mapper,mock(ElectronicSignatureVerifiedFactService.class),mock(GxpCreationAuditVerificationService.class));
        try {
            TenantContextHolder.setTenantId(168L);
            assertThrows(IllegalStateException.class,()->service.findDecisionReceipt(7L,"original","EDHR","EDHR_WORK_TASK","3006",null));
            verifyNoMoreInteractions(ignoreStubs(mapper));
        } finally { TenantContextHolder.clear(); }
    }

    @Test void actualReceiptSelectEnforcesTenantActorOperatorAndExactKey() throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:receipt_read_"+java.util.UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE bpm_approval_review_receipt(id BIGINT,tenant_id BIGINT,actor_id BIGINT,operator_id BIGINT,idempotency_key VARCHAR(128))");
        jdbc.update("INSERT INTO bpm_approval_review_receipt VALUES(1,168,7,7,'exact'),(2,169,7,7,'other-tenant'),(3,168,8,8,'other-actor'),(4,168,7,8,'other-operator')");
        var config = new Configuration(); config.setMapUnderscoreToCamelCase(true); config.addMapper(BpmApprovalReviewReceiptMapper.class);
        var factory = new SqlSessionFactoryBean(); factory.setDataSource(source); factory.setConfiguration(config);
        var mapper = new SqlSessionTemplate(factory.getObject()).getMapper(BpmApprovalReviewReceiptMapper.class);
        assertEquals(1L,mapper.selectExactForActor(168L,"exact",7L).getId());
        assertNull(mapper.selectExactForActor(168L,"other-tenant",7L));
        assertNull(mapper.selectExactForActor(168L,"other-actor",7L));
        assertNull(mapper.selectExactForActor(168L,"other-operator",7L));
        assertNull(mapper.selectExactForActor(168L,"exact",8L));
        assertNull(mapper.selectExactForActor(168L,"exac",7L));
        assertEquals(4,jdbc.queryForObject("SELECT COUNT(*) FROM bpm_approval_review_receipt",Integer.class));
    }
}
