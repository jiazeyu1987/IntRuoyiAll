package cn.iocoder.yudao.module.mes.service.pro.craft;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.craft.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionLifecycleService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteControlledContentAdapter;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 在真实事务中模拟第二条流程发布失败，验证第一条发布和工艺修改同时回滚。 */
class MesProCraftGroupTransactionTest {
    private JdbcTemplate jdbc;
    private MesProCraftService service;
    private MesProCraftChangeMapper changes;
    private MesProCraftMapper crafts;
    private MesProRouteVersionMapper versions;
    private MesProRouteVersionLifecycleService publisher;
    private MesProRouteControlledContentAdapter platform;
    private final MesProCraftChangeDO change = new MesProCraftChangeDO();

    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        TenantContextHolder.setTenantId(1L);
        var source = new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("create table state (id bigint primary key, status varchar(64))");
        jdbc.update("insert into state values(1,'PENDING'),(2,'old craft'),(101,'READY_TO_PUBLISH'),(102,'READY_TO_PUBLISH')");
        crafts=mock(MesProCraftMapper.class); changes=mock(MesProCraftChangeMapper.class);
        versions=mock(MesProRouteVersionMapper.class); publisher=mock(MesProRouteVersionLifecycleService.class);
        platform=mock(MesProRouteControlledContentAdapter.class);
        when(crafts.lockSequence(1L)).thenReturn(10L);
        change.setId(1L);change.setCraftId(2L);change.setStatus("PENDING");change.setChangeType("UPDATE");
        change.setCandidateIdsJson("[101,102]");change.setCandidateHashesJson("{\"101\":\"h101\",\"102\":\"h102\",\"101:source\":\"source101\",\"102:source\":\"source102\"}");
        MesProCraftDO next=new MesProCraftDO();next.setId(2L);next.setName("new craft");
        change.setAfterJson(JSON.toJSONString(next));
        when(changes.selectById(1L)).thenAnswer(call->{change.setStatus(state(1));return change;});
        when(changes.updateById(any(MesProCraftChangeDO.class))).thenAnswer(call->{
            MesProCraftChangeDO c=call.getArgument(0);return jdbc.update("update state set status=? where id=1",c.getStatus());});
        when(crafts.updateById(any(MesProCraftDO.class))).thenAnswer(call->{
            MesProCraftDO c=call.getArgument(0);return jdbc.update("update state set status=? where id=2",c.getName());});
        when(versions.selectById(anyLong())).thenAnswer(call->version(call.getArgument(0),state(call.getArgument(0))));
        when(versions.updateById(any(MesProRouteVersionDO.class))).thenAnswer(call->{
            MesProRouteVersionDO v=call.getArgument(0);
            return jdbc.update("update state set status=? where id=?",v.getLifecycleStatus(),v.getId());});
        when(versions.selectActiveByRouteIdForUpdate(anyLong())).thenAnswer(call->{
            MesProRouteVersionDO active=new MesProRouteVersionDO();active.setId((Long)call.getArgument(0)-100);
            active.setRouteSnapshotSha256("source"+active.getId());return active;});
        ObjectProvider<MesProRouteVersionLifecycleService> provider=mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(publisher);
        MesProCraftService target=new MesProCraftService();
        ReflectionTestUtils.setField(target,"craftMapper",crafts);ReflectionTestUtils.setField(target,"changeMapper",changes);
        ReflectionTestUtils.setField(target,"versionMapper",versions);ReflectionTestUtils.setField(target,"lifecycle",provider);
        ReflectionTestUtils.setField(target,"platform",platform);
        ProxyFactory proxy=new ProxyFactory(target);proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(source),new AnnotationTransactionAttributeSource()));
        service=(MesProCraftService)proxy.getProxy();
    }
    @AfterEach void cleanContext(){TenantContextHolder.clear();}
    private String state(long id){return jdbc.queryForObject("select status from state where id=?",String.class,id);}
    private MesProRouteVersionDO version(long id,String status){
        MesProRouteVersionDO v=new MesProRouteVersionDO();v.setId(id);v.setRouteId(id+100);v.setSourceRouteVersionId(id);
        v.setLifecycleStatus(status);v.setRouteSnapshotSha256("h"+id);
        v.setRouteSnapshotJson("{\"configSnapshots\":{\"craftChangeId\":1}}");return v;
    }
    @Test void oneApprovalCannotPublishAnyRouteOrModifyCraft(){
        jdbc.update("update state set status='PENDING_APPROVAL' where id=102");
        assertTrue(service.onRouteReady(version(101,"READY_TO_PUBLISH"),7L));
        verifyNoInteractions(publisher);assertEquals("PENDING",state(1));assertEquals("old craft",state(2));
    }
    @Test void allApprovalsPublishAllRoutesBeforeCraftBecomesCurrent(){
        when(publisher.publishCandidate(anyLong(),eq(7L))).thenAnswer(call->{
            assertEquals("old craft",state(2));assertEquals("APPLYING",state(1));
            long id=call.getArgument(0);jdbc.update("update state set status='ACTIVE' where id=?",id);return version(id,"ACTIVE");});
        assertTrue(service.onRouteReady(version(102,"READY_TO_PUBLISH"),7L));
        assertEquals("ACTIVE",state(101));assertEquals("ACTIVE",state(102));
        assertEquals("new craft",state(2));assertEquals("APPLIED",state(1));
    }
    @Test void secondPublishFailureRollsBackFirstPublishAndCraft(){
        when(publisher.publishCandidate(anyLong(),eq(7L))).thenAnswer(call->{
            long id=call.getArgument(0);jdbc.update("update state set status='ACTIVE' where id=?",id);
            if(id==102)throw new IllegalStateException("second projection failed");return version(id,"ACTIVE");});
        assertThrows(IllegalStateException.class,()->service.onRouteReady(version(102,"READY_TO_PUBLISH"),7L));
        assertEquals("READY_TO_PUBLISH",state(101));assertEquals("READY_TO_PUBLISH",state(102));
        assertEquals("old craft",state(2));assertEquals("PENDING",state(1));
    }
    @Test void changedRouteHashRejectsWholeBatchBeforePublishing(){
        change.setCandidateHashesJson("{\"101\":\"changed\",\"102\":\"h102\"}");
        assertThrows(RuntimeException.class,()->service.onRouteReady(version(102,"READY_TO_PUBLISH"),7L));
        verifyNoInteractions(publisher);assertEquals("old craft",state(2));assertEquals("PENDING",state(1));
    }
    @Test void changedActiveSourceRejectsWholeBatchBeforePublishing(){
        when(versions.selectActiveByRouteIdForUpdate(202L)).thenReturn(MesProRouteVersionDO.builder().id(999L).build());
        assertThrows(RuntimeException.class,()->service.onRouteReady(version(102,"READY_TO_PUBLISH"),7L));
        verifyNoInteractions(publisher);assertEquals("PENDING",state(1));
    }
    @Test void sameSourceVersionWithChangedContentRejectsWholeBatch(){
        when(versions.selectActiveByRouteIdForUpdate(202L)).thenReturn(MesProRouteVersionDO.builder()
                .id(102L).routeSnapshotSha256("changed source").build());
        assertThrows(RuntimeException.class,()->service.onRouteReady(version(102,"READY_TO_PUBLISH"),7L));
        verifyNoInteractions(publisher);assertEquals("PENDING",state(1));
    }
    @Test void ordinaryRoutePublicationDoesNotEnterCraftGroup(){
        MesProRouteVersionDO v=version(101,"READY_TO_PUBLISH");v.setRouteSnapshotJson("{\"configSnapshots\":{}}");
        assertFalse(service.onRouteReady(v,7L));verifyNoInteractions(publisher);
    }
    @Test void boundCraftCannotBeUnlinkedThroughOrdinaryConfigSave(){
        var before=JSON.parseObject("{\"productionProcessConfigs\":[{\"routeProcessId\":11,\"craftId\":2}]}");
        var after=JSON.parseObject("{\"productionProcessConfigs\":[{\"routeProcessId\":11}]}");
        assertThrows(RuntimeException.class,()->service.validateBindingTransitions(before,after));
    }
    @Test void cancellingModificationCancelsEveryCandidateAndKeepsOldCraft(){
        service.cancelChange(1L);
        assertEquals("CANCELLED",state(1));assertEquals("CANCELLED",state(101));assertEquals("CANCELLED",state(102));
        assertEquals("old craft",state(2));
        assertTrue(service.cancelledBatch(version(101,"CANCELLED")),"迟到的审批回调必须识别整批已作废");
        verifyNoInteractions(publisher);
    }
    @Test void cancellationFailureRollsBackEveryCandidateAndModification(){
        doThrow(new IllegalStateException("audit failed")).when(platform)
                .recordCancelled(argThat(v->v.getId()==102L),isNull());
        assertThrows(IllegalStateException.class,()->service.cancelChange(1L));
        assertEquals("PENDING",state(1));assertEquals("READY_TO_PUBLISH",state(101));assertEquals("READY_TO_PUBLISH",state(102));
        assertEquals("old craft",state(2));
    }
}
