# Task: 发布本地展厅公司现行内容

## Goal

直接在本地展厅运行数据中创建并发布一版真实公司内容，补齐数字展厅前台“主页 / 公司”页签依赖的 live company revision 前置条件。

## Scope

- 先确认上一后端任务状态并创建本任务记录。
- 使用当前本地后端真实能力创建展厅公司草稿。
- 通过正式的展厅提交、主管审批和高新审批发布链路将公司内容发布为现行版本。
- 回查 `/showroom/display/home` 与 `/showroom/display/company` 的真实响应，确认不再因缺少 live company revision 报错。
- 不引入 fallback、mock 或直接伪造前台成功。

## Non-Scope

- 不顺带重构展厅后端代码。
- 不批量发布产品、展厅或讲解内容，除非完成公司 live 前置必须显式依赖它们。
- 不提交或回滚与本任务无关的 DCC、MES、AI 变更。

## Previous Task Check

- Previous backend task: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-reset-all-non-admin-user-passwords\task.md`
- Status before this task: completed.
- Impact: no unfinished backend task blocks this showroom live-data publish work.

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Unrelated dirty files already exist in DCC paths, temporary artifact folders, and prior evidence files.
- Impact: this task must avoid touching or staging unrelated backend modifications.

## Milestones

- [ ] M1: 创建任务记录并确认当前 live company revision 缺失的真实失败链路。
- [ ] M2: 记录 RED 证据，证明前台公开 display 接口因缺少 live company revision 失败。
- [ ] M3: 创建并发布一版本地展厅公司现行内容。
- [ ] M4: 运行 GREEN 验证，确认主页 / 公司公开 display 接口恢复。
- [ ] M5: 更新任务记录、执行收尾预览并提交本任务相关改动。

## Expected Verification

- `node --test D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-frontstage-runtime.test.mjs`
- Authenticated `GET /showroom/display/home`
- Authenticated `GET /showroom/display/company`
- 如前端运行正常，补充真实前端路径 `http://localhost:8081/showroom/home` / `http://localhost:8081/showroom/company-intro` 验证

## Current Status

Completed. 已在本地展厅运行数据中补齐主公司现行内容，并通过真实后端接口发布公司讲解；前台主页 / 公司页签依赖的公开 display 接口已恢复。

## Final Verification Result

- PASS: `node --test D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-frontstage-runtime.test.mjs`
- PASS: authenticated `GET /showroom/display/home`
- PASS: authenticated `GET /showroom/display/company`
- PASS: authenticated `GET /showroom/display/narration?targetType=COMPANY&targetId=1&audienceType=PUBLIC&language=ZH`
- PASS: Playwright CLI real-browser smoke on `http://127.0.0.1:8081/showroom/home` and `http://127.0.0.1:8081/showroom/company-intro` showed `errorCount = 0`

## Residual Risk

- `showroom_company` / `showroom_company_revision` 的现行正文已持久化到 MySQL。
- The former narration-memory residual risk is now resolved by follow-up task `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-narration-live-persistence\`; company narration live data survives backend restarts.
