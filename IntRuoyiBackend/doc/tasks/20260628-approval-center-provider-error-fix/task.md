# 任务：审批中心 provider 异常清理

## 任务目标

- 修复审批中心聚合查询中的后端 provider 异常，去除页面上的 `Set of process instance ids is empty` 报错。
- 修复 DCC 历史审批汇总在源文件已删除场景下直接抛出 `APPROVAL_BUSINESS_OBJECT_REQUIRED` 导致整页失败的问题。
- 保持审批中心 fail-fast 原则，不吞异常；仅对已确认的历史列表根因做正式行为修正。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260627-release-required-sql-dcc-prereq-apply-gap\task.md`
- 状态：`COMPLETED`
- 处理说明：最近同仓后端任务已完成，本次单独处理审批中心 provider 异常，不覆盖发布 SQL 任务结果。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 真实 Playwright E2E 前必须先跑官方 `login-preflight.mjs` 最小登录路径，未跑通前不得直接下结论。
  - PowerShell 读取和记录中文内容时必须显式使用 UTF-8。
  - 真实 E2E 前必须先在执行日志记录 `GREEN: experience-preflight -> PASS`，再执行长链路验证。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，分别修正 provider 空流程实例集合处理与 DCC 历史审批汇总建模，不用前端掩盖异常。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: provider 空任务页不应触发 BPM 空集合异常 -> Given 审批中心聚合查询某 provider 返回空任务页 / When provider 继续构建流程实例映射 / Then 返回空分页结果而不是抛出 Set of process instance ids is empty。`
- `BDD: DCC 已办历史记录在源文件已删除时仍可显示摘要 -> Given DCC 历史审批任务仍存在但对应 controlled file 行已被物理删除 / When 审批中心查询已办列表 / Then 返回可展示的历史审批摘要并明确标记业务已删除，而不是整页报错。`

## 里程碑

1. M1：补任务文档、命令记录并建立 RED 测试。
2. M2：修复 provider 空流程实例集合异常。
3. M3：修复 DCC 历史审批摘要缺失异常并回归测试。

## 预期验证

- `mvn -pl yudao-module-dcc,yudao-module-bpm -am "-Dtest=DccApprovalTaskAdapterTest,ApprovalCenterServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node scripts/preflight/login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /approval-center/todo --target-text 审批中心`
- `node yudao-ui-admin-vue3/tests/e2e/approval-center-phase2-real.e2e.mjs`

## 最终验证结果

- `mvn -pl yudao-module-dcc,yudao-module-bpm -am "-Dtest=DccApprovalTaskAdapterTest,ApprovalCenterServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- `node scripts/preflight/login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /approval-center/todo --target-text 审批中心` -> PASS
- `$env:APPROVAL_CENTER_E2E_BASE_URL='http://localhost:8081'; $env:APPROVAL_CENTER_E2E_BACKEND_URL='http://127.0.0.1:48081'; node yudao-ui-admin-vue3/tests/e2e/approval-center-phase2-real.e2e.mjs` -> PASS

## 完成记录

- 审批中心真实页面已验证不再出现 `Set of process instance ids is empty` 与 `APPROVAL_BUSINESS_OBJECT_REQUIRED: DCC controlled file summary snapshot not found` 两条红字。
- 测试租户 `aoteman` 可从本机 `http://localhost:8081/approval-center/todo` 真实登录进入审批中心。
- 审批中心真实链路已完成列表加载、子页签可见、轨迹抽屉打开和模块详情跳转验证。
- E2E 产物已写入 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260628-approval-center-provider-error-fix\e2e-artifacts\`。

## Current Status

completed
