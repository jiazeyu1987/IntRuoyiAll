# Execution Log

INFO: experience-index -> matched docs/powershell-memory.md, docs/worktree-memory.md, docs/login-access.md, D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md.

BDD: 上传页自动关联现行版本 -> Given 申请人选择文件类别并输入已有文件编号, When 后端返回唯一现行版本, Then 页面展示现行版本号、文件名称、状态、路径、修改中，并清晰驱动升版/作废校验。

BDD: 变更方式前端校验 -> Given 申请人选择新建/升版/作废, When 填写编号、版本和上传文件, Then 页面按变更方式显示必填提示和明确错误，不静默失败。

BDD: 文控部门勾选 -> Given 流程进入文控归档前确认, When 文控勾选部门并提交, Then payload 包含最终部门范围，页面显示无接收人等后端阻塞原因。

GREEN: experience-preflight -> PASS, 已读取 PowerShell、worktree、login/E2E、前端样式门禁；本阶段仅进行本机 worktree 代码与测试。

RED: node tests/e2e/dcc-upload-current-version-static.spec.js -> FAIL, package.json 缺少脚本且上传页尚未提供按文件编号自动关联现行版本、变更方式与现行版本面板契约。

RED: node tests/e2e/dcc-doc-control-department-distribution-static.spec.js -> FAIL, 第 4 节点仍按用户选择电子接收人，未按流程图勾选部门下发范围。

GREEN: pnpm run e2e:dcc:upload-current-version:static -> PASS, 上传页提供 current-version 接口调用、现行版本面板、变更方式和 changeType 提交。

GREEN: pnpm run e2e:dcc:doc-control-department-distribution:static -> PASS, 文控第 4 节点使用部门树多选并提交 selectedDistributionDepartmentIds。

REGRESSION: NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check -> PASS。

GREEN: pnpm run e2e:dcc:doc-control-department-distribution:real -> PASS, 测试租户 aoteman 在 http://127.0.0.1:8086 提交并审批 `CODEX-DCC-DEPT-20260716010719`；第 4 节点请求包含 stampedPdfUploadTicket、sessionId 和 selectedDistributionDepartmentIds，最终文件 ACTIVE 并生成部门下发记录。

GREEN: pnpm run e2e:dcc:upload-current-version:real -> PASS, 使用真实 ACTIVE 文件编号 `CODEX-DCC-DEPT-20260716010719` 验证上传页展示现行版本并自动切换升版；未产生 DCC 写请求。
