package cn.iocoder.yudao.module.system.service.controlledcontent;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentTransitionAuditDO;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentVersionRefDO;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentTransitionAuditMapper;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentVersionRefMapper;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlledContentPersistenceModelTest {

    @Test
    void shouldBindVersionRefAndTransitionAuditToPlatformTables() {
        assertEquals("controlled_content_version_ref",
                ControlledContentVersionRefDO.class.getAnnotation(TableName.class).value());
        assertEquals("controlled_content_transition_audit",
                ControlledContentTransitionAuditDO.class.getAnnotation(TableName.class).value());
    }

    @Test
    void shouldExposeBaseMappersForLifecycleTables() {
        assertTrue(BaseMapperX.class.isAssignableFrom(ControlledContentVersionRefMapper.class));
        assertTrue(BaseMapperX.class.isAssignableFrom(ControlledContentTransitionAuditMapper.class));
    }

    @Test
    void shouldKeepOnlyLifecycleFieldsInVersionRefDO() {
        ControlledContentVersionRefDO ref = ControlledContentVersionRefDO.builder()
                .tenantId(122L)
                .contentType("DCC_CONTROLLED_FILE")
                .contentKey("1001")
                .nativeMasterId(1001L)
                .nativeVersionId(2002L)
                .versionNo("V2")
                .canonicalStatus("DRAFT")
                .domainStatus("DRAFT")
                .sourceVersionRefId(8L)
                .sourceNativeVersionId(1999L)
                .activeUniqueFlag(null)
                .openCandidateUniqueFlag(1)
                .approvalProcessInstanceId("process-1")
                .build();

        assertEquals(122L, ref.getTenantId());
        assertEquals("DCC_CONTROLLED_FILE", ref.getContentType());
        assertEquals("1001", ref.getContentKey());
        assertEquals(1001L, ref.getNativeMasterId());
        assertEquals(2002L, ref.getNativeVersionId());
        assertEquals("V2", ref.getVersionNo());
        assertEquals("DRAFT", ref.getCanonicalStatus());
        assertEquals("DRAFT", ref.getDomainStatus());
        assertEquals(1, ref.getOpenCandidateUniqueFlag());
    }

    @Test
    void shouldKeepOnlyTransitionAuditFieldsInTransitionDO() {
        ControlledContentTransitionAuditDO transition = ControlledContentTransitionAuditDO.builder()
                .tenantId(122L)
                .versionRefId(10L)
                .contentType("DCC_CONTROLLED_FILE")
                .contentKey("1001")
                .fromStatus("DRAFT")
                .toStatus("IN_REVIEW")
                .domainFromStatus("DRAFT")
                .domainToStatus("PENDING_DOC_CONTROL_REVIEW")
                .action("SUBMIT")
                .actorId(910272L)
                .reason("submit for approval")
                .build();

        assertEquals(122L, transition.getTenantId());
        assertEquals(10L, transition.getVersionRefId());
        assertEquals("DRAFT", transition.getFromStatus());
        assertEquals("IN_REVIEW", transition.getToStatus());
        assertEquals("SUBMIT", transition.getAction());
    }

    @Test
    void shouldNotExposeUnusedPlatformLifecycleColumnsInJavaModel() {
        Set<String> versionRefFields = Arrays.stream(ControlledContentVersionRefDO.class.getDeclaredFields())
                .map(field -> field.getName())
                .collect(Collectors.toSet());
        Set<String> transitionFields = Arrays.stream(ControlledContentTransitionAuditDO.class.getDeclaredFields())
                .map(field -> field.getName())
                .collect(Collectors.toSet());

        assertFalse(versionRefFields.contains("snapshotHash"));
        assertFalse(versionRefFields.contains("lockVersion"));
        assertFalse(transitionFields.contains("businessRefType"));
        assertFalse(transitionFields.contains("businessRefId"));
    }

    @Test
    void shouldNotExposeUnusedLedgerStyleMapperQueries() {
        Set<String> versionRefMapperMethods = Arrays.stream(ControlledContentVersionRefMapper.class.getDeclaredMethods())
                .map(method -> method.getName())
                .collect(Collectors.toSet());
        Set<String> transitionAuditMapperMethods =
                Arrays.stream(ControlledContentTransitionAuditMapper.class.getDeclaredMethods())
                        .map(method -> method.getName())
                        .collect(Collectors.toSet());

        assertFalse(versionRefMapperMethods.contains("selectListByContent"));
        assertFalse(transitionAuditMapperMethods.contains("selectListByVersionRefId"));
    }

}
