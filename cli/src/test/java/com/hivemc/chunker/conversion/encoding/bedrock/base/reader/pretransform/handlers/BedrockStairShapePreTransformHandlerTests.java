package com.hivemc.chunker.conversion.encoding.bedrock.base.reader.pretransform.handlers;

import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn;
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.ChunkerBlockIdentifier;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.VanillaBlockStates;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.FacingDirectionHorizontal;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.Half;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.StairShape;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Expected shapes follow Java's StairBlock#getStairsShape for the same layout.
 */
public class BedrockStairShapePreTransformHandlerTests {
    private static final int X = 8, Y = 64, Z = 8;

    private static ChunkerBlockIdentifier stair(FacingDirectionHorizontal facing, Half half) {
        return new ChunkerBlockIdentifier(ChunkerVanillaBlockType.OAK_STAIRS, Map.of(
                VanillaBlockStates.FACING_HORIZONTAL, facing,
                VanillaBlockStates.HALF, half,
                VanillaBlockStates.STAIR_SHAPE, StairShape.STRAIGHT
        ));
    }

    private static ChunkerBlockIdentifier stair(FacingDirectionHorizontal facing) {
        return stair(facing, Half.BOTTOM);
    }

    private static StairShape shape(ChunkerColumn column) {
        ChunkerBlockIdentifier result = new BedrockStairShapePreTransformHandler().handle(column, Map.of(), X, Y, Z, column.getBlock(X, Y, Z));
        return result.getState(VanillaBlockStates.STAIR_SHAPE);
    }

    private static ChunkerColumn centre(FacingDirectionHorizontal facing) {
        ChunkerColumn column = new ChunkerColumn(new ChunkCoordPair(0, 0));
        column.setBlock(X, Y, Z, stair(facing));
        return column;
    }

    @Test
    public void testLoneStairIsStraight() {
        assertEquals(StairShape.STRAIGHT, shape(centre(FacingDirectionHorizontal.NORTH)));
    }

    @Test
    public void testPerpendicularStairInFrontMakesOuterCorner() {
        ChunkerColumn left = centre(FacingDirectionHorizontal.NORTH);
        left.setBlock(X, Y, Z - 1, stair(FacingDirectionHorizontal.WEST));
        assertEquals(StairShape.OUTER_LEFT, shape(left));

        ChunkerColumn right = centre(FacingDirectionHorizontal.NORTH);
        right.setBlock(X, Y, Z - 1, stair(FacingDirectionHorizontal.EAST));
        assertEquals(StairShape.OUTER_RIGHT, shape(right));
    }

    @Test
    public void testPerpendicularStairBehindMakesInnerCorner() {
        ChunkerColumn left = centre(FacingDirectionHorizontal.NORTH);
        left.setBlock(X, Y, Z + 1, stair(FacingDirectionHorizontal.WEST));
        assertEquals(StairShape.INNER_LEFT, shape(left));

        ChunkerColumn right = centre(FacingDirectionHorizontal.NORTH);
        right.setBlock(X, Y, Z + 1, stair(FacingDirectionHorizontal.EAST));
        assertEquals(StairShape.INNER_RIGHT, shape(right));
    }

    @Test
    public void testOuterCornerChecksTheSideFacingAwayFromTheFrontStair() {
        // The front stair faces east, so Java checks the west side. A stair run
        // there keeps this stair straight; the east side is irrelevant.
        ChunkerColumn blocked = centre(FacingDirectionHorizontal.NORTH);
        blocked.setBlock(X, Y, Z - 1, stair(FacingDirectionHorizontal.EAST));
        blocked.setBlock(X - 1, Y, Z, stair(FacingDirectionHorizontal.NORTH));
        assertEquals(StairShape.STRAIGHT, shape(blocked));

        ChunkerColumn open = centre(FacingDirectionHorizontal.NORTH);
        open.setBlock(X, Y, Z - 1, stair(FacingDirectionHorizontal.EAST));
        open.setBlock(X + 1, Y, Z, stair(FacingDirectionHorizontal.NORTH));
        assertEquals(StairShape.OUTER_RIGHT, shape(open));
    }

    @Test
    public void testInnerCornerChecksTheSideTheBackStairFaces() {
        // The back stair faces east, so Java checks the east side.
        ChunkerColumn blocked = centre(FacingDirectionHorizontal.NORTH);
        blocked.setBlock(X, Y, Z + 1, stair(FacingDirectionHorizontal.EAST));
        blocked.setBlock(X + 1, Y, Z, stair(FacingDirectionHorizontal.NORTH));
        assertEquals(StairShape.STRAIGHT, shape(blocked));

        ChunkerColumn open = centre(FacingDirectionHorizontal.NORTH);
        open.setBlock(X, Y, Z + 1, stair(FacingDirectionHorizontal.EAST));
        open.setBlock(X - 1, Y, Z, stair(FacingDirectionHorizontal.NORTH));
        assertEquals(StairShape.INNER_RIGHT, shape(open));
    }

    @Test
    public void testSideStairWithDifferentHalfDoesNotBlockCorner() {
        ChunkerColumn column = centre(FacingDirectionHorizontal.NORTH);
        column.setBlock(X, Y, Z - 1, stair(FacingDirectionHorizontal.EAST));
        column.setBlock(X - 1, Y, Z, stair(FacingDirectionHorizontal.NORTH, Half.TOP));
        assertEquals(StairShape.OUTER_RIGHT, shape(column));
    }

    @Test
    public void testNeighbourWithDifferentHalfDoesNotConnect() {
        ChunkerColumn column = centre(FacingDirectionHorizontal.NORTH);
        column.setBlock(X, Y, Z - 1, stair(FacingDirectionHorizontal.EAST, Half.TOP));
        assertEquals(StairShape.STRAIGHT, shape(column));
    }
}
