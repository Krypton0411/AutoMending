# Auto Mending (NeoForge 1.21.1)

自动消耗经验值修复带有经验修补（Mending）附魔的物品，支持潜影盒 / Curios / Sophisticated Backpacks 内的物品。

> 由已重建的 Forge 1.20.1 母本工程移植而来，行为保持一致，并适配 NeoForge 1.21.1（NeoForge 21.1.252 / ModDevGradle / JDK 21）。

## 功能

- 每 2 tick 扫描一次（服务端 `PlayerTickEvent.Post`）
- 有经验值时，优先修复**损坏比例最高**、带有经验修补附魔、且可损坏的物品
- 每次修复最多消耗 **5 点经验**，每点经验修复 **2 点耐久**
- 扫描范围：主物品栏 → 盔甲 → 副手 → Curios 槽位 → 主物品栏内容器（潜影盒）→ 背包（主物品栏 / 副手 / Curios 槽内）
- 修复后向客户端同步经验条（`ClientboundSetExperiencePacket`）

## 兼容性

| 依赖 | 范围 | 类型 |
| --- | --- | --- |
| Minecraft | `[1.21.1,1.21.2)` | 必需 |
| NeoForge | `[21.1.252,)` | 必需 |
| Curios API | 任意 | 可选（反射，缺失时跳过） |
| Sophisticated Backpacks | 任意 | 可选（反射，缺失时跳过） |

## 与 1.20.1 母本的差异（本移植的全部改动）

1. **事件总线**：`MinecraftForge.EVENT_BUS` + `net.minecraftforge.event.TickEvent` →
   `net.neoforged.neoforge.common.NeoForge.EVENT_BUS` + `net.neoforged.neoforge.event.tick.PlayerTickEvent`。
   NeoForge 21.1 中 TickEvent 已拆分为 `event.tick` 包的独立类：订阅 `PlayerTickEvent.Post`（无 Phase 枚举），
   通过 `PlayerEvent.getEntity()` 取玩家。
2. **附魔判定**：1.21.1 中 `Enchantments.MENDING` 是 `ResourceKey<Enchantment>`（注册表化），
   改为 `level.registryAccess().holderOrThrow(Enchantments.MENDING)` + `stack.getEnchantments().getLevel(holder) > 0`。
3. **潜影盒读取**：1.21.1 弃用 NBT，改走 Data Components：
   `stack.get(DataComponents.CONTAINER)` → `ItemContainerContents.nonEmptyItems()`。
4. **Curios 反射入口**：1.21.x 的 `CuriosApi.getCuriosInventory(LivingEntity)` 返回
   `java.util.Optional<ICuriosItemHandler>`（不再是 LazyOptional），用 `isPresent()/get()`。
5. **Sophisticated Backpacks 反射入口**：1.21.x 已移除 `CapabilityBackpackWrapper`，
   改用静态工厂 `BackpackWrapper.fromStack(ItemStack)`（非背包物品返回 `Noop.INSTANCE`，安全）→ `getInventoryHandler()`。
6. **构建**：ModDevGradle 1.0.11 + JDK 21 + `pack_format=34` + `neoforge.mods.toml`（`loaderVersion="[4,)"`）。

## 构建

要求：JDK 21（本机位于 `D:\Programming languages\Java\Java21`）、Gradle 8.10.x（ModDevGradle 需要）。

```powershell
$env:JAVA_HOME = "D:\Programming languages\Java\Java21"
$env:Path = "D:\Programming languages\Java\Java21\bin;$env:Path"
gradle build
```

产物：`build/libs/auto-mending-1.21.1-1.0.2.jar`，放入 `.minecraft/mods/` 即可。
