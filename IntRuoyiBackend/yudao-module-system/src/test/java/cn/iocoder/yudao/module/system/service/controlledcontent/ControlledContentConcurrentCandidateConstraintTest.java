package cn.iocoder.yudao.module.system.service.controlledcontent;

import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentTransitionAuditDO;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentVersionRefDO;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentTransitionAuditMapper;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentVersionRefMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.ACTIVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.MES_ROUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlledContentConcurrentCandidateConstraintTest {

    @Mock
    private ControlledContentVersionRefMapper versionRefMapper;
    @Mock
    private ControlledContentTransitionAuditMapper transitionAuditMapper;

    private ControlledContentLifecycleCoreService lifecycleCoreService;

    @BeforeEach
    void setUp() {
        lifecycleCoreService = new ControlledContentLifecycleCoreService(versionRefMapper, transitionAuditMapper,
                new ControlledContentStateMachine());
    }

    @Test
    void createCandidateRef_whenAnotherRequestAlreadyOpenedCandidate_failsBeforeNativeRefOrAuditInsert() {
        ControlledContentKey key = ControlledContentKey.of(122L, MES_ROUTE, "9001");
        when(versionRefMapper.selectOpenCandidate(122L, "MES_ROUTE", "9001")).thenReturn(null);
        when(versionRefMapper.selectById(9000L)).thenReturn(ControlledContentVersionRefDO.builder()
                .id(9000L)
                .tenantId(122L)
                .contentType("MES_ROUTE")
                .contentKey("9001")
                .nativeVersionId(9001L)
                .canonicalStatus(ACTIVE.name())
                .activeUniqueFlag(1)
                .build());
        doThrow(new DuplicateKeyException("duplicate open candidate"))
                .when(versionRefMapper).insert(any(ControlledContentVersionRefDO.class));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> lifecycleCoreService.createCandidateRef(key, 9001L, 9003L, "V3", "DRAFT",
                        9000L, 9001L, 501L, "concurrent candidate"));

        assertEquals("controlled content already has an open candidate: concurrent create candidate",
                exception.getMessage());
        verify(transitionAuditMapper, never()).insert(any(ControlledContentTransitionAuditDO.class));
    }

    @Test
    void createCandidateRef_whenServicePrecheckFindsOpenCandidate_failsBeforeInsertOrAuditInsert() {
        ControlledContentKey key = ControlledContentKey.of(122L, MES_ROUTE, "9001");
        when(versionRefMapper.selectOpenCandidate(122L, "MES_ROUTE", "9001"))
                .thenReturn(ControlledContentVersionRefDO.builder()
                        .id(9002L)
                        .tenantId(122L)
                        .contentType("MES_ROUTE")
                        .contentKey("9001")
                        .versionNo("V2")
                        .canonicalStatus("DRAFT")
                        .openCandidateUniqueFlag(1)
                        .build());

        assertThrows(IllegalStateException.class,
                () -> lifecycleCoreService.createCandidateRef(key, 9001L, 9003L, "V3", "DRAFT",
                        9000L, 9001L, 501L, "concurrent candidate"));

        verify(versionRefMapper, never()).insert(any(ControlledContentVersionRefDO.class));
        verify(transitionAuditMapper, never()).insert(any(ControlledContentTransitionAuditDO.class));
    }

}
