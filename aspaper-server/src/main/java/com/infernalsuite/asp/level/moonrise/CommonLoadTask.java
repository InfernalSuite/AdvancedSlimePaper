package com.infernalsuite.asp.level.moonrise;

import ca.spottedleaf.concurrentutil.util.Priority;

public interface CommonLoadTask {

    boolean schedule(boolean schedule);

    Priority getPriority();

    boolean cancel();

    void lowerPriority(Priority priority);

    void raisePriority(Priority priority);

    void setPriority(Priority priority);
}
