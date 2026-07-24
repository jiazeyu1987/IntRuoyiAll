# 执行日志：DCC 单文件基础信息维护前端

BDD: 文控可见修改入口 -> Given 当前登录用户角色包含 `doc_control` / When 打开 DCC 受控浏览列表或文件详情 / Then 页面显示“修改基础信息”入口。

BDD: 非文控不可见修改入口 -> Given 当前登录用户角色不包含 `doc_control` / When 打开 DCC 受控浏览列表或文件详情 / Then 页面不显示“修改基础信息”入口。

BDD: 文控保存基础信息 -> Given 文控打开编辑弹窗并填写产品名称、文件名称、产品编号、文件编号、文件类别和受控目录 / When 点击保存 / Then 前端调用 `PUT /dcc/controlled-files/{id}/metadata` 并刷新当前数据。

BDD: 保存失败明确暴露 -> Given 后端返回目录不合法、权限不足或文件链冲突 / When 保存基础信息 / Then 弹窗显示后端错误，不关闭弹窗，不伪造成功。

RED: node scripts/dcc-controlled-file-metadata-edit.test.mjs -> FAIL, expected missing frontend metadata API contract, doc_control-only entry, shared edit dialog, and category-bound directory selection.

GREEN: node scripts/dcc-controlled-file-metadata-edit.test.mjs -> PASS, 4 tests.

GREEN: pnpm ts:check -> PASS.

GREEN: Playwright negative role gate at http://localhost:8081/dcc/controlled-file/browser using 测试租户/aoteman -> PASS, roles tenant_admin/showroom_publicity, metadata edit entry count 0.

BLOCKED: Playwright positive doc_control save path -> BLOCKED, 测试租户/aoteman does not have role code doc_control.

BLOCKED: npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-metadata-positive run-code --filename doc/tasks/20260604-dcc-controlled-file-metadata-edit/dcc-controlled-file-metadata-positive.mjs -> BLOCKED, 正式角色精简接口 `/system/role/simple-list` 未返回启用角色 code `doc_control`，脚本在赋权前 fail fast，未执行角色写入或文件基础信息保存。

BLOCKED: npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-metadata-copy-role run-code --filename doc/tasks/20260604-dcc-controlled-file-metadata-edit/dcc-controlled-file-metadata-copy-role-positive.mjs -> BLOCKED, `芋道源码` 租户角色分页接口未找到 `code=doc_control` 模板角色；脚本在写入测试租户前 fail fast，未创建角色、未赋权、未保存文件基础信息。

INFO: 用户要求在 `芋道源码` 与测试租户都新增 `doc_control` 角色；按项目基线，未修改受保护的 `芋道源码` 租户，仅在本机测试租户创建 `文控/code=doc_control`。

GREEN: npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-metadata-create-test-role run-code --filename doc/tasks/20260604-dcc-controlled-file-metadata-edit/dcc-controlled-file-metadata-create-test-role-positive.mjs -> PASS, 创建/确认测试租户角色 `910217/doc_control`，临时赋给 `测试租户/aoteman`，浏览页入口数量 10，详情页入口可见，保存文件 `2054545668044046254` 产品名称 `E2E文控验证1780562941888` 成功，随后恢复文件 productName 与用户原角色 `111,910209`。

GREEN: docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -N -e "SELECT id, tenant_id, name, HEX(name), code, status, deleted FROM system_role WHERE tenant_id=122 AND code='doc_control';" -> PASS, 角色 `910217/doc_control` 名称为 `文控`，UTF-8 hex 为 `E69687E68EA7`。

GREEN: final reverify `node scripts/dcc-controlled-file-metadata-edit.test.mjs` -> PASS, 4 tests.

GREEN: final reverify `pnpm ts:check` -> PASS.

GREEN: frontend-feature evidence validator -> PASS.

GREEN: git diff --check -> PASS, only CRLF normalization warnings.

GREEN: task-closeout-cleanup preview -> PASS, no blocked cleanup paths; preview kept task records and reported only task-specific auxiliary artifacts as cleanup candidates.
