<template>
  <Dialog v-model="visible" title="主管待办交接" width="1080px">
    <el-alert
type="info" :closable="false" class="mb-16px"
      title="已有待办保留原处理人。请逐项确认接收人和原因；eDHR、DCC 及业务模块待办须从对应业务入口处理。" />
    <el-form inline @submit.prevent="search">
      <el-form-item label="原处理人编号">
        <el-input-number v-model="query.assigneeUserId" :min="1" :controls="false" placeholder="可查询已删除账号" />
      </el-form-item>
      <el-form-item><el-button @click="search">查询</el-button></el-form-item>
    </el-form>
    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="name" label="待办名称" min-width="150" />
      <el-table-column prop="processInstanceId" label="流程实例" min-width="160" show-overflow-tooltip />
      <el-table-column label="原处理人" min-width="160">
        <template #default="{ row }">
          {{ row.assigneeName || '账号不可用' }}（{{ row.assigneeUserId || '未分配' }}）
          <el-tag v-if="!row.assigneeAvailable" type="danger" size="small">不可用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="交接" min-width="230">
        <template #default="{ row }">
          <el-button
v-if="row.transferable" link type="primary" @click="selectTask(row)"
            v-hasPermi="['bpm:task:update']">选择接收人</el-button>
          <span v-else>{{ row.blocker }}</span>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="query.pageNo" v-model:limit="query.pageSize" @pagination="load" />
    <template v-if="selected">
      <el-divider />
      <el-form ref="form" :model="handover" :rules="rules" label-width="90px" v-loading="saving">
        <el-form-item label="待办">{{ selected.name }}</el-form-item>
        <el-form-item label="接收人" prop="assigneeUserId">
          <el-select v-model="handover.assigneeUserId" filterable placeholder="请选择接收人">
            <el-option
v-for="user in users" :key="user.id" :value="user.id"
              :label="user.nickname + '（' + user.username + '）'"
              :disabled="String(user.id) === selected.assigneeUserId" />
          </el-select>
        </el-form-item>
        <el-form-item label="交接原因" prop="reason">
          <el-input v-model="handover.reason" type="textarea" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :disabled="saving" @click="submit">确认交接</el-button>
          <el-button :disabled="saving" @click="selected = undefined">取消选择</el-button>
        </el-form-item>
      </el-form>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import request from '@/config/axios'
import * as UserApi from '@/api/system/user'
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
const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const rows = ref<HandoverTask[]>([])
const total = ref(0)
const selected = ref<HandoverTask>()
const users = ref<UserApi.UserVO[]>([])
const form = ref()
const query = reactive({ pageNo: 1, pageSize: 10, assigneeUserId: undefined as number | undefined })
const handover = reactive({ assigneeUserId: undefined as number | undefined, reason: '' })
const rules = {
  assigneeUserId: [{ required: true, message: '请选择接收人', trigger: 'change' }],
  reason: [{ required: true, whitespace: true, message: '请输入交接原因', trigger: 'blur' }]
}
const load = async () => {
  loading.value = true
  try {
    const page = await request.get({ url: '/bpm/task/supervisor-handover/page', params: query })
    rows.value = page.list
    total.value = page.total
    selected.value = undefined
  } finally {
    loading.value = false
  }
}
const search = async () => {
  query.pageNo = 1
  await load()
}
const open = async () => {
  visible.value = true
  query.pageNo = 1
  query.assigneeUserId = undefined
  selected.value = undefined
  await load()
}
const selectTask = async (task: HandoverTask) => {
  users.value = await UserApi.getSimpleUserList()
  handover.assigneeUserId = undefined
  handover.reason = ''
  selected.value = task
  await nextTick()
  form.value?.clearValidate()
}
const submit = async () => {
  if (!selected.value || !(await form.value.validate())) return
  saving.value = true
  try {
    await request.put({
      url: '/bpm/task/supervisor-handover',
      data: {
        id: selected.value.id,
        previousAssigneeUserId: Number(selected.value.assigneeUserId),
        assigneeUserId: handover.assigneeUserId,
        reason: handover.reason.trim()
      }
    })
    message.success('待办已交接，仍待接收人审批')
    await load()
  } finally {
    saving.value = false
  }
}
defineExpose({ open })
</script>
