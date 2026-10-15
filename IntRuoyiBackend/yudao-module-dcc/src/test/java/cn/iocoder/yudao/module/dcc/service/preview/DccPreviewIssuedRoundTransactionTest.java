package cn.iocoder.yudao.module.dcc.service.preview;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.*;
import cn.iocoder.yudao.module.dcc.service.audit.*;
import cn.iocoder.yudao.module.dcc.service.token.*;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real event/trace/log mappers, Spring transaction and viewer HMAC, not an Office callback test. */
@Import({DccControlledPreviewAccessService.class,DccControlledFileAccessAuditService.class,DccViewerTokenService.class})
class DccPreviewIssuedRoundTransactionTest extends BaseDbUnitTest {
    @Resource DccControlledPreviewAccessService producer;
    @Resource DccControlledFileAccessAuditService audit;
    @Resource DccViewerTokenService tokens;
    @Resource DccControlledFileAccessEventMapper events;
    @Resource DccControlledFileWatermarkTraceMapper traces;
    @Resource DccControlledFileAccessLogMapper logs;
    @Resource PlatformTransactionManager txManager;
    @BeforeEach void setup(){TenantContextHolder.setTenantId(1L);
        Clock fixed=Clock.fixed(Instant.parse("2026-10-12T00:00:00Z"),ZoneOffset.UTC);
        ReflectionTestUtils.setField(producer,"clock",fixed);ReflectionTestUtils.setField(tokens,"clock",fixed);
        ReflectionTestUtils.setField(tokens,"hmacSecret","manager-issued-round-private-32-bytes-long");}
    @AfterEach void clear(){TenantContextHolder.clear();}
    DccPreviewAccessRequest request(String bpm){return new DccPreviewAccessRequest(1L,99L,20L,"A/1","DOC-20","PREVIEW",
        "CONTROLLED_PREVIEW",300L,"requester","Uploader",null,null,"Task Tenant","TRACE_CODE_ONLY","127.0.0.1","JUnit","round-proof",bpm);}
    DccPreviewAccessResult create(String bpm){return new TransactionTemplate(txManager).execute(s->producer.prepareAccess(request(bpm)));}
    @Test void issuedEventPersistsExactRoundBeforeItsAuthenticTokenIsReturned(){
        var result=create("actual-round-A");var saved=events.selectById(result.accessEventId());
        assertEquals("actual-round-A",saved.getProcessInstanceId());assertEquals(1L,saved.getTenantId());
        assertEquals(20L,saved.getControlledFileId());assertEquals(99L,saved.getUserId());
        var verified=tokens.verify(result.viewerToken(),new DccViewerTokenExpectedContext(1L,99L,20L,"A/1",saved.getId(),
            "CONTROLLED_PREVIEW",300L,result.viewerTokenNonce(),result.viewerTokenId()));
        assertEquals(saved.getId(),verified.getAccessEventId());assertEquals(1L,traces.selectCount(null));assertEquals(1L,logs.selectCount(null));
    }
    @Test void priorNullEventIsNeverFilledWhenAnotherRoundIsIssued(){var old=create(null);var before=events.selectById(old.accessEventId());
        var next=create("actual-round-B");assertEquals("actual-round-B",events.selectById(next.accessEventId()).getProcessInstanceId());
        assertEquals(before,events.selectById(old.accessEventId()));assertNull(before.getProcessInstanceId());}
    @Test void invalidRoundProofInputFailsWithoutEventOrToken(){for(String invalid:new String[]{""," padded-round","round ","a".repeat(129)})
        assertThrows(RuntimeException.class,()->create(invalid));assertEquals(0L,events.selectCount(null));}
    @Test void zeroInsertOrMissingPersistedRowIsNeverReportedAsTokenSuccess(){
        var denied=mock(DccControlledFileAccessEventMapper.class);
        ReflectionTestUtils.setField(audit,"accessEventMapper",denied);
        try{assertThrows(RuntimeException.class,()->create("actual-round-A"));assertEquals(0L,traces.selectCount(null));assertEquals(0L,logs.selectCount(null));
            when(denied.insert(any(DccControlledFileAccessEventDO.class))).thenAnswer(call->{DccControlledFileAccessEventDO row=call.getArgument(0);row.setId(9000L);return 1;});
            assertThrows(RuntimeException.class,()->create("actual-round-A"));assertEquals(0L,traces.selectCount(null));assertEquals(0L,logs.selectCount(null));}
        finally{ReflectionTestUtils.setField(audit,"accessEventMapper",events);}
    }
    @Test void lateTraceInsertFailureRollsBackTheIssuedRoundAndReturnsNoToken(){
        var denied=mock(DccControlledFileWatermarkTraceMapper.class);
        when(denied.insert(any(DccControlledFileWatermarkTraceDO.class))).thenThrow(new IllegalStateException("real trace write failed"));
        ReflectionTestUtils.setField(audit,"watermarkTraceMapper",denied);
        try{assertThrows(RuntimeException.class,()->create("actual-round-A"));assertEquals(0L,events.selectCount(null));assertEquals(0L,logs.selectCount(null));}
        finally{ReflectionTestUtils.setField(audit,"watermarkTraceMapper",traces);}
    }
}
