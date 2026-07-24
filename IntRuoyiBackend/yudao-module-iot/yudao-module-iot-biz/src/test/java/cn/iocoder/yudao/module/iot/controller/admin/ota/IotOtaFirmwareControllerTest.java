package cn.iocoder.yudao.module.iot.controller.admin.ota;

import cn.iocoder.yudao.module.iot.service.ota.IotOtaFirmwareService;
import cn.iocoder.yudao.module.iot.service.product.IotProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IotOtaFirmwareControllerTest {

    @Mock
    private IotOtaFirmwareService otaFirmwareService;
    @Mock
    private IotProductService productService;
    @InjectMocks
    private IotOtaFirmwareController controller;

    @Test
    void deleteOtaFirmware_delegatesAndKeepsDeleteContract() throws Exception {
        assertTrue(controller.deleteOtaFirmware(7L).getData());
        verify(otaFirmwareService).deleteOtaFirmware(7L);

        Method method = IotOtaFirmwareController.class.getDeclaredMethod("deleteOtaFirmware", Long.class);
        assertArrayEquals(new String[]{"/delete"}, method.getAnnotation(DeleteMapping.class).value());
        assertEquals("id", method.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('iot:ota-firmware:delete')",
                method.getAnnotation(PreAuthorize.class).value());
    }
}
