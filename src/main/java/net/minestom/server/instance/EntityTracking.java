package net.minestom.server.instance;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.stream.Stream;

@ApiStatus.Experimental
public interface EntityTracking {
    static EntityTracking synchronize(EntityTracking entityTracking) {
        return entityTracking instanceof EntityTrackingImpl.Synchronized ?
                entityTracking : new EntityTrackingImpl.Synchronized(entityTracking);
    }

    /**
     * Register an entity to be tracked.
     */
    void register(Entity entity, Point spawnPoint);

    /**
     * Same as #register() but also return the stream from #chunkRangeEntities().
     */
    List<Entity> registerAndView(Entity entity, Point spawnPoint, int range);

    /**
     * Unregister an entity tracking.
     */
    void unregister(Entity entity, Point point);

    /**
     * Called every time an entity move, you may want to verify if the new
     * position is in a different chunk.
     */
    void move(Entity entity, Point oldPoint, Point newPoint);

    /**
     * Called every time an entity move, you may want to verify if the new
     * position is in a different chunk.
     * <p>
     * Must returns the entities to add & remove.
     */
    List<Result> moveAndView(Entity entity, Point oldPoint, Point newPoint);

    /**
     * Returns the entities within a range.
     */
    List<Entity> nearbyEntities(Point point, double range);

    /**
     * Returns the entities present in the specified chunk.
     */
    List<Entity> chunkEntities(Point chunkPoint);

    /**
     * Returns the entities present and in range of the specified chunk.
     * <p>
     * This is used for auto-viewable features.
     */
    List<Entity> chunkRangeEntities(Point chunkPoint, int range);

    /**
     * Returns the entities newly visible and invisible from one position to another.
     */
    List<Result> difference(Point p1, Point p2);

    class Result {
        private Stream<Entity> addition;
        private Stream<Entity> removal;
    }
}
