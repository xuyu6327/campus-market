// 矢量图标组件：与 Web 端 icons.js 一致的线性 SVG 图标（24x24 viewBox，1.6px 描边）
// 渲染方式：CSS mask + SVG data URI（mask 只取 alpha，颜色由 background-color 控制）
const ICONS = {
  'home': '<path d="M3 10.5 12 3l9 7.5"/><path d="M5 9.5V21h14V9.5"/><path d="M9.5 21v-6h5v6"/>',
  'search': '<circle cx="11" cy="11" r="7.5"/><path d="M20.5 20.5 16.4 16.4"/>',
  'add': '<path d="M12 5v14M5 12h14"/>',
  'message': '<path d="M21 14.5a2 2 0 0 1-2 2H7.5L3 20.5V5.5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>',
  'user': '<path d="M20 21v-2.2a4.3 4.3 0 0 0-4.3-4.3H8.3A4.3 4.3 0 0 0 4 18.8V21"/><circle cx="12" cy="7.5" r="4.2"/>',
  'heart': '<path d="M20.7 4.9a5.4 5.4 0 0 0-7.7 0L12 6l-1-1.1a5.4 5.4 0 0 0-7.7 7.7L12 21l8.7-8.4a5.4 5.4 0 0 0 0-7.7z"/>',
  'arrow-left': '<path d="M19 12H5"/><path d="M12 19l-7-7 7-7"/>',
  'eye': '<path d="M1.5 12S5.5 4.5 12 4.5 22.5 12 22.5 12 18.5 19.5 12 19.5 1.5 12 1.5 12z"/><circle cx="12" cy="12" r="3.2"/>',
  'map-pin': '<path d="M20 10.5c0 6.5-8 12.5-8 12.5S4 17 4 10.5a8 8 0 1 1 16 0z"/><circle cx="12" cy="10.5" r="2.8"/>',
  'clock': '<circle cx="12" cy="12" r="9.5"/><path d="M12 6.5V12l3.5 2"/>',
  'grid': '<rect x="3.5" y="3.5" width="7" height="7" rx="1.5"/><rect x="13.5" y="3.5" width="7" height="7" rx="1.5"/><rect x="3.5" y="13.5" width="7" height="7" rx="1.5"/><rect x="13.5" y="13.5" width="7" height="7" rx="1.5"/>',
  'edit': '<path d="M16.5 3.5a2.6 2.6 0 0 1 3.7 3.7L7.5 19.9 2.5 21.5l1.6-5z"/>',
  'phone': '<path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6A19.79 19.79 0 0 1 2.12 4.18 2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.13.96.36 1.9.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.91.34 1.85.57 2.81.7A2 2 0 0 1 22 16.92z"/>',
  'chat': '<path d="M4 4h16a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H8l-5 4V6a2 2 0 0 1 1-2z"/><path d="M8 10h.01M12 10h.01M16 10h.01"/>',
  'image': '<rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="8.5" cy="9.5" r="1.5"/><path d="m21 15-4.5-4.5L8 19"/>',
  'book': '<path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>',
  'refresh': '<path d="M21 12a9 9 0 1 1-2.64-6.36L21 8"/><path d="M21 3v5h-5"/>',
  'close': '<path d="M18 6 6 18M6 6l12 12"/>',
  'chevron-left': '<path d="M15 18l-6-6 6-6"/>',
  'chevron-right': '<path d="M9 18l6-6-6-6"/>',
  'camera': '<path d="M3 7.5a2 2 0 0 1 2-2h2l1.5-2h7L17 5.5h2a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><circle cx="12" cy="13" r="3.8"/>',
  'flag': '<path d="M5 21V4"/><path d="M5 4h12l-2.5 4L17 12H5"/>',
  'bell': '<path d="M18 9.5a6 6 0 1 0-12 0c0 6-2.5 7-2.5 7h17S18 15.5 18 9.5z"/><path d="M10.5 20a1.8 1.8 0 0 0 3 0"/>',
  'star': '<path d="m12 3 2.6 5.6 6 .8-4.4 4.2 1.1 6L12 16.9 6.7 19.6l1.1-6L3.4 9.4l6-.8z"/>',
  'box': '<path d="M21 8 12 3 3 8v8l9 5 9-5z"/><path d="M3 8l9 5 9-5"/><path d="M12 13v8"/>',
  'shield': '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>',
  'settings': '<circle cx="12" cy="12" r="3.2"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33h.01a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51h.01a1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82v.01a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>',
  'chart': '<path d="M3 3v18h18"/><path d="M8 17V9M13 17V5M18 17v-7"/>',
  'doc': '<path d="M14 2.5H6.5a2 2 0 0 0-2 2v15a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2V8.5z"/><path d="M14 2.5V8.5h6"/><path d="M9 13h6M9 17h4"/>',
  'more': '<circle cx="5" cy="12" r="1.5" fill="black" stroke="none"/><circle cx="12" cy="12" r="1.5" fill="black" stroke="none"/><circle cx="19" cy="12" r="1.5" fill="black" stroke="none"/>',
  'smile': '<circle cx="12" cy="12" r="9.5"/><path d="M8.5 14.5a4 4 0 0 0 7 0"/><path d="M9 9.5h.01M15 9.5h.01"/>',
  'money': '<rect x="2.5" y="6" width="19" height="12" rx="2"/><path d="M12 10v4M9 12h6"/><circle cx="12" cy="12" r="1.2" fill="black" stroke="none"/>',
  'building': '<rect x="5" y="3.5" width="14" height="17" rx="1.5"/><path d="M9 7.5h.01M15 7.5h.01M9 11.5h.01M15 11.5h.01M9 15.5h.01M15 15.5h.01"/>'
};

Component({
  properties: {
    // 图标名（见上方 ICONS 键）
    name: { type: String, value: '' },
    // 图标颜色（默认主色）
    color: { type: String, value: '#FF6B35' },
    // 尺寸（rpx）
    size: { type: Number, value: 44 }
  },
  data: {
    mask: ''
  },
  observers: {
    name() {
      this.updateMask();
    }
  },
  lifetimes: {
    attached() {
      this.updateMask();
    }
  },
  methods: {
    updateMask() {
      const paths = ICONS[this.data.name] || '';
      const svg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='black' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'>" + paths + "</svg>";
      this.setData({ mask: 'url("data:image/svg+xml,' + encodeURIComponent(svg) + '")' });
    }
  }
});
