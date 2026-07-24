package cn.iocoder.yudao.module.showroom.content;

import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyRevision;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomContentService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowroomCompanyContentTest {

    private final ShowroomContentService contentService = new ShowroomContentService();

    @Test
    void companyDraftShouldPublishApprovedRevisionAndAuditChangedFields() {
        ShowroomCompanyRevision draft = contentService.saveCompanyDraft(new ShowroomCompanyDraft(
                null, "MAIN", "Yingtai", "Yingtai Medical", Map.of("development_history", "first draft")));

        assertEquals(1, draft.revisionNo());
        assertFalse(contentService.getCompany(draft.companyId()).currentRevisionId().isPresent());

        ShowroomCompanyRevision published = contentService.publishCompanyRevision(draft.revisionId(), 901L);

        assertEquals(draft.revisionId(), published.revisionId());
        assertEquals(draft.revisionId(), contentService.getCompany(draft.companyId()).currentRevisionId().orElseThrow());
        assertTrue(contentService.getCompany(draft.companyId()).live());
        assertEquals("development_history", contentService.versionAudits("COMPANY", draft.companyId()).get(0).fieldCode());
        assertEquals("PUBLISH", contentService.versionAudits("COMPANY", draft.companyId()).get(0).operatorAction());
    }

}
