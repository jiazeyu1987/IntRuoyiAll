package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccAutomaticDistributionRetryReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DccAutomaticDistributionRetryTest extends BaseMockitoUnitTest {
    @Mock DccControlledFileMapper controlledFileMapper;
    @Mock DccDistributionBatchMapper batchMapper;
    @Mock DccDistributionBatchService batchService;
    @Mock PermissionApi permissionApi;
    @Mock AdminUserApi adminUserApi;
    @Mock DccProjectAccessService projectAccessService;
    @Mock DccControlledFileCategoryPermissionSupport permissionSupport;
    @InjectMocks DccDistributionTaskServiceImpl service;
    DccControlledFileDO file;DccDistributionBatchDO batch;DccAutomaticDistributionRetryReqVO request;
    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);var now=LocalDateTime.of(2026,10,13,9,0);
        file=DccControlledFileDO.builder().id(42L).categoryId(15L).dccProjectCodeId(31L).versionNo("B/1").processInstanceId("round42")
                .status("ACTIVE").controlledTime(now).activatedTime(now).build();file.setTenantId(1L);
        batch=DccDistributionBatchDO.builder().id(51L).tenantId(1L).controlledFileId(42L).versionNo("B/1").approvalProcessInstanceId("round42").status("FAILED").build();
        request=new DccAutomaticDistributionRetryReqVO();request.setControlledFileId(42L);request.setBatchId(51L);request.setVersionNo("B/1");request.setApprovalProcessInstanceId("round42");
        lenient().when(controlledFileMapper.selectById(42L)).thenReturn(file);lenient().when(batchMapper.selectById(51L)).thenReturn(batch);lenient().when(batchMapper.selectVersion(1L,42L,"round42")).thenReturn(batch);
        lenient().when(permissionApi.hasAnyRoles(99L,"doc_control")).thenReturn(true);
        lenient().when(permissionApi.hasAnyPermissions(99L,"dcc:controlled-file:distribute")).thenReturn(true);
        lenient().when(permissionSupport.hasCategoryPermission(any(),any(),any())).thenReturn(true);
        lenient().when(projectAccessService.listReadableProjectIds(99L)).thenReturn(List.of(31L));
        lenient().when(adminUserApi.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0));
        var login=new cn.iocoder.yudao.framework.security.core.LoginUser().setId(99L).setTenantId(1L).setUserType(2);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(login,null));
        lenient().when(batchService.retryAutomaticBatch(51L,99L)).thenReturn(new DccDistributionBatchResult(51L,"FAILED","ROSTER_SOURCE_MISSING","缺正式来源"));
    }
    @AfterEach void clear(){TenantContextHolder.clear();org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    Object retry()throws Exception {
        try{return service.getClass().getMethod("retryAutomaticDistribution",Long.class,DccAutomaticDistributionRetryReqVO.class).invoke(service,99L,request);}
        catch(java.lang.reflect.InvocationTargetException error){if(error.getCause() instanceof RuntimeException runtime)throw runtime;throw error;}
    }
    @Test void authorizedExactRetryCallsOnlyServerEntryAndReturnsTruthfulBatchOutcome()throws Exception {
        assertDoesNotThrow(()->service.getClass().getMethod("retryAutomaticDistribution",Long.class,DccAutomaticDistributionRetryReqVO.class));
        retry();verify(batchService).retryAutomaticBatch(51L,99L);
    }
    @Test void permissionProjectCategoryRoleAndCurrentAccountAreIndependentBoundaries() {
        for(String missing:List.of("function","project","category","role","account","tenant")) {
            lenient().when(permissionApi.hasAnyPermissions(99L,"dcc:controlled-file:distribute")).thenReturn(!missing.equals("function"));
            lenient().when(permissionApi.hasAnyRoles(99L,"doc_control")).thenReturn(!missing.equals("role"));
            lenient().when(permissionSupport.hasCategoryPermission(any(),any(),any())).thenReturn(!missing.equals("category"));
            lenient().when(projectAccessService.listReadableProjectIds(99L)).thenReturn(missing.equals("project")?List.of():List.of(31L));
            lenient().when(adminUserApi.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(missing.equals("tenant")?2L:1L).setStatus(missing.equals("account")?1:0));
            assertThrows(Exception.class,this::retry,missing);
        }
        verifyNoInteractions(batchService);
    }
    @Test void expectedVersionRoundBatchAndActualActivationCannotBeForged() {
        request.setVersionNo("C/1");assertThrows(Exception.class,this::retry);request.setVersionNo("B/1");
        request.setApprovalProcessInstanceId("another-round");assertThrows(Exception.class,this::retry);request.setApprovalProcessInstanceId("round42");
        batch.setControlledFileId(43L);assertThrows(Exception.class,this::retry);batch.setControlledFileId(42L);
        file.setActivatedTime(null);assertThrows(Exception.class,this::retry);verifyNoInteractions(batchService);
    }
    @Test void clientsCannotSupplyApprovedBooleanOrRecipientList() {
        assertThrows(IllegalArgumentException.class,()->request.rejectUnexpectedField("rosterPolicyDecided",true));
        assertThrows(IllegalArgumentException.class,()->request.rejectUnexpectedField("recipientUserIds",List.of(99L)));
        try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()){assertEquals(4,factory.getValidator().validate(new DccAutomaticDistributionRetryReqVO()).size());}
    }
    @Test void actualResultJsonPreservesHighLongsAsDecimalStrings()throws Exception {
        var response=new cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccAutomaticDistributionResultRespVO();
        response.setControlledFileId(9007199254740993L);response.setBatchId(Long.MAX_VALUE);response.setVersionNo("B/1");response.setStatus("FAILED");
        var json=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(response);
        var node=new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
        assertTrue(node.get("controlledFileId").isTextual());assertEquals("9007199254740993",node.get("controlledFileId").textValue());
        assertTrue(node.get("batchId").isTextual());assertEquals("9223372036854775807",node.get("batchId").textValue());
    }
    @Test void actualJacksonRejectsCustomerRosterAndPolicyEvenWhenUnknownFieldsNormallyIgnored()throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper().disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        String legitimate="{\"controlledFileId\":\"9007199254740993\",\"batchId\":\"9223372036854775807\",\"versionNo\":\"B/1\",\"approvalProcessInstanceId\":\"round42\"}";
        var valid=mapper.readValue(legitimate,DccAutomaticDistributionRetryReqVO.class);assertEquals(9007199254740993L,valid.getControlledFileId());assertEquals(Long.MAX_VALUE,valid.getBatchId());
        for(String forbidden:List.of("\"rosterPolicyDecided\":true","\"recipientUserIds\":[99]","\"sourceIdentity\":\"customer\""))
            assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class,()->mapper.readValue(legitimate.substring(0,legitimate.length()-1)+","+forbidden+"}",DccAutomaticDistributionRetryReqVO.class));
    }
}
