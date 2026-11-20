<template>
  <!-- 背景装饰：柔光斑、彩虹、粉红泡泡/彩带（仅默认粉色主题）、流星、星光与小马剪影 -->
  <div class="sky">
    <!-- 柔光斑：大色块缓慢漂移，增加层次感 -->
    <div class="glow g1"></div>
    <div class="glow g2"></div>
    <div class="glow g3"></div>

    <!-- 彩虹弧已移除：SVG 弧线在背景里会呈现矩形边框感，与整体氛围不符 -->

    <!-- 白云：其它主题保留原样，粉色主题在 CSS 里隐藏 -->
    <div class="cloud c1"></div>
    <div class="cloud c2"></div>
    <div class="cloud c3"></div>

    <!-- 粉红泡泡：从底部缓缓升起，内层轻轻左右摇晃 -->
    <div
      v-for="(b, i) in balloons"
      :key="'b' + i"
      class="bg-bubble"
      :style="{ left: b.x, animationDuration: b.dur, animationDelay: b.delay }"
    >
      <div class="bg-bubble-bob">
        <span class="bg-bubble-core" :style="{ '--bc': b.c, '--bs': b.s + 'px' }"></span>
      </div>
    </div>

    <!-- 彩带：细细的弧形纸带，轻轻飘落 -->
    <i
      v-for="(r, i) in ribbons"
      :key="'r' + i"
      class="bg-ribbon"
      :style="{ left: r.left, animationDuration: r.dur, animationDelay: r.delay }"
    >
      <span
        class="bg-ribbon-piece"
        :style="{ '--rc': r.c, '--rr': r.rot + 'deg', '--rs': r.s }"
      ></span>
    </i>

    <!-- 原气球：其它主题保持原样，粉色主题在 CSS 里隐藏 -->
    <div
      v-for="(b, i) in legacyBalloons"
      :key="'lb' + i"
      class="balloon"
      :style="{ left: b.x, animationDuration: b.dur, animationDelay: b.delay }"
    >
      <div class="balloon-bob">
        <span class="balloon-body" :style="{ background: b.c }"></span>
        <span class="balloon-string"></span>
      </div>
    </div>

    <!-- 金黄落叶：只在橙色主题出现，取代原来的气球 -->
    <i
      v-for="(l, i) in leaves"
      :key="'leaf' + i"
      class="bg-leaf"
      :style="{ left: l.x, animationDuration: l.dur, animationDelay: l.delay }"
    >
      <span
        class="bg-leaf-shape"
        :class="l.type"
        :style="{ '--ls': l.s, '--lr': l.rot + 'deg' }"
      >{{ l.type === 'ginkgo' ? '🍂' : '🍁' }}</span>
    </i>

    <!-- 纸鸢：只在蓝色主题出现，从左下角斜着往上飞，尾巴带线 -->
    <div
      v-for="(k, i) in kites"
      :key="'kite' + i"
      class="bg-kite"
      :style="{
        left: k.x,
        animationDuration: k.dur,
        animationDelay: k.delay,
        '--kr': k.rot + 'deg'
      }"
    >
      <span class="bg-kite-body" :style="{ '--kc': k.c }"></span>
      <span class="bg-kite-string"></span>
    </div>

    <!-- 夜色流星：其它主题沿用原夜空装饰，粉色主题不显示 -->
    <span
      v-for="(ns, i) in nightStars"
      :key="'ns' + i"
      class="night-star"
      :style="{ top: ns.top, left: ns.left, animationDuration: ns.dur, animationDelay: ns.delay }"
    ></span>

    <!-- 星球：仅深色主题出现，取代白天的云朵与彩虹，深空感更贴切 -->
    <span
      v-for="(pl, i) in planets"
      :key="'pl' + i"
      class="night-planet"
      :class="{ 'has-ring': pl.ring }"
      :style="{
        top: pl.top, left: pl.left,
        width: pl.s + 'px', height: pl.s + 'px',
        '--pc': pl.c, '--pgl': pl.glow,
        animationDuration: pl.dur, animationDelay: pl.delay
      }"
    ></span>

    <!-- 流星：偶尔斜划而过，多条错时出现 -->
    <span
      v-for="(st, i) in stars"
      :key="'st' + i"
      class="shooting-star"
      :style="{ top: st.top, left: st.left, animationDuration: st.dur, animationDelay: st.delay }"
    ></span>

    <!-- 心印徽记：缓缓漂浮，悬停停顿，点击去看该小马的课程 -->
    <div
      v-for="(m, i) in marks"
      :key="'m' + i"
      class="mark-float"
      :style="{
        left: m.x, top: m.y, width: m.s + 'px',
        animationDuration: m.dur, animationDelay: m.delay
      }"
      :title="m.title"
      @click="goPony(m.pony)"
      @mouseenter="markHover"
    >
      <CutieMark :mark="m.pony" />
    </div>

    <span
      v-for="(f, i) in floaties"
      :key="'f' + i"
      class="float-item float-cloud"
      :style="{ top: f.top, left: f.left, fontSize: f.size, animationDelay: f.delay, animationDuration: f.dur }"
    >{{ f.char }}</span>
    <span
      v-for="(s, i) in sparkles"
      :key="'s' + i"
      class="sparkle"
      :style="{ top: s.top, left: s.left, fontSize: s.size, animationDelay: s.delay }"
    >{{ s.char }}</span>

    <!-- 甜品漂浮物：糖果 / 蛋糕 / 冰淇淋等，呼应「星糖谷」主题 -->
    <span
      v-for="(d, i) in desserts"
      :key="'d' + i"
      class="float-item float-dessert"
      :style="{ top: d.top, left: d.left, fontSize: d.size, animationDelay: d.delay, animationDuration: d.dur }"
    >{{ d.char }}</span>
    <div
      v-for="(p, i) in ponies"
      :key="'p' + i"
      class="pony"
      :style="{
        left: p.x,
        top: p.y,
        width: p.s + 'px',
        animationDelay: p.d + 's',
        animationDuration: 8 + p.d + 's'
      }"
    >
      <svg viewBox="0 0 120 100" xmlns="http://www.w3.org/2000/svg">
        <g :fill="p.c">
          <!-- Q 版小马：短圆腿 -->
          <rect x="38" y="68" width="11" height="24" rx="5.5" />
          <rect x="52" y="70" width="11" height="24" rx="5.5" />
          <rect x="72" y="70" width="11" height="24" rx="5.5" />
          <rect x="84" y="66" width="11" height="24" rx="5.5" />
          <!-- 圆润胖身体 -->
          <ellipse cx="58" cy="58" rx="31" ry="18" />
          <!-- 脖子 -->
          <path d="M68 40 q-2 12 -8 18 l18 4 q4 -10 8 -16 z" />
          <!-- 大圆头 -->
          <circle cx="86" cy="30" r="17" />
          <!-- 口鼻 -->
          <ellipse cx="100" cy="36" rx="8" ry="6" />
          <!-- 尖耳朵 -->
          <path d="M76 14 l3 -12 l10 8 z" />
          <!-- 飘逸鬃毛 -->
          <path d="M74 16 q-14 4 -16 20 q8 -6 13 -4 q-2 -12 12 -12 z" />
          <path d="M92 12 q12 0 16 10 q-9 -2 -12 4 q-2 -10 -10 -8 z" />
          <!-- 云朵状尾巴 -->
          <path d="M30 50 q-18 0 -20 20 q7 -3 9 3 q5 -9 12 -6 q-2 -12 8 -12 z" />
        </g>
      </svg>
    </div>
  </div>
</template>

<script>
import CutieMark from './CutieMark.vue'
import sound from '../sound'

export default {
  name: 'PonyBackground',
  components: { CutieMark },
  data() {
    return {
      // 发光星星：满屏散落的 ✦✧★，大小错落、节奏不同，营造星光闪烁感
      sparkles: [
        { top: '12%', left: '9%',  size: '18px', delay: '.2s',  char: '✦' },
        { top: '24%', left: '89%', size: '14px', delay: '1.1s', char: '✧' },
        { top: '40%', left: '6%',  size: '20px', delay: '.6s',  char: '★' },
        { top: '55%', left: '94%', size: '14px', delay: '1.6s', char: '✦' },
        { top: '70%', left: '4%',  size: '16px', delay: '.9s',  char: '✧' },
        { top: '82%', left: '91%', size: '18px', delay: '1.3s', char: '★' },
        { top: '6%',  left: '3%',  size: '13px', delay: '2.1s', char: '✦' },
        { top: '90%', left: '96%', size: '15px', delay: '.4s',  char: '✧' },
        { top: '18%', left: '24%', size: '12px', delay: '1.8s', char: '✧' },
        { top: '33%', left: '76%', size: '16px', delay: '2.4s', char: '✦' },
        { top: '47%', left: '17%', size: '13px', delay: '.35s', char: '★' },
        { top: '62%', left: '86%', size: '17px', delay: '1.45s', char: '✧' },
        { top: '76%', left: '29%', size: '12px', delay: '2.8s', char: '✦' },
        { top: '88%', left: '71%', size: '15px', delay: '.75s',  char: '★' },
        { top: '8%',  left: '58%', size: '14px', delay: '2.05s', char: '✦' },
        { top: '29%', left: '95%', size: '12px', delay: '1.2s', char: '★' },
        { top: '52%', left: '43%', size: '13px', delay: '2.6s', char: '✧' },
        { top: '66%', left: '12%', size: '15px', delay: '.5s',  char: '✦' },
        { top: '79%', left: '62%', size: '13px', delay: '1.9s', char: '★' },
        { top: '93%', left: '37%', size: '14px', delay: '2.3s', char: '✧' },
        { top: '4%',  left: '79%', size: '12px', delay: '1.55s', char: '✦' },
        { top: '22%', left: '64%', size: '13px', delay: '2.9s', char: '★' },
        { top: '44%', left: '51%', size: '12px', delay: '1.05s', char: '✧' },
        { top: '58%', left: '35%', size: '14px', delay: '2.15s', char: '✦' },
        { top: '72%', left: '48%', size: '12px', delay: '.28s',  char: '★' },
        { top: '86%', left: '26%', size: '13px', delay: '1.75s', char: '✧' },
        { top: '14%', left: '41%', size: '12px', delay: '2.45s', char: '✦' }
      ],
      // 粉红泡泡：柔和半透明，delay 用负值让首屏就有泡泡飘着
      balloons: [
        ['#ffd3e4', '6%',  '26s', '-2s'],
        ['#ffe0ec', '13%', '34s', '-15s'],
        ['#ffe8f2', '92%', '29s', '-25s'],
        ['#ffc9de', '80%', '37s', '-8s'],
        ['#ffd9e8', '88%', '31s', '-21s'],
        ['#ffedf4', '20%', '33s', '-30s'],
        ['#ffdcea', '27%', '28s', '-11s'],
        ['#ffcfe2', '72%', '35s', '-19s'],
        ['#ffd3e4', '45%', '30s', '-27s'],
        ['#ffe0ec', '54%', '32s', '-6s'],
        ['#ffe8f2', '3%',  '29s', '-24s'],
        ['#ffc9de', '9%',  '36s', '-33s'],
        ['#ffd9e8', '96%', '27s', '-13s'],
        ['#ffedf4', '84%', '34s', '-4s'],
        ['#ffdcea', '78%', '30s', '-36s'],
        ['#ffcfe2', '34%', '38s', '-22s'],
        ['#ffd3e4', '38%', '26s', '-16s'],
        ['#ffe0ec', '64%', '31s', '-29s'],
        ['#ffe8f2', '58%', '35s', '-9s'],
        ['#ffc9de', '48%', '28s', '-18s'],
        ['#ffd9e8', '24%', '31s', '-26s'],
        ['#ffedf4', '36%', '29s', '-12s'],
        ['#ffdcea', '86%', '33s', '-28s'],
        ['#ffcfe2', '42%', '36s', '-20s'],
        ['#ffd3e4', '68%', '27s', '-10s'],
        ['#ffe0ec', '16%', '34s', '-31s'],
        ['#ffe8f2', '52%', '30s', '-23s'],
        ['#ffc9de', '76%', '32s', '-14s'],
        ['#ffd9e8', '10%', '28s', '-17s'],
        ['#ffedf4', '60%', '35s', '-32s']
      ].map(([c, x, dur, delay], i) => ({ c, x, dur, delay, s: 30 + (i % 6) * 7 })),
      // 其它主题沿用原来的气球，完全不动
      legacyBalloons: [
        ['#f7a8c4', '6%',  '26s', '-2s'],  ['#b79cf0', '13%', '34s', '-15s'],
        ['#8fc7f0', '92%', '29s', '-25s'], ['#f3c969', '80%', '37s', '-8s'],
        ['#a9d6a8', '88%', '31s', '-21s'], ['#ffb3c8', '20%', '33s', '-30s'],
        ['#a5d8f0', '27%', '28s', '-11s'], ['#c9a8f0', '72%', '35s', '-19s'],
        ['#ffd9a0', '45%', '30s', '-27s'], ['#b8e6c9', '54%', '32s', '-6s'],
        ['#f7a8c4', '3%',  '29s', '-24s'], ['#b79cf0', '9%',  '36s', '-33s'],
        ['#8fc7f0', '96%', '27s', '-13s'], ['#f3c969', '84%', '34s', '-4s'],
        ['#a9d6a8', '78%', '30s', '-36s'], ['#ffb3c8', '34%', '38s', '-22s'],
        ['#a5d8f0', '38%', '26s', '-16s'], ['#c9a8f0', '64%', '31s', '-29s'],
        ['#ffd9a0', '58%', '35s', '-9s'],  ['#b8e6c9', '48%', '28s', '-18s'],
        ['#f7a8c4', '24%', '31s', '-26s'], ['#b79cf0', '36%', '29s', '-12s'],
        ['#8fc7f0', '86%', '33s', '-28s'], ['#f3c969', '42%', '36s', '-20s'],
        ['#a9d6a8', '68%', '27s', '-10s'], ['#ffb3c8', '16%', '34s', '-31s'],
        ['#a5d8f0', '52%', '30s', '-23s'], ['#c9a8f0', '76%', '32s', '-14s'],
        ['#ffd9a0', '10%', '28s', '-17s'], ['#b8e6c9', '60%', '35s', '-32s']
      ].map(([c, x, dur, delay]) => ({ c, x, dur, delay })),
      // 粉色彩带：只在默认粉色主题出现，数量克制
      ribbons: [
        { left: '7%',  c: '#f7a8c4', rot: -22, s: '34px', dur: '24s', delay: '-5s'  },
        { left: '16%', c: '#c9a8f0', rot:  18, s: '28px', dur: '29s', delay: '-17s' },
        { left: '25%', c: '#ffd9a0', rot: -12, s: '36px', dur: '26s', delay: '-9s'  },
        { left: '34%', c: '#ffb3c8', rot:  26, s: '30px', dur: '31s', delay: '-22s' },
        { left: '44%', c: '#a8d8f7', rot: -28, s: '32px', dur: '27s', delay: '-3s'  },
        { left: '55%', c: '#f7a8c4', rot:  14, s: '26px', dur: '33s', delay: '-14s' },
        { left: '64%', c: '#c9a8f0', rot: -18, s: '38px', dur: '25s', delay: '-20s' },
        { left: '73%', c: '#b8e6c9', rot:  22, s: '30px', dur: '30s', delay: '-7s'  },
        { left: '82%', c: '#ffd9a0', rot: -25, s: '34px', dur: '28s', delay: '-12s' },
        { left: '91%', c: '#ffb3c8', rot:  16, s: '28px', dur: '34s', delay: '-25s' },
        { left: '11%', c: '#a8d8f7', rot:  20, s: '32px', dur: '30s', delay: '-27s' },
        { left: '20%', c: '#ffd9a0', rot: -16, s: '28px', dur: '26s', delay: '-8s'  },
        { left: '29%', c: '#f7a8c4', rot:  24, s: '36px', dur: '32s', delay: '-19s' },
        { left: '38%', c: '#b8e6c9', rot: -22, s: '30px', dur: '28s', delay: '-2s'  },
        { left: '48%', c: '#c9a8f0', rot:  12, s: '34px', dur: '35s', delay: '-23s' },
        { left: '59%', c: '#ffb3c8', rot: -26, s: '26px', dur: '27s', delay: '-11s' },
        { left: '69%', c: '#ffd9a0', rot:  18, s: '32px', dur: '31s', delay: '-16s' },
        { left: '79%', c: '#a8d8f7', rot: -14, s: '28px', dur: '29s', delay: '-6s'  }
      ],
      // 金黄落叶：只在橙色主题出现，银杏叶与枫叶交替
      leaves: [
        { x: '5%',  type: 'ginkgo', s: '22px', rot: -26, dur: '20s', delay: '-4s'  },
        { x: '13%', type: 'maple',  s: '26px', rot:  18, dur: '24s', delay: '-15s' },
        { x: '22%', type: 'ginkgo', s: '20px', rot: -12, dur: '22s', delay: '-9s'  },
        { x: '31%', type: 'maple',  s: '24px', rot:  30, dur: '26s', delay: '-20s' },
        { x: '40%', type: 'ginkgo', s: '23px', rot: -20, dur: '19s', delay: '-2s'  },
        { x: '49%', type: 'maple',  s: '27px', rot:  14, dur: '27s', delay: '-12s' },
        { x: '58%', type: 'ginkgo', s: '21px', rot: -32, dur: '23s', delay: '-17s' },
        { x: '67%', type: 'maple',  s: '25px', rot:  22, dur: '21s', delay: '-6s'  },
        { x: '76%', type: 'ginkgo', s: '22px', rot: -16, dur: '25s', delay: '-22s' },
        { x: '85%', type: 'maple',  s: '28px', rot:  28, dur: '20s', delay: '-11s' },
        { x: '94%', type: 'ginkgo', s: '20px', rot: -24, dur: '24s', delay: '-18s' },
        { x: '9%',  type: 'maple',  s: '24px', rot:  16, dur: '28s', delay: '-25s' },
        { x: '27%', type: 'ginkgo', s: '21px', rot: -18, dur: '26s', delay: '-7s'  },
        { x: '45%', type: 'maple',  s: '26px', rot:  24, dur: '22s', delay: '-14s' },
        { x: '63%', type: 'ginkgo', s: '22px', rot: -28, dur: '29s', delay: '-3s'  },
        { x: '81%', type: 'maple',  s: '25px', rot:  20, dur: '23s', delay: '-19s' },
        { x: '18%', type: 'ginkgo', s: '20px', rot: -14, dur: '27s', delay: '-23s' },
        { x: '72%', type: 'maple',  s: '27px', rot:  26, dur: '25s', delay: '-8s'  }
      ],
      // 纸鸢：只在蓝色主题出现，从左下飞向右上，带飘线
      kites: [
        { c: '#ffd166', x: '2%',  rot: -14, dur: '28s', delay: '-4s'  },
        { c: '#ff9eb5', x: '8%',  rot:  10, dur: '32s', delay: '-18s' },
        { c: '#b79cf0', x: '14%', rot: -8,  dur: '30s', delay: '-25s' },
        { c: '#8fc7f0', x: '20%', rot:  16, dur: '34s', delay: '-10s' },
        { c: '#a7e3c9', x: '5%',  rot: -18, dur: '31s', delay: '-29s' },
        { c: '#ffd166', x: '17%', rot:  12, dur: '27s', delay: '-14s' },
        { c: '#ff9eb5', x: '11%', rot: -10, dur: '35s', delay: '-22s' },
        { c: '#b79cf0', x: '24%', rot:  14, dur: '29s', delay: '-7s'  },
        { c: '#8fc7f0', x: '1%',  rot: -16, dur: '33s', delay: '-31s' },
        { c: '#a7e3c9', x: '10%', rot:  12, dur: '26s', delay: '-12s' },
        { c: '#ffd166', x: '19%', rot: -6,  dur: '36s', delay: '-2s'  },
        { c: '#ff9eb5', x: '27%', rot:  18, dur: '30s', delay: '-27s' },
        { c: '#b79cf0', x: '4%',  rot: -12, dur: '28s', delay: '-16s' },
        { c: '#8fc7f0', x: '15%', rot:  8,  dur: '34s', delay: '-20s' },
        { c: '#a7e3c9', x: '22%', rot: -20, dur: '31s', delay: '-5s'  }
      ],
      // 甜品漂浮物：呼应「星糖谷」主题，数量克制，左右位置互为镜像
      desserts: [
        { char: '🍬', top: '16%', left: '9%',  size: '18px', delay: '-1s',  dur: '12s' },
        { char: '🍬', top: '16%', left: '88%', size: '18px', delay: '-8s',  dur: '14s' },
        { char: '🧁', top: '40%', left: '6%',  size: '16px', delay: '-9s',  dur: '12s' },
        { char: '🧁', top: '40%', left: '91%', size: '16px', delay: '-4s',  dur: '13s' },
        { char: '🍭', top: '64%', left: '10%', size: '17px', delay: '-3s',  dur: '14s' },
        { char: '🍭', top: '64%', left: '87%', size: '17px', delay: '-12s', dur: '13s' },
        { char: '🍩', top: '88%', left: '5%',  size: '14px', delay: '-12s', dur: '12s' },
        { char: '🍩', top: '88%', left: '92%', size: '14px', delay: '-6s',  dur: '14s' }
      ],
      // 流星：全部统一朝「右下」方向飞（斜杠「\」方向，彗核在右上端）
      // 左右两侧位置互为镜像（left 与 100-left 配对），画面更均衡
      stars: [
        { top: '8%',  left: '93%', dur: '11s', delay: '4s'  },
        { top: '8%',  left: '7%',  dur: '13s', delay: '-9s' },
        { top: '26%', left: '88%', dur: '15s', delay: '-3s' },
        { top: '26%', left: '12%', dur: '17s', delay: '-6s' },
        { top: '47%', left: '95%', dur: '14s', delay: '-12s' },
        { top: '47%', left: '5%',  dur: '19s', delay: '-8s' },
        { top: '68%', left: '90%', dur: '16s', delay: '-15s' },
        { top: '68%', left: '10%', dur: '12s', delay: '-2s' },
        { top: '86%', left: '96%', dur: '18s', delay: '-10s' },
        { top: '86%', left: '4%',  dur: '13s', delay: '-5s' }
      ],
      // 夜色流星：深色主题专用，同样统一方向 + 左右镜像
      nightStars: [
        { top: '6%',  left: '92%', dur: '9s',  delay: '-2s' },
        { top: '6%',  left: '8%',  dur: '11s', delay: '-8s' },
        { top: '20%', left: '87%', dur: '12s', delay: '-7s' },
        { top: '20%', left: '13%', dur: '14s', delay: '-13s' },
        { top: '33%', left: '95%', dur: '10s', delay: '-4s' },
        { top: '33%', left: '5%',  dur: '13s', delay: '-11s' },
        { top: '47%', left: '90%', dur: '11s', delay: '-5s' },
        { top: '47%', left: '10%', dur: '15s', delay: '-3s' },
        { top: '60%', left: '94%', dur: '12s', delay: '-6s' },
        { top: '60%', left: '6%',  dur: '10s', delay: '-14s' },
        { top: '74%', left: '89%', dur: '14s', delay: '-9s' },
        { top: '74%', left: '11%', dur: '12s', delay: '-1s' },
        { top: '87%', left: '93%', dur: '11s', delay: '-8s' },
        { top: '87%', left: '7%',  dur: '13s', delay: '-4s' }
      ],
      // 星球：深色主题专用，取代白天的云朵与彩虹；左右位置互为镜像
      planets: [
        { c: '#8f7ae8', glow: 'rgba(143,122,232,.55)', top: '12%', left: '6%',  s: 46, ring: true,  dur: '16s', delay: '-2s' },
        { c: '#8f7ae8', glow: 'rgba(143,122,232,.55)', top: '12%', left: '87%', s: 46, ring: true,  dur: '16s', delay: '-9s' },
        { c: '#e08ac0', glow: 'rgba(224,138,192,.45)', top: '30%', left: '10%', s: 30, ring: false, dur: '13s', delay: '-7s' },
        { c: '#e08ac0', glow: 'rgba(224,138,192,.45)', top: '30%', left: '83%', s: 30, ring: false, dur: '13s', delay: '-14s' },
        { c: '#6ec6e6', glow: 'rgba(110,198,230,.45)', top: '48%', left: '4%',  s: 24, ring: false, dur: '18s', delay: '-4s' },
        { c: '#6ec6e6', glow: 'rgba(110,198,230,.45)', top: '48%', left: '91%', s: 24, ring: false, dur: '18s', delay: '-11s' },
        { c: '#f3c969', glow: 'rgba(243,201,105,.45)', top: '66%', left: '9%',  s: 36, ring: true,  dur: '15s', delay: '-11s' },
        { c: '#f3c969', glow: 'rgba(243,201,105,.45)', top: '66%', left: '84%', s: 36, ring: true,  dur: '15s', delay: '-3s' },
        { c: '#a9d6a8', glow: 'rgba(169,214,168,.4)',  top: '84%', left: '5%',  s: 22, ring: false, dur: '20s', delay: '-6s' },
        { c: '#a9d6a8', glow: 'rgba(169,214,168,.4)',  top: '84%', left: '90%', s: 22, ring: false, dur: '20s', delay: '-13s' },
        { c: '#c39bf0', glow: 'rgba(195,155,240,.5)',  top: '20%', left: '13%', s: 18, ring: false, dur: '14s', delay: '-1s' },
        { c: '#c39bf0', glow: 'rgba(195,155,240,.5)',  top: '20%', left: '80%', s: 18, ring: false, dur: '14s', delay: '-8s' }
      ],
      // 白云与彩虹粒子：其它主题保留原样，粉色主题在 CSS 里隐藏白云
      floaties: [
        { char: '☁️', top: '8%',  left: '18%', size: '30px', delay: '0s',    dur: '11s' },
        { char: '☁️', top: '22%', left: '68%', size: '24px', delay: '-3s',   dur: '12s' },
        { char: '☁️', top: '38%', left: '8%',  size: '20px', delay: '-6s',   dur: '10s' },
        { char: '☁️', top: '52%', left: '84%', size: '26px', delay: '-2s',   dur: '13s' },
        { char: '☁️', top: '68%', left: '30%', size: '22px', delay: '-8s',   dur: '11s' },
        { char: '☁️', top: '84%', left: '62%', size: '28px', delay: '-5s',   dur: '12s' },
        { char: '🌈', top: '14%', left: '42%', size: '26px', delay: '-1s',   dur: '12s' },
        { char: '🌈', top: '44%', left: '92%', size: '22px', delay: '-7s',   dur: '13s' },
        { char: '🌈', top: '74%', left: '16%', size: '24px', delay: '-4s',   dur: '11s' },
        { char: '🌈', top: '90%', left: '88%', size: '20px', delay: '-9s',   dur: '12s' }
      ],
      ponies: [
        { c: '#b79cf0', x: '2%',  y: '6%',  s: 96, d: 0   },
        { c: '#f7a8c4', x: '1%',  y: '38%', s: 88, d: 2.2 },
        { c: '#f0a06a', x: '3%',  y: '74%', s: 82, d: 1.9 },
        { c: '#8fc7f0', x: '85%', y: '20%', s: 86, d: 1.4 },
        { c: '#f3c969', x: '86%', y: '55%', s: 90, d: 0.8 },
        { c: '#a9d6e5', x: '84%', y: '88%', s: 94, d: 3.0 }
      ],
      // 漂浮的心印徽记：全部贴屏幕左右外缘，中间留给卡片内容
      marks: [
        { pony: 'pinkie',     title: '泡芙 · 糖霜杯糕心印',    x: '3%',  y: '14%', s: 38, dur: '10s', delay: '-1s'  },
        { pony: 'twilight',   title: '星澜 · 星芒书页心印',  x: '93%', y: '10%', s: 34, dur: '12s', delay: '-4s'  },
        { pony: 'dash',       title: '云驰 · 彗星流云心印', x: '2%',  y: '40%', s: 36, dur: '11s', delay: '-7s'  },
        { pony: 'applejack',  title: '金穗 · 金麦穗心印', x: '94%', y: '62%', s: 34, dur: '13s', delay: '-2s'  },
        { pony: 'fluttershy', title: '茸茸 · 嫩叶绒球心印',    x: '6%',  y: '78%', s: 36, dur: '10s', delay: '-5s'  },
        { pony: 'rarity',     title: '绮罗 · 缎带菱晶心印',    x: '91%', y: '84%', s: 32, dur: '12s', delay: '-8s'  },
        { pony: 'pinkie',     title: '泡芙 · 糖霜杯糕心印',    x: '3%',  y: '3%',  s: 28, dur: '9s',  delay: '-3s'  },
        { pony: 'twilight',   title: '星澜 · 星芒书页心印',  x: '92%', y: '90%', s: 30, dur: '11s', delay: '-6s'  }
      ]
    }
  },
  methods: {
    /** 点击心印徽记：去看该小马的课程 */
    goPony(pony) {
      sound.twinkle()
      this.$router.push({ path: '/learn', query: { pony } })
    },
    /** 悬停心印徽记：极轻一声 */
    markHover() {
      sound.hover()
    }
  }
}
</script>
