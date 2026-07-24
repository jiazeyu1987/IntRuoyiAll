# Execution Log: 展厅产品 Codex CLI 讲解稿与双语语音生成

BDD: 产品详情一键生成中文讲解稿 -> Given 产品当前 revision 已存在且基础资料可读取 / When 用户在产品详情点击 `生成讲解稿` / Then 后端必须使用当前配置的 Codex CLI 基于真实产品资料生成并保存中文讲解稿草稿，不得伪造默认文案。

BDD: 生成语音时按当前中文稿生成双语音频 -> Given 产品已有当前中文讲解稿 / When 用户点击 `生成语音` / Then 后端必须先基于当前中文讲解稿生成中文语音，再通过当前配置的 Codex CLI 翻译出英文讲解稿并生成英文语音，两者都必须写入真实讲解版本。

BDD: 缺少中文讲解稿时显式失败 -> Given 产品当前没有可用的中文讲解稿 / When 用户点击 `生成语音` / Then 后端必须报出缺少中文讲解稿，不得 fallback 到旧英文稿、空稿或占位稿。

INFO: 2026-05-20 已将上一条同仓库任务 `20260520-showroom-cover-image-live-schema-regression` 标记为 blocked；其未修复的 live schema 漂移仍可能阻塞真实页面联调，但不影响当前 RED/GREEN 契约开发。

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，产品级 Codex CLI 讲解稿生成服务与脚本生成接口尚未存在。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-product-codex-bilingual-narration\backend-api-evidence.md` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-showroom-product-codex-bilingual-narration --mode preview` -> PASS，preview 状态 `ready`。

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，`yudao-server.jar` 已重建为包含新接口的最新 jar。

GREEN: `cmd /c D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS，后端运行态已重启到最新 jar。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-codex-narration run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-codex-bilingual-narration\scripts\verify-product-narration-dialog.mjs` -> PASS，真实浏览器路径在后端新接口生效后完成产品级讲解稿与双语语音生成。
