package cn.iocoder.yudao.module.mes.service.pro.processpool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolEventRevisionFieldChangeBO {

    private String fieldCode;
    private String fieldName;
    private String beforeValue;
    private String afterValue;
    private Boolean affectsQuantityFragment;
    private Long sourceQuantityFragmentId;
    private MesProcessPoolFragmentOriginalField originalField;
}
