package com.cerbon.cerbons_api.mixin.multipart_entities.client;

import com.cerbon.cerbons_api.api.multipart_entities.entity.MultipartAwareEntity;
import com.cerbon.cerbons_api.api.multipart_entities.util.CompoundOrientedBox;
import com.cerbon.cerbons_api.api.multipart_entities.util.Matrix3d;
import com.cerbon.cerbons_api.api.multipart_entities.util.OrientedBox;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the old EntityRenderDispatcherMixin: EntityRenderDispatcher#renderHitbox and
 * LevelRenderer#renderLineBox no longer exist in 26.1.2, debug geometry is now drawn with
 * the Gizmos system from EntityHitboxDebugRenderer.
 */
@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {

    private static final int ORIENTED_BOX_COLOR = 0xFF3366FF;   // blue outline
    private static final int VOXEL_BOX_COLOR = 0xFF33FF66;      // green outline

    // Vertex index bit layout: bit0 = +x, bit1 = +y, bit2 = +z
    private static final int[][] CUBE_EDGES = {
            {0, 1}, {0, 2}, {0, 4},
            {1, 3}, {1, 5},
            {2, 3}, {2, 6},
            {3, 7},
            {4, 5}, {4, 6},
            {5, 7},
            {6, 7}
    };

    @Inject(method = "showHitboxes", at = @At("TAIL"))
    private void cerbons_api$drawOrientedBoxes(Entity entity, float partialTicks, boolean isServerEntity, CallbackInfo ci) {
        if (!(entity.getBoundingBox() instanceof final CompoundOrientedBox compoundOrientedBox)) return;
        if (!(entity instanceof MultipartAwareEntity)) return;

        final Vec3 origin = entity.getPosition(partialTicks);
        final int obColor = ORIENTED_BOX_COLOR;
        final int voxelColor = VOXEL_BOX_COLOR;

        for (final OrientedBox orientedBox : compoundOrientedBox) {
            drawOrientedBox(orientedBox, origin, obColor);
        }

        compoundOrientedBox.toVoxelShape().forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                Gizmos.cuboid(new AABB(minX, minY, minZ, maxX, maxY, maxZ).move(origin), GizmoStyle.stroke(voxelColor)));
    }

    private static void drawOrientedBox(OrientedBox orientedBox, Vec3 origin, int argb) {
        final Vec3 center = origin.add(orientedBox.getCenter());
        final AABB extents = orientedBox.getExtents();
        final Matrix3d matrix = orientedBox.getMatrix();

        final double[] xs = {extents.minX, extents.maxX};
        final double[] ys = {extents.minY, extents.maxY};
        final double[] zs = {extents.minZ, extents.maxZ};

        final Vec3[] corners = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            final double x = xs[(i & 1)];
            final double y = ys[(i >> 1) & 1];
            final double z = zs[(i >> 2) & 1];
            corners[i] = center.add(matrix.transformX(x, y, z), matrix.transformY(x, y, z), matrix.transformZ(x, y, z));
        }

        for (final int[] edge : CUBE_EDGES) {
            Gizmos.line(corners[edge[0]], corners[edge[1]], argb);
        }
    }
}
