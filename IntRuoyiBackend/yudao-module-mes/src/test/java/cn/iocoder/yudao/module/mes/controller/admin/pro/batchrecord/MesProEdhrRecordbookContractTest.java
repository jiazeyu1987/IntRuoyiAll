package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrControlledTagCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrControlledTagPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrControlledTagStatusReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEntryCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEntryPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEntrySaveDraftReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEntrySubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookTemplateActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookTemplatePageReqVO;
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

class MesProEdhrRecordbookContractTest {

    @Test
    void templateControllerMappings_matchRecordbookContract() throws Exception {
        RequestMapping requestMapping = MesProEdhrRecordbookTemplateController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-recordbook-template"}, requestMapping.value());

        Method page = MesProEdhrRecordbookTemplateController.class.getDeclaredMethod("getPage",
                MesProEdhrRecordbookTemplatePageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, page.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-recordbook-template:query')",
                page.getAnnotation(PreAuthorize.class).value());

        Method create = MesProEdhrRecordbookTemplateController.class.getDeclaredMethod("create",
                MesProEdhrRecordbookTemplateCreateReqVO.class);
        assertArrayEquals(new String[]{"/create"}, create.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-recordbook-template:create')",
                create.getAnnotation(PreAuthorize.class).value());

        Method activate = MesProEdhrRecordbookTemplateController.class.getDeclaredMethod("activate",
                MesProEdhrRecordbookTemplateActivateReqVO.class);
        assertArrayEquals(new String[]{"/activate"}, activate.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-recordbook-template:activate')",
                activate.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void recordbookControllerMappings_matchMyRecordbookContract() throws Exception {
        RequestMapping requestMapping = MesProEdhrRecordbookController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-recordbook"}, requestMapping.value());

        Method page = MesProEdhrRecordbookController.class.getDeclaredMethod("getPage",
                MesProEdhrRecordbookPageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, page.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-recordbook:query')",
                page.getAnnotation(PreAuthorize.class).value());

        Method myPage = MesProEdhrRecordbookController.class.getDeclaredMethod("getMyPage",
                MesProEdhrRecordbookPageReqVO.class);
        assertArrayEquals(new String[]{"/my-page"}, myPage.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-recordbook:query')",
                myPage.getAnnotation(PreAuthorize.class).value());

        Method create = MesProEdhrRecordbookController.class.getDeclaredMethod("create",
                MesProEdhrRecordbookCreateReqVO.class);
        assertArrayEquals(new String[]{"/create"}, create.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-recordbook:create')",
                create.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void entryControllerMappings_matchDraftSubmitEventContract() throws Exception {
        RequestMapping requestMapping = MesProEdhrRecordbookEntryController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-recordbook-entry"}, requestMapping.value());

        Method page = MesProEdhrRecordbookEntryController.class.getDeclaredMethod("getPage",
                MesProEdhrRecordbookEntryPageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, page.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-recordbook-entry:query')",
                page.getAnnotation(PreAuthorize.class).value());

        Method get = MesProEdhrRecordbookEntryController.class.getDeclaredMethod("get", Long.class);
        assertArrayEquals(new String[]{"/get"}, get.getAnnotation(GetMapping.class).value());
        assertEquals("id", get.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-recordbook-entry:query')",
                get.getAnnotation(PreAuthorize.class).value());

        Method create = MesProEdhrRecordbookEntryController.class.getDeclaredMethod("create",
                MesProEdhrRecordbookEntryCreateReqVO.class);
        assertArrayEquals(new String[]{"/create"}, create.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-recordbook-entry:create')",
                create.getAnnotation(PreAuthorize.class).value());

        Method saveDraft = MesProEdhrRecordbookEntryController.class.getDeclaredMethod("saveDraft",
                MesProEdhrRecordbookEntrySaveDraftReqVO.class);
        assertArrayEquals(new String[]{"/save-draft"}, saveDraft.getAnnotation(PutMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-recordbook-entry:save')",
                saveDraft.getAnnotation(PreAuthorize.class).value());

        Method submit = MesProEdhrRecordbookEntryController.class.getDeclaredMethod("submit",
                MesProEdhrRecordbookEntrySubmitReqVO.class);
        assertArrayEquals(new String[]{"/submit"}, submit.getAnnotation(PutMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-recordbook-entry:submit')",
                submit.getAnnotation(PreAuthorize.class).value());

        Method eventPage = MesProEdhrRecordbookEntryController.class.getDeclaredMethod("getEventPage",
                MesProEdhrRecordbookEventPageReqVO.class);
        assertArrayEquals(new String[]{"/event/page"}, eventPage.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-recordbook-entry:query')",
                eventPage.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void tagControllerMappings_matchControlledTagContract() throws Exception {
        RequestMapping requestMapping = MesProEdhrTagController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-tag"}, requestMapping.value());

        Method page = MesProEdhrTagController.class.getDeclaredMethod("getPage",
                MesProEdhrControlledTagPageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, page.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-tag:query')",
                page.getAnnotation(PreAuthorize.class).value());

        Method create = MesProEdhrTagController.class.getDeclaredMethod("create",
                MesProEdhrControlledTagCreateReqVO.class);
        assertArrayEquals(new String[]{"/create"}, create.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-tag:create')",
                create.getAnnotation(PreAuthorize.class).value());

        Method activate = MesProEdhrTagController.class.getDeclaredMethod("activate",
                MesProEdhrControlledTagStatusReqVO.class);
        assertArrayEquals(new String[]{"/activate"}, activate.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-tag:activate')",
                activate.getAnnotation(PreAuthorize.class).value());

        Method disable = MesProEdhrTagController.class.getDeclaredMethod("disable",
                MesProEdhrControlledTagStatusReqVO.class);
        assertArrayEquals(new String[]{"/disable"}, disable.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-tag:disable')",
                disable.getAnnotation(PreAuthorize.class).value());
    }
}
