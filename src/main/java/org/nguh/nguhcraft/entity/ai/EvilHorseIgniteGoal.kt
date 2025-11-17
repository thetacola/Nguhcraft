package org.nguh.nguhcraft.entity.ai

import net.minecraft.entity.ai.goal.Goal
import org.nguh.nguhcraft.entity.mob.EvilHorseEntity
import java.util.EnumSet

class EvilHorseIgniteGoal(var evilHorse : EvilHorseEntity) : Goal() {
    val controls = this.setControls(EnumSet.of(Control.MOVE))

    override fun canStart(): Boolean {
        if (this.evilHorse.getFuseSpeed() > 0) {
            return true
        } else if (this.evilHorse.target != null && this.evilHorse.squaredDistanceTo(this.evilHorse.target) < 10.0) {
            return true
        } else {
            return false
        }
    }

    override fun start() {
        this.evilHorse.navigation.stop()
    }

    override fun stop() {
        this.evilHorse.target = null
    }

    override fun shouldRunEveryTick(): Boolean {
        return true
    }

    override fun tick() {
        if (this.evilHorse.target == null) {
            this.evilHorse.setFuseSpeed(-1)
        } else if (this.evilHorse.squaredDistanceTo(this.evilHorse.target) > 64.0) { // made 1 block more due to increased radius
            this.evilHorse.setFuseSpeed(-1)
        } else if (!this.evilHorse.visibilityCache.canSee(this.evilHorse.target)) {
            this.evilHorse.setFuseSpeed(-1)
        } else if (this.evilHorse.isTame && this.evilHorse.target == this.evilHorse.owner) {
            // this should prevent the evil horse from exploding when seeing its owner
            // should also make it not explode when the owner is the closest player as well
            this.evilHorse.setFuseSpeed(-1)
        } else {
            this.evilHorse.setFuseSpeed(1)
        }
    }
}