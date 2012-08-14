package se.miun.mediasense.eval;

import se.miun.mediasense.addinlayer.AddInManager;
import se.miun.mediasense.addinlayer.extensions.publishsubscribe.PublishSubscribeExtension;
import se.miun.mediasense.disseminationlayer.communication.CommunicationInterface;
import se.miun.mediasense.disseminationlayer.disseminationcore.DisseminationCore;
import se.miun.mediasense.disseminationlayer.lookupservice.LookupServiceInterface;
import se.miun.mediasense.interfacelayer.MediaSensePlatform;
import android.util.Log;

public class MediaSenseManager {

	private MediaSensePlatform platform;
	private DisseminationCore core;
	private AddInManager addInManager;
	private PublishSubscribeExtension pse;
	
	public MediaSenseManager(){
		// MediaSense Platform initialization
        platform = new MediaSensePlatform(); // Create the platform itself
        // Initialize the platform with chosen LookupService type and chosen Communication type. 
        platform.initalize(LookupServiceInterface.SERVER, CommunicationInterface.TCP); // For Server Lookup and TCP P2P communication
        core = platform.getDisseminationCore(); // Extract the core for accessing the primitive functions
        //////////////////////////////////////
        Log.i("MSINIT", "MediaSense Initialized");
	}
	
	public void loadPubSubExtension(){
		// Set the subscription extension
        addInManager = platform.getAddInManager();
        pse = new PublishSubscribeExtension();
        addInManager.loadAddIn(pse);
	}
    
    public void registerUCI(String uciToRegister){
    	final String uci = uciToRegister;
    	// Create and start a Thread to register to MediaSense
        Thread registerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				core.register(uci);
			}
		});
        registerThread.start();
        Log.i("REGISTER", uci+" registered");
    }
    
    public DisseminationCore getCore(){
    	return core;
    }
    
    public MediaSensePlatform getPlatform(){
    	return platform;
    }
    
    public PublishSubscribeExtension getPubSubExt(){
    	return pse;
    }
	
}
