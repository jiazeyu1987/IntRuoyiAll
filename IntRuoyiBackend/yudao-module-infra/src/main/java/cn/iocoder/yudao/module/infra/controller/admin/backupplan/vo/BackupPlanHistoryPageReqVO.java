package cn.iocoder.yudao.module.infra.controller.admin.backupplan.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 备份包历史分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class BackupPlanHistoryPageReqVO extends PageParam {
}
