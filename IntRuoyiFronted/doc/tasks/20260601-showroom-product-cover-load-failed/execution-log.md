# 执行日志

BDD: 产品列表封面内容地址必须返回图片 -> Given 产品列表接口返回 `cover_image=/admin-api/infra/file/28/get/showroom/product/cover/20260530/product-product_001-imported-cover.png` / When 浏览器请求该地址 / Then 响应应为 `image/png` 图片内容，而不是 JSON 错误。

BDD: 产品讲解语音地址必须返回音频 -> Given 产品讲解版本返回 `audioUrl=/admin-api/infra/file/28/get/showroom/narration/20260523/product-155-zh-ruoxi.wav` / When 浏览器请求该地址 / Then 响应应为 `audio/wav` 音频内容，而不是 JSON 错误。

BDD: 文件存储配置必须可被后端容器访问 -> Given 文件配置 28 是 S3/MinIO 存储 / When 后端通过 `/admin-api/infra/file/28/get/**` 读取对象 / Then S3 endpoint 必须是后端容器可达地址。

SETUP: 使用 `bug-regression-fix-loop` 缺陷回归流程。

SETUP: 已读取统一前端样式 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。

SETUP: 前序最近有效任务 `doc/tasks/20260601-nas-transfer-permission-snapshot-ready/task.md` 已标记 completed，不阻塞本任务。

SETUP: 当前仓库存在无关未提交改动 `src/views/system/nas/components/NasPermissionRestorePanel.vue`、`tests/e2e/dcc-nas-permission-restore-static.spec.js` 与无关未跟踪任务目录；本任务不修改、不回滚、不纳入。

DIAGNOSIS: `ssh root@172.30.30.58 "cd /opt/intruoyi/runtime && docker compose ps"` -> 测试服后端、前端、MySQL、Redis、Website 均运行；MinIO 由 `ragflow_compose-minio-1` 暴露宿主机 `9000`。

DIAGNOSIS: 查询测试服数据库 `showroom_product`/`showroom_product_revision` -> 截图中 `product_001`、`product_003`、`product_004`、`product_005`、`product_006` 当前封面均是 `/admin-api/infra/file/28/get/...png`，不是 `/admin-api/infra/file/get?id=...` 元数据地址。

DIAGNOSIS: 查询测试服数据库 -> 当前产品封面使用 config 28 的记录数为 310；当前产品封面使用 `/admin-api/infra/file/get?id=%` 的记录数为 0。

RED: `curl -sS -D - -o NUL http://172.30.30.58:8081/admin-api/infra/file/28/get/showroom/product/cover/20260530/product-product_001-imported-cover.png` -> FAIL, HTTP 200 但 `Content-Type: application/json`，不是 `image/png`。

RED: `curl -sS http://172.30.30.58:8081/admin-api/infra/file/28/get/showroom/product/cover/20260530/product-product_001-imported-cover.png` -> FAIL, 返回 `Unable to execute HTTP request: Connect to 127.0.0.1:9000 failed: Connection refused`。

DIAGNOSIS: 查询测试服 `infra_file_config.id=28` -> `endpoint` 为 `http://127.0.0.1:9000`，`domain` 为 `http://127.0.0.1:9000/yudao`。

DIAGNOSIS: 在后端容器内探测 MinIO -> `http://127.0.0.1:9000/minio/health/live` 返回 `000`；`http://172.30.30.58:9000/minio/health/live` 和 `http://host.docker.internal:9000/minio/health/live` 返回 `200`。

DIAGNOSIS: 在后端容器内探测对象 -> `http://127.0.0.1:9000/yudao/showroom/product/cover/20260530/product-product_001-imported-cover.png` 返回 `000`；`http://172.30.30.58:9000/yudao/showroom/product/cover/20260530/product-product_001-imported-cover.png` 和 `http://host.docker.internal:9000/yudao/showroom/product/cover/20260530/product-product_001-imported-cover.png` 返回 `200 image/png 4181`。

ROOT_CAUSE: 测试服文件配置 28 的 S3 endpoint 使用了容器内不可达的 `127.0.0.1:9000`。后端容器读取 `/admin-api/infra/file/28/get/**` 时连接的是后端容器自身的 9000 端口，导致连接拒绝；前端 `el-image` 收到 JSON 错误正文而不是图片内容，所以显示“加载失败”。

DIAGNOSIS: 查询测试服 `showroom_narration_version`/`infra_file` -> 最新产品讲解音频 `audio_file_id=4647` 对应 config 28，路径为 `showroom/narration/20260523/product-155-zh-ruoxi.wav`，类型为 `audio/wav`。

RED: `curl -sS -D - -o NUL http://172.30.30.58:8081/admin-api/infra/file/28/get/showroom/narration/20260523/product-155-zh-ruoxi.wav` -> FAIL, HTTP 200 但 `Content-Type: application/json`，不是 `audio/wav`。

RED: `curl -sS http://172.30.30.58:8081/admin-api/infra/file/28/get/showroom/narration/20260523/product-155-zh-ruoxi.wav` -> FAIL, 返回 `Unable to execute HTTP request: Connect to 127.0.0.1:9000 failed: Connection refused`。

DIAGNOSIS: 后端容器连通性复核 -> `http://127.0.0.1:9000/minio/health/live` 返回 `000`；`http://172.30.30.58:9000/minio/health/live` 与 `http://host.docker.internal:9000/minio/health/live` 返回 `200`。

ROOT_CAUSE: 产品语音与产品封面走同一个 `infra_file_config.id=28` 文件读取链路；config 28 的 endpoint 在后端容器内不可达，因此图片和语音都会返回 JSON 错误，前端不会也不应伪造可读状态。

BLOCKED: 修复需要修改测试服 `infra_file_config.id=28` 或通过后台文件配置页面更新 MinIO endpoint/domain；当前只授权查看测试服，未授权修改测试服配置。
