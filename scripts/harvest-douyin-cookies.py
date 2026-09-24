# -*- coding: utf-8 -*-
"""抖音 cookie 过期时运行本脚本重新采集：真实 Edge 访问抖音首页，导出 cookies.txt 供 yt-dlp 使用。
用法：python scripts/harvest-douyin-cookies.py  （需 playwright 已安装，会弹出一个浏览器窗口约 15 秒）"""
import os
from playwright.sync_api import sync_playwright

UA = ('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) '
      'Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0')
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'config', 'douyin-cookies.txt')

with sync_playwright() as p:
    browser = p.chromium.launch(channel='msedge', headless=False,
                                args=['--disable-blink-features=AutomationControlled'])
    ctx = browser.new_context(user_agent=UA, viewport={'width': 1380, 'height': 860})
    ctx.add_init_script("Object.defineProperty(navigator,'webdriver',{get:()=>undefined})")
    page = ctx.new_page()
    page.goto('https://www.douyin.com/', wait_until='domcontentloaded', timeout=60000)
    page.wait_for_timeout(9000)
    cookies = ctx.cookies('https://www.douyin.com')
    browser.close()

lines = ['# Netscape HTTP Cookie File']
for c in cookies:
    dom = c['domain']
    line = f"{dom}\t{'TRUE' if dom.startswith('.') else 'FALSE'}\t{c['path']}\t{'TRUE' if c['secure'] else 'FALSE'}\t{int(c['expires'])}\t{c['name']}\t{c['value']}"
    lines.append(('#HttpOnly_' if c.get('httpOnly') else '') + line)
os.makedirs(os.path.dirname(OUT), exist_ok=True)
open(OUT, 'w', encoding='utf-8').write('\n'.join(lines) + '\n')
print(f'已写入 {len(cookies)} 条 cookie -> {OUT}')
