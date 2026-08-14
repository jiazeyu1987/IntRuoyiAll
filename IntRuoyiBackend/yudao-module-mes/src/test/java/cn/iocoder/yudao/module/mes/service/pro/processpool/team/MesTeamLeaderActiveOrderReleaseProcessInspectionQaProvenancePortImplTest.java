package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePortImplTest {

    private final MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort port =
            new MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePortImpl();

    @Test
    void directDccOwnershipCreatesVerifiablePublishedQaProvenance() {
        DccProjectCodeDO project = project("ID");
        MesQaInspectionRegulationDO regulation = regulation(11L, "PQC-ANY-CODE");
        MesQaInspectionRegulationVersionDO version = version();

        MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort.Resolution result =
                port.verify(project, regulation, version);

        assertTrue(result.isVerifiedFor(project, regulation, version));
        assertEquals("DCC_QA_PROJECT_RELATION", result.getProvenanceType());
        assertEquals("11:21", result.getProvenanceId());
        assertFalse(result.getProvenanceSnapshotHash().isBlank());
    }

    @Test
    void matchingRegulationCodeDoesNotOverrideDifferentDccOwnership() {
        MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort.Resolution result =
                port.verify(project("ID"), regulation(12L, "PQC-ID-001"), version());

        assertEquals("PQC_DCC_QA_PROVENANCE_REQUIRED", result.getBlockerType());
        assertTrue(result.getBlockerMessage().contains("dccProjectCodeId=11"));
        assertTrue(result.getBlockerMessage().contains("regulationDccProjectCodeId=12"));
    }

    @Test
    void regulationWithoutDirectDccOwnershipIsBlocked() {
        MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort.Resolution result =
                port.verify(project("ID"), regulation(null, "PQC-ID-001"), version());

        assertEquals("PQC_DCC_QA_PROVENANCE_REQUIRED", result.getBlockerType());
        assertFalse(result.isVerifiedFor(project("ID"), regulation(null, "PQC-ID-001"), version()));
    }

    @Test
    void retiredLockedVersionRemainsVerifiableAfterRegulationPublishesANewerVersion() {
        DccProjectCodeDO project = project("ID");
        project.setStatus("DISABLE");
        MesQaInspectionRegulationDO regulation = regulation(11L, "PQC-ID-001");
        regulation.setCurrentVersionId(32L);
        MesQaInspectionRegulationVersionDO version = version();
        version.setLifecycleStatus("RETIRED");

        MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort.Resolution result =
                port.verify(project, regulation, version);

        assertTrue(result.isVerifiedFor(project, regulation, version));
        assertEquals("DCC_QA_PROJECT_RELATION", result.getProvenanceType());
    }

    private DccProjectCodeDO project(String projectCode) {
        DccProjectCodeDO project = DccProjectCodeDO.builder().id(11L).productMasterId(11L)
                .projectCode(projectCode).projectName("球囊扩张压力泵").status("ENABLE").build();
        project.setTenantId(1L);
        return project;
    }

    private MesQaInspectionRegulationDO regulation(Long dccProjectCodeId, String regulationCode) {
        MesQaInspectionRegulationDO regulation = MesQaInspectionRegulationDO.builder()
                .id(21L).dccProjectCodeId(dccProjectCodeId)
                .ownerModule(MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA)
                .regulationCode(regulationCode).regulationName("过程检验规程")
                .lifecycleStatus("PUBLISHED").currentVersionId(31L).build();
        regulation.setTenantId(1L);
        return regulation;
    }

    private MesQaInspectionRegulationVersionDO version() {
        MesQaInspectionRegulationVersionDO version = MesQaInspectionRegulationVersionDO.builder()
                .id(31L).regulationId(21L).versionNo("G/0").lifecycleStatus("PUBLISHED")
                .publishedAt(LocalDateTime.of(2026, 8, 11, 8, 0)).snapshotJson("{\"source\":\"PQC-ID-001\"}")
                .build();
        version.setTenantId(1L);
        return version;
    }
}
