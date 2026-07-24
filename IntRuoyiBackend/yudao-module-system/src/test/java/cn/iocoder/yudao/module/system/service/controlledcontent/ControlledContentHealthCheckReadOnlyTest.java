package cn.iocoder.yudao.module.system.service.controlledcontent;

import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentTransitionAuditDO;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentVersionRefDO;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentTransitionAuditMapper;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentVersionRefMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.DCC_CONTROLLED_FILE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlledContentHealthCheckReadOnlyTest {

    @InjectMocks
    private ControlledContentLifecycleHealthCheckService healthCheckService;

    @Mock
    private ControlledContentVersionRefMapper versionRefMapper;
    @Mock
    private ControlledContentTransitionAuditMapper transitionAuditMapper;

    @Test
    void checkContentKey_shouldReportMismatchWithoutRepairingLifecycleData() {
        ControlledContentKey key = ControlledContentKey.of(122L, DCC_CONTROLLED_FILE, "1001");
        when(versionRefMapper.countActiveRefs(122L, "DCC_CONTROLLED_FILE", "1001")).thenReturn(0L);
        when(versionRefMapper.countOpenCandidateRefs(122L, "DCC_CONTROLLED_FILE", "1001")).thenReturn(1L);
        when(transitionAuditMapper.countTransitions(122L, "DCC_CONTROLLED_FILE", "1001")).thenReturn(0L);

        ControlledContentLifecycleHealthCheckService.HealthCheckResult result =
                healthCheckService.checkContentKey(key, 1L, 0L);

        assertFalse(result.consistent());
        assertTrue(result.issues().contains("native active count 1 != platform active count 0"));
        assertTrue(result.issues().contains("native open candidate count 0 != platform open candidate count 1"));
        assertTrue(result.issues().contains("platform transition audit is missing"));
        verify(versionRefMapper, never()).insert(any(ControlledContentVersionRefDO.class));
        verify(versionRefMapper, never()).update(any(), any());
        verify(transitionAuditMapper, never()).insert(any(ControlledContentTransitionAuditDO.class));
    }
}
