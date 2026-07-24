# 任务：审批中心一级入口重构

## 任务目标

- 将现有单页 `/approval-center` 重构为与电子签名一致的一级统一入口容器。
- 用户可见名称统一为“审批中心”，并作为独立一级标签页打开。
- 第一版仅承载现有聚合审批能力：`待办 / 已办 / 我发起的 / 抄送我的 / 签名待处理`。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-remove-outer-card\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成，本次任务仅修改审批中心路由与容器页，不覆盖上一任务交付。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 审批中心页面继续遵循 IntPP 运维台风格，保持白底、紧凑工具栏、连续工作面，不做装饰性重设计。
  - PowerShell 读取和记录中文内容时必须显式使用 UTF-8。
  - 本轮仅做前端代码与静态验证，不执行真实 E2E、服务器写入、发布、恢复或其他高风险动作，因此无需 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，通过统一一级容器路由和子页签路由收敛审批入口，不继续维持单页 query 双制为主结构。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 审批中心以一级标签打开 -> Given 用户进入审批中心任一入口 / When 路由命中审批中心 / Then 页面以“审批中心”一级容器打开，而不是旧的单工作台隐藏页。`
- `BDD: 子页签由子路由驱动 -> Given 用户访问 /approval-center/todo 或其他审批中心子路径 / When 页面渲染 / Then 激活对应子页签并映射到正确的审批视图类型。`
- `BDD: 旧 query 入口自动归一 -> Given 用户访问 /approval-center?moduleCode=DCC&viewType=TODO / When 页面初始化 / Then 自动规范到 /approval-center/todo 并保留筛选参数。`
- `BDD: 非法审批页签直接报错 -> Given 用户访问不支持的审批中心子路径或非法 viewType / When 页面解析路由 / Then 直接显示明确错误提示，不静默降级到默认页签。`

## 里程碑

1. M1：补任务文档、命令记录和审批中心子路由 RED 测试。
2. M2：重构审批中心一级容器路由和容器页。
3. M3：回归现有静态契约并补证据文档。

## 预期验证

- `node scripts/approval-center-page-contract.test.mjs`
- `node tests/e2e/approval-center-root-tab-static.spec.mjs`
- `node tests/e2e/approval-center-phase2-static.spec.mjs`
- `node tests/e2e/approval-center-phase4-static.spec.mjs`
- `node tests/e2e/approval-center-phase5-retirement-static.spec.mjs`
- `node tests/e2e/approval-center-phase8-mes-feedback-static.spec.mjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-approval-center-root-tab-refactor\frontend-feature-evidence.md`

## 最终验证结果

- `node scripts/approval-center-page-contract.test.mjs` -> PASS
- `node tests/e2e/approval-center-root-tab-static.spec.mjs` -> PASS
- `node tests/e2e/approval-center-phase2-static.spec.mjs` -> PASS
- `node tests/e2e/approval-center-phase4-static.spec.mjs` -> PASS
- `node tests/e2e/approval-center-phase5-retirement-static.spec.mjs` -> PASS
- `node tests/e2e/approval-center-phase8-mes-feedback-static.spec.mjs` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-approval-center-root-tab-refactor\frontend-feature-evidence.md` -> PASS

## 完成记录

- 审批中心已重构为一级隐藏容器路由，根路径 `/approval-center` 自动归一到 `/approval-center/todo`。
- 审批中心已新增五个子路由页签：`todo`、`done`、`my-initiated`、`cc`、`signature-pending`。
- 审批中心页面已由子路由驱动页签状态，旧 `viewType` query 入口会自动归一到对应子路径。
- 页面文案已统一为“审批中心”，并保持现有聚合审批筛选、列表、轨迹抽屉与模块详情跳转能力不变。

## Current Status

completed
