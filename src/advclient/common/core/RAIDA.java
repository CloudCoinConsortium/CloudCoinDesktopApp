package global.cloudcoin.ccbank.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RAIDA {
        static String ltag = "RAIDA";
	public static int TOTAL_RAIDA_COUNT = 25;

	public DetectionAgent[] agents;
        
        int[] latencies, intLatencies, totalLatencies;
	ExecutorService service;

	public GLogger logger;

	final static int STATUS_OK = 1;
	final static int STATUS_FAILED = 2;

        String packageName;
	public RAIDA(GLogger logger) {
		agents = new DetectionAgent[TOTAL_RAIDA_COUNT];
                latencies = new int[TOTAL_RAIDA_COUNT];
                intLatencies = new int[TOTAL_RAIDA_COUNT];
                totalLatencies = new int[TOTAL_RAIDA_COUNT];
		for (int i = 0; i < TOTAL_RAIDA_COUNT; i++) {
			agents[i] = new DetectionAgent(i, logger);
		}

		this.logger = logger;
                
                packageName = getClass().getPackage().getName() + ".RAIDA";
	}

	private int[] getAllRAIDAs() {
		int[] rv = new int[TOTAL_RAIDA_COUNT];
		for (int i = 0; i < TOTAL_RAIDA_COUNT; i++)
			rv[i] = i;

		return rv;
	}
        
        public void setLatency(int raidaNumber, int latency, int intLatency) {
            latencies[raidaNumber] = latency;
            intLatencies[raidaNumber] = intLatency;
            totalLatencies[raidaNumber] = latency + intLatency;
        }
        
        public int[] getLatencies() {
            return totalLatencies;
        }

        public void setReadTimeout(int timeout) {
            for (int i = 0; i < TOTAL_RAIDA_COUNT; i++) {
                agents[i].setReadTimeout(timeout);
            }
        }
        
        public void setDefaultUrls() {
            for (int i = 0; i < TOTAL_RAIDA_COUNT; i++)
                agents[i].setDefaultFullUrl();
        }
        
        public void cancel() {
            for (int i = 0; i < TOTAL_RAIDA_COUNT; i++)
                agents[i].stopConnection();
        }
        
	public void setExactUrls(String[] urls) {
		for (int i = 0; i < TOTAL_RAIDA_COUNT; i++) {
			logger.info(ltag, "Set RAIDA url to " + urls[i]);
			agents[i].setExactFullUrl(urls[i]);
		}
	}

	public void setUrl(String ip, int basePort) {
		logger.info(ltag, "Set RAIDA ip " + ip + ":" + basePort);
		for (int i = 0; i < TOTAL_RAIDA_COUNT; i++) {
			agents[i].setFullUrl(ip, basePort);
		}
	}
        
        public void setUrlWithDomain(String domain) {
            for (int i = 0; i < TOTAL_RAIDA_COUNT; i++)
                agents[i].setUrlWithDomain(domain);
        }

	public String[] getRAIDAURLs() {
		String[] data;

		data = new String[TOTAL_RAIDA_COUNT];
        	for (int i = 0; i < TOTAL_RAIDA_COUNT; i++) {
                   	data[i] = agents[i].getFullURL();
            }

            return data;
	}

	public boolean isFailed(int idx) {
            return agents[idx].getStatus() == RAIDA.STATUS_FAILED;
	}

	public void setFailed(int idx) {
            logger.debug(ltag, "RAIDA " + idx + " is set failed");
            agents[idx].setStatus(RAIDA.STATUS_FAILED);
	}

	public long[] getLastLatencies() {
		long[] responses;

		responses = new long[TOTAL_RAIDA_COUNT];
		for (int i = 0; i < TOTAL_RAIDA_COUNT; i++)
			responses[i] = agents[i].getLastLatency();

		return responses;
	}

	public String[] query(String[] requests) {
            return query(requests, null, null, null);
	}

	public String[] query(String[] requests, String[] posts) {
            return query(requests, posts, null, null);
	}

	public String[] query(String[] requests, String[] posts, CallbackInterface cb) {
            return query(requests, posts, cb, null);
	}
        
        public String[] query(String[] requests, String[] posts, CallbackInterface cb, int[] rlist) {
            
            if (AppCore.currentMode == Config.OPERATION_MODE_FAST) {
                logger.debug(ltag, "Querying raida in parallel");
                return queryParallel(requests, posts, cb, rlist);
            } else if (AppCore.currentMode == Config.OPERATION_MODE_SLOW) {
                logger.debug(ltag, "Querying raida in serial mode");
                return querySync(requests, posts, cb, rlist);               
            } else {
                logger.error(ltag, "Invalid mode: " + AppCore.currentMode);
            }
            
            return null;
        }

	public String[] queryParallel(String[] requests, String[] posts, CallbackInterface cb, int[] rlist) {
            int raidalistSize;
            service = AppCore.getServiceExecutor();
            List<Future<Runnable>> futures = new ArrayList<Future<Runnable>>();

            if (rlist == null)
		rlist = getAllRAIDAs();

            raidalistSize = rlist.length;

            final String[] results = new String[raidalistSize];

            if (requests.length != raidalistSize) {
		logger.error(ltag, "Internal error. Wrong parameters");
		return null;
            }

            if (posts != null) {
		if (posts.length != raidalistSize) {
                    logger.error(ltag, "Internal error. Wrong post parameters");
                    return null;
		}
            } else {
		posts = new String[raidalistSize];
		for (int i = 0; i < raidalistSize; i++) {
                    posts[i] = null;
		}
            }
                
            StackTraceElement[] es = Thread.currentThread().getStackTrace();
            String callerClass = "";
            for (int i = 0; i < es.length; i++) {
                String className = es[i].getClassName();
                    
                if (className.equals(packageName) || className.equals("java.lang.Thread"))
                    continue;

                callerClass = className;

                break;
            }
    
            for (int i = 0; i < raidalistSize; i++) {
		final int rIdxFinal = rlist[i];
		final int iFinal = i;
		final String request = requests[i];
		final String post = posts[i];
		final CallbackInterface myCb = cb;
                final String fcallerClass = callerClass;

		Future f = service.submit(new Runnable() {
                    public void run() {
                        String url = "/service/" + request;
                        results[iFinal] = agents[rIdxFinal].doRequest(url, post, fcallerClass);
                        if (myCb != null)
                            myCb.callback(null);
                    }
                });
                
                futures.add(f);
            }

            for (Future<Runnable> f : futures) {
		try {
                    f.get(agents[0].getReadTimeout() * 2, TimeUnit.MILLISECONDS);
		} catch (ExecutionException e) {
                    logger.error(ltag, "Error executing the task: " + e.getMessage());
                    e.printStackTrace();
		} catch (TimeoutException e) {
                    logger.error(ltag, "Timeout during connection to the server");
		} catch (InterruptedException e) {
                    logger.error(ltag, "Task interrupted");
		}
            }

            return results;
	}

        public String[] querySync(String[] requests, String[] posts, CallbackInterface cb, int[] rlist) {
            int raidalistSize;

            if (rlist == null)
                rlist = getAllRAIDAs();

            raidalistSize = rlist.length;
            final String[] results = new String[raidalistSize];
            if (requests.length != raidalistSize) {
                logger.error(ltag, "Internal error. Wrong parameters");
                return null;
            }

            if (posts != null) {
                if (posts.length != raidalistSize) {
                    logger.error(ltag, "Internal error. Wrong post parameters");
                    return null;
                }
            } else {
                posts = new String[raidalistSize];
                for (int i = 0; i < raidalistSize; i++) {
                    posts[i] = null;
                }
            }
                
            StackTraceElement[] es = Thread.currentThread().getStackTrace();
            String callerClass = "";
            for (int i = 0; i < es.length; i++) {
                String className = es[i].getClassName();
                   
                if (className.equals(packageName) || className.equals("java.lang.Thread"))
                    continue;

                callerClass = className;

                break;
            }

                
            for (int i = 0; i < raidalistSize; i++) {
		final int rIdxFinal = rlist[i];
		final int iFinal = i;
		final String request = requests[i];
		final String post = posts[i];
		final CallbackInterface myCb = cb;
                final String fcallerClass = callerClass;
   
                
                String url = "/service/" + request;
                results[iFinal] = agents[rIdxFinal].doRequest(url, post, fcallerClass);
                if (myCb != null)
                    myCb.callback(null);
            }

            return results;
	}
        
    
	public String[] queryNoWait(String[] requests, int[] rlist) {
            int raidalistSize;
            service = AppCore.getServiceExecutor();
            List<Future<Runnable>> futures = new ArrayList<Future<Runnable>>();

            if (rlist == null)
		rlist = getAllRAIDAs();

            raidalistSize = rlist.length;

            final String[] results = new String[raidalistSize];
            if (requests.length != raidalistSize) {
		logger.error(ltag, "Internal error. Wrong parameters");
		return null;
            }
    
            for (int i = 0; i < raidalistSize; i++) {
		final int rIdxFinal = rlist[i];
		final int iFinal = i;
		final String request = requests[i];

		Future f = service.submit(new Runnable() {
                    public void run() {
                        String url = "/service/" + request;
                        results[iFinal] = agents[rIdxFinal].doRequest(url, null, "Main");
                    }
                });
                
                futures.add(f);
            }
/*
            for (Future<Runnable> f : futures) {
		try {
                    f.get(agents[0].getReadTimeout() * 2, TimeUnit.MILLISECONDS);
		} catch (ExecutionException e) {
                    logger.error(ltag, "Error executing the task: " + e.getMessage());
                    e.printStackTrace();
		} catch (TimeoutException e) {
                    logger.error(ltag, "Timeout during connection to the server");
		} catch (InterruptedException e) {
                    logger.error(ltag, "Task interrupted");
		}
            }*/

            return results;
	}
}
