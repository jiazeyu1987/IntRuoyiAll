package cn.iocoder.yudao.module.dcc.service.projectcode.onboarding;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.onboarding.DccProductOnboardingCreateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProductOnboardingRequestDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProductOnboardingRequestMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProductOnboardingStatusConstants;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PRODUCT_ONBOARDING_DUPLICATE_PROJECT_CODE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PRODUCT_ONBOARDING_MDM_PRODUCT_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccProductOnboardingServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private DccProductOnboardingRequestMapper requestMapper;
    @Mock
    private DccProjectCodeMapper projectCodeMapper;
    @Mock
    private MdmProductApi mdmProductApi;

    @InjectMocks
    private DccProductOnboardingServiceImpl onboardingService;

    @Test
    void createRequest_shouldPersistPendingApprovalWithoutCreatingProjectCode() {
        DccProductOnboardingCreateReqVO reqVO = validCreateReq();
        when(mdmProductApi.getProduct(5000L)).thenReturn(MdmProductRespDTO.builder()
                .id(5000L)
                .productCode("P-5000")
                .dccProductCode("A1234567890123")
                .nameCn("正式产品")
                .status("ENABLE")
                .build());
        doAnswer(invocation -> {
            DccProductOnboardingRequestDO request = invocation.getArgument(0);
            request.setId(100L);
            return 1;
        }).when(requestMapper).insert(any(DccProductOnboardingRequestDO.class));

        Long requestId = onboardingService.createRequest(88L, reqVO);

        assertEquals(100L, requestId);
        ArgumentCaptor<DccProductOnboardingRequestDO> requestCaptor =
                ArgumentCaptor.forClass(DccProductOnboardingRequestDO.class);
        verify(requestMapper).insert(requestCaptor.capture());
        DccProductOnboardingRequestDO request = requestCaptor.getValue();
        assertEquals(88L, request.getApplicantUserId());
        assertEquals(5000L, request.getProductMasterId());
        assertEquals("A1234567890123", request.getDccProductCode());
        assertEquals("新产品 DCC 项目", request.getProjectName());
        assertEquals("DCC-NEW-001", request.getProjectCode());
        assertEquals(DccProductOnboardingStatusConstants.PENDING_APPROVAL, request.getStatus());
        verify(projectCodeMapper, never()).insert(any(DccProjectCodeDO.class));
    }

    @Test
    void createRequest_shouldRejectExistingProjectCodeBeforeWritingRequest() {
        DccProductOnboardingCreateReqVO reqVO = validCreateReq();
        when(projectCodeMapper.selectByProjectNameAndProjectCode("新产品 DCC 项目", "DCC-NEW-001"))
                .thenReturn(DccProjectCodeDO.builder().id(3000L).build());

        assertServiceException(() -> onboardingService.createRequest(88L, reqVO),
                PRODUCT_ONBOARDING_DUPLICATE_PROJECT_CODE);

        verify(requestMapper, never()).insert(any(DccProductOnboardingRequestDO.class));
    }

    @Test
    void approveRequest_shouldCreateEnabledProjectCodeAndBindMdmProduct() {
        DccProductOnboardingRequestDO pending = pendingRequest();
        when(requestMapper.selectById(100L)).thenReturn(pending);
        when(mdmProductApi.getEnabledDccProduct(5000L)).thenReturn(MdmProductRespDTO.builder()
                .id(5000L)
                .productCode("P-5000")
                .dccProductCode("A1234567890123")
                .nameCn("正式产品")
                .status("ENABLE")
                .build());
        doAnswer(invocation -> {
            DccProjectCodeDO projectCode = invocation.getArgument(0);
            projectCode.setId(3000L);
            return 1;
        }).when(projectCodeMapper).insert(any(DccProjectCodeDO.class));

        DccProductOnboardingRequestDO approved = onboardingService.approveRequest(99L, 100L);

        ArgumentCaptor<DccProjectCodeDO> projectCodeCaptor = ArgumentCaptor.forClass(DccProjectCodeDO.class);
        verify(projectCodeMapper).insert(projectCodeCaptor.capture());
        DccProjectCodeDO projectCode = projectCodeCaptor.getValue();
        assertEquals(5000L, projectCode.getProductMasterId());
        assertEquals("新产品 DCC 项目", projectCode.getProjectName());
        assertEquals("DCC-NEW-001", projectCode.getProjectCode());
        assertEquals(DccProjectCodeStatusConstants.ENABLE, projectCode.getStatus());
        assertEquals(DccProductOnboardingStatusConstants.APPROVED, approved.getStatus());
        assertEquals(99L, approved.getApproverUserId());
        assertEquals(3000L, approved.getGeneratedProjectCodeId());
        assertNotNull(approved.getApprovedTime());
        verify(requestMapper).updateById(any(DccProductOnboardingRequestDO.class));
    }

    @Test
    void approveRequest_shouldIgnoreCurrentPendingRequestWhenCheckingDuplicatePendingProject() {
        DccProductOnboardingRequestDO pending = pendingRequest();
        when(requestMapper.selectById(100L)).thenReturn(pending);
        when(requestMapper.selectPendingByProjectNameAndProjectCode("新产品 DCC 项目", "DCC-NEW-001"))
                .thenReturn(pending);
        when(mdmProductApi.getEnabledDccProduct(5000L)).thenReturn(MdmProductRespDTO.builder()
                .id(5000L)
                .productCode("P-5000")
                .dccProductCode("A1234567890123")
                .nameCn("正式产品")
                .status("ENABLE")
                .build());
        doAnswer(invocation -> {
            DccProjectCodeDO projectCode = invocation.getArgument(0);
            projectCode.setId(3000L);
            return 1;
        }).when(projectCodeMapper).insert(any(DccProjectCodeDO.class));

        DccProductOnboardingRequestDO approved = onboardingService.approveRequest(99L, 100L);

        assertEquals(DccProductOnboardingStatusConstants.APPROVED, approved.getStatus());
        assertEquals(3000L, approved.getGeneratedProjectCodeId());
        verify(projectCodeMapper).insert(any(DccProjectCodeDO.class));
        verify(requestMapper).updateById(any(DccProductOnboardingRequestDO.class));
    }

    @Test
    void approveRequest_shouldRejectDisabledMdmProductWithoutCreatingProjectCode() {
        when(requestMapper.selectById(100L)).thenReturn(pendingRequest());
        when(mdmProductApi.getEnabledDccProduct(5000L))
                .thenThrow(new IllegalStateException("MDM_PRODUCT_DISABLED: 产品主数据已停用"));

        assertServiceException(() -> onboardingService.approveRequest(99L, 100L),
                PRODUCT_ONBOARDING_MDM_PRODUCT_INVALID, "MDM_PRODUCT_DISABLED: 产品主数据已停用");

        verify(projectCodeMapper, never()).insert(any(DccProjectCodeDO.class));
        verify(requestMapper, never()).updateById(any(DccProductOnboardingRequestDO.class));
    }

    private DccProductOnboardingCreateReqVO validCreateReq() {
        DccProductOnboardingCreateReqVO reqVO = new DccProductOnboardingCreateReqVO();
        reqVO.setProductMasterId(5000L);
        reqVO.setProjectName(" 新产品 DCC 项目 ");
        reqVO.setProjectCode(" DCC-NEW-001 ");
        reqVO.setCategory("DHF");
        reqVO.setPriority("高");
        return reqVO;
    }

    private DccProductOnboardingRequestDO pendingRequest() {
        return DccProductOnboardingRequestDO.builder()
                .id(100L)
                .productMasterId(5000L)
                .productCode("P-5000")
                .dccProductCode("A1234567890123")
                .productNameCn("正式产品")
                .projectName("新产品 DCC 项目")
                .projectCode("DCC-NEW-001")
                .category("DHF")
                .priority("高")
                .status(DccProductOnboardingStatusConstants.PENDING_APPROVAL)
                .applicantUserId(88L)
                .build();
    }
}
