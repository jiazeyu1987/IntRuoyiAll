package cn.iocoder.yudao.module.srm.controller.admin.naslocator;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo.SrmNasLocatorBlacklistRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo.SrmNasLocatorBlacklistSaveReqVO;
import cn.iocoder.yudao.module.srm.service.naslocator.SrmNasLocatorService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SrmNasLocatorControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private SrmNasLocatorController controller;

    @Mock
    private SrmNasLocatorService nasLocatorService;

    @Test
    void getBlacklist_shouldReturnPatterns() {
        SrmNasLocatorBlacklistRespVO respVO = new SrmNasLocatorBlacklistRespVO();
        respVO.setPatterns(List.of("*.pyc", "*MO13*.pdf"));
        when(nasLocatorService.getBlacklist()).thenReturn(respVO);

        CommonResult<SrmNasLocatorBlacklistRespVO> result = controller.getBlacklist();

        assertEquals(0, result.getCode());
        assertEquals(List.of("*.pyc", "*MO13*.pdf"), result.getData().getPatterns());
    }

    @Test
    void saveBlacklist_shouldDelegateToService() {
        SrmNasLocatorBlacklistSaveReqVO reqVO = new SrmNasLocatorBlacklistSaveReqVO();
        reqVO.setPatterns(List.of("*.pyc", "*MO13*.pdf"));

        CommonResult<Boolean> result = controller.saveBlacklist(reqVO);

        assertEquals(Boolean.TRUE, result.getData());
        verify(nasLocatorService).saveBlacklist(reqVO);
    }
}
