package org.nguh.nguhcraft.client.render.entity.feature

import net.minecraft.client.render.entity.feature.EnergySwirlOverlayFeatureRenderer
import net.minecraft.client.render.entity.feature.FeatureRendererContext
import net.minecraft.client.render.entity.model.CreeperEntityModel
import net.minecraft.client.render.entity.model.EntityModelLayers
import net.minecraft.client.render.entity.model.LoadedEntityModels
import net.minecraft.client.render.entity.state.CreeperEntityRenderState
import net.minecraft.util.Identifier
import org.nguh.nguhcraft.client.render.entity.state.EvilHorseEntityRenderState
import org.nguh.nguhcraft.render.entity.model.EvilHorseModel

class EvilHorseChargeFeatureRenderer(
    context: FeatureRendererContext<EvilHorseEntityRenderState?, EvilHorseModel?>?,
    loader: LoadedEntityModels
) : EnergySwirlOverlayFeatureRenderer<EvilHorseEntityRenderState, EvilHorseModel>
    (context) {
    private val model: EvilHorseModel

    init {
        this.model = EvilHorseModel(loader.getModelPart(EntityModelLayers.HORSE_ARMOR))
    }

    override fun shouldRender(state: EvilHorseEntityRenderState?): Boolean {
        return state?.charged == true
    }

    override fun getEnergySwirlX(partialAge: Float): Float {
        return partialAge * 0.01f
    }

    override fun getEnergySwirlTexture(): Identifier {
        return SKIN
    }

    override fun getEnergySwirlModel(): EvilHorseModel {
        return this.model
    }

    companion object {
        private val SKIN: Identifier = Identifier.ofVanilla("textures/entity/creeper/creeper_armor.png")
    }
}