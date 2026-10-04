<template>
  <Dialog v-model="visible" title="项目默认属性与负责人账号" width="820px">
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-alert v-if="historicalMissing" title="历史未记录，请主动补齐项目默认属性与负责人账号" type="info" :closable="false" />
    <el-form v-loading="loading" label-width="140px" :disabled="loading">
      <el-form-item label="项目负责人账号" required>
        <el-select v-model="leaderId" filterable placeholder="选择正式系统账号">
          <el-option v-for="user in users" :key="user.id" :value="user.id" :label="user.nickname + '（' + (user.username || '#' + user.id) + '）'" />
        </el-select>
      </el-form-item>
      <ProjectAttributesFields v-model="attributes" />
      <el-form-item label="修改原因" required><el-input v-model="changeReason" maxlength="500" /></el-form-item>
      <el-alert title="修改仅影响以后新申请，已保存草稿、在途申请与历史版本保留原值。" type="info" :closable="false" />
    </el-form>
    <template #footer>
      <el-button v-hasPermi="['dcc:project-code:update']" type="primary" :loading="loading" :disabled="!ready" @click="save">保存</el-button>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { getSimpleUserList } from '@/api/system/user'
import { getProjectCode } from '@/api/dcc/controlledFile/projectCodes'
import { configureProjectAttributes } from '@/api/dcc/controlledFile/projectAttributes'
import ProjectAttributesFields from '../../project-attributes/ProjectAttributesFields.vue'
import { emptyAttributes, validateAttributes, type ProjectAttributes } from '../../project-attributes/state'
import { projectConfigurationIdentity, projectConfigurationAccounts, type ProjectConfigurationAccount } from './project-attribute-configuration'
const emit = defineEmits<{ saved: [projectId: number | string] }>()
const message = useMessage()
const visible = ref(false), loading = ref(false), ready = ref(false), historicalMissing = ref(false), error = ref('')
let contextRevision = 0
let mounted = true
const changeReason = ref('')
const projectId = ref<string>(), leaderId = ref<string>()
const loadedProjectId = ref<string>()
const users = ref<ProjectConfigurationAccount[]>([]), attributes = ref<ProjectAttributes>(emptyAttributes())
const invalidate = () => { contextRevision++; ready.value = false; loading.value = false; loadedProjectId.value = undefined; leaderId.value = undefined; users.value = [] }
watch(visible, value => { if (!value) invalidate() }, { flush: 'sync' })
onBeforeUnmount(() => { mounted = false; invalidate() })
const open = async (id: number | string) => {
  if (!mounted) return
  const revision = ++contextRevision
  visible.value = true; loading.value = true; ready.value = false; error.value = ''
  changeReason.value = ''; projectId.value = undefined; loadedProjectId.value = undefined; leaderId.value = undefined; users.value = []; attributes.value = emptyAttributes(); historicalMissing.value = false
  try {
    const selectedProject = projectConfigurationIdentity(id)
    projectId.value = selectedProject
    const [project, accounts] = await Promise.all([getProjectCode(selectedProject), getSimpleUserList()])
    if (revision !== contextRevision || !visible.value) return
    if (!project || projectConfigurationIdentity(project.id) !== selectedProject) throw new Error('项目属性响应与当前所选项目身份不一致')
    const options = projectConfigurationAccounts(accounts)
    const leader = project.projectLeaderUserId == null ? undefined : projectConfigurationIdentity(project.projectLeaderUserId)
    if (leader && !options.some(user => user.id === leader)) throw new Error('项目负责人不在正式启用账号目录中，请核对项目与账号身份')
    users.value = options; leaderId.value = leader; loadedProjectId.value = selectedProject
    historicalMissing.value = !project.defaultAttributesJson || !project.projectLeaderUserId
    if (project.defaultAttributesJson) attributes.value = validateAttributes(JSON.parse(project.defaultAttributesJson))
    ready.value = true
  } catch (cause) { if (revision === contextRevision) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (revision === contextRevision) loading.value = false }
}
const save = async () => {
  if (!mounted || !visible.value || !ready.value || loading.value || projectId.value === undefined) return
  const revision = contextRevision, id = projectId.value
  loading.value = true; error.value = ''
  try {
    if (projectConfigurationIdentity(id) !== loadedProjectId.value) throw new Error('当前项目配置身份已变化，请重新打开项目')
    const leader = projectConfigurationIdentity(leaderId.value)
    if (!users.value.some(user => user.id === leader)) throw new Error('请选择正式启用账号目录中的负责人')
    validateAttributes(attributes.value)
    const payload = { projectLeaderUserId: leader, defaultAttributes: JSON.parse(JSON.stringify(attributes.value)), changeReason: changeReason.value }
    await configureProjectAttributes(id, payload)
    if (!mounted) return
    if (revision !== contextRevision || !visible.value || projectId.value !== id) { message.success(`项目 #${id} 配置已保存，请到原项目记录核对；当前弹框已保留。`); return }
    emit('saved', id)
    if (revision === contextRevision) visible.value = false
  } catch (cause) { if (revision === contextRevision) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (revision === contextRevision) loading.value = false }
}

defineExpose({ open })
</script>
