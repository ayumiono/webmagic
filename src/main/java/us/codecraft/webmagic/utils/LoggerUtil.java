package us.codecraft.webmagic.utils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;



/**
 * 日志系统
 * @author lance_yan@transing.com
 *
 */
public class LoggerUtil {
	
	private static final String INFO_FILE_SUFIX = "info.log";
	
	private static final String LOG_PATTERN = "%d{yy-MM-dd HH:mm:ss,SSS} %-5p ## %m%n";
	
	private static final String INFO_HIERARCHY = "info.";
	
	private static String maxFileSize = "10MB";

	private static int maxBackupIndex = 10;

	private static int maxBackupDateIndex = 10;

	private static String logDirPath = System.getProperty("log.dir");
	
	public static void debug(String format, Object[] argArray){
		FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
		debug(ft.getMessage());
	}
	
	/**
	 * 全局debug文件
	 * @param content
	 */
	public static void debug(String content) {
		Logger logger = Logger.getLogger("info");
		StringBuffer tempMessage = new StringBuffer();
		tempMessage.append(content);
		logger.debug(tempMessage.toString());
	}
	
	public static void debug(String format,Object[] argArray, Throwable e){
		FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
		debug(ft.getMessage(),e);
	}

	public static void debug(String content, Throwable e) {
		Logger logger = Logger.getLogger("info");
		StringBuffer tempMessage = new StringBuffer();
		tempMessage.append(content);
		logger.debug(tempMessage.toString(), e);
	}

	/**
	 * 业务模块
	 * @param module
	 * @param content
	 */
	public static void debug(String module, String content) {
		String loggerName = INFO_HIERARCHY + module;
		Logger logger = Logger.getLogger(loggerName);
		logger.setAdditivity(false);//子日志不打印到父日志中
		initBusiLog(logger, module, INFO_FILE_SUFIX);
		StringBuffer tempMessage = new StringBuffer();
		tempMessage.append(content);
		logger.debug(tempMessage.toString());
	}
	
	public static void debug(String module, String format,Object[] argArray){
		FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
		debug(module,ft.getMessage());
	}

	public static void debug(String module, String content, Throwable e) {
		String loggerName = INFO_HIERARCHY + module;
		Logger logger = Logger.getLogger(loggerName);
		logger.setAdditivity(false);//子日志不打印到父日志中
		initBusiLog(logger, module, INFO_FILE_SUFIX);
		StringBuffer tempMessage = new StringBuffer();
		tempMessage.append(content);
		logger.debug(tempMessage.toString(), e);
	}
	
	public static void debug(String module, String format, Object[] argArray, Throwable e){
		FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
		debug(module,ft.getMessage(),e);
	}
	
	/**
	 * 业务模块+子模块
	 * @param module
	 * @param subModule
	 * @param content
	 */
	public static void debug(String module, String subModule, String content){
		String loggerName = INFO_HIERARCHY + module + "." + subModule;
		Logger logger = Logger.getLogger(loggerName);
		logger.setAdditivity(false);//子日志不打印到父日志中
		initBusiLog(logger, module, subModule, INFO_FILE_SUFIX);
		StringBuffer tempMessage = new StringBuffer();
		tempMessage.append(content);
		logger.debug(tempMessage.toString());
	}
	
	public static void debug(String module, String subModule, String format, Object[] argArray){
		FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
		debug(module,subModule,ft.getMessage());
	}
	
	public static void debug(String module, String subModule, String content, Throwable e){
		String loggerName = INFO_HIERARCHY + module + "." + subModule + INFO_FILE_SUFIX;
		Logger logger = Logger.getLogger(loggerName);
		logger.setAdditivity(false);//子日志不打印到父日志中
		initBusiLog(logger, module, subModule, INFO_FILE_SUFIX);
		StringBuffer tempMessage = new StringBuffer();
		tempMessage.append(content);
		logger.debug(tempMessage.toString(),e);
	}
	
	public static void debug(String module, String subModule, String format, Object[] argArray, Throwable e){
		FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
		debug(module,subModule,ft.getMessage(),e);
	}

	/**
	 * 全局info日志
	 * @param content
	 */
	public static void info(String content) {
		Logger logger = Logger.getLogger("info");
		StringBuffer tempMessage = new StringBuffer();
		tempMessage.append(content);
		logger.info(tempMessage.toString());
	}
	
	public static void info(String format, Object[] argArray){
		FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
		info(ft.getMessage());
	}

	/**
	 * 业务模块
	 * @param module
	 * @param content
	 */
	public static void info(String module, String content) {
		String loggerName = INFO_HIERARCHY + module;
		Logger logger = Logger.getLogger(loggerName);
		logger.setAdditivity(false);//子日志不打印到父日志中
		initBusiLog(logger, module, INFO_FILE_SUFIX);
		StringBuffer tempMessage = new StringBuffer();
		tempMessage.append(content);
		logger.info(tempMessage.toString());
	}
	
	public static void info(String module,String format,Object[] argArray){
		FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
		info(module,ft.getMessage());
	}

	/**
	 * 包含业务模块和子模块
	 * @param module
	 * @param subModule
	 * @param content
	 */
	public static void info(String module, String subModule, String content) {
		String loggerName = INFO_HIERARCHY + module + "." + subModule;
		Logger logger = Logger.getLogger(loggerName);
		logger.setAdditivity(false);//子日志不打印到父日志中
		initBusiLog(logger, module, subModule, INFO_FILE_SUFIX);
		StringBuffer tempMessage = new StringBuffer();
		tempMessage.append(content);
		logger.info(tempMessage.toString());
	}
	
	public static void info(String module,String subModule,String format,Object[] argArray){
		FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
		info(module,subModule,ft.getMessage());
	}

	/**
	 * 输出到全局error日志中
	 * @param module
	 * @param content
	 * @param e
	 */
	public static void error(String content, Throwable e) {
		Logger logger = Logger.getLogger("error");
		StringBuffer tempMessage = new StringBuffer();
		tempMessage.append(content);
		logger.error(tempMessage.toString(),e);
	}
	
	public static void error(String format,Object[] argArray,Throwable e){
		FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
		error(ft.getMessage(),e);
	}
	
	/**
	 * 输出到全局info日志中
	 * @param content
	 * @param e
	 */
	public static void warn(String content, Throwable e) {
		Logger logger = Logger.getLogger("info");
		StringBuffer tempMessage = new StringBuffer();
		tempMessage.append(content);
		logger.warn(tempMessage.toString(),e);
	}
	
	public static void warn(String format, Object[] argArray, Throwable e){
		FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
		warn(ft.getMessage(),e);
	}
	
	public static void warn(String content){
		Logger logger = Logger.getLogger("info");
		StringBuffer tempMessage = new StringBuffer();
		tempMessage.append(content);
		logger.warn(tempMessage.toString());
	}
	
	public static void warn(String format, Object[] argArray){
		FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
		warn(ft.getMessage());
	}
	
	/**
	 * 输出到相同业务模块下的info日志中
	 * @param content
	 * @param module
	 * @param e
	 */
	public static void warn(String module, String content, Throwable e) {
		String loggerName = INFO_HIERARCHY+module;
		Logger logger = Logger.getLogger(loggerName);
		logger.setAdditivity(false);//子日志不打印到父日志中
		initBusiLog(logger, module, INFO_FILE_SUFIX);
		StringBuffer tempMessage = new StringBuffer();
		tempMessage.append(content);
		logger.warn(tempMessage.toString(),e);
	}
	
	public static void warn(String module, String format, Object[] argArray, Throwable e){
		FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
		warn(module,ft.getMessage(),e);
	}
	
	public static void warn(String module, String content) {
		String loggerName = INFO_HIERARCHY+module;
		Logger logger = Logger.getLogger(loggerName);
		logger.setAdditivity(false);//子日志不打印到父日志中
		initBusiLog(logger, module, INFO_FILE_SUFIX);
		StringBuffer tempMessage = new StringBuffer();
		tempMessage.append(content);
		logger.warn(tempMessage.toString());
	}
	
	public static void warn(String module, String format, Object[] argArray){
		FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
		warn(module,ft.getMessage());
	}

	private static void initBusiLog(Logger logger,String busiModule,String subModule,String logName){
		try {
			String logDir = System.getProperty("log.dir");
			if ((logDir == null) || (logDir.trim().length() <= 0)) {
				logDir = logDirPath;
			}
			if ((logDir == null) || (logDir.trim().length() <= 0)) {
				throw new RuntimeException("日志路径参数log.dir为空！");
			}
			if (!logDir.endsWith("//")) {
				logDir = logDir + "//";
			}
			String bizLogDirPath = logDir + busiModule + "//";
			Date curDate = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat();
			dateFormat.applyPattern("yyyyMMdd");
			StringBuffer dateDirStrBuf = new StringBuffer();
			dateDirStrBuf.append(bizLogDirPath);
			dateDirStrBuf.append("//");
			dateDirStrBuf.append(dateFormat.format(curDate));
			dateDirStrBuf.append("//");

			StringBuffer logNameStrBuf = new StringBuffer();
			logNameStrBuf.append(dateDirStrBuf);
			
			//子模块
			logNameStrBuf.append("//"+subModule+"//");
			
			logNameStrBuf.append(logName).toString();

			String loggerName = busiModule + "_" + subModule + "_" + logName;

			RollingFileAppender fileAppender = (RollingFileAppender) logger
					.getAppender(loggerName);

			if (fileAppender == null) {
				synchronized (logger) {
					if(logger.getAppender(loggerName)==null){
						PatternLayout patternLayout = new PatternLayout(LOG_PATTERN);
						File fileDir = new File(dateDirStrBuf.toString());
						if (!fileDir.exists()) {
							fileDir.mkdirs();
						}

						fileAppender = new RollingFileAppender(patternLayout,
								logNameStrBuf.toString());
						fileAppender.setEncoding("gbk");
						fileAppender.setMaxFileSize(maxFileSize);
						fileAppender.setMaxBackupIndex(maxBackupIndex);
						fileAppender.setName(loggerName);
						logger.addAppender(fileAppender);
					}
				}
			} else if (!fileAppender.getFile().equals(logNameStrBuf.toString())) {
				synchronized (logger) {
					if(!fileAppender.getFile().equals(logNameStrBuf.toString())){
						File bizLogDir = new File(bizLogDirPath);
						File[] files = bizLogDir.listFiles();
						if ((files != null) && (files.length > 1)) {
							int fileLen = files.length;
							if (fileLen >= maxBackupDateIndex) {
								File smallDateFile = null;
								for (File curfile : files) {
									if (smallDateFile == null) {
										smallDateFile = curfile;
									} else {
										String smallDateName = smallDateFile.getName();
										String curfileName = curfile.getName();
										if (curfileName.compareTo(smallDateName) < 0) {
											smallDateFile = curfile;
										}
									}
								}
								if ((smallDateFile != null) && (smallDateFile.exists())) {
									FileUtils.deleteDirectory(smallDateFile);
								}
							}
						}

						PatternLayout patternLayout = new PatternLayout(LOG_PATTERN);
						File fileDir = new File(dateDirStrBuf.toString());
						if (!fileDir.exists()) {
							fileDir.mkdirs();
						}
						fileAppender.close();
						logger.removeAppender(fileAppender);
						fileAppender = new RollingFileAppender(patternLayout,
								logNameStrBuf.toString());
						fileAppender.setEncoding("gbk");
						fileAppender.setMaxFileSize(maxFileSize);
						fileAppender.setMaxBackupIndex(maxBackupIndex);
						fileAppender.setName(loggerName);
						logger.addAppender(fileAppender);
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void initBusiLog(Logger logger, String busiModule,
			String logName) {
		try {
			
			String logDir = System.getProperty("log.dir");

			if ((logDir == null) || (logDir.trim().length() <= 0)) {
				logDir = logDirPath;
			}

			if ((logDir == null) || (logDir.trim().length() <= 0)) {
				throw new RuntimeException("日志路径参数log.dir为空！");
			}

			if (!logDir.endsWith("//")) {
				logDir = logDir + "//";
			}

			String bizLogDirPath = logDir + busiModule + "//";

			Date curDate = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat();
			dateFormat.applyPattern("yyyyMMdd");
			StringBuffer dateDirStrBuf = new StringBuffer();
			dateDirStrBuf.append(bizLogDirPath);
			dateDirStrBuf.append("/");
			dateDirStrBuf.append(dateFormat.format(curDate));
			dateDirStrBuf.append("/");

			StringBuffer logNameStrBuf = new StringBuffer();
			logNameStrBuf.append(dateDirStrBuf);
			logNameStrBuf.append(logName).toString();

			String loggerName = busiModule + "_" + logName;

			RollingFileAppender fileAppender = (RollingFileAppender) logger
					.getAppender(loggerName);

			if (fileAppender == null) {
				synchronized (logger) {
					if(logger.getAppender(loggerName)==null){
						PatternLayout patternLayout = new PatternLayout(LOG_PATTERN);
						File fileDir = new File(dateDirStrBuf.toString());
						if (!fileDir.exists()) {
							fileDir.mkdirs();
						}

						fileAppender = new RollingFileAppender(patternLayout,
								logNameStrBuf.toString());
						fileAppender.setEncoding("gbk");
						fileAppender.setMaxFileSize(maxFileSize);
						fileAppender.setMaxBackupIndex(maxBackupIndex);
						fileAppender.setName(loggerName);
						logger.addAppender(fileAppender);
					}
				}
			} else if (!fileAppender.getFile().equals(logNameStrBuf.toString())) {
				synchronized (logger) {
					if(!fileAppender.getFile().equals(logNameStrBuf.toString())){
						File bizLogDir = new File(bizLogDirPath);
						File[] files = bizLogDir.listFiles();
						if ((files != null) && (files.length > 1)) {
							int fileLen = files.length;
							if (fileLen >= maxBackupDateIndex) {
								File smallDateFile = null;
								for (File curfile : files) {
									if (smallDateFile == null) {
										smallDateFile = curfile;
									} else {
										String smallDateName = smallDateFile.getName();
										String curfileName = curfile.getName();
										if (curfileName.compareTo(smallDateName) < 0) {
											smallDateFile = curfile;
										}
									}

								}
								if ((smallDateFile != null) && (smallDateFile.exists())) {
									FileUtils.deleteDirectory(smallDateFile);
								}
							}
						}

						PatternLayout patternLayout = new PatternLayout(LOG_PATTERN);
						File fileDir = new File(dateDirStrBuf.toString());
						if (!fileDir.exists()) {
							fileDir.mkdirs();
						}

						fileAppender.close();

						logger.removeAppender(fileAppender);
						fileAppender = new RollingFileAppender(patternLayout,
								logNameStrBuf.toString());
						fileAppender.setEncoding("gbk");
						fileAppender.setMaxFileSize(maxFileSize);
						fileAppender.setMaxBackupIndex(maxBackupIndex);
						fileAppender.setName(loggerName);
						logger.addAppender(fileAppender);
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] arg) throws IOException {
//		System.setProperty("log.dir", "d:\\temp\\log");
//		Logger infoLogger = Logger.getLogger("info");
//		Logger errorLogger = Logger.getLogger("error");
//		String patternString = "%d{yy-MM-dd HH:mm:ss,SSS} %-5p %c(%F:%L) ## %m%n";
//		PatternLayout patternLayout = new PatternLayout(patternString);
//		RollingFileAppender infoAppender = new RollingFileAppender(patternLayout,"d:\\temp\\log\\rootInfo.log");
//		RollingFileAppender errorAppender = new RollingFileAppender(patternLayout,"d:\\temp\\log\\error.log");
//		infoLogger.setLevel(Level.DEBUG);
//		infoLogger.setAdditivity(false);
//		infoLogger.addAppender(infoAppender);
//		errorLogger.setLevel(Level.ERROR);
//		errorLogger.addAppender(errorAppender);
		//LoggerUtil.debug("ctripinfo", "test 4 bizmodule ctripinfo debug");
		//LoggerUtil.info("ctripinfo", "test 4 bizmodule ctripinfo info");
//		LoggerUtil.debug("ctripinfo", "proxyippool","test 4 bizmodule {} submodule {} debug",new Object[]{"ctripinfo","proxyippool"});
		//LoggerUtil.info("ctripinfo", "pipeline","test 4 bizmodule ctripinfo submodule pipeline info");
		//LoggerUtil.debug("jdbreadcrumb", "test 4 jdbreadcrumb ctripinfo debug");
		//LoggerUtil.info("jdbreadcrumb", "test 4 jdbreadcrumb ctripinfo info");
		//LoggerUtil.info("test 4 no biz module info");
		//LoggerUtil.error("test 4 error", new IllegalAccessException());
		//LoggerUtil.warn("test 4 warn", new IllegalAccessException());
		System.setProperty("log.dir", "d:\\log");	
		for(int i=0;i<200000;i++){
			final int finalint = i;
			new Thread(new Runnable(){
				@Override
				public void run() {
					LoggerUtil.info("LmdnaCarBiz","HttpClientDownloader","test 4 jdbreadcrumb ctripinfo info"+finalint);
				}}).start();
		}
	}
}
