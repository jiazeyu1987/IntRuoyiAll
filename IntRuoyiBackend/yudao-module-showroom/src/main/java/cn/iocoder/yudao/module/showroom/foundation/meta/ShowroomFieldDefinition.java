package cn.iocoder.yudao.module.showroom.foundation.meta;

import cn.iocoder.yudao.module.showroom.foundation.enums.ShowroomFieldTierEnum;

public record ShowroomFieldDefinition(String code, ShowroomFieldTierEnum tier, boolean publishRequired) {
}
