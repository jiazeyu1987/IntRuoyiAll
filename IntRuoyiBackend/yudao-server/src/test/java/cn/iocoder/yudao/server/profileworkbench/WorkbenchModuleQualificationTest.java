package cn.iocoder.yudao.server.profileworkbench;
import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccWorkbenchTodoMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.MesWorkbenchTodoMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomWorkbenchTodoMapper;
import cn.iocoder.yudao.module.system.dal.mysql.profileworkbench.WorkbenchVisibilityVersionMapper;
import cn.iocoder.yudao.module.system.service.profileworkbench.*;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.apache.ibatis.annotations.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class WorkbenchModuleQualificationTest {
 QueryContext q(){return new WorkbenchQuerySpec("visible",null,null,null).context(2,3,1).partition(null);}
 @Test void distributionUsesFileRecipientOriginalDistributionAndRejectsChangedParent(){
  var mapper=mock(DccWorkbenchTodoMapper.class,CALLS_REAL_METHODS);var q=q();when(mapper.distributionParent(q,7)).thenReturn(9L);when(mapper.distributionFile(q,9)).thenReturn(11L);when(mapper.lockFile(q,11)).thenReturn(11L);when(mapper.lockRecipient(q,7)).thenReturn(9L);when(mapper.lockDistribution(q,9)).thenReturn(11L);clearInvocations(mapper);
  mapper.lockDistributionIdentity(q,7);var order=inOrder(mapper);order.verify(mapper).lockFile(q,11);order.verify(mapper).lockRecipient(q,7);order.verify(mapper).lockDistribution(q,9);
  when(mapper.lockRecipient(q,7)).thenReturn(99L);clearInvocations(mapper);assertEquals(WorkbenchFailure.TASK_NOT_ELIGIBLE.code(),assertThrows(ServiceException.class,()->mapper.lockDistributionIdentity(q,7)).getCode());verify(mapper,never()).lockDistribution(q,99);verify(mapper,never()).lockDistribution(q,9);
 }
 @Test void trainingHasNoWaitingBusinessLockAndRejectsMovedProgress(){
  var mapper=mock(DccWorkbenchTodoMapper.class,CALLS_REAL_METHODS);var q=q();when(mapper.trainingFile(q,7)).thenReturn(11L);when(mapper.lockTrainingFile(q,11)).thenReturn(11L);when(mapper.lockTrainingProgress(q,7)).thenReturn(11L);clearInvocations(mapper);mapper.lockTrainingIdentity(q,7);var order=inOrder(mapper);order.verify(mapper).lockTrainingFile(q,11);order.verify(mapper).lockTrainingProgress(q,7);
  for(String name:List.of("lockTrainingFile","lockTrainingProgress")){try{var method=DccWorkbenchTodoMapper.class.getMethod(name,QueryContext.class,long.class);assertTrue(method.getAnnotation(Select.class).value()[0].endsWith("FOR UPDATE NOWAIT"));}catch(Exception e){throw new AssertionError(e);}}
  when(mapper.lockTrainingProgress(q,7)).thenReturn(12L);assertEquals(WorkbenchFailure.TASK_NOT_ELIGIBLE.code(),assertThrows(ServiceException.class,()->mapper.lockTrainingIdentity(q,7)).getCode());
 }
 @Test void mesReturnsOnlyExplicitlyLockedBatchStatusAndDoesNotChaseChangedReference(){
  var mapper=mock(MesWorkbenchTodoMapper.class,CALLS_REAL_METHODS);var q=q();when(mapper.edhrBatch(q,7)).thenReturn(9L);when(mapper.lockBatch(q,9)).thenReturn(30L);when(mapper.lockEdhr(q,7)).thenReturn(7L);when(mapper.lockedEdhrBatch(q,7)).thenReturn(9L);clearInvocations(mapper);assertEquals(30,mapper.lockEdhrIdentity(q,7));var order=inOrder(mapper);order.verify(mapper).lockBatch(q,9);order.verify(mapper).lockEdhr(q,7);order.verify(mapper).lockedEdhrBatch(q,7);
  when(mapper.lockedEdhrBatch(q,7)).thenReturn(10L);assertThrows(ServiceException.class,()->mapper.lockEdhrIdentity(q,7));verify(mapper,never()).lockBatch(q,10);
 }
 @Test void showroomLocksOriginalNotificationAndBrokenLinkFails(){
  var mapper=mock(ShowroomWorkbenchTodoMapper.class,CALLS_REAL_METHODS);var q=q();when(mapper.lockAssignment(q,7)).thenReturn(9L);when(mapper.lockNotification(q,9)).thenReturn(9L);clearInvocations(mapper);mapper.lockAssignmentIdentity(q,7);var order=inOrder(mapper);order.verify(mapper).lockAssignment(q,7);order.verify(mapper).lockNotification(q,9);when(mapper.lockNotification(q,9)).thenReturn(null);assertThrows(ServiceException.class,()->mapper.lockAssignmentIdentity(q,7));
 }
 @Test void allAnnotatedQualificationAndPersonalStatementsHaveExplicitTenSecondTimeout()throws Exception{
  for(Class<?> type:List.of(DccWorkbenchTodoMapper.class,MesWorkbenchTodoMapper.class,ShowroomWorkbenchTodoMapper.class,WorkbenchVisibilityVersionMapper.class))for(var method:type.getMethods())if(method.isAnnotationPresent(Select.class)||method.isAnnotationPresent(Insert.class)||method.isAnnotationPresent(Update.class)){assertNotNull(method.getAnnotation(Options.class),method.toString());assertEquals(10,method.getAnnotation(Options.class).timeout());}
  var initializer=WorkbenchVisibilityContextInitializer.class.getMethod("initialize",long.class,long.class).getAnnotation(org.springframework.transaction.annotation.Transactional.class);assertEquals(10,initializer.timeout());assertEquals(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW,initializer.propagation());assertFalse(initializer.readOnly());
 }
}
