import yaml

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'r', encoding='utf-8') as f:
    market = yaml.safe_load(f)

# Revert mobdrops and farming to original trade_amounts and prices
original_drops = {
    "ROTTEN_FLESH": (32, 3000, 75),
    "BONE": (32, 6750, 230),
    "STRING": (32, 6750, 230),
    "SPIDER_EYE": (16, 5000, 170),
    "GUNPOWDER": (16, 14000, 475),
    "ENDER_PEARL": (16, 24000, 700),
    "SLIME_BALL": (16, 15000, 500),
    "BLAZE_ROD": (16, 19500, 600),
    "GHAST_TEAR": (8, 32000, 925),
    "PHANTOM_MEMBRANE": (8, 22000, 700),
    "INK_SAC": (16, 3000, 150),
    "GLOW_INK_SAC": (16, 4500, 250),
    "RABBIT_HIDE": (16, 2000, 100),
    "RABBIT_FOOT": (16, 6000, 250),
    "TURTLE_SCUTE": (8, 15000, 500),
    "WITHER_SKELETON_SKULL": (1, 50000, 5000),
    "FEATHER": (32, 2000, 75),
    "LEATHER": (32, 4000, 125),
    "MAGMA_CREAM": (16, 15000, 500),
    "SHULKER_SHELL": (16, 45000, 2500)
}

original_farms = {
    "WHEAT": (16, 2500, 200),
    "CARROT": (16, 2500, 200),
    "POTATO": (16, 2500, 200),
    "BEETROOT": (16, 2500, 200),
    "SUGAR_CANE": (16, 3000, 250),
    "PUMPKIN": (8, 3500, 125),
    "MELON_SLICE": (8, 800, 45),
    "BAMBOO": (32, 1000, 200),
    "COCOA_BEANS": (16, 4000, 300),
    "NETHER_WART": (16, 5000, 350),
    "APPLE": (16, 2000, 150),
    "SWEET_BERRIES": (16, 1500, 120),
    "GLOW_BERRIES": (16, 2500, 180),
    "CHORUS_FRUIT": (16, 4000, 300),
    "HONEYCOMB": (16, 6000, 450),
    "HONEY_BLOCK": (8, 8000, 600),
    "KELP": (32, 1500, 100)
}

items = market.get('items', {})
for k, v in items.items():
    mat = v.get('material', '')
    if mat in original_drops:
        trade, buy, sell = original_drops[mat]
        v['trade-amount'] = trade
        if v['buy']['enabled']: v['buy']['price'] = buy
        if v['sell']['enabled']: v['sell']['price'] = sell
    elif mat in original_farms:
        trade, buy, sell = original_farms[mat]
        v['trade-amount'] = trade
        if v['buy']['enabled']: v['buy']['price'] = buy
        if v['sell']['enabled']: v['sell']['price'] = sell

class Dumper(yaml.Dumper):
    def increase_indent(self, flow=False, indentless=False):
        return super(Dumper, self).increase_indent(flow, False)

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'w', encoding='utf-8') as f:
    yaml.dump(market, f, default_flow_style=False, sort_keys=False, Dumper=Dumper)
print("Reverted mobdrops and farming to original amounts/prices.")
