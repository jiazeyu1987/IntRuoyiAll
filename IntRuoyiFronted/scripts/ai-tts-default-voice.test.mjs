import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('AI TTS api exposes shared aliyun nls default voice endpoints', () => {
  const source = readText('src/api/ai/tts/index.ts')

  assert.match(source, /AliyunNlsDefaultsVO/)
  assert.match(source, /AliyunNlsDefaultVoiceSaveReqVO/)
  assert.match(source, /getAliyunNlsDefaults/)
  assert.match(source, /saveAliyunNlsDefaultVoice/)
  assert.match(source, /\/ai\/tts-test\/aliyun-nls-defaults/)
  assert.match(source, /\/ai\/tts-test\/aliyun-nls-default-voice/)
})

test('AI TTS pane loads and saves shared aliyun nls default voice', () => {
  const source = readText('src/views/ai/music/manager/TtsTestPane.vue')

  assert.match(source, /getAliyunNlsDefaults/)
  assert.match(source, /saveAliyunNlsDefaultVoice/)
  assert.match(source, /handleSaveAliyunNlsVoice/)
  assert.match(source, /保存默认音色/)
})
