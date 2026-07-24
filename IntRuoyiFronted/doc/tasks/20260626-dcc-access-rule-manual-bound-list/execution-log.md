# 执行日志：DCC 访问规则左侧仅显示手动保存目录

- BDD: 左侧仅显示手动保存目录路径 -> Given 后端 access-rule-directories 只返回手动保存目录 / When 页面加载完成 / Then 左侧只显示这些目录的完整路径字符串。
- BDD: 继承目录进入页面时保持未保存目录态 -> Given 用户通过树形新增入口选择一个当前不在左侧列表中的目录 and 后端该目录已存在继承规则 / When 页面完成切换 / Then 右侧加载真实规则但标题区仍显示 未保存目录。
- BDD: 手动保存后目录进入左侧列表 -> Given 用户正在维护一个未保存目录草稿 / When 点击保存规则成功 / Then 页面刷新左侧列表并把当前目录切换为已绑定状态。
- BDD: 删除整组规则后目录从左侧消失 -> Given 当前目录属于左侧手动保存列表 / When 用户删除该目录全部访问规则 / Then 左侧列表刷新后不再显示该目录。
- BDD: 已绑定目录误判回归继续受保护 -> Given 当前目录已经在左侧手动保存列表中 / When 页面完成初始化或刷新规则 / Then 标题区不得错误显示 未保存目录。

- INFO: task-created -> 已创建前端任务文档，准备扩展访问规则左侧目录列表和草稿态静态合同。
- RED: access-rule-bound-directory-static -> FAIL，`node tests/e2e/dcc-access-rule-bound-directory-list-static.spec.js` 初次失败：`package.json` 未暴露 `e2e:dcc:access-rule-bound-directory-list:static` 脚本入口。
- RED: ts-check-default-memory -> FAIL，`pnpm ts:check` 在默认 Node 堆大小下因 `heap out of memory` 退出，未完成类型验证。
- GREEN: access-rule-save-validation-static -> PASS，`node tests/e2e/dcc-access-rule-save-validation-static.spec.js` 通过。
- GREEN: access-rule-header-context-static -> PASS，`node tests/e2e/dcc-access-rule-header-context-static.spec.js` 通过。
- GREEN: access-rule-bound-directory-static -> PASS，补充 `package.json` 脚本入口后，`node tests/e2e/dcc-access-rule-bound-directory-list-static.spec.js` 通过；静态契约确认左侧仅消费 `getAccessRuleDirectories()`，草稿态仅由“不在手动列表里”决定。
- GREEN: ts-check -> PASS，`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` 通过。
- GREEN: experience-preflight -> PASS，确认命中 `docs/login-access.md` 且本机入口 `http://localhost:8081/login?redirect=/index` 返回 `200`，允许进入官方登录最小路径预检。
- GREEN: login-preflight -> PASS，`node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /dcc/controlled-file/access-rules --target-text 访问规则 --timeout 90000` 真实登录已进入目标页。
- BLOCKER: real-e2e-sample-missing -> `node D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260626-dcc-access-rule-manual-bound-list\verify-access-rule-manual-bound.e2e.mjs` 已真实登录并检查 `/admin-api/dcc/directories/tree`、`/admin-api/dcc/directories/access-rule-directories` 及各目录真实规则，但当前测试租户未找到“有真实访问规则且不在手动保存列表里”的继承目录样本，故按规则停止最终页面验收，不伪造通过。
- INFO: runtime-diagnosis -> 用户追加反馈左侧仍显示大量 `质量管理/...` 目录后，复核页面源码确认左侧仅消费 `getAccessRuleDirectories()`；进一步排查发现问题不在前端口径，而在本机运行态仍使用旧后端 jar 且数据库尚未补上 `access_rule_manually_bound` 列。
- GREEN: local-runtime-aligned -> PASS，后端已通过 `restart-int-ruoyi-local.ps1` 自动补齐 `access_rule_manually_bound` 列并重启到新 runtime，`http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- GREEN: authenticated-page-response-check -> PASS，真实登录 `http://localhost:8081/dcc/controlled-file/access-rules` 后等待页面实际 `GET /admin-api/dcc/directories/access-rule-directories` 响应，返回 `code=0`、`data=[]`；页面左侧同步渲染 `0` 项，证明当前运行态下左侧已只显示手动保存目录。
