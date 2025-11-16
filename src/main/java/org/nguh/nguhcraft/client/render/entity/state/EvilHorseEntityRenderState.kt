package org.nguh.nguhcraft.client.render.entity.state

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.render.entity.state.LivingHorseEntityRenderState
import net.minecraft.item.ItemStack

@Environment(EnvType.CLIENT)
open class EvilHorseEntityRenderState() : LivingHorseEntityRenderState() {
    var armor = ItemStack.EMPTY
    var fuseTime = 0F
    var charged = false
}