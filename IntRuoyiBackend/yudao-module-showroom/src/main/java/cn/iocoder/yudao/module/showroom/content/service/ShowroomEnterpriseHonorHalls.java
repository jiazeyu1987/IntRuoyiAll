package cn.iocoder.yudao.module.showroom.content.service;

import java.util.List;

final class ShowroomEnterpriseHonorHalls {

    static final String LEGACY_HALL_CODE = "company_honor";

    static final List<Definition> DEFINITIONS = List.of(
            new Definition(
                    "hall_09",
                    "企业荣誉展柜1",
                    "Corporate Honors Showcase 1",
                    "企业荣誉展柜1集中呈现公司荣誉体系的第一组奖项，涵盖社会贡献、总部认定、专精特新、创新总部、商业单项冠军、高新技术、知识产权与质量体系等代表性成果，展示企业在规范经营、技术创新和行业认可方面的持续积累。",
                    "Corporate Honors Showcase 1 presents the first group of enterprise awards, covering social contribution, headquarters recognition, specialized and innovative enterprise honors, innovation headquarters, single-champion recognition, high-tech capability, intellectual property, and quality-system achievements. It highlights the company's sustained progress in compliant operations, technology innovation, and industry recognition.",
                    9),
            new Definition(
                    "hall_10",
                    "企业荣誉展柜2",
                    "Corporate Honors Showcase 2",
                    "企业荣誉展柜2集中呈现公司荣誉体系的第二组奖项，延续展示品牌影响力、技术创新、产品质量、行业资质、社会责任和市场信任等成果，承接 Excel 奖项页签后半部分奖项信息，体现企业长期稳健发展的综合实力。",
                    "Corporate Honors Showcase 2 presents the second group of enterprise awards, continuing the record of brand influence, technology innovation, product quality, industry qualifications, social responsibility, and market trust. It carries the latter half of the Excel Awards sheet and reflects the company's sustained and balanced growth.",
                    10)
    );

    private ShowroomEnterpriseHonorHalls() {
    }

    static boolean isEnterpriseHonorHallCode(String hallCode) {
        return DEFINITIONS.stream().anyMatch(definition -> definition.hallCode().equals(hallCode));
    }

    record Definition(String hallCode, String name, String nameEn, String description, String descriptionEn,
                      int displayOrder) {
    }
}
