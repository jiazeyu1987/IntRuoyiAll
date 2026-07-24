# 任务：Smart Release 构建并发布一次

## Goal

按照现有 Smart Release 文档和发布脚本完成构建与发布功能，并真实完成一次构建发布包、一次发布部署验证。

## Scope

- 审计当前构建与发布链路是否满足文档要求。
- 修复阻断 `build-release` 或 `deploy-release` 的本地代码/配置缺口。
- 使用受控配置构建一次发布包。
- 在授权目标环境执行一次发布，并记录部署后验证证据。
- 不把测试服、正式服、备份服 IP 写死到代码或发布包。

## Non-Scope

- 不操作正式服，除非用户再次明确指定正式发布目标。
- 不删除服务器文件、NAS 挂载或 MinIO/NAS 对象。
- 不使用 mock 成功替代真实构建或发布。
- 不用外网 fallback 替代离线基础镜像或受控资源证明。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺前置配置、构建失败、发布失败都记录 blocker，不伪装成功。
- `是否从根因和长期维护角度解决`：是；目标是补齐构建、发布、资源证明和目标配置的正式链路。
- `是否存在临时补丁或绕过`：否；未明确批准前不做一次性绕过。

## BDD 场景

- BDD: 构建发布包成功 -> Given 本机具备离线后端基础镜像 tar、目标配置和发布参数 / When 运行 build-release / Then 产出发布包、manifest/report、后端镜像 tar 或包内产物，并返回成功。
- BDD: 发布到目标环境成功 -> Given 已构建发布包且目标环境配置明确 / When 运行 deploy-release / Then 目标环境部署成功，部署日志和验证结果可追踪。
- BDD: 缺少前置条件必须阻塞 -> Given 目标 host、基础镜像、resource proof 或 target config 缺失 / When 构建或发布 / Then fail fast，错误说明缺失项和影响，不 fallback。

## Milestones

- [x] M1：审计构建/发布链路和服务器访问文档。
- [x] M2：修复本地功能缺口并完成回归测试。
- [x] M3：真实构建一次发布包。
- [x] M4：真实发布一次到授权目标环境。
- [x] M5：部署后验证、收尾预览和提交。

## Expected Verification

- 构建命令真实成功并记录产物路径。
- 发布命令真实成功并记录目标环境。
- 部署后验证能证明目标服务可用。
- 相关脚本/单元测试通过。

## Current Status

completed

## Final Verification

- 构建发布包成功：`20260606_smart_release_goal_0121`。
- NAS 发布包路径：`Backup/ReleasePackage/20260606_smart_release_goal_0121`。
- 测试服部署成功：`172.30.30.58`。
- 后端健康检查：`http://172.30.30.58:48081/actuator/health` 返回 HTTP 200，内容为 `{"status":"UP"}`。
- 前端入口：`http://172.30.30.58:8081/` 返回 HTTP 200。
- Website 展厅：`http://172.30.30.58:8083/showroom` 返回 HTTP 200。
- Java 配置契约测试：`mvn -pl yudao-module-infra -Dtest=RuntimeControlLocalConfigContractTest test` 通过。
