# 任务：修复展厅产品列表封面与语音读取失败

## 任务目标

排查展厅后台产品列表“封面”列显示“加载失败”以及产品讲解语音无法播放的真实原因，并给出可验证的修复方案。测试服只做只读诊断，不修改服务器配置或业务数据。

## 前序任务检查

- 最近有效前端任务 `doc/tasks/20260601-nas-transfer-permission-snapshot-ready/task.md` 已标记 completed，不阻塞本任务。
- 当前仓库存在 NAS 权限恢复相关未提交改动与无关未跟踪任务目录；本任务不修改、不回滚、不纳入。

## BDD 场景

BDD: 产品列表封面内容地址必须返回图片 -> Given 产品列表接口返回 `cover_image=/admin-api/infra/file/28/get/showroom/product/cover/20260530/product-product_001-imported-cover.png` / When 浏览器请求该地址 / Then 响应应为 `image/png` 图片内容，而不是 JSON 错误。

BDD: 产品讲解语音地址必须返回音频 -> Given 产品讲解版本返回 `audioUrl=/admin-api/infra/file/28/get/showroom/narration/20260523/product-155-zh-ruoxi.wav` / When 浏览器请求该地址 / Then 响应应为 `audio/wav` 音频内容，而不是 JSON 错误。

BDD: 文件存储配置必须可被后端容器访问 -> Given 文件配置 28 是 S3/MinIO 存储 / When 后端通过 `/admin-api/infra/file/28/get/**` 读取对象 / Then S3 endpoint 必须是后端容器可达地址。

## 里程碑

- [x] M1：建立任务文档与 BDD 场景。
- [x] M2：只读核对测试服产品 `cover_image` 字段。
- [x] M3：只读核对测试服文件接口响应与文件配置。
- [ ] M4：在获得修改测试服配置授权后，更新文件配置并验证封面与语音恢复。
- [ ] M5：记录最终验证证据并提交本任务改动。

## 预期验证

- RED：`curl http://172.30.30.58:8081/admin-api/infra/file/28/get/showroom/product/cover/20260530/product-product_001-imported-cover.png` 返回 `Content-Type: application/json`，正文为连接 `127.0.0.1:9000` 失败。
- RED：`curl http://172.30.30.58:8081/admin-api/infra/file/28/get/showroom/narration/20260523/product-155-zh-ruoxi.wav` 返回 `Content-Type: application/json`，正文为连接 `127.0.0.1:9000` 失败。
- GREEN：修复后封面 URL 应返回 `Content-Type: image/png`，语音 URL 应返回 `Content-Type: audio/wav`。
- REGRESSION：抽查 `product_001`、`product_003`、`product_004`、`product_005`、`product_006` 当前封面 URL 均返回图片，并抽查最新产品语音 URL 返回音频。

## 当前状态

status: blocked

## Current Status

blocked

## 阻塞

已确认根因是测试服 `infra_file_config.id=28` 的 MinIO endpoint 配置为 `http://127.0.0.1:9000`。该地址在后端容器内指向后端容器自身，不是宿主机 MinIO，因此 `/admin-api/infra/file/28/get/**` 返回 JSON 错误而不是图片或语音。修复需要用户明确授权修改测试服文件配置。
