package org.nguh.nguhcraft.entity

import net.minecraft.client.render.entity.feature.EnergySwirlOverlayFeatureRenderer
import net.minecraft.client.render.entity.feature.FeatureRendererContext
import net.minecraft.client.render.entity.model.LoadedEntityModels
import net.minecraft.util.Identifier

open class EvilHorseChargeFeatureRenderer(
    context: FeatureRendererContext<EvilHorseEntityRenderState?, EvilHorseModel?>?,
    loader: LoadedEntityModels
) : EnergySwirlOverlayFeatureRenderer<EvilHorseEntityRenderState, EvilHorseModel>
    (context) {
    protected var model: EvilHorseModel

    init {
        this.model = EvilHorseModel(loader.getModelPart(NguhEntities.MODEL_EVIL_HORSE_CHARGE_LAYER))
    }

    override fun shouldRender(state: EvilHorseEntityRenderState?): Boolean {
        if (state?.charged == true && !state.isBaby) {
            return true
        } else {
            return false
        }
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

open class EvilHorseBabyChargeFeatureRenderer (
    context: FeatureRendererContext<EvilHorseEntityRenderState?, EvilHorseModel?>?,
    loader: LoadedEntityModels
) : EvilHorseChargeFeatureRenderer(context, loader) {

    init {
        this.model = EvilHorseModel(loader.getModelPart(NguhEntities.MODEL_EVIL_HORSE_BABY_CHARGE_LAYER))
    }

    override fun shouldRender(state: EvilHorseEntityRenderState?): Boolean {
        if (state?.charged == true && state.isBaby) {
            return true
        } else {
            return false
        }
    }
}