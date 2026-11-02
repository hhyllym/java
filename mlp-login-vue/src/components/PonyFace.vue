<template>
  <!-- 助手头像：按 id 画可爱的卡通小马脸；没有对应画风的回退到 emoji -->
  <span class="pf">
    <!-- 穗龙：紫脸 + 绿耳绿角 + 大绿眼 + 笑嘴 -->
    <svg v-if="drawId === 'spike'" class="pf-svg" viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <defs>
        <linearGradient :id="gid + 'h'" x1="0.25" y1="0" x2="0.75" y2="1">
          <stop offset="0" stop-color="#d6b8f8" /><stop offset="1" stop-color="#a97fe2" />
        </linearGradient>
        <radialGradient :id="gid + 's'" cx="0.36" cy="0.24" r="0.55">
          <stop offset="0" stop-color="#ffffff" stop-opacity=".6" /><stop offset="1" stop-color="#ffffff" stop-opacity="0" />
        </radialGradient>
      </defs>
      <g><ellipse cx="9.9" cy="11.8" rx="3.1" ry="4.8" transform="rotate(-32 9.9 11.8)" fill="#84cda6" /><ellipse cx="9.9" cy="12" rx="1.5" ry="2.6" transform="rotate(-32 9.9 12)" fill="#b8ecd2" /></g>
      <g><ellipse cx="30.1" cy="11.8" rx="3.1" ry="4.8" transform="rotate(32 30.1 11.8)" fill="#84cda6" /><ellipse cx="30.1" cy="12" rx="1.5" ry="2.6" transform="rotate(32 30.1 12)" fill="#b8ecd2" /></g>
      <path d="M20 4.4 L23 11.4 L17 11.4 Z" fill="#84cda6" stroke="#84cda6" stroke-width="1.6" stroke-linejoin="round" />
      <ellipse cx="20" cy="21" rx="12.4" ry="11.4" :fill="'url(#' + gid + 'h)'" />
      <ellipse cx="20" cy="21" rx="12.4" ry="11.4" :fill="'url(#' + gid + 's)'" />
      <g class="pf-eyes">
        <ellipse cx="14.8" cy="21" rx="4" ry="4.7" fill="#fff" /><circle cx="14.9" cy="21.2" r="2.9" fill="#6dbf94" /><circle cx="14.9" cy="21.4" r="1.5" fill="#3d2a58" /><circle cx="13.7" cy="19.9" r="1" fill="#fff" /><circle cx="15.9" cy="22.5" r=".5" fill="#fff" opacity=".85" />
        <ellipse cx="25.2" cy="21" rx="4" ry="4.7" fill="#fff" /><circle cx="25.1" cy="21.2" r="2.9" fill="#6dbf94" /><circle cx="25.1" cy="21.4" r="1.5" fill="#3d2a58" /><circle cx="23.9" cy="19.9" r="1" fill="#fff" /><circle cx="26.1" cy="22.5" r=".5" fill="#fff" opacity=".85" />
      </g>
      <ellipse cx="9.6" cy="26.4" rx="2.9" ry="1.8" fill="#ff9ec4" opacity=".45" />
      <ellipse cx="30.4" cy="26.4" rx="2.9" ry="1.8" fill="#ff9ec4" opacity=".45" />
      <path d="M17 27.4 Q20 32.6 23 27.4 Z" fill="#6b4a8f" /><ellipse cx="20" cy="29.2" rx="1.4" ry=".75" fill="#ff9ec4" />
    </svg>

    <!-- 月亮公主：深蓝夜空脸 + 弯月额饰 + 青蓝眼 -->
    <svg v-else-if="drawId === 'luna'" class="pf-svg" viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <defs>
        <linearGradient :id="gid + 'h'" x1="0.25" y1="0" x2="0.75" y2="1">
          <stop offset="0" stop-color="#6d6cc4" /><stop offset="1" stop-color="#3b3a78" />
        </linearGradient>
      </defs>
      <path d="M11 9 q-4 3 -1 9 q5 -2 4 -9 z" fill="#5b5ab0" />
      <path d="M29 9 q4 3 1 9 q-5 -2 -4 -9 z" fill="#5b5ab0" />
      <path d="M20 3 a8 8 0 0 1 5 14 a10 10 0 1 0 0 -14 z" fill="#ffe27a" opacity=".95" />
      <ellipse cx="20" cy="22" rx="12.6" ry="11.6" :fill="'url(#' + gid + 'h)'" />
      <g class="pf-eyes">
        <ellipse cx="14.8" cy="22" rx="3.8" ry="4.6" fill="#fff" /><circle cx="14.9" cy="22.2" r="2.7" fill="#7fd6e6" /><circle cx="14.9" cy="22.4" r="1.4" fill="#26305e" /><circle cx="13.8" cy="20.9" r="1" fill="#fff" />
        <ellipse cx="25.2" cy="22" rx="3.8" ry="4.6" fill="#fff" /><circle cx="25.1" cy="22.2" r="2.7" fill="#7fd6e6" /><circle cx="25.1" cy="22.4" r="1.4" fill="#26305e" /><circle cx="24" cy="20.9" r="1" fill="#fff" />
      </g>
      <ellipse cx="9.6" cy="27.4" rx="2.8" ry="1.7" fill="#ff9ec4" opacity=".4" />
      <ellipse cx="30.4" cy="27.4" rx="2.8" ry="1.7" fill="#ff9ec4" opacity=".4" />
      <path d="M17.5 28 Q20 31 22.5 28" fill="none" stroke="#1f1e44" stroke-width="1.4" stroke-linecap="round" />
    </svg>

    <!-- 宇宙公主：奶白金脸 + 小太阳 + 暖橙粉眼 -->
    <svg v-else-if="drawId === 'celestia'" class="pf-svg" viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <defs>
        <linearGradient :id="gid + 'h'" x1="0.25" y1="0" x2="0.75" y2="1">
          <stop offset="0" stop-color="#fff4d6" /><stop offset="1" stop-color="#f6d9a6" />
        </linearGradient>
      </defs>
      <g fill="#f4b13a">
        <circle cx="20" cy="6" r="3.2" />
        <path d="M20 1 l1.2 3 h-2.4 z" /><path d="M20 11 l1.2 -3 h-2.4 z" />
        <path d="M11 6 l3 1.2 v-2.4 z" /><path d="M29 6 l-3 1.2 v-2.4 z" />
      </g>
      <ellipse cx="20" cy="22" rx="12.6" ry="11.6" :fill="'url(#' + gid + 'h)'" />
      <g class="pf-eyes">
        <ellipse cx="14.8" cy="22" rx="3.8" ry="4.6" fill="#fff" /><circle cx="14.9" cy="22.2" r="2.7" fill="#f3a05a" /><circle cx="14.9" cy="22.4" r="1.4" fill="#5a3a1e" /><circle cx="13.8" cy="20.9" r="1" fill="#fff" />
        <ellipse cx="25.2" cy="22" rx="3.8" ry="4.6" fill="#fff" /><circle cx="25.1" cy="22.2" r="2.7" fill="#f3a05a" /><circle cx="25.1" cy="22.4" r="1.4" fill="#5a3a1e" /><circle cx="24" cy="20.9" r="1" fill="#fff" />
      </g>
      <ellipse cx="9.6" cy="27.4" rx="2.8" ry="1.7" fill="#ff9ec4" opacity=".4" />
      <ellipse cx="30.4" cy="27.4" rx="2.8" ry="1.7" fill="#ff9ec4" opacity=".4" />
      <path d="M17.5 28 Q20 31.4 22.5 28" fill="none" stroke="#8a5a2a" stroke-width="1.5" stroke-linecap="round" />
    </svg>

    <!-- 紫悦：薰衣草脸 + 独角 + 六芒星 + 紫眼 -->
    <svg v-else-if="drawId === 'twilight'" class="pf-svg" viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <defs>
        <linearGradient :id="gid + 'h'" x1="0.25" y1="0" x2="0.75" y2="1">
          <stop offset="0" stop-color="#c8a8f2" /><stop offset="1" stop-color="#9b78e0" />
        </linearGradient>
      </defs>
      <path d="M20 3 L22 12 L18 12 Z" fill="#7c5cc4" />
      <ellipse cx="20" cy="22" rx="12.6" ry="11.6" :fill="'url(#' + gid + 'h)'" />
      <path d="M27 12 l1.1 2.4 -2.5 -.4 1.8 2 -2.1 -1 1.5 2.2 -2.3 -1.1 1 -.2 -2.3 1.1 1.5 -2.2 -2.1 1 1.8 -2 -2.5 .4 1.1 -2.4 z" fill="#fff" opacity=".85" />
      <g class="pf-eyes">
        <ellipse cx="14.8" cy="22" rx="3.8" ry="4.6" fill="#fff" /><circle cx="14.9" cy="22.2" r="2.7" fill="#8a5cd0" /><circle cx="14.9" cy="22.4" r="1.4" fill="#2a1a4e" /><circle cx="13.8" cy="20.9" r="1" fill="#fff" />
        <ellipse cx="25.2" cy="22" rx="3.8" ry="4.6" fill="#fff" /><circle cx="25.1" cy="22.2" r="2.7" fill="#8a5cd0" /><circle cx="25.1" cy="22.4" r="1.4" fill="#2a1a4e" /><circle cx="24" cy="20.9" r="1" fill="#fff" />
      </g>
      <ellipse cx="9.6" cy="27.4" rx="2.8" ry="1.7" fill="#ff9ec4" opacity=".42" />
      <ellipse cx="30.4" cy="27.4" rx="2.8" ry="1.7" fill="#ff9ec4" opacity=".42" />
      <path d="M17.5 28 Q20 31 22.5 28" fill="none" stroke="#3d2a58" stroke-width="1.4" stroke-linecap="round" />
    </svg>

    <!-- 珍奇：白紫脸 + 三钻额饰 + 紫睫大眼 -->
    <svg v-else-if="drawId === 'rarity'" class="pf-svg" viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <defs>
        <linearGradient :id="gid + 'h'" x1="0.25" y1="0" x2="0.75" y2="1">
          <stop offset="0" stop-color="#ffffff" /><stop offset="1" stop-color="#e7dcf6" />
        </linearGradient>
      </defs>
      <g fill="#b58fe0">
        <path d="M14 4 l1.6 3.2 -3.2 0 z" transform="rotate(0 14 5.6)" />
        <path d="M20 3 l1.6 3.2 -3.2 0 z" />
        <path d="M26 4 l1.6 3.2 -3.2 0 z" />
      </g>
      <ellipse cx="20" cy="22" rx="12.6" ry="11.6" :fill="'url(#' + gid + 'h)'" />
      <g class="pf-eyes">
        <ellipse cx="14.8" cy="22" rx="3.6" ry="4.6" fill="#fff" /><circle cx="14.9" cy="22.2" r="2.7" fill="#9b6fd0" /><circle cx="14.9" cy="22.4" r="1.4" fill="#2a1a4e" /><circle cx="13.8" cy="20.9" r="1" fill="#fff" />
        <path d="M11.4 19.4 q3.4 -2.2 6.8 0" fill="none" stroke="#9b6fd0" stroke-width="1" stroke-linecap="round" />
        <ellipse cx="25.2" cy="22" rx="3.6" ry="4.6" fill="#fff" /><circle cx="25.1" cy="22.2" r="2.7" fill="#9b6fd0" /><circle cx="25.1" cy="22.4" r="1.4" fill="#2a1a4e" /><circle cx="24" cy="20.9" r="1" fill="#fff" />
        <path d="M21.8 19.4 q3.4 -2.2 6.8 0" fill="none" stroke="#9b6fd0" stroke-width="1" stroke-linecap="round" />
      </g>
      <ellipse cx="9.8" cy="27.4" rx="2.6" ry="1.6" fill="#ff9ec4" opacity=".4" />
      <ellipse cx="30.2" cy="27.4" rx="2.6" ry="1.6" fill="#ff9ec4" opacity=".4" />
      <path d="M17.5 28 Q20 30.6 22.5 28" fill="none" stroke="#7a5aa8" stroke-width="1.4" stroke-linecap="round" />
    </svg>

    <!-- 云宝黛西：天蓝脸 + 彩虹鬃毛小簇 + 小白翅 + 亮蓝大眼 -->
    <svg v-else-if="drawId === 'rainbow'" class="pf-svg" viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <defs>
        <linearGradient :id="gid + 'h'" x1="0.25" y1="0" x2="0.75" y2="1">
          <stop offset="0" stop-color="#bfe7f7" /><stop offset="1" stop-color="#8fd0ef" />
        </linearGradient>
      </defs>
      <g>
        <path d="M20 2.5 q-3 1 -2 6 q3 -2 2 -6 z" fill="#ff6b6b" />
        <path d="M20 2.5 q0 3 1 6 q-3 -2 -1 -6 z" fill="#ffd166" />
        <path d="M20 2.5 q3 1 2 6 q-3 -2 -2 -6 z" fill="#5ad1e6" />
      </g>
      <ellipse cx="7" cy="24" rx="3.2" ry="2" fill="#ffffff" opacity=".85" transform="rotate(-18 7 24)" />
      <ellipse cx="33" cy="24" rx="3.2" ry="2" fill="#ffffff" opacity=".85" transform="rotate(18 33 24)" />
      <ellipse cx="20" cy="22" rx="12.6" ry="11.6" :fill="'url(#' + gid + 'h)'" />
      <g class="pf-eyes">
        <ellipse cx="14.8" cy="22" rx="4" ry="4.7" fill="#fff" /><circle cx="14.9" cy="22.2" r="2.9" fill="#39b6e8" /><circle cx="14.9" cy="22.4" r="1.5" fill="#274b6b" /><circle cx="13.7" cy="20.9" r="1" fill="#fff" /><circle cx="15.9" cy="23.5" r=".5" fill="#fff" opacity=".85" />
        <ellipse cx="25.2" cy="22" rx="4" ry="4.7" fill="#fff" /><circle cx="25.1" cy="22.2" r="2.9" fill="#39b6e8" /><circle cx="25.1" cy="22.4" r="1.5" fill="#274b6b" /><circle cx="24" cy="20.9" r="1" fill="#fff" /><circle cx="26.1" cy="23.5" r=".5" fill="#fff" opacity=".85" />
      </g>
      <ellipse cx="9.6" cy="27" rx="2.8" ry="1.7" fill="#ff9ec4" opacity=".5" />
      <ellipse cx="30.4" cy="27" rx="2.8" ry="1.7" fill="#ff9ec4" opacity=".5" />
      <path d="M17.5 28 Q20 31 22.5 28" fill="none" stroke="#2a6f8f" stroke-width="1.4" stroke-linecap="round" />
    </svg>

    <!-- 苹果嘉儿：暖橘脸 + 棕黄鬃毛 + 绿眼 + 雀斑 -->
    <svg v-else-if="drawId === 'applejack'" class="pf-svg" viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <defs>
        <linearGradient :id="gid + 'h'" x1="0.25" y1="0" x2="0.75" y2="1">
          <stop offset="0" stop-color="#f6d39a" /><stop offset="1" stop-color="#e3a866" />
        </linearGradient>
      </defs>
      <path d="M20 2.5 q-3 2 -1 7 q2 -3 1 -7 z" fill="#caa05a" />
      <path d="M20 2.5 q3 2 1 7 q-2 -3 -1 -7 z" fill="#caa05a" />
      <ellipse cx="20" cy="22" rx="12.6" ry="11.6" :fill="'url(#' + gid + 'h)'" />
      <g class="pf-eyes">
        <ellipse cx="14.8" cy="22" rx="3.8" ry="4.6" fill="#fff" /><circle cx="14.9" cy="22.2" r="2.7" fill="#76c47a" /><circle cx="14.9" cy="22.4" r="1.4" fill="#2e4a2e" /><circle cx="13.8" cy="20.9" r="1" fill="#fff" />
        <ellipse cx="25.2" cy="22" rx="3.8" ry="4.6" fill="#fff" /><circle cx="25.1" cy="22.2" r="2.7" fill="#76c47a" /><circle cx="25.1" cy="22.4" r="1.4" fill="#2e4a2e" /><circle cx="24" cy="20.9" r="1" fill="#fff" />
      </g>
      <g fill="#c98a52" opacity=".75">
        <circle cx="11" cy="26" r=".6" /><circle cx="13" cy="27.4" r=".6" />
        <circle cx="29" cy="26" r=".6" /><circle cx="27" cy="27.4" r=".6" />
      </g>
      <ellipse cx="9.6" cy="27.4" rx="2.6" ry="1.6" fill="#ff9ec4" opacity=".4" />
      <ellipse cx="30.4" cy="27.4" rx="2.6" ry="1.6" fill="#ff9ec4" opacity=".4" />
      <path d="M17.5 28 Q20 31 22.5 28" fill="none" stroke="#9a6a32" stroke-width="1.4" stroke-linecap="round" />
    </svg>

    <!-- 柔柔：鹅黄脸 + 粉鬃毛 + 大眼长睫 + 蝴蝶小标志 -->
    <svg v-else-if="drawId === 'fluttershy'" class="pf-svg" viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <defs>
        <linearGradient :id="gid + 'h'" x1="0.25" y1="0" x2="0.75" y2="1">
          <stop offset="0" stop-color="#fff0b8" /><stop offset="1" stop-color="#f6d77e" />
        </linearGradient>
      </defs>
      <path d="M20 2.5 q-4 2 -2 8 q3 -3 2 -8 z" fill="#f7b8d6" />
      <path d="M20 2.5 q4 2 2 8 q-3 -3 -2 -8 z" fill="#f7b8d6" />
      <ellipse cx="20" cy="22" rx="12.6" ry="11.6" :fill="'url(#' + gid + 'h)'" />
      <g class="pf-eyes">
        <ellipse cx="14.6" cy="22" rx="4.1" ry="4.9" fill="#fff" />
        <path d="M10.8 19.6 q3.8 -2.6 7.6 0" fill="none" stroke="#d98fb8" stroke-width="1" stroke-linecap="round" />
        <circle cx="14.7" cy="22.4" r="3" fill="#7fb0e0" /><circle cx="14.7" cy="22.6" r="1.6" fill="#3a2a4e" /><circle cx="13.4" cy="20.9" r="1.1" fill="#fff" /><circle cx="15.8" cy="23.6" r=".5" fill="#fff" opacity=".85" />
        <ellipse cx="25.4" cy="22" rx="4.1" ry="4.9" fill="#fff" />
        <path d="M21.6 19.6 q3.8 -2.6 7.6 0" fill="none" stroke="#d98fb8" stroke-width="1" stroke-linecap="round" />
        <circle cx="25.3" cy="22.4" r="3" fill="#7fb0e0" /><circle cx="25.3" cy="22.6" r="1.6" fill="#3a2a4e" /><circle cx="24" cy="20.9" r="1.1" fill="#fff" /><circle cx="26.6" cy="23.6" r=".5" fill="#fff" opacity=".85" />
      </g>
      <g transform="translate(31 14)" fill="#f29ab5" opacity=".9">
        <ellipse cx="-1.4" cy="0" rx="1.6" ry="2.2" /><ellipse cx="1.4" cy="0" rx="1.6" ry="2.2" />
        <circle cx="0" cy="0" r=".7" />
      </g>
      <ellipse cx="9.4" cy="27.4" rx="3" ry="1.8" fill="#ff9ec4" opacity=".5" />
      <ellipse cx="30.6" cy="27.4" rx="3" ry="1.8" fill="#ff9ec4" opacity=".5" />
      <path d="M17.5 28 Q20 30.4 22.5 28" fill="none" stroke="#c79a3a" stroke-width="1.4" stroke-linecap="round" />
    </svg>

    <!-- 碧琪：粉脸 + 蓬松卷鬃毛 + 气球簇 + 闪亮大眼 + 大笑 -->
    <svg v-else-if="drawId === 'pinkie'" class="pf-svg" viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <defs>
        <linearGradient :id="gid + 'h'" x1="0.25" y1="0" x2="0.75" y2="1">
          <stop offset="0" stop-color="#fcb3d4" /><stop offset="1" stop-color="#f48fb5" />
        </linearGradient>
      </defs>
      <g fill="#ef6fa6">
        <circle cx="13" cy="9" r="3.2" /><circle cx="20" cy="7" r="3.4" /><circle cx="27" cy="9" r="3.2" />
      </g>
      <circle cx="20" cy="3.2" r="2" fill="#ff6f91" />
      <path d="M20 5.2 v2.4" stroke="#ff6f91" stroke-width=".8" />
      <ellipse cx="20" cy="22" rx="12.6" ry="11.6" :fill="'url(#' + gid + 'h)'" />
      <g class="pf-eyes">
        <ellipse cx="14.8" cy="22" rx="4.1" ry="4.9" fill="#fff" /><circle cx="14.9" cy="22.2" r="3" fill="#7a4a8a" /><circle cx="14.9" cy="22.4" r="1.6" fill="#2a1a3e" /><circle cx="13.5" cy="20.7" r="1.2" fill="#fff" /><circle cx="16" cy="23.6" r=".5" fill="#fff" opacity=".9" />
        <ellipse cx="25.2" cy="22" rx="4.1" ry="4.9" fill="#fff" /><circle cx="25.1" cy="22.2" r="3" fill="#7a4a8a" /><circle cx="25.1" cy="22.4" r="1.6" fill="#2a1a3e" /><circle cx="23.7" cy="20.7" r="1.2" fill="#fff" /><circle cx="26.4" cy="23.6" r=".5" fill="#fff" opacity=".9" />
      </g>
      <ellipse cx="9.6" cy="27.2" rx="2.9" ry="1.8" fill="#ff7fae" opacity=".55" />
      <ellipse cx="30.4" cy="27.2" rx="2.9" ry="1.8" fill="#ff7fae" opacity=".55" />
      <path d="M16 28 Q20 33 24 28 Q20 30.5 16 28 Z" fill="#7a2e48" />
      <path d="M18.4 29.6 Q20 31.4 21.6 29.6 Z" fill="#ff7fae" />
    </svg>

    <span v-else class="pf-emoji">{{ a.emoji }}</span>
  </span>
</template>

<script>
export default {
  name: 'PonyFace',
  props: {
    a: { type: Object, required: true }
  },
  computed: {
    /** 需要画卡通脸的助手；其余回退 emoji */
    drawId() {
      const id = this.a && this.a.id
      return ['spike', 'luna', 'celestia', 'twilight', 'rarity', 'rainbow', 'applejack', 'fluttershy', 'pinkie'].includes(id) ? id : null
    },
    /** 渐变 id 唯一，避免多处实例互相覆盖 */
    gid() {
      return 'pf' + this.$.uid + '-'
    }
  }
}
</script>

<style scoped>
.pf {
  display: inline-flex; align-items: center; justify-content: center;
  width: 1.18em; height: 1.18em; line-height: 1;
}
.pf-svg { width: 100%; height: 100%; display: block; overflow: visible; }
.pf-emoji { font-size: 1em; line-height: 1; }

/* 眼睛每 5 秒眨一下（整体纵向压扁，绕中心） */
.pf-eyes { transform-box: view-box; transform-origin: 20px 21px; animation: pfBlink 5.2s ease-in-out infinite; }
@keyframes pfBlink {
  0%, 93%, 100% { transform: scaleY(1); }
  95.5% { transform: scaleY(.08); }
}
</style>
