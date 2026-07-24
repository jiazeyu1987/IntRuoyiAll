# 本机展厅产品媒体读取修复

## Task Goal

修复本机 IntRuoyi 展厅产品管理中产品封面图片和讲解语音无法读取的问题，仅修改本机环境，不操作测试服或正式服。

## Previous Task Check

- 上一个后端展厅任务 `doc/tasks/20260602-showroom-product-import-cover-current-file-missing/task.md` 已标记 `completed`。
- 当前仓库存在 DCC/infra 相关未提交改动和未跟踪任务目录，和本任务无关，本任务不接管、不回滚、不提交。

## BDD

BDD: 本机产品封面地址必须返回图片 -> Given 本机产品封面通过 `/admin-api/infra/file/28/get/**` 读取；When 浏览器或 curl 请求该地址；Then 响应应为真实图片内容，不能返回 JSON 错误。

BDD: 本机产品讲解语音地址必须返回音频 -> Given 本机产品讲解语音通过 `/admin-api/infra/file/28/get/**` 读取；When 浏览器或 curl 请求该地址；Then 响应应为真实音频内容，不能返回 JSON 错误。

BDD: 本机文件配置必须匹配后端运行位置 -> Given 后端在本机运行环境中读取 config 28 的 S3/MinIO 配置；When 后端访问 MinIO endpoint；Then endpoint 必须从后端运行位置可达，不得指向错误的本地环回地址。

BDD: 本机后端实际请求桶必须存在 showroom 媒体对象 -> Given 后端实际通过 MinIO 请求 `/yudao-dcc-e2e/showroom/**`；When 浏览器或 curl 请求产品图片和语音直链；Then 目标桶中必须存在对应对象，且返回媒体内容。

## Milestones

- [x] M1: 建立任务记录，确认上一展厅任务状态，并记录设计约束。
- [x] M2: 复现本机图片/语音读取失败并记录 RED。
- [x] M3: 定位本机后端运行位置、MinIO 可达地址和实际请求桶。
- [x] M4: 同步本机 `yudao/showroom/` 对象到后端实际请求的 `yudao-dcc-e2e/showroom/`。
- [x] M5: 验证本机图片和语音读取恢复，运行证据校验与收尾预览。

## Expected Verification

- RED: 本机 `/admin-api/infra/file/28/get/**` 图片或语音 URL 返回 JSON 错误或非目标媒体类型。
- GREEN: 修复后同一类 URL 返回 `image/*` 与 `audio/*`。
- REGRESSION: 后端进程可访问本机 MinIO，且实际请求桶中存在对应 showroom 对象。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260602-local-showroom-file-config-media-read\execution-log.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-local-showroom-file-config-media-read --mode preview`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。修复本机配置指向正式可达的 MinIO 地址，不在代码中加入兜底。
- `是否从根因和长期维护角度解决`：是。根因证据为后端实际请求 `yudao-dcc-e2e/showroom/**`，而本机 showroom 媒体对象只存在于 `yudao/showroom/**`；本机修复通过同步对象让运行时桶与数据对齐。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Completed Work

- 已建立任务文档。
- 已复现本机图片、语音 URL 返回 200 但 `Content-Type: application/json` 的错误响应。
- 已确认 `infra_file_config.id=28` 当前数据库配置为 `endpoint=http://127.0.0.1:9000`、`domain=http://127.0.0.1:9000/yudao`、`bucket=yudao`。
- 已用 MinIO `mc stat` 确认源桶 `yudao` 中目标图片和语音对象存在。
- 已用 MinIO trace 确认后端运行时实际请求 `/yudao-dcc-e2e/showroom/...`，目标桶缺少 showroom 对象导致 S3 404。
- 已将本机 `local/yudao/showroom/` 同步到 `local/yudao-dcc-e2e/showroom/`。
- 已从后端仓库根重新启动本机后端，服务恢复在 `http://127.0.0.1:48081`。

## Verification Evidence

- RED: `curl.exe -sS -D - -o NUL http://127.0.0.1:48081/admin-api/infra/file/28/get/showroom/product/cover/20260530/product-product_163-imported-cover.png` -> FAIL，响应 `Content-Type: application/json`。
- RED: `curl.exe -sS -D - -o NUL http://127.0.0.1:48081/admin-api/infra/file/28/get/showroom/narration/20260527/product-252-zh-ruoxi.wav` -> FAIL，响应 `Content-Type: application/json`。
- RED: `mc admin trace --json --errors local` + 图片 URL 请求 -> FAIL，MinIO trace 显示后端请求 `/yudao-dcc-e2e/showroom/product/cover/20260530/product-product_163-imported-cover.png` 并返回 404。
- GREEN: `mc cp --recursive local/yudao/showroom/ local/yudao-dcc-e2e/showroom/` -> PASS，传输 `1.25 GiB`。
- GREEN: `curl.exe -sS -D - -o NUL http://127.0.0.1:48081/admin-api/infra/file/28/get/showroom/product/cover/20260530/product-product_163-imported-cover.png` -> PASS，响应 `Content-Type: image/png`，`Content-Length: 4181`。
- GREEN: `curl.exe -sS -D - -o NUL http://127.0.0.1:48081/admin-api/infra/file/28/get/showroom/narration/20260527/product-252-zh-ruoxi.wav` -> PASS，响应 `Content-Type: audio/vnd.wave`。
- REGRESSION: `curl.exe -sS http://127.0.0.1:48081/actuator/health` -> PASS，响应 `{"status":"UP"}`。
- REGRESSION: `mc stat local/yudao-dcc-e2e/showroom/product/cover/20260530/product-product_163-imported-cover.png` -> PASS，`Content-Type: image/png`。
- REGRESSION: `mc stat local/yudao-dcc-e2e/showroom/narration/20260527/product-252-zh-ruoxi.wav` -> PASS，`Content-Type: audio/wav`。
- E2E: Playwright Chromium 打开图片直链 -> PASS，`status=200`，`Content-Type: image/png`；打开语音直链 -> PASS，触发下载 `showroom_narration_20260527_product-252-zh-ruoxi.wav`。

## Remaining Blockers

- 无。
