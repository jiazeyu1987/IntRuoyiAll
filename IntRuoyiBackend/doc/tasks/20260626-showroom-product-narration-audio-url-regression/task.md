# 任务：展厅产品语音弹框历史语音 audioUrl 回归修复

## 任务目标

- 修复展厅产品语音弹框打开历史已生成语音时，前端收到 `audioFileId` 却缺少真实 `audioUrl`，从而报出“讲解音频缺少真实 audioUrl”的回归问题。
- 保持“语音按钮先打开弹框、点击生成才真实生成”的新交互不变，只修复管理端读取已有语音的接口契约。
- 确保历史已存在的产品中英文语音在弹框中可直接播放，不要求用户重新生成。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-role-management-split-rename-navigation\task.md`
- 原状态：`进行中`
- 处理：已先显式更新为 `BLOCKED`，阻塞原因为当前线程切换到高优先级展厅语音回归修复，避免混入跨任务提交。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
- 适用强制门禁：
  - 本轮先做本机代码、数据库只读核查和定向后端回归测试，不做服务器写入、不做真实生成。
  - 若后续需要真实登录或 Playwright 验证，必须先运行官方 `login-preflight.mjs`，并在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS` 后再执行长链路操作。
  - 不允许用前端静默兜底掩盖后端漏字段；必须从接口契约根因修复 `audioUrl` 缺失。
  - 若要重启本机 `48081` 后端并做真实页面复核，必须先记录 `experience-preflight`，并核实当前运行实例、前端代理目标、重启脚本与受保护本机展厅文件配置门禁。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。不会把“有 audioFileId 但没 audioUrl”默默当成可接受状态，更不会前端伪造 URL。
- `是否从根因和长期维护角度解决`：是。直接修复管理端 `getNarration` 契约，使其与已有带音频 URL 的后台 VO 保持一致。
- `是否存在临时补丁或绕过`：否。不会要求用户重新生成历史语音，也不会把旧页面的宽松消费方式当成正式方案。

## BDD 场景

- `BDD: 历史产品语音读取必须返回可播放链接 -> Given 产品讲解版本已存在真实 audioFileId 且 infra_file 中仍存在该文件 / When 管理端调用 /showroom/narration/get 读取 PRODUCT + PUBLIC + ZH/EN / Then 返回结果必须同时包含真实 audioUrl，前端可直接播放历史语音。`
- `BDD: 没有音频文件时仍只返回未生成状态 -> Given 讲解版本存在但 audioFileId 为空 / When 管理端调用 /showroom/narration/get / Then 接口不得伪造 audioUrl，而是明确返回空音频状态。`
- `BDD: 历史语音可播放不依赖重新生成 -> Given 旧产品的中英文语音文件仍保存在 infra_file / When 用户只打开产品语音弹框不点击生成 / Then 系统直接展示已有语音，不要求再次调用生成接口。`

## 里程碑

1. M1：创建任务文档、阻塞上一任务、更新请求日志并记录根因线索。
2. M2：补后端 RED 回归测试，锁定 `getNarration` 必须返回真实 `audioUrl`。
3. M3：最小实现管理端 narration 响应 VO，修复历史语音可播放回归。
4. M4：运行定向 GREEN 验证并补齐 bug regression evidence。

## 预期验证

- `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-showroom-product-narration-audio-url-regression\bug-regression-evidence.md`

## 当前结论

- 本机数据库已确认 `infra_file.id IN (9198110021399, 9198110021400)` 真实存在，且 URL 分别为：
  - `http://127.0.0.1:9000/yudao/showroom/narration/20260527/product-1-zh-ruoxi.wav`
  - `http://127.0.0.1:9000/yudao/showroom/narration/20260527/product-1-en-ruoxi.wav`
- 当前根因收敛为：管理端 `GET /admin-api/showroom/narration/get` 直接返回 `ShowroomNarrationVersion` 领域对象；该对象没有 `audioUrl` 字段，但前端弹框按带 `audioUrl` 契约消费，导致历史语音被误判为不可播放。
- 用户继续看到旧报错的运行时原因也已确认：前端 `http://localhost:8081` 代理到本机 `48081`，但当前 `48081` 仍加载旧 jar `E:\Int\CacheData\IntRuoyi\runtime\backend-20260626-134808.jar`；其启动时间 `2026-06-26 13:48:20` 早于修复源码写入时间 `2026-06-26 14:29:39 / 14:31:26`，所以修复尚未进入实际运行实例。

## 最终验证结果

- `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，75 tests passed。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-showroom-product-narration-audio-url-regression\bug-regression-evidence.md` -> PASS。
- `powershell -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` -> PASS，本机 `48081` 已加载新构建 jar `D:\ProjectPackage\Int\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260626-154750.jar`。
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /showroom/product --target-text 产品` -> PASS。
- `Playwright 真实页面复核 /showroom/product -> 点击首行产品“语音”` -> PASS，弹框内中英文 narration 响应均返回真实 `audioUrl`，页面不再报“缺少真实 audioUrl”。

## 完成说明

- 管理端 `getNarration` 已改为返回显式 `NarrationVersionRespVO`，在 `audioFileId` 存在时同步返回真实 `audioUrl`。
- 历史产品语音文件不需要重生成；只要 `infra_file` 中仍存在对应文件，产品语音弹框即可直接播放。
- 本轮运行态也已收口：用户看到“还是老问题”的直接原因确实是后端没更新；重启本机 `48081` 后，真实页面已确认新接口生效。
- 当前测试租户产品页的真实可操作目标是 `productId=252 / product_001`，该目标的 ZH/EN narration 都能返回真实 `audioUrl` 并在弹框中播放；原报错里的 `targetId=1` 在当前测试租户产品页下不再是有效 narration 目标。
