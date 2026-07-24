# 任务：NAS 转移模板类别默认其他

## 任务目标

- 在 `NAS 管理 -> 转移到 DCC` 弹窗中默认选择真实 DCC 文件类别 `其他`。
- 不新增前端假选项；提交时继续携带真实 `templateCategoryId`。
- 如果接口未返回启用的 `其他`，前端必须失败并提示缺失前置条件，不回退到其他类别。

## 前序任务检查

- 前端上一同仓任务：`doc/tasks/20260526-runtime-control-full-row-visible/`
- 状态：completed
- 边界：本任务只修改 NAS 转移弹窗默认模板类别与对应测试/证据，不触碰运行控制台改动。

## BDD 场景

- BDD: 默认选择其他模板类别 -> Given 用户打开 `NAS管理 -> 转移到 DCC` 且 DCC 类别接口返回启用的 `其他` / When 转移弹窗加载模板类别 / Then 模板类别下拉框默认选中 `其他` 的真实类别 ID，提交时继续携带该 ID。
- BDD: 缺少其他模板类别时失败 -> Given DCC 类别接口未返回启用的 `其他` / When 用户打开转移弹窗 / Then 页面必须提示 `DCC 模板类别缺少启用的“其他”`，不得回退到 `产品技术要求` 或任意首项。

## 里程碑

- [x] M1：建立任务文档并确认前序任务边界。
- [x] M2：新增先失败的前端回归测试。
- [x] M3：最小修改 NAS 转移弹窗默认类别逻辑。
- [x] M4：运行目标测试、记录 RED/GREEN，并提交前端仓库改动。

## 预期验证

- RED：`node scripts/system-nas-management.test.mjs` -> FAIL，当前代码仍优先选择 `产品技术要求`。
- GREEN：`node scripts/system-nas-management.test.mjs` -> PASS。
- GREEN：frontend feature evidence 校验通过。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- BLOCKED：真实 `localhost:8081/system/nas` E2E 因本地前端/后端未运行，且本地测试租户缺少 DCC 源类别，无法在不修改芋道源码租户数据的前提下执行。
- GREEN：task-closeout-cleanup 预览通过。

## 当前状态

- 状态：completed。
- 已完成：任务文档初始化；新增前端 RED 断言；NAS 转移模板类别加载逻辑改为仅默认启用的 `其他`；目标静态测试和类型检查通过。
- 阻塞：真实 E2E 环境缺少运行中的 `localhost:8081` / `48081`，且本地测试租户 `tenant_id=122` 没有 DCC `产品技术要求` 源类别；未为了验证修改芋道源码租户数据。

## Current Status

Completed.

## 最终验证

- RED：`node scripts/system-nas-management.test.mjs` -> FAIL，当前源码未默认选择 `其他`，仍优先查找 `产品技术要求`。
- GREEN：`node scripts/system-nas-management.test.mjs` -> PASS。
- RED：`pnpm ts:check` -> FAIL，Node 默认 4GB 堆 OOM，未产生类型错误结论。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- BLOCKED：真实 E2E -> BLOCKED，本地前端和后端未运行；本地测试租户缺少 DCC 源类别，按项目规则不得用修改芋道源码租户数据替代。
