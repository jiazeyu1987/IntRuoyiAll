package cn.iocoder.yudao.module.system.service.codextest;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCasePageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCaseRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCaseSaveReqVO;
import jakarta.validation.Valid;

public interface CodexTestCaseService {

    Long createCase(@Valid CodexTestCaseSaveReqVO createReqVO);

    void updateCase(@Valid CodexTestCaseSaveReqVO updateReqVO);

    void deleteCase(Long id);

    CodexTestCaseRespVO getCase(Long id);

    PageResult<CodexTestCaseRespVO> getCasePage(CodexTestCasePageReqVO pageReqVO);

}
