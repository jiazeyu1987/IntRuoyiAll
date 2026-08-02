# Execution Log

## User Intent

用户确认如果修复 Docker/对象存储问题，DCC 原版发布 E2E 就可以继续；本任务负责恢复本机对象存储前置。

## BDD

- `BDD: 对象存储前置恢复 -> Given` DCC 原版发布真实 E2E 在上传预览阶段访问对象存储失败；`When` 本机对象存储按项目配置恢复；`Then` `127.0.0.1:9000` 可监听，真实页面 upload-preview 不再因连接拒绝返回业务 500。
- `BDD: 不绕过业务链路 -> Given` upload-preview 依赖正式文件服务；`When` 修复本机运行态；`Then` 不修改 DCC 业务代码、不切换存储实现、不使用 mock/API-only/SQL 改状态冒充通过。

## Evidence

- 2026-08-02：读取 `docs/local-runtime.md`、`docs/e2e-rules.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/experience-index.md`。
- 2026-08-02：初始宽搜索命令因仓库中既有损坏 MES target 目录和 PowerShell 通配参数噪音返回非 0，未据此修改文件或环境；后续改为收窄配置/脚本路径。
- 2026-08-02：按 `docs/e2e-rules.md` 补读 `docs/login-access.md`、`docs/worktree-restrictions.md`、`docs/database-rules.md`；DB/接口仅用于只读辅助核验。
- 2026-08-02：确认 `docker-minio-1` 原为停止态后执行 `docker start docker-minio-1`，容器状态变为 `Up (healthy)`，端口映射 `9000-9001->9000-9001`。
- 2026-08-02：`http://127.0.0.1:9000/minio/health/ready` 返回 HTTP 200，`/data/yudao` bucket 目录存在；文件配置只读核验 endpoint=`http://127.0.0.1:9000`、bucket=`yudao`、domain=`http://127.0.0.1:9000/yudao`，密钥未写入文档。
- 2026-08-02：MinIO 恢复后发现并发后端重启/构建导致 `48081` 一度无监听；等待 Maven package 结束后，使用项目标准 `restart-int-ruoyi-local.ps1 -Component backend` 恢复后端，`/actuator/health` 返回 `UP`。
- 2026-08-02：前端 `http://127.0.0.1:8081/` 返回 HTTP 200，后端监听 `48081`，MinIO ready HTTP 200。
- 2026-08-02：按 `project-experience-consolidation` 规则将复发门禁沉淀到已有 `docs/local-runtime.md#2026-08-02-dcc-上传预览本机-minio-前置门禁`，并在 `docs/experience-index.md` 增加关键词路由；未新建长期经验文档。

## Verification Evidence

- `GREEN: docker ps --filter name=docker-minio-1 -> PASS`，MinIO 容器 `Up (healthy)`。
- `GREEN: Invoke-WebRequest http://127.0.0.1:9000/minio/health/ready -> PASS`，HTTP 200。
- `GREEN: Invoke-RestMethod http://127.0.0.1:48081/actuator/health -> PASS`，`{"status":"UP"}`。
- `GREEN: node doc\tasks\20260802-dcc-minio-object-storage-runtime\verify-upload-preview.cjs -> PASS`，真实页面非 admin 上传人 `pengyunfeng` 登录、进入 `/dcc/controlled-file/upload`、填写上传字段并完成 `upload-preview`；未提交审批链路。
- `GREEN: upload-preview-result.json -> PASS`，runId=`20260802084900`，fileNumber=`CODX-DCC-MINIO-20260802084900`，previewFileName=`批记录节点-解析样本.docx`，previewKind=`OFFICE`，targetNetworkFailures/consoleErrors/pageErrors 均为空。
- `GREEN: yudao-server.log -> PASS`，最新 `CODX-DCC-MINIO-20260802084900` 的 `current-version` 与 `upload-preview` 均完成，未再出现对象存储 `Connection refused`。
- `GREEN: rg "DCC upload-preview|docker-minio-1|MinIO 9000" docs\experience-index.md docs\local-runtime.md -> PASS`，长期经验可按关键词定位。

## Blockers

- 无当前 blocker。DCC 原版发布 E2E 可从新文件号重新发起完整上传、四级审批/签名、发布生效和受控浏览验证；不要复用本次只验证上传预览的临时文件号。
