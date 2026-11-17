package cn.iocoder.yudao.module.mes.service.pro.batchrecord;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MesEdhrTaskDeadlineServiceTest {
  private MesEdhrTaskDeadlineService service;
  private MesProEdhrWorkTaskAssignmentRuleMapper rules;
  @BeforeEach void setup() {
    TenantContextHolder.setTenantId(71L);
    service=new MesEdhrTaskDeadlineService(); rules=mock(MesProEdhrWorkTaskAssignmentRuleMapper.class);
    ReflectionTestUtils.setField(service,"rules",rules);
  }
  @AfterEach void cleanup() { TenantContextHolder.clear(); }
  @Test void noBusinessAgreementHasNoInventedDeadline() {
    assertNull(service.calculateDueTime(8L,"DEVIATION_APPROVE",LocalDateTime.now()));
  }
  @Test void explicitMinutesUseOriginalAssignmentTime() {
    var assigned=LocalDateTime.of(2026,10,28,8,0);
    when(rules.selectEnabledByScopeAndType("ROUTE_DEADLINE",8L,"DEVIATION_APPROVE"))
      .thenReturn(new MesProEdhrWorkTaskAssignmentRuleDO().setDueMinutes(37));
    assertEquals(assigned.plusMinutes(37),service.calculateDueTime(8L,"DEVIATION_APPROVE",assigned));
  }
  @Test void malformedEnabledRuleFailsWithoutDefault() {
    when(rules.selectEnabledByScopeAndType("ROUTE_DEADLINE",8L,"QA_REVIEW"))
      .thenReturn(new MesProEdhrWorkTaskAssignmentRuleDO().setDueMinutes(0));
    assertThrows(IllegalStateException.class,()->service.calculateDueTime(8L,"QA_REVIEW",LocalDateTime.now()));
  }
  @Test void replayAndTransferCannotExtendExistingPromise() {
    var due=LocalDateTime.now().minusDays(1);
    var task=new MesProEdhrWorkTaskDO().setDueTime(due);
    service.applyToWorkTask(task); assertEquals(due,task.getDueTime()); verifyNoInteractions(rules);
  }
  @Test void missingFormalRouteOrUnknownNodeFails() {
    assertThrows(IllegalArgumentException.class,()->service.calculateDueTime(null,"QA_REVIEW",LocalDateTime.now()));
    assertThrows(IllegalArgumentException.class,()->service.calculateDueTime(8L,"UNKNOWN",LocalDateTime.now()));
  }
  @Test void approvedRecordRevisionUsesOwnDeadlineNotOrdinaryReworkRule() {
    var task=new MesProEdhrWorkTaskDO().setRouteId(8L).setTaskType("REWORK").setBusinessScopeType("RECORD_CHANGE");
    service.applyToWorkTask(task);
    verify(rules).selectEnabledByScopeAndType("ROUTE_DEADLINE",8L,"RECORD_CHANGE_REWORK");
  }
  @Test void productionDeviationUsesItsActiveOrdersFormalRoute() {
    var orders=mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper.class);
    ReflectionTestUtils.setField(service,"activeOrders",orders);
    var order=new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO()
      .setWorkOrderId(12L).setRouteId(8L);
    order.setTenantId(71L);
    when(orders.selectById(5L)).thenReturn(order);
    var deviation=new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO()
      .setTenantId(71L).setActiveOrderId(5L).setWorkOrderId(12L);
    assertEquals(8L,service.requireDeviationRouteId(deviation));
    var task=new MesProEdhrWorkTaskDO().setRouteId(service.requireDeviationRouteId(deviation)).setTaskType("DEVIATION_VERIFY");
    service.applyToWorkTask(task);
    verify(rules).selectEnabledByScopeAndType("ROUTE_DEADLINE",8L,"DEVIATION_VERIFY");
  }
  @Test void batchedDeviationUsesBatchRouteWithoutInferringFromOrder() {
    var batches=mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper.class);
    var orders=mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper.class);
    ReflectionTestUtils.setField(service,"batches",batches);
    ReflectionTestUtils.setField(service,"activeOrders",orders);
    when(batches.selectById(4L)).thenReturn(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO()
      .setTenantId(71L).setWorkOrderId(12L).setRouteId(9L));
    assertEquals(9L,service.requireDeviationRouteId(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO()
      .setTenantId(71L).setBatchExecutionId(4L).setActiveOrderId(5L).setWorkOrderId(12L)));
    verifyNoInteractions(orders);
  }
  @Test void missingOrCrossTenantOrderCannotSupplyRoute() {
    var orders=mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper.class);
    ReflectionTestUtils.setField(service,"activeOrders",orders);
    var deviation=new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO()
      .setTenantId(71L).setActiveOrderId(5L).setWorkOrderId(12L);
    assertThrows(IllegalStateException.class,()->service.requireDeviationRouteId(deviation));
    var order=new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO()
      .setWorkOrderId(12L).setRouteId(8L);
    order.setTenantId(72L);
    when(orders.selectById(5L)).thenReturn(order);
    assertThrows(IllegalStateException.class,()->service.requireDeviationRouteId(deviation));
  }
}
