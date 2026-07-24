package cn.iocoder.yudao.module.showroom.integration;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.showroom.dal.dataobject.keyword.ShowroomKeywordDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.keyword.ShowroomKeywordMapper;
import cn.iocoder.yudao.module.showroom.keyword.service.ShowroomKeywordGlossaryService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(ShowroomKeywordGlossaryService.class)
class ShowroomKeywordGlossaryIntegrationTest extends BaseDbUnitTest {

    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final Long TEST_TENANT_ID = 122L;
    private static final String UNIQUE_COMPANY_ZH = "上海测试翰愈医疗器械有限公司";
    private static final String UNIQUE_COMPANY_EN = "Shanghai Test Healing Medical Instruments Co., Ltd.";
    private static final String UNIQUE_BRAND_ZH = "翰愈";
    private static final String UNIQUE_BRAND_EN = "HealingX";

    @Resource
    private ShowroomKeywordGlossaryService glossaryService;

    @Resource
    private ShowroomKeywordMapper keywordMapper;

    @Test
    void prepareShouldPreferLongestTenantKeywordMatch() {
        TenantUtils.execute(DEFAULT_TENANT_ID, () -> {
            insertKeyword(UNIQUE_COMPANY_ZH, UNIQUE_COMPANY_EN);
            insertKeyword(UNIQUE_BRAND_ZH, UNIQUE_BRAND_EN);
        });

        ShowroomKeywordGlossaryService.PreparedGlossary prepared = TenantUtils.execute(DEFAULT_TENANT_ID,
                () -> glossaryService.prepare(UNIQUE_COMPANY_ZH));

        assertEquals("__SHOWROOM_TERM_1__", prepared.protectedText());
        assertEquals(1, prepared.matchedTerms().size());
        assertEquals(UNIQUE_COMPANY_ZH, prepared.matchedTerms().get(0).nameZh());
        assertEquals(UNIQUE_COMPANY_EN,
                prepared.restore("__SHOWROOM_TERM_1__"));
    }

    @Test
    void prepareShouldReplaceSubstringWithTenantKeywordValue() {
        TenantUtils.execute(DEFAULT_TENANT_ID, () -> insertKeyword(UNIQUE_BRAND_ZH, UNIQUE_BRAND_EN));

        ShowroomKeywordGlossaryService.PreparedGlossary prepared = TenantUtils.execute(DEFAULT_TENANT_ID,
                () -> glossaryService.prepare(UNIQUE_BRAND_ZH + "导管"));

        assertEquals("__SHOWROOM_TERM_1__导管", prepared.protectedText());
        assertEquals(UNIQUE_BRAND_EN + " catheter", prepared.restore("__SHOWROOM_TERM_1__ catheter"));
    }

    @Test
    void prepareShouldUseCurrentTenantKeywordOnly() {
        TenantUtils.execute(DEFAULT_TENANT_ID, () -> insertKeyword(UNIQUE_BRAND_ZH, UNIQUE_BRAND_EN));
        TenantUtils.execute(TEST_TENANT_ID, () -> insertKeyword(UNIQUE_BRAND_ZH, "Hanlin Brand"));

        String tenantOne = TenantUtils.execute(DEFAULT_TENANT_ID,
                () -> glossaryService.prepare(UNIQUE_BRAND_ZH + "导管").restore("__SHOWROOM_TERM_1__ catheter"));
        String tenantTwo = TenantUtils.execute(TEST_TENANT_ID,
                () -> glossaryService.prepare(UNIQUE_BRAND_ZH + "导管").restore("__SHOWROOM_TERM_1__ catheter"));

        assertEquals(UNIQUE_BRAND_EN + " catheter", tenantOne);
        assertEquals("Hanlin Brand catheter", tenantTwo);
    }

    private void insertKeyword(String nameZh, String nameEn) {
        ShowroomKeywordDO keyword = new ShowroomKeywordDO();
        keyword.setTenantId(TenantContextHolder.getRequiredTenantId());
        keyword.setNameZh(nameZh);
        keyword.setNameEn(nameEn);
        keywordMapper.insert(keyword);
    }
}
