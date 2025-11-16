package org.nguh.nguhcraft.entity

import EvilHorseRenderer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.client.render.entity.model.EntityModelLayer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.entity.mob.EvilHorseEntity
import org.nguh.nguhcraft.render.entity.model.EvilHorseModel


// TODO: make sections for the mobs, right now it's just one creature but that may change in the future

object NguhEntities {

    // =========================================================================
    //  Entity types
    // =========================================================================

    val EVIL_HORSE: EntityType<EvilHorseEntity> = register(
        "evil_horse",
        EntityType.Builder.create(::EvilHorseEntity, SpawnGroup.CREATURE)
            .dimensions(1.3964844F, 1.6F) // I cannot believe this is the actual, vanilla width of a horse
            .eyeHeight(1.52F)
            .passengerAttachments(1.44375F)
            .maxTrackingRange(128) // I just felt a little evil here :3
    )

    // =========================================================================
    //  Entity model layers
    // =========================================================================

    val MODEL_EVIL_HORSE_LAYER = EntityModelLayer(Id("evil_horse"), "main")

    private fun <C : Entity> register(
        key: String,
        type: EntityType.Builder<C>
    ): EntityType<C> = Registry.register(
        Registries.ENTITY_TYPE,
        Id(key),
        type.build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Id(key)))
    )


    fun Init() {
        FabricDefaultAttributeRegistry.register(EVIL_HORSE, EvilHorseEntity.createEvilHorseAttributes());
        onInitializeClient()
    }

    @Environment(EnvType.CLIENT)
    fun onInitializeClient() {
        EntityRendererRegistry.register(EVIL_HORSE, { context -> EvilHorseRenderer(context) })


        EntityModelLayerRegistry.registerModelLayer(
            MODEL_EVIL_HORSE_LAYER,
            EvilHorseModel::getTexturedModelData
        )
    }

}