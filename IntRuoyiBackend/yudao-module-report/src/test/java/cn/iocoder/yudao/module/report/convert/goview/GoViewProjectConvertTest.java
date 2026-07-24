package cn.iocoder.yudao.module.report.convert.goview;

import cn.iocoder.yudao.module.report.controller.admin.goview.vo.project.GoViewProjectCreateReqVO;
import cn.iocoder.yudao.module.report.controller.admin.goview.vo.project.GoViewProjectUpdateReqVO;
import cn.iocoder.yudao.module.report.dal.dataobject.goview.GoViewProjectDO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GoViewProjectConvertTest {

    @Test
    public void testConvertCreate_mapsNameOnly() {
        GoViewProjectCreateReqVO reqVO = new GoViewProjectCreateReqVO();
        reqVO.setName("dashboard");

        GoViewProjectDO result = GoViewProjectConvert.INSTANCE.convert(reqVO);

        assertEquals("dashboard", result.getName());
        assertNull(result.getStatus());
        assertNull(result.getContent());
        assertNull(result.getPicUrl());
        assertNull(result.getRemark());
    }

    @Test
    public void testConvertUpdate_mapsEditableFields() {
        GoViewProjectUpdateReqVO reqVO = new GoViewProjectUpdateReqVO();
        reqVO.setId(3L);
        reqVO.setName("dashboard");
        reqVO.setStatus(1);
        reqVO.setContent("{\"a\":1}");
        reqVO.setPicUrl("https://example.com/1.png");
        reqVO.setRemark("remark");

        GoViewProjectDO result = GoViewProjectConvert.INSTANCE.convert(reqVO);

        assertEquals(3L, result.getId());
        assertEquals("dashboard", result.getName());
        assertEquals(1, result.getStatus());
        assertEquals("{\"a\":1}", result.getContent());
        assertEquals("https://example.com/1.png", result.getPicUrl());
        assertEquals("remark", result.getRemark());
    }

}
