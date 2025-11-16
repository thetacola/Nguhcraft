
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.render.entity.AbstractHorseEntityRenderer
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.equipment.EquipmentModel
import net.minecraft.client.render.entity.feature.SaddleFeatureRenderer
import net.minecraft.client.render.entity.model.EntityModelLayers
import net.minecraft.client.render.entity.model.HorseEntityModel
import net.minecraft.client.render.entity.state.HorseEntityRenderState
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import org.nguh.nguhcraft.client.render.entity.feature.EvilHorseChargeFeatureRenderer
import org.nguh.nguhcraft.client.render.entity.state.EvilHorseEntityRenderState
import org.nguh.nguhcraft.entity.mob.EvilHorseEntity
import org.nguh.nguhcraft.render.entity.model.EvilHorseModel
import java.util.function.Function


@Environment(EnvType.CLIENT)
class EvilHorseRenderer(context: EntityRendererFactory.Context)
    : AbstractHorseEntityRenderer<EvilHorseEntity, EvilHorseEntityRenderState, EvilHorseModel>(
        context,
        EvilHorseModel(context.getPart(EntityModelLayers.HORSE)),
    EvilHorseModel(context.getPart(EntityModelLayers.HORSE_BABY))) {

    val saddleFeatureBody = this.addFeature(
        SaddleFeatureRenderer<EvilHorseEntityRenderState, EvilHorseModel, EvilHorseModel>(
            this,
            context.getEquipmentRenderer(),
            EquipmentModel.LayerType.HORSE_BODY,
            Function { evilHorseEntityRenderState: EvilHorseEntityRenderState? ->
                evilHorseEntityRenderState!!.armor },
            EvilHorseModel(context.getPart(EntityModelLayers.HORSE_ARMOR)),
            EvilHorseModel(context.getPart(EntityModelLayers.HORSE_ARMOR_BABY))
        )
    )
    val saddleFeatureSaddle = this.addFeature(
        SaddleFeatureRenderer<EvilHorseEntityRenderState, EvilHorseModel, EvilHorseModel>(
            this,
            context.getEquipmentRenderer(),
            EquipmentModel.LayerType.HORSE_SADDLE,
            Function { evilHorseEntityRenderState: EvilHorseEntityRenderState? ->
                evilHorseEntityRenderState!!.saddleStack },
            EvilHorseModel(context.getPart(EntityModelLayers.HORSE_ARMOR)),
            EvilHorseModel(context.getPart(EntityModelLayers.HORSE_ARMOR_BABY))
        )
    )
    val chargeFeature = this.addFeature(
        EvilHorseChargeFeatureRenderer(
            this,
            context.entityModels
        )
    )

    override fun scale(state: EvilHorseEntityRenderState, matrixStack: MatrixStack) {
        var f = state.fuseTime
        var g = 1.0F + MathHelper.sin(f * 100.0F) * f * 0.01F
        f = MathHelper.clamp(f, 0.0F, 1.0F)
        f *= f
        f *= f
        var h = (1.0F + f * 0.4F) * g
        var i = (1.0F + f * 0.1F) / g
        matrixStack.scale(h, i, h)
    }

    override fun getAnimationCounter(state: EvilHorseEntityRenderState): Float {
        var f = state.fuseTime
        if (f * 10.0F % 2 == 0.0F) {
            return 0.0F
        } else {
            return MathHelper.clamp(f, 0.5F, 1.0F)
        }
    }

    override fun getTexture(state: EvilHorseEntityRenderState?): Identifier? {
        return Identifier.of("nguhcraft", "textures/entity/evil_horse/evil_horse.png")
    }

    override fun createRenderState(): EvilHorseEntityRenderState? {
        return EvilHorseEntityRenderState()
    }

    override fun updateRenderState(
        entity: EvilHorseEntity,
        state: EvilHorseEntityRenderState,
        f: Float
    ) {
        super.updateRenderState(entity, state, f)
        state.fuseTime = entity.getLerpedFuseTime(f)
        state.charged = entity.isCharged()
        state.armor = entity.bodyArmor.copy()
    }

}

