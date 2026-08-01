# 生产组长职责文档写入桌面目录

## Task Goal

将最新统一后的“生产组长未来一天的系统操作”写入 `C:\Users\BJB110\Desktop\文档\职责\生产组长.md`。

## Milestones

- [x] 读取任务收尾和 PowerShell UTF-8 规则。
- [x] 确认目标目录存在且目标文件尚不存在。
- [x] 写入生产组长职责 Markdown 文件。
- [x] 验证 UTF-8 读取和关键口径。

## Expected Verification

- 使用 UTF-8 读取 `C:\Users\BJB110\Desktop\文档\职责\生产组长.md`。
- 检查关键口径：本地生产订单列表、纸质订单编号、本地调拨单列表、纸质调拨单号、活跃订单、FIFO、生产系数、设备台账、上下限不覆盖员工原始提交。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按用户最新统一口径写入职责文档。
- `是否存在临时补丁或绕过`：否。
