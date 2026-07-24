# 执行日志：展厅封面 503 错误信息收敛（后端）

BDD: 上游错误文本不应暴露 `Illegal char` 噪音 -> Given Codex CLI 输出文件里不是绝对路径而是上游错误文本 When 一键封面读取最后一条消息 Then 后端必须直接暴露真实错误文本，不再抛出 `Illegal char <:> at index ...`
RED: 用户现场错误 -> FAIL，当前一键封面失败提示前缀包含 `Illegal char <:> at index ...`，说明错误文本被误当作路径解析
GREEN: `mvn -pl yudao-module-showroom -am clean "-Dtest=ShowroomProductCoverImageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，新增回归已证明上游错误文本会直接暴露为 `SHOWROOM_COVER_GENERATION_FAILED: <真实错误>`
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-cover-503-error-message-hardening --mode preview` -> PASS
