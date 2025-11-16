package org.nguh.nguhcraft.entity.mob

import net.minecraft.entity.AreaEffectCloudEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LightningEntity
import net.minecraft.entity.ai.goal.*
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedData
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.passive.AbstractHorseEntity
import net.minecraft.entity.passive.CatEntity
import net.minecraft.entity.passive.OcelotEntity
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.registry.tag.ItemTags
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.storage.ReadView
import net.minecraft.storage.WriteView
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.world.World
import net.minecraft.world.WorldView
import net.minecraft.world.event.GameEvent

open class EvilHorseEntity(open val entityType: EntityType<out EvilHorseEntity>, world: World) :
    AbstractHorseEntity(entityType, world) {

    companion object {
        private val FUSE_SPEED: TrackedData<Int> =
            DataTracker.registerData(EvilHorseEntity::class.java, TrackedDataHandlerRegistry.INTEGER)
        private val CHARGED: TrackedData<Boolean> =
            DataTracker.registerData(EvilHorseEntity::class.java, TrackedDataHandlerRegistry.BOOLEAN)
        private val IGNITED: TrackedData<Boolean> =
            DataTracker.registerData(EvilHorseEntity::class.java, TrackedDataHandlerRegistry.BOOLEAN)
        private var DEFAULT_CHARGED: Boolean = false
        private var DEFAULT_IGNITED: Boolean = false
        private var DEFAULT_FUSE: Short = 60
        private var DEFAULT_EXPLOSION_RADIUS: Byte = 6
        fun createEvilHorseAttributes(): DefaultAttributeContainer.Builder? {
            return createAnimalAttributes()
                .add(EntityAttributes.JUMP_STRENGTH, 0.7)
                .add(EntityAttributes.MAX_HEALTH, 53.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.225)
                .add(EntityAttributes.STEP_HEIGHT, 1.0)
                .add(EntityAttributes.SAFE_FALL_DISTANCE, 6.0)
                .add(EntityAttributes.FALL_DAMAGE_MULTIPLIER, 0.5)
                .add(EntityAttributes.ATTACK_DAMAGE)
        }
    }

    private var headsDropped = 0
    private var lastFuseTime = 0
    private var currentFuseTime = 0
    private var fuseTime = 90
    private var explosionRadius: Byte = 6

    // functions stolen from HostileEntity
    override fun getSoundCategory(): SoundCategory? {
        return SoundCategory.HOSTILE
    }

    override fun isDisallowedInPeaceful(): Boolean {
        return true
    }

    override fun getPathfindingFavor(pos: BlockPos?, world: WorldView?): Float {
        return 0 - world!!.getPhototaxisFavor(pos)
    }

    override fun shouldDropExperience(): Boolean {
        return true
    }

    override fun shouldDropLoot(): Boolean {
        return true
    }

    fun isAngryAt(world: ServerWorld, player: PlayerEntity): Boolean {
        // TODO: make not angry at whoever's tamed it
        return true
    }

    // functions stolen from CreeperEntity

    override fun getSafeFallDistance(): Int {
        // this should allow it to drop onto people and explode
        if (target == null) {
            return getSafeFallDistance(0.0F)
        } else {
            return getSafeFallDistance(health - 1.0F)
        }
    }

    override fun handleFallDamage(
        fallDistance: Double,
        damagePerDistance: Float,
        damageSource: DamageSource?
    ): Boolean {
        val bl: Boolean = super.handleFallDamage(fallDistance, damagePerDistance, damageSource);
        currentFuseTime += (fallDistance * 1.5).toInt()
        if (currentFuseTime > this.fuseTime - 5) {
            currentFuseTime = this.fuseTime - 5
        }
        return bl
    }

    override fun initDataTracker(builder: DataTracker.Builder) {
        super.initDataTracker(builder)
        //builder.add(MOB_FLAGS, 0.toByte())
        builder.add(FUSE_SPEED, -1)
        builder.add(CHARGED, false)
        builder.add(IGNITED, false)
    }

    override fun writeCustomData(view: WriteView) {
        super.writeCustomData(view)
        view.putBoolean("powered", this.isCharged())
        view.putShort("Fuse", this.fuseTime.toShort())
        view.putByte("ExplosionRadius", this.explosionRadius.toByte())
        view.putBoolean("ignited", this.isIgnited())
    }

    override fun readCustomData(view: ReadView) {
        super.readCustomData(view)
        this.dataTracker.set(CHARGED, view.getBoolean("powered", false))
        this.fuseTime = view.getShort("Fuse", 90.toShort())
        this.explosionRadius = view.getByte("ExplosionRadius", 6.toByte())
        if (view.getBoolean("ignited", false)) {
            this.ignite()
        }
    }

    override fun tick() {
        if (this.isAlive) {
            this.lastFuseTime = this.currentFuseTime
            if (this.isIgnited()) {
                this.setFuseSpeed(1)
            }

            var i = this.getFuseSpeed()
            if (i > 0 && this.currentFuseTime == 0) {
                this.playSound(SoundEvents.ENTITY_CREEPER_PRIMED, 1.0F, 0.25F)
                this.emitGameEvent(GameEvent.PRIME_FUSE)
            }

            this.currentFuseTime += i
            if (this.currentFuseTime < 0) {
                this.currentFuseTime = 0
            }

            if (this.currentFuseTime >= this.fuseTime) {
                this.currentFuseTime = this.fuseTime
                this.explode()
            }
        }

        super.tick()
    }

    override fun dropEquipment(world: ServerWorld?, source: DamageSource?, causedByPlayer: Boolean) {
        // TODO: add evil horse head
        super.dropEquipment(world, source, causedByPlayer)
    }

    override fun tryAttack(world: ServerWorld?, target: Entity?): Boolean {
        // TODO: do not attack tamer if tamed
        return true
    }

    fun isCharged(): Boolean {
        return this.dataTracker.get(CHARGED)
    }

    // I have no idea what "lerped" means but this is part of the fuse math, apparently
    fun getLerpedFuseTime(tickProgress: Float): Float {
        return (MathHelper.lerp(tickProgress, this.lastFuseTime.toFloat(), this.currentFuseTime.toFloat())
                / (this.fuseTime - 2));
    }

    fun getFuseSpeed(): Int {
        return this.dataTracker.get(FUSE_SPEED)
    }

    fun setFuseSpeed(fuseSpeed: Int) {
        this.dataTracker.set(FUSE_SPEED, fuseSpeed)
    }

    // charge on lightning strike
    override fun onStruckByLightning(world: ServerWorld, lightning: LightningEntity) {
        super.onStruckByLightning(world, lightning)
        this.dataTracker.set(CHARGED, true)
    }

    // ignite if holding creeper igniters, otherwise do horse things
    override fun interactMob(player: PlayerEntity, hand: Hand): ActionResult? {
        var itemStack = player.getStackInHand(hand)
        if (itemStack.isIn(ItemTags.CREEPER_IGNITERS)) {
            // TODO: make the sound different if it's a fire charge
            val soundEvent = SoundEvents.ITEM_FLINTANDSTEEL_USE
            this.world.playSound(
                player, this.x, this.y, this.z, soundEvent, this.soundCategory,
                1.0F, this.random.nextFloat() * 0.4F + 0.8F
            )
            if (!this.world.isClient) {
                this.ignite()
                if (!itemStack.isDamageable()) {
                    itemStack.decrement(1)
                } else {
                    itemStack.damage(1, player, getSlotForHand(hand))
                }
            }

            return ActionResult.SUCCESS
        } else {
            return super.interactMob(player, hand)
        }
    }

    fun explode() {
        if (this.world is ServerWorld) {
            var f = 0F
            if (this.isCharged()) {
                f = 2.0F
            } else {
                f = 1.0F
            }
            this.dead = true
            world.createExplosion(
                this,
                this.x,
                this.y,
                this.z,
                this.explosionRadius * f,
                World.ExplosionSourceType.MOB
            )
            this.spawnEffectsCloud()
            this.onRemoval(world as ServerWorld, Entity.RemovalReason.KILLED)
            this.discard()
        }
    }

    fun spawnEffectsCloud() {
        var collection = this.getStatusEffects()
        if (!collection.isEmpty()) {
            var areaEffectCloudEntity = AreaEffectCloudEntity(this.world, this.x, this.y, this.z)
            areaEffectCloudEntity.radius = this.explosionRadius.toFloat() // this should be a fun change
            areaEffectCloudEntity.radiusOnUse = -0.5F
            areaEffectCloudEntity.waitTime = 10
            areaEffectCloudEntity.duration = 1000
            areaEffectCloudEntity.setPotionDurationScale(0.25F)
            areaEffectCloudEntity.radiusGrowth = -areaEffectCloudEntity.radius / areaEffectCloudEntity.duration

            for (statusEffectInstance in collection) {
                areaEffectCloudEntity.addEffect(StatusEffectInstance(statusEffectInstance))
            }

            this.world.spawnEntity(areaEffectCloudEntity)
        }
    }

    fun isIgnited(): Boolean {
        return this.dataTracker.get(IGNITED)
    }

    fun ignite() {
        this.dataTracker.set(IGNITED, true)
    }

    fun shouldDropHead(): Boolean {
        // TODO: implement head drops
        return false
    }

    fun onHeadDropped() {
        this.headsDropped++
    }

    // stolen from other places

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
    override fun getAmbientSound(): SoundEvent {
        return SoundEvents.ENTITY_HORSE_AMBIENT
    }

    override fun getDeathSound(): SoundEvent {
        return SoundEvents.ENTITY_HORSE_DEATH
    }

    override fun getEatSound(): SoundEvent {
        return SoundEvents.ENTITY_HORSE_EAT
    }

    fun getHurtSound(): SoundEvent {
        return SoundEvents.ENTITY_HORSE_HURT
    }

    override fun getSplashSound(): SoundEvent {
        return SoundEvents.ENTITY_HOSTILE_SPLASH
    }
}