# 任务：DCC 目录行操作按钮文案调整

## 任务目标

- 删除 DCC 目录管理行内“访问规则”按钮。
- 将行内“新建子目录 / 编辑 / 删除父文件夹”显示文案调整为“新建 / 编辑 / 删除”。
- 保留既有新建子目录、编辑目录和删除父文件夹行为，不修改后端接口、不新增 fallback。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-edhr-template-layout-missing-regression\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成；当前工作区存在多项未归属脏改，本次仅修改 DCC 目录行操作按钮相关文件和任务记录。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 前端页面保持 IntPP 运维台风格：白底、轻边框、紧凑控制，不做营销式重构。
  - 本轮仅做本机源码和静态验证；如进入真实 Playwright 登录写入验证，必须先运行登录预检并记录 `GREEN: experience-preflight -> PASS`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本次只调整行内操作入口和文案。
- `是否从根因和长期维护角度解决`：是。访问规则已并入文控权限页签，目录行内不再保留旧入口。
- `是否存在临时补丁或绕过`：否。不通过隐藏异常、mock 数据或接口绕过实现。

## BDD 场景

- `BDD: 目录行操作不再显示访问规则 -> Given 管理员打开 DCC 目录管理页 / When 查看目录行操作列 / Then 不再显示“访问规则”按钮。`
- `BDD: 目录行操作显示短文案 -> Given 管理员打开 DCC 目录管理页 / When 查看目录行操作列 / Then 保留操作显示为“新建 / 编辑 / 删除”。`
- `BDD: 目录行操作行为保持不变 -> Given 管理员点击行内“新建 / 编辑 / 删除” / When 触发按钮 / Then 仍分别打开新建子目录、编辑目录和删除父文件夹确认流程。`

## 里程碑

1. M1：创建任务文档和 RED 静态回归。`COMPLETED`
2. M2：最小修改目录行操作按钮。`COMPLETED`
3. M3：运行目标验证并更新证据。`COMPLETED`
4. M4：按验证结果收尾。`COMPLETED`

## 预期验证

- `node tests/e2e/dcc-directory-row-action-labels-static.spec.js`

## 最终验证结果

- `node tests/e2e/dcc-directory-row-action-labels-static.spec.js` -> PASS。
- 结论：目录行操作列不再显示“访问规则”，保留操作显示为“新建 / 编辑 / 删除”，对应行为入口保持原流程。
