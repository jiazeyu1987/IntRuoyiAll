# 任务：DCC 单文件基础信息维护前端

## 任务目标

在 DCC 受控浏览列表和文件详情页增加“修改基础信息”入口。只有当前登录角色数组严格包含 `doc_control` 的账号可见入口；保存后调用后端单文件基础信息维护接口并刷新列表或详情。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260604-layout-header-keep-module-search/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改 DCC 受控文件浏览、详情、API 类型和前端测试。

## BDD 场景

- BDD: 文控可见修改入口 -> Given 当前登录用户角色包含 `doc_control` / When 打开 DCC 受控浏览列表或文件详情 / Then 页面显示“修改基础信息”入口。
- BDD: 非文控不可见修改入口 -> Given 当前登录用户角色不包含 `doc_control` / When 打开 DCC 受控浏览列表或文件详情 / Then 页面不显示“修改基础信息”入口。
- BDD: 文控保存基础信息 -> Given 文控打开编辑弹窗并填写产品名称、文件名称、产品编号、文件编号、文件类别和受控目录 / When 点击保存 / Then 前端调用 `PUT /dcc/controlled-files/{id}/metadata` 并刷新当前数据。
- BDD: 保存失败明确暴露 -> Given 后端返回目录不合法、权限不足或文件链冲突 / When 保存基础信息 / Then 弹窗显示后端错误，不关闭弹窗，不伪造成功。

## Milestones

- [x] M1：建立任务文档并确认上一前端任务已完成。
- [x] M2：新增 RED 前端静态/契约测试。
- [x] M3：实现 API 类型、编辑弹窗、浏览列表入口和详情入口。
- [x] M4：运行前端脚本、类型检查和 Playwright 真实页面验证。
- [x] M5：记录证据、运行收尾预览并提交本任务前端改动。

## Expected Verification

- RED/GREEN：`node scripts/dcc-controlled-file-metadata-edit.test.mjs`
- GREEN：`pnpm ts:check`
- GREEN：Playwright 真实页面检查 `http://localhost:8081`
- GREEN：frontend feature evidence validator。
- GREEN：task-closeout-cleanup 预览。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。后端失败时显示错误并保留弹窗，不伪造保存成功。
- `是否从根因和长期维护角度解决`：是。新增明确 API 类型和复用 DCC 类别/目录树数据，不用 CSS 或测试专用内容绕过权限。
- `是否存在临时补丁或绕过`：否。入口严格按 `doc_control` 角色判断，不把 `super_admin` 前端视作文控。

## 当前状态

completed

## 当前证据

- RED：`node scripts/dcc-controlled-file-metadata-edit.test.mjs` -> FAIL，原因符合预期：缺少前端 metadata API 契约、`doc_control` 专用入口、共享编辑弹窗和类别绑定目录选择。
- GREEN：`node scripts/dcc-controlled-file-metadata-edit.test.mjs` -> PASS，4 tests。
- GREEN：`pnpm ts:check` -> PASS。
- GREEN：Playwright 负向角色门禁，`测试租户/aoteman` 角色为 `tenant_admin`、`showroom_publicity`，浏览页“修改基础信息”入口数量为 0。
- BLOCKED：正向 Playwright 前置条件探测 `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-metadata-positive run-code --filename doc/tasks/20260604-dcc-controlled-file-metadata-edit/dcc-controlled-file-metadata-positive.mjs` -> BLOCKED，正式角色精简接口未返回启用角色 code `doc_control`，脚本未执行赋权、未保存文件基础信息。
- BLOCKED：复制 `芋道源码` 文控角色模板探测 `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-metadata-copy-role run-code --filename doc/tasks/20260604-dcc-controlled-file-metadata-edit/dcc-controlled-file-metadata-copy-role-positive.mjs` -> BLOCKED，`芋道源码` 租户角色分页接口未找到 `code=doc_control` 模板角色，脚本在写入测试租户前停止。
- INFO：按受保护租户边界，未修改 `芋道源码` 租户；仅在本机测试租户创建 `文控/code=doc_control` 角色。
- GREEN：正向 Playwright `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-metadata-create-test-role run-code --filename doc/tasks/20260604-dcc-controlled-file-metadata-edit/dcc-controlled-file-metadata-create-test-role-positive.mjs` -> PASS，角色 `910217/doc_control` 生效，浏览页入口数量 `10`，详情页入口可见，保存并刷新后恢复文件字段与测试用户原角色。
- GREEN：测试租户恢复与编码复核 -> PASS，角色 `910217/doc_control` 名称为 `文控` 且 UTF-8 hex 为 `E69687E68EA7`；`测试租户/aoteman` 角色恢复为 `111,910209`；测试文件 `2054545668044046254` 的 `product_name` 恢复为空。
- GREEN：frontend-feature evidence validator -> PASS。
- GREEN：`git diff --check` -> PASS，仅 CRLF normalization warnings。
- GREEN：task-closeout-cleanup preview -> PASS，无 blocked cleanup paths；本轮仅执行预览并保留任务记录。

## 阻塞

- 无未解决阻塞。`芋道源码` 租户角色写入请求因受保护租户边界未执行；正向可写验证已在本机测试租户完成。
