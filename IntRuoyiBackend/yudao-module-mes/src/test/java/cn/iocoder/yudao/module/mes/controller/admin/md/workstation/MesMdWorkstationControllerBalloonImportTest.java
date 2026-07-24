package cn.iocoder.yudao.module.mes.controller.admin.md.workstation;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.BalloonProcessDeviceMappingImportRespVO;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkshopService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationCapacityService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.md.workstation.importer.BalloonProcessDeviceMappingImportService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesMdWorkstationControllerBalloonImportTest {

    @Mock
    private MesMdWorkstationService workstationService;
    @Mock
    private MesMdWorkshopService workshopService;
    @Mock
    private MesProProcessService processService;
    @Mock
    private MesMdWorkstationCapacityService workstationCapacityService;
    @Mock
    private BalloonProcessDeviceMappingImportService balloonProcessDeviceMappingImportService;
    @InjectMocks
    private MesMdWorkstationController controller;

    @Test
    void importBalloonProcessDeviceMapping_forwardsFileAndWorkshopId() {
        MockMultipartFile file = new MockMultipartFile("file", "balloon.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1, 2, 3});
        BalloonProcessDeviceMappingImportRespVO respVO = BalloonProcessDeviceMappingImportRespVO.builder()
                .processCount(33)
                .createdWorkstationCount(15)
                .build();
        when(balloonProcessDeviceMappingImportService.importMapping(file, 900011L)).thenReturn(respVO);

        CommonResult<BalloonProcessDeviceMappingImportRespVO> response =
                controller.importBalloonProcessDeviceMapping(file, 900011L);

        assertEquals(0, response.getCode());
        assertEquals(33, response.getData().getProcessCount());
        assertEquals(15, response.getData().getCreatedWorkstationCount());
        verify(balloonProcessDeviceMappingImportService).importMapping(file, 900011L);
    }
}
