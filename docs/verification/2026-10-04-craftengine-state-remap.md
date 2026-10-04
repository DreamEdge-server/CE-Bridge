CE-JEI-Bridge 1.2.1 evidence: CraftEngine client-visible block states + Jade tooltip names
Date: 2026-10-04

Scope
- Fix the Jade/JEI identity of CraftEngine blocks whose carrier state is remapped on the way to the
  client (Farmer's Delight crops on tripwire, feasts on campfires, non-harp note blocks, ...).
- Fix the Jade tooltip name/mod-name replacement, which silently did nothing on Jade 26.2.
- Version bump 1.2.0 -> 1.2.1 for the server artifacts and the 26.2/26.3 Fabric clients; the
  1.21.11 client is unchanged and stays at 1.1.0. BridgeProtocol.CURRENT_VERSION stays 3.

Root cause (both bugs reproduced with PapersDelight 1.2.1 + the Farmer's Delight content pack)
- CraftEngine maps block states twice. `ImmutableBlockState#visualBlockState()` returns the state
  assigned to the appearance; `ce debug export-block-state-mappings` shows a second table that
  collapses visually identical states onto one canonical state, e.g.
  `craftengine:custom_1114 (farmersdelight:cabbages[age=0])` ->
  `minecraft:tripwire[attached=true,disarmed=true,east=true,north=true,powered=false,south=false,west=true]`
  -> `minecraft:tripwire[...powered=true,south=true,west=true]`.
  `ce debug internal-block-state 1114` confirms the "Visual State" is the intermediate one, while the
  chunk a vanilla client receives carries the collapsed one. Publishing only the intermediate state
  made every collapsed carrier miss the state-keyed client caches, and the duplicate intermediate
  states shared by different blocks (196 of 846 distinct states in the payload) made several blocks
  resolve to one arbitrary icon.
- Jade's tooltip-collected callback (`IWailaClientRegistration#addTooltipCollectedCallback`) is not
  invoked for every block accessor on Jade 26.2.x, so the object-name/mod-name replacements never ran
  and the tooltip kept the vanilla carrier text ("Tripwire" / "Minecraft"). The Jade ids are identical
  in 26.2.9 and 26.2.10 (`object_name`, `mod_name`, `note_block`), so this is a callback-invocation
  difference, not a version-id difference.

Code changes
- SyncManager#clientsideStateStrings: for every block state, publish the assigned visual state and,
  when CraftEngine remaps it, the remapped state as well (BukkitCraftEngine.instance().networkManager()
  .remapBlockState(registryId, false) -> BlockRegistryMirror.byId(...) -> getAsString()). Both
  buildBlocksPayload and buildBlockIconsPayload use it, so the client caches hold a superset of keys -
  a lookup cannot regress, while the collapsed carriers start resolving.
- CeJadePlugin#CeBlockComponentProvider#appendTooltip: replace JadeIds.CORE_OBJECT_NAME with the
  CraftEngine item's hover name and JadeIds.CORE_MOD_NAME with the CraftEngine namespace, and drop the
  carrier-only note-block line, in the block component provider (which is guaranteed to run - it also
  provides the icon). The tooltip-collected callback keeps the same replacements as a fallback.
- Version 1.2.1: server/build.gradle.kts, client/gradle.properties, and the cp/required_assets lines in
  .github/workflows/build-publish.yml for the server and the two 26.x clients.

Build commands (all BUILD SUCCESSFUL)
- server: clean shadowJar -Ptarget=26.2, shadowJar -Ptarget=26.3, shadowJar -Ptarget=1.21.11
- server: bridgeChannelsTest -> BridgeChannelsTest: 1 test passed
- client: clean build -Ptarget=26.2, build -Ptarget=26.3
- client-legacy: clean build (unchanged sources, artifact unchanged apart from being rebuilt)

Artifacts
- server/build/libs/CraftEngineClientBridge-1.2.1-26.2.jar
  size: 63348
  sha256: B3C6EC77D9F970AD1A301D396296897BEC3711369818BABE20D29C29966577DF
  metadata: plugin.yml version 1.2.1, folia-supported: true
- server/build/libs/CraftEngineClientBridge-1.2.1-26.3.jar
  size: 63349
  sha256: 7A78748CF49A37ECF2C32401C257B7D8C960DDEA9F3BFFCB64B6EF63776BC585
  metadata: plugin.yml version 1.2.1, folia-supported: true
- server/build/libs/CraftEngineClientBridge-1.2.1-1.21.11.jar
  size: 63405
  sha256: 2C84552E834BB1A483CAEB37B91358CB4208FFE7E286FF507AE4AC9F5B02AD17
  metadata: plugin.yml version 1.2.1, folia-supported: true
- client/build/libs/ceclientmod-26.2-1.2.1.jar
  size: 68913
  sha256: 1BB46F2C2F2DE23801517CBB1E470B07B99E5CA61A9E01A6DBA880AA494C627E
  fabric.mod.json version 1.2.1
- client/build/libs/ceclientmod-26.3-1.2.1.jar
  size: 68913
  sha256: B939B3D3ED3058A24C3CBAA3DB27789EAF30F53D372F500E85F25F08126FE67A
  fabric.mod.json version 1.2.1
- client-legacy/build/libs/ceclientmod-1.21.11-1.1.0.jar
  size: 58870
  sha256: 54465EFBF5F3A8F9A5E15A0F209229412F7DE259CA1C197A8330FB4CCED63183
  (unchanged sources and version)
- Jar package audit: net/fabricmc, net/minecraft, mezz/jei, snownee/jade entries: 0 in the two 26.x
  client jars.

Payload effect
- Paper 26.2 build 129, CraftEngine 26.9.2, papersdelight 1.2.1, Farmer's Delight content pack 1.2.1,
  plugin 1.2.1-26.2: "CraftEngine sync rebuilt (generation 2): 32764B items (188 crafting outputs),
  252023B blocks, 618581B Jade block icons, 4B brewing, 349191B crafting display, 4448B smithing
  display" - blocks 136151B -> 252023B and icons 329327B -> 618581B versus publishing the assigned
  state only, i.e. the remapped keys are now included.

Live evidence (Paper 26.2 build 129, Java 25, CraftEngine 26.9.2, papersdelight 1.2.1, plugin 1.2.1-26.2,
client ceclientmod-26.2-1.2.1.jar + Jade 26.2.9 + JEI 30.7.0.39)
- PapersDelight license accepted ("PapersDelight LICENSE > 授权成功。"), "PapersDelight 已启动",
  CraftEngine loads the farmersdelight pack (items 613, blocks 182, recipes 278).
- A crop placed with `ce debug setblock <x> <y> <z> farmersdelight:cabbages` renders as the cabbage
  model and Jade's tooltip shows the cabbage-seeds icon (window capture: shots/crop-bothkeys2.png).
- Tooltip names: the provider-side replacement is what makes the object-name/mod-name lines show the
  CraftEngine identity; verified by the reporter on the same client after the fix.

Known constraint discovered while deploying (not a bridge issue)
- papersdelight 1.2.1 supports Minecraft 1.21.0-1.21.11 and 26.1.x-26.2.x only. On Paper 26.3 its
  version bridge throws `IllegalStateException: 不支持的 Minecraft 版本：26.3` inside the neighbour
  update path (`DEOBF_f$DEOBF_a` class init), which crashes the server. The Farmer's Delight test
  environment therefore runs Paper 26.2 (build 129) with CraftEngine 26.9.2.

Deployment evidence
- Local test servers only: _paper262/ (Paper 26.2 build 129 + CraftEngine 26.9.2 + papersdelight 1.2.1
  + the Farmer's Delight content pack + plugin 1.2.1-26.2, RCON 25575) and _folia_ce_test/ (Folia 26.2,
  used earlier in this session). Both have BridgeTest and The_hElend as operators (level 4).
- Client instances: 26.2-Fabric-JEI-Jade and 26.3-Fabric-JEI-Jade (Fabric API + Jade + JEI + the bridge
  mod, nothing else) under the PCL2 data directory.
- No repository file outside the intended change set was modified.
