package com.kltyton.playeranimationlibrarymorerotation.compat;

public interface PalMoreBendHolder {
    float palMore$getBendX();

    float palMore$getBendY();

    float palMore$getBendZ();

    void palMore$setBend(float bendX, float bendY, float bendZ);

    boolean palMore$hasBendVectorOverride();

    void palMore$setBendVectorOverride(boolean active);

    default boolean palMore$hasVectorBend() {
        return Math.abs(palMore$getBendY()) > 1.0E-5F || Math.abs(palMore$getBendZ()) > 1.0E-5F;
    }
}
