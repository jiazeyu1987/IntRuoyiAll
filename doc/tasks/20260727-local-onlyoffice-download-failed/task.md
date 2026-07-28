# 本地受控浏览 OnlyOffice 下载失败

## Task Goal

诊断并修复本地访问受控浏览详情时 OnlyOffice 显示 `错误码 -4，下载失败` 的原因；范围限定为本地 `int_main` 运行态、受控预览元数据、OnlyOffice 文档下载 URL 和 token 校验链路。

## Milestones

- [x] 读取本地运行态、后端、前端、E2E 与缺陷修复规则，确认任务边界。
- [x] 复现或定位本地 OnlyOffice `-4 下载失败` 的真实失败接口、日志或配置。
- [x] 增加最小回归测试或静态契约，先证明当前失败。
- [x] 实施最小修复，不引入 fallback、吞异常或 mock 成功。
- [x] 运行目标验证，并记录 RED/GREEN/REGRESSION 证据。

## Expected Verification

- 记录本地前端、后端和 OnlyOffice 文档下载 URL 的实际值。
- 证明失败来源是下载接口、token、网络可达性、配置或运行态未加载之一。
- 修复后目标测试通过；如需要本地页面验证，使用真实前端路径。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；本地 OnlyOffice 容器下载 URL 改为容器可访问的 Windows Host 地址，保留浏览器访问 OnlyOffice 的本机 `127.0.0.1:8080`。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 本地运行态门禁：`int_main` 本地前端固定 `8081`，后端固定 `48081`，不得随机换端口；端口归属不明时 fail fast。
- 后端门禁：接口和服务错误必须通过真实响应、日志或测试暴露，不得默认成功或隐藏下载失败。

## Completion Notes

- 本地代码、Jar 和 `48081` 运行态已修复并验证；测试服务器未发布，本地修复不要求发布测试服务器。
- 分支当前仍存在其他任务的未提交文件，且 `int_main` ahead origin；按项目 Git 规则，最终 closeout/推送需在清理并确认并发改动后执行。
