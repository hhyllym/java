<template>
  <!-- 背景装饰：柔光斑、彩虹、云朵、气球、流星、星光与小马剪影 -->
  <div class="sky">
    <!-- 柔光斑：大色块缓慢漂移，增加层次感 -->
    <div class="glow g1"></div>
    <div class="glow g2"></div>
    <div class="glow g3"></div>

    <!-- 彩虹弧：缓缓呼吸淡入淡出 -->
    <svg class="rainbow" viewBox="0 0 600 320" aria-hidden="true">
      <g fill="none" stroke-width="13" stroke-linecap="round">
        <path d="M60 300 A240 240 0 0 1 540 300" stroke="#f7a8c4" />
        <path d="M73 300 A227 227 0 0 1 527 300" stroke="#f3c969" />
        <path d="M86 300 A214 214 0 0 1 514 300" stroke="#a9d6a8" />
        <path d="M99 300 A201 201 0 0 1 501 300" stroke="#8fc7f0" />
        <path d="M112 300 A188 188 0 0 1 488 300" stroke="#b79cf0" />
      </g>
    </svg>

    <div class="cloud c1"></div>
    <div class="cloud c2"></div>
    <div class="cloud c3"></div>

    <!-- 气球：从底部缓缓升空，左右摇摆 -->
    <div
      v-for="(b, i) in balloons"
      :key="'b' + i"
      class="balloon"
      :style="{ left: b.x, animationDuration: b.dur, animationDelay: b.delay }"
    >
      <div class="balloon-bob">
        <span class="balloon-body" :style="{ background: b.c }"></span>
        <span class="balloon-string"></span>
      </div>
    </div>

    <!-- 流星：偶尔斜划而过 -->
    <span class="shooting-star"></span>

    <!-- 可爱标志：缓缓漂浮，悬停停顿，点击去看该小马的课程 -->
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
      v-for="(s, i) in sparkles"
      :key="'s' + i"
      class="sparkle"
      :style="{ top: s.top, left: s.left, fontSize: s.size, animationDelay: s.delay }"
    >{{ s.char }}</span>
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
          <rect x="32" y="66" width="9" height="26" rx="4" />
          <rect x="44" y="66" width="9" height="26" rx="4" />
          <rect x="74" y="66" width="9" height="26" rx="4" />
          <rect x="86" y="66" width="9" height="26" rx="4" />
          <ellipse cx="55" cy="56" rx="32" ry="18" />
          <path d="M80 52 q8 -22 24 -26 l6 9 q-14 6 -16 26 z" />
          <ellipse cx="102" cy="30" rx="14" ry="12" />
          <ellipse cx="115" cy="36" rx="7" ry="5" />
          <path d="M99 20 l3 -12 l9 8 z" />
          <path d="M90 22 q-12 2 -10 18 q8 -8 14 -5 q-4 -10 6 -13 z" />
          <path d="M24 50 q-14 4 -12 26 q9 -9 15 -7 q-5 -11 1 -19 z" />
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
      sparkles: [
        { top: '12%', left: '9%',  size: '18px', delay: '.2s', char: '✦' },
        { top: '24%', left: '89%', size: '14px', delay: '1.1s', char: '✧' },
        { top: '40%', left: '6%',  size: '20px', delay: '.6s', char: '★' },
        { top: '55%', left: '94%', size: '14px', delay: '1.6s', char: '✦' },
        { top: '70%', left: '4%',  size: '16px', delay: '.9s', char: '✧' },
        { top: '82%', left: '91%', size: '18px', delay: '1.3s', char: '★' },
        { top: '6%',  left: '3%',  size: '13px', delay: '2.1s', char: '✦' },
        { top: '90%', left: '96%', size: '15px', delay: '.4s', char: '✧' }
      ],
      // 气球：delay 用负值，让首屏就有气球在半空中
      balloons: [
        { c: '#f7a8c4', x: '6%',  dur: '26s', delay: '-2s'  },
        { c: '#b79cf0', x: '13%', dur: '34s', delay: '-15s' },
        { c: '#8fc7f0', x: '92%', dur: '29s', delay: '-25s' },
        { c: '#f3c969', x: '80%', dur: '37s', delay: '-8s'  },
        { c: '#a9d6a8', x: '88%', dur: '31s', delay: '-21s' }
      ],
      ponies: [
        { c: '#b79cf0', x: '2%', y: '10%', s: 98, d: 0   },
        { c: '#8fc7f0', x: '85%', y: '14%', s: 86, d: 1.4 },
        { c: '#f7a8c4', x: '2%', y: '56%', s: 92, d: 2.2 },
        { c: '#f3c969', x: '86%', y: '52%', s: 90, d: 0.8 },
        { c: '#f0a06a', x: '2%', y: '85%', s: 82, d: 1.9 },
        { c: '#a9d6e5', x: '86%', y: '84%', s: 94, d: 3.0 }
      ],
      // 漂浮的可爱标志：全部贴屏幕左右外缘，中间留给卡片内容
      marks: [
        { pony: 'pinkie',     title: '碧琪 · 气球标志',    x: '3%',  y: '14%', s: 38, dur: '10s', delay: '-1s'  },
        { pony: 'twilight',   title: '紫悦 · 六芒星标志',  x: '93%', y: '10%', s: 34, dur: '12s', delay: '-4s'  },
        { pony: 'dash',       title: '云宝 · 彩虹闪电标志', x: '2%',  y: '40%', s: 36, dur: '11s', delay: '-7s'  },
        { pony: 'applejack',  title: '苹果嘉儿 · 苹果标志', x: '94%', y: '62%', s: 34, dur: '13s', delay: '-2s'  },
        { pony: 'fluttershy', title: '柔柔 · 蝴蝶标志',    x: '6%',  y: '78%', s: 36, dur: '10s', delay: '-5s'  },
        { pony: 'rarity',     title: '珍奇 · 钻石标志',    x: '91%', y: '84%', s: 32, dur: '12s', delay: '-8s'  },
        { pony: 'pinkie',     title: '碧琪 · 气球标志',    x: '3%',  y: '3%',  s: 28, dur: '9s',  delay: '-3s'  },
        { pony: 'twilight',   title: '紫悦 · 六芒星标志',  x: '92%', y: '90%', s: 30, dur: '11s', delay: '-6s'  }
      ]
    }
  },
  methods: {
    /** 点击可爱标志：去看该小马的课程 */
    goPony(pony) {
      sound.twinkle()
      this.$router.push({ path: '/learn', query: { pony } })
    },
    /** 悬停可爱标志：极轻一声 */
    markHover() {
      sound.hover()
    }
  }
}
</script>
