package cn.iocoder.yudao.module.infra.convert.file;

import cn.iocoder.yudao.module.infra.controller.admin.file.vo.config.FileConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileConfigDO;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FileConfigConvertTest {

    @Test
    public void testConvertSaveReq_leavesManagedFieldsToServiceLayer() {
        FileConfigSaveReqVO reqVO = new FileConfigSaveReqVO();
        reqVO.setId(2L);
        reqVO.setName("local");
        reqVO.setStorage(1);
        reqVO.setConfig(Map.of("basePath", "D:/data"));
        reqVO.setRemark("remark");

        FileConfigDO result = FileConfigConvert.INSTANCE.convert(reqVO);

        assertEquals(2L, result.getId());
        assertEquals("local", result.getName());
        assertEquals(1, result.getStorage());
        assertEquals("remark", result.getRemark());
        assertNull(result.getConfig());
        assertNull(result.getMaster());
    }

}
