package hudson.plugins.parameterizedtrigger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Internal helpers that should be replaced by Java11 equivalents when the JDK baseline moves on.
 *
 */
@Restricted(NoExternalUse.class)
final class CollectionUtils {

    static <T> List<T> immutableList(Collection<T> collection1, Collection<T> collection2) {
        List<T> list = new ArrayList<>(collection1.size() + collection2.size());
        list.addAll(collection1);
        list.addAll(collection2);
        return Collections.unmodifiableList(list);
    }

    static <T> List<T> immutableList(Collection<T> collection, @SuppressWarnings("unchecked") T... ts) {
        return immutableList(collection, Arrays.asList(ts));
    }
}
