# Auto Mending (Forge 1.19.2)

自动消耗经验值修复带有经验修补（Mending）附魔的物品，支持潜影盒 / Curios / Sophisticated Backpacks 内的物品。

> 由已重建的 Forge 1.20.1 母本工程移植而来，行为保持一致，并适配 Forge 1.19.2（Forge 43.5.x / official mappings）。

## 功能

- 每 2 tick 扫描一次（服务端 `PlayerTickEvent`，END 阶段）
- 有经验值时，优先修复**损坏比例最高**、带有经验修补附魔、且可损坏的物品
- 每次修复最多消耗 **5 点经验**，每点经验修复 **2 点耐久**
- 扫描范围：主物品栏 → 盔甲 → 副手 → Curios 槽位 → 主物品栏内容器（潜影盒）→ 背包（主物品栏 / 副手 / Curios 槽内）
- 修复后向客户端同步经验条（`ClientboundSetExperiencePacket`）

## 兼容性

| 依赖 | 范围 | 类型 |
| --- | --- | --- |
| Minecraft | `[1.19.2,1.20)` | 必需 |
| Forge | `[43,)` | 必需 |
| Curios API | 任意 | 可选（反射，缺失时跳过） |
| Sophisticated Backpacks | 任意 | 可选（反射，缺失时跳过） |

## 与 1.20.1 母本的差异（本移植的全部改动）

1. **Curios 反射入口**：1.19.x 的 CuriosApi 没有静态 `getCuriosInventory(LivingEntity)`；
   改为 `CuriosApi.getCuriosHelper()` → `getCuriosHandler(LivingEntity)` → `LazyOptional<ICuriosItemHandler>`（后续 `getCurios()/getStacks()/getSlots()/getStackInSlot(int)` 链不变）。
2. **Entity.level**：1.19.2 尚未引入 `Entity.level()` getter，改用公开字段 `player.level`。
3. **构建版本**：Forge 43.5.0、official mappings 1.19.2、`pack_format=9`、`mod_version=1.19.2-1.0.2`。
4. Sophisticated Backpacks 反射链与 1.20.1 相同（1.19.x 保留 `CapabilityBackpackWrapper.BACKPACK_WRAPPER_CAPABILITY`）。

## 构建

要求：JDK 17（本机位于 `D:\Programming languages\Java\Java17`）、Gradle 8.1.x。

```powershell
$env:JAVA_HOME = "D:\Programming languages\Java\Java17"
$env:Path = "D:\Programming languages\Java\Java17\bin;$env:Path"
.\gradlew.bat build
```

产物：`build/libs/auto-mending-1.19.2-1.0.2.jar`，放入 `.minecraft/mods/` 即可。
