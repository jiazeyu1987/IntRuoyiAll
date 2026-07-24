# 任务：排产用途产能整数化

## 任务目标

- 将工艺路线排产用途配置中的 `产能(h)` 调整为整数输入与整数展示。
- 保存时对有限产能模式下的 `产能(h)` 执行正整数校验。
- 保持 `班次小时`、`标准班次产能`、`1000产品制作时间(h)` 等其他现有交互不变。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-simulate-return\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成，本次只收敛 `RouteUsePage.vue` 中排产用途的 `产能(h)` 精度和校验，不混入电子批记录或审批中心改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 排产用途配置继续保持 IntPP 运维台式紧凑表格风格，不做无关布局和视觉改版。
  - PowerShell 读取和记录中文文件时必须显式使用 UTF-8。
  - 本轮只做本机静态验证，不执行真实 Playwright、登录写入、服务器操作或其他高风险动作，因此无需 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。同步收敛输入控件、保存校验和静态契约，避免再次出现“页面显示允许小数、保存规则又不同步”的回归。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 排产用途有限产能按整数录入 -> Given 用户打开工艺路线排产用途配置 / When 编辑有限产能工序的 产能(h) / Then 输入控件只允许整数，不再保留小数位。`
- `BDD: 排产用途有限产能按正整数保存 -> Given 用户启用有限产能工序 / When 保存用途配置 / Then 系统要求 产能(h) 必须为大于 0 的整数，并在不满足时直接暴露明确错误。`
- `BDD: 标准班次产能继续按整数展示 -> Given 用户录入整数小时产能且存在班次小时 / When 页面实时计算标准班次产能 / Then 标准班次产能仍按整数显示。`

## 里程碑

1. M1：创建任务文档、更新请求命令记录并补整数契约 RED 用例。
2. M2：调整 `RouteUsePage.vue` 的 `产能(h)` 输入精度、格式化与保存校验。
3. M3：运行定向静态回归并补齐执行证据。

## 预期验证

- `node tests/e2e/mes-route-use-config-display-static.spec.js`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-route-use-hourly-capacity-integer\frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/mes-route-use-config-display-static.spec.js` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-route-use-hourly-capacity-integer\frontend-feature-evidence.md` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260628-route-use-hourly-capacity-integer --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` -> PASS，预览保留 `task.md` / `execution-log.md`，其余证据文件按规则归类为可清理产物

## 完成记录

- `RouteUsePage.vue` 中排产用途 `产能(h)` 输入已改为整数模式，不再保留小数位。
- 有限产能保存时已改为正整数校验，错误文案明确提示 `产能(h)必须是大于 0 的整数`。
- 历史小时产能加载时会统一收敛为整数，避免旧小数值在页面回显和保存口径之间不一致。

## Current Status

completed
