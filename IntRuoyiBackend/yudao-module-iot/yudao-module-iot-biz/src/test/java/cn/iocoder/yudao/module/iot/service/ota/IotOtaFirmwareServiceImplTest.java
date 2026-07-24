package cn.iocoder.yudao.module.iot.service.ota;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.iot.dal.dataobject.ota.IotOtaFirmwareDO;
import cn.iocoder.yudao.module.iot.dal.mysql.ota.IotOtaFirmwareMapper;
import cn.iocoder.yudao.module.iot.service.product.IotProductService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.module.iot.enums.ErrorCodeConstants.OTA_FIRMWARE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IotOtaFirmwareServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private IotOtaFirmwareServiceImpl otaFirmwareService;
    @Mock
    private IotOtaFirmwareMapper otaFirmwareMapper;
    @Mock
    private IotProductService productService;

    @Test
    void deleteOtaFirmware_whenExists_deletesById() {
        when(otaFirmwareMapper.selectById(7L)).thenReturn(new IotOtaFirmwareDO().setId(7L));

        otaFirmwareService.deleteOtaFirmware(7L);

        verify(otaFirmwareMapper).deleteById(7L);
    }

    @Test
    void deleteOtaFirmware_whenMissing_failsFast() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> otaFirmwareService.deleteOtaFirmware(7L));

        assertEquals(OTA_FIRMWARE_NOT_EXISTS.getCode(), exception.getCode());
    }
}
