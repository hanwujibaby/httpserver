package cannon.server.util;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author cannonfang
 * @name 房佳龙
 * @date 2014-1-9
 * @qq 271398203
 */
public final class CaseIgnoringComparator implements Comparator<String>, Serializable {

    private static final long serialVersionUID = 4582133183775373862L;

    public static final CaseIgnoringComparator INSTANCE = new CaseIgnoringComparator();

    private CaseIgnoringComparator() {
        super();
    }

    public int compare(String o1, String o2) {
        return o1.compareToIgnoreCase(o2);
    }

    private Object readResolve() {
        return INSTANCE;
    }
}