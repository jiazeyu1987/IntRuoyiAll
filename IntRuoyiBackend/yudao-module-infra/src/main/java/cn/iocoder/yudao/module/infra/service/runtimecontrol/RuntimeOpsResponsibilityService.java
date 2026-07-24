package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOwnerMatrixRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOwnerMatrixSaveReqVO;

import java.util.List;

public interface RuntimeOpsResponsibilityService {

    List<RuntimeControlOwnerMatrixRespVO> getOwnerMatrix(String environment, String action);

    RuntimeControlOwnerMatrixRespVO createOwner(RuntimeControlOwnerMatrixSaveReqVO reqVO);

    RuntimeControlOwnerMatrixRespVO updateOwner(Long id, RuntimeControlOwnerMatrixSaveReqVO reqVO);

    List<RuntimeControlOwnerMatrixRespVO> getRequiredOwners(String environment, String action);

    String findMissingRequiredOwnerReason(String environment, String action);

    void validateRequiredOwners(String environment, String action);
}
