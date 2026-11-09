<template>
  <span
    class="pet-sprite"
    :class="['ps-' + safeKind, 'ps-state-' + state, { 'ps-still': still, 'ps-mini': size <= 44 }]"
    :style="{ width: size + 'px', height: size + 'px' }"
    :title="title || kindName"
  >
    <!-- 图片模式 4.0：分层立绘 + 实时眨眼 + 弹性呼吸（加载失败回退下面的 SVG） -->
    <span v-if="usePhoto" class="ps-live" :style="liveStyle">
      <span class="ps-ground"></span>
      <img class="ps-layer ps-base" :src="photoSrc" :alt="kindName"
           draggable="false" @error="photoFailed = true" />
      <img class="ps-layer ps-headlayer" :src="photoSrc" alt="" draggable="false" />
      <span class="ps-blink">
        <i class="ps-lid ps-lid-l"></i><i class="ps-lid ps-lid-r"></i>
        <i class="ps-curve ps-curve-l"></i><i class="ps-curve ps-curve-r"></i>
      </span>
      <span v-if="!still" class="ps-photo-gloss"></span>
    </span>
    <svg v-else class="ps-svg" viewBox="0 0 120 120" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <defs>
        <!-- 主体渐变：亮面 → 暗面，做出球体体积 -->
        <linearGradient :id="uid + 'head'" x1="0.18" y1="0.02" x2="0.82" y2="1">
          <stop offset="0" :stop-color="palette.head1" />
          <stop offset="0.58" :stop-color="palette.head2" />
          <stop offset="1" :stop-color="palette.body2" />
        </linearGradient>
        <linearGradient :id="uid + 'body'" x1="0.2" y1="0" x2="0.8" y2="1">
          <stop offset="0" :stop-color="palette.body1" />
          <stop offset="1" :stop-color="palette.body2" />
        </linearGradient>
        <linearGradient :id="uid + 'earInner'" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" :stop-color="palette.inner" />
          <stop offset="1" :stop-color="palette.head2" />
        </linearGradient>
        <linearGradient :id="uid + 'iris'" x1="0" y1="0" x2="0.25" y2="1">
          <stop offset="0" :stop-color="palette.eyeLight" />
          <stop offset="0.55" :stop-color="palette.eye" />
          <stop offset="1" :stop-color="palette.eye" />
        </linearGradient>
        <!-- 边缘暗角：叠在轮廓上，圆滚滚的立体感 -->
        <radialGradient :id="uid + 'shade'" cx="0.46" cy="0.38" r="0.68">
          <stop offset="0.52" stop-color="#6b4a6b" stop-opacity="0" />
          <stop offset="1" stop-color="#6b4a6b" stop-opacity=".18" />
        </radialGradient>
        <radialGradient :id="uid + 'gloss'" cx="0.32" cy="0.2" r="0.62">
          <stop offset="0" stop-color="#ffffff" stop-opacity=".62" />
          <stop offset="1" stop-color="#ffffff" stop-opacity="0" />
        </radialGradient>
        <radialGradient :id="uid + 'blush'">
          <stop offset="0" stop-color="#ff7fb0" stop-opacity=".55" />
          <stop offset="1" stop-color="#ff7fb0" stop-opacity="0" />
        </radialGradient>
        <radialGradient :id="uid + 'metal'" cx="0.32" cy="0.24" r="0.75">
          <stop offset="0" stop-color="#fff6c8" />
          <stop offset="0.55" stop-color="#ffd86b" />
          <stop offset="1" stop-color="#e0a63f" />
        </radialGradient>
      </defs>

      <!-- 双层地面影：外圈柔光 + 内圈接触影 -->
      <ellipse class="ps-shadow-soft" cx="60" cy="110" rx="36" ry="7.5" fill="#7c5c9e" opacity=".07" />
      <ellipse class="ps-shadow" cx="60" cy="110" rx="27" ry="4.6" fill="#7c5c9e" opacity=".16" />

      <g class="ps-float">
        <!-- ==================== 小猫 ==================== -->
        <g v-if="safeKind === 'cat'" class="ps-animal">
          <!-- 卷卷尾巴：外描边 + 主体 + 亮芯 + 环纹 -->
          <g class="ps-tail">
            <path d="M83 90 C101 93 111 79 104 67 C99 58 89 61 89 69"
                  fill="none" :stroke="palette.line" stroke-width="14" stroke-linecap="round" opacity=".5" />
            <path d="M83 90 C101 93 111 79 104 67 C99 58 89 61 89 69"
                  fill="none" :stroke="palette.body2" stroke-width="11" stroke-linecap="round" />
            <path d="M84 90 C99 92 107 81 103 71"
                  fill="none" :stroke="palette.body1" stroke-width="4.5" stroke-linecap="round" opacity=".9" />
            <path d="M98 61 l-3 5 M106 70 l-5 2" :stroke="palette.accent" stroke-width="2.4" stroke-linecap="round" opacity=".65" />
          </g>

          <!-- 身体：体积渐变 + 暗角 + 胸口绒毛 + 带趾线的小爪 -->
          <g class="ps-body">
            <path d="M33 91 C33 78 44 72 60 72 C76 72 87 78 87 91 C87 104 76 110 60 110 C44 110 33 104 33 91 Z"
                  :fill="'url(#' + uid + 'body)'" :stroke="palette.line" stroke-width="1.6" />
            <path d="M33 91 C33 78 44 72 60 72 C76 72 87 78 87 91 C87 104 76 110 60 110 C44 110 33 104 33 91 Z"
                  :fill="'url(#' + uid + 'shade)'" />
            <path d="M48 80 q12 8 24 0 q-3 14 -12 16 q-9 -2 -12 -16 z" fill="#fff8ea" opacity=".88" />
            <path d="M52 84 q8 4 16 0" stroke="#efd9b8" stroke-width="1.2" fill="none" opacity=".75" />
            <g class="ps-paw ps-paw-l">
              <ellipse cx="45" cy="106" rx="9.5" ry="6.4" :fill="palette.head1" :stroke="palette.line" stroke-width="1.3" />
              <path d="M42 107 v2 M46 107.6 v2 M50 107 v1.8" :stroke="palette.line" stroke-width="1" stroke-linecap="round" opacity=".65" />
            </g>
            <g class="ps-paw ps-paw-r">
              <ellipse cx="75" cy="106" rx="9.5" ry="6.4" :fill="palette.head1" :stroke="palette.line" stroke-width="1.3" />
              <path d="M70 107 v1.8 M74 107.6 v2 M78 107 v2" :stroke="palette.line" stroke-width="1" stroke-linecap="round" opacity=".65" />
            </g>
          </g>

          <g class="ps-head">
            <!-- 三角耳：外耳 + 内耳渐变 + 耳尖高光 -->
            <g class="ps-ear ps-ear-l">
              <path d="M31 42 L27 15 Q27 13 29 14 L53 29 Z" :fill="palette.head2" :stroke="palette.line" stroke-width="2" stroke-linejoin="round" />
              <path d="M35 37 L33 22 L46 30 Z" :fill="'url(#' + uid + 'earInner)'" />
              <path d="M31 20 L33 33" stroke="#ffffff" stroke-width="2" stroke-linecap="round" opacity=".5" />
            </g>
            <g class="ps-ear ps-ear-r">
              <path d="M89 42 L93 15 Q93 13 91 14 L67 29 Z" :fill="palette.head2" :stroke="palette.line" stroke-width="2" stroke-linejoin="round" />
              <path d="M85 37 L87 22 L74 30 Z" :fill="'url(#' + uid + 'earInner)'" />
              <path d="M89 20 L87 33" stroke="#ffffff" stroke-width="2" stroke-linecap="round" opacity=".5" />
            </g>

            <!-- 脸颊小毛簇 -->
            <path d="M27 59 l-6 3 l5 2 l-4 5 l8 -2 z" :fill="palette.head2" :stroke="palette.line" stroke-width="1" stroke-linejoin="round" />
            <path d="M93 59 l6 3 l-5 2 l4 5 l-8 -2 z" :fill="palette.head2" :stroke="palette.line" stroke-width="1" stroke-linejoin="round" />

            <!-- 包子脸 -->
            <path d="M60 26 C80 26 95 39 95 57 C95 69 91 78 83 84 C76 89 69 90 60 90 C51 90 44 89 37 84 C29 78 25 69 25 57 C25 39 40 26 60 26 Z"
                  :fill="'url(#' + uid + 'head)'" :stroke="palette.line" stroke-width="1.8" />
            <path d="M60 26 C80 26 95 39 95 57 C95 69 91 78 83 84 C76 89 69 90 60 90 C51 90 44 89 37 84 C29 78 25 69 25 57 C25 39 40 26 60 26 Z"
                  :fill="'url(#' + uid + 'shade)'" />
            <ellipse class="ps-shine" cx="45" cy="39" rx="12" ry="6" fill="#ffffff" opacity=".42" transform="rotate(-25 45 39)" />
            <ellipse cx="60" cy="56" rx="32" ry="29" :fill="'url(#' + uid + 'gloss)'" />
            <path d="M53 31 q3 5 0 9 M60 29 q3 5 0 9 M67 31 q3 5 0 9"
                  :stroke="palette.accent" stroke-width="2.6" fill="none" stroke-linecap="round" opacity=".62" />

            <!-- 眼睛：眼白 / 虹膜渐变 / 瞳孔 / 双高光 / 上眼睑 / 睫毛 -->
            <g class="ps-eyes">
              <ellipse cx="47" cy="57" rx="8.6" ry="10.2" fill="#ffffff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="47.4" cy="57.8" rx="6.2" ry="8.2" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="47.4" cy="58.4" rx="3.2" ry="5.5" fill="#2f2145" />
              <circle cx="44.5" cy="54.2" r="2.5" fill="#ffffff" />
              <circle cx="50.4" cy="61.6" r="1.3" fill="#ffffff" opacity=".9" />
              <path d="M39 51 q8 -5 16 0" fill="none" :stroke="palette.line" stroke-width="1.8" stroke-linecap="round" opacity=".7" />
              <path d="M40 50 l-2 -2 M54 50 l2 -2" :stroke="palette.line" stroke-width="1.5" stroke-linecap="round" opacity=".6" />
              <ellipse cx="73" cy="57" rx="8.6" ry="10.2" fill="#ffffff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="72.6" cy="57.8" rx="6.2" ry="8.2" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="72.6" cy="58.4" rx="3.2" ry="5.5" fill="#2f2145" />
              <circle cx="69.5" cy="54.2" r="2.5" fill="#ffffff" />
              <circle cx="75.4" cy="61.6" r="1.3" fill="#ffffff" opacity=".9" />
              <path d="M65 51 q8 -5 16 0" fill="none" :stroke="palette.line" stroke-width="1.8" stroke-linecap="round" opacity=".7" />
              <path d="M66 50 l-2 -2 M80 50 l2 -2" :stroke="palette.line" stroke-width="1.5" stroke-linecap="round" opacity=".6" />
            </g>
            <g class="ps-eyes-smile">
              <path d="M39 57 q8 -9 16 0 M65 57 q8 -9 16 0" fill="none" stroke="#6b4a7a" stroke-width="2.7" stroke-linecap="round" />
            </g>

            <ellipse cx="34" cy="70" rx="8" ry="5" :fill="'url(#' + uid + 'blush)'" />
            <ellipse cx="86" cy="70" rx="8" ry="5" :fill="'url(#' + uid + 'blush)'" />
            <path d="M56.8 68.2 L63.2 68.2 Q60 72.6 60 72.6 Z" fill="#e98fa9" stroke="#d57f9c" stroke-width=".8" stroke-linejoin="round" />
            <ellipse cx="58.8" cy="68.8" rx="1.2" ry=".7" fill="#ffffff" opacity=".75" />
            <path class="ps-mouth" d="M60 72.6 q-4.5 6 -9 1 M60 72.6 q4.5 6 9 1"
                  fill="none" stroke="#95647c" stroke-width="1.9" stroke-linecap="round" />
            <path d="M29 65 l-10 -3 M28 70 l-11 1 M91 65 l10 -3 M92 70 l11 1"
                  :stroke="palette.line" stroke-width="1.4" stroke-linecap="round" opacity=".8" />
            <circle cx="31" cy="67" r=".9" fill="#c9a279" /><circle cx="31" cy="70" r=".9" fill="#c9a279" />
            <circle cx="89" cy="67" r=".9" fill="#c9a279" /><circle cx="89" cy="70" r=".9" fill="#c9a279" />
          </g>
        </g>

        <!-- ==================== 小狗 ==================== -->
        <g v-else-if="safeKind === 'dog'" class="ps-animal">
          <!-- 尾巴：双层 + 白尖 -->
          <g class="ps-tail ps-tail-dog">
            <path d="M84 89 C101 90 106 75 98 67" fill="none" :stroke="palette.line" stroke-width="13" stroke-linecap="round" opacity=".5" />
            <path d="M84 89 C101 90 106 75 98 67" fill="none" :stroke="palette.body2" stroke-width="10" stroke-linecap="round" />
            <path d="M98 67 q5 -4 8 0" fill="none" :stroke="palette.head1" stroke-width="5.5" stroke-linecap="round" />
            <path d="M88 88 C97 88 101 82 100 76" fill="none" :stroke="palette.body1" stroke-width="3.5" stroke-linecap="round" opacity=".8" />
          </g>

          <g class="ps-body">
            <path d="M33 92 C33 79 44 73 60 73 C76 73 87 79 87 92 C87 105 76 110 60 110 C44 110 33 105 33 92 Z"
                  :fill="'url(#' + uid + 'body)'" :stroke="palette.line" stroke-width="1.6" />
            <path d="M33 92 C33 79 44 73 60 73 C76 73 87 79 87 92 C87 105 76 110 60 110 C44 110 33 105 33 92 Z"
                  :fill="'url(#' + uid + 'shade)'" />
            <path d="M49 81 q11 7 22 0 q-3 13 -11 15 q-8 -2 -11 -15 z" fill="#fff6de" opacity=".9" />
            <g class="ps-paw ps-paw-l">
              <ellipse cx="45" cy="106" rx="9.5" ry="6.4" :fill="palette.head1" :stroke="palette.line" stroke-width="1.3" />
              <path d="M42 107 v2 M46 107.6 v2 M50 107 v1.8" :stroke="palette.line" stroke-width="1" stroke-linecap="round" opacity=".65" />
            </g>
            <g class="ps-paw ps-paw-r">
              <ellipse cx="75" cy="106" rx="9.5" ry="6.4" :fill="palette.head1" :stroke="palette.line" stroke-width="1.3" />
              <path d="M70 107 v1.8 M74 107.6 v2 M78 107 v2" :stroke="palette.line" stroke-width="1" stroke-linecap="round" opacity=".65" />
            </g>
          </g>

          <g class="ps-head">
            <!-- 包子脸 -->
            <path d="M60 25 C80 25 94 38 94 56 C94 68 90 77 82 83 C75 88 68 89 60 89 C52 89 45 88 38 83 C30 77 26 68 26 56 C26 38 40 25 60 25 Z"
                  :fill="'url(#' + uid + 'head)'" :stroke="palette.line" stroke-width="1.8" />
            <path d="M60 25 C80 25 94 38 94 56 C94 68 90 77 82 83 C75 88 68 89 60 89 C52 89 45 88 38 83 C30 77 26 68 26 56 C26 38 40 25 60 25 Z"
                  :fill="'url(#' + uid + 'shade)'" />
            <ellipse class="ps-shine" cx="45" cy="38" rx="12" ry="6" fill="#ffffff" opacity=".44" transform="rotate(-25 45 38)" />
            <ellipse cx="60" cy="55" rx="31" ry="28" :fill="'url(#' + uid + 'gloss)'" />
            <!-- 口鼻浅色区 -->
            <ellipse cx="60" cy="70" rx="15" ry="11" fill="#fff8e8" opacity=".82" />
            <!-- 两撮呆毛 -->
            <g class="ps-ahoge">
              <path d="M57 27 q-6 -9 1 -14 q0 7 5 10 M64 27 q5 -8 10 -8 q-5 5 -5 10" :fill="palette.accent" />
            </g>
            <!-- 憨憨小眉毛 -->
            <path d="M41 46 q5 -3 10 -1 M69 45 q5 -2 10 1" :stroke="palette.line" stroke-width="2" fill="none" stroke-linecap="round" opacity=".7" />

            <g class="ps-eyes">
              <ellipse cx="47" cy="56" rx="8.4" ry="10" fill="#ffffff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="47.4" cy="56.8" rx="6.1" ry="8" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="47.4" cy="57.4" rx="3.1" ry="5.3" fill="#2f2018" />
              <circle cx="44.6" cy="53.3" r="2.5" fill="#ffffff" />
              <circle cx="50.3" cy="60.5" r="1.3" fill="#ffffff" opacity=".9" />
              <path d="M39 50 q8 -5 16 0" fill="none" :stroke="palette.line" stroke-width="1.8" stroke-linecap="round" opacity=".7" />
              <ellipse cx="73" cy="56" rx="8.4" ry="10" fill="#ffffff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="72.6" cy="56.8" rx="6.1" ry="8" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="72.6" cy="57.4" rx="3.1" ry="5.3" fill="#2f2018" />
              <circle cx="69.6" cy="53.3" r="2.5" fill="#ffffff" />
              <circle cx="75.3" cy="60.5" r="1.3" fill="#ffffff" opacity=".9" />
              <path d="M65 50 q8 -5 16 0" fill="none" :stroke="palette.line" stroke-width="1.8" stroke-linecap="round" opacity=".7" />
            </g>
            <g class="ps-eyes-smile">
              <path d="M39 56 q8 -9 16 0 M65 56 q8 -9 16 0" fill="none" stroke="#5a3d29" stroke-width="2.7" stroke-linecap="round" />
            </g>

            <ellipse cx="34" cy="69" rx="8" ry="5" :fill="'url(#' + uid + 'blush)'" />
            <ellipse cx="86" cy="69" rx="8" ry="5" :fill="'url(#' + uid + 'blush)'" />
            <!-- 鼻子：渐变 + 高光 + 鼻孔 -->
            <ellipse cx="60" cy="67" rx="5.6" ry="4" fill="#6b4a3a" :stroke="palette.line" stroke-width=".8" />
            <ellipse cx="58.4" cy="65.8" rx="1.8" ry="1" fill="#ffffff" opacity=".65" />
            <circle cx="57.8" cy="68.2" r=".7" fill="#3d2a20" opacity=".7" />
            <circle cx="62.2" cy="68.2" r=".7" fill="#3d2a20" opacity=".7" />
            <path d="M60 71 v3.5 M60 74.5 q-5 3.5 -8 .3 M60 74.5 q5 3.5 8 .3"
                  fill="none" stroke="#6b4a3a" stroke-width="1.7" stroke-linecap="round" />
            <path class="ps-tongue" d="M55.5 76.5 q4.5 10.5 9 0 z" fill="#ff8fae" stroke="#e8779a" stroke-width=".7" />
            <path d="M60 78 v6" stroke="#e8779a" stroke-width=".9" opacity=".7" />

            <!-- 垂耳：外耳渐变 + 内侧暗面 + 边缘绒毛 + 高光 -->
            <g class="ps-ear ps-ear-l">
              <ellipse cx="27" cy="55" rx="11.5" ry="22" transform="rotate(-13 27 55)" :fill="palette.accent" :stroke="palette.line" stroke-width="1.5" />
              <ellipse cx="27.5" cy="55" rx="6" ry="17" transform="rotate(-13 27.5 55)" :fill="'url(#' + uid + 'earInner)'" opacity=".8" />
              <path d="M20 42 q-3 9 0 18 M22 38 q-2 6 -1 10" :stroke="palette.head1" stroke-width="1.6" fill="none" stroke-linecap="round" opacity=".55" />
            </g>
            <g class="ps-ear ps-ear-r">
              <ellipse cx="93" cy="55" rx="11.5" ry="22" transform="rotate(13 93 55)" :fill="palette.accent" :stroke="palette.line" stroke-width="1.5" />
              <ellipse cx="92.5" cy="55" rx="6" ry="17" transform="rotate(13 92.5 55)" :fill="'url(#' + uid + 'earInner)'" opacity=".8" />
              <path d="M100 42 q3 9 0 18 M98 38 q2 6 1 10" :stroke="palette.head1" stroke-width="1.6" fill="none" stroke-linecap="round" opacity=".55" />
            </g>

            <!-- 项圈：渐变带 + 缝线 + 金属扣 + 铃铛 -->
            <path d="M38 81 q22 12 44 0" fill="none" stroke="#e77fae" stroke-width="7.5" stroke-linecap="round" opacity=".55" />
            <path d="M38 81 q22 12 44 0" fill="none" stroke="#ff9ec4" stroke-width="5.5" stroke-linecap="round" />
            <path d="M41 82 q19 9 38 0" fill="none" stroke="#ffffff" stroke-width="1" stroke-dasharray="2 3" opacity=".6" />
            <rect x="56.5" y="85" width="7" height="4" rx="1.5" fill="#e9c15e" stroke="#c99b3e" stroke-width=".7" />
            <circle cx="60" cy="91" r="5.6" :fill="'url(#' + uid + 'metal)'" stroke="#d9a33f" stroke-width="1.1" />
            <path d="M57.3 92.5 h5.4" stroke="#b9842e" stroke-width="1.2" stroke-linecap="round" />
            <circle cx="58" cy="89" r="1.5" fill="#ffffff" opacity=".8" />
          </g>
        </g>

        <!-- ==================== 垂耳兔 ==================== -->
        <g v-else-if="safeKind === 'rabbit'" class="ps-animal">
          <!-- 圆球尾 -->
          <g class="ps-tail ps-tail-ball">
            <circle cx="88" cy="94" r="9" :fill="palette.head1" :stroke="palette.line" stroke-width="1.4" />
            <circle cx="85.5" cy="91.5" r="3.2" fill="#ffffff" opacity=".75" />
          </g>

          <g class="ps-body">
            <path d="M33 91 C33 78 44 72 60 72 C76 72 87 78 87 91 C87 104 76 110 60 110 C44 110 33 104 33 91 Z"
                  :fill="'url(#' + uid + 'body)'" :stroke="palette.line" stroke-width="1.6" />
            <path d="M33 91 C33 78 44 72 60 72 C76 72 87 78 87 91 C87 104 76 110 60 110 C44 110 33 104 33 91 Z"
                  :fill="'url(#' + uid + 'shade)'" />
            <path d="M48 80 q12 8 24 0 q-3 14 -12 16 q-9 -2 -12 -16 z" fill="#ffffff" opacity=".85" />
            <g class="ps-paw ps-paw-l">
              <ellipse cx="45" cy="106" rx="9.5" ry="6.4" :fill="palette.head1" :stroke="palette.line" stroke-width="1.3" />
              <path d="M42 107 v2 M46 107.6 v2 M50 107 v1.8" :stroke="palette.line" stroke-width="1" stroke-linecap="round" opacity=".6" />
            </g>
            <g class="ps-paw ps-paw-r">
              <ellipse cx="75" cy="106" rx="9.5" ry="6.4" :fill="palette.head1" :stroke="palette.line" stroke-width="1.3" />
              <path d="M70 107 v1.8 M74 107.6 v2 M78 107 v2" :stroke="palette.line" stroke-width="1" stroke-linecap="round" opacity=".6" />
            </g>
          </g>

          <g class="ps-head">
            <!-- 长长竖耳（会一前一后摆） -->
            <g class="ps-ear ps-ear-l ps-ear-long">
              <path d="M46 35 C39 21 38 6 43 4 C49 2 53 17 53 35 Z" :fill="palette.head2" :stroke="palette.line" stroke-width="1.8" />
              <path d="M46.5 31 C42 19 42 9 44 9 C48 9 49 20 49.5 31 Z" :fill="'url(#' + uid + 'earInner)'" />
              <path d="M44 13 L44 27" stroke="#ffffff" stroke-width="1.6" stroke-linecap="round" opacity=".55" />
            </g>
            <g class="ps-ear ps-ear-r ps-ear-long">
              <path d="M74 35 C81 21 82 6 77 4 C71 2 67 17 67 35 Z" :fill="palette.head2" :stroke="palette.line" stroke-width="1.8" />
              <path d="M73.5 31 C78 19 78 9 76 9 C72 9 71 20 70.5 31 Z" :fill="'url(#' + uid + 'earInner)'" />
              <path d="M76 13 L76 27" stroke="#ffffff" stroke-width="1.6" stroke-linecap="round" opacity=".55" />
            </g>

            <path d="M60 26 C80 26 95 39 95 57 C95 69 91 78 83 84 C76 89 69 90 60 90 C51 90 44 89 37 84 C29 78 25 69 25 57 C25 39 40 26 60 26 Z"
                  :fill="'url(#' + uid + 'head)'" :stroke="palette.line" stroke-width="1.8" />
            <path d="M60 26 C80 26 95 39 95 57 C95 69 91 78 83 84 C76 89 69 90 60 90 C51 90 44 89 37 84 C29 78 25 69 25 57 C25 39 40 26 60 26 Z"
                  :fill="'url(#' + uid + 'shade)'" />
            <ellipse class="ps-shine" cx="45" cy="39" rx="12" ry="6" fill="#ffffff" opacity=".45" transform="rotate(-25 45 39)" />
            <ellipse cx="60" cy="56" rx="32" ry="29" :fill="'url(#' + uid + 'gloss)'" />

            <g class="ps-eyes">
              <ellipse cx="47" cy="57" rx="8.6" ry="10.2" fill="#ffffff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="47.4" cy="57.8" rx="6.2" ry="8.2" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="47.4" cy="58.4" rx="3.2" ry="5.5" fill="#4a2a44" />
              <circle cx="44.5" cy="54.2" r="2.5" fill="#ffffff" />
              <circle cx="50.4" cy="61.6" r="1.3" fill="#ffffff" opacity=".9" />
              <path d="M39 51 q8 -5 16 0" fill="none" :stroke="palette.line" stroke-width="1.8" stroke-linecap="round" opacity=".7" />
              <ellipse cx="73" cy="57" rx="8.6" ry="10.2" fill="#ffffff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="72.6" cy="57.8" rx="6.2" ry="8.2" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="72.6" cy="58.4" rx="3.2" ry="5.5" fill="#4a2a44" />
              <circle cx="69.5" cy="54.2" r="2.5" fill="#ffffff" />
              <circle cx="75.4" cy="61.6" r="1.3" fill="#ffffff" opacity=".9" />
              <path d="M65 51 q8 -5 16 0" fill="none" :stroke="palette.line" stroke-width="1.8" stroke-linecap="round" opacity=".7" />
            </g>
            <g class="ps-eyes-smile">
              <path d="M39 57 q8 -9 16 0 M65 57 q8 -9 16 0" fill="none" stroke="#8a5570" stroke-width="2.7" stroke-linecap="round" />
            </g>

            <ellipse cx="33" cy="70" rx="8" ry="5" :fill="'url(#' + uid + 'blush)'" />
            <ellipse cx="87" cy="70" rx="8" ry="5" :fill="'url(#' + uid + 'blush)'" />
            <path d="M57.4 68 L62.6 68 L60 71.2 Z" fill="#f29ab8" stroke="#dd85a4" stroke-width=".8" stroke-linejoin="round" />
            <path class="ps-mouth" d="M60 71.2 v2.4 M60 73.6 q-3.6 3 -6 .3 M60 73.6 q3.6 3 6 .3"
                  fill="none" stroke="#a56c88" stroke-width="1.7" stroke-linecap="round" />
            <path d="M57.8 75.6 h4.4 v4 q-2.2 1.6 -4.4 0 z" fill="#ffffff" stroke="#e3c9d5" stroke-width=".8" />
            <path d="M60 75.6 v4" stroke="#e3c9d5" stroke-width=".7" />
          </g>
        </g>

        <!-- ==================== 赤狐 ==================== -->
        <g v-else-if="safeKind === 'fox'" class="ps-animal">
          <!-- 大蓬松扫帚尾（带白尖） -->
          <g class="ps-tail ps-tail-big">
            <path d="M80 94 C99 102 114 90 109 73 C106 64 99 61 96 67 C93 73 100 78 98 83 C95 88 87 85 83 80 Z"
                  :fill="palette.body2" :stroke="palette.line" stroke-width="1.6" stroke-linejoin="round" />
            <path d="M104 66 q7 -1 8 6 q-1 6 -8 6 q4 -6 0 -12 z" :fill="palette.head1" :stroke="palette.line" stroke-width="1" />
            <path d="M86 90 C97 95 105 87 103 79" fill="none" :stroke="palette.body1" stroke-width="4" stroke-linecap="round" opacity=".8" />
          </g>

          <g class="ps-body">
            <path d="M33 91 C33 78 44 72 60 72 C76 72 87 78 87 91 C87 104 76 110 60 110 C44 110 33 104 33 91 Z"
                  :fill="'url(#' + uid + 'body)'" :stroke="palette.line" stroke-width="1.6" />
            <path d="M33 91 C33 78 44 72 60 72 C76 72 87 78 87 91 C87 104 76 110 60 110 C44 110 33 104 33 91 Z"
                  :fill="'url(#' + uid + 'shade)'" />
            <path d="M48 80 q12 8 24 0 q-3 14 -12 16 q-9 -2 -12 -16 z" fill="#fff6ea" opacity=".9" />
            <g class="ps-paw ps-paw-l">
              <ellipse cx="45" cy="106" rx="9.5" ry="6.4" :fill="palette.head1" :stroke="palette.line" stroke-width="1.3" />
              <path d="M42 107 v2 M46 107.6 v2 M50 107 v1.8" :stroke="palette.line" stroke-width="1" stroke-linecap="round" opacity=".6" />
            </g>
            <g class="ps-paw ps-paw-r">
              <ellipse cx="75" cy="106" rx="9.5" ry="6.4" :fill="palette.head1" :stroke="palette.line" stroke-width="1.3" />
              <path d="M70 107 v1.8 M74 107.6 v2 M78 107 v2" :stroke="palette.line" stroke-width="1" stroke-linecap="round" opacity=".6" />
            </g>
          </g>

          <g class="ps-head">
            <g class="ps-ear ps-ear-l">
              <path d="M30 44 L23 12 Q23 10 25 11 L54 28 Z" :fill="palette.head2" :stroke="palette.line" stroke-width="2" stroke-linejoin="round" />
              <path d="M34 38 L29 20 L45 30 Z" :fill="'url(#' + uid + 'earInner)'" />
              <path d="M26 14 L30 31" stroke="#9a6234" stroke-width="2.4" stroke-linecap="round" opacity=".55" />
            </g>
            <g class="ps-ear ps-ear-r">
              <path d="M90 44 L97 12 Q97 10 95 11 L66 28 Z" :fill="palette.head2" :stroke="palette.line" stroke-width="2" stroke-linejoin="round" />
              <path d="M86 38 L91 20 L75 30 Z" :fill="'url(#' + uid + 'earInner)'" />
              <path d="M94 14 L90 31" stroke="#9a6234" stroke-width="2.4" stroke-linecap="round" opacity=".55" />
            </g>

            <path d="M60 26 C80 26 95 39 95 57 C95 69 91 78 83 84 C76 89 69 90 60 90 C51 90 44 89 37 84 C29 78 25 69 25 57 C25 39 40 26 60 26 Z"
                  :fill="'url(#' + uid + 'head)'" :stroke="palette.line" stroke-width="1.8" />
            <path d="M60 26 C80 26 95 39 95 57 C95 69 91 78 83 84 C76 89 69 90 60 90 C51 90 44 89 37 84 C29 78 25 69 25 57 C25 39 40 26 60 26 Z"
                  :fill="'url(#' + uid + 'shade)'" />
            <ellipse class="ps-shine" cx="45" cy="39" rx="12" ry="6" fill="#ffffff" opacity=".42" transform="rotate(-25 45 39)" />
            <ellipse cx="60" cy="56" rx="32" ry="29" :fill="'url(#' + uid + 'gloss)'" />
            <!-- 白色吻部 -->
            <path d="M38 65 q22 -7 44 0 q-4 18 -22 23 q-18 -5 -22 -23 z" fill="#fff6ea" opacity=".96" />

            <g class="ps-eyes">
              <ellipse cx="47" cy="56" rx="8.6" ry="10" fill="#ffffff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="47.4" cy="56.8" rx="6.2" ry="8" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="47.4" cy="57.4" rx="3.2" ry="5.3" fill="#3d2817" />
              <circle cx="44.5" cy="53.2" r="2.5" fill="#ffffff" />
              <circle cx="50.4" cy="60.6" r="1.3" fill="#ffffff" opacity=".9" />
              <path d="M38 49 q8 -5 16 0" fill="none" :stroke="palette.line" stroke-width="1.8" stroke-linecap="round" opacity=".7" />
              <path d="M38 49 l-3 -2" :stroke="palette.line" stroke-width="1.6" stroke-linecap="round" opacity=".7" />
              <ellipse cx="73" cy="56" rx="8.6" ry="10" fill="#ffffff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="72.6" cy="56.8" rx="6.2" ry="8" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="72.6" cy="57.4" rx="3.2" ry="5.3" fill="#3d2817" />
              <circle cx="69.5" cy="53.2" r="2.5" fill="#ffffff" />
              <circle cx="75.4" cy="60.6" r="1.3" fill="#ffffff" opacity=".9" />
              <path d="M66 49 q8 -5 16 0" fill="none" :stroke="palette.line" stroke-width="1.8" stroke-linecap="round" opacity=".7" />
              <path d="M82 49 l3 -2" :stroke="palette.line" stroke-width="1.6" stroke-linecap="round" opacity=".7" />
            </g>
            <g class="ps-eyes-smile">
              <path d="M39 56 q8 -9 16 0 M65 56 q8 -9 16 0" fill="none" stroke="#7a4c25" stroke-width="2.7" stroke-linecap="round" />
            </g>

            <ellipse cx="33" cy="68" rx="8" ry="5" :fill="'url(#' + uid + 'blush)'" />
            <ellipse cx="87" cy="68" rx="8" ry="5" :fill="'url(#' + uid + 'blush)'" />
            <ellipse cx="60" cy="70" rx="4.6" ry="3.3" fill="#5b3a26" />
            <ellipse cx="58.7" cy="69" rx="1.4" ry=".8" fill="#ffffff" opacity=".65" />
            <path class="ps-mouth" d="M60 73 v2 M60 75 q-4 3.5 -7 .3 M60 75 q4 3.5 7 .3"
                  fill="none" stroke="#8a5a3a" stroke-width="1.7" stroke-linecap="round" />
          </g>
        </g>

        <!-- ==================== 棕熊 ==================== -->
        <g v-else-if="safeKind === 'bear'" class="ps-animal">
          <g class="ps-tail ps-tail-ball">
            <circle cx="88" cy="95" r="7.5" :fill="palette.body2" :stroke="palette.line" stroke-width="1.3" />
            <circle cx="86" cy="92.5" r="2.4" fill="#ffffff" opacity=".5" />
          </g>

          <g class="ps-body">
            <path d="M33 91 C33 78 44 72 60 72 C76 72 87 78 87 91 C87 104 76 110 60 110 C44 110 33 104 33 91 Z"
                  :fill="'url(#' + uid + 'body)'" :stroke="palette.line" stroke-width="1.6" />
            <path d="M33 91 C33 78 44 72 60 72 C76 72 87 78 87 91 C87 104 76 110 60 110 C44 110 33 104 33 91 Z"
                  :fill="'url(#' + uid + 'shade)'" />
            <path d="M48 80 q12 8 24 0 q-3 14 -12 16 q-9 -2 -12 -16 z" fill="#fff3e0" opacity=".85" />
            <g class="ps-paw ps-paw-l">
              <ellipse cx="45" cy="106" rx="9.5" ry="6.4" :fill="palette.head1" :stroke="palette.line" stroke-width="1.3" />
              <path d="M42 107 v2 M46 107.6 v2 M50 107 v1.8" :stroke="palette.line" stroke-width="1" stroke-linecap="round" opacity=".6" />
            </g>
            <g class="ps-paw ps-paw-r">
              <ellipse cx="75" cy="106" rx="9.5" ry="6.4" :fill="palette.head1" :stroke="palette.line" stroke-width="1.3" />
              <path d="M70 107 v1.8 M74 107.6 v2 M78 107 v2" :stroke="palette.line" stroke-width="1" stroke-linecap="round" opacity=".6" />
            </g>
          </g>

          <g class="ps-head">
            <!-- 圆圆小耳 -->
            <g class="ps-ear ps-ear-l">
              <circle cx="33" cy="32" r="12" :fill="palette.head2" :stroke="palette.line" stroke-width="1.8" />
              <circle cx="33" cy="32" r="6.5" :fill="'url(#' + uid + 'earInner)'" />
              <circle cx="30" cy="29" r="2.4" fill="#ffffff" opacity=".5" />
            </g>
            <g class="ps-ear ps-ear-r">
              <circle cx="87" cy="32" r="12" :fill="palette.head2" :stroke="palette.line" stroke-width="1.8" />
              <circle cx="87" cy="32" r="6.5" :fill="'url(#' + uid + 'earInner)'" />
              <circle cx="84" cy="29" r="2.4" fill="#ffffff" opacity=".5" />
            </g>

            <path d="M60 26 C80 26 95 39 95 57 C95 69 91 78 83 84 C76 89 69 90 60 90 C51 90 44 89 37 84 C29 78 25 69 25 57 C25 39 40 26 60 26 Z"
                  :fill="'url(#' + uid + 'head)'" :stroke="palette.line" stroke-width="1.8" />
            <path d="M60 26 C80 26 95 39 95 57 C95 69 91 78 83 84 C76 89 69 90 60 90 C51 90 44 89 37 84 C29 78 25 69 25 57 C25 39 40 26 60 26 Z"
                  :fill="'url(#' + uid + 'shade)'" />
            <ellipse class="ps-shine" cx="45" cy="39" rx="12" ry="6" fill="#ffffff" opacity=".4" transform="rotate(-25 45 39)" />
            <ellipse cx="60" cy="56" rx="32" ry="29" :fill="'url(#' + uid + 'gloss)'" />
            <!-- 浅色圆吻 -->
            <ellipse cx="60" cy="70" rx="15" ry="11.5" fill="#fff3e0" opacity=".94" />

            <g class="ps-eyes">
              <ellipse cx="47" cy="56" rx="8.4" ry="10" fill="#ffffff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="47.4" cy="56.8" rx="6.1" ry="8" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="47.4" cy="57.4" rx="3.1" ry="5.3" fill="#2f1f14" />
              <circle cx="44.6" cy="53.3" r="2.5" fill="#ffffff" />
              <circle cx="50.3" cy="60.5" r="1.3" fill="#ffffff" opacity=".9" />
              <path d="M39 50 q8 -5 16 0" fill="none" :stroke="palette.line" stroke-width="1.8" stroke-linecap="round" opacity=".7" />
              <ellipse cx="73" cy="56" rx="8.4" ry="10" fill="#ffffff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="72.6" cy="56.8" rx="6.1" ry="8" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="72.6" cy="57.4" rx="3.1" ry="5.3" fill="#2f1f14" />
              <circle cx="69.6" cy="53.3" r="2.5" fill="#ffffff" />
              <circle cx="75.3" cy="60.5" r="1.3" fill="#ffffff" opacity=".9" />
              <path d="M65 50 q8 -5 16 0" fill="none" :stroke="palette.line" stroke-width="1.8" stroke-linecap="round" opacity=".7" />
            </g>
            <g class="ps-eyes-smile">
              <path d="M39 56 q8 -9 16 0 M65 56 q8 -9 16 0" fill="none" stroke="#4a3122" stroke-width="2.7" stroke-linecap="round" />
            </g>

            <ellipse cx="33" cy="69" rx="8" ry="5" :fill="'url(#' + uid + 'blush)'" />
            <ellipse cx="87" cy="69" rx="8" ry="5" :fill="'url(#' + uid + 'blush)'" />
            <ellipse cx="60" cy="66.5" rx="5.2" ry="3.8" fill="#5b3d29" />
            <ellipse cx="58.5" cy="65.5" rx="1.6" ry=".9" fill="#ffffff" opacity=".6" />
            <path class="ps-mouth" d="M60 70 v3 M60 73 q-4.5 3.5 -8 .3 M60 73 q4.5 3.5 8 .3"
                  fill="none" stroke="#6b4a34" stroke-width="1.7" stroke-linecap="round" />
          </g>
        </g>

        <!-- ==================== 金丝熊 ==================== -->
        <g v-else-if="safeKind === 'hamster'" class="ps-animal">
          <g class="ps-tail ps-tail-ball">
            <circle cx="87" cy="98" r="5" :fill="palette.body2" :stroke="palette.line" stroke-width="1.1" />
          </g>

          <g class="ps-body">
            <path d="M33 91 C33 78 44 72 60 72 C76 72 87 78 87 91 C87 104 76 110 60 110 C44 110 33 104 33 91 Z"
                  :fill="'url(#' + uid + 'body)'" :stroke="palette.line" stroke-width="1.6" />
            <path d="M33 91 C33 78 44 72 60 72 C76 72 87 78 87 91 C87 104 76 110 60 110 C44 110 33 104 33 91 Z"
                  :fill="'url(#' + uid + 'shade)'" />
            <path d="M48 80 q12 8 24 0 q-3 14 -12 16 q-9 -2 -12 -16 z" fill="#fff8e8" opacity=".9" />
            <!-- 捧着瓜子的小爪 -->
            <g class="ps-paw ps-hold">
              <ellipse cx="52" cy="95" rx="6" ry="4.6" :fill="palette.head1" :stroke="palette.line" stroke-width="1.1" />
              <ellipse cx="68" cy="95" rx="6" ry="4.6" :fill="palette.head1" :stroke="palette.line" stroke-width="1.1" />
              <ellipse cx="60" cy="92" rx="3.2" ry="5" fill="#b98a52" stroke="#8f6738" stroke-width=".7" transform="rotate(-22 60 92)" />
            </g>
          </g>

          <g class="ps-head">
            <g class="ps-ear ps-ear-l">
              <circle cx="34" cy="33" r="9.5" :fill="palette.head2" :stroke="palette.line" stroke-width="1.6" />
              <circle cx="34" cy="33" r="5.2" :fill="'url(#' + uid + 'earInner)'" />
            </g>
            <g class="ps-ear ps-ear-r">
              <circle cx="86" cy="33" r="9.5" :fill="palette.head2" :stroke="palette.line" stroke-width="1.6" />
              <circle cx="86" cy="33" r="5.2" :fill="'url(#' + uid + 'earInner)'" />
            </g>

            <path d="M60 26 C80 26 95 39 95 57 C95 69 91 78 83 84 C76 89 69 90 60 90 C51 90 44 89 37 84 C29 78 25 69 25 57 C25 39 40 26 60 26 Z"
                  :fill="'url(#' + uid + 'head)'" :stroke="palette.line" stroke-width="1.8" />
            <path d="M60 26 C80 26 95 39 95 57 C95 69 91 78 83 84 C76 89 69 90 60 90 C51 90 44 89 37 84 C29 78 25 69 25 57 C25 39 40 26 60 26 Z"
                  :fill="'url(#' + uid + 'shade)'" />
            <ellipse class="ps-shine" cx="45" cy="39" rx="12" ry="6" fill="#ffffff" opacity=".42" transform="rotate(-25 45 39)" />
            <ellipse cx="60" cy="56" rx="32" ry="29" :fill="'url(#' + uid + 'gloss)'" />
            <!-- 鼓鼓的腮帮（会一鼓一鼓） -->
            <g class="ps-cheek">
              <ellipse cx="33" cy="71" rx="12" ry="10" :fill="palette.head1" :stroke="palette.line" stroke-width="1.2" />
              <ellipse cx="87" cy="71" rx="12" ry="10" :fill="palette.head1" :stroke="palette.line" stroke-width="1.2" />
              <ellipse cx="30" cy="68" rx="4" ry="2.5" fill="#ffffff" opacity=".6" />
              <ellipse cx="84" cy="68" rx="4" ry="2.5" fill="#ffffff" opacity=".6" />
            </g>

            <g class="ps-eyes">
              <ellipse cx="47" cy="55" rx="8" ry="9.6" fill="#ffffff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="47.4" cy="55.8" rx="5.8" ry="7.7" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="47.4" cy="56.4" rx="3" ry="5.1" fill="#2a1c12" />
              <circle cx="44.7" cy="52.4" r="2.4" fill="#ffffff" />
              <circle cx="50.2" cy="59.4" r="1.2" fill="#ffffff" opacity=".9" />
              <ellipse cx="73" cy="55" rx="8" ry="9.6" fill="#ffffff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="72.6" cy="55.8" rx="5.8" ry="7.7" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="72.6" cy="56.4" rx="3" ry="5.1" fill="#2a1c12" />
              <circle cx="69.7" cy="52.4" r="2.4" fill="#ffffff" />
              <circle cx="75.2" cy="59.4" r="1.2" fill="#ffffff" opacity=".9" />
            </g>
            <g class="ps-eyes-smile">
              <path d="M40 55 q7 -8 14 0 M66 55 q7 -8 14 0" fill="none" stroke="#4a3427" stroke-width="2.6" stroke-linecap="round" />
            </g>

            <ellipse cx="36" cy="70" rx="6" ry="4" :fill="'url(#' + uid + 'blush)'" />
            <ellipse cx="84" cy="70" rx="6" ry="4" :fill="'url(#' + uid + 'blush)'" />
            <path d="M57.6 67.5 L62.4 67.5 L60 70.5 Z" fill="#e79b9b" stroke="#d08282" stroke-width=".7" stroke-linejoin="round" />
            <path class="ps-mouth" d="M60 70.5 v2 M60 72.5 q-3 2.6 -5 .3 M60 72.5 q3 2.6 5 .3"
                  fill="none" stroke="#8a6a4a" stroke-width="1.5" stroke-linecap="round" />
            <path d="M58 74.6 h4 v3.2 q-2 1.2 -4 0 z" fill="#ffffff" stroke="#dcc096" stroke-width=".7" />
          </g>
        </g>

        <!-- ==================== 小企鹅 ==================== -->
        <g v-else-if="safeKind === 'penguin'" class="ps-animal">
          <!-- 橙脚 -->
          <path d="M48 96 q-9 6 -13 9 q11 2 17 -5 z" fill="#f0a24a" stroke="#d98a35" stroke-width="1" stroke-linejoin="round" />
          <path d="M72 96 q9 6 13 9 q-11 2 -17 -5 z" fill="#f0a24a" stroke="#d98a35" stroke-width="1" stroke-linejoin="round" />

          <g class="ps-head">
            <!-- 深蓝灰身体 + 大白肚 -->
            <path d="M60 28 C79 28 90 45 90 68 C90 88 78 100 60 100 C42 100 30 88 30 68 C30 45 41 28 60 28 Z"
                  :fill="'url(#' + uid + 'head)'" :stroke="palette.line" stroke-width="1.8" />
            <path d="M60 28 C79 28 90 45 90 68 C90 88 78 100 60 100 C42 100 30 88 30 68 C30 45 41 28 60 28 Z"
                  :fill="'url(#' + uid + 'shade)'" />
            <path d="M60 43 C74 43 82 57 82 74 C82 88 73 97 60 97 C47 97 38 88 38 74 C38 57 46 43 60 43 Z"
                  fill="#ffffff" opacity=".95" />
            <ellipse class="ps-shine" cx="46" cy="42" rx="10" ry="5" fill="#ffffff" opacity=".35" transform="rotate(-28 46 42)" />
            <ellipse cx="60" cy="60" rx="27" ry="30" :fill="'url(#' + uid + 'gloss)'" />
            <!-- 头顶呆毛 -->
            <g class="ps-ahoge">
              <path d="M57 29 q-5 -10 2 -14 q2 7 6 10 M64 29 q5 -8 10 -7 q-5 5 -6 9" :fill="palette.accent" />
            </g>
            <!-- 小翅膀会拍 -->
            <g class="ps-wing ps-wing-l">
              <path d="M32 58 q-13 8 -9 25 q3 8 12 2 q5 -9 3 -24 z" :fill="palette.body2" :stroke="palette.line" stroke-width="1.3" />
              <path d="M28 66 q-5 8 -3 15" stroke="#ffffff" stroke-width="2" fill="none" opacity=".4" stroke-linecap="round" />
            </g>
            <g class="ps-wing ps-wing-r">
              <path d="M88 58 q13 8 9 25 q-3 8 -12 2 q-5 -9 -3 -24 z" :fill="palette.body2" :stroke="palette.line" stroke-width="1.3" />
              <path d="M92 66 q5 8 3 15" stroke="#ffffff" stroke-width="2" fill="none" opacity=".4" stroke-linecap="round" />
            </g>

            <g class="ps-eyes">
              <ellipse cx="48" cy="57" rx="7.6" ry="9.2" fill="#f2f8ff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="48.4" cy="57.8" rx="5.5" ry="7.3" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="48.4" cy="58.4" rx="2.9" ry="4.9" fill="#1d3450" />
              <circle cx="45.8" cy="54.5" r="2.3" fill="#ffffff" />
              <circle cx="51.2" cy="61" r="1.2" fill="#ffffff" opacity=".9" />
              <ellipse cx="72" cy="57" rx="7.6" ry="9.2" fill="#f2f8ff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="71.6" cy="57.8" rx="5.5" ry="7.3" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="71.6" cy="58.4" rx="2.9" ry="4.9" fill="#1d3450" />
              <circle cx="68.8" cy="54.5" r="2.3" fill="#ffffff" />
              <circle cx="74.2" cy="61" r="1.2" fill="#ffffff" opacity=".9" />
            </g>
            <g class="ps-eyes-smile">
              <path d="M41 57 q7 -8 14 0 M65 57 q7 -8 14 0" fill="none" stroke="#2f4a6b" stroke-width="2.6" stroke-linecap="round" />
            </g>

            <ellipse cx="36" cy="69" rx="7" ry="4.4" :fill="'url(#' + uid + 'blush)'" />
            <ellipse cx="84" cy="69" rx="7" ry="4.4" :fill="'url(#' + uid + 'blush)'" />
            <path d="M54 67 L66 67 L60 74 Z" fill="#ffc061" stroke="#e39a3e" stroke-width="1" stroke-linejoin="round" />
            <path d="M55.5 71 L64.5 71 L60 77 Z" fill="#f0a24a" stroke="#d98a35" stroke-width=".8" stroke-linejoin="round" />
            <path d="M57 68.2 h6" stroke="#ffffff" stroke-width="1.3" stroke-linecap="round" opacity=".7" />
          </g>
        </g>

        <!-- ==================== 花枝鼠（小家鼠） ==================== -->
        <g v-else-if="safeKind === 'mouse'" class="ps-animal">
          <!-- 细长卷尾 -->
          <g class="ps-tail ps-tail-thin">
            <path d="M83 96 C103 101 113 88 106 76 C102 69 96 70 97 76"
                  fill="none" :stroke="palette.line" stroke-width="6.5" stroke-linecap="round" opacity=".5" />
            <path d="M83 96 C103 101 113 88 106 76 C102 69 96 70 97 76"
                  fill="none" :stroke="palette.body2" stroke-width="4.5" stroke-linecap="round" />
            <path d="M84 96 C98 99 105 92 105 84" fill="none" :stroke="palette.head1" stroke-width="1.8" stroke-linecap="round" opacity=".8" />
          </g>

          <g class="ps-body">
            <path d="M33 91 C33 78 44 72 60 72 C76 72 87 78 87 91 C87 104 76 110 60 110 C44 110 33 104 33 91 Z"
                  :fill="'url(#' + uid + 'body)'" :stroke="palette.line" stroke-width="1.6" />
            <path d="M33 91 C33 78 44 72 60 72 C76 72 87 78 87 91 C87 104 76 110 60 110 C44 110 33 104 33 91 Z"
                  :fill="'url(#' + uid + 'shade)'" />
            <path d="M48 80 q12 8 24 0 q-3 14 -12 16 q-9 -2 -12 -16 z" fill="#ffffff" opacity=".9" />
            <g class="ps-paw ps-paw-l">
              <ellipse cx="45" cy="106" rx="9.5" ry="6.4" :fill="palette.inner" :stroke="palette.line" stroke-width="1.2" />
              <path d="M42 107 v2 M46 107.6 v2 M50 107 v1.8" :stroke="palette.line" stroke-width="1" stroke-linecap="round" opacity=".6" />
            </g>
            <g class="ps-paw ps-paw-r">
              <ellipse cx="75" cy="106" rx="9.5" ry="6.4" :fill="palette.inner" :stroke="palette.line" stroke-width="1.2" />
              <path d="M70 107 v1.8 M74 107.6 v2 M78 107 v2" :stroke="palette.line" stroke-width="1" stroke-linecap="round" opacity=".6" />
            </g>
          </g>

          <g class="ps-head">
            <!-- 超大圆耳 -->
            <g class="ps-ear ps-ear-l">
              <circle cx="31" cy="34" r="14" :fill="palette.head2" :stroke="palette.line" stroke-width="1.8" />
              <circle cx="31" cy="34" r="9" :fill="'url(#' + uid + 'earInner)'" />
              <circle cx="27" cy="30" r="3" fill="#ffffff" opacity=".55" />
            </g>
            <g class="ps-ear ps-ear-r">
              <circle cx="89" cy="34" r="14" :fill="palette.head2" :stroke="palette.line" stroke-width="1.8" />
              <circle cx="89" cy="34" r="9" :fill="'url(#' + uid + 'earInner)'" />
              <circle cx="85" cy="30" r="3" fill="#ffffff" opacity=".55" />
            </g>

            <!-- 下巴略收的小尖脸 -->
            <path d="M60 27 C79 27 94 39 94 56 C94 70 88 81 76 87 C70 90 65 91 60 91 C55 91 50 90 44 87 C32 81 26 70 26 56 C26 39 41 27 60 27 Z"
                  :fill="'url(#' + uid + 'head)'" :stroke="palette.line" stroke-width="1.8" />
            <path d="M60 27 C79 27 94 39 94 56 C94 70 88 81 76 87 C70 90 65 91 60 91 C55 91 50 90 44 87 C32 81 26 70 26 56 C26 39 41 27 60 27 Z"
                  :fill="'url(#' + uid + 'shade)'" />
            <ellipse class="ps-shine" cx="45" cy="39" rx="12" ry="6" fill="#ffffff" opacity=".46" transform="rotate(-25 45 39)" />
            <ellipse cx="60" cy="56" rx="32" ry="29" :fill="'url(#' + uid + 'gloss)'" />

            <g class="ps-eyes">
              <ellipse cx="47" cy="56" rx="8.2" ry="9.8" fill="#ffffff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="47.4" cy="56.8" rx="5.9" ry="7.9" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="47.4" cy="57.4" rx="3" ry="5.2" fill="#272742" />
              <circle cx="44.7" cy="53.4" r="2.4" fill="#ffffff" />
              <circle cx="50.3" cy="60.4" r="1.2" fill="#ffffff" opacity=".9" />
              <ellipse cx="73" cy="56" rx="8.2" ry="9.8" fill="#ffffff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="72.6" cy="56.8" rx="5.9" ry="7.9" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="72.6" cy="57.4" rx="3" ry="5.2" fill="#272742" />
              <circle cx="69.7" cy="53.4" r="2.4" fill="#ffffff" />
              <circle cx="75.3" cy="60.4" r="1.2" fill="#ffffff" opacity=".9" />
            </g>
            <g class="ps-eyes-smile">
              <path d="M40 56 q7 -8 14 0 M66 56 q7 -8 14 0" fill="none" stroke="#4a4a6b" stroke-width="2.6" stroke-linecap="round" />
            </g>

            <ellipse cx="34" cy="69" rx="7" ry="4.5" :fill="'url(#' + uid + 'blush)'" />
            <ellipse cx="86" cy="69" rx="7" ry="4.5" :fill="'url(#' + uid + 'blush)'" />
            <path d="M57.6 69 L62.4 69 L60 72 Z" fill="#f19fb4" stroke="#d9829a" stroke-width=".7" stroke-linejoin="round" />
            <path class="ps-mouth" d="M60 72 v2 M60 74 q-3 2.6 -5 .3 M60 74 q3 2.6 5 .3"
                  fill="none" stroke="#8a8aa6" stroke-width="1.5" stroke-linecap="round" />
            <path d="M58 76 h4 v3.2 q-2 1.2 -4 0 z" fill="#ffffff" stroke="#c9d0e0" stroke-width=".7" />
            <path d="M28 68 l-10 -3 M28 73 l-10 1 M92 68 l10 -3 M92 73 l10 1"
                  :stroke="palette.line" stroke-width="1.3" stroke-linecap="round" opacity=".8" />
          </g>
        </g>

        <!-- ==================== 红松鼠 ==================== -->
        <g v-else-if="safeKind === 'squirrel'" class="ps-animal">
          <!-- 超大蓬松尾巴，向上卷过身侧 -->
          <g class="ps-tail ps-tail-big ps-tail-sq">
            <path d="M80 96 C105 105 117 82 106 61 C99 48 87 47 85 58 C83 67 94 69 94 77 C94 85 86 87 80 82 Z"
                  :fill="palette.body2" :stroke="palette.line" stroke-width="1.6" stroke-linejoin="round" />
            <path d="M90 52 q9 -6 13 3 q-3 8 -11 9 q5 -6 -2 -12 z" :fill="palette.head1" :stroke="palette.line" stroke-width="1" />
            <path d="M85 93 C99 98 107 84 102 71" fill="none" :stroke="palette.body1" stroke-width="4.5" stroke-linecap="round" opacity=".8" />
            <path d="M88 60 q-4 6 -2 12 M98 56 q-3 5 -2 9" :stroke="palette.accent" stroke-width="2" fill="none" stroke-linecap="round" opacity=".45" />
          </g>

          <g class="ps-body">
            <path d="M33 91 C33 78 44 72 60 72 C76 72 87 78 87 91 C87 104 76 110 60 110 C44 110 33 104 33 91 Z"
                  :fill="'url(#' + uid + 'body)'" :stroke="palette.line" stroke-width="1.6" />
            <path d="M33 91 C33 78 44 72 60 72 C76 72 87 78 87 91 C87 104 76 110 60 110 C44 110 33 104 33 91 Z"
                  :fill="'url(#' + uid + 'shade)'" />
            <path d="M48 80 q12 8 24 0 q-3 14 -12 16 q-9 -2 -12 -16 z" fill="#fff3e2" opacity=".9" />
            <!-- 捧着松果的小爪 -->
            <g class="ps-paw ps-hold">
              <ellipse cx="52" cy="95" rx="6" ry="4.6" :fill="palette.head1" :stroke="palette.line" stroke-width="1.1" />
              <ellipse cx="68" cy="95" rx="6" ry="4.6" :fill="palette.head1" :stroke="palette.line" stroke-width="1.1" />
              <path d="M56 93 q4 -7 8 0 q-1 8 -4 10 q-3 -2 -4 -10 z" fill="#b3793f" stroke="#8a5a2c" stroke-width=".8" />
              <path d="M57 90 l3 -2 M60 89 l1 -2 M63 90 l-2 -2" stroke="#8a5a2c" stroke-width=".8" fill="none" opacity=".7" />
            </g>
          </g>

          <g class="ps-head">
            <!-- 尖耳 + 耳尖簇毛 -->
            <g class="ps-ear ps-ear-l">
              <path d="M31 43 L25 14 Q25 12 27 13 L53 29 Z" :fill="palette.head2" :stroke="palette.line" stroke-width="2" stroke-linejoin="round" />
              <path d="M35 37 L30 21 L45 30 Z" :fill="'url(#' + uid + 'earInner)'" />
              <path d="M26 14 q-3 -9 2 -11 q2 6 3 10 z" :fill="palette.accent" />
            </g>
            <g class="ps-ear ps-ear-r">
              <path d="M89 43 L95 14 Q95 12 93 13 L67 29 Z" :fill="palette.head2" :stroke="palette.line" stroke-width="2" stroke-linejoin="round" />
              <path d="M85 37 L90 21 L75 30 Z" :fill="'url(#' + uid + 'earInner)'" />
              <path d="M94 14 q3 -9 -2 -11 q-2 6 -3 10 z" :fill="palette.accent" />
            </g>

            <path d="M60 26 C80 26 95 39 95 57 C95 69 91 78 83 84 C76 89 69 90 60 90 C51 90 44 89 37 84 C29 78 25 69 25 57 C25 39 40 26 60 26 Z"
                  :fill="'url(#' + uid + 'head)'" :stroke="palette.line" stroke-width="1.8" />
            <path d="M60 26 C80 26 95 39 95 57 C95 69 91 78 83 84 C76 89 69 90 60 90 C51 90 44 89 37 84 C29 78 25 69 25 57 C25 39 40 26 60 26 Z"
                  :fill="'url(#' + uid + 'shade)'" />
            <ellipse class="ps-shine" cx="45" cy="39" rx="12" ry="6" fill="#ffffff" opacity=".42" transform="rotate(-25 45 39)" />
            <ellipse cx="60" cy="56" rx="32" ry="29" :fill="'url(#' + uid + 'gloss)'" />
            <path d="M43 66 q17 -6 34 0 q-3 16 -17 21 q-14 -5 -17 -21 z" fill="#fff3e2" opacity=".93" />

            <g class="ps-eyes">
              <ellipse cx="47" cy="55" rx="8.4" ry="10" fill="#ffffff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="47.4" cy="55.8" rx="6.1" ry="8" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="47.4" cy="56.4" rx="3.1" ry="5.3" fill="#3a2312" />
              <circle cx="44.6" cy="52.3" r="2.5" fill="#ffffff" />
              <circle cx="50.3" cy="59.5" r="1.3" fill="#ffffff" opacity=".9" />
              <path d="M39 49 q8 -5 16 0" fill="none" :stroke="palette.line" stroke-width="1.8" stroke-linecap="round" opacity=".7" />
              <ellipse cx="73" cy="55" rx="8.4" ry="10" fill="#ffffff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="72.6" cy="55.8" rx="6.1" ry="8" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="72.6" cy="56.4" rx="3.1" ry="5.3" fill="#3a2312" />
              <circle cx="69.6" cy="52.3" r="2.5" fill="#ffffff" />
              <circle cx="75.3" cy="59.5" r="1.3" fill="#ffffff" opacity=".9" />
              <path d="M65 49 q8 -5 16 0" fill="none" :stroke="palette.line" stroke-width="1.8" stroke-linecap="round" opacity=".7" />
            </g>
            <g class="ps-eyes-smile">
              <path d="M39 55 q8 -9 16 0 M65 55 q8 -9 16 0" fill="none" stroke="#6b4327" stroke-width="2.7" stroke-linecap="round" />
            </g>

            <ellipse cx="33" cy="68" rx="7.5" ry="4.6" :fill="'url(#' + uid + 'blush)'" />
            <ellipse cx="87" cy="68" rx="7.5" ry="4.6" :fill="'url(#' + uid + 'blush)'" />
            <path d="M57.6 68 L62.4 68 L60 71 Z" fill="#a9683e" stroke="#8a5330" stroke-width=".7" stroke-linejoin="round" />
            <path class="ps-mouth" d="M60 71 v2 M60 73 q-3 2.6 -5 .3 M60 73 q3 2.6 5 .3"
                  fill="none" stroke="#8a5a3a" stroke-width="1.5" stroke-linecap="round" />
            <path d="M58 75 h4 v3.4 q-2 1.2 -4 0 z" fill="#ffffff" stroke="#e0c4a4" stroke-width=".7" />
          </g>
        </g>

        <!-- ==================== 小鸟 ==================== -->
        <g v-else class="ps-animal">
          <!-- 三层分叉尾羽 -->
          <g class="ps-tailfeather">
            <path d="M76 84 q14 4 22 14 q-12 -2 -20 -6 z" :fill="palette.body2" :stroke="palette.line" stroke-width="1.2" stroke-linejoin="round" />
            <path d="M76 88 q13 6 18 13 q-11 -3 -17 -7 z" :fill="palette.body1" :stroke="palette.line" stroke-width="1" stroke-linejoin="round" opacity=".95" />
            <path d="M77 92 q10 6 13 11 q-9 -3 -13 -7 z" :fill="palette.head1" opacity=".9" />
          </g>

          <!-- 小脚：腿 + 三趾 + 趾甲 -->
          <path d="M50 97 v12 M70 97 v12" :stroke="palette.line" stroke-width="4.2" stroke-linecap="round" opacity=".45" />
          <path d="M50 97 v12 M70 97 v12" stroke="#f0a24a" stroke-width="3" stroke-linecap="round" />
          <path d="M50 109 l-6 3 M50 109 l6 3 M50 109 v3 M70 109 l-6 3 M70 109 l6 3 M70 109 v3"
                stroke="#f0a24a" stroke-width="2.4" stroke-linecap="round" fill="none" />
          <path d="M43 112 l-2 .6 M57 112 l2 .6 M63 112 l-2 .6 M77 112 l2 .6" stroke="#d98a35" stroke-width="1.2" stroke-linecap="round" />

          <g class="ps-head">
            <!-- 蛋形身体：上窄下宽 -->
            <path d="M60 33 C79 33 91 48 91 67 C91 86 78 100 60 100 C42 100 29 86 29 67 C29 48 41 33 60 33 Z"
                  :fill="'url(#' + uid + 'head)'" :stroke="palette.line" stroke-width="1.8" />
            <path d="M60 33 C79 33 91 48 91 67 C91 86 78 100 60 100 C42 100 29 86 29 67 C29 48 41 33 60 33 Z"
                  :fill="'url(#' + uid + 'shade)'" />
            <ellipse class="ps-shine" cx="46" cy="47" rx="11" ry="6" fill="#ffffff" opacity=".5" transform="rotate(-28 46 47)" />
            <ellipse cx="60" cy="63" rx="29" ry="30" :fill="'url(#' + uid + 'gloss)'" />
            <!-- 肚皮：三层绒毛 -->
            <path d="M44 72 q16 -6 32 0 q6 16 -4 24 q-12 6 -24 0 q-10 -8 -4 -24 z" fill="#f2fbff" opacity=".9" />
            <path d="M48 82 q12 5 24 0 M50 89 q10 4 20 0" stroke="#cbeaf8" stroke-width="1.4" fill="none" stroke-linecap="round" opacity=".7" />

            <!-- 头顶三根带厚度的羽毛 -->
            <g class="ps-ahoge">
              <path d="M55 36 q-7 -11 -1 -17 q3 8 7 12 z" :fill="palette.accent" :stroke="palette.line" stroke-width=".8" />
              <path d="M61 34 q0 -13 7 -17 q0 9 -1 15 z" :fill="palette.body2" :stroke="palette.line" stroke-width=".8" />
              <path d="M67 36 q7 -10 14 -9 q-6 7 -10 12 z" :fill="palette.head1" :stroke="palette.line" stroke-width=".8" />
            </g>

            <!-- 会扇的翅膀：每侧 3 片分层羽毛 -->
            <g class="ps-wing ps-wing-l">
              <path d="M33 60 q-18 2 -17 20 q1 12 17 7 q9 -6 8 -20 z" :fill="palette.body2" :stroke="palette.line" stroke-width="1.3" />
              <path d="M31 66 q-13 2 -12 15 q1 7 11 4" fill="none" :stroke="palette.head1" stroke-width="4" stroke-linecap="round" opacity=".8" />
              <path d="M29 73 q-8 2 -7 9 M30 80 q-6 2 -5 6" :stroke="palette.line" stroke-width="1.3" fill="none" stroke-linecap="round" opacity=".6" />
              <path d="M25 65 q-5 4 -5 10" stroke="#ffffff" stroke-width="2" fill="none" opacity=".55" stroke-linecap="round" />
            </g>
            <g class="ps-wing ps-wing-r">
              <path d="M87 60 q18 2 17 20 q-1 12 -17 7 q-9 -6 -8 -20 z" :fill="palette.body2" :stroke="palette.line" stroke-width="1.3" />
              <path d="M89 66 q13 2 12 15 q-1 7 -11 4" fill="none" :stroke="palette.head1" stroke-width="4" stroke-linecap="round" opacity=".8" />
              <path d="M91 73 q8 2 7 9 M90 80 q6 2 5 6" :stroke="palette.line" stroke-width="1.3" fill="none" stroke-linecap="round" opacity=".6" />
              <path d="M95 65 q5 4 5 10" stroke="#ffffff" stroke-width="2" fill="none" opacity=".55" stroke-linecap="round" />
            </g>

            <g class="ps-eyes">
              <ellipse cx="49" cy="59" rx="8.4" ry="10" fill="#ffffff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="49.4" cy="59.8" rx="6.1" ry="8" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="49.4" cy="60.4" rx="3.1" ry="5.3" fill="#26415c" />
              <circle cx="46.6" cy="56.3" r="2.5" fill="#ffffff" />
              <circle cx="52.3" cy="63.5" r="1.3" fill="#ffffff" opacity=".9" />
              <path d="M41 53 q8 -5 16 0" fill="none" :stroke="palette.line" stroke-width="1.8" stroke-linecap="round" opacity=".7" />
              <ellipse cx="71" cy="59" rx="8.4" ry="10" fill="#ffffff" :stroke="palette.line" stroke-width=".9" />
              <ellipse cx="70.6" cy="59.8" rx="6.1" ry="8" :fill="'url(#' + uid + 'iris)'" />
              <ellipse cx="70.6" cy="60.4" rx="3.1" ry="5.3" fill="#26415c" />
              <circle cx="67.6" cy="56.3" r="2.5" fill="#ffffff" />
              <circle cx="73.3" cy="63.5" r="1.3" fill="#ffffff" opacity=".9" />
              <path d="M63 53 q8 -5 16 0" fill="none" :stroke="palette.line" stroke-width="1.8" stroke-linecap="round" opacity=".7" />
            </g>
            <g class="ps-eyes-smile">
              <path d="M41 59 q8 -9 16 0 M63 59 q8 -9 16 0" fill="none" stroke="#35688e" stroke-width="2.7" stroke-linecap="round" />
            </g>

            <ellipse cx="36" cy="70" rx="7.5" ry="4.8" :fill="'url(#' + uid + 'blush)'" />
            <ellipse cx="84" cy="70" rx="7.5" ry="4.8" :fill="'url(#' + uid + 'blush)'" />
            <!-- 上下喙分开 + 高光 -->
            <path d="M54.5 68.5 L65.5 68.5 L60 75 Z" fill="#ffc061" :stroke="palette.line" stroke-width="1" stroke-linejoin="round" />
            <path d="M55.5 72 L64.5 72 L60 79 Z" fill="#f0a24a" stroke="#d98a35" stroke-width=".9" stroke-linejoin="round" />
            <path d="M57 69.5 h6" stroke="#ffffff" stroke-width="1.4" stroke-linecap="round" opacity=".7" />
          </g>
        </g>
      </g>
    </svg>

    <!-- 立绘上的游走高光，让静态图也有一层活着的光泽 -->
    <span v-if="usePhoto && !still" class="ps-photo-gloss"></span>

    <!-- 装扮角标（衣橱里的帽子/蝴蝶结等） -->
    <span v-if="cosmetic" class="ps-cosmetic" :style="{ fontSize: cosmeticSize + 'px' }" :title="cosmeticName || ''">{{ cosmetic }}</span>

    <!-- 状态小特效：爱心 / 星星 / 呼呼 / 脚印 -->
    <span v-for="(f, i) in fx" :key="i" class="ps-fx"
          :style="{ left: (50 + (i - 1) * 22) + '%', animationDelay: (i * 0.32) + 's' }">{{ f }}</span>
  </span>
</template>

<script>
/**
 * 宠物形象 3.0：AI 原创 3D 毛绒立绘 + CSS 动作（SVG 为兜底）。
 *
 * 默认走 public/pets/<kind>.webp 的 3D 毛绒立绘，圆形相框呈现，
 * 用呼吸浮动 / 歪头 / 蹦跳 / 踱步 / 点头 / 游走高光 + 状态粒子让它"会动"；
 * 图片缺失或加载失败时自动回退到内置的纯 SVG Q 版形象（离线也能显示）。
 *
 * 14 个真实品种：狸花猫 / 萨摩耶 / 玄凤鹦鹉 / 垂耳兔 / 赤狐 / 棕熊 /
 * 金丝熊 / 小企鹅 / 花枝鼠 / 红松鼠 / 布偶猫 / 柯基犬 / 柴犬 / 虎皮鹦鹉。
 *
 * props:
 *   kind      品种 key（旧数据 dragon 自动当 dog，未知值兜底 cat）
 *   size      显示尺寸 px
 *   state     idle / hungry / happy / play / eat / walk
 *   cosmetic  装扮 emoji，戴在右上角
 *   still     关掉动画（聊天页紧凑小条用）
 *   title     鼠标悬停提示
 */
let seq = 0

const PALETTES = {
  cat: {
    head1: '#fff4df', head2: '#f7d6a4', body1: '#fbe4bd', body2: '#efc78c',
    eye: '#8660a0', eyeLight: '#c19ae0', accent: '#e9bd83', line: '#e0b27c',
    inner: '#ffb3c8', name: '狸花猫'
  },
  dog: {
    head1: '#fff1d2', head2: '#f2cb89', body1: '#f8dca8', body2: '#e7b672',
    eye: '#7b5638', eyeLight: '#b98b5d', accent: '#d9a35f', line: '#d7a15c',
    inner: '#c98d5a', name: '萨摩耶'
  },
  bird: {
    head1: '#e2f5ff', head2: '#a5dcf5', body1: '#c8ecfb', body2: '#89c8e9',
    eye: '#4f86b8', eyeLight: '#9ed2f2', accent: '#5fb4dd', line: '#79b9dd',
    inner: '#bfe6f8', name: '玄凤鹦鹉'
  },
  rabbit: {
    head1: '#fffafc', head2: '#f7e3ee', body1: '#fdf0f5', body2: '#efd3e2',
    eye: '#a76b8e', eyeLight: '#e0a9c4', accent: '#e9b7cd', line: '#dcb2c6',
    inner: '#ffb3c8', name: '垂耳兔'
  },
  fox: {
    head1: '#ffe6cc', head2: '#f8b978', body1: '#fcd09c', body2: '#e99a55',
    eye: '#8a5a2b', eyeLight: '#c99255', accent: '#d98b4a', line: '#d99a63',
    inner: '#ffd9bd', name: '赤狐'
  },
  bear: {
    head1: '#f7e4cd', head2: '#d9ae82', body1: '#e8c39b', body2: '#c99a6c',
    eye: '#5b3d29', eyeLight: '#a47a52', accent: '#b98a5f', line: '#c39a72',
    inner: '#f0cba8', name: '棕熊'
  },
  hamster: {
    head1: '#fff3dc', head2: '#f2d6a8', body1: '#fae6c2', body2: '#e6c58f',
    eye: '#4a3427', eyeLight: '#9c7850', accent: '#d9b47c', line: '#dcc096',
    inner: '#ffc9c9', name: '金丝熊'
  },
  penguin: {
    head1: '#eaf2fb', head2: '#8fa9c9', body1: '#b9cee6', body2: '#6b87ad',
    eye: '#2f4a6b', eyeLight: '#7fa6cf', accent: '#54739c', line: '#7e9bbd',
    inner: '#dce9f7', name: '小企鹅'
  },
  mouse: {
    head1: '#f6f8fd', head2: '#cdd6e8', body1: '#e4eaf6', body2: '#b3bfd6',
    eye: '#4a4a6b', eyeLight: '#9aa6c8', accent: '#a8b3cc', line: '#b9c3d6',
    inner: '#ffc0cf', name: '花枝鼠'
  },
  squirrel: {
    head1: '#ffe9d0', head2: '#f0b077', body1: '#fbd3a6', body2: '#d99252',
    eye: '#6b4327', eyeLight: '#b98550', accent: '#c77c42', line: '#d39a63',
    inner: '#ffd0b0', name: '红松鼠'
  },
  ragdoll: {
    head1: '#fffaf3', head2: '#e5d6c6', body1: '#f5e8db', body2: '#cdb49b',
    eye: '#4a7fc1', eyeLight: '#9ec6f0', accent: '#a98a70', line: '#c9ae94',
    inner: '#ffd0d8', name: '布偶猫'
  },
  corgi: {
    head1: '#fff0dc', head2: '#f0b877', body1: '#f9d6a8', body2: '#e0a35c',
    eye: '#6b4126', eyeLight: '#b58552', accent: '#d99a58', line: '#d6a166',
    inner: '#ffb9c6', name: '柯基犬'
  },
  shiba: {
    head1: '#fff2dd', head2: '#e8a35c', body1: '#f7cb92', body2: '#d68b46',
    eye: '#5c3b23', eyeLight: '#a87848', accent: '#c98240', line: '#cf9455',
    inner: '#ffc9b8', name: '柴犬'
  },
  budgie: {
    head1: '#e9f7ff', head2: '#9ad4f0', body1: '#c9ecfb', body2: '#82c2e6',
    eye: '#3f6f96', eyeLight: '#a3d6f2', accent: '#5fb2d9', line: '#79b8db',
    inner: '#fff0b8', name: '虎皮鹦鹉'
  }
}

/** 14 个真实品种的 3D 毛绒立绘（public/pets/，单张约 30KB WebP） */
const PET_PHOTOS = {
  cat: 'cat', dog: 'dog', bird: 'bird', rabbit: 'rabbit', fox: 'fox',
  bear: 'bear', hamster: 'hamster', penguin: 'penguin', mouse: 'mouse',
  squirrel: 'squirrel', ragdoll: 'ragdoll', corgi: 'corgi',
  shiba: 'shiba', budgie: 'budgie'
}

export default {
  name: 'PetSprite',
  props: {
    kind: { type: String, default: 'cat' },
    size: { type: Number, default: 96 },
    state: { type: String, default: 'idle' },
    cosmetic: { type: String, default: '' },
    cosmeticName: { type: String, default: '' },
    still: { type: Boolean, default: false },
    title: { type: String, default: '' }
  },
  data() {
    return { uid: 'ps' + (++seq), photoFailed: false }
  },
  computed: {
    /** 图片加载失败时自动回退到内置 SVG 形象 */
    usePhoto() { return !this.photoFailed },
    photoSrc() { return '/pets/' + (PET_PHOTOS[this.safeKind] || 'cat') + '.webp' },
    liveStyle() {
      const n = parseInt(String(this.uid).slice(2), 10) || 0
      return {
        '--fur': this.palette.head2,
        '--fur-light': this.palette.head1,
        '--ink': this.palette.eye,
        '--ps-delay': (-(n % 9) * 0.31).toFixed(2) + 's'
      }
    },
    safeKind() {
      const k = String(this.kind || '').toLowerCase()
      if (k === 'dog' || k === 'dragon' || k === 'puppy') return 'dog'
      if (Object.prototype.hasOwnProperty.call(PALETTES, k)) return k
      return 'cat'
    },
    palette() { return PALETTES[this.safeKind] },
    kindName() { return this.palette.name },
    cosmeticSize() { return Math.max(11, Math.round(this.size * 0.2)) },
    fx() {
      if (this.state === 'happy' || this.state === 'play') return ['💗', '✨', '💫']
      if (this.state === 'eat') return ['🍖', '💗']
      if (this.state === 'walk') return ['🐾', '✨']
      if (this.state === 'hungry') return ['💤']
      return []
    }
  }
}
</script>

<style scoped>
.pet-sprite {
  position: relative;
  display: inline-block;
  flex-shrink: 0;
  line-height: 0;
  vertical-align: middle;
  user-select: none;
}
.ps-svg { width: 100%; height: 100%; overflow: visible; display: block; }

/* 呼吸浮动 + 左右轻摆（合成一层，避免嵌套 transform 抖动） */
.ps-float { transform-box: fill-box; transform-origin: 50% 88%; animation: psFloat 2.8s ease-in-out infinite; }
@keyframes psFloat {
  0%, 100% { transform: translateY(0) rotate(-1.5deg) scale(1); }
  50% { transform: translateY(-5px) rotate(1.5deg) scale(1.012, .988); }
}
.ps-shadow { transform-box: fill-box; transform-origin: center; animation: psShadow 2.8s ease-in-out infinite; }
.ps-shadow-soft { transform-box: fill-box; transform-origin: center; animation: psShadowSoft 2.8s ease-in-out infinite; }
@keyframes psShadow { 0%, 100% { transform: scaleX(1); opacity: .16; } 50% { transform: scaleX(.84); opacity: .1; } }
@keyframes psShadowSoft { 0%, 100% { transform: scaleX(1); opacity: .07; } 50% { transform: scaleX(.88); opacity: .04; } }

/* 周期性歪头（连耳朵五官一起歪） */
.ps-head { transform-box: fill-box; transform-origin: 50% 92%; animation: psTilt 6.8s ease-in-out infinite; }
@keyframes psTilt {
  0%, 76%, 100% { transform: rotate(0deg); }
  82% { transform: rotate(-7deg); }
  90% { transform: rotate(5deg); }
}

/* 高光随呼吸微微游走 */
.ps-shine { transform-box: fill-box; transform-origin: center; animation: psShine 2.8s ease-in-out infinite; }
@keyframes psShine { 0%, 100% { opacity: .38; transform: translateX(0); } 50% { opacity: .56; transform: translateX(2px); } }

/* 眨眼：大部分时间睁着，偶尔快速一闭 */
.ps-eyes { transform-box: fill-box; transform-origin: center; animation: psBlink 4.6s infinite; }
@keyframes psBlink {
  0%, 88%, 94%, 100% { transform: scaleY(1); }
  91% { transform: scaleY(.1); }
}
/* 笑眼：开心 / 玩耍时换成弯月牙 */
.ps-eyes-smile { opacity: 0; transition: opacity .2s; }
.ps-state-happy .ps-eyes, .ps-state-play .ps-eyes { opacity: 0; animation: none; }
.ps-state-happy .ps-eyes-smile, .ps-state-play .ps-eyes-smile { opacity: 1; }

/* 猫 / 狗 尾巴 */
.ps-tail { transform-box: fill-box; transform-origin: 0% 100%; animation: psWag 1.6s ease-in-out infinite; }
.ps-tail-dog { animation-duration: .55s; }
@keyframes psWag { 0%, 100% { transform: rotate(-9deg); } 50% { transform: rotate(11deg); } }

/* 圆球尾 / 大蓬松尾 / 细长尾：各自的摆法 */
.ps-tail-ball { transform-origin: 15% 60%; animation: psBallWag 1.5s ease-in-out infinite; }
@keyframes psBallWag { 0%, 100% { transform: rotate(-10deg) translateY(0); } 50% { transform: rotate(10deg) translateY(-2px); } }
.ps-tail-big { animation: psBigWag 2.1s ease-in-out infinite; }
.ps-tail-sq { animation-duration: 2.6s; }
@keyframes psBigWag { 0%, 100% { transform: rotate(-13deg) scaleY(1); } 50% { transform: rotate(14deg) scaleY(1.06); } }
.ps-tail-thin { animation: psThinWag 1.3s ease-in-out infinite; }
@keyframes psThinWag { 0%, 100% { transform: rotate(-14deg); } 50% { transform: rotate(16deg); } }

/* 兔子的长耳：一前一后慢慢摆 */
.ps-ear-long { transform-origin: 50% 100%; animation: psLongEar 3.2s ease-in-out infinite; }
.ps-ear-long.ps-ear-r { animation-delay: .45s; }
@keyframes psLongEar { 0%, 100% { transform: rotate(-5deg); } 50% { transform: rotate(6deg); } }

/* 仓鼠腮帮一鼓一鼓 */
.ps-cheek { transform-box: fill-box; transform-origin: center; animation: psCheekPuff 1.9s ease-in-out infinite; }
@keyframes psCheekPuff { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.06, 1.03); } }

/* 企鹅左右摇摆 */
.ps-penguin .ps-float { animation: psWaddle 1.7s ease-in-out infinite; }
@keyframes psWaddle {
  0%, 100% { transform: translateY(0) rotate(-5deg); }
  50% { transform: translateY(-3px) rotate(5deg); }
}

/* 耳朵抖一抖 */
.ps-ear { transform-box: fill-box; transform-origin: 50% 100%; animation: psTwitch 5.2s ease-in-out infinite; }
.ps-ear-r { animation-delay: .35s; }
@keyframes psTwitch { 0%, 82%, 100% { transform: rotate(0); } 86% { transform: rotate(-7deg); } 92% { transform: rotate(5deg); } }

/* 呆毛晃 */
.ps-ahoge { transform-box: fill-box; transform-origin: 50% 100%; animation: psAhoge 2.9s ease-in-out infinite; }
@keyframes psAhoge { 0%, 100% { transform: rotate(-4deg); } 50% { transform: rotate(5deg); } }

/* 小鸟翅膀扑棱 + 飘得更高一点 */
.ps-wing { transform-box: fill-box; animation: psFlap .9s ease-in-out infinite; }
.ps-wing-l { transform-origin: 100% 10%; }
.ps-wing-r { transform-origin: 0% 10%; animation-delay: .08s; }
@keyframes psFlap {
  0%, 100% { transform: rotate(0deg) scaleY(1); }
  50% { transform: rotate(-12deg) scaleY(.82); }
}
.ps-bird .ps-float { animation-duration: 2s; }
.ps-bird .ps-shadow, .ps-bird .ps-shadow-soft { animation-duration: 2s; }
.ps-tailfeather { transform-box: fill-box; transform-origin: 0% 50%; animation: psWag 1.9s ease-in-out infinite; }

/* 小爪子轻轻踏步 */
.ps-paw { transform-box: fill-box; transform-origin: center; animation: psPaw 2.8s ease-in-out infinite; }
.ps-paw-r { animation-delay: .5s; }
@keyframes psPaw { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-1.6px); } }

/* 舌头（开心时伸得更长） */
.ps-tongue { transform-box: fill-box; transform-origin: 50% 0%; animation: psTongue 1.4s ease-in-out infinite; }
@keyframes psTongue { 0%, 100% { transform: scaleY(1); } 50% { transform: scaleY(1.18); } }

/* 状态：饿了 → 灰灰的、动作变慢、眼睛半闭 */
.ps-state-hungry { filter: grayscale(.55); }
.ps-state-hungry .ps-float,
.ps-state-hungry .ps-shadow,
.ps-state-hungry .ps-shadow-soft,
.ps-state-hungry .ps-tail,
.ps-state-hungry .ps-wing,
.ps-state-hungry .ps-tail-ball,
.ps-state-hungry .ps-tail-big,
.ps-state-hungry .ps-tail-thin,
.ps-state-hungry .ps-ear-long,
.ps-state-hungry .ps-cheek,
.ps-state-hungry.ps-penguin .ps-float { animation-duration: 4.5s; }
.ps-state-hungry .ps-eyes { animation: psHalfClose 4.5s infinite; }
@keyframes psHalfClose { 0%, 100% { transform: scaleY(.72); } 50% { transform: scaleY(.6); } }

/* 状态：开心 / 玩耍 → 蹦得更起劲 */
.ps-state-happy .ps-float,
.ps-state-play .ps-float { animation: psHop .62s ease-in-out infinite; }
@keyframes psHop {
  0%, 100% { transform: translateY(0) rotate(-3deg) scale(1); }
  35% { transform: translateY(-11px) rotate(4deg) scale(1.04, .96); }
  70% { transform: translateY(-2px) rotate(-2deg) scale(.98, 1.02); }
}
.ps-state-play .ps-tail, .ps-state-happy .ps-tail { animation-duration: .32s; }

/* 状态：吃东西 → 嘴巴一张一张 */
.ps-state-eat .ps-mouth { transform-box: fill-box; transform-origin: 50% 0%; animation: psMunch .42s ease-in-out infinite; }
@keyframes psMunch { 0%, 100% { transform: scaleY(1); } 50% { transform: scaleY(1.7); } }

/* 状态：散步 → 左右踱步、爪子交替、尾巴加快 */
.ps-state-walk .ps-float { animation: psWalk 1.15s ease-in-out infinite; }
@keyframes psWalk {
  0%, 100% { transform: translate(-7px, 0) rotate(-4deg); }
  50% { transform: translate(7px, -4px) rotate(4deg); }
}
.ps-state-walk .ps-paw { animation-duration: .55s; }
.ps-state-walk .ps-paw-l { animation-delay: .27s; }
.ps-state-walk .ps-tail { animation-duration: .5s; }

/* 装扮角标 */
.ps-cosmetic {
  position: absolute; top: -8%; right: -8%;
  line-height: 1;
  filter: drop-shadow(0 2px 3px rgba(160, 108, 213, .4));
  animation: psCosmetic 2.6s ease-in-out infinite;
}
@keyframes psCosmetic { 0%, 100% { transform: translateY(0) rotate(-6deg); } 50% { transform: translateY(-3px) rotate(7deg); } }

/* 冒出来的小心心 / 星星 */
.ps-fx {
  position: absolute;
  top: 4%;
  font-size: 13px; line-height: 1;
  pointer-events: none;
  animation: psFx 1.5s ease-out infinite;
  opacity: 0;
}
@keyframes psFx {
  0% { opacity: 0; transform: translateY(6px) scale(.5) rotate(-10deg); }
  25% { opacity: 1; }
  100% { opacity: 0; transform: translateY(-30px) scale(1.1) rotate(12deg); }
}

/* 小尺寸（聊天页小条）：特效和角标收一收，避免糊成一团 */
.ps-mini .ps-fx { display: none; }
.ps-mini .ps-cosmetic { font-size: 12px; }

/* 静态模式 */
.ps-still .ps-float,
.ps-still .ps-shadow,
.ps-still .ps-shadow-soft,
.ps-still .ps-head,
.ps-still .ps-shine,
.ps-still .ps-eyes,
.ps-still .ps-tail,
.ps-still .ps-tail-ball,
.ps-still .ps-tail-big,
.ps-still .ps-tail-thin,
.ps-still .ps-ear,
.ps-still .ps-ear-long,
.ps-still .ps-cheek,
.ps-still .ps-ahoge,
.ps-still .ps-wing,
.ps-still .ps-paw,
.ps-still .ps-tongue,
.ps-still .ps-tailfeather,
.ps-still .ps-cosmetic,
.ps-still .ps-fx { animation: none; }

/* 尊重系统「减少动态效果」设置 */
@media (prefers-reduced-motion: reduce) {
  .pet-sprite * { animation: none !important; }
}

/* ============ 图片模式 4.0：分层活体动画（真 3D 舞台的 2D 降级） ============ */
.ps-live {
  position: absolute; inset: 0; display: block;
  border-radius: 50%; transform-origin: 50% 88%;
  animation: psLiveIdle 2.8s ease-in-out infinite;
  animation-delay: var(--ps-delay); will-change: transform;
}
@keyframes psLiveIdle {
  0%, 100% { transform: translateY(0) rotate(-1.2deg); }
  50% { transform: translateY(-4%) rotate(1.2deg); }
}
.ps-ground {
  position: absolute; left: 14%; right: 14%; bottom: 0; height: 10%;
  border-radius: 50%; z-index: 0;
  background: radial-gradient(ellipse, rgba(124,92,158,.3), rgba(124,92,158,0) 72%);
  animation: psGround 2.8s ease-in-out infinite; animation-delay: var(--ps-delay);
}
@keyframes psGround { 0%,100% { transform: scaleX(1); opacity:.8 } 50% { transform: scaleX(.76); opacity:.45 } }

.ps-layer {
  position: absolute; inset: 0; width: 100%; height: 100%;
  object-fit: cover; border-radius: 50%; display: block; background: #fdf3e9;
}
/* 底层：弹性呼吸（squash & stretch），脚底锚定 */
.ps-base {
  box-shadow: 0 5px 16px rgba(150,106,190,.22), 0 0 0 2px rgba(255,255,255,.72);
  transform-origin: 50% 92%;
  animation: psBreath 2.8s ease-in-out infinite; animation-delay: var(--ps-delay);
}
@keyframes psBreath {
  0%,100% { transform: scale(1,1); }
  25% { transform: scale(.982,1.024); }
  50% { transform: scale(1.016,.98); }
  75% { transform: scale(.99,1.012); }
}
/* 头部层：柔边遮罩只留上半身，单独歪头 / 点头 */
.ps-headlayer {
  z-index: 2; background: transparent; transform-origin: 50% 74%;
  -webkit-mask-image: linear-gradient(to bottom, #000 0%, #000 38%, rgba(0,0,0,.8) 50%, transparent 68%);
  mask-image: linear-gradient(to bottom, #000 0%, #000 38%, rgba(0,0,0,.8) 50%, transparent 68%);
  animation: psHeadTilt 6.4s ease-in-out infinite;
  animation-delay: calc(var(--ps-delay) - .18s);
}
@keyframes psHeadTilt {
  0%, 68%, 100% { transform: rotate(0) translateY(0); }
  75% { transform: rotate(-6deg) translateY(-1%); }
  83% { transform: rotate(5deg) translateY(1%); }
  91% { transform: rotate(-2deg) translateY(0); }
}
/* 眨眼：眼睑毛色跟品种自动匹配，压住大眼再画一道弯月牙 */
.ps-blink {
  position: absolute; inset: 0; z-index: 3; pointer-events: none;
  opacity: 0; animation: psBlinkOn 4.6s linear infinite;
  animation-delay: var(--ps-delay);
}
@keyframes psBlinkOn { 0%,87%,93%,100% { opacity:0 } 88%,92% { opacity:1 } }
.ps-lid {
  position: absolute; top: 40%; width: 18%; height: 11%; border-radius: 50%;
  background: radial-gradient(ellipse at 50% 38%, var(--fur-light) 0%, var(--fur) 58%, transparent 80%);
  backdrop-filter: blur(1.5px);
}
.ps-lid-l { left: 31%; }
.ps-lid-r { right: 31%; }
.ps-curve {
  position: absolute; top: 45.5%; width: 10.5%; height: 4.5%;
  border-top: 2px solid var(--ink); border-radius: 60% 60% 0 0; opacity: .72;
}
.ps-curve-l { left: 35%; }
.ps-curve-r { right: 35%; }

.ps-photo-gloss {
  position: absolute; inset: 0; z-index: 4; border-radius: 50%;
  pointer-events: none; mix-blend-mode: soft-light;
  background: linear-gradient(115deg, transparent 38%, rgba(255,255,255,.55) 50%, transparent 62%);
  background-size: 260% 100%;
  animation: psGloss 5.2s ease-in-out infinite; animation-delay: var(--ps-delay);
}
@keyframes psGloss { 0%,62% { background-position:160% 0; opacity:0 } 70% { opacity:1 } 100% { background-position:-70% 0; opacity:0 } }

/* 状态动作：蹦跳 / 咀嚼点头 / 左右踱步 / 打盹 */
.ps-state-happy .ps-live, .ps-state-play .ps-live { animation: psLiveHop .62s ease-in-out infinite; }
@keyframes psLiveHop {
  0%,100% { transform: translateY(0) rotate(-3deg) scale(1); }
  35% { transform: translateY(-12%) rotate(4deg) scale(1.05,.95); }
  70% { transform: translateY(-2%) rotate(-2deg) scale(.97,1.03); }
}
.ps-state-happy .ps-blink, .ps-state-play .ps-blink { animation-duration: 2.2s; }
.ps-state-eat .ps-live { animation: psLiveMunch .45s ease-in-out infinite; }
@keyframes psLiveMunch { 0%,100% { transform: translateY(0) scaleY(1) } 50% { transform: translateY(3%) scaleY(.96) } }
.ps-state-eat .ps-headlayer { animation: psNod .45s ease-in-out infinite; }
@keyframes psNod { 0%,100% { transform: translateY(0) rotate(0) } 50% { transform: translateY(3%) rotate(-3deg) } }
.ps-state-walk .ps-live { animation: psLiveWalk 1.1s ease-in-out infinite; }
@keyframes psLiveWalk { 0%,100% { transform: translate(-8%,0) rotate(-5deg) } 50% { transform: translate(8%,-4%) rotate(5deg) } }
.ps-state-walk .ps-base { animation-duration: 1.1s; }
.ps-state-hungry .ps-live, .ps-state-hungry .ps-base,
.ps-state-hungry .ps-headlayer, .ps-state-hungry .ps-ground { animation-duration: 4.6s; }
.ps-state-hungry .ps-layer { filter: grayscale(.6); }
.ps-state-hungry .ps-blink { animation: none; opacity: .85; }
.ps-state-hungry .ps-lid { height: 6%; top: 43%; }

.ps-still .ps-live, .ps-still .ps-ground, .ps-still .ps-base,
.ps-still .ps-headlayer, .ps-still .ps-blink, .ps-still .ps-photo-gloss { animation: none; }
.ps-still .ps-blink { opacity: 0; }
.ps-mini .ps-blink { display: none; }
@media (prefers-reduced-motion: reduce) {
  .ps-live, .ps-ground, .ps-base, .ps-headlayer, .ps-blink, .ps-photo-gloss { animation: none !important; }
  .ps-blink { opacity: 0 !important; }
}

</style>
