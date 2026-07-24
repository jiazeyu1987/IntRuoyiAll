# 任务：排产工作台设置区文案压缩

## 任务目标

- 将 `/mes/pro/scheduler-workbench` 设置区截图中的可见文案压缩为 4 个字以内。
- 保持现有功能、权限、交互和布局不变，只调整用户可见文本。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-feedback-pending-table-wrap\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成并记录验证结果，本次仅处理排产工作台文案，不混入报工页面改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - PowerShell 读取与记录中文文案时必须显式走 UTF-8，避免任务文档、测试断言和页面源码乱码。
  - 前端文案调整也必须先有 BDD 与 RED 证据，再做最小实现，不得直接跳过测试改页面。
  - 页面仍需保持 IntPP 运维台风格；本次只压缩文案，不增加兜底分支、隐藏逻辑或兼容写法。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接统一页面源码中的用户可见文案与静态契约，避免截图区继续出现冗长文本。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 班次与策略标题压缩 -> Given 排产员打开工作台设置区 When 页面渲染设置面板 Then 标题、副标题与班次说明均使用 4 字以内的简短文案。`
- `BDD: 策略表单标签压缩 -> Given 排产员查看策略表单 When 表单渲染 Then 各字段标签、选项与保护项文案均压缩为 4 字以内且保留原业务含义。`
- `BDD: 冒烟测试区文案压缩 -> Given 具备冒烟测试权限的排产员进入工作台 When 冒烟设置区渲染 Then 区块标题、审批开关和启停按钮文案均使用 4 字以内的简短文案。`

## 里程碑

1. M1：创建任务文档、执行日志并将静态契约改为新文案，形成 RED。`COMPLETED`
2. M2：更新工作台页面文案并保持结构、权限与功能不变。`COMPLETED`
3. M3：运行相关静态测试并回填结果。`COMPLETED`

## 预期验证

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-shift-hours-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-policy-settings-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-smoke-toggle-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-density-layout-static.spec.js`

## 当前阻塞

- 无。

## 最终验证结果

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-shift-hours-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-policy-settings-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-smoke-toggle-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-density-layout-static.spec.js` -> PASS

## 完成结果

- 排产工作台设置区截图范围内的主要可见文案已压缩为 4 字以内。
- 页面交互、权限控制、表单绑定和布局结构保持不变，仅更新了用户可见文案与相应静态契约。
