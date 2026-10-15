package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;
import java.lang.reflect.Method;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DccActivatedDistributionTest extends BaseMockitoUnitTest {
    @Mock DccControlledFileMapper controlledFileMapper;
    @Mock DccControlledFileDistributionMapper distributionMapper;
    @Mock DccControlledFileDistributionRecipientMapper recipientMapper;
    @Mock DccControlledFileMessageJobMapper messageJobMapper;
    @Mock DccControlledFileMessageDeliveryService messageDeliveryService;
    @InjectMocks DccWorkflowDistributionService service;
    final LocalDateTime now=LocalDateTime.of(2026,10,11,9,0);
    DccControlledFileDO file;
    DccControlledFileLifecycleEvent event;
    DccDistributionRosterSnapshot roster;
    List<DccControlledFileDistributionDO> distributions=new ArrayList<>();
    List<DccControlledFileDistributionRecipientDO> recipients=new ArrayList<>();
    Map<Long,DccControlledFileMessageJobDO> jobs=new LinkedHashMap<>();
    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);TransactionSynchronizationManager.setActualTransactionActive(true);
        file=DccControlledFileDO.builder().id(42L).masterId(10L).versionNo("B/1").processInstanceId("round-42")
                .status("ACTIVE").controlledTime(now.minusDays(1)).activatedTime(now).build();file.setTenantId(1L);
        event=new DccControlledFileLifecycleEvent("event-42","ACTIVATED",1L,10L,42L,null,"B/1","round-42",now);
        roster=new DccDistributionRosterSnapshot(1L,42L,"B/1","round-42",now,"approved-policy-input",
                List.of(new DccDistributionRosterSnapshot.Department(61L,List.of(99L,100L))));
        lenient().when(controlledFileMapper.selectByIdAndTenantForUpdate(1L,42L)).thenReturn(file);
        lenient().when(distributionMapper.selectListByControlledFileId(42L)).thenAnswer(ignored->List.copyOf(distributions));
        lenient().when(distributionMapper.selectListByControlledFileIdForUpdate(1L,42L)).thenAnswer(ignored->List.copyOf(distributions));
        lenient().when(recipientMapper.selectListByDistributionIdForUpdate(eq(1L),anyLong())).thenAnswer(call->recipients.stream()
                .filter(row->Objects.equals(row.getDistributionId(),call.getArgument(1))).toList());
        lenient().when(recipientMapper.selectListByDistributionId(anyLong())).thenAnswer(call->recipients.stream()
                .filter(row->Objects.equals(row.getDistributionId(),call.getArgument(0))).toList());
        lenient().doAnswer(call->{var row=call.getArgument(0,DccControlledFileDistributionDO.class);row.setId(51L);distributions.add(row);return 1;})
                .when(distributionMapper).insert(any(DccControlledFileDistributionDO.class));
        lenient().doAnswer(call->{var row=call.getArgument(0,DccControlledFileDistributionRecipientDO.class);row.setId(71L+recipients.size());recipients.add(row);return 1;})
                .when(recipientMapper).insert(any(DccControlledFileDistributionRecipientDO.class));
        lenient().doAnswer(call->{var row=call.getArgument(0,DccControlledFileMessageJobDO.class);row.setId(81L+jobs.size());jobs.put(row.getId(),row);return 1;})
                .when(messageJobMapper).insert(any(DccControlledFileMessageJobDO.class));
        lenient().when(messageJobMapper.selectById(anyLong())).thenAnswer(call->jobs.get(call.getArgument(0)));
        lenient().when(recipientMapper.updateById(any(DccControlledFileDistributionRecipientDO.class))).thenAnswer(call->{
            var update=call.getArgument(0,DccControlledFileDistributionRecipientDO.class);
            recipients.stream().filter(row->Objects.equals(row.getId(),update.getId())).forEach(row->row.setMessageJobId(update.getMessageJobId()));return 1;
        });
        lenient().when(controlledFileMapper.updateById(any(DccControlledFileDO.class))).thenAnswer(call->{
            var update=call.getArgument(0,DccControlledFileDO.class);file.setDistributedTime(update.getDistributedTime());file.setDistributionPayloadHash(update.getDistributionPayloadHash());return 1;
        });
    }
    @AfterEach void clear() { TenantContextHolder.clear();TransactionSynchronizationManager.setActualTransactionActive(false); }
    void dispatch() throws Exception {
        Method method=assertDoesNotThrow(()->DccWorkflowDistributionService.class.getMethod("dispatchActivatedDistribution",DccControlledFileLifecycleEvent.class,DccDistributionRosterSnapshot.class),
                "formal activation dispatch must exist on the existing distribution owner");
        try { method.invoke(service,event,roster); } catch(java.lang.reflect.InvocationTargetException ex) {
            if(ex.getCause() instanceof RuntimeException runtime)throw runtime;throw ex;
        }
    }
    @Test void activationCreatesOneObligationAndJobPerFrozenRecipientAndReplayAddsNone() throws Exception {
        dispatch();dispatch();
        assertEquals(1,distributions.size());assertEquals(2,recipients.size());assertEquals(2,jobs.size());
        assertEquals(List.of(99L,100L),recipients.stream().map(DccControlledFileDistributionRecipientDO::getUserId).toList());
        assertNotNull(file.getDistributedTime());assertEquals(now,file.getActivatedTime());
    }
    @Test void pendingEffectiveCreatesNothing() {
        file.setStatus("CONTROLLED_PENDING_EFFECTIVE");file.setActivatedTime(null);
        assertThrows(Exception.class,this::dispatch);assertTrue(jobs.isEmpty());assertTrue(recipients.isEmpty());
    }
    @Test void missingFormalSnapshotDoesNotGuessAudience() {
        roster=null;assertThrows(Exception.class,this::dispatch);assertTrue(jobs.isEmpty());
    }
    @Test void controlledEventDoesNotDispatch() {
        event=new DccControlledFileLifecycleEvent("event-42","CONTROLLED",1L,10L,42L,null,"B/1","round-42",now);
        assertThrows(Exception.class,this::dispatch);assertTrue(jobs.isEmpty());
    }
    @Test void conflictingReplayCannotCreateNewAudience() throws Exception {
        dispatch();roster=new DccDistributionRosterSnapshot(1L,42L,"B/1","round-42",now,"approved-policy-input",
                List.of(new DccDistributionRosterSnapshot.Department(61L,List.of(123L))));
        assertThrows(Exception.class,this::dispatch);assertEquals(2,jobs.size());assertEquals(2,recipients.size());
    }
}
