# 测试服务器 Sub2API 访问与代理节点管理

更新日期：2026-11-11。本文面向日常页面操作及服务器节点维护，仅适用于测试服务器。

## 1. 访问与登录

| 用途 | 地址或位置 |
| --- | --- |
| Web 入口 | http://39.106.23.28:8080/ |
| IP 管理页 | http://39.106.23.28:8080/admin/proxies |
| 健康检查 | http://39.106.23.28:8080/health |
| SSH | `ssh root@39.106.23.28` |
| Sub2API 部署目录 | `/root/apps/sub2api` |
| ShellCrash 持久配置 | `/etc/ShellCrash/yamls/config.yaml` |
| Nginx 入口配置 | `/root/apps/sub2api/nginx/sub2api-entry.conf` |

浏览器打开 Web 入口，用当前有效的 Sub2API 管理员账号登录，再进入“IP 管理”。直接打开 IP 管理页时，也需要管理员登录态。

SSH 密码见 [服务器信息](server-info.md) 的“测试服务器”章节。SSH root 密码与 Sub2API 管理员密码是两套凭据，不能混用。本文未取得并验证当前管理员账号密码，不能把容器初始化密码当作当前登录密码。

本次只读复核：服务器内部 `/health` 返回 `{"status":"ok"}`，`shellcrash.service` 为 `active`。本次没有执行页面登录、增删节点或全量节点连通性测试。

在 Windows PowerShell 登录：

```powershell
ssh root@39.106.23.28
```

登录后检查服务（以下命令在服务器 Linux shell 执行）：

```bash
cd /root/apps/sub2api
docker compose ps
curl --fail --max-time 15 http://127.0.0.1:8080/health
systemctl is-active shellcrash.service
docker compose logs --tail=100 sub2api
```

## 2. 理解 IP 管理中的代理地址

IP 管理保存的是 Sub2API 能连接的 HTTP/SOCKS 代理入口。FlClash 订阅中的 `vless`、`anytls` 节点需要先由服务器 ShellCrash 转为 mixed 监听，再登记到 IP 管理中。

当前部署的调用关系：

```text
Sub2API 容器 → host.docker.internal:节点端口 → ShellCrash → 对应代理节点
```

- 默认代理：`http://host.docker.internal:7890`，出口由 `LocalFlClash` 组当前选择的节点决定。
- 节点专属代理：`http://host.docker.internal:<该节点的监听端口>`，每个端口固定对应一个节点。
- 主机字段填写 `host.docker.internal`，不带 `http://`；协议和端口单独填写。
- 不填 `127.0.0.1`：在 Sub2API 容器中它代表容器自身。
- 不照抄历史端口范围或节点数量。以当前 ShellCrash `listeners` 和 IP 管理表为准。

## 3. 在页面添加代理

### 3.1 添加已有 HTTP/SOCKS 代理入口

1. 登录管理员，进入“IP 管理”，点击添加代理按钮。
2. 填写名称、协议、主机和端口；代理要求认证时填写其用户名、密码。
3. 添加测试服务器 ShellCrash 的入口时，协议选 `http`，主机填 `host.docker.internal`，端口填已配置且可用的节点监听端口。默认入口的端口为 `7890`。
4. 保存后，在该条记录上执行“测试连接”，核对出口国家和连通性。失败时停止将其分配给账号，先检查监听和节点。
5. 需要使用它的账号，在账号管理中编辑并显式选择该代理，再执行账号测试。仅添加 IP 记录不会自动改变账号绑定。

外部供应商直接提供 HTTP/SOCKS 代理时，主机和端口填写供应商地址，无须增加 ShellCrash 节点。

### 3.2 添加本机 FlClash 中的新节点

本机配置来源：`C:\Users\BJB110\AppData\Roaming\com.follow\clash\config.yaml`。节点定义含凭据，不应输出到日志或提交仓库。

1. 读取服务器当前配置和 IP 管理记录，核对节点名称、已有监听端口与账号绑定。为新节点选择未占用端口，保留已有节点端口身份。
2. 在服务器留存配置副本，将本机需要的节点完整定义合并到服务器配置的 `proxies` 中。保留既有规则和其他仍使用的节点。
3. 在 `listeners` 中添加该节点的 mixed 监听，`proxy` 必须精确等于节点名称。下面仅为配置结构示例；端口和名称须替换为本次实际核对的值，不能原样当作可用节点：

```yaml
listeners:
  - name: sub2api-node-example
    type: mixed
    listen: 0.0.0.0
    port: 8400
    proxy: 新节点的完整名称
```

4. 先将候选配置写到单独文件，如 `/etc/ShellCrash/yamls/config.yaml.candidate`。使用运行中的同一内核校验：

```bash
/proc/$(pgrep -x CrashCore | head -1)/exe -t -d /etc/ShellCrash -f /etc/ShellCrash/yamls/config.yaml.candidate
```

5. 只有校验成功后才替换持久配置并重启：

```bash
cp /etc/ShellCrash/yamls/config.yaml.candidate /etc/ShellCrash/yamls/config.yaml
systemctl restart shellcrash.service
systemctl is-active shellcrash.service
```

6. 从 Sub2API 容器内通过新端口验证；以下 `8400` 同样须换成实际端口：

```bash
docker exec sub2api curl --fail --max-time 15 -x http://host.docker.internal:8400 'http://ip-api.com/json/?lang=zh-CN'
docker exec sub2api curl --fail --max-time 15 -o /dev/null -w '%{http_code}\n' -x http://host.docker.internal:8400 https://www.gstatic.com/generate_204
```

第一项应返回 `status=success` 和预期出口国家，第二项应返回 HTTP `204`。失败节点不登记为启用状态。容器内实测通过后，按 3.1 在 IP 管理中添加对应入口。

### 3.3 批量刷新可用节点

每次刷新都重新读取本机最新配置，并从测试服务器 Sub2API 容器内逐个检测；本机能连接不代表服务器也能连接。按“节点定义 → 监听 → 容器检测 → IP 记录”的顺序同步。只启用本次验证通过的节点，不可用节点停用；不擅自迁移账号绑定，不复活软删除记录，不复制其他环境的主键。

历史 `output/proxy-refresh-*` 脚本含固定日期、前置断言和备份文件名，不能不检查就重复执行。重新运行前应核对现有配置、当前数据结构、端口占用及默认出口；缺少前提时停止相应操作并报告。

## 4. 删除代理节点

### 4.1 删除 IP 管理记录

1. 在 IP 管理定位目标代理，核对名称、地址、端口及关联账号。
2. 若有关联账号，先在账号管理中将这些账号显式改绑到已验证可用的代理，并验证账号。不要自动切为直连。
3. 回到 IP 管理，点击该条记录的删除按钮，确认删除。
4. 刷新列表，确认记录消失，并复核相关账号仍能使用。

仓库留存源码中的删除逻辑会拒绝删除仍有关联账号的代理；如页面提示“正在使用”，应先处理账号绑定。仅需暂时停止分配时，可编辑为停用，无须删除。

### 4.2 同时移除服务器节点和监听

删除 IP 管理记录不会自动删除 ShellCrash 节点或监听。需要彻底移除时，在删除记录并确认无其他使用者后：

1. 删除该端口对应的 `listeners` 条目。
2. 检查 `proxy-groups` 和其他监听是否仍引用该节点；处理所有引用后，才删除 `proxies` 中的节点定义。
3. 若它是 `LocalFlClash` 当前出口，先明确选择并验证替代节点；不得留下悬空引用。
4. 按添加流程先校验候选配置，再替换并重启 ShellCrash；复核默认入口和其余受影响监听。

不要直接硬删数据库行来绕开关联检查。不要把现有端口换给不同节点，避免原账号在不知情时切换出口。

## 5. 必须保留的配置与验证

- 保留 `host.docker.internal:host-gateway` 容器主机映射。
- 保留规则顶部的 `DOMAIN,api.asxs.top,DIRECT` 和 `DOMAIN,asxs.calsil.lol,DIRECT`；规则存在不代表这些上游当前可访问，账号实际调用需另行验证。
- 不把 `ip-api.com` 配成 `DIRECT`，否则出口检测可能显示测试服务器所在地区。
- 保留 `default:7890`，删除前必须核对账号和默认出口用途。
- ShellCrash 重启会影响经过该服务的连接，应在变更后复核默认代理、所改监听和 Sub2API 健康状态。

检查生效路由及默认入口：

```bash
curl --fail --max-time 10 http://127.0.0.1:9999/rules
docker exec sub2api curl --fail --max-time 15 -x http://host.docker.internal:7890 'http://ip-api.com/json/?lang=zh-CN'
curl --fail --max-time 15 http://127.0.0.1:8080/health
```

若连接失败，依次检查 `shellcrash.service`、对应端口监听、节点参数、容器到宿主的连接和节点实际出口。不要用默认成功值或自动直连掩盖故障。

## 6. 管理接口参考

仓库留存源码提供以下管理员接口，均需要当前有效的管理员 Bearer Token；Token 不写入本文或日志。当前部署版本的具体字段以其页面请求为准。

| 操作 | 方法与路径 |
| --- | --- |
| 列出代理 | `GET /api/v1/admin/proxies` |
| 添加代理 | `POST /api/v1/admin/proxies` |
| 修改或停用 | `PUT /api/v1/admin/proxies/:id` |
| 删除代理 | `DELETE /api/v1/admin/proxies/:id` |
| 测试连接 | `POST /api/v1/admin/proxies/:id/test` |
| 查看关联账号 | `GET /api/v1/admin/proxies/:id/accounts` |

添加的基本字段为 `name`、`protocol`、`host`、`port`，认证字段为 `username`、`password`。日常操作优先通过页面完成，删除前先查看关联账号。
