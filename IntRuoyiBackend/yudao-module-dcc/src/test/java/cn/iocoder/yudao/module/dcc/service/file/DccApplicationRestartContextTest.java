package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.flowable.engine.history.HistoricProcessInstance;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DccApplicationRestartContextTest {
    DccControlledFileQueryServiceImpl service;
    BpmProcessInstanceService bpm;
    DccControlledFileMapper files;
    FormActionInstanceMapper forms;
    HistoricProcessInstance history;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        service=new DccControlledFileQueryServiceImpl();bpm=mock(BpmProcessInstanceService.class);files=mock(DccControlledFileMapper.class);forms=mock(FormActionInstanceMapper.class);
        var permission=mock(PermissionApi.class);when(permission.hasAnyPermissions(eq(99L),any(String[].class))).thenReturn(true);
        var categories=mock(DccControlledFileCategoryPermissionSupport.class);when(categories.hasCategoryPermission(any(),any(),any())).thenReturn(true);
        var access=mock(DccProjectAccessService.class);when(access.hasProjectEditorOrOwner(99L,5L)).thenReturn(true);
        var rounds=mock(DccApplicationRoundService.class);when(rounds.require("OBSOLETE",20L,"exact-bpm")).thenReturn(7);
        Object[] dependencies={"bpmProcessInstanceService",bpm,"controlledFileMapper",files,"restartFormMapper",forms,"permissionApi",permission,"permissionSupport",categories,"projectAccessService",access,"applicationRoundService",rounds};
        for(int i=0;i<dependencies.length;i+=2)ReflectionTestUtils.setField(service,(String)dependencies[i],dependencies[i+1]);
        history=mock(HistoricProcessInstance.class);when(bpm.getHistoricProcessInstance("exact-bpm")).thenReturn(history);
        when(history.getId()).thenReturn("exact-bpm");when(history.getTenantId()).thenReturn("1");when(history.getStartUserId()).thenReturn("99");when(history.getEndTime()).thenReturn(new Date());
    }
    @AfterEach void cleanup(){TenantContextHolder.clear();}
    @ParameterizedTest @CsvSource({"UPLOAD,REJECTED,3","UPLOAD,WITHDRAWN,4","REVISION,REJECTED,3","REVISION,WITHDRAWN,4","OBSOLETE,REJECTED,3","OBSOLETE,ABANDONED,4"})
    void exactFailedMessageRoundRestartsEachApplicationKind(String kind,String status,int outcome) {
        String key="dcc-controlled-file-"+kind.toLowerCase(Locale.ROOT);
        when(history.getProcessDefinitionKey()).thenReturn(key);when(history.getProcessVariables()).thenReturn(Map.of("PROCESS_STATUS",outcome));
        var file=DccControlledFileDO.builder().id(20L).tenantId(1L).dccProjectCodeId(5L).categoryId(2L).requesterId(99L).versionNo("A/1").status(status).processInstanceId("exact-bpm").processDefinitionKey(key)
                .changeType("UPLOAD".equals(kind)?"NEW":"REVISION").revisionChangeType("UPLOAD".equals(kind)?"INITIAL":"PARTIAL").build();
        if("OBSOLETE".equals(kind)) {
            file.setStatus("ACTIVE");file.setControlledTime(LocalDateTime.now());
            when(history.getBusinessKey()).thenReturn("FORM_ACTION:original-form-code");
            var form=FormActionInstanceDO.builder().id(50L).tenantId(1L).applicantUserId(99L).dataDomain("DCC").systemCode("DCC").objectType("CONTROLLED_FILE").objectId("20").objectVersion("A/1").actionCode("OBSOLETE").instanceCode("original-form-code").bpmProcessInstanceId("exact-bpm").status(status).build();
            when(forms.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(form);when(files.selectById(20L)).thenReturn(file);
        } else {
            when(history.getBusinessKey()).thenReturn("20");when(files.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(file);
        }
        var context=service.getApplicationRestartContext(99L,"exact-bpm");assertNotNull(context);assertEquals(kind,context.get("applicationType"));assertEquals("20",context.get("controlledFileId"));
        if("OBSOLETE".equals(kind)){assertEquals("50",context.get("reworkSourceFormInstanceId"));assertEquals("original-form-code",context.get("reworkSourceFormInstanceCode"));assertEquals(7,context.get("reworkSourceAttributeRound"));}
        assertNull(service.getApplicationRestartContext(88L,"exact-bpm"));
        TenantContextHolder.setTenantId(2L);assertNull(service.getApplicationRestartContext(99L,"exact-bpm"));TenantContextHolder.setTenantId(1L);
        when(history.getProcessVariables()).thenReturn(Map.of("PROCESS_STATUS",2));assertNull(service.getApplicationRestartContext(99L,"exact-bpm"));
        when(history.getProcessVariables()).thenReturn(Map.of("PROCESS_STATUS",outcome));when(history.getEndTime()).thenReturn(null);assertNull(service.getApplicationRestartContext(99L,"exact-bpm"));
    }
}
