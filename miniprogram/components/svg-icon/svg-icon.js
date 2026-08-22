// 矢量图标组件：与 Web 端 icons.js 一致的线性 SVG 图标（24x24 viewBox，1.6px 描边）
// 渲染方式：<image> 加载小程序包内本地 SVG 文件（icons/{name}-{COLOR}.svg，颜色按使用场景预生成）
// 用法：<svg-icon name="heart" size="44" color="#FF6B35"/>
// 说明：WXSS url() 不支持本地文件且 mask/base64 在部分环境不渲染，image 组件加载本地 svg 为最稳方案；
//       新增 name+color 组合需在 icons/ 目录生成对应文件（参考项目根 miniprogram 交付说明）
const ICON_NAMES = [
  'home', 'search', 'add', 'message', 'user', 'heart', 'arrow-left', 'eye',
  'map-pin', 'clock', 'grid', 'edit', 'phone', 'chat', 'image', 'book',
  'refresh', 'close', 'chevron-left', 'chevron-right', 'camera', 'flag', 'bell',
  'star', 'box', 'shield', 'settings', 'chart', 'doc', 'more', 'smile', 'money', 'building'
];

function colorKey(c) {
  return (c || '#FF6B35').replace('#', '').toUpperCase();
}

Component({
  properties: {
    // 图标名（对应 icons/{name}-{COLOR}.svg）
    name: { type: String, value: '' },
    // 图标颜色（需存在 icons/{name}-{COLOR}.svg 文件，见生成脚本）
    color: { type: String, value: '#FF6B35' },
    // 尺寸（rpx）
    size: { type: Number, value: 44 }
  },

  data: {
    src: ''
  },

  observers: {
    'name, color': function () {
      this.setData({ src: this.buildSrc() });
    }
  },

  lifetimes: {
    attached() {
      this.setData({ src: this.buildSrc() });
    }
  },

  methods: {
    buildSrc() {
      const n = this.data.name;
      if (ICON_NAMES.indexOf(n) < 0) return '';
      return '/components/svg-icon/icons/' + n + '-' + colorKey(this.data.color) + '.png';
    }
  }
});
