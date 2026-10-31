package com.cerbon.cerbons_api.api.general.particle;

import com.cerbon.cerbons_api.api.static_utilities.RandomUtils;
import com.cerbon.cerbons_api.api.static_utilities.VecUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Function;

public class SimpleParticle extends SingleQuadParticle {
    private final ParticleContext particleContext;
    private final IParticleGeometry particleGeometry;
    private final boolean cycleSprites;

    private Function<SimpleParticle, Float> rotationOverride;
    private Function<SimpleParticle, Vec3> velocityOverride;
    private Function<SimpleParticle, Vec3> positionOverride;
    private Function<Float, Float> scaleOverride;
    private Function<Float, Vec3> colorOverride;
    private Function<Float, Integer> brightnessOverride;
    private Vec3 colorVariation = Vec3.ZERO;

    private float prevRotation = 0f;
    private float rotation = 0f;
    public float ageRatio = 1f;

    public SimpleParticle(ParticleContext particleContext, int particleAge, IParticleGeometry particleGeometry, boolean cycleSprites, boolean doCollision) {
        super(particleContext.level(), particleContext.pos().x(), particleContext.pos().y(), particleContext.pos().z(), null);
        this.particleContext = particleContext;
        this.lifetime = particleAge;
        this.particleGeometry = particleGeometry;
        this.cycleSprites = cycleSprites;
        this.hasPhysics = doCollision;
        this.xd = particleContext.vel().x();
        this.yd = particleContext.vel().y();
        this.zd = particleContext.vel().z();

        if (cycleSprites) setSpriteFromAge(particleContext.spriteSet());
        else setSprite(particleContext.spriteSet().get(this.random));
    }

    @Override
    public @NotNull SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE_TERRAIN;
    }

    public @NotNull Vec3 getPos() {
        return new Vec3(x, y, z);
    }

    public int getAge() {
        return age;
    }

    @Override
    public void tick() {
        super.tick();
        if (!isAlive()) return;

        if (cycleSprites) setSpriteFromAge(particleContext.spriteSet());
        ageRatio = (float) age / lifetime;
        setColorFromOverride(colorOverride, ageRatio);
        setScaleFromOverride(scaleOverride, ageRatio);
        setVelocityFromOverride(velocityOverride);
        setPositionFromOverride(positionOverride);
        setRotationFromOverride(rotationOverride);
    }

    private void setRotationFromOverride(Function<SimpleParticle, Float> rotationOverride) {
        if (rotationOverride == null) return;

        float rot = rotationOverride.apply(this);
        prevRotation = rotation;
        rotation = rot;
    }

    private void setVelocityFromOverride(Function<SimpleParticle, Vec3> velocityOverride) {
        if (velocityOverride == null) return;

        Vec3 velocity = velocityOverride.apply(this);
        xd = velocity.x();
        yd = velocity.y();
        zd = velocity.z();
    }

    private void setPositionFromOverride(Function<SimpleParticle, Vec3> positionOverride) {
        if (positionOverride == null) return;

        Vec3 pos = positionOverride.apply(this);
        setPos(pos.x(), pos.y(), pos.z());
    }

    private void setScaleFromOverride(Function<Float, Float> scaleOverride, float ageRatio) {
        if (scaleOverride == null) return;

        quadSize = scaleOverride.apply(ageRatio);
        setSize(0.2f * quadSize, 0.2f * quadSize);
    }

    private void setColorFromOverride(Function<Float, Vec3> colorOverride, float ageRatio) {
        if (colorOverride == null) return;

        Vec3 color = colorOverride.apply(ageRatio);
        Vec3 variedColor = VecUtils.coerceAtMost(VecUtils.coerceAtLeast(color.add(colorVariation), Vec3.ZERO), VecUtils.unit);
        setColor((float) variedColor.x(), (float) variedColor.y(), (float) variedColor.z());
    }

    public void setBrightnessOverride(Function<Float, Integer> override) {
        brightnessOverride = override;
    }

    public void setColorOverride(Function<Float, Vec3> override) {
        colorOverride = override;
        setColorFromOverride(override, 0f);
    }

    public void setScaleOverride(Function<Float, Float> override) {
        scaleOverride = override;
        setScaleFromOverride(override, 0f);
    }

    public void setColorVariation(double variation) {
        colorVariation = RandomUtils.randVec().scale(variation);
        setColorFromOverride(colorOverride, 0f);
    }

    public void setVelocityOverride(Function<SimpleParticle, Vec3> override) {
        velocityOverride = override;
    }

    public void setPositionOverride(Function<SimpleParticle, Vec3> override) {
        positionOverride = override;
    }

    public void setRotationOverride(Function<SimpleParticle, Float> override) {
        rotationOverride = override;

        if (rotationOverride != null) {
            rotation = rotationOverride.apply(this);
            prevRotation = rotationOverride.apply(this);
        }
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return brightnessOverride != null ? brightnessOverride.apply(ageRatio) : super.getLightCoords(partialTick);
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTicks) {
        // The 26.1.2 particle pipeline submits quads as (center, quaternion, scale) instead of
        // letting the particle build the quad directly. Derive those values from the corners
        // produced by the custom geometry so arbitrary geometries keep working.
        Vector3f[] corners = particleGeometry.getGeometry(
                camera,
                partialTicks,
                xo, yo, zo,
                x, y, z,
                getQuadSize(partialTicks),
                Mth.lerp(partialTicks, prevRotation, rotation)
        );

        Vector3f center = new Vector3f(corners[0]).add(corners[2]).mul(0.5f);
        Vector3f right = new Vector3f(corners[0]).sub(corners[3]).mul(0.5f);
        Vector3f up = new Vector3f(corners[1]).sub(corners[0]).mul(0.5f);
        Vector3f normal = new Vector3f(right).cross(up);

        Matrix3f basis = new Matrix3f();
        basis.setColumn(0, right);
        basis.setColumn(1, up);
        basis.setColumn(2, normal);

        Quaternionf rotationQuaternion = new Quaternionf().setFromUnnormalized(basis);

        state.add(
                getLayer(),
                center.x(),
                center.y(),
                center.z(),
                rotationQuaternion.x,
                rotationQuaternion.y,
                rotationQuaternion.z,
                rotationQuaternion.w,
                1.0F,
                getU0(),
                getU1(),
                getV0(),
                getV1(),
                ARGB.colorFromFloat(alpha, rCol, gCol, bCol),
                getLightCoords(partialTicks)
        );
    }
}
