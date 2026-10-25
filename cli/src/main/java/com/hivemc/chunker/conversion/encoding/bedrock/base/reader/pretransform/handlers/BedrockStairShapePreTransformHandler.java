package com.hivemc.chunker.conversion.encoding.bedrock.base.reader.pretransform.handlers;

import com.hivemc.chunker.conversion.handlers.pretransform.Edge;
import com.hivemc.chunker.conversion.handlers.pretransform.manager.handler.block.BlockPreTransformHandler;
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.ChunkerBlockIdentifier;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockGroups;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.VanillaBlockStates;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.FacingDirectionHorizontal;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.Half;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.StairShape;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Calculate the shape of the stair based on neighbouring stairs.
 */
public class BedrockStairShapePreTransformHandler implements BlockPreTransformHandler {
    @Override
    public Set<Edge> getRequiredEdges(ChunkerColumn column, int x, int y, int z, ChunkerBlockIdentifier blockIdentifier) {
        // Require all edges, as this doesn't do an in-depth search of what edges are needed
        return Set.of(Edge.ALL_EDGES);
    }

    @Override
    public ChunkerBlockIdentifier handle(ChunkerColumn column, Map<Edge, ChunkerColumn> neighbours, int x, int y, int z, ChunkerBlockIdentifier blockIdentifier) {
        // Mirrors Java's StairBlock#getStairsShape: both corner checks compare the
        // side cell against this stair, not against the neighbouring stair.
        FacingDirectionHorizontal facingDirection = Objects.requireNonNull(blockIdentifier.getState(VanillaBlockStates.FACING_HORIZONTAL));

        // A perpendicular stair in front makes an outer corner, unless this stair
        // already continues a run on the side facing away from that stair.
        ChunkerBlockIdentifier front = getRelative(column, neighbours, x, y, z, facingDirection.asDirection());
        if (canConnect(blockIdentifier, front)) {
            FacingDirectionHorizontal frontFacing = Objects.requireNonNull(front.getState(VanillaBlockStates.FACING_HORIZONTAL));
            if (facingDirection.isAdjacent(frontFacing)) {
                ChunkerBlockIdentifier side = getRelative(column, neighbours, x, y, z, frontFacing.asDirection().getOpposite());
                if (canTakeShape(blockIdentifier, side)) {
                    StairShape stairShape = frontFacing.equals(facingDirection.rotateAntiClockwise()) ? StairShape.OUTER_LEFT : StairShape.OUTER_RIGHT;
                    return blockIdentifier.copyWith(VanillaBlockStates.STAIR_SHAPE, stairShape);
                }
            }
        }

        // A perpendicular stair behind makes an inner corner, unless this stair
        // already continues a run on the side that stair faces.
        ChunkerBlockIdentifier back = getRelative(column, neighbours, x, y, z, facingDirection.asDirection().getOpposite());
        if (canConnect(blockIdentifier, back)) {
            FacingDirectionHorizontal backFacing = Objects.requireNonNull(back.getState(VanillaBlockStates.FACING_HORIZONTAL));
            if (facingDirection.isAdjacent(backFacing)) {
                ChunkerBlockIdentifier side = getRelative(column, neighbours, x, y, z, backFacing.asDirection());
                if (canTakeShape(blockIdentifier, side)) {
                    StairShape stairShape = backFacing.equals(facingDirection.rotateAntiClockwise()) ? StairShape.INNER_LEFT : StairShape.INNER_RIGHT;
                    return blockIdentifier.copyWith(VanillaBlockStates.STAIR_SHAPE, stairShape);
                }
            }
        }

        // Return default
        return blockIdentifier.copyWith(VanillaBlockStates.STAIR_SHAPE, StairShape.STRAIGHT);
    }

    public boolean canConnect(ChunkerBlockIdentifier source, ChunkerBlockIdentifier relative) {
        if (!ChunkerVanillaBlockGroups.STAIRS.contains(relative.getType())) return false;

        // Grab the half of the stairs
        Half sourceHalf = source.getState(VanillaBlockStates.HALF);

        // If they're the same slab type they can connect
        return sourceHalf == relative.getState(VanillaBlockStates.HALF);
    }

    public boolean canTakeShape(ChunkerBlockIdentifier source, ChunkerBlockIdentifier relative) {
        // It can take shape if it's not stairs
        if (!ChunkerVanillaBlockGroups.STAIRS.contains(relative.getType())) return true;

        // It can take shape if the direction differs
        FacingDirectionHorizontal sourceDirection = source.getState(VanillaBlockStates.FACING_HORIZONTAL);
        if (sourceDirection != relative.getState(VanillaBlockStates.FACING_HORIZONTAL)) return true;

        // Grab the half of the stairs
        Half sourceHalf = source.getState(VanillaBlockStates.HALF);

        // It can take shape if the half differs
        return sourceHalf != relative.getState(VanillaBlockStates.HALF);
    }
}
