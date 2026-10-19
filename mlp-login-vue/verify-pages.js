/** 验证：dev server 能编译新页面 + 后端能返回新小马 */
const fs = require('fs');
const out = [];

async function check(url, label) {
  try {
    const res = await fetch(url);
    const text = await res.text();
    const bad = text.includes('Internal server error') || text.includes('Transform failed') || text.includes('SyntaxError') || text.includes('error');
    out.push(`${label}: HTTP ${res.status} len=${text.length} ${bad ? '!!! 疑似编译错误' : 'ok'}`);
    if (bad) out.push('  片段: ' + text.slice(0, 300).replace(/\n/g, ' '));
  } catch (e) {
    out.push(`${label}: 请求失败 ${e.message}`);
  }
}

(async () => {
  // dev server 编译检查（Vite 按需编译，出错会返回错误信息）
  await check('http://localhost:5173/', '首页');
  await check('http://localhost:5173/src/views/LearnView.vue', 'LearnView');
  await check('http://localhost:5173/src/views/GameView.vue', 'GameView');
  await check('http://localhost:5173/src/views/FriendsView.vue', 'FriendsView');
  await check('http://localhost:5173/src/views/ChatView.vue', 'ChatView');
  await check('http://localhost:5173/src/components/games/GameDessert.vue', 'GameDessert');
  await check('http://localhost:5173/src/components/games/GameParty.vue', 'GameParty');
  await check('http://localhost:5173/src/components/games/GameStyle.vue', 'GameStyle');

  // 后端图鉴是否含新小马
  try {
    const res = await fetch('http://localhost:8081/api/home/search?keyword=%E7%94%9C%E5%93%81&page=1&size=10');
    const json = await res.json();
    const names = (json.data && json.data.list || []).map(p => p.name);
    out.push('搜索「甜品」: code=' + json.code + ' 命中=' + JSON.stringify(names));
  } catch (e) {
    out.push('搜索接口失败: ' + e.message);
  }

  fs.writeFileSync('C:/Users/18017/Desktop/1java-main/1java-main/mlp-login-vue/verify-result.txt', out.join('\n'), 'utf8');
  console.log('done');
})();
