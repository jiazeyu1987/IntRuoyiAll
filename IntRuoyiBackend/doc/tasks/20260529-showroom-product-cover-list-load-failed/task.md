# 任务：修复展厅产品管理封面加载失败

## 任务目标

修复展厅后台产品管理列表中封面全部显示“加载失败”的问题。实际根因为本地 `infra_file_config.id=28` 的 MinIO 主配置被临时任务改到 `edhr-retention-verifier-20260528` 桶，而产品封面文件记录和真实对象都在 `yudao` 桶，后端代理因此从错误桶读取并返回 S3 404 JSON。

## BDD 场景

- BDD: 产品管理封面必须可直接加载 -> Given 本地产品管理列表存在已上传封面的真实产品 / When 用户进入展厅后台产品管理 / Then 封面列的图片请求必须返回真实图片内容类型，不得返回 JSON、HTML 或文件元数据。
- BDD: MinIO 主配置必须指向历史文件所在桶 -> Given `infra_file` 中 5803 条 `config_id=28` 记录指向 `http://127.0.0.1:9000/yudao/...` / When 后端通过 `/admin-api/infra/file/28/get/...` 代理读取文件 / Then `infra_file_config.id=28` 必须指向 `yudao` 桶，不能指向临时验证桶。

## 里程碑

- [x] M1：确认相关仓库状态、已有任务文档和受影响代码路径。
- [x] M2：用真实产品管理路径复现封面加载失败并记录根因。
- [x] M3：记录 RED 证据，确认本地后端对真实封面 URL 返回 S3 404 JSON。
- [x] M4：最小修复本地 `infra_file_config.id=28` 的 MinIO bucket/domain 指向 `yudao`。
- [x] M5：运行接口与真实页面验证，记录证据。
- [ ] M6：运行 task-closeout-cleanup 预览并提交本任务文档。

## 预期验证

- 本地接口：`GET http://127.0.0.1:48081/admin-api/infra/file/28/get/showroom/product/cover/20260524/product-product_001-cover.png` 返回 `image/png`。
- 本地接口：`GET http://127.0.0.1:48081/admin-api/infra/file/28/get/20260521/...jpg` 返回 `image/jpeg`，覆盖同根公司 V8 封面路径。
- 真实页面：Playwright 登录 `http://127.0.0.1:8081/showroom/product` 后，产品管理首屏 20 个封面响应均为 `image/*`，DOM 图片全部完成加载。
- 测试服只读验证：`172.30.30.58` 同产品封面与公司 V8 封面均返回 `image/*`。

## 当前状态

completed

## 备注

- 检查到后端仓库存在其他并行 `in_progress` 任务文档。本次不改写无关任务状态，避免破坏并行工作记录。
- 本次没有引入 fallback、默认图或代码兼容分支；修复方式为恢复本地 MinIO 主文件配置与既有文件记录一致。
- `infra_file` 中仍有 20 条临时 `edhr-retention-verifier-20260528` URL 记录；本次不改写这些临时记录，避免扩大范围。若后续需要保留该校验桶，应为其建立独立文件配置，而不是复用 28 号主配置。
