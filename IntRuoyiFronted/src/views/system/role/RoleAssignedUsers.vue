<template>
  <Dialog v-model="visible" title="权限角色已分配人员" width="850px">
    <div class="mb-4">
      权限角色：<el-tag>{{ roleName }}</el-tag>
      <p class="mb-0 text-13px text-gray-500">按当前账号的数据权限展示，包含已停用账号。</p>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="mb-4" />
    <el-table
      v-loading="loading"
      :data="users"
      border
      max-height="420"
      :empty-text="error ? '人员列表加载失败' : '暂无已分配人员'"
    >
      <el-table-column label="用户账号" prop="username" min-width="160" />
      <el-table-column label="姓名" prop="nickname" min-width="120" />
      <el-table-column label="所属部门" prop="deptName" min-width="180" />
      <el-table-column label="账号状态" prop="status" width="110" align="center">
        <template #default="{ row }">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="row.status" />
        </template>
      </el-table-column>
    </el-table>
    <div class="overflow-hidden">
      <Pagination
        v-model:page="query.pageNo"
        v-model:limit="query.pageSize"
        :total="total"
        storage-key="system.role.assigned-users"
        @pagination="getList"
      />
    </div>
    <template #footer>
      <el-button :loading="loading" @click="getList">刷新</el-button>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { DICT_TYPE } from '@/utils/dict'
import * as UserApi from '@/api/system/user'
import type { RoleVO } from '@/api/system/role'

const visible = ref(false)
const loading = ref(false)
const error = ref('')
const roleName = ref('')
const roleId = ref<number>()
const users = ref<UserApi.UserListVO[]>([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 20 })
let requestId = 0

const getList = async () => {
  if (roleId.value === undefined) throw new Error('缺少待查看的权限角色编号')
  const currentRequest = ++requestId
  const params = { ...query, roleId: roleId.value }
  loading.value = true
  error.value = ''
  users.value = []
  try {
    const data = await UserApi.getUserPage(params)
    if (currentRequest !== requestId) return
    users.value = data.list
    total.value = data.total
  } catch (cause) {
    if (currentRequest === requestId) error.value = '已分配人员加载失败，请点击刷新重试。'
    throw cause
  } finally {
    if (currentRequest === requestId) loading.value = false
  }
}

const open = async (role: RoleVO) => {
  roleId.value = role.id
  roleName.value = role.name
  query.pageNo = 1
  total.value = 0
  visible.value = true
  await nextTick()
  await getList()
}

defineExpose({ open })
</script>
