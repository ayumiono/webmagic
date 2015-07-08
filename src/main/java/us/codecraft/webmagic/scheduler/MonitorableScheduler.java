package us.codecraft.webmagic.scheduler;

import us.codecraft.webmagic.Task;

/**
 * The scheduler whose requests can be counted for monitor.
 *
 * @author code4crafter@gmail.com
 * @since 0.5.0
 */
public interface MonitorableScheduler extends Scheduler {

    public long getLeftRequestsCount(Task task);

    public long getTotalRequestsCount(Task task);

}