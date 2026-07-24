package cn.iocoder.yudao.module.infra.convert.config;

import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigConvertTest {

    @Test
    public void testConvertSaveReq_mapsEditableFieldsOnly() {
        ConfigSaveReqVO reqVO = new ConfigSaveReqVO();
        reqVO.setId(1L);
        reqVO.setCategory("biz");
        reqVO.setName("name");
        reqVO.setKey("system.test");
        reqVO.setValue("value");
        reqVO.setVisible(true);
        reqVO.setRemark("remark");

        ConfigDO result = ConfigConvert.INSTANCE.convert(reqVO);

        assertEquals(1L, result.getId());
        assertEquals("biz", result.getCategory());
        assertEquals("name", result.getName());
        assertEquals("system.test", result.getConfigKey());
        assertEquals("value", result.getValue());
        assertTrue(result.getVisible());
        assertEquals("remark", result.getRemark());
        assertNull(result.getType());
    }

}
