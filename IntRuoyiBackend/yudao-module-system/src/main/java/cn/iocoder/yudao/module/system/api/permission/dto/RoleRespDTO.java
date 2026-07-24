package cn.iocoder.yudao.module.system.api.permission.dto;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import lombok.Data;

/**
 * 角色 Response DTO
 */
@Data
public class RoleRespDTO {

    /**
     * 角色编号
     */
    private Long id;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色标识
     */
    private String code;

    /**
     * 角色状态
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;
}
