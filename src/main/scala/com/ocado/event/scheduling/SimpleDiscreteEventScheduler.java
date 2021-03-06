package com.ocado.event.scheduling;

import java.util.TreeSet;

import com.ocado.event.Event;
import com.ocado.time.AdjustableTimeProvider;
import com.ocado.time.TimeProvider;

public class SimpleDiscreteEventScheduler implements EventScheduler {
    private final AdjustableTimeProvider timeProvider;

    private final TreeSet<Event> scheduledEvents = new TreeSet<>(EVENT_COMPARATOR);

    private final Runnable endCallback;

    private boolean isExecuting = false;
    private boolean shouldStop = false;

    public SimpleDiscreteEventScheduler(AdjustableTimeProvider timeProvider, Runnable endCallback) {
        this.timeProvider = timeProvider;
        this.endCallback = endCallback;
    }

    @Override
    public void schedule(Event event) {
        if (event.time < timeProvider.getTime()) {
            stop();
            IllegalStateException exception = new IllegalStateException("Attempted to schedule " + event + " " + timeProvider.getTime() + " in the past");
            exception.printStackTrace();
            throw exception;
        }

        reallySchedule(event);
    }

    private void reallySchedule(Event event) {
        System.out.println("Scheduling " + event + " at " + timeProvider.getTime() + " on thread " + Thread.currentThread().getId());
        event.setScheduler(this);
        event.setTimeProvider(timeProvider);

        scheduledEvents.add(event);

        if (!isExecuting) {
            startExecutingEvents();
        }
    }

    @Override
    public void doNow(Runnable r, String description) {
        reallySchedule(Event.at(0, description).run(r));
    }

    @Override
    public void cancel(Event event) {
        scheduledEvents.remove(event);
    }

    private Event getNextEvent() {
        if (!scheduledEvents.isEmpty()) {
            return scheduledEvents.pollFirst();
        }
        return null;
    }

    private void startExecutingEvents() {
        Event nextEvent;
        while ((nextEvent = getNextEvent()) != null) {
            if (shouldStop) {
                break;
            }
            isExecuting = true;
            if (timeProvider.getTime() < nextEvent.time) {
                timeProvider.setTime(nextEvent.time);
            }
            try {
                nextEvent.action();
            } catch (Throwable t) {
                System.out.println("Simulation failed at " + timeProvider.getTime());
                t.printStackTrace();
                stop();
            }
        }
        isExecuting = false;
    }

    @Override
    public void stop() {
        shouldStop = true;
        endCallback.run();
    }

    @Override
    public TimeProvider getTimeProvider() {
        return timeProvider;
    }
}
