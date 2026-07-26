package cn.iocoder.yudao.module.system.service.codextest;

import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerCheckpointResultReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerClaimReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerClaimRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerCompleteCaseReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerHeartbeatReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerHeartbeatRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerProgressReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerRegisterReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerRegisterRespVO;
import jakarta.validation.Valid;

public interface CodexTestRunnerService {

    void validateRunnerToken(String token);

    CodexTestRunnerRegisterRespVO registerRunner(@Valid CodexTestRunnerRegisterReqVO registerReqVO, String token);

    CodexTestRunnerClaimRespVO claimTasks(@Valid CodexTestRunnerClaimReqVO claimReqVO, String token);

    CodexTestRunnerHeartbeatRespVO heartbeat(@Valid CodexTestRunnerHeartbeatReqVO heartbeatReqVO, String token);

    void saveCheckpointResult(@Valid CodexTestRunnerCheckpointResultReqVO resultReqVO, String token);

    void reportProgress(@Valid CodexTestRunnerProgressReqVO progressReqVO, String token);

    void completeCase(@Valid CodexTestRunnerCompleteCaseReqVO completeReqVO, String token);

}
