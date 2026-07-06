import yaml

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2_src/gen_market.py', 'r', encoding='utf-8') as f:
    for line in f.readlines():
        if '("ROTTEN_FLESH"' in line or '("BONE"' in line or '("GUNPOWDER"' in line or '("BLAZE_ROD"' in line:
            print(line.strip())
