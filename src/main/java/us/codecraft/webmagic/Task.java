package us.codecraft.webmagic;

/**
 * Interface for identifying different tasks.<br>
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.1.0
 * @see us.codecraft.webmagic.scheduler.Scheduler
 * @see us.codecraft.webmagic.pipeline.Pipeline
 */
public interface Task extends StatusfulResponseVerify{

    /**
     * unique id for a spider.
     *
     * @return uuid
     */
    public String getUUID();

    /**
     * site of a task
     *
     * @return site
     */
    public Site getSite();
    
}
