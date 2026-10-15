package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;import static org.mockito.ArgumentMatchers.*;

class DccApprovalMatrixBaselineServiceTest {
    DccApprovalMatrixSourceService sources;DccControlledFileMapper files;DccApprovalMatrixTokenService tokens;DccApprovalMatrixBaselineService service;
    DccApprovalMatrixSourceService.Source initial,current;
    @BeforeEach void fixture()throws Exception{
        TenantContextHolder.setTenantId(1L);sources=mock(DccApprovalMatrixSourceService.class);files=mock(DccControlledFileMapper.class);
        var key=new DccApprovalMatrixTokenProperties();key.setSigningSecret("w05-matrix-confirmation-isolated-key-at-least-32-bytes");tokens=new DccApprovalMatrixTokenService();ReflectionTestUtils.setField(tokens,"properties",key);
        service=new DccApprovalMatrixBaselineService();ReflectionTestUtils.setField(service,"sources",sources);ReflectionTestUtils.setField(service,"tokens",tokens);ReflectionTestUtils.setField(service,"files",files);
        Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        var raw=JsonUtils.parseObject(Files.readString(root.resolve("doc/tasks/20261011-dcc-w02-types-matrix/immutable-public-matrix-v2.fixture.json")),Map.class);
        initial=source(raw);raw.put("routeId","9007199254741100");raw.put("routeVersionNo",3);current=source(raw);
        when(sources.read(7L)).thenReturn(initial);when(sources.readForUpdate(7L)).thenReturn(initial);
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    DccApprovalMatrixSourceService.Source source(Map raw){String canonical=DccApprovalMatrixCanonicalizer.canonicalize(raw);var baseline=DccApprovalMatrixSourceService.readCanonical(canonical);return new DccApprovalMatrixSourceService.Source(DccCategoryApprovalRouteDO.builder().id(baseline.getRouteId()).versionNo(baseline.getRouteVersionNo()).actionType("LEGACY").build(),List.of(),baseline,canonical);}
    DccControlledFileRoutePreviewReqVO request(){var request=new DccControlledFileRoutePreviewReqVO();request.setCategoryId(7L);request.setDccProjectCodeId(2L);request.setActionType("NEW");request.setApplicationContextKey("this-application-context");return request;}
    DccApprovalMatrixBaselineService.Expectations expected(DccApprovalMatrixBaselineService.Comparison comparison,Boolean confirmed){var after=comparison.after().baseline();return new DccApprovalMatrixBaselineService.Expectations(after.getRouteId(),after.getRouteVersionNo(),after.getMatrixDigest(),comparison.baselineToken(),comparison.confirmationToken(),confirmed,"this-application-context");}
    @Test void firstFormalComparisonAndChangedAfterRequireExactExplicitConfirmation(){var request=request();var first=service.preview(9L,request);assertFalse(first.changed());request.setApprovalMatrixBaselineToken(first.baselineToken());when(sources.read(7L)).thenReturn(current);when(sources.readForUpdate(7L)).thenReturn(current);var changed=service.preview(9L,request);assertTrue(changed.changed());assertEquals(initial.canonical(),changed.beforeCanonical());assertThrows(IllegalStateException.class,()->service.confirm(9L,2L,7L,null,"NEW",expected(changed,false)));var result=service.confirm(9L,2L,7L,null,"NEW",expected(changed,true));assertEquals(current.baseline().getRouteId(),result.source().baseline().getRouteId());verifyNoInteractions(files);}
    @Test void expectedAfterStaleAndMissingBaselineNeverUsesLatestAsBefore(){var first=service.preview(9L,request());when(sources.readForUpdate(7L)).thenReturn(current);assertThrows(IllegalStateException.class,()->service.confirm(9L,2L,7L,null,"NEW",expected(first,true)));var absent=new DccApprovalMatrixBaselineService.Expectations(current.baseline().getRouteId(),3,current.baseline().getMatrixDigest(),null,null,true,"this-application-context");assertThrows(IllegalArgumentException.class,()->service.confirm(9L,2L,7L,null,"NEW",absent));}
    @Test void savedWorkingComparisonKeepsDurableOriginalBaseline(){var file=DccControlledFileDO.builder().id(5L).tenantId(1L).requesterId(9L).status("WORKING").dccProjectCodeId(2L).draftApprovalMatrixJson(initial.canonical()).build();when(files.selectById(5L)).thenReturn(file);when(sources.read(7L)).thenReturn(current);var request=request();request.setControlledFileId(5L);var compared=service.preview(9L,request);assertTrue(compared.changed());assertEquals(initial.canonical(),compared.beforeCanonical());assertEquals(initial.canonical(),file.getDraftApprovalMatrixJson());}
    @Test void existingWorkingWithoutBaselineCannotBeRebasedSilentlyToCurrent(){var file=DccControlledFileDO.builder().id(5L).tenantId(1L).requesterId(9L).status("WORKING").dccProjectCodeId(2L).build();when(files.selectById(5L)).thenReturn(file);var request=request();request.setControlledFileId(5L);assertThrows(IllegalStateException.class,()->service.preview(9L,request));assertNull(file.getDraftApprovalMatrixJson());}
    @Test void realCreationThenFilePreviewTokenCanSaveAndConfirmWithoutChangingOriginalBaseline(){
        var config=new com.baomidou.mybatisplus.core.MybatisConfiguration();var builder=new org.apache.ibatis.builder.MapperBuilderAssistant(config,"ic13-save-token");builder.setCurrentNamespace(DccControlledFileMapper.class.getName());com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(builder,DccControlledFileDO.class);
        var ds=new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:h2:mem:matrix_token_save_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa","");var jdbc=new org.springframework.jdbc.core.JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE draft(id BIGINT PRIMARY KEY,baseline CLOB)");jdbc.update("INSERT INTO draft VALUES(5,NULL)");
        when(files.update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenAnswer(call->{var wrapper=(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DccControlledFileDO>)call.getArgument(1);var json=wrapper.getParamNameValuePairs().values().stream().filter(v->v instanceof String&&((String)v).startsWith("{\"actionType\"")).findFirst().orElseThrow();return jdbc.update("UPDATE draft SET baseline=? WHERE id=5 AND baseline IS NULL",json);});
        var file=DccControlledFileDO.builder().id(5L).tenantId(1L).requesterId(9L).status("WORKING").categoryId(7L).dccProjectCodeId(2L).changeType("NEW").build();when(files.selectById(5L)).thenReturn(file);
        var tx=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds));
        var first=service.preview(9L,request());tx.executeWithoutResult(status->service.initializeDraft(9L,file,first.baselineToken(),"this-application-context"));
        var savedRequest=request();savedRequest.setControlledFileId(5L);when(sources.read(7L)).thenReturn(current);when(sources.readForUpdate(7L)).thenReturn(current);
        var after=service.preview(9L,savedRequest);assertTrue(after.changed());
        tx.executeWithoutResult(status->service.initializeDraft(9L,file,after.baselineToken(),"this-application-context"));
        assertEquals(initial.canonical(),jdbc.queryForObject("SELECT baseline FROM draft WHERE id=5",String.class));
        var confirmed=tx.execute(status->service.confirm(9L,2L,7L,5L,"NEW",expected(after,true)));assertEquals(current.baseline().getRouteId(),confirmed.source().baseline().getRouteId());
        assertThrows(IllegalArgumentException.class,()->tx.executeWithoutResult(status->service.initializeDraft(9L,file,first.baselineToken(),"this-application-context")));
        verify(files,times(1)).update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }
    @Test void durableBaselineCasAndOuterLateFailureUseRealPrivateTransactionRatherThanOverwritingBefore(){
        var configuration=new com.baomidou.mybatisplus.core.MybatisConfiguration();var builder=new org.apache.ibatis.builder.MapperBuilderAssistant(configuration,"ic13-baseline-cas");builder.setCurrentNamespace(DccControlledFileMapper.class.getName());com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(builder,DccControlledFileDO.class);
        var dataSource=new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:h2:mem:matrix_cas_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa","");var jdbc=new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE draft(id BIGINT PRIMARY KEY,baseline CLOB)");jdbc.update("INSERT INTO draft VALUES(5,NULL)");
        when(files.update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenAnswer(call->{var wrapper=(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DccControlledFileDO>)call.getArgument(1);var json=wrapper.getParamNameValuePairs().values().stream().filter(v->v instanceof String&&((String)v).startsWith("{\"actionType\"")).findFirst().orElseThrow();return jdbc.update("UPDATE draft SET baseline=? WHERE id=5 AND baseline IS NULL",json);});
        var file=DccControlledFileDO.builder().id(5L).tenantId(1L).requesterId(9L).status("WORKING").categoryId(7L).dccProjectCodeId(2L).changeType("NEW").build();
        var comparison=service.preview(9L,request());var tx=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource));
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(status->{service.initializeDraft(9L,file,comparison.baselineToken(),"this-application-context");throw new IllegalStateException("late body/attribute failure");}));
        assertNull(jdbc.queryForObject("SELECT baseline FROM draft WHERE id=5",String.class));file.setDraftApprovalMatrixJson(null);
        tx.executeWithoutResult(status->service.initializeDraft(9L,file,comparison.baselineToken(),"this-application-context"));
        assertEquals(initial.canonical(),jdbc.queryForObject("SELECT baseline FROM draft WHERE id=5",String.class));
        when(sources.read(7L)).thenReturn(current);var different=service.preview(9L,request());
        assertThrows(IllegalArgumentException.class,()->tx.executeWithoutResult(status->service.initializeDraft(9L,file,different.baselineToken(),"this-application-context")));
        assertEquals(initial.canonical(),jdbc.queryForObject("SELECT baseline FROM draft WHERE id=5",String.class));
    }
}
