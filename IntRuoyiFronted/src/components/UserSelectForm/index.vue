<template>
  <UserSelectDialogV2 ref="dialog" v-bind="$attrs" multiple :min="1" @selected="onSelected" />
</template>
<script setup lang="ts">
import UserSelectDialogV2 from '@/views/system/user/components/UserSelectDialogV2.vue'
import type { UserOption } from '@/views/system/user/components/userPicker'
defineOptions({ name: 'UserSelectForm', inheritAttrs: false })
const emit = defineEmits<{ confirm: [id: any, userList: any[]] }>()
const dialog = ref<InstanceType<typeof UserSelectDialogV2>>()
const open = (id: any, selectedList: UserOption[] = []) =>
  dialog.value?.open(
    selectedList.map((row) => row.id),
    [],
    id,
    selectedList
  )
const onSelected = (rows: UserOption[], id: any) => emit('confirm', id, rows)
defineExpose({ open })
</script>
