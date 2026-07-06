import re

file_path = 'd:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2_src/gen_market.py'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# I will just write a new add_item function using regex replacement for the entire function
new_func = '''def add_item(category, material, display_name, trade_amount, buy_price, sell_price):
    global counter
    
    b_p = int(round(buy_price / trade_amount)) if buy_price > 0 else 0
    s_p = int(round(sell_price / trade_amount)) if sell_price > 0 else 0
    
    # Avoid price 0 if original price was small
    if buy_price > 0 and b_p == 0: b_p = 1
    if sell_price > 0 and s_p == 0: s_p = 1
    
    items[material.lower()] = {
        "material": material,
        "display-name": "&f" + display_name,
        "trade-amount": 1,
        "category": category,
        "buy": {
            "enabled": True if buy_price > 0 else False,
            "price": b_p
        },
        "sell": {
            "enabled": True if sell_price > 0 else False,
            "price": s_p
        }
    }'''

content = re.sub(r'def add_item\(.*?\):\n.*?sell_price\n        \}\n    \}', new_func, content, flags=re.DOTALL)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print('Patched add_item')
