package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTraceDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTracePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTracePageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTraceVerifyReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordDomainTraceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProBatchRecordDomainTraceControllerTest {

    @Mock
    private MesProBatchRecordDomainTraceService domainTraceService;

    @InjectMocks
    private MesProBatchRecordDomainTraceController controller;

    @Test
    void detailPageVerify_delegateToDomainTraceService() {
        MesProBatchRecordDomainTraceDetailRespVO detail = new MesProBatchRecordDomainTraceDetailRespVO()
                .setExecutionId(1L)
                .setExecutionCode("BRE-001")
                .setStatus("BLOCKED");
        when(domainTraceService.getTraceDetail(1L)).thenReturn(detail);

        MesProBatchRecordDomainTracePageRespVO row = new MesProBatchRecordDomainTracePageRespVO()
                .setExecutionId(1L)
                .setExecutionCode("BRE-001")
                .setStatus("BLOCKED");
        MesProBatchRecordDomainTracePageReqVO pageReqVO = new MesProBatchRecordDomainTracePageReqVO();
        when(domainTraceService.getTracePage(pageReqVO)).thenReturn(new PageResult<>(List.of(row), 1L));

        MesProBatchRecordDomainTraceVerifyReqVO verifyReqVO = new MesProBatchRecordDomainTraceVerifyReqVO()
                .setExecutionId(1L)
                .setExpectedDomainTraceHash("expected-hash");
        when(domainTraceService.verify(verifyReqVO)).thenReturn(detail);

        assertSame(detail, controller.detail(1L).getData());
        assertSame(row, controller.page(pageReqVO).getData().getList().get(0));
        assertSame(detail, controller.verify(verifyReqVO).getData());
        verify(domainTraceService).verify(verifyReqVO);
    }

    @Test
    void contractMappings_matchDomainTraceEndpointAndPermissions() throws Exception {
        RequestMapping requestMapping = MesProBatchRecordDomainTraceController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/batch-record-execution/domain-trace"}, requestMapping.value());

        Method detailMethod = MesProBatchRecordDomainTraceController.class.getDeclaredMethod("detail", Long.class);
        assertArrayEquals(new String[]{"/detail"}, detailMethod.getAnnotation(GetMapping.class).value());
        assertEquals("executionId", detailMethod.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-execution:domain-trace-query')",
                detailMethod.getAnnotation(PreAuthorize.class).value());

        Method pageMethod = MesProBatchRecordDomainTraceController.class.getDeclaredMethod("page",
                MesProBatchRecordDomainTracePageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, pageMethod.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-execution:domain-trace-query')",
                pageMethod.getAnnotation(PreAuthorize.class).value());

        Method verifyMethod = MesProBatchRecordDomainTraceController.class.getDeclaredMethod("verify",
                MesProBatchRecordDomainTraceVerifyReqVO.class);
        assertArrayEquals(new String[]{"/verify"}, verifyMethod.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-execution:domain-trace-verify')",
                verifyMethod.getAnnotation(PreAuthorize.class).value());
    }
}
