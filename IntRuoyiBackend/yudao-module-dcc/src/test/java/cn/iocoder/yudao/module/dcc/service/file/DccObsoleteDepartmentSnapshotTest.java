package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTaskAssigneeSnapshotMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
class DccObsoleteDepartmentSnapshotTest {
 @Test void storesOfficialObligationDepartmentAndRejectsWrongSource(){
  TenantContextHolder.setTenantId(1L);
  try {
   var service=new DccObsoleteFormExecutionPreparationService();var mapper=mock(DccControlledFileTaskAssigneeSnapshotMapper.class);var users=mock(AdminUserApi.class);var departments=mock(DeptApi.class);
   ReflectionTestUtils.setField(service,"taskSnapshots",mapper);ReflectionTestUtils.setField(service,"users",users);ReflectionTestUtils.setField(service,"departments",departments);
   when(mapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
   when(mapper.insert(any(DccControlledFileTaskAssigneeSnapshotDO.class))).thenReturn(1);
   when(users.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0).setNickname("负责人").setDeptId(999L));
   when(departments.getDept(51L)).thenReturn(new DeptRespDTO().setId(51L).setName("义务部门"));
   var head=new DccObsoleteFormRuntimeSnapshotDO(1L,1L,2L,20L,5L,99L,"form","A/1","bpm",1,"def","digest","contract","{}","proof",null);
   var member=new DccObsoleteFormRuntimeObligationDO(1L,1L,1L,2L,20L,"bpm","obligation","MATRIX_REVIEW","node",1,1,51L,"DEPT","[51]","[99]","ANY",null,false,null);
   var runtime=new DccObsoleteFormRuntimeStore.RuntimeEvidence(head,List.of(member));service.freezeDepartmentAssignments(runtime);
   var captured=org.mockito.ArgumentCaptor.forClass(DccControlledFileTaskAssigneeSnapshotDO.class);verify(mapper).insert(captured.capture());assertEquals("义务部门",captured.getValue().getDepartmentName());assertEquals(51L,captured.getValue().getDepartmentId());
   when(departments.getDept(51L)).thenReturn(new DeptRespDTO().setId(52L).setName("错误部门"));assertThrows(IllegalArgumentException.class,()->service.freezeDepartmentAssignments(runtime));
   when(departments.getDept(51L)).thenReturn(null);assertThrows(IllegalArgumentException.class,()->service.freezeDepartmentAssignments(runtime));
   when(departments.getDept(51L)).thenReturn(new DeptRespDTO().setId(51L).setName("  "));assertThrows(IllegalArgumentException.class,()->service.freezeDepartmentAssignments(runtime));
   var historical=DccControlledFileTaskAssigneeSnapshotDO.builder().obligationId("obligation").departmentId(51L).leaderUserId(99L).departmentName(null).build();
   when(mapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(historical));
   clearInvocations(mapper,departments);service.freezeDepartmentAssignments(runtime);
   verifyNoInteractions(departments);verify(mapper,never()).insert(any(DccControlledFileTaskAssigneeSnapshotDO.class));
   verifyNoMoreInteractions(departments);assertNull(historical.getDepartmentName());
   when(mapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
   TenantContextHolder.setTenantId(2L);assertThrows(IllegalArgumentException.class,()->service.freezeDepartmentAssignments(runtime));
  } finally {TenantContextHolder.clear();}
 }
}