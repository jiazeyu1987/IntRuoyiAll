# 执行日志：批量重置非管理员用户密码为 111111

BDD: 非管理员用户密码批量重置 -> Given 当前运行库存在管理员账号 `admin` 与若干普通用户 and 系统密码规则使用 BCrypt, When 执行排除管理员的批量密码重置为 `111111`, Then 所有非管理员用户都应写入新的 BCrypt 密码哈希且管理员密码保持不变。

RED: `GET /admin-api/system/user/page?pageNo=1&pageSize=500` -> FAIL, 每页条数最大值为 200，说明必须先按实际分页上限盘点用户。

GREEN: `POST /admin-api/system/auth/login` with `tenant-id: 1` and `admin/admin123` -> PASS.

GREEN: `PUT /admin-api/system/user/update-password` for `2121` non-admin users -> PASS, all requests returned `code=0`.

GREEN: `POST /admin-api/system/auth/login` with `tenant-id: 1` and `zhangsan/111111`, `dccquery/111111`, `A4020063/111111` -> PASS.

GREEN: `POST /admin-api/system/auth/login` with `tenant-id: 1` and `admin/111111` -> PASS as a negative check, returned `登录失败，账号密码不正确`, confirming admin password stayed unchanged.
