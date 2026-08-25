import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { VitePWA } from 'vite-plugin-pwa'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  return {
    plugins: [
      vue(),
      VitePWA({
        registerType: 'autoUpdate',
        injectRegister: null,
        includeAssets: ['favicon.svg', 'pwa/apple-touch-icon.png'],
        manifest: {
          name: 'Love Cube',
          short_name: 'Love Cube',
          description: '内容、活动与社交，从 Love Cube 开始',
          lang: 'zh-CN',
          start_url: '/',
          scope: '/',
          display: 'standalone',
          background_color: '#F8FAFC',
          theme_color: '#ffffff',
          icons: [
            { src: '/pwa/icon-192.png', sizes: '192x192', type: 'image/png' },
            { src: '/pwa/icon-512.png', sizes: '512x512', type: 'image/png' },
            { src: '/pwa/icon-512-maskable.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' }
          ]
        },
        workbox: {
          // 带 hash 的静态资源可预缓存；入口 HTML 不预缓存，避免发版后仍打开旧 index
          globPatterns: ['**/*.{js,css,svg,png,webp,woff2}'],
          navigateFallback: null,
          skipWaiting: true,
          clientsClaim: true,
          cleanupOutdatedCaches: true,
          runtimeCaching: [
            {
              urlPattern: ({ request }) => request.mode === 'navigate',
              handler: 'NetworkFirst',
              options: {
                cacheName: 'lc-html',
                networkTimeoutSeconds: 3,
                expiration: { maxEntries: 4, maxAgeSeconds: 24 * 60 * 60 }
              }
            },
            {
              urlPattern: ({ url }) =>
                url.pathname.includes('/admin/api/') || url.pathname.includes('/admin/ws/'),
              handler: 'NetworkOnly'
            }
          ]
        },
        devOptions: {
          enabled: false
        }
      })
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
        '@f': fileURLToPath(new URL('./src/modules/fellowship', import.meta.url))
      }
    },
    server: {
      host: '0.0.0.0',
      // 固定端口 + strictPort：避免占用时静默改 5174，换电脑/多项目时地址一致
      port: Number(env.VITE_DEV_PORT) || 5173,
      strictPort: true,
      proxy: {
        '/admin/api': {
          target: env.VITE_BACKEND_ORIGIN || 'http://xifg.com.cn:8090',
          changeOrigin: true
        },
        '/admin/ws': {
          target: env.VITE_BACKEND_ORIGIN || 'http://xifg.com.cn:8090',
          changeOrigin: true,
          ws: true
        }
      }
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor-vue': ['vue', 'vue-router', 'pinia'],
            'vendor-vant': ['vant'],
            'vendor-axios': ['axios']
          }
        }
      }
    }
  }
})
