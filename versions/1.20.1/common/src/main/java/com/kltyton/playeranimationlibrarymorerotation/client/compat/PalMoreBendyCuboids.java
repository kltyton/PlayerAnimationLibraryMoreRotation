package com.kltyton.playeranimationlibrarymorerotation.client.compat;

import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.kosmx.bendylib.ModelPartAccessor;
import io.github.kosmx.bendylib.MutableCuboid;
import io.github.kosmx.bendylib.impl.BendableCuboid;
import io.github.kosmx.bendylib.impl.ICuboid;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Layers 3-axis bend rotation and bend-local position/scale vertex deformation on top of
 * bendy-lib 4.0.0's BendableCuboid while preserving native playerAnimator 2-parameter bends.
 */
public final class PalMoreBendyCuboids {
    private static final float EPSILON = 1.0E-5F;
    private static final Map<ModelPart, PalMoreBendState> LAST_VECTOR_BEND =
            Collections.synchronizedMap(new WeakHashMap<>());

    private PalMoreBendyCuboids() {
    }

    public static void applyVectorBend(ModelPart part, PalMoreBendState state) {
        boolean hasActive = state.hasActiveBend();
        PalMoreBendState previous = LAST_VECTOR_BEND.get(part);

        if (!hasActive && previous == null) {
            return;
        }

        if (!hasActive) {
            restoreNativeBend(part);
            LAST_VECTOR_BEND.remove(part);
            return;
        }

        int cuboidCount = ModelPartAccessor.getCuboids(part).size();
        int applied = 0;
        for (int index = 0; index < cuboidCount; index++) {
            Optional<MutableCuboid> optional = ModelPartAccessor.optionalGetCuboid(part, index);
            if (optional.isEmpty()) {
                continue;
            }
            MutableCuboid mutable = optional.get();
            if (!mutable.hasMutator("bend")) {
                continue;
            }
            ICuboid activated = mutable.getAndActivateMutator("bend");
            if (activated instanceof BendableCuboid cuboid) {
                applyToCuboid(cuboid, state);
                applied++;
            }
        }

        if (applied == 0) {
            LAST_VECTOR_BEND.remove(part);
        } else {
            LAST_VECTOR_BEND.put(part, state);
        }
    }

    public static void clearPartState(ModelPart part) {
        PalMoreBendState previous = LAST_VECTOR_BEND.remove(part);
        if (previous == null) {
            return;
        }
        int cuboidCount = ModelPartAccessor.getCuboids(part).size();
        for (int index = 0; index < cuboidCount; index++) {
            Optional<MutableCuboid> optional = ModelPartAccessor.optionalGetCuboid(part, index);
            if (optional.isEmpty()) {
                continue;
            }
            MutableCuboid mutable = optional.get();
            ICuboid mutator = mutable.getMutator("bend");
            if (mutator instanceof BendableCuboid cuboid) {
                cuboid.applyBend(0.0F, 0.0F);
            }
            mutable.getAndActivateMutator(null);
        }
    }

    /**
     * Applies extended arm bend (bendX/Y/Z + bend.position + bend.scale) to the held-item PoseStack,
     * replacing playerAnimator's native 2-parameter bend matrix when sidecar bend data is active.
     */
    public static void applyArmItemBend(
            PoseStack poseStack,
            PalMoreBendState state,
            float nativeBendAxis,
            float nativeBend
    ) {
        if (!state.hasActiveBend()) {
            return;
        }

        float pivotY = 0.25F;
        if (!isZero(nativeBend)) {
            poseStack.translate(0.0F, pivotY, 0.0F);
            float axisAngle = -nativeBendAxis;
            Vector3f axisVec = new Vector3f((float) Math.cos(axisAngle), 0.0F, (float) Math.sin(axisAngle));
            poseStack.mulPose(new Quaternionf().rotateAxis(-nativeBend, axisVec));
            poseStack.translate(0.0F, -pivotY, 0.0F);
        }

        boolean hasTransform = state.hasNonTrivialTransform();
        if (hasTransform) {
            poseStack.translate(
                    state.positionX() / 16.0F,
                    -state.positionY() / 16.0F,
                    state.positionZ() / 16.0F
            );
        }

        poseStack.translate(0.0F, pivotY, 0.0F);
        if (hasTransform) {
            poseStack.scale(state.scaleX(), state.scaleY(), state.scaleZ());
        }
        if (!isZero(state.bendZ())) {
            poseStack.mulPose(Axis.ZP.rotation(state.bendZ()));
        }
        if (!isZero(state.bendY())) {
            poseStack.mulPose(Axis.YP.rotation(state.bendY()));
        }
        if (!isZero(state.bendX())) {
            poseStack.mulPose(Axis.XP.rotation(state.bendX()));
        }
        poseStack.translate(0.0F, -pivotY, 0.0F);
    }

    private static void applyToCuboid(BendableCuboid cuboid, PalMoreBendState state) {
        cuboid.applyBend(0.0F, state.bendX());
        if (!state.hasExtendedDeformation()) {
            return;
        }

        float pivotX = cuboid.getBendX();
        float pivotY = cuboid.getBendY();
        float pivotZ = cuboid.getBendZ();
        float height = Math.max(EPSILON, cuboid.bendHeight());
        boolean hasTransform = state.hasNonTrivialTransform();

        cuboid.iteratePositions(pos -> {
            Vector3f original = pos.getOriginalPos();
            Vector3f vector = new Vector3f(pos.getPos());
            float factor = bendSegmentFactor(cuboid, original.y, pivotY, height);
            if (hasTransform) {
                applyBendOnlyTransform(vector, factor, pivotX, pivotY, pivotZ, state);
            }
            if (!isZero(state.bendZ())) {
                rotateZ(vector, pivotX, pivotY, pivotZ, state.bendZ() * factor);
            }
            if (!isZero(state.bendY())) {
                rotateY(vector, pivotX, pivotY, pivotZ, state.bendY() * factor);
            }
            pos.setPos(vector);
        });
    }

    private static void restoreNativeBend(ModelPart part) {
        int cuboidCount = ModelPartAccessor.getCuboids(part).size();
        for (int index = 0; index < cuboidCount; index++) {
            Optional<MutableCuboid> optional = ModelPartAccessor.optionalGetCuboid(part, index);
            if (optional.isEmpty()) {
                continue;
            }
            MutableCuboid mutable = optional.get();
            ICuboid mutator = mutable.getMutator("bend");
            if (mutator instanceof BendableCuboid cuboid) {
                cuboid.applyBend(cuboid.getBendAxis(), cuboid.getBend());
            }
        }
    }

    private static float bendSegmentFactor(BendableCuboid cuboid, float y, float hingeY, float height) {
        float halfHeight = Math.max(EPSILON, height * 0.5F);
        float raw = cuboid.getBendDirection() == Direction.DOWN
                ? (hingeY - y) / halfHeight
                : (y - hingeY) / halfHeight;
        return Math.max(0.0F, Math.min(1.0F, raw));
    }

    private static void applyBendOnlyTransform(
            Vector3f vector,
            float factor,
            float pivotX,
            float pivotY,
            float pivotZ,
            PalMoreBendState state
    ) {
        float scaleX = lerp(1.0F, state.scaleX(), factor);
        float scaleY = lerp(1.0F, state.scaleY(), factor);
        float scaleZ = lerp(1.0F, state.scaleZ(), factor);

        vector.x = pivotX + (vector.x - pivotX) * scaleX + state.positionX() * factor;
        vector.y = pivotY + (vector.y - pivotY) * scaleY - state.positionY() * factor;
        vector.z = pivotZ + (vector.z - pivotZ) * scaleZ + state.positionZ() * factor;
    }

    private static float lerp(float start, float end, float delta) {
        return start + (end - start) * delta;
    }

    private static void rotateY(Vector3f vector, float pivotX, float pivotY, float pivotZ, float angle) {
        float x = vector.x - pivotX;
        float z = vector.z - pivotZ;
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);
        vector.x = pivotX + x * cos + z * sin;
        vector.z = pivotZ - x * sin + z * cos;
    }

    private static void rotateZ(Vector3f vector, float pivotX, float pivotY, float pivotZ, float angle) {
        float x = vector.x - pivotX;
        float y = vector.y - pivotY;
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);
        vector.x = pivotX + x * cos - y * sin;
        vector.y = pivotY + x * sin + y * cos;
    }

    private static boolean isZero(float value) {
        return Math.abs(value) <= EPSILON;
    }
}
