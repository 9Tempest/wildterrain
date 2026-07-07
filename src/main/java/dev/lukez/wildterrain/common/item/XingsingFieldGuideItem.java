package dev.lukez.wildterrain.common.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class XingsingFieldGuideItem extends Item {
    public XingsingFieldGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            ClientAccess.openGuide();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.wildterrain.xingsing_field_guide.tooltip"));
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientAccess {
        private static void openGuide() {
            net.minecraft.client.Minecraft.getInstance()
                    .setScreen(new dev.lukez.wildterrain.client.screen.XingsingGuideScreen());
        }
    }
}
