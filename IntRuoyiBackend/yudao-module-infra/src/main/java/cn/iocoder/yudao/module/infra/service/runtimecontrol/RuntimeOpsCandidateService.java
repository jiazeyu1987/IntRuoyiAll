package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRollbackCandidateRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRestoreCandidateRespVO;

import java.util.List;

public interface RuntimeOpsCandidateService {

    List<RuntimeControlRollbackCandidateRespVO> listRollbackCandidates();

    List<RuntimeControlRestoreCandidateRespVO> listRestoreCandidates();

    RuntimeControlRollbackCandidateRespVO requireAvailableRollbackCandidate(String candidateId);

    RuntimeControlRestoreCandidateRespVO requireAvailableRestoreCandidate(String candidateId);
}
