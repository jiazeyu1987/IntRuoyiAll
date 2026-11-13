package cn.iocoder.yudao.module.iot.controller.admin.device;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.iot.controller.admin.device.vo.device.*;
import cn.iocoder.yudao.module.iot.service.device.IotDeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static org.mockito.Mockito.*;

class IotDeviceExcelRoundTripTest {
    @Test
    void controllerExportPreservesRequiredProductKeyOnImport() throws Exception {
        var controller = spy(new IotDeviceController());
        var service = mock(IotDeviceService.class);
        ReflectionTestUtils.setField(controller, "deviceService", service);
        var device = new IotDeviceRespVO();
        device.setDeviceName("device01");
        device.setProductKey("product01");
        doReturn(success(new PageResult<>(List.of(device), 1L))).when(controller).getDevicePage(any());
        var response = new MockHttpServletResponse();
        controller.exportDeviceExcel(new IotDevicePageReqVO(), response);
        var file = new MockMultipartFile("file", "设备.xls", "application/octet-stream", response.getContentAsByteArray());
        for (boolean update : List.of(false, true)) {
            controller.importDevice(file, update);
            verify(service).importDevice(argThat(rows -> rows.size() == 1
                    && "device01".equals(rows.get(0).getDeviceName())
                    && "product01".equals(rows.get(0).getProductKey())), eq(update));
        }
    }
}
