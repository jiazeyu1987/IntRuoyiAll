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
}
