package com.kltyton.playeranimationlibrarymorerotation.client.compat;

import com.kltyton.playeranimationlibrarymorerotation.client.compat.PalMoreTrack.VectorTrack;
import com.kltyton.playeranimationlibrarymorerotation.compat.PalMoreBendState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Sidecar animation channels parsed from Bedrock/PAL JSON that playerAnimator 1.20.1 does not natively store:
 * 3-axis bend rotation, bend-local position/scale, per-bone scale, and effect timelines.
 */
public final class PalMoreAnimationExtras {
    public static final PalMoreAnimationExtras EMPTY = new PalMoreAnimationExtras(
            Map.of(), Map.of(), Map.of(), Map.of(), PalMoreEffectTimeline.EMPTY
    );

    private final Map<String, VectorTrack> bendRotationTracks;
    private final Map<String, VectorTrack> bendPositionTracks;
    private final Map<String, VectorTrack> bendScaleTracks;
    private final Map<String, VectorTrack> boneScaleTracks;
    private final PalMoreEffectTimeline effectTimeline;

    public PalMoreAnimationExtras(
            Map<String, VectorTrack> bendRotationTracks,
            Map<String, VectorTrack> bendPositionTracks,
            Map<String, VectorTrack> bendScaleTracks,
            Map<String, VectorTrack> boneScaleTracks,
            PalMoreEffectTimeline effectTimeline
    ) {
        this.bendRotationTracks = bendRotationTracks;
        this.bendPositionTracks = bendPositionTracks;
        this.bendScaleTracks = bendScaleTracks;
        this.boneScaleTracks = boneScaleTracks;
        this.effectTimeline = effectTimeline;
    }

    public boolean isEmpty() {
        return this.bendRotationTracks.isEmpty()
                && this.bendPositionTracks.isEmpty()
                && this.bendScaleTracks.isEmpty()
                && this.boneScaleTracks.isEmpty()
                && this.effectTimeline.isEmpty();
    }

    public PalMoreEffectTimeline effectTimeline() {
        return this.effectTimeline;
    }

    public PalMoreBendState sampleBend(String boneName, float tick) {
        VectorTrack rotTrack = this.bendRotationTracks.get(boneName);
        VectorTrack posTrack = this.bendPositionTracks.get(boneName);
        VectorTrack scaleTrack = this.bendScaleTracks.get(boneName);

        boolean hasRot = rotTrack != null && rotTrack.hasKeyframes();
        boolean hasTransform = (posTrack != null && posTrack.hasKeyframes())
                || (scaleTrack != null && scaleTrack.hasKeyframes());
        if (!hasRot && !hasTransform) {
            return PalMoreBendState.NONE;
        }

        Vector3f rot = hasRot ? rotTrack.sample(tick, 0.0F) : new Vector3f(0.0F, 0.0F, 0.0F);
        Vector3f pos = posTrack != null && posTrack.hasKeyframes()
                ? posTrack.sample(tick, 0.0F)
                : new Vector3f(0.0F, 0.0F, 0.0F);
        Vector3f scale = scaleTrack != null && scaleTrack.hasKeyframes()
                ? scaleTrack.sample(tick, 1.0F)
                : new Vector3f(1.0F, 1.0F, 1.0F);

        return new PalMoreBendState(
                rot.x, rot.y, rot.z, hasRot,
                pos.x, pos.y, pos.z,
                scale.x, scale.y, scale.z,
                hasTransform
        );
    }

    public @Nullable Vector3f sampleBoneScale(String boneName, float tick) {
        VectorTrack scaleTrack = this.boneScaleTracks.get(boneName);
        if (scaleTrack == null || !scaleTrack.hasKeyframes()) {
            return null;
        }
        return scaleTrack.sample(tick, 1.0F);
    }
}
