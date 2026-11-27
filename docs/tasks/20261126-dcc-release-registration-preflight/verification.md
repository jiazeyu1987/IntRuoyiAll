# DCC迁移登记与目标预检修复

## 结果与范围

- 修复提交：`8e8b4cf7cf23b4aa2473a190f5c3822332b13411`，基线：`1fedb340e1dbb7242d06a4068f9a47fac8fb5425`。
- 对应失败构建：`JOB-fbf7db2cc2`，版本 `2026-11-26-22-25-01`。将4份漏登记SQL加入正式 `release-sources.json/migrations`，没有排除执行或修改历史迁移。
- 新增文件夹模板菜单同名预检；将CE材料预检由两条查询修为一条只读查询，保留全部历史行的校验并返回具体阻断原因。
- 全量计划继续发现已登记的 `20261126_dcc_registration_certificate_typed_event_notify_template` 缺少强制预检，本次一并新增同名预检。
- 仅修改来源登记和3份预检。5份相关正式迁移与基线Git内容差异为空；原有本机修复、两份无关文档及JFR保留。

## 实际发布链条核对

使用真实维护代码 `C:/SpugHandoff/release/source/ops/spug/release-center/intruoyi_build.py`，未替换解析器或计划器：

1. `migration_inputs:144` → `release_source_catalog.release_paths:264`：根目录936份SQL与正式迁移、历史来源、引用、排除分类完全一致，重复/漏登记/缺文件均为0；正式输入924条迁移、1252项资产、8项来源引用，当前数据库契约收集通过。
2. `migration_inputs:160` → `release_target_preflight_contract.validate`：非schema迁移或声明 `requiresTargetPreflight=true` 的schema绑定同名预检。所有实际绑定通过单条SELECT/WITH、只读、环境元数据、唯一PASS标记及资产SHA256核验。
3. `historical_integrity:183`：只读本机 `state.sqlite3` 的已登记快照，测试855条、正式851条历史记录均与候选实际checkout字节一致。
4. `target_input_plan:223`：两环境全量依赖、处理器及必需预检均通过。对3项本次修复的绑定逐项在内存移除，两环境均准确报 `required target preflight missing (1)`，共6项负向检查通过，未放松门禁。

| 迁移 | 原类型 | 原requiresTargetPreflight | 正式预检绑定 | 测试58/正式57 |
| --- | --- | --- | --- | --- |
| `20261125_dcc_folder_template_menu` | menu | false | 本次新增，非schema强制要求 | APPLY / APPLY |
| `20261125_dcc_registration_certificate_products` | schema | false | 按现有合同不要求 | APPLY / APPLY |
| `20261126_dcc_registration_certificate_remove_ce_materials` | schema | true | 本次修正已有预检 | APPLY / APPLY |
| `20261126_dcc_registration_certificate_typed_event_notify_template` | seed | false | 本次新增，非schema强制要求 | APPLY / APPLY |
| `20261126_dcc_registration_fda_change_no_approval_date` | schema | false | 按现有合同不要求 | APPLY / APPLY |

两环境各5条APPLY、3项必需预检，缺失数均为0。产品绑定迁移的既有冲突断言、FDA日期迁移的原类型/元数据均未改；不将未绑定的手工产品诊断SQL声称为正式目标预检。

预检逐节点对应原迁移断言：文件夹模板核验主数据根、项目查询/更新权限、产品目录、注册证排序、菜单身份冲突及套餐JSON；CE材料核验2个已启用CHECK，覆盖所有租户和已删除记录的文件/材料类型；类型通知核验唯一系统模板、至少一个租户模板及逐租户无重复。授权和数据修改仍仅由原迁移执行。

## 验证证据与字节边界

- 干净detached稀疏checkout：`C:/SpugHandoff/release/dcc-inputs-80681bb00`，实际HEAD固定为上述修复提交，验证时 `git status --porcelain` 为空，仅检出SQL目录，无应用服务和端口占用。
- SQL遵守 `.gitattributes` 的CRLF包字节。主工作区原有 `20261110_mes_production_quantity_draft.sql` 为LF，与干净checkout的规范化文本完全一致，但原始SHA不同；不修改该旧文件，最终历史校验使用干净checkout通过。摘要见同目录 `release-input-verification.json`，不是Git blob的LF摘要。
- 本机隔离MySQL `8.0.39`，无网络、无端口映射，仅使用合成测试表。全部实际预检在 `START TRANSACTION READ ONLY` 内执行：菜单24个、CE材料13个、类型通知9个，共46个场景通过。覆盖首用/既有合法身份、缺失/重复前置菜单、错误排序、ID/路径冲突、非法/空套餐JSON、缺失/未启用CHECK、跨租户及已删除历史材料、系统与租户模板缺失/重复。未修改目标数据库。
- 应用来源登记测试2/2通过；3份预检的真实只读合同校验通过；普通及暂存菜单检查、提交hook与分支端口检查、`git diff --check` 通过。
- 本机复现脚本及详细日志：`.runtime/test-dcc-release-target-preflight.py`、`.runtime/verify-dcc-release-contract.py`、`.runtime/dcc-release-mysql-tests.log`、`.runtime/dcc-release-mysql-typed-event-tests.log`、`.runtime/dcc-release-fixed-inputs.json`。通过的37个场景在新增通知预检后未重复运行，仅新增9个通知场景。
- 验证后只移除了本轮生成的干净checkout和两个带任务归属标识的隔离MySQL容器；未启动应用服务、未申请端口槽位、未操作失败构建目录。两份既有无关文档及JFR的前后SHA256全部一致。

## 交接与剩余边界

本次发布输入阻塞已消除。没有连接58/57服务器，没有执行业务迁移、补业务数据、构建镜像或发布；本地台账计划不代表目标数据预检已经通过。维护线程应从最终提交重新构建，再在58/57发布流程执行真实只读目标预检；发现历史材料、菜单或模板冲突时继续明确阻断，不能补数据或改历史SQL凑通过。原失败构建目录保留。
