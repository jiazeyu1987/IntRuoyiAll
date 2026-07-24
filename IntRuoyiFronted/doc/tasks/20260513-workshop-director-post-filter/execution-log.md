BDD: workshop owner selector only shows workshop directors -> Given the local environment has a `WORKSHOP_DIRECTOR` post with assigned users, When an admin opens the MES workshop form and chooses a responsible user, Then the selector only lists enabled users assigned to the `WORKSHOP_DIRECTOR` post.

BDD: workshop owner selection fails fast on missing workshop director configuration -> Given the workshop form depends on the workshop director candidate contract, When the required post or candidate data is missing, Then the UI surfaces the error instead of silently widening the candidate list.

RED: real Playwright flow on `http://127.0.0.1:8081/mes/md/workstation/workshop` before the frontend change -> FAIL, the add-workshop dialog `负责人` dropdown rendered the unrestricted user list, including unrelated users such as `芋道源码`, `张三`, and `李四`.

GREEN: `npx eslint src/views/mes/md/workstation/workshop/WorkshopForm.vue` -> PASS.

GREEN: real Playwright flow on temporary Vite instance `http://127.0.0.1:8084/mes/md/workstation/workshop` -> PASS, opening the add-workshop dialog and expanding `负责人` showed exactly 2 dropdown options after the page fetched `WORKSHOP_DIRECTOR` plus paged `system/user/page` data from `127.0.0.1:48081`.
