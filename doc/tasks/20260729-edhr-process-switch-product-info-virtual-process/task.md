# 20260729 eDHR 切换工序产品信息虚拟工序修复

## Task Goal

修复 eDHR 填写页“切换工序”弹窗把产品信息任务合并到粗洗工序、并在“切换填写人”中把产品信息填写人归入粗洗工序的问题。产品信息必须沿用正式批记录任务数据来源，同时在页面交互中作为独立虚拟 `80 产品信息` 工序处理。

## Milestones

- [x] 定位切换工序与切换填写人的错误分组链路。
- [x] 建立聚焦回归测试并记录 RED。
- [x] 实施最小正式修复。
- [ ] 运行目标测试、相邻回归和类型检查。
- [ ] 完成真实页面只读 E2E 验证。
- [ ] 完成任务收尾、提交并推送。

## Expected Verification

- “切换工序”从当前批次全部普通任务构建列表时，产品信息任务必须使用独立分组键，显示名称为“产品信息”，显示排序为 `80`。
- 粗洗工序卡片不得包含产品信息任务；产品信息卡片只能包含产品信息任务。
- 当前页面位于产品信息任务时，“切换填写人”只能展示产品信息任务的填写人，不得混入粗洗工序其它表单填写人。
- 当前页面位于粗洗工序时，“切换填写人”不得混入产品信息任务填写人。
- 不修改后端来源 `routeProcessId`，不使用 `formBindings`、表单槽位或工序开始配置推断产品信息。
- 目标静态合同、相邻工序切换合同和 `pnpm ts:check` 通过；运行态前置齐备时通过真实 Playwright 只读验证。

## Applicable Gates

- `docs/frontend-development.md#eDHR 产品信息虚拟 80 工序门禁`：产品信息可保留来源 `routeProcessId/routeProcessSort`，但页面必须按 `MAIN + BATCH_RECORD + 产品信息/80` 独立分组，不得并入来源工序。
- `docs/frontend-development.md#eDHR 辅助模式当前工序 assistRows 路由门禁`：切换工序必须继续展示当前批次全部普通任务，并保持正式 `openTask`、只读 execution 和 `task/preview` 导航边界。
- `docs/frontend-development.md#前端 Route Query ID 比较门禁`：`batchTaskId`、`workTaskId`、`routeProcessId` 等标识比较必须统一使用 route-id 语义。
- `docs/frontend-development.md#切换填写人 FormCenter 槽位导航门禁`：填写人候选必须保留同一显示工序内传统批记录与 FormCenter 槽位任务，不得放宽正式打开权限。
- `docs/e2e-rules.md#Windows 换行与脚本行为同步`：静态合同按稳定函数和标识定位，读取源码时兼容 CRLF/LF。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；统一填写页内产品信息虚拟工序识别、分组和当前工序任务范围，消除仅按来源 `routeProcessId` 合并造成的规则漂移。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress
