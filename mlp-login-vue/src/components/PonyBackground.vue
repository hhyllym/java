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
      class="float-item"
      :style="{ top: f.top, left: f.left, fontSize: f.size, animationDelay: f.delay, animationDuration: f.dur }"
    >{{ f.char }}</span>
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
        { c: '#a9d6a8', x: '88%', dur: '31s', delay: '-21s' },
        { c: '#ffb3c8', x: '20%', dur: '33s', delay: '-30s' },
        { c: '#a5d8f0', x: '27%', dur: '28s', delay: '-11s' },
        { c: '#c9a8f0', x: '72%', dur: '35s', delay: '-19s' },
        { c: '#ffd9a0', x: '45%', dur: '30s', delay: '-27s' },
        { c: '#b8e6c9', x: '54%', dur: '32s', delay: '-6s'  },
        { c: '#f7a8c4', x: '3%',  dur: '29s', delay: '-24s' },
        { c: '#b79cf0', x: '9%',  dur: '36s', delay: '-33s' },
        { c: '#8fc7f0', x: '96%', dur: '27s', delay: '-13s' },
        { c: '#f3c969', x: '84%', dur: '34s', delay: '-4s'  },
        { c: '#a9d6a8', x: '78%', dur: '30s', delay: '-36s' },
        { c: '#ffb3c8', x: '34%', dur: '38s', delay: '-22s' },
        { c: '#a5d8f0', x: '38%', dur: '26s', delay: '-16s' },
        { c: '#c9a8f0', x: '64%', dur: '31s', delay: '-29s' },
        { c: '#ffd9a0', x: '58%', dur: '35s', delay: '-9s'  },
        { c: '#b8e6c9', x: '48%', dur: '28s', delay: '-18s' }
      ],
      // 白云与彩虹粒子：缓慢漂浮
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
