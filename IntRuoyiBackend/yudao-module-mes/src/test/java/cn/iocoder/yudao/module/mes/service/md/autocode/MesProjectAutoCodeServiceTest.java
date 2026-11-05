package cn.iocoder.yudao.module.mes.service.md.autocode;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.api.projectcode.DccProjectCreatedEvent;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.autocode.*;
import cn.iocoder.yudao.module.mes.dal.mysql.md.autocode.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MesProjectAutoCodeServiceTest {
    private MesProjectAutoCodeService service;
    private DccProjectCodeMapper projects;
    private DccProjectAccessService access;
    private MesMdAutoCodeRuleMapper rules;
    private MesMdAutoCodePartMapper parts;
    private JdbcTemplate jdbc;
    private List<MesMdAutoCodePartDO> stored;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(170L);
        service=new MesProjectAutoCodeService(); projects=mock(DccProjectCodeMapper.class);
        access=mock(DccProjectAccessService.class); rules=mock(MesMdAutoCodeRuleMapper.class);
        parts=mock(MesMdAutoCodePartMapper.class); jdbc=mock(JdbcTemplate.class); stored=new ArrayList<>();
        ReflectionTestUtils.setField(service,"projectMapper",projects); ReflectionTestUtils.setField(service,"projectAccess",access);
        ReflectionTestUtils.setField(service,"ruleMapper",rules); ReflectionTestUtils.setField(service,"partMapper",parts);
        ReflectionTestUtils.setField(service,"jdbc",jdbc);
        when(projects.selectOne(any(Wrapper.class))).thenReturn(DccProjectCodeDO.builder().id(42L).projectCode("QSW").status("ENABLE").build());
        when(jdbc.queryForObject(anyString(),eq(Long.class),any(Object[].class))).thenReturn(0L);
        when(rules.insert(any(MesMdAutoCodeRuleDO.class))).thenAnswer(c -> { ((MesMdAutoCodeRuleDO)c.getArgument(0)).setId(10L);return 1; });
        when(parts.insert(any(MesMdAutoCodePartDO.class))).thenAnswer(c -> {stored.add(c.getArgument(0));return 1;});
        when(parts.selectListByRuleId(anyLong())).thenAnswer(c -> stored);
    }
    @AfterEach void close() { TenantContextHolder.clear(); }
    @Test void missingRuleInitializesProjectPrefixAndSerial() {
        var r=service.ensure(42L,"PRO_PROCESS_GX_CODE");
        assertEquals(42L,r.getProjectId());assertEquals("QSW-GX-",stored.get(0).getFixCharacter());
        assertEquals(1,stored.get(1).getSerialStartNo());assertEquals(1,stored.get(1).getSerialStep());
        verify(access).assertProjectViewerOrAbove(null,42L);
    }
    @Test void recreatedRuleContinuesHistoricalNumbersIncludingDeletedRows() {
        when(jdbc.queryForObject(anyString(),eq(Long.class),any(Object[].class))).thenReturn(37L);
        service.ensure(42L,"PRO_ROUTE_CODE");assertEquals(38,stored.get(1).getSerialStartNo());
        assertEquals("QSW-GYLC-",stored.get(0).getFixCharacter());
    }
    @Test void existingCustomRuleIsNotOverwrittenOrReset() {
        var r=service.ensure(42L,"PRO_PROCESS_GX_CODE");
        stored.get(0).setFixCharacter("CUSTOM-").setLength(7);stored.get(1).setSerialStartNo(15);
        clearInvocations(rules,parts,jdbc);
        when(rules.selectOne(any(Wrapper.class))).thenReturn(r);
        assertSame(r,service.ensure(42L,"PRO_PROCESS_GX_CODE"));
        verify(rules,never()).insert(any(MesMdAutoCodeRuleDO.class));verify(parts,never()).insert(any(MesMdAutoCodePartDO.class));
        verifyNoInteractions(jdbc);assertEquals("CUSTOM-",stored.get(0).getFixCharacter());assertEquals(15,stored.get(1).getSerialStartNo());
    }
    @Test void projectsHaveSeparateRulesAndPrefixes() {
        var first=service.ensure(42L,"PRO_ROUTE_CODE");stored.clear();
        when(projects.selectOne(any(Wrapper.class))).thenReturn(DccProjectCodeDO.builder().id(43L).projectCode("IDI").status("ENABLE").build());
        var second=service.ensure(43L,"PRO_ROUTE_CODE");
        assertNotEquals(first.getProjectId(),second.getProjectId());assertEquals("IDI-GYLC-",stored.get(0).getFixCharacter());
    }
    @Test void disabledExistingRuleIsNotReplaced() {
        when(rules.selectOne(any(Wrapper.class))).thenReturn(MesMdAutoCodeRuleDO.builder().id(10L).status(1).build());
        assertThrows(ServiceException.class,()->service.ensure(42L,"PRO_ROUTE_CODE"));
        verify(rules,never()).insert(any(MesMdAutoCodeRuleDO.class));verifyNoInteractions(jdbc);
    }
    @Test void emptyExistingRuleFailsWithoutAddingParts() {
        when(rules.selectOne(any(Wrapper.class))).thenReturn(MesMdAutoCodeRuleDO.builder().id(10L).status(0).build());
        assertThrows(ServiceException.class,()->service.ensure(42L,"PRO_ROUTE_CODE"));
        verify(parts,never()).insert(any(MesMdAutoCodePartDO.class));
    }
    @Test void malformedSerialFailsWithoutDefaulting() {
        var r=service.ensure(42L,"PRO_ROUTE_CODE"); when(rules.selectOne(any(Wrapper.class))).thenReturn(r);
        stored.get(1).setSerialStep(0);assertThrows(ServiceException.class,()->service.ensure(42L,"PRO_ROUTE_CODE"));
    }
    @Test void unknownRuleAndMissingProjectFailWithoutWrites() {
        assertThrows(ServiceException.class,()->service.ensure(42L,"UNKNOWN"));
        assertThrows(ServiceException.class,()->service.ensure(null,"PRO_ROUTE_CODE"));
        verifyNoInteractions(rules,parts,jdbc);
    }
    @Test void inaccessibleProjectCannotInitialize() {
        doThrow(new IllegalArgumentException("denied")).when(access).assertProjectViewerOrAbove(null,42L);
        assertThrows(IllegalArgumentException.class,()->service.ensure(42L,"PRO_ROUTE_CODE"));
        verifyNoInteractions(projects,rules,parts,jdbc);
    }
    @Test void disabledProjectCannotInitialize() {
        when(projects.selectOne(any(Wrapper.class))).thenReturn(DccProjectCodeDO.builder().id(42L).status("DISABLE").build());
        assertThrows(ServiceException.class,()->service.ensure(42L,"PRO_ROUTE_CODE"));verifyNoInteractions(rules,parts,jdbc);
    }
    @Test void projectCreationInitializesBothTemplates() {
        when(parts.selectListByRuleId(anyLong())).thenAnswer(c -> stored.subList(stored.size()-2,stored.size()));
        service.initialize(new DccProjectCreatedEvent(42L));
        verify(rules,times(2)).insert(any(MesMdAutoCodeRuleDO.class));assertEquals(4,stored.size());
        assertEquals(Set.of("QSW-GX-","QSW-GYLC-"),Set.of(stored.get(0).getFixCharacter(),stored.get(2).getFixCharacter()));
    }
}
