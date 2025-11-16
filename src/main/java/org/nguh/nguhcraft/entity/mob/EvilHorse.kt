package org.nguh.nguhcraft.entity.mob

import net.minecraft.entity.EntityType
import net.minecraft.entity.ai.goal.*
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedData
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.entity.passive.AbstractHorseEntity
import net.minecraft.entity.passive.CatEntity
import net.minecraft.entity.passive.OcelotEntity
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.world.World
import org.nguh.nguhcraft.entity.ai.EvilHorseIgniteGoal

object EvilHorse {
    private var FUSE_SPEED : TrackedData<Int> =
        DataTracker.registerData(EvilHorse::class.java, TrackedDataHandlerRegistry.INTEGER)
    private var CHARGED : TrackedData<Boolean> =
        DataTracker.registerData(EvilHorse::class.java, TrackedDataHandlerRegistry.BOOLEAN)
    private var IGNITED : TrackedData<Boolean> =
        DataTracker.registerData(EvilHorse::class.java, TrackedDataHandlerRegistry.BOOLEAN)
    private var DEFAULT_CHARGED : Boolean = false
    private var DEFAULT_IGNITED : Boolean = false
    private var DEFAULT_FUSE : Short = 60
    private var DEFAULT_EXPLOSION_RADIUS : Byte = 6

    open class EvilHorse(open val entityType: EntityType<out EvilHorse>, world: World) : AbstractHorseEntity(entityType, world){
        private var headsDropped = 0
        private var lastFuseTime = 0
        private var currentFuseTime = 0
        private var fuseTime = 30
        private var explosionRadius = 6

        fun getFuseSpeed(): Int {
            return this.dataTracker.get(FUSE_SPEED)
        }

        fun setFuseSpeed(fuseSpeed: Int) {
            this.dataTracker.set(FUSE_SPEED, fuseSpeed)
        }

        override fun initGoals() {
            this.goalSelector.add(1, SwimGoal(this))
            //this.goalSelector.add(2, EvilHorseIgniteGoal(this))
            this.goalSelector.add(3, FleeEntityGoal(this, OcelotEntity::class.java, 6.0f, 1.0, 1.2))
            this.goalSelector.add(3, FleeEntityGoal(this, CatEntity::class.java, 6.0f, 1.0, 1.2))
            this.goalSelector.add(4, MeleeAttackGoal(this, 1.0, false))
            this.goalSelector.add(5, WanderAroundFarGoal(this, 0.8))
            this.goalSelector.add(6, LookAtEntityGoal(this, PlayerEntity::class.java, 128.0f))
            this.goalSelector.add(6, LookAroundGoal(this))
            this.goalSelector.add(7, LookAtEntityGoal(this, VillagerEntity::class.java, 128.0f))
            this.targetSelector.add(1, ActiveTargetGoal(this, PlayerEntity::class.java, true))
            this.targetSelector.add(2, RevengeGoal(this))
        }

        // TODO: make custom sounds for the evil horse that kills you
        // maybe the ambient sound could be just someone saying "hello it is me the evil horse that kills you evilly"
        override fun getAmbientSound(): SoundEvent { return SoundEvents.ENTITY_HORSE_AMBIENT }
        override fun getDeathSound(): SoundEvent { return SoundEvents.ENTITY_HORSE_DEATH }
        override fun getEatSound(): SoundEvent { return SoundEvents.ENTITY_HORSE_EAT }

        companion object {
            fun createEvilHorseAttributes(): DefaultAttributeContainer.Builder? {
                return createAnimalAttributes()
                    .add(EntityAttributes.JUMP_STRENGTH, 0.7)
                    .add(EntityAttributes.MAX_HEALTH, 53.0)
                    .add(EntityAttributes.MOVEMENT_SPEED, 0.225)
                    .add(EntityAttributes.STEP_HEIGHT, 1.0)
                    .add(EntityAttributes.SAFE_FALL_DISTANCE, 6.0)
                    .add(EntityAttributes.FALL_DAMAGE_MULTIPLIER, 0.5)
            }
        }
    }
}