# EDHR 待处理工序标题单行显示

## 任务目标

- 修复批执行详情页左侧待处理工序蓝框内标题换行问题。
- 蓝框内工序标题必须单行显示，超长时用省略号截断，不撑高卡片、不换行。

## 里程碑

1. [x] 建立任务文档与 BDD/TDD 记录。
2. [x] 新增静态回归测试，捕获待处理工序标题单行省略契约。
3. [x] 修复 `BatchExecutionDetailPage.vue` 中待处理标题样式。
4. [x] 运行验证并提交本次任务相关改动。

## 经验门禁

- PowerShell 命令前已读取 `docs/powershell-memory.md`，本轮避免 Bash heredoc 与未指定编码的中文读写。
- 前端样式改动已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，沿用蓝/中性运维控制台风格与长文本省略规则。
- 本次不涉及真实 E2E 写入、服务器操作、数据库修改、发布或受保护资源变更。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，移除导致待处理标题换行的样式覆盖，恢复单行省略契约。
- 是否存在临时补丁或绕过：否。

## 预期验证

- `node tests/e2e/edhr-pending-task-name-single-line-static.spec.js` 先 RED 后 GREEN。
- `node --check tests/e2e/edhr-pending-task-name-single-line-static.spec.js` 通过。

## 验证结果

- RED：`node tests/e2e/edhr-pending-task-name-single-line-static.spec.js` -> FAIL，待处理工序标题缺少独立样式并被复盘标题换行样式覆盖。
- GREEN：`node --check tests/e2e/edhr-pending-task-name-single-line-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/edhr-pending-task-name-single-line-static.spec.js` -> PASS。

## 当前状态

- 已完成。

## Current Status

completed.
