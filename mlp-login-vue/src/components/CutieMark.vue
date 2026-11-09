<template>
  <!--
    小马乐园「心印徽记」，纯手绘 SVG，viewBox 统一 64x64。
    六枚心印全部为本项目原创图案：糖霜杯糕 / 星芒书页 / 彗星流云 /
    金麦穗 / 嫩叶绒球 / 缎带菱晶，不使用任何既有作品的标志造型。
    mark 是内部图案 key（pinkie / twilight / dash / applejack / fluttershy / rarity），
    只为兼容已有存档与数据，界面上不展示。
  -->
  <svg viewBox="0 0 64 64" class="cutie" aria-hidden="true">

    <!-- ===== 泡芙 · 欢悦：三只糖霜纸杯蛋糕 ===== -->
    <g v-if="mark === 'pinkie'">
      <g v-for="(c, i) in [[20, 26, '#f6a8c4', '#fff1b8'], [44, 26, '#8fd0ea', '#fff1b8'], [32, 44, '#f8cf6b', '#ffd8e6']]" :key="i">
        <!-- 杯托 -->
        <path :d="`M${c[0] - 8} ${c[1] - 1} L${c[0] + 8} ${c[1] - 1} L${c[0] + 5.5} ${c[1] + 11} L${c[0] - 5.5} ${c[1] + 11} Z`"
              :fill="c[2]" stroke="#d98fae" stroke-width="1" stroke-linejoin="round" />
        <path :d="`M${c[0] - 6} ${c[1] + 3} H${c[0] + 6} M${c[0] - 5} ${c[1] + 7} H${c[0] + 5}`"
              stroke="#fff" stroke-width="1" opacity="0.75" />
        <!-- 奶油顶 -->
        <path :d="`M${c[0] - 8} ${c[1] - 1} q1 -7 6 -6 q2 -6 6 -3 q5 -1 4 6 q2 4 -3 4 h-11 q-3 0 -2 -1 Z`"
              :fill="c[3]" stroke="#e9c9a0" stroke-width="0.9" />
        <circle :cx="c[0]" :cy="c[1] - 10" r="2.4" fill="#ef6f8e" />
        <path :d="`M${c[0]} ${c[1] - 12} q1 -3 3 -3`" stroke="#79b46a" stroke-width="1.2" fill="none" stroke-linecap="round" />
      </g>
      <!-- 彩屑 -->
      <circle cx="12" cy="14" r="1.8" fill="#8fd0ea" />
      <circle cx="52" cy="16" r="1.8" fill="#f6a8c4" />
      <circle cx="51" cy="52" r="1.6" fill="#f8cf6b" />
      <circle cx="13" cy="52" r="1.6" fill="#b9a3e8" />
    </g>

    <!-- ===== 星澜 · 星辉：六瓣星芒 + 摊开的书页 ===== -->
    <g v-else-if="mark === 'twilight'">
      <g transform="translate(32 22)">
        <g fill="#9a83e8">
          <ellipse cx="0" cy="-9" rx="4.2" ry="8" />
          <ellipse cx="0" cy="-9" rx="4.2" ry="8" transform="rotate(60)" />
          <ellipse cx="0" cy="-9" rx="4.2" ry="8" transform="rotate(120)" />
          <ellipse cx="0" cy="-9" rx="4.2" ry="8" transform="rotate(180)" />
          <ellipse cx="0" cy="-9" rx="4.2" ry="8" transform="rotate(240)" />
          <ellipse cx="0" cy="-9" rx="4.2" ry="8" transform="rotate(300)" />
        </g>
        <circle cx="0" cy="0" r="4.6" fill="#fff5c8" />
      </g>
      <path d="M9 39 Q20 34 32 39 Q44 34 55 39 L55 53 Q44 48 32 53 Q20 48 9 53 Z"
            fill="#fff" stroke="#c7b6ec" stroke-width="1.4" stroke-linejoin="round" />
      <path d="M32 39 V53" stroke="#c7b6ec" stroke-width="1.4" />
      <path d="M14 43 Q21 40 28 43 M36 43 Q43 40 50 43" stroke="#d8cdec" stroke-width="1.2" fill="none" />
      <circle cx="11" cy="17" r="1.8" fill="#f8cf6b" />
      <circle cx="53" cy="15" r="1.6" fill="#f6a8c4" />
    </g>

    <!-- ===== 云驰 · 守护：彗星穿过流云 ===== -->
    <g v-else-if="mark === 'dash'">
      <defs>
        <linearGradient id="cometTail" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="#8fd0ea" stop-opacity="0.95" />
          <stop offset="100%" stop-color="#8fd0ea" stop-opacity="0" />
        </linearGradient>
      </defs>
      <!-- 三道渐隐尾迹 -->
      <path d="M12 12 Q26 18 38 30" stroke="url(#cometTail)" stroke-width="4" fill="none" stroke-linecap="round" />
      <path d="M18 8 Q32 16 42 27" stroke="url(#cometTail)" stroke-width="3" fill="none" stroke-linecap="round" />
      <path d="M8 22 Q22 25 34 33" stroke="url(#cometTail)" stroke-width="2.5" fill="none" stroke-linecap="round" />
      <!-- 彗星头 -->
      <circle cx="42" cy="31" r="7" fill="#fff3c4" stroke="#f2c86a" stroke-width="1.4" />
      <circle cx="40" cy="29" r="2.2" fill="#fff" />
      <!-- 流云 -->
      <g fill="#fff" stroke="#bcd9ec" stroke-width="1.2">
        <circle cx="20" cy="46" r="7" />
        <circle cx="31" cy="42" r="9" />
        <circle cx="42" cy="46" r="7" />
        <ellipse cx="31" cy="49" rx="17" ry="6" />
      </g>
      <path d="M12 55 H50" stroke="#cbe4f2" stroke-width="1.6" stroke-linecap="round" />
    </g>

    <!-- ===== 金穗 · 真切：三支金色麦穗 ===== -->
    <g v-else-if="mark === 'applejack'">
      <g v-for="(a, i) in [[22, 32, -22], [32, 28, 0], [42, 32, 22]]" :key="i"
         :transform="`translate(${a[0]} ${a[1]}) rotate(${a[2]})`">
        <path d="M0 18 V-6" stroke="#c99a48" stroke-width="2" stroke-linecap="round" />
        <g fill="#f2c768" stroke="#d8a94f" stroke-width="0.8">
          <ellipse cx="0" cy="-11" rx="3.2" ry="5" />
          <ellipse cx="-4" cy="-6" rx="2.8" ry="4.4" transform="rotate(-28 -4 -6)" />
          <ellipse cx="4" cy="-6" rx="2.8" ry="4.4" transform="rotate(28 4 -6)" />
          <ellipse cx="-4.6" cy="0" rx="2.6" ry="4" transform="rotate(-28 -4.6 0)" />
          <ellipse cx="4.6" cy="0" rx="2.6" ry="4" transform="rotate(28 4.6 0)" />
          <ellipse cx="-4" cy="6" rx="2.4" ry="3.6" transform="rotate(-28 -4 6)" />
          <ellipse cx="4" cy="6" rx="2.4" ry="3.6" transform="rotate(28 4 6)" />
        </g>
      </g>
      <path d="M16 50 Q32 55 48 50" stroke="#c99a48" stroke-width="2" fill="none" stroke-linecap="round" />
    </g>

    <!-- ===== 茸茸 · 柔心：三片嫩叶围着绒球 ===== -->
    <g v-else-if="mark === 'fluttershy'">
      <g fill="#a8ddb4" stroke="#79bd8c" stroke-width="1">
        <path d="M32 30 Q18 26 15 14 Q28 14 32 30 Z" />
        <path d="M32 30 Q46 26 49 14 Q36 14 32 30 Z" />
        <path d="M32 34 Q22 42 20 54 Q32 50 32 34 Z" />
        <path d="M32 34 Q42 42 44 54 Q32 50 32 34 Z" />
      </g>
      <path d="M32 30 V52" stroke="#79bd8c" stroke-width="1.4" fill="none" />
      <circle cx="32" cy="27" r="7.5" fill="#fff6f1" stroke="#f3c9c0" stroke-width="1.2" />
      <circle cx="29.5" cy="25" r="1.5" fill="#e79c9c" />
      <circle cx="34.5" cy="25" r="1.5" fill="#e79c9c" />
      <path d="M30 30 Q32 32 34 30" stroke="#e79c9c" stroke-width="1.2" fill="none" stroke-linecap="round" />
    </g>

    <!-- ===== 绮罗 · 分享：三枚缎带菱晶 ===== -->
    <g v-else-if="mark === 'rarity'">
      <g v-for="(d, i) in [[22, 26, '#a8d8f7'], [42, 26, '#e3b6ef'], [32, 44, '#f7b7cf']]" :key="i"
         :transform="`translate(${d[0]} ${d[1]})`">
        <path d="M0 -10 L9 0 L0 10 L-9 0 Z" :fill="d[2]" stroke="#fff" stroke-width="1.2" />
        <path d="M0 -10 L0 10 M-9 0 L9 0" stroke="#fff" stroke-width="0.9" opacity="0.7" />
        <circle cx="0" cy="0" r="2.3" fill="#fff" />
        <path d="M-6 9 Q-11 14 -8 17 Q-4 16 0 10 Q4 16 8 17 Q11 14 6 9 Z" :fill="d[2]" opacity="0.85" />
      </g>
    </g>

    <!-- ===== 兜底：四瓣小花 ===== -->
    <g v-else>
      <g fill="#f7c94c">
        <ellipse cx="32" cy="22" rx="6" ry="10" />
        <ellipse cx="32" cy="42" rx="6" ry="10" />
        <ellipse cx="22" cy="32" rx="10" ry="6" />
        <ellipse cx="42" cy="32" rx="10" ry="6" />
      </g>
      <circle cx="32" cy="32" r="5" fill="#fff4c9" />
    </g>
  </svg>
</template>

<script>
export default {
  name: 'CutieMark',
  props: {
    /** 内部心印图案 key，兼容已有数据；界面只显示图案本身 */
    mark: { type: String, required: true }
  }
}
</script>
