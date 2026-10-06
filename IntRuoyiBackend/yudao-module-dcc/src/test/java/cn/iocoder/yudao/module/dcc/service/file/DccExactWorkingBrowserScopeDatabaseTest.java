package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual SQL candidate filters/real Query aggregation. Permission APIs remain parent test ports. */
class DccExactWorkingBrowserScopeDatabaseTest extends DccRelationNameMetadataDatabaseTest {
    private static final long CURRENT=9007199254740993L;

    @BeforeEach void exactFixture() {
        jdbc.update("UPDATE dcc_controlled_file SET version_no='A/2' WHERE id=?",CURRENT);
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=?,current_active_controlled_file_id=? WHERE id=10",CURRENT,CURRENT);
        var history=new DccControlledFileDO();BeanUtils.copyProperties(files.selectById(CURRENT),history);
        history.setId(300L);history.setVersionNo("A/1");history.setStatus("OBSOLETE");history.setTitle("OLD-ONLY");files.insert(history);
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,file_name,file_number,dcc_project_code_id,status,latest_controlled_file_id,current_active_controlled_file_id) VALUES(40,1,2,'OTHER','OTHER',5,'ACTIVE_CHAIN',400,400)");
        var unrelated=new DccControlledFileDO();BeanUtils.copyProperties(files.selectById(CURRENT),unrelated);
        unrelated.setId(400L);unrelated.setMasterId(40L);unrelated.setFileNumber("OTHER");files.insert(unrelated);
        ReflectionTestUtils.setField(query,"downloadPolicyService",new cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyService());
    }

    private DccControlledFilePageReqVO request(boolean exact) {
        var request=new DccControlledFilePageReqVO();request.setPageNo(1);request.setPageSize(10);request.setLatestVersionOnly(true);
        // This behavior RED remains compilable before the formal optional contract exists.
        if(exact && Arrays.stream(request.getClass().getDeclaredFields()).anyMatch(f->f.getName().equals("workingFileId"))) {
            ReflectionTestUtils.setField(request,"workingFileId",CURRENT);ReflectionTestUtils.setField(request,"workingMasterId",10L);
        }
        return request;
    }

    @Test void exactPairBoundsRealCandidateMastersWithoutChangingTheVersionChain() {
        var response=query.getControlledFileBrowserPage(99L,request(true));
        assertEquals(1,response.getTotal(),"only the exact file's Master may reach this operation browser");
        assertEquals(CURRENT,response.getList().get(0).getId());
        assertTrue(response.getList().get(0).getVersionHistory().stream().allMatch(v->v.getId()!=400L));
    }

    @Test void historicalOnlyKeywordIsStillFilteredAfterCanonicalAggregation() {
        var req=request(true);req.setKeyword("OLD-ONLY");
        assertEquals(0,query.getControlledFileBrowserPage(99L,req).getTotal());
    }

    @Test void exactIdentityDoesNotGrantScopeAndWrongOrHalfIdentityNeverFallsBackToBroadScan() {
        var scope=(DccControlledFileAssignmentScopeService)ReflectionTestUtils.getField(query,"assignmentScopeService");
        when(scope.isWithinAssignedFileScope(99L,CURRENT)).thenReturn(false);
        when(scope.isWithinAssignedFileScope(99L,300L)).thenReturn(false);
        assertEquals(0,query.getControlledFileBrowserPage(99L,request(true)).getTotal());
        var req=request(true);ReflectionTestUtils.setField(req,"workingMasterId",40L);
        assertThrows(RuntimeException.class,()->query.getControlledFileBrowserPage(99L,req));
        ReflectionTestUtils.setField(req,"workingMasterId",null);
        assertThrows(RuntimeException.class,()->query.getControlledFileBrowserPage(99L,req));
        ReflectionTestUtils.setField(req,"workingMasterId",10L);
        jdbc.update("UPDATE dcc_controlled_file SET tenant_id=2 WHERE id=?",CURRENT);
        assertThrows(RuntimeException.class,()->query.getControlledFileBrowserPage(99L,req));
        jdbc.update("UPDATE dcc_controlled_file SET tenant_id=1 WHERE id=?",CURRENT);
        ReflectionTestUtils.setField(req,"workingMasterId",10L);req.setBrowserScope("GLOBAL");
        assertThrows(RuntimeException.class,()->query.getControlledFileBrowserPage(99L,req));
        assertThrows(RuntimeException.class,()->query.getControlledFilePage(99L,request(true)));
    }

    @Test void ordinaryRequestWithoutExactPairRetainsItsExistingFullListBehavior() {
        assertEquals(2,query.getControlledFileBrowserPage(99L,request(false)).getTotal());
    }
}
