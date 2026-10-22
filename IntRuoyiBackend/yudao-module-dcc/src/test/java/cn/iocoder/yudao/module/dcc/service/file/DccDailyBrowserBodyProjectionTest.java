package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileActionProjectionRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileVersionHistoryRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** The browser consumes allowedActions; closing only canPreview leaves a live daily entry. */
class DccDailyBrowserBodyProjectionTest {
    private final DccControlledFileQueryServiceImpl query = new DccControlledFileQueryServiceImpl();

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "WORKING", "REJECTED", "PENDING_APPLICANT_REWORK",
            "PENDING_MATRIX_REVIEW", "CONTROLLED_PENDING_EFFECTIVE", "SUPERSEDED", "OBSOLETE"})
    void dailyRowClosesBothPreviewProjectionsWithoutRemovingManagementActions(String status) {
        var row = response();
        restrict(row, file(10L, status));
        assertFalse(row.getCanPreview());
        assertEquals(List.of("CHECKIN", "REVISE"), row.getActionProjection().getAllowedActions());
        assertEquals(17L, row.getActionProjection().getPendingRequestId());
        assertTrue(row.getActionProjection().getActionLocked());
    }

    @Test void everySelectedHistoryOptionClosesItsOwnPreviewAction() {
        var mapper = mock(DccControlledFileMapper.class);
        ReflectionTestUtils.setField(query, "controlledFileMapper", mapper);
        var active = file(10L, "ACTIVE"); active.setMasterId(100L);
        var working = file(11L, "WORKING");
        var pending = file(12L, "CONTROLLED_PENDING_EFFECTIVE");
        when(mapper.selectListByMasterId(100L)).thenReturn(List.of(active, working, pending));
        var row = response();
        row.setVersionHistory(List.of(history(10L), history(11L), history(12L), history(13L)));
        restrict(row, active);
        assertTrue(row.getCanPreview());
        assertTrue(row.getVersionHistory().get(0).getCanPreview());
        assertTrue(row.getVersionHistory().get(0).getActionProjection().getAllowedActions().contains("PREVIEW"));
        for (var option : row.getVersionHistory().subList(1, 4)) {
            assertFalse(option.getCanPreview());
            assertEquals(List.of("CHECKIN", "REVISE"), option.getActionProjection().getAllowedActions());
            assertNotNull(option.getId(), "keep the metadata/traceability identity");
        }
    }

    @Test void activeRestrictionNeverRestoresDeniedPreviewAndRequiresActivation() {
        var denied = response(); denied.setCanPreview(false);
        restrict(denied, file(10L, "ACTIVE"));
        assertFalse(denied.getActionProjection().getAllowedActions().contains("PREVIEW"));
        var unactivated = file(10L, "ACTIVE"); unactivated.setActivatedTime(null);
        var row = response(); restrict(row, unactivated);
        assertFalse(row.getCanPreview());
        assertFalse(row.getActionProjection().getAllowedActions().contains("PREVIEW"));
    }

    @Test void missingProjectionDoesNotInventActions() {
        var row = response(); row.setActionProjection(null);
        restrict(row, file(10L, "WORKING"));
        assertFalse(row.getCanPreview()); assertNull(row.getActionProjection());
    }

    private void restrict(DccControlledFileRespVO row, DccControlledFileDO file) {
        ReflectionTestUtils.invokeMethod(query, "restrictDailyBodyProjection", row, file);
    }
    private DccControlledFileDO file(Long id, String status) {
        var file = DccControlledFileDO.builder().id(id).status(status).requesterId(99L).build();
        if ("ACTIVE".equals(status)) file.setActivatedTime(LocalDateTime.of(2026, 10, 15, 10, 0));
        return file;
    }
    private DccControlledFileActionProjectionRespVO actions() {
        var actions = new DccControlledFileActionProjectionRespVO();
        actions.setAllowedActions(List.of("PREVIEW", "CHECKIN", "REVISE"));
        actions.setActionLocked(true); actions.setPendingRequestId(17L);
        return actions;
    }
    private DccControlledFileRespVO response() {
        var row = new DccControlledFileRespVO(); row.setCanPreview(true); row.setActionProjection(actions()); return row;
    }
    private DccControlledFileVersionHistoryRespVO history(Long id) {
        var row = new DccControlledFileVersionHistoryRespVO();
        row.setId(id); row.setCanPreview(true); row.setActionProjection(actions()); return row;
    }
}
