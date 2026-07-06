import yaml

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'r', encoding='utf-8') as f:
    market = yaml.safe_load(f)

glitches = []
for k, v in market.get('items', {}).items():
    b = v.get('buy', {})
    s = v.get('sell', {})
    if b.get('enabled') and s.get('enabled'):
        bp = b.get('price', 0)
        sp = s.get('price', 0)
        if bp <= sp:
            glitches.append(f"{k} (Buy: {bp}, Sell: {sp})")

if glitches:
    print("WARNING - INFINITE MONEY GLITCHES FOUND:")
    for g in glitches:
        print(g)
else:
    print("ECONOMY SAFE - Buy prices are higher than sell prices.")
