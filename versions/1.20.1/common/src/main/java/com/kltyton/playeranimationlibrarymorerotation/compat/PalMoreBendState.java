package com.kltyton.playeranimationlibrarymorerotation.compat;

/**
 * Evaluated bend rotation (radians) and bend-local position/scale state for one bone.
 */
public record PalMoreBendState(
        float bendX,
        float bendY,
        float bendZ,
        boolean vectorOverride,
        float positionX,
        float positionY,
        float positionZ,
        float scaleX,
        float scaleY,
        float scaleZ,
        boolean transformOverride
) {
    private static final float EPSILON = 1.0E-5F;

    public static final PalMoreBendState NONE = new PalMoreBendState(
            0.0F, 0.0F, 0.0F, false,
            0.0F, 0.0F, 0.0F,
            1.0F, 1.0F, 1.0F, false
    );

    public boolean hasActiveBend() {
        return this.vectorOverride || this.transformOverride;
    }

    public boolean hasNonTrivialTransform() {
        return this.transformOverride
                && (Math.abs(this.positionX) > EPSILON
                || Math.abs(this.positionY) > EPSILON
                || Math.abs(this.positionZ) > EPSILON
                || Math.abs(this.scaleX - 1.0F) > EPSILON
                || Math.abs(this.scaleY - 1.0F) > EPSILON
                || Math.abs(this.scaleZ - 1.0F) > EPSILON);
    }

    public boolean hasVectorYZ() {
        return Math.abs(this.bendY) > EPSILON || Math.abs(this.bendZ) > EPSILON;
    }

    public boolean hasExtendedDeformation() {
        return hasVectorYZ() || hasNonTrivialTransform();
    }

    public PalMoreBendState mirrored() {
        if (!hasActiveBend()) {
            return this;
        }
        return new PalMoreBendState(
                this.bendX,
                -this.bendY,
                -this.bendZ,
                this.vectorOverride,
                -this.positionX,
                this.positionY,
                this.positionZ,
                this.scaleX,
                this.scaleY,
                this.scaleZ,
                this.transformOverride
        );
    }

    public static PalMoreBendState lerp(PalMoreBendState from, PalMoreBendState to, float alpha) {
        if (!from.hasActiveBend() && !to.hasActiveBend()) {
            return NONE;
        }
        float clamped = Math.max(0.0F, Math.min(1.0F, alpha));
        return new PalMoreBendState(
                lerpFloat(from.bendX, to.bendX, clamped),
                lerpFloat(from.bendY, to.bendY, clamped),
                lerpFloat(from.bendZ, to.bendZ, clamped),
                from.vectorOverride || to.vectorOverride,
                lerpFloat(from.positionX, to.positionX, clamped),
                lerpFloat(from.positionY, to.positionY, clamped),
                lerpFloat(from.positionZ, to.positionZ, clamped),
                lerpFloat(from.scaleX, to.scaleX, clamped),
                lerpFloat(from.scaleY, to.scaleY, clamped),
                lerpFloat(from.scaleZ, to.scaleZ, clamped),
                from.transformOverride || to.transformOverride
        );
    }

    private static float lerpFloat(float start, float end, float delta) {
        return start + (end - start) * delta;
    }
}
