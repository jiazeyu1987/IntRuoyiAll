package cn.iocoder.yudao.module.system.service.codextest;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestExecutionCancelReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCodeReadonlyExecutionStartReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestExecutionPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestExecutionRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestExecutionStartReqVO;
import jakarta.validation.Valid;

import java.util.List;

public interface CodexTestExecutionService {

    Long startExecution(@Valid CodexTestExecutionStartReqVO startReqVO, Long requestedBy);

    Long startCodeReadonlyExecution(@Valid CodexTestCodeReadonlyExecutionStartReqVO startReqVO, Long requestedBy);

    void cancelExecution(@Valid CodexTestExecutionCancelReqVO cancelReqVO);

    PageResult<CodexTestExecutionRespVO> getExecutionPage(CodexTestExecutionPageReqVO pageReqVO);

    CodexTestExecutionRespVO getExecution(Long id);

    CodexTestExecutionRespVO getExecutionResult(Long id, Long requestedBy);

    List<CodexTestExecutionRespVO> getExecutionMonitor();

    void rollupExecution(Long executionId);

}
