const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')

function loadComponent(file, imports, returned, extra = {}) {
  const source = fs.readFileSync(path.resolve(__dirname, '../../src', file), 'utf8')
  const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  const js = ts.transpileModule(script, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
  }).outputText
  const { emit, ...otherBindings } = extra
  const bindings = {
    ...vue,
    defineOptions() {},
    defineExpose() {},
    defineEmits: () => emit || (() => {}),
    useI18n: () => ({ t: (key) => key }),
    useMessage: () => ({ success() {}, error() {} }),
    ...otherBindings
  }
  return new Function(
    'require',
    'exports',
    ...Object.keys(bindings),
    `${js}; return {${returned}}`
  )(
    (name) => {
      assert.ok(name in imports, `unexpected import: ${name}`)
      return imports[name]
    },
    {},
    ...Object.values(bindings)
  )
}

async function capacityForm() {
  const deviceId = '2117828464390517003'
  const processId = '2117828464390517004'
  const saved = []
  const emitted = []
  let failSave = true
  const form = loadComponent(
    'views/mes/dv/machinery/MachineryProcessCapacityForm.vue',
    {
      '@/api/mes/dv/machinery/process': {
        DvMachineryProcessApi: {
          getMachineryProcessList: async (id) => {
            assert.equal(id, deviceId)
            return [{ processId, standardHourlyCapacity: 20, remark: '原产能' }]
          },
          saveCapacity: async (data) => {
            saved.push(data)
            if (failSave) throw new Error('保存失败')
          }
        }
      },
      '@/api/mes/pro/process': {
        ProProcessApi: {
          getProcessSimpleList: async () => [
            { id: processId, name: '工序', code: 'P1' },
            { id: '102', name: '另一工序', code: 'P2' }
          ]
        }
      }
    },
    'open, submit, formRef, formData, visible, loading, saving',
    { emit: (event) => emitted.push(event) }
  )
  form.formRef.value = { validate: async () => true, clearValidate() {} }
  await form.open(deviceId, processId)
  await vue.nextTick()
  assert.equal(form.formData.value.processId, processId, 'Long ID must retain every digit')
  assert.equal(form.formData.value.standardHourlyCapacity, 20)
  form.formData.value.standardHourlyCapacity = 100
  form.formData.value.remark = '模拟测试'
  await assert.rejects(form.submit(), /保存失败/)
  assert.equal(form.visible.value, true, 'failed saves retain the form and values')
  assert.equal(form.saving.value, false)
  assert.deepEqual(emitted, [], 'failed saves never emit success')
  failSave = false
  await form.submit()
  assert.deepEqual(saved[1], {
    machineryId: deviceId,
    processId,
    standardHourlyCapacity: 100,
    remark: '模拟测试'
  })
  assert.equal(form.visible.value, false)
  assert.deepEqual(emitted, ['success'])
  await form.open(deviceId)
  form.formData.value.processId = '102'
  await vue.nextTick()
  assert.equal(
    form.formData.value.standardHourlyCapacity,
    undefined,
    'a new pair must not inherit another process capacity'
  )
  assert.equal(form.formData.value.remark, '')
  await assert.rejects(form.open(deviceId, '999'), /不在可维护工序列表/)
  assert.equal(form.visible.value, false)
  assert.equal(form.loading.value, false)
}

async function workerPosts() {
  let result = []
  let failure = false
  const form = loadComponent(
    'views/mes/md/workstation/WorkstationWorkerList.vue',
    {
      '@/api/mes/md/workstation/worker': {},
      '@/api/system/post': {
        getSimplePostList: async () => {
          if (failure) throw new Error('岗位服务不可用')
          return result
        }
      },
      '@/views/system/post/PostForm.vue': {}
    },
    'loadPosts, postList, postLoading, postLoadError',
    {
      defineProps: () => ({ workstationId: undefined, formType: 'update' })
    }
  )
  await form.loadPosts()
  assert.equal(form.postList.value.length, 0, 'empty candidates must not fabricate a post')
  assert.equal(form.postLoadError.value, '')
  failure = true
  await assert.rejects(form.loadPosts(), /岗位服务不可用/)
  assert.ok(form.postLoadError.value)
  assert.equal(form.postLoading.value, false)
  failure = false
  result = [{ id: '101', name: '模拟测试岗位' }]
  await form.loadPosts()
  assert.equal(form.postLoadError.value, '')
  assert.equal(
    form.postList.value[0].id,
    '101',
    'refresh must include a newly maintained real post'
  )
}

;(async () => {
  await capacityForm()
  await workerPosts()
  console.log(
    'PASS: capacity save failures, exact IDs, process isolation, post loading and refresh'
  )
})().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
