# Verification Report

## Result

`BLOCKED`

已使用用户确认的正确 Word 完成真实页面导入和候选升版验证。文件可被正式解析器识别，Word 导入缺少统一受控内容生命周期登记的问题已经按 BDD/TDD 修复并通过 24 条定向回归；修复后真实页面导入也已正常完成。当前阻塞是重启后的 `int_main` 前端依赖目录不完整，且另一个非本任务依赖安装长期占用全局包仓库，暂时无法恢复 `8081` 继续发布验证。

## Correct Input

- 文件：`C:\Users\BJB110\Desktop\文档\1\RE-PP-IDPR-01（A 1） 按压式球囊扩张压力泵生产记录--2026.02.02生效(1)\RE-PP-IDPR-01（A 1） 按压式球囊扩张压力泵生产记录--2026.02.02生效.doc`
- 大小：`937108` 字节。
- 真实页面身份：`芋道源码/admin`。
- DCC 项目：`IDPR / 按压式球囊扩张压力泵`。

## Passed Business Evidence

- 首次导入成功：生成路线 `RT000036 / 980143`、生效 V1 `681`、批记录 V1.0 `137`、16 份批记录表单、15 道工序和 15 条逐工序正式批记录绑定。
- 再次升版成功：生成批记录 V2.0 `138` 和路线草稿候选 V2 `684`；发布前路线仍指向 V1 `681`，没有直接覆盖生效路线。
- V1/V2 流程一致：15 个工序节点、14 条工序间关系、2 条开始/结束边界。
- V2 保留 15 条逐工序正式批记录绑定和 4 条工序开始附件配置；表单槽位为 0 条，工序开始生产负责人为 0 条，工序结束业务绑定为 0 条。
- 发布失败后的只读复核再次通过：V1 仍为 `ACTIVE`、V2 仍为 `DRAFT`，正式路线和既有绑定没有被错误修改。

## Blocking Defect

- 真实页面动作：工艺路线版本工作区中对 V2 点击“提交发布”。
- 页面请求：POST `/admin-api/mes/pro/route-version/submit-publish?id=684`。
- 结果：后端返回“系统异常”，没有发布成功。
- 精确异常：`controlled content active ref does not exist for route: 980143`。
- 根因：Word 导入新建路线时直接创建了路线和生效 V1，却没有登记统一受控内容的生效引用；随后直接创建 V2 草稿候选，也没有对应候选引用。发布提交尝试补登记 V2 时必须先找到 V1 生效引用，因此失败并回滚。

## Remaining Coverage

- M2“不勾选工艺流程”尚未使用本次 Word 完成真实页面负向导入。
- 本次 Word 两次导入工序集合相同，未覆盖 Word 新增工序后的正式身份建立。
- 当前路线没有非空表单槽位，只证明 `formBindings` 从空保持为空，不能证明非空表单槽位复制。
- V2 尚未发布，无法验证发布后的正式投影结果。
- 路线 `RT000036`、V1/V2 和批记录版本保留用于修复后复测，未执行清理。

## Evidence Files

- `output/playwright/batch-record-word-route-post-merge/bootstrap.json`
- `output/playwright/batch-record-word-route-post-merge/upgrade.json`
- `output/playwright/batch-record-word-route-post-merge/publish.json`
- `output/playwright/batch-record-word-route-post-merge/upgrade-candidate-created.png`

## Required Next Step

先等待或经用户明确授权结束长期占用全局包仓库的外部 `pnpm install`，再按锁文件恢复 `IntRuoyiFronted/node_modules` 并启动 `8081`。随后继续当前 V2 发布、发布后绑定核验、未勾选工艺流程负向路径和任务数据清理；不得使用替代依赖目录、其它工作树前端或 API 写入代替真实页面。
