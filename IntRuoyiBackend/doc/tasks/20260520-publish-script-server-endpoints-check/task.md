# 任务：核对发布脚本中的测试与正式服务器地址

## 目标

检查 `ruoyi-vue-pro` 仓库中的发布脚本，确认真实测试服务器、正式服务器地址以及对应的部署目录和入口脚本，并用可读方式说明给用户。

## 范围

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-publish-script-server-endpoints-check\**`

## 非范围

- 不修改发布脚本
- 不执行真实发布
- 不变更服务器配置

## 上一任务检查

- 上一任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-backup-ops-v1-review-loop\task.md`
- 状态：`completed`
- 说明：上一任务已完成备份脚本骨架与评审闭环，本任务仅做发布脚本只读核对。

## 里程碑

- [x] M1：创建任务文档并确认只读范围。
- [x] M2：检查测试/正式发布脚本中的服务器地址与目录。
- [x] M3：整理入口脚本与对应环境关系。
- [x] M4：更新记录并向用户说明。

## 预期验证

- 能明确指出测试服务器 host、正式服务器 host、远端目录、入口 BAT/PS1 文件

## 当前状态

Completed.

## 最终验证结果

- PASS：确认测试服务器在发布脚本中固定为 `172.30.30.58`
- PASS：确认正式服务器在发布脚本中固定为 `172.30.30.57`
- PASS：确认两套环境的远端运行目录都为 `/opt/intruoyi/runtime`
- PASS：确认统一入口脚本 `运维工具.bat` 已分别指向测试发布和正式发布 BAT 包装器

## 结果说明

- 本次为只读核对任务，未修改任何发布脚本
