# Verification Report

## Result

PASS，工艺路线列表已统一为“芋道源码 / admin”默认列布局，并在 Chrome 与 Edge 真实登录路径中显示一致。

## Behavior

- 新列表配置 key：`mes.pro.route.main.admin-layout-v1`。
- 默认显示：路线编码、路线名称、状态、当前生效版本、待发布版本、关联产品、创建时间、操作。
- 默认隐藏：负责人、关键工序、关系图。
- “显示字段”继续可用并按新 key 自动保存。
- 导入、导出、产品、编辑、复制、版本、删除继续使用原权限指令，没有提升普通用户权限。

## Verification

- `node tests/e2e/mes-route-admin-list-layout-static.spec.js` -> PASS。
- `node tests/e2e/mes-route-list-edit-create-candidate-static.spec.js` -> PASS。
- `node tests/e2e/mes-pro-route-version-workspace-static.spec.js` -> PASS。
- `node tests/e2e/user-table-column-config-static.spec.js` -> PASS。
- `node --check tests/e2e/mes-route-admin-list-layout-static.spec.js` -> PASS。
- `node --check tests/e2e/mes-route-admin-list-layout-real.e2e.js` -> PASS。
- `pnpm ts:check` -> PASS。
- 官方登录前置 -> PASS，`http://127.0.0.1:8081/mes/pro/route`，身份标签 `芋道源码/admin`。
- Chrome/Edge 真实只读 E2E -> PASS；两个浏览器表头一致，新配置 key 被读取，旧 key 未读取，写请求数 0，console error 数 0。
- 前端 `8081` HTTP 200，后端 `48081` health `UP`，进程归属 `E:\IntRuoyi`。
- `git diff --check -- <task-owned-files>` -> PASS。
- `validate_frontend_feature.py` -> PASS。
- task-closeout-cleanup preview/apply -> PASS；仅清理任务期 `frontend-feature-evidence.md`。

## Safety

- 未修改后端接口、角色、菜单、租户或业务数据。
- 未执行远程环境操作。
- 未覆盖并行版本弹窗改动或调度工作台任务文档。

## Closeout

- 经验沉淀、cleanup、实现提交、远端合并和合并后复验均已完成。
- 实现提交：`c786123f`。
- 远端合并提交：`79040df4`。
- 合并后复验：目标静态合同、相邻合同、全局列配置合同、`pnpm ts:check`、Chrome/Edge 真实只读 E2E 均 PASS。
- 当前推送阻塞：`git push origin int_main`、`git ls-remote --heads origin int_main` 和延迟后的再次 push 均因 `Recv failure: Connection was reset` 失败。
- 当前任务状态：`ready_for_closeout`；实现和验证 PASS，最终 completed 等待 GitHub HTTPS 连接恢复并成功推送。
