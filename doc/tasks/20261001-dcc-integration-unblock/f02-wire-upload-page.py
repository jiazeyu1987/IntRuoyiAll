from pathlib import Path

path = Path('C:/IntRuoyi/20261001-dcc-integration/IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue')
text = path.read_text(encoding='utf-8')
start = text.index('        <el-form-item v-if="!isExternalReview" label="关联文件">')
end = text.index('        <el-form-item v-if="!isExternalReview" label="阶段">', start)
text = text[:start] + '''        <el-form-item v-if="!isExternalReview" label="项目文件夹" prop="projectFolderId">
          <div class="w-full">
            <el-tree-select v-model="formData.projectFolderId" :data="projectFolderTree" node-key="id"
              :props="{ label: 'name', children: 'children', disabled: (node) => !node.active }"
              check-strictly :loading="projectFoldersLoading" :disabled="!formData.dccProjectCodeId || submitLoading"
              placeholder="请选择项目文件夹" class="!w-460px" />
            <el-alert v-if="projectFoldersError" :title="projectFoldersError" type="error" :closable="false" />
            <span v-else-if="formData.dccProjectCodeId && !projectFoldersLoading && !projectFolders.length">项目尚未生成独立目录</span>
          </div>
        </el-form-item>
        <el-form-item v-if="!isExternalReview" label="登记说明" prop="projectFolderChangeReason">
          <el-input v-model="formData.projectFolderChangeReason" maxlength="500" show-word-limit
            placeholder="填写文件归入此项目文件夹的说明" :disabled="submitLoading" class="!w-560px" />
        </el-form-item>
        <el-form-item v-if="!isExternalReview" label="关联文件">
          <div class="w-full">
            <el-button :disabled="!formData.dccProjectCodeId || !formData.projectFolderId || !previewUpload || submitLoading"
              @click="openUploadRelations">关联</el-button>
            <span class="ml-8px">已选 {{ selectedUploadRelations.length }} 个文件</span>
            <div class="mt-8px"><el-tag v-for="file in selectedUploadRelations" :key="file.masterId" class="mr-6px">
              {{ file.fileName }} · {{ file.versionNo }}{{ file.pendingEffect ? ' · 待生效' : '' }}
            </el-tag></div>
          </div>
        </el-form-item>
''' + text[end:]
text = text.replace('    </el-form>\n  </ContentWrap>', '''    </el-form>
    <DccFileSelector v-if="uploadRelationSource" v-model="uploadRelationsVisible" :source="uploadRelationSource"
      purpose="relations" :directories="uploadRelationDirectories" :selected="selectedUploadRelations"
      :load-page="loadUploadRelationPage" :persist="persistUploadRelations" :open-preview="previewUploadRelation" />
  </ContentWrap>''', 1)
text = text.replace("import ProjectApplicationAttributes from '../project-attributes/ProjectApplicationAttributes.vue'", """import ProjectApplicationAttributes from '../project-attributes/ProjectApplicationAttributes.vue'
import DccFileSelector from '../relations/DccFileSelector.vue'
import type { SelectorSource, DirectoryNode } from '../relations/DccFileSelector.vue'
import type { FileCandidate, SelectorQuery } from '../relations/selector-state'
import { mapReferenceDirectoryNodes, referenceIdentity } from '../relations/project-reference-contract'
import { loadDccSelectorPage } from '@/api/dcc/controlledFile/applicationRead'
import { getProjectDiscoveryPage, type DccDiscoveredProject } from '@/api/dcc/controlledFile/projectDiscovery'
import { getProjectFolders, type ProjectFolder } from '@/api/dcc/controlledFile/projectAttributes'
import { buildProjectFolderTree } from '../basic-data/components/project-folder-tree'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import { buildControlledFileViewerPath } from '../view/presentation'""")
for line in ['  getProjectCodeControlledFilesPage,\n','  getProjectCodePage,\n','  type DccProjectCodeRespVO,\n','  type ControlledFileVO,\n']:
    assert line in text, line
    text = text.replace(line, '', 1)
start = text.index('const projectCodeOptions = ref<DccProjectCodeRespVO[]>([])')
end = text.index('const fileTypeTaxonomies =', start)
text = text[:start] + '''const projectCodeOptions = ref<DccDiscoveredProject[]>([])
const projectFolders = ref<ProjectFolder[]>([])
const projectFoldersLoading = ref(false)
const projectFoldersError = ref('')
let projectFolderRequestSequence = 0
const uploadRelationsVisible = ref(false)
const uploadRelationSource = ref<SelectorSource>()
const uploadRelationDirectories = ref<DirectoryNode[]>([])
const selectedUploadRelations = ref<FileCandidate[]>([])
''' + text[end:]
text = text.replace('const acceptedProjectCodeId = ref<number | null>(null)', 'const acceptedProjectCodeId = ref<number | string | null>(null)')
text = text.replace('const projectProductResolvedId = ref<number | null>(null)', 'const projectProductResolvedId = ref<number | string | null>(null)')
for line in ["const relatedFileOptionsLoading = ref(false)\n","const relatedFileOptionsError = ref('')\n"]:
    assert line in text, line
    text = text.replace(line,'',1)
text = text.replace('  dccProjectCodeId: null,\n', "  dccProjectCodeId: null,\n  projectFolderId: null,\n  projectFolderChangeReason: '',\n", 1)
text = text.replace('const formatProjectCodeOptionLabel = (project: DccProjectCodeRespVO) =>\n  [project.projectName, project.projectCode, project.docControlNo]', 'const formatProjectCodeOptionLabel = (project: DccDiscoveredProject) =>\n  [project.projectName, project.projectCode]')
text = text.replace('const page = await getProjectCodePage({', 'const page = await getProjectDiscoveryPage({', 1)
text = text.replace('projectCodeOptions.value = page.list || []', 'projectCodeOptions.value = page.list', 1)
text = text.replace('const loadProjectFileTemplate = async (projectCodeId: number)', 'const loadProjectFileTemplate = async (projectCodeId: number | string)')
text = text.replace('const loadUploadNameOptions = async (dccProjectCodeId: number,', 'const loadUploadNameOptions = async (dccProjectCodeId: number | string,')
start = text.index('  relatedFileRequestSequence += 1', text.index('const handleProjectCodeChange ='))
end = text.index('const handleFileTypeTaxonomyChange =', start)
text = text[:start] + '''  projectFolderRequestSequence += 1
  projectFolders.value = []
  projectFoldersError.value = ''
  formData.projectFolderId = null
  formData.relatedControlledFileIds = []
  selectedUploadRelations.value = []
  uploadRelationsVisible.value = false
  uploadRelationSource.value = undefined
  resetProjectFileTemplateSelection()
  applyDccProjectCodeProductNumber()
  if (formData.dccProjectCodeId) {
    await Promise.all([
      loadUploadProjectFolders(formData.dccProjectCodeId),
      loadProjectFileTemplate(formData.dccProjectCodeId)
    ])
  }
}

const projectFolderTree = computed(() => formData.dccProjectCodeId
  ? buildProjectFolderTree(String(formData.dccProjectCodeId), projectFolders.value) : [])
const selectedProjectFolder = computed(() => projectFolders.value.find(folder =>
  String(folder.id) === String(formData.projectFolderId) && folder.active))

const loadUploadProjectFolders = async (projectId: number | string) => {
  const sequence = ++projectFolderRequestSequence
  projectFoldersLoading.value = true
  projectFoldersError.value = ''
  try {
    const rows = await getProjectFolders(projectId)
    if (sequence !== projectFolderRequestSequence || String(formData.dccProjectCodeId) !== String(projectId)) return
    projectFolders.value = rows
  } catch (error) {
    if (sequence !== projectFolderRequestSequence || String(formData.dccProjectCodeId) !== String(projectId)) return
    projectFolders.value = []
    projectFoldersError.value = error instanceof Error ? error.message : String(error)
  } finally {
    if (sequence === projectFolderRequestSequence) projectFoldersLoading.value = false
  }
}

const buildUploadRelationSource = (): SelectorSource => {
  if (!selectedProjectCode.value || !selectedProjectFolder.value || !previewUpload.value
    || projectFoldersLoading.value || projectFoldersError.value) throw new Error('请先选择项目文件夹并上传文件')
  const tenantId = referenceIdentity(getVisitTenantId() ?? getTenantId())
  const projectId = referenceIdentity(formData.dccProjectCodeId)
  const folderId = referenceIdentity(formData.projectFolderId)
  return {
    contextKey: JSON.stringify([tenantId, projectId, folderId, previewUpload.value.sessionId,
      previewUpload.value.fileName, formData.fileNumber, formData.versionNo]),
    tenantId, projectId, folderId, projectName: selectedProjectCode.value.projectName,
    folderName: selectedProjectFolder.value.name, fileName: previewUpload.value.fileName,
    fileNumber: formData.fileNumber, versionNo: formData.versionNo || '', unsubmitted: true
  }
}

const openUploadRelations = () => {
  try {
    uploadRelationSource.value = buildUploadRelationSource()
    uploadRelationDirectories.value = mapReferenceDirectoryNodes(uploadRelationSource.value.tenantId,
      uploadRelationSource.value.projectId, projectFolderTree.value)
    uploadRelationsVisible.value = true
  } catch (error) {
    message.error(error instanceof Error ? error.message : String(error))
  }
}

const loadUploadRelationPage = (query: SelectorQuery) => {
  if (!uploadRelationSource.value) throw new Error('关联窗口尚未打开')
  return loadDccSelectorPage(uploadRelationSource.value.tenantId, query)
}

const persistUploadRelations = async (rows: FileCandidate[]) => {
  const current = buildUploadRelationSource()
  if (current.contextKey !== uploadRelationSource.value?.contextKey) throw new Error('项目或上传文件已变化，请重新打开关联窗口')
  if (rows.some(row => row.tenantId !== current.tenantId)) throw new Error('关联文件租户不一致')
  const copy = rows.map(row => {
    if (!row.controlled) throw new Error('只能关联受控版本')
    return { ...row, controlledFileId: referenceIdentity(row.controlledFileId), masterId: referenceIdentity(row.masterId) }
  })
  if (new Set(copy.map(row => row.masterId)).size !== copy.length) throw new Error('关联文件不能重复')
  selectedUploadRelations.value = copy
  formData.relatedControlledFileIds = copy.map(row => row.controlledFileId)
}

const previewUploadRelation = async (row: FileCandidate) => {
  if (!row.canPreview) throw new Error('没有该文件正文查看权限')
  await router.push(buildControlledFileViewerPath(row.controlledFileId, 'browser', route.fullPath))
}

''' + text[end:]
text = text.replace("  if (!isExternalReview.value) {\n    if (projectAttributesLoading.value", """  if (!isExternalReview.value) {
    if (projectFoldersLoading.value || projectFoldersError.value || !selectedProjectFolder.value
      || String(selectedProjectFolder.value.projectCodeId) !== String(draft.dccProjectCodeId)) {
      throw new Error(projectFoldersError.value || '请选择当前项目的有效文件夹')
    }
    if (!draft.projectFolderChangeReason?.trim() || draft.projectFolderChangeReason.trim().length > 500) {
      throw new Error('请填写500字以内的文件夹登记说明')
    }
    if (projectAttributesLoading.value""", 1)
path.write_text(text, encoding='utf-8')
print('PASS: public upload logical folder, formal relation dialog and exact string identities wired')
