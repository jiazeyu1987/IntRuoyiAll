# 05 功能、配置与设计摘要

## 1. 目的

汇总当前 IntRuoyi 系统中可支撑 CSV 的功能设计、配置设计、数据设计和证据来源，作为 FS/配置说明的最小版本。

## 2. 架构摘要

| 层级 | 设计摘要 | 证据 |
| --- | --- | --- |
| 前端 | Vue 3/Vite/TypeScript 管理端，承载 MES、DCC、注册证、表单中心、测试管理等页面。 | `IntRuoyiFronted/package.json` |
| 后端 | Java 17/Spring Boot/Maven 多模块单体，模块包含 System、Signature、BPM、DCC、MES、ERP、Infra、MDM、Showroom。 | `IntRuoyiBackend/pom.xml` |
| 数据库 | MySQL release migration 管理 schema、菜单、权限、业务表和验证相关表。 | `IntRuoyiBackend/sql/mysql/` |
| 存储 | 对象存储/NAS 用于文件、图片、导出包和备份证据。 | 备份恢复设计、DCC 设计 |
| 审批 | BPM/Flowable 支撑文控、表单、批记录和变更审批。 | BPM/审批相关 SQL 和控制器 |
| 质量记录 | eDHR、DCC、电子签名、GxP 审计和备份恢复证据共同构成受控电子记录。 | ADR-0002、ADR-0003、MES/DCC 设计 |

## 3. 功能设计摘要

### 3.1 MES/eDHR 批记录

- 生产角色链路覆盖一线生产、PQC、PQC 组长、QA、放行负责人和系统自动处理。
- 批记录、过程检验单和损耗单在活跃订单完成节点统一回填，避免过早生成最终记录。
- 一线生产和 PQC 提交需形成正式来源事实，后续复核后进入批记录和放行链路。
- 当前源码包含批记录执行、审批、追踪、签名、OQ/PQ、验证包、统一变更、偏差等控制器和 VO。

### 3.2 DCC 文控与注册证

- DCC 设计采用逻辑文件 + Revision/Iteration 版本模型，发布成功后保持单一当前正式版本。
- 受控文件支持审批、发布、浏览、下载、签名、文件 hash 和版本历史。
- 注册证管理覆盖上传、续证、旧证、提醒、下载授权和角色权限迁移。
- 迁移和历史数据处理要求 fail fast，不允许默认项目、默认分类或文件名猜测。

### 3.3 电子签名

- 统一电子签名内核设计要求本人重新认证、签名内容快照、签名时间、证据 hash、失败锁定和周期复核。
- DCC、BPM、MES/eDHR 保留业务入口，通过内部签名能力生成统一证据。
- 当前安全审查表明设计层面已覆盖账户唯一性、密码策略、锁定、内容绑定、时间和审批顺序，但运营证据仍需补齐。

### 3.4 GxP 审计追踪

- 统一审计设计要求业务成功写入与审计事件同事务提交，审计失败则业务失败。
- 审计事件包含对象、动作、原因、actor、before/after、diff、策略、时间、证据链接和 hash。
- 审计记录应只追加，不提供更新、删除、补写或“标记有效”接口。
- 当前已有 ADR、数据模型、后端 API 设计和核心 SQL/源码骨架，但生产 WORM、特权审计和周期审查运行证据仍缺。

### 3.5 表单中心和 Word 解析

- 表单中心和 MES 批记录保留独立业务入口，底层共享 Word 结构解析能力。
- parse-only JSON 下载不得调用会写库的模板导入或批记录导入接口。
- `.doc` 和 `.docx` 需要真实样本验证，解析失败不得返回空成功。

### 3.6 备份恢复

- 首版备份恢复设计要求每周全量、每日增量、链校验、测试槽位恢复演练和审查证据 ZIP。
- 页面不能自行判定备份成功；所有状态应由后端 manifest、checksum、operation report 和 rehearsal report 派生。
- 长期法规归档不能由灾备备份替代。

## 4. 配置和安全要求

| 领域 | 最低设计要求 | 当前缺口 |
| --- | --- | --- |
| 账号和权限 | 独立实名账号、租户隔离、角色权限、项目/数据范围、职责分离 | 正式职责矩阵和账号发放记录待补 |
| 签名密码 | 8 位四类字符、90 天、前 5 次历史、失败锁定 | 生产策略启用和培训记录待补 |
| 审计策略 | 版本化 `gxp-audit-policy.yaml`、批准引用、覆盖报告 | 策略文件批准和 CI 运行证据待补 |
| 时间 | 服务器受控时间、NTP/chrony、签名时间不可手填 | 生产时间源和导出证据待补 |
| 对象存储 | 文件 hash、留存、WORM/Object Lock 如适用、恢复验证 | 正式 bucket/版本/retainUntil 证据待补 |
| 备份恢复 | 全量/增量、链校验、恢复演练、证据包 | 真实演练报告待补 |
| 接口 | ERP/BPM/存储/报表接口对账、失败关闭、重试和审计 | 接口验证报告待补 |

## 5. 设计证据到验证项映射

| 设计证据 | 覆盖内容 | 需验证 |
| --- | --- | --- |
| `docs/product/production-role-system-operations.md` | 生产角色、PQC、批记录、放行主流程 | PQ 主业务流程 |
| `docs/product/frontline-process-material-batch-record-mvp-prd.md` | 一线多物料批记录、ERP 同步边界 | OQ/PQ 物料批号和数量规则 |
| `docs/product/dcc-windchill-version-phase1-prd.md` | DCC 版本、检出、检入、发布、历史 | OQ DCC 版本生命周期 |
| `docs/adr/ADR-0001-dcc-windchill-version-model.md` | DCC 版本架构决策 | 设计评审和迁移预检 |
| `docs/adr/ADR-0002-unified-gxp-audit-trail.md` | 统一审计架构决策 | 审计同事务、只追加、归档恢复 |
| `docs/adr/ADR-0003-unified-electronic-signature-kernel.md` | 统一签名架构决策 | 签名认证、时间、内容绑定 |
| `docs/security/electronic-signature-periodic-compliance-review-sop.md` | 电子签名周期审查 SOP | 周期审查执行和批准 |
| `docs/system/shared-word-template-parser-design.md` | Word 解析和 parse-only 边界 | 真实 DOC/DOCX 解析 |
| `docs/changes/20260907-backup-minimal-closure.md` | 备份恢复最小闭环 | 恢复演练和证据 ZIP |

## 6. 当前 FS 结论

当前系统具备较多设计和工程证据，足以支持 CSV 文档包初稿和验证计划编制；但它们不是受控 QA 文件。正式 FS/配置说明需要将上述设计转入受控文档编号、版本、审批和变更控制，并在 RTM 中逐条追踪到测试结果。
