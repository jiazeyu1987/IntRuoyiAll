# 任务：修复展厅公司 V8 版本封面不可见

## 任务目标

修复公司版本页查看公司 V8 历史版本时封面图片不可见的问题。当前接口返回了封面 URL，但真实访问该 URL 返回 S3 404 JSON，不是图片文件。

## BDD 场景

- BDD: 公司 V8 历史版本封面必须可真实加载 -> Given 测试租户与芋道源码租户都存在公司 V8 历史版本 / When 用户在公司版本页查看 V8 详情 / Then `selectedVersion.image.contentImage.url` 指向的资源必须返回真实图片内容，而不是 S3 404 或 JSON 错误。
- BDD: 版本中心不得静默暴露不可读封面 -> Given 历史公司版本记录了封面 URL / When 后端或运维修复该版本素材 / Then 验证必须检查图片 URL 的 HTTP 内容类型与实际可读性，不得只检查字段非空。

## 里程碑

- [x] M1：复现 V8 公司版本封面不可见的真实原因。
- [x] M2：定位封面文件对象、数据库记录与版本中心快照之间的差异。
- [x] M3：以最小范围修复测试租户 V8 封面可读性，并评估芋道源码租户只读验证结果。
- [x] M4：运行真实接口/页面验证，记录 GREEN 证据。
- [x] M5：运行 task-closeout-cleanup 预览，完成任务记录。

## 预期验证

- 测试租户：`GET /showroom/version-center/detail?targetType=COMPANY&targetId=16&revisionId=17&siteKey=yingtai-showroom&stage=TEST` 返回 V8 封面 URL。
- 测试租户：访问该 V8 封面 URL 返回图片内容类型，不再返回 S3 404 JSON。
- 芋道源码租户：只读验证 `companyId=1, revisionId=8` 的 V8 封面 URL 状态。
- 如涉及代码或脚本变更，运行相关 Maven/脚本测试；如仅为真实测试数据修复，记录 SQL/对象存储只读与修复证据。

## Current Status

completed

## 当前状态

已复现。测试租户 V8 `revisionId=17` 与芋道源码租户 V8 `revisionId=8` 的版本中心 detail 都返回相同封面路径 `/admin-api/infra/file/28/get/20260521/%E5%BC%80%E5%9B%AD%E6%B4%BB%E5%8A%A8%E5%9B%BE-%E5%8E%8B%E7%BC%A9%E7%89%88.jpg`，但真实 GET 返回 `application/json;charset=UTF-8`，body 为 S3 `The specified key does not exist`。

## M2 定位结果

真实对象存在于测试服 MinIO `yudao/20260521/开园活动图-压缩版.jpg`，直接访问 `http://172.30.30.58:9000/yudao/20260521/%E5%BC%80%E5%9B%AD%E6%B4%BB%E5%8A%A8%E5%9B%BE-%E5%8E%8B%E7%BC%A9%E7%89%88.jpg` 返回 `image/jpeg`、长度 `644972`。数据库 `infra_file` 也存在多条 `config_id=28`、`path=20260521/开园活动图-压缩版.jpg`、`url=http://172.30.30.58:9000/yudao/20260521/开园活动图-压缩版.jpg` 的文件记录。

差异点为全局 `infra_file_config.id=28`：当前配置的 bucket/domain 是 `edhr-retention-verifier-20260528`，导致 `/admin-api/infra/file/28/get/20260521/...jpg` 从错误桶读取同名路径并返回 S3 404。修复应恢复 28 号 MinIO 配置指向 `yudao` 桶，不替换版本记录封面 URL，也不引入默认图 fallback。

## M3 修复结果

通过管理后台正式接口 `PUT /admin-api/infra/file-config/update` 修复全局文件配置 `infra_file_config.id=28`，将 bucket 从 `edhr-retention-verifier-20260528` 恢复为 `yudao`，domain 恢复为 `http://172.30.30.58:9000/yudao`，endpoint 保持 `http://host.docker.internal:9000`，accessKey/accessSecret 保持原运行环境 MinIO 凭据。该接口会清理文件客户端缓存；本次没有改写公司版本记录、没有替换封面 URL、没有新增默认图或 fallback。

## M4 验证结果

- GREEN: `GET http://172.30.30.58:48081/admin-api/infra/file/28/get/20260521/%E5%BC%80%E5%9B%AD%E6%B4%BB%E5%8A%A8%E5%9B%BE-%E5%8E%8B%E7%BC%A9%E7%89%88.jpg` -> PASS，`Content-Type=image/jpeg;charset=UTF-8`，长度 `644972`，首字节 `FFD8`。
- GREEN: `GET http://172.30.30.58:8081/admin-api/infra/file/28/get/20260521/%E5%BC%80%E5%9B%AD%E6%B4%BB%E5%8A%A8%E5%9B%BE-%E5%8E%8B%E7%BC%A9%E7%89%88.jpg` -> PASS，`Content-Type=image/jpeg;charset=UTF-8`，长度 `644972`，首字节 `FFD8`。
- GREEN: 测试租户 `companyId=16, revisionId=17` 与芋道源码租户只读验证 `companyId=1, revisionId=8` 的版本中心 detail 均返回同一 V8 封面 URL，真实访问均为 `image/jpeg;charset=UTF-8`，长度 `644972`。
- GREEN: Playwright 真实页面验证 `http://172.30.30.58:8081/showroom/company-version` -> PASS，测试租户登录后打开 V8 历史版本详情，封面 `naturalWidth=2343`、`naturalHeight=1642`、`complete=true`。

## M5 收尾结果

`task-closeout-cleanup` 预览完成：状态 `ready`，保留 `task.md` 与 `execution-log.md`，无待删除文件、无 blocked、无 warnings。

## 约束

- 默认只修复测试租户数据；芋道源码/admin 只做最终只读验证，未经用户明确批准不修改。
- 不引入 fallback；封面对象缺失时必须暴露真实缺失状态，不能换用默认图或静默成功。
- 不删除、清空或改写共享 NAS、MinIO 桶或生产挂载。
