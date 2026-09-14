# 20260810 DCC 项目代码分配范围修复

## Task Goal

修复 DCC 项目代码分配中跨项目文件、已分配文件转项目、文件类型变更和执行人权限校验的实际业务阻断，保证管理员能把文件划分给正确 DCC 项目，并且文件类型修改的目录联动规则清晰、可验证。

## Milestones

- [x] 创建任务文档并记录适用门禁
- [x] 补充 BDD 场景和 RED 回归测试
- [x] 实现最小后端修复
- [x] 运行 DCC 定向 Maven 回归和技能证据校验
- [x] 提交 worktree 变更并按规则融合到 int_main

## Expected Verification

- Maven 定向测试覆盖 DCC 项目代码分配服务和受控文件元数据更新服务。
- backend-api-delivery evidence 校验通过。
- bug-regression-fix-loop evidence 校验通过。
- scoped git diff check / branch runtime port guard 在合并前通过。

## Current Status

completed

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；按 DCC 项目代码正式归属、目录和权限契约修正服务校验。
- 是否存在临时补丁或绕过：否。

## Experience Gate Summary

- 已读取 docs/experience-index.md，匹配 DCC 项目代码、文件分类、目录路径、项目代码定位、权限菜单和直接 SQL 禁止项。
- 适用门禁：DCC 项目代码相关修复必须通过正式服务和测试 fixture 落地，不得用直接 SQL、前端 payload、默认项目代码或空目录掩盖后端范围校验缺陷。
- 适用门禁：涉及 Git/worktree/合并必须遵守 docs/worktree-restrictions.md、docs/powershell-memory.md 和 branch runtime port guard。

## Cleanup Candidates

- doc/tasks/20260810-dcc-project-code-assignment-scope/backend-api-evidence.md
- doc/tasks/20260810-dcc-project-code-assignment-scope/bug-regression-evidence.md
