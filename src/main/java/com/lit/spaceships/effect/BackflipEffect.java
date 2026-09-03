package com.peaceman.alpha.effect;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class BackflipEffect {
    private static final double HORIZONTAL_LAUNCH = 0.8D;
    private static final double VERTICAL_LAUNCH = 0.9D;
    private static final float BONUS_FALL_DAMAGE = 1.0F;
    private static final int ANIMATION_TICKS = 10;

    private BackflipEffect() {
    }

    public static void apply(LivingEntity attacker, LivingEntity target) {
        if (attacker.level().isClientSide) {
            return;
        }

        Vec3 away = target.position().subtract(attacker.position());
        Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(attacker.getLookAngle().x, 0.0D, attacker.getLookAngle().z);
        }
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        horizontal = horizontal.normalize();

        target.setDeltaMovement(
                horizontal.x * HORIZONTAL_LAUNCH,
                VERTICAL_LAUNCH,
                horizontal.z * HORIZONTAL_LAUNCH
        );
        target.hasImpulse = true;

        target.hurt(target.damageSources().fall(), BONUS_FALL_DAMAGE);
        BackflipAnimator.start(target, ANIMATION_TICKS);
    }
}
