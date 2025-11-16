
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.MobEntityRenderer
import net.minecraft.client.render.entity.state.HorseEntityRenderState
import net.minecraft.client.render.entity.state.LivingHorseEntityRenderState
import net.minecraft.util.Identifier
import org.nguh.nguhcraft.entity.NguhEntities
import org.nguh.nguhcraft.entity.mob.EvilHorse
import org.nguh.nguhcraft.render.entity.model.EvilHorseModel


class EvilHorseRenderer(context: EntityRendererFactory.Context) :
    MobEntityRenderer<EvilHorse.EvilHorse?, LivingHorseEntityRenderState, EvilHorseModel?>(
    context,
    EvilHorseModel(context.getPart(NguhEntities.MODEL_EVIL_HORSE_LAYER)),
    0.5f
) {
    override fun getTexture(state: LivingHorseEntityRenderState?): Identifier? {
        return Identifier.of("nguhcraft", "textures/entity/evil_horse/evil_horse.png")
    }

    override fun createRenderState(): LivingHorseEntityRenderState? {
        return HorseEntityRenderState()
    }
}