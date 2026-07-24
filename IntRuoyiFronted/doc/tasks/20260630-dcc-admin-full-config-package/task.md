# 任务：文控管理员全量数据包页签（前端）

- Task ID: `20260630-dcc-admin-full-config-package`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-30`
- Current Status: `blocked`

## Task Goal

在文控中心下新增“文控管理员”页签，提供与排产工作台同风格的导出/导入单文件数据包入口，直接接入后端正式聚合接口完成文控中心配置迁移。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-dcc-browser-cache-write-failure\task.md`
- 状态：`blocked`
- 处理说明：已因用户切换到更高优先级需求而显式阻塞；本次只在 DCC 文控中心页签与相关 API 合同内增补文控管理员入口。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md` 与 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 前端任务文档、日志与静态测试文件统一 UTF-8。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 保持文控中心当前白底紧凑运营台风格，只在现有页签体系内增补文控管理员页签与按钮，不做无关重排。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按钮直接命中正式聚合接口，不在前端串多个导入导出接口伪装“全量包”。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 文控中心显示文控管理员页签 -> Given 用户进入文控中心 / When 页面渲染子页签 / Then 可见新的“文控管理员”页签。`
- `BDD: 文控管理员页签显示全量包按钮 -> Given 用户打开文控管理员页签 / When 页面渲染操作区 / Then 可见导出数据包与导入数据包按钮，并保留单文件选择器合同。`
- `BDD: 前端 API 指向正式聚合接口 -> Given 用户执行文控中心全量包导出或导入 / When 前端发起请求 / Then 请求命中新后端聚合接口，而不是前端自行串调目录、类别、规则等多个接口。`

## Milestones

1. M1：建立前端任务文档并锁定页签/API 合同。`completed`
2. M2：补 RED 静态检查。`completed`
3. M3：实现页签、按钮与 API 接入。`completed`
4. M4：运行静态验证并回填证据。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\dcc-admin-full-config-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\dcc-admin-full-config-route-static.spec.js`

## Current Blockers

- 用户已切换到更高优先级的测试服 DCC 受控浏览报错排障；本任务当前静态实现已完成，但真实租户导出/导入联调尚未执行，先显式阻塞，待用户恢复优先级后继续。
