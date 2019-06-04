package global.cloudcoin.ccbank.core;

import java.util.Hashtable;
import java.lang.reflect.Constructor;
import java.util.Set;

public class ServantRegistry {

    private String ltag = "ServantRegistry";

    Hashtable<String, Servant> servants;

    public ServantRegistry() {
        this.servants = new Hashtable<String, Servant>();
    }

    private void setServant(String name, Object servant) {
        this.servants.put(name, (Servant) servant);
    }

    public Servant getServant(String name) {
        return (Servant) this.servants.get(name);
    }

    public Set<String> getServantKeySet() {
        return servants.keySet();
    }
    
    public boolean registerServants(String[] servants, String rootDir, GLogger logger) {
        for (String servant : servants) {
            if (!registerServant(servant, rootDir, logger)) {
                logger.error(ltag, "Failed to register servant " + servant);
                return false;
            }
        }

        return true;
    }

    public boolean registerServant(String name, String rootDir, GLogger logger) {
        Package packageObj = this.getClass().getPackage();
        String classBinName = packageObj.getName();

        classBinName = classBinName.substring(0, classBinName.lastIndexOf('.'));

        try {
            ClassLoader classLoader = this.getClass().getClassLoader();

            classBinName += "." + name + "." + name;

            Class loadedMyClass = classLoader.loadClass(classBinName);

            Constructor constructor = loadedMyClass.getDeclaredConstructor(String.class, GLogger.class);
            Object myClassObject = constructor.newInstance(rootDir, logger);

            setServant(name, myClassObject);
        } catch (ClassNotFoundException e) {
            logger.error(ltag, "Class not found for " + name + " " + classBinName);
            e.printStackTrace();

            return false;
        } catch (Exception e) {
            logger.error(ltag, "Failed to instantiate class");
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public void changeUser(String user) {
        Set<String> keys = servants.keySet();
        for (String k : keys) {
            Servant s = servants.get(k);
            if (!s.isUserBound()) 
                continue;
            
            if (isRunning(k))
                s.cancel();
            
            while (isRunning(k)) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                
                }
            }
            
            s.changeUser(user);
        }
    }
    
    public void changeServantUser(String servant, String user) {
        Servant s = servants.get(servant);
        while (isRunning(servant)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                
            }
        }
            
        s.changeUser(user);
    }
    
    
    public boolean isRunning(String name) {
        String packageName = getClass().getPackage().getName();
        String targetClass;

        targetClass = packageName.replaceAll(".core$", "") + "." + name + "." + name;

        if (getServant(name) == null)
            return false;

        Set<Thread> threadSet  = Thread.getAllStackTraces().keySet();
        for (Thread t : threadSet) {
            StackTraceElement[] es = t.getStackTrace();
            String className;

            for (int i = 0; i < es.length; i++) {
                className = es[i].getClassName();

                if (className.startsWith(targetClass))
                    return true;
            }
        }

        return false;
    }
}
