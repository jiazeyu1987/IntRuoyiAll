# 真实 Word 样本验证

## 样本映射

| 目标版本 | 来源文件 | 包装阶段 | 解析结论 |
| --- | --- | --- | --- |
| A | `PQC-CR-003（A 7）初包装过程检验规程.docx(1).docx` | 初包装 | 表 2 为检验规则表，共 6 行，包含 3 个主要检验项目和备注行。 |
| A | `PQC-CR-004（A 1）大中包装过程检验规程 (2)(1).docx` | 大中包装 | 表 2 为检验规则表，共 5 行，包含 3 个主要检验项目和备注行。 |
| B | `PQC-MECR-001（B 1）美联初包装过程检验规程--2026.08.10生效(1).docx` | 美联初包装 | 表 2 为检验内容表，共 6 行，包含 3 个主要检验项目和备注行。 |

## A-01 初包装要点

- 表头：序号、检验项目、接受标准、检验方法、检具、检验规则。
- 检验项目：外观、密封强度、密封泄漏。
- 抽样规则示例：外观首件 20 件，巡检按 `GB/T 2828.1，一般水平I，AQL=0.25`；密封强度和密封泄漏首件 5 件。
- 密封强度存在同一项目下的补充标准：通用 `≧1.2N/15mm`，百瑞吉产品 `≥1.5N/15mm`。

## A-02 大中包装要点

- 表头：序号、检验项目、接受标准、检验方法、检具、检验规则。
- 检验项目：外观、标签/说明书/合格证、包装标识。
- 抽样规则：三项均为 `GB/T 2828.1，S-2，AQL=2.5`。
- 备注中明确首检数量为 3 件。

## B-01 美联初包装要点

- 表头：序号、检验项目、接受标准、检验方法、检验器具及设备、抽样方案。
- 检验项目：外观（吸塑盒、面纸、顶头袋）、密封强度（吸塑盒、面纸）、密封泄漏（吸塑盒、面纸、顶头袋）。
- 抽样规则示例：外观首件 20 件，巡检按 `GB/T 2828.1，一般水平I，AQL=0.25`；密封强度含首件 5 件和巡检 `GB/T 2828.1，S-2 AQL=2.5`。
- 工具字段使用“检验器具及设备”，必须映射到系统的检验工具/设备字段，不能因表头不同丢失。

## 对设计的约束

- A 版本必须支持两份来源文件共同构成一个发布版本，产品绑定只指向一次 A，不为初包装和大中包装创建两条产品绑定。
- B 版本只有一份来源文件，发布和绑定时不能自动继承 A 的大中包装来源。
- A-01 的“百瑞吉产品要求”不是普通通用标准，必须输出为 `condition_type=PRODUCT_SET_REQUIRED` 的条件标准；发布前未选择正式产品 ID 时阻断。
- B-01 的“美联”不是产品名称自动匹配条件，默认作为来源文件和包装阶段线索；若业务要求限制美联产品，必须由 QA 管理员显式配置 `PRODUCT_SET` 适用范围。
- Word 解析必须支持上述两组表头同义词，缺少必填列或出现未知必填表头时 fail fast。
- 发布快照必须保存来源文件摘要、包装阶段、来源表行和原始摘录，满足后续审计追溯。

## Normalized Fixture 断言

```json
{
  "A": {
    "sources": [
      {
        "code": "PQC-CR-003",
        "versionText": "A 7",
        "stage": "INITIAL_PACKAGING",
        "sha256": "10EC699F57CCC8E7119ABA24AADC3BF5F06F3787298C6007FF00B523FFC9F041",
        "requiredItems": ["外观", "密封强度", "密封泄漏"],
        "conditionalStandards": [
          {
            "itemName": "密封强度",
            "conditionText": "百瑞吉产品要求",
            "conditionType": "PRODUCT_SET_REQUIRED",
            "standardContains": "1.5N/15mm"
          }
        ],
        "samplingAssertions": ["首件：20件", "AQL=0.25", "首件：5件"]
      },
      {
        "code": "PQC-CR-004",
        "versionText": "A 1",
        "stage": "MIDDLE_LARGE_PACKAGING",
        "sha256": "7EF8BFC39A96CDC645F9D310796E19C0F6863DE161C5281AD9B538F8FA800316",
        "requiredItems": ["外观", "标签、说明书、合格证", "包装标识"],
        "samplingAssertions": ["GB/T 2828.1", "S-2", "AQL=2.5", "首检数量为3件"]
      }
    ]
  },
  "B": {
    "sources": [
      {
        "code": "PQC-MECR-001",
        "versionText": "B 1",
        "stage": "MEILIAN_INITIAL_PACKAGING",
        "sha256": "4F75621C9396A6C5485C665E39285CE0F5CB4EC623145E4CA8AC555B3CDA476F",
        "requiredItems": ["外观（吸塑盒、面纸、顶头袋）", "密封强度（吸塑盒、面纸）", "密封泄漏（吸塑盒、面纸、顶头袋）"],
        "toolAssertions": ["万能材料试验机", "裁刀", "秒表"],
        "samplingAssertions": ["首件：20件", "首件：5件", "S-2 AQL=2.5"],
        "applicabilityRequiresQaDecision": true
      }
    ]
  }
}
```

开发阶段 parser 单测必须断言以上 normalized fixture，不得只断言表格数量。

## 执行证据

- `officecli view ... text --max-lines 40`：三份样本均可读取章节与表结构。
- `officecli get ... /body/tbl[2] --depth 2 --json`：三份样本均可读取表头、项目、标准、方法、工具和抽样字段。
- 可执行样本命令：
  - `officecli view doc/tasks/20260908-common-qa-regulation-product-binding/source-docx/A-01-PQC-CR-003.docx text --max-lines 40`
  - `officecli get doc/tasks/20260908-common-qa-regulation-product-binding/source-docx/A-01-PQC-CR-003.docx /body/tbl[2] --depth 2 --json`
  - `officecli view doc/tasks/20260908-common-qa-regulation-product-binding/source-docx/A-02-PQC-CR-004.docx text --max-lines 40`
  - `officecli get doc/tasks/20260908-common-qa-regulation-product-binding/source-docx/A-02-PQC-CR-004.docx /body/tbl[2] --depth 2 --json`
  - `officecli view doc/tasks/20260908-common-qa-regulation-product-binding/source-docx/B-01-PQC-MECR-001.docx text --max-lines 40`
  - `officecli get doc/tasks/20260908-common-qa-regulation-product-binding/source-docx/B-01-PQC-MECR-001.docx /body/tbl[2] --depth 2 --json`
- B 源文件被 WPS 打开导致 officecli 不能直接读取源文件；验证时使用同字节任务副本 `source-docx/B-01-PQC-MECR-001.docx` 解析，副本来自原路径只读复制。
- 任务副本 sha256：A-01 `10EC699F57CCC8E7119ABA24AADC3BF5F06F3787298C6007FF00B523FFC9F041`，A-02 `7EF8BFC39A96CDC645F9D310796E19C0F6863DE161C5281AD9B538F8FA800316`，B-01 `4F75621C9396A6C5485C665E39285CE0F5CB4EC623145E4CA8AC555B3CDA476F`。
- `officecli validate` 显示三份样本自身存在 OpenXML schema 问题：A-01/A-02 为 styles.xml `uiPriority` 顺序问题，B-01 额外存在若干编号层级 `ilvl` 最小值问题。开发导入验收不能假设历史 Word 文件 schema 零错误，但必须保证检验规则表解析成功、必填列完整、解析警告可审计。
