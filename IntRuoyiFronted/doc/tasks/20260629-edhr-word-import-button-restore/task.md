# 任务：恢复电子批记录 Word 导入按钮

- Task ID: `20260629-edhr-word-import-button-restore`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

恢复电子批记录三栏页面中的 Word 导入入口，允许用户从现有页面直接触发 `.doc` 模板导入，同时保持当前三栏布局和已有导入 API/状态流不变。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-smart-scheduling-smoke\task.md`
- 状态：`blocked`
- 处理说明：上一任务已处于阻塞状态，本次仅在电子批记录页范围内改动，不混入排产冒烟链路。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 本次为前端回归修复，先只命中 PowerShell/编码门禁，不扩展到真实 E2E 高风险动作。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文任务文档、测试和日志读写必须显式 UTF-8。
  - PowerShell 命令串联不得使用 `&&`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接恢复现有导入链路的可见入口，不增加兼容分支。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 电子批记录页签恢复 Word 导入入口 -> Given 用户进入电子批记录三栏页面 / When 页面渲染批记录名称面板标题区 / Then 用户可以看到“导入 Word”按钮并触发现有 .doc 文件选择与导入流程。`

## Milestones

1. M1：定位导入入口缺失根因并建立任务文档。`completed`
2. M2：先让导入入口静态契约以 RED 失败暴露。`completed`
3. M3：最小恢复入口并验证 GREEN 通过。`completed`
4. M4：补齐执行证据与任务收尾。`completed`

## Expected Verification

- `node tests/e2e/electronic-batch-record-word-import-entry-static.spec.js`

## Current Blockers

- 无。

## Final Verification Result

- `node tests/e2e/electronic-batch-record-word-import-entry-static.spec.js`：PASS。
- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js`：PASS。
