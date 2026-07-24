# 执行记录：DCC 目录行操作按钮文案调整

BDD: 目录行操作不再显示访问规则 -> Given 管理员打开 DCC 目录管理页 / When 查看目录行操作列 / Then 不再显示“访问规则”按钮。
BDD: 目录行操作显示短文案 -> Given 管理员打开 DCC 目录管理页 / When 查看目录行操作列 / Then 保留操作显示为“新建 / 编辑 / 删除”。
BDD: 目录行操作行为保持不变 -> Given 管理员点击行内“新建 / 编辑 / 删除” / When 触发按钮 / Then 仍分别打开新建子目录、编辑目录和删除父文件夹确认流程。

READONLY: 已读取 `docs/experience-index.md`，命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本轮尚未执行真实登录写入 E2E、服务器操作、数据库写入或发布动作。

RED: `node tests/e2e/dcc-directory-row-action-labels-static.spec.js` -> FAIL，目录行操作列仍显示“访问规则”按钮。

GREEN: `node tests/e2e/dcc-directory-row-action-labels-static.spec.js` -> PASS。

FIX: 已删除目录行内“访问规则”入口；“新建子目录 / 编辑 / 删除父文件夹”显示为“新建 / 编辑 / 删除”，并同步 DCC 按钮真实流程用例的按钮名。
