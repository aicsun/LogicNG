package org.logicng.handlers;

public class TimeoutDnnfCompilationHandler implements DnnfCompilationHandler {

    private final long timeout;
    private long end;

    public TimeoutDnnfCompilationHandler(final long timeout) {
        this.timeout = timeout;
    }

    @Override
    public void start() {
        this.end = System.currentTimeMillis() + this.timeout;
    }

    @Override
    public boolean shannonExpansion() {
        return System.currentTimeMillis() <= this.end;
    }

    @Override
    public void end() {
        // nothing to do here
    }
}
