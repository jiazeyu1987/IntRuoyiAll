# Execution Log: 展厅公司 V8 版本封面不可见

BDD: 公司 V8 历史版本封面必须可真实加载 -> Given 测试租户与芋道源码租户都存在公司 V8 历史版本 / When 用户在公司版本页查看 V8 详情 / Then `selectedVersion.image.contentImage.url` 指向的资源必须返回真实图片内容，而不是 S3 404 或 JSON 错误。

BDD: 版本中心不得静默暴露不可读封面 -> Given 历史公司版本记录了封面 URL / When 后端或运维修复该版本素材 / Then 验证必须检查图片 URL 的 HTTP 内容类型与实际可读性，不得只检查字段非空。

RED: `GET http://172.30.30.58:48081/admin-api/showroom/version-center/detail?targetType=COMPANY&targetId=16&revisionId=17&siteKey=yingtai-showroom&stage=TEST` -> PASS 返回 V8 detail，但 `selectedVersion.image.contentImage.url` 指向 `/admin-api/infra/file/28/get/20260521/%E5%BC%80%E5%9B%AD%E6%B4%BB%E5%8A%A8%E5%9B%BE-%E5%8E%8B%E7%BC%A9%E7%89%88.jpg`。

RED: `GET http://172.30.30.58:48081/admin-api/infra/file/28/get/20260521/%E5%BC%80%E5%9B%AD%E6%B4%BB%E5%8A%A8%E5%9B%BE-%E5%8E%8B%E7%BC%A9%E7%89%88.jpg` -> FAIL，返回 `application/json;charset=UTF-8`，body 包含 `The specified key does not exist`，不是图片。

RED: `GET http://172.30.30.58:8081/admin-api/infra/file/28/get/20260521/%E5%BC%80%E5%9B%AD%E6%B4%BB%E5%8A%A8%E5%9B%BE-%E5%8E%8B%E7%BC%A9%E7%89%88.jpg` -> FAIL，前端域名代理同样返回 S3 404 JSON，说明页面图片不可见来自文件对象不可读。

INFO: `GET http://172.30.30.58:9000/yudao/20260521/%E5%BC%80%E5%9B%AD%E6%B4%BB%E5%8A%A8%E5%9B%BE-%E5%8E%8B%E7%BC%A9%E7%89%88.jpg` -> PASS，`Content-Type=image/jpeg`，长度 `644972`，证明真实对象存在。

INFO: `find /var/lib/docker/intruoyi-data/runtime-data/minio -maxdepth 3 -type d` -> PASS，测试服 MinIO 同时存在 `yudao` 与 `edhr-retention-verifier-20260528` 桶；`yudao/20260521/开园活动图-压缩版.jpg` 存在。

INFO: `SELECT id,name,storage,master,config FROM infra_file_config WHERE id=28` -> PASS，当前 28 号 MinIO 配置指向 bucket `edhr-retention-verifier-20260528`、domain `http://host.docker.internal:9000/edhr-retention-verifier-20260528`，与历史文件记录中的 `yudao` bucket 不一致。

GREEN: `PUT http://172.30.30.58:48081/admin-api/infra/file-config/update` -> PASS，`updateCode=0`、`updateData=true`，`infra_file_config.id=28` 从 bucket `edhr-retention-verifier-20260528` 恢复为 bucket `yudao`、domain `http://172.30.30.58:9000/yudao`，endpoint 保持 `http://host.docker.internal:9000`。

GREEN: `GET http://172.30.30.58:48081/admin-api/infra/file/28/get/20260521/%E5%BC%80%E5%9B%AD%E6%B4%BB%E5%8A%A8%E5%9B%BE-%E5%8E%8B%E7%BC%A9%E7%89%88.jpg` -> PASS，`Content-Type=image/jpeg;charset=UTF-8`，长度 `644972`，首字节 `FFD8`。

GREEN: `GET http://172.30.30.58:8081/admin-api/infra/file/28/get/20260521/%E5%BC%80%E5%9B%AD%E6%B4%BB%E5%8A%A8%E5%9B%BE-%E5%8E%8B%E7%BC%A9%E7%89%88.jpg` -> PASS，`Content-Type=image/jpeg;charset=UTF-8`，长度 `644972`，首字节 `FFD8`。

GREEN: version-center detail check -> PASS，测试租户 `companyId=16, revisionId=17` 与芋道源码租户只读验证 `companyId=1, revisionId=8` 都返回 `/admin-api/infra/file/28/get/20260521/%E5%BC%80%E5%9B%AD%E6%B4%BB%E5%8A%A8%E5%9B%BE-%E5%8E%8B%E7%BC%A9%E7%89%88.jpg`，真实 GET 均返回 `image/jpeg;charset=UTF-8`、长度 `644972`。

GREEN: Playwright real user path `http://172.30.30.58:8081/showroom/company-version` -> PASS，测试租户登录后打开 V8 历史版本详情，封面图片 `src` 为前端代理 `/admin-api/infra/file/28/get/20260521/...jpg`，`naturalWidth=2343`，`naturalHeight=1642`，`complete=true`。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260529-showroom-company-v8-cover-file-404 --mode preview` -> PASS，`status=ready`，keep `task.md` 与 `execution-log.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。
