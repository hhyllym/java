<template>
  <!-- 背景装饰：云朵、星光与小马剪影 -->
  <div class="sky">
    <div class="cloud c1"></div>
    <div class="cloud c2"></div>
    <div class="cloud c3"></div>
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
export default {
  name: 'PonyBackground',
  data() {
    return {
      sparkles: [
        { top: '12%', left: '18%', size: '18px', delay: '.2s', char: '✦' },
        { top: '24%', left: '78%', size: '14px', delay: '1.1s', char: '✧' },
        { top: '40%', left: '30%', size: '20px', delay: '.6s', char: '★' },
        { top: '55%', left: '85%', size: '14px', delay: '1.6s', char: '✦' },
        { top: '70%', left: '14%', size: '16px', delay: '.9s', char: '✧' },
        { top: '82%', left: '60%', size: '18px', delay: '1.3s', char: '★' },
        { top: '33%', left: '62%', size: '13px', delay: '2.1s', char: '✦' },
        { top: '90%', left: '40%', size: '15px', delay: '.4s', char: '✧' }
      ],
      ponies: [
        { c: '#b79cf0', x: '3%',  y: '10%', s: 98, d: 0   },
        { c: '#8fc7f0', x: '78%', y: '14%', s: 86, d: 1.4 },
        { c: '#f7a8c4', x: '4%',  y: '56%', s: 92, d: 2.2 },
        { c: '#f3c969', x: '80%', y: '52%', s: 90, d: 0.8 },
        { c: '#f0a06a', x: '6%',  y: '85%', s: 82, d: 1.9 },
        { c: '#a9d6e5', x: '76%', y: '84%', s: 94, d: 3.0 }
      ]
    }
  }
}
</script>
