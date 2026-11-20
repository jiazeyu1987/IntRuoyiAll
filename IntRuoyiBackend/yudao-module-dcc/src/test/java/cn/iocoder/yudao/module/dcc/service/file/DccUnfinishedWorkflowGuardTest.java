package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_WORKFLOW_IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccUnfinishedWorkflowGuardTest {
    private final DccControlledFileQueryServiceImpl query = new DccControlledFileQueryServiceImpl();
    private final DccControlledFileMapper files = mock(DccControlledFileMapper.class);
    private final DccControlledFileDO baseline = file(20L, "ACTIVE", "C/1");
    private final DccControlledFileDO sibling = file(21L, "WORKING", "C/1-1");
    private final DccControlledFileDO selected = file(22L, "WORKING", "C/1-2");
    private final DccControlledFileDO failed = file(23L, "WITHDRAWN", "C/2");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(query, "controlledFileMapper", files);
        ReflectionTestUtils.setField(query, "versionPolicy", DccControlledFileVersionPolicy.defaultPolicy());
        // Two independent checkins from the same baseline; only selected belongs to the failed attempt's lineage.
        sibling.setPredecessorControlledFileId(baseline.getId());
        selected.setPredecessorControlledFileId(baseline.getId());
        failed.setPredecessorControlledFileId(selected.getId());
        failed.setProcessInstanceId("terminated-revision");
        failed.setRevisionChangeType("PARTIAL");
        when(files.selectListByMasterIdForUpdate(10L)).thenReturn(List.of(baseline, sibling, selected, failed));
    }

    @ParameterizedTest
    @ValueSource(strings = {"WITHDRAWN", "REJECTED"})
    void retainedSiblingDraftDoesNotBlockFailedRevisionRestartOrCorrection(String status) {
        failed.setStatus(status);
        assertDoesNotThrow(() -> check(failed));
        assertDoesNotThrow(() -> check(selected));
        assertDoesNotThrow(() -> check(baseline));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RUNNING_PROCESS", "FORMAL_APPLICATION", "OTHER_BASELINE", "OTHER_IDENTITY", "OTHER_FORMAL_VERSION"})
    void onlyUnsubmittedIterationsOfTheSameBaselineAreExcluded(String boundary) {
        switch (boundary) {
            case "RUNNING_PROCESS" -> sibling.setProcessInstanceId("running-revision");
            case "FORMAL_APPLICATION" -> sibling.setRevisionChangeType("PARTIAL");
            case "OTHER_BASELINE" -> sibling.setRevisionBaseActiveControlledFileId(99L);
            case "OTHER_IDENTITY" -> sibling.setTenantId(2L);
            case "OTHER_FORMAL_VERSION" -> sibling.setVersionNo("B/1-1");
        }
        var error = assertThrows(ServiceException.class, () -> check(failed));
        assertEquals(CONTROLLED_FILE_WORKFLOW_IN_PROGRESS.getCode(), error.getCode());
    }

    private void check(DccControlledFileDO current) {
        ReflectionTestUtils.invokeMethod(query, "rejectWhenMasterHasOtherUnfinishedWorkflow", 10L, current);
    }

    private static DccControlledFileDO file(Long id, String status, String version) {
        return DccControlledFileDO.builder().id(id).tenantId(1L).masterId(10L).dccProjectCodeId(5L)
                .categoryId(2L).fileTypeTaxonomyId(6L).fileNumber("N-1").sourceOriginalFileName("SOP.pdf")
                .status(status).versionNo(version).revisionBaseActiveControlledFileId(20L).build();
    }
}
