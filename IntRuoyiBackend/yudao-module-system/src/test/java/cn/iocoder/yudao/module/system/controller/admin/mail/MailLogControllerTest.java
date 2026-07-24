package cn.iocoder.yudao.module.system.controller.admin.mail;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.mail.vo.log.MailLogPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.mail.MailLogDO;
import cn.iocoder.yudao.module.system.service.mail.MailLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailLogControllerTest {

    @Mock
    private MailLogService mailLogService;
    @InjectMocks
    private MailLogController controller;

    @Test
    void exportMailLog_exportsAllMatchingRowsAndKeepsContract() throws Exception {
        MailLogPageReqVO reqVO = new MailLogPageReqVO();
        when(mailLogService.getMailLogPage(reqVO))
                .thenReturn(new PageResult<>(List.of(new MailLogDO().setId(3L)), 1L));
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.exportMailLog(reqVO, response);

        assertEquals(PageParam.PAGE_SIZE_NONE, reqVO.getPageSize());
        assertTrue(response.getContentAsByteArray().length > 0);
        verify(mailLogService).getMailLogPage(reqVO);
        Method method = MailLogController.class.getDeclaredMethod("exportMailLog",
                MailLogPageReqVO.class, jakarta.servlet.http.HttpServletResponse.class);
        assertArrayEquals(new String[]{"/export-excel"}, method.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('system:mail-log:export')",
                method.getAnnotation(PreAuthorize.class).value());
    }
}
