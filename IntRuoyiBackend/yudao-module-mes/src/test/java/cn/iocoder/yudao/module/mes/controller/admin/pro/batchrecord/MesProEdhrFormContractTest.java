package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormCreateInstanceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormInstancePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormInstanceSaveDraftReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormInstanceSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormTemplatePageReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProEdhrFormContractTest {

    @Test
    void templateControllerMappings_matchFirstSliceContract() throws Exception {
        RequestMapping requestMapping = MesProEdhrFormTemplateController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-form-template"}, requestMapping.value());

        Method page = MesProEdhrFormTemplateController.class.getDeclaredMethod("getPage",
                MesProEdhrFormTemplatePageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, page.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-form-template:query')",
                page.getAnnotation(PreAuthorize.class).value());

        Method create = MesProEdhrFormTemplateController.class.getDeclaredMethod("create",
                MesProEdhrFormTemplateCreateReqVO.class);
        assertArrayEquals(new String[]{"/create"}, create.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-form-template:create')",
                create.getAnnotation(PreAuthorize.class).value());

        Method activate = MesProEdhrFormTemplateController.class.getDeclaredMethod("activate",
                MesProEdhrFormActivateReqVO.class);
        assertArrayEquals(new String[]{"/activate"}, activate.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-form-template:activate')",
                activate.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void instanceControllerMappings_matchFirstSliceContract() throws Exception {
        RequestMapping requestMapping = MesProEdhrFormInstanceController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-form-instance"}, requestMapping.value());

        Method page = MesProEdhrFormInstanceController.class.getDeclaredMethod("getPage",
                MesProEdhrFormInstancePageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, page.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-form-instance:query')",
                page.getAnnotation(PreAuthorize.class).value());

        Method get = MesProEdhrFormInstanceController.class.getDeclaredMethod("get", Long.class);
        assertArrayEquals(new String[]{"/get"}, get.getAnnotation(GetMapping.class).value());
        assertEquals("id", get.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-form-instance:query')",
                get.getAnnotation(PreAuthorize.class).value());

        Method create = MesProEdhrFormInstanceController.class.getDeclaredMethod("create",
                MesProEdhrFormCreateInstanceReqVO.class);
        assertArrayEquals(new String[]{"/create"}, create.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-form-instance:create')",
                create.getAnnotation(PreAuthorize.class).value());

        Method saveDraft = MesProEdhrFormInstanceController.class.getDeclaredMethod("saveDraft",
                MesProEdhrFormInstanceSaveDraftReqVO.class);
        assertArrayEquals(new String[]{"/save-draft"}, saveDraft.getAnnotation(PutMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-form-instance:save')",
                saveDraft.getAnnotation(PreAuthorize.class).value());

        Method submit = MesProEdhrFormInstanceController.class.getDeclaredMethod("submit",
                MesProEdhrFormInstanceSubmitReqVO.class);
        assertArrayEquals(new String[]{"/submit"}, submit.getAnnotation(PutMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-form-instance:submit')",
                submit.getAnnotation(PreAuthorize.class).value());

        Method eventPage = MesProEdhrFormInstanceController.class.getDeclaredMethod("getEventPage",
                MesProEdhrFormEventPageReqVO.class);
        assertArrayEquals(new String[]{"/event/page"}, eventPage.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-form-instance:query')",
                eventPage.getAnnotation(PreAuthorize.class).value());
    }
}
