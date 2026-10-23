package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadPreviewReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileCheckoutDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileCheckoutMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectFileTemplateService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DccSourceUploadContextTest extends BaseMockitoUnitTest {
    @Mock private DccFileCategoryMapper categoryMapper;
    @Mock private DccProjectCodeMapper projectCodeMapper;
    @Mock private DccProjectFileTemplateService projectFileTemplateService;
    @Mock private cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService fileTypeTaxonomyAdminService;
    @Mock private DccProjectAccessService projectAccessService;
    @Mock private DccControlledFileCategoryPermissionSupport categoryPermissionSupport;
    @Mock private DccControlledFileMapper controlledFileMapper;
    @Mock private DccControlledFileCheckoutMapper checkoutMapper;
    @InjectMocks private DccControlledFileWorkflowServiceImpl workflowService;

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(1L);
        lenient().when(categoryMapper.selectById(10L)).thenReturn(DccFileCategoryDO.builder()
                .id(10L).active(true).fileTypeTaxonomyId(20L).build());
        lenient().when(categoryPermissionSupport.hasCategoryPermission(any(), any(), any())).thenReturn(true);
        lenient().when(projectCodeMapper.selectById(30L)).thenReturn(DccProjectCodeDO.builder()
                .id(30L).status(DccProjectCodeStatusConstants.ENABLE).build());
        lenient().when(fileTypeTaxonomyAdminService.resolveActivePath(20L)).thenReturn(
                new cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath(20L,"体系","技术","报告",null,null));
        lenient().when(fileTypeTaxonomyAdminService.listActiveDescendantIds(20L)).thenReturn(java.util.List.of(20L));
        lenient().when(fileTypeTaxonomyAdminService.resolveActiveCategoryId(20L)).thenReturn(10L);
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void newUploadRequiresExactCategoryMapping() {
        var request = request("NEW_UPLOAD");
        request.setFileTypeTaxonomyId(null);
        assertServiceException(() -> workflowService.validateSourceUploadContext(99L, request),
                PROJECT_FILE_TEMPLATE_SELECTION_INVALID);
        request.setFileTypeTaxonomyId(21L);
        assertServiceException(() -> workflowService.validateSourceUploadContext(99L, request),
                PROJECT_FILE_TEMPLATE_SELECTION_INVALID);
        verifyNoInteractions(projectFileTemplateService);
    }

    @Test
    void validLeafRequiresProjectAccessWithoutTemplate() {
        var request = request("NEW_UPLOAD");
        workflowService.validateSourceUploadContext(99L, request);
        verify(projectAccessService).assertProjectEditorOrOwner(99L, 30L);
        lenient().doThrow(exception(PROJECT_FILE_TEMPLATE_SELECTION_INVALID)).when(projectFileTemplateService)
                .validateUploadLocation(30L, 20L);
        workflowService.validateSourceUploadContext(99L, request);
        verifyNoInteractions(projectFileTemplateService);
    }

    @Test
    void newUploadAllowsFreeNameForActiveLeafWithoutTemplate() {
        var request = request("NEW_UPLOAD");
        request.setFileName("新建文件-用户自定义名称");
        workflowService.validateSourceUploadContext(99L, request);
        verify(projectFileTemplateService,never()).validateUploadLocation(any(),any());
        verify(projectFileTemplateService, never())
                .validateUploadSelection(30L, 20L, "新建文件-用户自定义名称");
    }

    @Test
    void nonLeafTypeAndProjectPermissionStillRejectBeforeAnyUpload() {
        when(fileTypeTaxonomyAdminService.listActiveDescendantIds(20L)).thenReturn(java.util.List.of(20L,21L));
        assertServiceException(()->workflowService.validateSourceUploadContext(99L,request("NEW_UPLOAD")),
                FILE_TYPE_TAXONOMY_LEVEL_INVALID);
        doThrow(exception(DCC_PROJECT_ACCESS_DENIED)).when(projectAccessService).assertProjectEditorOrOwner(99L,30L);
        assertServiceException(()->workflowService.validateSourceUploadContext(99L,request("NEW_UPLOAD")),DCC_PROJECT_ACCESS_DENIED);
        verifyNoInteractions(projectFileTemplateService);
    }

    @Test
    void ambiguousActiveCategoryMappingStillBlocksNormalPreviewWithoutTemplate() {
        var taxonomy=spy(new cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminServiceImpl());
        org.springframework.test.util.ReflectionTestUtils.setField(taxonomy,"categoryMapper",categoryMapper);
        var path=new cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath(20L,"体系","技术","报告",null,null);
        doReturn(path).when(taxonomy).resolveActivePath(20L);
        lenient().doReturn(java.util.List.of(20L)).when(taxonomy).listActiveDescendantIds(20L);
        lenient().doReturn(java.util.List.of(path)).when(taxonomy).listActiveDescendantPaths(20L);
        when(categoryMapper.selectList()).thenReturn(java.util.List.of(
                DccFileCategoryDO.builder().id(10L).active(true).fileTypeTaxonomyId(20L).build(),
                DccFileCategoryDO.builder().id(11L).active(true).fileTypeTaxonomyId(20L).build()));
        org.springframework.test.util.ReflectionTestUtils.setField(workflowService,"fileTypeTaxonomyAdminService",taxonomy);
        assertServiceException(()->workflowService.validateSourceUploadContext(99L,request("NEW_UPLOAD")),
                PROJECT_FILE_TEMPLATE_CATEGORY_INVALID);
        verifyNoInteractions(projectFileTemplateService,controlledFileMapper);
    }

    @Test
    void disabledProjectCannotUpload() {
        when(projectCodeMapper.selectById(30L)).thenReturn(DccProjectCodeDO.builder().id(30L)
                .status(DccProjectCodeStatusConstants.DISABLE).build());
        assertServiceException(() -> workflowService.validateSourceUploadContext(99L, request("NEW_UPLOAD")),
                PROJECT_CODE_DISABLED);
        verifyNoInteractions(projectFileTemplateService);
    }

    @Test
    void checkinRequiresExactCheckoutActorAndVersionWithoutCurrentTemplate() {
        var request = request("CHECKIN");
        request.setControlledFileId(40L);
        when(controlledFileMapper.selectById(40L)).thenReturn(DccControlledFileDO.builder()
                .id(40L).tenantId(1L).masterId(50L).categoryId(10L).dccProjectCodeId(30L).status("ACTIVE").build());
        var checkout = DccControlledFileCheckoutDO.builder().actorId(88L).baseIterationId(40L).build();
        when(checkoutMapper.selectActiveByMasterId(1L, 50L)).thenReturn(checkout);
        assertServiceException(() -> workflowService.validateSourceUploadContext(99L, request),
                CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        checkout.setActorId(99L);
        checkout.setBaseIterationId(41L);
        assertServiceException(() -> workflowService.validateSourceUploadContext(99L, request),
                CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        checkout.setBaseIterationId(40L);
        workflowService.validateSourceUploadContext(99L, request);
        verify(projectAccessService).assertProjectEditorOrOwner(99L, 30L);
        verifyNoInteractions(projectFileTemplateService);
    }

    @Test
    void externalReviewDoesNotRequireProjectTemplateAndUnknownContextFails() {
        var request = request("EXTERNAL_REVIEW");
        request.setDccProjectCodeId(null);
        request.setFileTypeTaxonomyId(null);
        request.setFileName(null);
        workflowService.validateSourceUploadContext(99L, request);
        verifyNoInteractions(projectFileTemplateService);
        request.setUploadContext("UNKNOWN");
        assertServiceException(() -> workflowService.validateSourceUploadContext(99L, request),
                PROJECT_FILE_TEMPLATE_SELECTION_INVALID);
    }

    private DccControlledFileUploadPreviewReqVO request(String context) {
        var request = new DccControlledFileUploadPreviewReqVO();
        request.setUploadContext(context);
        request.setCategoryId(10L);
        request.setDccProjectCodeId(30L);
        request.setFileTypeTaxonomyId(20L);
        request.setFileName("SOP-001");
        return request;
    }

    @Test
    void sourceSessionsBindContextTemplateAndExactVersionWithinStorageLimit() {
        String prefix = DccSourceUploadSession.newUploadPrefix(30L, 20L, "SOP-001");
        String session = DccSourceUploadSession.scope(prefix, "client-session");
        org.junit.jupiter.api.Assertions.assertTrue(session.length() <= 128);
        DccSourceUploadSession.require(session, prefix);
        for (String other : java.util.List.of(DccSourceUploadSession.externalPrefix(),
                DccSourceUploadSession.checkinPrefix(40L),
                DccSourceUploadSession.newUploadPrefix(31L, 20L, "SOP-001"),
                DccSourceUploadSession.newUploadPrefix(30L, 21L, "SOP-001"),
                DccSourceUploadSession.newUploadPrefix(30L, 20L, "SOP-002"))) {
            assertServiceException(() -> DccSourceUploadSession.require(session, other),
                    CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
        }
        String checkin = DccSourceUploadSession.scope(DccSourceUploadSession.checkinPrefix(40L), "client-session");
        assertServiceException(() -> DccSourceUploadSession.require(checkin, DccSourceUploadSession.checkinPrefix(41L)),
                CONTROLLED_FILE_UPLOAD_TICKET_INVALID);
    }
}
