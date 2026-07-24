# 执行日志：修复展厅产品管理封面加载失败

BDD: 产品管理封面必须可直接加载 -> Given 测试环境产品管理列表存在已上传封面的真实产品 / When 用户进入展厅后台产品管理 / Then 封面列的图片请求必须返回真实图片内容类型，不得返回 JSON、HTML 或文件元数据。

BDD: MinIO 主配置必须指向历史文件所在桶 -> Given `infra_file` 中 5803 条 `config_id=28` 记录指向 `http://127.0.0.1:9000/yudao/...` / When 后端通过 `/admin-api/infra/file/28/get/...` 代理读取文件 / Then `infra_file_config.id=28` 必须指向 `yudao` 桶，不能指向临时验证桶。

## RED

- RED: Playwright 真实登录 `http://127.0.0.1:8081/login?redirect=/showroom/product` 后进入产品管理 -> FAIL，产品接口返回 20 条封面 URL，但图片请求 `GET /admin-api/infra/file/28/get/showroom/product/cover/20260524/product-product_001-cover.png` 返回 `application/json;charset=UTF-8`，DOM 图片显示加载失败。
- RED: `GET http://127.0.0.1:48081/admin-api/infra/file/28/get/showroom/product/cover/20260524/product-product_001-cover.png` -> FAIL，返回 `application/json;charset=UTF-8`，body 包含 S3 `The specified key does not exist`，不是图片。
- RED: `SELECT ... FROM infra_file_config WHERE id=28` -> FAIL，`bucket=edhr-retention-verifier-20260528`、`domain=http://127.0.0.1:9000/edhr-retention-verifier-20260528`，但 `infra_file` 中 `config_id=28` 有 5803 条 `yudao` URL、仅 20 条临时 `edhr-retention-verifier-20260528` URL。
- RED: MinIO 对象只读检查 -> PASS 证明根因，`/data/yudao/showroom/product/cover/20260524/product-product_001-cover.png` 等产品封面对象存在；故失败不是对象缺失，而是主配置读错桶。

## GREEN

- GREEN: `UPDATE infra_file_config SET config=JSON_SET(config, '$.bucket', 'yudao', '$.domain', 'http://127.0.0.1:9000/yudao') WHERE id=28 ...` -> PASS，仅修复本地文件配置 bucket/domain，不修改产品、版本或租户业务数据。
- GREEN: `GET http://127.0.0.1:48081/admin-api/infra/file/28/get/showroom/product/cover/20260524/product-product_001-cover.png` -> PASS，返回 `image/png;charset=UTF-8`，长度 `1640739`。
- GREEN: `GET http://127.0.0.1:48081/admin-api/infra/file/28/get/20260521/...jpg` -> PASS，返回 `image/jpeg;charset=UTF-8`，长度 `644972`，同根公司 V8 封面路径也恢复可读。
- GREEN: Playwright 真实登录并访问 `http://127.0.0.1:8081/showroom/product` -> PASS，捕获 20 个产品封面响应，`badResponses=[]`；DOM 中 20 个封面图片 `complete=true` 且 `naturalWidth>0`。

## REGRESSION

- REGRESSION: 测试服只读验证 `http://172.30.30.58:48081/admin-api/infra/file/28/get/showroom/product/cover/20260524/product-product_001-cover.png` -> PASS，返回 `image/png;charset=UTF-8`，长度 `1640739`。
- REGRESSION: 测试服只读验证 `http://172.30.30.58:48081/admin-api/infra/file/28/get/20260521/...jpg` -> PASS，返回 `image/jpeg;charset=UTF-8`，长度 `644972`。
- REGRESSION: 本次没有生产代码变更；问题为本地运行环境文件配置漂移，因此未运行 Maven 单元测试。

## Blockers

- 当前无。
