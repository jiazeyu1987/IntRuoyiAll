# Verification Report

## Scope

纠正表单模板三个按钮被错误批记录绑定校验阻断的问题。最终行为是三个按钮只操作当前 FormCenter 模板，前端、BPM 契约和未发布迁移均与批记录绑定解耦。

## Implemented Behavior

- `打开`调用 `TemplateViewDialog`。
- `编辑`调用 `openSelectedTemplateAction('edit')`。
- `填写`重置当前模板模拟值并打开 `fillDialogVisible`。
- 前端类型、BPM VO/DO 和运行态映射移除七个错误批记录绑定字段。
- 删除错误新增迁移和旧绑定合同，不执行破坏性数据库删列。

## Verification Checks

- Change request validator: PASS。
- Frontend focused static contract: PASS。
- Frontend TypeScript check: PASS。
- BPM independence contract: PASS，2 tests。
- Database independence pytest: PASS，2 tests。
- Real login preflight: PASS，身份标签 `芋道源码/admin`。
- Real Playwright E2E with installed Chrome: PASS。
- System design structure validator and self-test: PASS。
- Frontend/backend/database evidence validators and self-tests: PASS。
- Bug regression evidence validator and self-test: PASS。
- Change request validator and self-test: PASS。
- UTF-8 strict read: PASS，任务目录 11 个 Markdown 文件均可读取。
- Task-owned `git diff --check`: PASS。
- Branch runtime port guard: PASS，`int_main` frontend `8081` / backend `48081`。

## Real E2E Evidence

- Frontend: `http://127.0.0.1:8081`，PID `41928`，归属主工作区 Vite。
- Backend: `http://127.0.0.1:48081`，PID `54560`，health `UP`，归属主工作区 Jar。
- Browser: `C:\Program Files\Google\Chrome\Application\chrome.exe`。
- `打开`: PASS，显示“查看表单模板”，pathname 保持 `/mdm/form-center/template`。
- `编辑`: PASS，显示 `.form-template-rules-dialog`，pathname 保持 `/mdm/form-center/template`。
- `填写`: PASS，显示 `.form-template-fill-dialog`，pathname 保持 `/mdm/form-center/template`。
- 三个动作均未出现“当前模板未绑定批记录表单”，未跳转 MES 页面。
- 本次真实 E2E 为只读/前端模拟操作，未写数据库、未创建批记录执行数据。

## Known Unrelated Regression

- `node tests\e2e\form-center-static.spec.js` 当前仅在无关策略路由断言失败：缺少 `activeMenu: '/mdm/form-center/policy'`。
- 该失败不属于三个按钮范围，本任务未修改无关路由或断言。

## Data Safety

- 本地 schema 已存在此前误加的七个列和索引。
- 当前代码不再读取或写入这些字段。
- 未执行 `DROP COLUMN`、`DROP INDEX` 或远端数据库操作。

## Blockers

- 当前功能和目标验证无 blocker。
- 物理 schema 清理不在本任务授权范围。
- 项目经验沉淀：PASS，已合并到现有 `docs/frontend-development.md`，无需新建长期经验文档。
- Cleanup 和首次 Git 同步已完成。
- 无关未跟踪目录 `doc/tasks/20260727-route-flow-batch-record-form-source/` 保持不动，不属于本任务。

## Final Result

- Status: completed。
- Remote: `origin/int_main` 已包含实现、设计和验证证据。
- First push head: `97ecf51a`。
