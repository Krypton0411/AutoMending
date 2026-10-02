# Auto Mending

自动消耗经验值修复带有经验修补（Mending）附魔的物品，支持潜影盒 / Curios / Sophisticated Backpacks 内的物品。
不再需要手动捡经验球，系统会自动消耗经验值修复带有经验修补附魔的物品，包括潜影盒内的物品。

Automatically uses your experience to repair items with the Mending enchantment, including items inside shulker boxes, Curios slots and Sophisticated Backpacks. No more manually collecting XP orbs - the mod spends your experience automatically to repair enchanted items.

## 源码 / Source Code

完整可编译的 Gradle 工程以明文形式存放于各版本分支中。
The complete, buildable Gradle projects live as plain files in version branches:

| 版本 / Version | 分支 / Branch | 加载器 / Loader |
| --- | --- | --- |
| 1.20.1 | `main` | Forge 1.20.1 (>=47) |
| 1.19.2 | `forge-1.19.2` | Forge 1.19.2 (>=43.5) |
| 1.21.1 | `neoforge-1.21.1` | NeoForge 1.21.1 (>=21.1.252) |

## 成品模组 / Compiled Jars

| 版本 / Version | 文件 / File | 加载器 / Loader |
| --- | --- | --- |
| 1.20.1 | `auto-mending-1.20.1-1.0.2.jar` | Forge 1.20.1 |
| 1.19.2 | `auto-mending-1.19.2-1.0.2.jar` | Forge 1.19.2 |
| 1.21.1 | `auto-mending-1.21.1-1.0.2.jar` | NeoForge 1.21.1 |

直接放入 `.minecraft/mods/` 即可使用。Drop the jar for your version into `.minecraft/mods/` and you are ready to go.

## 功能 / Features

- 每 2 tick 扫描一次（服务端 `PlayerTickEvent`）/ Scans every 2 ticks (server-side `PlayerTickEvent`)
- 有经验值时，优先修复**损坏比例最高**、带有经验修补附魔、且可损坏的物品 / Repairs the most damaged item with the Mending enchantment first while you have XP
- 每次修复最多消耗 **5 点经验**，每点经验修复 **2 点耐久** / Up to 5 XP per repair, 2 durability per XP
- 扫描范围：主物品栏 → 盔甲 → 副手 → Curios 槽位 → 主物品栏内容器（潜影盒）→ 背包 / Scans: inventory → armor → offhand → Curios slots → shulker boxes → backpacks
- 修复后向客户端同步经验条 / XP bar is synced to the client after repairing

## 兼容性 / Compatibility

| 依赖 / Dependency | 类型 / Type |
| --- | --- |
| Minecraft + Forge / NeoForge | 必需 / Required (see table above) |
| Curios API | 可选（反射调用，缺失时跳过）/ Optional (reflection, skipped if absent) |
| Sophisticated Backpacks | 可选（反射调用，缺失时跳过）/ Optional (reflection, skipped if absent) |

## 构建 / Build

克隆对应分支后在工程根目录执行 / Clone the branch and run in the project root:

- **Forge 1.20.1 / 1.19.2**：JDK 17 + Gradle 8.1.x

```powershell
$env:JAVA_HOME = "D:\Programming languages\Java\Java17"
gradle build
```

- **NeoForge 1.21.1**：JDK 21 + Gradle 8.10.x（ModDevGradle）

```powershell
$env:JAVA_HOME = "D:\Programming languages\Java\Java21"
gradle build
```

产物位于 `build/libs/` / Output lands in `build/libs/`.

## License

MIT
