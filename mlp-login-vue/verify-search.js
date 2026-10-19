/** 验证甜品/甜点搜索 + 分类树概览 */
const fs = require('fs');
(async () => {
  const out = [];
  for (const kw of ['甜品', '甜点', '时尚', '纸杯蛋糕', '糖果', '派对']) {
    try {
      const res = await fetch('http://localhost:8081/api/home/search?keyword=' + encodeURIComponent(kw) + '&page=1&size=12');
      const j = await res.json();
      const names = ((j.data && j.data.list) || []).map(p => p.name);
      out.push(`「${kw}」 total=${j.data.total} 命中=${JSON.stringify(names)}`);
    } catch (e) {
      out.push(`「${kw}」失败: ${e.message}`);
    }
  }
  // 概览：分类树 + 统计
  try {
    const res = await fetch('http://localhost:8081/api/home/overview');
    const j = await res.json();
    const d = j.data || {};
    out.push('overview: code=' + j.code + ' 一级分类=' + (d.categoryTree || []).length + ' 统计=' + JSON.stringify(d.stats));
  } catch (e) {
    out.push('overview 失败: ' + e.message);
  }
  fs.writeFileSync('C:/Users/18017/Desktop/1java-main/1java-main/mlp-login-vue/verify-result.txt', out.join('\n'), 'utf8');
  console.log('done');
})();
