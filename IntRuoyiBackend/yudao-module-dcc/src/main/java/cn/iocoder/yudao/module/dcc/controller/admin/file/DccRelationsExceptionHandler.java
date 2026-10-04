package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationInputFailure;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** Controller-local business boundary, inherited by D endpoints before the global catch-all. */
public class DccRelationsExceptionHandler {
    @ExceptionHandler({DccRelationFailure.class,DccRelationInputFailure.class})
    public CommonResult<?> handle(RuntimeException error){
        String hint=switch(error.getMessage()) {
            case "DCC_REFERENCE_CONFIRMATION_REQUIRED" -> "请二次确认后再取消引用";
            case "DCC_REFERENCE_SELECTION_REQUIRED" -> "请先选择引用文件";
            case "DCC_REFERENCE_DUPLICATE_SELECTION" -> "同一文件只能引用一次，请核对所选文件";
            case "DCC_REFERENCE_VERSION_CONFLICT" -> "此文件夹已引用该文件的其他版本，不能直接覆盖";
            case "DCC_REFERENCE_CONCURRENT_CHANGE" -> "引用已被取消并重新建立，请刷新后核对";
            case "DCC_REFERENCE_SOURCE_NOT_CONTROLLED" -> "源文件没有可用受控版本";
            case "DCC_REFERENCE_NOT_FOUND" -> "当前文件夹的引用不存在，请刷新后核对";
            case "DCC_RELATION_CONCURRENT_CHANGE" -> "关联已被其他操作修改，请刷新后核对";
            case "DCC_RELATION_COMMAND_REPLAY_CONFLICT" -> "同一保存请求的文件、操作者或内容已变化，请重新核对";
            case "DCC_RELATION_SOURCE_VERSION_CHANGED" -> "当前文件的受控版本已变化，请重新打开后核对关联";
            case "DCC_RELATION_APPROVAL_SNAPSHOT_FROZEN" -> "审批关系已冻结，请通过当前关联窗口修改现有关系";
            case "DCC_RELATION_CURRENT_SET_NOT_INITIALIZED" -> "当前关联尚未建立正式来源，请联系文控完成接入";
            case "DCC_RELATION_LATEST_CONTROLLED_MISSING" -> "关联目标没有可用最新受控版本";
            case "DCC_RELATION_REASON_REQUIRED" -> "请填写操作原因";
            default -> "关系操作未完成，请核对当前文件、项目及权限";
        };
        // Stable machine reason remains visible, without leaking underlying exception details.
        return CommonResult.error(cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationErrorCodes.BUSINESS_FAILURE,hint+"（"+error.getMessage()+"）");
    }
}
