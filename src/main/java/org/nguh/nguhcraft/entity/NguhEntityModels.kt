package org.nguh.nguhcraft.entity

import net.minecraft.client.model.Dilation
import net.minecraft.client.model.ModelPart
import net.minecraft.client.model.TexturedModelData
import net.minecraft.client.render.entity.model.AbstractHorseEntityModel
import net.minecraft.client.render.entity.model.ModelTransformer
import org.nguh.nguhcraft.entity.EvilHorseEntityRenderState

class EvilHorseModel(modelPart: ModelPart) : AbstractHorseEntityModel<EvilHorseEntityRenderState>(modelPart) {

    companion object {
        // Mojank requirement.
        // Instead of just, scaling the model itself 1.1, horses have this transformer. I have no idea why.
        val HORSE_TRANSFORMER = ModelTransformer.scaling(1.1F)
        val CHARGE_DILATION_RADIUS = 0.3F

        private fun getTexturedModelData(dilation: Dilation): TexturedModelData {
            val modelData = getModelData(dilation)
            val texturedModelData = TexturedModelData.of(modelData, 64, 64)
                .transform(HORSE_TRANSFORMER)
            return texturedModelData
        }
        private fun getBabyTexturedModelData(dilation: Dilation): TexturedModelData {
            val modelData = getBabyHorseModelData(dilation)
            val texturedModelData = TexturedModelData.of(modelData, 64, 64)
                .transform(HORSE_TRANSFORMER)
            return texturedModelData
        }

        // Fabric-jank requirement.
        // Instead of just, taking a TexturedModelData, the register needs a function with no parameters.

        fun getEvilHorseTMD(): TexturedModelData {
            return getTexturedModelData(Dilation.NONE)
        }
        fun getEvilHorseChargeTMD(): TexturedModelData {
            return getTexturedModelData(Dilation(CHARGE_DILATION_RADIUS))
        }
        fun getEvilHorseBabyTMD(): TexturedModelData {
            return getBabyTexturedModelData(Dilation.NONE)
        }
        fun getEvilHorseBabyChargeTMD(): TexturedModelData {
            return getBabyTexturedModelData(Dilation(CHARGE_DILATION_RADIUS))
        }
    }
}