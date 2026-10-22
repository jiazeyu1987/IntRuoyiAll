package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectApplicationAttributesMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real Form/numeric reservations/attributes, plus all twelve original native/form round cases. */
@Import(DccProjectAttributesService.class)
class DccObsoleteReservedAttributesForkTest extends DccObsoleteFormDraftRoundTest {
    @Resource DccProjectAttributesService attributes;
    @Resource DccProjectApplicationAttributesMapper savedRows;
    @MockitoBean DccProjectAccessService projectAccess;
    final DccProjectAttributes d1=new DccProjectAttributes(List.of("CE"),null,"Y","N","N",null);
    final DccProjectAttributes d2=new DccProjectAttributes(List.of("FDA"),null,"N","Y","Y","目标项目");
    record Reserved(int source,int intervening,int target) { }
    Reserved reserved() {
        controlledFamily();jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(d1));
        int previous=tx(()->rounds.reserveObsoleteFormDraft(99L,20L,901L,"FORM-901"));
        tx(()->{attributes.beginDraft(99L,5L,"OBSOLETE",20L,previous);return attributes.freeze(99L,5L,"OBSOLETE",20L,previous);});
        jdbc.update("UPDATE bpm_form_action_instance SET status='IN_APPROVAL',bpm_process_instance_id='old-real-bpm' WHERE id=901 AND tenant_id=1");
        tx(()->rounds.bindObsoleteFormDraft(99L,20L,901L,"FORM-901",previous,"old-real-bpm"));
        jdbc.update("UPDATE bpm_form_action_instance SET status='REJECTED' WHERE id=901 AND tenant_id=1");
        int middle=tx(()->rounds.reserveObsoleteFormDraft(99L,20L,902L,"FORM-902"));
        tx(()->attributes.beginDraft(99L,5L,"OBSOLETE",20L,middle));
        jdbc.update("INSERT INTO bpm_form_action_instance(id,instance_code,tenant_id,applicant_user_id,status,system_code,object_type,object_id,object_version,action_code,object_state,form_data_json,deleted) VALUES(903,'FORM-903',1,99,'DRAFT','DCC','CONTROLLED_FILE','20','A/1','OBSOLETE','ACTIVE','{}',0)");
        int next=tx(()->rounds.reserveObsoleteFormDraft(99L,20L,903L,"FORM-903"));
        jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(d2));
        return new Reserved(previous,middle,next);
    }
    @AfterEach void removeReworkForm() {
        jdbc.update("DELETE FROM bpm_form_action_instance WHERE id=903 AND tenant_id=1 AND object_id='20' AND system_code='DCC'");
    }
    DccProjectApplicationAttributesDO fork(int previous,int target) {
        return ReflectionTestUtils.invokeMethod(attributes,"forkForReworkToReservedRound",99L,5L,"OBSOLETE",20L,previous,target);
    }
    @Test void trueThirdReservedRoundCopiesOriginalBytesAndNeverTouchesInterveningFormOrCurrentDefaults() {
        var r=reserved();assertEquals(1,r.source());assertEquals(2,r.intervening());assertEquals(3,r.target());
        var original=savedRows.find("OBSOLETE",20L,r.source());var intervening=savedRows.find("OBSOLETE",20L,r.intervening());
        var result=tx(()->{assertEquals(r.target(),rounds.requireObsoleteFormDraft(99L,20L,903L,"FORM-903"));return fork(r.source(),r.target());});
        assertEquals(r.target(),result.getApplicationRound());assertEquals(original.getDefaultSourceJson(),result.getDefaultSourceJson());assertEquals(original.getActualAttributesJson(),result.getActualAttributesJson());
        assertEquals(20L,result.getSourceApplicationId());assertEquals(r.source(),result.getSourceApplicationRound());assertFalse(result.getSubmitted());
        assertEquals(original,savedRows.find("OBSOLETE",20L,r.source()));assertEquals(intervening,savedRows.find("OBSOLETE",20L,r.intervening()));assertEquals(d2,attributes.defaults(jdbcProject()));
    }
    cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO jdbcProject() {
        return cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO.builder().defaultAttributesJson(jdbc.queryForObject("SELECT default_attributes_json FROM dcc_project_code WHERE id=5",String.class)).build();
    }
    @Test void exactReplayPreservesThisDraftManualActualAndDoesNotWriteAgain() {
        var r=reserved();tx(()->fork(r.source(),r.target()));tx(()->attributes.saveDraft(99L,5L,"OBSOLETE",20L,r.target(),d2));
        var before=savedRows.find("OBSOLETE",20L,r.target());assertEquals(before,tx(()->fork(r.source(),r.target())));
        assertEquals(before,savedRows.find("OBSOLETE",20L,r.target()));assertEquals(d2,attributes.readValue(before.getActualAttributesJson()));assertEquals(d1,attributes.readValue(before.getDefaultSourceJson()));
    }
    @Test void occupiedTargetFromAnotherSourceCannotBeBorrowedEvenInTheSameProject() {
        var r=reserved();var occupied=savedRows.find("OBSOLETE",20L,r.intervening());
        assertThrows(RuntimeException.class,()->tx(()->fork(r.source(),r.intervening())));assertEquals(occupied,savedRows.find("OBSOLETE",20L,r.intervening()));
        tx(()->fork(r.source(),r.target()));jdbc.update("UPDATE dcc_project_application_attributes SET source_application_round=7 WHERE application_type='OBSOLETE' AND application_id=20 AND application_round=?",r.target());
        var altered=savedRows.find("OBSOLETE",20L,r.target());assertThrows(RuntimeException.class,()->tx(()->fork(r.source(),r.target())));assertEquals(altered,savedRows.find("OBSOLETE",20L,r.target()));
    }
    @Test void frozenTargetAndUnsubmittedSourceCannotBeCopiedOrOverwritten() {
        var r=reserved();assertThrows(RuntimeException.class,()->tx(()->fork(r.intervening(),r.target())));assertNull(savedRows.find("OBSOLETE",20L,r.target()));
        tx(()->fork(r.source(),r.target()));tx(()->attributes.freeze(99L,5L,"OBSOLETE",20L,r.target()));var before=savedRows.find("OBSOLETE",20L,r.target());
        assertThrows(RuntimeException.class,()->tx(()->fork(r.source(),r.target())));assertEquals(before,savedRows.find("OBSOLETE",20L,r.target()));
    }
    @Test void lateFailureRollsBackOnlyTheNewTargetAndLeavesSourceAndOtherFormWholeRows() {
        var r=reserved();var source=savedRows.find("OBSOLETE",20L,r.source());var middle=savedRows.find("OBSOLETE",20L,r.intervening());
        assertThrows(IllegalStateException.class,()->tx(()->{fork(r.source(),r.target());throw new IllegalStateException("new rework caller failed late");}));
        assertNull(savedRows.find("OBSOLETE",20L,r.target()));assertEquals(source,savedRows.find("OBSOLETE",20L,r.source()));assertEquals(middle,savedRows.find("OBSOLETE",20L,r.intervening()));
    }
    @Test void invalidTargetNumbersMissingTransactionAndDeniedAccessRejectBeforeInsert() {
        var r=reserved();assertThrows(RuntimeException.class,()->fork(r.source(),r.target()));
        for(int target:new int[]{0,r.source(),r.source()-1})assertThrows(RuntimeException.class,()->tx(()->fork(r.source(),target)));
        doThrow(new IllegalStateException("actual project permission denied")).when(projectAccess).assertProjectEditorOrOwner(99L,5L);
        assertThrows(RuntimeException.class,()->tx(()->fork(r.source(),r.target())));assertNull(savedRows.find("OBSOLETE",20L,r.target()));
    }
    @Test void wrongTenantOrCorruptedSourceJsonCannotCreateTarget() {
        var r=reserved();cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(2L);
        assertThrows(RuntimeException.class,()->tx(()->fork(r.source(),r.target())));cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
        jdbc.update("UPDATE dcc_project_application_attributes SET actual_attributes_json='{}' WHERE application_type='OBSOLETE' AND application_id=20 AND application_round=?",r.source());
        assertThrows(RuntimeException.class,()->tx(()->fork(r.source(),r.target())));assertNull(savedRows.find("OBSOLETE",20L,r.target()));
    }
}
