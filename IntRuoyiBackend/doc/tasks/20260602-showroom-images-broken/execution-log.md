# 执行日志：修复展厅图片再次不可用

BDD: 展厅默认文件配置必须保持受保护值 -> Given 本机展厅媒体依赖 `infra_file_config.id=28` / When 检查当前运行库文件配置 / Then bucket 必须为 `yudao` 且 domain 必须为 `http://127.0.0.1:9000/yudao`。

BDD: 展厅媒体 URL 不得漂移到非默认域 -> Given `infra_file` 中 `config_id=28` 且 `path LIKE 'showroom/%'` 的媒体记录 / When 检查媒体 URL / Then URL 必须以 `http://127.0.0.1:9000/yudao/showroom/` 开头。

BDD: 图片接口必须返回真实图片内容 -> Given 展厅页面引用的公司或产品图片 URL / When 通过后台文件代理请求该 URL / Then 响应必须为 `image/*`，不得返回缺失对象 JSON、404 或 HTML。

## Diagnosis

- `2026-06-02 13:47` 后端健康检查 `curl.exe -sS -i http://127.0.0.1:48081/actuator/health` 返回 HTTP 200 / `{"status":"UP"}`。
- `2026-06-02 13:48` 只读查询 `infra_file_config.id=28`，确认 bucket=`yudao`、domain=`http://127.0.0.1:9000/yudao`、endpoint=`http://127.0.0.1:9000`，受保护配置未漂移。
- `2026-06-02 13:49` 只读查询 `infra_file WHERE config_id=28 AND path LIKE 'showroom/%' AND deleted=0`，total=`2843`、drifted=`0`，展厅媒体 URL 未漂移。
- `2026-06-02 13:50` 复现图片代理异常：`curl.exe -sS -I http://127.0.0.1:48081/admin-api/infra/file/28/get/showroom/product/cover/20260530/product-product_164-imported-cover.png` 返回 HTTP 200 但 `Content-Type: application/json`。
- `2026-06-02 13:51` 读取异常正文，后端文件代理仍尝试连接旧 endpoint `host.docker.internal:9000` 并超时，说明运行中后端文件客户端持有旧 S3 endpoint。
- `2026-06-02 13:55` 等待超过 `FileConfigServiceImpl` 的 10 秒异步刷新窗口后再次请求，仍为 `Content-Type: application/json`，旧 client 未自然恢复。
- `2026-06-02 14:09` 通过本机运行态登录后读取 `/admin-api/infra/file-config/get?id=28`，发现运行服务返回的 master 文件配置实际为 `endpoint=http://host.docker.internal:9000`、`domain=http://172.30.30.58:9000/yudao`，与本地 Docker 库中的受保护值不一致。
- `2026-06-02 14:12` 检查端口占用确认根因：本机 `ssh.exe` 进程以 `-L 23306:192.168.48.3:3306 -L 26379:192.168.48.2:6379 root@172.30.30.58` 抢占了 `127.0.0.1:23306/26379`，导致本机后端虽写着“连本地端口”，实际命中了 SSH 隧道后的远端库/Redis。
- `2026-06-02 14:20` 正式修复策略改为调整本机重启脚本：后端运行连接改走未被 SSH 隧道遮蔽的 Docker loopback `127.0.0.2:23306/26379`，并在启动前加入端口路由 fail-fast 守卫；同时显式覆盖 master/slave 两个数据源。

## RED

RED: `curl.exe -sS -I http://127.0.0.1:48081/admin-api/infra/file/28/get/showroom/product/cover/20260530/product-product_164-imported-cover.png` -> FAIL, expected `Content-Type: image/*`, actual `Content-Type: application/json`。
RED: `python -X utf8 -m pytest script\tests\test_runtime_control_scripts.py -k "unshadowed_docker_loopback" -q` -> FAIL, restart script still bound backend MySQL/Redis runtime to `127.0.0.1`, so it could not defend against SSH local-port shadowing.

## GREEN

GREEN: `python -X utf8 -m pytest script\tests\test_runtime_control_scripts.py -q` -> PASS, `12 passed`; restart script contract now locks backend runtime to `${LocalDockerRuntimeHost}=127.0.0.2`, covers master/slave datasource override, and fails fast when Docker loopback ports are shadowed.
GREEN: `curl.exe -sS -i http://127.0.0.1:48081/actuator/health` -> PASS, local backend recovered with HTTP 200 / `{"status":"UP"}` after restart.
GREEN: 运行态只读查询 `/admin-api/infra/file-config/get?id=28` -> PASS, active master file config now returns `endpoint=http://127.0.0.1:9000` and `domain=http://127.0.0.1:9000/yudao`.
GREEN: `curl.exe -sS -I http://127.0.0.1:48081/admin-api/infra/file/28/get/showroom/product/cover/20260530/product-product_164-imported-cover.png` -> PASS, HTTP 200 with `Content-Type: image/png` and correct inline filename.

## Final Result

- 本次故障根因不是图片文件丢失，也不是 28 号受保护配置漂移，而是本机 `127.0.0.1:23306/26379` 被 SSH 本地转发抢占，导致后端误连远端库/Redis，读取到远端的 master 文件配置。
- 正式修复为调整本机后端重启脚本的运行连接主机到未被遮蔽的 `127.0.0.2`，并补上端口路由 fail-fast 守卫与 master/slave 双数据源显式覆盖。
- 修复后，本机运行态 file-config 已恢复受保护值，展厅图片代理恢复返回真实 `image/png`。
