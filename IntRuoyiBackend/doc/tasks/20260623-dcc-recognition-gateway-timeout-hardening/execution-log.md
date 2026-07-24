# DCC 内容识别前端网关超时硬化执行日志

- BDD: 长耗时 DCC 识别请求不应被 60 秒网关超时提前截断 -> Given backend 内容识别可能需要 80 秒以上 / When 前端通过 /admin-api/ 调用识别接口 / Then nginx 必须等待足够长时间，让页面拿到 backend 的真实成功或失败结果。
- BDD: 文件下载代理与普通 admin-api 代理保持一致超时合同 -> Given DCC 文件内容读取同样可能较慢 / When 经过 /admin-api/infra/file/ 代理访问 / Then 代理超时也必须显式配置，而不是使用默认短超时。
- GREEN: previous-task-check -> PASS，已核对短编码硬化任务完成态，本轮只处理 frontend nginx 超时合同。
- GREEN: experience-preflight -> PASS，本轮仅修改 backend 仓发布模板与脚本测试，不执行服务器写入。
- RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-short-code-recognition-hardening\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL，新增断言证明当前 `script/deploy/int-ruoyi-test/nginx.conf` 未为 `/admin-api/` 与 `/admin-api/infra/file/` 配置 `proxy_read_timeout/proxy_send_timeout`。
- GREEN: frontend-nginx-timeout-contract -> PASS，已在 `script/deploy/int-ruoyi-test/nginx.conf` 对两个 admin-api 代理块增加 `proxy_connect_timeout 60s`、`proxy_read_timeout 300s`、`proxy_send_timeout 300s`。
- GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-dcc-short-code-recognition-hardening\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，`87 passed`。
