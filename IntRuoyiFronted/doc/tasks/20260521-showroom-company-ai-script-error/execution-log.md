# Execution Log

BDD: 展厅公司点击 AI生成介绍 使用当前公司真实版本 -> Given 用户进入 `http://localhost:8081/showroom/company` 并打开“编辑公司信息”弹框 / When 用户点击 `AI生成介绍` / Then 前端必须基于当前公司真实可生成版本调用介绍生成接口，成功回填中英文介绍；若缺少前置条件，必须显式暴露准确原因，不得无原因报错或静默降级。

BDD: 展厅公司存在可编辑草稿时仍能生成介绍 -> Given 页面已加载当前公司信息与编辑表单 / When 用户尚未点击“保存”但当前公司已有可用于生成介绍的真实版本 / Then `AI生成介绍` 不应仅因弹框内是草稿态就阻断，必须取真实可用版本或显式提示缺少哪个真实版本前置条件。

RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-ai-script-error-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-ai-script-error\scripts\reproduce-showroom-company-ai-script-error.mjs` -> FAIL，真实页面点击 `AI生成介绍` 后生成请求在 30 秒窗口内拿不到响应，按钮持续 loading；同轮抓到 `/admin-api/showroom/company/current` 返回 `revisionId=5 / status=PUBLISHED / companyType=MAIN`，证明前置数据齐全，阻塞发生在后端 AI 调用链路。

INFO: 根因收敛到 companion backend 任务 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-company-ai-script-timeout\`：`CodexCliChatModel.executePrompt()` 先阻塞读 stdout 到 EOF，再执行 `waitFor(timeout)`，导致本地 Codex CLI 未退出时超时控制失效。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-ai-script-error-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-ai-script-error\scripts\reproduce-showroom-company-ai-script-error.mjs` -> PASS，重打后端 jar 并重启本地 `48081/8081` 后，真实页面点击 `AI生成介绍` 返回 `code=0`，toast 显示“中英文介绍已生成，可继续微调后再生成语音”，中文介绍文本框已回填生成内容。
