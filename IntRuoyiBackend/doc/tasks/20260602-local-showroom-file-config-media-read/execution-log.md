# Execution Log

## Bug

本机展厅产品管理中的产品封面图片和讲解语音直链返回 JSON 错误响应，浏览器无法读取媒体内容。

## Expected

同一类 `/admin-api/infra/file/28/get/showroom/**` 直链应返回真实媒体内容：图片返回 `image/*`，语音返回 `audio/*`。

## Reproduction

- `curl.exe -sS -D - -o NUL http://127.0.0.1:48081/admin-api/infra/file/28/get/showroom/product/cover/20260530/product-product_163-imported-cover.png`
- `curl.exe -sS -D - -o NUL http://127.0.0.1:48081/admin-api/infra/file/28/get/showroom/narration/20260527/product-252-zh-ruoxi.wav`

## Root Cause

本机 MinIO 源桶 `yudao` 中存在 showroom 媒体对象，但后端运行时实际请求 `yudao-dcc-e2e/showroom/**`。目标桶缺少同名对象，MinIO 对后端请求返回 S3 404，后端最终输出 JSON 错误响应。

## BDD

BDD: 本机产品封面地址必须返回图片 -> Given 本机产品封面通过 `/admin-api/infra/file/28/get/**` 读取；When 浏览器或 curl 请求该地址；Then 响应应为真实图片内容，不能返回 JSON 错误。

BDD: 本机产品讲解语音地址必须返回音频 -> Given 本机产品讲解语音通过 `/admin-api/infra/file/28/get/**` 读取；When 浏览器或 curl 请求该地址；Then 响应应为真实音频内容，不能返回 JSON 错误。

BDD: 本机文件配置必须匹配后端运行位置 -> Given 后端在本机运行环境中读取 config 28 的 S3/MinIO 配置；When 后端访问 MinIO endpoint；Then endpoint 必须从后端运行位置可达，不得指向错误的本地环回地址。

BDD: 本机后端实际请求桶必须存在 showroom 媒体对象 -> Given 后端实际通过 MinIO 请求 `/yudao-dcc-e2e/showroom/**`；When 浏览器或 curl 请求产品图片和语音直链；Then 目标桶中必须存在对应对象，且返回媒体内容。

## TDD Evidence

- STATUS: task-created -> 已建立本机媒体读取修复任务，下一步复现本机 RED。
- RED: `curl.exe -sS -D - -o NUL http://127.0.0.1:48081/admin-api/infra/file/28/get/showroom/product/cover/20260530/product-product_163-imported-cover.png` -> FAIL, expected image/png but got `Content-Type: application/json`.
- RED: `curl.exe -sS -D - -o NUL http://127.0.0.1:48081/admin-api/infra/file/28/get/showroom/narration/20260527/product-252-zh-ruoxi.wav` -> FAIL, expected audio/* but got `Content-Type: application/json`.
- RED: `mc admin trace --json --errors local` while requesting the image URL -> FAIL, backend requested `/yudao-dcc-e2e/showroom/product/cover/20260530/product-product_163-imported-cover.png` and MinIO returned 404.
- DIAG: `mc stat local/yudao/showroom/product/cover/20260530/product-product_163-imported-cover.png` -> PASS, source object exists with `Content-Type: image/png`.
- DIAG: `mc stat local/yudao/showroom/narration/20260527/product-252-zh-ruoxi.wav` -> PASS, source object exists with `Content-Type: audio/wav`.
- FIX: `mc cp --recursive local/yudao/showroom/ local/yudao-dcc-e2e/showroom/` -> PASS, synced 1.25 GiB of local showroom media into the bucket actually requested by the running backend.
- GREEN: `curl.exe -sS -D - -o NUL http://127.0.0.1:48081/admin-api/infra/file/28/get/showroom/product/cover/20260530/product-product_163-imported-cover.png` -> PASS, got `Content-Type: image/png` and `Content-Length: 4181`.
- GREEN: `curl.exe -sS -D - -o NUL http://127.0.0.1:48081/admin-api/infra/file/28/get/showroom/narration/20260527/product-252-zh-ruoxi.wav` -> PASS, got `Content-Type: audio/vnd.wave`.
- GREEN: `curl.exe -sS http://127.0.0.1:48081/actuator/health` -> PASS, got `{"status":"UP"}`.
- REGRESSION: `mc stat local/yudao-dcc-e2e/showroom/product/cover/20260530/product-product_163-imported-cover.png` -> PASS, target object exists with `Content-Type: image/png`.
- REGRESSION: `mc stat local/yudao-dcc-e2e/showroom/narration/20260527/product-252-zh-ruoxi.wav` -> PASS, target object exists with `Content-Type: audio/wav`.
- E2E: `node -e "<Playwright Chromium opens image URL and listens for audio download>"` from `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` -> PASS, image got `200 image/png`, audio triggered download `showroom_narration_20260527_product-252-zh-ruoxi.wav`.

## Verification

- 图片直链验证通过：`Content-Type: image/png`，`Content-Length: 4181`。
- 语音直链验证通过：`Content-Type: audio/vnd.wave`。
- 后端健康检查通过：`{"status":"UP"}`。
- MinIO 目标桶对象验证通过：目标图片和语音对象均存在，保留正确媒体元数据。
- Playwright Chromium 浏览器验证通过：图片直链可打开，语音直链可触发下载。

## Blockers

- 无。
