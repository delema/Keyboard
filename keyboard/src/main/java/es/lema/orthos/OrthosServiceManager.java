package es.lema.orthos;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import junit.framework.Assert;

import es.lema.orthos.service.IOrthosService;
import es.lema.orthos.service.IOrthosSession;

public final class OrthosServiceManager {
    private static final String TAG = OrthosServiceManager.class.getSimpleName();

    private static OrthosServiceManager instance = null;
    
    private static OrthosServiceConnection connection = null;
    private static IOrthosService service = null;
    private static IOrthosSession session = null;

    private OrthosServiceManager(Context context) {
        Log.d(TAG, "OrthosServiceManager()");
    	connection = new OrthosServiceConnection();
        Intent intent = new Intent(IOrthosService.class.getName());
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

	/*
	 * Create the instance manager
	 */
    public static OrthosServiceManager create(Context context) {
		Log.d(TAG, "create()");
        synchronized (OrthosServiceManager.class) {
            if (instance == null) {
            	instance = new OrthosServiceManager(context);
            }
            return instance;
       }
     };

    /*
     * Get the instance manager
     */
    public static OrthosServiceManager getInstance() {
		Log.d(TAG, "getInstance()");
        Assert.assertNotNull(instance);
        synchronized (OrthosServiceManager.class) {
        	return instance;
        }
    }

    /*
     * Get the session manager
     */
    public IOrthosSession getSession(String locale) throws RemoteException {
        Log.d(TAG, "getSession(): " + locale);
        Assert.assertNotNull(locale);
        Assert.assertNotNull(service);
        synchronized (OrthosServiceManager.class) {
            String language = locale.split("_")[0];
            if (session == null) {
                session = service.createSession(language);
            }
            else {
                session.setLocale(language);
            }
            Assert.assertNotNull(session);
            return session;
        }
    }

/*
     * Bind with the service
     */
    private static class OrthosServiceConnection implements ServiceConnection {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(TAG, "onServiceConnected(): " + binder.getClass().getName());
			service = IOrthosService.Stub.asInterface(binder);
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.d(TAG, "onServiceDisconnected()");
			service = null;
			session = null;
		}
	}
}
