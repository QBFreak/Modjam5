package team.thegoldenhoe.cameraobscura.client.renderers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mia.craftstudio.libgdx.Vector3;
import com.mia.craftstudio.minecraft.client.CSClientModelWrapperVBO;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;
import team.thegoldenhoe.cameraobscura.CSModelMetadata;
import team.thegoldenhoe.cameraobscura.utils.ModelHandler;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.List;

public class RenderPropInv implements IBakedModel {
    protected static final List<BakedQuad> dummyList = ImmutableList.of();// Collections.emptyList();
    protected static final ItemOverrideList overrides = new ItemOverrideList(Lists.<ItemOverride>newArrayList()) {
        @Override
        public IBakedModel handleItemState(final IBakedModel originalModel, final ItemStack stack, final World world, final EntityLivingBase entity) {
            ((RenderPropInv) originalModel).modelID = stack.getItemDamage();
            return originalModel;
        }
    };
    protected int modelID = 0;
    protected boolean disableRender = false;
    protected TransformType lastTransformType;

    @Override
    public List<BakedQuad> getQuads(@Nullable final IBlockState state, @Nullable final EnumFacing side, final long rand) {
        //TODO : We do our special rendering here (call for the special renderer ?)
        if (side == null && !disableRender) {
            GL11.glPushMatrix();
            final CSModelMetadata modelData = ModelHandler.getModelByID(modelID);
            GL11.glTranslatef(1.0F, 1.0F, 1.0F);
            GL11.glTranslatef(modelData.itemOffset.x, modelData.itemOffset.y, modelData.itemOffset.z);
            GL11.glScalef(modelData.itemScale, modelData.itemScale, modelData.itemScale);

            final int modelRotIndex = lastTransformType == TransformType.GUI ? modelData.rotInventory : modelData.rotHeld;

            ((CSClientModelWrapperVBO) modelData.wrapper).render(null, 0f, 2, false, modelRotIndex * 22.5f, Vector3.invY, Vector3.Zero, Vector3.negHalfXZ);
            GL11.glPopMatrix();
        }
        return dummyList;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getTexture(Blocks.SOUL_SAND.getDefaultState());
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return ItemCameraTransforms.DEFAULT;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return overrides;
    }

    private static final Matrix4f matrixGround;
    private static final Matrix4f matrixThird;
    private static final Matrix4f matrixFirst;
    private static final Matrix4f matrixGui;

    static {
        matrixGround = convertMatrix(new org.lwjgl.util.vector.Matrix4f(ModelRotation.X0_Y0.getMatrix4d()));
        org.lwjgl.util.vector.Matrix4f transformMatrix = new org.lwjgl.util.vector.Matrix4f();
        transformMatrix.scale(new Vector3f(0.25f, 0.25f, 0.25f));
        matrixGround.mul(TRSRTransformation.toVecmath(transformMatrix));

        matrixThird = convertMatrix(new org.lwjgl.util.vector.Matrix4f(ModelRotation.X0_Y0.getMatrix4d()));
        transformMatrix = new org.lwjgl.util.vector.Matrix4f();
        transformMatrix.rotate(75f * (float) Math.PI / 180, new Vector3f(1f, 0f, 0f));
        transformMatrix.rotate(-45f * (float) Math.PI / 180, new Vector3f(0f, 1f, 0f));
        transformMatrix.scale(new Vector3f(0.35f, 0.35f, 0.35f));
        transformMatrix.translate(new Vector3f(-0.25f, 0f, -0.25f));
        matrixThird.mul(TRSRTransformation.toVecmath(transformMatrix));

        matrixFirst = convertMatrix(new org.lwjgl.util.vector.Matrix4f(ModelRotation.X0_Y0.getMatrix4d()));
        transformMatrix = new org.lwjgl.util.vector.Matrix4f();
        transformMatrix.rotate(-45f * (float) Math.PI / 180, new Vector3f(0f, 1f, 0f));
        transformMatrix.scale(new Vector3f(0.4f, 0.4f, 0.4f));
        matrixFirst.mul(TRSRTransformation.toVecmath(transformMatrix));

        matrixGui = convertMatrix(new org.lwjgl.util.vector.Matrix4f(ModelRotation.X0_Y0.getMatrix4d()));
        transformMatrix = new org.lwjgl.util.vector.Matrix4f();
        transformMatrix.rotate(30f * (float) Math.PI / 180, new Vector3f(1f, 0f, 0f));
        transformMatrix.rotate(-45f * (float) Math.PI / 180, new Vector3f(0f, 1f, 0f));
        transformMatrix.scale(new Vector3f(0.625f, 0.625f, 0.625f));
        matrixGui.mul(TRSRTransformation.toVecmath(transformMatrix));
    }

    // Adapted from net.minecraftforge.client.ForgeHooksClient.getMatrix(ModelRotation)
    protected static Matrix4f convertMatrix(final org.lwjgl.util.vector.Matrix4f inMatrix) {
        final Matrix4f ret = new Matrix4f(TRSRTransformation.toVecmath(inMatrix));
        final Matrix4f tmp = new Matrix4f();
        tmp.setIdentity();
        tmp.m03 = tmp.m13 = tmp.m23 = .5f;
        ret.mul(tmp, ret);
        tmp.invert();
        ret.mul(tmp);
        return ret;
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(final TransformType cameraTransformType) {
        lastTransformType = cameraTransformType;

        if (cameraTransformType == TransformType.GROUND) {
            return Pair.of(this, matrixGround);
        } else if (cameraTransformType == TransformType.THIRD_PERSON_RIGHT_HAND) {
            return Pair.of(this, matrixThird);
        } else if (cameraTransformType == TransformType.FIRST_PERSON_RIGHT_HAND) {
            return Pair.of(this, matrixFirst);
        } else if (cameraTransformType == TransformType.THIRD_PERSON_LEFT_HAND) {
            return Pair.of(this, matrixThird);
        } else if (cameraTransformType == TransformType.FIRST_PERSON_LEFT_HAND) {
            return Pair.of(this, matrixFirst);
        } else if (cameraTransformType == TransformType.GUI) {
            return Pair.of(this, matrixGui);
        } else {
            return Pair.of(this, null);
        }
    }
}
