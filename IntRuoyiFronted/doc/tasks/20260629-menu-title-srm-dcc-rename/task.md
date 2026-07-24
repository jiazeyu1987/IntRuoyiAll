# 任务：SRM 与文控中心菜单改名（前端）

- Task ID: `20260629-menu-title-srm-dcc-rename`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

同步前端自动化与菜单断言中的正式菜单名称，将 `供应商关系管理` 改为 `SRM`，将 `DCC文控中心` 改为 `文控中心`。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-role-export-import-sync\task.md`
- 状态：`in_progress`
- 处理说明：用户已明确授权跳过该门禁，本次仅执行菜单名称微调并在任务记录中留痕。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`：任务开始前先命中相关经验并摘录。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：中文文档和输出保持 UTF-8。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：不改导航结构与交互，只改正式菜单文案与断言。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。直接以真实菜单标题更新测试契约，不增加兼容旧标题的双写逻辑。
- 是否存在临时补丁或绕过：否。

## Milestones

- M1: 建立任务文档与执行日志。状态：completed。
- M2: 更新前端静态/E2E 断言中的菜单标题。状态：completed。
- M3: 执行定向验证并回填证据。状态：completed。

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\dcc-upload-size-policy-management-static.spec.js`

## Current Blockers

- 暂无。

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\dcc-upload-size-policy-management-static.spec.js` -> PASS
- `rg -n -F "供应商关系管理"` / `rg -n -F "DCC文控中心"` 定向扫描前端测试与脚本 -> PASS
