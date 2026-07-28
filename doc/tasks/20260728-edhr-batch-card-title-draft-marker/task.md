# 20260728 eDHR 右侧表单卡片标题与草稿标识修复

## Task Goal

修复 eDHR 批次详情右侧当前工序表单卡片的标题歧义：每张卡片不再重复显示同一个批次执行编号，改为显示真实表单名称；当任务状态为草稿时，在名称后追加 ASCII `*`。

## Scope

- 修改前端批次详情页 `BatchExecutionDetailPage.vue` 的右侧当前工序表单卡片标题展示。
- 不修改后端接口、表单任务生成、排序、权限、打开填写、动态表单和产品信息固定表单数据规则。
- 不合并、不隐藏不同表单任务。

## Milestones

- [x] M1: 建立聚焦静态合同并获得 RED 证据。
- [x] M2: 实现卡片标题名称与草稿 `*` 标识。
- [x] M3: 更新受影响静态合同并获得 GREEN 证据。
- [x] M4: 运行类型检查和可用真实页面只读验证前置检查。
- [ ] M5: 收尾清理、经验沉淀、提交与推送状态记录。

## Expected Verification

- `node tests/e2e/edhr-batch-card-title-draft-marker-static.spec.js`
- `node tests/e2e/edhr-batch-companion-forms-right-panel-static.spec.js`
- `node tests/e2e/edhr-batch-process-companion-forms-static.spec.js`
- `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js`
- `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js`
- `pnpm ts:check`
- 当前 `8081/48081`、登录态和可读批次详情数据齐备时，使用 Playwright 走真实批次详情页面只读验证；前置缺失时 fail fast 记录 blocker。

## BDD Scenarios

### Scenario 1: 右侧卡片标题使用表单名称

- Given 用户打开 eDHR 批次详情页并选中某个工序
- When 右侧展示该工序的多张表单任务卡片
- Then 每张卡片的主标题显示该任务的表单名称，而不是重复显示同一个批次执行编号

### Scenario 2: 草稿任务名称追加星号

- Given 当前工序下存在状态为草稿的表单任务
- When 右侧渲染该任务卡片
- Then 卡片标题在表单名称后追加 ASCII `*`

### Scenario 3: 非草稿任务名称不追加星号

- Given 当前工序下存在待打开、填写中、已完成或其他非草稿状态的表单任务
- When 右侧渲染该任务卡片
- Then 卡片标题仅显示表单名称，不追加 `*`

## Applicable Experience Gates

- 前端静态合同隔离门禁：窄范围页面缺陷先新增聚焦静态合同并记录 RED/GREEN；宽合同存在无关失败时不得顺手扩大修复。
- 静态合同与真实 E2E 同步门禁：更新静态合同后必须重跑涉及的静态合同；真实 E2E 前置缺失时不得用 API-only 替代页面验证。
- eDHR 右侧红框元信息隐藏门禁：修改右侧栏时必须确认单据卡片填写人仍保留，并运行红框隐藏与填写人保留合同。
- eDHR 路线表单跳过口径门禁：右侧路线表单卡片修改不得改变可选/必填、打开填写、查看和跳过动作口径。
- 工艺路线三类配置术语契约：本任务只修批记录表单任务卡片展示，不把表单槽位或工序开始配置替代为批记录表单来源。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，移除卡片级重复批次号标题并新增显式展示 helper。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260728-edhr-batch-card-title-draft-marker/task.md
- doc/tasks/20260728-edhr-batch-card-title-draft-marker/execution-log.md
- doc/tasks/20260728-edhr-batch-card-title-draft-marker/verification-report.md
- doc/tasks/20260728-edhr-batch-card-title-draft-marker/bug-regression-evidence.md
- doc/tasks/20260728-edhr-batch-card-title-draft-marker/frontend-feature-evidence.md

## Current Status

ready_for_closeout
