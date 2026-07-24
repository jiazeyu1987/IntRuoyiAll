# 任务：展厅公司生成语音真实 E2E 验证

## Goal

使用真实测试租户、真实前端页面 `http://localhost:8081/showroom/company`、真实后端接口和当前 live 公司数据，对 `AI生成介绍 -> 生成语音` 链路做一次完整 E2E 验证，确认本轮修复后不再出现 `SHOWROOM_AUDIO_GENERATION_FAILED: aliyun_nls_tts_failed status=414`。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-narration-audio-e2e\**`
- Playwright 真实浏览器链路
- 必要时的本机前后端运行状态检查
- 如验证表明当前运行时未加载最新修复，可包含最小运行时重启步骤与复验记录

## Non-Scope

- 不修改前端业务代码、页面文案或新增测试控件。
- 不使用 mock 数据、接口桩或绕过真实登录链路。
- 不顺带推进在途的英文介绍可编辑改造任务。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-editable-english-narration\task.md`
- Status before this task: `Blocked on 2026-05-21`
- Impact: 上一同仓任务已按用户优先级显式阻塞；本次仅验证当前真实链路，不混入其前端改造代码。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在其他在途改动与未提交任务目录。
- Impact: 本任务只新增验证文档与一次性 E2E 脚本，不覆盖无关前端改动。

## Milestones

- [x] M1: 处理上一同仓任务状态并创建本次 E2E 验证任务文档。
- [x] M2: 编写真实 Playwright 回放脚本，锁定登录、打开公司编辑弹框、生成介绍、生成语音与结果采集。
- [x] M3: 运行真实 E2E；若失败，记录准确阻塞点并在必要范围内完成最小运行时处理后复验。
- [x] M4: 回写执行日志、运行 closeout preview，并按验证结果提交本任务文档。

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-narration-audio-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-narration-audio-e2e\scripts\verify-showroom-company-narration-audio.mjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-narration-audio-e2e --mode preview`

## Current Status

Completed on 2026-05-21.

已确认：

- `npx.cmd` 可用，可执行 Playwright CLI。
- 真实前端入口仍固定为 `http://localhost:8081`。
- 当前验证目标是公司页真实 `生成语音` 链路，不是源码级前端回归。
- 已使用真实测试租户 `测试租户 / aoteman / admin123` 成功登录并进入 `公司信息`。
- 已在真实页面依次触发 `AI生成介绍` 与 `生成语音`；脚本内断言确认音频生成接口响应 `code=0`，并返回中英文 `audioFileId`。
- 成功页复核显示弹框中中英文介绍均已回填，且当前页共渲染 `4` 个 `audio` 元素，说明只读区与草稿区都已挂载双语音频。

## Blockers And Impact

- Blocker: none.
- Impact: 当前本地运行时下，`展厅公司 -> 生成语音` 真实链路已通过复验，未再出现 `SHOWROOM_AUDIO_GENERATION_FAILED: aliyun_nls_tts_failed status=414`。

## Final Verification Result

- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-narration-audio-e2e open http://127.0.0.1:8081/login?redirect=%2Fshowroom%2Fcompany`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-narration-audio-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-narration-audio-e2e\scripts\verify-showroom-company-narration-audio.mjs`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-narration-audio-e2e snapshot`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-narration-audio-e2e eval \"() => Array.from(document.querySelectorAll('textarea')).map((el, index) => ({ index, length: el.value.length, disabled: el.disabled }))\"`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-narration-audio-e2e eval \"() => ({ audioElementCount: document.querySelectorAll('audio').length, buttonTexts: Array.from(document.querySelectorAll('button')).map((el) => el.innerText.trim()).filter(Boolean) })\"`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-narration-audio-e2e --mode preview`
