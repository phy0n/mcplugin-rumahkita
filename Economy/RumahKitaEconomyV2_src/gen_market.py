import yaml

market = {
    "version": 24,
    "gui": {
        "main": {
            "title": "&8Market Categories",
            "size": 54,
            "fill": "GRAY_STAINED_GLASS_PANE",
            "slots": {
                "voucher": 48,
                "info": 49,
                "balance": 50,
                "admin": 53
            }
        }
    },
    "categories": {
        "blocks": {
            "display-name": "&bBuilding Blocks",
            "material": "GRASS_BLOCK",
            "lore": ["&7Semua jenis block (Batu, Kayu, Tanah, Nether).", "&7Hanya bisa dibeli (Buy Only)."],
            "slot": 11
        },
        "blocks_color": {
            "display-name": "&dColors & Decor",
            "material": "RED_DYE",
            "lore": ["&7Wool, Concrete, Terracotta, Glass, Dye.", "&7Hanya bisa dibeli (Buy Only)."],
            "slot": 13
        },
        "nature": {
            "display-name": "&aNature",
            "material": "OAK_SAPLING",
            "lore": ["&7Sapling, Daun, Bunga, dan Tanaman lainnya.", "&7Hanya bisa dibeli (Buy Only)."],
            "slot": 15
        },
        "furniture": {
            "display-name": "&fFurniture & Functional",
            "material": "CRAFTING_TABLE",
            "lore": ["&7Meja, lampu, kasur, alat.", "&7Hanya bisa dibeli (Buy Only)."],
            "slot": 20
        },
        "redstone": {
            "display-name": "&4Redstone & Mechanism",
            "material": "REDSTONE",
            "lore": ["&7Komponen redstone dan mekanik.", "&7Hanya bisa dibeli (Buy Only)."],
            "slot": 22
        },
        "food": {
            "display-name": "&6Food & Consumables",
            "material": "BREAD",
            "lore": ["&7Berbagai macam makanan.", "&7Hanya bisa dibeli (Buy Only)."],
            "slot": 24
        },
        "mobdrops": {
            "display-name": "&cMob Drops",
            "material": "ROTTEN_FLESH",
            "lore": ["&7Jual hasil mob farm kamu di sini.", "&aBisa Dibeli & Dijual."],
            "slot": 29
        },
        "farming": {
            "display-name": "&eFarming",
            "material": "WHEAT",
            "lore": ["&7Jual hasil ladang kamu di sini.", "&aBisa Dibeli & Dijual."],
            "slot": 31
        },
        "misc": {
            "display-name": "&5Miscellaneous",
            "material": "NAME_TAG",
            "lore": ["&7Berbagai item serbaguna dan langka.", "&7Hanya bisa dibeli (Buy Only)."],
            "slot": 33
        }
    },
    "items": {}
}

items = {}

def add_item(cat, mat, name, trade, buy, sell):
    buy_p = int(round(buy / trade)) if buy > 0 else 0
    sell_p = int(round(sell / trade)) if sell > 0 else 0
    
    if buy > 0 and buy_p == 0: buy_p = 1
    if sell > 0 and sell_p == 0: sell_p = 1
    
    items[mat.lower()] = {
        "material": mat.upper(),
        "display-name": f"&f{name}",
        "trade-amount": 1,
        "category": cat,
        "buy": {
            "enabled": True if buy > 0 else False,
            "price": buy_p
        },
        "sell": {
            "enabled": True if sell > 0 else False,
            "price": sell_p,
            "daily-limit-per-player": 15000 if sell > 0 else 0
        },
        "pricing": {
            "mode": "DEMAND",
            "demand-enabled": True,
            "min-buy-price": max(0, buy_p // 2),
            "max-buy-price": buy_p * 2,
            "min-sell-price": max(0, sell_p // 2),
            "max-sell-price": sell_p * 2,
            "max-change-percent-per-update": 2.0,
            "max-change-percent-per-day": 10.0,
            "admin-locked-price": False
        },
        "stock": {
            "enabled": False,
            "current": -1,
            "max": -1,
            "restock-amount": 0,
            "restock-interval-minutes": 0
        }
    }

# MASSIVE BUILDING BLOCKS GENERATION

# 1. Dirt & Natural Grounds
dirts = [
    ("GRASS_BLOCK", "Grass Block", 150),
    ("DIRT", "Dirt", 75),
    ("COARSE_DIRT", "Coarse Dirt", 75),
    ("ROOTED_DIRT", "Rooted Dirt", 100),
    ("MUD", "Mud", 100),
    ("PODZOL", "Podzol", 150),
    ("MYCELIUM", "Mycelium", 150),
    ("CRIMSON_NYLIUM", "Crimson Nylium", 150),
    ("WARPED_NYLIUM", "Warped Nylium", 150),
    ("DIRT_PATH", "Dirt Path", 100),
    ("SAND", "Sand", 75),
    ("RED_SAND", "Red Sand", 75),
    ("GRAVEL", "Gravel", 75),
    ("GLASS", "Glass", 100),
    ("GLASS_PANE", "Glass Pane", 40),
    ("ICE", "Ice", 150),
    ("PACKED_ICE", "Packed Ice", 300),
    ("BLUE_ICE", "Blue Ice", 600),
    ("SNOW_BLOCK", "Snow Block", 75),
    ("OBSIDIAN", "Obsidian", 400),
    ("CRYING_OBSIDIAN", "Crying Obsidian", 500),
    ("GLOWSTONE", "Glowstone", 250),
    ("SHROOMLIGHT", "Shroomlight", 300),
    ("SEA_LANTERN", "Sea Lantern", 400),
    ("HAY_BLOCK", "Hay Block", 200),
    ("MAGMA_BLOCK", "Magma Block", 200),
    ("BONE_BLOCK", "Bone Block", 300),
    ("DRIED_KELP_BLOCK", "Dried Kelp Block", 250),
    ("BEE_NEST", "Bee Nest", 500),
    ("BEEHIVE", "Beehive", 300),
    ("HONEYCOMB_BLOCK", "Honeycomb Block", 300),
    ("SCULK", "Sculk", 250),
    ("AMETHYST_BLOCK", "Amethyst Block", 300),
    ("CALCITE", "Calcite", 150),
    ("DRIPSTONE_BLOCK", "Dripstone Block", 150),
    ("TUFF", "Tuff", 100),
    ("SPONGE", "Sponge", 1000)
]
for mat, name, buy_p in dirts:
    add_item("blocks", mat, name, 32, buy_p, -1)

# 2. Stones families (Base, Stairs, Slab, Wall, Chiseled, Polished, Bricks)
# Format: (Prefix, Name, BasePrice, has_stairs, has_slab, has_wall)
stone_families = [
    ("STONE", "Stone", 100, True, True, False),
    ("COBBLESTONE", "Cobblestone", 100, True, True, True),
    ("STONE_BRICKS", "Stone Bricks", 125, True, True, True),
    ("MOSSY_COBBLESTONE", "Mossy Cobblestone", 125, True, True, True),
    ("MOSSY_STONE_BRICKS", "Mossy Stone Bricks", 150, True, True, True),
    ("GRANITE", "Granite", 100, True, True, True),
    ("POLISHED_GRANITE", "Polished Granite", 125, True, True, False),
    ("DIORITE", "Diorite", 100, True, True, True),
    ("POLISHED_DIORITE", "Polished Diorite", 125, True, True, False),
    ("ANDESITE", "Andesite", 100, True, True, True),
    ("POLISHED_ANDESITE", "Polished Andesite", 125, True, True, False),
    ("DEEPSLATE", "Deepslate", 150, False, False, False),
    ("COBBLED_DEEPSLATE", "Cobbled Deepslate", 150, True, True, True),
    ("POLISHED_DEEPSLATE", "Polished Deepslate", 175, True, True, True),
    ("DEEPSLATE_BRICKS", "Deepslate Bricks", 200, True, True, True),
    ("DEEPSLATE_TILES", "Deepslate Tiles", 200, True, True, True),
    ("TUFF", "Tuff", 100, True, True, True),
    ("POLISHED_TUFF", "Polished Tuff", 125, True, True, True),
    ("TUFF_BRICKS", "Tuff Bricks", 150, True, True, True),
    ("SANDSTONE", "Sandstone", 100, True, True, True),
    ("SMOOTH_SANDSTONE", "Smooth Sandstone", 125, True, True, False),
    ("CUT_SANDSTONE", "Cut Sandstone", 125, False, True, False),
    ("RED_SANDSTONE", "Red Sandstone", 100, True, True, True),
    ("SMOOTH_RED_SANDSTONE", "Smooth Red Sandstone", 125, True, True, False),
    ("CUT_RED_SANDSTONE", "Cut Red Sandstone", 125, False, True, False),
    ("PRISMARINE", "Prismarine", 200, True, True, True),
    ("PRISMARINE_BRICKS", "Prismarine Bricks", 250, True, True, False),
    ("DARK_PRISMARINE", "Dark Prismarine", 250, True, True, False),
    ("NETHER_BRICKS", "Nether Bricks", 150, True, True, True),
    ("RED_NETHER_BRICKS", "Red Nether Bricks", 200, True, True, True),
    ("QUARTZ_BLOCK", "Quartz Block", 250, False, False, False), # Quartz stairs/slabs use just QUARTZ_
    ("SMOOTH_QUARTZ", "Smooth Quartz", 250, True, True, False),
    ("PURPUR_BLOCK", "Purpur Block", 250, False, False, False), # Purpur stairs/slabs use just PURPUR_
    ("END_STONE", "End Stone", 200, False, False, False),
    ("END_STONE_BRICKS", "End Stone Bricks", 250, True, True, True),
    ("BLACKSTONE", "Blackstone", 150, True, True, True),
    ("POLISHED_BLACKSTONE", "Polished Blackstone", 175, True, True, True),
    ("POLISHED_BLACKSTONE_BRICKS", "Polished Blackstone Bricks", 200, True, True, True),
    ("MUD_BRICKS", "Mud Bricks", 125, True, True, True),
    ("BRICKS", "Bricks", 150, True, True, True)
]
for pfx, name, price, hs, hsl, hw in stone_families:
    # Handle naming quirks
    base_mat = pfx
    st_mat = f"{pfx}_STAIRS"
    sl_mat = f"{pfx}_SLAB"
    wl_mat = f"{pfx}_WALL"
    
    if pfx == "QUARTZ_BLOCK":
        st_mat = "QUARTZ_STAIRS"
        sl_mat = "QUARTZ_SLAB"
        hs, hsl = True, True
    elif pfx == "PURPUR_BLOCK":
        st_mat = "PURPUR_STAIRS"
        sl_mat = "PURPUR_SLAB"
        hs, hsl = True, True
    elif pfx == "BRICKS":
        st_mat = "BRICK_STAIRS"
        sl_mat = "BRICK_SLAB"
        wl_mat = "BRICK_WALL"
    elif pfx == "PRISMARINE_BRICKS":
        sl_mat = "PRISMARINE_BRICK_SLAB"
        st_mat = "PRISMARINE_BRICK_STAIRS"
    
    add_item("blocks", base_mat, name, 32, price, -1)
    if hs: add_item("blocks", st_mat, f"{name} Stairs", 32, price, -1)
    if hsl: add_item("blocks", sl_mat, f"{name} Slab", 32, int(price/2), -1)
    if hw: add_item("blocks", wl_mat, f"{name} Wall", 32, price, -1)

# Add special chiseled / pillars
specials = [
    ("CHISELED_STONE_BRICKS", "Chiseled Stone Bricks", 150),
    ("CHISELED_DEEPSLATE", "Chiseled Deepslate", 200),
    ("CHISELED_TUFF", "Chiseled Tuff", 150),
    ("CHISELED_SANDSTONE", "Chiseled Sandstone", 125),
    ("CHISELED_RED_SANDSTONE", "Chiseled Red Sandstone", 125),
    ("CHISELED_QUARTZ_BLOCK", "Chiseled Quartz Block", 250),
    ("QUARTZ_PILLAR", "Quartz Pillar", 250),
    ("QUARTZ_BRICKS", "Quartz Bricks", 250),
    ("PURPUR_PILLAR", "Purpur Pillar", 250),
    ("CHISELED_NETHER_BRICKS", "Chiseled Nether Bricks", 175),
    ("CRACKED_NETHER_BRICKS", "Cracked Nether Bricks", 175),
    ("CHISELED_POLISHED_BLACKSTONE", "Chiseled Polished Blackstone", 200),
    ("CRACKED_POLISHED_BLACKSTONE_BRICKS", "Cracked Polished Blackstone Bricks", 200),
    ("CRACKED_STONE_BRICKS", "Cracked Stone Bricks", 150),
    ("CRACKED_DEEPSLATE_BRICKS", "Cracked Deepslate Bricks", 200),
    ("CRACKED_DEEPSLATE_TILES", "Cracked Deepslate Tiles", 200)
]
for mat, name, p in specials:
    add_item("blocks", mat, name, 32, p, -1)

# 3. Woods
woods = [
    ("OAK", "Oak", 75), ("SPRUCE", "Spruce", 75), ("BIRCH", "Birch", 75), 
    ("JUNGLE", "Jungle", 75), ("ACACIA", "Acacia", 75), ("DARK_OAK", "Dark Oak", 75), 
    ("MANGROVE", "Mangrove", 75), ("CHERRY", "Cherry", 75), 
    ("BAMBOO", "Bamboo", 75), ("CRIMSON", "Crimson", 100), ("WARPED", "Warped", 100)
]
for w, name, price in woods:
    is_nether = w in ["CRIMSON", "WARPED"]
    is_bamboo = w == "BAMBOO"
    
    if is_bamboo:
        add_item("blocks", "BAMBOO_BLOCK", "Bamboo Block", 16, price, -1)
        add_item("blocks", "STRIPPED_BAMBOO_BLOCK", "Stripped Bamboo Block", 16, price+10, -1)
        add_item("blocks", "BAMBOO_PLANKS", "Bamboo Planks", 32, price, -1)
        add_item("blocks", "BAMBOO_MOSAIC", "Bamboo Mosaic", 32, price, -1)
        add_item("blocks", "BAMBOO_STAIRS", "Bamboo Stairs", 32, price, -1)
        add_item("blocks", "BAMBOO_MOSAIC_STAIRS", "Bamboo Mosaic Stairs", 32, price, -1)
        add_item("blocks", "BAMBOO_SLAB", "Bamboo Slab", 32, int(price/2), -1)
        add_item("blocks", "BAMBOO_MOSAIC_SLAB", "Bamboo Mosaic Slab", 32, int(price/2), -1)
        add_item("blocks", "BAMBOO_FENCE", "Bamboo Fence", 32, price, -1)
        add_item("blocks", "BAMBOO_FENCE_GATE", "Bamboo Fence Gate", 16, price, -1)
        add_item("blocks", "BAMBOO_DOOR", "Bamboo Door", 16, price, -1)
        add_item("blocks", "BAMBOO_TRAPDOOR", "Bamboo Trapdoor", 16, price, -1)
    else:
        log_mat = f"{w}_STEM" if is_nether else f"{w}_LOG"
        wood_mat = f"{w}_HYPHAE" if is_nether else f"{w}_WOOD"
        strip_log = f"STRIPPED_{w}_STEM" if is_nether else f"STRIPPED_{w}_LOG"
        strip_wood = f"STRIPPED_{w}_HYPHAE" if is_nether else f"STRIPPED_{w}_WOOD"
        
        add_item("blocks", log_mat, f"{name} Log" if not is_nether else f"{name} Stem", 16, price, -1)
        add_item("blocks", wood_mat, f"{name} Wood" if not is_nether else f"{name} Hyphae", 16, price+10, -1)
        add_item("blocks", strip_log, f"Stripped {name} Log" if not is_nether else f"Stripped {name} Stem", 16, price+10, -1)
        add_item("blocks", strip_wood, f"Stripped {name} Wood" if not is_nether else f"Stripped {name} Hyphae", 16, price+15, -1)
        
        add_item("blocks", f"{w}_PLANKS", f"{name} Planks", 32, price, -1)
        add_item("blocks", f"{w}_STAIRS", f"{name} Stairs", 32, price, -1)
        add_item("blocks", f"{w}_SLAB", f"{name} Slab", 32, int(price/2), -1)
        add_item("blocks", f"{w}_FENCE", f"{name} Fence", 32, price, -1)
        add_item("blocks", f"{w}_FENCE_GATE", f"{name} Fence Gate", 16, price, -1)
        add_item("blocks", f"{w}_DOOR", f"{name} Door", 16, price, -1)
        add_item("blocks", f"{w}_TRAPDOOR", f"{name} Trapdoor", 16, price, -1)

# 4. Copper
coppers = [
    ("COPPER", "Copper", 300),
    ("EXPOSED_COPPER", "Exposed Copper", 300),
    ("WEATHERED_COPPER", "Weathered Copper", 300),
    ("OXIDIZED_COPPER", "Oxidized Copper", 300)
]
for pfx, name, price in coppers:
    add_item("blocks", f"{pfx}_BLOCK" if pfx=="COPPER" else pfx, f"{name} Block", 32, price, -1)
    add_item("blocks", f"CUT_{pfx}", f"Cut {name}", 32, price, -1)
    add_item("blocks", f"CUT_{pfx}_STAIRS", f"Cut {name} Stairs", 32, price, -1)
    add_item("blocks", f"CUT_{pfx}_SLAB", f"Cut {name} Slab", 32, int(price/2), -1)
    add_item("blocks", f"CHISELED_{pfx}", f"Chiseled {name}", 32, price, -1)
    add_item("blocks", f"{pfx}_GRATE", f"{name} Grate", 32, price, -1)
    add_item("blocks", f"{pfx}_DOOR", f"{name} Door", 16, price, -1)
    add_item("blocks", f"{pfx}_TRAPDOOR", f"{name} Trapdoor", 16, price, -1)
    # Copper bulbs are already in redstone, but we'll add them to blocks_color or redstone. They're in redstone below.

# 5. Colors & Decor (Concrete, Wool, Terracotta, Glazed Terracotta, Stained Glass, Carpet, Bed, Dyes)
colors = ["WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY", "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"]
block_types = [
    ("_CONCRETE", " Concrete", 32, 112),
    ("_WOOL", " Wool", 32, 100),
    ("_TERRACOTTA", " Terracotta", 32, 100),
    ("_GLAZED_TERRACOTTA", " Glazed Terracotta", 32, 125),
    ("_STAINED_GLASS", " Stained Glass", 32, 150),
    ("_STAINED_GLASS_PANE", " Stained Glass Pane", 32, 60),
    ("_CARPET", " Carpet", 32, 50),
    ("_BED", " Bed", 1, 150)
]
for suffix, name_suffix, trade_amt, price in block_types:
    for c in colors:
        base = c.replace('_',' ').title()
        add_item("blocks_color", f"{c}{suffix}", f"{base}{name_suffix}", trade_amt, price, -1)

for c in colors:
    base = c.replace('_',' ').title()
    add_item("blocks_color", f"{c}_DYE", f"{base} Dye", 16, 150, -1)


# 6. Nature (Plants & Flowers)
plants = [
    ("OAK_SAPLING", "Oak Sapling"), ("SPRUCE_SAPLING", "Spruce Sapling"), ("BIRCH_SAPLING", "Birch Sapling"),
    ("JUNGLE_SAPLING", "Jungle Sapling"), ("ACACIA_SAPLING", "Acacia Sapling"), ("DARK_OAK_SAPLING", "Dark Oak Sapling"),
    ("MANGROVE_PROPAGULE", "Mangrove Propagule"), ("CHERRY_SAPLING", "Cherry Sapling"),
    ("OAK_LEAVES", "Oak Leaves"), ("SPRUCE_LEAVES", "Spruce Leaves"), ("BIRCH_LEAVES", "Birch Leaves"),
    ("JUNGLE_LEAVES", "Jungle Leaves"), ("ACACIA_LEAVES", "Acacia Leaves"), ("DARK_OAK_LEAVES", "Dark Oak Leaves"),
    ("MANGROVE_LEAVES", "Mangrove Leaves"), ("CHERRY_LEAVES", "Cherry Leaves"), ("AZALEA_LEAVES", "Azalea Leaves"), ("FLOWERING_AZALEA_LEAVES", "Flowering Azalea Leaves"),
    ("AZALEA", "Azalea"), ("FLOWERING_AZALEA", "Flowering Azalea"),
    ("FERN", "Fern"), ("LARGE_FERN", "Large Fern"), ("SHORT_GRASS", "Short Grass"), ("TALL_GRASS", "Tall Grass"),
    ("DEAD_BUSH", "Dead Bush"), ("HANGING_ROOTS", "Hanging Roots"), ("BIG_DRIPLEAF", "Big Dripleaf"),
    ("SMALL_DRIPLEAF", "Small Dripleaf"), ("SPORE_BLOSSOM", "Spore Blossom"),
    ("WEEPING_VINES", "Weeping Vines"), ("TWISTING_VINES", "Twisting Vines"), ("NETHER_SPROUTS", "Nether Sprouts"),
    ("CRIMSON_ROOTS", "Crimson Roots"), ("WARPED_ROOTS", "Warped Roots"), ("CRIMSON_FUNGUS", "Crimson Fungus"), ("WARPED_FUNGUS", "Warped Fungus"),
    ("LILY_PAD", "Lily Pad"), ("VINE", "Vine"), ("GLOW_LICHEN", "Glow Lichen")
]
for mat, name in plants:
    add_item("nature", mat, name, 32, 75, -1)

flowers = [
    ("DANDELION", "Dandelion"), ("POPPY", "Poppy"), ("BLUE_ORCHID", "Blue Orchid"),
    ("ALLIUM", "Allium"), ("AZURE_BLUET", "Azure Bluet"), ("RED_TULIP", "Red Tulip"),
    ("ORANGE_TULIP", "Orange Tulip"), ("WHITE_TULIP", "White Tulip"), ("PINK_TULIP", "Pink Tulip"),
    ("OXEYE_DAISY", "Oxeye Daisy"), ("CORNFLOWER", "Cornflower"), ("LILY_OF_THE_VALLEY", "Lily of the Valley"),
    ("WITHER_ROSE", "Wither Rose"), ("SUNFLOWER", "Sunflower"), ("LILAC", "Lilac"),
    ("ROSE_BUSH", "Rose Bush"), ("PEONY", "Peony"),
    ("PITCHER_PLANT", "Pitcher Plant"), ("TORCHFLOWER", "Torchflower")
]
for mat, name in flowers:
    buy_p = 500 if mat == "WITHER_ROSE" else 60
    add_item("nature", mat, name, 32, buy_p, -1)

# 7. Furniture & Functional
furn = [
    ("CRAFTING_TABLE", "Crafting Table", 1, 150),
    ("FURNACE", "Furnace", 1, 200),
    ("SMOKER", "Smoker", 1, 300),
    ("BLAST_FURNACE", "Blast Furnace", 1, 300),
    ("CHEST", "Chest", 1, 200),
    ("BARREL", "Barrel", 1, 200),
    ("ENDER_CHEST", "Ender Chest", 1, 1000),
    ("ANVIL", "Anvil", 1, 1500),
    ("ENCHANTING_TABLE", "Enchanting Table", 1, 2000),
    ("BREWING_STAND", "Brewing Stand", 1, 800),
    ("CAULDRON", "Cauldron", 1, 350),
    ("COMPOSTER", "Composter", 1, 150),
    ("STONECUTTER", "Stonecutter", 1, 250),
    ("LOOM", "Loom", 1, 250),
    ("CARTOGRAPHY_TABLE", "Cartography Table", 1, 250),
    ("SMITHING_TABLE", "Smithing Table", 1, 250),
    ("GRINDSTONE", "Grindstone", 1, 250),
    ("BELL", "Bell", 1, 5000),
    ("LANTERN", "Lantern", 1, 400),
    ("SOUL_LANTERN", "Soul Lantern", 1, 450),
    ("CAMPFIRE", "Campfire", 1, 250),
    ("SOUL_CAMPFIRE", "Soul Campfire", 1, 300),
    ("BOOKSHELF", "Bookshelf", 1, 750),
    ("CHISELED_BOOKSHELF", "Chiseled Bookshelf", 1, 800),
    ("PAINTING", "Painting", 1, 300),
    ("ITEM_FRAME", "Item Frame", 1, 250),
    ("GLOW_ITEM_FRAME", "Glow Item Frame", 1, 350),
    ("ARMOR_STAND", "Armor Stand", 1, 500)
]
for mat, name, trade, buy in furn:
    add_item("furniture", mat, name, trade, buy, -1)

# 8. Redstone
redstone = [
    ("REDSTONE", "Redstone Dust", 32, 300),
    ("REDSTONE_TORCH", "Redstone Torch", 16, 200),
    ("REPEATER", "Redstone Repeater", 1, 150),
    ("COMPARATOR", "Redstone Comparator", 1, 200),
    ("TARGET", "Target", 1, 150),
    ("LEVER", "Lever", 16, 100),
    ("OAK_BUTTON", "Oak Button", 16, 100),
    ("STONE_BUTTON", "Stone Button", 16, 100),
    ("OAK_PRESSURE_PLATE", "Oak Pressure Plate", 16, 150),
    ("STONE_PRESSURE_PLATE", "Stone Pressure Plate", 16, 150),
    ("LIGHT_WEIGHTED_PRESSURE_PLATE", "Light Weighted Pressure Plate", 16, 250),
    ("HEAVY_WEIGHTED_PRESSURE_PLATE", "Heavy Weighted Pressure Plate", 16, 250),
    ("DAYLIGHT_DETECTOR", "Daylight Detector", 1, 250),
    ("REDSTONE_LAMP", "Redstone Lamp", 1, 300),
    ("TRIPWIRE_HOOK", "Tripwire Hook", 16, 200),
    ("TRAPPED_CHEST", "Trapped Chest", 1, 250),
    ("HOPPER", "Hopper", 1, 2000),
    ("DISPENSER", "Dispenser", 1, 800),
    ("DROPPER", "Dropper", 1, 400),
    ("OBSERVER", "Observer", 1, 1500),
    ("PISTON", "Piston", 1, 800),
    ("STICKY_PISTON", "Sticky Piston", 1, 1200),
    ("NOTE_BLOCK", "Note Block", 1, 300),
    ("JUKEBOX", "Jukebox", 1, 2000),
    ("SLIME_BLOCK", "Slime Block", 1, 1000),
    ("CRAFTER", "Crafter", 1, 1500),
    ("COPPER_BULB", "Copper Bulb", 1, 500),
    ("EXPOSED_COPPER_BULB", "Exposed Copper Bulb", 1, 500),
    ("WEATHERED_COPPER_BULB", "Weathered Copper Bulb", 1, 500),
    ("OXIDIZED_COPPER_BULB", "Oxidized Copper Bulb", 1, 500)
]
for mat, name, trade, buy in redstone:
    add_item("redstone", mat, name, trade, buy, -1)

# 9. Foods
foods = [
    ("BREAD", "Bread", 16, 400),
    ("COOKED_BEEF", "Steak", 16, 800),
    ("COOKED_PORKCHOP", "Cooked Porkchop", 16, 800),
    ("COOKED_MUTTON", "Cooked Mutton", 16, 600),
    ("COOKED_CHICKEN", "Cooked Chicken", 16, 500),
    ("COOKED_RABBIT", "Cooked Rabbit", 16, 600),
    ("COOKED_COD", "Cooked Cod", 16, 400),
    ("COOKED_SALMON", "Cooked Salmon", 16, 500),
    ("BAKED_POTATO", "Baked Potato", 16, 300),
    ("GOLDEN_CARROT", "Golden Carrot", 16, 2500),
    ("GOLDEN_APPLE", "Golden Apple", 1, 1000),
    ("ENCHANTED_GOLDEN_APPLE", "Enchanted Golden Apple", 1, 15000),
    ("PUMPKIN_PIE", "Pumpkin Pie", 16, 600),
    ("MUSHROOM_STEW", "Mushroom Stew", 1, 100),
    ("RABBIT_STEW", "Rabbit Stew", 1, 150),
    ("BEETROOT_SOUP", "Beetroot Soup", 1, 100),
    ("SUSPICIOUS_STEW", "Suspicious Stew", 1, 250),
    ("COOKIE", "Cookie", 16, 200),
    ("CAKE", "Cake", 1, 500),
    ("HONEY_BOTTLE", "Honey Bottle", 1, 150)
]
for mat, name, trade, buy in foods:
    add_item("food", mat, name, trade, buy, -1)

# 10. Mobdrops
drops = [
    ("ROTTEN_FLESH", "Rotten Flesh", 32, 3000, 250),
    ("BONE", "Bone", 32, 6750, 300),
    ("STRING", "String", 32, 6750, 300),
    ("SPIDER_EYE", "Spider Eye", 16, 5000, 200),
    ("GUNPOWDER", "Gunpowder", 16, 14000, 375),
    ("ENDER_PEARL", "Ender Pearl", 16, 24000, 375),
    ("SLIME_BALL", "Slimeball", 16, 15000, 375),
    ("BLAZE_ROD", "Blaze Rod", 16, 19500, 375),
    ("GHAST_TEAR", "Ghast Tear", 8, 32000, 188),
    ("PHANTOM_MEMBRANE", "Phantom Membrane", 8, 22000, 188),
    ("INK_SAC", "Ink Sac", 16, 3000, 200),
    ("GLOW_INK_SAC", "Glow Ink Sac", 16, 4500, 250),
    ("RABBIT_HIDE", "Rabbit Hide", 16, 2000, 200),
    ("RABBIT_FOOT", "Rabbit Foot", 16, 6000, 250),
    ("TURTLE_SCUTE", "Turtle Scute", 8, 15000, 188),
    ("FEATHER", "Feather", 32, 2000, 250),
    ("LEATHER", "Leather", 32, 4000, 300)
]
for mat, name, trade, buy, sell in drops:
    add_item("mobdrops", mat, name, trade, buy, sell)

# 11. Farming
farms = [
    ("WHEAT", "Wheat", 16, 2500, 125),
    ("CARROT", "Carrot", 16, 2500, 125),
    ("POTATO", "Potato", 16, 2500, 125),
    ("BEETROOT", "Beetroot", 16, 2500, 125),
    ("SUGAR_CANE", "Sugar Cane", 16, 3000, 200),
    ("PUMPKIN", "Pumpkin", 8, 3500, 100),
    ("MELON_SLICE", "Melon Slice", 8, 800, 63),
    ("BAMBOO", "Bamboo", 32, 1000, 250),
    ("COCOA_BEANS", "Cocoa Beans", 16, 4000, 300),
    ("NETHER_WART", "Nether Wart", 16, 5000, 375),
    ("APPLE", "Apple", 16, 2000, 200),
    ("SWEET_BERRIES", "Sweet Berries", 16, 1500, 200),
    ("GLOW_BERRIES", "Glow Berries", 16, 2500, 250),
    ("CHORUS_FRUIT", "Chorus Fruit", 16, 4000, 375),
    ("HONEYCOMB", "Honeycomb", 16, 6000, 500),
    ("HONEY_BLOCK", "Honey Block", 8, 8000, 300),
    ("KELP", "Kelp", 32, 1500, 250)
]
for mat, name, trade, buy, sell in farms:
    add_item("farming", mat, name, trade, buy, sell)

# 12. Misc
misc = [
    ("NAME_TAG", "Name Tag", 1, 5000),
    ("LEAD", "Lead", 1, 2000),
    ("SADDLE", "Saddle", 1, 8000),
    ("HORSE_ARMOR_IRON", "Iron Horse Armor", 1, 10000),
    ("HORSE_ARMOR_GOLD", "Gold Horse Armor", 1, 15000),
    ("HORSE_ARMOR_DIAMOND", "Diamond Horse Armor", 1, 30000),
    ("BOOK", "Book", 16, 1500),
    ("PAPER", "Paper", 32, 800),
    ("TOTEM_OF_UNDYING", "Totem of Undying", 1, 100000)
]
for mat, name, trade, buy_p in misc:
    add_item("misc", mat, name, trade, buy_p, -1)

market["items"] = items

class Dumper(yaml.Dumper):
    def increase_indent(self, flow=False, indentless=False):
        return super(Dumper, self).increase_indent(flow, False)

with open('d:/xampp/htdocs/PersonalProject/mc/Economy/RumahKitaEconomyV2/market.yml', 'w', encoding='utf-8') as f:
    yaml.dump(market, f, default_flow_style=False, sort_keys=False, Dumper=Dumper, allow_unicode=True)
