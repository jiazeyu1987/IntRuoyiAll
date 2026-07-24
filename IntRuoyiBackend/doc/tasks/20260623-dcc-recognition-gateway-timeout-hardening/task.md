# 任务：DCC 内容识别前端网关超时硬化

## 任务目标

修复测试服前端经 `8081 -> /admin-api/` 反向代理调用 DCC 内容识别时，nginx 在约 60 秒提前返回 `504 Gateway Time-out` 的问题，保证前端至少能等待 backend 的真实识别结果返回。

## 当前状态

COMPLETED：frontend nginx 模板已补齐 `admin-api` 反向代理超时配置，发布工具测试通过；当前前端 `60s` 级别 `504` 根因已在本地收口。

## Current Status

COMPLETED

## 上一任务检查

- 上一个 backend 任务：`D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-short-code-recognition-hardening\doc\tasks\20260623-dcc-short-code-recognition-hardening\task.md`
- 状态：`COMPLETED`
- 处理：短编码文件名直连硬化已完成；本任务只处理前端网关对长耗时识别请求的超时配置。

## 经验门禁

- 已读取：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
- 本任务适用强制门禁：
  - 当前修复必须落在真正用于 frontend image 构建的 `ruoyi-vue-pro/script/deploy/int-ruoyi-test/nginx.conf`。
  - 先补失败回归断言，再改 nginx 模板。
  - 不得通过前端静默吞错或客户端重试伪装“识别完成”。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是
- `是否存在临时补丁或绕过`：否

## BDD 场景

- `BDD: 长耗时 DCC 识别请求不应被 60 秒网关超时提前截断 -> Given backend 内容识别可能需要 80 秒以上 / When 前端通过 /admin-api/ 调用识别接口 / Then nginx 必须等待足够长时间，让页面拿到 backend 的真实成功或失败结果。`
- `BDD: 文件下载代理与普通 admin-api 代理保持一致超时合同 -> Given DCC 文件内容读取同样可能较慢 / When 经过 /admin-api/infra/file/ 代理访问 / Then 代理超时也必须显式配置，而不是使用默认短超时。`

## 里程碑

1. 为 frontend nginx 模板补 RED 契约。`DONE`
2. 修改 admin-api 代理超时配置。`DONE`
3. 跑通发布工具测试。`DONE`

## 预期验证

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-short-code-recognition-hardening\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`

## 完成结果

- `script/deploy/int-ruoyi-test/nginx.conf` 已对 `/admin-api/` 与 `/admin-api/infra/file/` 显式增加 `proxy_connect_timeout 60s`、`proxy_read_timeout 300s`、`proxy_send_timeout 300s`。
- 发布工具回归 `87` 项已通过。
- 测试服是否重发该前端超时修复，留待与内容识别样本选择一起决定。
