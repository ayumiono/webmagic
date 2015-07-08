package us.codecraft.webmagic.scheduler;

import org.apache.http.annotation.ThreadSafe;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Task;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Basic Scheduler implementation.<br>
 * Store urls to fetch in LinkedBlockingQueue and remove duplicate urls by HashMap.
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.1.0
 */
@ThreadSafe
public class QueueScheduler implements MonitorableScheduler, Cloneable {

    private BlockingQueue<Request> queue = new LinkedBlockingQueue<Request>();
    
    private AtomicLong count = new AtomicLong(0);
    
    @Override
    public synchronized Request poll(Task task) {
        return queue.poll();
    }

    @Override
    public long getLeftRequestsCount(Task task) {
        return queue.size();
    }

    @Override
    public long getTotalRequestsCount(Task task) {
        return count.get();
    }
    
	@Override
	public void push(Request request, Task task) {
		if(request.getExtra(Request.CYCLE_TRIED_TIMES) == null){
			count.incrementAndGet();
		}
		queue.add(request);
	}
}
