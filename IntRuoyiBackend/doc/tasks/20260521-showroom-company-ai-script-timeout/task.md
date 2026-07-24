# 任务：修复展厅公司 AI生成介绍 卡死超时失效

## Goal

修复 `展厅 -> 展厅公司` 点击 `AI生成介绍` 后长时间转圈、无法及时返回结果的问题。要求后端本地 Codex CLI 调用必须真正受超时控制，超时或失败时显式报错，不得无限阻塞、静默降级或 fallback 到其他模型。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-ai\src\main\java\cn\iocoder\yudao\module\ai\framework\ai\core\model\codexcli\CodexCliChatModel.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-ai\src\test\java\cn\iocoder\yudao\module\ai\framework\ai\core\model\codexcli\CodexCliChatModelTest.java`
- 如验证证明仍需补充 showroom 侧回归，再最小补测相关模块
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-company-ai-script-timeout\**`

## Non-Scope

- 不改公司页面路由、按钮文案或权限矩阵。
- 不引入备用 AI 服务或降级分支。
- 不顺带修改产品封面、TTS 或无关模块的业务逻辑。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-yingtai-showroom-narration\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 上一同仓任务已完成，不阻塞本次后端缺陷修复。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在其他 showroom 与基础模块改动。
- Impact: 本次仅修改 Codex CLI 通用模型、对应定向测试和本任务文档，避免混入无关变更。

## Milestones

- [x] M1: 创建后端 companion 任务文档并确认上一同仓任务已完成。
- [x] M2: 记录 BDD/RED，补定向回归测试锁定“子进程未退出时 timeout 仍必须生效”。
- [x] M3: 最小修复 Codex CLI 调用超时控制。
- [x] M4: 运行定向 GREEN，并回放前端真实路径确认 `AI生成介绍` 不再挂住。
- [x] M5: 更新任务文档、执行 closeout preview，并按结果准备提交。

## Expected Verification

- `mvn --% -pl yudao-module-ai -Dtest=CodexCliChatModelTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test`
- 如需 showroom 补充验证：`http://localhost:8081/showroom/company` 真实点击 `AI生成介绍`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-company-ai-script-timeout --mode preview`

## Current Status

Completed on 2026-05-21.

已完成定向 RED/GREEN、后端重打包、本地运行时重启与真实页面回放。根因确认是 `CodexCliChatModel.executePrompt()` 在修复前先阻塞读取 stdout 到 EOF，再执行 `waitFor(timeout)`；现已改为 stdout 重定向到临时文件，并先按 timeout 等待进程，再读取日志与输出文件。真实 `/showroom/company` 页面点击 `AI生成介绍` 已恢复成功。

## Blockers And Impact

- Blocker: none.
- Impact: 当前复用 `CodexCliChatModel` 的 showroom 公司讲解生成链路已解除“超时失效导致长期卡住”的阻塞。

## Final Verification Result

- PASS: `mvn --% -pl yudao-module-ai -Dtest=CodexCliChatModelTest -Dsurefire.failIfNoSpecifiedTests=false test`
- PASS: `mvn --% -pl yudao-server -am -DskipTests package`
- PASS: `cmd /c D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-ai-script-error-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-ai-script-error\scripts\reproduce-showroom-company-ai-script-error.mjs`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-company-ai-script-timeout --mode preview`
