package cn.iocoder.yudao.module.showroom.foundation.contract;

import java.util.Set;

public final class ShowroomPublishContract {

    private static final Set<String> REQUIRED_PRODUCT_PUBLISH_FIELDS = Set.of("name_en");
    private static final Set<String> REQUIRED_PRODUCT_COMPLETENESS_FIELDS = Set.of(
            "name_cn", "name_en", "owner_company_id", "product_owner_type", "lifecycle_stage");

    private ShowroomPublishContract() {
    }

    public static Set<String> requiredProductPublishFields() {
        return REQUIRED_PRODUCT_PUBLISH_FIELDS;
    }

    public static Set<String> requiredProductCompletenessFields() {
        return REQUIRED_PRODUCT_COMPLETENESS_FIELDS;
    }

}
