<template>
  <UserSelectDialogV2
    ref="pickerRef"
    :title="`分配权限角色：${role?.name ?? ''}`"
    align-center
    :assigned-role-id="role?.id"
    allow-unassign
    :assignment-saving="saving"
    multiple
    :min="1"
    @selected="submitForm"
    @unassign="unassignUsers"
  />
</template>

<script setup lang="ts">
import { h } from 'vue'
import { ElMessageBox } from 'element-plus'
import type { RoleVO } from '@/api/system/role'
import * as PermissionApi from '@/api/system/permission'
import UserSelectDialogV2 from '@/views/system/user/components/UserSelectDialogV2.vue'
import type { UserOption } from '@/views/system/user/components/userPicker'
import { confirmUserChange } from '@/views/system/user/user-change-confirmation'

const message = useMessage()
const saving = ref(false)
const role = ref<RoleVO>()
const pickerRef = ref<InstanceType<typeof UserSelectDialogV2>>()

const open = async (row: RoleVO) => {
  if (saving.value) return
  role.value = row
  await nextTick()
  if (!pickerRef.value) throw new Error('人员选择器尚未就绪')
  await pickerRef.value.open()
}

const emit = defineEmits<{ success: [] }>()
const submitForm = (rows: UserOption[]) => changeUserRoles(rows, false)
const unassignUsers = (rows: UserOption[]) => changeUserRoles(rows, true)
const changeUserRoles = async (rows: UserOption[], removing: boolean) => {
  if (saving.value) return
  if (!role.value) throw new Error('分配角色尚未就绪')
  let assigned = 0
  let skipped = 0
  let targetLabel = ''
  const action = removing ? '解除' : '分配'
  saving.value = true
  try {
    if (!rows.length || rows.some(user => !Number.isSafeInteger(Number(user.id)) || Number(user.id) <= 0)) {
      throw new Error('请选择有效用户')
    }
    const users = [...new Map(rows.map(user => [Number(user.id), { ...user, id: Number(user.id) }])).values()]
    const roleId = role.value.id
    const content = h('div', { class: 'role-assignment-summary' }, [
      h('div', { class: 'role-assignment-summary__section' }, [
        h('div', { class: 'role-assignment-summary__label' }, '权限'),
        h('div', { class: 'role-assignment-summary__role' }, role.value.name)
      ]),
      h('div', { class: 'role-assignment-summary__section' }, [
        h('div', { class: 'role-assignment-summary__label' }, '人员'),
        h('div', { class: 'role-assignment-summary__names' }, users.map(user =>
          h('span', { class: 'role-assignment-summary__name', key: user.id }, user.nickname)
        ))
      ])
    ])
    const confirmed = await confirmUserChange(
      (content, title) => ElMessageBox.confirm(content, title, {
        customClass: 'role-assignment-confirmation',
        modalClass: 'app-confirm-message-box-overlay',
        confirmButtonText: `确认${action}`,
        cancelButtonText: '取消',
        closeOnClickModal: false
      }),
      content,
      `确认${action}权限`
    )
    if (!confirmed) return
    // 确认后读取现有角色，沿用用户管理的正式授权及审计接口。
    for (const user of users) {
      const userId = user.id
      targetLabel = `${user.nickname}（${user.username}，ID:${userId}）`
      const roleIds: number[] = await PermissionApi.getUserRoleList(userId)
      if (removing ? !roleIds.includes(roleId) : roleIds.includes(roleId)) {
        pickerRef.value?.markRoleAssignment(userId, !removing)
        skipped++
        continue
      }
      await PermissionApi.assignUserRole({
        userId,
        roleIds: removing ? roleIds.filter(id => id !== roleId) : [...roleIds, roleId],
        // 现有接口要求审计说明，记录实际分配动作，无需用户填写原因。
        reason: removing
          ? `权限角色页面解除用户（ID:${userId}）的角色（ID:${roleId}）`
          : `权限角色页面分配角色（ID:${roleId}）给用户（ID:${userId}）`,
        idempotencyKey: `SYSTEM-PERM-USER-ROLE-${userId}-${crypto.randomUUID()}`
      })
      assigned++
      pickerRef.value?.markRoleAssignment(userId, !removing)
    }
    if (!assigned) {
      message.info(removing ? '所选用户均未拥有此权限角色，无需解除' : '所选用户均已拥有此权限角色，无需重复分配')
      return
    }
    message.success(`成功${action} ${assigned} 人${skipped ? `，${skipped} 人${removing ? '未拥有' : '已拥有'}该角色，已跳过` : ''}`)
  } catch (error) {
    if (targetLabel) {
      message.error(`为${targetLabel}${action}失败，已成功${action} ${assigned} 人，已跳过 ${skipped} 人；后续${action}已停止，请重试。`)
    }
    throw error
  } finally {
    saving.value = false
    if (assigned) emit('success')
  }
}

defineExpose({ open })
</script>

<style lang="scss">
.role-assignment-confirmation {
  width: min(460px, calc(100vw - 32px));
  padding: 24px;
  border-radius: 12px;

  .el-message-box__header {
    padding: 0 24px 20px 0;
  }

  .el-message-box__title {
    font-size: 18px;
    font-weight: 600;
  }

  .el-message-box__content {
    padding: 0;
  }

  .el-message-box__message {
    width: 100%;
  }

  .el-message-box__btns {
    padding: 24px 0 0;
    gap: 8px;
  }

  .role-assignment-summary {
    display: grid;
    gap: 20px;
    max-height: 50vh;
    overflow-y: auto;
    overflow-wrap: anywhere;
  }

  .role-assignment-summary__label {
    margin-bottom: 8px;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }

  .role-assignment-summary__role {
    padding: 12px 16px;
    border-left: 3px solid var(--el-color-primary);
    border-radius: 4px;
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    font-size: 16px;
    font-weight: 600;
    line-height: 1.6;
  }

  .role-assignment-summary__names {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .role-assignment-summary__name {
    max-width: 100%;
    padding: 6px 12px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 6px;
    background: var(--el-fill-color-light);
    color: var(--el-text-color-primary);
    line-height: 1.5;
  }
}
</style>
