package com.ceclientmod.jade;

import com.ceclientmod.CraftEngineClientModInit;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;

/**
 * Instantiated by Jade via the "jade" fabric.mod.json entrypoint (Fabric's own entrypoint mechanism -
 * explicit class name declared there, no classpath scanning) - never referenced from our own client
 * entrypoint, so the game still launches fine without Jade installed.
 *
 * CraftEngine blocks and furniture arrive as vanilla block states/entities. The bridge replaces Jade's
 * icon with the exact client-bound source item and also replaces disguised blocks' titles with that
 * item's client-resolved hover name.
 */
public final class CeJadePlugin implements IWailaPlugin {

    private static final Identifier UID = Identifier.fromNamespaceAndPath("ceclientmod", "jade_plugin");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        CeBlockComponentProvider blocks = new CeBlockComponentProvider();
        registration.registerBlockComponent(blocks, Block.class);
        registration.registerBlockIcon(blocks, Block.class);
        registration.registerEntityIcon(new CeFurnitureIconProvider(), Entity.class);
        registration.addTooltipCollectedCallback(Integer.MAX_VALUE, (box, accessor) -> {
            if (!(accessor instanceof BlockAccessor blockAccessor)) return;
            ITooltip tooltip = box.getTooltip();
            CraftEngineClientModInit.blockIcons().iconFor(blockAccessor.getBlockState())
                    .ifPresent(stack -> tooltip.replace(
                            JadeIds.CORE_OBJECT_NAME,
                            IThemeHelper.get().title(stack.getHoverName())));
            CraftEngineClientModInit.blocks().ceIdFor(blockAccessor.getBlockState())
                    .ifPresent(ceId -> {
                        // Jade's own bottom mod-name line is resolved from the vanilla carrier block/item,
                        // so a CraftEngine block would be labelled "Minecraft". Show the CraftEngine id's
                        // namespace instead, keeping Jade's mod-name styling.
                        tooltip.replace(JadeIds.CORE_MOD_NAME, IThemeHelper.get().modName(namespaceOf(ceId)));
                        // Vanilla providers describe the carrier block the CraftEngine block is disguised
                        // as - e.g. a note block's instrument/note ("Snare Drum F"). That is disguise
                        // noise for a CraftEngine block, so drop those lines.
                        tooltip.remove(JadeIds.MC_NOTE_BLOCK);
                    });
        });
    }

    private static String namespaceOf(String ceId) {
        int colon = ceId.indexOf(':');
        return colon < 0 ? ceId : ceId.substring(0, colon);
    }

    private static final class CeBlockComponentProvider implements IBlockComponentProvider {

        @Override
        public Element getIcon(BlockAccessor accessor, IPluginConfig config, Element currentIcon) {
            return CraftEngineClientModInit.blockIcons().iconFor(accessor.getBlockState())
                    .<Element>map(JadeUI::item)
                    .orElse(currentIcon);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            // Jade's own object-name/mod-name lines describe the vanilla carrier block ("Tripwire" /
            // "Minecraft"); replace them with the CraftEngine item's identity. Doing it here - in the
            // block component provider, which is guaranteed to be invoked - instead of relying only on
            // the tooltip-collected callback, which some Jade versions do not call for every accessor.
            CraftEngineClientModInit.blockIcons().iconFor(accessor.getBlockState()).ifPresent(stack ->
                    tooltip.replace(JadeIds.CORE_OBJECT_NAME, IThemeHelper.get().title(stack.getHoverName())));
            CraftEngineClientModInit.blocks().ceIdFor(accessor.getBlockState()).ifPresent(ceId -> {
                tooltip.replace(JadeIds.CORE_MOD_NAME, IThemeHelper.get().modName(namespaceOf(ceId)));
                tooltip.remove(JadeIds.MC_NOTE_BLOCK);
            });
            // The raw CraftEngine id is diagnostic detail - like Jade's own coordinate/registry-name
            // lines, only show it in details mode (Shift by default).
            if (!accessor.showDetails()) return;
            CraftEngineClientModInit.blocks().ceIdFor(accessor.getBlockState())
                    .ifPresent(ceId -> tooltip.add(Component.literal("CraftEngine: " + ceId)));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }

    private static final class CeFurnitureIconProvider implements IEntityComponentProvider {
        @Override
        public Element getIcon(EntityAccessor accessor, IPluginConfig config, Element currentIcon) {
            return CraftEngineClientModInit.furnitureIcons().iconFor(accessor.getEntity())
                    .<Element>map(JadeUI::item)
                    .orElse(currentIcon);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
