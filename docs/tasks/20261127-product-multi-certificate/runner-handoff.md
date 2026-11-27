# 测试58精确回填执行包交接

2026-11-27已封存私密目录 `C:/SpugHandoff/release/private/test58-preserved-recovery-plan-20261127/runner-v1/`，完整调用见该目录 `HANDOFF.md`。交付后不修改；后续更正须新版本并重新测试。仓库不保存逐证业务数据、私密回执或凭据。

## 固定摘要

| 文件 | 原始字节SHA256 |
| --- | --- |
| carrier-seal.json | 2c5fa0d9c9ffea252b5b69762ce9399b5a0170c4c6d8198753db5ef9adf3f9cd |
| run.py | 03b3619427ebf96acafe6c4324031a8dee056b1c648506a3a67853d1f49402a0 |
| worker.py | 11caad33e3dcef94a57fc5c4c1bdd4412aa2cbb5afd4a2962eb9cdd20ddcec45 |
| inputs-manifest.json | 5e00e85a3e48b7e6da45d558692d647311e2310a814c1e110d809ee93b6722f6 |
| test-evidence.json | 2b1498528ad0df7d1252ab6bc0980085ae8248fbc4a34493e0d837bd1b963525 |

精确增量SQL原始1,027,444字节，SHA256 `deafd09d069613230a7b283c63e17666c68431adf9db2d6090cb9905b551cab6`；规范到LF后实际执行1,027,024字节，SHA256 `40e29590cafcf75b062cee2e4e7e1ae7ba18c0f55c80d978bf35a09b0b33a377`。入口同时校验这两种字节，不能混用。

## 调用与安全边界

仅固定test58：`verify-bundle → inspect-current → 正式recover-release → accept-recovery → inspect-original → quiesce → apply → inspect-completed → resume-writers → 正式observe/precheck → 新包发布`。恢复后的入口动作均绑定恢复receipt.jobId；不是原失败任务ID，也不是accept操作任务ID。

quiesce保存精确容器ID、镜像和原运行状态，先停前端，再停全部冻结注册writers/后端，确认SQL和事务排空；不停止数据库。apply恢复原修复的ID、时间和完整内容，不改账本；恢复服务时只启动原先运行的同一容器，前端最后。原修复为165主档、169快照、16受托企业及169审计。

数据库增量会话通过唯一令牌、连接ID、server UUID追踪，发送增量前先将连接身份fsync入journal。Docker客户端超时不等于MySQL结束：只有精确会话消失且活动SQL、事务为零才恢复原保护并分类。未证明则30，保留维护保护和停写；原worker结束后仅用reconcile只读分类及恢复已知保护，不重放SQL。

0表示动作通过；10表示原始态、未应用；20表示不一致/阻塞；30表示结果或会话未知。10/20/30均不得继续恢复writers或盲目重放apply。无持久化连接身份时不猜测、不自动KILL，会明确阻断等待查明。

## 已验证范围

8项集成测试通过，详见同目录 `runner-verification.json`。原始相关检查点在隔离MySQL8.0.39恢复，network=none、无公开端口；实际SQL验证完整回填、全事务回滚、真实COMMIT后回执丢失、触发器交接中断、原始行变化拒绝、57目标拒绝。真实超时实验让容器MySQL在事务内延迟COMMIT并结束Docker客户端：会话关闭前apply/reconcile均返回30，关闭后才完整核对完成态及恢复保护。

恢复验收回执与应用容器编排为测试替身，业务行、事务、保护及MySQL会话是真实隔离数据库；不能据此宣称真实58恢复、服务健康或E2E通过。

维护线程随后回报：真实58的verify-bundle与inspect-current通过，当前修复完成态全字段一致、preflight PASS、writers停止；正式recover-release `JOB-a727c820e0` 已启动，绑定原失败任务 `JOB-a2775541ef` 和原tag `2026-11-26-23-54-25`。这是维护侧状态回报，本线程未再次连接58；恢复与后续回填仍由维护线程串行完成。
