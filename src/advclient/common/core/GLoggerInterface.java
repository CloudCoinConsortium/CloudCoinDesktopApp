package global.cloudcoin.ccbank.core;

public interface GLoggerInterface {

    int GL_DEBUG = 1;
    int GL_INFO = 2;
    int GL_VERBOSE = 3;
    int GL_ERROR = 4;

    void onLog(int level, String tag, String message);

}