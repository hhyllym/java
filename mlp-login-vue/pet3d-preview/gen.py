import io, os

ROOT = r'D:\workSpace\java\mlp-login-vue'
BUNDLE = os.path.join(ROOT, 'pet3d-preview', 'bundle.js')
OUT = r'C:\Users\18017\WorkBuddy\2026-11-08-14-35-35\星糖谷真3D宠物.html'

js = io.open(BUNDLE, encoding='utf-8').read().replace('</script', '<\\/script')

HTML = u'''<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>星糖谷 · 真 3D 宠物图鉴</title>
<style>
  * { box-sizing: border-box; }
  body {
    margin: 0; padding: 26px 18px 40px;
    font-family: "PingFang SC","Microsoft YaHei",system-ui,-apple-system,sans-serif;
    color: #4a3c5c;
    background: radial-gradient(circle at 12% 8%, #fff2fb 0, transparent 42%),
                radial-gradient(circle at 88% 12%, #eef4ff 0, transparent 40%),
                linear-gradient(160deg, #fbf8ff 0%, #fdf6f0 100%);
    min-height: 100vh;
  }
  .wrap { max-width: 1000px; margin: 0 auto; }
  header { text-align: center; margin-bottom: 22px; }
  h1 { margin: 0 0 8px; font-size: 25px; letter-spacing: .5px; color: #6b4f8f; }
  .sub { font-size: 12.5px; color: #9a89ad; line-height: 1.7; }
  .badge {
    display: inline-block; margin: 0 4px; padding: 3px 10px; border-radius: 999px;
    background: #fff; border: 1px solid #eadcf5; font-size: 11px; color: #8b6bab;
  }
  .main { display: flex; gap: 22px; align-items: flex-start; flex-wrap: wrap; }
  .left { flex: 1 1 420px; min-width: 320px; }
  .right { flex: 1 1 380px; min-width: 300px; }
  .panel {
    background: rgba(255,255,255,.86); border: 1px solid #eee2f5;
    border-radius: 20px; padding: 16px;
    box-shadow: 0 10px 30px rgba(150,106,190,.1);
  }
  .stage {
    position: relative; width: 100%; aspect-ratio: 1 / 1; max-height: 460px;
    border-radius: 20px; overflow: hidden; cursor: grab;
    background: radial-gradient(circle at 50% 34%, #fffdf8 0%, #fdf1e7 52%, #f0e2f7 100%);
    border: 2px solid #fff;
    box-shadow: inset 0 0 0 1px rgba(255,255,255,.9), 0 14px 34px rgba(150,106,190,.16);
  }
  .stage:active { cursor: grabbing; }
  #cv { display: block; width: 100%; height: 100%; touch-action: none; }
  .stage-tag {
    position: absolute; top: 12px; left: 12px; pointer-events: none;
    background: rgba(255,255,255,.8); border: 1px solid #f0e2f7;
    color: #8b6bab; font-size: 10.5px; padding: 4px 10px; border-radius: 999px;
  }
  .tip { text-align: center; font-size: 11.5px; color: #a08fb5; margin: 12px 0 6px; }
  .status {
    text-align: center; font-size: 11px; color: #7f6b98; min-height: 18px;
    background: #f8f2fd; border-radius: 999px; padding: 6px 10px; margin-top: 8px;
  }
  .errmsg { display: none; color: #c0526a; font-size: 12px; margin-top: 10px; }
  .info { padding: 4px 4px 14px; border-bottom: 1px dashed #eadcf5; margin-bottom: 14px; }
  .info h2 { margin: 0 0 6px; font-size: 21px; color: #6b4f8f; }
  .info p { margin: 0; font-size: 12.5px; color: #9a89ad; line-height: 1.6; }
  .sec { margin-bottom: 14px; }
  .sec h3 { margin: 0 0 9px; font-size: 12px; color: #8b76a5; font-weight: 600; }
  .states { display: flex; flex-wrap: wrap; gap: 7px; }
  .stab {
    border: 1.5px solid #eee2f5; background: #fff; color: #7f6b98;
    border-radius: 999px; padding: 6px 12px; font-size: 11.5px; cursor: pointer;
    transition: all .16s;
  }
  .stab:hover { border-color: #d3b8ec; transform: translateY(-1px); }
  .stab.on { background: linear-gradient(135deg,#b98ce0,#e2a6d4); color: #fff; border-color: transparent; }
  .kinds { display: grid; grid-template-columns: repeat(4, 1fr); gap: 7px; }
  .chip {
    border: 1.5px solid #eee2f5; background: #fff; color: #6b5b80;
    border-radius: 12px; padding: 8px 3px; cursor: pointer;
    display: flex; flex-direction: column; align-items: center; gap: 2px;
    transition: all .16s;
  }
  .chip b { font-size: 11px; font-weight: 600; }
  .chip small { font-size: 9px; color: #b3a3c4; }
  .chip:hover { border-color: #d3b8ec; transform: translateY(-2px); }
  .chip.on {
    background: linear-gradient(135deg,#f3e7ff,#ffeaf4);
    border-color: #c9a4e8; color: #6b4f8f;
    box-shadow: 0 4px 12px rgba(160,108,213,.16);
  }
  .toggle {
    width: 100%; border: 1.5px solid #eee2f5; background: #fff; color: #7f6b98;
    border-radius: 12px; padding: 9px; font-size: 12px; cursor: pointer; transition: all .16s;
  }
  .toggle:hover { border-color: #d3b8ec; }
  .toggle.off { background: #f6f2fa; color: #a89bbb; }
  .note {
    margin-top: 14px; font-size: 10.5px; color: #a89bbb; line-height: 1.75;
    background: rgba(255,255,255,.6); border-radius: 12px; padding: 10px 12px;
  }
  .note b { color: #8b6bab; }
  @media (max-width: 720px) { .kinds { grid-template-columns: repeat(3, 1fr); } }
</style>
</head>
<body>
<div class="wrap">
  <header>
    <h1>🐾 星糖谷 · 真 3D 宠物图鉴</h1>
    <div class="sub">
      <span class="badge">WebGL 实时渲染</span>
      <span class="badge">可拖动旋转</span>
      <span class="badge">14 个真实品种</span>
      <span class="badge">部件级动画</span><br>
      头、耳朵、尾巴、翅膀、眼睑都是独立 3D 部件，各自运动 —— 不是图片在晃。
    </div>
  </header>

  <div class="main">
    <div class="left panel">
      <div id="stage" class="stage">
        <canvas id="cv"></canvas>
        <span class="stage-tag">真 3D · 有透视有光影</span>
      </div>
      <div class="tip">🖐️ 按住拖动转一圈 · 滚轮缩放 · 点一下摸摸它</div>
      <div id="status" class="status"></div>
      <div id="errmsg" class="errmsg"></div>
    </div>

    <div class="right panel">
      <div class="info">
        <h2 id="pname">萨摩耶</h2>
        <p id="pdesc"></p>
      </div>
      <div class="sec">
        <h3>动作状态</h3>
        <div id="states" class="states"></div>
      </div>
      <div class="sec">
        <h3>切换品种（14 种）</h3>
        <div id="kinds" class="kinds"></div>
      </div>
      <button id="toggleRotate" class="toggle">🔄 自动转身：开</button>
      <div class="note">
        <b>和上一版的区别：</b>之前是一张 2D 图片整体平移；这次是浏览器实时算出来的
        3D 几何体，有球体体积、前后遮挡、三点柔光和接触阴影，能转到侧面和背面看。
        程序化建模，零外部模型素材、离线可用、不侵权。
      </div>
    </div>
  </div>
</div>
<script>__BUNDLE__</script>
</body>
</html>
'''

html = HTML.replace('__BUNDLE__', js)
io.open(OUT, 'w', encoding='utf-8').write(html)
print('OK', OUT)
print('size: %.1f KB' % (os.path.getsize(OUT) / 1024))
print('placeholders left:', '__BUNDLE__' in html)
