<template>
  <!--
    小马的可爱标志（Cutie Mark），纯手绘 SVG，viewBox 统一 64x64。
    用 v-if 按 mark 类型画不同的图案，颜色直接写死（可爱标志是固定色）。
  -->
  <svg viewBox="0 0 64 64" class="cutie" aria-hidden="true">

    <!-- ===== 碧琪：三个气球（两蓝一黄） ===== -->
    <g v-if="mark === 'pinkie'">
      <path d="M22 34 Q19 40 23 44" stroke="#9b8fb0" stroke-width="1.6" fill="none" stroke-linecap="round" />
      <path d="M42 34 Q45 40 41 44" stroke="#9b8fb0" stroke-width="1.6" fill="none" stroke-linecap="round" />
      <path d="M32 47 Q32 52 30 56" stroke="#9b8fb0" stroke-width="1.6" fill="none" stroke-linecap="round" />
      <ellipse cx="22" cy="21" rx="9" ry="11" fill="#5ab4e8" />
      <path d="M20 31 L22 35 L24 31 Z" fill="#5ab4e8" />
      <ellipse cx="42" cy="21" rx="9" ry="11" fill="#5ab4e8" />
      <path d="M40 31 L42 35 L44 31 Z" fill="#5ab4e8" />
      <ellipse cx="32" cy="34" rx="8" ry="10" fill="#f7d94c" />
      <path d="M30 43 L32 47 L34 43 Z" fill="#f7d94c" />
    </g>

    <!-- ===== 紫悦：六芒星 + 两颗小星星 ===== -->
    <g v-else-if="mark === 'twilight'">
      <path d="M32 10 L48 37.8 L16 37.8 Z" fill="#ef5f9d" />
      <path d="M32 54 L48 26.2 L16 26.2 Z" fill="#ef5f9d" />
      <path d="M13 15 L15 20 L13 25 L11 20 Z" fill="#fff" stroke="#f3a7c8" stroke-width="1" />
      <path d="M51 39 L53 44 L51 49 L49 44 Z" fill="#fff" stroke="#f3a7c8" stroke-width="1" />
    </g>

    <!-- ===== 云宝：白云 + 彩虹闪电 ===== -->
    <g v-else-if="mark === 'dash'">
      <defs>
        <linearGradient id="rainbowBolt" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%"  stop-color="#ef5350" />
          <stop offset="30%" stop-color="#ffb74d" />
          <stop offset="50%" stop-color="#ffee58" />
          <stop offset="72%" stop-color="#66bb6a" />
          <stop offset="100%" stop-color="#42a5f5" />
        </linearGradient>
      </defs>
      <path d="M34 27 L50 27 L42 41 L52 41 L30 59 L37 46 L26 46 Z"
            fill="url(#rainbowBolt)" stroke="#fff" stroke-width="1.2" stroke-linejoin="round" />
      <g fill="#ffffff" stroke="#cfe4f3" stroke-width="1">
        <circle cx="20" cy="23" r="7" />
        <circle cx="30" cy="19" r="9" />
        <circle cx="40" cy="23" r="7" />
        <ellipse cx="30" cy="26" rx="16" ry="6" />
      </g>
    </g>

    <!-- ===== 苹果嘉儿：三个苹果 ===== -->
    <g v-else-if="mark === 'applejack'">
      <g v-for="(a, i) in [[21, 25], [43, 25], [32, 43]]" :key="i">
        <ellipse :cx="a[0]" :cy="a[1]" rx="9" ry="8.5" fill="#e85a4f" />
        <path :d="`M${a[0]} ${a[1] - 7.5} q0 -5 2.5 -6.5`" stroke="#7a5a3a" stroke-width="2" fill="none" stroke-linecap="round" />
        <ellipse :cx="a[0] + 4.5" :cy="a[1] - 11" rx="3" ry="1.8" fill="#66bb6a" :transform="`rotate(-30 ${a[0] + 4.5} ${a[1] - 11})`" />
      </g>
    </g>

    <!-- ===== 柔柔：三只粉蝴蝶 ===== -->
    <g v-else-if="mark === 'fluttershy'">
      <g v-for="(b, i) in [[22, 23], [42, 23], [32, 43]]" :key="i"
         :transform="`translate(${b[0]} ${b[1]})`">
        <ellipse cx="-6" cy="-2.5" rx="5.5" ry="3.5" fill="#f48fb1" transform="rotate(-20)" />
        <ellipse cx="6" cy="-2.5" rx="5.5" ry="3.5" fill="#f48fb1" transform="rotate(20)" />
        <ellipse cx="-4.5" cy="4" rx="4" ry="3" fill="#f8b8cd" transform="rotate(-10)" />
        <ellipse cx="4.5" cy="4" rx="4" ry="3" fill="#f8b8cd" transform="rotate(10)" />
        <ellipse cx="0" cy="0" rx="2" ry="5.5" fill="#d4709a" />
      </g>
    </g>

    <!-- ===== 珍奇：三颗钻石 ===== -->
    <g v-else-if="mark === 'rarity'">
      <g v-for="(d, i) in [[22, 26], [42, 26], [32, 44]]" :key="i">
        <path :d="`M${d[0]} ${d[1] - 11} L${d[0] + 9} ${d[1]} L${d[0]} ${d[1] + 11} L${d[0] - 9} ${d[1]} Z`" fill="#4fc3f7" />
        <path :d="`M${d[0]} ${d[1] - 6} L${d[0] + 5} ${d[1]} L${d[0]} ${d[1] + 6} L${d[0] - 5} ${d[1]} Z`" fill="#b6e9fb" />
      </g>
    </g>

    <!-- ===== 兜底：普通小星星 ===== -->
    <g v-else>
      <path d="M32 12 L37 27 L52 32 L37 37 L32 52 L27 37 L12 32 L27 27 Z" fill="#f7c94c" />
    </g>
  </svg>
</template>

<script>
export default {
  name: 'CutieMark',
  props: {
    /** 可爱标志类型：pinkie / twilight / dash / applejack / fluttershy / rarity */
    mark: { type: String, required: true }
  }
}
</script>
