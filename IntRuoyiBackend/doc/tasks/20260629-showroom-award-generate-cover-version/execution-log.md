# 执行日志：奖项行内生图并自动发布新版本

BDD: 奖项生图接口成功后自动发布新版本 -> Given 奖项当前封面可读且当前版本中英文语音完整 / When 调用 /showroom/award/generate-cover-image / Then 系统生成新封面、创建新修订版、克隆可复用语音并发布为当前版本。

BDD: 奖项缺少当前封面时生图失败 -> Given 奖项当前封面为空或不可读 / When 调用 /showroom/award/generate-cover-image / Then 接口显式失败且不产生新发布版本。

BDD: 奖项当前版本缺少可发布语音时生图失败 -> Given 奖项当前版本缺少中英文已发布语音 / When 调用 /showroom/award/generate-cover-image / Then 接口显式失败且不发布半成品修订版。

RED: mvn --% -pl yudao-module-showroom -Dtest=ShowroomAwardGenerateCoverIntegrationTest,ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL, 新增奖项生图链路后出现构造器兼容问题、测试约束不符和 mock 命中不稳定。

GREEN: mvn --% -pl yudao-module-showroom -Dtest=ShowroomAwardGenerateCoverIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS

GREEN: mvn --% -pl yudao-module-showroom -Dtest=ShowroomAwardGenerateCoverIntegrationTest,ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS

BDD: 本地 Codex CLI 生图必须在超时内返回或显式失败 -> Given 奖项生图调用本地 Codex CLI / When 子进程持续输出但未在超时内结束 / Then 后端必须按 timeout fail fast，而不是让页面只表现为转圈结束但图片不变。

RED: 真实接口与子进程观察 -> FAIL，Codex CLI 已被拉起但输出路径文件为空；当前实现先同步读 stdout 再 waitFor(timeout)，导致 timeout 门禁失效。

GREEN: mvn --% -pl yudao-module-showroom -Dtest=ShowroomNativeImageGenerationServiceTest,ShowroomAwardGenerateCoverIntegrationTest,ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS

GREEN: experience-preflight -> PASS

RED: 真实页面 AWARD-003 生图 -> FAIL，后端日志显示 `SHOWROOM_AWARD_COVER_GENERATION_FAILED: local codex cli timed out after 240000 ms`，但同轮 `codex-cli-cover-path-*.txt` 已写出 `C:\Users\BJB110\AppData\Local\Temp\award-cover-enhanced-AWARD-003-20260629.png`，说明当前实现仍在等待子进程自然退出而没有在 PNG 落盘后立即收口。

GREEN: mvn --% -pl yudao-module-showroom -Dtest=ShowroomNativeImageGenerationServiceTest,ShowroomAwardGenerateCoverIntegrationTest,ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS, 新增“PNG 已就绪但进程继续挂起时立即返回”回归用例通过。

BDD: 奖项生图子进程必须继承正式图片 API 配置 -> Given showroom 奖项生图通过本地 Codex CLI 拉起图片脚本 / When 后端显式配置 `yudao.ai.codex-cli.open-ai-api-key` 与 `open-ai-base-url` / Then 子进程必须收到对应 `OPENAI_API_KEY` 与 `OPENAI_BASE_URL`，不能继续隐式吃宿主机随机环境变量。

RED: mvn --% -pl yudao-module-showroom,yudao-module-ai -Dtest=ShowroomNativeImageGenerationServiceTest,ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL，新增环境传递回归前不存在正式配置入口，且测试初稿错误假设当前进程环境中没有 `OPENAI_API_KEY`。

GREEN: mvn --% -pl yudao-module-showroom,yudao-module-ai -Dtest=ShowroomNativeImageGenerationServiceTest,ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS，后端已显式支持把 `yudao.ai.codex-cli.open-ai-api-key/open-ai-base-url` 注入奖项生图子进程，5+11 项定向测试通过。

Bdd: 奖项生图必须严格走 `$generate-ai-scene-image` 而不能偏离到任何 fallback -> Given 用户明确要求生图按钮只允许使用 `$generate-ai-scene-image` 技能 / When Codex CLI 运行期尝试落到 `imagegen`、`scripts/image_gen.py` 或本地非生成式增强 / Then 后端必须显式失败并阻止把 fallback 产物发布为新版本。

GREEN: mvn --% -pl yudao-module-showroom,yudao-module-ai -Dtest=ShowroomNativeImageGenerationServiceTest,ShowroomAwardGenerateCoverIntegrationTest,ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS，已新增“命中 fallback marker 直接失败”与“prompt 强制 `$generate-ai-scene-image`”回归覆盖，当前代码层严格禁止偏离指定技能链路。

GREEN: experience-preflight -> PASS, 真实登录已进入目标页: baseUrl=http://localhost:8081, tenant=测试租户, username=aoteman, targetPath=/showroom/product

RED: 真实页面 AWARD-003 生图（2026-06-29 16:09:14） -> FAIL，真实点击行内 `生图` 后前端 toast 为 `系统异常`，`/admin-api/showroom/award/generate-cover-image` 返回 `code=500`，奖项行内容未变化。

BLOCKER: AWARD-003 真实页面生图运行时能力缺失 -> 同轮后端日志记录 `SHOWROOM_AWARD_COVER_GENERATION_FAILED: codex cli used forbidden fallback path: image_gen.py`；对应 stdout `C:\Users\BJB110\AppData\Local\Temp\codex-cli-cover-stdout-5926864419531485456.log` 显示 Codex CLI 已按提示优先执行 `$generate-ai-scene-image`，但明确返回 `FAIL: built-in image generation tool unavailable in this session.`。由于本任务禁止 fallback，后端按设计拒绝任何后续 fallback 产物，故无法把 AWARD-003 成功发布。

BLOCKER: AWARD-003 真实页面生图外部 provider -> 当前已验证三套正式图片 API 凭据/地址组合均未可用：`https://api.gptsapi.net/v1` 返回 `401 Token is invalid`；`https://api.asxs.top/v1` + `auth.json` 返回 `503 全部渠道不可提供当前模型，请稍后重试`；`https://api.asxs.top/v1` + `auth_cheap.json` 返回 `401 invalid_api_key`。在可用图片 provider 恢复前，真实按钮链路仍会 fail fast，无法把 AWARD-003 成功发布。

BDD: 奖项生图应复用展厅产品生图的正式方式 -> Given 用户要求奖项生图改为展厅产品生图同一方式 / When 奖项封面生成执行正式 prompt 生图 / Then 奖项链路仍需校验当前封面可读，但不得再把本地源图作为额外输入路径传给子进程。

RED: mvn --% -pl yudao-module-showroom,yudao-module-ai -Dtest=ShowroomAwardGenerateCoverIntegrationTest,ShowroomNativeImageGenerationServiceTest,ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL，`ShowroomAwardGenerateCoverIntegrationTest` 缺少 `Path` import，说明奖项回归仍未完成。

GREEN: mvn --% -pl yudao-module-showroom,yudao-module-ai -Dtest=ShowroomAwardGenerateCoverIntegrationTest,ShowroomNativeImageGenerationServiceTest,ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS，奖项封面服务已切换为与产品封面相同的正式 prompt 生图方式，仅保留当前封面可读门禁，不再把本地源图传入子进程。
