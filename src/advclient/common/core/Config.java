package global.cloudcoin.ccbank.core;

public class Config {

    public static String DIR_ROOT = "CloudCoinWallet";
    public static String DIR_BANK = "Bank";
    public static String DIR_COUNTERFEIT = "Counterfeit";
    public static String DIR_DANGEROUS = "Dangerous";
    public static String DIR_DEPOSIT = "Deposit";
    public static String DIR_DETECTED = "Detected";
    public static String DIR_EXPORT = "Export";
    public static String DIR_EMAILOUT = "EmailOut";
    public static String DIR_FRACKED = "Fracked";
    public static String DIR_GALLERY = "Gallery";
    public static String DIR_ID = "ID";
    public static String DIR_IMPORT = "Import";
    public static String DIR_IMPORTED = "Imported";
    public static String DIR_LOGS = "Logs";
    public static String DIR_LOST = "Lost";
    public static String DIR_MIND = "Mind";
    public static String DIR_PARTIAL = "Partial";
    public static String DIR_PAYFORWARD = "PayForward";
    public static String DIR_PREDETECT = "Predetect";
    public static String DIR_RECEIPTS = "Receipts";
    public static String DIR_REQUESTS = "Requests";
    public static String DIR_REQUESTRESPONSE = "RequestsResponse";
    public static String DIR_SUSPECT = "Suspect";
    public static String DIR_TEMPLATES = "Templates";
    public static String DIR_TRASH = "Trash";
    public static String DIR_TRUSTEDTRANSFER = "TrustedTransfer";
    public static String DIR_CONFIG = "Config";
    public static String DIR_SENT = "Sent";
    public static String DIR_VAULT = "Vault";



    public static String DIR_ACCOUNTS = "Accounts";
    public static String DIR_DEFAULT_USER = "Default_User_NonExist";
    public static String DIR_MAIN_LOGS = "Logs";
    public static String DIR_MAIN_TRASH = "Trash";
    public static String DIR_BACKUPS = "Backups";

    public static String GLOBAL_CONFIG_FILENAME = "global.config";
    public static int THREAD_POOL_SIZE = 60;

    public static int READ_TIMEOUT = 40000; // ms
    public static int CONNECTION_TIMEOUT = 7000; // ms
    
    public static int REQUEST_CHANGE_READ_TIMEOUT = 50000; // ms
    
    public static int FIX_FRACKED_TIMEOUT = 40000; // ms
    //public static int MULTI_FIX_TIMEOUT = 40000; // ms
    public static int MULTI_DETECT_TIMEOUT = 40000; // ms
    public static int ECHO_TIMEOUT = 5000;



    public static int MAX_ALLOWED_FAILED_RAIDAS = 3;

    public static String BACKUP0_RAIDA_IP = "95.179.159.178";
    public static int BACKUP0_RAIDA_PORT = 40000;

    public static String BACKUP1_RAIDA_IP = "66.42.61.239";
    public static int BACKUP1_RAIDA_PORT = 40000;

    public static String BACKUP2_RAIDA_IP = "45.32.134.65";
    public static int BACKUP2_RAIDA_PORT = 40000;


    public static String RAIDA_STATUS_READY = "ready";
    public static String REQUEST_STATUS_PASS = "pass";
    public static String REQUEST_STATUS_FAIL = "fail";


    public static String DEFAULT_EXPORT_DIR = "";
    public static String DEFAULT_DEPOSIT_DIR = "";

    public static int MAX_ALLOWED_LATENCY = 20000;


    public static int IDX_1 = 0;
    public static int IDX_5 = 1;
    public static int IDX_25 = 2;
    public static int IDX_100 = 3;
    public static int IDX_250 = 4;
    public static int IDX_TOTAL = 5;

    public static int IDX_FOLDER_BANK = 0;
    public static int IDX_FOLDER_FRACKED = 1;
    public static int IDX_FOLDER_LOST = 2;
    public static int IDX_FOLDER_VAULT = 3;
    public static int IDX_FOLDER_LAST = 4;
    

    public static int DEFAULT_NN = 1;

    public static String DEFAULT_TAG = "CC";
    public static String BACKUP_TAG = "BACKUP";

    public static int DEFAULT_MAX_COINS_MULTIDETECT = 400;


    public static int PASS_THRESHOLD = 20;

    public static int TYPE_STACK = 1;
    public static int TYPE_JPEG = 2;
    public static int TYPE_CSV = 3;

    public static String JPEG_MARKER = "01C34A46494600010101006000601D05";


    public static String SENDER_DOMAIN = "teleportnow.cc";

    public static int RAIDANUM_TO_QUERY_BY_DEFAULT = 7;
    public static int RAIDANUM_TO_QUERY_REQUEST_CHANGE = 18;

    public static int MAX_COMMON_LOG_LENGTH_MB = 128;


    final public static int CHANGE_METHOD_5A = 1;

    final public static int CHANGE_METHOD_25A = 2;
    final public static int CHANGE_METHOD_25B = 3;
    final public static int CHANGE_METHOD_25C = 4;
    final public static int CHANGE_METHOD_25D = 5;

    final public static int CHANGE_METHOD_100A = 6;
    final public static int CHANGE_METHOD_100B = 7;
    final public static int CHANGE_METHOD_100C = 8;
    final public static int CHANGE_METHOD_100D = 9;
    final public static int CHANGE_METHOD_100E = 10;

    final public static int CHANGE_METHOD_250A = 11;
    final public static int CHANGE_METHOD_250B = 12;
    final public static int CHANGE_METHOD_250C = 13;
    final public static int CHANGE_METHOD_250D = 14;
    final public static int CHANGE_METHOD_250E = 15;

    final public static int VAULTER_OP_VAULT = 1;
    final public static int VAULTER_OP_UNVAULT = 2;
    
    final public static int MAX_WALLET_LENGTH = 64;
    
    final public static String TRANSACTION_FILENAME = "transactions.txt";
    
    final public static int FIX_PROTOCOL_VERSION = 4;
    
    final public static String DDNS_DOMAIN = "skywallet.cc";
    
    public static String DDNSSN_SERVER = "ddns.cloudcoin.global";
    
    final public static String MAIN_LOG_FILENAME = "main.log";
    
    final public static String PICK_ERROR_MSG = "PickError";
    
    final public static String DEFAULT_DIR_MARKER = ".default.wallet";
    
    final public static String CHANGE_SKY_DOMAIN = "change.skywallet.cc";
    
    final public static String AGREEMENT_FILE = "agreement.html";
    
    final public static int LOG_BUFFER_SIZE = 1024000;
    
    
    final public static int MIN_MULTI_NOTES = 10;
    final public static int MAX_MULTI_NOTES = 4000;
    
    final public static int MIN_ECHO_TIMEOUT = 2000;
    final public static int MAX_ECHO_TIMEOUT = 120000;
    
    final public static int MIN_DETECT_TIMEOUT = 2000;
    final public static int MAX_DETECT_TIMEOUT = 120000;
    
    final public static int MIN_FIX_TIMEOUT = 2000;
    final public static int MAX_FIX_TIMEOUT = 120000;
    
    final public static int MIN_READ_TIMEOUT = 4000;
    final public static int MAX_READ_TIMEOUT = 120000;
    
    
    public static int PUBLIC_CHANGE_MAKER_ID = 2;
    
    
    public static int SECONDS_TO_AGGREGATE_ENVELOPES = 30;
    
}
