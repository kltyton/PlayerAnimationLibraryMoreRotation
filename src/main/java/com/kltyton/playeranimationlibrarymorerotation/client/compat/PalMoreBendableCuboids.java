package com.kltyton.playeranimationlibrarymorerotation.client.compat;

import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendHolder;
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
        BendVectorState previous = LAST_VECTOR_BEND.get(part);

        if (isZero(bendY) && isZero(bendZ) && previous == null) {
            return;
        }

        if (!(part instanceof BendableModelPart bendablePart)) {
            LAST_VECTOR_BEND.remove(part);
            return;
        }

        BendableCube cube = bendablePart.bc$getCuboid(0);
        if (!(cube instanceof ModelPart.Cube modelCube)) {
            LAST_VECTOR_BEND.remove(part);
            return;
        }

        if (isZero(bendY) && isZero(bendZ)) {
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
        float pivotY = cube.getBendY();
        float pivotZ = cube.getBendZ();
        boolean inverted = cube.isBendInverted();
        float minY = Math.min(modelCube.minY, modelCube.maxY);
        float maxY = Math.max(modelCube.minY, modelCube.maxY);
        float height = Math.max(EPSILON, maxY - minY);
        float signedBendY = inverted ? -bendY : bendY;
        float signedBendZ = inverted ? -bendZ : bendZ;

        cuboid.iteratePositions(original -> {
            Vector3f result = nativeX.apply(new Vector3f(original));
            float factor = bendFactor(original.y, minY, height, inverted);
            if (!isZero(signedBendY)) {
                rotateY(result, pivotX, pivotY, pivotZ, signedBendY * factor);
            }
            if (!isZero(signedBendZ)) {
                rotateZ(result, pivotX, pivotY, pivotZ, signedBendZ * factor);
            }
            return result;
        });

        LAST_VECTOR_BEND.put(part, new BendVectorState(bendX, bendY, bendZ));
    }

    public static void clearPartState(ModelPart part) {
        LAST_VECTOR_BEND.remove(part);
    }

    private static void applyNativeBendDirect(BendableCube cube, float bendX) {
        if (cube instanceof BendableCuboid cuboid) {
            cuboid.iteratePositions(BendUtil.getBend(cube, bendX));
        } else {
            cube.applyBend(bendX);
        }
    }

    private static float bendFactor(float y, float minY, float height, boolean inverted) {
        float normalized = Math.clamp((y - minY) / height, 0.0F, 1.0F);
        return inverted ? 1.0F - normalized : normalized;
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

    private record BendVectorState(float bendX, float bendY, float bendZ) {
    }
}
