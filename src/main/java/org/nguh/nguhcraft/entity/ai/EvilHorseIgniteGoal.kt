package org.nguh.nguhcraft.entity.ai

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.mob.CreeperEntity
import org.nguh.nguhcraft.entity.mob.EvilHorseEntity

class EvilHorseIgniteGoal(var evilhorse : EvilHorseEntity) : Goal() {
    private var target: LivingEntity? = evilhorse.target

    override fun canStart(): Boolean {
        return evilhorse.getFuseSpeed() > 0 || target != null && evilhorse.squaredDistanceTo(target) < 9.0;
    }
}