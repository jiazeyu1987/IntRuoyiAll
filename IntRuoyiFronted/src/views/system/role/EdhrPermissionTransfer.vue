<template>
  <el-dropdown v-if="canExportRole || canExport" trigger="click">
    <el-button
      plain
      type="success"
      :loading="exportLoading || exporting"
      class="permission-transfer-action"
    >
      <Icon class="mr-5px" icon="ep:download" />
      导出
      <Icon class="ml-5px" icon="ep:arrow-down" />
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-if="canExportRole"
          :disabled="exportLoading"
          @click="emit('exportRole')"
        >
          权限角色配置包（JSON）
        </el-dropdown-item>
        <el-dropdown-item v-if="canExport" :disabled="exporting" @click="exportFile">
          eDHR 权限（Excel）
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
  <el-dropdown v-if="canImportRole || canImport" trigger="click">
    <el-button
      plain
      type="warning"
      :loading="importLoading || busy"
      class="permission-transfer-action"
    >
      <Icon class="mr-5px" icon="ep:upload" />
      导入
      <Icon class="ml-5px" icon="ep:arrow-down" />
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-if="canImportRole"
          :disabled="importLoading"
          @click="emit('importRole')"
        >
          权限角色配置包（JSON）
        </el-dropdown-item>
        <el-dropdown-item v-if="canImport" :disabled="busy" @click="open">
          eDHR 权限（Excel）
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
  <el-dialog
    v-model="visible"
    title="eDHR 账号角色权限迁移"
    width="min(1200px, 96vw)"
    :close-on-click-modal="false"
  >
    <el-alert type="info" :closable="false" show-icon class="mb-4">
      仅允许从芋道源码导出、在瑛泰集团导入。请使用已存在且启用的目标账号；导入保留账号原有角色，不复制密码、签名、班组关系或历史任务候选。
    </el-alert>
    <div class="transfer-upload">
      <input
        ref="fileInput"
        type="file"
        accept=".xlsx"
        aria-label="选择 eDHR 权限 Excel"
        :disabled="busy"
        @change="chooseFile"
      />
      <el-button :disabled="!file" :loading="busy" @click="previewFile(true)"
        >重新预览授权</el-button
      >
    </div>
    <el-alert v-if="result" :title="result" type="success" :closable="false" class="mt-4" />
    <template v-if="preview">
      <h3>1. 明确账号映射</h3>
      <p>按完整用户名匹配，不按姓名推断。修改目标账号后，须重新预览才能确认。</p>
      <el-table :data="mappings" border max-height="260" row-key="sourceUsername">
        <el-table-column prop="sourceUsername" label="来源账号" min-width="150" />
        <el-table-column prop="sourceName" label="来源姓名" min-width="120" />
        <el-table-column label="目标账号（可修改）" min-width="220">
          <template #default="{ row }">
            <el-input
              v-model="row.targetUsername"
              :aria-label="`目标账号 ${row.sourceUsername}`"
              :disabled="busy"
              @input="invalidate"
            />
          </template>
        </el-table-column>
      </el-table>
      <el-alert
        v-if="dirty"
        title="账号映射已修改，请点击重新预览授权。"
        type="warning"
        :closable="false"
        class="mt-4"
      />
      <el-alert
        v-if="preview.errors.length"
        title="以下问题阻止整批导入，请修正后重新预览"
        type="error"
        :closable="false"
        class="mt-4"
      >
        <ul
          ><li v-for="error in preview.errors" :key="error">{{ error }}</li></ul
        >
      </el-alert>
      <h3>2. 核对角色及完整菜单授权</h3>
      <p v-if="preview.createCategory">将创建“eDHR权限迁移”角色分类。</p>
      <el-collapse>
        <el-collapse-item v-for="role in preview.roles" :key="role.code" :name="role.code">
          <template #title>
            {{ role.name }} · {{ role.code }} · {{ role.roleId ? '复用角色' : '新建角色' }} ·
            数据范围：{{ role.dataScope }}
          </template>
          <p>来源角色编码：{{ role.sourceCode }}</p>
          <el-alert v-if="role.existingMembers.length" type="warning" :closable="false">
            此角色新增授权也将影响已有成员：{{ role.existingMembers.join('、') }}
          </el-alert>
          <el-table :data="role.menus" border max-height="360">
            <el-table-column label="变更" width="80"
              ><template #default="{ row }">{{
                row.added ? '新增' : '已有'
              }}</template></el-table-column
            >
            <el-table-column prop="name" label="菜单名称" min-width="150" />
            <el-table-column prop="permission" label="权限标识" min-width="300" />
            <el-table-column prop="purpose" label="业务用途" min-width="360" />
          </el-table>
          <p v-if="!role.menus.length">仅配置精确业务身份角色，不隐含菜单授权。</p>
        </el-collapse-item>
      </el-collapse>
      <h3>3. 核对账号授权结果</h3>
      <el-table :data="preview.accounts" border max-height="300">
        <el-table-column prop="targetUsername" label="目标账号" min-width="140" />
        <el-table-column prop="targetName" label="目标姓名" min-width="120" />
        <el-table-column label="保留的已有角色" min-width="240"
          ><template #default="{ row }">{{
            row.existingRoles.join('、') || '无'
          }}</template></el-table-column
        >
        <el-table-column label="本次角色编码" min-width="280"
          ><template #default="{ row }">{{ row.roleCodes.join('、') }}</template></el-table-column
        >
        <el-table-column label="新增角色编码" min-width="280"
          ><template #default="{ row }">{{
            row.addedRoles.join('、') || '无（授权已存在）'
          }}</template></el-table-column
        >
      </el-table>
      <el-form label-position="top" class="mt-4">
        <el-form-item label="授权变更原因（5至500字）">
          <el-input
            v-model="reason"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            :disabled="busy"
          />
        </el-form-item>
      </el-form>
      <el-checkbox v-model="acknowledged" :disabled="busy || dirty || preview.errors.length > 0">
        已核对目标账号、完整授权、数据范围及已有成员影响，确认整批导入
      </el-checkbox>
    </template>
    <template #footer>
      <el-button :disabled="busy" @click="visible = false">关闭</el-button>
      <el-button type="primary" :loading="busy" :disabled="!ready" @click="confirmImport"
        >确认导入</el-button
      >
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { checkPermi } from '@/utils/permission'
import download from '@/utils/download'
import * as Transfer from '@/api/system/role/edhrPermissionTransfer'

defineProps<{ exportLoading: boolean; importLoading: boolean }>()
const emit = defineEmits<{ success: []; exportRole: []; importRole: [] }>()
const message = useMessage()
const canExportRole = computed(() => checkPermi(['system:role:export']))
const canImportRole = computed(() => checkPermi(['system:role:create', 'system:role:update']))
const canExport = computed(() =>
  ['system:role:export', 'system:user:query'].every((p) => checkPermi([p]))
)
const canImport = computed(() =>
  [
    'system:role:create',
    'system:permission:assign-role-menu',
    'system:permission:assign-user-role',
    'system:permission:assign-role-data-scope',
    'system:user:query'
  ].every((p) => checkPermi([p]))
)
const visible = ref(false)
const exporting = ref(false)
const busy = ref(false)
const file = ref<File>()
const fileInput = ref<HTMLInputElement>()
const preview = ref<Transfer.TransferPreview>()
const mappings = ref<Transfer.AccountMapping[]>([])
const acknowledged = ref(false)
const dirty = ref(false)
const reason = ref('')
const result = ref('')
const ready = computed(
  () =>
    !!file.value &&
    !!preview.value?.token &&
    !dirty.value &&
    !preview.value.errors.length &&
    acknowledged.value &&
    reason.value.trim().length >= 5
)

const open = () => {
  file.value = undefined
  preview.value = undefined
  mappings.value = []
  reason.value = ''
  result.value = ''
  acknowledged.value = false
  dirty.value = false
  if (fileInput.value) fileInput.value.value = ''
  visible.value = true
}
const exportFile = async () => {
  exporting.value = true
  try {
    download.excel(await Transfer.exportEdhrPermissions(), 'eDHR账号角色权限.xlsx')
  } finally {
    exporting.value = false
  }
}
const invalidate = () => {
  dirty.value = true
  acknowledged.value = false
  if (preview.value) preview.value.token = ''
}
const chooseFile = async (event: Event) => {
  const chosen = (event.target as HTMLInputElement).files?.[0]
  file.value = undefined
  preview.value = undefined
  mappings.value = []
  result.value = ''
  acknowledged.value = false
  if (!chosen) return
  if (
    !chosen.name.toLowerCase().endsWith('.xlsx') ||
    chosen.size > 5 * 1024 * 1024 ||
    !chosen.size
  ) {
    message.error('请选择非空且不超过5MB的.xlsx文件')
    return
  }
  file.value = chosen
  await previewFile(false)
}
const previewFile = async (useMappings: boolean) => {
  if (!file.value) return
  busy.value = true
  acknowledged.value = false
  if (preview.value) preview.value.token = ''
  try {
    preview.value = await Transfer.previewEdhrPermissions(
      file.value,
      useMappings ? mappings.value : undefined
    )
    mappings.value = preview.value.mappings.map((row) => ({ ...row }))
    dirty.value = false
  } finally {
    busy.value = false
  }
}
const confirmImport = async () => {
  if (!ready.value || !file.value || !preview.value) return
  busy.value = true
  try {
    const data = await Transfer.confirmEdhrPermissions(
      file.value,
      mappings.value,
      preview.value.token,
      reason.value
    )
    result.value = data.unchanged
      ? '导入完成：授权已存在，本次无变更。'
      : `导入成功：新建角色 ${data.createdRoles} 个，更新菜单授权 ${data.changedRoles} 个角色，新增角色关联 ${data.changedAccounts} 个账号。`
    preview.value.token = ''
    acknowledged.value = false
    emit('success')
  } finally {
    // Any failure requires fresh state review before another confirmation.
    if (preview.value) preview.value.token = ''
    acknowledged.value = false
    busy.value = false
  }
}
</script>

<style scoped>
.permission-transfer-action {
  min-width: 108px;
}
.transfer-upload {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
h3 {
  margin: 24px 0 10px;
  font-size: 16px;
}
p {
  color: #606266;
  line-height: 1.7;
}
</style>
