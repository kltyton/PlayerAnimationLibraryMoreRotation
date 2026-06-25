package com.kltyton.playeranimationlibrarymorerotation.client.compat;

import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendHolder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zigythebird.bendable_cuboids.api.BendableCube;
import com.zigythebird.bendable_cuboids.api.BendableModelPart;
import com.zigythebird.bendable_cuboids.impl.BendUtil;
import com.zigythebird.bendable_cuboids.impl.BendableCuboid;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

public final class PalMoreBendableCuboids {
    private static final float EPSILON = 1.0E-5F;
    private static final Map<ModelPart, BendVectorState> LAST_VECTOR_BEND = Collections.synchronizedMap(new WeakHashMap<>());

    private PalMoreBendableCuboids() {
    }

    public static void applyVectorBend(ModelPart part, PalMoreBendHolder bend) {
        BendVectorState state = BendVectorState.from(bend);
        boolean hasExtendedBend = hasExtendedBend(state);
        BendVectorState previous = LAST_VECTOR_BEND.get(part);

        if (!hasExtendedBend && previous == null) {
            return;
        }

        if (!((Object) part instanceof BendableModelPart bendablePart)) {
            LAST_VECTOR_BEND.remove(part);
            return;
        }

        if (!hasExtendedBend) {
            resetNativeBend(bendablePart, state.bendX());
            LAST_VECTOR_BEND.remove(part);
            return;
        }

        if (state.equals(previous)) {
            return;
        }

        int appliedCount = applyExtendedBend(bendablePart, state);
        if (appliedCount == 0) {
            LAST_VECTOR_BEND.remove(part);
            return;
        }

        LAST_VECTOR_BEND.put(part, state);
    }

    private static int resetNativeBend(BendableModelPart part, float bendX) {
        int count = 0;
        for (int index = 0; ; index++) {
            BendableCube cube = part.bc$getCuboid(index);
            if (cube == null) {
                break;
            }
            applyNativeBendDirect(cube, bendX);
            count++;
        }
        return count;
    }

    private static int applyExtendedBend(BendableModelPart part, BendVectorState state) {
        int count = 0;
        for (int index = 0; ; index++) {
            BendableCube cube = part.bc$getCuboid(index);
            if (cube == null) {
                break;
            }
            applyExtendedBend(cube, state);
            count++;
        }
        return count;
    }

    private static void applyExtendedBend(BendableCube cube, BendVectorState state) {
        if (!(cube instanceof BendableCuboid cuboid)) {
            cube.applyBend(state.bendX());
            return;
        }

        Function<Vector3f, Vector3f> nativeBend = BendUtil.getBend(cube, state.bendX());
        float pivotX = cube.getBendX();
        float pivotY = cube.getBendY();
        float pivotZ = cube.getBendZ();
        float height = Math.max(EPSILON, cube.bendHeight());

        cuboid.iteratePositions(original -> {
            Vector3f vector = nativeBend.apply(original);
            float factor = bendLowerSegmentFactor(original.y, pivotY, height);
            if (hasTransform(state)) {
                applyBendOnlyTransform(vector, factor, pivotX, pivotY, pivotZ, state);
            }
            if (!isZero(state.bendZ())) {
                rotateZ(vector, pivotX, pivotY, pivotZ, -state.bendZ() * factor);
            }
            if (!isZero(state.bendY())) {
                rotateY(vector, pivotX, pivotY, pivotZ, -state.bendY() * factor);
            }
            return vector;
        });
    }

    public static void clearPartState(ModelPart part) {
        BendVectorState previous = LAST_VECTOR_BEND.remove(part);
        if (previous != null && (Object) part instanceof BendableModelPart bendablePart) {
            resetNativeBend(bendablePart, 0.0F);
        }
    }

    public static void applyArmItemBend(PoseStack poseStack, PalMoreBendHolder bend) {
        float bendX = bend.palMore$getBendX();
        boolean hasBendY = !isZero(bend.palMore$getBendY());
        boolean hasBendZ = !isZero(bend.palMore$getBendZ());
        boolean hasTransform = hasBendTransform(bend);
        if (!hasBendY && !hasBendZ && !hasTransform) {
            return;
        }

        float pivotY = 0.25F;
        if (!isZero(bendX)) {
            poseStack.translate(0.0F, pivotY, 0.0F);
            poseStack.mulPose(Axis.XP.rotation(-bendX));
            poseStack.translate(0.0F, -pivotY, 0.0F);
        }

        if (hasTransform) {
            poseStack.translate(
                    bend.palMore$getBendPositionX() / 16.0F,
                    -bend.palMore$getBendPositionY() / 16.0F,
                    bend.palMore$getBendPositionZ() / 16.0F
            );
        }

        poseStack.translate(0.0F, pivotY, 0.0F);
        if (hasTransform) {
            poseStack.scale(bend.palMore$getBendScaleX(), bend.palMore$getBendScaleY(), bend.palMore$getBendScaleZ());
        }
        if (hasBendZ) {
            poseStack.mulPose(Axis.ZP.rotation(-bend.palMore$getBendZ()));
        }
        if (hasBendY) {
            poseStack.mulPose(Axis.YP.rotation(-bend.palMore$getBendY()));
        }
        if (!isZero(bendX)) {
            poseStack.mulPose(Axis.XP.rotation(bendX));
        }
        poseStack.translate(0.0F, -pivotY, 0.0F);
    }

    private static void applyNativeBendDirect(BendableCube cube, float bendX) {
        if (cube instanceof BendableCuboid cuboid) {
            cuboid.iteratePositions(BendUtil.getBend(cube, bendX));
        } else {
            cube.applyBend(bendX);
        }
    }

    private static float bendLowerSegmentFactor(float y, float hingeY, float height) {
        float halfHeight = Math.max(EPSILON, height * 0.5F);
        return Math.clamp((y - hingeY) / halfHeight, 0.0F, 1.0F);
    }

    private static void applyBendOnlyTransform(Vector3f vector, float factor, float pivotX, float pivotY, float pivotZ, BendVectorState state) {
        float scaleX = lerp(1.0F, state.scaleX(), factor);
        float scaleY = lerp(1.0F, state.scaleY(), factor);
        float scaleZ = lerp(1.0F, state.scaleZ(), factor);

        vector.x = pivotX + (vector.x - pivotX) * scaleX + state.positionX() * factor;
        vector.y = pivotY + (vector.y - pivotY) * scaleY - state.positionY() * factor;
        vector.z = pivotZ + (vector.z - pivotZ) * scaleZ + state.positionZ() * factor;
    }

    private static boolean hasBendTransform(PalMoreBendHolder bend) {
        return hasTransform(BendVectorState.from(bend));
    }

    private static boolean hasExtendedBend(BendVectorState state) {
        return !isZero(state.bendY()) || !isZero(state.bendZ()) || hasTransform(state);
    }

    private static boolean hasTransform(BendVectorState state) {
        return state.transformOverride()
                && (!isZero(state.positionX())
                || !isZero(state.positionY())
                || !isZero(state.positionZ())
                || !isZero(state.scaleX() - 1.0F)
                || !isZero(state.scaleY() - 1.0F)
                || !isZero(state.scaleZ() - 1.0F));
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

    private record BendVectorState(
            float bendX,
            float bendY,
            float bendZ,
            float positionX,
            float positionY,
            float positionZ,
            float scaleX,
            float scaleY,
            float scaleZ,
            boolean transformOverride
    ) {
        static BendVectorState from(PalMoreBendHolder bend) {
            return new BendVectorState(
                    bend.palMore$getBendX(),
                    bend.palMore$getBendY(),
                    bend.palMore$getBendZ(),
                    bend.palMore$getBendPositionX(),
                    bend.palMore$getBendPositionY(),
                    bend.palMore$getBendPositionZ(),
                    bend.palMore$getBendScaleX(),
                    bend.palMore$getBendScaleY(),
                    bend.palMore$getBendScaleZ(),
                    bend.palMore$hasBendTransformOverride()
            );
        }
    }
}
