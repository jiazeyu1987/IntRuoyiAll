package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.*;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessResult;
import cn.iocoder.yudao.module.dcc.service.token.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual H2 callback logs/readAt, Query and two real HMAC token services; storage/user/audit remain explicit external ports. */
class DccW08OfficeOpenedConfirmationDatabaseTest extends BaseDbUnitTest {
    @Resource DataSource dataSource;
    @Resource(name="dccControlledFileMapper") DccControlledFileMapper files;
    @Resource DccControlledFileDistributionMapper distributions;
    @Resource DccControlledFileDistributionRecipientMapper recipients;
    @Resource DccControlledFileMessageJobMapper jobs;
    @Resource DccControlledFileAccessLogMapper logs;
    @Resource DccControlledFileAccessEventMapper events;
    @Resource DccControlledFileWatermarkTraceMapper traces;
    DccW08DistributionPreviewTest fixture;
    DccControlledFileQueryServiceImpl query;
    DccViewerTokenService viewer;
    DccIssuedViewerToken issued;
    GxpAuditService auditPort;
    JdbcTemplate jdbc;
    TransactionTemplate transaction;
    String officeToken;

    @BeforeEach void setup() throws Exception {
        try(var connection=dataSource.getConnection()){assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        jdbc=new JdbcTemplate(dataSource);transaction=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        fixture=new DccW08DistributionPreviewTest();fixture.setup();query=fixture.query;
        fixture.configureRealOffice();
        fixture.file.setFileName("issued-A1.docx");fixture.file.setTitle("Issued A1");fixture.file.setSubmitterId(99L);fixture.file.setRequesterId(99L);
        fixture.file.setDirectoryId(5L);fixture.file.setSourceFileId(100L);fixture.file.setOriginalFileId(100L);
        files.insert(fixture.file);fixture.distribution.setDepartmentId(5L);distributions.insert(fixture.distribution);
        recipients.insert(fixture.recipient);fixture.job.setTemplateCode("dcc_distribution");jobs.insert(fixture.job);
        ReflectionTestUtils.setField(query,"controlledFileMapper",files);ReflectionTestUtils.setField(query,"distributionMapper",distributions);
        ReflectionTestUtils.setField(query,"distributionRecipientMapper",recipients);ReflectionTestUtils.setField(query,"messageJobMapper",jobs);
        ReflectionTestUtils.setField(query,"accessLogMapper",logs);ReflectionTestUtils.setField(query,"accessEventMapper",events);ReflectionTestUtils.setField(query,"watermarkTraceMapper",traces);
        var now=LocalDateTime.now().withNano(0);
        jdbc.update("INSERT INTO dcc_controlled_file_access_event(id,tenant_id,access_event_code,controlled_file_id,file_version_no,user_id,access_type,purpose,result,occurred_at) VALUES(30,1,'event',5,'A/1',99,'PREVIEW','DIST_PREVIEW:10:11','SUCCESS',?)",now);
        jdbc.update("INSERT INTO dcc_controlled_file_watermark_trace(id,tenant_id,trace_code,access_event_id,access_event_code,controlled_file_id,file_number,file_version_no,user_id,privacy_mode,watermark_payload_json,issued_at,expires_at) VALUES(31,1,'trace',30,'event',5,'SOP-1','A/1',99,'TRACE_CODE_ONLY','{}',?,?)",now,now.plusMinutes(15));
        viewer=new DccViewerTokenService();ReflectionTestUtils.setField(viewer,"hmacSecret","w08-explicit-authenticated-office-proof-secret-20261011");
        issued=viewer.issue(new DccViewerTokenIssueCommand(1L,99L,5L,"A/1",30L,"DIST_PREVIEW:10:11",900L));
        ReflectionTestUtils.setField(query,"viewerTokenService",viewer);
        when(fixture.preview.prepareAccess(any())).thenReturn(new DccPreviewAccessResult(30L,"event",31L,"trace",issued.token(),issued.payload().getTokenId(),issued.payload().getNonce(),now,now.plusMinutes(15),"{}"));
        var receipt=new DccDistributionReceiptServiceImpl();auditPort=mock(GxpAuditService.class);
        ReflectionTestUtils.setField(receipt,"controlledFileMapper",files);ReflectionTestUtils.setField(receipt,"distributionMapper",distributions);
        ReflectionTestUtils.setField(receipt,"distributionRecipientMapper",recipients);ReflectionTestUtils.setField(receipt,"messageJobMapper",jobs);
        ReflectionTestUtils.setField(receipt,"adminUserApi",ReflectionTestUtils.getField(query,"adminUserApi"));ReflectionTestUtils.setField(receipt,"gxpAuditService",auditPort);
        ReflectionTestUtils.setField(query,"distributionReceiptService",receipt);
        officeToken=query.getDistributionPreviewMetadata(99L,5L,10L,11L,context()).getOnlyofficeDocumentUrl().split("token=",2)[1];
        SecurityContextHolder.clearContext();
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();TenantContextHolder.clear();}
    void login(Long actor,Long tenant){var user=new LoginUser();user.setId(actor);user.setTenantId(tenant);user.setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,java.util.List.of()));}
    DccRequestAuditContext context(){return new DccRequestAuditContext("127.0.0.1","real-unit-browser-context","office-proof-request");}
    boolean confirm(String nonce){return transaction.execute(status->query.confirmDistributionOfficeBodyOpened(99L,5L,10L,11L,issued.token(),"event","trace",issued.payload().getTokenId(),nonce,context()));}
    LocalDateTime opened(){return recipients.selectById(11L).getReadAt();}
    void callback() throws Exception {assertArrayEquals(new byte[]{4,5,6},query.readOnlyOfficePreviewFile(5L,officeToken,context()).bytes());}

    @Test void anonymousCallbackPersistsExactSuccessfulReadThenAuthenticatedBrowserRecordsFirstOpen() throws Exception {
        assertNull(opened());callback();assertNull(opened());verifyNoInteractions(auditPort);
        var read=jdbc.queryForMap("SELECT * FROM dcc_controlled_file_access_log WHERE action_type='OFFICE_READ'");
        assertEquals(1L,((Number)read.get("TENANT_ID")).longValue());assertEquals("DIST_PREVIEW:10:11",read.get("PURPOSE"));assertEquals("ALLOWED",read.get("RESULT"));
        login(99L,1L);assertTrue(confirm(issued.payload().getNonce()));var first=opened();assertNotNull(first);
        assertTrue(confirm(issued.payload().getNonce()));assertEquals(first,opened());verify(auditPort,times(1)).append(any());
        assertNull(recipients.selectById(11L).getAcknowledgedAt());
        assertNull(((ThreadLocal<?>)ReflectionTestUtils.getField(query,"distributionPreviewContext")).get());
    }
    @Test void metadataAndClientReadyWithoutPersistentOfficeSuccessCannotInventOpen() {
        login(99L,1L);assertThrows(RuntimeException.class,()->confirm(issued.payload().getNonce()));assertNull(opened());verifyNoInteractions(auditPort);
        jdbc.update("INSERT INTO dcc_controlled_file_access_log(tenant_id,controlled_file_id,access_event_id,access_event_code,watermark_trace_code,file_version_no,user_id,action_type,purpose,result) VALUES(1,5,30,'event','trace','A/1',99,'PREVIEW','DIST_PREVIEW:10:11','ALLOWED')");
        assertThrows(RuntimeException.class,()->confirm(issued.payload().getNonce()));assertNull(opened());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"NO_LOGIN","OTHER_USER","OTHER_TENANT","WRONG_NONCE","EXPIRED","OLD_VERSION","RECOVERED","WRONG_EVENT_LOG","FAILED_JOB"})
    void wrongOrExpiredAuthenticatedProofRecordsNoReadAt(String invalid) throws Exception {
        callback();login(99L,1L);String nonce=issued.payload().getNonce();
        switch(invalid){
            case "NO_LOGIN" -> SecurityContextHolder.clearContext();case "OTHER_USER" -> login(98L,1L);case "OTHER_TENANT" -> login(99L,2L);
            case "WRONG_NONCE" -> nonce="foreign-nonce";
            case "EXPIRED" -> ReflectionTestUtils.setField(viewer,"clock",Clock.fixed(Instant.ofEpochSecond(issued.payload().getExpiresAtEpochSecond()+1),ZoneOffset.UTC));
            case "OLD_VERSION" -> jdbc.update("UPDATE dcc_controlled_file SET version_no='B/1' WHERE id=5");
            case "RECOVERED" -> jdbc.update("UPDATE dcc_controlled_file_distribution SET status='RECOVERED' WHERE id=10");
            case "WRONG_EVENT_LOG" -> jdbc.update("UPDATE dcc_controlled_file_access_log SET access_event_id=999 WHERE action_type='OFFICE_READ'");
            case "FAILED_JOB" -> jdbc.update("UPDATE dcc_controlled_file_message_job SET status='FAILED' WHERE id=12");
        }
        String actualNonce=nonce;assertThrows(RuntimeException.class,()->confirm(actualNonce));assertNull(opened());verifyNoInteractions(auditPort);
        assertNull(((ThreadLocal<?>)ReflectionTestUtils.getField(query,"distributionPreviewContext")).get());
    }
    @Test void lateHumanAuditFailureRollsBackReadAtAndPropagates() throws Exception {
        callback();login(99L,1L);doThrow(new IllegalStateException("actual human audit refused")).when(auditPort).append(any());
        assertEquals("actual human audit refused",assertThrows(IllegalStateException.class,()->confirm(issued.payload().getNonce())).getMessage());
        assertNull(opened());assertNull(((ThreadLocal<?>)ReflectionTestUtils.getField(query,"distributionPreviewContext")).get());
    }
}
