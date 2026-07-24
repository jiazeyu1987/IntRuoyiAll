# 任务：排产员工作台策略区标签重叠修复

## 任务目标

- 修复 `/mes/pro/scheduler-workbench` 排产设置区中 `排产优先级规则`、`发布/重排保护规则` 标签在当前布局下换行挤压的问题。
- 保持现有字段、权限、接口、表单校验和保存行为不变。
- 让策略设置区在桌面宽度下保持紧凑且可读，不做无关视觉重构。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-mes-feedback-confirm-batch-cross-page\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成，本次只处理排产员工作台设置区的排版缺陷，不混入报工页或其他 MES 页面改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 页面样式保持 IntPP 运维台的紧凑表单与浅边框风格，不做无关视觉重做。
  - 涉及真实登录或 Playwright 只读复验前，第一条登录相关命令必须先执行官方 `login-preflight.mjs`。
  - PowerShell 读取和记录中文文件时必须显式使用 UTF-8。
  - 执行真实浏览器只读复验前，先在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。只修正布局约束，不增加兜底分支。
- `是否从根因和长期维护角度解决`：是。通过稳定的标签宽度和不可折行约束修复表单项排版，而不是只对单个截图位置做临时位移补丁。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 策略设置长标签不再折行挤压 -> Given 用户打开排产员工作台设置区 / When 页面渲染 排产优先级规则 与 发布/重排保护规则 表单项 / Then 标签必须完整展示且不折成两行挤压控件或与复选框文案重叠。`
- `BDD: 策略设置栅格换行时仍保持可读 -> Given 设置区存在多列策略项 / When 页面宽度变化导致表单项换列 / Then 每个表单项只在栅格层级换行，不因标签宽度不足出现文字重叠。`

## 里程碑

1. M1：建立任务台账并补 RED 静态回归。`COMPLETED`
2. M2：最小修改工作台策略设置区模板与样式。`COMPLETED`
3. M3：运行 GREEN 静态回归与定向验证。`COMPLETED`
4. M4：回写证据、命令记录和收尾预览。`COMPLETED`

## 预期验证

- `node tests/e2e/mes-scheduler-workbench-policy-label-layout-static.spec.js`
- `node tests/e2e/mes-scheduler-workbench-policy-settings-static.spec.js`
- `node tests/e2e/mes-scheduler-workbench-density-layout-static.spec.js`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-mes-scheduler-workbench-policy-label-overlap\bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-mes-scheduler-workbench-policy-label-overlap\frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/mes-scheduler-workbench-policy-label-layout-static.spec.js` -> PASS
- `node tests/e2e/mes-scheduler-workbench-policy-settings-static.spec.js` -> PASS
- `node tests/e2e/mes-scheduler-workbench-density-layout-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/scheduler-workbench --target-text 排产设置` -> PASS
- `只读 Playwright 真实页面复验` -> PASS，截图 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\20260628-mes-scheduler-workbench-policy-label-overlap\policy-label-layout-after-fix.png` 显示策略区不再重叠，DOM 读数确认两条长标签均为 `white-space: nowrap`

## 完成记录

- 工作台策略区的四个表单项都切换为统一的标签列 + 内容列布局。
- `排产优先级规则` 与 `发布/重排保护规则` 已增加独立类和不可折行标签约束，避免长标签被表单栅格挤成两行。
- 保护规则复选框内容区允许自动换行，避免标签和复选框文案再次互相压住。
