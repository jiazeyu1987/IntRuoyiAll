# 启动 int_main 本地后端

## 任务目标

在 `E:\IntRuoyi` 启动 `int_main` 本地后端，固定监听 `48081`，并验证健康检查为 `UP`。

## 里程碑

- [x] 核对运行规则、端口占用和进程归属
- [x] 确认后端已启动且监听进程属于 `E:\IntRuoyi\IntRuoyiBackend`
- [x] 验证 `/actuator/health` 返回 `UP`
- [x] 完成 cleanup preview/apply
- [ ] 提交并推送任务记录

## 预期验证

- `48081` 监听进程命令行可确认属于当前 `int_main` 后端
- `http://127.0.0.1:48081/actuator/health` 返回 `status=UP`

## Current Status

ready_for_closeout

Git 收尾阻塞：`int_main` 当前存在多个并行任务的未提交改动，且与本任务共享分支。按任务所有权规则不能将这些非本任务改动制作成脏工作区基线提交，也不能跳过基线门禁直接提交本任务记录，因此暂不标记 `completed`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是；按固定运行端口和正式本地配置启动，不修改配置绕过前置条件
- `是否存在临时补丁或绕过`：否

## 经验门禁

- 启动前确认 `48081` 占用；未知进程或其他 runtime profile 占用时立即阻塞，不强杀、不换端口。
- 后端必须使用 `E:\IntRuoyi\IntRuoyiBackend` 的正式本地数据源配置；数据库认证或连接失败时不得声明启动成功。
- 长期运行进程的 stdout/stderr 写入稳定运行目录，不写入本任务 cleanup 候选目录。
- 成功标准为监听进程归属正确且健康检查返回 `UP`，不能仅依据进程存在。
