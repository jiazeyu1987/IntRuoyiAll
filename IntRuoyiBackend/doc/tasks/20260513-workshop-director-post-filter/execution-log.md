BDD: workshop owner candidates are limited to workshop directors -> Given an admin maintains system posts and MES workshops, When the admin opens the workshop form to choose a responsible user, Then only enabled users assigned to the `WORKSHOP_DIRECTOR` post are available to choose.

BDD: workshop director post and user assignments exist in live data -> Given the local environment already contains users `wuxiaolei` and `guliya`, When the task provisions the `WORKSHOP_DIRECTOR` post and assigns those users to it, Then the post exists once, both users are attached to it, and missing users or write failures surface as explicit errors.

RED: `mvn -pl yudao-module-mes -am -Dtest=MesMdWorkshopControllerChargeUserListTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, the reactor stops in `yudao-module-system` on pre-existing `framework.security` compilation errors before the new MES controller test can compile.

GREEN: authenticated live admin API against `http://127.0.0.1:48081/admin-api` -> PASS, post `WORKSHOP_DIRECTOR` exists with id `8`, and users `539/wuxiaolei` and `1314/guliya` both return `postIds: [8]`.

GREEN: `mvn -pl yudao-module-mes -am -Dtest=MesMdWorkshopControllerChargeUserListTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, the current workspace now compiles through `yudao-module-system` and the new MES controller regression test passes with 2/2 tests green.
