package org.nguh.nguhcraft.entity

import net.minecraft.entity.AreaEffectCloudEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityStatuses
import net.minecraft.entity.EntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LazyEntityReference
import net.minecraft.entity.LightningEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.ai.goal.ActiveTargetGoal
import net.minecraft.entity.ai.goal.AnimalMateGoal
import net.minecraft.entity.ai.goal.FleeEntityGoal
import net.minecraft.entity.ai.goal.LookAroundGoal
import net.minecraft.entity.ai.goal.LookAtEntityGoal
import net.minecraft.entity.ai.goal.MeleeAttackGoal
import net.minecraft.entity.ai.goal.RevengeGoal
import net.minecraft.entity.ai.goal.SwimGoal
import net.minecraft.entity.ai.goal.TemptGoal
import net.minecraft.entity.ai.goal.WanderAroundFarGoal
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedData
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.passive.AbstractHorseEntity
import net.minecraft.entity.passive.AnimalEntity
import net.minecraft.entity.passive.CatEntity
import net.minecraft.entity.passive.GoatEntity
import net.minecraft.entity.passive.OcelotEntity
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.particle.ParticleTypes
import net.minecraft.registry.tag.ItemTags
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.storage.ReadView
import net.minecraft.storage.WriteView
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.random.Random
import net.minecraft.world.GameRules
import net.minecraft.world.World
import net.minecraft.world.WorldView
import net.minecraft.world.event.GameEvent
import org.nguh.nguhcraft.item.NguhItems
import java.util.Optional
import java.util.function.DoubleSupplier
import java.util.function.IntUnaryOperator
import java.util.function.Predicate

open class EvilHorseEntity(open val entityType: EntityType<out EvilHorseEntity>, world: World) :
    AbstractHorseEntity(entityType, world) {

    companion object {
        private val FUSE_SPEED: TrackedData<Int> =
            DataTracker.registerData(EvilHorseEntity::class.java, TrackedDataHandlerRegistry.INTEGER)
        private val CHARGED: TrackedData<Boolean> =
            DataTracker.registerData(EvilHorseEntity::class.java, TrackedDataHandlerRegistry.BOOLEAN)
        private val IGNITED: TrackedData<Boolean> =
            DataTracker.registerData(EvilHorseEntity::class.java, TrackedDataHandlerRegistry.BOOLEAN)
        private val TAMEABLE_FLAGS: TrackedData<Byte> =
            DataTracker.registerData(EvilHorseEntity::class.java, TrackedDataHandlerRegistry.BYTE)
        private val OWNER_UUID: TrackedData<Optional<LazyEntityReference<LivingEntity>>> =
            DataTracker.registerData(EvilHorseEntity::class.java, TrackedDataHandlerRegistry.LAZY_ENTITY_REFERENCE)
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
        val bl: Boolean = super.handleFallDamage(fallDistance, damagePerDistance, damageSource)
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
        builder.add(TAMEABLE_FLAGS, 0)
        builder.add(OWNER_UUID, Optional.empty())
    }

    override fun writeCustomData(view: WriteView) {
        super.writeCustomData(view)
        view.putBoolean("powered", this.isCharged())
        view.putShort("Fuse", this.fuseTime.toShort())
        view.putByte("ExplosionRadius", this.explosionRadius)
        view.putBoolean("ignited", this.isIgnited())
        LazyEntityReference.writeData(this.ownerReference, view, "Owner")
    }

    override fun readCustomData(view: ReadView) {
        super.readCustomData(view)
        this.dataTracker.set(CHARGED, view.getBoolean("powered", false))
        this.fuseTime = view.getShort("Fuse", 90.toShort())
        this.explosionRadius = view.getByte("ExplosionRadius", 6.toByte())
        if (view.getBoolean("ignited", false)) {
            this.ignite()
        }
        val lazyEntityReference = LazyEntityReference.fromDataOrPlayerName<LivingEntity>(view, "Owner", this.world)
        if (lazyEntityReference != null) {
            try {
                this.dataTracker.set(OWNER_UUID, Optional.of(lazyEntityReference))
                this.setTamed(true, false)
            } catch (e: Throwable) {
                this.setTamed(false, true)
            }
        } else {
            this.dataTracker.set(OWNER_UUID, Optional.empty())
            this.setTamed(false, true)
        }
    }

    override fun tick() {
        if (this.isAlive) {
            this.lastFuseTime = this.currentFuseTime
            if (this.isIgnited()) {
                this.setFuseSpeed(1)
            }

            val i = this.getFuseSpeed()
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

    override fun setTarget(target: LivingEntity?) {
        if (target !is GoatEntity) {
            super.setTarget(target)
        }
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
                / (this.fuseTime - 2))
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
        val itemStack = player.getStackInHand(hand)
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
            val bl = !this.isBaby && this.isTame && player.shouldCancelInteraction()

            if (!this.hasPassengers() && !bl) {
                var itemStack = player.getStackInHand(hand)
                if (!itemStack.isEmpty) {
                    if (this.isBreedingItem(itemStack)) {
                        return this.interactHorse(player, itemStack)
                    }
                    if (!this.isTame) {
                        this.playAngrySound()
                        return ActionResult.SUCCESS
                    }
                }
                return super.interactMob(player, hand)
            } else {
                return super.interactMob(player, hand)
            }
        }
    }

    override fun canBreedWith(other: AnimalEntity?): Boolean {
        if (other == this) {
            return false;
        } else {
            if (other is EvilHorseEntity) {
                if (this.isTamed() == other.isTamed()) {
                    return this.canBreed() && other.canBreed()
                } else {
                    return false
                }
            } else {
                return false
            }
        }
    }

    override fun createChild(world: ServerWorld, entity: PassiveEntity): PassiveEntity? {
        val evilHorseEntity = NguhEntities.EVIL_HORSE.create(world, SpawnReason.BREEDING)
        if (evilHorseEntity != null && entity is EvilHorseEntity) {
            this.setChildAttributes(entity, evilHorseEntity)
            if (this.isTamed()) {
                evilHorseEntity.setOwner(this.ownerReference)
                evilHorseEntity.setTamed(true, true)
            }
        }
        return evilHorseEntity
    }

    override fun canUseSlot(slot: EquipmentSlot?): Boolean {
        return true
    }

    override fun damageArmor(source: DamageSource, amount: Float) {
        this.damageEquipment(source, amount, *arrayOf<EquipmentSlot>(EquipmentSlot.BODY))
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
            this.onRemoval(world as ServerWorld, RemovalReason.KILLED)
            this.discard()
        }
    }

    fun spawnEffectsCloud() {
        val collection = this.statusEffects
        if (!collection.isEmpty()) {
            val areaEffectCloudEntity = AreaEffectCloudEntity(this.world, this.x, this.y, this.z)
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

    // stolen from HorseEntity

    override fun initAttributes(random: Random) {
        this.getAttributeInstance(EntityAttributes.MAX_HEALTH)!!.baseValue =
            getChildHealthBonus(IntUnaryOperator { bound: Int -> random.nextInt(bound) }).toDouble()
        this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED)!!.baseValue =
            getChildMovementSpeedBonus(DoubleSupplier { random.nextDouble() })
        this.getAttributeInstance(EntityAttributes.JUMP_STRENGTH)!!.baseValue =
            getChildJumpStrengthBonus(DoubleSupplier { random.nextDouble() })
    }

    override fun playWalkSound(group: BlockSoundGroup?) {
        super.playWalkSound(group)
        if (this.random.nextInt(10) == 0) {
            this.playSound(SoundEvents.ENTITY_HORSE_BREATHE, group!!.getVolume() * 0.6f, group.getPitch())
        }
    }

    // stolen from TameableEntity

    fun setTamed(tamed: Boolean, updateAttributes: Boolean) {
        val b = this.dataTracker.get(TAMEABLE_FLAGS)
        if (tamed) {
            this.dataTracker.set(TAMEABLE_FLAGS, (b.toInt() or 4).toByte())
        } else {
            this.dataTracker.set(TAMEABLE_FLAGS, (b.toInt() and -5).toByte())
        }
    }

    fun setTamedBy(player: PlayerEntity) {
        this.setTamed(true, true)
    }

    override fun setOwner(owner: LivingEntity?) {
        this.dataTracker.set(OWNER_UUID, Optional.ofNullable(owner).map(::LazyEntityReference))
    }

    fun setOwner(owner: LazyEntityReference<LivingEntity>?) {
        this.dataTracker.set(OWNER_UUID, Optional.ofNullable(owner))
    }

    override fun onDeath(damageSource: DamageSource) {
        if (this.world is ServerWorld) {
            val serverWorld = world as ServerWorld
            if (serverWorld.gameRules.getBoolean(GameRules.SHOW_DEATH_MESSAGES)
                && this.owner is ServerPlayerEntity
            ) {
                val spe = owner as ServerPlayerEntity
                spe.sendMessage(this.damageTracker.deathMessage)
            }
        }

        super.onDeath(damageSource)
    }

    // stolen from WolfEntity

    fun tryTame(player: PlayerEntity) {
        if (this.random.nextInt(3) == 0) {
            this.setTamedBy(player)
            this.world.sendEntityStatus(this, EntityStatuses.ADD_POSITIVE_PLAYER_REACTION_PARTICLES)
        } else {
            this.world.sendEntityStatus(this, EntityStatuses.ADD_NEGATIVE_PLAYER_REACTION_PARTICLES)
        }
    }

    protected fun showEmoteParticle(positive: Boolean) {
        var particleEffect = ParticleTypes.HEART
        if (!positive) {
            particleEffect = ParticleTypes.SMOKE
        }

        for (i in 0..6) {
            val d = this.random.nextGaussian() * 0.02
            val e = this.random.nextGaussian() * 0.02
            val f = this.random.nextGaussian() * 0.02
            this.world.addParticleClient(
                particleEffect,
                this.getParticleX(1.0),
                this.randomBodyY + 0.5,
                this.getParticleZ(1.0),
                d,
                e,
                f
            )
        }
    }

    override fun isBreedingItem(stack: ItemStack): Boolean {
        return stack.isOf(NguhItems.EVIL_WHEAT)
    }

    override fun handleStatus(status: Byte) {
        if (status == EntityStatuses.ADD_POSITIVE_PLAYER_REACTION_PARTICLES) {
            this.showEmoteParticle(true)
        } else if (status == EntityStatuses.ADD_NEGATIVE_PLAYER_REACTION_PARTICLES) {
            this.showEmoteParticle(false)
        } else {
            super.handleStatus(status)
        }
    }

    fun isTamed(): Boolean {
        return (this.dataTracker.get<Byte>(TAMEABLE_FLAGS).toInt() and 4) != 0
    }


    // stolen from other places

    override fun initGoals() {
        this.goalSelector.add(1, SwimGoal(this))
        this.goalSelector.add(2, EvilHorseIgniteGoal(this))
        this.goalSelector.add(3, FleeEntityGoal(this, OcelotEntity::class.java, 6.0f, 1.0, 1.2))
        this.goalSelector.add(3, FleeEntityGoal(this, CatEntity::class.java, 6.0f, 1.0, 1.2))
        this.goalSelector.add(4, AnimalMateGoal(this, 1.0))
        this.goalSelector.add(5,
            TemptGoal(this, 1.2, Predicate { stack: ItemStack? -> stack!!.isIn(ItemTags.HORSE_FOOD) }, false)
        )
        this.goalSelector.add(5,
            TemptGoal(this, 1.2, Predicate { stack: ItemStack? -> stack!!.isIn(ItemTags.HORSE_TEMPT_ITEMS) }, false)
        )
        this.goalSelector.add(6, MeleeAttackGoal(this, 1.0, false))
        this.goalSelector.add(7, WanderAroundFarGoal(this, 0.8))
        this.goalSelector.add(8, LookAtEntityGoal(this, PlayerEntity::class.java, 128.0f))
        this.goalSelector.add(8, LookAroundGoal(this))
        this.goalSelector.add(9, LookAtEntityGoal(this, VillagerEntity::class.java, 128.0f))
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

    override fun getHurtSound(source: DamageSource): SoundEvent {
        return SoundEvents.ENTITY_HORSE_HURT
    }

    override fun getSplashSound(): SoundEvent {
        return SoundEvents.ENTITY_HOSTILE_SPLASH
    }
}