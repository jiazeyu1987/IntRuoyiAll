<template>
  <Dialog
    v-model="visible"
    title="确认主管待办交接"
    width="1080px"
    :close-on-click-modal="false"
    :close-on-press-escape="!saving"
    :show-close="!saving"
  >
    <el-alert :title="context?.notice" type="info" :closable="false" class="mb-12px" />
    <el-alert
      title="待办仅在确认后转交，接收人仍须审批。eDHR、DCC及业务模块待办须到对应业务入口交接。"
      type="warning"
      :closable="false"
      class="mb-12px"
    />
    <div class="mb-12px"
      >原处理人：{{ context?.assigneeName }}（{{ context?.assigneeUserId }}）</div
    >
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="mb-12px" />
    <el-table :data="rows" v-loading="loading || saving" empty-text="该范围内没有未完成待办">
      <el-table-column prop="name" label="待办名称" min-width="150" />
      <el-table-column
        prop="processInstanceId"
        label="流程实例"
        min-width="160"
        show-overflow-tooltip
      />
      <el-table-column label="原处理人" min-width="160">
        <template #default="{ row }">
          {{ row.assigneeName || '账号不可用' }}（{{ row.assigneeUserId }}）
          <el-tag v-if="!row.assigneeAvailable" type="danger" size="small">不可用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="交接" min-width="230">
        <template #default="{ row }">
          <el-button
            v-if="row.transferable"
            link
            type="primary"
            :disabled="saving"
            @click="selectTask(row)"
            v-hasPermi="['bpm:task:update']"
            >确认接收人</el-button
          >
          <span v-else>{{ row.blocker }}</span>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="pageNo"
      v-model:limit="pageSize"
      :disabled="saving"
      @pagination="load"
    />
    <template v-if="selected">
      <el-divider />
      <el-form ref="form" :model="handover" :rules="rules" label-width="90px" :disabled="saving">
        <el-form-item label="待办">{{ selected.name }}</el-form-item>
        <el-form-item label="接收人" prop="assigneeUserId">
          <UserSelectV2
            v-model="handover.assigneeUserId"
            :user-options="users"
            :disabled-ids="[
              selected.assigneeUserId,
              ...(context?.applicantUserId ? [context.applicantUserId] : [])
            ]"
            :disabled="Boolean(context?.suggestedAssigneeUserId) || saving"
            placeholder="请选择接收人"
          />
        </el-form-item>
        <el-form-item label="交接原因" prop="reason">
          <el-input v-model="handover.reason" type="textarea" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submit">确认交接</el-button>
          <el-button :disabled="saving" @click="selected = undefined">取消选择</el-button>
        </el-form-item>
      </el-form>
    </template>
    <template #footer>
      <el-button :disabled="loading || saving || !canQuery" @click="load">刷新待办</el-button>
      <el-button :disabled="saving" @click="visible = false">{{
        total ? '稍后交接' : '关闭'
      }}</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import request from '@/config/axios'
import * as UserApi from '@/api/system/user'
import { checkPermi } from '@/utils/permission'
import type { SupervisorHandoverContext } from './supervisor-handover'
interface HandoverTask {
  id: string
  name: string
  processInstanceId: string
  assigneeUserId: string
  assigneeName?: string
  assigneeAvailable: boolean
  transferable: boolean
  blocker?: string
}
const message = useMessage()
const visible = ref(false),
  loading = ref(false),
  saving = ref(false)
const error = ref('')
const context = ref<SupervisorHandoverContext>()
const rows = ref<HandoverTask[]>([])
const total = ref(0),
  pageNo = ref(1),
  pageSize = ref(10)
const selected = ref<HandoverTask>()
const users = ref<UserApi.UserVO[]>([])
const form = ref()
const canQuery = computed(() => checkPermi(['bpm:task:manager-query']))
const handover = reactive({ assigneeUserId: undefined as number | string | undefined, reason: '' })
const rules = {
  assigneeUserId: [{ required: true, message: '请选择接收人', trigger: 'change' }],
  reason: [{ required: true, whitespace: true, message: '请输入交接原因', trigger: 'blur' }]
}
let generation = 0
const load = async () => {
  if (!context.value || !canQuery.value) {
    error.value = '当前账号没有待办管理查询权限，请联系有权限的管理员办理交接'
    return
  }
  const current = ++generation
  loading.value = true
  error.value = ''
  rows.value = []
  selected.value = undefined
  try {
    const page = await request.get({
      url: '/bpm/task/supervisor-handover/page',
      params: {
        pageNo: pageNo.value,
        pageSize: pageSize.value,
        assigneeUserId: context.value.assigneeUserId,
        applicantUserId: context.value.applicantUserId
      }
    })
    if (current !== generation) return
    rows.value = page.list
    total.value = page.total
    if (!page.list.length && page.total > 0 && pageNo.value > 1) {
      pageNo.value--
      await load()
    }
  } catch (cause) {
    if (current === generation)
      error.value =
        '待办查询失败，请刷新重试：' + String(cause instanceof Error ? cause.message : cause)
  } finally {
    if (current === generation) loading.value = false
  }
}
const open = async (value: SupervisorHandoverContext) => {
  context.value = value
  pageNo.value = 1
  total.value = 0
  rows.value = []
  selected.value = undefined
  visible.value = true
  await load()
}
const selectTask = async (task: HandoverTask) => {
  const current = generation
  const candidates = await UserApi.getSimpleUserList()
  if (current !== generation || !visible.value) return
  users.value = candidates
  handover.assigneeUserId = context.value?.suggestedAssigneeUserId
  handover.reason = context.value?.reason || ''
  selected.value = task
  await nextTick()
  form.value?.clearValidate()
}
const submit = async () => {
  if (saving.value || !selected.value || !(await form.value.validate())) return
  saving.value = true
  error.value = ''
  try {
    await request.put({
      url: '/bpm/task/supervisor-handover',
      data: {
        id: selected.value.id,
        previousAssigneeUserId: selected.value.assigneeUserId,
        assigneeUserId: handover.assigneeUserId,
        applicantUserId: context.value?.applicantUserId,
        reason: handover.reason.trim()
      }
    })
    message.success('待办已交接，仍待接收人审批')
    await load()
  } catch (cause) {
    error.value = '交接未完成：' + String(cause instanceof Error ? cause.message : cause)
  } finally {
    saving.value = false
  }
}
watch(visible, (value) => {
  if (!value) generation++
})
defineExpose({ open })
</script>
