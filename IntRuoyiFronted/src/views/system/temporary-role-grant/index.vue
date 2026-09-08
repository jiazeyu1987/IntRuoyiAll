<template>
  <ContentWrap>
    <div class="mb-12px text-12px text-[var(--el-text-color-secondary)]">
      临时角色授权：用于紧急场景的限时角色授予，必须填写原因和有效截止时间，并保留审批、撤销、过期与使用记录。
    </div>
    <el-form ref="queryFormRef" :model="queryParams" class="temporary-role-grant-toolbar" label-position="top" @submit.prevent>
      <div class="temporary-role-grant-toolbar__filters">
        <el-form-item label="被授权用户" prop="userId">
          <el-select v-model="queryParams.userId" clearable filterable placeholder="请选择用户">
            <el-option v-for="user in userOptions" :key="user.id" :label="user.nickname || user.username" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="临时角色" prop="roleId">
          <el-select v-model="queryParams.roleId" clearable filterable placeholder="请选择角色">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" clearable placeholder="请选择状态">
            <el-option label="待审批" value="PENDING" />
            <el-option label="有效中" value="ACTIVE" />
            <el-option label="已撤销" value="REVOKED" />
            <el-option label="已过期" value="EXPIRED" />
          </el-select>
        </el-form-item>
      </div>
      <div class="temporary-role-grant-toolbar__actions">
        <el-button @click="handleQuery"><Icon class="mr-5px" icon="ep:search" />搜索</el-button>
        <el-button @click="resetQuery"><Icon class="mr-5px" icon="ep:refresh" />重置</el-button>
        <el-button v-hasPermi="['system:temporary-role-grant:create']" plain type="primary" @click="openCreateDialog">
          <Icon class="mr-5px" icon="ep:plus" />新增临时授权
        </el-button>
      </div>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="被授权用户" min-width="130">
        <template #default="{ row }">{{ userNameMap.get(row.userId) || row.userId }}</template>
      </el-table-column>
      <el-table-column label="临时角色" min-width="140">
        <template #default="{ row }">{{ roleNameMap.get(row.roleId) || row.roleId }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="100">
        <template #default="{ row }"><el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="原因" prop="reason" min-width="220" show-overflow-tooltip />
      <el-table-column label="申请时间" prop="applyTime" width="170" />
      <el-table-column label="生效时间" prop="effectiveTime" width="170" />
      <el-table-column label="有效截止时间" prop="expireTime" width="170" />
      <el-table-column label="操作" fixed="right" width="230">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" v-hasPermi="['system:temporary-role-grant:approve']" link type="primary" @click="handleApprove(row)">审批通过</el-button>
          <el-button v-if="row.status === 'PENDING' || row.status === 'ACTIVE'" v-hasPermi="['system:temporary-role-grant:revoke']" link type="danger" @click="openRevokeDialog(row)">撤销</el-button>
          <el-button v-hasPermi="['system:temporary-role-grant:query']" link type="primary" @click="openAuditDialog(row)">审计记录</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <Dialog v-model="createDialogVisible" title="新增临时角色授权" width="620px">
    <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="120px">
      <el-form-item label="被授权用户" prop="userId">
        <el-select v-model="createForm.userId" filterable placeholder="请选择用户">
          <el-option v-for="user in userOptions" :key="user.id" :label="user.nickname || user.username" :value="user.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="临时角色" prop="roleId">
        <el-select v-model="createForm.roleId" filterable placeholder="请选择角色">
          <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="有效截止时间" prop="expireTime">
        <el-date-picker v-model="createForm.expireTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择有效截止时间" />
      </el-form-item>
      <el-form-item label="授权原因" prop="reason">
        <el-input v-model="createForm.reason" :rows="3" maxlength="500" show-word-limit type="textarea" placeholder="请填写紧急处理原因" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createDialogVisible = false">取消</el-button>
      <el-button :loading="submitLoading" type="primary" @click="handleCreate">提交申请</el-button>
    </template>
  </Dialog>

  <Dialog v-model="revokeDialogVisible" title="撤销临时角色授权" width="520px">
    <el-form ref="revokeFormRef" :model="revokeForm" :rules="revokeRules" label-width="90px">
      <el-form-item label="撤销原因" prop="reason">
        <el-input v-model="revokeForm.reason" :rows="3" maxlength="500" show-word-limit type="textarea" placeholder="请填写撤销原因" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="revokeDialogVisible = false">取消</el-button>
      <el-button :loading="submitLoading" type="danger" @click="handleRevoke">确认撤销</el-button>
    </template>
  </Dialog>

  <Dialog v-model="auditDialogVisible" title="临时角色授权审计记录" width="760px">
    <el-table :data="auditList">
      <el-table-column label="事件" prop="eventType" width="110" />
      <el-table-column label="权限标识" prop="permissionCode" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作人" prop="operatorUsername" width="120" />
      <el-table-column label="说明" prop="message" min-width="180" show-overflow-tooltip />
      <el-table-column label="时间" prop="createTime" width="170" />
    </el-table>
  </Dialog>
</template>

<script setup lang="ts">
import * as TemporaryRoleGrantApi from '@/api/system/temporaryRoleGrant'
import * as RoleApi from '@/api/system/role'
import * as UserApi from '@/api/system/user'

const message = useMessage()

const loading = ref(false)
const submitLoading = ref(false)
const total = ref(0)
const list = ref<TemporaryRoleGrantApi.TemporaryRoleGrantVO[]>([])
const auditList = ref<TemporaryRoleGrantApi.TemporaryRoleGrantAuditVO[]>([])
const roleOptions = ref<RoleApi.RoleVO[]>([])
const userOptions = ref<UserApi.UserVO[]>([])
const queryFormRef = ref()
const createFormRef = ref()
const revokeFormRef = ref()
const createDialogVisible = ref(false)
const revokeDialogVisible = ref(false)
const auditDialogVisible = ref(false)
const selectedGrant = ref<TemporaryRoleGrantApi.TemporaryRoleGrantVO>()

const queryParams = reactive({ pageNo: 1, pageSize: 10, userId: undefined as number | undefined, roleId: undefined as number | undefined, status: undefined as string | undefined })
const createForm = reactive({ userId: undefined as number | undefined, roleId: undefined as number | undefined, expireTime: '', reason: '' })
const revokeForm = reactive({ id: undefined as number | undefined, reason: '' })

const createRules = {
  userId: [{ required: true, message: '请选择被授权用户', trigger: 'change' }],
  roleId: [{ required: true, message: '请选择临时角色', trigger: 'change' }],
  expireTime: [{ required: true, message: '请选择有效截止时间', trigger: 'change' }],
  reason: [{ required: true, message: '请填写授权原因', trigger: 'blur' }]
}
const revokeRules = { reason: [{ required: true, message: '请填写撤销原因', trigger: 'blur' }] }

const userNameMap = computed(() => new Map(userOptions.value.map((user) => [user.id, user.nickname || user.username])))
const roleNameMap = computed(() => new Map(roleOptions.value.map((role) => [role.id, role.name])))

const statusText = (status: string) => ({ PENDING: '待审批', ACTIVE: '有效中', REVOKED: '已撤销', EXPIRED: '已过期' }[status] || status)
const statusTagType = (status: string) => ({ PENDING: 'warning', ACTIVE: 'success', REVOKED: 'info', EXPIRED: 'danger' }[status] || 'info')

const getList = async () => {
  loading.value = true
  try {
    const data = await TemporaryRoleGrantApi.getTemporaryRoleGrantPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const loadOptions = async () => {
  const [roles, users] = await Promise.all([RoleApi.getSimpleRoleList(), UserApi.getUserPage({ pageNo: 1, pageSize: 100 })])
  roleOptions.value = roles
  userOptions.value = users.list || []
}

const handleQuery = () => { queryParams.pageNo = 1; getList() }
const resetQuery = () => { queryFormRef.value?.resetFields(); handleQuery() }

const openCreateDialog = () => {
  Object.assign(createForm, { userId: undefined, roleId: undefined, expireTime: '', reason: '' })
  createDialogVisible.value = true
}

const handleCreate = async () => {
  await createFormRef.value?.validate()
  submitLoading.value = true
  try {
    await TemporaryRoleGrantApi.createTemporaryRoleGrant(createForm as TemporaryRoleGrantApi.TemporaryRoleGrantCreateReqVO)
    message.success('临时角色授权申请已提交')
    createDialogVisible.value = false
    await getList()
  } finally {
    submitLoading.value = false
  }
}

const handleApprove = async (row: TemporaryRoleGrantApi.TemporaryRoleGrantVO) => {
  await message.confirm('确认审批通过该临时角色授权？')
  await TemporaryRoleGrantApi.approveTemporaryRoleGrant(row.id)
  message.success('临时角色授权已生效')
  await getList()
}

const openRevokeDialog = (row: TemporaryRoleGrantApi.TemporaryRoleGrantVO) => {
  selectedGrant.value = row
  Object.assign(revokeForm, { id: row.id, reason: '' })
  revokeDialogVisible.value = true
}

const handleRevoke = async () => {
  await revokeFormRef.value?.validate()
  submitLoading.value = true
  try {
    await TemporaryRoleGrantApi.revokeTemporaryRoleGrant(revokeForm as TemporaryRoleGrantApi.TemporaryRoleGrantRevokeReqVO)
    message.success('临时角色授权已撤销')
    revokeDialogVisible.value = false
    await getList()
  } finally {
    submitLoading.value = false
  }
}

const openAuditDialog = async (row: TemporaryRoleGrantApi.TemporaryRoleGrantVO) => {
  selectedGrant.value = row
  auditList.value = await TemporaryRoleGrantApi.getTemporaryRoleGrantAuditList(row.id)
  auditDialogVisible.value = true
}

onMounted(async () => {
  await loadOptions()
  await getList()
})
</script>

<style scoped>
.temporary-role-grant-toolbar {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.temporary-role-grant-toolbar__filters {
  display: grid;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  gap: 12px;
}

.temporary-role-grant-toolbar__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (width <= 768px) {
  .temporary-role-grant-toolbar__filters {
    grid-template-columns: 1fr;
  }
}
</style>
