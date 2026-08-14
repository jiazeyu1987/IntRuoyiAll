# QA 适用检验类型派生规则

## Task Goal

将 QA 规程检验项目表中的“适用检验类型”改为由抽样方案和“是否需要末检”设置自动派生：上午巡检、下午巡检默认包含；抽样方案只识别是否首检、首检数量和巡检 AQL 抽样比例；末检仅由顶部开关控制。

## Milestones

- [x] M1：记录 BDD 场景并建立聚焦 RED 合同。
- [x] M2：实现唯一派生规则并同步页面展示、完整性检查和保存载荷。
- [x] M3：完成目标测试、相邻回归、类型检查和真实页面验证。

## Expected Verification

- `node tests/e2e/qa-regulation-applicable-types-derived-static.spec.js`
- `node tests/e2e/qa-regulation-applicable-types-default-visible-static.spec.js`
- `node tests/e2e/qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs`
- `pnpm ts:check`
- Playwright 只读验证：无“首件”的抽样方案显示上午/下午巡检；有“首件：N 件”的方案额外显示首检；末检开关决定所有行是否显示末检。

## Applicable Experience Gate

- 命中 `docs/frontend-development.md#前端提交前严格验证与草稿态计算隔离门禁`：草稿表格只读派生不得调用会抛错的提交级严格校验；保存前必须严格校验 AQL 和首检数量。
- 命中 `docs/backend-development.md#MES-PQC-项目级检验快照门禁`：发布载荷中的 FIRST/PATROL/FINAL 项目及数量比例必须形成正式项目级规程快照，不得由页面文案或旧数组替代。
- 命中 `docs/backend-development.md#PQC-末检适用性按显式-true-要求-FINAL`：末检关闭时不得序列化 FINAL 项目，开启时必须包含 FINAL 项目。
- 验证要求：纯函数测试覆盖首检识别、默认双巡检、AQL 百分比原值传递和末检开关联动；静态合同锁定页面、完整性检查和保存载荷共用正式派生逻辑。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；页面展示、完整性检查和保存载荷共用同一正式派生函数。
- `是否存在临时补丁或绕过`：否。

## Cleanup Candidates

- `doc/tasks/20260809-qa-applicable-types-derived/frontend-feature-evidence.md`
- `doc/tasks/20260809-qa-applicable-types-derived/qa-applicable-types-derived.e2e.cjs`
- `IntRuoyiFronted/output/playwright/20260809-qa-applicable-types-derived/`

## Current Status

completed：实现、自动化测试、类型检查、真实页面只读验收、任务自有临时产物清理和项目经验沉淀均已完成。
