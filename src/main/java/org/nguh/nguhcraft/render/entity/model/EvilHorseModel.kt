package org.nguh.nguhcraft.render.entity.model

import net.minecraft.client.model.*
import net.minecraft.client.render.entity.model.AbstractHorseEntityModel
import net.minecraft.client.render.entity.model.EntityModelPartNames
import net.minecraft.client.render.entity.state.LivingHorseEntityRenderState
import net.minecraft.client.model.Dilation
import org.nguh.nguhcraft.client.render.entity.state.EvilHorseEntityRenderState

class EvilHorseModel(modelPart: ModelPart) : AbstractHorseEntityModel<EvilHorseEntityRenderState>(modelPart) {

    companion object {
        fun getTexturedModelData(): TexturedModelData {
            val modelData = getModelData(Dilation.NONE)
            val texturedModelData = TexturedModelData.of(modelData, 64, 64)
            return texturedModelData
        }
    }
}