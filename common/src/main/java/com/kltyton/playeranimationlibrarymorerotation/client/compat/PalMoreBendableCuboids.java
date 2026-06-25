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
        float bendX = bend.palMore$getBendX();
        float bendY = bend.palMore$getBendY();
        float bendZ = bend.palMore$getBendZ();
        boolean hasTransform = hasBendTransform(bend);
        BendVectorState previous = LAST_VECTOR_BEND.get(part);

        if (isZero(bendY) && isZero(bendZ) && !hasTransform && previous == null) {
            return;
        }

        if (!((Object) part instanceof BendableModelPart bendablePart)) {
            LAST_VECTOR_BEND.remove(part);
            return;
        }

        BendableCube cube = bendablePart.bc$getCuboid(0);
        if (!(cube instanceof ModelPart.Cube modelCube)) {
            LAST_VECTOR_BEND.remove(part);
            return;
        }

        if (isZero(bendY) && isZero(bendZ) && !hasTransform) {
            applyNativeBendDirect(cube, bendX);
            LAST_VECTOR_BEND.remove(part);
            return;
        }

        if (!(cube instanceof BendableCuboid cuboid)) {
            cube.applyBend(bendX);
            LAST_VECTOR_BEND.remove(part);
            return;
        }

        Function<Vector3f, Vector3f> nativeX = BendUtil.getBend(cube, bendX);
        float pivotX = cube.getBendX();
        float pivotZ = cube.getBendZ();
        float minY = Math.min(modelCube.minY, modelCube.maxY);
        float maxY = Math.max(modelCube.minY, modelCube.maxY);
        float height = Math.max(EPSILON, maxY - minY);
        float hingeY = minY + height * 0.5F;
        boolean inverted = cube.isBendInverted();
        float signedBendY = inverted ? -bendY : bendY;
        float signedBendZ = inverted ? -bendZ : bendZ;

        cuboid.iteratePositions(original -> {
            Vector3f result = nativeX.apply(new Vector3f(original));
            float lowerSegmentFactor = bendLowerSegmentFactor(original.y, hingeY, height);
            if (!isZero(signedBendY)) {
                rotateY(result, pivotX, hingeY, pivotZ, signedBendY * lowerSegmentFactor);
            }
            if (!isZero(signedBendZ)) {
                rotateZ(result, pivotX, hingeY, pivotZ, signedBendZ * lowerSegmentFactor);
            }
            if (hasTransform) {
                applyBendOnlyTransform(result, lowerSegmentFactor, pivotX, hingeY, pivotZ, bend);
            }
            return result;
        });

        LAST_VECTOR_BEND.put(part, new BendVectorState(
                bendX,
                bendY,
                bendZ,
                bend.palMore$getBendPositionX(),
                bend.palMore$getBendPositionY(),
                bend.palMore$getBendPositionZ(),
                bend.palMore$getBendScaleX(),
                bend.palMore$getBendScaleY(),
                bend.palMore$getBendScaleZ()
        ));
    }

    public static void clearPartState(ModelPart part) {
        LAST_VECTOR_BEND.remove(part);
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

    private static void applyBendOnlyTransform(Vector3f vector, float factor, float pivotX, float pivotY, float pivotZ, PalMoreBendHolder bend) {
        float scaleX = lerp(1.0F, bend.palMore$getBendScaleX(), factor);
        float scaleY = lerp(1.0F, bend.palMore$getBendScaleY(), factor);
        float scaleZ = lerp(1.0F, bend.palMore$getBendScaleZ(), factor);

        vector.x = pivotX + (vector.x - pivotX) * scaleX + bend.palMore$getBendPositionX() * factor;
        vector.y = pivotY + (vector.y - pivotY) * scaleY - bend.palMore$getBendPositionY() * factor;
        vector.z = pivotZ + (vector.z - pivotZ) * scaleZ + bend.palMore$getBendPositionZ() * factor;
    }

    private static boolean hasBendTransform(PalMoreBendHolder bend) {
        return bend.palMore$hasBendTransformOverride()
                && (!isZero(bend.palMore$getBendPositionX())
                || !isZero(bend.palMore$getBendPositionY())
                || !isZero(bend.palMore$getBendPositionZ())
                || !isZero(bend.palMore$getBendScaleX() - 1.0F)
                || !isZero(bend.palMore$getBendScaleY() - 1.0F)
                || !isZero(bend.palMore$getBendScaleZ() - 1.0F));
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
            float scaleZ
    ) {
    }
}
