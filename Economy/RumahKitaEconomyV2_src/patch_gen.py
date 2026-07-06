import re

file_path = 'd:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2_src/gen_market.py'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace the trade_amount usage inside add_item
old_code = '''    items[material.lower()] = {
        "material": material,
        "display-name": "&f" + display_name,
        "trade-amount": trade_amount,
        "category": category,
        "buy": {
            "enabled": True if buy_price > 0 else False,
            "price": buy_price
        },
        "sell": {
            "enabled": True if sell_price > 0 else False,
            "price": sell_price
        }
    }'''

new_code = '''    items[material.lower()] = {
        "material": material,
        "display-name": "&f" + display_name,
        "trade-amount": 1,
        "category": category,
        "buy": {
            "enabled": True if buy_price > 0 else False,
            "price": int(round(buy_price / trade_amount)) if buy_price > 0 else 0
        },
        "sell": {
            "enabled": True if sell_price > 0 else False,
            "price": int(round(sell_price / trade_amount)) if sell_price > 0 else 0
        }
    }'''

if old_code in content:
    content = content.replace(old_code, new_code)
elif 'items[material.lower()] = {' in content:
    # Use regex if exactly not matching
    content = re.sub(r'\"trade-amount\": [a-zA-Z0-9_]+,', '\"trade-amount\": 1,', content)
    content = re.sub(r'\"price\": buy_price', '\"price\": int(round(buy_price / trade_amount)) if buy_price > 0 else 0', content)
    content = re.sub(r'\"price\": sell_price', '\"price\": int(round(sell_price / trade_amount)) if sell_price > 0 else 0', content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print('Updated gen_market.py')
