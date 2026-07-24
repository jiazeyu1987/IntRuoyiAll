# 任务：展厅发布读回 origin 按环境配置

## 目标

将展厅发布读回 `showroom.release.public-website-origin` 的本机默认值放回后端环境配置，避免根目录启动脚本硬编码业务配置；服务器部署继续按自身环境提供公开站点入口。

## 里程碑

- [x] M1：补充回归测试，锁定 `restart-ruoyi.bat` 不携带该业务参数。
- [x] M2：在 `application-local.yaml` 配置本机默认值。
- [x] M3：验证测试通过并记录证据。
- [x] M4：提交本任务相关后端改动。

## 预期验证

- `python -m pytest script/tests/test_restart_ruoyi_script.py -q`

## 当前状态

已完成。`application-local.yaml` 提供本机默认读回 origin，根脚本和后端本机运行脚本均不再通过命令行硬编码该业务参数；服务器部署配置继续按自身环境传入公开站点入口。
