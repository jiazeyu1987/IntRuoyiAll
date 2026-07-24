# 20260615 主分支构建发布到测试服务器

## 任务目标

使用主工作区 `D:\ProjectPackage\Int\IntRuoyi` 下的主分支工作树构建 IntRuoyi，并只发布到测试服务器，不要求正式服务器，不操作正式服务器。

## 范围

- 后端：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`，分支 `int_main`。
- 前端：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`，分支 `int_main`。
- 发布目标：测试服务器 `172.30.30.58`。
- 不发布正式服务器。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。本次避免使用发布 worktree，直接以主工作区作为构建输入，消除 worktree 代码滞后导致旧接口进包的问题。
- 是否存在临时补丁或绕过：否。

## 经验门禁

- 发布、服务器写入、重启前必须先阅读 `docs/server-access.md`、`docs/release-backup-restore.md`。
- 涉及发布前必须记录 `GREEN: experience-preflight -> PASS`。
- 正式服务器默认禁止操作；本次仅测试服务器。
- PowerShell 不使用 `&&`。
- 发布证据必须包含环境、目标、构建命令、发布命令、制品标签、验证命令和回滚说明。

## 里程碑

1. 前置检查：确认主工作区分支、工作区状态、DCC 新接口在源码中存在。
2. 验证：运行 DCC 后端目标测试和前端 NAS 静态测试。
3. 构建：通过运行控制台“构建发布包”按钮构建发布包。
4. 发布：通过运行控制台“部署发布包到测试服”按钮只部署到测试服务器 `172.30.30.58`。
5. 验收：核验测试服镜像标签、健康状态、前后端 bundle/class 中包含分片上传接口。

## Current Status

completed

## 当前状态

已完成。

已通过运行控制台按钮完成主工作区构建与测试服务器发布，最终测试服发布包为 `26-06-16 03:21:37` / `26-06-16_03-21-37`。发布完成后，后端健康检查、前端首页、PDF worker MIME 验证均通过，测试服 `docker compose config` 已确认后端启动参数包含 `--yudao.captcha.enable=false`，与前端测试构建登录链路一致。

本轮同时完成发布链路中的正式修复：

- 补齐 SQL 迁移 `release-migration:` 元数据与发布迁移策略门禁。
- 修复 `release_preflight_plan.py` 对 Manifest v1 `database.schemaMigrations` 的读取，以及 checksum 漂移时 required SQL 重放计划。
- 修复测试服 `deploy-release` 自动生成 `preflight-plan.json` 的脚本根路径。
- 将 Kingdee 租户唯一索引、DCC 产品可见组、普惠排产菜单、eDHR 执行列表隐藏菜单等迁移改为可重放且失败可见。
- 修复测试服 compose 验证码配置不一致问题。
- 修复 DCC 本地文件夹导入同用户同根目录 `UPLOADING` 任务续传时被 active task 阻断的问题。

最终验证证据见 `execution-log.md`：发布工具与迁移脚本测试通过，DCC/Infra 目标 Maven 测试通过，测试服按钮发布成功，DCC 续传服务测试通过。

## 注意事项

本任务在用户要求下使用主分支工作区构建；发布包反映当时主工作区内容。提交收尾时需排除 `runtime/` 运行期产物，仅提交源码、脚本、SQL、测试和本任务记录。
