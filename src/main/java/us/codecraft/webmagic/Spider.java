package us.codecraft.webmagic;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.exception.PageProcessException;
import us.codecraft.webmagic.pipeline.CollectorPipeline;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.pipeline.ResultItemsCollectorPipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.proxy.Proxy;
import us.codecraft.webmagic.scheduler.QueueScheduler;
import us.codecraft.webmagic.scheduler.Scheduler;
import us.codecraft.webmagic.statusful.StatusfulConnection;
import us.codecraft.webmagic.thread.CountableThreadPool;
import us.codecraft.webmagic.utils.LoggerUtil;
import us.codecraft.webmagic.utils.UrlUtils;

import com.google.common.collect.Lists;

/**
 * Entrance of a crawler.<br>
 * A spider contains four modules: Downloader, Scheduler, PageProcessor and
 * Pipeline.<br>
 * Every module is a field of Spider. <br>
 * The modules are defined in interface. <br>
 * You can customize a spider with various implementations of them. <br>
 * Examples: <br>
 * <br>
 * A simple crawler: <br>
 * Spider.create(new SimplePageProcessor("http://my.oschina.net/",
 * "http://my.oschina.net/*blog/*")).run();<br>
 * <br>
 * Store results to files by FilePipeline: <br>
 * Spider.create(new SimplePageProcessor("http://my.oschina.net/",
 * "http://my.oschina.net/*blog/*")) <br>
 * .pipeline(new FilePipeline("/data/temp/webmagic/")).run(); <br>
 * <br>
 * Use FileCacheQueueScheduler to store urls and cursor in files, so that a
 * Spider can resume the status when shutdown. <br>
 * Spider.create(new SimplePageProcessor("http://my.oschina.net/",
 * "http://my.oschina.net/*blog/*")) <br>
 * .scheduler(new FileCacheQueueScheduler("/data/temp/webmagic/cache/")).run(); <br>
 *
 * @author code4crafter@gmail.com <br>
 * @see Downloader
 * @see Scheduler
 * @see PageProcessor
 * @see Pipeline
 * @since 0.1.0
 */
public class Spider implements Runnable, Task, Cloneable{
	
    protected Downloader downloader = new HttpClientDownloader();

    protected List<Pipeline> pipelines = new ArrayList<Pipeline>();
    
    protected PageProcessor pageProcessor;

    protected List<Request> startRequests;

    protected Site site;

    protected QueueScheduler scheduler;

    protected CountableThreadPool threadPool;

    protected ExecutorService executorService;

    protected int threadNum = 1;

    protected AtomicInteger stat = new AtomicInteger(STAT_INIT);;

    protected boolean exitWhenComplete = true;

    protected final static int STAT_INIT = 0;

    protected final static int STAT_RUNNING = 1;

    protected final static int STAT_STOPPED = 2;

    protected boolean spawnUrl = true;

    protected boolean destroyWhenExit = true;

    private ReentrantLock newUrlLock = new ReentrantLock();

    private Condition newUrlCondition = newUrlLock.newCondition();

    private List<SpiderListener> spiderListeners;

    private AtomicLong pageCount;
    
    private Date startTime;

    private int emptySleepTime = 30000;
    
    private String uuid;
    
    public Spider(){}

    /**
     * create a spider with pageProcessor.
     *
     * @param pageProcessor
     * @return new spider
     * @see PageProcessor
     */
    public static Spider create(PageProcessor pageProcessor) {
        return new Spider(pageProcessor);
    }
    
    /**
     * create a spider with pageProcessor.
     *
     * @param pageProcessor
     */
    public Spider(PageProcessor pageProcessor) {
        this.pageProcessor = pageProcessor;
        this.site = pageProcessor.getSite();
        this.startRequests = pageProcessor.getSite().getStartRequests();
    }
    
    /**
     * create a spider without pageProcessor.
     *
     * @param pageProcessor
     */
    public Spider(Site site){
    	this.site = site;
    }
    
    /**
     * Set startUrls of Spider.<br>
     * Prior to startUrls of Site.
     *
     * @param startUrls
     * @return this
     */
    public Spider startUrls(List<String> startUrls) {
        checkIfRunning();
        this.startRequests = UrlUtils.convertToRequests(startUrls);
        return this;
    }

    /**
     * Set startUrls of Spider.<br>
     * Prior to startUrls of Site.
     *
     * @param startRequests
     * @return this
     */
    public Spider startRequest(List<Request> startRequests) {
        checkIfRunning();
        this.startRequests = startRequests;
        return this;
    }

    /**
     * set scheduler for Spider
     *
     * @param scheduler
     * @return this
     * @Deprecated
     * @see #setScheduler(us.codecraft.webmagic.scheduler.Scheduler)
     */
    public Spider scheduler(QueueScheduler scheduler) {
        return setScheduler(scheduler);
    }

    /**
     * set scheduler for Spider
     *
     * @param scheduler
     * @return this
     * @see Scheduler
     * @since 0.2.1
     */
    public Spider setScheduler(QueueScheduler scheduler) {
        checkIfRunning();
        Scheduler oldScheduler = this.scheduler;
        this.scheduler = scheduler;
        if (oldScheduler != null) {
            Request request;
            while ((request = oldScheduler.poll(this)) != null) {
                this.scheduler.push(request, this);
            }
        }
        return this;
    }

    /**
     * add a pipeline for Spider
     *
     * @param pipeline
     * @return this
     * @see Pipeline
     * @since 0.2.1
     */
    public Spider addPipeline(Pipeline pipeline) {
        checkIfRunning();
        this.pipelines.add(pipeline);
        return this;
    }

    /**
     * set pipelines for Spider
     *
     * @param pipelines
     * @return this
     * @see Pipeline
     * @since 0.4.1
     */
    public Spider setPipelines(List<Pipeline> pipelines) {
        checkIfRunning();
        this.pipelines = pipelines;
        return this;
    }
    
    public List<Pipeline> getPipelines(){
    	return this.pipelines;
    }

    /**
     * clear the pipelines set
     *
     * @return this
     */
    public Spider clearPipeline() {
    	checkIfRunning();
        pipelines = new ArrayList<Pipeline>();
        return this;
    }

    /**
     * set the downloader of spider
     *
     * @param downloader
     * @return this
     * @see Downloader
     */
    public Spider setDownloader(Downloader downloader) {
        checkIfRunning();
        this.downloader = downloader;
        return this;
    }
    
    public Spider setPageProcessor(PageProcessor pageProcessor){
    	checkIfRunning();
    	this.pageProcessor = pageProcessor;
    	return this;
    }
    
    public Spider setSite(Site site){
    	checkIfRunning();
    	this.site = site;
    	return this;
    }

    protected void initComponent() {
        if(scheduler == null){
        	scheduler = new QueueScheduler();
        }
        if(stat == null){
        	stat = new AtomicInteger(STAT_INIT);
        }
        if(newUrlLock == null){
        	newUrlLock = new ReentrantLock();
            newUrlCondition = newUrlLock.newCondition();
        }
        if(pageCount == null){
        	pageCount = new AtomicLong(0);
        }
        if (startRequests != null) {
            for (Request request : startRequests) {
                scheduler.push(request, this);
            }
            startRequests.clear();
        }
        startTime = new Date();
    }

    @Override
    public void run() {
        checkRunningStat();
        initComponent();
        LoggerUtil.info(uuid, "Spider " + getUUID() + " started!");
        while (!Thread.currentThread().isInterrupted() && stat.get() == STAT_RUNNING) {
        	Request request = scheduler.poll(this);
            if (request == null) {
                if (threadPool.getThreadAlive() == 0) {
                	if(exitWhenComplete){
                		break;
                	}
                }
                waitNewUrl();
            } else {
                final Request requestFinal = request;
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            processRequest(requestFinal);//处理请求
                        } catch (Exception e) {
                            onError(requestFinal);
                            LoggerFactory.getLogger(Spider.class).error(String.format("process request %s error", requestFinal.getUrl()), e);
                        } finally {
                            pageCount.incrementAndGet();
                            signalNewUrl();
                        }
                    }
                });
            }
        }
        LoggerUtil.info(uuid,"spider中断...");
        stat.set(STAT_STOPPED);
        if (destroyWhenExit) {
            close();
        }
    }

    protected void onError(Request request) {
        if (CollectionUtils.isNotEmpty(spiderListeners)) {
            for (SpiderListener spiderListener : spiderListeners) {
                spiderListener.onError(request);
            }
        }
    }

    protected void onSuccess(Request request) {
        if (CollectionUtils.isNotEmpty(spiderListeners)) {
            for (SpiderListener spiderListener : spiderListeners) {
                spiderListener.onSuccess(request);
            }
        }
    }
    
    protected void onMatchSuccess(Request request){
    	if (CollectionUtils.isNotEmpty(spiderListeners)) {
            for (SpiderListener spiderListener : spiderListeners) {
                spiderListener.onMatchSuccess(request);
            }
        }
    }
    
    protected void onMatchError(Request request){
    	if (CollectionUtils.isNotEmpty(spiderListeners)) {
            for (SpiderListener spiderListener : spiderListeners) {
                spiderListener.onMatchError(request);
            }
        }
    }

    private void checkRunningStat() {
        while (true) {
            int statNow = stat.get();
            if (statNow == STAT_RUNNING) {
                throw new IllegalStateException("Spider is already running!");
            }
            if (stat.compareAndSet(statNow, STAT_RUNNING)) {
                break;
            }
        }
    }

    public void close() {
    	for (Pipeline pipeline : pipelines) {
            destroyEach(pipeline);
        }
    	destroyEach(downloader);
		destroyEach(pageProcessor);
		threadPool.shutdown();
		LoggerUtil.info(uuid, "Spider " + getUUID() + " closed!");
    }

    private void destroyEach(Object object) {
        if (object instanceof Closeable) {
            try {
                ((Closeable) object).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**子类可重写该方法，实现特定的业务流程
     * 处理流程：
     * 1.入口request
     * 2.下载
     * 3.下载出错——>重试，直到下载正常
     * 4.pageProcessor解析页面规则
     * 5.do...1,2,3,4 while(当前request下载请求链不为空nextRequest!=null)
     * 5.如果产生新的request,添加到当前spider的schedual中
     * @param request
     */
    protected void processRequest(Request request) {
    	Request currentRequest = request;
    	try{
    		for(;;){
        		Page page = downloader.download(currentRequest, this);
        		if(page!=null && !page.isNeedCycleRetry()){
        			//下载成功
        			onSuccess(currentRequest);
        		}
                if (page == null) {
                	//下载失败
                    onError(currentRequest);
                    return;
                }
                if (page.isNeedCycleRetry()) {
                	//需要重新下载
                    extractAndAddRequests(page, true);
                    return;
                }
                try {
        			pageProcessor.process(page);
        			//解析成功
        			onMatchSuccess(currentRequest);
        			extractAndAddRequests(page, spawnUrl);
        			if (!page.getResultItems().isSkip()) {
        	            for (Pipeline pipeline : pipelines) {
        	                pipeline.process(page, this);
        	            }
        	        }
        			
        			Request next = currentRequest.getNextRequest();
        			
        			if(next == null){
        				break;
        			}else{
        				if(site.getHttpProxyPool()!=null && site.getHttpProxyPool().isEnable()){
            				Proxy proxy = currentRequest.getProxy();//拿到当前代理
            				//取到request请求链的下一个request
            				next.setProxy(proxy);//将同一代理传给下一个request
        					currentRequest = next;
                    	}else{
                    		currentRequest = next;
                    	}
        			}
        		} catch (PageProcessException e) {
        			currentRequest.setMatchErrMsg(e.getMessage());
        			//解析失败
        			onMatchError(currentRequest);
        			return;
        		}
        	}
    	}finally{
    		//释放资源
    		if (site.getHttpProxyPool()!=null && site.getHttpProxyPool().isEnable()) {
    			try {
	    			int statusCode = (Integer) currentRequest.getExtra(Request.STATUS_CODE);
	            	Proxy proxy = currentRequest.getProxy();
                	site.returnHttpProxyToPool(proxy, statusCode);
				} catch (Exception e) {
					LoggerFactory.getLogger(Spider.class).error(e.getMessage(),e);
				}
                currentRequest.clean();
            }
            if(site.getConnectionPool()!=null){
            	int statusFulConnCode = (Integer) currentRequest.getExtra(Request.STATUSFUL_CONN_CODE);
            	StatusfulConnection conn = currentRequest.getStatusfulConn();
            	site.returnStatusfulConn(conn, statusFulConnCode);
            }
            //等待
    		if(site.getHttpProxyPool()==null || !site.getHttpProxyPool().isEnable()){
    			sleep(site.getSleepTime());
    		}
    	}
    }

    protected void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void extractAndAddRequests(Page page, boolean spawnUrl) {
        if (spawnUrl && CollectionUtils.isNotEmpty(page.getTargetRequests())) {
            for (Request request : page.getTargetRequests()) {
                addRequest(request);
            }
        }
    }

    private void addRequest(Request request) {
        if (site.getDomain() == null && request != null && request.getUrl() != null) {
            site.setDomain(UrlUtils.getDomain(request.getUrl()));
        }
        if(scheduler == null){
        	scheduler = new QueueScheduler();
        }
        scheduler.push(request, this);
    }

    protected void checkIfRunning() {
        if (stat.get() == STAT_RUNNING) {
            throw new IllegalStateException("Spider is already running!");
        }
    }

    public void runAsync() {
        Thread thread = new Thread(this);
        thread.setDaemon(false);
        thread.start();
    }

    public Spider addUrl(String... urls) {
        for (String url : urls) {
            addRequest(new Request(url));
        }
        signalNewUrl();
        return this;
    }

    /**
     * Download urls synchronizing.
     *
     * @param urls
     * @return
     */
    public <T> List<T> getAll(Collection<String> urls) {
        destroyWhenExit = false;
        spawnUrl = false;
        startRequests.clear();
        for (Request request : UrlUtils.convertToRequests(urls)) {
            addRequest(request);
        }
        CollectorPipeline collectorPipeline = getCollectorPipeline();
        pipelines.add(collectorPipeline);
        run();
        spawnUrl = true;
        destroyWhenExit = true;
        return collectorPipeline.getCollected();
    }

    protected CollectorPipeline getCollectorPipeline() {
        return new ResultItemsCollectorPipeline();
    }

    public <T> T get(String url) {
        List<String> urls = Lists.newArrayList(url);
        List<T> resultItemses = getAll(urls);
        if (resultItemses != null && resultItemses.size() > 0) {
            return resultItemses.get(0);
        } else {
            return null;
        }
    }

    /**
     * Add urls with information to crawl.<br/>
     *
     * @param requests
     * @return
     */
    public Spider addRequest(Request... requests) {
        for (Request request : requests) {
            addRequest(request);
        }
        signalNewUrl();
        return this;
    }

    private void waitNewUrl() {
        newUrlLock.lock();
        try {
            //double check
            if (threadPool.getThreadAlive() == 0 && exitWhenComplete) {
                return;
            }
            newUrlCondition.await(emptySleepTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LoggerUtil.warn(uuid, "waitNewUrl - interrupted error");
        } finally {
            newUrlLock.unlock();
        }
    }

    private void signalNewUrl() {
        try {
            newUrlLock.lock();
            newUrlCondition.signalAll();
        } finally {
            newUrlLock.unlock();
        }
    }

    public void start() {
        runAsync();
    }

    public void stop() {
        if (stat.compareAndSet(STAT_RUNNING, STAT_STOPPED)) {
            LoggerUtil.info(uuid, String.format("Spider %s stop success!", getUUID()));
        } else {
            LoggerUtil.info(uuid, String.format("Spider %s stop fail!", getUUID()));
        }
    }

    /**
     * start with more than one threads
     *
     * @param threadNum
     * @return this
     */
    public Spider thread(int threadNum) {
        checkIfRunning();
        this.threadNum = threadNum;
        if (threadNum <= 0) {
            throw new IllegalArgumentException("threadNum should be more than one!");
        }
        downloader.setThread(threadNum);
        //为了clone出来的spider共享同一个threadPool
        if (threadPool == null || threadPool.isShutdown()) {
            if (executorService != null && !executorService.isShutdown()) {
                threadPool = new CountableThreadPool(threadNum, executorService);
            } else {
            	executorService = new ThreadPoolExecutor(threadNum,threadNum,emptySleepTime, TimeUnit.MILLISECONDS,new LinkedBlockingDeque<Runnable>(),new SpiderThreadFactory());
            	if(executorService instanceof ThreadPoolExecutor){
            		((ThreadPoolExecutor) executorService).allowCoreThreadTimeOut(true);
            	}
            	threadPool = new CountableThreadPool(threadNum, executorService);
            }
        }
        return this;
    }

    /**
     * start with more than one threads
     *
     * @param threadNum
     * @return this
     */
    public Spider thread(ExecutorService executorService, int threadNum) {
        checkIfRunning();
        this.threadNum = threadNum;
        if (threadNum <= 0) {
            throw new IllegalArgumentException("threadNum should be more than one!");
        }
        return this;
    }

    public boolean isExitWhenComplete() {
        return exitWhenComplete;
    }

    /**
     * Exit when complete. <br/>
     * True: exit when all url of the site is downloaded. <br/>
     * False: not exit until call stop() manually.<br/>
     *
     * @param exitWhenComplete
     * @return
     */
    public Spider setExitWhenComplete(boolean exitWhenComplete) {
        this.exitWhenComplete = exitWhenComplete;
        return this;
    }

    public boolean isSpawnUrl() {
        return spawnUrl;
    }

    /**
     * Get page count downloaded by spider.
     *
     * @return total downloaded page count
     * @since 0.4.1
     */
    public long getPageCount() {
        return pageCount.get();
    }

    /**
     * Get running status by spider.
     *
     * @return running status
     * @see Status
     * @since 0.4.1
     */
    public Status getStatus() {
        return Status.fromValue(stat.get());
    }


    public enum Status {
        Init(0), Running(1), Stopped(2);

        private Status(int value) {
            this.value = value;
        }

        private int value;

        int getValue() {
            return value;
        }

        public static Status fromValue(int value) {
            for (Status status : Status.values()) {
                if (status.getValue() == value) {
                    return status;
                }
            }
            //default value
            return Init;
        }
    }

    /**
     * Get thread count which is running
     *
     * @return thread count which is running
     * @since 0.4.1
     */
    public int getThreadAlive() {
        if (threadPool == null) {
            return 0;
        }
        return threadPool.getThreadAlive();
    }

    /**
     * Whether add urls extracted to download.<br>
     * Add urls to download when it is true, and just download seed urls when it is false. <br>
     * DO NOT set it unless you know what it means!
     *
     * @param spawnUrl
     * @return
     * @since 0.4.0
     */
    public Spider setSpawnUrl(boolean spawnUrl) {
        this.spawnUrl = spawnUrl;
        return this;
    }

    @Override
    public String getUUID() {
        return uuid;
    }

    public Spider setExecutorService(ExecutorService executorService) {
        checkIfRunning();
        this.executorService = executorService;
        return this;
    }

    @Override
    public Site getSite() {
        return site;
    }

    public List<SpiderListener> getSpiderListeners() {
        return spiderListeners;
    }

    public Spider setSpiderListeners(List<SpiderListener> spiderListeners) {
        this.spiderListeners = spiderListeners;
        return this;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }
    
    /**
     * Set wait time when no url is polled.<br></br>
     *
     * @param emptySleepTime In MILLISECONDS.
     */
    public void setEmptySleepTime(int emptySleepTime) {
        this.emptySleepTime = emptySleepTime;
    }
    
    public int hashCode(){
    	return new HashCodeBuilder().append(getUUID()).hashCode();
    }
    
    public Spider setUUID(String uuid){
    	this.uuid = uuid;
    	return this;
    }
    
    public String getLoggerName(){
    	return this.uuid;
    }
    
    @Override
    public Object clone(){
    	Spider spider = null;
    	//浅克隆site,downloader,pageprocessor,threadPool,executorService
    	try {
			spider = (Spider)super.clone();
			//专属
			spider.pipelines = new ArrayList<Pipeline>();
			spider.pipelines.addAll(this.pipelines);
//	    	spider.scheduler = new QueueScheduler();
//	    	spider.startTime = new Date();
	    	spider.stat = new AtomicInteger(STAT_INIT);
//	    	spider.newUrlLock = new ReentrantLock();
//	    	spider.newUrlCondition = spider.newUrlLock.newCondition();
//	    	spider.pageCount = new AtomicLong(0);
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
    	return spider;
    }
    
    class SpiderThreadFactory implements ThreadFactory{
        final AtomicInteger poolNumber = new AtomicInteger(1);
        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;

        SpiderThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null)? s.getThreadGroup() :
                                 Thread.currentThread().getThreadGroup();
            namePrefix = "spider-" +getUUID()+"-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

	/* 
	 * 子类重写
	 */
	public boolean validProxyIpSafe(Page page) {
		return true;
	}

	/* 
	 * 子类重写
	 */
	public boolean validExpire(Page page) {
		return true;
	}

	/* 
	 * 子类重写
	 */
	public boolean validUser(Page page) {
		return true;
	}

	/* 
	 * 子类重写
	 */
	public boolean validPageContent(Page page) {
		return true;
	}

	/* 
	 * 子类重写
	 */
	public boolean validUserAction(Page page) {
		return true;
	}
}
