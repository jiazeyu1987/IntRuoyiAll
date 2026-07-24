# 任务：产品主数据引用按钮缺少 id 参数回归修复

## 任务目标

- 修复产品主数据列表点击`引用`按钮时报错 `Required request parameter 'id' for method parameter type Long is not present` 的前端回归。
- 保持现有后端 `GET /mdm/product/references` 契约不变，只修正前端请求参数名。
- 为该点击链路补一条静态回归，防止再次把 `id` 传成其他字段名。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个 frontend 任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-showroom-product-audio-modal\task.md`
- 状态：`COMPLETED`
- 处理：已确认上一任务文档标记完成，不阻塞本次产品主数据缺陷修复。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 本轮仅修复产品主数据列表的点击链路和 API 参数契约，不做无关视觉重构。
  - 本轮只执行静态回归、契约验证和类型检查，不做真实登录或写入型 E2E。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。保持接口真实报错语义，只修正前端请求参数名。
- `是否从根因和长期维护角度解决`：是。直接对齐前后端正式契约 `id`，并补充静态回归约束。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 点击引用按钮时前端必须提交当前行 id -> Given 用户在产品主数据列表点击某一行“引用”按钮 / When 前端请求 /mdm/product/references / Then 查询参数必须包含该行 id，后端返回引用统计后页面打开展示弹框。`

## 里程碑

1. M1：创建任务台账并新增 RED 静态回归。`COMPLETED`
2. M2：最小修复前端 API 参数名并通过 GREEN 验证。`COMPLETED`
3. M3：补齐缺陷证据、请求命令日志和收尾预览。`COMPLETED`

## 预期验证

- `node tests/e2e/mdm-product-reference-id-static.spec.js`
- `node scripts/mdm-product-master-contract.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mdm-product-reference-id-missing\bug-regression-evidence.md`

## 最终验证结果

- `node tests/e2e/mdm-product-reference-id-static.spec.js` -> PASS
- `node scripts/mdm-product-master-contract.test.mjs` -> FAIL，当前脚本命中仓库现有 `src/api/dcc/controlledFile/workflow.ts` 契约断言 `productMasterId:\s*number` 不匹配，与本次 `references id` 参数修复无关。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> FAIL，本地 `node_modules` 缺少 `@volar/typescript/lib/quickstart/runTsc`，未进入业务类型检查阶段。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mdm-product-reference-id-missing\bug-regression-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-mdm-product-reference-id-missing --mode preview` -> PASS，预览结果 `status=ready`；默认保留 `task.md`、`execution-log.md`，若后续执行 apply 将把 `bug-regression-evidence.md` 视为可删候选。
