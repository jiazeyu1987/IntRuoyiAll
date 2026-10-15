package cn.iocoder.yudao.module.bpm.service.definition;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Model;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class BpmUnmanagedModelClaimServiceTest {
 BpmUnmanagedModelClaimService service;RepositoryService repository;PermissionApi permissions;GxpAuditService audit;Model model;
 @BeforeEach void setup(){service=new BpmUnmanagedModelClaimService();repository=mock(RepositoryService.class);permissions=mock(PermissionApi.class);audit=mock(GxpAuditService.class);model=mock(Model.class);
  ReflectionTestUtils.setField(service,"repository",repository);ReflectionTestUtils.setField(service,"permissions",permissions);ReflectionTestUtils.setField(service,"audit",audit);
  TenantContextHolder.setTenantId(1L);SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(1L).setTenantId(1L).setUserType(2),new MockHttpServletRequest());
  when(repository.getModel("model")).thenReturn(model);when(model.getId()).thenReturn("model");when(model.getTenantId()).thenReturn("1");when(model.getMetaInfo()).thenReturn("{\"type\":10,\"formType\":20,\"unknown\":\"retain\",\"managerUserIds\":[]}");
  when(permissions.hasAnyRoles(1L,"super_admin")).thenReturn(true);when(permissions.hasAnyPermissions(1L,"bpm:model:update")).thenReturn(true);
 }
 @AfterEach void clean(){TenantContextHolder.clear();SecurityContextHolder.clearContext();}
 @Test void claimsOnlyManagerMetadataAndAppendsAudit(){service.claim(1L,"model","本机维护认领");verify(model).setMetaInfo(argThat(v->v.contains("\"managerUserIds\":[1]")&&v.contains("\"unknown\":\"retain\"")&&v.contains("\"formType\":20")));verify(repository).saveModel(model);verify(audit).append(argThat(c->"bpm.model.manager.claim".equals(c.getOperationId())&&"model".equals(c.getSubjectId())));verify(repository,never()).addModelEditorSource(anyString(),any());}
 @Test void existingManagersCannotBeTakenOver(){when(model.getMetaInfo()).thenReturn("{\"managerUserIds\":[9]}");assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.claim(1L,"model","reason"));verify(repository,never()).saveModel(any());}
 @Test void otherTenantCannotBeClaimed(){when(model.getTenantId()).thenReturn("2");assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.claim(1L,"model","reason"));verify(repository,never()).saveModel(any());}
 @Test void roleOrPermissionMissingRejects(){when(permissions.hasAnyRoles(1L,"super_admin")).thenReturn(false);assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.claim(1L,"model","reason"));verify(repository,never()).saveModel(any());}
 @Test void permissionMissingRejects(){when(permissions.hasAnyPermissions(1L,"bpm:model:update")).thenReturn(false);assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.claim(1L,"model","reason"));verify(repository,never()).saveModel(any());}
 @Test void clientActorCannotReplaceLogin(){assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.claim(9L,"model","reason"));verify(repository,never()).saveModel(any());}
 @Test void blankReasonRejects(){assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.claim(1L,"model","  "));verify(repository,never()).saveModel(any());}
 @Test void auditFailureNeverReportsSuccess(){doThrow(new IllegalStateException("audit unavailable")).when(audit).append(any());assertThrows(IllegalStateException.class,()->service.claim(1L,"model","reason"));}
}
