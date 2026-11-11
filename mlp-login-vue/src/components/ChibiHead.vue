<template>
  <!-- 大头 Q 版（无身体）：大圆头 + 马耳 + 鬃毛发色 + 大眼睛 + 腮红，白色描边贴纸感 -->
  <svg class="chibi-head" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <defs>
      <linearGradient :id="gid + 'h'" x1="0.3" y1="0" x2="0.7" y2="1">
        <stop offset="0" :stop-color="c.hair1" /><stop offset="1" :stop-color="c.hair2" />
      </linearGradient>
      <radialGradient :id="gid + 's'" cx="0.4" cy="0.2" r="0.65">
        <stop offset="0" stop-color="#ffffff" stop-opacity=".5" /><stop offset="1" stop-color="#ffffff" stop-opacity="0" />
      </radialGradient>
    </defs>

    <!-- 后层头发（整片 + 两侧短尾） -->
    <path :fill="'url(#' + gid + 'h)'" d="M50 6 q44 0 46 46 q2 40 -14 48 q-32 12 -64 0 q-16 -8 -14 -48 q2 -46 46 -46 z" />

    <!-- 马耳朵（头顶两侧，立起，白描边） -->
    <g v-if="c.id !== 'spike'" :fill="c.hair2" stroke="#ffffff" stroke-width="2" stroke-linejoin="round">
      <path d="M30 28 L24 2 L44 18 Z" />
      <path d="M70 28 L76 2 L56 18 Z" />
    </g>
    <path v-if="c.id !== 'spike'" :fill="c.hair1" d="M33 24 L28 8 L40 19 Z" />
    <path v-if="c.id !== 'spike'" :fill="c.hair1" d="M67 24 L72 8 L60 19 Z" />
    <!-- 豆豆龙：绿色龙鳍耳 -->
    <g v-else fill="#84cda6" stroke="#ffffff" stroke-width="2" stroke-linejoin="round">
      <path d="M30 26 q-14 -6 -12 -26 q10 6 16 14 z" />
      <path d="M70 26 q14 -6 12 -26 q-10 6 -16 14 z" />
    </g>

    <!-- 脸（大圆头） -->
    <g class="cb-head">
      <circle cx="50" cy="56" r="35" :fill="c.skin" stroke="#ffffff" stroke-width="2.2" />
      <circle cx="50" cy="56" r="35" :fill="'url(#' + gid + 's)'" />

      <!-- 刘海 -->
      <path :fill="c.hair1" d="M16 52 q-2 -36 34 -36 q36 0 34 36 q-10 -18 -22 -14 q-10 -16 -22 -2 q-12 -4 -24 16 z" />

      <!-- 角色小发饰 -->
      <g v-if="c.acc === 'star'" fill="#f8e9a0" opacity=".95" transform="translate(70 26)">
        <ellipse cx="0" cy="-3" rx="1.4" ry="2.9" /><ellipse cx="0" cy="-3" rx="1.4" ry="2.9" transform="rotate(60)" /><ellipse cx="0" cy="-3" rx="1.4" ry="2.9" transform="rotate(120)" /><ellipse cx="0" cy="-3" rx="1.4" ry="2.9" transform="rotate(180)" /><ellipse cx="0" cy="-3" rx="1.4" ry="2.9" transform="rotate(240)" /><ellipse cx="0" cy="-3" rx="1.4" ry="2.9" transform="rotate(300)" /><circle r="1.5" fill="#ffffff" />
      </g>
      <path v-else-if="c.acc === 'moon'" d="M74 14 a9 9 0 0 1 6 16 a11 11 0 1 0 0 -16 z" fill="#ffe27a" opacity=".95" transform="translate(-4 1) scale(.85)" />
      <g v-else-if="c.acc === 'sun'" fill="#f4b13a" transform="translate(72 24) scale(.8)">
        <circle r="3.8" /><path d="M0 -8 l1.4 3.6 h-2.8 z" /><path d="M0 8 l1.4 -3.6 h-2.8 z" /><path d="M-8 0 l3.6 1.4 v-2.8 z" /><path d="M8 0 l-3.6 1.4 v-2.8 z" />
      </g>
      <g v-else-if="c.acc === 'diamond'" fill="#b58fe0" transform="translate(72 24) scale(1.1)">
        <path d="M0 -3.6 l2.2 3.6 -2.2 3.6 -2.2 -3.6 z" /><path d="M-6.5 -2.8 l1.8 2.8 -1.8 2.8 -1.8 -2.8 z" /><path d="M6.5 -2.8 l1.8 2.8 -1.8 2.8 -1.8 -2.8 z" />
      </g>
      <g v-else-if="c.acc === 'cloud'" fill="#ffffff" opacity=".95" transform="translate(72 22)">
        <ellipse rx="5" ry="2.8" /><circle cx="-2.2" cy="-1.8" r="2.4" /><circle cx="2.2" cy="-1.6" r="1.9" />
      </g>
      <g v-else-if="c.acc === 'apple'" transform="translate(72 24)">
        <circle cx="-1.6" cy="0" r="3" fill="#e05a4a" /><circle cx="1.8" cy="0" r="3" fill="#e05a4a" /><path d="M0 -3.2 q1 -3.4 3.4 -4" fill="none" stroke="#6fb86d" stroke-width="1.4" stroke-linecap="round" />
      </g>
      <g v-else-if="c.acc === 'butterfly'" transform="translate(72 24)">
        <ellipse cx="-2.6" cy="-1" rx="2.8" ry="2" fill="#f7a8c4" transform="rotate(-24)" /><ellipse cx="2.6" cy="-1" rx="2.8" ry="2" fill="#f7a8c4" transform="rotate(24)" /><rect x="-.6" y="-2.8" width="1.2" height="5.8" rx=".6" fill="#a06cd5" />
      </g>
      <g v-else-if="c.acc === 'balloon'" transform="translate(72 22)">
        <ellipse rx="3.6" ry="4.2" fill="#e56a9e" /><path d="M0 4.2 l-1.5 2.8 h3 z" fill="#e56a9e" /><path d="M0 7 q2 3 0 5.4" fill="none" stroke="#e56a9e" stroke-width="1" stroke-linecap="round" />
      </g>

      <!-- 大眼睛（高光 + 长睫） -->
      <g class="cb-eyes">
        <g>
          <ellipse cx="38" cy="66" rx="9" ry="10.6" fill="#ffffff" stroke="#e3d4ee" stroke-width=".9" />
          <circle cx="38.5" cy="67" r="6.8" :fill="c.eye" />
          <circle cx="38.5" cy="67.8" r="3.6" fill="#33234a" />
          <circle cx="35.6" cy="62.8" r="2.4" fill="#ffffff" />
          <circle cx="40.6" cy="70" r="1.1" fill="#ffffff" opacity=".85" />
        </g>
        <g>
          <ellipse cx="62" cy="66" rx="9" ry="10.6" fill="#ffffff" stroke="#e3d4ee" stroke-width=".9" />
          <circle cx="61.5" cy="67" r="6.8" :fill="c.eye" />
          <circle cx="61.5" cy="67.8" r="3.6" fill="#33234a" />
          <circle cx="58.6" cy="62.8" r="2.4" fill="#ffffff" />
          <circle cx="63.6" cy="70" r="1.1" fill="#ffffff" opacity=".85" />
        </g>
      </g>

      <!-- 腮红 + 小嘴 -->
      <ellipse cx="28" cy="78" rx="5.6" ry="3.1" fill="#ff9ec4" opacity=".5" />
      <ellipse cx="72" cy="78" rx="5.6" ry="3.1" fill="#ff9ec4" opacity=".5" />
      <path d="M44 80 Q50 86 56 80" fill="none" stroke="#8a5a6a" stroke-width="2" stroke-linecap="round" />
    </g>
  </svg>
</template>

<script>
/**
 * 大头 Q 版（无身体）：带马耳 + 鬃毛发色发饰的 Q 版头像。
 * 用法 <ChibiHead id="twilight" />，宽高由外层容器控制。
 */
const CHARS = {
  spike:      { id: 'spike',      skin: '#fdeee4', hair1: '#a97fe2', hair2: '#8d5fd0', eye: '#6dbf94', acc: null },
  luna:       { id: 'luna',       skin: '#eceaf8', hair1: '#5b5ab0', hair2: '#3b3a78', eye: '#7fd6e6', acc: 'moon' },
  celestia:   { id: 'celestia',   skin: '#fff4ea', hair1: '#f6d9a6', hair2: '#eecbe0', eye: '#f3a05a', acc: 'sun' },
  twilight:   { id: 'twilight',   skin: '#fdeee4', hair1: '#9b78e0', hair2: '#7c5cc4', eye: '#8a5cd0', acc: 'star' },
  rarity:     { id: 'rarity',     skin: '#fdf6fb', hair1: '#b58fe0', hair2: '#9b6fd0', eye: '#9b6fd0', acc: 'diamond' },
  rainbow:    { id: 'rainbow',    skin: '#fdeee4', hair1: '#5ba3e0', hair2: '#3f86cf', eye: '#4aa3e8', acc: 'cloud' },
  applejack:  { id: 'applejack',  skin: '#fde8d8', hair1: '#f0c05a', hair2: '#e0a63e', eye: '#5aa85a', acc: 'apple' },
  fluttershy: { id: 'fluttershy', skin: '#fdeee4', hair1: '#f3d9a6', hair2: '#eccf96', eye: '#6dbf94', acc: 'butterfly' },
  pinkie:     { id: 'pinkie',     skin: '#fdeee4', hair1: '#f28ab8', hair2: '#e56a9e', eye: '#d84a8a', acc: 'balloon' }
}

export default {
  name: 'ChibiHead',
  props: {
    /** 角色 id；未知 id 回退为泡芙 */
    id: { type: String, default: 'pinkie' }
  },
  computed: {
    c() {
      return CHARS[this.id] || CHARS.pinkie
    },
    gid() {
      return 'ch' + this.$.uid + '-'
    }
  }
}
</script>

<style scoped>
.chibi-head { display: block; width: 100%; height: auto; }
/* 头部轻轻摆动 */
.cb-head {
  transform-box: view-box; transform-origin: 50px 56px;
  animation: cbHeadTilt 6.5s ease-in-out infinite;
}
@keyframes cbHeadTilt {
  0%,100% { transform: rotate(-2.4deg); }
  50%     { transform: rotate(2.4deg); }
}
/* 眨眼 */
.cb-eyes { transform-box: view-box; transform-origin: 50px 66px; animation: cbBlink 5.2s ease-in-out infinite; }
@keyframes cbBlink {
  0%, 93%, 100% { transform: scaleY(1); }
  95.5% { transform: scaleY(.08); }
}
</style>
