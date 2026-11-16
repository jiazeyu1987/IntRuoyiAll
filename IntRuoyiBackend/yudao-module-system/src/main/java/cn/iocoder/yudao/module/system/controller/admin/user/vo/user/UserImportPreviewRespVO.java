package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

/** 文件内声明的人员和组织范围；并非最终成功写入数量，写入时仍需重新校验。 */
@Data
@AllArgsConstructor
public class UserImportPreviewRespVO {
    private Integer rowCount;
    private List<String> personnel;
    private List<String> organizations;
}
