package io.github.sunshinewzy.shining.core.machine.structure;

import org.bukkit.Axis;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Wall;
import org.bukkit.block.data.type.Wall.Height;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import taboolib.module.nms.MinecraftVersion;

/**
 * Used to rotate blocks and block sections when building or testing for the presence of a MultiBlockStructure
 * @author Redempt
 */
public class Rotator {
	
	private static final BlockFace[] BLOCK_FACES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
	
	@SuppressWarnings("SameParameterValue")
	private static <T> int indexOf(T[] arr, T key) {
		for (int i = 0; i < arr.length; i++) {
			if (arr[i].equals(key)) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Rotates a BlockFace according to given values
	 * @param face The BlockFace to rotate
	 * @param rotation The number of clockwise rotations to apply
	 * @param mirror Whether to mirror over the X axis
	 * @return The rotated BlockFace
	 */
	public static BlockFace rotateBlockFace(BlockFace face, int rotation, boolean mirror) {
		rotation %= 4;
		if (rotation < 0) {
			rotation += 4;
		}
		int ind = indexOf(BLOCK_FACES, face);
		if (ind == -1) {
			return face;
		}
		if (mirror && (ind == 1 || ind == 3)) {
			ind = ind + 2;
		}
		ind = (ind + rotation) % 4;
		return BLOCK_FACES[ind];
	}
	
	/**
	 * Rotates a BlockFace according to given values
	 * @param face The BlockFace to rotate
	 * @param rotation The number of clockwise rotations to apply
	 * @return The rotated BlockFace
	 */
	public static BlockFace rotateBlockFace(BlockFace face, int rotation) {
		return rotateBlockFace(face, rotation, false);
	}
	
	public static int calculateRotation(BlockFace from, BlockFace to) {
		int fromIndex = indexOf(BLOCK_FACES, from);
		if (fromIndex == -1) return 0;
		int toIndex = indexOf(BLOCK_FACES, to);
		if (toIndex == -1) return 0;
		
		int rotation = toIndex - fromIndex;
		while (rotation < 0) {
			rotation += 4;
		}
		return rotation;
	}
	
	private int rotation;
	private boolean mirrored;
	private int x = 0;
	private int z = 0;
	
	/**
	 * Constructs a new Rotator
	 * @param rotation The number of 90-degree clockwise rotations this Rotator applies
	 * @param mirrored Whether this Rotator should mirror over the X axis
	 */
	public Rotator(int rotation, boolean mirrored) {
		while (rotation < 0) {
			rotation += 4;
		}
		this.rotation = rotation % 4;
		this.mirrored = mirrored;
	}
	
	public Rotator(@NotNull BlockFace from, @NotNull BlockFace to, boolean mirrored) {
		this(calculateRotation(from, to), mirrored);
	}
	
	public Rotator(@NotNull BlockFace from, @NotNull BlockFace to) {
		this(from, to, false);
	}
	
	/**
	 * Rotates block data. NOTE: Only works for 1.13+
	 * @param data The block data to rotate
	 * @return The rotated block data
	 */
	public BlockData rotate(BlockData data) {
		data = data.clone();
		if (data instanceof Directional) {
			Directional d = (Directional) data;
			BlockFace face = rotateBlockFace(d.getFacing());
			d.setFacing(face);
		}
		if (data instanceof MultipleFacing) {
			MultipleFacing d = (MultipleFacing) data;
			Boolean[] directions = new Boolean[4];
			for (int i = 0; i < 4; i++) {
				directions[i] = d.hasFace(BLOCK_FACES[i]);
			}
			rotate(directions);
			for (int i = 0; i < 4; i++) {
				d.setFace(BLOCK_FACES[i], directions[i]);
			}
		}
		if (data instanceof Orientable) {
			Orientable d = (Orientable) data;
			if (rotation % 2 != 0 && d.getAxis() != Axis.Y) {
				d.setAxis(d.getAxis() == Axis.X ? Axis.Z : Axis.X);
			}
		}
		if (MinecraftVersion.INSTANCE.getMajor() >= MinecraftVersion.V1_16 && data instanceof Wall) {
			Wall d = (Wall) data;
			Height[] heights = new Height[4];
			for (int i = 0; i < 4; i++) {
				heights[i] = d.getHeight(BLOCK_FACES[i]);
			}
			rotate(heights);
			for (int i = 0; i < 4; i++) {
				d.setHeight(BLOCK_FACES[i], heights[i]);
			}
		}
		return data;
	}
	
	@SuppressWarnings("unchecked")
	private <T> void rotate(T[] arr) {
		Object[] rot = new Object[4];
		System.arraycopy(arr, 0, rot, 0, 4);
		for (int i = 0; i < 4; i++) {
			int dir = (i + 4 - rotation) % 4;
			arr[i] = (T) rot[dir];
		}
		if (mirrored) {
			int first = rotation % 2 == 0 ? 1 : 0;
			int second = rotation % 2 == 0 ? 3 : 2;
			T tmp = arr[first];
			arr[first] = arr[second];
			arr[second] = tmp;
		}
	}
	
	/**
	 * Rotates a BlockFace according to this Rotator
	 * @param face The BlockFace to rotate
	 * @return The rotated BlockFace
	 */
	public BlockFace rotateBlockFace(BlockFace face) {
		return rotateBlockFace(face, rotation, mirrored);
	}
	
	/**
	 * Rotates a Vector according to this Rotator
	 * @param vector The Vector to rotate
	 * @return The rotated Vector
	 */
	public Vector rotateVector(Vector vector) {
		setLocation(vector.getBlockX(), vector.getBlockZ());
		return new Vector(getRotatedX(), vector.getY(), getRotatedZ());
	}
	
	/**
	 * Sets the relative coordinates this Rotator will rotate
	 * @param x The relative X coordinate
	 * @param z The relative Z coordinate
	 */
	public void setLocation(int x, int z) {
		this.x = mirrored ? -x : x;
		this.z = z;
	}
	
	/**
	 * @return The rotated relative X
	 */
	public int getRotatedX() {
		switch (rotation) {
			case 0:
				return x;
			case 1:
				return -z;
			case 2:
				return -x;
			case 3:
				return z;
		}
		return 0;
	}
	
	/**
	 * @return The rotated relative Z
	 */
	public int getRotatedZ() {
		switch (rotation) {
			case 0:
				return z;
			case 1:
				return x;
			case 2:
				return -z;
			case 3:
				return -x;
		}
		return 0;
	}
	
	/**
	 * Gets a Rotator which will negate the operations of this Rotator
	 * @return The inverse Rotator
	 */
	public Rotator getInverse() {
		return new Rotator(-rotation, mirrored);
	}
	
	/**
	 * Gets a clone of this Rotator
	 * @return The clone of this Rotator
	 */
	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public Rotator clone() {
		return new Rotator(rotation, mirrored);
	}
	
	/**
	 * Gets the rotation, in number of 90-degree clockwise rotations
	 * @return The rotation
	 */
	public int getRotation() {
		return rotation;
	}
	
	/**
	 * Sets the rotation
	 * @param rotation The rotation to set
	 */
	public void setRotation(int rotation) {
		this.rotation = rotation % 4;
	}
	
	/**
	 * Sets whether this rotator mirrors over the X axis
	 * @param mirrored Whether this rotator mirrors over the X axis
	 */
	public void setMirrored(boolean mirrored) {
		this.mirrored = mirrored;
	}
	
	/**
	 * Gets whether this rotator mirrors over the X axis
	 * @return Whether this rotator mirrors over the X axis
	 */
	public boolean isMirrored() {
		return mirrored;
	}
	
}