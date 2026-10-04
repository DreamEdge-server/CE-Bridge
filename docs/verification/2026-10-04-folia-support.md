CE-JEI-Bridge Folia support and 1.2.0 evidence
Date: 2026-10-04

Scope
- The server plugin now declares `folia-supported: true` and no longer uses any
  legacy scheduler, so the same jar loads and runs on Paper 26.2 / 26.3 / 1.21.11
  and on Folia 26.2.
- Version bump 1.1.0 -> 1.2.0, server artifacts only.
- client/, client-legacy/ and protocol/ are untouched; BridgeProtocol.CURRENT_VERSION
  stays 3 and the wire labels stay unchanged.
- No new build target: Paper implements the Folia scheduler API
  (Bukkit#getGlobalRegionScheduler, RegionScheduler, AsyncScheduler,
  Bukkit#isOwnedByCurrentRegion, Entity#getScheduler), so one jar per target serves
  both server families.

Code changes (server/ only)
- build.gradle.kts: version 1.1.0 -> 1.2.0 (renames the three server jars;
  .github/workflows/build-publish.yml follows for its cp and required_assets lines,
  ceclientmod-*-1.1.0.jar lines untouched because no client is rebuilt).
- plugin.yml: `folia-supported: true`. Folia's SpigotPluginProviderFactory refuses to
  load a plugin without it ("not marked as supporting Folia!"); Paper ignores the key.
- CraftEngineClientBridge#onJoin: `getServer().getScheduler().runTaskLater(...)` (throws
  UnsupportedOperationException on Folia - CraftScheduler#handle is patched to always
  throw) -> `player.getScheduler().runDelayed(this, task -> pushAllTo(player), null, 40L)`.
  The defunct `player.isOnline()` guard is gone: the entity scheduler runs `retired`
  instead of the task for a removed entity.
- CraftEngineClientBridge#pushAllTo: split into the public entry point - inline when
  `Bukkit.isOwnedByCurrentRegion(player)`, otherwise `player.getScheduler().run(...)` -
  and `pushAllToOwned` (the previous body). On Paper the ownership check is true on the
  main thread, so the HELLO-triggered push keeps its previous timing exactly; the
  fan-outs (CraftEngine reload on the global region tick thread, /cebridge resync on the
  console/RCON thread) now hop onto each player's region thread on Folia.
- CraftEngineClientBridge#resolveFurnitureIcon: ownership guard before any Craft* entity
  access. CraftEntity#getHandle is region-threading-patched with
  `TickThread.ensureTickThread(this.entity, "Accessing entity state off owning region's
  thread")`; a foreign-region probe now answers with an empty FurnitureIcon instead.
- CraftEngineClientBridge#furnitureProbeLimits: HashMap -> ConcurrentHashMap (probes are
  handled on per-region threads, onQuit removes from another).
- BridgeCommand reload: the rebuild is handed to the global region scheduler (on Paper:
  the main thread) because CraftEngine's recipe registries are plain
  EnumMap/LinkedHashMap/ArrayList structures that its own reload mutates on the tick
  thread; they must not be read from a Folia region thread. The reply is now
  "Queued a CraftEngine sync rebuild; see the server log for the result." and the result
  is logged with the sender name. The HELLO push, the CraftEngine-reload fan-out and
  /cebridge resync keep returning frames as before.

Build commands (all BUILD SUCCESSFUL)
- server: clean shadowJar -Ptarget=26.2
- server: shadowJar -Ptarget=26.3
- server: shadowJar -Ptarget=1.21.11
- server: bridgeChannelsTest

Test results
- BridgeChannelsTest: 1 test passed
- Protocol sources and tests were not modified.

Artifacts
- server/build/libs/CraftEngineClientBridge-1.2.0-26.2.jar
  size: 62837
  sha256: 81201CFA9129F061BC075EEB39E79485FCAC31C652058EF63CB7F39595910DEF
  class major: 69 (Java 25)
  metadata: minecraft_target=26.x, plugin.yml version 1.2.0, folia-supported: true

- server/build/libs/CraftEngineClientBridge-1.2.0-26.3.jar
  size: 62838
  sha256: 9739DC34439DE97BAA7B1EE17B650BF3A2B196871CC3E42554294F73A888B82B
  class major: 69 (Java 25)
  metadata: minecraft_target=26.x, plugin.yml version 1.2.0, folia-supported: true

- server/build/libs/CraftEngineClientBridge-1.2.0-1.21.11.jar
  size: 62894
  sha256: 10E4484F7336E689D028468D980219DAD48D120761E8C51AADBFD9059F64C32B
  class major: 65 (Java 21)
  metadata: minecraft_target=26.x, plugin.yml version 1.2.0, folia-supported: true

Jar package audit
- net/fabricmc, net/minecraft, mezz/jei, snownee/jade entries: 0 in all three jars.
- Client artifacts were not rebuilt; the release keeps ceclientmod-*-1.1.0.jar.

Live Folia evidence (Folia 26.2-7-ver/26.2.x@14b7fee, API 26.2.build.7-beta, Java 25,
127.0.0.1:25565, RCON 25585, CraftEngine 26.7.4 + packetevents 2.14.0 + plugin 1.2.0-26.2)
- "[CraftEngineClientBridge] Enabling CraftEngineClientBridge v1.2.0"
- "Could not load plugin" / "is not marked as supporting Folia" occurrences: 0, i.e. the
  manifest flag is what makes Folia accept the plugin.
- "[CraftEngine] Enabling CraftEngine v26.7.4" and, once its content load fires the reload
  event, "[CraftEngineClientBridge] CraftEngine sync rebuilt (generation 2): 14507B items
  (82 crafting outputs), 57735B blocks, 141826B Jade block icons, 4B brewing, 279009B
  crafting display, 4015B smithing display".
  The block/icon/display sizes differ slightly from the Paper numbers below because
  CraftEngine 26.7.4 parses the same 26.9.2-era resource directory with its older content
  model. Brewing is empty because the reference server's brewing recipes are registered by
  its Source* addon plugins, which are not part of this test server.
- RCON: `cebridge info` -> items=14507B blocks=57735B brewing=4B;
  `cebridge reload` -> "Queued a CraftEngine sync rebuild; see the server log for the
  result.", then generation 4 rebuilt with identical sizes and "CraftEngine sync caches
  rebuilt on request of Rcon" (this Folia/Paper build names the RCON sender "Rcon");
  second `cebridge info` identical; `cebridge resync` -> "Pushed a fresh sync to all online
  players.".
- UnsupportedOperationException / "Accessing entity state off owning region" occurrences in
  the whole log: 0, including after a headless client join, two rebuilds and a resync.
- CraftEngine test data note: a 26.9.2-generated plugins/CraftEngine directory makes
  26.7.4's bootstrapper fail (Config#updateConfigCache NPE on a null configVersion); the
  test dir was regenerated by 26.7.4 and only resources/ was copied over.

Live client evidence (headless probe, protocol 776; scratch tool at
_folia_ce_test/tools/BridgeProbe.java, outside the repository)
- The probe joins with online-mode=false, registers the nine bridge channels with
  minecraft:register, and sends hello (protocol 3, target 26.x, capabilities 31) plus a
  furniture probe for its own entity id.
- Server answers (frames decoded with BridgeProtocol#decodeFrame; the client-side payload
  envelope is the VarInt-length-prefixed frame written by BridgeChannels):
  - items 14507B (1 frame), blocks 57735B (2), brewing 4B (1), crafting_display 279009B
    (10), smithing_display 4015B (1), block_icons 141826B (5) - arrived once for the HELLO
    handshake, once from the 40-tick join fallback at t=+1940ms, then again for
    `cebridge reload` and `cebridge resync` driven over RCON while the client stayed online.
    The join-fallback push proves EntityScheduler#runDelayed executes on Folia; the resync
    push proves the cross-region path (RCON thread -> not owned -> EntityScheduler#run ->
    delivered on the player's region thread).
  - exactly one ceclientbridge:furniture_icon frame: requestId=1, entityId=<player entity>,
    ceId='', appearance 0B - the probe handler ran, the ownership guard let the
    own-region entity through, and CraftEngine reported "not furniture".

Paper 26.3 regression (Paper 26.3-38-main@d5cc7d4, CraftEngine 26.9.2, plugin
1.2.0-26.3, RCON 25575)
- "[CraftEngineClientBridge] Enabling CraftEngineClientBridge v1.2.0" and
  "CraftEngine sync rebuilt (generation 2): 14507B items (82 crafting outputs), 57723B
  blocks, 141814B Jade block icons, 29838B brewing, 304044B crafting display, 4015B
  smithing display".
- RCON `cebridge info` -> items=14507B blocks=57723B brewing=29838B (identical to 1.1.0).
- RCON `cebridge dump default:amethyst_torch` -> server-bound and client-bound both render
  ItemStack{NETHER_BRICK x 1, ..., item-model=default:amethyst_torch, ...}.
- `cebridge reload` -> "Queued a CraftEngine sync rebuild..." followed by generation 3 with
  the same sizes and the sender-named log line; a second in-process rebuild is byte-identical.
- UnsupportedOperationException occurrences in the whole log: 0.

Crafting-display payload size drift (not code-related)
The one field that does not repeat across boots is the crafting display size: it was
304155B in the 1.1.0 run recorded in 2026-10-04-263-support.md, and on the same server
directory today it is 304044B and 304374B for 1.2.0 and 303806B for 1.1.0 (A/B: the 1.1.0
jar was reinstalled, restarted and measured in the same environment). The payload embeds
per-boot regenerated plugin content - plugins/ArcMenu/generated/images.index and
arcmenu-resourcepack.zip are rewritten during every boot - so its size is not stable for
either version. All other payload sizes are byte-identical across every run and version.
No 1.2.0 run produced a different items/blocks/icons/brewing/smithing size.

Known, pre-existing note
- The 1.21.11 artifact carries minecraft_target=26.x, exactly as recorded for 1.1.0 in
  2026-10-04-263-support.md: processResources' expand values are not tracked as task
  inputs, so the 1.21.11 build reuses the up-to-date output of the previous 26-family
  build in the same build directory (CI builds 26.2 first as well). BridgeHandshake's
  cross-family rule accepts the mismatch, so it has no runtime effect; left unchanged as
  it is outside this change.

Deployment evidence
- Only local test servers were touched: _folia_ce_test/ (new scratch server, CraftEngine
  26.7.4 + packetevents 2.14.0 + CraftEngineClientBridge-1.2.0-26.2.jar, RCON 25585,
  scratch probe under tools/) and _server263/plugins/, which now holds
  CraftEngineClientBridge-1.2.0-26.3.jar (sha256 9739DC34...); the 1.1.0-26.3.jar was
  moved to _server263/plugins-disabled-20261004/.
- The _server263 server is running Paper 26.3 build 38 with the 1.2.0 jar installed.
- No repository file outside the intended change set was modified; nothing was pushed.
