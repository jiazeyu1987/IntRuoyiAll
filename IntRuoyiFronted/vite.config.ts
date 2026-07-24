import fs from 'fs'
import { gracefulify } from 'graceful-fs'
import { createRequire } from 'node:module'
import {resolve} from 'path'
import type {ConfigEnv, UserConfig} from 'vite'
import {loadEnv} from 'vite'
import {createVitePlugins} from './build/vite'
import {exclude, include} from "./build/vite/optimize"

gracefulify(fs)
// 当前执行node命令时文件夹的地址(工作目录)
const root = process.cwd()
const requireFromBpmnTokenSimulation = createRequire(
    require.resolve('bpmn-js-token-simulation/package.json')
)
const randomColorPath = requireFromBpmnTokenSimulation.resolve('randomcolor')

// 路径查找
function pathResolve(dir: string) {
    return resolve(root, '.', dir)
}

function getCliOptionValue(optionNames: string[]) {
    for (let index = 0; index < process.argv.length; index += 1) {
        const arg = process.argv[index]
        for (const optionName of optionNames) {
            if (arg === optionName) {
                const value = process.argv[index + 1]
                return value && !value.startsWith('-') ? value : undefined
            }
            if (arg.startsWith(`${optionName}=`)) {
                return arg.slice(optionName.length + 1)
            }
        }
    }
    return undefined
}

function sanitizeCachePart(value: string) {
    return value.replace(/[^a-zA-Z0-9_-]/g, '-')
}

// https://vitejs.dev/config/
export default ({command, mode}: ConfigEnv): UserConfig => {
    let env = {} as Record<string, string>
    const isBuild = command === 'build'
    if (!isBuild) {
        env = loadEnv((process.argv[3] === '--mode' ? process.argv[4] : process.argv[3]), root)
    } else {
        env = loadEnv(mode, root)
    }
    const processEnvOverrides = Object.fromEntries(
        Object.entries(process.env).filter(([key, value]) => key.startsWith('VITE_') && typeof value === 'string')
    ) as Record<string, string>
    env = {...env, ...processEnvOverrides}
    const isBatchRecordPreviewMode = mode === 'batch-record-preview'
    const cliPort = getCliOptionValue(['--port', '-p'])
    const effectiveDevPort = cliPort || env.VITE_PORT
    if (!isBuild && !effectiveDevPort) {
        throw new Error('Vite dev cache isolation requires VITE_PORT or --port')
    }
    const effectiveDevPortNumber = effectiveDevPort ? Number(effectiveDevPort) : undefined
    if (!isBuild && (!effectiveDevPortNumber || Number.isNaN(effectiveDevPortNumber))) {
        throw new Error(`Vite dev port must be a valid number, got: ${effectiveDevPort}`)
    }
    const cacheKey = `${sanitizeCachePart(mode)}-${sanitizeCachePart(effectiveDevPort || 'build')}`
    const proxyTarget = env.VITE_PROXY_TARGET || env.VITE_BASE_URL
    const useSameOriginApiProxy = !isBuild && !isBatchRecordPreviewMode && !!env.VITE_PROXY_TARGET
    const enableJmreportProxy = isBatchRecordPreviewMode || !!env.VITE_PROXY_TARGET
    const useWindowsSafeOptimize = !isBuild && env.VITE_OPTIMIZE_PROFILE === 'windows-safe'
    const windowsSafeOptimizeInclude = [
        'qs',
        'url',
        'vue',
        'mitt',
        'axios',
        'pinia',
        'dayjs',
        'dayjs/plugin/advancedFormat.js',
        'dayjs/plugin/customParseFormat.js',
        'dayjs/plugin/dayOfYear.js',
        'dayjs/plugin/isSameOrAfter.js',
        'dayjs/plugin/isSameOrBefore.js',
        'dayjs/plugin/localeData.js',
        'dayjs/plugin/weekOfYear.js',
        'dayjs/plugin/weekYear.js',
        'qrcode',
        'jsbarcode',
        'vue-router',
        'vue-types',
        'vue-i18n',
        'crypto-js',
        'cropperjs',
        'lodash-es',
        'nprogress',
        'randomcolor',
        'web-storage-cache',
        '@iconify/iconify',
        '@vueuse/core',
        '@zxcvbn-ts/core',
        '@form-create/designer',
        '@form-create/element-ui',
        '@form-create/element-ui/auto-import',
        '@element-plus/icons-vue',
        'element-plus',
        'echarts/core',
        'echarts/charts',
        'echarts/components',
        'echarts/renderers',
        'echarts-wordcloud',
        '@wangeditor-next/editor',
        '@wangeditor-next/editor-for-vue',
        '@microsoft/fetch-event-source',
        'markdown-it',
        'markmap-view',
        'markmap-lib',
        'markmap-toolbar',
        'highlight.js'
    ]
    const devWatchIgnored = [
        /[\\/]node_modules[\\/]/,
        /[\\/]node_modules\.[^\\/]+[\\/]/,
        /[\\/]dist[^\\/]*[\\/]/,
        /[\\/]\.git[\\/]/,
        /[\\/]\.tmp[\\/]/,
        /[\\/]\.runtime[\\/]/,
        /[\\/]runtime[\\/]/,
        /[\\/]doc[\\/]/,
        /[\\/]tests[\\/]/,
        /[\\/]output[\\/]/,
        /[\\/]output-[^\\/]+\.log$/,
        /[\\/][^\\/]+\.log$/,
        /[\\/]test-results[\\/]/,
        /[\\/]\.playwright-cli[\\/]/,
        /[\\/]yudao-ui-admin-vue3[\\/]yudao-ui-admin-vue3[\\/]/
    ]
    if (isBatchRecordPreviewMode && !proxyTarget) {
        throw new Error('batch-record-preview mode requires VITE_PROXY_TARGET or VITE_BASE_URL')
    }
    const runtimeBaseUrl = isBatchRecordPreviewMode
        ? `http://127.0.0.1:${env.VITE_PORT}`
        : useSameOriginApiProxy
          ? ''
        : env.VITE_BASE_URL
    return {
        base: env.VITE_BASE_PATH,
        root: root,
        cacheDir: `node_modules/.vite-${cacheKey}`,
        server: {
            port: effectiveDevPortNumber,
            host: "0.0.0.0",
            open: env.VITE_OPEN === 'true',
            // 避免生成产物、任务证据和依赖备份耗尽 Windows 文件句柄。
            watch: {
              ignored: devWatchIgnored
            },
            // 本地跨域代理. 展厅前台的 preview/audio 资产使用相对 /admin-api 路径，
            // Vite 本地验证需要将这类资源请求转发到后端，否则会被 SPA fallback 吞掉。
            // 批记录预览模式还需要把 /jmreport 直接代理到后端纯报表页。
            // JimuBI 仪表盘设计器入口使用 /drag/list，同源代理模式下也必须转发到后端。
            proxy: {
              ['/admin-api']: {
                target: proxyTarget,
                ws: false,
                changeOrigin: true
              },
              ...(enableJmreportProxy ? {
                ['/jmreport']: {
                  target: proxyTarget,
                  ws: false,
                  changeOrigin: true,
                },
                ['/drag']: {
                  target: proxyTarget,
                  ws: false,
                  changeOrigin: true,
                }
              } : {})
            },
        },
        define: {
            'import.meta.env.VITE_BASE_URL': JSON.stringify(runtimeBaseUrl),
        },
        plugins: createVitePlugins(isBuild),
        css: {
            preprocessorOptions: {
                scss: {
                    additionalData: '@use "@/styles/variables.scss" as *;',
                    javascriptEnabled: true,
                    silenceDeprecations: ["legacy-js-api"], // 参考自 https://stackoverflow.com/questions/78997907/the-legacy-js-api-is-deprecated-and-will-be-removed-in-dart-sass-2-0-0
                }
            }
        },
        resolve: {
            extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.scss', '.css'],
            alias: [
                {
                    find: 'randomcolor',
                    replacement: randomColorPath
                },
                {
                    find: 'vue-i18n',
                    replacement: 'vue-i18n/dist/vue-i18n.cjs.js'
                },
                {
                    find: /\@\//,
                    replacement: `${pathResolve('src')}/`
                }
            ]
        },
        build: {
            minify: 'terser',
            outDir: env.VITE_OUT_DIR || 'dist',
            sourcemap: env.VITE_SOURCEMAP === 'true' ? 'inline' : false,
            // brotliSize: false,
            terserOptions: {
                compress: {
                    drop_debugger: env.VITE_DROP_DEBUGGER === 'true',
                    drop_console: env.VITE_DROP_CONSOLE === 'true'
                }
            },
            rollupOptions: {
                // Keep build-time module loading under the Windows file-handle limit.
                maxParallelFileOps: 1,
                output: {
                    manualChunks: {
                      echarts: ['echarts'], // 将 echarts 单独打包，参考 https://gitee.com/yudaocode/yudao-ui-admin-vue3/issues/IAB1SX 讨论
                      'form-create': ['@form-create/element-ui'], // 参考 https://github.com/yudaocode/yudao-ui-admin-vue3/issues/148 讨论
                      'form-designer': ['@form-create/designer'],
                    }
                },
            },
        },
        optimizeDeps: useWindowsSafeOptimize
          ? { noDiscovery: true, include: windowsSafeOptimizeInclude, exclude }
          : { include, exclude }
    }
}
