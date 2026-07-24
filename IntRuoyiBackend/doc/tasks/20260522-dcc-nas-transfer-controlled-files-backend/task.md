# 任务：DCC NAS目录转移后端实现

## Goal

在当前 `IntRuoyi` 的 `infra + DCC` 后端中实现 NAS 目录转移到 DCC 受控目录/受控文件的能力，并同步放开 DCC 受控文件链路的非 PDF 支持。

本任务必须满足：

- 新增 DCC NAS 批量转移接口
- DCC 后端可按 NAS 相对路径读取文件字节和元信息
- 目录可按同名相对路径自动复用或创建
- 类别可按“有文件的目录”复用或创建，并从模板类别克隆治理配置
- DCC 受控文件提交流程支持非 PDF 入库
- 预览分流支持 `PDF / IMAGE / TEXT / OFFICE / DOWNLOAD_ONLY`
- Office 预览走 OnlyOffice，只读 token 化访问

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-dcc\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-dcc-nas-transfer-controlled-files-backend\**`

## Non-Scope

- 不修改旧 `IntDCC` Python 服务
- 不删除 NAS 源文件
- 不对未知二进制文件承诺在线预览

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-nas-share-root-to-quality-system-files\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: NAS 管理页当前共享根已切到 `\\172.30.30.4\质量体系文件`，本任务基于该运行态继续扩展后端能力。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在与 MES / showroom 无关本任务的大量用户改动
- Impact: 本任务只修改 `yudao-module-infra`、`yudao-module-dcc` 与本任务文档，避免混入其他功能域

## Milestones

- [x] M1: 创建任务文档并确认上一同仓任务完成状态
- [x] M2: 记录 BDD 与 RED，锁定 NAS 文件读取、DCC 批量转移、非 PDF 支持与预览分流契约
- [x] M3: 扩展 infra NAS 文件读取能力并补测试
- [x] M4: 实现 DCC NAS 批量转移、类别模板克隆、非 PDF 提交与预览元信息接口
- [x] M5: 跑后端定向验证、证据校验和 closeout preview

## Expected Verification

- `mvn -pl yudao-module-infra -Dtest=NasBrowserServiceImplTest,FileControllerTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-dcc -Dtest=DccControlledFileUploadApiTest,DccDirectoryAdminServiceImplTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260522-dcc-nas-transfer-controlled-files-backend/backend-api-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-dcc-nas-transfer-controlled-files-backend --mode preview`

## Current Status

Completed on 2026-05-22. 后端实现、变更记录、定向验证、证据校验和 closeout preview 已完成；`NAS 转移不走审批，且跳过分发/培训` 已实现并完成真实联调。当前本地运行态对 `1. QMS documents/PD可编辑` 的真实转移结果为 `createdFileCount=4`、`failedFileCount=0`。

## Blockers And Impact

- Blocker: 本地 `http://127.0.0.1:8082` 没有可访问的 OnlyOffice 服务
- Impact:
  - Office 预览代码与 fail-fast 校验已完成，但本机无法完成真实 OnlyOffice 预览联调；这不阻塞 NAS 转移成功
